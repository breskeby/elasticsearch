/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

import org.elasticsearch.cluster.EstimatedHeapUsageCollector;
import org.elasticsearch.cluster.metadata.TemplateDecoratorProvider;
import org.elasticsearch.index.mapper.RootObjectMapperNamespaceValidator;
import org.elasticsearch.repositories.SnapshotShardContextFactory;
import org.elasticsearch.snapshots.IndexMetadataRestoreTransformer;
import org.elasticsearch.xpack.stateless.allocation.StatelessHeapUsageCollector;
import org.elasticsearch.xpack.stateless.mapper.ServerlessRootObjectMapperNamespaceValidator;
import org.elasticsearch.xpack.stateless.recovery.StatelessRestoreTransformer;
import org.elasticsearch.xpack.stateless.snapshots.StatelessSnapshotShardContextFactory;
import org.elasticsearch.xpack.stateless.templates.StatelessTemplateSettingsDecoratorProvider;

module org.elasticsearch.xpack.stateless {
    requires org.elasticsearch.base;
    requires org.elasticsearch.blobcache;
    requires org.elasticsearch.logging;
    requires org.elasticsearch.server;
    requires org.elasticsearch.xcore;
    requires org.elasticsearch.xcontent;

    requires org.elasticsearch.serverless.constants;
    requires org.elasticsearch.serverless.stateless.api;

    requires org.apache.logging.log4j;

    requires org.apache.lucene.core;
    requires org.apache.log4j;

    // TODO: remove unnecessary "to" clauses ES-13786
    exports org.elasticsearch.xpack.stateless to org.elasticsearch.server, org.elasticsearch.serverless.stateless;
    exports org.elasticsearch.xpack.stateless.action to org.elasticsearch.server, org.elasticsearch.serverless.stateless;
    exports org.elasticsearch.xpack.stateless.mapper to org.elasticsearch.server, org.elasticsearch.serverless.stateless;
    exports org.elasticsearch.xpack.stateless.xpack to org.elasticsearch.server, org.elasticsearch.serverless.stateless;
    exports org.elasticsearch.xpack.stateless.recovery to org.elasticsearch.server, org.elasticsearch.serverless.stateless;
    exports org.elasticsearch.xpack.stateless.commits to org.elasticsearch.server, org.elasticsearch.serverless.stateless;
    exports org.elasticsearch.xpack.stateless.objectstore to org.elasticsearch.server, org.elasticsearch.serverless.stateless;
    exports org.elasticsearch.xpack.stateless.cache to org.elasticsearch.server, org.elasticsearch.serverless.stateless;
    exports org.elasticsearch.xpack.stateless.cache.action to org.elasticsearch.server, org.elasticsearch.serverless.stateless;
    exports org.elasticsearch.xpack.stateless.metering.action to org.elasticsearch.server, org.elasticsearch.serverless.stateless;
    exports org.elasticsearch.xpack.stateless.reshard to org.elasticsearch.server, org.elasticsearch.serverless.stateless;
    exports org.elasticsearch.xpack.stateless.allocation to org.elasticsearch.server; // For StatelessHeapUsageCollector

    exports org.elasticsearch.xpack.stateless.autoscaling;
    exports org.elasticsearch.xpack.stateless.autoscaling.indexing;
    exports org.elasticsearch.xpack.stateless.autoscaling.search;
    exports org.elasticsearch.xpack.stateless.autoscaling.memory;
    exports org.elasticsearch.xpack.stateless.lucene.stats to org.elasticsearch.server, org.elasticsearch.serverless.stateless;
    exports org.elasticsearch.xpack.stateless.cluster.coordination to org.elasticsearch.server, org.elasticsearch.serverless.stateless;
    exports org.elasticsearch.xpack.stateless.autoscaling.search.load;
    exports org.elasticsearch.xpack.stateless.engine to org.elasticsearch.server, org.elasticsearch.serverless.stateless;
    exports org.elasticsearch.xpack.stateless.multiproject to org.elasticsearch.server, org.elasticsearch.serverless.stateless; // For
                                                                                                                                // PrimaryTermAndGeneration
    exports org.elasticsearch.xpack.stateless.recovery.shardinfo to org.elasticsearch.server; // For PrimaryTermAndGeneration
    exports org.elasticsearch.xpack.stateless.snapshots to org.elasticsearch.server; // for stateless snapshots
    exports org.elasticsearch.xpack.stateless.templates to org.elasticsearch.server;

    provides org.elasticsearch.action.search.OnlinePrewarmingServiceProvider
        with
            org.elasticsearch.xpack.stateless.cache.StatelessOnlinePrewarmingServiceProvider;
    provides EstimatedHeapUsageCollector with StatelessHeapUsageCollector;
    provides IndexMetadataRestoreTransformer with StatelessRestoreTransformer;
    provides RootObjectMapperNamespaceValidator with ServerlessRootObjectMapperNamespaceValidator;
    provides SnapshotShardContextFactory with StatelessSnapshotShardContextFactory;
    provides TemplateDecoratorProvider with StatelessTemplateSettingsDecoratorProvider;
}
