package com.digitallizard.nicecompass;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.webkit.WebView;

public class HelpActivity extends Activity {
	private static final String HELP_HTML_URI = "file:///android_asset/help.html";
	private WebView webView;
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
    	// check if the home button has been selected
		if(item.getItemId() == android.R.id.home){
			Intent intent = new Intent(this, CompassActivity.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(intent);
			return true; // we have received the press so we can report true
		} else {
			return super.onOptionsItemSelected(item); // pass the press onto the parent
		}
    }
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.help);
		
		// display the up arrow on the action bar
        ActionBar actionBar = this.getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
		
		webView = (WebView)findViewById(R.id.helpWebView);
		webView.loadUrl(HELP_HTML_URI);
	}
}
