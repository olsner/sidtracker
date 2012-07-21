package se.olsner.sidtracker;

import android.util.Log;

public class SIDControl {

	private static final int PER_VOICE_REGS = 7;
	private static final int GATE_OFFSET = 4;
	private static final int FREQ_LO = 0;
	private static final int FREQ_HI = 1;
	
	
	private SID sid;
	private SoundQueue queue;
	
	public SIDControl(SID sid, SoundQueue queue) {
		super();
		this.sid = sid;
		this.queue = queue;
	}

	public void setGate(int voice, boolean gate) {
		Log.d("SIDControl", "Trying to gate voice "+voice+" to "+gate);
		queue.postControlMessage(sid.new ChangeRegisterMessage(voice*PER_VOICE_REGS + GATE_OFFSET, 0xfe, gate ? 1 : 0));
	}

	public void setFilterCutoff(int freq) {
		queue.postControlMessage(sid.new SetRegistersMessage(new int[] { 0x15, freq & 0x7, 0x16, freq >> 3 } ));
	}

	public void setFrequencyHz(int voice, int hz) {
		int base = voice*PER_VOICE_REGS;
		int cycles = SID.getFrequencyValueFromHz(hz);
		queue.postControlMessage(sid.new SetRegistersMessage(new int[] { base + FREQ_LO, cycles & 0xff, base + FREQ_HI, cycles >> 8 }));
	}
	
	

}
