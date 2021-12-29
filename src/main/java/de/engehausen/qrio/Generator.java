package de.engehausen.qrio;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Base64;
import java.util.Map;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.imageio.ImageIO;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.MissingOptionException;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.NotFoundException;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import de.engehausen.qrio.util.ChunkIterator;
import de.engehausen.qrio.util.FrameEnumerator;
import de.engehausen.qrio.util.OptionsHelper;

/**
 * Generator for a sequence of QR codes which represent a file.
 * See {@code README.md} for usage.
 */
public class Generator {

	// non-base 64 character to use for padding
	protected static final char PAD_CHARACTER = '!';
	private static final String PAD_SYMBOL = Character.toString(PAD_CHARACTER);
	private static final String PADDING_MAX = PAD_SYMBOL.repeat(4);

	private static final Logger LOGGER = LoggerFactory.getLogger(Generator.class);

	private static final String IMAGE_FORMAT = "png";
	private static final String COMMAND_NAME = "generate";
	private static final Options OPTIONS;
	private static final String OPT_INPUT = "input";
	private static final String OPT_INPUT_DESC = "input file to encode (mandatory)";
	private static final String OPT_DIMENSION = "dimension";
	private static final String OPT_DIMENSION_DESC = "QR dimension";
	private static final String OPT_DIMENSION_DEFAULT = "128";
	private static final String OPT_BYTES = "bytes";
	private static final String OPT_BYTES_DESC = "bytes per QR code (a multiple of two)";
	private static final String OPT_BYTES_DEFAULT = "384";
	private static final String OPT_SHOW = "show";
	private static final String OPT_SHOW_DESC = "show QR codes";
	private static final String OPT_WAIT = "wait";
	private static final String OPT_WAIT_DESC = "wait in ms between frames in playback mode";
	private static final String OPT_WAIT_DEFAULT = "500";
	private static final String OPT_PREFIX = "prefix";
	private static final String OPT_PREFIX_DESC = "prefix of QR code images";
	private static final String OPT_QUIRKS_MODE = "quirks-mode";
	private static final String OPT_QUIRKS_MODE_DESC = "zxing sometimes cannot decode QR codes it produced itself. This tries to compensate (recommended).";
	private static final String OPT_ENUMERATE_FRAMES = "enumerate";
	private static final String OPT_ENUMERATE_FRAMES_DESC = "stores a frame counter with the data; can be used to fail fast on decoding";
	private static final String OPT_VERBOSE = "verbose";
	private static final String OPT_VERBOSE_DESC = "turn on debug information";

	private static final Map<EncodeHintType, Object> QUALITY = Map.of(
		EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H,
		EncodeHintType.MARGIN, 0
		);
	private static boolean QUIRKS_MODE;
	private static boolean ENUMERATE;

	static {
		final Options options = new Options();
		options.addOption(OptionsHelper
			.optionWithArg(OPT_INPUT, OPT_INPUT_DESC)
			.required()
			.build()
		);
		options.addOption(OptionsHelper
			.option(OPT_SHOW, OPT_SHOW_DESC)
			.build()
		);
		options.addOption(OptionsHelper
			.option(OPT_QUIRKS_MODE, OPT_QUIRKS_MODE_DESC)
			.build()
		);
		options.addOption(OptionsHelper
			.optionWithArg(OPT_WAIT, toDefault(OPT_WAIT_DESC, OPT_WAIT_DEFAULT))
			.build()
		);
		options.addOption(OptionsHelper
			.optionWithArg(OPT_PREFIX, OPT_PREFIX_DESC)
			.build()
		);
		options.addOption(OptionsHelper
			.optionWithArg(OPT_DIMENSION, toDefault(OPT_DIMENSION_DESC, OPT_DIMENSION_DEFAULT))
			.build()
		);
		options.addOption(OptionsHelper
			.optionWithArg(OPT_BYTES, toDefault(OPT_BYTES_DESC, OPT_BYTES_DEFAULT))
			.build()
		);
		options.addOption(OptionsHelper
			.option(OPT_ENUMERATE_FRAMES, OPT_ENUMERATE_FRAMES_DESC)
			.build()
		);
		options.addOption(OptionsHelper
			.option(OPT_VERBOSE, OPT_VERBOSE_DESC)
			.build()
		);
		OPTIONS = options;
	}

	private static String toDefault(final String text, final String value) {
		return String.format("%s (default: %s)", text, value);
	}

	// naive use of zxing QR code generator to encode the given bytes
	private static BufferedImage create(final QRCodeWriter encoder, final byte[] input, final int dimension) {
		return create(encoder, Base64.getEncoder().encodeToString(input), dimension);
	}
	private static BufferedImage create(final QRCodeWriter encoder, final String pseudoBase64, final int dimension) {
		try {
			return MatrixToImageWriter
				.toBufferedImage(
					encoder.encode(
						pseudoBase64,
						BarcodeFormat.QR_CODE,
						dimension,
						dimension,
						QUALITY)
					);
		} catch (WriterException e) {
			throw new IllegalStateException(e);
		}
	}
	// generates a more "reliable" QR code for the given bytes
	// as zxing seems to sometimes not be able to decode a QR
	// code it generated itself, this performs a sanity check
	// if the check fails, an attempt is made to slightly alter
	// the bytes with "padding" content to produce a decodable
	// QR code
	private static BufferedImage createQuirksMode(final QRCodeWriter encoder, final byte[] input, final int dimension) {
		BufferedImage img;
		String str = Base64.getEncoder().encodeToString(input);
		boolean ok = false;
		do {
			img = create(encoder, str, dimension);
			try {
				final BufferedImage scaledAndColor = new BufferedImage(3 * img.getWidth(), 3 * img.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
				final Graphics2D g2d = (Graphics2D) scaledAndColor.getGraphics();
				try {
					g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
					g2d.drawImage(img, 0, 0, 3 * img.getWidth(), 3 * img.getHeight(), null);
				} finally {
					g2d.dispose();
				}
				final byte[] sanity = Reader.readQR(scaledAndColor);
				ok = Arrays.equals(sanity, input);
				if (!ok) {
					// "this should not happen", zxing reads incorrect data from
					// of a scaled version of a code it created itself
					str = pad(str);
				}
			} catch (NotFoundException e) {
				// "this should not happen", zxing cannot read a scaled version
				// of a code it created itself
				str = pad(str);
			}
		} while (!ok);
		return img;
	}

	protected static Stream<BufferedImage> create(final InputStream input, final int chunkSize, final int dimension) {
		final QRCodeWriter encoder = new QRCodeWriter();
		final Function<byte[], byte[]> augmenter = ENUMERATE ? new FrameEnumerator() : Function.identity();
		return StreamSupport
			.stream(
				Spliterators.spliterator(
					new ChunkIterator(input, chunkSize),
					1,
					Spliterator.NONNULL | Spliterator.ORDERED),
					false
			)
			.map(augmenter)
			.map(bytes -> Generator.QUIRKS_MODE ? createQuirksMode(encoder, bytes, dimension) : create(encoder, bytes, dimension));
	}

	private static String pad(final String str) {
		final String result = PAD_SYMBOL + str;
		LOGGER.trace("need to pad: {}", str);
		if (result.startsWith(PADDING_MAX)) {
			throw new IllegalStateException("cannot produce readable QR code (zxing issue)");
		}
		return result;
	}

	private static void printHelp() {
		new HelpFormatter().printHelp(COMMAND_NAME, OPTIONS);
	}

	/**
	 * Main entry point. See {@code README.md} for usage.
	 * @param args input arguments
	 * @throws ParseException in case of error
	 * @throws FileNotFoundException in case of error
	 */
	public static void main(final String... args) throws ParseException, FileNotFoundException {
		if (args.length == 0) {
			printHelp();
			return;
		}

		try {
			final CommandLineParser clip = new DefaultParser();
			final CommandLine cli = clip.parse(OPTIONS, args, true);
			OptionsHelper.verbose(cli.hasOption(OPT_VERBOSE));
			final int bytes = Integer.parseInt(cli.getOptionValue(OPT_BYTES, OPT_BYTES_DEFAULT));
			if (bytes % 2 == 1) {
				System.out.println("byte size must be a multiple of two");
				System.exit(1);
			}
			final int dimension = Integer.parseInt(cli.getOptionValue(OPT_DIMENSION, OPT_DIMENSION_DEFAULT));
			final int delay = Integer.parseInt(cli.getOptionValue(OPT_WAIT, OPT_WAIT_DEFAULT));
			final String filename = cli.getOptionValue(OPT_INPUT);
			final File input = new File(filename);
			if (!input.exists()) {
				System.out.printf("Error: %s does not exist", filename);
				System.exit(1);
			}
			QUIRKS_MODE = cli.hasOption(OPT_QUIRKS_MODE);
			ENUMERATE= cli.hasOption(OPT_ENUMERATE_FRAMES);
			final Stream<BufferedImage> imageStream = create(new FileInputStream(input), bytes, dimension);
			if (cli.hasOption(OPT_SHOW)) {
				new Viewer(dimension, imageStream, delay).show();
			} else {
				final String prefix = cli.getOptionValue(OPT_PREFIX);
				if (prefix == null) {
					System.out.println("Prefix required if not showing directly\n");
					printHelp();
					System.exit(1);
				}
				final AtomicInteger count = new AtomicInteger();
				imageStream
					.forEach(img -> {
						try {
							ImageIO.write(img, IMAGE_FORMAT, new File(String.format("%s%04d.png", prefix, count.incrementAndGet())));
						} catch (IOException e) {
							throw new IllegalStateException(e);
						}
					});
				LOGGER.debug(String.format("wrote %s%04d.png ... %s%04d.png", prefix, 0, prefix, count.get()));
			}
		} catch (MissingOptionException e) {
			System.out.printf("%s%n%n", e.getMessage());
			printHelp();
			System.exit(1);
		}

	}

}