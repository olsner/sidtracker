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

public class SIDBackingTrack implements Runnable {

	interface Listener {
		public void onGateChange(boolean gate);
	}

	private SoundQueue queue;
	private SID sid;
	final int user = 1; // bitmask of ignored/user-controlled voices?
	final int userGateReg = (user - 1) * 7 + 4;
	final InputStream track;

	Listener listener;

	int[] cycles = new int[1];
	long nextActionTime;
	int nextActionReg;
	int nextActionXOR;

	// Gate for user channel as determined by song
	boolean correctGate;
	// Gate currently set in sid register
	boolean currentGate;
	// Gate as set by user by touching
	volatile boolean userGate;

	public SIDBackingTrack(SID sid, SoundQueue queue, InputStream track)
	{
		this.queue = queue;
		this.sid = sid;
		this.track = track;
	}

	public void run() {
		next();
		while (queue.isLive())
		{
			int offset = 0;
			short[] buffer = queue.getBufferFromPool();
			if (buffer == null) buffer = new short[441]; // 1/100th of a second... Maybe we should have less though?
			while (offset < buffer.length)
			{
				nextIfNeeded();
				checkGate();
				int requested = cycles[0];
				//Log.i("SIDTracker", "Clocking for "+requested+" cycles");
				int samples = sid.clock(cycles, buffer, offset, buffer.length - offset);
				offset += samples;
				//Log.i("SIDTracker", "Made "+samples+" samples for "+(requested - cycles[0])+" cycles ("+requested+" cycles requested");
				//Log.i("SIDTracker", "Filled "+offset+" of "+buffer.length+" samples");
			}
			postBuffer(buffer);
		}
		
		System.out.println("SID thread done!");
	}

	private static int readByte(InputStream in) {
		try {
			return in.read();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private static int readInt16(InputStream in) {
		return (readByte(in) << 8) | readByte(in);
	}

	private static long readInt(InputStream in) {
		int i = readByte(in);
		switch (i)
		{
		case 0xff: return ((long)readInt16(in) << 16) | readInt16(in);
		case 0xfe: return readInt16(in);
		default: return i;
		}
	}

	private void nextIfNeeded() {
		if (cycles[0] == 0) {
			next();
		}
	}

	private void next() {
		long now = sid.getCycleCount();
		// now doesn't change, and we will eventually read a non-zero delta
		while (nextActionTime <= now)
		{
			/*if (channelForReg(nextActionReg) == user) {
				sid.fakeXor(nextActionReg, nextActionXOR);
			} else {
				sid.xor(nextActionReg, nextActionXOR);
			}*/
			if (nextActionReg == userGateReg) {
				boolean gate = (nextActionXOR & 1) == (correctGate ? 0 : 1);
				if (gate != correctGate)
				{
					correctGate = gate;
					Log.i("SIDTracker", "Correct gate is now "+gate);
					if (listener != null)
						listener.onGateChange(gate);
				}
				nextActionXOR &= 0xfe;
			}
			sid.xor(nextActionReg, nextActionXOR);
			read();
		}
		cycles[0] = (int)(nextActionTime - now);
	}

	private void read() {
		nextActionTime += readInt(track);
		nextActionReg = readByte(track);
		nextActionXOR = readByte(track);
	}

	private void postBuffer(short[] buffer) {
		queue.postRunningControlMessages(buffer);
	}

	private int channelForReg(int reg) {
		if (reg < 21) {
			return 1 + reg / 7;
		} else if (reg >= 0x1b) {
			return 3;
		} else {
			// Global register
			return 0;
		}
	}

	public void userSetGate(boolean gate) {
		userGate = gate;
	}

	private void checkGate() {
		//Log.i("SIDTracker", "checkGate: user "+userGate+" correct "+correctGate+" current "+currentGate);
		// Always ungate at correct time, otherwise gate when both user and
		// the song are gated.
		if (!correctGate || userGate) {
			setGate(correctGate);
		}
		//setGate(userGate);
	}

	public boolean getCorrectGate() {
		return correctGate;
	}

	private void setGate(boolean gate) {
		if (gate == currentGate) return;

		sid.xor(userGateReg, 1);
		currentGate = gate;
	}

	public void setListener(Listener listener) {
		this.listener = listener;
	}
}
