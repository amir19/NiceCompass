/*******************************************************************************
 * BBC News Reader
 * Released under the BSD License. See README or LICENSE.
 * Copyright (c) 2011, Digital Lizard (Oscar Key, Thomas Boby)
 * All rights reserved.
 ******************************************************************************/
package com.digitallizard.nicecompass;

import android.content.Context;
import android.hardware.GeomagneticField;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

public class CompassManager implements SensorEventListener {
	/** constants **/
	private static final int LOCATION_UPDATE_MIN_TIME = 60000; // the min time in millisecs
	private static final int LOCATION_UPDATE_MIN_DISTANCE = 10000; // the min distance in metres
	public static final int STATUS_GOOD = 0;
	public static final int STATUS_INTERFERENCE = 1;
	public static final int STATUS_INACTIVE = 2;
	private static final float MAGNETIC_INTERFERENCE_THRESHOLD_MODIFIER = 1.05f;
	
	/** variables **/
	private Context context;
	private final LocationManager locationManager;
	private final LocationListener locationListener;
	private final SensorManager sensorManager;
	private final Sensor magSensor;
	private final Sensor accelSensor;
	private GeomagneticField geoField;
	private boolean sensorsRegistered; // stores the event listener state
	private boolean sensorHasNewData; // improves performance by only computing the data when required
	private float[] magValues;
	private float[] accelValues;
	private float[] orientationDataCache;
	private Location locationCache;
	private int status;
	
	void interferenceTest(float[] values) {
		// get the expected values
		float threshold = getExpectedFieldStrength() * MAGNETIC_INTERFERENCE_THRESHOLD_MODIFIER;
		float totalStrength = 1f;
		// loop through the values and test that they are not more than X% above the expected values
		for(int i = 0; i < values.length; i++){
			totalStrength *= values[i];
		}
		if(totalStrength > threshold){
			// report possible interference
			status = STATUS_INTERFERENCE;
		} else {
			status = STATUS_GOOD;
		}
	}
	
	float getExpectedFieldStrength(){
		// a geo field is required for accurate data
		if(geoField != null){
			return geoField.getFieldStrength();
		} else {
			// provide a field strength over average
			return 60*60*60f;
		}
	}
	
	void updateGeoField() {
		Location location = getLocation();
		// we can do nothing without location
		if(location != null) {
			// update the geomagnetic field
			geoField = new GeomagneticField(
		             Double.valueOf(location.getLatitude()).floatValue(),
		             Double.valueOf(location.getLongitude()).floatValue(),
		             Double.valueOf(location.getAltitude()).floatValue(),
		             System.currentTimeMillis());
		}
	}
	
	float convertToTrueNorth(float bearing){
		return bearing + getDeclination();
	}
	
	float[] getOrientationData() {
		// if there is no new data, bail here
		if(!sensorHasNewData || magValues == null || accelValues == null){
			return orientationDataCache;
		}
		
		// compute the orientation data
		float[] R = new float[16];
        float[] I = new float[16];
        SensorManager.getRotationMatrix(R, I, accelValues, magValues);
        orientationDataCache = new float[3];
        SensorManager.getOrientation(R, orientationDataCache);
		
		// flag the data as computed
		sensorHasNewData = false;
		
		// return the new data
		return orientationDataCache;
	}
	
	public synchronized boolean isActive() {
		// are the sensors registered
		return sensorsRegistered;
	}
	
	public int getStatus() {
		return status;
	}
	
	public float getDeclination() {
		// if there is no geomagnetic field, just use the normal bearing
		if(geoField != null) {
			return geoField.getDeclination(); // convert magnetic north into true north
		}
		else {
			return 0f; // set the declination to 0
		}
	}
	
	public String getCardinal(boolean trueNorth) {
		return CardinalConverter.cardinalFromBearing(getBearing(trueNorth));
	}
	
	public float getBearing(boolean trueNorth) {
		// update the values
		float[] orientationData = getOrientationData();
		
		// bail if the orientation data was null
		if(orientationData == null) {
			return 0f;
		}
		
		// convert the orientation data into a bearing
		float azimuth = orientationData[0];
		float bearing = azimuth * (360 / (2 * (float)Math.PI)); // convert from radians into degrees
		
		// check if we need to convert this into true
		if(trueNorth) {
			bearing = convertToTrueNorth(bearing);
		}
		
		return bearing;
	}
	
	public float getPositiveBearing(boolean trueNorth) {
		// take the given bearing and convert it into 0 <= x < 360
		float bearing = getBearing(trueNorth);
		if(bearing < 0){
			bearing += 360;
		}
		return bearing;
	}
	
	public Location getLocation() {
		return locationCache;
	}
	
	public void unregisterSensors() {
		if(sensorsRegistered){
			// unregister our sensor listeners
			locationManager.removeUpdates(locationListener);
			sensorManager.unregisterListener(this, magSensor);
			sensorManager.unregisterListener(this, accelSensor);
			sensorsRegistered = false;
			status = STATUS_INACTIVE;
		}
	}
	
	public void registerSensors() {
		if(!sensorsRegistered) {
			// register our sensor listeners
			locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, LOCATION_UPDATE_MIN_TIME, LOCATION_UPDATE_MIN_DISTANCE, locationListener);
			sensorManager.registerListener(this, magSensor, SensorManager.SENSOR_DELAY_UI);
			sensorManager.registerListener(this, accelSensor, SensorManager.SENSOR_DELAY_UI);
			sensorsRegistered = true;
		}
	}
	
	public void onSensorChanged(SensorEvent event) {
		// save the data from the sensor
		switch(event.sensor.getType()){
		case Sensor.TYPE_MAGNETIC_FIELD:
			magValues = event.values.clone();
			// check for interference
			interferenceTest(magValues);
			sensorHasNewData = true;
			break;
		case Sensor.TYPE_ACCELEROMETER:
			accelValues = event.values.clone();
			sensorHasNewData = true;
			break;
		}
	}
	
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
	} 
	
	public CompassManager(Context context) {
		// initialize variables
		this.context = context;
		locationManager = (LocationManager)context.getSystemService(Context.LOCATION_SERVICE);
		sensorManager = (SensorManager)context.getSystemService(Context.SENSOR_SERVICE);
		magSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
		accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		sensorsRegistered = false;
		sensorHasNewData = false;
		status = STATUS_INACTIVE;
		
		// define a listener that listens for location updates
		locationListener = new LocationListener() {
			@Override
			public void onStatusChanged(String arg0, int arg1, Bundle arg2) {
				// TODO Auto-generated method stub
			}
			
			@Override
			public void onProviderEnabled(String arg0) {
				// TODO Auto-generated method stub
			}
			
			@Override
			public void onProviderDisabled(String arg0) {
				// TODO Auto-generated method stub
			}
			
			@Override
			public void onLocationChanged(Location location) {
				// store the new location
				locationCache = location;
				updateGeoField(); // update the geomagnetic field
			}
		};
	}
}
