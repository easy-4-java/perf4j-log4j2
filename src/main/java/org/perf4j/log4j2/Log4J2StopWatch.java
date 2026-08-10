package org.perf4j.log4j2;


import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.perf4j.LoggingStopWatch;

/**
 * Log4j 2.x implementation of Perf4j's {@link LoggingStopWatch}. When one of
 * the {@code stop(...)} or {@code lap(...)} methods is invoked the stop watch
 * hands the rendered timing string to the configured Log4j 2 {@link Logger} at
 * the configured {@link Level}.
 *
 * <p>The class exposes several convenience constructors that allow the caller
 * to fix:</p>
 * <ul>
 *   <li>the tag (a logical grouping key for the timing event);</li>
 *   <li>an optional descriptive message;</li>
 *   <li>the {@link Logger} used to persist the event (defaults to
 *       {@code org.perf4j.TimingLogger});</li>
 *   <li>the {@link Level} used for normal stops (defaults to
 *       {@link Level#INFO});</li>
 *   <li>the {@link Level} used when an exception is passed to the stop/lap
 *       method (defaults to {@link Level#WARN}).</li>
 * </ul>
 *
 * @author Alex Devine
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 3.0.0
 * @see LoggingStopWatch
 * @see org.perf4j.log4j2.aop.TimingAspect
 */
@SuppressWarnings("serial")
public class Log4J2StopWatch extends LoggingStopWatch {
    /**
     * Log4j 2 logger used to persist the stop-watch line. Resolved through
     * {@link LogManager#getLogger} on construction.
     */
    private transient Logger logger;
    /**
     * Level used for normal stop/lap calls (those that do not take an exception).
     */
    private Level normalPriority;
    /**
     * Level used for stop/lap calls that take an exception.
     */
    private Level exceptionPriority;

    // --- Constructors ---

    /**
     * Creates a Log4J2StopWatch with a blank tag, no message and started at
     * the instant of creation. The Logger with the name
     * {@value LoggingStopWatch#DEFAULT_LOGGER_NAME} is used to log stop watch
     * messages at {@link Level#INFO}, or at {@link Level#WARN} when an
     * exception is passed to {@code stop(...)} or {@code lap(...)}.
     */
    public Log4J2StopWatch() {
        this("", null, LogManager.getLogger(DEFAULT_LOGGER_NAME), Level.INFO, Level.WARN);
    }

    /**
     * Creates a Log4J2StopWatch with a blank tag, no message and started at
     * the instant of creation, using the supplied {@link Logger} at
     * {@link Level#INFO}, or {@link Level#WARN} when stop/lap is called with
     * an exception.
     *
     * @param logger the {@link Logger} used to persist the stop watch.
     */
    public Log4J2StopWatch(Logger logger) {
        this("", null, logger, Level.INFO, Level.WARN);
    }

    /**
     * Creates a Log4J2StopWatch with a blank tag, no message and started at
     * the instant of creation, using the supplied {@link Logger} at the
     * supplied {@link Level}, or {@link Level#WARN} when stop/lap is called
     * with an exception.
     *
     * @param logger         the {@link Logger} used to persist the stop watch.
     * @param normalPriority the level for normal stop/lap calls.
     */
    public Log4J2StopWatch(Logger logger, Level normalPriority) {
        this("", null, logger, normalPriority, Level.WARN);
    }

    /**
     * Creates a Log4J2StopWatch with a blank tag, no message and started at
     * the instant of creation, using the supplied {@link Logger} at the
     * supplied levels.
     *
     * @param logger            the {@link Logger} used to persist the stop watch.
     * @param normalPriority    the level for normal stop/lap calls.
     * @param exceptionPriority the level for stop/lap calls that take an exception.
     */
    public Log4J2StopWatch(Logger logger, Level normalPriority, Level exceptionPriority) {
        this("", null, logger, normalPriority, exceptionPriority);
    }

    /**
     * Creates a Log4J2StopWatch with the supplied tag, no message and started
     * at the instant of creation. The Logger with the name
     * {@value LoggingStopWatch#DEFAULT_LOGGER_NAME} is used to log stop watch
     * messages at {@link Level#INFO}, or at {@link Level#WARN} when an
     * exception is passed to {@code stop(...)} or {@code lap(...)}.
     *
     * @param tag the tag name for this timing call. Tags are used to group
     *            timing logs; each timed code block should normally use a
     *            unique tag. Tags may use dot-notation for hierarchical
     *            grouping.
     */
    public Log4J2StopWatch(String tag) {
        this(tag, null, LogManager.getLogger(DEFAULT_LOGGER_NAME), Level.INFO, Level.WARN);
    }

    /**
     * Creates a Log4J2StopWatch with the supplied tag, no message and started
     * at the instant of creation, using the supplied {@link Logger} at
     * {@link Level#INFO}, or {@link Level#WARN} when stop/lap is called with
     * an exception.
     *
     * @param tag    the tag name for this timing call.
     * @param logger the {@link Logger} used to persist the stop watch.
     */
    public Log4J2StopWatch(String tag, Logger logger) {
        this(tag, null, logger, Level.INFO, Level.WARN);
    }

    /**
     * Creates a Log4J2StopWatch with the supplied tag, no message and started
     * at the instant of creation, using the supplied {@link Logger} at the
     * supplied level, or {@link Level#WARN} when stop/lap is called with an
     * exception.
     *
     * @param tag            the tag name for this timing call.
     * @param logger         the {@link Logger} used to persist the stop watch.
     * @param normalPriority the level for normal stop/lap calls.
     */
    public Log4J2StopWatch(String tag, Logger logger, Level normalPriority) {
        this(tag, null, logger, normalPriority, Level.WARN);
    }

    /**
     * Creates a Log4J2StopWatch with the supplied tag, no message and started
     * at the instant of creation, using the supplied {@link Logger} at the
     * supplied levels.
     *
     * @param tag               the tag name for this timing call.
     * @param logger            the {@link Logger} used to persist the stop watch.
     * @param normalPriority    the level for normal stop/lap calls.
     * @param exceptionPriority the level for stop/lap calls that take an exception.
     */
    public Log4J2StopWatch(String tag, Logger logger, Level normalPriority, Level exceptionPriority) {
        this(tag, null, logger, normalPriority, exceptionPriority);
    }

    /**
     * Creates a Log4J2StopWatch with the supplied tag and message, started at
     * the instant of creation. The Logger with the name
     * {@value LoggingStopWatch#DEFAULT_LOGGER_NAME} is used at
     * {@link Level#INFO}, or {@link Level#WARN} when stop/lap is called with
     * an exception.
     *
     * @param tag     the tag name for this timing call.
     * @param message additional text appended to the stop-watch log statement.
     */
    public Log4J2StopWatch(String tag, String message) {
        this(tag, message, LogManager.getLogger(DEFAULT_LOGGER_NAME), Level.INFO, Level.WARN);
    }

    /**
     * Creates a Log4J2StopWatch with the supplied tag and message, started at
     * the instant of creation, using the supplied {@link Logger} at
     * {@link Level#INFO}, or {@link Level#WARN} when stop/lap is called with
     * an exception.
     *
     * @param tag     the tag name for this timing call.
     * @param message additional text appended to the stop-watch log statement.
     * @param logger  the {@link Logger} used to persist the stop watch.
     */
    public Log4J2StopWatch(String tag, String message, Logger logger) {
        this(tag, message, logger, Level.INFO, Level.WARN);
    }

    /**
     * Creates a Log4J2StopWatch with the supplied tag and message, started at
     * the instant of creation, using the supplied {@link Logger} at the
     * supplied level, or {@link Level#WARN} when stop/lap is called with an
     * exception.
     *
     * @param tag            the tag name for this timing call.
     * @param message        additional text appended to the stop-watch log statement.
     * @param logger         the {@link Logger} used to persist the stop watch.
     * @param normalPriority the level for normal stop/lap calls.
     */
    public Log4J2StopWatch(String tag, String message, Logger logger, Level normalPriority) {
        this(tag, message, logger, normalPriority, Level.WARN);
    }

    /**
     * Creates a Log4J2StopWatch with the supplied tag and message, started at
     * the instant of creation, using the supplied {@link Logger} at the
     * supplied levels.
     *
     * @param tag               the tag name for this timing call.
     * @param message           additional text appended to the stop-watch log statement.
     * @param logger            the {@link Logger} used to persist the stop watch.
     * @param normalPriority    the level for normal stop/lap calls.
     * @param exceptionPriority the level for stop/lap calls that take an exception.
     */
    public Log4J2StopWatch(String tag, String message, Logger logger, Level normalPriority, Level exceptionPriority) {
        this(System.currentTimeMillis(), -1L, tag, message, logger, normalPriority, exceptionPriority);
    }

    /**
     * Low-level constructor used primarily for deserialization of log records
     * and for testing. Client code should normally use the higher-level
     * constructors.
     *
     * @param startTime         the start time in milliseconds since the epoch.
     * @param elapsedTime       the elapsed time in milliseconds, or {@code -1L}
     *                          if the stop watch is still running.
     * @param tag               the tag name for this timing call.
     * @param message           additional text appended to the stop-watch log statement.
     * @param logger            the {@link Logger} used to persist the stop watch.
     * @param normalPriority    the level for normal stop/lap calls.
     * @param exceptionPriority the level for stop/lap calls that take an exception.
     */
    public Log4J2StopWatch(long startTime, long elapsedTime, String tag, String message,
                          Logger logger, Level normalPriority, Level exceptionPriority) {
        super(startTime, elapsedTime, tag, message);
        this.logger = logger;
        this.normalPriority = normalPriority;
        this.exceptionPriority = exceptionPriority;
    }

    // --- Bean Methods ---

    /**
     * Returns the Log4j 2 {@link Logger} used to persist the stop watch.
     *
     * @return the {@link Logger} used for stop-watch persistence.
     */
    public Logger getLogger() { return logger; }

    /**
     * Replaces the Log4j 2 {@link Logger} used to persist stop watch events.
     *
     * @param logger the replacement {@link Logger}. Must not be {@code null}.
     * @return this instance, for fluent method chaining.
     */
    public Log4J2StopWatch setLogger(Logger logger) {
        this.logger = logger;
        return this;
    }

    /**
     * Returns the {@link Level} applied to normal stop/lap calls.
     *
     * @return the level used for normal stop/lap calls.
     */
    public Level getNormalPriority() { return normalPriority; }

    /**
     * Replaces the {@link Level} applied to normal stop/lap calls.
     *
     * @param normalPriority the replacement level. Must not be {@code null}.
     * @return this instance, for fluent method chaining.
     */
    public Log4J2StopWatch setNormalPriority(Level normalPriority) {
        this.normalPriority = normalPriority;
        return this;
    }

    /**
     * Returns the {@link Level} applied to stop/lap calls that take an
     * exception.
     *
     * @return the level used for exceptional stop/lap calls.
     */
    public Level getExceptionPriority() { return exceptionPriority; }

    /**
     * Replaces the {@link Level} applied to stop/lap calls that take an
     * exception. The level should normally be equal to or higher than the
     * {@linkplain #getNormalPriority() normal priority}.
     *
     * @param exceptionPriority the replacement level. Must not be {@code null}.
     * @return this instance, for fluent method chaining.
     */
    public Log4J2StopWatch setExceptionPriority(Level exceptionPriority) {
        this.exceptionPriority = exceptionPriority;
        return this;
    }

    /**
     * Covariant override of {@link LoggingStopWatch#setTimeThreshold(long)}.
     *
     * @param timeThreshold the new time threshold in milliseconds.
     * @return this instance, for fluent method chaining.
     */
    public Log4J2StopWatch setTimeThreshold(long timeThreshold) {
        super.setTimeThreshold(timeThreshold);
        return this;
    }

    /**
     * Covariant override of {@link LoggingStopWatch#setTag(String)}.
     *
     * @param tag the replacement tag.
     * @return this instance, for fluent method chaining.
     */
    public Log4J2StopWatch setTag(String tag) {
        super.setTag(tag);
        return this;
    }

    /**
     * Covariant override of {@link LoggingStopWatch#setMessage(String)}.
     *
     * @param message the replacement message.
     * @return this instance, for fluent method chaining.
     */
    public Log4J2StopWatch setMessage(String message) {
        super.setMessage(message);
        return this;
    }

    /**
     * Covariant override of
     * {@link LoggingStopWatch#setNormalAndSlowSuffixesEnabled(boolean)}.
     *
     * @param normalAndSlowSuffixesEnabled the new value.
     * @return this instance, for fluent method chaining.
     */
    public Log4J2StopWatch setNormalAndSlowSuffixesEnabled(boolean normalAndSlowSuffixesEnabled) {
    	super.setNormalAndSlowSuffixesEnabled(normalAndSlowSuffixesEnabled);
    	return this;
    }

    /**
     * Covariant override of {@link LoggingStopWatch#setNormalSuffix(String)}.
     *
     * @param normalSuffix the replacement normal suffix.
     * @return this instance, for fluent method chaining.
     */
    public Log4J2StopWatch setNormalSuffix(String normalSuffix) {
    	super.setNormalSuffix(normalSuffix);
    	return this;
    }

    /**
     * Covariant override of {@link LoggingStopWatch#setSlowSuffix(String)}.
     *
     * @param slowSuffix the replacement slow suffix.
     * @return this instance, for fluent method chaining.
     */
    public Log4J2StopWatch setSlowSuffix(String slowSuffix) {
    	super.setSlowSuffix(slowSuffix);
    	return this;
    }

    // --- Helper Methods ---
    /**
     * Indicates whether the underlying {@link Logger} is enabled at the
     * configured {@linkplain #getNormalPriority() normal priority}.
     *
     * @return {@code true} if a normal stop/lap call will produce a log
     *         statement; {@code false} otherwise.
     */
    public boolean isLogging() {
        return logger.isEnabled(normalPriority);
    }

    /**
     * Hands off the rendered stop-watch string to the underlying Log4j 2
     * {@link Logger} at the appropriate level.
     *
     * @param stopWatchAsString the rendered stop-watch string.
     * @param exception         an optional exception. When {@code null} the
     *                          statement is logged at
     *                          {@link #getNormalPriority()}; otherwise it is
     *                          logged at {@link #getExceptionPriority()}.
     */
    protected void log(String stopWatchAsString, Throwable exception) {
        logger.log((exception == null) ? normalPriority : exceptionPriority, stopWatchAsString, exception);
    }

    // --- Object Methods ---

    /**
     * Creates a shallow copy of this stop watch.
     *
     * @return a clone of this instance.
     */
    public Log4J2StopWatch clone() {
        return (Log4J2StopWatch) super.clone();
    }

    /**
     * Custom serialization that persists the logger name alongside the
     * inherited state.
     *
     * @param stream the output stream.
     * @throws IOException if the underlying stream fails.
     */
    private void writeObject(ObjectOutputStream stream) throws IOException {
        stream.defaultWriteObject();
        stream.writeUTF(logger.getName());
    }

    /**
     * Custom deserialization that re-resolves the {@link Logger} from the
     * {@link LogManager} by name.
     *
     * @param stream the input stream.
     * @throws IOException if the underlying stream fails.
     * @throws ClassNotFoundException if a serialized class cannot be resolved.
     */
    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
        stream.defaultReadObject();
        this.logger = LogManager.getLogger(stream.readUTF());
    }
}
