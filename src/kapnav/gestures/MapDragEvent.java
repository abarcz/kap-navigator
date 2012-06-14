package kapnav.gestures;

public class MapDragEvent extends MapGestureEvent {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	final float mTranslationX;
	final float mTranslationY;
	
	public MapDragEvent(Object source, float translationX, float translationY) {
		super(source, "DRAG");
		mTranslationX = translationX;
		mTranslationY = translationY;
	}
	
	public float getTranslationX() {
		return mTranslationX;
	}
	
	public float getTranslationY() {
		return mTranslationY;
	}

}
