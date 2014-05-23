package com.kurovsky.simplespeedometer;

import android.content.Context;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.location.GpsStatus;

	public class Positioning extends Thread implements LocationListener {
		private static final int ONE_MINUTE = 1000 * 60 * 1;
	    private LocationManager mLocationManager;
	    private Location  mLocation = new Location(LocationManager.GPS_PROVIDER); 
	    private Location  mCurrentBestLocation = null;
	    private Positioning mPositioning;
	    private volatile Handler mHandler;
	    private long mUpdateFreq = 0;
	    private Handler mUiHandler;
	    private volatile boolean mFlagReqLocUpdates = false;
	    private volatile boolean mFlagStopLocUpdates = false;
	    private Context mContext;
		
		public Positioning(Context context, int UpdateFreq) {
			mLocationManager = (LocationManager)context.getSystemService(context.LOCATION_SERVICE); 
			mPositioning = this;
			mUpdateFreq = UpdateFreq;
			mUiHandler = ((SimpleSpeedometer)context).mHandler;
			mContext = context;
			start();
		}

		public boolean IsPositioningEnabled() {
			try {
				if (mLocationManager != null) {
					if (mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) return true;
				}
			}
			catch(Exception e){
				android.util.Log.v("Sp", "No suitable permission is present for the Gps provider or provider is null");
			}
			return false;
		}

		public boolean LaunchLocationSettings(){
			android.util.Log.v("", "Launching GPS settings activity...");
			Intent callIntent = new Intent(new Intent("android.settings.LOCATION_SOURCE_SETTINGS"));
			mContext.startActivity(callIntent);
	        return true;
	    }
		
		private void StopLocUpdates() {
			try {
				mLocationManager.removeUpdates(mPositioning);
			}
			catch (Exception ex){	
				android.util.Log.v("Sp", "LocationThread: removeUpdates failed!");
			}
		}
		
		private void ReqLocUpdates() {
			StopLocUpdates();
		
			try {
				mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, mUpdateFreq, 0.0F, mPositioning);	
			}
			catch (Exception ex){ 
				android.util.Log.v("Sp", "LocationThread: LocationManager.GPS_PROVIDER failed!");
			}

			try {
				mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, mUpdateFreq, 0.0F, mPositioning);
			}
			catch (Exception ex) {
				android.util.Log.v("Sp", "LocationThread: LocationManager.NETWORK_PROVIDER failed!");
			}
		}
		
	    public void run() {
	    	if (mLocationManager == null) return;

	    	Looper.prepare();
	    
	    	mHandler = new Handler() {
	    		public void handleMessage(Message msg) {
	    			if (msg.what == 101){
	    				StopLocUpdates();
	    			}
	    			else if (msg.what == 102){   	
	    				ReqLocUpdates();
	    			}
	    		};
	    	};
	    	
	    	if (mFlagReqLocUpdates){
	    		ReqLocUpdates();
	    	}
	    	else if (mFlagStopLocUpdates)
	    		StopLocUpdates();

	    	Looper.loop();
	    }
		
	    public void StartPositioning() {
	    	mFlagReqLocUpdates = true;
	    	if (mHandler != null) {
	    		Message msg = mHandler.obtainMessage();
	    		msg.what = 102;
	    		mHandler.sendMessage(msg);
	    	}
	    }

	    public void StopPositioning() {
	    	mFlagStopLocUpdates = true;
	    	if (mHandler != null) {
	    		Message msg = mHandler.obtainMessage();
	    		msg.what = 101;
	    		mHandler.sendMessage(msg);
	    	}
	    }

	    public double GetSpeed() {
	    	double Speed = -1.;
	    	synchronized (this) {
	    		if (mLocation.hasSpeed())
	    			Speed = mLocation.getSpeed();
	    	}
	    	return Speed;
	    }

	    public void onLocationChanged(Location location) {
	    	if (isBetterLocation(location, mCurrentBestLocation)) {
	    		synchronized (this) {
	    			mLocation.set(location);
	    		}

				if (mLocation.hasSpeed()) {
					Message m = Message.obtain(mUiHandler, SimpleSpeedometer.MSG_NEW_SPEED, null);
					mUiHandler.sendMessage(m);
				}

				if (mCurrentBestLocation == null)
					mCurrentBestLocation = new Location(location); 
				else
					mCurrentBestLocation.set(location);
			}
		}
		
		/** Determines whether one Location reading is better than the current Location fix
	    * @param location  The new Location that you want to evaluate
		* @param currentBestLocation  The current Location fix, to which you want to compare the new one */
		protected boolean isBetterLocation(Location location, Location currentBestLocation) {
			if (currentBestLocation == null) {
				// A new location is always better than no location
				return true;
			}
		
			// Check whether the new location fix is newer or older
			long timeDelta = location.getTime() - currentBestLocation.getTime();
			boolean isSignificantlyNewer = timeDelta > ONE_MINUTE;
			boolean isSignificantlyOlder = timeDelta < -ONE_MINUTE;
			boolean isNewer = timeDelta > 0;		
					
			// DG-Kurovsky: Return to NETWORK_PROVIDER only if GPS_PROVIDER is significantly outdated
			if (!location.getProvider().equals(LocationManager.GPS_PROVIDER) && currentBestLocation.getProvider().equals(LocationManager.GPS_PROVIDER) && !isSignificantlyNewer) 
				return false;

			// If it's been more than two minutes since the current location, use the new location
			// because the user has likely moved
			if (isSignificantlyNewer) {
				return true;
			// If the new location is more than two minutes older, it must be worse
			} else if (isSignificantlyOlder) {
				return false;
			}
		
			// Check whether the new location fix is more or less accurate
			int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
			boolean isMoreAccurate = (accuracyDelta < 0);
			boolean isSignificantlyLessAccurate = (accuracyDelta > 200);
		
			// Determine location quality using a combination of timeliness and accuracy
			if (isMoreAccurate) {
				return true;
			} else if (isNewer && !isSignificantlyLessAccurate) {
				return true;
			}
			return false;
		}
		
		public void onProviderDisabled(String provider) {
		}

		public void onProviderEnabled(String provider) {
		}

		public void onStatusChanged(String arg0, int arg1, Bundle arg2) {
		}
	}
	

