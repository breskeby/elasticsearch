/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.gradle.internal.rewrite;

import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.logging.Logging;
import org.gradle.api.plugins.JavaBasePlugin;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.tasks.SourceSet;

import java.util.HashSet;
import java.util.Set;

/**
 * Adds the RewriteExtension to the current project and registers tasks per-sourceSet.
 * Only needs to be applied to projects with java sources. No point in applying this to any project that does
 * not have java sources of its own, such as the root project in a multi-project builds.
 */
public class RewritePlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        RewriteExtension maybeExtension = project.getExtensions().findByType(RewriteExtension.class);
        if (maybeExtension == null) {
            maybeExtension = project.getExtensions().create("rewrite", RewriteExtension.class, project);
        }
        final RewriteExtension extension = maybeExtension;

        // Rewrite module dependencies put here will be available to all rewrite tasks
        Configuration rewriteConf = project.getConfigurations().maybeCreate("rewrite");
        Set<SourceSet> sourceSets = new HashSet<>();
        Task rewriteRun = project.getTasks().create("rewriteRun", RewriteTask.class, rewriteConf, sourceSets, extension);
        // DomainObjectCollection.all() accepts a function to be applied to both existing and subsequently added members of the collection
        // Do not replace all() with any form of collection iteration which does not share this important property
        project.getPlugins().withType(JavaBasePlugin.class, javaBasePlugin -> {
            JavaPluginExtension javaPluginExtension = project.getExtensions().getByType(JavaPluginExtension.class);
            javaPluginExtension.getSourceSets().all( sourceSet -> {
                if(sourceSet.getName().equals("main")) {
                    sourceSets.add(sourceSet);

                    // This is intended to ensure that any Groovy/Kotlin/etc. sources are available for type attribution during parsing
                    // This may not be necessary if sourceSet.getCompileClasspath() guarantees that such sources will have been compiled
                    Task compileTask = project.getTasks().getByPath(sourceSet.getCompileJavaTaskName());
                    rewriteRun.dependsOn(compileTask);
                }
            });
        });
    }
}
