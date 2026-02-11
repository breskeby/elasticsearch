/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.stateless.autoscaling.memory;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.ActionResponse;
import org.elasticsearch.client.internal.Client;
import org.elasticsearch.common.util.concurrent.ThreadContext;
import org.elasticsearch.logging.LogManager;
import org.elasticsearch.logging.Logger;
import org.elasticsearch.threadpool.ThreadPool;

public class MergeMemoryEstimatePublisher {
    private static final Logger LOGGER = LogManager.getLogger(MergeMemoryEstimatePublisher.class);

    private final ThreadPool threadPool;
    private final Client client;

    public MergeMemoryEstimatePublisher(ThreadPool threadPool, Client client) {
        this.threadPool = threadPool;
        this.client = client;
    }

    public void publish(TransportPublishMergeMemoryEstimate.Request request) {
        threadPool.generic().execute(() -> {
            ThreadContext threadContext = client.threadPool().getThreadContext();
            try (ThreadContext.StoredContext ignored = threadContext.stashContext()) {
                threadContext.markAsSystemContext();
                final var failureLoggingActionListener = ActionListener.<ActionResponse.Empty>noop()
                    .delegateResponse((l, e) -> LOGGER.warn("failed to publish shard merge memory estimate", e));
                client.execute(TransportPublishMergeMemoryEstimate.INSTANCE, request, failureLoggingActionListener);
            }
        });
    }
}
