package de.engehausen.qrio;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;

import org.jcodec.api.JCodecException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.google.zxing.NotFoundException;

import de.engehausen.qrio.util.ChunkIterator;

public class IntegrationTest {

	private static final String VIDEO_CAPTURE = "/video.mp4";
	private static final String DEMO_PDF = "/demo.pdf";

	@Test
	public void produceConsumeTest() throws IOException, URISyntaxException {
		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		Generator.create(
			getClass().getResourceAsStream(DEMO_PDF),
			256,
			128
		).map(img -> {
			try {
				return Reader.readQR(img);
			} catch (NotFoundException e) {
				throw new IllegalStateException(e);
			}
		}).forEach(bytes -> {
			try {
				out.write(bytes);
			} catch (IOException e) {
				throw new IllegalStateException(e);
			}
		});
		Assertions.assertArrayEquals(
			Files.readAllBytes(
				Paths.get(getClass().getResource(DEMO_PDF).toURI())
			),
			out.toByteArray()
		);
	}

	@Test
	public void testVideoChunks() throws IOException, URISyntaxException, JCodecException {
		// the bytes of the example file, as expected to be read
		final Iterator<byte[]> chunks = new ChunkIterator(getClass().getResourceAsStream(DEMO_PDF), 768);
		final AtomicInteger count = new AtomicInteger();
		Reader.readVideo(
			Paths.get(getClass().getResource(VIDEO_CAPTURE).toURI()).toFile()
		).forEach(bytes -> {
			count.incrementAndGet();
			final byte[] expected = chunks.next();
			Assertions.assertArrayEquals(expected, bytes, String.format("chunk %d error", count.get()));
		});
	}

}
