/*******************************************************************************
 * BBC News Reader
 * Released under the BSD License. See README or LICENSE.
 * Copyright (c) 2011, Digital Lizard (Oscar Key, Thomas Boby)
 * All rights reserved.
 ******************************************************************************/
package com.digitallizard.nicecompass;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.widget.LinearLayout;

public class CompassActivity extends Activity {
	private CompassManager compass;
	private CompassSurface surface;
	private LinearLayout surfaceContainer;

	public void onPause() {
		// unregister from the compass to prevent undue battery drain
		compass.unregisterSensors();
		// stop the animation
		surface.stopAnimation();
		// call the superclass
		super.onPause();
	}
	
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
        
        // initialize variables
        compass = new CompassManager(this);
        surface = new CompassSurface(this, compass);
        surfaceContainer = (LinearLayout)findViewById(R.id.compassSurfaceContainer);
        
        // add the compass
        surfaceContainer.addView(surface);
    }
}
