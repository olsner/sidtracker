package se.olsner.sidtracker;

public class SIDControl {

	private static final int PER_VOICE_REGS = 7;
	private static final int GATE_OFFSET = 4;
	private SID sid;
	private SoundQueue queue;
	
	public SIDControl(SID sid, SoundQueue queue) {
		super();
		this.sid = sid;
		this.queue = queue;
	}

	public void setGate(int voice, boolean gate) {
		System.err.println("Trying to gate voice "+voice+" to "+gate);
		queue.postControlMesssage(sid.new ChangeRegisterMessage(voice*PER_VOICE_REGS + GATE_OFFSET, 0xfe, gate ? 1 : 0));
	}
	
	

}
