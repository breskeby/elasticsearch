/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.gradle.internal;

import groovy.lang.Closure;

import org.gradle.api.file.FileCollection;
import org.gradle.api.file.FileSystemLocation;
import org.gradle.api.file.FileTree;
import org.gradle.api.provider.Provider;
import org.gradle.api.specs.Spec;
import org.gradle.api.tasks.TaskDependency;

import java.io.File;
import java.util.Iterator;
import java.util.Set;
import java.util.Spliterator;
import java.util.function.Consumer;

public class IdeaCompileClassPathFileCollection implements FileCollection {

    @Override
    public File getSingleFile() throws IllegalStateException {
        return delegate.getSingleFile();
    }

    @Override
    public Set<File> getFiles() {
        return delegate.getFiles();
    }

    @Override
    public boolean contains(File file) {
        return delegate.contains(file);
    }

    @Override
    public String getAsPath() {
        return delegate.getAsPath();
    }

    @Override
    public FileCollection plus(FileCollection fileCollection) {
        return delegate.plus(fileCollection);
    }

    @Override
    public FileCollection minus(FileCollection fileCollection) {
        return delegate.minus(fileCollection);
    }

    @Override
    public FileCollection filter(Closure closure) {
        return delegate.filter(closure);
    }

    @Override
    public FileCollection filter(Spec<? super File> spec) {
        return delegate.filter(spec);
    }

    @Override
    public boolean isEmpty() {
        return delegate.isEmpty();
    }

    @Override
    public FileTree getAsFileTree() {
        return delegate.getAsFileTree();
    }

    @Override
    public Provider<Set<FileSystemLocation>> getElements() {
        return delegate.getElements();
    }

    @Override
    public void addToAntBuilder(Object o, String s, AntType antType) {
        delegate.addToAntBuilder(o, s, antType);
    }

    @Override
    public Object addToAntBuilder(Object o, String s) {
        return delegate.addToAntBuilder(o, s);
    }

    @Override
    public Iterator<File> iterator() {
        return delegate.iterator();
    }

    @Override
    public void forEach(Consumer<? super File> action) {
        delegate.forEach(action);
    }

    @Override
    public Spliterator<File> spliterator() {
        return delegate.spliterator();
    }

    @Override
    public TaskDependency getBuildDependencies() {
        return delegate.getBuildDependencies();
    }

    private FileCollection delegate;

    public IdeaCompileClassPathFileCollection(FileCollection delegate) {
        this.delegate = delegate;
    }
}
