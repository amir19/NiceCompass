/*******************************************************************************
 * BBC News Reader
 * Released under the BSD License. See README or LICENSE.
 * Copyright (c) 2011, Digital Lizard (Oscar Key, Thomas Boby)
 * All rights reserved.
 ******************************************************************************/
package com.digitallizard.nicecompass;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;

public class CompassManager implements SensorEventListener {
	/** variables **/
	private boolean compassRegistered; // stores the event listener state
	
	public void onSensorChanged(SensorEvent event) {
		
	}
	
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		
	}
	
	public void unregisterCompass() {
		// unregister our sensor listener
	}
	
	public void registerCompass() {
		// register our sensor listener
	}
	
	public CompassManager() {
		// initialize variables
		compassRegistered = false;
	}
}
