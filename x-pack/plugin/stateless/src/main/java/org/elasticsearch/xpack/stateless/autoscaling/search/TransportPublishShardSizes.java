/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.stateless.autoscaling.search;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.ActionResponse;
import org.elasticsearch.action.ActionType;
import org.elasticsearch.action.support.ActionFilters;
import org.elasticsearch.action.support.master.TransportMasterNodeAction;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.block.ClusterBlockException;
import org.elasticsearch.cluster.block.ClusterBlockLevel;
import org.elasticsearch.cluster.service.ClusterService;
import org.elasticsearch.common.util.concurrent.EsExecutors;
import org.elasticsearch.injection.guice.Inject;
import org.elasticsearch.tasks.Task;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.TransportService;

public class TransportPublishShardSizes extends TransportMasterNodeAction<PublishShardSizesRequest, ActionResponse.Empty> {

    public static final String NAME = "cluster:monitor/stateless/autoscaling/push_shard_sizes";
    public static final ActionType<ActionResponse.Empty> INSTANCE = new ActionType<>(NAME);

    private final SearchMetricsService searchTierMetricsService;

    @Inject
    public TransportPublishShardSizes(
        TransportService transportService,
        ClusterService clusterService,
        ThreadPool threadPool,
        ActionFilters actionFilters,
        SearchMetricsService searchTierMetricsService
    ) {
        super(
            NAME,
            transportService,
            clusterService,
            threadPool,
            actionFilters,
            PublishShardSizesRequest::new,
            in -> ActionResponse.Empty.INSTANCE,
            EsExecutors.DIRECT_EXECUTOR_SERVICE
        );
        this.searchTierMetricsService = searchTierMetricsService;
    }

    @Override
    protected void masterOperation(
        Task task,
        PublishShardSizesRequest request,
        ClusterState state,
        ActionListener<ActionResponse.Empty> listener
    ) {
        ActionListener.completeWith(listener, () -> {
            searchTierMetricsService.processShardSizesRequest(request);
            return ActionResponse.Empty.INSTANCE;
        });
    }

    @Override
    protected ClusterBlockException checkBlock(PublishShardSizesRequest request, ClusterState state) {
        return state.blocks().globalBlockedException(ClusterBlockLevel.METADATA_READ);
    }
}
