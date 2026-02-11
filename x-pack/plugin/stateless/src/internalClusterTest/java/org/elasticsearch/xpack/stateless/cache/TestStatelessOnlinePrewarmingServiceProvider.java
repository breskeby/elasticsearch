/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.stateless.cache;

import org.elasticsearch.action.search.OnlinePrewarmingService;
import org.elasticsearch.action.search.OnlinePrewarmingServiceProvider;
import org.elasticsearch.cluster.service.ClusterService;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.telemetry.TelemetryProvider;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.xpack.stateless.StatelessPlugin;

/**
 * This is the equivalent of {@link org.elasticsearch.xpack.stateless.cache.StatelessOnlinePrewarmingServiceProvider}
 * but for the integration test suite. We need another implementation as SPI needs a constructor with the
 * {@link org.elasticsearch.plugins.Plugin} parameter (which is {@link StatelessPlugin} in production)
 * however, in ITs we use a different test plugin instead of {@link StatelessPlugin}
 */
public class TestStatelessOnlinePrewarmingServiceProvider implements OnlinePrewarmingServiceProvider {

    private final StatelessOnlinePrewarmingIT.TestCacheStatelessPluginNoRecoveryPrewarming plugin;

    public TestStatelessOnlinePrewarmingServiceProvider() {
        throw new IllegalStateException("This no arg constructor only exists for SPI validation");
    }

    public TestStatelessOnlinePrewarmingServiceProvider(StatelessOnlinePrewarmingIT.TestCacheStatelessPluginNoRecoveryPrewarming plugin) {
        this.plugin = plugin;
    }

    @Override
    public OnlinePrewarmingService create(
        Settings settings,
        ThreadPool threadPool,
        ClusterService clusterService,
        TelemetryProvider telemetryProvider
    ) {
        return new StatelessOnlinePrewarmingService(
            settings,
            threadPool,
            plugin.getStatelessSharedBlobCacheService(),
            telemetryProvider.getMeterRegistry()
        );
    }
}
