/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.gradle.internal.conventions.info;

import org.gradle.api.Project;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.process.ExecOperations;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ParallelDetector {

    private static Integer _defaultParallel = null;

    public static int findDefaultParallel(ExecOperations execOperations, ProviderFactory providerFactory) {
        // Since it costs IO to compute this, and is done at configuration time we want to cache this if possible
        // It's safe to store this in a static variable since it's just a primitive so leaking memory isn't an issue
        if (_defaultParallel == null) {
            File cpuInfoFile = new File("/proc/cpuinfo");
            if (cpuInfoFile.exists()) {
                // Count physical cores on any Linux distro ( don't count hyper-threading )
                Map<String, Integer> socketToCore = new HashMap<>();
                String currentID = "";

                try (BufferedReader reader = new BufferedReader(new FileReader(cpuInfoFile))) {
                    for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                        if (line.contains(":")) {
                            List<String> parts = Arrays.stream(line.split(":", 2)).map(String::trim).collect(Collectors.toList());
                            String name = parts.get(0);
                            String value = parts.get(1);
                            // the ID of the CPU socket
                            if (name.equals("physical id")) {
                                currentID = value;
                            }
                            // Number of cores not including hyper-threading
                            if (name.equals("cpu cores")) {
                                assert currentID.isEmpty() == false;
                                socketToCore.put("currentID", Integer.valueOf(value));
                                currentID = "";
                            }
                        }
                    }
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
                _defaultParallel = socketToCore.values().stream().mapToInt(i -> i).sum();
            } else if (isMac(providerFactory)) {
                // Ask macOS to count physical CPUs for us
                ByteArrayOutputStream stdout = new ByteArrayOutputStream();
                execOperations.exec(spec -> {
                    spec.setExecutable("sysctl");
                    spec.args("-n", "hw.physicalcpu");
                    spec.setStandardOutput(stdout);
                });

                _defaultParallel = Integer.parseInt(stdout.toString().trim());
            }

            _defaultParallel = Runtime.getRuntime().availableProcessors() / 2;
        }

        return _defaultParallel;
    }

    private static boolean isMac(ProviderFactory providers) {
        return providers.systemProperty("os.name").forUseAtConfigurationTime().getOrElse("").startsWith("Mac");
    }

}
