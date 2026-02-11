/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.stateless.recovery;

import org.elasticsearch.cluster.metadata.IndexMetadata;
import org.elasticsearch.cluster.routing.allocation.ExistingShardsAllocator;
import org.elasticsearch.snapshots.IndexMetadataRestoreTransformer;

/**
 * This is a temporary class meant to remediate missing allocator settings in indices that have been snapshot. Affected indices
 * will be remediated via {@link RemedialAllocationSettingService}, but any snapshots taken before this remediation executes will
 * persist the incorrect settings in the snapshot data. If these snapshots are ever restored, then the remediation will be undone
 * and the indices restored may be subject to allocation issues. This transformer only needs to exist for as long as those snapshots
 * are around. Once they age out, this can be removed.
 */
public class StatelessRestoreTransformer implements IndexMetadataRestoreTransformer {
    @Override
    public IndexMetadata updateIndexMetadata(IndexMetadata original) {
        if (ExistingShardsAllocator.EXISTING_SHARDS_ALLOCATOR_SETTING.exists(original.getSettings()) == false) {
            // Remediate missing index setting
            return RemedialAllocationSettingService.addStatelessExistingShardsAllocatorSetting(original);
        }
        return original;
    }
}
