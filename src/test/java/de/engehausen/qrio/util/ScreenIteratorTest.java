package de.engehausen.qrio.util;

import java.awt.image.BufferedImage;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ScreenIteratorTest {

	@Test
	public void testIteration() {
		final Map<BufferedImage, byte[]> map = new HashMap<>();
		final Deque<BufferedImage> queue = new LinkedList<>();
		for (int i = 0; i < 10; i++) {
			final BufferedImage img = new BufferedImage(2, 2, BufferedImage.TYPE_BYTE_BINARY);
			final byte[] bytes = i < 5 ? new byte[4] : new byte[0];
			queue.add(img);
			map.put(img, bytes);
		}
		final ScreenIterator iterator = new ScreenIterator(
			500,
			1,
			() -> queue.removeFirst(),
			(image) -> map.get(image));
		Assertions.assertNotNull(iterator.timer);
		int count = 0;
		while (iterator.hasNext()) {
			final byte[] b = iterator.next();
			Assertions.assertNotNull(b);
			Assertions.assertTrue(b.length > 0);
			count++;
		}
		Assertions.assertEquals(5, count);
		Assertions.assertNull(iterator.timer);
	}

}
