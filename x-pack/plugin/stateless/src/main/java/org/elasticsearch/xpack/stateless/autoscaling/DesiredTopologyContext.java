/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.stateless.autoscaling;

import org.elasticsearch.cluster.LocalNodeMasterListener;
import org.elasticsearch.cluster.service.ClusterService;
import org.elasticsearch.core.Nullable;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Holds an instance of DesiredClusterTopology which can be passed when fetching metrics
 * (POST /_internal/serverless/autoscaling). Each subsequent POST updates the topology.
 */
public class DesiredTopologyContext implements LocalNodeMasterListener {

    private final ClusterService clusterService;
    private volatile DesiredClusterTopology desiredClusterTopology;
    private final List<DesiredTopologyListener> listeners = new CopyOnWriteArrayList<>();

    public DesiredTopologyContext(ClusterService clusterService) {
        this.clusterService = clusterService;
    }

    public void init() {
        clusterService.addLocalNodeMasterListener(this);
    }

    public void updateDesiredClusterTopology(DesiredClusterTopology desiredClusterTopology) {
        DesiredClusterTopology previous = this.desiredClusterTopology;
        this.desiredClusterTopology = desiredClusterTopology;

        if (previous == null && desiredClusterTopology != null) {
            for (DesiredTopologyListener listener : listeners) {
                listener.onDesiredTopologyAvailable(desiredClusterTopology);
            }
        }
    }

    public void addListener(DesiredTopologyListener listener) {
        listeners.add(listener);
    }

    @Nullable
    public DesiredClusterTopology getDesiredClusterTopology() {
        return desiredClusterTopology;
    }

    @Override
    public void onMaster() {
        // Clear any stale topology that might have been set during master transition
        desiredClusterTopology = null;
    }

    @Override
    public void offMaster() {
        // Remove topology to avoid stale data in case this node is elected master again in the future
        desiredClusterTopology = null;
    }
}
