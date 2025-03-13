/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the "Elastic License
 * 2.0", the "GNU Affero General Public License v3.0 only", and the "Server Side
 * Public License v 1"; you may not use this file except in compliance with, at
 * your election, the "Elastic License 2.0", the "GNU Affero General Public
 * License v3.0 only", or the "Server Side Public License, v 1".
 */

package org.elasticsearch.packaging.util;

import org.testcontainers.containers.GenericContainer;

import java.nio.file.Path;
import java.nio.file.Paths;

public interface Installation {

    static DefaultInstallation ofArchive(Shell sh, Distribution distribution, Path home) {
        return new DefaultInstallation(
            sh,
            distribution,
            home,
            home.resolve("config"),
            home.resolve("data"),
            home.resolve("logs"),
            home.resolve("plugins"),
            home.resolve("modules"),
            null,
            null,
            9200
        );
    }

    static DefaultInstallation ofPackage(Shell sh, Distribution distribution) {

        final Path envFile = (distribution.packaging == Distribution.Packaging.RPM)
            ? Paths.get("/etc/sysconfig/elasticsearch")
            : Paths.get("/etc/default/elasticsearch");

        return new DefaultInstallation(
            sh,
            distribution,
            Paths.get("/usr/share/elasticsearch"),
            Paths.get("/etc/elasticsearch"),
            Paths.get("/var/lib/elasticsearch"),
            Paths.get("/var/log/elasticsearch"),
            Paths.get("/usr/share/elasticsearch/plugins"),
            Paths.get("/usr/share/elasticsearch/modules"),
            Paths.get("/var/run/elasticsearch"),
            envFile,
            9200
        );
    }

    static DefaultInstallation ofContainer(Shell sh, Distribution distribution, int port) {
        String root = "/usr/share/elasticsearch";
        return new DefaultInstallation(
            sh,
            distribution,
            Paths.get(root),
            Paths.get(root + "/config"),
            Paths.get(root + "/data"),
            Paths.get(root + "/logs"),
            Paths.get(root + "/plugins"),
            Paths.get(root + "/modules"),
            null,
            null,
            port
        );
    }

    static DefaultInstallation ofContainer(Shell sh, Distribution distribution) {
        return ofContainer(sh, distribution, 9200);
    }

    static Installation ofContainer(GenericContainer<?> container, Distribution distribution) {
        return new DockerInstallation(container, distribution);
    }

    class DockerInstallation implements Installation {
        public DockerInstallation(GenericContainer<?> container, Distribution distribution) {

        }
    }
}
