// vim:noet:
package se.olsner.sidtracker;

import android.app.Activity;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ToggleButton;

import java.io.*;

import org.tukaani.xz.XZInputStream;
import org.tukaani.xz.LZMA2InputStream;

public class SIDTracker extends Activity {

	class AudioPlayerThread implements Runnable {
		private SoundQueue queue;
		private int sampleRate;
		
		public AudioPlayerThread(SoundQueue queue, int sampleRate) {
			this.queue = queue;
			this.sampleRate = sampleRate;
		}
		
		public void run() {
			AudioTrack output = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, sampleRate / 2, AudioTrack.MODE_STREAM);
			output.play();
			Log.d("AudioPlayerThread", "output state is "+output.getState()+", play state is "+output.getPlayState());
			
			while (queue.isLive())
			{
				short[] buffer = queue.get();
				output.write(buffer, 0, buffer.length);
				queue.releaseBufferToPool(buffer);
			}
			
			output.stop();
			output.release();
			Log.d("AudioPlayerThread", "Audio thread done!");
		}
	}
	
	// FIXME This state needs to move out of the Activity since that'll get recreated at various times (rotation, for instance...)
	private SID sid = new SID();
	private SoundQueue queue = new SoundQueue();
	private SIDBackingTrack sidBackingTrack;
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		//createFollowMode();
	}

	private void createFollowMode() {
		final View touchView = findViewById(R.id.touchInputView);
		touchView.setOnTouchListener(new OnTouchListener() {
			int lastFilter = -1, lastFreq = -1;
			boolean lastGate = false;
			
			public boolean onTouch(View v, MotionEvent event) {
				int filterMin = SID.MAX_FILTER_CUTOFF / 3, filterMax = SID.MAX_FILTER_CUTOFF;
				int freqMin = 100, freqMax = SID.MAX_FREQUENCY;
				int x = (int)event.getX();
				// Flip Y to make up = higher
				int y = v.getHeight() - (int)event.getY();
				int filter = filterMin + (filterMax - filterMin) * x / v.getWidth();
				int freq = freqMin + (freqMax - freqMin) * y / v.getHeight();
				if (lastFilter != filter)
				{
					//sidBackingTrack.setFilterCutoff(filter);
					lastFilter = filter;
				}
				if (lastFreq != freq)
				{
					//sidBackingTrack.setFrequencyHz(0, freq);
					lastFreq = freq;
				}
				boolean gate = getPointerStateFromAction(event.getActionMasked());
				if (lastGate != gate) {
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
		});

		sidBackingTrack.setListener(new SIDBackingTrack.Listener() {
			public void onGateChange(final boolean correctGate) {
				touchView.post(new Runnable() {
					public void run() {
						Log.d("TouchView", "Setting background color to "+correctGate);
						touchView.setBackgroundColor(correctGate ? 0xffff0000 : 0xff000000);
						touchView.invalidate();
					}
				});
			}
		});
	}

	InputStream openAsset(String path) throws IOException {
		return new LZMA2InputStream(getAssets().open(path), 1 << 20);
	}

	Track openTrack(String name) throws IOException {
		return new BufferedTrack(
			new InputStreamTrack(openAsset(name+".lzma2")),
			16);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		queue.resume();
		try {
			sidBackingTrack = new SIDBackingTrack(
				sid, queue, openTrack("spellbound"));
			new Thread(sidBackingTrack).start();
		} catch (IOException e) {
			Log.e("SIDTracker", "Failed loading backing track", e);
			throw new RuntimeException(e);
		}
		createFollowMode();
		new Thread(new AudioPlayerThread(queue, sid.getSampleRate())).start();
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		queue.pause();
	}

	static double nano2s(double d) {
		return d / 1000000000;
	}
}
