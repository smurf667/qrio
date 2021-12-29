package de.engehausen.qrio.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class FrameEnumeratorTest {

	@Test
	public void testEnumeration() {
		final byte[] data = new byte[] { 1, 2 };
		final FrameEnumerator enumerator = new FrameEnumerator();
		for (int i = 0; i < 130; i++) {
			final byte[] result = enumerator.apply(data);
			Assertions.assertEquals(data.length + 1, result.length);
			Assertions.assertEquals(i % 128, result[0]);
		}
	}

	@Test
	public void testInvalid() {
		Assertions.assertThrows(
			IllegalArgumentException.class,
			() ->  new FrameEnumerator().apply(new byte[] { 1 })
		);
	}
	
}
