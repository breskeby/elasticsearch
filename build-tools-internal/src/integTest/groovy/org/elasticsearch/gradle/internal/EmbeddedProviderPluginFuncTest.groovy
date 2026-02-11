/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the "Elastic License
 * 2.0", the "GNU Affero General Public License v3.0 only", and the "Server Side
 * Public License v 1"; you may not use this file except in compliance with, at
 * your election, the "Elastic License 2.0", the "GNU Affero General Public
 * License v3.0 only", or the "Server Side Public License, v 1".
 */

package org.elasticsearch.gradle.internal

import org.elasticsearch.gradle.fixtures.AbstractGradleFuncTest
import org.gradle.testkit.runner.TaskOutcome

import java.util.zip.ZipEntry
import java.util.zip.ZipFile

class EmbeddedProviderPluginFuncTest extends AbstractGradleFuncTest {

    def "embedded provider fat jar does not include impl MANIFEST.MF"() {
        given:
        def implBuild = subProject(":impl")
        implBuild << """
            plugins {
              id 'java'
            }
            group = 'org.acme'
            version = '1.0'
        """
        file("impl/src/main/java/org/acme/Impl.java") << """
            package org.acme;
            public class Impl {
              public static String message() { return "hello"; }
            }
        """.stripIndent().stripTrailing()

        buildFile << """
            plugins {
              id 'java'
              id 'elasticsearch.embedded-providers'
            }
            group = 'org.acme'
            version = '1.0'

            embeddedProviders {
              impl 'test', project(':impl')
            }
        """
        file("src/main/java/org/acme/Main.java") << """
            package org.acme;
            public class Main { }
        """.stripIndent().stripTrailing()

        when:
        def result = gradleRunner("jar", "-g", gradleUserHome).build()

        then:
        result.task(":jar").outcome == TaskOutcome.SUCCESS
        result.task(":generateImplProviderImpl")?.outcome in [TaskOutcome.SUCCESS, TaskOutcome.UP_TO_DATE]
        file("build/libs/hello-world-1.0.jar").exists()

        and: "impl jar content is embedded but impl MANIFEST.MF is not"
        File jar = file("build/libs/hello-world-1.0.jar")
        boolean hasEmbeddedImplClass = false
        boolean hasEmbeddedImplManifest = false

        try (ZipFile zipFile = new ZipFile(jar)) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries()
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement()
                String name = entry.getName()
                if (name.startsWith("IMPL-JARS/test/") && name.endsWith(".class")) {
                    hasEmbeddedImplClass = true
                }
                if (name.startsWith("IMPL-JARS/test/") && name.endsWith("META-INF/MANIFEST.MF")) {
                    hasEmbeddedImplManifest = true
                }
            }
        }

        assert hasEmbeddedImplClass : "Expected at least one embedded impl .class under IMPL-JARS/test/"
        assert hasEmbeddedImplManifest == false : "Did not expect IMPL-JARS/test/**/META-INF/MANIFEST.MF in output jar"
    }
}

