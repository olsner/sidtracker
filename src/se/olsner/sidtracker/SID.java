package se.olsner.sidtracker;

public class SID {
	private long nativeData;
	private int[] registerValues = new int[32];

	public SID() {
		nativeInit();
	}

	private native void nativeInit();

	public int read(int reg)
	{
		assert reg > 0 && reg < 0x19;
		return registerValues[reg];
	}

	private native void nativeWrite(int reg, int value);
	
	public void write(int reg, int value)
	{
		registerValues[reg] = value;
		nativeWrite(reg, value);
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
	public native int clock(int[] cycles, short[] output, int offset, int length);

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
		return 44100;
	}
}
