package kapnav.core;

import java.text.DecimalFormat;
import java.util.Timer;
import java.util.TimerTask;

import kapnav.gestures.MapDragEvent;
import kapnav.gestures.MapGestureListener;
import kapnav.gestures.MapGestureReader;
import kapnav.gestures.MapHoldEvent;
import kapnav.gestures.MapUpEvent;
import kapnav.gestures.MapZoomEvent;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.PointF;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.Toast;

public class KapNavActivity extends Activity  implements MapGestureListener {
	
	SharedPreferences mPreferences;
	float mStoredMapCenterX;
	float mStoredMapCenterY;
	float mStoredMapScale;
	boolean mStoredMapCenterOnCursor;
	
	MapGestureReader mGestureReader = null;
	
	LocationManager mLocationManager = null;
	LocationListener mLocationListener = null;
	boolean mLocationWasSet = false;
	Timer mLocationAvailableTimer = null;
	
	RelativeLayout mLayout = null;
	
	static final int FILE_SELECT_CODE = 0;
	String mMapFilename = "";
	MapView mMapView = null;
	StatsView mStatsView = null;
	CompassView mCompassView = null;
	
	SensorManager mSensorManager;
	Sensor mCompassSensor;
	boolean mCompassActive = true;
	
	final SensorEventListener mCompassListener = new SensorEventListener() {
		public void onSensorChanged(SensorEvent event) {
			updateSensorValues(event.values);
		}
		public void onAccuracyChanged(Sensor sensor, int accuracy) {
		}
	};
	
	protected void updateSensorValues(float[] values) {
		mCompassView.updateSensorValues(values);
	}
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Log.d("CORE", "onCreate");
		loadPreferences();
		
		initializeUserInterface();
		initializeCompassServices();
		initializeLocationServices();
	}
	
	protected void initializeUserInterface() {
		mGestureReader = new MapGestureReader();
		mGestureReader.addEventListener(this);
		
		mLayout =  new RelativeLayout(this);
		RelativeLayout.LayoutParams labelLayoutParams = new RelativeLayout.LayoutParams(
				LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);
		mLayout.setLayoutParams(labelLayoutParams);

		if (mMapFilename == "") {
			mMapFilename = "/sdcard/kapnav/ursynow.png";
		}
		mMapView = new MapView(getApplicationContext());
		
		KapNavApplication appState = ((KapNavApplication)this.getApplication());
		if (appState.hasMap()) {
			Bitmap map = appState.getMap();
			mMapView.restoreMap(mMapFilename, map);
		} else {
			Bitmap map = mMapView.loadMap(mMapFilename);
			appState.storeMap(map);
		}
		mMapView.setOnTouchListener(mGestureReader);
		mMapView.translateMap(mStoredMapCenterX, mStoredMapCenterY);
		mMapView.zoomMap(mStoredMapScale, new PointF(mStoredMapCenterX, mStoredMapCenterY));
		if (mStoredMapCenterOnCursor) {
			mMapView.centerOnCursor();
		}
		mLayout.addView(mMapView);

		mCompassView = new CompassView(getApplicationContext());
		mCompassView.invalidate();
		mLayout.addView(mCompassView);
		
		mStatsView = new StatsView(getApplicationContext());
		mLayout.addView(mStatsView);		

		setContentView(mLayout);
	}
	
	protected void initializeCompassServices() {
		mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		mCompassSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
		
		if (mCompassActive) {
			mSensorManager.registerListener(mCompassListener, mCompassSensor,
					SensorManager.SENSOR_DELAY_GAME);
		} else {
			mCompassView.setVisibility(View.INVISIBLE);
		}
	}
	
	protected void initializeLocationServices() {
		mLocationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
		mLocationListener = new LocationListener() {
			public void onLocationChanged(Location location) {
				setLocation(location);
			}

			public void onStatusChanged(String provider, int status, Bundle extras) {}

			public void onProviderEnabled(String provider) {}

			public void onProviderDisabled(String provider) {}
		};
		  
		Location lastKnownLocation = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
		setLocation(lastKnownLocation);
		
		mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, mLocationListener);
		
		mLocationAvailableTimer = new Timer();
	}
	
	class LocationAvailableTask extends TimerTask {
		private Handler gpsCheckHandler = new Handler(){
			@Override
			public void dispatchMessage(Message msg) {
				super.dispatchMessage(msg);
				checkIfLocationUpdated();
			}
		};
		public void run() { 
			gpsCheckHandler.sendEmptyMessage(0);
		}
	}
	
	protected void checkIfLocationUpdated() {
		if (!mLocationWasSet) {
			Toast.makeText(this, "GPS position not available.", 
					Toast.LENGTH_LONG).show();
		}
		mLocationWasSet = false;
	}
	
	protected void setLocation(Location location) {
		if (location != null) {
			mMapView.setCursorLocation(location);
			mStatsView.setLocation(location);
			mLocationWasSet = true;
		}
	}

	protected void showFileChooser() {
		Intent intent = new Intent(Intent.ACTION_GET_CONTENT); 
		intent.setType("*/*"); 
		intent.addCategory(Intent.CATEGORY_OPENABLE);
		try {
			startActivityForResult(
					Intent.createChooser(intent, "Select map file using appropriate file manager"),
					FILE_SELECT_CODE);
		} catch (android.content.ActivityNotFoundException ex) {
			// Potentially direct the user to the Market with a Dialog
			Toast.makeText(this, "Please install a File Manager.", 
					Toast.LENGTH_SHORT).show();
		}
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		//Log.d("CORE", "onActivityResult");
		switch (requestCode) {
			case FILE_SELECT_CODE:	  
				if (resultCode == RESULT_OK) {  
					Uri uri = data.getData();
					String path = getPath(this, uri);
					Bitmap map = mMapView.loadMap(path);
					if (map != null) {
						KapNavApplication appState = ((KapNavApplication)this.getApplication());
						appState.storeMap(map);
					}
					mMapFilename = mMapView.getMapFilename();
				}		   
				break;
		}
		super.onActivityResult(requestCode, resultCode, data);
	}
	
	public static String getPath(Context context, Uri uri) {
		if ("content".equalsIgnoreCase(uri.getScheme())) {
			String[] projection = { "_data" };
			Cursor cursor = null;

			try {
				cursor = context.getContentResolver().query(uri, projection, null, null, null);
				int column_index = cursor
				.getColumnIndexOrThrow("_data");
				if (cursor.moveToFirst()) {
					return cursor.getString(column_index);
				}
			} catch (Exception e) {
				// Eat it
			}
		} else if ("file".equalsIgnoreCase(uri.getScheme())) {
			return uri.getPath();
		}
		return null;
	}

	protected void loadPreferences () {
		mPreferences = getApplicationContext().getSharedPreferences("KAP_NAV", Context.MODE_PRIVATE);
		mCompassActive = mPreferences.getBoolean("SHOW_COMPASS", true);
		mMapFilename = mPreferences.getString("MAP_FILENAME", "");
		
		mStoredMapCenterX = mPreferences.getFloat("MAP_CENTER_X", 0);
		mStoredMapCenterY = mPreferences.getFloat("MAP_CENTER_Y", 0);
		mStoredMapScale = mPreferences.getFloat("MAP_SCALE", 1);
		mStoredMapCenterOnCursor = mPreferences.getBoolean("MAP_CENTER_ON_CURSOR", false);
	}
	
	protected void storePreferences () {
		mPreferences.edit().putBoolean("SHOW_COMPASS", mCompassActive).commit();
		mPreferences.edit().putString("MAP_FILENAME", mMapFilename).commit();
		
		PointF viewCenter = mMapView.getViewCenter();
		mPreferences.edit().putFloat("MAP_CENTER_X", viewCenter.x).commit();
		mPreferences.edit().putFloat("MAP_CENTER_Y", viewCenter.y).commit();
		
		float mapScale = mMapView.getScale();
		mPreferences.edit().putFloat("MAP_SCALE", mapScale).commit();
		
		boolean centerOnCursor = mMapView.getCenterOnCursorState();
		mPreferences.edit().putBoolean("MAP_CENTER_ON_CURSOR", centerOnCursor).commit();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		mLocationAvailableTimer = new Timer();
		mLocationAvailableTimer.scheduleAtFixedRate(new LocationAvailableTask(), 5000, 10000);
	}
	
	@Override
	protected void onPause() {
		mLocationAvailableTimer.cancel();
		mLocationAvailableTimer.purge();
		mLocationAvailableTimer = null;
		super.onPause();
	}
	
	@Override
	public void onDestroy() {
		Log.d("CORE", "onDestroy");
		mMapView.unlinkMap();
		System.gc();
		storePreferences();
		super.onDestroy();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main_menu, menu);
		return true;
	}
	
	protected void activateCompass() {
		mSensorManager.registerListener(mCompassListener, mCompassSensor,
				SensorManager.SENSOR_DELAY_GAME);
		mCompassView.setVisibility(View.VISIBLE);
		mCompassActive = true;
	}
	
	protected void disableCompass() {
		mSensorManager.unregisterListener(mCompassListener);
		mCompassView.setVisibility(View.INVISIBLE);
		mCompassActive = false;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.my_location_menu:
				mMapView.centerOnCursor();
				return true;
			case R.id.map_select_menu:
				showFileChooser();
				return true;
			case R.id.compass_menu:
				if (mCompassActive) {
					disableCompass();
				} else {
					activateCompass();
				}
				return true;
			default:
				break;
		}
		return super.onOptionsItemSelected(item);
	}

	public void mapGestureOccured(MapZoomEvent event) {
		//Log.d("EVENT", "ZOOM");
		float scale = event.getScale();
		PointF scaleCenter = event.getScaleCenter();
		
		mMapView.zoomMap(scale, scaleCenter);	
	}

	public void mapGestureOccured(MapDragEvent event) {
		//Log.d("EVENT", "DRAG");
		float translationX = event.getTranslationX();
		float translationY = event.getTranslationY();
		
		mMapView.translateMap(translationX, translationY);
	}
	
	public void mapGestureOccured(MapHoldEvent event) {
		//Log.d("EVENT", "HOLD");
		PointF target = event.getHoldPoint();
		double distance = mMapView.calculateMilesDistanceFrom(target);
		Log.d("EVENT", "HOLD: " + event.getHoldPoint().x + ", " + event.getHoldPoint().y + ": " + distance);
		
		DecimalFormat distanceFormat = new DecimalFormat("####0.00");
		Toast.makeText(this, "" + distanceFormat.format(distance) + "Nm",
				Toast.LENGTH_SHORT).show();
	}
	
	public void mapGestureOccured(MapUpEvent event) {
		//Log.d("EVENT", "UP");

	}
}