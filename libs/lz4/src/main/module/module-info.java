/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */
// @formatter:off
@SuppressWarnings({"requires-automatic","requires-transitive-automatic","module"})
module org.elasticsearch.lz4 {
    requires transitive org.lz4.java;

    exports org.elasticsearch.lz4;
}
// @formatter:on
