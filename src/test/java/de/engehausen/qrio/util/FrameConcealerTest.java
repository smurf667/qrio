package de.engehausen.qrio.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class FrameConcealerTest {

	@Test
	public void testEnumerating() {
		final byte[] enumerated1 = new byte[] { 3, 2, 1 };
		final byte[] enumerated2 = new byte[] { 4, 0, 0 };
		final byte[] enumerated3 = new byte[] { 6, 1, 0 };
		final FrameConcealer concealer = new FrameConcealer();
		final byte[] result1 = concealer.apply(enumerated1);
		Assertions.assertEquals(2, result1.length);
		Assertions.assertEquals(2, result1[0]);
		Assertions.assertEquals(1, result1[1]);
		final byte[] result2 = concealer.apply(enumerated2);
		Assertions.assertEquals(2, result2.length);
		Assertions.assertEquals(0, result2[0]);
		Assertions.assertEquals(0, result2[1]);
		Assertions.assertThrows(IllegalStateException.class, () -> concealer.apply(enumerated3));
	}

	@Test
	public void testPlain() {
		final byte[] plain = new byte[2];
		final FrameConcealer concealer = new FrameConcealer();
		Assertions.assertSame(plain, concealer.apply(plain));
	}

}
