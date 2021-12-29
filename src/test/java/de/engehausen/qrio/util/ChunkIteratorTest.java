package de.engehausen.qrio.util;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Iterator;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ChunkIteratorTest {

	private static final String EXAMPLE = "hello world";

	@Test
	public void testSingleBytes() {
		final int chunkSize = 1;
		final ChunkIterator iterator = new ChunkIterator(createDemoStream(), chunkSize);
		int chunks = 0;
		while (iterator.hasNext()) {
			final byte[] b = iterator.next();
			Assertions.assertEquals(chunkSize, b.length);
			Assertions.assertEquals(EXAMPLE.charAt(chunks), b[0]);
			chunks++;
		}
	}

	@Test
	public void testMultiBytes() {
		final ChunkIterator iterator = new ChunkIterator(createDemoStream(), 4);
		final Iterator<String> expected = Arrays.asList(
			"hell",
			"o wo",
			"rld"
		).iterator();
		while (iterator.hasNext()) {
			Assertions.assertEquals(expected.next(), new String(iterator.next()));
		}
	}

	private InputStream createDemoStream() {
		return new ByteArrayInputStream(EXAMPLE.getBytes(StandardCharsets.UTF_8));
	}

}