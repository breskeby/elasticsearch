/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.gradle.stableplugin;

import org.elasticsearch.gradle.Version;
import org.elasticsearch.gradle.VersionProperties;
import org.elasticsearch.gradle.dependencies.CompileOnlyResolvePlugin;
import org.elasticsearch.gradle.jarhell.JarHellPlugin;
import org.elasticsearch.gradle.plugin.BasePluginPlugin;
import org.elasticsearch.gradle.plugin.GeneratePluginPropertiesTask;
import org.elasticsearch.gradle.plugin.PluginPropertiesExtension;
import org.elasticsearch.gradle.test.GradleTestPolicySetupPlugin;
import org.elasticsearch.gradle.testclusters.ElasticsearchCluster;
import org.elasticsearch.gradle.testclusters.RunTask;
import org.elasticsearch.gradle.testclusters.TestClustersPlugin;
import org.elasticsearch.gradle.util.GradleUtils;
import org.gradle.api.Action;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.Transformer;
import org.gradle.api.artifacts.type.ArtifactTypeDefinition;
import org.gradle.api.file.CopySpec;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.RegularFile;
import org.gradle.api.plugins.BasePlugin;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.Sync;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.bundling.Zip;

import java.io.File;
import java.util.Map;
import java.util.concurrent.Callable;
import javax.inject.Inject;

/**
 * Encapsulates build configuration for an Elasticsearch plugin.
 */
public class StablePluginBuildPlugin implements Plugin<Project> {

    public static final String PLUGIN_EXTENSION_NAME = "stableesplugin";
    public static final String BUNDLE_PLUGIN_TASK_NAME = "stablebundlePlugin";
    public static final String EXPLODED_BUNDLE_PLUGIN_TASK_NAME = "stableexplodedBundlePlugin";
    public static final String EXPLODED_BUNDLE_CONFIG = "stableexplodedBundleZip";

    private final ProviderFactory providerFactory;

    @Inject
    public StablePluginBuildPlugin(ProviderFactory providerFactory) {
        this.providerFactory = providerFactory;
    }

    @Override
    public void apply(final Project project) {
        project.getPluginManager().apply(BasePluginPlugin.class);
        PluginPropertiesExtension pluginExtension = project.getExtensions().getByType(PluginPropertiesExtension.class);

        final var pluginNamedComponents = project.getTasks().register("pluginNamedComponents", StableGenerateNamedComponentsTask.class, t -> {
            SourceSet mainSourceSet = GradleUtils.getJavaSourceSets(project).findByName(SourceSet.MAIN_SOURCE_SET_NAME);
            t.setPluginClasses(mainSourceSet.getOutput().getClassesDirs());
            t.setClasspath(project.getConfigurations().getByName(JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME));
        });

        pluginExtension.getBundleSpec().from(pluginNamedComponents);

        project.getTasks().withType(GeneratePluginPropertiesTask.class).named("pluginProperties").configure(task -> {
            task.
        });

//
//        var extension = project.getExtensions().create(PLUGIN_EXTENSION_NAME, StablePluginPropertiesExtension.class, project);
//        configureDependencies(project);
//
//        final var bundleTask = createBundleTasks(project, extension);
//        project.getConfigurations().getByName("default").extendsFrom(project.getConfigurations().getByName("runtimeClasspath"));
//
//        // allow running ES with this plugin in the foreground of a build
//        var testClusters = testClusters(project, TestClustersPlugin.EXTENSION_NAME);
//        var runCluster = testClusters.register("stablerunTask", c -> {
//            // TODO: use explodedPlugin here for modules
//            if (GradleUtils.isModuleProject(project.getPath())) {
//                c.module(bundleTask.flatMap((Transformer<Provider<RegularFile>, Zip>) zip -> zip.getArchiveFile()));
//            } else {
//                c.plugin(bundleTask.flatMap((Transformer<Provider<RegularFile>, Zip>) zip -> zip.getArchiveFile()));
//            }
//        });
//
//        project.getTasks().register("stablerun", RunTask.class, r -> {
//            r.useCluster(runCluster);
//            r.dependsOn(project.getTasks().named(BUNDLE_PLUGIN_TASK_NAME));
//        });
    }

    @SuppressWarnings("unchecked")
    private static NamedDomainObjectContainer<ElasticsearchCluster> testClusters(Project project, String extensionName) {
        return (NamedDomainObjectContainer<ElasticsearchCluster>) project.getExtensions().getByName(extensionName);
    }

    private static void configureDependencies(final Project project) {
        var dependencies = project.getDependencies();
//        dependencies.add("compileOnly", "org.elasticsearch:elasticsearch:" + VersionProperties.getElasticsearch());
        dependencies.add("testImplementation", "org.elasticsearch.test:framework:" + VersionProperties.getElasticsearch());
    }

    /**
     * Adds bundle tasks which builds the dir and zip containing the plugin jars,
     * metadata, properties, and packaging files
     */
    private TaskProvider<Zip> createBundleTasks(final Project project, StablePluginPropertiesExtension extension) {
        final var pluginMetadata = project.file("src/main/plugin-metadata");

        final var buildProperties = project.getTasks().register("stablepluginProperties", StableGeneratePluginPropertiesTask.class, task -> {
            task.getPluginName().set(providerFactory.provider(extension::getName));
            task.getPluginDescription().set(providerFactory.provider(extension::getDescription));
            task.getModular().set(providerFactory.provider(extension::isModular));
            task.getPluginVersion().set(providerFactory.provider(extension::getVersion));
            task.getElasticsearchVersion().set(Version.fromString(VersionProperties.getElasticsearch()).toString());
            var javaExtension = project.getExtensions().getByType(JavaPluginExtension.class);
            task.getJavaVersion().set(providerFactory.provider(() -> javaExtension.getTargetCompatibility().toString()));
            task.getClassname().set(providerFactory.provider(extension::getClassname));
            task.getExtendedPlugins().set(providerFactory.provider(extension::getExtendedPlugins));
            task.getHasNativeController().set(providerFactory.provider(extension::isHasNativeController));
            task.getRequiresKeystore().set(providerFactory.provider(extension::isRequiresKeystore));
            task.getIsLicensed().set(providerFactory.provider(extension::isLicensed));

            var mainSourceSet = project.getExtensions().getByType(SourceSetContainer.class).getByName(SourceSet.MAIN_SOURCE_SET_NAME);
            FileCollection moduleInfoFile = mainSourceSet.getOutput().getAsFileTree().matching(p -> p.include("module-info.class"));
            task.getModuleInfoFile().setFrom(moduleInfoFile);
        });

        final var pluginNamedComponents = project.getTasks().register("pluginNamedComponents", StableGenerateNamedComponentsTask.class, t -> {
                SourceSet mainSourceSet = GradleUtils.getJavaSourceSets(project).findByName(SourceSet.MAIN_SOURCE_SET_NAME);
                t.setPluginClasses(mainSourceSet.getOutput().getClassesDirs());
                t.setClasspath(project.getConfigurations().getByName(JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME));
        });

        // add the plugin properties and metadata to test resources, so unit tests can
        // know about the plugin (used by test security code to statically initialize the plugin in unit tests)
        var testSourceSet = project.getExtensions().getByType(SourceSetContainer.class).getByName("test");
        Map<String, Object> map = Map.of("builtBy", buildProperties);
        testSourceSet.getOutput().dir(map, new File(project.getBuildDir(), "generated-resources"));
        testSourceSet.getResources().srcDir(pluginMetadata);

        var bundleSpec = createBundleSpec(project, pluginMetadata, buildProperties, pluginNamedComponents);
        extension.setBundleSpec(bundleSpec);
        // create the actual bundle task, which zips up all the files for the plugin
        final var bundle = project.getTasks().register("stablebundlePlugin", Zip.class, zip -> zip.with(bundleSpec));
        project.getTasks().named(BasePlugin.ASSEMBLE_TASK_NAME).configure(task -> task.dependsOn(bundle));

        // also make the zip available as a configuration (used when depending on this project)
        var configuration = project.getConfigurations().create("stablezip");
        configuration.getAttributes().attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, ArtifactTypeDefinition.ZIP_TYPE);
        project.getArtifacts().add("stablezip", bundle);

        var explodedBundle = project.getTasks().register(EXPLODED_BUNDLE_PLUGIN_TASK_NAME, Sync.class, sync -> {
            sync.with(bundleSpec);
            sync.into(new File(project.getBuildDir(), "explodedBundle/" + extension.getName()));
        });

        // also make the exploded bundle available as a configuration (used when depending on this project)
        var explodedBundleZip = project.getConfigurations().create(EXPLODED_BUNDLE_CONFIG);
        explodedBundleZip.setCanBeResolved(false);
        explodedBundleZip.setCanBeConsumed(true);
        explodedBundleZip.getAttributes().attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, ArtifactTypeDefinition.DIRECTORY_TYPE);
        project.getArtifacts().add(EXPLODED_BUNDLE_CONFIG, explodedBundle);
        return bundle;
    }

    private static CopySpec createBundleSpec(
        Project project,
        File pluginMetadata,
        TaskProvider<StableGeneratePluginPropertiesTask> buildProperties,
        TaskProvider<StableGenerateNamedComponentsTask> pluginNamedComponents) {
        var bundleSpec = project.copySpec();
        bundleSpec.from(buildProperties);
        bundleSpec.from(pluginNamedComponents);
        bundleSpec.from(pluginMetadata, copySpec -> {
            // metadata (eg custom security policy)
            // the codebases properties file is only for tests and not needed in production
            copySpec.exclude("plugin-security.codebases");
        });
        bundleSpec.from(
            (Callable<TaskProvider<Task>>) () -> project.getPluginManager().hasPlugin("com.github.johnrengelman.shadow")
                ? project.getTasks().named("shadowJar")
                : project.getTasks().named("jar")
        );
        bundleSpec.from(
            project.getConfigurations()
                .getByName("runtimeClasspath")
                .minus(project.getConfigurations().getByName(CompileOnlyResolvePlugin.RESOLVEABLE_COMPILE_ONLY_CONFIGURATION_NAME))
        );

        // extra files for the plugin to go into the zip
        bundleSpec.from("src/main/packaging");// TODO: move all config/bin/_size/etc into packaging
        bundleSpec.from("src/main", copySpec -> {
            copySpec.include("config/**");
            copySpec.include("bin/**");
        });
        return bundleSpec;
    }
}
