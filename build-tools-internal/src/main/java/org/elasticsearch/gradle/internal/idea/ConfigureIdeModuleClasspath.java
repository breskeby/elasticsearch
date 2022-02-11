/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.gradle.internal.idea;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.FileVisitDetails;
import org.gradle.api.file.FileVisitor;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.TaskAction;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.elasticsearch.gradle.internal.idea.IdeaJavaModuleApiPlugin.JAVA_MODULE_API_IDENTIFIER;
import static org.elasticsearch.gradle.internal.idea.XmlUtils.docFromFile;
import static org.elasticsearch.gradle.internal.idea.XmlUtils.writeDoc;

public class ConfigureIdeModuleClasspath extends DefaultTask {

    private File ideaRoot;

    @InputDirectory
    public File getIdeaRoot() {
        return ideaRoot;
    }

    public void setIdeaRoot(File ideaRoot) {
        this.ideaRoot = ideaRoot;
    }

    @TaskAction
    void tweakClasspath() {
        // throw new GradleException("boom");
        File modulesRoot = new File(ideaRoot, "modules");
        getProject().fileTree(modulesRoot).matching(patternFilterable -> patternFilterable.include("**/*.iml")).visit(new FileVisitor() {
            @Override
            public void visitDir(FileVisitDetails fileDetails) {
            }

            @Override
            public void visitFile(FileVisitDetails fileVisitDetails) {
                File imlFile = fileVisitDetails.getFile();
                Document doc = docFromFile(imlFile);
                visitDoc(doc);
                writeDoc(doc, imlFile);
            }
        });
    }

    private void visitDoc(Document doc) {
        updateOrderEntriesValue(doc);
    }

    private static void updateOrderEntriesValue(Document doc) throws GradleException {
        NodeList orderEntries = doc.getElementsByTagName("orderEntry");
        for (int i = 0; i < orderEntries.getLength(); i++) {
            Element entry = (Element) orderEntries.item(i);
            if (isModuleApiReference(entry)) {
                if (entry.getAttribute("type").equals("module-library")) {
                    Element classes = (Element) entry.getElementsByTagName("CLASSES").item(0);
                    Element classesRoot = (Element) classes.getElementsByTagName("root").item(0);
                    if (classesRoot != null) {
                        String classesUrl = classesRoot.getAttribute("url");
                        Element sources = (Element) entry.getElementsByTagName("SOURCES").item(0);
                        Element rootSourceElement = doc.createElement("root");
                        rootSourceElement.setAttribute("url", calculateSourceRootFromClasses(classesUrl));
                        sources.appendChild(rootSourceElement);
                    }
                }
            }
        }
    }

    private static String calculateSourceRootFromClasses(String classesUrl) {
        Pattern p = Pattern.compile(".*\\$MODULE_DIR\\$/(.*)(build/java-module-api)");
        Matcher m = p.matcher(classesUrl);
        if(m.matches()) {
            return "file://$MODULE_DIR$/" + m.group(1) + "src/main/java";
        }
        throw new GradleException("Cannot calculate Root source for " + classesUrl);
    }

    private static boolean isModuleApiReference(Element element) {
        if (element.getAttribute("type").equals("module-library")) {
            Element classes = (Element) element.getElementsByTagName("CLASSES").item(0);
            Element classesRoot = (Element) classes.getElementsByTagName("root").item(0);
            if (classesRoot != null) {
                String classesUrl = classesRoot.getAttribute("url");
                return classesUrl.matches(".*" + JAVA_MODULE_API_IDENTIFIER + ".*");
            }
        }
        return false;
    }
}
