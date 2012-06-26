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
	private SoundQueue queue;
	private SID sid;
	final int user = 3; // bitmask of ignored/user-controlled voices?
	final InputStream track;

	int[] cycles = new int[1];
	long nextActionTime;
	int nextActionReg;
	int nextActionXOR;

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
				int requested = cycles[0];
				int samples = sid.clock(cycles, buffer, offset, buffer.length - offset);
				offset += samples;
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
			if (channelForReg(nextActionReg) == user)
				sid.fakeXor(nextActionReg, nextActionXOR);
			else
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

}
