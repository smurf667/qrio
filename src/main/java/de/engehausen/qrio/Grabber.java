package de.engehausen.qrio;

import java.awt.AWTException;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.DisplayMode;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Function;
import java.util.stream.StreamSupport;

import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.WindowConstants;
import javax.swing.event.ChangeListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.engehausen.qrio.util.ScreenIterator;
import de.engehausen.qrio.util.Screenshot;

/**
 * UI-based tool to take consecutive screenshots of a region
 * displaying QR codes, converting these back to bytes in a file.
 */
public class Grabber {

	private static final Logger LOGGER = LoggerFactory.getLogger(Grabber.class);

	private static final Dimension LABEL_DIMENSION = new Dimension(96, 18);
	private static final Dimension VALUE_DIMENSION = new Dimension(48, 18);
	private static final int SCAN_MIN_MS = 100;
	private static final int SCAN_MAX_MS = 2000;
	private static final int TIMEOUT_MIN_S = 0;
	private static final int TIMEOUT_MAX_S = 5;

	private final DisplayMode displayMode;
	private final JFrame frame;
	private final ImagePanel panel;
	private final JPanel preparePanel;
	private final Function<byte[], byte[]> postprocessor;
	private int scanInterval;
	private int scanTimeout;
	private final File output;

	/**
	 * Creates the tool.
	 * @param output the file to output
	 * @param postprocessor a post-processor for the bytes read
	 * @throws AWTException in case of error
	 */
	public Grabber(final File output, final Function<byte[], byte[]> postprocessor) throws AWTException {
		this.output = output;
		this.postprocessor = postprocessor;
		displayMode = GraphicsEnvironment
			.getLocalGraphicsEnvironment()
			.getScreenDevices()[0]
			.getDisplayMode();
		frame = new JFrame();
		frame.setSize(displayMode.getWidth(), displayMode.getHeight());
		frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		panel = new ImagePanel(this);
		panel.setMinimumSize(frame.getSize());
		panel.setPreferredSize(frame.getSize());
		panel.setSize(frame.getWidth(), frame.getHeight());
		frame.getContentPane().add(panel, BorderLayout.CENTER);
		frame.pack();
		preparePanel = createInitialDialog();
	}

	protected void captured() {
		frame.setVisible(false);
		LOGGER.debug("capture region set to {}", panel.getArea());
		final ScreenIterator screenIterator = new ScreenIterator(scanInterval, scanTimeout, panel.getArea());
		new Thread(() -> {
			final Function<byte[], byte[]> beeper = scanTimeout > 0 ?
				Function.identity() :
				(bytes) -> {
					Toolkit.getDefaultToolkit().beep();
					return bytes; 
				};
			try {
				Reader.write(
					output,
					StreamSupport
						.stream(Spliterators.spliterator(
							screenIterator,
							1,
							Spliterator.NONNULL | Spliterator.ORDERED),
							false)
						.map(postprocessor)
						.map(beeper)
				);
				System.exit(0);
			} catch (Throwable t) {
				System.out.printf("%s%n%n", t.getMessage());
				System.exit(1);
			}
		}).start();
		if (scanTimeout == 0) {
			SwingUtilities.invokeLater(() -> {
				JOptionPane.showMessageDialog(frame, "Grabber will stop when you close this dialog.");
				screenIterator.stop();
			});
		}
	}

	private JPanel createInitialDialog() {
		final JPanel result = new JPanel();
		result.setLayout(new BoxLayout(result, BoxLayout.Y_AXIS));
		result.add(slider("Scan interval", "ms", SCAN_MIN_MS, SCAN_MAX_MS, 2 * SCAN_MIN_MS, 20, evt -> {
			final JSlider src = (JSlider) evt.getSource();
			scanInterval = src.getValue();
		}));
		result.add(slider("Scan timeout", "s", TIMEOUT_MIN_S, TIMEOUT_MAX_S, TIMEOUT_MAX_S - 1, 1, evt -> {
			final JSlider src = (JSlider) evt.getSource();
			scanTimeout = src.getValue();
		}));
		result.add(new JLabel("Click OK and select QR code region from the"));
		result.add(new JLabel("screenshot taken after a two second delay."));
		return result;
	}

	private JPanel slider(final String name, final String unit, final int min, final int max, final int initial, final int ticks, final ChangeListener listener) {
		final JPanel result = new JPanel();
		result.setLayout(new BoxLayout(result, BoxLayout.X_AXIS));
		final JLabel title = new JLabel(name);
		title.setMinimumSize(LABEL_DIMENSION);
		title.setPreferredSize(LABEL_DIMENSION);
		result.add(title);
		final JLabel value = new JLabel(Integer.toString(min) + unit);
		value.setMinimumSize(VALUE_DIMENSION);
		value.setPreferredSize(VALUE_DIMENSION);
		final JSlider slider = new JSlider(min, max);
		slider.setOrientation(JSlider.HORIZONTAL);
//		slider.setMajorTickSpacing(ticks);
		slider.setMinorTickSpacing(ticks);
		slider.setSnapToTicks(true);
		slider.addChangeListener(listener);
		slider.addChangeListener(evt -> {
			final JSlider src = (JSlider) evt.getSource();
			value.setText(Integer.toString(src.getValue()) + unit);
		});
		result.add(slider);
		result.add(value);
		slider.setValue(initial);
		return result;
	}

	/**
	 * Starts the tool by showing the grabbing parameters.
	 * Once the parameters are set, the capture region will be
	 * defined by the user, and afterwards grabbing commences.
	 */
	public void show() {
		SwingUtilities.invokeLater(() -> {
			JOptionPane.showMessageDialog(
				frame,
				preparePanel,
				"Set grabbing parameters",
				JOptionPane.INFORMATION_MESSAGE
				);
			final Timer timer = new Timer(2000, event -> {
				setScreenshot(
					Screenshot.capture(
							false,
							new Rectangle(displayMode.getWidth(), displayMode.getHeight())
						)
					);
				frame.setVisible(true);
			});
			timer.setRepeats(false);
			timer.start();
		});
	}

	protected void setScreenshot(final BufferedImage screenshot) {
		panel.setImage(screenshot);
		frame.setSize(screenshot.getWidth(), screenshot.getHeight());
		frame.setTitle("Select region to grab");
	}

	private static class ImagePanel extends JPanel implements MouseListener, MouseMotionListener {

		private static final long serialVersionUID = 1L;
		private static final Cursor SELECTOR = new Cursor(Cursor.CROSSHAIR_CURSOR);

		protected BufferedImage image;
		private final Color highlight;
		private final Point start;
		private final Point end;
		private final Rectangle area;
		private final Grabber grabber;

		public ImagePanel(final Grabber parent) {
			super();
			grabber = parent;
			highlight = new Color(0.8f, 0f, 0f, 0.25f);
			start = new Point();
			end = new Point();
			area = new Rectangle();
			addMouseListener(this);
			addMouseMotionListener(this);
		}
	
		public void setImage(final BufferedImage img) {
			image = img;
			repaint();
		}

		public Rectangle getArea() {
			return area;
		}

		@Override
		public void paint(final Graphics g) {
			final Graphics2D g2d = (Graphics2D) g;
			g2d.setColor(Color.RED);
			g2d.fillRect(0, 0, getWidth(), getHeight());
			g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
			g2d.drawImage(image, 0, 0, image.getWidth(), image.getHeight(), this);
			g2d.setColor(highlight);
			area.x = Math.min(start.x, end.x);
			area.y = Math.min(start.y, end.y);
			area.width = Math.max(start.x, end.x) - area.x;
			area.height = Math.max(start.y, end.y) - area.y;
			g2d.fillRect(area.x, area.y, area.width, area.height);
		}

		@Override
		public void mousePressed(final MouseEvent e) {
			start.setLocation(e.getPoint());
			end.setLocation(e.getPoint());
			repaint();
		}

		@Override
		public void mouseReleased(final MouseEvent e) {
			end.setLocation(e.getPoint());
			grabber.captured();
		}

		@Override
		public void mouseEntered(final MouseEvent e) {
			setCursor(SELECTOR);
		}

		@Override
		public void mouseExited(final MouseEvent e) {
			setCursor(Cursor.getDefaultCursor());
		}

		@Override
		public void mouseClicked(final MouseEvent e) {
		}

		@Override
		public void mouseDragged(final MouseEvent e) {
			end.setLocation(e.getPoint());
			repaint();
		}

		@Override
		public void mouseMoved(final MouseEvent e) {
		}

	}

}
