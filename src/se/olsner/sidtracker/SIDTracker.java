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

import com.googlecode.androidannotations.annotations.*;

@EActivity(R.layout.sidtracker)
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
	private SoundQueue queue = new SoundQueue();

	@ViewById
	TrackView trackView;
	
	InputStream openAsset(String path) throws IOException {
		return new LZMA2InputStream(getAssets().open(path), 1 << 20);
	}

	private static final int LOOKAHEAD = 1000;

	BufferedTrack openTrack(String name) throws IOException {
		return new BufferedTrack(
			new InputStreamTrack(openAsset(name+".lzma2")),
			LOOKAHEAD);
	}

	//@AfterViews
	void startThreads() {
		SID sid = new SID();
		Log.d("SIDTracker", "Starting threads...");

		try {
			BufferedTrack track = openTrack("spellbound");
			SIDBackingTrack sidBackingTrack =
				new SIDBackingTrack(sid, queue, track);
			new Thread(sidBackingTrack).start();

			trackView.init(track, sidBackingTrack);
		} catch (IOException e) {
			Log.e("SIDTracker", "Failed loading backing track", e);
			throw new RuntimeException(e);
		}
		new Thread(new AudioPlayerThread(queue, sid.getSampleRate())).start();
	}

	@Override
	protected void onResume() {
		super.onResume();
		queue.resume();
		startThreads();
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
