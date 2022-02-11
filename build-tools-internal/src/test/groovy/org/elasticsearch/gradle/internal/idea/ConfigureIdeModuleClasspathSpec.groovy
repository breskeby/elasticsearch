/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1 you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.gradle.internal.idea

import org.apache.tools.ant.filters.StringInputStream
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import org.w3c.dom.Document
import spock.lang.Specification

import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.Transformer
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

class ConfigureIdeModuleClasspathSpec extends Specification {

    @Rule
    TemporaryFolder testProjectDir = new TemporaryFolder()

    File ideaRoot

    def setup() {
        ideaRoot = testProjectDir.newFolder("idea")
    }

    def "links sources for api jars"() {
        Project project = ProjectBuilder.builder().withProjectDir(testProjectDir.root).build()
        given:
        def task = project.tasks.create("tweakIdeaClasspath", ConfigureIdeModuleClasspath)
        setupIdeaIml()
        when:
        task.setIdeaRoot(ideaRoot)
        task.tweakClasspath()
        then:
        formattedXml(consumerImlFile().text) == formattedXml('''
<module external.linked.project.id=":consumer:main" external.linked.project.path="$MODULE_DIR$/../../../../consumer" external.root.project.path="$MODULE_DIR$/../../../.." external.system.id="GRADLE" external.system.module.group="org.elasticsearch.client" external.system.module.type="sourceSet" external.system.module.version="8.1.0-SNAPSHOT" type="JAVA_MODULE" version="4">
  <component name="NewModuleRootManager" LANGUAGE_LEVEL="JDK_17">
    <output url="file://$MODULE_DIR$/../../../../client/benchmark/build/classes/java/main" />
    <exclude-output />
    <content url="file://$MODULE_DIR$/../../../../client/benchmark/src/main">
      <sourceFolder url="file://$MODULE_DIR$/../../../../client/benchmark/src/main/java" isTestSource="false" />
      <sourceFolder url="file://$MODULE_DIR$/../../../../client/benchmark/src/main/resources" type="java-resource" />
    </content>
    <orderEntry type="inheritedJdk" />
    <orderEntry type="sourceFolder" forTests="false" />
    <orderEntry type="library" name="Gradle: org.apache.commons:commons-math3:3.2" level="project" />
    <orderEntry type="module-library" scope="PROVIDED">
      <library>
        <CLASSES>
          <root url="file://$MODULE_DIR$/../../../../server/build/java-module-api" />
        </CLASSES>
        <JAVADOC />
        <SOURCES>
            <root url="file://$MODULE_DIR$/../../../../server/src/main/java" />
        </SOURCES>
      </library>
    </orderEntry>
  </component>
</module>
''')
    }

    private String formattedXml(String input) {
        def serialize = serialize(input)
        serialize
    }

    void setupIdeaIml() {
        consumerImlFile() <<
                serialize('''
<module external.linked.project.id=":consumer:main" external.linked.project.path="$MODULE_DIR$/../../../../consumer" external.root.project.path="$MODULE_DIR$/../../../.." external.system.id="GRADLE" external.system.module.group="org.elasticsearch.client" external.system.module.type="sourceSet" external.system.module.version="8.1.0-SNAPSHOT" type="JAVA_MODULE" version="4">
  <component name="NewModuleRootManager" LANGUAGE_LEVEL="JDK_17">
    <output url="file://$MODULE_DIR$/../../../../client/benchmark/build/classes/java/main" />
    <exclude-output />
    <content url="file://$MODULE_DIR$/../../../../client/benchmark/src/main">
      <sourceFolder url="file://$MODULE_DIR$/../../../../client/benchmark/src/main/java" isTestSource="false" />
      <sourceFolder url="file://$MODULE_DIR$/../../../../client/benchmark/src/main/resources" type="java-resource" />
    </content>
    <orderEntry type="inheritedJdk" />
    <orderEntry type="sourceFolder" forTests="false" />
    <orderEntry type="library" name="Gradle: org.apache.commons:commons-math3:3.2" level="project" />
    <orderEntry type="module-library" scope="PROVIDED">
      <library>
        <CLASSES>
          <root url="file://$MODULE_DIR$/../../../../server/build/java-module-api" />
        </CLASSES>
        <JAVADOC />
        <SOURCES/>
      </library>
    </orderEntry>
  </component>
</module>
'''.stripIndent())
    }

    private File consumerImlFile() {
        def imlFile = new File(ideaRoot, "modules/consumer/elasticsearch.consumer.main.iml")
        imlFile.parentFile.mkdirs()
        imlFile
    }

    private String serialize(String inputString) throws Exception {
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance()
        DocumentBuilder docBuilder = docFactory.newDocumentBuilder()
        Document doc = docBuilder.parse(new StringInputStream(inputString))
        doc.getDocumentElement().normalize()
        TransformerFactory transformerFactory = TransformerFactory.newInstance()

        Transformer transformer = transformerFactory.newTransformer()
        DOMSource source = new DOMSource(doc)
        def stream = new ByteArrayOutputStream()
        StreamResult result = new StreamResult(stream)
        transformer.setOutputProperty(OutputKeys.INDENT, "yes")
        transformer.transform(source, result)
        return stream.toString()
    }
}
