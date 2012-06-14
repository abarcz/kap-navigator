package kapnav.gestures;

import android.graphics.PointF;

public class MapHoldEvent extends MapGestureEvent {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	final PointF mPoint;

	public MapHoldEvent(Object source, PointF point) {
		super(source, "HOLD");
		mPoint = point;
	}
	
	public PointF getHoldPoint() {
		return mPoint;
	}

}
