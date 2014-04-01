package com.cloverstudio.spika;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.cloverstudio.spika.dialog.HookUpProgressDialog;
import com.cloverstudio.spika.extendables.SideBarActivity;
import com.cloverstudio.spika.management.UsersManagement;
import com.cloverstudio.spika.utils.Const;
import com.cloverstudio.spika.utils.ConstServer;

public class InformationActivity extends SideBarActivity {

	private WebView mWebView;
	private HookUpProgressDialog mProgressDialog;
	private String mUrl=SpikaApp.getInstance().getBaseUrl()+Const.INFORMATION_FOLDER;
	
	@SuppressLint("SetJavaScriptEnabled")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_information);
		setSideBar(getString(R.string.INFORMATION));
		
		mWebView = (WebView) findViewById(R.id.web_view);
		mProgressDialog = new HookUpProgressDialog(this);
		mProgressDialog.show();
		

		mWebView.getSettings().setJavaScriptEnabled(true);
		mWebView.setWebChromeClient(new WebChromeClient());
		mWebView.setWebViewClient(new MyWebViewClient());
		
		mUrl=mUrl + UsersManagement.getLoginUser().getToken();
		
		mWebView.loadUrl(mUrl);
	}
	
	private class MyWebViewClient extends WebViewClient {
		
		@Override
		public void onPageFinished(WebView view, String url) {
			mProgressDialog.dismiss();
			super.onPageFinished(view, url);
	    }
		
		@Override
		public boolean shouldOverrideUrlLoading(WebView view, String url) {
			mProgressDialog.show();
			mWebView.loadUrl(url);
			return true;
//			return super.shouldOverrideUrlLoading(view, url);
		}
	}
	
	@Override
	public void onBackPressed() {
		if(mWebView.getUrl().equals(mUrl)){
			super.onBackPressed();
		}else{
			mProgressDialog.show();
			mWebView.loadUrl(mUrl);
		}
	}
}
