/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */
package org.elasticsearch.xpack.monitoring.test;

import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.script.MockScriptEngine;
import org.elasticsearch.script.MockScriptPlugin;
import org.elasticsearch.script.ScriptEngineService;

import java.util.Collections;
import java.util.Map;
import java.util.function.Function;

/**
 * A mock script engine that registers itself under the 'painless' name so that watches that use it can still be used in tests.
 */
public class MockPainlessScriptEngine extends MockScriptEngine {

    public static final String NAME = "painless";

    public static class TestPlugin extends MockScriptPlugin {
        @Override
        public ScriptEngineService getScriptEngineService(Settings settings) {
            return new MockPainlessScriptEngine();
        }

        @Override
        protected Map<String, Function<Map<String, Object>, Object>> pluginScripts() {
            return Collections.emptyMap();
        }
    }

    @Override
    public String getType() {
        return NAME;
    }

    @Override
    public String getExtension() {
        return NAME;
    }

    @Override
    public Object compile(String name, String script, Map<String, String> params) {
        // We always return the script's source as it is
        return new MockCompiledScript(name, params, script, null);
    }

    @Override
    public boolean isInlineScriptEnabled() {
        return true;
    }
}
