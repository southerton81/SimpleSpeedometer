package com.kurovsky.simplespeedometer;

import android.os.Bundle;
import android.support.v4.app.Fragment;

public class SpeedFragment extends Fragment {
	private double mSpeed = -1;
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRetainInstance(true);
	}

	public void SetSpeed(double Speed) {
		mSpeed = Speed;
	}
	
	public double GetSpeed() {
		return mSpeed;
	}
}
