package se.olsner.sidtracker;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

import android.app.Activity;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Bundle;

public class SIDTracker extends Activity {

	private static class SoundQueue
	{
		ArrayBlockingQueue<short[]> queue = new ArrayBlockingQueue<short[]>(10);
		ArrayBlockingQueue<short[]> pool = new ArrayBlockingQueue<short[]>(10);
		boolean live = true;
		
		public boolean isLive() {
			return live;
		}
		
		public short[] getBufferFromPool()
		{
			return pool.poll();
		}
		
		public void releaseBufferToPool(short[] buffer)
		{
			pool.offer(buffer);
		}
		
		boolean post(short[] buffer)
		{
			try {
				return queue.offer(buffer, 1000, TimeUnit.SECONDS);
			} catch (InterruptedException e) {
				return false;
			}
		}
		
		short[] get()
		{
			try {
				return queue.poll(1, TimeUnit.SECONDS);
			} catch (InterruptedException e) {
				return null;
			}
		}
	}
	
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

			byte gate = 16;
			long samples = 0, cycles = 0;
			while (queue.isLive())
			{
				short[] buffer = queue.getBufferFromPool();
				if (buffer == null) buffer = new short[5000];
				if (samples > 44100) // 1s
				{
					sid.write(4, gate ^= 1);
					samples -= 44100;
				}
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
		}
	}

	static {
		System.loadLibrary("sidtracker");
	}

	private SoundQueue queue = new SoundQueue();
	private SID sid = new SID();
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		NativeTest.testFunc(42, false);
		NativeTest.testFunc(43, true);
		NativeTest.testFunc(1, 2);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		new Thread(new SIDThread(sid, queue)).start();
		new Thread(new AudioPlayerThread(queue, sid.getSampleRate())).start();
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		queue.live = false;
	}

	static double nano2s(double d) {
		return d / 1000000000;
	}
}