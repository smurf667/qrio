package de.engehausen.qrio;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Paths;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import de.engehausen.qrio.util.StatefulQrReader;

// this test should be removed if the zxing library is improved 
// if zxing is updated and this test fails, then delete the test ;-)
public class UnreadableTest {

	// this is a QR code generated by zxing, scaled with linear
	// interpolation (factor 3) - and it cannot be read by
	// zxing anymore 
	private static final String IMAGE = "/unreadable.png";

	@Test
	public void readUnreadable() throws URISyntaxException, IOException {
		final BufferedImage img = ImageIO.read(Paths.get(getClass().getResource(IMAGE).toURI()).toFile());
		final StatefulQrReader reader = new StatefulQrReader(0);
		Assertions.assertEquals(0, reader.apply(img).length, "'should' have no decoded bytes");
		// this is actually what we would expect
//		Assertions.assertNotEquals(0, reader.apply(img).length, "should have decoded bytes");
	}

}
