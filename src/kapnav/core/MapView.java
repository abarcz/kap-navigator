package kapnav.core;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.location.Location;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

public class MapView extends ImageView {
	
	double mTopLeftLat;
	double mTopLeftLon;
	double mBottomRightLat;
	double mBottomRightLon;
	
	Location mLocation = null;
	
	Matrix mMapMatrix = null;
	boolean mMapLoaded = false;
	String mMapFilename = "";
	
	Paint mPaint = new Paint();
	Path mCursor = new Path();
	PointF mCursorPosition = null;
	boolean mCenterOnCursor = true;
	
	// set to true if you want the image to stick to borders of view
	final boolean VALIDATE_IMAGE_MOVE = false;
	
	public MapView(Context context) {
		super(context);
		mMapMatrix = new Matrix();
		this.setScaleType(ScaleType.MATRIX);
		mLocation = new Location("");

		//mCursor.moveTo(0, -10);
		//mCursor.lineTo(-10, 20);
		//mCursor.lineTo(10, 20);
		//mCursor.close();
		
		mCursor.moveTo(0, -15);
		mCursor.lineTo(-12, 22);
		mCursor.lineTo(0, 15);
		mCursor.lineTo(12, 22);
		mCursor.close();
		
		mPaint.setAntiAlias(true);
		mPaint.setColor(Color.MAGENTA);
	}
	
	public void setCursorLocation(Location location) {
		if (location != null) {
			mLocation = location;
		}
	}
	
	public Bitmap loadMap(String filename) {
		String plyFilename = removeExtension(filename) + ".ply";
		Bitmap map = null;
		try {
			map = BitmapFactory.decodeFile(filename);
			readCoordinatesFile(plyFilename);
		} catch (Exception e) {
			Log.d("MAPVIEW", "ERROR: " + e.getMessage());
			Toast.makeText(this.getContext(), "Couldn't load image and/or coordinates file.", 
					Toast.LENGTH_SHORT).show();
			return null;
		}
		setMap(map);
		Toast.makeText(this.getContext(), "Map: " + filename + " successfully loaded.", 
				Toast.LENGTH_SHORT).show();
		mMapFilename = filename;
		return map;
	}
	
	public void restoreMap(String filename, Bitmap map) {
		String plyFilename = removeExtension(filename) + ".ply";
		try {
			readCoordinatesFile(plyFilename);
		} catch (Exception e) {
			Log.d("MAPVIEW", "ERROR: " + e.getMessage());
			Toast.makeText(this.getContext(), "Couldn't load coordinates file.", 
					Toast.LENGTH_SHORT).show();
			return;
		}
		setMap(map);
		mMapFilename = filename;
	}
	
	public String getMapFilename() {
		return mMapFilename;
	}

	@Override
	protected void onDraw(Canvas canvas) {
		if (mCenterOnCursor) {
			centerOnCursor();
		}
		// draw map
		super.onDraw(canvas);
		
		// draw cursor
		PointF realCursorPosition = calculateCursorPosition();
		canvas.translate(realCursorPosition.x, realCursorPosition.y);
		canvas.rotate(calculateCursorRotation());
		canvas.drawPath(mCursor, mPaint);
	}
	
	protected float calculateCursorRotation() {
		return mLocation.getBearing();
	}
	
	protected PointF calculateCursorPosition() {
		if (!mMapLoaded) {
			return new PointF();
		}
		double latitude = mLocation.getLatitude();
		double longitude = mLocation.getLongitude();
		
		Rect imgBounds = this.getDrawable().getBounds();
		double imgWidth = imgBounds.right;
		double imgHeight = imgBounds.bottom;
		
		double x = ((longitude - mTopLeftLon) * imgWidth) / (mBottomRightLon - mTopLeftLon);
		double y = ((latitude - mTopLeftLat) * imgHeight) / (mBottomRightLat - mTopLeftLat);
		
		float[] values = new float[9];
		mMapMatrix.getValues(values);
		float newX = (float) x * values[0] + values[2];
		float newY = (float) y * values[4] + values[5];
		return new PointF(newX, newY);
	}
	
	public double calculateMilesDistanceFrom(PointF target) {
		PointF cursorPosition = calculateCursorPosition();
		
		float rawDistanceX = target.x - cursorPosition.x;
		float rawDistanceY = target.y - cursorPosition.y;
		double rawDistance = (float) Math.sqrt(Math.pow(rawDistanceX, 2) + Math.pow(rawDistanceY, 2));
		
		float[] values = new float[9];
		mMapMatrix.getValues(values);
		
		double scale = values[0];
		rawDistance = rawDistance / scale;
		
		Rect imgBounds = this.getDrawable().getBounds();
		double imgHeight = imgBounds.bottom;
		double latHeight = Math.abs(mTopLeftLat - mBottomRightLat);
		double milesHeight = latHeight * 60;
		
		double milesDistance = (rawDistance * milesHeight) / imgHeight;
		return milesDistance;
	}
	
	public void centerOnCursor() {
		mCenterOnCursor = true;
		if (!mMapLoaded) {
			return;
		}
		int viewWidth = this.getWidth();
		int viewHeight = this.getHeight();
		
		float viewCenterX = viewWidth / 2;
		float viewCenterY = viewHeight / 2;
		
		PointF cursorPosition = calculateCursorPosition();
		float translationX = viewCenterX - cursorPosition.x;
		float translationY = viewCenterY - cursorPosition.y;
		translateMap(translationX, translationY);
		//restore after translate
		mCenterOnCursor = true;
	}
	
	public boolean getCenterOnCursorState() {
		return mCenterOnCursor;
	}
	
	protected void readCoordinatesFile(String filename) throws IOException {
		FileInputStream fstream = new FileInputStream(filename);
		DataInputStream inputStream = new DataInputStream(fstream);
		BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
		String line;

		// the order of PLY points should be: topLeft, bottomLeft, bottomRight, topRight
		for (int j = 0; j < 4; j++) {
			line = reader.readLine();
			Log.d("MAPVIEW", line);
			String[] parts = line.split(",");
			if (j == 0) {
				Double latitude = new Double(parts[1]);
				Double longitude = new Double(parts[2]);
				mTopLeftLon = longitude;
				mTopLeftLat = latitude;
			} else if (j == 2) {
				Double latitude = new Double(parts[1]);
				Double longitude = new Double(parts[2]);
				mBottomRightLon = longitude;
				mBottomRightLat = latitude;
			}
		}
		inputStream.close();
		Log.d("MAPVIEW", "TL.x " + mTopLeftLon);
		Log.d("MAPVIEW", "TL.y " + mTopLeftLat);
		Log.d("MAPVIEW", "BR.x " + mBottomRightLon);
		Log.d("MAPVIEW", "BR.y " + mBottomRightLat);
	}
	
	protected static String removeExtension(String path) {
		int extensionIndex = path.lastIndexOf(".");
		if (extensionIndex == -1) {
			return path;
		}
		return path.substring(0, extensionIndex);
	}
	
	protected void setMap(Bitmap map) {
		if (mMapLoaded) {
			unlinkMap();
			System.gc();
		}
		this.setImageBitmap(map);
		mMapMatrix = new Matrix();
		this.setImageMatrix(mMapMatrix);
		mMapLoaded = true;
	}
	
	public void unlinkMap() {
		this.setImageBitmap(null);
		mMapLoaded = false;
	}
	
	public PointF getViewCenter() {
		float[] values = new float[9];
		mMapMatrix.getValues(values);
		float x = values[2];
		float y = values[5];
		return new PointF(x, y);
	}
	
	public float getScale() {
		float[] values = new float[9];
		mMapMatrix.getValues(values);
		return values[0];
	}
	
	/*public MapView(Context context, AttributeSet attrs) {
		super(context, attrs);
		// TODO Auto-generated constructor stub
		mMapMatrix = new Matrix();
	}

	public MapView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		// TODO Auto-generated constructor stub
		mMapMatrix = new Matrix();
	}*/
	
	public void zoomMap(float scale, PointF scaleCenter) {
		if (!mMapLoaded) {
			return;
		}
		mCenterOnCursor = false;
		if (VALIDATE_IMAGE_MOVE) {
			int viewWidth = this.getWidth();
			int viewHeight = this.getHeight();
			float viewRatio = (float)viewHeight / viewWidth;
			
			Rect imgBounds = this.getDrawable().getBounds();
			int imgWidth = imgBounds.right;
			int imgHeight = imgBounds.bottom;
			float imgRatio = (float)imgHeight / imgWidth;
			
			RectF imgBoundsF = rectToRectF(imgBounds);
			
			Matrix newMapMatrix = new Matrix();
			newMapMatrix.set(mMapMatrix);
			newMapMatrix.postScale(scale, scale, scaleCenter.x,
					scaleCenter.y);
			
			newMapMatrix.mapRect(imgBoundsF);
			
			int drawLeft = (int)imgBoundsF.left;
			int drawTop = (int)imgBoundsF.top;
			int drawRight = (int)imgBoundsF.right;
			int drawBottom = (int)imgBoundsF.bottom;
			
			float translateX = 0;
			float translateY = 0;
			if (drawLeft > 0) {
				translateX = -drawLeft;
			}
			if (drawTop > 0) {
				translateY = -drawTop;
			}
			
			if (imgRatio > viewRatio) {
				// keep bottom edge glued to view edge
				if (drawBottom < viewHeight) {
					float fix = viewHeight - drawBottom;
					float maxFix = -drawTop;
					if (fix <= maxFix) {
						translateY = fix;
					} else {
						return;
					}
				}
			} else {
				// keep right edge glued to view edge
				if (drawRight < viewWidth) {
					float fix = viewWidth - drawRight;
					float maxFix = -drawLeft;
					if (fix <= maxFix) {
						translateX = fix;
					} else {
						return;
					}
					
				}
			}
			
			Matrix newMatrix = new Matrix();
			newMatrix.set(mMapMatrix);
			newMatrix.postScale(scale, scale, scaleCenter.x,
					scaleCenter.y);
			PointF validTranslation = validateMapTranslation(newMatrix, translateX, translateY);
			if (validTranslation.equals(translateX, translateY)) {
				mMapMatrix.postScale(scale, scale, scaleCenter.x,
						scaleCenter.y);
				mMapMatrix.postTranslate(translateX, translateY);
				this.setImageMatrix(mMapMatrix);
				this.postInvalidate();
			}
		} else {
			mMapMatrix.postScale(scale, scale, scaleCenter.x,
					scaleCenter.y);
			this.setImageMatrix(mMapMatrix);
			this.postInvalidate();
		}
	}
	
	protected PointF validateMapTranslation(Matrix matrix, float translationX, float translationY) {
		int viewWidth = this.getWidth();
		int viewHeight = this.getHeight();
		
		if (viewWidth == 0) {
			//view is not initialized
			return new PointF(translationX, translationY);
		}
		
		float viewRatio = viewHeight / viewWidth;
		
		Rect imgBounds = this.getDrawable().getBounds();
		int imgWidth = imgBounds.right;
		int imgHeight = imgBounds.bottom;
		float imgRatio = imgHeight / imgWidth;
		
		RectF imgBoundsF = rectToRectF(imgBounds);
		
		Matrix newMapMatrix = new Matrix();
		newMapMatrix.set(matrix);
		newMapMatrix.postTranslate(translationX, translationY);
		
		newMapMatrix.mapRect(imgBoundsF);
		
		int drawLeft = (int)imgBoundsF.left;
		int drawTop = (int)imgBoundsF.top;
		int drawRight = (int)imgBoundsF.right;
		int drawBottom = (int)imgBoundsF.bottom;
		
		if (drawLeft > 0) {
			translationX = 0;
		}
		if (drawTop > 0) {
			translationY = 0;
		}	
		if (imgRatio > viewRatio) {
			// keep bottom edge glued to view edge
			if (drawBottom < viewHeight) {
				translationY = 0;
			}
			// the image must not escape to the left
			if ((drawRight < viewWidth) && (drawLeft < 0)) {
				translationX = 0;
			}
		} else {
			// keep right edge glued to view edge
			if (drawRight < viewWidth) {
				translationX = 0;
			}
			// the image must not escape to the top
			if ((drawBottom < viewHeight) && (drawTop <= 0)) {
				translationY = 0;
			}
		}
		
		PointF validTranslation = new PointF();
		validTranslation.x = translationX;
		validTranslation.y = translationY;
		return validTranslation;
	}
	
	public void translateMap(float translationX, float translationY) {
		if (!mMapLoaded) {
			return;
		}
		mCenterOnCursor = false;
		if (VALIDATE_IMAGE_MOVE) {
			PointF validTranslation = validateMapTranslation(mMapMatrix, translationX, translationY);
			mMapMatrix.postTranslate(validTranslation.x, validTranslation.y);
		} else {
			mMapMatrix.postTranslate(translationX, translationY);
		}
		this.setImageMatrix(mMapMatrix);
		this.postInvalidate();
	}
	
	public void printMatrix() {
		float[] values = new float[9];
		mMapMatrix.getValues(values);
		Log.d("MAPVIEW", "" + values[0] + ", " + values[1] + ", " + values[2]);
		Log.d("MAPVIEW", "" + values[3] + ", " + values[4] + ", " + values[5]);
		Log.d("MAPVIEW", "" + values[6] + ", " + values[7] + ", " + values[8]);
	}
	
	protected RectF rectToRectF(Rect source) {
		RectF target = new RectF();
		target.right = source.right;
		target.left = source.left;
		target.top = source.top;
		target.bottom = source.bottom;
		return target;
	}
}
