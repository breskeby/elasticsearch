/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the "Elastic License
 * 2.0", the "GNU Affero General Public License v3.0 only", and the "Server Side
 * Public License v 1"; you may not use this file except in compliance with, at
 * your election, the "Elastic License 2.0", the "GNU Affero General Public
 * License v3.0 only", or the "Server Side Public License, v 1".
 */

package org.elasticsearch.test.fixtures.elasticsearch;

import org.apache.lucene.util.IOUtils;
import org.elasticsearch.test.fixtures.testcontainers.DockerEnvironmentAwareTestContainer;
import org.junit.rules.TemporaryFolder;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.Network;
import org.testcontainers.images.RemoteDockerImage;
import org.testcontainers.shaded.org.apache.commons.io.FileUtils;
import org.testcontainers.utility.MountableFile;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

public final class OldElasticsearchTestContainer extends DockerEnvironmentAwareTestContainer {
    public static final String DOCKER_IMAGE = "es6820-fixture:1.0";
    public static final int PORT = 9200;
    private final TemporaryFolder tempFolder = new TemporaryFolder();

    private Callable<File> repoPath;

    private Map<String,String> settings =  new HashMap<>();

    public OldElasticsearchTestContainer() {
        super(new RemoteDockerImage(DOCKER_IMAGE));
        withNetwork(Network.newNetwork());
        addExposedPorts(PORT);
        canBeReused();
    }

    public OldElasticsearchTestContainer withSetting(String key, String value) {
        this.settings.put(key, value);
        return this;
    }

    public OldElasticsearchTestContainer withRepoPath(Callable<File> repoPath) {
        this.repoPath = repoPath;
        return this;
    }

    public String getRepositoryPath() {
        return "/usr/share/elasticsearch/repo";
    }

    @Override
    public void start() {
        try {
            tempFolder.create();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        settings.put("network.host", "0.0.0.0");
        settings.put("xpack.license.self_generated.type", "trial");
        settings.put("xpack.ml.enabled", "false");

        if(repoPath != null) {
            File repoPathCalled = null;
            try {
                repoPathCalled = repoPath.call();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            withFileSystemBind(repoPathCalled.getAbsolutePath(), getRepositoryPath(), BindMode.READ_WRITE);
            settings.put("path.repo", getRepositoryPath());
        }

        try {
            File file = tempFolder.newFile("elasticsearch.yml");
            try (FileWriter writer = new FileWriter(file)) {
                settings.forEach((key, value) -> {
                    try {
                        writer.write(key + ": " + value + "\n");
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
            }

            String content = FileUtils.readFileToString(file);
            System.out.println("content = \n" + content);
            withCopyFileToContainer(MountableFile.forHostPath(file.toPath()), "/opt/elasticsearch/config/elasticsearch.yml");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        super.start();
    }

    public int getPort() {
        return getMappedPort(PORT);
    }

}
