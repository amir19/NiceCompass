package com.digitallizard.nicecompass;

import java.text.DecimalFormat;
import java.text.NumberFormat;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.Log;
import android.view.SurfaceView;

public class CompassSurface extends SurfaceView implements Runnable {
	/** constants **/
	private static final int TARGET_FPS = 30;
	private static final int MINIMUM_SLEEP_TIME = 10;
	
	private static final int REQUIRED_BEARING_CHANGE = 8;
	private static final int REQUIRED_BEARING_REPEAT = 50;
	
	private static final float COMPASS_ACCEL_RATE = 0.9f;
	private static final float COMPASS_SPEED_MODIFIER = 0.3f;
	private static final float COMPASS_LOCKON_DISTANCE = 1f;
	private static final float COMPASS_MINIMUM_SPEED = 1f;
	
	/** variables **/
	private CompassManager compass;
	private Thread animationThread;
	private volatile boolean isRunning;
	private boolean useTrueNorth;
	private float currentFps;
	
	private String accuracyText;
	
	private float bearing;
	private int repeatedBearingCount;
	private volatile String bearingText;
	private DecimalFormat bearingFormat;
	private volatile String declenationText;
	private DecimalFormat declenationFormat;
	
	private float compassCurrentBearing;
	private float compassSpeed;
	private float compassAccel;
	
	
	float getTextCenterOffset(String text, Paint paint) {
		float[] widths = new float[text.length()];
		paint.getTextWidths(text, widths);
		float totalWidth = 0;
		for(int i = 0; i < text.length(); i++){
			totalWidth += widths[i];
		}
		return totalWidth / 2;
	}
	
	void updateAccuracy() {
		accuracyText = CompassManager.COMPASS_ACCURACY_MESSAGES[compass.getAccuracy()];
	}
	
	void updateCompass() {
		float newBearing = compass.getPositiveBearing(useTrueNorth());
		// adjust the new bearing to prevent problems involving 360 -- 0
		if(compassCurrentBearing < 90 && newBearing > 270){
			newBearing -= 360;
		}
		if(compassCurrentBearing > 270 && newBearing < 90){
			newBearing +=360; 
		}
		//accuracyText = "target: "+newBearing+" position:"+compassCurrentBearing;
		
		float distance = newBearing - compassCurrentBearing;
		float targetSpeed =  distance * COMPASS_SPEED_MODIFIER;
		// accelerate the compass accordingly
		if(targetSpeed > compassSpeed){
			compassSpeed += COMPASS_ACCEL_RATE;
		}
		if(targetSpeed < compassSpeed){
			compassSpeed -= COMPASS_ACCEL_RATE;
		}
		// stop the compass speed dropping too low
		/*if(Math.abs(compassSpeed) < COMPASS_MINIMUM_SPEED && compassSpeed < 0 && Math.abs(distance) > COMPASS_LOCKON_DISTANCE){
			compassSpeed = -COMPASS_MINIMUM_SPEED;
		}
		if(Math.abs(compassSpeed) < COMPASS_MINIMUM_SPEED && compassSpeed > 0 && Math.abs(distance) > COMPASS_LOCKON_DISTANCE){
			compassSpeed = COMPASS_MINIMUM_SPEED;
		}*/
		compassCurrentBearing += compassSpeed; 
		
		// adjust the bearing for a complete circle
		if(compassCurrentBearing >= 360) {
			compassCurrentBearing -= 360;
		}
		if(compassCurrentBearing < 0) {
			compassCurrentBearing += 360;
		}
	}
	
	void updateBearing() {
		// work out the bearing, dampening jitter
		float newBearing = compass.getPositiveBearing(useTrueNorth());
		if(Math.abs(bearing - newBearing) > REQUIRED_BEARING_CHANGE) {
			bearing = newBearing; // the change is to insignificant to be displayed
			repeatedBearingCount = 0; // reset the repetition count
		} else {
			repeatedBearingCount ++;
			if(repeatedBearingCount > REQUIRED_BEARING_REPEAT) {
				bearing = newBearing;
				repeatedBearingCount = 0;
			}
		}
		bearingText = bearingFormat.format(bearing);
		bearingText += "\u00B0 "; // add the degrees symbol
		bearingText += CardinalConverter.cardinalFromPositiveBearing(bearing); // add the cardinal information
		
		declenationText = "variation: "+declenationFormat.format(compass.getDeclination())+"\u00B0";
	}
	
	void update(float delta) {
		updateBearing();
		updateCompass();
		updateAccuracy();
	}
	
	synchronized void triggerDraw() {
		Canvas canvas = null;
		try {
			canvas = this.getHolder().lockCanvas();
			if(canvas != null) {
				this.onDraw(canvas);
			}
		} finally {
			if (canvas != null) {
				this.getHolder().unlockCanvasAndPost(canvas);
			}
		}
	}
	
	@Override
	public void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		
		canvas.drawColor(Color.BLACK); // blank the screen
		
		// initialize paint
		Paint greyPaint = new Paint();
		greyPaint.setColor(Color.GRAY);
		Paint redPaint = new Paint();
		redPaint.setColor(Color.RED);
		
		// draw the bearing information
		greyPaint.setTextSize(70f);
		canvas.drawText(bearingText, (canvas.getWidth() / 2) - getTextCenterOffset(bearingText, greyPaint), canvas.getHeight() / 6, greyPaint);
		greyPaint.setTextSize(25f);
		canvas.drawText(declenationText, (canvas.getWidth() / 2) - getTextCenterOffset(declenationText, greyPaint), (canvas.getHeight() / 6) + 45, greyPaint);
		
		// draw the compass
		canvas.rotate(compassCurrentBearing * -1, canvas.getWidth() / 2, 400);
		Rect needle = new Rect(230, 200, 250, 600);
		canvas.drawRect(needle, greyPaint);
		Rect point = new Rect(230, 200, 250, 220);
		canvas.drawRect(point, redPaint);
		canvas.restore();
		
		// draw the accuracy meter
		canvas.drawText(accuracyText, (canvas.getWidth() / 2) - getTextCenterOffset(accuracyText, greyPaint), canvas.getHeight() - (canvas.getHeight() / 7), greyPaint);
		
		// draw the fps
		greyPaint.setTextSize(15f);
		canvas.drawText(Float.toString(currentFps) + " FPS", 5, canvas.getHeight() - 10, greyPaint);
	}
	
	public synchronized void useTrueNorth(boolean useTrueNorth) {
		this.useTrueNorth = useTrueNorth;
	}
	
	public synchronized boolean useTrueNorth() {
		return useTrueNorth;
	}
	
	public void stopAnimation() {
		isRunning = false; // stop the animation loop
	}
	
	public void startAnimation() {
		// set the compass position to prevent spinning
		compassCurrentBearing = compass.getPositiveBearing(useTrueNorth());
		
		isRunning = true; // flag the loop as running
		// create and start the thread
		animationThread = new Thread(this);
		animationThread.start();
	}
	
	public void run() {
		// initialize a timing variable
		long maxSleepTime = (long) Math.floor(1000 / TARGET_FPS);
		// loop whilst we are told to
		while (isRunning) {
			// record the start time
			long startTime = System.currentTimeMillis();
			
			// update the animation
			update(1); // TODO set up a delta system
			triggerDraw(); // draw the update
			
			// work out how long to sleep for
			long finishTime = System.currentTimeMillis();
			long requiredSleepTime = maxSleepTime - (finishTime - startTime);
			// check if the sleep time was too low
			if(requiredSleepTime < MINIMUM_SLEEP_TIME) {  
				requiredSleepTime = MINIMUM_SLEEP_TIME;
			}
			currentFps = 1000 / (requiredSleepTime + (finishTime - startTime));
			// try to sleep for this time
			try {
				Thread.sleep(requiredSleepTime);
			} catch (InterruptedException e) {
				// do nothing
			}
		}
	}
	
	public CompassSurface(Context context, CompassManager compass, boolean useTrueNorth) {
		super(context);
		this.compass = compass;
		useTrueNorth(useTrueNorth);
		
		// initialize the number formatters
		bearingFormat = new DecimalFormat("000");
		declenationFormat = new DecimalFormat("00.0");
	}
}
