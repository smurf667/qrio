package de.engehausen.qrio.util;

import java.util.function.Function;

/**
 * Adds a frame counter (mod 128) to a given byte array.
 * This is to be used in conjunction with {@link FrameConcealer}.
 * The instance is stateful.
 */
public class FrameEnumerator implements Function<byte[], byte[]> {

	private int frame;

	/**
	 * Transforms the input byte array.
	 * @param src the byte array to process. If the length is odd, an
	 * {@code IllegalStateException} is thrown.
	 * @return the byte array with a frame counter in its first byte. 
	 */
	public byte[] apply(final byte[] src) {
		if (src.length % 2 == 1) {
			throw new IllegalArgumentException("input array must be divisable by two");
		}
		final byte[] result = new byte[1 + src.length];
		System.arraycopy(src, 0, result, 1, src.length);
		result[0] = (byte) ((frame++) & 0x7f);
		return result;
	}

}
