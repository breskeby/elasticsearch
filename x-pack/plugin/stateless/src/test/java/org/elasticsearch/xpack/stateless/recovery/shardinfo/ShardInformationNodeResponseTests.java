/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.stateless.recovery.shardinfo;

import org.elasticsearch.common.io.stream.Writeable;
import org.elasticsearch.test.AbstractWireSerializingTestCase;

public class ShardInformationNodeResponseTests extends AbstractWireSerializingTestCase<
    TransportFetchSearchShardInformationAction.Response> {

    @Override
    protected Writeable.Reader<TransportFetchSearchShardInformationAction.Response> instanceReader() {
        return TransportFetchSearchShardInformationAction.Response::new;
    }

    @Override
    protected TransportFetchSearchShardInformationAction.Response createTestInstance() {
        return createLabeledTestInstance("1");
    }

    public static TransportFetchSearchShardInformationAction.Response createLabeledTestInstance(String label) {
        return new TransportFetchSearchShardInformationAction.Response(randomNonNegativeLong());
    }

    @Override
    protected TransportFetchSearchShardInformationAction.Response mutateInstance(
        TransportFetchSearchShardInformationAction.Response instance
    ) {
        return switch (between(0, 1)) {
            case 0 -> new TransportFetchSearchShardInformationAction.Response(-instance.getLastSearcherAcquiredTime());
            case 1 -> new TransportFetchSearchShardInformationAction.Response(randomNonNegativeLong());
            default -> throw new AssertionError("option is out of range");
        };
    }

    public void testTest() {
        assertTrue(true);
    }
}
