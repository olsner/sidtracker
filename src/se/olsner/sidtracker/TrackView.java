package se.olsner.sidtracker;

import android.content.*;
import android.graphics.*;
import android.os.Bundle;
import android.util.*;
import android.view.MotionEvent;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ToggleButton;

import java.io.*;

import com.googlecode.androidannotations.annotations.*;

@EView
public class TrackView extends View implements SIDBackingTrack.Listener {

    boolean lastGate = false;
    Track track;
    SIDBackingTrack sidBackingTrack;

    public TrackView(Context context, AttributeSet set) {
        super(context, set);
    }

    public void init(Track track, SIDBackingTrack backingTrack) {
        if (sidBackingTrack != null) {
            sidBackingTrack.setListener(null);
        }
        this.track = track;
        this.sidBackingTrack = backingTrack;
        backingTrack.setListener(this);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        /*int filterMin = SID.MAX_FILTER_CUTOFF / 3;
        int filterMax = SID.MAX_FILTER_CUTOFF;
        int freqMin = 100, freqMax = SID.MAX_FREQUENCY;

        int x = (int)event.getX();
        // Flip Y to make up = higher
        int y = getHeight() - (int)event.getY();

        int filter = filterMin + (filterMax - filterMin) * x / getWidth();
        int freq = freqMin + (freqMax - freqMin) * y / getHeight();

        if (lastFilter != filter)
        {
            lastFilter = filter;
        }
        if (lastFreq != freq)
        {
            lastFreq = freq;
        }*/

        boolean gate = getPointerStateFromAction(event.getActionMasked());
        if (lastGate != gate && sidBackingTrack != null) {
            sidBackingTrack.userSetGate(gate);
            lastGate = gate;
        }
        return true;
    }

    private boolean getPointerStateFromAction(int actionMasked) {
        switch (actionMasked)
        {
        case MotionEvent.ACTION_DOWN:
        case MotionEvent.ACTION_MOVE:
        case MotionEvent.ACTION_POINTER_DOWN:
            return true;
        default:
            return false;
        }
    }

    @UiThread @Override
    public void onGateChange(boolean correctGate) {
        Log.d("TouchView", "Setting background color to "+correctGate);
        setBackgroundColor(correctGate ? 0xffff0000 : 0xff000000);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        //long time = sidBackingTrack.getClock();
    }
}
