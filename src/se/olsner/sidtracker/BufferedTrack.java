package se.olsner.sidtracker;

import android.util.Log;

import java.io.*;

public class BufferedTrack implements Track {
    final Track track;
    final int lookahead;

    final long[] times;
    final byte[] regXors;
    int pos;

    BufferedTrack(Track track, int lookahead) throws IOException {
        this.track = track;
        this.lookahead = lookahead;
        // We keep a 2*lookahead buffer, to reduce the number of fills
        this.times = new long[2 * lookahead];
        this.regXors = new byte[4 * lookahead];
        readOne();
    }

    public boolean available() {
        return pos > 0 || track.available();
    }

    public void read() throws IOException {
        if (pos > 0)
        {
            pos--;
            System.arraycopy(times, 1, times, 0, pos);
            System.arraycopy(regXors, 2, regXors, 0, 2 * pos);
        }
        if (pos < lookahead)
        {
            fill();
        }
    }

    private void readOne() throws IOException {
        track.read();
        times[pos] = track.nextTime();
        regXors[2 * pos] = (byte)track.nextReg();
        regXors[2 * pos + 1] = (byte)track.nextXor();
        pos++;
    }

    private void fill() throws IOException {
        while (pos < times.length && track.available()) {
            readOne();
        }
    }

    public long nextTime() { return time(0); }
    public int nextReg() { return reg(0); }
    public int nextXor() { return xor(0); }

    public long time(int i) {
        assert i < pos;
        return times[i];
    }

    public int reg(int i) {
        assert i < pos;
        return regXors[2 * i] & 0xff;
    }

    public int xor(int i) {
        assert i < pos;
        return regXors[2 * i + 1] & 0xff;
    }

    public int getFutureItems(long end) {
        int count = 0;
        while (count < pos && times[count] < end) count++;
        return count;
    }
}

