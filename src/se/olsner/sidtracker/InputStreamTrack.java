package se.olsner.sidtracker;

import java.io.*;

public class InputStreamTrack implements Track {
    final InputStream track;

    long time;
    int reg;
    int xor;

    InputStreamTrack(InputStream track) {
        this.track = track;
    }

    public boolean available() {
        try {
            return track.available() > 0;
        } catch (IOException e) {
            return false;
        }
    }

    public void read() throws IOException {
        time += readLong();
        reg = readByte();
        xor = readByte();
    }

    public long nextTime() { return time; }
    public int nextReg() { return reg; }
    public int nextXor() { return xor; }

    private int readByte() throws IOException {
        return track.read();
    }

    private int readInt16() throws IOException {
        return (readByte() << 8) | readByte();
    }

    private long readLong() throws IOException {
        int i = readByte();
        switch (i)
        {
        case 0xff: return ((long)readInt16() << 16) | readInt16();
        case 0xfe: return readInt16();
        default: return i;
        }
    }

}

