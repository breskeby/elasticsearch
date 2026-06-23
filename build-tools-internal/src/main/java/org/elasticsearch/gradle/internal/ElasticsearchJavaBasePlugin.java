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
import org.elasticsearch.gradle.internal.info.GlobalBuildInfoPlugin;
import org.elasticsearch.gradle.internal.precommit.CheckForbiddenApisTask;
import org.elasticsearch.gradle.internal.test.MutedTestPlugin;
import org.elasticsearch.gradle.internal.test.TestUtil;
import org.elasticsearch.gradle.test.SystemPropertyCommandLineArgumentProvider;
import org.gradle.api.JavaVersion;
import org.gradle.api.Named;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.RegularFile;
import org.gradle.api.plugins.JavaBasePlugin;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.compile.AbstractCompile;
import org.gradle.api.tasks.compile.CompileOptions;
import org.gradle.api.tasks.compile.GroovyCompile;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.api.tasks.javadoc.Javadoc;
import org.gradle.api.tasks.testing.Test;
import org.gradle.external.javadoc.CoreJavadocOptions;
import org.gradle.jvm.toolchain.JavaLanguageVersion;
import org.gradle.jvm.toolchain.JavaToolchainService;
import org.gradle.process.CommandLineArgumentProvider;

import java.io.File;
import java.util.Collections;
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

    private static final String EXTRACT_FOREIGN_API_TASK_NAME = "extractForeignApiJar";

    /**
     * Configures the project so that source code can use {@code java.lang.foreign} types
     * (e.g. {@code MemorySegment}) without {@code --enable-preview} on JDK 21. On JDK 22+
     * the Foreign Function and Memory API is standard, so this is a no-op.
     *
     * <p> Works by patching {@code java.base} at compile time with a stub JAR whose
     * {@code java.lang.foreign} classes have the {@code @PreviewFeature} annotation
     * stripped. Also enables forbidden-API checking for renamed preview APIs, so that
     * direct usage of methods like {@code getUtf8String} or {@code allocateUtf8String}
     * is caught at build time. Prefer applying the {@code elasticsearch.foreign-api}
     * plugin in a project's {@code build.gradle}:
     * <pre>{@code
     *   apply plugin: 'elasticsearch.foreign-api'
     * }</pre>
     */
    public static void enableForeignAccess(Project project) {
        int minRuntime = minimumRuntimeVersion(project);

        TaskProvider<ExtractForeignApiTask> extractTask = project.getTasks()
            .register(EXTRACT_FOREIGN_API_TASK_NAME, ExtractForeignApiTask.class, t -> {
                t.getOutputJar().set(project.getLayout().getBuildDirectory().file("jdk21-foreign-api.jar"));
                t.onlyIf("JDK 21 required for preview API stubs", task -> Runtime.version().feature() == 21);
            });

        Provider<RegularFile> jarFile = extractTask.flatMap(ExtractForeignApiTask::getOutputJar);

        project.getTasks().withType(JavaCompile.class).configureEach(compileTask -> {
            var provider = new ForeignAccessArgumentProvider(jarFile, compileTask.getOptions().getRelease(), minRuntime);
            compileTask.getOptions().getCompilerArgumentProviders().add(provider);
        });

        project.getTasks().withType(Javadoc.class).configureEach(javadocTask -> {
            if (minRuntime == 21) {
                javadocTask.dependsOn(extractTask);
                javadocTask.doFirst(t -> {
                    File jar = jarFile.get().getAsFile();
                    CoreJavadocOptions options = (CoreJavadocOptions) javadocTask.getOptions();
                    options.addStringOption("-patch-module", "java.base=" + jar.getAbsolutePath());
                });
            }
        });

        project.getTasks().withType(CheckForbiddenApisTask.class).configureEach(t -> t.checkForeignApiUsage(jarFile));
    }

    /**
     * Provides {@code --patch-module java.base=<jar>} compiler arguments when the
     * compile release is 21 and the stub JAR exists. CC-safe: all inputs are tracked
     * via {@code @InputFile} / {@code @Internal} annotations.
     */
    static class ForeignAccessArgumentProvider implements CommandLineArgumentProvider, Named {
        private final Provider<RegularFile> jarFile;
        private final Property<Integer> releaseProperty;
        private final int minRuntime;

        ForeignAccessArgumentProvider(Provider<RegularFile> jarFile, Property<Integer> releaseProperty, int minRuntime) {
            this.jarFile = jarFile;
            this.releaseProperty = releaseProperty;
            this.minRuntime = minRuntime;
        }

        @Override
        public Iterable<String> asArguments() {
            int release = releaseProperty.getOrElse(minRuntime);
            if (release == 21 && jarFile.isPresent()) {
                File jar = jarFile.get().getAsFile();
                if (jar.exists()) {
                    return List.of("--patch-module", "java.base=" + jar.getAbsolutePath());
                }
            }
            return Collections.emptyList();
        }

        @InputFile
        @PathSensitive(PathSensitivity.NONE)
        @org.gradle.api.tasks.Optional
        public Provider<RegularFile> getJarFile() {
            return jarFile;
        }

        @Internal
        @Override
        public String getName() {
            return "foreign-access-arg-provider";
        }
    }

    private static int minimumRuntimeVersion(Project project) {
        BuildParameterExtension params = project.getRootProject().getExtensions().getByType(BuildParameterExtension.class);
        return Integer.parseInt(params.getMinimumRuntimeVersion().getMajorVersion());
    }

    private static Provider<Integer> releaseVersionProviderFromCompileTask(Project project, AbstractCompile compileTask) {
        return project.provider(() -> {
            JavaVersion javaVersion = JavaVersion.toVersion(compileTask.getTargetCompatibility());
            return Integer.parseInt(javaVersion.getMajorVersion());
        });
    }

}
