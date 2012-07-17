// vim:noet:
package se.olsner.sidtracker;

public class SID {
	private static final int CYCLES_PER_SECOND = 985248;
	private static final int SAMPLE_RATE = 44100;
	
	public static final int MAX_FILTER_CUTOFF = 2047;
	public static final int MAX_FREQUENCY = 3848;

	public class SetRegistersMessage implements Runnable {
		int[] values;
		
		public SetRegistersMessage(int[] values) {
			this.values = values;
		}

		public void run() {
			for (int i = 0; i < values.length; i += 2) {
				write(values[i], values[i+1]);
			}
		}
	}

	public class ChangeRegisterMessage implements Runnable {
		private int reg, and, xor;
		
		public ChangeRegisterMessage(int reg, int and, int xor) {
			this.reg = reg;
			this.and = and;
			this.xor = xor;
		}

		public void run() {
			write(reg, (read(reg) & and) ^ xor);
		}
		
		@Override
		public String toString() {
			return "SID.ChangeRegisterMessage("+reg+" &= "+and+" ^= "+xor+")";
		}
	}

	static {
		System.loadLibrary("sidtracker");
	}

	private long nativeData;
	private byte[] registerValues = new byte[32];
	private long cycleCounter;

	public SID() {
		nativeInit(CYCLES_PER_SECOND, SAMPLE_RATE);
	}

	private native void nativeInit(int cyclesPerSecond, int sampleRate);

	public int read(int reg)
	{
		assert reg >= 0 && reg < 0x1d;
		return registerValues[reg] & 0xff;
	}

	private native void nativeWrite(int reg, int value);
	
	public void write(int reg, int value)
	{
		registerValues[reg] = (byte)value;
		nativeWrite(reg, value);
	}

	public void xor(int reg, int value)
	{
		write(reg, read(reg) ^ value);
	}

	/**
	 * Clocks the SID, writing output samples into the given output array
	 * (mono).
	 * 
	 * @param cycles
	 *            The maximum number of cycles to clock, given as an array with
	 *            one element. The array will be updated with the number of
	 *            cycles left to clock - this may not be 0 if the output array
	 *            was too small.
	 * @param output
	 *            an array of shorts to write the samples into
	 * @param offset
	 *            the offset in the output array to write the output samples
	 * @param length
	 *            the maximum number of output samples to write
	 * @return The number of samples successfully written to output
	 */
	public int clock(int[] cycles, short[] output, int offset, int length)
	{
		int requested = cycles[0];
		int res = nativeClock(cycles, output, offset, length);
		cycleCounter += requested - cycles[0];
		return res;
	}

	private native int nativeClock(int[] cycles, short[] output, int offset, int length);

	private static final int CLOCK_CHUNK = 100000;

	/**
	 * Convenience wrapper around {@link #clock(int[], short[], int, int)} for
	 * cases where the number of cycles clocked isn't interesting but only the
	 * output samples are.
	 * 
	 * @param output
	 *            The array to write samples to
	 * @param offset
	 *            the offset in the array to start writing
	 * @param length
	 *            the number of samples to write
	 * @return The number of cycles clocked
	 */
	public int clockFully(short[] output, int offset, int length) {
		int[] temp = new int[1];
		int totalClocked = 0;
		while (length > 0) {
			temp[0] = CLOCK_CHUNK; // Arbitrary number of clocks
			int written = clock(temp, output, offset, length);
			length -= written;
			offset += written;
			totalClocked += CLOCK_CHUNK - temp[0];
		}
		return totalClocked;
	}

	public int clockFully(short[] output) {
		return clockFully(output, 0, output.length);
	}

	public int getSampleRate() {
		return SAMPLE_RATE;
	}

	public int getCyclesPerSecond() {
		return CYCLES_PER_SECOND;
	}

	/**
	 * Get number of cycles clocked from initialization of the SID.
	 */
	public long getCycleCount() {
		return cycleCounter;
	}

	int getFrequencyValueFromHz(int hz) {
		return (int)((long)hz * (18<<24) / 17734475);
	}
}
