/*******************************************************************************
 * NiceCompass
 * Released under the BSD License. See README or LICENSE.
 * Copyright (c) 2011, Digital Lizard (Oscar Key, Thomas Boby)
 * All rights reserved.
 ******************************************************************************/
package com.digitallizard.nicecompass;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.LinearLayout;

public class CompassActivity extends Activity {
	public static final String PREF_FILE_NAME = "com.digitallizard.nicecompass_preferences";
	public static final String PREFKEY_USE_TRUE_NORTH = "useTrueNorth";
	public static final boolean DEFAULT_USE_TRUE_NORTH = true;
	
	private CompassManager compass;
	private CompassSurface surface;
	private LinearLayout surfaceContainer;
	
    @Override
	public boolean onOptionsItemSelected(MenuItem item) {
    	if(item.getTitle().equals(getResources().getString(R.string.menu_title_help))){
    		Intent intent = new Intent(this, HelpActivity.class);
    		startActivity(intent);
    		return true; //we have received the press so we can report true
    	} else {
    		return super.onOptionsItemSelected(item);
    	}
    }
    
    @Override
	public boolean onCreateOptionsMenu(Menu menu) {
    	super.onCreateOptionsMenu(menu);
    	// inflate the menu XML file
    	MenuInflater menuInflater = new MenuInflater(this);
    	menuInflater.inflate(R.menu.menu, menu);
    	return true; // we have made the menu so we can return true
    }

	@Override
	public void onPause() {
		// save the current north state
		SharedPreferences settings = this.getSharedPreferences(PREF_FILE_NAME, MODE_PRIVATE);
		Editor editor = settings.edit();
		editor.putBoolean(PREFKEY_USE_TRUE_NORTH, surface.useTrueNorth());
		editor.commit();
		
		// unregister from the compass to prevent undue battery drain
		compass.unregisterSensors();
		// stop the animation
		surface.stopAnimation();
		// call the superclass
		super.onPause();
	}
	
	@Override
	public void onResume() {
		// class the superclass
		super.onResume();
		// register to receive events from the compass
		compass.registerSensors();
		// start the animation
		surface.startAnimation();
	}
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // create the gui
        setContentView(R.layout.main);
        
        // load in the settings
        SharedPreferences settings = this.getSharedPreferences(PREF_FILE_NAME, MODE_PRIVATE);
        boolean useTrueNorth = settings.getBoolean(PREFKEY_USE_TRUE_NORTH, DEFAULT_USE_TRUE_NORTH);
        
        // initialize variables
        compass = new CompassManager(this);
        surface = new CompassSurface(this, compass, useTrueNorth);
        surfaceContainer = (LinearLayout)findViewById(R.id.compassSurfaceContainer);
        
        // prevent gradient banding
        surface.getHolder().setFormat(android.graphics.PixelFormat.TRANSPARENT);
        
        // add the compass
        surfaceContainer.addView(surface);
    }
}
