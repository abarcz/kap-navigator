package kapnav.core;

import java.text.DecimalFormat;

import android.content.Context;
import android.graphics.Color;
import android.location.Location;
import android.widget.TextView;

public class StatsView extends TextView {
	
	int mLatitudeDegrees = 0;
	int mLatitudeMinutes = 0;
	int mLongtitudeDegrees = 0;
	int mLongtitudeMinutes = 0;
	
	Location mLocation;

	public StatsView(Context context) {
		super(context);
		mLocation = new Location("");
		this.setLines(2);
		this.setTextColor(Color.BLACK);
		this.setBackgroundColor(Color.LTGRAY);
		this.setTextSize(20);
		displayStats();
	}
	
	public void setLocation(Location location) {
		if (location != null) {
			mLocation = location;
			displayStats();
		}
	}
	
	public void displayStats() {
		double latitude = mLocation.getLatitude();
		int latitudeDegrees = (int) latitude;
		int latitudeMinutes = (int) ((latitude % 1) * 60);
		char latSymbol = (latitude >= 0)?'N':'S';
		
		double longtitude = mLocation.getLongitude();
		int longtitudeDegrees = (int) longtitude;
		int longtitudeMinutes = (int) ((longtitude % 1) * 60);
		char lonSymbol = (longtitude >= 0)?'E':'W';
		
		float speedMps = mLocation.getSpeed();
		double speedKnots = 0.514 * speedMps;
		
		int course = (int) mLocation.getBearing();
		
		String latDegStr = String.format("%02d", latitudeDegrees);
		String latMinStr = String.format("%02d", latitudeMinutes);
		String lonDegStr = String.format("%03d", longtitudeDegrees);
		String lonMinStr = String.format("%02d", longtitudeMinutes);
		String cogStr = String.format("%03d", course);
		
		DecimalFormat speedFormat = new DecimalFormat("00.0");
		
		String deg = " ";
		String min = "'";
		String pad = "  ";
		String topLine = pad + latDegStr + deg + latMinStr + min + latSymbol
				+ "    " + lonDegStr + deg + lonMinStr + min + lonSymbol + pad;
		String bottomLine = pad + "SOG: " + speedFormat.format(speedKnots) + "kts "
				+ "COG: " + cogStr + deg + pad;
		this.setText(topLine + '\n' + bottomLine);
	}

}
