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
	final Track track;

	Listener listener;

	int[] cycles = new int[1];

	// Gate for user channel as determined by song
	boolean correctGate;
	// Gate currently set in sid register
	boolean currentGate;
	// Gate as set by user by touching
	volatile boolean userGate;

	public SIDBackingTrack(SID sid, SoundQueue queue, Track track)
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

		Log.d("SIDBackingTrack", "SID thread done!");
	}

	private void nextIfNeeded() {
		if (cycles[0] == 0) {
			next();
		}
	}

	private void next() {
		long now = sid.getCycleCount();

		// now doesn't change, and we will eventually read a non-zero delta
		while (track.nextTime() <= now)
		{
			int reg = track.nextReg();
			int xor = track.nextXor();
			if (reg == userGateReg) {
				boolean gate = (xor & 1) == (correctGate ? 0 : 1);
				if (gate != correctGate)
				{
					correctGate = gate;
					Log.d("SIDTracker", "Correct gate is now "+gate);
					if (listener != null)
						listener.onGateChange(gate);
				}
				xor &= 0xfe;
			}
			sid.xor(reg, xor);

			// FIXME Handle reaching the end of the track:
			// 1. Check track.available, choose a suitable number of cycles
			// to keep clocking (1/100th of a second?)
			// 2. Signal the listener that we've reached the end of the song.
			read();
		}
		cycles[0] = (int)(track.nextTime() - now);
	}

	private void read() {
		try {
			track.read();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private void postBuffer(short[] buffer) {
		queue.postRunningControlMessages(buffer);
	}

	private int channelForReg(int reg) {
		return SID.channelForReg(reg);
	}

	public void userSetGate(boolean gate) {
		userGate = gate;
	}

	public boolean getCorrectGate() {
		return correctGate;
	}

	public long getCycleCount() {
		return sid.getCycleCount();
	}

	public int getUserChannel() {
		return user;
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

	private void setGate(boolean gate) {
		if (gate == currentGate) return;

		sid.xor(userGateReg, 1);
		currentGate = gate;
	}

	public void setListener(Listener listener) {
		this.listener = listener;
	}
}
