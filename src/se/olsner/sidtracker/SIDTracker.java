package se.olsner.sidtracker;


import android.app.Activity;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Bundle;
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
				if (buffer == null) {
					System.err.println("Nothing in queue!");
					try {
						queue.wait();
					} catch (InterruptedException e) {
					}
					continue;
				}
				double time0 = System.nanoTime();
				output.write(buffer, 0, buffer.length);
				double time1 = System.nanoTime();
				System.err.println(""+buffer.length+" samples output in "+nano2s(time1-time0)+"s");
				queue.releaseBufferToPool(buffer);
			}
			
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
			sid.write(24, 15); // volume = max
			sid.write(5, 0); // attack/decay
			sid.write(6, 0xf0); // sustain/release

			sid.write(0, 0xd6); // note, low byte
			sid.write(1, 0x1c); // note, high byte

			sid.write(4, 16); // gate/waveform: triangle wave, gate=0
			long samples = 0, cycles = 0;
			while (queue.isLive())
			{
				pollControlMessage();
				short[] buffer = queue.getBufferFromPool();
				if (buffer == null) buffer = new short[441]; // 1/100th of a second... Maybe we should have less though?
				/*if (samples > 44100) // 1s
				{
					sid.write(4, gate ^= 1);
					samples -= 44100;
				}*/
				double time0 = System.nanoTime();
				long cycles0 = sid.clockFully(buffer, 0, buffer.length);
				double time1 = System.nanoTime();
				System.err.println(""+buffer.length+" samples for "+cycles0+"cycles in "+nano2s(time1-time0)+"s");
				cycles += cycles0;
				samples += buffer.length;
				// FIXME This will block for a long time waiting for the
				// playback thread to catch up. We may need to watch a
				// second queue for commands though...
				queue.post(buffer);
			}
			
			System.out.println("SID thread done!");
		}

		private void pollControlMessage() {
			Runnable control = queue.pollControlMessage();
			if (control != null)
			{
				System.err.println("Running control message! "+control);
				control.run();
			}
		}
	}

	static {
		System.loadLibrary("sidtracker");
	}

	private SID sid = new SID();
	private SoundQueue queue;
	private SIDControl sidControl;
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		NativeTest.testFunc(42, false);
		NativeTest.testFunc(43, true);
		NativeTest.testFunc(1, 2);
		
		ToggleButton gateToggle = (ToggleButton)findViewById(R.id.toggleGATE);
		gateToggle.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				sidControl.setGate(0, isChecked);
			}
		});
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		queue = new SoundQueue();
		sidControl = new SIDControl(sid, queue);
		new Thread(new SIDThread(sid, queue)).start();
		new Thread(new AudioPlayerThread(queue, sid.getSampleRate())).start();
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		queue.live = false;
		queue = null;
		sidControl = null;
	}

	static double nano2s(double d) {
		return d / 1000000000;
	}
}