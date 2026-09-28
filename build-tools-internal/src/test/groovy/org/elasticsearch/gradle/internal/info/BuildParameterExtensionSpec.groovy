/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the "Elastic License
 * 2.0", the "GNU Affero General Public License v3.0 only", and the "Server Side
 * Public License v 1"; you may not use this file except in compliance with, at
 * your election, the "Elastic License 2.0", the "GNU Affero General Public
 * License v3.0 only", or the "Server Side Public License, v 1".
 */

package org.elasticsearch.gradle.internal.info

import spock.lang.Ignore
import spock.lang.Specification

import org.gradle.api.Action
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.jvm.toolchain.JavaToolchainSpec
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Assert

import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

import static org.junit.Assert.fail

class BuildParameterExtensionSpec extends Specification {

    ProjectBuilder projectBuilder = new ProjectBuilder()

    def "bwcTestsEnabled can be overridden after creation"() {
        given:
        def project = projectBuilder.build()
        def providers = project.providers
        def buildParams = extension(project, providers, true, false, true)

        expect:
        buildParams.bwcTestsEnabled.get()

        when:
        buildParams.bwcTestsEnabled.set(false)

        then:
        buildParams.bwcTestsEnabled.get() == false
    }

    def "ciSplitBuild is exposed through the typed extension"() {
        given:
        def project = projectBuilder.build()
        def providers = project.providers

        expect:
        extension(project, providers, true, true, false).ciSplitBuild
        extension(project, providers, true, false, true).ciSplitBuild == false
    }

    def "onReleaseBuild only executes action for release builds"() {
        given:
        def releaseProject = projectBuilder.build()
        def snapshotProject = projectBuilder.build()
        def releaseBuildParams = extension(releaseProject, releaseProject.providers, false, false, true)
        def snapshotBuildParams = extension(snapshotProject, snapshotProject.providers, true, false, true)
        Action<BuildParameterExtension> releaseAction = Mock()
        Action<BuildParameterExtension> snapshotAction = Mock()

        when:
        releaseBuildParams.onReleaseBuild(releaseAction)
        snapshotBuildParams.onReleaseBuild(snapshotAction)

        then:
        1 * releaseAction.execute(releaseBuildParams)
        0 * snapshotAction.execute(_)
    }

    @Ignore
    def "#getterName is cached anc concurrently accessible"() {
        given:
        def project = projectBuilder.build()
        def providers = project.getProviders()
        def buildParams = extension(project, providers, true, false, true)
        int numberOfThreads = 10

        when:
        var service = Executors.newFixedThreadPool(numberOfThreads)
        var latch = new CountDownLatch(numberOfThreads)
        def testedProvider = buildParams."$getterName"()
        def futures = (1..numberOfThreads).collect {
            service.submit(
                () -> {
                    try {
                        testedProvider.get()
                    } catch (AssertionError e) {
                        latch.countDown()
                        Assert.fail("Accessing cached provider more than once")
                    }
                    latch.countDown()
                }
            )
        }
        latch.await(10, TimeUnit.SECONDS)

        then:
        futures.collect { it.state() }.any() { it == Future.State.FAILED } == false

        where:
        getterName << [
            "getRuntimeJavaHome",
            "getJavaToolChainSpec",
            "getRuntimeJavaDetails",
            "getRuntimeJavaVersion",
            "getBwcVersionsProvider"
        ]
    }

    private BuildParameterExtension extension(
        Project project,
        ProviderFactory providers,
        boolean snapshotBuild,
        boolean ciSplitBuild,
        boolean bwcTestsEnabled
    ) {
        def runtimeJava = new RuntimeJava(
            providerMock(new File("/tmp/runtime-java")),
            providerMock(JavaVersion.VERSION_11),
            providerMock("vendor details"),
            true
        )
        def toolchainSpec = providerMock(Mock(Action<JavaToolchainSpec>))
        def gitRevision = providerMock("git-revision")
        def gitOrigin = providerMock("git-origin")
        def testSeed = providerMock("deadbeef:0")
        def bwcVersions = providerMock(Mock(org.elasticsearch.gradle.internal.BwcVersions))
        def bwcTestsEnabledProperty = project.objects.property(Boolean)
        bwcTestsEnabledProperty.convention(bwcTestsEnabled)

        return project.extensions.create(
            BuildParameterExtension.class,
            "buildParameters",
            DefaultBuildParameterExtension.class,
            providers,
            runtimeJava,
            toolchainSpec,
            [Mock(JavaHome), Mock(JavaHome)],
            JavaVersion.VERSION_11,
            JavaVersion.VERSION_11,
            JavaVersion.VERSION_11,
            gitRevision,
            gitOrigin,
            testSeed,
            false,
            5,
            snapshotBuild,
            ciSplitBuild,
            bwcTestsEnabledProperty,
            bwcVersions
        )
    }

    private <T> Provider<T> providerMock(T value) {
        Provider<T> provider = Mock(Provider)
        AtomicInteger counter = new AtomicInteger(0)
        provider.getOrNull() >> {
            return counter.getAndIncrement() == 1 ? fail("Accessing cached provider more than once") : value
        }
        provider.get() >> {
            fail("Accessing cached provider directly")
        }
        return provider
    }
}
