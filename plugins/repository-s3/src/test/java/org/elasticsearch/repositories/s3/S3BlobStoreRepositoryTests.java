/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.elasticsearch.repositories.s3;

import com.amazonaws.http.AmazonHttpClient;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import fixture.s3.S3HttpHandler;
import org.elasticsearch.action.ActionRunnable;
import org.elasticsearch.action.admin.indices.forcemerge.ForceMergeResponse;
import org.elasticsearch.action.support.PlainActionFuture;
import org.elasticsearch.cluster.metadata.IndexMetadata;
import org.elasticsearch.cluster.metadata.RepositoryMetadata;
import org.elasticsearch.cluster.service.ClusterService;
import org.elasticsearch.common.SuppressForbidden;
import org.elasticsearch.common.blobstore.BlobContainer;
import org.elasticsearch.common.blobstore.BlobPath;
import org.elasticsearch.common.blobstore.BlobStore;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.regex.Regex;
import org.elasticsearch.common.settings.MockSecureSettings;
import org.elasticsearch.common.settings.Setting;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.ByteSizeUnit;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.NamedXContentRegistry;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.repositories.RepositoriesService;
import org.elasticsearch.repositories.RepositoryData;
import org.elasticsearch.repositories.RepositoryMissingException;
import org.elasticsearch.repositories.RepositoryStats;
import org.elasticsearch.repositories.blobstore.BlobStoreRepository;
import org.elasticsearch.repositories.blobstore.ESMockAPIBasedRepositoryIntegTestCase;
import org.elasticsearch.snapshots.SnapshotId;
import org.elasticsearch.snapshots.SnapshotsService;
import org.elasticsearch.snapshots.mockstore.BlobStoreWrapper;
import org.elasticsearch.test.BackgroundIndexer;
import org.elasticsearch.test.ESIntegTestCase;
import org.elasticsearch.threadpool.ThreadPool;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.StreamSupport;

import static org.elasticsearch.test.hamcrest.ElasticsearchAssertions.assertAcked;
import static org.elasticsearch.test.hamcrest.ElasticsearchAssertions.assertHitCount;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.startsWith;

@SuppressForbidden(reason = "this test uses a HttpServer to emulate an S3 endpoint")
// Need to set up a new cluster for each test because cluster settings use randomized authentication settings
@ESIntegTestCase.ClusterScope(scope = ESIntegTestCase.Scope.TEST)
public class S3BlobStoreRepositoryTests extends ESMockAPIBasedRepositoryIntegTestCase {

    private static final TimeValue TEST_COOLDOWN_PERIOD = TimeValue.timeValueSeconds(10L);

    private String region;
    private String signerOverride;

    @Override
    public void setUp() throws Exception {
        if (randomBoolean()) {
            region = "test-region";
        }
        if (region != null && randomBoolean()) {
            signerOverride = randomFrom("AWS3SignerType", "AWS4SignerType");
        } else if (randomBoolean()) {
            signerOverride = "AWS3SignerType";
        }
        super.setUp();
    }

    @Override
    protected String repositoryType() {
        return S3Repository.TYPE;
    }

    @Override
    protected Settings repositorySettings() {
        return Settings.builder()
            .put(super.repositorySettings())
            .put(S3Repository.BUCKET_SETTING.getKey(), "bucket")
            .put(S3Repository.CLIENT_NAME.getKey(), "test")
            // Don't cache repository data because some tests manually modify the repository data
            .put(BlobStoreRepository.CACHE_REPOSITORY_DATA.getKey(), false)
            .build();
    }

    @Override
    protected Collection<Class<? extends Plugin>> nodePlugins() {
        return Collections.singletonList(TestS3RepositoryPlugin.class);
    }

    @Override
    protected Map<String, HttpHandler> createHttpHandlers() {
        return Collections.singletonMap("/bucket", new S3StatsHttpHandler(new S3BlobStoreHttpHandler("bucket")));
    }

    @Override
    protected HttpHandler createErroneousHttpHandler(final HttpHandler delegate) {
        return new S3StatsHttpHandler(new S3ErroneousHttpHandler(delegate, randomIntBetween(2, 3)));
    }

    @Override
    protected Settings nodeSettings(int nodeOrdinal) {
        final MockSecureSettings secureSettings = new MockSecureSettings();
        secureSettings.setString(S3ClientSettings.ACCESS_KEY_SETTING.getConcreteSettingForNamespace("test").getKey(), "access");
        secureSettings.setString(S3ClientSettings.SECRET_KEY_SETTING.getConcreteSettingForNamespace("test").getKey(), "secret");

        final Settings.Builder builder = Settings.builder()
            .put(ThreadPool.ESTIMATED_TIME_INTERVAL_SETTING.getKey(), 0) // We have tests that verify an exact wait time
            .put(S3ClientSettings.ENDPOINT_SETTING.getConcreteSettingForNamespace("test").getKey(), httpServerUrl())
            // Disable chunked encoding as it simplifies a lot the request parsing on the httpServer side
            .put(S3ClientSettings.DISABLE_CHUNKED_ENCODING.getConcreteSettingForNamespace("test").getKey(), true)
            // Disable request throttling because some random values in tests might generate too many failures for the S3 client
            .put(S3ClientSettings.USE_THROTTLE_RETRIES_SETTING.getConcreteSettingForNamespace("test").getKey(), false)
            .put(super.nodeSettings(nodeOrdinal))
            .setSecureSettings(secureSettings);

        if (signerOverride != null) {
            builder.put(S3ClientSettings.SIGNER_OVERRIDE.getConcreteSettingForNamespace("test").getKey(), signerOverride);
        }
        if (region != null) {
            builder.put(S3ClientSettings.REGION.getConcreteSettingForNamespace("test").getKey(), region);
        }
        return builder.build();
    }

    public void testEnforcedCooldownPeriod() throws IOException {
        final String repoName = createRepository(randomName(), Settings.builder().put(repositorySettings())
            .put(S3Repository.COOLDOWN_PERIOD.getKey(), TEST_COOLDOWN_PERIOD).build());

        final SnapshotId fakeOldSnapshot = client().admin().cluster().prepareCreateSnapshot(repoName, "snapshot-old")
            .setWaitForCompletion(true).setIndices().get().getSnapshotInfo().snapshotId();
        final RepositoriesService repositoriesService = internalCluster().getCurrentMasterNodeInstance(RepositoriesService.class);
        final BlobStoreRepository repository = (BlobStoreRepository) repositoriesService.repository(repoName);
        final RepositoryData repositoryData =
            PlainActionFuture.get(f -> repository.threadPool().generic().execute(() -> repository.getRepositoryData(f)));
        final RepositoryData modifiedRepositoryData = repositoryData.withVersions(Collections.singletonMap(fakeOldSnapshot,
            SnapshotsService.SHARD_GEN_IN_REPO_DATA_VERSION.minimumCompatibilityVersion()));
        final BytesReference serialized =
            BytesReference.bytes(modifiedRepositoryData.snapshotsToXContent(XContentFactory.jsonBuilder(), false));
        PlainActionFuture.get(f -> repository.threadPool().generic().execute(ActionRunnable.run(f, () -> {
            try (InputStream stream = serialized.streamInput()) {
                repository.blobStore().blobContainer(repository.basePath()).writeBlobAtomic(
                    BlobStoreRepository.INDEX_FILE_PREFIX + modifiedRepositoryData.getGenId(), stream, serialized.length(), true);
            }
        })));

        final String newSnapshotName = "snapshot-new";
        final long beforeThrottledSnapshot = repository.threadPool().relativeTimeInNanos();
        client().admin().cluster().prepareCreateSnapshot(repoName, newSnapshotName).setWaitForCompletion(true).setIndices().get();
        assertThat(repository.threadPool().relativeTimeInNanos() - beforeThrottledSnapshot, greaterThan(TEST_COOLDOWN_PERIOD.getNanos()));

        final long beforeThrottledDelete = repository.threadPool().relativeTimeInNanos();
        client().admin().cluster().prepareDeleteSnapshot(repoName, newSnapshotName).get();
        assertThat(repository.threadPool().relativeTimeInNanos() - beforeThrottledDelete, greaterThan(TEST_COOLDOWN_PERIOD.getNanos()));

        final long beforeFastDelete = repository.threadPool().relativeTimeInNanos();
        client().admin().cluster().prepareDeleteSnapshot(repoName, fakeOldSnapshot.getName()).get();
        assertThat(repository.threadPool().relativeTimeInNanos() - beforeFastDelete, lessThan(TEST_COOLDOWN_PERIOD.getNanos()));
    }

    public void testRequestStats() throws Exception {
        final String repository = createRepository(randomName());
        final String index = "index-no-merges";
        createIndex(index, Settings.builder()
            .put(IndexMetadata.SETTING_NUMBER_OF_SHARDS, 1)
            .put(IndexMetadata.SETTING_NUMBER_OF_REPLICAS, 0)
            .build());

        final long nbDocs = randomLongBetween(100, 1000);
        try (BackgroundIndexer indexer = new BackgroundIndexer(index, "_doc", client(), (int) nbDocs)) {
            waitForDocs(nbDocs, indexer);
        }

        flushAndRefresh(index);
        ForceMergeResponse forceMerge = client().admin().indices().prepareForceMerge(index).setFlush(true).setMaxNumSegments(1).get();
        assertThat(forceMerge.getSuccessfulShards(), equalTo(1));
        assertHitCount(client().prepareSearch(index).setSize(0).setTrackTotalHits(true).get(), nbDocs);

        final String snapshot = "snapshot";
        assertSuccessfulSnapshot(client().admin().cluster().prepareCreateSnapshot(repository, snapshot)
            .setWaitForCompletion(true).setIndices(index));

        assertAcked(client().admin().indices().prepareDelete(index));

        assertSuccessfulRestore(client().admin().cluster().prepareRestoreSnapshot(repository, snapshot).setWaitForCompletion(true));
        ensureGreen(index);
        assertHitCount(client().prepareSearch(index).setSize(0).setTrackTotalHits(true).get(), nbDocs);

        assertAcked(client().admin().cluster().prepareDeleteSnapshot(repository, snapshot).get());

        final RepositoryStats repositoryStats = StreamSupport.stream(
            internalCluster().getInstances(RepositoriesService.class).spliterator(), false)
            .map(repositoriesService -> {
                try {
                    return repositoriesService.repository(repository);
                } catch (RepositoryMissingException e) {
                    return null;
                }
            })
            .filter(r -> r != null)
            .map(r -> r.stats())
            .reduce((s1, s2) -> s1.merge(s2))
            .get();
        final long sdkGetCalls = repositoryStats.requestCounts.get("GET");
        final long sdkListCalls = repositoryStats.requestCounts.get("LIST");

        final long getCalls = handlers.values().stream()
            .mapToLong(h -> {
                while (h instanceof DelegatingHttpHandler) {
                    if (h instanceof S3StatsHttpHandler) {
                        return ((S3StatsHttpHandler) h).getCalls.get();
                    }
                    h = ((DelegatingHttpHandler) h).getDelegate();
                }
                assert false;
                return 0L;
            })
            .sum();
        final long listCalls = handlers.values().stream()
            .mapToLong(h -> {
                while (h instanceof DelegatingHttpHandler) {
                    if (h instanceof S3StatsHttpHandler) {
                        return ((S3StatsHttpHandler) h).listCalls.get();
                    }
                    h = ((DelegatingHttpHandler) h).getDelegate();
                }
                assert false;
                return 0L;
            })
            .sum();

        assertEquals("SDK sent " + sdkGetCalls + " GET calls and handler measured " + getCalls + " GET calls", getCalls, sdkGetCalls);
        assertEquals("SDK sent " + sdkListCalls + " LIST calls and handler measured " + listCalls + " LIST calls", listCalls, sdkListCalls);
    }

    /**
     * S3RepositoryPlugin that allows to disable chunked encoding and to set a low threshold between single upload and multipart upload.
     */
    public static class TestS3RepositoryPlugin extends S3RepositoryPlugin {

        public TestS3RepositoryPlugin(final Settings settings) {
            super(settings);
        }

        @Override
        public List<Setting<?>> getSettings() {
            final List<Setting<?>> settings = new ArrayList<>(super.getSettings());
            settings.add(S3ClientSettings.DISABLE_CHUNKED_ENCODING);
            return settings;
        }

        @Override
        protected S3Repository createRepository(RepositoryMetadata metadata, NamedXContentRegistry registry,
                                                ClusterService clusterService) {
            return new S3Repository(metadata, registry, service, clusterService) {

                @Override
                public BlobStore blobStore() {
                    return new BlobStoreWrapper(super.blobStore()) {
                        @Override
                        public BlobContainer blobContainer(final BlobPath path) {
                            return new S3BlobContainer(path, (S3BlobStore) delegate()) {
                                @Override
                                long getLargeBlobThresholdInBytes() {
                                    return ByteSizeUnit.MB.toBytes(1L);
                                }

                                @Override
                                void ensureMultiPartUploadSize(long blobSize) {
                                }
                            };
                        }
                    };
                }
            };
        }
    }

    @SuppressForbidden(reason = "this test uses a HttpHandler to emulate an S3 endpoint")
    private class S3BlobStoreHttpHandler extends S3HttpHandler implements BlobStoreHttpHandler {

        S3BlobStoreHttpHandler(final String bucket) {
            super(bucket);
        }

        @Override
        public void handle(final HttpExchange exchange) throws IOException {
            validateAuthHeader(exchange);
            super.handle(exchange);
        }

        private void validateAuthHeader(HttpExchange exchange) {
            final String authorizationHeaderV4 = exchange.getRequestHeaders().getFirst("Authorization");
            final String authorizationHeaderV3 = exchange.getRequestHeaders().getFirst("X-amzn-authorization");

            if ("AWS3SignerType".equals(signerOverride)) {
                assertThat(authorizationHeaderV3, startsWith("AWS3"));
            } else if ("AWS4SignerType".equals(signerOverride)) {
                assertThat(authorizationHeaderV4, containsString("aws4_request"));
            }
            if (region != null && authorizationHeaderV4 != null) {
                assertThat(authorizationHeaderV4, containsString("/" + region + "/s3/"));
            }
        }
    }

    /**
     * HTTP handler that injects random S3 service errors
     *
     * Note: it is not a good idea to allow this handler to simulate too many errors as it would
     * slow down the test suite.
     */
    @SuppressForbidden(reason = "this test uses a HttpServer to emulate an S3 endpoint")
    private static class S3ErroneousHttpHandler extends ErroneousHttpHandler {

        S3ErroneousHttpHandler(final HttpHandler delegate, final int maxErrorsPerRequest) {
            super(delegate, maxErrorsPerRequest);
        }

        @Override
        protected String requestUniqueId(final HttpExchange exchange) {
            // Amazon SDK client provides a unique ID per request
            return exchange.getRequestHeaders().getFirst(AmazonHttpClient.HEADER_SDK_TRANSACTION_ID);
        }
    }

    @SuppressForbidden(reason = "this test uses a HttpServer to emulate an S3 endpoint")
    private static class S3StatsHttpHandler implements DelegatingHttpHandler {

        private final HttpHandler delegate;

        public final AtomicLong getCalls = new AtomicLong();
        public final AtomicLong listCalls = new AtomicLong();

        S3StatsHttpHandler(final HttpHandler delegate) {
            this.delegate = delegate;
        }

        @Override
        public HttpHandler getDelegate() {
            return delegate;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            final String request = exchange.getRequestMethod() + " " + exchange.getRequestURI().toString();
            if (Regex.simpleMatch("GET /*/?prefix=*", request)) {
                listCalls.incrementAndGet();
            } else if (Regex.simpleMatch("GET /*/*", request)) {
                getCalls.incrementAndGet();
            }
            delegate.handle(exchange);
        }
    }
}
