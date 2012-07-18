package se.olsner.sidtracker;

import android.app.*;
import android.content.*;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.*;

import java.io.*;

import com.googlecode.androidannotations.annotations.*;

@EActivity(R.layout.main_menu)
public class MainMenu extends Activity {

    Typeface font;

    public MainMenu() {
    }

    @UiThread
    void initializationDone() {
        View view = findViewById(R.id.loading_layout);
        view.setVisibility(View.GONE);

        TextView play = (TextView)findViewById(R.id.play);
        play.setTextColor(getResources().getColor(R.color.enabled));
        play.setEnabled(true);
        Log.i("MainMenu", "Initialization done, Play enabled!");
    }

    @AfterViews
    void initSID() {
        if (SID.isInitialized()) {
            Log.i("SID", "SID init already done, hiding progress.");
            initializationDone();
        } else {
            backgroundInitSID();
        }
    }

    @Background
    void backgroundInitSID() {
        Log.i("SID", "Initializing SID");
        new SID();
        initializationDone();
        Log.i("SID", "SID init done");
    }

    private void setC64Font(int id) {
        ((TextView)findViewById(id)).setTypeface(font);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @AfterViews
    void setFonts() {
        font = Typeface.createFromAsset(getAssets(), "C64_User_Mono_v1.0-STYLE.ttf");
        setC64Font(R.id.play);
        setC64Font(R.id.exit);
        setC64Font(R.id.loading_text);
    }

    @Click(R.id.play)
    public void onPlayClicked(View v) {
        Log.i("MainMenu", "Play clicked, kicking off the stuff!");
        startActivity(new Intent(this, SIDTracker.class));
    }
}
