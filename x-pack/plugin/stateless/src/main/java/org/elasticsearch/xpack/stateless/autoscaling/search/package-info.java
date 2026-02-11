/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

/**
 * This package contains infrastructure to collect and provide information necessary on scaling the serverless
 * search tier. Cluster scaling decisions are ultimately made by the
 * <a href="https://github.com/elastic/elasticsearch-autoscaler/">elasticsearch-autoscaler</a>
 * in the control plane, but the services in this package collect and prepare the signals the controllers decisions
 * are based on.
 * <p>
 * There are two main services that perform periodic tasks:
 *
 *  <ul>
 *     <li>{@link org.elasticsearch.xpack.stateless.autoscaling.search.SearchMetricsService}</li>
 *     <li>{@link org.elasticsearch.xpack.stateless.autoscaling.search.ReplicasUpdaterService}</li>
 * </ul>
 *
 * For more information see the service classes javadocs.
 */
package org.elasticsearch.xpack.stateless.autoscaling.search;
