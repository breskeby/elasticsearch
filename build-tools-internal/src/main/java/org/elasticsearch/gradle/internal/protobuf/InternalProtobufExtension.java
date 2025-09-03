/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the "Elastic License
 * 2.0", the "GNU Affero General Public License v3.0 only", and the "Server Side
 * Public License v 1"; you may not use this file except in compliance with, at
 * your election, the "Elastic License 2.0", the "GNU Affero General Public
 * License v3.0 only", or the "Server Side Public License, v 1".
 */

package org.elasticsearch.gradle.internal.protobuf;

import com.google.protobuf.gradle.ExecutableLocator;
import com.google.protobuf.gradle.ToolsLocator;
import com.google.protobuf.gradle.ProtobufExtension;

import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;

import javax.inject.Inject;

public abstract class InternalProtobufExtension extends ProtobufExtension {

    private final ToolsLocator tools;

    private final Provider<String> defaultGeneratedFilesBaseDir;

    private final Provider<String> defaultJavaExecutablePath;
//
//    public Property<String> getGeneratedFilesBaseDirProperty() {
//return null;
//    }

    @Inject
    public InternalProtobufExtension(Project project){
        super(project);
        this.tools = new InternalToolsLocator(project);
        this.defaultGeneratedFilesBaseDir = project.getLayout().getBuildDirectory().dir("generated/sources/proto").map(directory -> directory.getAsFile().getPath());
//        this.getGeneratedFilesBaseDirProperty2().convention(defaultGeneratedFilesBaseDir);
        this.defaultJavaExecutablePath = project.getProviders().provider(() ->  ProtobufExtension.computeJavaExePath());

    }

    public Provider<String> getDefaultGeneratedFilesBaseDir() {
        return defaultGeneratedFilesBaseDir;
    }

    public Provider<String> getDefaultJavaExecutablePath() {
        return defaultJavaExecutablePath;
    }

//    /**
//     * Locates the protoc executable. The closure will be manipulating an
//     * ExecutableLocator.
//     */
//    public void protoc(Action<ExecutableLocator> configureAction) {
//        configureAction.execute(tools.getProtoc());
//    }
}
