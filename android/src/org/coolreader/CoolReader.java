// Main Class
package org.coolreader;

import java.io.File;
import java.lang.reflect.Field;

import org.coolreader.crengine.BackgroundThread;
import org.coolreader.crengine.BaseDialog;
import org.coolreader.crengine.BookmarksDlg;
import org.coolreader.crengine.CRDB;
import org.coolreader.crengine.Engine;
import org.coolreader.crengine.FileBrowser;
import org.coolreader.crengine.FileInfo;
import org.coolreader.crengine.History;
import org.coolreader.crengine.OptionsDialog;
import org.coolreader.crengine.ReaderView;
import org.coolreader.crengine.Scanner;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;
import android.text.InputFilter;
import android.text.method.DigitsKeyListener;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Toast;

public class CoolReader extends Activity
{
	Engine mEngine;
	ReaderView mReaderView;
	Scanner mScanner;
	FileBrowser mBrowser;
	FrameLayout mFrame;
	//View startupView;
	History mHistory;
	CRDB mDB;
	private BackgroundThread mBackgroundThread;
	
	public Scanner getScanner()
	{
		return mScanner;
	}
	
	public History getHistory() 
	{
		return mHistory;
	}
	
	public ReaderView getReaderView() 
	{
		return mReaderView;
	}
	
	public CRDB getDB()
	{
		return mDB;
	}
	
	private static String PREF_FILE = "CR3LastBook";
	private static String PREF_LAST_BOOK = "LastBook";
	public String getLastSuccessfullyOpenedBook()
	{
		SharedPreferences pref = getSharedPreferences(PREF_FILE, 0);
		String res = pref.getString(PREF_LAST_BOOK, null);
		pref.edit().putString(PREF_LAST_BOOK, null).commit();
		return res;
	}
	
	public void setLastSuccessfullyOpenedBook( String filename )
	{
		SharedPreferences pref = getSharedPreferences(PREF_FILE, 0);
		pref.edit().putString(PREF_LAST_BOOK, filename).commit();
	}
	
	private boolean mFullscreen = false;
	public boolean isFullscreen() {
		return mFullscreen;
	}

	public void applyFullscreen( Window wnd )
	{
		if ( mFullscreen ) {
			//mActivity.getWindow().requestFeature(Window.)
			wnd.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, 
			        WindowManager.LayoutParams.FLAG_FULLSCREEN );
		} else {
			wnd.setFlags(0, 
			        WindowManager.LayoutParams.FLAG_FULLSCREEN );
		}
	}
	public void setFullscreen( boolean fullscreen )
	{
		if ( mFullscreen!=fullscreen ) {
			mFullscreen = fullscreen;
			applyFullscreen( getWindow() );
		}
	}
	
	int screenOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR;
	public void applyScreenOrientation( Window wnd )
	{
		if ( wnd!=null ) {
			WindowManager.LayoutParams attrs = wnd.getAttributes();
			attrs.screenOrientation = screenOrientation;
			wnd.setAttributes(attrs);
		}
	}

	public void setScreenOrientation( int angle )
	{
		int newOrientation = screenOrientation;
		if ( angle==4 )
			newOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR;
		else if ( (angle&1)!=0 )
			newOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
		else
			newOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
		if ( newOrientation!=screenOrientation ) {
			screenOrientation = newOrientation;
			setRequestedOrientation(screenOrientation);
			applyScreenOrientation(getWindow());
		}
	}

	private Runnable backlightTimerTask = null; 
	private class ScreenBacklightControl
	{
		PowerManager.WakeLock wl = null;
		public ScreenBacklightControl()
		{
		}
		public static final int SCREEN_BACKLIGHT_DURATION_STEPS = 3;
		public static final int SCREEN_BACKLIGHT_TIMER_STEP = 60*1000;
		int backlightCountDown = 0; 
		public void onUserActivity()
		{
			if ( wl==null ) {
				PowerManager pm = (PowerManager)getSystemService(
			            Context.POWER_SERVICE);
				wl = pm.newWakeLock(
			        PowerManager.SCREEN_BRIGHT_WAKE_LOCK
			        | PowerManager.ON_AFTER_RELEASE,
			        "cr3");
			}
			if ( !wl.isHeld() )
				wl.acquire();
			backlightCountDown = SCREEN_BACKLIGHT_DURATION_STEPS;
			if ( backlightTimerTask==null ) {
				backlightTimerTask = new Runnable() {
					public void run() {
						if ( backlightTimerTask!=this )
							return;
						if ( backlightCountDown<=0 )
							release();
						else {
							backlightCountDown--;
							BackgroundThread.instance().postGUI(backlightTimerTask, SCREEN_BACKLIGHT_TIMER_STEP);
						}
					}
				};
				BackgroundThread.instance().postGUI(backlightTimerTask, SCREEN_BACKLIGHT_TIMER_STEP);
			}
		}
		public boolean isHeld()
		{
			return wl!=null && wl.isHeld();
		}
		public void release()
		{
			if ( wl.isHeld() )
				wl.release();
			backlightTimerTask = null;
		}
	}
	
	int initialBatteryState = -1;
	String fileToLoadOnStart = null;
	BroadcastReceiver intentReceiver;
	ScreenBacklightControl backlightControl = new ScreenBacklightControl();
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
		Log.i("cr3", "CoolReader.onCreate() entered");
		super.onCreate(savedInstanceState);
		
		intentReceiver = new BroadcastReceiver() {

			@Override
			public void onReceive(Context context, Intent intent) {
				Log.i("cr3", "Battery state changed. Intent=" + intent);
				int level = intent.getIntExtra("level", 0);
				if ( mReaderView!=null )
					mReaderView.setBatteryState(level);
				else
					initialBatteryState = level;
			}
			
		};
		registerReceiver(intentReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
		
        // testing background thread
    	mBackgroundThread = BackgroundThread.instance();
		mFrame = new FrameLayout(this);
		mEngine = new Engine(this, mBackgroundThread);
		mBackgroundThread.setGUI(mFrame);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		//this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, 
        //       WindowManager.LayoutParams.FLAG_FULLSCREEN );
//		startupView = new View(this) {
//		};
//		startupView.setBackgroundColor(Color.BLACK);
		mReaderView = new ReaderView(this, mEngine, mBackgroundThread);
		File dbdir = getDir("db", Context.MODE_PRIVATE);
		dbdir.mkdirs();
		File dbfile = new File(dbdir, "cr3db.sqlite");
		mDB = new CRDB(dbfile);
       	mHistory = new History(mDB);

       	mScanner = new Scanner(this, mDB, mEngine); //, Environment.getExternalStorageDirectory(), "SD"
		Log.i("cr3", "initializing scanner");
        mScanner.initRoots();

		mBrowser = new FileBrowser(this, mEngine, mScanner, mHistory);
		mFrame.addView(mReaderView);
		mFrame.addView(mBrowser);
//		mFrame.addView(startupView);
		setContentView( mFrame );
		showView(mBrowser);
        Log.i("cr3", "initializing browser");
        mBrowser.init();
        Log.i("cr3", "initializing reader");
        mReaderView.init();
        mBrowser.showDirectory(mScanner.getRoot(), null);
        
        fileToLoadOnStart = null;
		Intent intent = getIntent();
		if ( intent!=null && Intent.ACTION_VIEW.equals(intent.getAction()) ) {
			Uri uri = intent.getData();
			if ( uri!=null ) {
				fileToLoadOnStart = extractFileName(uri);
			}
			intent.setData(null);
		}
		if ( initialBatteryState>=0 )
			mReaderView.setBatteryState(initialBatteryState);
        
        Log.i("cr3", "CoolReader.onCreate() exiting");
    }
    
    public void setScreenBacklightLevel( int percent )
    {
    	if ( percent<-1 )
    		percent = -1;
    	else if ( percent>100 )
    		percent = -1;
    	screenBacklightBrightness = percent;
    	onUserActivity();
    }
    
    private int screenBacklightBrightness = -1; // use default
    private boolean brightnessHackError = false;
    public void onUserActivity()
    {
    	if ( backlightControl==null )
    		return;
    	backlightControl.onUserActivity();
    	// Hack
    	if ( backlightControl.isHeld() )
    	BackgroundThread.guiExecutor.execute(new Runnable() {
			@Override
			public void run() {
				try {
			        Window wnd = getWindow();
			        if ( wnd!=null ) {
			        	LayoutParams attrs =  wnd.getAttributes();
			        	boolean changed = false;
			        	float b;
			        	if ( screenBacklightBrightness>=0 ) {
			        		b = screenBacklightBrightness / 100.0f;
				        	if ( b<0.0f ) // BRIGHTNESS_OVERRIDE_OFF
				        		b = 0.0f;
				        	else if ( b>1.0f )
				        		b = 1.0f; //BRIGHTNESS_OVERRIDE_FULL
			        	} else
			        		b = -1.0f; //BRIGHTNESS_OVERRIDE_NONE
			        	if ( attrs.screenBrightness != b ) {
			        		attrs.screenBrightness = b;
			        		changed = true;
			        	}
			        	// hack to set buttonBrightness field
			        	if ( !brightnessHackError )
			        	try {
				        	Field bb = attrs.getClass().getField("buttonBrightness");
				        	if ( bb!=null ) {
				        		Float oldValue = (Float)bb.get(attrs);
				        		//if ( oldValue==null || oldValue.floatValue()!=0 ) {
				        			bb.set(attrs, Float.valueOf(0.0f));
					        		changed = true;
				        		//}
				        	}
			        	} catch ( Exception e ) {
			        		Log.e("cr3", "WindowManager.LayoutParams.buttonBrightness field is not found, cannot turn buttons backlight off");
			        		brightnessHackError = true;
			        	}
			        	//attrs.buttonBrightness = 0;
			        	if ( changed ) {
			        		Log.d("cr3", "Window attribute changed: " + attrs);
			        		wnd.setAttributes(attrs);
			        	}
			        	//attrs.screenOrientation = LayoutParams.SCREEN_;
			        }
				} catch ( Exception e ) {
					// ignore
				}
			}
    	});
    }
    
    boolean mDestroyed = false;
	@Override
	protected void onDestroy() {

		Log.i("cr3", "CoolReader.onDestroy() entered");
		mDestroyed = true;
		if ( !CLOSE_BOOK_ON_STOP )
			mReaderView.close();
		
		//if ( mReaderView!=null )
		//	mReaderView.close();
		
		//if ( mHistory!=null && mDB!=null ) {
			//history.saveToDB();
		//}
		if ( intentReceiver!=null ) {
			unregisterReceiver(intentReceiver);
			intentReceiver = null;
		}

		if ( mReaderView!=null ) {
			mReaderView.destroy();
		}
		if ( mEngine!=null ) {
			mEngine.uninit();
		}
		if ( mDB!=null ) {
			final CRDB db = mDB;
			mBackgroundThread.executeBackground(new Runnable() {
				public void run() {
					db.close();
				}
			});
		}
//		if ( mBackgroundThread!=null ) {
//			mBackgroundThread.quit();
//		}
			
		mDB = null;
		mReaderView = null;
		mEngine = null;
		mBackgroundThread = null;
		Log.i("cr3", "CoolReader.onDestroy() exiting");
		super.onDestroy();
	}

	private String extractFileName( Uri uri )
	{
		if ( uri!=null ) {
			if ( uri.equals(Uri.parse("file:///")) )
				return null;
			else
				return uri.getPath();
		}
		return null;
	}
	
	@Override
	protected void onNewIntent(Intent intent) {
		Log.i("cr3", "onNewIntent : " + intent);
		if ( mDestroyed ) {
			Log.e("cr3", "engine is already destroyed");
			return;
		}
		String fileToOpen = null;
		if ( Intent.ACTION_VIEW.equals(intent.getAction()) ) {
			Uri uri = intent.getData();
			if ( uri!=null ) {
				fileToOpen = extractFileName(uri);
			}
			intent.setData(null);
		}
		if ( fileToOpen!=null ) {
			// load document
			final String fn = fileToOpen;
			mReaderView.loadDocument(fileToOpen, new Runnable() {
				public void run() {
					showToast("Error occured while loading " + fn);
				}
			});
		}
	}

	@Override
	protected void onPause() {
		Log.i("cr3", "CoolReader.onPause() : saving reader state");
		releaseBacklightControl();
		mReaderView.save();
		super.onPause();
	}
	
	public void releaseBacklightControl()
	{
		backlightControl.release();
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		Log.i("cr3", "CoolReader.onPostCreate()");
		super.onPostCreate(savedInstanceState);
	}

	@Override
	protected void onPostResume() {
		Log.i("cr3", "CoolReader.onPostResume()");
		super.onPostResume();
	}

	private boolean restarted = false;
	@Override
	protected void onRestart() {
		Log.i("cr3", "CoolReader.onRestart()");
		restarted = true;
		super.onRestart();
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		Log.i("cr3", "CoolReader.onRestoreInstanceState()");
		super.onRestoreInstanceState(savedInstanceState);
	}

	@Override
	protected void onResume() {
		Log.i("cr3", "CoolReader.onResume()");
		super.onResume();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		Log.i("cr3", "CoolReader.onSaveInstanceState()");
		super.onSaveInstanceState(outState);
	}

	static final boolean LOAD_LAST_DOCUMENT_ON_START = true; 
	
	@Override
	protected void onStart() {
		Log.i("cr3", "CoolReader.onStart()");
		super.onStart();
		
		backlightControl.onUserActivity();
		
		if ( mReaderView!=null && currentView==mReaderView && mReaderView.isBookLoaded() ) {
			showReader();
			return;
		}
		
		//!stopped && 
		if ( restarted && mReaderView!=null && mReaderView.isBookLoaded() ) {
	        restarted = false;
	        return;
		}
		if ( !stopped ) {
	        mEngine.showProgress( 5, R.string.progress_starting_cool_reader );
			//mEngine.setHyphenationDictionary( HyphDict.RUSSIAN );
		}
        //Log.i("cr3", "waiting for engine tasks completion");
        //engine.waitTasksCompletion();
		restarted = false;
		stopped = false;
		final String fileName = fileToLoadOnStart;
        mEngine.execute(new Engine.EngineTask() {

			public void done() {
		        Log.i("cr3", "trying to load last document");
				if ( fileName!=null || LOAD_LAST_DOCUMENT_ON_START ) {
					//currentView=mReaderView;
					if ( fileName!=null ) {
						mReaderView.loadDocument(fileName, new Runnable() {
							public void run() {
								// cannot open recent book: load another one
								Log.e("cr3", "Cannot open document " + fileToLoadOnStart + " starting file browser");
								showBrowser(null);
							}
						});
					} else {
						mReaderView.loadLastDocument(new Runnable() {
							public void run() {
								// cannot open recent book: load another one
								Log.e("cr3", "Cannot open last document, starting file browser");
								showBrowser(null);
							}
						});
					}
				} else {
					showBrowser(null);
				}
				fileToLoadOnStart = null;
			}

			public void fail(Exception e) {
			}

			public void work() throws Exception {
				// do nothing
			}
        	
        });
	}

	public final static boolean CLOSE_BOOK_ON_STOP = false;
	private boolean stopped = false;
	@Override
	protected void onStop() {
		Log.i("cr3", "CoolReader.onStop() entering");
		stopped = true;
		// will close book at onDestroy()
		if ( CLOSE_BOOK_ON_STOP )
			mReaderView.close();
		super.onStop();
		Log.i("cr3", "CoolReader.onStop() exiting");
	}

	private View currentView;
	public void showView( View view )
	{
		if ( currentView==view )
			return;
		mFrame.bringChildToFront(view);
		for ( int i=0; i<mFrame.getChildCount(); i++ ) {
			View v = mFrame.getChildAt(i);
			v.setVisibility(view==v?View.VISIBLE:View.INVISIBLE);
		}
		currentView = view;
	}
	
	public void showReader()
	{
		Log.v("cr3", "showReader() is called");
		showView(mReaderView);
	}
	
	public boolean isBookOpened()
	{
		return mReaderView.isBookLoaded();
	}
	
	public void loadDocument( FileInfo item )
	{
		//showView(readerView);
		//setContentView(readerView);
		mReaderView.loadDocument(item);
	}
	
	public void showBrowser( final FileInfo fileToShow )
	{
		Log.v("cr3", "showBrowser() is called");
		if ( currentView == mReaderView )
			mReaderView.save();
		mEngine.runInGUI( new Runnable() {
			public void run() {
				showView(mBrowser);
		        mEngine.hideProgress();
		        if ( fileToShow==null )
		        	mBrowser.showLastDirectory();
		        else
		        	mBrowser.showDirectory(fileToShow, fileToShow);
			}
		});
	}

	public void showBrowserRecentBooks()
	{
		Log.v("cr3", "showBrowserRecentBooks() is called");
		if ( currentView == mReaderView )
			mReaderView.save();
		mEngine.runInGUI( new Runnable() {
			public void run() {
				showView(mBrowser);
		        mEngine.hideProgress();
	        	mBrowser.showRecentBooks();
			}
		});
	}

	public void showBrowserRoot()
	{
		Log.v("cr3", "showBrowserRoot() is called");
		if ( currentView == mReaderView )
			mReaderView.save();
		mEngine.runInGUI( new Runnable() {
			public void run() {
				showView(mBrowser);
		        mEngine.hideProgress();
	        	mBrowser.showRootDirectory();
			}
		});
	}

	private void fillMenu(Menu menu) {
		menu.clear();
	    MenuInflater inflater = getMenuInflater();
	    if ( currentView==mReaderView )
	    	inflater.inflate(R.menu.cr3_reader_menu, menu);
	    else {
	    	inflater.inflate(R.menu.cr3_browser_menu, menu);
	    	if ( !isBookOpened() ) {
	    		MenuItem item = menu.findItem(R.id.book_back_to_reading);
	    		if ( item!=null )
	    			item.setEnabled(false);
	    	}
	    }
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		fillMenu(menu);
	    return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		fillMenu(menu);
	    return true;
	}

	public void showToast( String msg )
	{
		Toast toast = Toast.makeText(this, msg, Toast.LENGTH_LONG);
		toast.show();
	}

	public interface InputHandler {
		boolean validate( String s ) throws Exception;
		void onOk( String s ) throws Exception;
		void onCancel();
	};
	
	public static class InputDialog extends BaseDialog {
		private InputHandler handler;
		private EditText input;
		public InputDialog( Activity activity, final String title, boolean isNumberEdit, final InputHandler handler )
		{
			super(activity, R.string.dlg_button_ok, R.string.dlg_button_cancel );
			this.handler = handler;
			setTitle(title);
	        input = new EditText(getContext());
	        if ( isNumberEdit )
		        input.getText().setFilters(new InputFilter[] {
		        	new DigitsKeyListener()        
		        });
	        setView(input);
		}
		@Override
		protected void onNegativeButtonClick() {
            cancel();
            handler.onCancel();
		}
		@Override
		protected void onPositiveButtonClick() {
            String value = input.getText().toString().trim();
            try {
            	if ( handler.validate(value) )
            		handler.onOk(value);
            	else
            		handler.onCancel();
            } catch ( Exception e ) {
            	handler.onCancel();
            }
            cancel();
		}
	}
	
	public void showInputDialog( final String title, boolean isNumberEdit, final InputHandler handler )
	{
        final InputDialog dlg = new InputDialog(this, title, isNumberEdit, handler);
        dlg.show();
	}

	private int orientationFromSensor = 0;
	public int getOrientationFromSensor()
	{
		return orientationFromSensor;
	}
	
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		// pass
		orientationFromSensor = newConfig.orientation==Configuration.ORIENTATION_LANDSCAPE ? 1 : 0;
		//final int orientation = newConfig.orientation==Configuration.ORIENTATION_LANDSCAPE ? ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE : ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
//		if ( orientation!=screenOrientation ) {
//			Log.d("cr3", "Screen orientation has been changed: ask for change");
//			AlertDialog.Builder dlg = new AlertDialog.Builder(this);
//			dlg.setTitle(R.string.win_title_screen_orientation_change_apply);//R.string.win_title_options_apply);
//			dlg.setPositiveButton(R.string.dlg_button_ok, new OnClickListener() {
//				public void onClick(DialogInterface arg0, int arg1) {
//					//onPositiveButtonClick();
//					Properties oldSettings = mReaderView.getSettings();
//					Properties newSettings = new Properties(oldSettings);
//					newSettings.setInt(ReaderView.PROP_APP_SCREEN_ORIENTATION, orientation==ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE ? 1 : 0);
//					mReaderView.setSettings(newSettings, oldSettings);
//				}
//			});
//			dlg.setNegativeButton(R.string.dlg_button_cancel, new OnClickListener() {
//				public void onClick(DialogInterface arg0, int arg1) {
//					//onNegativeButtonClick();
//				}
//			});
//		}
		super.onConfigurationChanged(newConfig);
	}

	String[] mFontFaces;

	public void showOptionsDialog()
	{
		final CoolReader _this = this;
		mBackgroundThread.executeBackground(new Runnable() {
			public void run() {
				mFontFaces = mEngine.getFontFaceList();
				mBackgroundThread.executeGUI(new Runnable() {
					public void run() {
						OptionsDialog dlg = new OptionsDialog(_this, mReaderView, mFontFaces);
						dlg.show();
					}
				});
			}
		});
	}
	
	public void showBookmarksDialog()
	{
		BookmarksDlg dlg = new BookmarksDlg(this, mReaderView);
		dlg.show();
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int itemId = item.getItemId();
		if ( mReaderView.onMenuItem(itemId))
			return true; // processed by ReaderView
		// other commands
		switch ( itemId ) {
		case R.id.book_sort_order:
			showToast("Sorry, this option is not yet supported");
			break;
		case R.id.book_root:
			mBrowser.showRootDirectory();
			return true;
		case R.id.book_back_to_reading:
			if ( isBookOpened() )
				showReader();
			else
				showToast("No book opened");
			return true;
		default:
			return false;
			//return super.onOptionsItemSelected(item);
		}
		return true;
	}
	
	public void showGoToPageDialog() {
		showInputDialog("Enter page number", true, new InputHandler() {
			int pageNumber = 0;
			public boolean validate(String s) {
				pageNumber = Integer.valueOf(s); 
				return pageNumber>0;
			}
			public void onOk(String s) {
				mReaderView.goToPage(pageNumber);
			}
			public void onCancel() {
			}
		});
	}
	public void showGoToPercentDialog() {
		showInputDialog("Enter position %", true, new InputHandler() {
			int percent = 0;
			public boolean validate(String s) {
				percent = Integer.valueOf(s); 
				return percent>=0 && percent<=100;
			}
			public void onOk(String s) {
				mReaderView.goToPercent(percent);
			}
			public void onCancel() {
			}
		});
	}


}
