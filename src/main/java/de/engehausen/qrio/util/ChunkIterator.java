package de.engehausen.qrio.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

/**
 * An iterator for byte "chunks" based on a given input stream.
 */
public class ChunkIterator implements Iterator<byte[]> {

	private final byte[] buffer;
	private final InputStream stream;

	/**
	 * Creates the iterator using the given input stream and chunk size.
	 * @param stream the input stream
	 * @param chunkSize the size of the returned chunks (the last chunk
	 * may have less bytes)
	 */
	public ChunkIterator(final InputStream stream, final int chunkSize) {
		buffer = new byte[chunkSize];
		this.stream = stream;
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean hasNext() {
		try {
			return stream.available() > 0;
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public byte[] next() {
		try {
			final int size = stream.read(buffer);
			final byte[] result = new byte[size];
			System.arraycopy(buffer, 0, result, 0, size);
			return result;
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

}