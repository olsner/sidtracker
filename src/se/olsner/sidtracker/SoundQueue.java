package se.olsner.sidtracker;

import android.util.Log;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

public class SoundQueue
{
	ArrayBlockingQueue<short[]> queue = new ArrayBlockingQueue<short[]>(2);
	ArrayBlockingQueue<short[]> pool = new ArrayBlockingQueue<short[]>(10);
	ArrayBlockingQueue<Runnable> controlQueue = new ArrayBlockingQueue<Runnable>(1);
	boolean live = false; // The queue starts out paused, use resume() before starting producers and consumers
	
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
			return queue.poll(1000, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			return null;
		}
	}

	public void postControlMessage(Runnable controlMessage) {
		try {
			Log.d("SoundQueue", "Offering control message...");
			controlQueue.offer(controlMessage, 1000, TimeUnit.SECONDS);
			Log.d("SoundQueue", "Control message offered");
		} catch (InterruptedException e) {
			assert false : e;
		}
	}
	
	public Runnable pollControlMessage() {
		return controlQueue.poll();
	}

	public void postRunningControlMessages(short[] buffer) {
		boolean posted = false;
		do {
			try {
				posted = queue.offer(buffer, 1, TimeUnit.MILLISECONDS);
			} catch (InterruptedException e) {
				// nothing...
			}
			Runnable control;
			while ((control = pollControlMessage()) != null) {
				Log.d("SoundQueue", "Running control message "+control);
				control.run();
				control = null;
			}
			
		} while (!posted);
	}

	public void pause() {
		live = false;
		// TODO Post something to make all waiting threads immediately stop waiting and return without a message...
		queue.clear();
		controlQueue.clear();
	}
	
	/**
	 * Call before starting up producers and consumers
	 */
	public void resume() {
		live = true;
	}
}
