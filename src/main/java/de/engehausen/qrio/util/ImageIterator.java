package de.engehausen.qrio.util;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.NoSuchElementException;

import javax.imageio.ImageIO;

/**
 * Iterate of images. The PNG images are read from the file system
 * using the given prefix. Count starts at one and image names
 * use four digits.
 */
public class ImageIterator implements Iterator<BufferedImage> {

	private final String prefix;
	private File file;
	private int count;

	private static String getFilename(final String prefix, final int count) {
		return String.format("%s%04d.png", prefix, count);
	}

	/**
	 * Creates the iterator using the given prefix for file names.
	 * @param prefix the prefix to use
	 */
	public ImageIterator(final String prefix) {
		this.prefix = prefix;
		count = 1;
		if (!hasNext()) {
			throw new IllegalStateException(String.format("File %s not found", getFilename(prefix, count)));
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean hasNext() {
		file = new File(getFilename(prefix, count));
		return file.exists();
	}

	/**
	 * {@inheritDoc}
	 */
	public BufferedImage next() {
		try {
			try {
				return ImageIO.read(file);
			} catch (IOException e) {
				throw new NoSuchElementException(file.toString());
			}
		} finally {
			count++;
		}
	}

}