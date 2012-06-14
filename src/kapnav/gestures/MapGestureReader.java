
package kapnav.gestures;

import android.graphics.PointF;
import android.os.Handler;
import android.util.FloatMath;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;

public class MapGestureReader extends MapGestureSource implements OnTouchListener {

	public enum TouchState {IDLE, DRAG, ZOOM}
	
	TouchState mState = TouchState.IDLE;
	PointF mStartingPoint = new PointF();
	PointF mScalingCenterPoint = new PointF();
	float mStartingPointersDist = 1;
	
	final float MIN_POINTERS_DIST = 10;
	final float MIN_DIM_CHANGE = 10;
	final int HOLD_TIMEOUT = 500;		// ms

	Handler mTimeoutHandler = new Handler();
	boolean mActionOccuredDuringTimeout = false;

	public boolean onTouch(View v, MotionEvent event) {
		
		switch (event.getAction() & MotionEvent.ACTION_MASK) {
		
			case MotionEvent.ACTION_DOWN:
				mStartingPoint.set(event.getX(), event.getY());
				mState = TouchState.DRAG;
				setHoldTimeout();
				break;
				
			case MotionEvent.ACTION_POINTER_DOWN:
				mStartingPointersDist = pointersDistance(event);
				if (mStartingPointersDist > MIN_POINTERS_DIST) {
					mScalingCenterPoint = middlePoint(event);
					mState = TouchState.ZOOM;
				}
				mActionOccuredDuringTimeout = true;
				break;
				
			case MotionEvent.ACTION_UP:
				mState = TouchState.IDLE;
				mActionOccuredDuringTimeout = true;
				fireEvent(new MapUpEvent(this));
				break;
				
			case MotionEvent.ACTION_POINTER_UP:
				mState = TouchState.IDLE;
				mActionOccuredDuringTimeout = true;
				break;
				
			case MotionEvent.ACTION_MOVE:
				if (maxDimensionChange(event) < MIN_DIM_CHANGE) {
					break;
				}
				if (mState == TouchState.DRAG) {
					float translationX = event.getX() - mStartingPoint.x;
					float translationY = event.getY() - mStartingPoint.y;
					fireEvent(new MapDragEvent(this, translationX, translationY));
					mStartingPoint.set(event.getX(), event.getY());
				} else if (mState == TouchState.ZOOM) {
					float finalPointersDist = pointersDistance(event);
					if (finalPointersDist > MIN_POINTERS_DIST) {
						float scale = finalPointersDist / mStartingPointersDist;
						fireEvent(new MapZoomEvent(this, mScalingCenterPoint, scale));
						mScalingCenterPoint = middlePoint(event);
						mStartingPointersDist = finalPointersDist;
					}
				}
				mActionOccuredDuringTimeout = true;
				break;
		}
		return true;
	}

	protected float pointersDistance(MotionEvent event) {
		float x = event.getX(0) - event.getX(1);
		float y = event.getY(0) - event.getY(1);
		return FloatMath.sqrt(x * x + y * y);
	}
	
	protected float maxDimensionChange(MotionEvent event) {
		float distX = Math.abs(event.getX() - mStartingPoint.x);
		float distY = Math.abs(event.getY() - mStartingPoint.y);
		return Math.max(distX, distY);
	}

	protected PointF middlePoint(MotionEvent event) {
		PointF middlePoint = new PointF();
		float x = event.getX(0) + event.getX(1);
		float y = event.getY(0) + event.getY(1);
		middlePoint.set(x /2, y / 2);
		return middlePoint;
	}
	
	protected void setHoldTimeout() {
		mActionOccuredDuringTimeout = false;
		mTimeoutHandler.removeCallbacks(mTouchAndHoldTask);
        mTimeoutHandler.postDelayed(mTouchAndHoldTask, HOLD_TIMEOUT);
	}

	protected Runnable mTouchAndHoldTask = new Runnable() {
		public void run() {
			if (!mActionOccuredDuringTimeout) {
				fireEvent(new MapHoldEvent(this, mStartingPoint));
			}
		}
	};
}
