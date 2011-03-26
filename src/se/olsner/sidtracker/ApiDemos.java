package se.olsner.sidtracker;

import android.app.Activity;
import android.os.Bundle;

public class ApiDemos extends Activity {
	
	static {
		System.loadLibrary("sidtracker");
	}
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        NativeTest.testFunc(42, false);
        NativeTest.testFunc(43, true);
    }
}