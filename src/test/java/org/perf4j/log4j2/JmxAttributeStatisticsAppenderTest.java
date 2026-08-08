package org.perf4j.log4j2;

import java.io.Serializable;
import java.lang.management.ManagementFactory;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.message.Message;
import org.junit.After;
import org.junit.Test;
import org.perf4j.GroupedTimingStatistics;
import org.perf4j.TimingStatistics;
import org.perf4j.helpers.StatisticsExposingMBean;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for {@link JmxAttributeStatisticsAppender}.
 *
 * @since 3.0.0
 */
public class JmxAttributeStatisticsAppenderTest {

    private static final String UNIQUE_MBEAN_PREFIX = "org.perf4j.test:type=TestStats,id=";

    private JmxAttributeStatisticsAppender appender;

    @After
    public void tearDown() {
        if (appender != null) {
            try {
                appender.close();
            } catch (Exception ignored) {
                // cleanup best-effort
            }
        }
    }

    // --- createAppender / factory ---

    @Test
    public void shouldCreateAppenderViaPluginFactory() {
        JmxAttributeStatisticsAppender created = JmxAttributeStatisticsAppender.createAppender(null, null, "myAppender", true);
        assertNotNull(created);
        assertEquals("myAppender", created.getName());
        appender = created;
    }

    @Test
    public void shouldCreateAppenderWithDefaultIgnoreExceptions() {
        appender = JmxAttributeStatisticsAppender.createAppender(null, null, "defaultIgnore", false);
        assertNotNull(appender);
    }

    // --- constructors ---

    @Test
    public void shouldConstructWithThreeArgs() {
        appender = new JmxAttributeStatisticsAppender("threeArg", null, null);
        assertEquals("threeArg", appender.getName());
    }

    @Test
    public void shouldConstructWithFourArgs() {
        appender = new JmxAttributeStatisticsAppender("fourArg", null, null, true);
        assertEquals("fourArg", appender.getName());
    }

    @Test
    public void shouldConstructWithFourArgsIgnoringExceptions() {
        appender = new JmxAttributeStatisticsAppender("fourArgFalse", null, null, false);
        assertEquals("fourArgFalse", appender.getName());
    }

    // --- mBeanName getter / setter ---

    @Test
    public void shouldDefaultMBeanNameToStatisticsExposingMBeanDefault() {
        appender = new JmxAttributeStatisticsAppender("mbeanDefault", null, null);
        assertEquals(StatisticsExposingMBean.DEFAULT_MBEAN_NAME, appender.getMBeanName());
    }

    @Test
    public void shouldSetAndRetrieveMBeanName() {
        appender = new JmxAttributeStatisticsAppender("mbeanSet", null, null);
        String custom = "org.perf4j.custom:type=Custom";
        appender.setMBeanName(custom);
        assertEquals(custom, appender.getMBeanName());
    }

    // --- tagNamesToExpose getter / setter ---

    @Test
    public void shouldDefaultTagNamesToExposeToNull() {
        appender = new JmxAttributeStatisticsAppender("tagDefault", null, null);
        assertNull(appender.getTagNamesToExpose());
    }

    @Test
    public void shouldSetAndRetrieveTagNamesToExpose() {
        appender = new JmxAttributeStatisticsAppender("tagSet", null, null);
        appender.setTagNamesToExpose("dbCall,fileWrite");
        assertEquals("dbCall,fileWrite", appender.getTagNamesToExpose());
    }

    // --- notificationThresholds getter / setter ---

    @Test
    public void shouldDefaultNotificationThresholdsToNull() {
        appender = new JmxAttributeStatisticsAppender("notifDefault", null, null);
        assertNull(appender.getNotificationThresholds());
    }

    @Test
    public void shouldSetAndRetrieveNotificationThresholds() {
        appender = new JmxAttributeStatisticsAppender("notifSet", null, null);
        appender.setNotificationThresholds("dbCallMean(<100),dbCallMax(<1000)");
        assertEquals("dbCallMean(<100),dbCallMax(<1000)", appender.getNotificationThresholds());
    }

    // --- activateOptions ---

    @Test
    public void shouldActivateOptionsSuccessfullyWithTags() {
        appender = new JmxAttributeStatisticsAppender("activateOk", null, null);
        String mbeanName = UNIQUE_MBEAN_PREFIX + System.nanoTime();
        appender.setMBeanName(mbeanName);
        appender.setTagNamesToExpose("dbCall,fileWrite");
        appender.activateOptions();
        assertNotNull(appender.mBean);
    }

    @Test
    public void shouldActivateOptionsWithNotificationThresholds() {
        appender = new JmxAttributeStatisticsAppender("activateWithNotif", null, null);
        String mbeanName = UNIQUE_MBEAN_PREFIX + System.nanoTime();
        appender.setMBeanName(mbeanName);
        appender.setTagNamesToExpose("dbCall");
        appender.setNotificationThresholds("dbCallMean(<100)");
        appender.activateOptions();
        assertNotNull(appender.mBean);
    }

    @Test(expected = RuntimeException.class)
    public void shouldThrowWhenTagNamesToExposeIsNullDuringActivation() {
        appender = new JmxAttributeStatisticsAppender("activateNull", null, null);
        appender.activateOptions();
    }

    // --- append ---

    @Test
    public void shouldAppendGroupedTimingStatisticsEvent() {
        appender = new JmxAttributeStatisticsAppender("appendGts", null, null);
        String mbeanName = UNIQUE_MBEAN_PREFIX + System.nanoTime();
        appender.setMBeanName(mbeanName);
        appender.setTagNamesToExpose("dbCall");
        appender.activateOptions();

        GroupedTimingStatistics stats = createGroupedTimingStatistics("dbCall", 100, 50);
        LogEvent event = createLogEventWithMessage(new GroupedTimingStatisticsMessage(stats));
        appender.append(event);
        // no exception means success; the MBean received the update
    }

    @Test
    public void shouldIgnoreNonGroupedTimingStatisticsEvent() {
        appender = new JmxAttributeStatisticsAppender("appendNonGts", null, null);
        String mbeanName = UNIQUE_MBEAN_PREFIX + System.nanoTime();
        appender.setMBeanName(mbeanName);
        appender.setTagNamesToExpose("dbCall");
        appender.activateOptions();

        LogEvent event = createLogEventWithStringMessage("just a plain log message");
        appender.append(event);
        // no exception means the non-GTS event was silently ignored
    }

    @Test
    public void shouldIgnoreEventWhenMBeanIsNull() {
        appender = new JmxAttributeStatisticsAppender("appendNull", null, null);
        // do NOT activateOptions — mBean remains null
        GroupedTimingStatistics stats = createGroupedTimingStatistics("tag", 10, 5);
        LogEvent event = createLogEventWithMessage(new GroupedTimingStatisticsMessage(stats));
        appender.append(event);
        // no exception means the event was silently ignored
    }

    // --- requiresLayout ---

    @Test
    public void shouldReturnFalseForRequiresLayout() {
        appender = new JmxAttributeStatisticsAppender("noLayout", null, null);
        assertFalse(appender.requiresLayout());
    }

    // --- close ---

    @Test
    public void shouldCloseAfterActivation() throws Exception {
        appender = new JmxAttributeStatisticsAppender("closeOk", null, null);
        String mbeanName = UNIQUE_MBEAN_PREFIX + System.nanoTime();
        appender.setMBeanName(mbeanName);
        appender.setTagNamesToExpose("dbCall");
        appender.activateOptions();
        assertNotNull(appender.mBean);

        appender.close();
        // After close, the MBean should be unregistered — verify via MBeanServer
        MBeanServer server = ManagementFactory.getPlatformMBeanServer();
        assertFalse(server.isRegistered(new ObjectName(mbeanName)));
        appender = null; // prevent double-close in tearDown
    }

    @Test
    public void shouldSwallowExceptionDuringCloseWhenNotActivated() {
        appender = new JmxAttributeStatisticsAppender("closeNoActivate", null, null);
        // close() without prior activateOptions — should not throw
        appender.close();
        appender = null;
    }

    // --- getMBeanServer ---

    @Test
    public void shouldReturnPlatformMBeanServer() {
        appender = new JmxAttributeStatisticsAppender("mbeanServer", null, null);
        MBeanServer server = appender.getMBeanServer();
        assertNotNull(server);
        assertEquals(ManagementFactory.getPlatformMBeanServer(), server);
    }

    // --- round-trip: activate, append, close ---

    @Test
    public void shouldFullLifecycleActivateAppendClose() {
        appender = new JmxAttributeStatisticsAppender("lifecycle", null, null);
        String mbeanName = UNIQUE_MBEAN_PREFIX + System.nanoTime();
        appender.setMBeanName(mbeanName);
        appender.setTagNamesToExpose("tagA,tagB");
        appender.setNotificationThresholds("tagAMean(<500)");
        appender.activateOptions();

        GroupedTimingStatistics stats = createGroupedTimingStatistics("tagA", 200, 30);
        appender.append(createLogEventWithMessage(new GroupedTimingStatisticsMessage(stats)));

        appender.close();
        appender = null; // prevent double-close in tearDown
    }

    // --- helpers ---

    /**
     * Creates a {@link GroupedTimingStatistics} with a single tag entry.
     */
    private static GroupedTimingStatistics createGroupedTimingStatistics(String tag, long mean, long stddev) {
        SortedMap<String, TimingStatistics> map = new TreeMap<>();
        map.put(tag, new TimingStatistics(mean, stddev, mean + 10, mean - 10, 5));
        return new GroupedTimingStatistics(map, System.currentTimeMillis() - 1000, System.currentTimeMillis(), false);
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

    private static LogEvent createLogEventWithMessage(Message message) {
        return new Log4jLogEvent("testLogger", null, "fqcn", Level.INFO, message, null);
    }

    private static LogEvent createLogEventWithStringMessage(String text) {
        return new Log4jLogEvent("testLogger", null, "fqcn", Level.INFO,
                new org.apache.logging.log4j.message.SimpleMessage(text), null);
    }
}
