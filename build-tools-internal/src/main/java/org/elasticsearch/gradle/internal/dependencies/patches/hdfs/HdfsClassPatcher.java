/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the "Elastic License
 * 2.0", the "GNU Affero General Public License v3.0 only", and the "Server Side
 * Public License v 1"; you may not use this file except in compliance with, at
 * your election, the "Elastic License 2.0", the "GNU Affero General Public
 * License v3.0 only", or the "Server Side Public License, v 1".
 */

package org.elasticsearch.gradle.internal.dependencies.patches.hdfs;

import org.gradle.api.artifacts.transform.*;
import org.gradle.api.file.FileSystemLocation;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;

import java.io.*;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

@CacheableTransform
public abstract class HdfsClassPatcher implements TransformAction<HdfsClassPatcher.Parameters> {
    static final Map<String, Function<ClassWriter, ClassVisitor>> patchers = Map.of(
        "org/apache/hadoop/util/ShutdownHookManager.class",
        ShutdownHookManagerPatcher::new,
        "org/apache/hadoop/util/Shell.class",
        ShellPatcher::new,
        "org/apache/hadoop/security/UserGroupInformation.class",
        SubjectGetSubjectPatcher::new,
        "org/apache/hadoop/security/authentication/client/KerberosAuthenticator.class",
        SubjectGetSubjectPatcher::new
    );

    interface Parameters extends TransformParameters {
        @Input
        @Optional
        List<String> getMatchingArtifacts();
        void setMatchingArtifacts(List<String> matchingArtifacts);
    }

    @Classpath
    @InputArtifact
    public abstract Provider<FileSystemLocation> getInputArtifact();

    @Override
    public void transform(TransformOutputs outputs) {
        File inputFile = getInputArtifact().get().getAsFile();
        List<String> matchingArtifacts = getParameters().getMatchingArtifacts();
        if (matchingArtifacts.isEmpty() == false && matchingArtifacts.stream().noneMatch(supported -> inputFile.getName().contains(supported))) {
            outputs.file(getInputArtifact());
        } else {
            File outputFile = outputs.file(inputFile.getName().replace(".jar", "-patched.jar"));
            try (JarFile jarFile = new JarFile(inputFile); JarOutputStream jos = new JarOutputStream(new FileOutputStream(outputFile))) {
                Enumeration<JarEntry> entries = jarFile.entries();
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    String entryName = entry.getName();
                    // Add the entry to the new JAR file
                    jos.putNextEntry(new JarEntry(entryName));
                    // System.out.println("EntryName = " + entryName);
                    if (patchers.containsKey(entryName)) {
                        Function<ClassWriter, ClassVisitor> patcher = patchers.get(entryName);
                        System.out.println("[HdfsClassPatcher] Patching " + entryName + " in " + inputFile.getName());
                        byte[] classToPatch = jarFile.getInputStream(entry).readAllBytes();
                        ClassReader classReader = new ClassReader(classToPatch);
                        ClassWriter classWriter = new ClassWriter(classReader, 0);
                        classReader.accept(patcher.apply(classWriter), 0);
                        jos.write(classWriter.toByteArray());
                    } else {
                        // Read the entry's data and write it to the new JAR
                        try (InputStream is = jarFile.getInputStream(entry)) {
                            byte[] buffer = new byte[1024];
                            int bytesRead;
                            while ((bytesRead = is.read(buffer)) != -1) {
                                jos.write(buffer, 0, bytesRead);
                            }
                        }
                    }
                    jos.closeEntry();
                }
            } catch (FileNotFoundException ex) {
                throw new RuntimeException(ex);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
    }
}
