package com.kurovsky.simplespeedometer;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

public class SimpleSpeedometer extends FragmentActivity {
	private static Positioning mPositioning;
	private static TextView mSpeedTv = null;
	private static StringBuilder mString = new StringBuilder();
	private static boolean mIsKmH = true;
	private static boolean mIsStarted = false;
	private static boolean mIsLocationSettingsLaunched = false;
	private static double mSpeed = -1;
	private Typeface mFont;
	
	SpeedFragment mSpeedFragment;

	public static final int MSG_NEW_SPEED = 10;

	public static Handler mHandler = new Handler() {
		public void handleMessage(Message msg) {
			if (msg.what == MSG_NEW_SPEED) {
				UpdateSpeedTv();
			}
		}
	};

	private static void SetSpeedText() {
		int SpeedRounded;

		if (mIsKmH)
			SpeedRounded = (int) Math.round(mSpeed * 3.6);
		else
			SpeedRounded = (int) Math.round(mSpeed * 2.2369362920544);

		mString.setLength(0);
		mString.append(SpeedRounded);
		mSpeedTv.setText(mString.toString());
	}
	
	public static void UpdateSpeedTv() {
		if (mSpeedTv != null) {
			mSpeed = mPositioning.GetSpeed();
			if (mSpeed == -1)
				mSpeedTv.setText("-");
			else 
				SetSpeedText();
		}
	}

	private void UpdateUnitsTv() {
		TextView UnitsTv = (TextView) findViewById(R.id.UnitsView);
		if (mIsKmH) 
			UnitsTv.setText("km/h");
		else
			UnitsTv.setText("mph");
	}
	
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_simple_speedometer);

        mPositioning = new Positioning(this, 0);
        
        FragmentManager Fm = getSupportFragmentManager();
        mSpeedFragment = (SpeedFragment) Fm.findFragmentByTag("SpeedFragment");

        if (mSpeedFragment == null) {
        	mSpeedFragment = new SpeedFragment();
        	Fm.beginTransaction().add(mSpeedFragment, "SpeedFragment").commit();
        }
   
        TextView UnitsTv = (TextView) findViewById(R.id.UnitsView);
        UnitsTv.setOnClickListener(new View.OnClickListener() {
        	public void onClick(View v) {
        		mIsKmH = !mIsKmH;
        		UpdateSpeedTv();
        		UpdateUnitsTv();
        	}
        });
        UpdateUnitsTv();
        
        mSpeedTv = (TextView) findViewById(R.id.SpeedView);
        mSpeedTv.setOnClickListener(new View.OnClickListener() {
        	public void onClick(View v) {
        		mIsKmH = !mIsKmH;
        		UpdateSpeedTv();
        		UpdateUnitsTv();
        	}
        });

        final Button Btn = (Button) findViewById(R.id.ButtonStart);

        Btn.setOnClickListener(new View.OnClickListener() {
        	public void onClick(View v) {
        		if (!mPositioning.IsPositioningEnabled()){
        			mIsLocationSettingsLaunched = true;
        			mPositioning.LaunchLocationSettings();
        		}
        		else {
        			mIsStarted = !mIsStarted;
        			OnStartPositioning(mIsStarted);
        		}
        	}
        });

        mFont = Typeface.createFromAsset(getAssets(), "RobotoCondensed-Regular.ttf");
        if (mFont != null) {
        	mSpeedTv.setTypeface(mFont);
        	UnitsTv.setTypeface(mFont);
        	Btn.setTypeface(mFont);
        }
    }
    
	protected void onPause() {	
		super.onPause();
		
		SharedPreferences Sp = PreferenceManager.getDefaultSharedPreferences(this);
		SharedPreferences.Editor PrefsEditor = Sp.edit();
		PrefsEditor.putBoolean("IsKmH", mIsKmH);
		PrefsEditor.putBoolean("IsStarted", mIsStarted);
		PrefsEditor.putBoolean("IsLocationSettingsLaunched", mIsLocationSettingsLaunched);
		PrefsEditor.commit();
		
		if (mSpeedFragment != null)
			mSpeedFragment.SetSpeed(mSpeed);
		
		mPositioning.StopPositioning();
	}

	protected void onResume() {	 
		super.onResume();

		SharedPreferences Sp = PreferenceManager.getDefaultSharedPreferences(this);
		mIsKmH = Sp.getBoolean("IsKmH", true);
		mIsStarted = Sp.getBoolean("IsStarted", false);
		mIsLocationSettingsLaunched = Sp.getBoolean("IsLocationSettingsLaunched", false);

		UpdateUnitsTv();

		final Button Btn = (Button) findViewById(R.id.ButtonStart);
		Btn.setText(R.string.string_Start);
		
		if (!mPositioning.IsPositioningEnabled())
			Btn.setText(R.string.string_TurnOnGps);
		else if (mIsLocationSettingsLaunched) {
			mIsStarted = true; // Location Settings were launched and GPS turned on, start GPS immediately
			mIsLocationSettingsLaunched = false;
		}
		
		if (mIsStarted && mPositioning.IsPositioningEnabled())
			OnStartPositioning(mIsStarted);

		if (mSpeedFragment != null) {
			mSpeed = mSpeedFragment.GetSpeed();
			if ((mSpeedTv != null) && (mSpeed != -1))
				SetSpeedText();
		}
	}

	private void OnStartPositioning(boolean start) {
		final Button Btn = (Button) findViewById(R.id.ButtonStart);
			
		if (start) {
			Btn.setText(R.string.string_Stop);
			mPositioning.StartPositioning();
		}
		else {
			Btn.setText(R.string.string_Start);
			mPositioning.StopPositioning();
			mSpeedTv.setText("-");
		}
	}
	
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.simple_speedometer, menu);
        return true;
    }
}
