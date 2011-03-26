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
			// TODO Set up some nice test contents in the registers...
			short[] buffer = new short[44100];
			while (true)
			{
				sid.clockFully(buffer);
				output.write(buffer, 0, buffer.length);
			}
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