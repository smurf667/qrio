package de.engehausen.qrio.util;

import org.apache.commons.cli.Option;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * Helper to build CLI options.
 */
public class OptionsHelper {

	/**
	 * Creates a builder for the given option name and description.
	 * The first character of the option name is used for the
	 * option's short form.
	 * @param name the option name
	 * @param description the option description
	 * @return the builder for further processing
	 */
	public static Option.Builder option(final String name, final String description) {
		return Option.builder(name.substring(0, 1))
		.longOpt(name)
		.desc(description);
	}

	/**
	 * Creates a builder for the given option name and description,
	 * with one argument (named the same as the option).
	 * The first character of the option name is used for the
	 * option's short form.
	 * @param name the option name
	 * @param description the option description
	 * @return the builder for further processing
	 */
	public static Option.Builder optionWithArg(final String name, final String description) {
		return option(name, description)
		.argName(name)
		.hasArg();
	}

	/**
	 * Activates verbose mode by turning on {@code DEBUG} log level.
	 * @param activate flag to activate verbose mode
	 */
	public static void verbose(final boolean activate) {
		if (activate) {
			final Object root = LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
			if (root instanceof Logger) {
				((Logger) root).setLevel(Level.DEBUG);
			}
		}
	}

}
