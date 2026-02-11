/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.stateless.metering.action;

import org.elasticsearch.common.blobstore.BlobStoreActionStats;
import org.elasticsearch.repositories.RepositoriesService;
import org.elasticsearch.repositories.RepositoryInfo;
import org.elasticsearch.repositories.RepositoryStats;
import org.elasticsearch.repositories.RepositoryStatsSnapshot;
import org.elasticsearch.test.ESTestCase;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.elasticsearch.repositories.RepositoryStatsSnapshot.UNKNOWN_CLUSTER_VERSION;
import static org.elasticsearch.xpack.stateless.metering.action.GetBlobStoreStatsNodeResponseTests.randomRepositoryStats;
import static org.elasticsearch.xpack.stateless.metering.action.GetBlobStoreStatsNodeResponseTests.randomRequestNames;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TransportGetBlobStoreStatsActionTests extends ESTestCase {

    public void testComputeObsRepositoryStats() {
        final RepositoriesService repositoriesService = mock(RepositoriesService.class);
        final Set<String> requestNames = randomRequestNames();
        final List<RepositoryStatsSnapshot> repositoryStatsSnapshots = randomRepositoryStatsSnapshots(
            randomAlphaOfLengthBetween(1, 5),
            requestNames
        );
        when(repositoriesService.repositoriesStats()).thenReturn(repositoryStatsSnapshots);

        final RepositoryStats repositoryStats = TransportGetBlobStoreStatsAction.computeObsRepositoryStats(repositoriesService);

        if (repositoryStatsSnapshots.isEmpty()) {
            assertThat(repositoryStats.actionStats, anEmptyMap());
        } else {
            assertThat(repositoryStats.actionStats.keySet(), equalTo(requestNames));
        }

        repositoryStats.actionStats.forEach((k, v) -> {
            BlobStoreActionStats expectedStats = BlobStoreActionStats.ZERO;
            for (var repositoryStatsSnapshot : repositoryStatsSnapshots) {
                expectedStats = repositoryStatsSnapshot.getRepositoryStats().actionStats.get(k).add(expectedStats);
            }
            assertThat("incorrect count for " + k, v, equalTo(expectedStats));
        });
    }

    private List<RepositoryStatsSnapshot> randomRepositoryStatsSnapshots(String repositoryType, Set<String> requestNames) {
        final boolean archived = randomBoolean();
        return randomList(
            0,
            3,
            () -> new RepositoryStatsSnapshot(
                new RepositoryInfo(randomUnicodeOfLength(10), randomAlphaOfLength(8), repositoryType, Map.of(), randomNonNegativeLong()),
                randomRepositoryStats(requestNames),
                archived ? randomNonNegativeLong() : UNKNOWN_CLUSTER_VERSION,
                archived
            )
        );
    }
}
