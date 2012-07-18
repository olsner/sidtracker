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

public class MainMenu extends Activity {

    Typeface font;

    public MainMenu() {
    }

    private static void reportProgress(final ProgressBar bar, final int progress) {
        bar.post(new Runnable() { public void run() {
            bar.setProgress(progress);
        }});
    }
    private void reportInitializationDone(final View view) {
        view.post(new Runnable() { public void run() {
            view.setVisibility(View.GONE);
            TextView play = (TextView)findViewById(R.id.play);
            play.setTextColor(getResources().getColor(R.color.enabled));
            play.setEnabled(true);
            Log.i("MainMenu", "Initialization done, Play enabled!");
        }});
    }

    private void initSID() {
        final View progressView = findViewById(R.id.loading_layout);
        if (SID.isInitialized()) {
            Log.i("SID", "SID init already done, hiding progress.");
            reportInitializationDone(progressView);
            return;
        }

        final ProgressBar progress = (ProgressBar)findViewById(R.id.loading_progressbar);
        new Thread(new Runnable() { public void run() {
            Log.i("SID", "Initializing SID");
            reportProgress(progress, 1);
            new SID();
            reportProgress(progress, 100);
            reportInitializationDone(progressView);
            Log.i("SID", "SID init done");
        }} ).start();
    }

    private void setC64Font(int id) {
        ((TextView)findViewById(id)).setTypeface(font);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main_menu);

        font = Typeface.createFromAsset(getAssets(), "C64_User_Mono_v1.0-STYLE.ttf");
        setC64Font(R.id.play);
        setC64Font(R.id.exit);
        setC64Font(R.id.loading_text);
        initSID();
    }

    public void onPlayClicked(View v) {
        Log.i("MainMenu", "Play clicked, kicking off the stuff!");
        startActivity(new Intent(this, SIDTracker.class));
    }
}
