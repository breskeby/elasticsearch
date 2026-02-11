/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.stateless.autoscaling.search;

import co.elastic.elasticsearch.stateless.api.ShardSizeStatsReader;

import org.elasticsearch.common.UUIDs;
import org.elasticsearch.common.io.stream.Writeable;
import org.elasticsearch.core.Tuple;
import org.elasticsearch.index.shard.ShardId;
import org.elasticsearch.test.AbstractWireSerializingTestCase;
import org.elasticsearch.xpack.stateless.lucene.stats.ShardSizeTests;

import java.util.Map;

public class PublishShardSizesRequestTests extends AbstractWireSerializingTestCase<PublishShardSizesRequest> {

    @Override
    protected Writeable.Reader<PublishShardSizesRequest> instanceReader() {
        return PublishShardSizesRequest::new;
    }

    @Override
    protected PublishShardSizesRequest createTestInstance() {
        return new PublishShardSizesRequest(UUIDs.randomBase64UUID(), randomShardSizes());
    }

    private static ShardId randomShardId() {
        return new ShardId(randomIdentifier(), UUIDs.randomBase64UUID(), randomIntBetween(0, 99));
    }

    private static Map<ShardId, ShardSizeStatsReader.ShardSize> randomShardSizes() {
        return randomMap(1, 25, () -> Tuple.tuple(randomShardId(), ShardSizeTests.randomShardSize()));
    }

    @Override
    protected PublishShardSizesRequest mutateInstance(PublishShardSizesRequest instance) {
        return switch (randomInt(1)) {
            case 0 -> new PublishShardSizesRequest(
                randomValueOtherThan(instance.getNodeId(), UUIDs::randomBase64UUID),
                instance.getShardSizes()
            );
            default -> new PublishShardSizesRequest(
                instance.getNodeId(),
                randomValueOtherThan(instance.getShardSizes(), PublishShardSizesRequestTests::randomShardSizes)
            );
        };
    }
}
