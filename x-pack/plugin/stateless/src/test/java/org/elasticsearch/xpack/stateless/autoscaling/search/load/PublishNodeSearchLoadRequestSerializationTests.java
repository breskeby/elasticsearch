/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.stateless.autoscaling.search.load;

import org.elasticsearch.common.UUIDs;
import org.elasticsearch.common.io.stream.Writeable;
import org.elasticsearch.test.AbstractWireSerializingTestCase;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.stateless.autoscaling.MetricQuality;

public class PublishNodeSearchLoadRequestSerializationTests extends AbstractWireSerializingTestCase<PublishNodeSearchLoadRequest> {

    @Override
    protected Writeable.Reader<PublishNodeSearchLoadRequest> instanceReader() {
        return PublishNodeSearchLoadRequest::new;
    }

    @Override
    protected PublishNodeSearchLoadRequest createTestInstance() {
        return new PublishNodeSearchLoadRequest(
            UUIDs.randomBase64UUID(),
            randomLong(),
            randomDoubleBetween(0, 512, true),
            MetricQuality.EXACT
        );
    }

    @Override
    protected PublishNodeSearchLoadRequest mutateInstance(PublishNodeSearchLoadRequest instance) {
        return switch (randomInt(2)) {
            case 0 -> new PublishNodeSearchLoadRequest(
                randomValueOtherThan(instance.getNodeId(), UUIDs::randomBase64UUID),
                instance.getSeqNo(),
                instance.getSearchLoad(),
                MetricQuality.EXACT
            );
            case 1 -> new PublishNodeSearchLoadRequest(
                instance.getNodeId(),
                randomValueOtherThan(instance.getSeqNo(), ESTestCase::randomLong),
                instance.getSearchLoad(),
                MetricQuality.EXACT
            );
            case 2 -> new PublishNodeSearchLoadRequest(
                instance.getNodeId(),
                instance.getSeqNo(),
                randomValueOtherThan(instance.getSearchLoad(), () -> randomDoubleBetween(0, 512, true)),
                MetricQuality.EXACT
            );
            default -> throw new IllegalStateException("Unexpected value: " + randomInt(2));
        };
    }
}
