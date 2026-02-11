/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.stateless.cache;

import org.elasticsearch.client.internal.node.NodeClient;
import org.elasticsearch.rest.BaseRestHandler;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.Scope;
import org.elasticsearch.rest.ServerlessScope;
import org.elasticsearch.rest.action.RestChunkedToXContentListener;
import org.elasticsearch.xpack.stateless.StatelessPlugin;
import org.elasticsearch.xpack.stateless.cache.action.ClearBlobCacheNodesRequest;

import java.util.List;

import static org.elasticsearch.rest.RestRequest.Method.POST;

/**
 * Rest endpoint for clearing all blob caches. This is intended for use with benchmark testing in cold-cache scenarios.
 */
@ServerlessScope(Scope.INTERNAL)
public class ClearBlobCacheRestHandler extends BaseRestHandler {
    @Override
    public String getName() {
        return "clear_blob_cache";
    }

    @Override
    public List<Route> routes() {
        return List.of(new Route(POST, "/_internal/blob_caches/clear"));
    }

    @Override
    protected RestChannelConsumer prepareRequest(final RestRequest request, final NodeClient client) {
        return restChannel -> client.execute(
            StatelessPlugin.CLEAR_BLOB_CACHE_ACTION,
            new ClearBlobCacheNodesRequest(),
            new RestChunkedToXContentListener<>(restChannel)
        );
    }
}
