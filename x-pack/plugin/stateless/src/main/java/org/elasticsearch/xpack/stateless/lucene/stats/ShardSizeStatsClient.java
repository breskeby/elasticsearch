/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.stateless.lucene.stats;

import co.elastic.elasticsearch.stateless.api.ShardSizeStatsReader.ShardSize;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.client.internal.Client;
import org.elasticsearch.client.internal.node.NodeClient;
import org.elasticsearch.core.TimeValue;
import org.elasticsearch.index.shard.ShardId;

import java.util.Map;

public class ShardSizeStatsClient {

    private final NodeClient client;

    public ShardSizeStatsClient(Client client) {
        this.client = (NodeClient) client;
    }

    public void getAllShardSizes(TimeValue boostWindowInterval, ActionListener<Map<ShardId, ShardSize>> listener) {
        client.executeLocally(
            GetAllShardSizesAction.INSTANCE,
            new GetAllShardSizesAction.Request(boostWindowInterval),
            listener.map(GetAllShardSizesAction.Response::getShardSizes)
        );
    }

    public void getShardSize(ShardId shardId, TimeValue boostWindowInterval, ActionListener<ShardSize> listener) {
        client.executeLocally(
            GetShardSizeAction.INSTANCE,
            new GetShardSizeAction.Request(shardId, boostWindowInterval),
            listener.map(GetShardSizeAction.Response::getShardSize)
        );
    }
}
