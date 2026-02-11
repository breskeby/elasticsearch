/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.stateless.metering.action;

import org.elasticsearch.action.support.nodes.BaseNodeResponse;
import org.elasticsearch.cluster.node.DiscoveryNode;
import org.elasticsearch.common.blobstore.BlobStoreActionStats;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.util.Maps;
import org.elasticsearch.repositories.RepositoryStats;
import org.elasticsearch.xcontent.ToXContent;
import org.elasticsearch.xcontent.ToXContentFragment;
import org.elasticsearch.xcontent.XContentBuilder;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;

public class GetBlobStoreStatsNodeResponse extends BaseNodeResponse implements ToXContentFragment {

    private final RepositoryStats repositoryStats;
    private final RepositoryStats obsRepositoryStats;

    public GetBlobStoreStatsNodeResponse(StreamInput in) throws IOException {
        super(in);
        this.repositoryStats = new RepositoryStats(in);
        this.obsRepositoryStats = new RepositoryStats(in);
    }

    public GetBlobStoreStatsNodeResponse(DiscoveryNode node, RepositoryStats repositoryStats, RepositoryStats obsRepositoryStats) {
        super(node);
        this.repositoryStats = repositoryStats;
        this.obsRepositoryStats = obsRepositoryStats;
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, ToXContent.Params params) throws IOException {
        builder.startObject(getNode().getId());
        {
            builder.startObject("object_store_stats");
            builder.field("request_counts", getRequestCounts(repositoryStats));
            builder.endObject();
        }
        {
            builder.startObject("operational_backup_service_stats");
            builder.field("request_counts", getRequestCounts(obsRepositoryStats));
            builder.endObject();
        }
        builder.endObject();
        return builder;
    }

    static Map<String, Long> getRequestCounts(RepositoryStats repositoryStats) {
        return Maps.transformValues(repositoryStats.actionStats, BlobStoreActionStats::requests);
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        repositoryStats.writeTo(out);
        obsRepositoryStats.writeTo(out);
    }

    public RepositoryStats getRepositoryStats() {
        return repositoryStats;
    }

    public RepositoryStats getObsRepositoryStats() {
        return obsRepositoryStats;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GetBlobStoreStatsNodeResponse that = (GetBlobStoreStatsNodeResponse) o;
        return Objects.equals(getNode().getId(), that.getNode().getId())
            && Objects.equals(repositoryStats, that.repositoryStats)
            && Objects.equals(obsRepositoryStats, that.obsRepositoryStats);
    }

    @Override
    public int hashCode() {
        return Objects.hash(repositoryStats, obsRepositoryStats);
    }

    @Override
    public String toString() {
        return "GetBlobStoreStatsNodeResponse{" + "repositoryStats=" + repositoryStats + ", obsRepositoryStats=" + obsRepositoryStats + '}';
    }
}
