package kapnav.gestures;

import java.util.EventObject;

public class MapGestureEvent extends EventObject {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	final String mGestureName;
	
	public MapGestureEvent(Object source, String gestureName) {
		super(source);
		mGestureName = gestureName;
	}
	
	public String getGestureName() {
		return mGestureName;
	}
}
