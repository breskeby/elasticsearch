/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.stateless.cache;

import org.elasticsearch.common.settings.ClusterSettings;
import org.elasticsearch.common.settings.Setting;
import org.elasticsearch.core.TimeValue;

/**
 * This class holds dynamic settings to control the behavior of {@link SearchCommitPrefetcher}.
 * <p>
 * It listens to changes in the cluster settings and updates its internal state accordingly.
 */
public class SearchCommitPrefetcherDynamicSettings {

    private static final TimeValue DEFAULT_SEARCH_IDLE_TIME = TimeValue.timeValueDays(3);
    public static final Setting<Boolean> PREFETCH_COMMITS_UPON_NOTIFICATIONS_ENABLED_SETTING = Setting.boolSetting(
        "stateless.search.prefetch_commits.enabled",
        true,
        Setting.Property.Dynamic,
        Setting.Property.NodeScope
    );

    public static final Setting<TimeValue> PREFETCH_SEARCH_IDLE_TIME_SETTING = Setting.timeSetting(
        "stateless.search.prefetch_commits.search_idle_time",
        DEFAULT_SEARCH_IDLE_TIME,
        TimeValue.ZERO,
        Setting.Property.Dynamic,
        Setting.Property.NodeScope
    );
    private volatile boolean prefetchingEnabled;
    private volatile long prefetchSearchIdleTimeInMillis;

    public SearchCommitPrefetcherDynamicSettings(ClusterSettings clusterSettings) {
        clusterSettings.initializeAndWatch(PREFETCH_COMMITS_UPON_NOTIFICATIONS_ENABLED_SETTING, value -> this.prefetchingEnabled = value);
        clusterSettings.initializeAndWatch(
            PREFETCH_SEARCH_IDLE_TIME_SETTING,
            value -> this.prefetchSearchIdleTimeInMillis = value.millis()
        );
    }

    public boolean prefetchingEnabled() {
        return prefetchingEnabled;
    }

    public long searchIdleTimeInMillis() {
        return prefetchSearchIdleTimeInMillis;
    }
}
