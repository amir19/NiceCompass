/*******************************************************************************
 * BBC News Reader
 * Released under the BSD License. See README or LICENSE.
 * Copyright (c) 2011, Digital Lizard (Oscar Key, Thomas Boby)
 * All rights reserved.
 ******************************************************************************/
package com.digitallizard.nicecompass;

import android.app.Activity;
import android.os.Bundle;

public class CompassActivity extends Activity {
	private CompassManager compass;
	private CompassSurface surface;

	public void onPause() {
		// unregister from the compass to prevent undue battery drain
		compass.unregisterSensors();
		// call the superclass
		super.onPause();
	}
	
	public void onResume() {
		// class the superclass
		super.onResume();
		// register to receive events from the compass
		compass.registerSensors();
	}
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // initialize variables
        compass = new CompassManager(this);
        surface = new CompassSurface(this);
        
        // create the gui
        setContentView(R.layout.main);
    }
}
