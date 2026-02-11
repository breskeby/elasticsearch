/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.stateless;

import org.elasticsearch.blobcache.shared.SharedBlobCacheService;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.ByteSizeUnit;
import org.elasticsearch.common.unit.ByteSizeValue;
import org.elasticsearch.common.util.CollectionUtils;
import org.elasticsearch.index.query.MatchAllQueryBuilder;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.snapshots.mockstore.MockRepository;
import org.elasticsearch.xpack.stateless.cache.StatelessOnlinePrewarmingService;
import org.elasticsearch.xpack.stateless.objectstore.ObjectStoreService;

import java.util.Collection;
import java.util.Locale;

import static org.elasticsearch.test.hamcrest.ElasticsearchAssertions.assertHitCount;
import static org.elasticsearch.xpack.stateless.objectstore.ObjectStoreTestUtils.getObjectStoreMockRepository;

public class StatelessPrefetchIT extends AbstractStatelessPluginIntegTestCase {

    @Override
    protected boolean addMockFsRepository() {
        return false;
    }

    protected Settings.Builder nodeSettings() {
        return super.nodeSettings().put(ObjectStoreService.TYPE_SETTING.getKey(), ObjectStoreService.ObjectStoreType.MOCK)
            .put(StatelessOnlinePrewarmingService.STATELESS_ONLINE_PREWARMING_ENABLED.getKey(), false);
    }

    @Override
    protected Collection<Class<? extends Plugin>> nodePlugins() {
        return CollectionUtils.appendToCopy(super.nodePlugins(), MockRepository.Plugin.class);
    }

    public void testSearchShardsStarted() throws Exception {
        startMasterAndIndexNode();
        // create large enough cache with small enough pages on the search node to allow prefetching all data
        String searchNode = startSearchNode(
            Settings.builder()
                .put(
                    SharedBlobCacheService.SHARED_CACHE_SIZE_SETTING.getKey(),
                    ByteSizeValue.of(randomIntBetween(10, 100), ByteSizeUnit.MB).getStringRep()
                )
                .put(
                    SharedBlobCacheService.SHARED_CACHE_REGION_SIZE_SETTING.getKey(),
                    ByteSizeValue.of(256, ByteSizeUnit.KB).getStringRep()
                )
                .build()
        );
        final String indexName = randomAlphaOfLength(10).toLowerCase(Locale.ROOT);
        createIndex(indexName, indexSettings(1, 1).build());
        ensureGreen(indexName);
        int rounds = randomIntBetween(1, 4);
        int totalDocs = 0;
        for (int i = 0; i < rounds; i++) {
            int cnt = randomIntBetween(10, 100);
            indexDocsAndRefresh(indexName, cnt);
            totalDocs += cnt;
        }

        logger.info("--> blocking repository");
        var mockRepository = getObjectStoreMockRepository(getObjectStoreService(searchNode));
        mockRepository.setBlockOnAnyFiles();
        logger.info("--> running search and verifying that the repository was not accessed because prefetching warmed the cache already");
        assertHitCount(client().prepareSearch().setQuery(new MatchAllQueryBuilder()), totalDocs);
        assertFalse(mockRepository.blocked());
    }
}
