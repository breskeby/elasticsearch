/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the "Elastic License
 * 2.0", the "GNU Affero General Public License v3.0 only", and the "Server Side
 * Public License v 1"; you may not use this file except in compliance with, at
 * your election, the "Elastic License 2.0", the "GNU Affero General Public
 * License v3.0 only", or the "Server Side Public License, v 1".
 */

package org.elasticsearch.test.rest.yaml;

import com.carrotsearch.randomizedtesting.annotations.Name;
import com.carrotsearch.randomizedtesting.annotations.ParametersFactory;

import org.elasticsearch.core.SuppressForbidden;
import org.elasticsearch.test.cluster.ElasticsearchCluster;
import org.junit.ClassRule;
import org.junit.rules.TemporaryFolder;

@SuppressForbidden(reason = "TemporaryFolder junit rule only provides java.io.File api")
public class MDPYamlTestSuiteIT extends ESClientYamlSuiteTestCase {

    private static TemporaryFolder sharedData = new TemporaryFolder();

    @ClassRule
    public static ElasticsearchCluster cluster = ElasticsearchCluster.local()
        .setting("path.shared_data", sharedData.getRoot().getPath())
        .build();

    public MDPYamlTestSuiteIT(@Name("yaml") ClientYamlTestCandidate testCandidate) {
        super(testCandidate);
    }

    @ParametersFactory
    public static Iterable<Object[]> parameters() throws Exception {
        return createParameters();
    }

    @Override
    protected String getTestRestCluster() {
        return cluster.getHttpAddresses();
    }

}
