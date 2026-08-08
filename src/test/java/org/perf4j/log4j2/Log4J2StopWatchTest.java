package org.perf4j.log4j2;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.message.Message;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for {@link Log4J2StopWatch}.
 *
 * <p>These tests construct stop watches with a variety of constructor
 * combinations and verify that the configured {@link Logger} and levels
 * are honoured, both for normal and exceptional paths.</p>
 *
 * @since 3.0.0
 */
public class Log4J2StopWatchTest {

    private static final String TEST_LOGGER = "org.perf4j.log4j2.Log4J2StopWatchTest";

    private LoggerContext context;
    private org.apache.logging.log4j.core.Logger testLogger;
    private TestAppender recorder;

    @Before
    public void setUp() {
        context = LoggerContext.getContext(false);
        testLogger = context.getLogger(TEST_LOGGER);
        recorder = new TestAppender("testAppender");
        recorder.start();
        testLogger.addAppender(recorder);
        testLogger.setLevel(Level.ALL);
    }

    @After
    public void tearDown() {
        testLogger.removeAppender(recorder);
        recorder.stop();
    }

    @Test
    public void shouldDefaultToPerf4jTimingLoggerWhenNoLoggerProvided() {
        Log4J2StopWatch watch = new Log4J2StopWatch();
        assertNotNull(watch.getLogger());
        assertEquals("org.perf4j.TimingLogger", watch.getLogger().getName());
        assertEquals(Level.INFO, watch.getNormalPriority());
        assertEquals(Level.WARN, watch.getExceptionPriority());
    }

    @Test
    public void shouldUseSuppliedLoggerAndDefaultPriorities() {
        Log4J2StopWatch watch = new Log4J2StopWatch(testLogger);
        assertSame(testLogger, watch.getLogger());
        assertEquals(Level.INFO, watch.getNormalPriority());
        assertEquals(Level.WARN, watch.getExceptionPriority());
    }

    @Test
    public void shouldAcceptCustomNormalPriority() {
        Log4J2StopWatch watch = new Log4J2StopWatch(testLogger, Level.DEBUG);
        assertEquals(Level.DEBUG, watch.getNormalPriority());
        assertEquals(Level.WARN, watch.getExceptionPriority());
    }

    @Test
    public void shouldAcceptCustomNormalAndExceptionPriority() {
        Log4J2StopWatch watch = new Log4J2StopWatch(testLogger, Level.DEBUG, Level.ERROR);
        assertEquals(Level.DEBUG, watch.getNormalPriority());
        assertEquals(Level.ERROR, watch.getExceptionPriority());
    }

    @Test
    public void shouldUseSuppliedTagWithDefaults() {
        Log4J2StopWatch watch = new Log4J2StopWatch("checkout");
        assertEquals("checkout", watch.getTag());
        assertEquals("org.perf4j.TimingLogger", watch.getLogger().getName());
    }

    @Test
    public void shouldUseSuppliedTagAndLogger() {
        Log4J2StopWatch watch = new Log4J2StopWatch("checkout", testLogger);
        assertEquals("checkout", watch.getTag());
        assertSame(testLogger, watch.getLogger());
    }

    @Test
    public void shouldUseSuppliedTagLoggerAndPriority() {
        Log4J2StopWatch watch = new Log4J2StopWatch("checkout", testLogger, Level.DEBUG);
        assertEquals("checkout", watch.getTag());
        assertSame(testLogger, watch.getLogger());
        assertEquals(Level.DEBUG, watch.getNormalPriority());
    }

    @Test
    public void shouldUseFullConstructorWithTagAndLogger() {
        Log4J2StopWatch watch = new Log4J2StopWatch("checkout", testLogger, Level.DEBUG, Level.ERROR);
        assertEquals("checkout", watch.getTag());
        assertSame(testLogger, watch.getLogger());
        assertEquals(Level.DEBUG, watch.getNormalPriority());
        assertEquals(Level.ERROR, watch.getExceptionPriority());
    }

    @Test
    public void shouldAcceptTagAndMessageWithDefaults() {
        Log4J2StopWatch watch = new Log4J2StopWatch("checkout", "cart contents");
        assertEquals("checkout", watch.getTag());
        assertEquals("cart contents", watch.getMessage());
        assertEquals("org.perf4j.TimingLogger", watch.getLogger().getName());
    }

    @Test
    public void shouldAcceptTagMessageAndLogger() {
        Log4J2StopWatch watch = new Log4J2StopWatch("checkout", "cart contents", testLogger);
        assertEquals("checkout", watch.getTag());
        assertEquals("cart contents", watch.getMessage());
        assertSame(testLogger, watch.getLogger());
    }

    @Test
    public void shouldAcceptTagMessageLoggerAndNormalPriority() {
        Log4J2StopWatch watch = new Log4J2StopWatch("checkout", "cart contents", testLogger, Level.DEBUG);
        assertEquals("checkout", watch.getTag());
        assertEquals("cart contents", watch.getMessage());
        assertSame(testLogger, watch.getLogger());
        assertEquals(Level.DEBUG, watch.getNormalPriority());
        assertEquals(Level.WARN, watch.getExceptionPriority());
    }

    @Test
    public void shouldAcceptFullConstructorWithTagAndMessage() {
        Log4J2StopWatch watch = new Log4J2StopWatch(
                "checkout", "cart contents", testLogger, Level.DEBUG, Level.ERROR);
        assertEquals("checkout", watch.getTag());
        assertEquals("cart contents", watch.getMessage());
        assertSame(testLogger, watch.getLogger());
        assertEquals(Level.DEBUG, watch.getNormalPriority());
        assertEquals(Level.ERROR, watch.getExceptionPriority());
    }

    @Test
    public void shouldAcceptDeepConstructorWithExplicitTiming() {
        long start = 1000L;
        long elapsed = 250L;
        Log4J2StopWatch watch = new Log4J2StopWatch(start, elapsed, "checkout", "msg",
                testLogger, Level.DEBUG, Level.ERROR);
        assertEquals(start, watch.getStartTime());
        assertEquals(elapsed, watch.getElapsedTime());
        assertEquals("checkout", watch.getTag());
        assertEquals("msg", watch.getMessage());
        assertSame(testLogger, watch.getLogger());
        assertEquals(Level.DEBUG, watch.getNormalPriority());
        assertEquals(Level.ERROR, watch.getExceptionPriority());
    }

    @Test
    public void shouldReturnTrueWhenLoggerEnabledAtNormalPriority() {
        Log4J2StopWatch watch = new Log4J2StopWatch(testLogger, Level.DEBUG);
        assertTrue(watch.isLogging());
    }

    @Test
    public void shouldReturnFalseWhenLoggerDisabledAtNormalPriority() {
        testLogger.setLevel(Level.WARN);
        Log4J2StopWatch watch = new Log4J2StopWatch(testLogger, Level.DEBUG);
        assertFalse(watch.isLogging());
    }

    @Test
    public void shouldLogAtNormalPriorityWhenStoppedWithoutException() {
        Log4J2StopWatch watch = new Log4J2StopWatch("checkout", "msg", testLogger, Level.INFO);
        watch.stop();
        assertEquals(1, recorder.events.size());
        assertEquals(Level.INFO, recorder.events.get(0).getLevel());
    }

    @Test
    public void shouldLogAtExceptionPriorityWhenStoppedWithException() {
        Log4J2StopWatch watch = new Log4J2StopWatch("checkout", "msg", testLogger, Level.INFO, Level.ERROR);
        Exception error = new RuntimeException("boom");
        watch.stop("checkout", error);
        assertEquals(1, recorder.events.size());
        assertEquals(Level.ERROR, recorder.events.get(0).getLevel());
        assertSame(error, recorder.events.get(0).getThrown());
    }

    @Test
    public void shouldSetLoggerViaSetter() {
        Log4J2StopWatch watch = new Log4J2StopWatch();
        Logger resolved = LogManager.getLogger("org.perf4j.log4j2.otherLogger");
        Log4J2StopWatch returned = watch.setLogger(resolved);
        assertSame(resolved, watch.getLogger());
        assertSame(watch, returned);
    }

    @Test
    public void shouldSetNormalPriorityViaSetter() {
        Log4J2StopWatch watch = new Log4J2StopWatch();
        Log4J2StopWatch returned = watch.setNormalPriority(Level.DEBUG);
        assertEquals(Level.DEBUG, watch.getNormalPriority());
        assertSame(watch, returned);
    }

    @Test
    public void shouldSetExceptionPriorityViaSetter() {
        Log4J2StopWatch watch = new Log4J2StopWatch();
        Log4J2StopWatch returned = watch.setExceptionPriority(Level.FATAL);
        assertEquals(Level.FATAL, watch.getExceptionPriority());
        assertSame(watch, returned);
    }

    @Test
    public void shouldReturnSelfForCovariantSetters() {
        Log4J2StopWatch watch = new Log4J2StopWatch();
        assertSame(watch, watch.setTimeThreshold(10L));
        assertSame(watch, watch.setTag("t"));
        assertSame(watch, watch.setMessage("m"));
        assertSame(watch, watch.setNormalAndSlowSuffixesEnabled(true));
        assertSame(watch, watch.setNormalSuffix(".N"));
        assertSame(watch, watch.setSlowSuffix(".S"));
        assertEquals("t.N", watch.getTag());
        assertEquals("m", watch.getMessage());
        assertTrue(watch.isNormalAndSlowSuffixesEnabled());
        assertEquals(".N", watch.getNormalSuffix());
        assertEquals(".S", watch.getSlowSuffix());
    }

    @Test
    public void shouldCloneAndPreserveConfiguration() {
        Log4J2StopWatch original = new Log4J2StopWatch(
                "checkout", "msg", testLogger, Level.DEBUG, Level.ERROR);
        Log4J2StopWatch copy = original.clone();
        assertEquals(original.getTag(), copy.getTag());
        assertEquals(original.getMessage(), copy.getMessage());
        assertEquals(original.getNormalPriority(), copy.getNormalPriority());
        assertEquals(original.getExceptionPriority(), copy.getExceptionPriority());
        assertEquals(original.getLogger().getName(), copy.getLogger().getName());
    }

    @Test
    public void shouldRoundTripThroughSerialization() throws Exception {
        Log4J2StopWatch original = new Log4J2StopWatch(
                "checkout", "msg", testLogger, Level.DEBUG, Level.ERROR);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(original);
        }

        Log4J2StopWatch restored;
        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray()))) {
            restored = (Log4J2StopWatch) ois.readObject();
        }

        assertEquals(original.getTag(), restored.getTag());
        assertEquals(original.getMessage(), restored.getMessage());
        assertEquals(original.getNormalPriority(), restored.getNormalPriority());
        assertEquals(original.getExceptionPriority(), restored.getExceptionPriority());
        assertEquals(original.getLogger().getName(), restored.getLogger().getName());
        assertNotNull(restored.getLogger());
    }

    /**
     * Verifies that the rendered LogEvent for a normal stop carries the
     * stringified stop watch payload.
     */
    @Test
    public void shouldRenderStopWatchPayloadAsMessage() {
        Log4J2StopWatch watch = new Log4J2StopWatch("checkout", "msg", testLogger, Level.INFO);
        watch.stop();
        assertEquals(1, recorder.events.size());
        Message message = recorder.events.get(0).getMessage();
        assertNotNull(message);
        String formatted = message.getFormattedMessage();
        assertNotNull(formatted);
        assertTrue(formatted, formatted.contains("checkout"));
    }

    /**
     * Sanity check: the helper class is referenced via the {@link Appender}
     * interface so the test tool bridge is wired.
     */
    @Test
    public void shouldCompileAgainstAppenderInterface() {
        Appender anyAppender = recorder;
        assertNotNull(anyAppender);
        assertEquals("testAppender", anyAppender.getName());
    }

    /**
     * Minimal {@link AbstractAppender} that records every LogEvent it
     * receives. Avoids pulling in the {@code log4j-core-test} artifact.
     */
    private static final class TestAppender extends AbstractAppender {
        private final List<LogEvent> events = new ArrayList<LogEvent>();

        private TestAppender(String name) {
            super(name, null, PatternLayout.createDefaultLayout(), true, Property.EMPTY_ARRAY);
        }

        @Override
        public void append(LogEvent event) {
            events.add(event.toImmutable());
        }
    }
}
