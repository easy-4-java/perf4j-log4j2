package org.perf4j.log4j2.aop;


import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.perf4j.aop.AbstractEjbTimingAspect;
import org.perf4j.log4j2.Log4J2StopWatch;

/**
 * Log4j 2.x-backed EJB {@code @Interceptors} timing aspect. Wires the generic
 * {@link AbstractEjbTimingAspect} to the Log4j 2 logging backend by
 * constructing {@link Log4J2StopWatch} instances.
 *
 * <p>To use this aspect in client code, reference this class from the
 * {@link javax.interceptor.Interceptors} annotation on the EJB
 * (or other Jakarta EE interceptor target) that should be profiled.</p>
 *
 * @author Alex Devine
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 3.0.0
 * @see AbstractEjbTimingAspect
 * @see Log4J2StopWatch
 * @see TimingAspect
 */
public class EjbTimingAspect extends AbstractEjbTimingAspect {

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
