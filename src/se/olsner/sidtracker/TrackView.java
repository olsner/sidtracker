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

    private static final int HEIGHT_MS = 1000;

    private static final int FILL_COLOR = 0xffff0000;
    private static final int CLEAR_COLOR = 0xff000000;
    private static final int CLEAR_COLOR_OK = 0xff00ff00;
    private static final int CLEAR_COLOR_GATE = 0xff0000ff;
    private static final Paint FILL_PAINT = new Paint();
    static {
        FILL_PAINT.setColor(FILL_COLOR);
        FILL_PAINT.setStyle(Paint.Style.FILL);
    }

    boolean lastGate = false;
    boolean correctGate = false;
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
        postInvalidateDelayed(100);
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
            Log.d("TouchView", "user gate="+gate);

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
        //setBackgroundColor(correctGate ? 0xffff0000 : 0xff000000);
        this.correctGate = correctGate;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        long time = sidBackingTrack.getClock();
        // TODO Should just make sure getFutureItems returns at least one event
        // after the time, rather than guess that 2*h is enough to include it.
        int[] items = sidBackingTrack.getFutureItems(2 * HEIGHT_MS);
        final int h = getHeight(), w = getWidth();
        int lasty = h;
        boolean lastgate = sidBackingTrack.getCorrectGate();
//        Log.d("TouchView", "lastgate="+lastgate+" lasty="+lasty+" t="+time);
        canvas.drawColor(correctGate && lastGate ? CLEAR_COLOR_OK : CLEAR_COLOR);
        for (int i = 0; i < items.length; i += 2) {
            int ms = items[i] >> 1;
            boolean gate = (items[i] & 1) != 0;

            int y = h - (h * ms / HEIGHT_MS);
            int x = (w * items[i + 1]) / SID.MAX_FREQUENCY;

//            Log.d("TouchView", "lastgate="+lastgate+" gate="+gate+" @"+ms+"ms: "+y+".."+lasty);

            if (lastgate) canvas.drawRect(x, Math.min(y, lasty - 1), x + 1, lasty, FILL_PAINT);
            lasty = y;
            lastgate = gate;
        }
        postInvalidateDelayed(10);
    }
}
