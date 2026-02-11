/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.stateless.autoscaling.indexing;

import org.elasticsearch.action.ActionRequestValidationException;
import org.elasticsearch.action.support.master.MasterNodeRequest;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.core.TimeValue;
import org.elasticsearch.xpack.stateless.autoscaling.indexing.IngestionLoad.NodeIngestionLoad;

import java.io.IOException;
import java.util.Objects;

public class PublishNodeIngestLoadRequest extends MasterNodeRequest<PublishNodeIngestLoadRequest> {

    private final String nodeId;
    private final String nodeName;
    private final long seqNo;
    private final NodeIngestionLoad ingestionLoad;

    public PublishNodeIngestLoadRequest(String nodeId, String nodeName, long seqNo, NodeIngestionLoad ingestionLoad) {
        super(TimeValue.MINUS_ONE);
        this.nodeId = nodeId;
        this.nodeName = nodeName;
        this.seqNo = seqNo;
        this.ingestionLoad = ingestionLoad;
    }

    public PublishNodeIngestLoadRequest(StreamInput in) throws IOException {
        super(in);
        this.nodeId = in.readString();
        this.nodeName = in.readString();
        this.seqNo = in.readLong();
        this.ingestionLoad = NodeIngestionLoad.from(in);
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        out.writeString(nodeId);
        out.writeString(nodeName);
        out.writeLong(seqNo);
        ingestionLoad.writeTo(out);
    }

    public String getNodeId() {
        return nodeId;
    }

    public String getNodeName() {
        return nodeName;
    }

    public long getSeqNo() {
        return seqNo;
    }

    public NodeIngestionLoad getIngestionLoad() {
        return ingestionLoad;
    }

    @Override
    public ActionRequestValidationException validate() {
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PublishNodeIngestLoadRequest that = (PublishNodeIngestLoadRequest) o;
        return seqNo == that.seqNo
            && Objects.equals(that.ingestionLoad, ingestionLoad)
            && Objects.equals(nodeId, that.nodeId)
            && Objects.equals(nodeName, that.nodeName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nodeId, seqNo, ingestionLoad, nodeName);
    }

    @Override
    public String toString() {
        return "PublishNodeIngestLoadRequest{nodeId='"
            + nodeId
            + "', nodeName='"
            + nodeName
            + "', seqNo="
            + seqNo
            + ", ingestionLoad="
            + ingestionLoad
            + '}';
    }
}
