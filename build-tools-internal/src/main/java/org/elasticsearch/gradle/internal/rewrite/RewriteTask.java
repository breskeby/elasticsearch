/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.gradle.internal.rewrite;

import org.gradle.api.Action;
import org.gradle.api.DefaultTask;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskAction;
import org.gradle.workers.ProcessWorkerSpec;
import org.gradle.workers.WorkQueue;
import org.gradle.workers.WorkerExecutor;

import javax.inject.Inject;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import static java.util.stream.Collectors.toList;

public class RewriteTask extends DefaultTask {

    private final Configuration configuration;
    private final Collection<SourceSet> sourceSets;
    private WorkerExecutor workerExecutor;
    protected final RewriteExtension extension;
    @Inject
    public RewriteTask(
        Configuration configuration,
        Collection<SourceSet> sourceSets,
        RewriteExtension extension,
        WorkerExecutor workerExecutor
    ) {
        this.configuration = configuration;
        this.extension = extension;
        this.sourceSets = sourceSets;
        this.workerExecutor = workerExecutor;
        this.setGroup("rewrite");
        this.setDescription("Apply the active refactoring recipes");
    }

    @TaskAction
    public void run() {
        WorkQueue workQueue = workerExecutor.processIsolation(new Action<ProcessWorkerSpec>() {
            @Override
            public void execute(ProcessWorkerSpec processWorkerSpec) {
                processWorkerSpec.getClasspath().from(configuration);
                processWorkerSpec.getForkOptions().jvmArgs("--add-exports");
                processWorkerSpec.getForkOptions().jvmArgs("jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED");

                processWorkerSpec.getForkOptions().jvmArgs("--add-exports");
                processWorkerSpec.getForkOptions().jvmArgs("jdk.compiler/com.sun.tools.javac.main=ALL-UNNAMED");

                processWorkerSpec.getForkOptions().jvmArgs("--add-exports");
                processWorkerSpec.getForkOptions().jvmArgs("jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED");

                processWorkerSpec.getForkOptions().jvmArgs("--add-exports");
                processWorkerSpec.getForkOptions().jvmArgs("jdk.compiler/com.sun.tools.javac.comp=ALL-UNNAMED");

                processWorkerSpec.getForkOptions().jvmArgs("--add-exports");
                processWorkerSpec.getForkOptions().jvmArgs("jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED");

                processWorkerSpec.getForkOptions().jvmArgs("--add-exports");
                processWorkerSpec.getForkOptions().jvmArgs("jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED");

                processWorkerSpec.getForkOptions().jvmArgs("--add-exports");
                processWorkerSpec.getForkOptions().jvmArgs("jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED");

                processWorkerSpec.getForkOptions().jvmArgs("--add-exports");
                processWorkerSpec.getForkOptions().jvmArgs("jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED");
                processWorkerSpec.getForkOptions().workingDir(getProject().getProjectDir());
                processWorkerSpec.getForkOptions().setMaxHeapSize("4g");

            }
        });

        if (sourceSets.size() > 0) {
            List<File> javaPaths = sourceSets.iterator()
                .next()
                .getAllJava()
                .getFiles()
                .stream()
                .filter(it -> it.isFile() && it.getName().endsWith(".java"))
                .collect(toList());
            List<File> dependencyPaths = sourceSets.iterator()
                .next()
                .getCompileClasspath()
                .getFiles()
                .stream()
                .collect(toList());

            workQueue.submit(RewriteWorker.class, parameters -> {
                parameters.getAllJavaPaths().addAll(javaPaths);
                parameters.getAllDependencyPaths().addAll(dependencyPaths);
                parameters.getActiveRecipes().addAll(extension.getActiveRecipes());
                parameters.getProjectDirectory().fileProvider(getProject().getProviders().provider(() -> getProject().getProjectDir()));
            });
            //
        }
    }


    @Input
    public SortedSet<String> getActiveRecipes() {
        String activeRecipeProp = System.getProperty("activeRecipe");
        if (activeRecipeProp == null) {
            return new TreeSet<>(extension.getActiveRecipes());
        } else {
            return new TreeSet<>(Collections.singleton(activeRecipeProp));
        }
    }

    @Input
    public SortedSet<String> getActiveStyles() {
        return new TreeSet<>(extension.getActiveStyles());
    }

}