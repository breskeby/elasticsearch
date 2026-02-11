/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.stateless.autoscaling;

import org.elasticsearch.common.io.stream.Writeable;
import org.elasticsearch.xcontent.ToXContentObject;
import org.elasticsearch.xcontent.XContentBuilder;

import java.io.IOException;

public interface AutoscalingMetrics extends ToXContentObject, Writeable {
    default XContentBuilder serializeMetric(XContentBuilder builder, long value, MetricQuality quality) throws IOException {
        builder.startObject();
        builder.field("value", value);
        builder.field("quality", quality.getLabel());
        builder.endObject();
        return builder;
    }

    default XContentBuilder serializeMetric(XContentBuilder builder, double value, MetricQuality quality) throws IOException {
        builder.startObject();
        builder.field("value", value);
        builder.field("quality", quality.getLabel());
        builder.endObject();
        return builder;
    }

    default XContentBuilder serializeMetric(XContentBuilder builder, String name, long value, MetricQuality quality) throws IOException {
        builder.startObject(name);
        builder.field("value", value);
        builder.field("quality", quality.getLabel());
        builder.endObject();
        return builder;
    }

    default XContentBuilder serializeMetric(XContentBuilder builder, String nodeID, double value, MetricQuality quality)
        throws IOException {
        builder.startObject();
        builder.field("nodeID", nodeID);
        builder.field("value", value);
        builder.field("quality", quality.getLabel());
        builder.endObject();
        return builder;
    }
}
