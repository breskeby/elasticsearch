/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.stateless.autoscaling.search;

import org.elasticsearch.test.ESTestCase;

import java.util.List;
import java.util.Set;

public class ReplicasScaleDownStateTests extends ESTestCase {

    public void testAllOperations() {
        ReplicasScaleDownState state = new ReplicasScaleDownState();
        assertTrue(state.isEmpty());

        ReplicasScaleDownState.PerIndexState result = state.updateMaxReplicasRecommended("index1", 5);
        assertEquals(1, result.signalCount());
        assertEquals(5, result.maxReplicasRecommended());
        assertFalse(state.isEmpty());

        ReplicasScaleDownState.PerIndexState retrieved = state.getState("index1");
        assertNotNull(retrieved);
        assertEquals(1, retrieved.signalCount());
        assertEquals(5, retrieved.maxReplicasRecommended());

        result = state.updateMaxReplicasRecommended("index1", 3);
        assertEquals(2, result.signalCount());
        assertEquals(5, result.maxReplicasRecommended()); // max of 5 and 3

        result = state.updateMaxReplicasRecommended("index1", 8);
        assertEquals(3, result.signalCount());
        assertEquals(8, result.maxReplicasRecommended());

        state.updateMaxReplicasRecommended("index2", 2);
        state.updateMaxReplicasRecommended("index3", 1);
        assertNotNull(state.getState("index2"));
        assertNotNull(state.getState("index3"));

        state.clearStateForIndices(List.of("index1"));
        assertNull(state.getState("index1"));
        assertNotNull(state.getState("index2"));
        assertNotNull(state.getState("index3"));

        state.clearStateExceptForIndices(Set.of("index2"));
        assertNotNull(state.getState("index2"));
        assertNull(state.getState("index3"));

        state.clearState();
        assertTrue(state.isEmpty());
    }
}
