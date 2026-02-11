/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.stateless.autoscaling.memory;

import co.elastic.elasticsearch.serverless.constants.ProjectType;

import org.elasticsearch.test.ESTestCase;

import static org.elasticsearch.xpack.stateless.autoscaling.memory.HeapToSystemMemory.MAX_HEAP_SIZE;
import static org.elasticsearch.xpack.stateless.autoscaling.memory.HeapToSystemMemory.VECTOR_HEAP_THRESHOLD;
import static org.elasticsearch.xpack.stateless.autoscaling.memory.HeapToSystemMemory.dataNode;
import static org.elasticsearch.xpack.stateless.autoscaling.memory.HeapToSystemMemory.tier;
import static org.hamcrest.CoreMatchers.equalTo;

public class HeapToSystemMemoryTests extends ESTestCase {
    public void testDefaultHeapToSystemMemory() {
        long heapInBytes = randomLongBetween(MemoryMetricsService.WORKLOAD_MEMORY_OVERHEAD, MAX_HEAP_SIZE);
        assertThat(dataNode(heapInBytes, ProjectType.ELASTICSEARCH_GENERAL_PURPOSE), equalTo(heapInBytes * 2));
        assertThat(tier(heapInBytes, ProjectType.ELASTICSEARCH_GENERAL_PURPOSE), equalTo(heapInBytes * 2));

        heapInBytes = randomLongBetween(MAX_HEAP_SIZE + 1, MAX_HEAP_SIZE * 10);
        assertThat(dataNode(heapInBytes, ProjectType.ELASTICSEARCH_GENERAL_PURPOSE), equalTo(MAX_HEAP_SIZE * 2));
        assertThat(tier(heapInBytes, ProjectType.ELASTICSEARCH_GENERAL_PURPOSE), equalTo(heapInBytes * 2));
    }

    public void testVectorHeapToSystemMemory() {
        long heapInBytes = randomLongBetween(MemoryMetricsService.WORKLOAD_MEMORY_OVERHEAD, VECTOR_HEAP_THRESHOLD);
        assertThat(dataNode(heapInBytes, ProjectType.ELASTICSEARCH_VECTOR), equalTo(heapInBytes * 2));
        assertThat(tier(heapInBytes, ProjectType.ELASTICSEARCH_VECTOR), equalTo(heapInBytes * 2));

        heapInBytes = randomLongBetween(VECTOR_HEAP_THRESHOLD, MAX_HEAP_SIZE);
        assertThat(dataNode(heapInBytes, ProjectType.ELASTICSEARCH_VECTOR), equalTo(heapInBytes * 4));
        assertThat(tier(heapInBytes, ProjectType.ELASTICSEARCH_VECTOR), equalTo(heapInBytes * 4));

        heapInBytes = randomLongBetween(MAX_HEAP_SIZE + 1, MAX_HEAP_SIZE * 10);
        assertThat(dataNode(heapInBytes, ProjectType.ELASTICSEARCH_VECTOR), equalTo(MAX_HEAP_SIZE * 4));
        assertThat(tier(heapInBytes, ProjectType.ELASTICSEARCH_VECTOR), equalTo(heapInBytes * 4));
    }
}
