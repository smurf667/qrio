package de.engehausen.qrio.util;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.TreeMap;

import org.jcodec.api.FrameGrab;
import org.jcodec.api.JCodecException;
import org.jcodec.api.PictureWithMetadata;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.scale.AWTUtil;

/**
 * Iterator for frames of an {@code .mp4} video.
 */
public class VideoIterator implements Iterator<BufferedImage> {

	private static int BUFFER_SIZE = 5;

	protected final FrameGrab grab;
	// frames may not be decoded in order - see https://github.com/jcodec/jcodec/issues/165
	protected final TreeMap<Double, BufferedImage> reorderBuffer = new TreeMap<>();
	protected boolean done;

	/**
	 * Creates the iterator for the given {@code .mp4} file.
	 * @param file the video file to read
	 * @throws IOException in case of error
	 * @throws JCodecException in case of error
	 */
	public VideoIterator(final File file) throws IOException, JCodecException {
		grab = FrameGrab.createFrameGrab(NIOUtils.readableChannel(file));
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean hasNext() {
		if (!done) {
			while (reorderBuffer.size() < BUFFER_SIZE) {
				try {
					final PictureWithMetadata data = grab.getNativeFrameWithMetadata();
					if (data == null) {
						done = true;
						break;
					}
					final Double key = Double.valueOf(data.getTimestamp());
					reorderBuffer.put(key, AWTUtil.toBufferedImage(data.getPicture()));
				} catch (IOException e) {
					done = true;
					break;
				}
			}
		}
		return !reorderBuffer.isEmpty();
	}

	/**
	 * {@inheritDoc}
	 */
	public BufferedImage next() {
		if (reorderBuffer.isEmpty()) {
			throw new NoSuchElementException();
		}
		return reorderBuffer.remove(reorderBuffer.firstKey());
	}

}