package kapnav.gestures;

import java.util.EventListener;

public interface MapGestureListener extends EventListener {
	
	public void mapGestureOccured(MapDragEvent event);
	public void mapGestureOccured(MapZoomEvent event);
	public void mapGestureOccured(MapHoldEvent event);
	public void mapGestureOccured(MapUpEvent event);

}
