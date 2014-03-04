package com.cloverstudio.spika;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

import com.cloverstudio.spika.extendables.SideBarActivity;
import com.cloverstudio.spika.management.UsersManagement;
import com.cloverstudio.spika.utils.ConstServer;

public class InformationActivity extends SideBarActivity {

	WebView webView;
	ProgressBar progressBar;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_information);
		setSideBar(getString(R.string.INFORMATION));
		
		webView = (WebView) findViewById(R.id.web_view);
		progressBar = (ProgressBar) findViewById(R.id.progress_information);
		
		webView.getSettings().setJavaScriptEnabled(true);
		webView.setWebChromeClient(new WebChromeClient());
		webView.setWebViewClient(new MyWebViewClient());
		webView.loadUrl(ConstServer.INFORMATION_URL + UsersManagement.getLoginUser().getToken());
	}
	
	private class MyWebViewClient extends WebViewClient {
		
		@Override
		public void onPageFinished(WebView view, String url) {
			Log.e("URL LOADED", url);
			progressBar.setVisibility(View.GONE);
			super.onPageFinished(view, url);
	    }
	}
}
