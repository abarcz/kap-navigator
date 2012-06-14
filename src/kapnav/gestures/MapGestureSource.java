package kapnav.gestures;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class MapGestureSource {

	private List<MapGestureListener> mListeners = new ArrayList<MapGestureListener>();

	public synchronized void addEventListener(MapGestureListener listener)  {
		mListeners.add(listener);
	}

	public synchronized void removeEventListener(MapGestureListener listener)   {
		mListeners.remove(listener);
	}

	protected synchronized void fireEvent(MapZoomEvent event) {
		Iterator<MapGestureListener> i = mListeners.iterator();
		while(i.hasNext())  {
			i.next().mapGestureOccured(event);
		}
	}
	
	protected synchronized void fireEvent(MapDragEvent event) {
		Iterator<MapGestureListener> i = mListeners.iterator();
		while(i.hasNext())  {
			i.next().mapGestureOccured(event);
		}
	}
	
	protected synchronized void fireEvent(MapHoldEvent event) {
		Iterator<MapGestureListener> i = mListeners.iterator();
		while(i.hasNext())  {
			i.next().mapGestureOccured(event);
		}
	}
	
	protected synchronized void fireEvent(MapUpEvent event) {
		Iterator<MapGestureListener> i = mListeners.iterator();
		while(i.hasNext())  {
			i.next().mapGestureOccured(event);
		}
	}
}
