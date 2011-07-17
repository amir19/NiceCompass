package com.digitallizard.nicecompass;

import java.text.DecimalFormat;
import java.text.NumberFormat;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.SurfaceView;
import android.view.Window;

public class CompassSurface extends SurfaceView implements Runnable {
	/** constants **/
	private static final int TARGET_FPS = 30;
	private static final int MINIMUM_SLEEP_TIME = 10;
	
	private static final int REQUIRED_BEARING_CHANGE = 5;
	private static final int REQUIRED_BEARING_REPEAT = 40; 
	
	private static final float INNER_COMPASS_CARD_RATIO = 7f / 11f;
	/*private static final int GREATER_DIVISION_INTERVAL = 90;
	private static final int DIVISION_TEXT_INTERVAL = 15;
	private static final int DIVISION_INCREMENT = 5; // must be factor of 360*/
	
	private static final float COMPASS_ACCEL_RATE = 0.9f;
	private static final float COMPASS_SPEED_MODIFIER = 0.26f;
	//private static final float COMPASS_LOCKON_DISTANCE = 1f;
	//private static final float COMPASS_MINIMUM_SPEED = 1f;
	
	/** variables **/
	private CompassManager compass;
	private Thread animationThread;
	private volatile boolean isRunning;
	private boolean useTrueNorth;
	private float currentFps;
	
	// images
	private Bitmap background;
	private Bitmap card;
	
	// paint
	Paint imagePaint;
	Paint blackPaint;
	Paint greyPaint;
	Paint darkGreyPaint;
	Paint redPaint;
	
	private String statusText;
	
	private float bearing;
	private int repeatedBearingCount;
	private volatile String bearingText;
	private DecimalFormat bearingFormat;
	private volatile String declenationText;
	private DecimalFormat declenationFormat;
	
	private float compassCurrentBearing;
	private float compassSpeed;
	
	private long totalFrames;
	private long totalTime;
	
	
	void initDrawing() {
		//background = BitmapFactory.decodeResource(getResources(), R.drawable.background);
		card = BitmapFactory.decodeResource(getResources(), R.drawable.card);
		
		imagePaint = new Paint();
		imagePaint.setDither(true);
		blackPaint = new Paint();
		blackPaint.setColor(Color.BLACK);
		greyPaint = new Paint();  
		greyPaint.setARGB(255, 179, 179, 179);
		darkGreyPaint = new Paint();
		darkGreyPaint.setARGB(255, 112, 112, 112);
		redPaint = new Paint();
		redPaint.setColor(Color.RED);
	}
	
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
		int status = compass.getStatus();
		switch(status){
		case CompassManager.STATUS_INTERFERENCE:
			statusText = "INTERFERENCE DETECTED!";
			break;
		case CompassManager.STATUS_INACTIVE:
			statusText = "COMPASS INACTIVE";
			break;
			default:
				statusText = "";
		}
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
		
		// work out scale factors for percent
		float widthScale = canvas.getWidth() / 100f;
		float heightScale = canvas.getHeight() / 100f;

		//canvas.drawColor(Color.BLACK); // blank the screen
		canvas.drawARGB(255, 24, 24, 24);
		//canvas.drawBitmap(background, null, new Rect(0, 0, canvas.getWidth(), canvas.getHeight()), imagePaint);
		//canvas.drawBitmap(background, 0, 0, imagePaint);
		
		// draw the bearing information
		greyPaint.setTextSize(70f);
		canvas.drawText(bearingText, (50 * widthScale) - getTextCenterOffset(bearingText, greyPaint), 15 * heightScale, greyPaint);
		greyPaint.setTextSize(25f);
		canvas.drawText(declenationText, (50 * widthScale) - getTextCenterOffset(declenationText, greyPaint), 20 * heightScale, greyPaint);
		
		// draw the inside of the compass card
		int cardDiameter = (int)Math.floor(90 * widthScale);
		canvas.drawCircle(50 * widthScale, 60 * heightScale, (cardDiameter * INNER_COMPASS_CARD_RATIO) / 2, greyPaint);
		
		// draw the compass card
		canvas.rotate(compassCurrentBearing * -1, 50 * widthScale, 60 * heightScale);
		int cardX = (int)Math.floor(50 * widthScale - (cardDiameter / 2));
		int cardY = (int)Math.floor(60 * heightScale - (cardDiameter / 2));
		Rect cardRect = new Rect(cardX, cardY, cardX + cardDiameter, cardY + cardDiameter);
		canvas.drawBitmap(card, null, cardRect, imagePaint);
		/*Rect needle = new Rect(230, 200, 250, 600);
		canvas.drawRect(needle, greyPaint);
		Rect point = new Rect(230, 200, 250, 220);
		canvas.drawRect(point, redPaint);*/
		canvas.restore();
		
		// draw the bezel
		darkGreyPaint.setStyle(Paint.Style.STROKE);
		darkGreyPaint.setStrokeWidth(6f); 
		canvas.drawCircle(50 * widthScale, 60 * heightScale, cardDiameter / 2 + 2f, darkGreyPaint);
		canvas.drawLine(50 * widthScale, cardY, 50 * widthScale, cardY + ((1 - INNER_COMPASS_CARD_RATIO) * cardDiameter / 2), darkGreyPaint);
		darkGreyPaint.setStyle(Paint.Style.FILL);
		
		// draw the accuracy meter
		redPaint.setTextSize(25f);
		canvas.drawText(statusText, (canvas.getWidth() / 2) - getTextCenterOffset(statusText, redPaint), canvas.getHeight() - 30, redPaint);
		
		// draw the fps
		greyPaint.setTextSize(15f);
		canvas.drawText(Float.toString(currentFps) + " FPS", 1 * widthScale, 98 * heightScale, greyPaint);
	}
	
	public synchronized void useTrueNorth(boolean useTrueNorth) {
		this.useTrueNorth = useTrueNorth;
	}
	
	public synchronized boolean useTrueNorth() {
		return useTrueNorth;
	}
	
	public void stopAnimation() {
		isRunning = false; // stop the animation loop
		float avgFps = (long)(totalFrames * 1000l) / (long)totalTime;
		Log.v("compass", "total frames:"+totalFrames+" total time:"+totalTime+" avg. fps:"+Float.toString(avgFps));
	}
	
	public void startAnimation() {
		// set the compass position to prevent spinning
		compassCurrentBearing = compass.getPositiveBearing(useTrueNorth());
		
		// set variables for working out avg fps
		totalFrames = 0;
		totalTime = 0;
		
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
			totalFrames ++;
			totalTime += (requiredSleepTime + (finishTime - startTime));
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
		
		// initialize images
		initDrawing();
	}
}
