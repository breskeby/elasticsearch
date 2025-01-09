/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the "Elastic License
 * 2.0", the "GNU Affero General Public License v3.0 only", and the "Server Side
 * Public License v 1"; you may not use this file except in compliance with, at
 * your election, the "Elastic License 2.0", the "GNU Affero General Public
 * License v3.0 only", or the "Server Side Public License, v 1".
 */

package org.elasticsearch.test.fixtures.elasticsearch;

import org.elasticsearch.test.fixtures.testcontainers.DockerEnvironmentAwareTestContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.containers.wait.strategy.WaitAllStrategy;
import org.testcontainers.images.RemoteDockerImage;

import java.time.Duration;

public final class OldElasticsearchTestContainer extends DockerEnvironmentAwareTestContainer {
    public static final String DOCKER_IMAGE = "es6820-fixture:1.0";
    public static final int PORT = 9300;

    public OldElasticsearchTestContainer() {
        super(new RemoteDockerImage(DOCKER_IMAGE));
        withNetwork(Network.newNetwork());
        addExposedPorts(9300);
        setWaitStrategy(
            new WaitAllStrategy().withStartupTimeout(Duration.ofSeconds(120))
                .withStrategy(Wait.forLogMessage(".*mode [basic] - valid.*", 1))
                .withStrategy(Wait.forListeningPort())
        );
    }

    public int getPort() {
        return getMappedPort(PORT);
    }

}
