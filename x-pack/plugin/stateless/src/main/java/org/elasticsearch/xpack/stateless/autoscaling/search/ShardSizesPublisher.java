/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.stateless.autoscaling.search;

import co.elastic.elasticsearch.stateless.api.ShardSizeStatsReader.ShardSize;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.client.internal.Client;
import org.elasticsearch.index.shard.ShardId;
import org.elasticsearch.threadpool.ThreadPool;

import java.util.Map;

public class ShardSizesPublisher {

    private final Client client;

    public ShardSizesPublisher(Client client) {
        this.client = client;
    }

    public void publishSearchShardDiskUsage(String nodeId, Map<ShardId, ShardSize> shardSizes, ActionListener<Void> listener) {
        assert ThreadPool.assertCurrentThreadPool(ThreadPool.Names.GENERIC);
        var request = new PublishShardSizesRequest(nodeId, shardSizes);
        client.execute(TransportPublishShardSizes.INSTANCE, request, listener.map(unused -> null));
    }
}
