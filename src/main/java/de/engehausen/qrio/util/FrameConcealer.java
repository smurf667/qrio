package de.engehausen.qrio.util;

import java.util.Arrays;
import java.util.function.Function;

/**
 * Verifies and removes the first byte of byte arrays of uneven lengths
 * (which indicate they care a frame counter in the first byte).
 * The instance is stateful.
 */
public class FrameConcealer implements Function<byte[], byte[]> {

	protected byte expect;

	/**
	 * Creates the concealer.
	 */
	public FrameConcealer() {
		expect = -1;
	}

	/**
	 * Transforms the input byte array.
	 * @param src the byte array to process
	 * @return the frame-less byte array. If the input has even length, it is
	 * returned as is. If the length is odd, the first byte is taken as the
	 * frame counter. If the counter value is not one more than from the
	 * previous frame, an {@code IllegalStateException} is thrown.
	 */
	public byte[] apply(final byte[] src) {
		if (expect >= 0) {
			if (src[0] != expect) {
				throw new IllegalStateException(String.format("decoding error: input is not the next frame (%s!=%s)", Integer.toHexString(src[0]), Integer.toHexString(expect)));
			}
		}
		if (isActive() || src.length % 2 == 1) {
			expect = (byte) ((src[0] + 1) & 0x7f);
			return Arrays.copyOfRange(src, 1, src.length);
		}
		// this is not enumerated
		return src;
	}

	protected boolean isActive() {
		return expect != -1;
	}

}
