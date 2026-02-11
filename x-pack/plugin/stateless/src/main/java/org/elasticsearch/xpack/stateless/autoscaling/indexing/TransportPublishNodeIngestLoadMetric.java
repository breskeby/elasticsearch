/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.stateless.autoscaling.indexing;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.ActionResponse;
import org.elasticsearch.action.ActionType;
import org.elasticsearch.action.support.ActionFilters;
import org.elasticsearch.action.support.master.TransportMasterNodeAction;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.block.ClusterBlockException;
import org.elasticsearch.cluster.block.ClusterBlockLevel;
import org.elasticsearch.cluster.service.ClusterService;
import org.elasticsearch.injection.guice.Inject;
import org.elasticsearch.tasks.Task;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.TransportService;

public class TransportPublishNodeIngestLoadMetric extends TransportMasterNodeAction<PublishNodeIngestLoadRequest, ActionResponse.Empty> {

    public static final String NAME = "cluster:monitor/stateless/autoscaling/push_node_ingest_load";
    public static final ActionType<ActionResponse.Empty> INSTANCE = new ActionType<>(NAME);

    private final IngestMetricsService ingestMetricsService;

    @Inject
    public TransportPublishNodeIngestLoadMetric(
        TransportService transportService,
        ClusterService clusterService,
        ThreadPool threadPool,
        ActionFilters actionFilters,
        IngestMetricsService ingestMetricsService
    ) {
        super(
            NAME,
            transportService,
            clusterService,
            threadPool,
            actionFilters,
            PublishNodeIngestLoadRequest::new,
            in -> ActionResponse.Empty.INSTANCE,
            threadPool.executor(ThreadPool.Names.GENERIC)
        );
        this.ingestMetricsService = ingestMetricsService;
    }

    @Override
    protected void masterOperation(
        Task task,
        PublishNodeIngestLoadRequest request,
        ClusterState state,
        ActionListener<ActionResponse.Empty> listener
    ) {
        ActionListener.completeWith(listener, () -> {
            ingestMetricsService.trackNodeIngestLoad(
                state,
                request.getNodeId(),
                request.getNodeName(),
                request.getSeqNo(),
                request.getIngestionLoad()
            );
            return ActionResponse.Empty.INSTANCE;
        });
    }

    @Override
    protected ClusterBlockException checkBlock(PublishNodeIngestLoadRequest request, ClusterState state) {
        return state.blocks().globalBlockedException(ClusterBlockLevel.METADATA_READ);
    }
}
