/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.stateless.autoscaling.indexing;

import org.elasticsearch.common.io.stream.Writeable;
import org.elasticsearch.test.AbstractWireSerializingTestCase;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.stateless.autoscaling.MetricQuality;

import java.io.IOException;

public class NodeIngestLoadSnapshotTests extends AbstractWireSerializingTestCase<NodeIngestLoadSnapshot> {

    @Override
    protected Writeable.Reader<NodeIngestLoadSnapshot> instanceReader() {
        return NodeIngestLoadSnapshot::new;
    }

    @Override
    protected NodeIngestLoadSnapshot createTestInstance() {
        return new NodeIngestLoadSnapshot(
            randomAlphaOfLength(12),
            randomAlphaOfLength(12),
            randomDoubleBetween(0, 128, true),
            randomFrom(MetricQuality.values())
        );
    }

    @Override
    protected NodeIngestLoadSnapshot mutateInstance(NodeIngestLoadSnapshot instance) throws IOException {
        return switch (between(0, 3)) {
            case 0 -> new NodeIngestLoadSnapshot(randomAlphaOfLength(14), instance.nodeName(), instance.load(), instance.metricQuality());
            case 1 -> new NodeIngestLoadSnapshot(instance.nodeId(), randomAlphaOfLength(14), instance.load(), instance.metricQuality());
            case 2 -> new NodeIngestLoadSnapshot(
                instance.nodeId(),
                instance.nodeName(),
                randomDoubleBetween(256, 512, true),
                instance.metricQuality()
            );
            case 3 -> new NodeIngestLoadSnapshot(
                randomValueOtherThan(instance.nodeId(), ESTestCase::randomIdentifier),
                instance.nodeName(),
                instance.load(),
                randomValueOtherThan(instance.metricQuality(), () -> randomFrom(MetricQuality.values()))
            );
            default -> throw new IllegalStateException("Unexpected value");
        };
    }
}
