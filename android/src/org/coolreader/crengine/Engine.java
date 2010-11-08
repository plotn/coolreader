package org.coolreader.crengine;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.coolreader.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.os.Environment;
import android.util.Log;

/**
 * CoolReader Engine class.
 *
 * Only one instance is allowed.
 */
public class Engine {
	
	private final Activity mActivity;
	private final BackgroundThread mBackgroundThread;
	//private final View mMainView;
	//private final ExecutorService mExecutor = Executors.newFixedThreadPool(1);
	

	public interface EngineTask {
		public void work() throws Exception;
		public void done();
		public void fail( Exception e );
	}
	
//	public static class FatalError extends RuntimeException {
//		private Engine engine;
//		private String msg;
//		public FatalError( Engine engine, String msg )
//		{
//			this.engine = engine;
//			this.msg = msg;
//		}
//		public void handle()
//		{
//			engine.fatalError(msg);
//		}
//	}
	

	private class TaskHandler implements Runnable {
		final EngineTask task;
		public TaskHandler( EngineTask task )
		{
			this.task = task;
		}
		public void run() {
			try {
				Log.i("cr3", "running task " + task.getClass().getSimpleName() + " in engine thread");
				if ( !initialized )
					throw new IllegalStateException("Engine not initialized");
				// run task
				task.work();
				Log.i("cr3", "exited task.work() " + task.getClass().getSimpleName() + " in engine thread");
				// post success callback
				mBackgroundThread.postGUI(new Runnable() {
					public void run() {
						Log.i("cr3", "running task.done() " + task.getClass().getSimpleName() + " in gui thread");
						task.done();
					}
				});
//			} catch ( final FatalError e ) {
				//TODO:
//				Handler h = view.getHandler();
//				
//				if ( h==null ) {
//					View root = view.getRootView();
//					h = root.getHandler();
//				}
//				if ( h==null ) {
//					//
//					e.handle();
//				} else {
//					h.postAtFrontOfQueue(new Runnable() {
//						public void run() {
//							e.handle();
//						}
//					});
//				}
			} catch ( final Exception e ) {
				// post error callback
				mBackgroundThread.postGUI(new Runnable() {
					public void run() {
						Log.e("cr3", "running task.fail("+e.getMessage()+") " + task.getClass().getSimpleName() + " in gui thread ");
						task.fail(e);
					}
				});
			}
		}
	}

	/**
	 * Execute task in Engine thread
	 * @param task is task to execute
	 */
	public void execute( final EngineTask task )
	{
		
		Log.d("cr3", "executing task " + task.getClass().getSimpleName());
		TaskHandler taskHandler = new TaskHandler( task );
		mBackgroundThread.executeBackground( taskHandler );
	}
	
	/**
	 * Schedule Runnable for execution in GUI thread after all current Engine queue tasks done.  
	 * @param task
	 */
	public void runInGUI( final Runnable task )
	{
		execute( new EngineTask() {

			public void done() {
				mBackgroundThread.postGUI(task);
			}

			public void fail(Exception e) {
				// do nothing
			}

			public void work() throws Exception {
				// do nothing
			}
		});
	}

	public void fatalError( String msg)
	{
		AlertDialog dlg = new AlertDialog.Builder(mActivity).setMessage(msg).setTitle("CoolReader fatal error").show();
		try {
			Thread.sleep(10);
		} catch ( InterruptedException e ) {
			// do nothing
		}
		dlg.dismiss();
		mActivity.finish();
	}
	
	private ProgressDialog mProgress;
	private boolean enable_progress = true; 
	private boolean progressShown = false;
	private static int PROGRESS_STYLE = ProgressDialog.STYLE_HORIZONTAL;
	public void showProgress( final int mainProgress, final int resourceId )
	{
		showProgress( mainProgress, mActivity.getResources().getString(resourceId) );
	}
	private void showProgress( final int mainProgress, final String msg )
	{
		if ( mainProgress==10000 ) {
			Log.v("cr3", "mainProgress==10000 : calling hideProgress");
			hideProgress();
			return;
		}
		Log.v("cr3", "showProgress(" + mainProgress + ", \"" + msg + "\") is called");
		if ( enable_progress ) {
			mBackgroundThread.executeGUI( new Runnable() {
				public void run() {
					// show progress
					Log.v("cr3", "showProgress() - in GUI thread");
					if ( mProgress==null ) {
						if ( PROGRESS_STYLE == ProgressDialog.STYLE_HORIZONTAL ) {
							mProgress = new ProgressDialog(mActivity);
							mProgress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
							mProgress.setIcon(R.drawable.cr3_logo);
							mProgress.setMax(10000);
							mProgress.setCancelable(false);
							mProgress.setProgress(mainProgress);
							mProgress.setTitle(mActivity.getResources().getString(R.string.progress_please_wait));
							mProgress.setMessage(msg);
							mProgress.show();
						} else {
							mProgress = ProgressDialog.show(mActivity, "Please Wait", msg);
							mProgress.setCancelable(false);
							mProgress.setProgress(mainProgress);
						}
					} else {
						mProgress.setProgress(mainProgress);
						mProgress.setMessage(msg);
						if ( !mProgress.isShowing() ) {
							mProgress.show();
							progressShown = true;
						}
					}
				}
			});
		}
	}
	
	public void hideProgress()
	{
		Log.v("cr3", "hideProgress() is called");
		mBackgroundThread.executeGUI( new Runnable() {
			public void run() {
				// hide progress
				if ( mProgress!=null ) {
//					if ( mProgress.isShowing() )
//						mProgress.hide();
					progressShown = false;
					mProgress.dismiss();
					mProgress = null;
					Log.v("cr3", "hideProgress() - in GUI thread");
				}
			}
		});
	}
	
	public boolean isProgressShown()
	{
		return progressShown;
	}
	
	public String loadResourceUtf8( int id )
	{
		try {
			InputStream is = this.mActivity.getResources().openRawResource( id );
			return loadResourceUtf8(is);
		} catch ( Exception e ) {
			Log.e("cr3", "cannot load resource");
			return null;
		}
	}
	
	public String loadResourceUtf8( InputStream is )
	{
		try {
			int available = is.available();
			if ( available<=0 )
				return null;
			byte buf[] = new byte[available];
			if ( is.read(buf)!=available )
				throw new IOException("Resource not read fully");
			is.close();
			String utf8 = new String(buf, 0, available, "UTF8");
			return utf8;
		} catch ( Exception e ) {
			Log.e("cr3", "cannot load resource");
			return null;
		}
	}
	
	public byte[] loadResourceBytes( int id )
	{
		try {
			InputStream is = this.mActivity.getResources().openRawResource( id );
			return loadResourceBytes(is);
		} catch ( Exception e ) {
			Log.e("cr3", "cannot load resource");
			return null;
		}
	}
	
	public byte[] loadResourceBytes( InputStream is )
	{
		try {
			int available = is.available();
			if ( available<=0 )
				return null;
			byte buf[] = new byte[available];
			if ( is.read(buf)!=available )
				throw new IOException("Resource not read fully");
			is.close();
			return buf;
		} catch ( Exception e ) {
			Log.e("cr3", "cannot load resource");
			return null;
		}
	}
	
	/**
	 * Initialize CoolReader Engine
	 * @param fontList is array of .ttf font pathnames to load
	 */
	public Engine( Activity activity, BackgroundThread backgroundThread )
	{
		this.mActivity = activity;
		this.mBackgroundThread = backgroundThread;
		//this.mMainView = mainView;
		Log.i("cr3", "Engine() : scheduling init task");
		mBackgroundThread.executeBackground( new Runnable() {
			public void run()
			{
				try {
					Log.i("cr3", "Engine() : running init() in engine thread");
					init();
//					android.view.ViewRoot.getRunQueue().post(new Runnable() {
//						public void run() {
//							
//						}
//					});
				} catch ( final Exception e ) {
					Log.e("cr3", "Exception while initializing Engine", e);
//					handler.post(new Runnable() {
//						public void run() {
//							// TODO: fatal error
//						}
//					});
				}
			}
		});			
	}

	private native boolean initInternal( String[] fontList );
	private native void uninitInternal();
	private native String[] getFontFaceListInternal();
	private native boolean setCacheDirectoryInternal( String dir, int size  );
    private native boolean scanBookPropertiesInternal( FileInfo info );
    private static final int HYPH_NONE = 0; 
    private static final int HYPH_ALGO = 1; 
    private static final int HYPH_DICT = 2; 
    private native boolean setHyphenationMethod( int type, byte[] dictData );
    
    public enum HyphDict {
    	NONE(HYPH_NONE, 0, "[None]"),
    	ALGORITHM(HYPH_ALGO,0, "[Algorythmic]"),
    	RUSSIAN(HYPH_DICT,R.raw.russian_enus_hyphen, "Russian"),
    	ENGLISH(HYPH_DICT,R.raw.english_us_hyphen, "English US"),
    	GERMAN(HYPH_DICT,R.raw.german_hyphen, "German"),
    	UKRAINIAN(HYPH_DICT,R.raw.ukrain_hyphen, "Ukrainian"),
    	SPANISH(HYPH_DICT,R.raw.spanish_hyphen, "Spanish"),
    	FRENCH(HYPH_DICT,R.raw.french_hyphen, "French"),
    	BULGARIAN(HYPH_DICT,R.raw.bulgarian_hyphen, "Bulgarian"),
    	;
    	public final int type;
    	public final int resource;
    	public final String name;
    	private HyphDict( int type, int resource, String name ) {
    		this.type = type;
    		this.resource = resource;
    		this.name = name;
    	}
    };
    
    public void setHyphenationDictionary( final HyphDict dict )
    {
    	execute( new EngineTask() {

			public void done() {
				//
			}

			public void fail(Exception e) {
				//
			}

			public void work() throws Exception {
				byte[] data = null;
				if ( dict.type==HYPH_DICT && dict.resource!=0 ) {
					data = loadResourceBytes( dict.resource );
				}
				setHyphenationMethod(dict.type, data);
			}
    	});
    }
    
    public boolean scanBookProperties(FileInfo info)
    {
		if ( !initialized )
			throw new IllegalStateException("CREngine is not initialized");
    	return scanBookPropertiesInternal( info );
    }
	
	public String[] getFontFaceList()
	{
		if ( !initialized )
			throw new IllegalStateException("CREngine is not initialized");
		return getFontFaceListInternal();
	}
	
	final int CACHE_DIR_SIZE = 32000000;
	
	private String createCacheDir( File baseDir, String subDir )
	{
		String cacheDirName = null;
		if ( baseDir.isDirectory() ) {
			if ( baseDir.canWrite() ) {
				if ( subDir!=null ) {
					baseDir = new File(baseDir, subDir);
					baseDir.mkdir();
				}
				if ( baseDir.exists() && baseDir.canWrite() ) {
					File cacheDir = new File(baseDir, ".cache");
					if ( cacheDir.exists() || cacheDir.mkdirs() ) {
						if ( cacheDir.canWrite() ) {
							cacheDirName = cacheDir.getAbsolutePath();
						}
					}
				}
			} else {
				Log.i("cr3", baseDir.toString() + " is read only");
			}
		} else {
			Log.i("cr3", baseDir.toString() + " is not found");
		}
		return cacheDirName;
	}
	
	private void initCacheDirectory()
	{
		String cacheDirName = null;
		// SD card
		cacheDirName = createCacheDir( Environment.getExternalStorageDirectory(), "Books" );
		// internal SD card on Nook
		if ( cacheDirName==null )
			cacheDirName = createCacheDir( new File("/system/media/sdcard"), "Books" );
		// internal flash
		if ( cacheDirName==null ) {
			File cacheDir = mActivity.getDir("cache", Context.MODE_PRIVATE);
			if ( cacheDir.isDirectory() && cacheDir.canWrite() )
				cacheDirName = cacheDir.getAbsolutePath();
		}
		// set cache directory for engine
		if ( cacheDirName!=null ) {
			Log.i("cr3", cacheDirName + " will be used for cache, maxCacheSize=" + CACHE_DIR_SIZE);
			setCacheDirectoryInternal(cacheDirName, CACHE_DIR_SIZE);
		}
	}
	
	private void init() throws IOException
	{
		if ( initialized )
			throw new IllegalStateException("Already initialized");
    	installLibrary();
    	String[] fonts = findFonts();
		if ( !initInternal( fonts ) )
			throw new IOException("Cannot initialize CREngine JNI");
		// Initialization of cache directory
		initCacheDirectory();
		initialized = true;
	}
	
//	public void waitTasksCompletion()
//	{
//        Log.i("cr3", "waiting for engine tasks completion");
//		try {
//			mExecutor.awaitTermination(0, TimeUnit.SECONDS);
//		} catch (InterruptedException e) {
//			// ignore
//		}
//	}
	
	/**
	 * Uninitialize engine.
	 */
	public void uninit()
	{
		Log.i("cr3", "Engine.uninit() is called");
		BackgroundThread.backgroundExecutor.execute(new Runnable() {
			public void run() {
				Log.i("cr3", "Engine.uninit() : in background thread");
				if ( initialized ) {
					uninitInternal();
					initialized = false;
				}
			}
		});
		//TODO:
		//waitTasksCompletion();
	}
	
	protected void finalize() throws Throwable
	{
		if ( initialized ) {
			uninitInternal();
			initialized = false;
		}
	}
	
	static private boolean initialized = false;

	private String[] findFonts()
	{
		File[] fontDirs = {
				new File( Environment.getRootDirectory(), "fonts"),
				new File( Environment.getExternalStorageDirectory(), "fonts"),
				new File( new File("/system/media/sdcard"), "fonts") //Nook internal SD
		};
		ArrayList<String> fontPaths = new ArrayList<String>(); 
		for ( File fontDir : fontDirs ) {
			if ( fontDir.isDirectory() ) {
				Log.v("cr3", "Scanning directory " + fontDir.getAbsolutePath() + " for font files");
				// get font names
				String[] fileList = fontDir.list(
						new FilenameFilter() { 
							public boolean  accept(File  dir, String  filename)
							{
								return filename.toLowerCase().endsWith(".ttf") && !filename.endsWith("Fallback.ttf");
							}
						});
				// append path
				for ( int i=0; i<fileList.length; i++ ) {
					String pathName = new File(fontDir, fileList[i]).getAbsolutePath();
					fontPaths.add( pathName );
					Log.v("cr3", "found font: " + pathName);
				}
			}
		}
		return fontPaths.toArray(new String[] {});
	}
	
	private boolean force_install_library = false;
	private void installLibrary()
	{
		try {
			if ( force_install_library )
				throw new Exception("forcing install");
			// try loading library w/o manual installation
			Log.i("cr3", "trying to load library cr3engine w/o installation");
			System.loadLibrary("cr3engine");
			Log.i("cr3", "cr3engine loaded successfully");
		} catch ( Exception ee ) {
			Log.i("cr3", "cr3engine not found using standard paths, will install manually");
			File sopath = mActivity.getDir("libs", Context.MODE_PRIVATE);
			File soname = new File(sopath, "libcr3engine.so");
			try {
				sopath.mkdirs();
		    	File zip = new File(mActivity.getPackageCodePath());
		    	ZipFile zipfile = new ZipFile(zip);
		    	ZipEntry zipentry = zipfile.getEntry("lib/armeabi/libcr3engine.so");
		    	if ( !soname.exists() || zipentry.getSize()!=soname.length() ) {
			    	InputStream is = zipfile.getInputStream(zipentry);
					OutputStream os = new FileOutputStream(soname);
			        Log.i("cr3", "Installing JNI library " + soname.getAbsolutePath());
					final int BUF_SIZE = 0x10000;
					byte[] buf = new byte[BUF_SIZE];
					int n;
					while ((n = is.read(buf)) > 0)
					    os.write(buf, 0, n);
			        is.close();
			        os.close();
		    	} else {
			        Log.i("cr3", "JNI library " + soname.getAbsolutePath() + " is up to date");
		    	}
				System.load(soname.getAbsolutePath());
			} catch ( Exception e ) {
		        Log.e("cr3", "cannot install cr3engine library", e);
			}
		}
	}
	
}
