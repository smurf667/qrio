package de.engehausen.qrio.util;

import java.util.Deque;
import java.util.LinkedList;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.google.zxing.NotFoundException;

public class StatefulQrReaderTest {

	@Test
	public void testNoNew() {
		final byte[] bytes = new byte[] { 1, 2, 3 };
		final StatefulQrReader reader = new StatefulQrReader(0, (img) -> bytes);
		for (int i = 0; i < 4; i++) {
			final byte[] actual = reader.apply(null);
			if (i == 0) {
				Assertions.assertEquals(bytes, actual);
			} else {
				Assertions.assertNotNull(actual);
				Assertions.assertEquals(0, actual.length);
			}
		}
	}

	@Test
	public void testMemory() {
		final Deque<byte[]> data = new LinkedList<>();
		for (int i = 0; i < 4; i++) {
			final byte[] bytes = new byte[] { 1, 2, 3 };
			bytes[0] = (byte) (i % 3);
			data.add(bytes);
		}
		final StatefulQrReader reader = new StatefulQrReader(data.size(), (img) -> data.removeLast());
		for (int i = 0; i < 4; i++) {
			final byte[] actual = reader.apply(null);
			Assertions.assertEquals(i == 3 ? 0 : 3, actual.length);
		}
	}

	@Test
	public void testNotFound() {
		final StatefulQrReader reader = new StatefulQrReader(0, (img) -> { throw NotFoundException.getNotFoundInstance(); });
		final byte[] actual = reader.apply(null);
		Assertions.assertNotNull(actual);
		Assertions.assertEquals(0, actual.length);
	}
	

}