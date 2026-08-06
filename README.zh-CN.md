# perf4j-log4j2

[English](./README.md) | [简体中文](./README.zh-CN.md)

![Java](https://img.shields.io/badge/Java-8-orange)
![License](https://img.shields.io/badge/License-Apache%202.0-blue)

## 目录

- [1. 项目概述](#1-项目概述)
- [2. 功能与状态](#2-功能与状态)
- [3. 环境要求与兼容性](#3-环境要求与兼容性)
- [4. 架构与模块](#4-架构与模块)
- [5. 安装](#5-安装)
- [6. 快速开始](#6-快速开始)
- [7. 配置](#7-配置)
- [8. 核心用法 / API](#8-核心用法--api)
- [9. 测试与构建](#9-测试与构建)
- [10. 版本线与分支](#10-版本线与分支)
- [11. 贡献与许可](#11-贡献与许可)

---

## 1. 项目概述

**perf4j-log4j2** 将 [Perf4J](http://perf4j.codehaus.org) 计时库移植到 **Log4j 2.x**。原版 Perf4J 仅支持 Log4j 1.x / logback / java.util.logging；本扩展提供了基于 Log4j 2.x 的计时秒表、JMX 统计 Appender、CSV 统计 Layout，以及 AOP/EJB 计时切面。

它是一个库而非应用：在业务代码中嵌入秒表，将产生的计时消息交给本组件提供的 Appender / Layout 进行聚合、CSV 导出或 JMX 监控。

| 是                                             | 不是                                  |
| :--------------------------------------------- | :------------------------------------ |
| Perf4J 组件的 Log4j 2.x 实现                    | 指标看板或告警服务                     |
| 秒表、JMX Appender、CSV Layout、切面            | Micrometer / Prometheus 客户端的替代品 |
| 与 Perf4J 0.9.16 的概念兼容                     | log4j 1.x 桥接                        |

典型场景：

| 场景                   | 说明                                                          |
| :--------------------- | :------------------------------------------------------------ |
| 方法级计时日志         | `@Profiled` 注解或手动 `Log4J2StopWatch` 代码块               |
| JMX 监控               | 按 tag 暴露 mean/min/max/count/TPS 等 MBean 属性              |
| CSV 报表               | 将 `GroupedTimingStatistics` 统计窗口导出为逗号分隔行          |

## 2. 功能与状态

| 能力                                                    | 状态       | 说明                                                                  |
| :------------------------------------------------------ | :--------- | :-------------------------------------------------------------------- |
| `Log4J2StopWatch`（面向 log4j2 的 LoggingStopWatch）     | 已实现     | 默认 Logger `org.perf4j.TimingLogger`；正常 INFO / 异常 WARN；链式 setter；支持序列化 |
| `JmxAttributeStatisticsAppender`                        | 已实现     | log4j2 插件 `JmxAttributes`；注册 `StatisticsExposingMBean`；可选通知阈值 |
| `StatisticsCsvLayout`                                   | 已实现     | 将 `GroupedTimingStatistics` 输出为 CSV；支持 `Pivot` / `Columns` / `PrintNonStatistics` |
| `aop.TimingAspect`（AspectJ）                            | 已实现     | `@Aspect` 类；为每个 `@Profiled` 连接点创建 `Log4J2StopWatch`           |
| `aop.EjbTimingAspect`（EJB 拦截器）                      | 已实现     | Perf4J `AbstractEjbTimingAspect` 接入 log4j2                           |
| 单元测试                                                | 暂无       | 本分支 `src/test` 无测试源码                                            |

## 3. 环境要求与兼容性

| 要求     | 版本                    |
| :------- | :---------------------- |
| JDK      | 8+                      |
| Maven    | 3.0+（已内置 wrapper）   |
| Log4j 2.x| 2.24.x（`log4j-core`）  |
| Perf4J   | 0.9.16                  |
| AspectJ  | 1.8.9（`aspectjrt`）    |

easy4j 项目的版本线：

| 分支           | JDK  | 版本模式   | 说明                            |
| :------------- | :--- | :--------- | :------------------------------ |
| `feature/1.0.x` | 8    | `1.0.x.*`  | 本文档对应分支                   |
| `feature/2.0.x` | 17   | `2.0.x.*`  | JDK 17 版本线                   |
| `feature/3.0.x` | 21   | `3.0.x.*`  | JDK 21 版本线                   |

## 4. 架构与模块

```text
   @Profiled / 手动计时
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
 (CSV 行)            (StatisticsExposingMBean)
      |                |
      v                v
   文件 / 标准输出   JMX 控制台 / 告警
```

单模块 Maven 项目（`jar` 打包）：

| 包                        | 职责                                            |
| :------------------------ | :---------------------------------------------- |
| `org.perf4j.log4j2`       | 秒表、JMX Appender、CSV Layout                  |
| `org.perf4j.log4j2.aop`   | AspectJ 与 EJB 计时切面                         |

## 5. 安装

制品发布在阿里云私服与 GitHub Releases，**尚未发布到 Maven Central**。

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

## 6. 快速开始

```java
import org.perf4j.log4j2.Log4J2StopWatch;

public class OrderService {

    public void createOrder(String orderId) {
        Log4J2StopWatch stopWatch = new Log4J2StopWatch("orderService.createOrder");
        try {
            // ... 业务逻辑 ...
        } finally {
            stopWatch.stop();
        }
    }
}
```

预期结果：当 `org.perf4j.TimingLogger` 处于 INFO 级别时，每次 `stop()` 都会通过 log4j2 输出一行 Perf4J 计时日志（例如 `start[1234567890123] time[42] tag[orderService.createOrder]`）。带异常的 `stop(Throwable)` / `lap(Throwable)` 调用改为 WARN 级别输出。

## 7. 配置

无属性文件；在 `log4j2.xml` 中接线。最小配置：

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

说明：

- `JmxAttributes` 通过 `JmxAttributeStatisticsAppender` 上的 log4j2 `@Plugin` 注解解析。
- 调优选项（`MBeanName`、`TagNamesToExpose`、`NotificationThresholds`）是 JavaBean 属性；当前的 `@PluginFactory` 只暴露 `name` 与 `ignoreExceptions`，因此这些选项需在构造后编程设置——若需在 XML 中绑定这些选项，则需要扩展插件工厂（**假设**）。
- `StatisticsCsvLayout` 未标注 log4j2 插件注解；请以编程方式使用（见第 8 节）。

## 8. 核心用法 / API

编程方式接线 JMX 与 CSV：

```java
// 1) 面向 GroupedTimingStatistics 的 CSV Layout
StatisticsCsvLayout csvLayout = StatisticsCsvLayout.createDefaultLayout();
csvLayout.setColumns("tag,start,stop,mean,min,max,stddev,count,tps");

// 2) JMX Appender — 选项为 JavaBean 属性
JmxAttributeStatisticsAppender jmxAppender =
        new JmxAttributeStatisticsAppender("JmxAttributes", null, null);
jmxAppender.setTagNamesToExpose("orderService.createOrder");
jmxAppender.setNotificationThresholds("orderService.createOrderMean(<1000)");
jmxAppender.activateOptions();
```

- `setTagNamesToExpose("a,b")` 会将 `aMean`、`aStdDev`、`aMin`、`aMax`、`aCount`、`aTPS` 等暴露为 MBean 属性；`setNotificationThresholds("aMean(<100),bTPS(>1)")` 在数值超出可接受区间（`<值`、`>值`、`最小值-最大值`）时发送 JMX 通知。
- MBean 默认对象名为 `org.perf4j:type=StatisticsExposingMBean,name=Perf4J`。

AOP 用法（AspectJ）：

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

EJB 用法：在 EJB 上标注 `@Interceptors(EjbTimingAspect.class)`（沿用 Perf4J 的 `@Profiled` 约定）。

## 9. 测试与构建

```bash
./mvnw clean verify
```

- 仓库内置 Maven wrapper（`mvnw`）。
- 已配置 JaCoCo 行覆盖率 90% 门禁（`haltOnFailure=false`）。
- 本分支没有单元测试——覆盖率门禁实际上未被有效执行（已知缺口）；秒表与 Appender/Layout 的测试是最有价值的补充。

## 10. 版本线与分支

| 分支           | JDK  | 版本模式   | 维护说明                          |
| :------------- | :--- | :--------- | :-------------------------------- |
| `feature/1.0.x` | 8    | `1.0.x.*`  | 当前分支                          |
| `feature/2.0.x` | 17   | `2.0.x.*`  | JDK 17 版本线                     |
| `feature/3.0.x` | 21   | `3.0.x.*`  | JDK 21 版本线                     |

制品通过阿里云 Maven 私服与 GitHub Releases 分发。请按 JDK 基线选择对应分支。

## 11. 贡献与许可

欢迎贡献——尤其是单元测试，以及为 CSV Layout 提供带 `@Plugin` 注解的工厂方法。较大改动请先提交 issue 讨论。

本项目基于 [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0) 许可。
