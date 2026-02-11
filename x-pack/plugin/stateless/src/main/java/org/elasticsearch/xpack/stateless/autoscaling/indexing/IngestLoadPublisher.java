/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.stateless.autoscaling.indexing;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.client.internal.Client;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.xpack.stateless.autoscaling.indexing.IngestionLoad.NodeIngestionLoad;

import java.util.concurrent.atomic.AtomicLong;

public class IngestLoadPublisher {

    private final Client client;
    private final ThreadPool threadPool;
    private final AtomicLong seqNoSupplier = new AtomicLong();

    public IngestLoadPublisher(Client client, ThreadPool threadPool) {
        this.client = client;
        this.threadPool = threadPool;
    }

    public void publishIngestionLoad(NodeIngestionLoad ingestionLoad, String nodeId, String nodeName, ActionListener<Void> listener) {
        threadPool.generic().execute(() -> {
            var request = new PublishNodeIngestLoadRequest(nodeId, nodeName, seqNoSupplier.incrementAndGet(), ingestionLoad);
            client.execute(TransportPublishNodeIngestLoadMetric.INSTANCE, request, listener.map(unused -> null));
        });
    }
}
