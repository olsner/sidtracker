package se.olsner.sidtracker;

import java.io.IOException;

public interface Track {
    boolean available();
    void read() throws IOException;

    long nextTime();
    int nextReg();
    int nextXor();
}

