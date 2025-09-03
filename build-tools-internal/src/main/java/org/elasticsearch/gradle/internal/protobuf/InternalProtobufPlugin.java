/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the "Elastic License
 * 2.0", the "GNU Affero General Public License v3.0 only", and the "Server Side
 * Public License v 1"; you may not use this file except in compliance with, at
 * your election, the "Elastic License 2.0", the "GNU Affero General Public
 * License v3.0 only", or the "Server Side Public License, v 1".
 */

package org.elasticsearch.gradle.internal.protobuf;

import groovy.lang.Closure;

import com.google.protobuf.gradle.Utils;
import com.google.protobuf.gradle.internal.DefaultProtoSourceSet;
import com.google.protobuf.gradle.tasks.ProtoSourceSet;
import com.google.protobuf.gradle.ProtobufExtract;
import com.google.protobuf.gradle.ProtobufExtension;
import com.google.protobuf.gradle.GenerateProtoTask;

import org.gradle.api.Action;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.attributes.LibraryElements;
import org.gradle.api.attributes.Usage;
import org.gradle.api.file.CopySpec;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.FileSystemOperations;
import org.gradle.api.file.SourceDirectorySet;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.SourceSet;
import org.gradle.language.jvm.tasks.ProcessResources;

import javax.inject.Inject;

import java.io.File;
import java.util.Collection;
import java.util.List;

public abstract class InternalProtobufPlugin implements Plugin<Project> {

    private Project project;

    private NamedDomainObjectContainer<ProtoSourceSet> protoSourceSets;
    private InternalProtobufExtension extension;

    @Inject
    protected abstract FileSystemOperations getFileSystemOperations();


    @Override
    public void apply(Project project) {
        this.project = project;
        JavaPluginExtension javaPluginExtension = project.getExtensions().getByType(JavaPluginExtension.class);

        extension = project.getExtensions().create("protobuf", InternalProtobufExtension.class, project);
//        project.getExtensions().create("protobuf2", ProtobufExtension.class, project);

        protoSourceSets =
            project.getObjects().domainObjectContainer(ProtoSourceSet.class, name -> new DefaultProtoSourceSet(name, project.getObjects()));

        Collection<Closure> postConfigure = new java.util.ArrayList<>();
        Provider<Task> dummyTask = project.getTasks().register("protobufDummy");
        javaPluginExtension.getSourceSets().configureEach(sourceSet -> {
            ProtoSourceSet protoSourceSet = protoSourceSets.create(sourceSet.getName());
            addSourceSetExtension(sourceSet, protoSourceSet);
            Configuration protobufConfig = createProtobufConfiguration(protoSourceSet);
            Configuration compileProtoPath = createCompileProtoPathConfiguration(protoSourceSet);
            addTasksForSourceSet(sourceSet, protoSourceSet, protobufConfig, compileProtoPath, postConfigure, dummyTask);
        });
    }

    /**
     * Creates an internal 'compileProtoPath' configuration for the given source set that extends
     * compilation configurations as a bucket of dependencies with resources attribute.
     * The extract-include-protos task of each source set will extract protobuf files from
     * resolved dependencies in this configuration.
     *
     * <p> For Java projects only.
     * <p> This works around 'java-library' plugin not exposing resources to consumers for compilation.
     */
    private Configuration createCompileProtoPathConfiguration(ProtoSourceSet protoSourceSet) {
        String compileProtoConfigName = Utils.getConfigName(protoSourceSet.getName(), "compileProtoPath");
        Configuration compileConfig =
            project.getConfigurations().getByName(Utils.getConfigName(protoSourceSet.getName(), "compileOnly"));
        Configuration implementationConfig =
            project.getConfigurations().getByName(Utils.getConfigName(protoSourceSet.getName(), "implementation"));
        return project.getConfigurations().create(compileProtoConfigName, it -> {
            it.setTransitive(true);
            it.setExtendsFrom(List.of(compileConfig, implementationConfig));
            it.setCanBeConsumed(false);
            it.getAttributes()
                // Variant attributes are not inherited. Setting it too loosely can
                // result in ambiguous variant selection errors.
                // CompileProtoPath only need proto files from dependency's resources.
                // LibraryElement "resources" is compatible with "jar" (if a "resources" variant is
                // not found, the "jar" variant will be used).
                .attribute(
                    LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE,
                    project.getObjects().named(LibraryElements.class, LibraryElements.RESOURCES))
                // Although variants with any usage has proto files, not setting usage attribute
                // can result in ambiguous variant selection if the producer provides multiple
                // variants with different usage attribute.
                // Preserve the usage attribute from CompileOnly and Implementation.
                .attribute(
                    Usage.USAGE_ATTRIBUTE,
                    project.getObjects().named(Usage.class, Usage.JAVA_RUNTIME));
        });
    }

    /**
     * Creates a 'protobuf' configuration for the given source set. The build author can
     * configure dependencies for it. The extract-protos task of each source set will
     * extract protobuf files from dependencies in this configuration.
     */
    private Configuration createProtobufConfiguration(ProtoSourceSet protoSourceSet) {
        String protobufConfigName = Utils.getConfigName(protoSourceSet.getName(), "protobuf");
        return project.getConfigurations().create(protobufConfigName, new Action<Configuration>() {
            @Override
            public void execute(Configuration it) {
                it.setVisible(false);
                it.setTransitive(true);
            }
        });
    }

    /**
     * Adds the proto extension to the SourceSet, e.g., it creates
     * sourceSets.main.proto and sourceSets.test.proto.
     */
    private SourceDirectorySet addSourceSetExtension(SourceSet sourceSet, ProtoSourceSet protoSourceSet) {
        String name = sourceSet.getName();
        SourceDirectorySet sds = protoSourceSet.getProto();
        sourceSet.getExtensions().add("proto", sds);
        sds.srcDir("src/" + name + "/proto");
        sds.include("**/*.proto");
        return sds;
    }

    /**
     * Creates Protobuf tasks for a sourceSet in a Java project.
     */
    private void addTasksForSourceSet(
        SourceSet sourceSet, ProtoSourceSet protoSourceSet, Configuration protobufConfig,
        Configuration compileProtoPath, Collection<Closure> postConfigure, Provider<Task> dummyTask) {
        Provider<ProtobufExtract> extractProtosTask = setupExtractProtosTask(protoSourceSet, protobufConfig, dummyTask);

        Provider<ProtobufExtract> extractIncludeProtosTask = setupExtractIncludeProtosTask(
            protoSourceSet, compileProtoPath, dummyTask);

//        // Make protos in 'test' sourceSet able to import protos from the 'main' sourceSet.
//        // Pass include proto files from main to test.
//        if (Utils.isTest(sourceSet.getName())) {
//            protoSourceSet.includesFrom(sourceSets.getByName("main"))
//        }

        Provider<GenerateProtoTask> generateProtoTask = addGenerateProtoTask(protoSourceSet, it -> {
            it.setSourceSet(sourceSet);
            it.doneInitializing();
            it.getBuiltins().maybeCreate("java");
        });

        sourceSet.getJava().srcDirs(protoSourceSet.getOutput());

        // Include source proto files in the compiled archive, so that proto files from
        // dependent projects can import them.
        project.getTasks().named(sourceSet.getTaskName("process", "resources"), ProcessResources.class).configure(it ->
            it.from(protoSourceSet.getProto(), (Action<CopySpec>) cs -> cs.include("**/*.proto")));
    }
//        postConfigure.add {
//            project.plugins.withId("eclipse") {
//                // This is required because the intellij/eclipse plugin does not allow adding source directories
//                // that do not exist. The intellij/eclipse config files should be valid from the start.
//                generateProtoTask.get().getOutputSourceDirectories().each { File outputDir ->
//                    outputDir.mkdirs()
//                }
//            }
//
//            project.plugins.withId("idea") {
//                boolean isTest = Utils.isTest(sourceSet.name)
//                protoSourceSet.proto.srcDirs.each { File protoDir ->
//                    Utils.addToIdeSources(project, isTest, protoDir, false)
//                }
//                Utils.addToIdeSources(project, isTest, project.files(extractProtosTask).singleFile, true)
//                Utils.addToIdeSources(project, isTest, project.files(extractIncludeProtosTask).singleFile, true)
//                generateProtoTask.get().getOutputSourceDirectories().each { File outputDir ->
//                    Utils.addToIdeSources(project, isTest, outputDir, true)
//                }
//            }
//        }


    /**
     * Adds a task to run protoc and compile all proto source files for a sourceSet or variant.
     *
     * @param sourceSetOrVariantName the name of the sourceSet (Java) or
     * variant (Android) that this task will run for.
     *
     * @param sourceSets the sourceSets that contains the proto files to be
     * compiled. For Java it's the sourceSet that sourceSetOrVariantName stands
     * for; for Android it's the collection of sourceSets that the variant includes.
     */
    private Provider<GenerateProtoTask> addGenerateProtoTask(
        ProtoSourceSet protoSourceSet,
        Action<GenerateProtoTask> configureAction
    ) {
        String sourceSetName = protoSourceSet.getName();
        String taskName = "generate" + Utils.getSourceSetSubstringForTaskNames(sourceSetName) + "Proto";
        Provider<String> defaultGeneratedFilesBaseDir = extension.getDefaultGeneratedFilesBaseDir();
        Provider<String> generatedFilesBaseDirProvider = extension.getDefaultGeneratedFilesBaseDir();
        Provider<GenerateProtoTask> task = project.getTasks().register(taskName, GenerateProtoTask.class, it ->{

//            CopyActionFacade copyActionFacade = CopyActionFacade.Loader.create(project, project.getObjects());
            it.setDescription("Compiles Proto source for '" + sourceSetName + "'");
            it.setOutputBaseDir(defaultGeneratedFilesBaseDir.map( dir -> dir + "/" + sourceSetName));
            it.addSourceDirs(protoSourceSet.getProto());
            it.addIncludeDir(protoSourceSet.getProto().getSourceDirectories());
            it.addIncludeDir(protoSourceSet.getIncludeProtoDirs());
            it.doLast( tsk -> {
                String generatedFilesBaseDir = generatedFilesBaseDirProvider.get();
                if (generatedFilesBaseDir == defaultGeneratedFilesBaseDir.get()) {
                    return;
                }
                // Purposefully don't wire this up to outputs, as it can be mixed with other files.
                getFileSystemOperations().copy( spec -> {
                    spec.setIncludeEmptyDirs(false);
                    spec.from(it.getOutputBaseDir());
                    spec.into(generatedFilesBaseDir + "/" + sourceSetName);
                });
            });
            configureAction.execute(it);
        });
        protoSourceSet.getOutput().from(task.map(it -> it.getOutputBaseDir()));
        return task;
    }

    /**
     * Sets up a task to extract protos from compile dependencies of a sourceSet, Those are needed
     * for imports in proto files, but they won't be compiled since they have already been compiled
     * in their own projects or artifacts.
     *
     * <p>This task is per-sourceSet for both Java and per variant for Android.
     */
    private Provider<ProtobufExtract> setupExtractIncludeProtosTask(
        ProtoSourceSet protoSourceSet,
        FileCollection archives,
        Provider<Task> dummyTask
    ) {
        String taskName = "extractInclude" + Utils.getSourceSetSubstringForTaskNames(protoSourceSet.getName()) + "Proto";
        Provider<ProtobufExtract> task = project.getTasks().register(taskName, ProtobufExtract.class,it -> {
            it.setDescription("Extracts proto files from compile dependencies for includes");
            it.getDestDir().set(getExtractedIncludeProtosDir(protoSourceSet.getName()));
            it.getInputFiles().from(archives);
            it.getDummyTaskDependency().from(dummyTask);
        });
        protoSourceSet.getIncludeProtoDirs().from(task);
        return task;
    }

    private File getExtractedIncludeProtosDir(String sourceSetName) {
        return project.getLayout().getBuildDirectory().dir("extracted-include-protos/" + sourceSetName).get().getAsFile();
    }

    /**
     * Sets up a task to extract protos from protobuf dependencies. They are
     * treated as sources and will be compiled.
     *
     * <p>This task is per-sourceSet, for both Java and Android. In Android a
     * variant may have multiple sourceSets, each of these sourceSets will have
     * its own extraction task.
     */
    private Provider<ProtobufExtract> setupExtractProtosTask(
        ProtoSourceSet protoSourceSet,
        Configuration protobufConfig,
        Provider<Task> dummyTask
    ) {
        String sourceSetName = protoSourceSet.getName();
        String taskName = getExtractProtosTaskName(sourceSetName);
        Provider<ProtobufExtract> task = project.getTasks().register(taskName, ProtobufExtract.class, it -> {
            it.setDescription("Extracts proto files/dependencies specified by 'protobuf' configuration");
            it.getDestDir().set(getExtractedProtosDir(sourceSetName));
            it.getInputFiles().from(protobufConfig);
            it.getDummyTaskDependency().from(dummyTask);
        });
        protoSourceSet.getProto().srcDir(task);
        return task;
    }


    private String getExtractProtosTaskName(String sourceSetName) {
        return "extract" + Utils.getSourceSetSubstringForTaskNames(sourceSetName) + "Proto";
    }

    private File getExtractedProtosDir(String sourceSetName) {
        return project.getLayout().getBuildDirectory().dir("extracted-protos/" + sourceSetName).get().getAsFile();
    }
}
