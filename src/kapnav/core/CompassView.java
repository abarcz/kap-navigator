package kapnav.core;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;
import android.view.WindowManager;

public class CompassView extends View {

	Paint mPaint = new Paint();
	Path mCompassNeedle = new Path();
	
	float[] mSensorValues = null;
	
	protected void updateSensorValues(float[] values) {
		mSensorValues = values;
		this.postInvalidate();
	}

	public CompassView(Context context) {
		super(context);
		this.setBackgroundColor(0);

		mCompassNeedle.moveTo(0, -25);
		mCompassNeedle.lineTo(-8, 30);
		mCompassNeedle.lineTo(0, 25);
		mCompassNeedle.lineTo(8, 30);
		mCompassNeedle.close();

		mPaint.setAntiAlias(true);
		mPaint.setColor(Color.RED);
	}

	@Override
	protected void onDraw(Canvas canvas) {
		int width = canvas.getWidth();
		int height = canvas.getHeight();
		int translationX = width * 5 / 6;
		int translationY = height * 1 / 10;

		canvas.translate(translationX, translationY);
		if (mSensorValues != null) {
			canvas.rotate(calculateRotation());
		}
		canvas.drawPath(mCompassNeedle, mPaint);
	}
	
	protected float calculateRotation() {
		//float roll = mSensorValues[2];
		float pitch = mSensorValues[1];
		float yaw = mSensorValues[0];
		
		// check if phone is bottom up
		float correction = 0;
		if (Math.abs(pitch) > 135) {
			// bottom-up
			correction += 180;
		} else {
			yaw = -yaw;
		}
		
		// check if phone screen is rotated
		WindowManager windowManager =  (WindowManager) this.getContext().getSystemService(Context.WINDOW_SERVICE);
		int screenRotation = windowManager.getDefaultDisplay().getRotation();
		if (screenRotation == 3) {
			correction += 90;
		}
		if (screenRotation == 1) {
			correction -= 90;
		}
		
		return yaw + correction;
	}
}
