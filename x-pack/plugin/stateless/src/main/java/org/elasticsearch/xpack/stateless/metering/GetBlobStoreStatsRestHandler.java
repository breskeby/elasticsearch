/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.stateless.metering;

import org.elasticsearch.client.internal.node.NodeClient;
import org.elasticsearch.common.Strings;
import org.elasticsearch.rest.BaseRestHandler;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.Scope;
import org.elasticsearch.rest.ServerlessScope;
import org.elasticsearch.rest.action.RestActions;
import org.elasticsearch.xpack.stateless.StatelessPlugin;
import org.elasticsearch.xpack.stateless.metering.action.GetBlobStoreStatsNodesRequest;

import java.util.List;

import static org.elasticsearch.rest.RestRequest.Method.GET;

/**
 * Rest endpoint for getting blob store usage stats
 */
@ServerlessScope(Scope.INTERNAL)
public class GetBlobStoreStatsRestHandler extends BaseRestHandler {
    @Override
    public String getName() {
        return "get_blob_store_metering_action";
    }

    @Override
    public List<Route> routes() {
        return List.of(new Route(GET, "/_internal/blob_store/stats"));
    }

    @Override
    protected RestChannelConsumer prepareRequest(final RestRequest request, final NodeClient client) {
        String[] nodesIds = Strings.splitStringByCommaToArray(request.param("nodeId"));
        return restChannel -> client.execute(
            StatelessPlugin.GET_BLOB_STORE_STATS_ACTION,
            new GetBlobStoreStatsNodesRequest(),
            new RestActions.NodesResponseRestListener<>(restChannel)
        );
    }
}
