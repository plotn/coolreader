package org.coolreader.crengine;

import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import org.coolreader.CoolReader;
import org.coolreader.R;

import java.util.ArrayList;

public class SomeButtonsToolbarDlg {
	PopupWindow mWindow;
	View mAnchor;
	CoolReader mCoolReader;
	View mPanel;
	String mTitle;
	TextView mViewTitle;
	int closeSecTimeLeft = 0;
	boolean mCloseOnTouchOutside;
	ArrayList<String> mButtonsOrTexts;
	Object o;

	public interface ButtonPressedCallback {
		public void done(Object o, String btnPressed);
	}

	public final ButtonPressedCallback callback;

	static public void showDialog( CoolReader coolReader, View anchor,
		int closeSecTime, boolean closeOnTouchOutside,
		String sTitle, ArrayList<String> buttonsOrTexts, Object o, ButtonPressedCallback callback)
	{
		SomeButtonsToolbarDlg dlg = new SomeButtonsToolbarDlg(coolReader, anchor, closeSecTime, closeOnTouchOutside,
				sTitle, buttonsOrTexts, o, callback);
		Log.d("cr3", "question popup: " + dlg.mWindow.getWidth() + "x" + dlg.mWindow.getHeight());
	}

	private void closeDialog() {
		mWindow.dismiss();
	}

	public SomeButtonsToolbarDlg(CoolReader coolReader, View anchor,
								 int closeSecTime, boolean closeOnTouchOutside,
			String sTitle, ArrayList<String> buttonsOrTexts, Object o, ButtonPressedCallback callback)
	{
		mCoolReader = coolReader;
		mTitle = StrUtils.getNonEmptyStr(sTitle, true);
		mButtonsOrTexts = buttonsOrTexts;
		mCloseOnTouchOutside = closeOnTouchOutside;
		this.callback = callback;
		mAnchor = anchor;
		this.o = o;

		View panel = (LayoutInflater.from(coolReader.getApplicationContext()).inflate(R.layout.somebuttons_toolbar, null));
		panel.measure(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

		mWindow = new PopupWindow( mAnchor.getContext() );

		mWindow.setTouchInterceptor((v, event) -> {
			if ( event.getAction()==MotionEvent.ACTION_OUTSIDE ) {
				if (mCloseOnTouchOutside) {
					if (callback!=null) callback.done(o, "{{cancel}}");
					closeDialog();
					return true;
				}
			}
			return false;
		});
		//super(panel);
		int colorGrayC;
		int colorGray;
		int colorIcon;
		int colorIconL;
		TypedArray a = mCoolReader.getTheme().obtainStyledAttributes(new int[]
				{R.attr.colorThemeGray2Contrast, R.attr.colorThemeGray2, R.attr.colorIcon, R.attr.colorIconL});
		colorGrayC = a.getColor(0, Color.GRAY);
		colorGray = a.getColor(1, Color.GRAY);
		colorIcon = a.getColor(2, Color.BLACK);
		colorIconL = a.getColor(3, Color.WHITE);
		a.recycle();


		ColorDrawable c = new ColorDrawable(colorGrayC);
		c.setAlpha(130);
		//mWindow.setBackgroundDrawable(c);

		mPanel = panel;
		int colr = Color.argb(170, Color.red(colorGray),Color.green(colorGray),Color.blue(colorGray));
		panel.setBackgroundColor(colr);
		int colr2 = Color.argb(170, Color.red(colorGrayC),Color.green(colorGrayC),Color.blue(colorGrayC));

		LinearLayout llButtonsPanel = mPanel.findViewById(R.id.buttonsPanel);
		Properties props = new Properties(mCoolReader.settings());
		int newTextSize = props.getInt(Settings.PROP_FONT_SIZE, 16)-2;
		int iCntRecent = 0;
		if ((!StrUtils.isEmptyStr(mTitle)) || (closeSecTime > 0)) {
			mViewTitle = new TextView(mCoolReader);
			mViewTitle.setText(mTitle);
			LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(
					ViewGroup.LayoutParams.MATCH_PARENT,
					ViewGroup.LayoutParams.WRAP_CONTENT);
			llp.setMargins(8, 0, 8, 0);
			mViewTitle.setLayoutParams(llp);
			mViewTitle.setMaxLines(3);
			mViewTitle.setEllipsize(TextUtils.TruncateAt.END);
			mViewTitle.setBackgroundColor(colr2);
			mViewTitle.setTextColor(colorIcon);
			mViewTitle.setTypeface(Typeface.DEFAULT_BOLD);
			mViewTitle.setTextSize(TypedValue.COMPLEX_UNIT_PX, newTextSize);
			llButtonsPanel.addView(mViewTitle);
		}
		if (llButtonsPanel!=null) {
			for (final String s: mButtonsOrTexts) {
				if (s.startsWith("*")) {
					String s1 = s.substring(1);
					TextView tv = new TextView(mCoolReader);
					tv.setText(s1);
					LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(
							ViewGroup.LayoutParams.MATCH_PARENT,
							ViewGroup.LayoutParams.WRAP_CONTENT);
					llp.setMargins(8, 0, 8, 0);
					tv.setLayoutParams(llp);
					tv.setMaxLines(8);
					tv.setEllipsize(TextUtils.TruncateAt.END);
					tv.setBackgroundColor(colr2);
					tv.setTextColor(colorIcon);
					tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, newTextSize - 2);
					llButtonsPanel.addView(tv);
				} else {
					Button someButton = new Button(mCoolReader);
					someButton.setText(s);
					someButton.setTextSize(TypedValue.COMPLEX_UNIT_PX, newTextSize);
					someButton.setTextColor(colorIconL);
					someButton.setBackgroundColor(colorGray);
					LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(
							ViewGroup.LayoutParams.MATCH_PARENT,
							ViewGroup.LayoutParams.WRAP_CONTENT);
					llp.setMargins(8, 0, 8, 0);
					someButton.setLayoutParams(llp);
					someButton.setMaxLines(3);
					someButton.setEllipsize(TextUtils.TruncateAt.END);
					llButtonsPanel.addView(someButton);
					someButton.setOnClickListener(v -> {
						if (callback!=null) callback.done(o, ((Button) v).getText().toString());
						closeDialog();
					});
				}
				LinearLayout llBlank = new LinearLayout(mCoolReader);
				llBlank.setBackgroundColor(colr2);
				LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(
						ViewGroup.LayoutParams.MATCH_PARENT,
						ViewGroup.LayoutParams.WRAP_CONTENT);
				llp.setMargins(8, 0, 8, 0);
				llBlank.setLayoutParams(llp);
				llBlank.setMinimumHeight(8);
				llButtonsPanel.addView(llBlank);
			}
		}

		mPanel.setFocusable(true);
		mPanel.setOnKeyListener((v, keyCode, event) -> {
			if ( event.getAction()==KeyEvent.ACTION_UP ) {
				switch ( keyCode ) {
				case KeyEvent.KEYCODE_BACK:
					closeDialog();
					return true;
				}
			} else if ( event.getAction()==KeyEvent.ACTION_DOWN ) {
					switch ( keyCode ) {
					}
				}
			if ( keyCode == KeyEvent.KEYCODE_BACK) {
				return true;
			}
			return false;
		});

		mWindow.setOnDismissListener(() -> {
		});
		
		mWindow.setBackgroundDrawable(new BitmapDrawable());
		mWindow.setWidth(WindowManager.LayoutParams.FILL_PARENT);
		mWindow.setHeight(WindowManager.LayoutParams.WRAP_CONTENT);

		mWindow.setFocusable(false);
		mWindow.setTouchable(true);
		mWindow.setOutsideTouchable(true);
		mWindow.setContentView(panel);
		
		
		int [] location = new int[2];
		mAnchor.getLocationOnScreen(location);

		int popupY = location[1] + mAnchor.getHeight() - mPanel.getHeight();
		mWindow.showAtLocation(mAnchor, Gravity.TOP | Gravity.CENTER_HORIZONTAL, location[0], popupY);

		Handler handler = new Handler();

		if (closeSecTime != 0) {
			closeSecTimeLeft = closeSecTime;

			handler.postDelayed(() -> {
				if (mWindow != null) {
					if (callback != null) callback.done(o, "{{timeout}}");
					mWindow.dismiss();
				}
			}, closeSecTime * 1000);
			tick1s();
		}
		mCoolReader.tintViewIcons(mPanel);
	}

	private void tick1s() {
		Handler handler = new Handler();
		handler.postDelayed(() -> {
			closeSecTimeLeft = closeSecTimeLeft - 1;
			if (mViewTitle != null) mViewTitle.setText((mTitle + " (" + closeSecTimeLeft + ")").trim());
			if (closeSecTimeLeft > 1) tick1s();
		}, 1000);
	}
	
}
