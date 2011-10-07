package com.digitallizard.nicecompass;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebView;

public class HelpActivity extends Activity {
	private static final String HELP_HTML_URI = "file:///android_asset/help.html";
	private WebView webView;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.help);
		
		webView = (WebView)findViewById(R.id.helpWebView);
		webView.loadUrl(HELP_HTML_URI);
	}
}
