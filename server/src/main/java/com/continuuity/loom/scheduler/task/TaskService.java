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
package com.continuuity.loom.scheduler.task;

import com.continuuity.loom.cluster.Cluster;
import com.continuuity.loom.common.conf.Constants;
import com.continuuity.loom.common.queue.Element;
import com.continuuity.loom.common.queue.QueueGroup;
import com.continuuity.loom.common.zookeeper.IdService;
import com.continuuity.loom.management.LoomStats;
import com.continuuity.loom.scheduler.Actions;
import com.continuuity.loom.scheduler.ClusterAction;
import com.continuuity.loom.scheduler.callback.CallbackData;
import com.continuuity.loom.spec.ProvisionerAction;
import com.continuuity.loom.store.cluster.ClusterStore;
import com.continuuity.loom.store.cluster.ClusterStoreService;
import com.continuuity.loom.store.credential.CredentialStore;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

/**
 * Service for performing operations on {@link ClusterTask}s.
 */
public class TaskService {
  private static final Logger LOG = LoggerFactory.getLogger(TaskService.class);

  private final ClusterStore clusterStore;
  private final CredentialStore credentialStore;
  private final Actions actions = Actions.getInstance();
  private final LoomStats loomStats;
  private final IdService idService;
  private final Gson gson;
  private final QueueGroup callbackQueues;

  @Inject
  private TaskService(ClusterStoreService clusterStoreService,
                      CredentialStore credentialStore,
                      LoomStats loomStats,
                      @Named(Constants.Queue.CALLBACK) QueueGroup callbackQueues,
                      IdService idService,
                      Gson gson) {
    this.clusterStore = clusterStoreService.getSystemView();
    this.credentialStore = credentialStore;
    this.loomStats = loomStats;
    this.idService = idService;
    this.gson = gson;
    this.callbackQueues = callbackQueues;
  }

  /**
   * Get the rollback task that should run if the given task fails.
   *
   * @param task Task that needs to get rolled back.
   * @return Cluster task that will roll back the given failed task.
   */
  private ClusterTask getRollbackTask(ClusterTask task) {
    ProvisionerAction rollback = actions.getRollbackActions().get(task.getTaskName());
    if (rollback == null) {
      return null;
    }

    TaskId rollbackTaskId = idService.getNewTaskId(JobId.fromString(task.getJobId()));
    ClusterTask rollbackTask = new ClusterTask(rollback, rollbackTaskId, task.getNodeId(),
                                               task.getService(), task.getClusterAction());

    return rollbackTask;
  }

  /**
   * Get tasks that must be run in order to retry the given task that failed on a given node in a given cluster.
   * For example, to retry a service installation, we just retry the task again. However, to retry a node confirm,
   * we need to first delete the created node, create another node, and then confirm that node.
   *
   * @param task Task that failed and must be retried.
   * @return List of tasks that must be executed to retry the given failed task.
   */
  public List<ClusterTask> getRetryTask(ClusterTask task) {
    ProvisionerAction retryAction = actions.getRetryAction().get(task.getTaskName());

    // If no retry action, return self.
    if (retryAction == null) {
      return ImmutableList.of(task);
    }

    List<ProvisionerAction> taskOrder = actions.getActionOrder().get(task.getClusterAction());
    if (taskOrder == null) {
      throw new IllegalStateException("Not able to get task order for action " + task.getClusterAction());
    }

    List<ClusterTask> retryTasks = Lists.newArrayList();
    // check if the task needs to be rolled back. Currently only the case for confirm tasks, which need to delete
    // the node they were confirming before they can create another node.
    ClusterTask rollbackTask = getRollbackTask(task);
    if (rollbackTask != null) {
      retryTasks.add(rollbackTask);
    }
    // Create tasks from retry task to current task.
    int retryActionIndex = taskOrder.indexOf(retryAction);
    int currentActionIndex = taskOrder.indexOf(task.getTaskName());
    for (int i = retryActionIndex; i < currentActionIndex; ++i) {
      ProvisionerAction action = taskOrder.get(i);
      TaskId retryTaskId = idService.getNewTaskId(JobId.fromString(task.getJobId()));
      ClusterTask retry = new ClusterTask(action, retryTaskId, task.getNodeId(), task.getService(),
                                          task.getClusterAction());
      retryTasks.add(retry);
    }
    retryTasks.add(task);
    return retryTasks;
  }

  /**
   * Sets the status of the given job to {@link ClusterJob.Status#FAILED} and the status of the cluster to some given
   * status.
   *
   * @param job Job to fail.
   * @param cluster Cluster to set the status for.
   * @param status Status to set the cluster to.
   * @param message Error message.
   * @throws IOException
   * @throws IllegalAccessException
   */
  public void failJobAndSetClusterStatus(ClusterJob job, Cluster cluster, Cluster.Status status, String message)
    throws IOException, IllegalAccessException {
    cluster.setStatus(status);
    clusterStore.writeCluster(cluster);

    job.setJobStatus(ClusterJob.Status.FAILED);
    if (message != null) {
      job.setStatusMessage(message);
    }
    clusterStore.writeClusterJob(job);

    loomStats.getFailedClusterStats().incrementStat(job.getClusterAction());
    callbackQueues.add(cluster.getAccount().getTenantId(),
                       new Element(gson.toJson(new CallbackData(CallbackData.Type.FAILURE, cluster, job))));
  }

  /**
   * Sets the status of the given job to {@link ClusterJob.Status#FAILED} and the status of the cluster to the default
   * failure status as given in {@link com.continuuity.loom.scheduler.ClusterAction#getFailureStatus()}.
   *
   * @param job Job to fail.
   * @param cluster Cluster to set the status for.
   * @throws IOException
   */
  public void failJobAndSetClusterStatus(ClusterJob job, Cluster cluster) throws IOException, IllegalAccessException {
    failJobAndSetClusterStatus(job, cluster, job.getClusterAction().getFailureStatus(), null);
  }

  /**
   * Sets the status of the given job to {@link ClusterJob.Status#FAILED} and the status of the given cluster to
   * {@link com.continuuity.loom.cluster.Cluster.Status#TERMINATED}.
   *
   * @param job Job to fail.
   * @param cluster Cluster to terminate.
   * @param message Error message.
   * @throws IOException
   */
  public void failJobAndTerminateCluster(ClusterJob job, Cluster cluster, String message)
    throws IOException, IllegalAccessException {
    failJobAndSetClusterStatus(job, cluster, Cluster.Status.TERMINATED, message);
  }

  /**
   * Sets the status of the given job to {@link ClusterJob.Status#FAILED} and persists it to the store.
   *
   * @param job Job to fail.
   * @throws IOException
   */
  public void failJob(ClusterJob job) throws IOException {
    job.setJobStatus(ClusterJob.Status.FAILED);
    clusterStore.writeClusterJob(job);
  }

  /**
   * Sets the status of the given job to {@link ClusterJob.Status#RUNNING} and add it to the queue to be run.
   *
   * @param job Job to start.
   * @param cluster Cluster the job is for.
   * @throws IOException
   */
  public void startJob(ClusterJob job, Cluster cluster) throws IOException {
    // TODO: wrap in a transaction
    LOG.debug("Starting job {} for cluster {}", job.getJobId(), cluster.getId());
    job.setJobStatus(ClusterJob.Status.RUNNING);
    // Note: writing job status as RUNNING, will allow other operations on the job
    // (like cancel, etc.) to happen in parallel.
    clusterStore.writeClusterJob(job);
    callbackQueues.add(cluster.getAccount().getTenantId(),
                       new Element(gson.toJson(new CallbackData(CallbackData.Type.START, cluster, job))));
  }

  /**
   * Sets the status of the given job to {@link ClusterJob.Status#COMPLETE} and the status of the given cluster to
   * {@link com.continuuity.loom.cluster.Cluster.Status#ACTIVE}.
   *
   * @param job Job to complete.
   * @param cluster Cluster the job was for.
   * @throws IOException
   */
  public void completeJob(ClusterJob job, Cluster cluster) throws IOException, IllegalAccessException {
    job.setJobStatus(ClusterJob.Status.COMPLETE);
    clusterStore.writeClusterJob(job);
    LOG.debug("Job {} is complete", job.getJobId());

    // Update cluster status
    if (job.getClusterAction() == ClusterAction.CLUSTER_DELETE) {
      cluster.setStatus(Cluster.Status.TERMINATED);
    } else {
      cluster.setStatus(Cluster.Status.ACTIVE);
    }
    clusterStore.writeCluster(cluster);

    loomStats.getSuccessfulClusterStats().incrementStat(job.getClusterAction());
    if (job.getClusterAction() == ClusterAction.CLUSTER_DELETE) {
      wipeSensitiveFields(cluster);
    }
    callbackQueues.add(cluster.getAccount().getTenantId(),
                       new Element(gson.toJson(new CallbackData(CallbackData.Type.SUCCESS, cluster, job))));
  }

  /**
   * Starts a task by setting the status of the task to {@link ClusterTask.Status#IN_PROGRESS} and the submit time
   * to the current timestamp.
   *
   * @param clusterTask Task to start.
   * @throws IOException
   */
  public void startTask(ClusterTask clusterTask) throws IOException {
    clusterTask.setStatus(ClusterTask.Status.IN_PROGRESS);
    clusterTask.setSubmitTime(System.currentTimeMillis());
    clusterStore.writeClusterTask(clusterTask);

    // Update stats
    loomStats.getProvisionerStats().incrementStat(clusterTask.getTaskName());
  }

  /**
   * Drop a task by setting the status of the task to {@link ClusterTask.Status#DROPPED} and the status time to the
   * current timestamp. Tasks can be dropped if there is no longer any point in executing them. For example, if another
   * task in the same stage has failed, the entire job cannot complete so there is no point in executing any
   * unexecuted task in the job.
   *
   * @param clusterTask Task to drop.
   * @throws IOException
   */
  public void dropTask(ClusterTask clusterTask) throws IOException {
    clusterTask.setStatus(ClusterTask.Status.DROPPED);
    clusterTask.setStatusTime(System.currentTimeMillis());
    clusterStore.writeClusterTask(clusterTask);

    // Update stats
    loomStats.getDroppedProvisionerStats().incrementStat(clusterTask.getTaskName());
  }

  /**
   * Fail a task by setting the status of the task to {@link ClusterTask.Status#FAILED} and the status time to the
   * current timestamp and the status code to the given code.
   *
   * @param clusterTask Task to fail.
   * @param status Status code of the failed task.
   * @throws IOException
   */
  public void failTask(ClusterTask clusterTask, int status) throws IOException {
    clusterTask.setStatus(ClusterTask.Status.FAILED);
    clusterTask.setStatusCode(status);
    clusterTask.setStatusTime(System.currentTimeMillis());
    clusterStore.writeClusterTask(clusterTask);

    // Update stats
    loomStats.getFailedProvisionerStats().incrementStat(clusterTask.getTaskName());
  }

  /**
   * Complete a task by setting the status of the task to {@link ClusterTask.Status#COMPLETE} and the status time to
   * the current timestamp and the status code to the given code.
   *
   * @param clusterTask Task to complete.
   * @param status Status code of the completed task.
   * @throws IOException
   */
  public void completeTask(ClusterTask clusterTask, int status) throws IOException {
    clusterTask.setStatus(ClusterTask.Status.COMPLETE);
    clusterTask.setStatusCode(status);
    clusterTask.setStatusTime(System.currentTimeMillis());
    clusterStore.writeClusterTask(clusterTask);

    // update stats
    loomStats.getSuccessfulProvisionerStats().incrementStat(clusterTask.getTaskName());
  }

  private void wipeSensitiveFields(Cluster cluster) throws IOException {
    String tenantId = cluster.getAccount().getTenantId();
    String clusterId = cluster.getId();
    LOG.trace("wiping credentials for cluster {} with account {}.", cluster.getId(), cluster.getAccount());
    credentialStore.wipe(tenantId, clusterId);
  }
}
