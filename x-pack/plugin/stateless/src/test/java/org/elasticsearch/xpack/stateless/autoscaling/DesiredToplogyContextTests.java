/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.stateless.autoscaling;

import org.elasticsearch.cluster.service.ClusterService;
import org.elasticsearch.test.ClusterServiceUtils;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.threadpool.TestThreadPool;
import org.elasticsearch.threadpool.ThreadPool;
import org.junit.After;
import org.junit.Before;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.elasticsearch.xpack.stateless.autoscaling.DesiredClusterTopologyTestUtils.randomDesiredClusterTopology;

public class DesiredToplogyContextTests extends ESTestCase {

    private ThreadPool testThreadPool;

    @Before
    private void setup() {
        testThreadPool = new TestThreadPool(getTestName());
    }

    @After
    public void cleanup() {
        testThreadPool.shutdownNow();
    }

    public void testListenerCalledWhenTopologyPopulated() {
        ClusterService clusterService = ClusterServiceUtils.createClusterService(testThreadPool);
        DesiredTopologyContext context = new DesiredTopologyContext(clusterService);

        AtomicBoolean listenerCalled = new AtomicBoolean(false);
        DesiredTopologyListener listener = topology -> { listenerCalled.set(true); };

        context.addListener(listener);
        assertNull(context.getDesiredClusterTopology());
        context.updateDesiredClusterTopology(randomDesiredClusterTopology());
        assertTrue("Listener should be called when topology transitions from null to populated", listenerCalled.get());

        listenerCalled.set(false);
        context.updateDesiredClusterTopology(randomDesiredClusterTopology());
        assertFalse("Listener should NOT be called when topology is updated after already being populated", listenerCalled.get());
    }

    public void testOnMasterClearsStaleTopology() {
        ClusterService clusterService = ClusterServiceUtils.createClusterService(testThreadPool);
        DesiredTopologyContext context = new DesiredTopologyContext(clusterService);

        context.updateDesiredClusterTopology(randomDesiredClusterTopology());
        assertNotNull("Topology should be set", context.getDesiredClusterTopology());

        context.onMaster();
        assertNull("Topology should be cleared when node becomes master", context.getDesiredClusterTopology());
    }

    public void testOffMasterClearsTopology() {
        ClusterService clusterService = ClusterServiceUtils.createClusterService(testThreadPool);
        DesiredTopologyContext context = new DesiredTopologyContext(clusterService);

        context.updateDesiredClusterTopology(randomDesiredClusterTopology());
        assertNotNull("Topology should be set", context.getDesiredClusterTopology());

        context.offMaster();
        assertNull("Topology should be cleared when node goes off master", context.getDesiredClusterTopology());
    }
}
