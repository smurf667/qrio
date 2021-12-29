package de.engehausen.qrio.util;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Iterator of decoded bytes of QR codes.
 * This takes screenshots with a given interval, decodes
 * the QR code image and exposes the decoded bytes.
 * Uses an additional thread.
 */
public class ScreenIterator implements Iterator<byte[]> {

	private static final Logger LOGGER = LoggerFactory.getLogger(ScreenIterator.class);
	private static final long WAIT = 1000;

	protected ScheduledFuture<?> timer;

	private final int timeout;
	private final Queue<byte[]> next;
	private LocalDateTime lastUpdate;
	private boolean isStopped;

	/**
	 * Creates the iterator, taking screenshots of the given area
	 * with the given interval, and ending if no new bytes could be read
	 * after the given timeout.
	 * @param interval the interval with which to take screenshots, in milliseconds
	 * @param timeout the number of seconds to wait until no new bytes could be read
	 * @param area the area to take a screenshot of
	 */
	public ScreenIterator(final int interval, final int timeout, final Rectangle area) {
		this(interval, timeout, () -> Screenshot.capture(true, area), new StatefulQrReader(0));
	}

	protected ScreenIterator(final int interval, final int timeout, final Supplier<BufferedImage> shooter, final Function<BufferedImage, byte[]> parser) {
		this.timeout = timeout;

		lastUpdate = LocalDateTime.now();
		next = new LinkedList<>();
		final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);
		timer = executorService.scheduleAtFixedRate(
			() -> {
				if (isStopped || !decode(shooter.get(), parser)) {
					timer.cancel(false);
					timer = null;
					executorService.shutdown();
				}
			},
			5,
			interval,
			TimeUnit.MILLISECONDS
		);
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean hasNext() {
		if (timer != null) {
			synchronized(this) {
				while (timer != null && next.isEmpty()) {
					try {
						wait(WAIT);
					} catch (InterruptedException e) {
						// ignore interruptions
					}
				}
				return !next.isEmpty();
			}
		}
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	public byte[] next() {
		synchronized(this) {
			if (next.isEmpty()) {
				throw new NoSuchElementException();
			}
			return next.poll();
		}
	}

	/**
	 * Stops taking screenshots.
	 */
	public void stop() {
		isStopped = true;
	}

	/**
	 * Updates the iterator's next image, if required.
	 * @param image the new image
	 * @param reader the QR reader
	 * @return {@code false} if the timer should stop, {@code true} otherwise.
	 */
	protected boolean decode(final BufferedImage image, final Function<BufferedImage, byte[]> reader) {
		final byte[] bytes = reader.apply(image);
		if (bytes.length > 0) {
			addNext(bytes);
		} else {
			// check timeout
			final long val = ChronoUnit.SECONDS.between(lastUpdate, LocalDateTime.now());
			if (timeout > 0 && val >= timeout) {
				LOGGER.debug("timeout/end");
				addNext(null);
				stop();
				return false;
			}
		}
		return true;
	}

	protected void addNext(final byte[] bytes) {
		synchronized (this) {
			if (bytes != null) {
				next.add(bytes);
				lastUpdate = LocalDateTime.now();
			} else {
				next.clear();
			}
			notifyAll();
		}
	}
}
