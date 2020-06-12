/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.elasticsearch.gradle;

import org.elasticsearch.gradle.info.BuildParams;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

import static org.elasticsearch.gradle.DistributionDownloadPlugin.projectDependency;
import static org.elasticsearch.gradle.DistributionDownloadPlugin.setupRootDownload;

public class InternalDistributionDownloadPlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        project.getPluginManager().apply(DistributionDownloadPlugin.class);
        project.afterEvaluate(this::wireLocalDistributions);
    }

    private void wireLocalDistributions(Project project) {
        NamedDomainObjectContainer<ElasticsearchDistribution> distributionsContainer = DistributionDownloadPlugin.getContainer(project);
        distributionsContainer.matching(d -> d.isLocal()).forEach(d -> {
            Object dependencyNotation = projectDependencyNotation(project, d);
            project.getDependencies().add(d.configuration.getName(), projectDependencyNotation(project, d));
            // ensure a root level download task exists
            if (d.getType().shouldExtract()) {
                setupRootDownload(project.getRootProject(), d, projectDependencyNotation(project, d));
            }
        });
    }

    private Object projectDependencyNotation(Project project, ElasticsearchDistribution distribution) {
        // non-external project, so depend on local build
        if (VersionProperties.getElasticsearch().equals(distribution.getVersion())) {
            return projectDependency(project, distributionProjectPath(distribution), "default");
        } else {
            BwcVersions.UnreleasedVersionInfo unreleasedInfo = BuildParams.getBwcVersions()
                .unreleasedInfo(Version.fromString(distribution.getVersion()));
            assert distribution.getBundledJdk();
            return projectDependency(project, unreleasedInfo.gradleProjectPath, distributionProjectName(distribution));
        }
    }

    private static String distributionProjectPath(ElasticsearchDistribution distribution) {
        String projectPath = ":distribution";
        switch (distribution.getType()) {
            case INTEG_TEST_ZIP:
                projectPath += ":archives:integ-test-zip";
                break;

            case DOCKER:
                projectPath += ":docker:";
                projectPath += distributionProjectName(distribution);
                break;

            default:
                projectPath += distribution.getType() == ElasticsearchDistribution.Type.ARCHIVE ? ":archives:" : ":packages:";
                projectPath += distributionProjectName(distribution);
                break;
        }
        return projectPath;
    }

    /**
     * Works out the gradle project name that provides a distribution artifact.
     *
     * @param distribution the distribution from which to derive a project name
     * @return the name of a project. It is not the full project path, only the name.
     */
    private static String distributionProjectName(ElasticsearchDistribution distribution) {
        ElasticsearchDistribution.Platform platform = distribution.getPlatform();
        Architecture architecture = distribution.getArchitecture();
        String projectName = "";

        final String archString = platform == ElasticsearchDistribution.Platform.WINDOWS || architecture == Architecture.X64
            ? ""
            : "-" + architecture.toString().toLowerCase();

        if (distribution.getFlavor() == ElasticsearchDistribution.Flavor.OSS) {
            projectName += "oss-";
        }

        if (distribution.getBundledJdk() == false) {
            projectName += "no-jdk-";
        }

        switch (distribution.getType()) {
            case ARCHIVE:
                projectName += platform.toString() + archString + (platform == ElasticsearchDistribution.Platform.WINDOWS
                    ? "-zip"
                    : "-tar");
                break;

            case DOCKER:
                projectName += "docker" + archString + "-export";
                break;

            default:
                projectName += distribution.getType();
                break;
        }

        return projectName;
    }
}
