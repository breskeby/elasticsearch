/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.stateless.allocation;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.cluster.EstimatedHeapUsageCollector;
import org.elasticsearch.cluster.service.ClusterService;
import org.elasticsearch.xpack.stateless.StatelessPlugin;
import org.elasticsearch.xpack.stateless.autoscaling.memory.MemoryMetricsService;

import java.util.Map;

public class StatelessHeapUsageCollector implements EstimatedHeapUsageCollector {

    private final StatelessPlugin stateless;

    public StatelessHeapUsageCollector() {
        throw new IllegalStateException("This no arg constructor only exists for SPI validation");
    }

    public StatelessHeapUsageCollector(StatelessPlugin stateless) {
        this.stateless = stateless;
    }

    @Override
    public void collectClusterHeapUsage(ActionListener<Map<String, Long>> listener) {
        MemoryMetricsService memoryMetricsService = stateless.getMemoryMetricsService();
        ClusterService clusterService = stateless.getClusterService();
        ActionListener.completeWith(listener, () -> memoryMetricsService.getPerNodeMemoryMetrics(clusterService.state().nodes()));
    }
}
