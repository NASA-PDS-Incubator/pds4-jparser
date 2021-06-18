package gov.nasa.arc.pds.tools.util;

import java.text.MessageFormat;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A helper class for Locale functionality and locale specific functionality.
 * This includes message retrieval and formatting from message bundles.
 * 
 * @author jagander
 * @version $Revision: $
 * 
 */
public class LocaleUtils {

	public static final String DEFAULT_LOCALE_KEY = "en_US"; //$NON-NLS-1$

	public static final String DEFAULT_LANGUAGE = "en"; //$NON-NLS-1$

	public static final Locale DEFAULT_LOCALE = new Locale(DEFAULT_LOCALE_KEY);

	public static final String DEFAULT_BUNDLE_ROOT = "resources"; //$NON-NLS-1$

	public static final boolean DEFAULT_DEV_MODE = false;

	private Locale locale;

	public ResourceBundle bundle;

	private Boolean devMode = false;

	// capture 2 letter language designation followed by optional 2 letter
	// country code followed by optional variant
	// NOTE: variant may contain an underscore (ex. "Traditional_WIN") so a
	// split won't help
	// NOTE: country code and variant are preceded by an underscore
	// NOTE: language and country code are always 2 letter but variant may be
	// any length
	public final static Pattern LOCALE_PATTERN = Pattern
			.compile("(\\w{2})(_(\\w{2}))?(_(\\w+))?"); //$NON-NLS-1$

	public final static String NUMERIC_FORMAT = "{0,number,integer}"; //$NON-NLS-1$

	public static final Map<Set<Object>, ResourceBundle> BUNDLE_CACHE = new HashMap<Set<Object>, ResourceBundle>();

	public LocaleUtils() {
		this(null, null, DEFAULT_BUNDLE_ROOT);
	}

	public LocaleUtils(final Locale locale) {
		this(locale, DEFAULT_DEV_MODE, DEFAULT_BUNDLE_ROOT);
	}

	public LocaleUtils(final Boolean devMode) {
		this(null, devMode, DEFAULT_BUNDLE_ROOT);
	}

	public LocaleUtils(final String bundleRoot) {
		this(null, DEFAULT_DEV_MODE, bundleRoot);
	}

	public LocaleUtils(final ResourceBundle bundle) {
		this(bundle.getLocale(), DEFAULT_DEV_MODE, bundle);
	}

	public LocaleUtils(final Locale locale, final Boolean devMode) {
		this(locale, devMode, DEFAULT_BUNDLE_ROOT);
	}

	public LocaleUtils(final Locale locale, final String bundleRoot) {
		this(locale, DEFAULT_DEV_MODE, bundleRoot);
	}

	public LocaleUtils(final Locale locale, final ResourceBundle bundle) {
		this(locale, DEFAULT_DEV_MODE, bundle);
	}

	public LocaleUtils(final Locale locale, final Boolean devMode,
			final String bundleRoot) {
		setLocale(locale);
		setDevMode(devMode);
		this.bundle = ResourceBundle.getBundle(bundleRoot, this.locale);
	}

	// NOTE: not getting locale from bundle since, if did not find properties
	// file specific to that locale, it will set locale to a fallback. We still
	// want originally specified locale for formatting even if that is the case.
	public LocaleUtils(final Locale locale, final Boolean devMode,
			final ResourceBundle bundle) {
		setLocale(locale);
		setDevMode(devMode);
		this.bundle = bundle;
	}

	protected void setLocale(final Locale locale) {
		this.locale = (locale == null) ? DEFAULT_LOCALE : locale;
	}

	public Locale getLocale() {
		return this.locale;
	}

	protected void setDevMode(final Boolean devMode) {
		if (devMode != null) {
			this.devMode = devMode;
		}
	}

	public boolean isDevMode() {
		return this.devMode;
	}

	// override bundle
	protected void setBundle(final ResourceBundle bundle) {
		this.bundle = bundle;
	}

	// get the resource bundle
	public ResourceBundle getBundle() {
		return this.bundle;
	}

	// override bundle by name
	protected void setBundleName(String bundleName) {
		this.bundle = ResourceBundle.getBundle(bundleName, this.locale);
	}

	// since you can instantiate invalid locales, the best check is to see if
	// it's in the list of available locales
	public static boolean isValidLocale(final Locale locale) {
		Locale[] availLocales = Locale.getAvailableLocales();
		for (Locale testLocale : availLocales) {
			if (testLocale.equals(locale)) {
				return true;
			}
		}
		return false;
	}

	// get locale from locale string
	public static Locale stringToLocale(final String localeName) {
		String language = null;
		String country = null;
		String variant = null;
		Locale locale = null;

		final Matcher matcher = LOCALE_PATTERN.matcher(localeName);
		// check if locale string appears to be in correct form
		if (!matcher.matches()) {
			throw new RuntimeException(
					"Invalid locale name. The form must be LL[_CC][_VVV] where LL = 2 char language code, CC is an optional 2 char country code and VVV is an optional N character variant.");
		}

		// get parts of locale string
		language = matcher.group(1);
		country = matcher.group(3);
		variant = matcher.group(5);

		// create Locale from parts if present
		if (variant != null) {
			locale = new Locale(language, country, variant);
		} else if (country != null) {
			locale = new Locale(language, country);
		} else if (language != null) {
			locale = new Locale(language);
		}

		return locale;
	}

	// convert locale to a string representation
	// NOTE: toString() should be sufficient to identify the locale
	// this method exists to make that clear and to make central point
	// for further changes if necessary
	public static String localeToString(final Locale locale) {
		return locale.toString();
	}

	/**
	 * This retrieves a message from the appropriate properties file for your
	 * given Locale. It also uses any provided arguments to make substitutions
	 * and formatting changes to the found message. Note that this uses the OGNL
	 * expression language syntax for formatting.
	 * 
	 * @param key
	 *            - properties key used to look up message
	 * @param arguments
	 *            - an array of arguments to be used in the message, uses OGNL
	 *            syntax
	 * @return found and formatted message
	 */
	public String getText(final String key, final Object... arguments) {
		return getText(key, getBundle(), arguments);
	}

	// translation retrieval
	// TODO: throw exception on failure to find?
	private String getText(final String key, final ResourceBundle bundle,
			Object... arguments) {
		// some resources may be null rather than a key with empty value, short
		// circuit here
		if (key == null) {
			return null;
		}
		String message = null;
		try {
			// TODO: try to find in overriding bundle first then look in default
			message = bundle.getString(key);
		} catch (MissingResourceException e) {
			if (this.devMode) {
				throw new RuntimeException("Unable to find message for key \""
						+ key + "\".");
			}
			// return key as message if not in dev mode
			return key;
		}
		// substitute arguments and format
		return formatText(message, arguments);
	}

	public String formatText(final String message, Object... arguments) {
		Object[] args = null;
		// allow use of List to reduce legacy issues and increase
		// flexibility
		if (arguments != null && arguments.length == 1
				&& arguments[0] instanceof List) {
			args = ((List<?>) arguments[0]).toArray();
		} else {
			args = arguments;
		}

		MessageFormat format = new MessageFormat(message, this.locale);

		return format.format(args);
	}

	// helper method returns locale specific formatting for a single number
	public String getNumber(final Number number) {
		return formatText(NUMERIC_FORMAT, number);
	}

	public String formatBytes(final double memory) {
		return formatBytes(memory, null);
	}

	/**
	 * Format bytes to human readable text
	 * 
	 * @param memory
	 *            The memory value to format.
	 * @param override
	 *            The locale to be used (determines decimal and grouping
	 *            separators).
	 * @return The formatted number string.
	 */
	@SuppressWarnings("nls")
	public String formatBytes(final double memory, final Locale override) {
		String result = "";
		final Locale curLocale = override != null ? override : this.locale;
		final NumberFormat format = NumberFormat.getInstance(curLocale);

		final double value = memory;
		if (value < 0.9 * FileUtils.ONE_KB) {
			result = format.format(value) + " B";
		} else if (value < 0.9 * FileUtils.ONE_MB) {
			result = format.format(value / FileUtils.ONE_KB) + " KB";
		} else if (value < 0.9 * FileUtils.ONE_GB) {
			result = format.format(value / FileUtils.ONE_MB) + " MB";
		} else if (value < 0.9 * FileUtils.ONE_TB) {
			result = format.format(value / FileUtils.ONE_GB) + " GB";
		} else if (value < 0.9 * FileUtils.ONE_PB) {
			result = format.format(value / FileUtils.ONE_TB) + " TB";
		} else {
			result = format.format(value / FileUtils.ONE_PB) + " PB";
		}

		return result;
	}

}
