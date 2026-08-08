package org.perf4j.log4j2;

import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.commons.csv.CSVFormat;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.layout.AbstractCsvLayout;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.SimpleMessage;
import org.junit.Before;
import org.junit.Test;
import org.perf4j.GroupedTimingStatistics;
import org.perf4j.TimingStatistics;
import org.perf4j.helpers.GroupedTimingStatisticsCsvFormatter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for {@link StatisticsCsvLayout}.
 *
 * @since 3.0.0
 */
public class StatisticsCsvLayoutTest {

    private StatisticsCsvLayout layout;

    @Before
    public void setUp() {
        layout = new StatisticsCsvLayout(null, null, CSVFormat.DEFAULT, null, null);
    }

    // --- static factories ---

    @Test
    public void shouldCreateDefaultLayout() {
        AbstractCsvLayout defaultLayout = StatisticsCsvLayout.createDefaultLayout();
        assertNotNull(defaultLayout);
        assertTrue(defaultLayout instanceof StatisticsCsvLayout);
    }

    @Test
    public void shouldCreateLayoutWithCustomFormat() {
        AbstractCsvLayout custom = StatisticsCsvLayout.createLayout(CSVFormat.MYSQL);
        assertNotNull(custom);
        assertTrue(custom instanceof StatisticsCsvLayout);
    }

    // --- constructor ---

    @Test
    public void shouldConstructWithFullParameters() {
        StatisticsCsvLayout custom = new StatisticsCsvLayout(null, null, CSVFormat.EXCEL, "header", "footer");
        assertNotNull(custom);
    }

    // --- pivot getter / setter ---

    @Test
    public void shouldDefaultPivotToFalse() {
        assertFalse(layout.isPivot());
    }

    @Test
    public void shouldSetAndRetrievePivot() {
        layout.setPivot(true);
        assertTrue(layout.isPivot());
        layout.setPivot(false);
        assertFalse(layout.isPivot());
    }

    // --- columns getter / setter ---

    @Test
    public void shouldDefaultColumnsToFormatterDefault() {
        assertEquals(GroupedTimingStatisticsCsvFormatter.DEFAULT_FORMAT_STRING, layout.getColumns());
    }

    @Test
    public void shouldSetAndRetrieveColumns() {
        layout.setColumns("tag,mean,count");
        assertEquals("tag,mean,count", layout.getColumns());
    }

    // --- printNonStatistics getter / setter ---

    @Test
    public void shouldDefaultPrintNonStatisticsToFalse() {
        assertFalse(layout.isPrintNonStatistics());
    }

    @Test
    public void shouldSetAndRetrievePrintNonStatistics() {
        layout.setPrintNonStatistics(true);
        assertTrue(layout.isPrintNonStatistics());
    }

    // --- activateOptions ---

    @Test
    public void shouldActivateOptionsAndBuildFormatter() {
        assertNull(layout.csvFormatter);
        layout.activateOptions();
        assertNotNull(layout.csvFormatter);
    }

    @Test
    public void shouldActivateOptionsWithPivotMode() {
        layout.setPivot(true);
        layout.setColumns("start,stop,tagMean");
        layout.activateOptions();
        assertNotNull(layout.csvFormatter);
    }

    // --- format ---

    @Test
    public void shouldFormatGroupedTimingStatistics() {
        layout.activateOptions();
        GroupedTimingStatistics stats = createGroupedTimingStatistics("dbCall", 100, 20);
        Log4jLogEvent event = createLog4jLogEvent(new GroupedTimingStatisticsMessage(stats));
        String result = layout.format(event);
        assertNotNull(result);
        assertTrue("CSV output should contain tag name", result.contains("dbCall"));
    }

    @Test
    public void shouldFormatGroupedTimingStatisticsWithMultipleTags() {
        layout.activateOptions();
        GroupedTimingStatistics stats = createGroupedTimingStatistics("tagA", 50, 10);
        stats.getStatisticsByTag().put("tagB", new TimingStatistics(200, 30, 230, 170, 10));
        Log4jLogEvent event = createLog4jLogEvent(new GroupedTimingStatisticsMessage(stats));
        String result = layout.format(event);
        assertNotNull(result);
        assertTrue(result.contains("tagA"));
        assertTrue(result.contains("tagB"));
    }

    @Test
    public void shouldReturnEscapedStringWhenPrintNonStatisticsEnabled() {
        layout.setPrintNonStatistics(true);
        layout.activateOptions();
        Log4jLogEvent event = createLog4jLogEventWithString("plain message");
        String result = layout.format(event);
        assertNotNull(result);
        assertTrue("should contain the plain message text", result.contains("plain message"));
    }

    @Test
    public void shouldReturnEmptyStringWhenPrintNonStatisticsDisabled() {
        layout.setPrintNonStatistics(false);
        layout.activateOptions();
        Log4jLogEvent event = createLog4jLogEventWithString("plain message");
        String result = layout.format(event);
        assertEquals("", result);
    }

    @Test
    public void shouldReturnEmptyStringForNonStatisticsByDefault() {
        layout.activateOptions();
        Log4jLogEvent event = createLog4jLogEventWithString("irrelevant");
        String result = layout.format(event);
        assertEquals("", result);
    }

    // --- ignoresThrowable ---

    @Test
    public void shouldReturnTrueForIgnoresThrowable() {
        assertTrue(layout.ignoresThrowable());
    }

    // --- toSerializable ---

    @Test
    public void shouldReturnNullForToSerializable() {
        Log4jLogEvent event = createLog4jLogEventWithString("any");
        assertNull(layout.toSerializable(event));
    }

    // --- pivot format ---

    @Test
    public void shouldFormatInPivotMode() {
        layout.setPivot(true);
        layout.setColumns("start,stop");
        layout.activateOptions();
        GroupedTimingStatistics stats = createGroupedTimingStatistics("tag", 100, 20);
        Log4jLogEvent event = createLog4jLogEvent(new GroupedTimingStatisticsMessage(stats));
        String result = layout.format(event);
        assertNotNull(result);
        // pivot mode produces a single line with start/stop columns
        assertFalse("pivot output should not be empty", result.isEmpty());
    }

    // --- helpers ---

    private static GroupedTimingStatistics createGroupedTimingStatistics(String tag, long mean, long stddev) {
        SortedMap<String, TimingStatistics> map = new TreeMap<>();
        map.put(tag, new TimingStatistics(mean, stddev, mean + 10, Math.max(0, mean - 10), 5));
        long now = System.currentTimeMillis();
        return new GroupedTimingStatistics(map, now - 1000, now, false);
    }

    /**
     * A {@link GroupedTimingStatistics} that also implements {@link Message}
     * so that it can be passed as the message of a {@link Log4jLogEvent}.
     */
    private static class GroupedTimingStatisticsMessage extends GroupedTimingStatistics implements Message {
        GroupedTimingStatisticsMessage(GroupedTimingStatistics delegate) {
            super(delegate.getStatisticsByTag(), delegate.getStartTime(), delegate.getStopTime(),
                    delegate.isCreateRollupStatistics());
        }

        @Override
        public String getFormattedMessage() {
            return toString();
        }

        @Override
        public String getFormat() {
            return null;
        }

        @Override
        public Object[] getParameters() {
            return new Object[0];
        }

        @Override
        public Throwable getThrowable() {
            return null;
        }
    }

    private static Log4jLogEvent createLog4jLogEvent(Message message) {
        return new Log4jLogEvent("testLogger", null, "fqcn", Level.INFO, message, (Throwable) null);
    }

    private static Log4jLogEvent createLog4jLogEventWithString(String text) {
        return new Log4jLogEvent("testLogger", null, "fqcn", Level.INFO, new SimpleMessage(text), (Throwable) null);
    }
}
