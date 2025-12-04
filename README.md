# Adaptive CPU Pinning Framework for Java

[![Java](https://img.shields.io/badge/Java-17+-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2+-green.svg)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Maven Central](https://img.shields.io/maven-central/v/com.reene4444/adaptive-cpu-pinning.svg)](https://central.sonatype.com/artifact/com.reene4444/adaptive-cpu-pinning)

Adaptive CPU affinity framework for Java with business-aware isolation, virtual thread support, and chaos engineering integration.

## Features

- Adaptive CPU binding based on workload, QPS, latency, and GC pressure
- Business-aware isolation with Spring Boot annotations
- Virtual thread support (Java 21+)
- Chaos engineering integration

## Installation

```xml
<dependency>
    <groupId>com.reene4444</groupId>
    <artifactId>adaptive-cpu-pinning</artifactId>
    <version>1.0.0</version>
</dependency>
```

## Quick Start

```yaml
cpu:
  pinning:
    enabled: true
```
```java
@AffinityPool("my-service", cpus = {0,1,2,3})
@Service
public class MyService {
    @AdaptiveAffinity(workloadType = WorkloadType.CPU_INTENSIVE)
    public void process() {
        // Automatically pinned to optimal CPUs
    }
}
```

## Performance & Requirements

30-50% latency reduction, 20-40% throughput improvement. CPU pin overhead: ~5μs. [Benchmarks](https://github.com/Reene444/adaptive-cpu-pinning-benchmarks/blob/main/BENCHMARK_RESULTS.md)

Java 17+, Spring Boot 3.2+ (optional), Linux (primary). Apache License 2.0
