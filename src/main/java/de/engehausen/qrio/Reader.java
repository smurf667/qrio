package de.engehausen.qrio;

import java.awt.AWTException;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Collections;
import java.util.Map;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.MissingOptionException;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.jcodec.api.JCodecException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;

import de.engehausen.qrio.util.FrameConcealer;
import de.engehausen.qrio.util.ImageIterator;
import de.engehausen.qrio.util.OptionsHelper;
import de.engehausen.qrio.util.StatefulQrReader;
import de.engehausen.qrio.util.VideoIterator;

/**
 * Reader for sequences of QR codes which represent a file.
 * The tool supports grabbing QR codes from the screen, reading
 * individual frames from the file system or from an {@code .mp4}
 * video. See {@code README.md} for usage.
 */
public class Reader {

	private static final Logger LOGGER = LoggerFactory.getLogger(Reader.class);

	private static final String COMMAND_NAME = "read";

	private static final Options OPTIONS;
	private static final String OPT_OUTPUT = "output";
	private static final String OPT_OUTPUT_DESC = "decoded file to write (mandatory)";
	private static final String OPT_FILE = "file";
	private static final String OPT_FILE_DESC = "read .mp4 QR code video";
	private static final String OPT_GRAB = "grab";
	private static final String OPT_GRAB_DESC = "use screenshots to grab QR codes";
	private static final String OPT_PREFIX = "prefix";
	private static final String OPT_PREFIX_DESC = "prefix of QR code images to read from file system";
	private static final String OPT_VERBOSE = "verbose";
	private static final String OPT_VERBOSE_DESC = "turn on debug information";

	private static final MultiFormatReader READER;

	static {
		final Options options = new Options();
		options.addOption(OptionsHelper
			.optionWithArg(OPT_OUTPUT, OPT_OUTPUT_DESC)
			.required()
			.build()
		);
		options.addOption(OptionsHelper
			.option(OPT_GRAB, OPT_GRAB_DESC)
			.build()
		);
		options.addOption(OptionsHelper
			.optionWithArg(OPT_FILE, OPT_FILE_DESC)
			.build()
		);
		options.addOption(OptionsHelper
			.optionWithArg(OPT_PREFIX, OPT_PREFIX_DESC)
			.build()
		);
		options.addOption(OptionsHelper
			.option(OPT_VERBOSE, OPT_VERBOSE_DESC)
			.build()
		);
		OPTIONS = options;
		final MultiFormatReader reader = new MultiFormatReader();
		reader.setHints(Map.of(
			DecodeHintType.TRY_HARDER, Boolean.TRUE,
			DecodeHintType.POSSIBLE_FORMATS, Collections.singleton(BarcodeFormat.QR_CODE)
			)
		);
		READER = reader;
	}

	/**
	 * Reads the bytes of an image containing a QR code.
	 * @param image the QR code image to process
	 * @return the decoded bytes (may be empty, but never {@code null})
	 * @throws NotFoundException in case of error
	 */
	public static byte[] readQR(final BufferedImage image) throws NotFoundException {
		final Result result = READER
			.decode(
				new BinaryBitmap(
					new HybridBinarizer(
						new BufferedImageLuminanceSource(image)
					)
				)
			);
		if (result.getNumBits() == 0) {
			return new byte[0]; // funny decoding issues where there are no result bits
		}
		// if this was padded, remove padding characters
		// this does not use lastIndexOf, because if
		// padding characters are present, they WILL be
		// at the beginning
		final String str = result.getText();
		int idx = 0;
		while (idx < str.length() && str.charAt(idx) == Generator.PAD_CHARACTER) {
			idx++;
		}
		if (idx > 0) {
			return Base64.getDecoder().decode(str.substring(idx));
		}
		return Base64.getDecoder().decode(str);
	}

	protected static void write(final File output, final Stream<byte[]> stream) throws IOException {
		try (final FileOutputStream out = new FileOutputStream(output)) {
			stream
				.forEach(chunk -> {
				try {
					out.write(chunk);
				} catch (IOException e) {
					throw new IllegalStateException(e);
				}
			});
		}
		LOGGER.debug("wrote {}", output);
	}

	protected static Stream<byte[]> readVideo(final File input) throws IOException, JCodecException {
		final FrameConcealer concealer = new FrameConcealer();
		final StatefulQrReader qrReader = new StatefulQrReader();
		return StreamSupport
			.stream(
				Spliterators.spliterator(
					new VideoIterator(input),
					1,
					Spliterator.NONNULL | Spliterator.IMMUTABLE | Spliterator.NONNULL),
					false
			)
			.map(qrReader::apply)
			.filter(b -> b.length > 0)
			.map(concealer);
	}

	protected static Stream<byte[]> readFiles(final String prefix) throws IOException, JCodecException {
		final Function<byte[], byte[]> postProcess = new FrameConcealer();
		return StreamSupport
			.stream(
				Spliterators.spliterator(
					new ImageIterator(prefix),
					1,
					Spliterator.NONNULL | Spliterator.ORDERED),
					false
			)
			.map(img -> {
				try {
					return readQR(img);
				} catch (NotFoundException e) {
					throw new IllegalStateException(e);
				}
			})
			.map(postProcess);
	}

	private static void printHelp() {
		new HelpFormatter().printHelp(COMMAND_NAME, OPTIONS);
	}

	/**
	 * Main entry point. See {@code README.md} for usage.
	 * @param args input arguments
	 * @throws ParseException in case of error
	 * @throws AWTException in case of error
	 * @throws JCodecException in case of error
	 * @throws IOException in case of error
	 */
	public static void main(String[] args) throws ParseException, AWTException, IOException, JCodecException {
		if (args.length == 0) {
			printHelp();
			return;
		}

		try {
			final CommandLineParser clip = new DefaultParser();
			final CommandLine cli = clip.parse(OPTIONS, args, true);
			OptionsHelper.verbose(cli.hasOption(OPT_VERBOSE));
			final String filename = cli.getOptionValue(OPT_OUTPUT);
			final File output = new File(filename);
			if (cli.hasOption(OPT_GRAB)) {
				new Grabber(output, new FrameConcealer()).show();
			} else if (cli.hasOption(OPT_FILE)) {
				final File input = new File(cli.getOptionValue(OPT_FILE));
				write(
					output,
					readVideo(input)
				);
			} else {
				final String prefix = cli.getOptionValue(OPT_PREFIX);
				if (prefix == null) {
					System.out.println("Prefix required if neither grabbing nor reading video\n");
					printHelp();
					System.exit(1);
				}
				write(
					output,
					readFiles(prefix)
				);
				write(
					output,
					StreamSupport
						.stream(
							Spliterators.spliterator(
								new ImageIterator(prefix),
								1,
								Spliterator.NONNULL | Spliterator.ORDERED),
								false
						).map(img -> {
							try {
								return readQR(img);
							} catch (NotFoundException e) {
								throw new IllegalStateException(e);
							}
						}
					)
				);
			}
		} catch (MissingOptionException|IllegalStateException e) {
			System.out.printf("%s%n%n", e.getMessage());
			printHelp();
			System.exit(1);
		}

	}
}
