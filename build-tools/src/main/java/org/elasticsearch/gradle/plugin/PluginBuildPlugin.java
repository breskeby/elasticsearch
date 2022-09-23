/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.gradle.plugin;

import org.elasticsearch.gradle.Version;
import org.elasticsearch.gradle.VersionProperties;
import org.elasticsearch.gradle.dependencies.CompileOnlyResolvePlugin;
import org.elasticsearch.gradle.jarhell.JarHellPlugin;
import org.elasticsearch.gradle.test.GradleTestPolicySetupPlugin;
import org.elasticsearch.gradle.testclusters.ElasticsearchCluster;
import org.elasticsearch.gradle.testclusters.RunTask;
import org.elasticsearch.gradle.testclusters.TestClustersPlugin;
import org.elasticsearch.gradle.util.GradleUtils;
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
public class PluginBuildPlugin implements Plugin<Project> {

    private final ProviderFactory providerFactory;

    @Inject
    public PluginBuildPlugin(ProviderFactory providerFactory) {
        this.providerFactory = providerFactory;
    }

    @Override
    public void apply(final Project project) {
        project.getPluginManager().apply(BasePluginPlugin.class);


        project.getTasks().withType(GeneratePluginPropertiesTask.class).named("pluginProperties").configure(generatePluginPropertiesTask -> {
            generatePluginPropertiesTask.getClassname().set();
        });





        var extension = project.getExtensions().create(PLUGIN_EXTENSION_NAME, PluginPropertiesExtension.class, project);
        configureDependencies(project);

        final var bundleTask = createBundleTasks(project, extension);
        project.getConfigurations().getByName("default").extendsFrom(project.getConfigurations().getByName("runtimeClasspath"));

        // allow running ES with this plugin in the foreground of a build
        var testClusters = testClusters(project, TestClustersPlugin.EXTENSION_NAME);
        var runCluster = testClusters.register("runTask", c -> {
            // TODO: use explodedPlugin here for modules
            if (GradleUtils.isModuleProject(project.getPath())) {
                c.module(bundleTask.flatMap((Transformer<Provider<RegularFile>, Zip>) zip -> zip.getArchiveFile()));
            } else {
                c.plugin(bundleTask.flatMap((Transformer<Provider<RegularFile>, Zip>) zip -> zip.getArchiveFile()));
            }
        });

        project.getTasks().register("run", RunTask.class, r -> {
            r.useCluster(runCluster);
            r.dependsOn(project.getTasks().named(BUNDLE_PLUGIN_TASK_NAME));
        });
    }

    @SuppressWarnings("unchecked")
    private static NamedDomainObjectContainer<ElasticsearchCluster> testClusters(Project project, String extensionName) {
        return (NamedDomainObjectContainer<ElasticsearchCluster>) project.getExtensions().getByName(extensionName);
    }

    private static void configureDependencies(final Project project) {
        var dependencies = project.getDependencies();
        dependencies.add("compileOnly", "org.elasticsearch:elasticsearch:" + VersionProperties.getElasticsearch());
        dependencies.add("testImplementation", "org.elasticsearch.test:framework:" + VersionProperties.getElasticsearch());
    }

}
