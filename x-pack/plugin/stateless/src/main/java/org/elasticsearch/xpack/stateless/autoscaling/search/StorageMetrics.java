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

public record StorageMetrics(
    long maxInteractiveSizeInBytes,
    long totalInteractiveDataSizeInBytes,
    long totalDataSizeInBytes,
    MetricQuality quality
) implements AutoscalingMetrics {

    public StorageMetrics(StreamInput in) throws IOException {
        this(in.readLong(), in.readLong(), in.readLong(), MetricQuality.readFrom(in));
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        out.writeLong(maxInteractiveSizeInBytes);
        out.writeLong(totalInteractiveDataSizeInBytes);
        out.writeLong(totalDataSizeInBytes);
        quality.writeTo(out);
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.field("max_interactive_data_size_in_bytes");
        serializeMetric(builder, maxInteractiveSizeInBytes, quality);

        builder.field("total_interactive_data_size_in_bytes");
        serializeMetric(builder, totalInteractiveDataSizeInBytes, quality);

        builder.field("total_data_size_in_bytes");
        serializeMetric(builder, totalDataSizeInBytes, quality);
        return builder;
    }
}
