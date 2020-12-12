package org.coolreader.crengine;

import org.coolreader.CoolReader;
import org.coolreader.R;
import org.coolreader.db.CRDBService;

import android.content.Context;
import android.content.res.TypedArray;
import android.database.DataSetObserver;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

public class SearchDlg extends BaseDialog {
	BaseActivity mCoolReader;
	ReaderView mReaderView;
	private LayoutInflater mInflater;
	View mDialogView;
	EditText mEditView;
	BookInfo mBookInfo;
	Button mSkim;
	Button mDoSearch;
	Button mSearchPages;
	Button mBtnCaseSentitive;
	Button mBtnSearchReverse;
	ArrayList<String> mSearches;
	private SearchList mList;
	boolean bCaseSensitive = false;
	boolean bReverse = false;
	ImageButton btnFake;

	@Override
	protected void onPositiveButtonClick()
	{
		KeyboardUtils.hideKeyboard(mCoolReader,mEditView);
    	String pattern = mEditView.getText().toString();
    	if ( pattern==null || pattern.length()==0 )
    		mCoolReader.showToast(R.string.no_pattern);
    	else if ( mBookInfo == null )
    		Log.e("search", "No opened book!");
    	else {
		    activity.getDB().saveSearchHistory(mBookInfo,
				    mEditView.getText().toString());
		    mReaderView.findText(mEditView.getText().toString(), bReverse, !bCaseSensitive);
	    }
        cancel();
	}
	
	@Override
	protected void onNegativeButtonClick()
	{
		// override it
        cancel();
	}

	public final static int ITEM_POSITION=0;

	class SearchListAdapter extends BaseAdapter {
		public boolean areAllItemsEnabled() {
			return true;
		}

		public boolean isEnabled(int arg0) {
			return true;
		}

		public int getCount() {
			return mSearches.size();
		}

		public Object getItem(int position) {
			if ( position<0 || position>=mSearches.size() )
				return null;
			return mSearches.get(position);
		}

		public long getItemId(int position) {
			return position;
		}

		public int getItemViewType(int position) {
			return ITEM_POSITION;
		}

		public int getViewTypeCount() {
			return 4;
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			View view;
			int res = R.layout.dict_item;
			view = mInflater.inflate(res, null);
			TextView labelView = view.findViewById(R.id.dict_item_shortcut);
			TextView titleTextView = view.findViewById(R.id.dict_item_title);
			String s = (String)getItem(position);
			if ( labelView!=null ) {
				labelView.setText(String.valueOf(position+1));
			}
			if ( s!=null ) {
				if ( titleTextView!=null )
					titleTextView.setText(s);
			} else {
				if ( titleTextView!=null )
					titleTextView.setText("");
			}
			return view;
		}

		public boolean hasStableIds() {
			return true;
		}

		public boolean isEmpty() {
			return mSearches.size()==0;
		}

		private ArrayList<DataSetObserver> observers = new ArrayList<DataSetObserver>();

		public void registerDataSetObserver(DataSetObserver observer) {
			observers.add(observer);
		}

		public void unregisterDataSetObserver(DataSetObserver observer) {
			observers.remove(observer);
		}
	}

	class SearchList extends BaseListView {

		public SearchList(Context context, boolean shortcutMode ) {
			super(context, true);
			setChoiceMode(ListView.CHOICE_MODE_SINGLE);
			setLongClickable(true);
			setAdapter(new SearchDlg.SearchListAdapter());
			setOnItemLongClickListener((arg0, arg1, position, arg3) -> {
				//openContextMenu(SearchDlg.SearchList.this);
				mEditView.setText(mSearches.get(position));
				searchPagesClick();
				return true;
			});
		}

		@Override
		public boolean performItemClick(View view, int position, long id) {
			mEditView.setText(mSearches.get(position));
			return true;
		}
	}

	public void searchPagesClick() {
		final String sText = mEditView.getText().toString().trim();
		mReaderView.CheckAllPagesLoadVisual();
		final GotoPageDialog dlg = new GotoPageDialog(activity, title, sText, bCaseSensitive,
				new GotoPageDialog.GotoPageHandler() {
					int pageNumber = 0;

					@Override
					public boolean validate(String s) {
						pageNumber = Integer.valueOf(s);
						return pageNumber > 0; // && pageNumber <= mReaderView.props.pageCount;
					}

					@Override
					public void onOk(String s) {
						mReaderView.goToPage(pageNumber);
					}

					@Override
					public void onOkPage(String s) {
						mReaderView.goToPage(pageNumber);
					}

					@Override
					public void onCancel() {
					}
				});
		dlg.show();
		dismiss();
	}

	private void buttonPressed(Button btn) {
		int colorGrayC;
		TypedArray a = mCoolReader.getTheme().obtainStyledAttributes(new int[]
				{R.attr.colorThemeGray2Contrast});
		colorGrayC = a.getColor(0, Color.GRAY);
		a.recycle();
		int colorGrayCT=Color.argb(30,Color.red(colorGrayC),Color.green(colorGrayC),Color.blue(colorGrayC));
		int colorGrayCT2=Color.argb(200,Color.red(colorGrayC),Color.green(colorGrayC),Color.blue(colorGrayC));

		if (btn == mBtnCaseSentitive) bCaseSensitive = !bCaseSensitive;
		if (btn == mBtnSearchReverse) bReverse = !bReverse;

		mCoolReader.tintViewIcons(mBtnCaseSentitive, PorterDuff.Mode.CLEAR,true);
		mCoolReader.tintViewIcons(mBtnSearchReverse, PorterDuff.Mode.CLEAR,true);

		mBtnCaseSentitive.setBackgroundColor(colorGrayCT);
		mBtnSearchReverse.setBackgroundColor(colorGrayCT);

		if (bCaseSensitive) {
			mBtnCaseSentitive.setBackgroundColor(colorGrayCT2);
			mCoolReader.tintViewIcons(mBtnCaseSentitive,true);
		}
		if (bReverse) {
			mBtnSearchReverse.setBackgroundColor(colorGrayCT2);
			mCoolReader.tintViewIcons(mBtnSearchReverse,true);
		}
	}
	
	public SearchDlg(BaseActivity coolReader, ReaderView readerView, String initialText)
	{
		super("SearchDlg", coolReader, coolReader.getResources().getString(R.string.win_title_search), true, false);
        setCancelable(true);
		this.mCoolReader = coolReader;
		this.mReaderView = readerView;
		TypedArray a = activity.getTheme().obtainStyledAttributes(new int[]
				{R.attr.colorThemeGray2, R.attr.colorThemeGray2Contrast, R.attr.colorIcon});
		int colorGrayC = a.getColor(1, Color.GRAY);
		a.recycle();
		int colorGrayCT=Color.argb(30,Color.red(colorGrayC),Color.green(colorGrayC),Color.blue(colorGrayC));
		int colorGrayCT2=Color.argb(200,Color.red(colorGrayC),Color.green(colorGrayC),Color.blue(colorGrayC));

		this.mBookInfo = mReaderView.getBookInfo();
		setPositiveButtonImage(
				Utils.resolveResourceIdByAttr(activity, R.attr.cr3_viewer_find_drawable, R.drawable.icons8_search),
				//R.drawable.icons8_search,
				R.string.action_search);
        mInflater = LayoutInflater.from(getContext());
        mDialogView = mInflater.inflate(R.layout.search_dialog, null);
    	mEditView = mDialogView.findViewById(R.id.search_text);
    	if (initialText != null)
    		mEditView.setText(initialText);
		mSkim = mDialogView.findViewById(R.id.btn_skim);
		mSkim.setOnClickListener(v -> {
			KeyboardUtils.hideKeyboard(mCoolReader,mEditView);
			mReaderView.onCommand(ReaderCommand.DCMD_SKIM, 0);
			cancel();
		});
		mSkim.setBackgroundColor(colorGrayCT2);
		mSearchPages = mDialogView.findViewById(R.id.btn_search_pages);
    	mSearchPages.setOnClickListener(v -> searchPagesClick());
		mSearchPages.setBackgroundColor(colorGrayCT2);
		Utils.setDashedButton(mSearchPages);
		mDoSearch = mDialogView.findViewById(R.id.btn_do_search);
		mDoSearch.setOnClickListener(v -> onPositiveButtonClick());
		mDoSearch.setBackgroundColor(colorGrayCT2);
		Utils.setDashedButton(mDoSearch);
		Button btnMinus1 = mDialogView.findViewById(R.id.search_dlg_clear_hist_btn);
		btnMinus1.setBackgroundColor(colorGrayCT2);

		btnMinus1.setOnClickListener(v -> {
			activity.getDB().clearSearchHistory(mBookInfo);
			mCoolReader.showToast(mCoolReader.getString(R.string.search_hist_will_be_cleared));
			dismiss();
		});

		mBtnCaseSentitive = mDialogView.findViewById(R.id.btn_case_sentitive);
		mBtnCaseSentitive.setBackgroundColor(colorGrayCT2);
		mBtnCaseSentitive.setOnClickListener(v -> {
			buttonPressed(mBtnCaseSentitive);
		});
		mBtnSearchReverse = mDialogView.findViewById(R.id.btn_search_reverse);
		mBtnSearchReverse.setBackgroundColor(colorGrayCT2);
		mBtnSearchReverse.setOnClickListener(v -> {
			buttonPressed(mBtnSearchReverse);
		});
		btnFake = mDialogView.findViewById(R.id.btn_fake);
		Drawable img = getContext().getResources().getDrawable(R.drawable.icons8_toc_item_normal);
		Drawable img1 = img.getConstantState().newDrawable().mutate();
		Drawable img2 = img.getConstantState().newDrawable().mutate();
		BackgroundThread.instance().postGUI(() -> buttonPressed(null), 200);
		mBtnCaseSentitive.setCompoundDrawablesWithIntrinsicBounds(img1, null, null, null);
		mBtnSearchReverse.setCompoundDrawablesWithIntrinsicBounds(img2, null, null, null);

		activity.getDB().loadSearchHistory(this.mBookInfo, searches -> {
			mSearches = searches;
			ViewGroup body = mDialogView.findViewById(R.id.history_list);
			mList = new SearchList(activity, false);
			body.addView(mList);
		});
		mCoolReader.tintViewIcons(mDialogView);
		//setView(mDialogView);
		//setFlingHandlers(mList, null, null);
		// setup buttons
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setView(mDialogView);
	}
}
