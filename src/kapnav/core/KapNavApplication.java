package kapnav.core;

import android.app.Application;
import android.graphics.Bitmap;

public class KapNavApplication extends Application {
	
	Bitmap mMapData = null;
	
	public boolean hasMap() {
		return (mMapData != null);
	}
	
	public void storeMap(Bitmap map) {
		mMapData = map;
	}
	
	public Bitmap getMap() {
		return mMapData;
	}

}
