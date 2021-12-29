package de.engehausen.qrio.util;

import java.awt.image.BufferedImage;

import com.google.zxing.NotFoundException;

/**
 * Transformer for buffered images to decoded byte arrays.
 */
@FunctionalInterface
public interface QrCodeReader {

	/**
	 * Reads the QR code of the given image and returns the decoded bytes.
	 * @param img the image to decode
	 * @return the decoded bytes
	 * @throws NotFoundException in case of error
	 */
	byte[] read(BufferedImage img) throws NotFoundException;

}
