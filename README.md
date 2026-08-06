# perf4j-log4j2

[English](./README.md) | [简体中文](./README.zh-CN.md)

## Table of Contents

- [1. Project Overview](#1-project-overview)
- [2. Features & Status](#2-features--status)
- [3. Requirements & Compatibility](#3-requirements--compatibility)
- [4. Architecture & Modules](#4-architecture--modules)
- [5. Installation](#5-installation)
- [6. Quick Start](#6-quick-start)
- [7. Configuration](#7-configuration)
- [8. Core Usage / API](#8-core-usage--api)
- [9. Testing & Build](#9-testing--build)
- [10. Versioning & Branches](#10-versioning--branches)
- [11. Contributing & License](#11-contributing--license)

## 1. Project Overview

**perf4j-log4j2** ports the [Perf4J](http://perf4j.codehaus.org) timing library to **Log4j 2.x**. The original
Perf4J only supports Log4j 1.x / logback / java.util.logging; this extension provides a Log4j 2.x stop watch,
a JMX statistics appender and a CSV statistics layout, plus the AOP/EJB timing aspects.

It is a library, not an application: you embed the stop watch in your code, then route the emitted timing
messages to the provided appender/layout for aggregation, CSV export or JMX monitoring.

| Is                                             | Is not                                  |
| :--------------------------------------------- | :-------------------------------------- |
| Log4j 2.x implementation of Perf4J components  | A metrics dashboard or alerting service |
| Stop-watch, JMX appender, CSV layout, aspects  | A replacement for Micrometer/Prometheus clients |
| Compatible with Perf4J 0.9.16 concepts         | A log4j 1.x bridge                      |

Typical scenarios:

| Scenario                     | Description                                                      |
| :--------------------------- | :---------------------------------------------------------------- |
| Method timing in logs        | `@Profiled` / manual `Log4J2StopWatch` blocks                    |
| JMX-based monitoring         | Expose per-tag mean/min/max/count/TPS as MBean attributes        |
| CSV reporting                | Export `GroupedTimingStatistics` windows as comma-separated rows |

## 2. Features & Status

| Capability                                        | Status      | Notes                                                              |
| :------------------------------------------------ | :---------- | :----------------------------------------------------------------- |
| `Log4J2StopWatch` (LoggingStopWatch for log4j2)   | Implemented | Default logger `org.perf4j.TimingLogger`; INFO normal / WARN on exception; fluent setters; serialization-safe |
| `JmxAttributeStatisticsAppender`                  | Implemented | log4j2 plugin `JmxAttributes`; registers `StatisticsExposingMBean`; optional notification thresholds |
| `StatisticsCsvLayout`                             | Implemented | CSV output of `GroupedTimingStatistics`; `Pivot` / `Columns` / `PrintNonStatistics` options |
| `aop.TimingAspect` (AspectJ)                      | Implemented | `@Aspect` class; creates `Log4J2StopWatch` per `@Profiled` join point |
| `aop.EjbTimingAspect` (EJB interceptor)           | Implemented | Perf4J `AbstractEjbTimingAspect` wired to log4j2                    |
| Unit tests                                        | Not present | No `src/test` sources on this branch                               |

## 3. Requirements & Compatibility

| Requirement   | Version                |
| :------------ | :--------------------- |
| JDK           | 8+                     |
| Maven         | 3.0+ (wrapper included)|
| Log4j 2.x     | 2.24.x (`log4j-core`)  |
| Perf4J        | 0.9.16                 |
| AspectJ       | 1.8.9 (`aspectjrt`)    |

Version lines of the easy4j project:

| Branch        | JDK  | Version pattern | Notes                       |
| :------------ | :--- | :-------------- | :-------------------------- |
| `feature/1.0.x` | 8    | `1.0.x.*`       | This README, current branch |
| `feature/2.0.x` | 17   | `2.0.x.*`       | JDK 17 line                 |
| `feature/3.0.x` | 21   | `3.0.x.*`       | JDK 21 line                 |

## 4. Architecture & Modules

```text
   @Profiled / manual timing
              |
              v
   Log4J2StopWatch  (org.perf4j.log4j2)
              |
              v
  logger "org.perf4j.TimingLogger" (log4j2)
              |
      +-------+--------+
      |                |
      v                v
 StatisticsCsvLayout  JmxAttributeStatisticsAppender
 (CSV rows)          (StatisticsExposingMBean)
      |                |
      v                v
   files / stdout   JMX console / alerts
```

Single-module Maven project (`jar` packaging):

| Package                 | Responsibility                                       |
| :---------------------- | :--------------------------------------------------- |
| `org.perf4j.log4j2`     | Stop watch, JMX appender, CSV layout                 |
| `org.perf4j.log4j2.aop` | AspectJ and EJB timing aspects                       |

## 5. Installation

Artifacts are published to the aliyun repository and GitHub Releases; they are **not** on Maven Central yet.

```xml
<dependency>
    <groupId>io.github.easy4j</groupId>
    <artifactId>perf4j-log4j2</artifactId>
    <version>1.0.x.20260630-SNAPSHOT</version>
</dependency>
```

```groovy
implementation 'io.github.easy4j:perf4j-log4j2:1.0.x.20260630-SNAPSHOT'
```

## 6. Quick Start

```java
import org.perf4j.log4j2.Log4J2StopWatch;

public class OrderService {

    public void createOrder(String orderId) {
        Log4J2StopWatch stopWatch = new Log4J2StopWatch("orderService.createOrder");
        try {
            // ... business logic ...
        } finally {
            stopWatch.stop();
        }
    }
}
```

Expected result: with the `org.perf4j.TimingLogger` logger enabled at INFO level, each `stop()` writes a
Perf4J timing line (e.g. `start[1234567890123] time[42] tag[orderService.createOrder]`) through log4j2.
Exception-aware `stop(Throwable)` / `lap(Throwable)` calls are logged at WARN instead.

## 7. Configuration

There is no properties file; wire it through `log4j2.xml`. Minimal configuration:

```xml
<Configuration>
    <Appenders>
        <JmxAttributes name="JmxAttributes"/>
        <Console name="Console"/>
    </Appenders>
    <Loggers>
        <Logger name="org.perf4j.TimingLogger" level="INFO" additivity="false">
            <AppenderRef ref="JmxAttributes"/>
            <AppenderRef ref="Console"/>
        </Logger>
    </Loggers>
</Configuration>
```

Notes:

- `JmxAttributes` resolves through the log4j2 `@Plugin` annotation on `JmxAttributeStatisticsAppender`.
- The tuning options (`MBeanName`, `TagNamesToExpose`, `NotificationThresholds`) are JavaBean properties; the
  current `@PluginFactory` only exposes `name` and `ignoreExceptions`, so those options must be set
  programmatically after construction — XML binding of the options would require extending the plugin factory
  (**Assumption**).
- `StatisticsCsvLayout` is not annotated as a log4j2 plugin; use it programmatically (see Section 8).

## 8. Core Usage / API

Programmatic JMX + CSV wiring:

```java
// 1) CSV layout over GroupedTimingStatistics
StatisticsCsvLayout csvLayout = StatisticsCsvLayout.createDefaultLayout();
csvLayout.setColumns("tag,start,stop,mean,min,max,stddev,count,tps");

// 2) JMX appender — options are JavaBean properties
JmxAttributeStatisticsAppender jmxAppender =
        new JmxAttributeStatisticsAppender("JmxAttributes", null, null);
jmxAppender.setTagNamesToExpose("orderService.createOrder");
jmxAppender.setNotificationThresholds("orderService.createOrderMean(<1000)");
jmxAppender.activateOptions();
```

- `setTagNamesToExpose("a,b")` exposes `aMean`, `aStdDev`, `aMin`, `aMax`, `aCount`, `aTPS`, ... as MBean
  attributes; `setNotificationThresholds("aMean(<100),bTPS(>1)")` sends JMX notifications when values leave
  the acceptable range (`<value`, `>value`, `min-max`).
- The MBean default object name is `org.perf4j:type=StatisticsExposingMBean,name=Perf4J`.

AOP usage (AspectJ):

```java
import org.perf4j.log4j2.aop.TimingAspect;

@Aspect
public class ServiceTimingAspect extends TimingAspect {
}
```

```java
import org.perf4j.annotation.Profiled;

public class OrderService {
    @Profiled(tag = "orderService.createOrder")
    public void createOrder(String orderId) { /* ... */ }
}
```

EJB usage: annotate the EJB with `@Interceptors(EjbTimingAspect.class)` (Perf4J `@Profiled` convention).

## 9. Testing & Build

```bash
./mvnw clean verify
```

- Maven wrapper (`mvnw`) is committed to the repository.
- JaCoCo is configured with a line-coverage rule of 90% (`haltOnFailure=false`).
- There are no unit tests on this branch — the coverage gate is not effectively enforced yet (known gap);
  tests for the stop watch and the appender/layout are the most valuable contributions.

## 10. Versioning & Branches

| Branch        | JDK  | Version pattern | Maintenance                          |
| :------------ | :--- | :-------------- | :----------------------------------- |
| `feature/1.0.x` | 8    | `1.0.x.*`       | Current branch                       |
| `feature/2.0.x` | 17   | `2.0.x.*`       | JDK 17 line                          |
| `feature/3.0.x` | 21   | `3.0.x.*`       | JDK 21 line                          |

Artifacts are distributed via the aliyun Maven repository and GitHub Releases. Use the branch matching your
JDK baseline.

## 11. Contributing & License

Contributions are welcome — especially unit tests and a `@Plugin`-annotated factory for the CSV layout.
Please open an issue before larger changes.

This project is licensed under the [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0).
