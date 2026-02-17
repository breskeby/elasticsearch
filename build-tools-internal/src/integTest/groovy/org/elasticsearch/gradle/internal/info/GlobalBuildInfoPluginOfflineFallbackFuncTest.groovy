/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the "Elastic License
 * 2.0", the "GNU Affero General Public License v3.0 only", and the "Server Side
 * Public License v 1"; you may not use this file except in compliance with, at
 * your election, the "Elastic License 2.0", the "GNU Affero General Public
 * License v3.0 only", or the "Server Side Public License, v 1".
 */
package org.elasticsearch.gradle.internal.info

import org.elasticsearch.gradle.fixtures.AbstractGradleFuncTest
import org.gradle.testkit.runner.TaskOutcome

class GlobalBuildInfoPluginOfflineFallbackFuncTest extends AbstractGradleFuncTest {

    def setup() {
        // This test intentionally runs with --offline and does not configure toolchain download repositories.
        // The build-params extension holds providers that are not configuration-cache serializable in this minimal build.
        configurationCacheCompatible = false

        file("build-tools-internal/version.properties") << """
            elasticsearch     = 9.1.0
            lucene            = 10.2.2

            bundled_jdk_vendor = openjdk
            bundled_jdk = 24+36@1f9ff9062db4449d8ca828c504ffae90
            minimumJdkVersion = 21
            minimumRuntimeJava = 21
            minimumCompilerJava = 21
        """

        file("server/src/main/java/org/elasticsearch/Version.java") << """
            package org.elasticsearch;
            public class Version {
                public static final Version V_8_17_8 = new Version(8_17_08_99);
                public static final Version V_8_18_0 = new Version(8_18_00_99);
                public static final Version V_8_18_1 = new Version(8_18_01_99);
                public static final Version V_8_18_2 = new Version(8_18_02_99);
                public static final Version V_8_18_3 = new Version(8_18_03_99);
                public static final Version V_8_19_0 = new Version(8_19_00_99);
                public static final Version V_9_0_0 = new Version(9_00_00_99);
                public static final Version V_9_0_1 = new Version(9_00_01_99);
                public static final Version V_9_0_2 = new Version(9_00_02_99);
                public static final Version V_9_0_3 = new Version(9_00_03_99);
                public static final Version V_9_1_0 = new Version(9_01_00_99);
                public static final Version CURRENT = V_9_1_0;
            }
        """.stripIndent()

        file("branches.json") << """
            {
              "branches": [
                { "branch": "main", "version": "9.1.0" }
              ]
            }
        """.stripIndent()

        buildFile << """
            plugins {
              id 'elasticsearch.global-build-info'
            }

            tasks.register("resolveBwcVersions") {
              doLast {
                println "UNRELEASED=" + project.buildParams.bwcVersions.unreleased
              }
            }
        """
    }

    def "falls back to repo-root branches.json when offline"() {
        when:
        def result = gradleRunner(
            "resolveBwcVersions",
            "--offline",
            "--warning-mode",
            "all",
            "-Porg.elasticsearch.build.branches-file-location=https://invalid.example.com/branches.json"
        ).build()

        then:
        result.task(":resolveBwcVersions").outcome == TaskOutcome.SUCCESS
        assertOutputContains(result.output, "Gradle is running in offline mode; cannot download branches.json")
        assertOutputContains(result.output, "Falling back to local branches.json")
        result.output.contains("Failed to download branches.json") == false
    }
}

