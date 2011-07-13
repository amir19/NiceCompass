package com.digitallizard.nicecompass;

import java.text.DecimalFormat;
import java.text.NumberFormat;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;
import android.view.SurfaceView;

public class CompassSurface extends SurfaceView implements Runnable {
	/** constants **/
	private static final int TARGET_FPS = 24;
	private static final int MINIMUM_SLEEP_TIME = 10;
	
	private static final int REQUIRED_BEARING_CHANGE = 8;
	private static final int REQUIRED_BEARING_REPEAT = 40;
	
	/** variables **/
	private Context context;
	private CompassManager compass;
	private Thread animationThread;
	private volatile boolean isRunning;
	private DecimalFormat bearingFormat;
	private boolean useTrueNorth;
	
	private float bearing;
	private int repeatedBearingCount;
	private volatile String bearingText;
	
	void update(float delta) {
		// update the values
		// TODO perform updates
		// work out the bearing, dampening jitter
		float newBearing = compass.getPositiveBearing(useTrueNorth);
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
		
		// draw what has been created
		triggerDraw();
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
		Paint textPaint = new Paint();
		textPaint.setColor(Color.GRAY);
		textPaint.setTextSize(70f);
		
		// draw the bearing information
		canvas.drawText(bearingText, canvas.getWidth() / 2, canvas.getHeight() / 5, textPaint);
		
		//canv
	}
	
	public void stopAnimation() {
		isRunning = false; // stop the animation loop
	}
	
	public void startAnimation() {
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
			
			// work out how long to sleep for
			long finishTime = System.currentTimeMillis();
			long requiredSleepTime = maxSleepTime - (finishTime - startTime);
			// check if the sleep time was too low
			if(requiredSleepTime < MINIMUM_SLEEP_TIME) {
				requiredSleepTime = MINIMUM_SLEEP_TIME;
			}
			// try to sleep for this time
			try {
				Thread.sleep(requiredSleepTime);
			} catch (InterruptedException e) {
				// do nothing
			}
		}
	}
	
	public CompassSurface(Context context, CompassManager compass) {
		super(context);
		this.context = context;
		this.compass = compass;
		useTrueNorth = false;
		
		// initialize the number formatter
		bearingFormat = new DecimalFormat("000");
	}
}
