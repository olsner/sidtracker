package se.olsner.sidtracker;

import android.app.Activity;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Bundle;

public class ApiDemos extends Activity {

	private static final class SIDThread implements Runnable {
		public void run() {
			SID sid = new SID();
			AudioTrack output = new AudioTrack(AudioManager.STREAM_MUSIC, sid.getSampleRate(), AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, 22050, AudioTrack.MODE_STREAM);
			output.play();
			System.err.println("output state is "+output.getState()+", play state is "+output.getPlayState());
			
			sid.write(24, 15); // volume = max
			sid.write(5, 0); // attack/decay
			sid.write(6, 0xf0); // sustain/release

			sid.write(0, 0xd6); // note, low byte
			sid.write(1, 0x1c); // note, high byte

			// FIXME Set up a threaded producer/consumer thingy for this!
			byte gate = 16;
			short[] buffer = new short[11025];
			long samples = 0;
			while (true)
			{
				if (samples > 44100) // 1s
				{
					sid.write(4, gate ^= 1);
					samples -= 44100;
				}
				double time0 = System.nanoTime();
				sid.clockFully(buffer, 0, buffer.length);
				double time1 = System.nanoTime();
				System.err.println(""+buffer.length+" samples generated in "+nano2s(time1 - time0)+"s");
				int outputted = output.write(buffer, 0, buffer.length);
				samples += outputted;
				double time2 = System.nanoTime();
				System.err.println(""+outputted+" samples sent to output in "+nano2s(time2 - time1)+"s");
				
			}
		}

		private double nano2s(double d) {
			return d / 1000000000;
		}
	}

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
		NativeTest.testFunc(1, 2);

		new Thread(new SIDThread()).start();
	}
}