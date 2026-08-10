package org.perf4j.log4j2.aop;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.aspectj.lang.annotation.Aspect;
import org.perf4j.aop.AbstractTimingAspect;
import org.perf4j.log4j2.Log4J2StopWatch;

/**
 * Log4j 2.x-backed AspectJ timing aspect. Extends the generic
 * {@link AbstractTimingAspect} and wires the {@link Log4J2StopWatch} factory
 * so that the aspect persists its timing statements to Log4j 2.
 *
 * <p>Typical usage is to attach this aspect to methods annotated with
 * {@link org.perf4j.aop.Profiled} via the AspectJ pointcut expression
 * {@code @within(org.perf4j.aop.Profiled)} or equivalent.</p>
 *
 * @author Alex Devine
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 3.0.0
 * @see AbstractTimingAspect
 * @see Log4J2StopWatch
 * @see EjbTimingAspect
 */
@Aspect
public class TimingAspect extends AbstractTimingAspect {

    /**
     * Constructs a {@link Log4J2StopWatch} using a logger resolved from
     * {@link LogManager} with the supplied name. The supplied level name is
     * coerced to a {@link Level} via {@link Level#toLevel(String, Level)};
     * unknown levels fall back to {@link Level#INFO}.
     *
     * @param loggerName the name of the Log4j 2 logger to use.
     * @param levelName  the textual level to log at. When {@code null} or
     *                   unrecognized, {@link Level#INFO} is used.
     * @return a stop watch ready for use by the surrounding join point.
     */
    protected Log4J2StopWatch newStopWatch(String loggerName, String levelName) {
        Level level = Level.toLevel(levelName, Level.INFO);
        return new Log4J2StopWatch(LogManager.getLogger(loggerName), level, level);
    }

}
