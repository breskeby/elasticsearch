/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.stateless.autoscaling.search;

import org.elasticsearch.index.shard.ShardId;

@FunctionalInterface
public interface ShardSizeCollector {

    ShardSizeCollector NOOP = shardId -> { assert false : "should not be called"; };

    void collectShardSize(ShardId shardId);
}
