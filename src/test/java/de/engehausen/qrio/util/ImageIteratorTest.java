package de.engehausen.qrio.util;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ImageIteratorTest {

	private static final String IMAGE_FILE = "/image0001.png";

	@Test
	public void testIteration() throws URISyntaxException {
		final Path path = Paths.get(getClass().getResource(IMAGE_FILE).toURI());
		final ImageIterator iterator = new ImageIterator(path.getParent().resolve("image").toString());
		Assertions.assertTrue(iterator.hasNext());
		Assertions.assertNotNull(iterator.next());
		Assertions.assertTrue(iterator.hasNext());
		Assertions.assertNotNull(iterator.next());
		Assertions.assertFalse(iterator.hasNext());
		Assertions.assertThrows(NoSuchElementException.class, () -> iterator.next());
	}

	@Test
	public void testBad() {
		Assertions.assertThrows(IllegalStateException.class, () -> new ImageIterator("does-not-exist"));
	}

}