/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.oldrepos.tests;

import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.test.cluster.ElasticsearchCluster;
import org.elasticsearch.test.cluster.local.LocalClusterConfigProvider;
import org.elasticsearch.test.cluster.local.LocalClusterSpecBuilder;
import org.elasticsearch.test.cluster.local.distribution.DistributionType;
import org.elasticsearch.test.cluster.util.Version;
import org.elasticsearch.test.rest.ESRestTestCase;
import org.junit.ClassRule;

import java.io.IOException;

import static org.hamcrest.Matchers.is;

public class OldDistroIT extends ESRestTestCase {

    @ClassRule
    public static ElasticsearchCluster cluster = ElasticsearchCluster.local()
        // .version(Version.fromString("7.17.20"))
        .version(Version.fromString("6.0.0"))
        .distribution(DistributionType.DEFAULT)
        .setting("xpack.security.enabled", "false")
        .setting("xpack.ml.enabled", "false")
        .apply(builder -> {
            if (System.getenv("JAVA_HOME") != null) {
                builder.environment("JAVA_HOME", System.getenv("JAVA_HOME"));
            }
        })
        .build();

    @Override
    protected String getTestRestCluster() {
        return cluster.getHttpAddresses();
    }

    public void testCreateIndex() throws IOException {
        Request request = request("PUT", "/" + "blubb");
        Response response = client().performRequest(request);
        assertThat(response.getStatusLine().getStatusCode(), is(200));
    }

    private Request request(String method, String endpoint) {
        Request request = new Request(method, endpoint);
        request.setOptions(request.getOptions().toBuilder().addHeader("X-elastic-product-origin", "kibana").build());
        return request;
    }
}
