/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.stateless.autoscaling.search;

/**
* Hook interface for testing and observability of the replicas updater service.
* Production code uses {@link NoopExecutionListener}.
* Tests can inject custom implementations to control execution timing.
*/
interface ReplicasUpdaterExecutionListener {
    /**
     * Called when the replicas updater loop starts executing
     */
    void onRunStart(boolean immediateScaleDown, boolean onlyScaleDownToTopologyBounds);

    /**
     * Called when the loop completes
     */
    void onRunComplete();

    /**
     * Called trigger to run the loop is skipped because already running
     */
    void onSkipped();

    /**
     * Called when the currently running loop executes a pending request to run
     */
    void onPendingRunExecution();

    /**
     * Called immediately after the state transitions to IDLE
     */
    void onStateTransitionToIdle();

    class NoopExecutionListener implements ReplicasUpdaterExecutionListener {

        public static final NoopExecutionListener INSTANCE = new NoopExecutionListener();

        @Override
        public void onRunStart(boolean immediateScaleDown, boolean onlyScaleDownToTopologyBounds) {}

        @Override
        public void onRunComplete() {}

        @Override
        public void onSkipped() {}

        @Override
        public void onPendingRunExecution() {}

        @Override
        public void onStateTransitionToIdle() {}
    }
}
