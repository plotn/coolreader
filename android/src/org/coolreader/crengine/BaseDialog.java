package org.coolreader.crengine;

import org.coolreader.CoolReader;
import org.coolreader.R;

import android.app.Dialog;
import android.content.DialogInterface;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

public class BaseDialog extends Dialog {

	View layoutView;
	ViewGroup buttonsLayout;
	public ViewGroup upperTextLayout;
	ViewGroup contentsLayout;
	BaseActivity activity;
	String title;
	String upperText;
	boolean needCancelButton;
	String dlgName;
	int positiveButtonImage;
	int positiveButtonContentDescriptionId = R.string.dlg_button_ok;
	int negativeButtonImage;
	int negativeButtonContentDescriptionId = R.string.action_go_back;
	int thirdButtonImage;
	int thirdButtonContentDescriptionId;
	int addButtonImage;
	int addButtonContentDescriptionId;

	public void setPositiveButtonImage(int imageId, int descriptionId) {
		positiveButtonImage = imageId;
		positiveButtonContentDescriptionId = descriptionId;
	}
	public void setNegativeButtonImage(int imageId, int descriptionId) {
		negativeButtonImage = imageId;
		negativeButtonContentDescriptionId = descriptionId;
	}
	public void setThirdButtonImage(int imageId, int descriptionId) {
		thirdButtonImage = imageId;
		thirdButtonContentDescriptionId = descriptionId;
	}
	public void setAddButtonImage(int imageId, int descriptionId) {
		addButtonImage = imageId;
		addButtonContentDescriptionId = descriptionId;
	}
	
	public static final boolean DARK_THEME = !DeviceInfo.isForceHCTheme(BaseActivity.getScreenForceEink());
	public BaseDialog(String dlgName, BaseActivity activity )
	{
		this(dlgName, activity, "", false, false );
	}
	public BaseDialog(String dlgName, BaseActivity activity, String title, boolean showNegativeButton, boolean windowed )
	{
		this(dlgName, activity, title, showNegativeButton, activity.isFullscreen(), activity.isNightMode(), windowed );
	}
	public BaseDialog(String dlgName, BaseActivity activity, String title, boolean showNegativeButton, boolean fullscreen, boolean dark, boolean windowed )
	{
		//super(activity, fullscreen ? R.style.Dialog_Fullscreen : R.style.Dialog_Normal);
		//super(activity, fullscreen ? R.style.Dialog_Fullscreen : android.R.style.Theme_Dialog); //android.R.style.Theme_Light_NoTitleBar_Fullscreen : android.R.style.Theme_Light
		super(activity,
				windowed ? activity.getCurrentTheme().getDialogThemeId() :
				(fullscreen
				? activity.getCurrentTheme().getFullscreenDialogThemeId()
				: activity.getCurrentTheme().getDialogThemeId()
				));
		setOwnerActivity(activity);
		this.activity = activity;
		this.title = title;
		this.needCancelButton = showNegativeButton;
		this.dlgName = dlgName;
		getWindow().requestFeature(Window.FEATURE_NO_TITLE);
//		requestWindowFeature(Window.FEATURE_OPTIONS_PANEL);
		if (!DeviceInfo.isEinkScreen(BaseActivity.getScreenForceEink())) {
			WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
			lp.alpha = 1.0f;
			lp.dimAmount = 0.0f;
			lp.format = DeviceInfo.getPixelFormat(BaseActivity.getScreenForceEink());
			lp.gravity = Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL;
			lp.horizontalMargin = 0;
			lp.verticalMargin = 0;
			lp.windowAnimations = 0;
			lp.layoutAnimationParameters = null;
			//lp.memoryType = WindowManager.LayoutParams.MEMORY_TYPE_PUSH_BUFFERS;
			getWindow().setAttributes(lp);
		}
		Log.i("cr3", "BaseDialog.window=" + getWindow());
        setCancelable(true);
        setOnDismissListener(dialog -> onClose());
		setOnShowListener(dialog -> whenShow());
        onCreate();
	}

	protected void whenShow() {
		// when dialog is shown
		if (activity instanceof CoolReader) {
			CoolReader cr = (CoolReader)activity;
			cr.getmBaseDialog().put(this.dlgName,this);
			Window wnd = cr.getWindow();
			if (wnd != null) {
				WindowManager.LayoutParams attrs = wnd.getAttributes();
				Window wnd2 = getWindow();
				if (wnd2 != null) {
					WindowManager.LayoutParams attrs2 = wnd2.getAttributes();
					attrs2.screenBrightness = attrs.screenBrightness;
					wnd2.setAttributes(attrs2);
				}
			}
		}
	}

	public void setView( View view )
	{
		this.view = view;
		if ( layoutView==null ) {
			layoutView = createLayout(view);
			setContentView(layoutView);
		}
		contentsLayout.removeAllViews();
		activity.tintViewIcons(view);
		contentsLayout.addView(view);
	}
	
	protected void onPositiveButtonClick()
	{
		// override it
		dismiss();
	}
	
	protected void onNegativeButtonClick()
	{
		// override it
		dismiss();
	}

	protected void onThirdButtonClick()
	{
		// override it
		dismiss();
	}

	protected void onAddButtonClick()
	{
		// override it
		dismiss();
	}

	protected void createButtonsPane( ViewGroup parent, ViewGroup layout )
	{
		//getWindow().getDecorView().getWidth()
		ImageButton positiveButton = layout.findViewById(R.id.base_dlg_btn_positive);
		ImageButton negativeButton = layout.findViewById(R.id.base_dlg_btn_negative);
		ImageButton backButton = layout.findViewById(R.id.base_dlg_btn_back);
		ImageButton addButton = layout.findViewById(R.id.base_dlg_btn_add);
		activity.tintViewIcons(layout);
		if (positiveButtonImage != 0) {
			positiveButton.setImageResource(positiveButtonImage);
			if (positiveButtonContentDescriptionId != 0)
				Utils.setContentDescription(positiveButton, getContext().getString(positiveButtonContentDescriptionId));
			//backButton.setImageResource(positiveButtonImage);
			activity.tintViewIcons(positiveButton,true);
		}
		if (thirdButtonImage != 0) {
			negativeButton.setImageResource(thirdButtonImage);
			if (thirdButtonContentDescriptionId != 0)
				Utils.setContentDescription(negativeButton, getContext().getString(thirdButtonContentDescriptionId));
			activity.tintViewIcons(negativeButton,true);
		}
		if (addButtonImage != 0) {
			addButton.setImageResource(addButtonImage);
			if (addButtonContentDescriptionId != 0) {
				Utils.setContentDescription(addButton, getContext().getString(addButtonContentDescriptionId));
				addButton.setOnClickListener(v -> onAddButtonClick());
			}
			activity.tintViewIcons(addButton,true);
		}
		if (negativeButtonImage != 0) {
			if (thirdButtonImage == 0) {
				negativeButton.setImageResource(negativeButtonImage);
				if (negativeButtonContentDescriptionId != 0)
					Utils.setContentDescription(negativeButton, getContext().getString(negativeButtonContentDescriptionId));
			}
			backButton.setImageResource(negativeButtonImage);
			if (negativeButtonContentDescriptionId != 0)
				Utils.setContentDescription(backButton, getContext().getString(negativeButtonContentDescriptionId));
		}
		if (needCancelButton) {
			//layout.removeView(backButton);
			if (thirdButtonImage == 0) {
				layout.removeView(negativeButton);
			} else {
				negativeButton.setOnClickListener(v -> onThirdButtonClick());
			}
			positiveButton.setOnClickListener(v -> onPositiveButtonClick());
			//negativeButton.setOnClickListener(new View.OnClickListener() {
			backButton.setOnClickListener(v -> onNegativeButtonClick());
		} else {
			layout.removeView(positiveButton);
			layout.removeView(negativeButton);
			if (title != null) {
				backButton.setOnClickListener(v -> onPositiveButtonClick());
			} else {
				parent.removeView(layout);
                buttonsLayout = null;
			}
		}
		if (title != null)
			setTitle(title);
		if (upperText != null)
			setUpperText(upperText);
		if (buttonsLayout != null) {
			buttonsLayout.setOnTouchListener((v, event) -> {
				if (event.getAction() == MotionEvent.ACTION_DOWN) {
					int x = (int)event.getX();
					int dx = v.getWidth();
					if (x < dx / 3) {
						if (needCancelButton)
							onNegativeButtonClick();
						else
							onPositiveButtonClick();
					} else if (x > dx * 2 / 3) {
						onPositiveButtonClick();
					}
					return true;
				}
				return false;
			});
		}
	}

	@Override
	public void setTitle(CharSequence title) {
		this.title = String.valueOf(title);
		if (buttonsLayout != null) {
	        TextView lbl = buttonsLayout.findViewById(R.id.base_dlg_title);
	        if (lbl != null)
	        	lbl.setText(title != null ? title : "");
		}
	}

	public void setUpperText(CharSequence upperText) {
		this.upperText = String.valueOf(upperText);
		if (upperTextLayout != null) {
			TextView lbl = upperTextLayout.findViewById(R.id.base_dlg_upper_text);
			if (lbl != null)
				lbl.setText(upperText != null ? upperText : "");
		}
	}

	protected void updateGlobalMargin(ViewGroup v,
			boolean left, boolean top, boolean right, boolean bottom) {
		if (v == null) return;
		ViewGroup.LayoutParams lp = v.getLayoutParams();
		int globalMargins = activity.settings().getInt(Settings.PROP_GLOBAL_MARGIN, 0);
		if (globalMargins > 0)
			if (lp instanceof ViewGroup.MarginLayoutParams) {
				if (top) ((ViewGroup.MarginLayoutParams) lp).topMargin = globalMargins;
				if (bottom) ((ViewGroup.MarginLayoutParams) lp).bottomMargin = globalMargins;
				if (left) ((ViewGroup.MarginLayoutParams) lp).leftMargin = globalMargins;
				if (right) ((ViewGroup.MarginLayoutParams) lp).rightMargin = globalMargins;
			}
	}

	protected View createLayout( View view )
	{
        LayoutInflater mInflater = LayoutInflater.from(getContext());
        ViewGroup layout = (ViewGroup)mInflater.inflate(R.layout.base_dialog, null);
		upperTextLayout = layout.findViewById(R.id.base_dlg_upper_text_panel);
		buttonsLayout = layout.findViewById(R.id.base_dlg_button_panel);
		if (upperText == null) {
			layout.removeView(upperTextLayout);
			updateGlobalMargin(buttonsLayout, true, true, true, false);
		}
		if (buttonsLayout != null) {
            if ( needCancelButton || title != null) {
            	createButtonsPane(layout, buttonsLayout);
            } else {
            	layout.removeView(buttonsLayout);
                buttonsLayout = null;
            }
        }
		contentsLayout = layout.findViewById(R.id.base_dialog_content_view);
        contentsLayout.addView(view);

        updateGlobalMargin(contentsLayout, true, false, true, true);
		updateGlobalMargin(upperTextLayout, true, true, true, false);

		setTitle(title);
		setUpperText(upperText);

		return layout;
	}
	
	protected void onCreate() {
		// when dialog is created
		Log.d("DLG","BaseDialog.onCreate()");
		activity.onDialogCreated(this);
	}
	
	protected void onClose() {
		// when dialog is closed
		if (activity instanceof CoolReader) {
			CoolReader cr = (CoolReader)activity;
			cr.getmBaseDialog().remove(this.dlgName);
		}
		// buggins method of active dialog tracking - i think mine is better :)
		Log.d("DLG","BaseDialog.onClose()");
		activity.onDialogClosed(this);
	}

	
	/**
	 * Set View's gesture handlers for LTR and RTL horizontal fling
	 * @param view
	 * @param ltrHandler, pass null to call onNegativeButtonClick
	 * @param rtlHandler, pass null to call onPositiveButtonClick
	 */
	public void setFlingHandlers(View view, Runnable ltrHandler, Runnable rtlHandler) {
		if (ltrHandler == null)
			ltrHandler = () -> {
				// cancel
				onNegativeButtonClick();
			};
		if (rtlHandler == null)
			rtlHandler = () -> {
				// ok
				onPositiveButtonClick();
			};
		final GestureDetector detector = new GestureDetector(new MyGestureListener(ltrHandler, rtlHandler));
		view.setOnTouchListener((v, event) -> detector.onTouchEvent(event));
	}

	private class MyGestureListener extends SimpleOnGestureListener {
		Runnable ltrHandler;
		Runnable rtlHandler;
		
		public MyGestureListener(Runnable ltrHandler, Runnable rtlHandler) {
			this.ltrHandler = ltrHandler;
			this.rtlHandler = rtlHandler;
		}

		@Override
		public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
				float velocityY) {
			if (e1 == null || e2 == null)
				return false;
			int thresholdDistance = activity.getPalmTipPixels() * 2;
			int thresholdVelocity = activity.getPalmTipPixels();
			int x1 = (int)e1.getX();
			int x2 = (int)e2.getX();
			int y1 = (int)e1.getY();
			int y2 = (int)e2.getY();
			int dist = x2 - x1;
			int adist = dist > 0 ? dist : -dist;
			int ydist = y2 - y1;
			int aydist = ydist > 0 ? ydist : -ydist;
			int vel = (int)velocityX;
			if (vel<0)
				vel = -vel;
			if (vel > thresholdVelocity && adist > thresholdDistance && adist > aydist * 2) {
				if (dist > 0) {
					Log.d("cr3", "LTR fling detected");
					if (ltrHandler != null) {
						ltrHandler.run();
						return true;
					}
				} else {
					Log.d("cr3", "RTL fling detected");
					if (rtlHandler != null) {
						rtlHandler.run();
						return true;
					}
				}
			}
			return false;
		}
		
	}
	
	@Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
		activity.onUserActivity();
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			onNegativeButtonClick();
			return true;
		}
        if( this.view != null ) {
            if (this.view.onKeyDown(keyCode, event))
            	return true;
        }
        return super.onKeyDown(keyCode, event);
    }
    
	protected View view;
}
