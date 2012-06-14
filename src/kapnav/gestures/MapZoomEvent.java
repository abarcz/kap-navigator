package kapnav.gestures;

import android.graphics.PointF;

public class MapZoomEvent extends MapGestureEvent {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	final float mScale;
	final PointF mScaleCenter;

	public MapZoomEvent(Object source, PointF scaleCenter, float scale) {
		super(source, "ZOOM");
		mScaleCenter = scaleCenter;
		mScale = scale;
	}
	
	public float getScale() {
		return mScale;
	}
	
	public PointF getScaleCenter() {
		return mScaleCenter;
	}

}
