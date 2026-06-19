/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the "Elastic License
 * 2.0", the "GNU Affero General Public License v3.0 only", and the "Server Side
 * Public License v 1"; you may not use this file except in compliance with, at
 * your election, the "Elastic License 2.0", the "GNU Affero General Public
 * License v3.0 only", or the "Server Side Public License, v 1".
 */

package org.elasticsearch.gradle.internal;

import org.elasticsearch.gradle.VersionProperties;
import org.elasticsearch.gradle.internal.conventions.precommit.PrecommitTaskPlugin;
import org.elasticsearch.gradle.internal.info.BuildParameterExtension;
import org.elasticsearch.gradle.internal.precommit.CheckForbiddenApisTask;
import org.elasticsearch.gradle.internal.info.GlobalBuildInfoPlugin;
import org.elasticsearch.gradle.internal.test.MutedTestPlugin;
import org.elasticsearch.gradle.internal.test.TestUtil;
import org.elasticsearch.gradle.test.SystemPropertyCommandLineArgumentProvider;
import org.gradle.api.JavaVersion;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.file.FileCollection;
import org.gradle.api.plugins.JavaBasePlugin;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.compile.AbstractCompile;
import org.gradle.api.tasks.compile.CompileOptions;
import org.gradle.api.tasks.compile.GroovyCompile;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.api.tasks.javadoc.Javadoc;
import org.gradle.api.tasks.testing.Test;
import org.gradle.external.javadoc.CoreJavadocOptions;
import org.gradle.jvm.toolchain.JavaLanguageVersion;
import org.gradle.jvm.toolchain.JavaToolchainService;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import javax.inject.Inject;

/**
 * A wrapper around Gradle's Java Base plugin that applies our
 * common configuration for production code.
 */
public class ElasticsearchJavaBasePlugin implements Plugin<Project> {

    private final JavaToolchainService javaToolchains;
    private BuildParameterExtension buildParams;

    @Inject
    ElasticsearchJavaBasePlugin(JavaToolchainService javaToolchains) {
        this.javaToolchains = javaToolchains;
    }

    @Override
    public void apply(Project project) {
        // make sure the global build info plugin is applied to the root project
        project.getRootProject().getPluginManager().apply(GlobalBuildInfoPlugin.class);
        buildParams = project.getRootProject().getExtensions().getByType(BuildParameterExtension.class);
        project.getPluginManager().apply(JavaBasePlugin.class);
        // common repositories setup
        project.getPluginManager().apply(RepositoriesSetupPlugin.class);
        project.getPluginManager().apply(ElasticsearchTestBasePlugin.class);
        project.getPluginManager().apply(PrecommitTaskPlugin.class);
        project.getPluginManager().apply(MutedTestPlugin.class);
        configureCompile(project);
        configureInputNormalization(project);
        configureNativeLibraryPath(project);

        // convenience access to common versions used in dependencies
        project.getExtensions().getExtraProperties().set("versions", VersionProperties.getVersions());
    }

    /**
     * Adds compiler settings to the project
     */
    public void configureCompile(Project project) {
        project.getExtensions().getExtraProperties().set("compactProfile", "full");
        JavaPluginExtension java = project.getExtensions().getByType(JavaPluginExtension.class);
        if (buildParams.getJavaToolChainSpec().getOrNull() != null) {
            java.toolchain(buildParams.getJavaToolChainSpec().get());
        }
        java.setSourceCompatibility(buildParams.getMinimumRuntimeVersion());
        java.setTargetCompatibility(buildParams.getMinimumRuntimeVersion());
        project.getTasks().withType(JavaCompile.class).configureEach(compileTask -> {
            compileTask.getJavaCompiler().set(javaToolchains.compilerFor(spec -> {
                spec.getLanguageVersion().set(JavaLanguageVersion.of(buildParams.getMinimumRuntimeVersion().getMajorVersion()));
            }));

            CompileOptions compileOptions = compileTask.getOptions();
            /*
             * -path because gradle will send in paths that don't always exist.
             * -missing because we have tons of missing @returns and @param.
             * -serial because we don't use java serialization.
             */
            // don't even think about passing args with -J-xxx, oracle will ask you to submit a bug report :)
            // fail on all javac warnings.
            // TODO Discuss moving compileOptions.getCompilerArgs() to use provider api with Gradle team.
            List<String> compilerArgs = compileOptions.getCompilerArgs();
            compilerArgs.add("-Werror");
            int compilerMajor = Integer.parseInt(buildParams.getMinimumRuntimeVersion().getMajorVersion());
            String xlintExclusions = "all,-path,-serial,-options,-deprecation,-try,-removal,-processing";
            if (compilerMajor >= 22) {
                xlintExclusions += ",-incubating";
            }
            compilerArgs.add("-Xlint:" + xlintExclusions);
            compilerArgs.add("-Xdoclint:all");
            compilerArgs.add("-Xdoclint:-missing");
            compileOptions.setEncoding("UTF-8");
            compileOptions.setIncremental(true);
            // workaround for https://github.com/gradle/gradle/issues/14141
            compileTask.getConventionMapping().map("sourceCompatibility", () -> java.getSourceCompatibility().toString());
            compileTask.getConventionMapping().map("targetCompatibility", () -> java.getTargetCompatibility().toString());
            compileOptions.getRelease().set(releaseVersionProviderFromCompileTask(project, compileTask));
            compileOptions.setIncremental(buildParams.getCi() == false);
        });
        // also apply release flag to groovy, which is used in build-tools
        project.getTasks().withType(GroovyCompile.class).configureEach(compileTask -> {
            // TODO: this probably shouldn't apply to groovy at all?
            compileTask.getOptions().getRelease().set(releaseVersionProviderFromCompileTask(project, compileTask));
        });
    }

    /**
     * Apply runtime classpath input normalization so that changes in JAR manifests don't break build cacheability
     */
    public static void configureInputNormalization(Project project) {
        project.getNormalization().getRuntimeClasspath().ignore("META-INF/MANIFEST.MF");
        project.getNormalization().getRuntimeClasspath().ignore("IMPL-JARS/**/META-INF/MANIFEST.MF");
    }

    private static void configureNativeLibraryPath(Project project) {
        String nativeProject = ":libs:native:native-libraries";
        Configuration nativeConfig = project.getConfigurations().create("nativeLibs");
        nativeConfig.defaultDependencies(deps -> {
            deps.add(project.getDependencies().project(Map.of("path", nativeProject, "configuration", "default")));
        });
        // This input to the following lambda needs to be serializable. Configuration is not serializable, but FileCollection is.
        FileCollection nativeConfigFiles = nativeConfig;

        project.getTasks().withType(Test.class).configureEach(test -> {
            var systemProperties = test.getExtensions().getByType(SystemPropertyCommandLineArgumentProvider.class);
            var libraryPath = (Supplier<String>) () -> TestUtil.getTestLibraryPath(nativeConfigFiles.getAsPath());

            test.dependsOn(nativeConfigFiles);
            systemProperties.systemProperty("es.nativelibs.path", libraryPath);
        });
    }

    /**
     * Configures the project so that source code can use {@code java.lang.foreign} types
     * (e.g. {@code MemorySegment}) without {@code --enable-preview} on JDK 21. On JDK 22+
     * the Foreign Function and Memory API is standard, so this is a no-op.
     *
     * <p> Works by patching {@code java.base} at compile time with a stub JAR whose
     * {@code java.lang.foreign} classes have the {@code @PreviewFeature} annotation
     * stripped. Also enables forbidden-API checking for renamed preview APIs, so that
     * direct usage of methods like {@code getUtf8String} or {@code allocateUtf8String}
     * is caught at build time. Call from a project's {@code build.gradle}:
     * <pre>{@code
     *   ElasticsearchJavaBasePlugin.enableForeignAccess(project)
     * }</pre>
     */
    public static void enableForeignAccess(Project project) {
        project.getTasks().withType(JavaCompile.class).configureEach(compileTask -> {
            compileTask.doFirst(t -> {
                int release = taskRelease(project, compileTask.getOptions().getRelease());
                if (release == 21) {
                    Path jarPath = extractForeignApiJar(project);
                    compileTask.getOptions().getCompilerArgs().add("--patch-module");
                    compileTask.getOptions().getCompilerArgs().add("java.base=" + jarPath);
                }
            });
        });
        project.getTasks().withType(Javadoc.class).configureEach(javadocTask -> {
            javadocTask.doFirst(t -> {
                int release = minimumRuntimeVersion(project);
                if (release == 21) {
                    Path jarPath = extractForeignApiJar(project);
                    CoreJavadocOptions options = (CoreJavadocOptions) javadocTask.getOptions();
                    options.addStringOption("-patch-module", "java.base=" + jarPath);
                }
            });
        });
        project.getTasks().withType(CheckForbiddenApisTask.class).configureEach(CheckForbiddenApisTask::checkForeignApiUsage);
    }

    private static int taskRelease(Project project, Property<Integer> releaseProperty) {
        return releaseProperty.getOrElse(minimumRuntimeVersion(project));
    }

    private static int minimumRuntimeVersion(Project project) {
        BuildParameterExtension params = project.getRootProject().getExtensions().getByType(BuildParameterExtension.class);
        return Integer.parseInt(params.getMinimumRuntimeVersion().getMajorVersion());
    }

    private static Path extractForeignApiJar(Project project) {
        Path dest = project.getLayout().getBuildDirectory().getAsFile().get().toPath().resolve("jdk21-foreign-api.jar");
        if (Files.exists(dest)) {
            return dest;
        }
        try (InputStream is = ElasticsearchJavaBasePlugin.class.getResourceAsStream("/jdk/jdk21-foreign-api.jar")) {
            if (is == null) {
                throw new IllegalStateException("jdk21-foreign-api.jar resource not found on build classpath");
            }
            Files.createDirectories(dest.getParent());
            Path tmp = Files.createTempFile(dest.getParent(), "jdk21-foreign-api", ".jar.tmp");
            try {
                Files.copy(is, tmp, StandardCopyOption.REPLACE_EXISTING);
                try {
                    Files.move(tmp, dest, StandardCopyOption.ATOMIC_MOVE);
                } catch (IOException ignored) {
                    // another task won the race — dest already exists
                }
            } finally {
                Files.deleteIfExists(tmp);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to extract jdk21-foreign-api.jar", e);
        }
        return dest;
    }

    private static Provider<Integer> releaseVersionProviderFromCompileTask(Project project, AbstractCompile compileTask) {
        return project.provider(() -> {
            JavaVersion javaVersion = JavaVersion.toVersion(compileTask.getTargetCompatibility());
            return Integer.parseInt(javaVersion.getMajorVersion());
        });
    }

}
