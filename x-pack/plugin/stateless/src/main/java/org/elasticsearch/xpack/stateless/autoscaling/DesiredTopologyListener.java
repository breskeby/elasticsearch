/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.stateless.autoscaling;

/**
 * Used in conjunction with {@link DesiredTopologyContext} to react to changes in the desired cluster topology.
 */
public interface DesiredTopologyListener {
    /**
     * Called when a desired cluster topology becomes available for the first time.
     * <p>
     * This method is invoked on the master node when the topology transitions from {@code null} to a non-null state. This will most often
     * occur when a node is a newly-elected master node, and it receives its first topology via
     * {@code POST /_internal/serverless/autoscaling}. It is not called for subsequent topology updates.
     *
     * @param topology the newly available desired cluster topology, never {@code null}
     */
    void onDesiredTopologyAvailable(DesiredClusterTopology topology);
}
