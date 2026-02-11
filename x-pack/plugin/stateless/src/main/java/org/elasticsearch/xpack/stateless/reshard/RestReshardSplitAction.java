/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.stateless.reshard;

import org.elasticsearch.client.internal.node.NodeClient;
import org.elasticsearch.common.settings.Setting;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.rest.BaseRestHandler;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.Scope;
import org.elasticsearch.rest.ServerlessScope;
import org.elasticsearch.rest.action.EmptyResponseListener;

import java.io.IOException;
import java.util.List;

import static org.elasticsearch.common.settings.Setting.boolSetting;
import static org.elasticsearch.rest.RestRequest.Method.POST;

@ServerlessScope(Scope.INTERNAL)
public class RestReshardSplitAction extends BaseRestHandler {

    public static final Setting<Boolean> RESHARD_ALLOWED = boolSetting("rest.internal.reshard_allowed", false, Setting.Property.NodeScope);
    private final boolean reshardAllowed;

    public RestReshardSplitAction(Settings settings) {
        reshardAllowed = RESHARD_ALLOWED.get(settings);
    }

    @Override
    public String getName() {
        return "reshard_action";
    }

    @Override
    public List<Route> routes() {
        return List.of(new Route(POST, "/_internal/reshard/split"));
    }

    @Override
    protected RestChannelConsumer prepareRequest(RestRequest request, NodeClient client) throws IOException {
        if (reshardAllowed == false) {
            throw new IllegalStateException("reshard is not allowed");
        }
        ReshardIndexRequest actionRequest = ReshardIndexRequest.fromXContent(request.contentParser());
        return channel -> client.execute(TransportReshardAction.TYPE, actionRequest, new EmptyResponseListener(channel));
    }
}
