package de.engehausen.qrio.util;

import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Deque;
import java.util.LinkedList;
import java.util.function.Function;
import java.util.zip.CRC32;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.zxing.NotFoundException;

import de.engehausen.qrio.Reader;

/**
 * Decoder for bytes of a QR code image.
 * This will return the bytes for the same consecutive input only once.
 */
public class StatefulQrReader implements Function<BufferedImage, byte[]> {

	private static final Logger LOGGER = LoggerFactory.getLogger(StatefulQrReader.class);

	private static final int MAX_MEMORY = 5;
	private static final byte[] EMPTY = new byte[0];

	protected byte[] last;
	private final Deque<Long> memory;
	private final QrCodeReader reader;
	private final CRC32 crc;
	private final int max;

	/**
	 * Creates the reader with a "memory" for recently
	 * returned past bytes and suppresses those (i.e.,
	 * will return only unique byte arrays for a "recent"
	 * images.
	 */
	public StatefulQrReader() {
		this(MAX_MEMORY, (img) -> Reader.readQR(img));
	}

	/**
	 * Creates the reader with a "memory" for recently
	 * returned past bytes and suppresses those (i.e.,
	 * will return only unique byte arrays for a "recent"
	 * images.
	 * @param max the size of the "memory" for recent images
	 */
	public StatefulQrReader(final int max) {
		this(max, (img) -> Reader.readQR(img));
	}

	protected StatefulQrReader(final int max, final QrCodeReader reader) {
		// keep the last MAX_MEMORY checksums of chunks in
		// memory and reject "recently seen" chunks - this is
		// a hack, because for reasons unknown the video
		// may contain frames with previous QR codes (pure observation
		// on test data)
		this.max = max;
		memory = new LinkedList<>();
		crc = new CRC32();
		this.reader = reader;
	}

	/**
	 * Converts the QR code image into the bytes encoded in it.
	 * @param img the image to decode
	 * @return the decoded bytes; may be empty, but never {@code null}
	 */
	public byte[] apply(final BufferedImage img) {
		try {
			final byte[] next = reader.read(img);
			if (!Arrays.equals(next, last)) {
				if (max > 0) {
					crc.reset();
					crc.update(next);
					final Long checksum = Long.valueOf(crc.getValue());
					if (memory.contains(checksum)) {
						LOGGER.debug("payload seen before, ignoring ({})", checksum);
						return EMPTY;
					} else {
						memory.addFirst(checksum);
						if (memory.size() > max) {
							memory.removeLast();
						}
					}
				}
				try {
					LOGGER.debug("emitting new data");
					return next;
				} finally {
					last = next;
				}
			}
			LOGGER.debug("previous data seen, returning empty");
			return EMPTY;
		} catch (NotFoundException e) {
			LOGGER.debug("QR code not found - rejecting image");
			return EMPTY;
		}
	}

}