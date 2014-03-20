/*
 * Copyright 2012-2014, Continuuity, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.continuuity.loom.management;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Collects Loom stats for JMX.
 */
public class LoomStats implements LoomStatsMXBean {
  private final AtomicInteger queueLength;

  private final ProvisionerStats provisionerStats;
  private final ProvisionerStats failedProvisionerStats;
  private final ProvisionerStats successfulProvisionerStats;
  private final ProvisionerStats droppedProvisionerStats;

  private final ClusterStats clusterStats;
  private final ClusterStats failedClusterStats;
  private final ClusterStats successfulClusterStats;

  public LoomStats() {
    this.queueLength = new AtomicInteger(0);

    this.provisionerStats = new ProvisionerStats();
    this.failedProvisionerStats = new ProvisionerStats();
    this.successfulProvisionerStats = new ProvisionerStats();
    this.droppedProvisionerStats = new ProvisionerStats();

    this.clusterStats = new ClusterStats();
    this.failedClusterStats = new ClusterStats();
    this.successfulClusterStats = new ClusterStats();
  }

  @Override
  public long getQueueLength() {
    return queueLength.get();
  }

  @Override
  public ProvisionerStats getProvisionerStats() {
    return provisionerStats;
  }

  @Override
  public ProvisionerStats getFailedProvisionerStats() {
    return failedProvisionerStats;
  }

  @Override
  public ProvisionerStats getSuccessfulProvisionerStats() {
    return successfulProvisionerStats;
  }

  @Override
  public ProvisionerStats getDroppedProvisionerStats() {
    return droppedProvisionerStats;
  }

  @Override
  public ClusterStats getClusterStats() {
    return clusterStats;
  }

  @Override
  public ClusterStats getFailedClusterStats() {
    return failedClusterStats;
  }

  @Override
  public ClusterStats getSuccessfulClusterStats() {
    return successfulClusterStats;
  }

  public void setQueueLength(int queueLength) {
    this.queueLength.set(queueLength);
  }
}
