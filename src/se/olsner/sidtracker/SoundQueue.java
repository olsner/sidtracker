package se.olsner.sidtracker;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

public class SoundQueue
{
	ArrayBlockingQueue<short[]> queue = new ArrayBlockingQueue<short[]>(2);
	ArrayBlockingQueue<short[]> pool = new ArrayBlockingQueue<short[]>(10);
	ArrayBlockingQueue<Runnable> controlQueue = new ArrayBlockingQueue<Runnable>(1);
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
			return queue.poll(1000, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			return null;
		}
	}

	public void postControlMesssage(Runnable controlMessage) {
		try {
			System.err.println("Offering control message...");
			controlQueue.offer(controlMessage, 1000, TimeUnit.SECONDS);
			System.err.println("Control message offered");
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
				System.out.println("Running control message "+control);
				control.run();
				control = null;
			}
			
		} while (!posted);
	}
}