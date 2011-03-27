package se.olsner.sidtracker;


import android.app.Activity;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ToggleButton;

public class SIDTracker extends Activity {

	private static final class AudioPlayerThread implements Runnable {
		private SoundQueue queue;
		private int sampleRate;
		
		public AudioPlayerThread(SoundQueue queue, int sampleRate) {
			this.queue = queue;
			this.sampleRate = sampleRate;
		}
		
		public void run() {
			AudioTrack output = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, sampleRate / 2, AudioTrack.MODE_STREAM);
			output.play();
			System.err.println("output state is "+output.getState()+", play state is "+output.getPlayState());
			
			while (queue.isLive())
			{
				short[] buffer = queue.get();
				output.write(buffer, 0, buffer.length);
				queue.releaseBufferToPool(buffer);
			}
			
			output.stop();
			output.release();
			System.out.println("Audio thread done!");
		}
	}
	
	private static final class SIDThread implements Runnable {
		private SoundQueue queue;
		private SID sid;
		
		public SIDThread(SID sid, SoundQueue queue) {
			this.queue = queue;
			this.sid = sid;
		}

		public void run() {
			while (queue.isLive())
			{
				short[] buffer = queue.getBufferFromPool();
				if (buffer == null) buffer = new short[441]; // 1/100th of a second... Maybe we should have less though?
				sid.clockFully(buffer, 0, buffer.length);
				postBuffer(buffer);
			}
			
			System.out.println("SID thread done!");
		}

		private void postBuffer(short[] buffer) {
			queue.postRunningControlMessages(buffer);
		}
	}

	static {
		System.loadLibrary("sidtracker");
	}

	// FIXME This state needs to move out of the Activity since that'll get recreated at various times (rotation, for instance...)
	private SID sid = new SID();
	private SoundQueue queue = new SoundQueue();
	private SIDControl sidControl = new SIDControl(sid, queue);
	
	public SIDTracker() {
		initSID();
	}
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		
		ToggleButton gateToggle = (ToggleButton)findViewById(R.id.toggleGATE);
		gateToggle.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				sidControl.setGate(0, isChecked);
			}
		});
		
		final View touchView = findViewById(R.id.touchInputView);
		touchView.setOnTouchListener(new OnTouchListener() {
			public boolean onTouch(View v, MotionEvent event) {
				int filterMin = SID.MAX_FILTER_CUTOFF / 3, filterMax = SID.MAX_FILTER_CUTOFF;
				int freqMin = 100, freqMax = SID.MAX_FREQUENCY;
				int x = (int)event.getX();
				// Flip Y to make up = higher
				int y = v.getHeight() - (int)event.getY();
				sidControl.setFilterCutoff(filterMin + (filterMax - filterMin) * x / v.getWidth());
				sidControl.setFrequencyHz(0, freqMin + (freqMax - freqMin) * y / v.getHeight());
				return true;
			}
		});
	}

	private void initSID() {
		sid.write(23, (7 << 4) | 7); // resonancy = 7, enable filter for all channels
		sid.write(24, 16 | 15); // enable lowpass filter, volume = 15
		sid.write(5, 0); // attack/decay
		sid.write(6, 0xf0); // sustain/release

		sid.write(0, 0xd6); // note, low byte
		sid.write(1, 0x1c); // note, high byte

		sid.write(4, 32); // gate/waveform: triangle wave, gate=0
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		queue.resume();
		new Thread(new SIDThread(sid, queue)).start();
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