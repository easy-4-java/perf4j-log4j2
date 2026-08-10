package org.perf4j.log4j2;

import java.nio.charset.Charset;

import org.apache.commons.csv.CSVFormat;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.layout.AbstractCsvLayout;
import org.apache.logging.log4j.core.layout.CsvParameterLayout;
import org.perf4j.GroupedTimingStatistics;
import org.perf4j.helpers.GroupedTimingStatisticsCsvFormatter;
import org.perf4j.helpers.MiscUtils;

/**
 * Log4j 2.x layout that renders {@link GroupedTimingStatistics} log events as
 * comma-separated values. This layout is intended to be attached to a
 * downstream appender that is itself attached to an
 * {@link AsyncCoalescingStatisticsAppender}.
 *
 * <p>By default, each {@link GroupedTimingStatistics} event is emitted as one
 * line per tagged timing statistic, with the following columns:</p>
 * <ol>
 *   <li>tag - the tag name of the code block that the statistics refer to</li>
 *   <li>start - the start time of the timing window</li>
 *   <li>stop - the stop time of the timing window</li>
 *   <li>mean - the mean execution time within the timing window</li>
 *   <li>min - the minimum execution time within the timing window</li>
 *   <li>max - the maximum execution time within the timing window</li>
 *   <li>stddev - the standard deviation of execution times within the window</li>
 *   <li>count - the number of stop-watch logs captured in the window</li>
 * </ol>
 *
 * <p>The {@link #setColumns(String) Columns} option can override the default
 * column list. The special token {@code tps} can be added to expose
 * transactions-per-second.</p>
 *
 * <p>Setting {@link #setPivot(boolean) Pivot} to {@code true} collapses the
 * output to a single line per {@link GroupedTimingStatistics} event; in that
 * case the columns should reference specific tags (e.g.
 * {@code "start,stop,codeBlock1Mean,codeBlock2Mean"}).</p>
 *
 * @author Alex Devine
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 3.0.0
 * @see GroupedTimingStatisticsCsvFormatter
 * @see AsyncCoalescingStatisticsAppender
 */
public class StatisticsCsvLayout extends AbstractCsvLayout {

    /**
     * Convenience factory that builds the layout with the Log4j 2 default
     * charset and CSV format.
     *
     * @return a new {@link StatisticsCsvLayout} configured with the Log4j 2
     *         defaults.
     */
	public static AbstractCsvLayout createDefaultLayout() {
		return new StatisticsCsvLayout(null, Charset.forName(DEFAULT_CHARSET), CSVFormat.valueOf(DEFAULT_FORMAT), null, null);
	}

    /**
     * Convenience factory that builds the layout with a caller-supplied
     * {@link CSVFormat} while keeping the default charset.
     *
     * @param format the CSV format to use. Must not be {@code null}.
     * @return a new {@link StatisticsCsvLayout} configured with the supplied
     *         CSV format.
     */
	public static AbstractCsvLayout createLayout(final CSVFormat format) {
		return new StatisticsCsvLayout(null, Charset.forName(DEFAULT_CHARSET), format, null, null);
	}

    /**
     * Constructs the layout with full control over every CSV concern.
     *
     * @param config    the Log4j 2 configuration. May be {@code null}.
     * @param charset   the character set used to encode the output.
     * @param csvFormat the CSV format applied to the output.
     * @param header    the optional header to prepend to each batch.
     * @param footer    the optional footer to append to each batch.
     */
    public StatisticsCsvLayout(final Configuration config, final Charset charset, final CSVFormat csvFormat,
			final String header, final String footer) {
		super(config, charset, csvFormat, header, footer);
	}

	// --- configuration options ---
	/**
	 * Pivot option. When {@code true}, the layout emits a single line per
	 * {@link GroupedTimingStatistics} event instead of one line per tag.
	 */
	private boolean pivot = false;
	/**
	 * Columns option, a comma-separated list of column values to output.
	 * Defaults to {@link GroupedTimingStatisticsCsvFormatter#DEFAULT_FORMAT_STRING}.
	 */
	private String columns = GroupedTimingStatisticsCsvFormatter.DEFAULT_FORMAT_STRING;
	/**
	 * PrintNonStatistics option. When {@code true}, non-{@link GroupedTimingStatistics}
	 * events are emitted using their string representation.
	 */
	private boolean printNonStatistics = false;

	// --- contained objects ---
	/**
	 * The CSV formatter that actually performs the rendering. Built in
	 * {@link #activateOptions()} from the current {@link #pivot} and
	 * {@link #columns} values.
	 */
	protected GroupedTimingStatisticsCsvFormatter csvFormatter;

	// --- configuration options ---

	/**
	 * Returns the {@link #pivot Pivot} option.
	 *
	 * @return {@code true} when a single line is emitted per
	 *         {@link GroupedTimingStatistics} event; {@code false} for one
	 *         line per tag.
	 */
	public boolean isPivot() {
		return pivot;
	}

	/**
	 * Sets the {@link #pivot Pivot} option.
	 *
	 * @param pivot the new Pivot option value. {@code true} collapses the
	 *              output to a single line per event.
	 */
	public void setPivot(boolean pivot) {
		this.pivot = pivot;
	}

	/**
	 * Returns the configured {@link #columns Columns} option.
	 *
	 * @return the comma-separated list of column tokens to be emitted.
	 */
	public String getColumns() {
		return columns;
	}

	/**
	 * Sets the comma-separated list of column tokens to emit.
	 *
	 * @param columns the new Columns option value. See the class-level Javadoc
	 *                for the supported token names.
	 */
	public void setColumns(String columns) {
		this.columns = columns;
	}

	/**
	 * Returns the {@link #printNonStatistics PrintNonStatistics} option.
	 *
	 * @return {@code true} if non-{@link GroupedTimingStatistics} events are
	 *         emitted as their string value; {@code false} if they are
	 *         suppressed.
	 */
	public boolean isPrintNonStatistics() {
		return printNonStatistics;
	}

	/**
	 * Sets the {@link #printNonStatistics PrintNonStatistics} option.
	 *
	 * @param printNonStatistics the new PrintNonStatistics option value.
	 */
	public void setPrintNonStatistics(boolean printNonStatistics) {
		this.printNonStatistics = printNonStatistics;
	}

    /**
     * Formats the supplied log event as CSV.
     *
     * <p>If the event carries a {@link GroupedTimingStatistics} message it is
     * delegated to the {@link GroupedTimingStatisticsCsvFormatter}. Otherwise
     * the event is either rendered as its string representation (when
     * {@link #printNonStatistics} is enabled) or skipped entirely.</p>
     *
     * @param event the log event to format.
     * @return the CSV-encoded rendering, or an empty string when the event
     *         should be suppressed.
     */
	public String format(Log4jLogEvent event) {
		try {
			// we assume that the event is a GroupedTimingStatistics object
			return csvFormatter.format((GroupedTimingStatistics) event.getMessage());
		} catch (ClassCastException cce) {
			// then it's not a GroupedTimingStatistics object
			if (isPrintNonStatistics()) {
				return MiscUtils.escapeStringForCsv(event.getMessage().toString(), new StringBuilder())
						.append(MiscUtils.NEWLINE).toString();
			} else {
				return "";
			}
		}
	}

	/**
	 * This layout always ignores {@link Throwable} instances attached to the
	 * underlying log event &mdash; only the message body is rendered.
	 *
	 * @return always {@code true}.
	 */
	public boolean ignoresThrowable() {
		return true;
	}

    /**
     * Builds the underlying {@link GroupedTimingStatisticsCsvFormatter} from
     * the current {@link #pivot} and {@link #columns} options.
     */
	public void activateOptions() {
		csvFormatter = new GroupedTimingStatisticsCsvFormatter(isPivot(), getColumns());
	}

    /**
     * Returns {@code null} &mdash; this layout delegates the actual rendering
     * to {@link #format(Log4jLogEvent)} and intentionally does not implement
     * the generic {@link org.apache.logging.log4j.core.Layout} {@code toSerializable}
     * contract.
     *
     * @param event the log event to serialize.
     * @return always {@code null}.
     */
	@Override
	public String toSerializable(LogEvent event) {
		return null;
	}
}
