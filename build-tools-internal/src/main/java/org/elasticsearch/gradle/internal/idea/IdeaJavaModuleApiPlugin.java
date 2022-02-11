/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.gradle.internal.idea;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.attributes.Attribute;
import org.gradle.api.attributes.Bundling;
import org.gradle.api.attributes.Category;
import org.gradle.api.attributes.LibraryElements;
import org.gradle.api.attributes.Usage;
import org.gradle.api.attributes.java.TargetJvmVersion;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.tasks.Copy;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetOutput;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.bundling.Jar;

import java.io.File;
import java.util.Set;

import javax.inject.Inject;

import static org.gradle.api.attributes.Category.LIBRARY;
import static org.gradle.api.attributes.LibraryElements.CLASSES;
import static org.gradle.api.attributes.LibraryElements.JAR;
import static org.gradle.api.attributes.LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE;

/**
 * This plugin provides an outgoing variant that only exposes public api classes that are calculated
 * from the module-info.java file.
 *
 * This variant is meant to replace apiElements variant for consuming. In addition to apiElements
 * this moduleApiElements variant exposes the attribute 'org.elasticsearch.java-module-api` of type
 * boolean value 'true'.
 *
 * See {@link IdeaJavaModuleApiConsumerPlugin} as an example how to request this variant.
 *
 * TODO: provide a plain classes variant in addition to jar variant
 * */
public class IdeaJavaModuleApiPlugin implements Plugin<Project> {

    public static final String JAVA_MODULE_API_APPENDIX = "java-module-api";
    private final ObjectFactory objectFactory;

    @Inject
    public IdeaJavaModuleApiPlugin(ObjectFactory objectFactory) {
        this.objectFactory = objectFactory;
    }

    @Override
    public void apply(Project project) {
        project.getPluginManager().withPlugin("java-base", p -> {
            // TODO handle all sourceSets
            project.getExtensions().getByType(JavaPluginExtension.class).getSourceSets().all(sourceSet -> {
                if (sourceSet.getName().equals("main") == false) {
                    return;
                }

                if (project.file("src/main/java/module-info.java").exists() == false) {
                    return;
                }
                Configuration moduleApiElements = createModuleApisVariant(project, sourceSet);

                SourceSetOutput sourceSetOutput = sourceSet.getOutput();
                String exportModuleInfoName = sourceSet.getTaskName("export", "ModuleInfo");
                TaskProvider<ReadModuleExports> exportModuleInfo = project.getTasks()
                    .register(exportModuleInfoName, ReadModuleExports.class);
                exportModuleInfo.configure(e -> e.setClassFiles(sourceSetOutput.getClassesDirs()));

                String modulesApiName = sourceSet.getTaskName("modulesApi", "Classes");
                TaskProvider<Copy> moduleApiClasses = project.getTasks().register(modulesApiName, Copy.class, t -> {
                    t.dependsOn(exportModuleInfo);
                    t.setDestinationDir(new File(project.getBuildDir(), "modules-api-classes"));
                    t.from(sourceSetOutput);
                    t.include(e -> {
                        Set<String> exports = exportModuleInfo.get().getExports();
                        String path = e.getRelativePath().getPathString();
                        if (path.endsWith(".class")) {
                            if (path.equals("module-info.class")) {
                                return true;
                            }
                            int lastSlash = path.lastIndexOf('/');
                            if (lastSlash == -1) {
                                lastSlash = 0;
                            }
                            String packagePath = path.substring(0, lastSlash);
                            return exports.contains(packagePath) != false;
                        }
                        return true;
                    });
                });
                moduleApiElements.getOutgoing().artifact(moduleApiClasses);
            });

        });
    }

    /**
     * creates a consumable configuration with similar attributes as gradles apiElements.
     *
     * In addition, we add an attribute 'org.elasticsearch.java-module-api` of type boolean
     * */
    private Configuration createModuleApisVariant(Project project, SourceSet sourceSet) {
        Attribute<Boolean> javaModuleAttribute = Attribute.of("org.elasticsearch.java-module-api", Boolean.class);
        return project.getConfigurations().create(sourceSet.getName() + "ModuleApiElements", moduleApiElements -> {
            moduleApiElements.extendsFrom(project.getConfigurations().getByName(sourceSet.getImplementationConfigurationName()));
            moduleApiElements.setCanBeConsumed(true);
            moduleApiElements.setCanBeResolved(false);
            moduleApiElements.getAttributes()
                .attribute(Category.CATEGORY_ATTRIBUTE, objectFactory.named(Category.class, LIBRARY))
                .attribute(LIBRARY_ELEMENTS_ATTRIBUTE, objectFactory.named(LibraryElements.class, CLASSES))
                .attribute(Bundling.BUNDLING_ATTRIBUTE, objectFactory.named(Bundling.class, Bundling.EXTERNAL))
                // TODO rework hard coded jvm version
                .attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 17)
                .attribute(Usage.USAGE_ATTRIBUTE, objectFactory.named(Usage.class, Usage.JAVA_API))
                .attribute(javaModuleAttribute, Boolean.TRUE);
        });

    }
}
