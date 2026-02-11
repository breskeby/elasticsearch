/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.stateless.cache.action;

import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.transport.AbstractTransportRequest;

import java.io.IOException;

public class ClearBlobCacheNodeRequest extends AbstractTransportRequest {

    public ClearBlobCacheNodeRequest(StreamInput in) throws IOException {
        super(in);
    }

    ClearBlobCacheNodeRequest() {}

    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
    }
}
