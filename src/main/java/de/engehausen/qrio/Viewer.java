package de.engehausen.qrio;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.util.Iterator;
import java.util.stream.Stream;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.WindowConstants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A UI player for a series of QR codes.
 * The images can be "flipped through" once using the "cursor right"
 * key or played once by pressing the "p" key ("play").
 */
public class Viewer {

	private static final Logger LOGGER = LoggerFactory.getLogger(Viewer.class);

	private final JFrame frame;
	private final ImagePanel panel;
	private final Iterator<BufferedImage> images;
	private final int autoModeDelay;
	private int index;
	private Timer timer;

	/**
	 * Creates the viewer for the given display size, image stream and frame delay.
	 * @param size the display size
	 * @param stream the stream of QR code images
	 * @param delay the delay in milliseconds between frames.
	 */
	public Viewer(final int size, final Stream<BufferedImage> stream, final int delay) {
		images = stream.iterator();
		if (!images.hasNext()) {
			throw new IllegalStateException("no images to display");
		}
		autoModeDelay = delay;
		index = 1;
		frame = new JFrame();
		frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		panel = new ImagePanel(size);
		panel.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY, 4));
		panel.setOpaque(false);
		frame.getContentPane().add(panel, BorderLayout.CENTER);
		frame.pack();
		frame.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(final KeyEvent e) {
				switch (e.getKeyCode()) {
				case KeyEvent.VK_RIGHT:
					next();
					break;
				case KeyEvent.VK_P:
					initTimer();
					break;
				default:
					break;
				}
			}
		});
		next();
	}

	private void initTimer() {
		if (timer == null) {
			timer = new Timer(autoModeDelay, evt -> {
				if (!next()) {
					timer.stop();
				}
			});
			timer.start();
		}
	}

	private boolean next() {
		if (images.hasNext()) {
			LOGGER.debug("showing frame {}", Integer.toString(index));
			final BufferedImage img = images.next();
			frame.setTitle(String.format("Frame %04d", index));
			panel.setImage(img);
			index++;
		}
		return images.hasNext();
	}

	/**
	 * Shows the tool's UI.
	 */
	public void show() {
		SwingUtilities.invokeLater(() -> {
			frame.setVisible(true);
			JOptionPane.showMessageDialog(frame, "Press 'p' to start video, use cursor right to flip through.");
		});
	}

	private static class ImagePanel extends JPanel {

		private static final long serialVersionUID = 1L;

		protected BufferedImage image;
		private final Dimension dimension;

		public ImagePanel(final int size) {
			dimension = new Dimension(4 + 3 * size, 4 + 3 * size);
		}

		public void setImage(final BufferedImage img) {
			image = img;
			repaint();
		}

		@Override
		public void paint(final Graphics g) {
			final int w = getWidth();
			final int h = getHeight();
			final Graphics2D g2d = (Graphics2D) g;
			g2d.setColor(Color.BLACK);
			g2d.fillRect(0, 0, w, h);
			if (image != null) {
				final int s = Math.min(w, h);
				g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
				g2d.drawImage(image, 0, 0, s, s, this);
			}
			super.paint(g);
		}

		@Override
		public Dimension getPreferredSize() {
			return dimension;
		}

		@Override
		public Dimension getMinimumSize() {
			return dimension;
		}

	}

}
