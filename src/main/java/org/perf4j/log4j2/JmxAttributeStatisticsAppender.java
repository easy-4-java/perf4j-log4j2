package org.perf4j.log4j2;


import java.io.Serializable;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.perf4j.GroupedTimingStatistics;
import org.perf4j.helpers.AcceptableRangeConfiguration;
import org.perf4j.helpers.MiscUtils;
import org.perf4j.helpers.StatisticsExposingMBean;

/**
 * Log4j 2.x {@link Appender} that consumes {@link GroupedTimingStatistics} log
 * messages and pushes the rolled-up statistics into a JMX MBean so external
 * monitoring tools can observe mean/min/max/stddev/count/TPS values per tag.
 *
 * <p>This appender is designed to be attached downstream of an
 * {@link AsyncCoalescingStatisticsAppender}: the upstream appender coalesces
 * stop-watch logs into a {@link GroupedTimingStatistics} on a periodic time
 * slice, and this appender merely forwards those groups to the
 * {@link StatisticsExposingMBean} it manages.</p>
 *
 * <p>In addition to the periodic attribute updates, the appender can be
 * configured with <i>notification thresholds</i> (described under
 * {@link #getNotificationThresholds()}) so that a JMX notification is fired when
 * a tagged statistic falls outside of an acceptable range (e.g. the mean
 * execution time for a critical code path exceeds a budget).</p>
 *
 * <p>Configuration knobs:</p>
 * <ul>
 *   <li><b>name</b> &mdash; the appender name (Log4j 2 required).</li>
 *   <li><b>tagNamesToExpose</b> &mdash; comma-separated tag names whose
 *       statistics should be exposed as JMX attributes.</li>
 *   <li><b>mBeanName</b> &mdash; the JMX {@link ObjectName} under which the
 *       underlying MBean is registered. Defaults to
 *       {@link StatisticsExposingMBean#DEFAULT_MBEAN_NAME}.</li>
 *   <li><b>notificationThresholds</b> &mdash; comma-separated
 *       {@link AcceptableRangeConfiguration} strings.</li>
 * </ul>
 *
 * @author [@Loong Wan](https://github.com/loong10k)
 * @since 3.0.0
 * @see StatisticsExposingMBean
 * @see AcceptableRangeConfiguration
 * @see GroupedTimingStatistics
 */
@Plugin(name = "JmxAttributes", category = Node.CATEGORY, elementType = Appender.ELEMENT_TYPE, printObject = true)
public class JmxAttributeStatisticsAppender extends AbstractAppender {

    /**
     * Log4j 2 plugin factory used to instantiate this appender from a
     * configuration file.
     *
     * @param layout           the optional layout applied to the log events. May be {@code null}.
     * @param filter           the optional filter applied to accepted events. May be {@code null}.
     * @param name             the appender name. Required by Log4j 2.
     * @param ignoreExceptions whether exceptions during appending should be ignored.
     * @return a fully constructed {@link JmxAttributeStatisticsAppender}.
     */
    @PluginFactory
    public static JmxAttributeStatisticsAppender createAppender(
            @PluginElement("Layout") final Layout<? extends Serializable> layout,
            @PluginElement("Filter") final Filter filter,
            @PluginAttribute("name") final String name,
            @PluginAttribute(value = "ignoreExceptions", defaultBoolean = true) final boolean ignoreExceptions) {
        return new JmxAttributeStatisticsAppender(name, filter, layout, ignoreExceptions);
    }

    /**
     * Constructs a new appender that surfaces exceptions during the append
     * lifecycle (the legacy Log4j 2 constructor compatibility form).
     *
     * @param name   the appender name.
     * @param filter the optional filter applied to events. May be {@code null}.
     * @param layout the optional layout. May be {@code null}.
     */
    protected JmxAttributeStatisticsAppender(String name, Filter filter,
			Layout<? extends Serializable> layout) {
		super(name, filter, layout);
	}

    /**
     * Constructs a new appender with explicit control over exception handling.
     *
     * @param name             the appender name.
     * @param filter           the optional filter applied to events. May be {@code null}.
     * @param layout           the optional layout. May be {@code null}.
     * @param ignoreExceptions whether exceptions during appending should be ignored.
     */
    protected JmxAttributeStatisticsAppender(String name, Filter filter,
			Layout<? extends Serializable> layout, boolean ignoreExceptions) {
		super(name, filter, layout, ignoreExceptions);
	}


	// --- configuration options ---
    /**
     * The JMX {@link ObjectName} under which the {@link StatisticsExposingMBean}
     * is registered. Defaults to {@link StatisticsExposingMBean#DEFAULT_MBEAN_NAME}.
     */
    private String mBeanName = StatisticsExposingMBean.DEFAULT_MBEAN_NAME;
    /**
     * Comma-separated list of tag names whose statistics should be exposed as
     * JMX attributes (e.g. {@code "databaseCall,fileWrite"}). Required: must be
     * supplied before {@link #activateOptions()} is called.
     */
    private String tagNamesToExpose;
    /**
     * Comma-separated list of notification thresholds in
     * {@link AcceptableRangeConfiguration} syntax. May be {@code null} to
     * disable notifications.
     */
    private String notificationThresholds;

    // --- state variables ---
    /**
     * The live MBean that is registered with the platform MBean server. Built
     * during {@link #activateOptions()} and updated on every {@link LogEvent}.
     */
    protected StatisticsExposingMBean mBean;

    // --- options ---
    /**
     * Returns the configured JMX {@link ObjectName} for the statistics MBean.
     *
     * @return the JMX object name. Defaults to
     *         {@link StatisticsExposingMBean#DEFAULT_MBEAN_NAME}.
     */
    public String getMBeanName() {
        return mBeanName;
    }

    /**
     * Sets the JMX {@link ObjectName} used when registering the
     * {@link StatisticsExposingMBean}.
     *
     * @param mBeanName the new JMX object name. Must be a syntactically valid
     *                  {@link ObjectName}; validation occurs in
     *                  {@link #activateOptions()}.
     */
    public void setMBeanName(String mBeanName) {
        this.mBeanName = mBeanName;
    }

    /**
     * Returns the comma-separated list of tag names whose statistics are
     * exposed as JMX attributes.
     *
     * @return the configured tag names, or {@code null} if not yet set.
     */
    public String getTagNamesToExpose() {
        return tagNamesToExpose;
    }

    /**
     * Sets the comma-separated list of tag names whose statistics should be
     * exposed as JMX attributes.
     *
     * @param tagNamesToExpose the comma-separated list of tag names. Required
     *                         before {@link #activateOptions()} is called.
     */
    public void setTagNamesToExpose(String tagNamesToExpose) {
        this.tagNamesToExpose = tagNamesToExpose;
    }

    /**
     * Returns the configured notification thresholds.
     *
     * <p>An example configuration:</p>
     * <pre>databaseCallMean(&lt;100),databaseCallMax(&lt;1000),fileWriteMean(5-200),fileWriteTPS(&gt;1)</pre>
     *
     * @return the notification threshold configuration, or {@code null} if no
     *         thresholds are configured.
     */
    public String getNotificationThresholds() {
        return notificationThresholds;
    }

    /**
     * Sets the notification thresholds as a comma-separated list of
     * {@link AcceptableRangeConfiguration} expressions.
     *
     * @param notificationThresholds the threshold expressions, or {@code null}
     *                               to disable notifications.
     */
    public void setNotificationThresholds(String notificationThresholds) {
        this.notificationThresholds = notificationThresholds;
    }

    /**
     * Validates the configuration, builds the {@link StatisticsExposingMBean},
     * and registers it against the platform MBean server.
     *
     * @throws RuntimeException if {@link #tagNamesToExpose} is {@code null}, or
     *                          if the MBean cannot be registered with the JMX
     *                          server (for example because the configured
     *                          {@link #mBeanName} is not a valid
     *                          {@link ObjectName}).
     */
    public void activateOptions() {
        if (tagNamesToExpose == null) {
            throw new RuntimeException("You must set the TagNamesToExpose option before activating this appender");
        }

        //parse the options, create the mBean and register it
        String[] tagNames = MiscUtils.splitAndTrim(tagNamesToExpose, ",");

        List<AcceptableRangeConfiguration> rangeConfigs = new ArrayList<AcceptableRangeConfiguration>();
        if (notificationThresholds != null) {
            String[] rangeConfigStrings = MiscUtils.splitAndTrim(notificationThresholds, ",");
            for (String rangeConfigString : rangeConfigStrings) {
                rangeConfigs.add(new AcceptableRangeConfiguration(rangeConfigString));
            }
        }

        mBean = new StatisticsExposingMBean(mBeanName, Arrays.asList(tagNames), rangeConfigs);

        try {
            MBeanServer mBeanServer = getMBeanServer();
            mBeanServer.registerMBean(mBean, new ObjectName(mBeanName));
        } catch (Exception e) {
            throw new RuntimeException("Error registering statistics MBean: " + e.getMessage(), e);
        }
    }

    // --- appender interface methods ---

    /**
     * Forwards each accepted {@link LogEvent} to the underlying MBean. Events
     * whose message is not a {@link GroupedTimingStatistics} (or for which the
     * MBean has not yet been activated) are silently ignored.
     *
     * @param event the log event emitted by the Log4j 2 runtime.
     */
    public void append(LogEvent event) {
        Object logMessage = event.getMessage();
        if (logMessage instanceof GroupedTimingStatistics && mBean != null) {
            mBean.updateCurrentTimingStatistics((GroupedTimingStatistics) logMessage);
        }
    }

    /**
     * Indicates that this appender does not require a layout to function.
     *
     * @return {@code false} &mdash; the appender is layout-agnostic.
     */
    public boolean requiresLayout() {
        return false;
    }

    /**
     * Unregisters the underlying MBean from the platform MBean server. Errors
     * during unregistration are intentionally swallowed &mdash; the MBean may
     * already be gone during container shutdown.
     */
    public void close() {
        try {
            MBeanServer mBeanServer = getMBeanServer();
            mBeanServer.unregisterMBean(new ObjectName(mBeanName));
        } catch (Exception e) {
            //fine, if we can't unregister it's not a big deal
        }
    }

    // --- helper methods ---
    /**
     * Returns the MBean server against which the statistics MBean is registered.
     * Defaults to the platform MBean server but subclasses may override to use
     * a custom server.
     *
     * @return the MBean server used for registrations.
     */
    protected MBeanServer getMBeanServer() {
        return ManagementFactory.getPlatformMBeanServer();
    }


}
