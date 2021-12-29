package de.engehausen.qrio.util;

import java.awt.AWTException;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.image.BufferedImage;
import java.awt.image.MultiResolutionImage;
import java.util.List;

/**
 * Utility to take a screenshot of a region.
 */
public class Screenshot {

	private static final Robot ROBOT;

	static {
		try {
			ROBOT = new Robot();
		} catch (AWTException e) {
			throw new IllegalStateException(e);
		}
	}

	/**
	 * Creates a screenshot of the given region.
	 * @param nativeResolution {@code true} to use native resolution, {@code false} to use the (potentially) scaled version
	 * @param area the area to capture
	 * @return the screenshot
	 */
	public static BufferedImage capture(final boolean nativeResolution, final Rectangle area) {
		return nativeResolution ?
			findBufferedImage(ROBOT.createMultiResolutionScreenCapture(area)) :
			ROBOT.createScreenCapture(area);
	}

	private static BufferedImage findBufferedImage(final MultiResolutionImage multiImage) {
		final List<Image> images = multiImage.getResolutionVariants();
		for (int i = images.size(); --i > 0; ) {
			final Image candidate = images.get(i);
			if (candidate instanceof BufferedImage) {
				return (BufferedImage) candidate;
			}
		}
		throw new IllegalStateException("cannot handle screenshot");
	}

}
