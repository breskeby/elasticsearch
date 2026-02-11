/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.stateless.autoscaling.search;

import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.xcontent.XContentBuilder;
import org.elasticsearch.xpack.stateless.autoscaling.AutoscalingMetrics;
import org.elasticsearch.xpack.stateless.autoscaling.MetricQuality;

import java.io.IOException;

public record MaxShardCopies(int maxCopies, MetricQuality quality) implements AutoscalingMetrics {
    public MaxShardCopies(StreamInput in) throws IOException {
        this(in.readInt(), MetricQuality.readFrom(in));
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        out.writeInt(maxCopies);
        quality.writeTo(out);
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.field("max_shard_copies");
        serializeMetric(builder, maxCopies, quality);
        return builder;
    }
}
