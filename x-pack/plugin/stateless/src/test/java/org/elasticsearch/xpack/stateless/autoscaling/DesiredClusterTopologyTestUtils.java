/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.stateless.autoscaling;

import static org.elasticsearch.test.ESTestCase.randomFloat;
import static org.elasticsearch.test.ESTestCase.randomIntBetween;
import static org.elasticsearch.test.ESTestCase.randomLongBetween;

public class DesiredClusterTopologyTestUtils {

    public static DesiredClusterTopology randomDesiredClusterTopology() {
        return new DesiredClusterTopology(randomTierTopology(), randomTierTopology());
    }

    public static DesiredClusterTopology.TierTopology randomTierTopology() {
        return new DesiredClusterTopology.TierTopology(
            randomIntBetween(1, 10),
            randomLongBetween(1, 8_589_934_592L) + "b",
            randomFloat(),
            randomFloat(),
            randomFloat()
        );
    }
}
