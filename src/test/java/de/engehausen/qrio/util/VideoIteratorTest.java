package de.engehausen.qrio.util;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Paths;

import org.jcodec.api.JCodecException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class VideoIteratorTest {

	private static final String VIDEO_FILE = "/video.mp4";
	private static final int FRAME_COUNT = 232;

	@Test
	public void testReadVideo() throws URISyntaxException, IOException, JCodecException {
		final VideoIterator iterator = new VideoIterator(
			Paths.get(getClass().getResource(VIDEO_FILE).toURI()).toFile()
		);
		int count = 0;
		while (iterator.hasNext()) {
			count++;
			Assertions.assertNotNull(iterator.next());
		}
		Assertions.assertEquals(FRAME_COUNT, count);
	}

}
