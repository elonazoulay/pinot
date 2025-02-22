/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.pinot.thirdeye.anomaly.detection.trigger;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.pinot.thirdeye.anomaly.task.TaskConstants;
import org.apache.pinot.thirdeye.datalayer.bao.DAOTestBase;
import org.apache.pinot.thirdeye.datalayer.bao.DatasetConfigManager;
import org.apache.pinot.thirdeye.datalayer.bao.DetectionConfigManager;
import org.apache.pinot.thirdeye.datalayer.bao.MetricConfigManager;
import org.apache.pinot.thirdeye.datalayer.bao.TaskManager;
import org.apache.pinot.thirdeye.datalayer.dto.DatasetConfigDTO;
import org.apache.pinot.thirdeye.datalayer.dto.DetectionConfigDTO;
import org.apache.pinot.thirdeye.datalayer.dto.MetricConfigDTO;
import org.apache.pinot.thirdeye.datalayer.dto.TaskDTO;
import org.apache.pinot.thirdeye.datasource.DAORegistry;
import org.apache.pinot.thirdeye.detection.DetectionPipelineTaskInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.apache.pinot.thirdeye.dashboard.resources.EntityManagerResource.*;

public class DataAvailabilityTaskSchedulerTest {
  private static final Logger LOG = LoggerFactory.getLogger(DataAvailabilityTaskSchedulerTest.class);
  private static final long TEST_TIME = System.currentTimeMillis();
  private static String TEST_DATASET_PREFIX = "ds_trigger_scheduler_";
  private DAOTestBase testDAOProvider;
  private DataAvailabilityTaskScheduler dataAvailabilityTaskScheduler;
  private long metricId1;
  private long metricId2;

  @BeforeMethod
  public void beforeMethod() {
    testDAOProvider = DAOTestBase.getInstance();
    MetricConfigManager metricConfigManager = DAORegistry.getInstance().getMetricConfigDAO();
    final String TEST_METRIC_PREFIX = "metric_trigger_scheduler_";

    MetricConfigDTO metric1 = new MetricConfigDTO();
    metric1.setDataset(TEST_DATASET_PREFIX + 1);
    metric1.setName(TEST_METRIC_PREFIX + 1);
    metric1.setActive(true);
    metric1.setDerived(false);
    metric1.setAlias("");
    metricId1 = metricConfigManager.save(metric1);

    MetricConfigDTO metric2 = new MetricConfigDTO();
    metric2.setDataset(TEST_DATASET_PREFIX + 2);
    metric2.setName(TEST_METRIC_PREFIX + 2);
    metric2.setActive(true);
    metric1.setDerived(false);
    metric2.setAlias("");
    metricId2 = metricConfigManager.save(metric2);
    dataAvailabilityTaskScheduler = new DataAvailabilityTaskScheduler(60, 24 * 60 * 60);
  }

  @AfterMethod
  public void afterMethod() {
    testDAOProvider.cleanup();
  }

  @Test
  public void testCreateOneTask() {
    List<Long> metrics = Arrays.asList(metricId1, metricId2);
    long detectionId = createDetection(1, metrics, TEST_TIME - TimeUnit.DAYS.toMillis(1),0);
    createDataset(1, TEST_TIME);
    createDataset(2, TEST_TIME);
    dataAvailabilityTaskScheduler.run();
    TaskManager taskManager = DAORegistry.getInstance().getTaskDAO();
    List<TaskDTO> tasks = taskManager.findAll();
    Assert.assertEquals(tasks.size(), 1);
    TaskDTO task = tasks.get(0);
    Assert.assertEquals(task.getStatus(), TaskConstants.TaskStatus.WAITING);
    Assert.assertEquals(task.getJobName(), TaskConstants.TaskType.DETECTION.toString() + "_" + detectionId);
  }

  @Test
  public void testCreateMultipleTasks() {
    List<Long> metrics1 = Arrays.asList(metricId1, metricId2);
    long oneDayAgo = TEST_TIME - TimeUnit.DAYS.toMillis(1);
    long detection1 = createDetection(1, metrics1, oneDayAgo, 0);
    long detection2 = createDetection(2, metrics1, oneDayAgo, 0);
    List<Long> singleMetric = Collections.singletonList(metricId2);
    long detection3 = createDetection(3, singleMetric, oneDayAgo, 0);
    createDataset(1, TEST_TIME);
    createDataset(2, TEST_TIME);
    dataAvailabilityTaskScheduler.run();
    TaskManager taskManager = DAORegistry.getInstance().getTaskDAO();
    List<TaskDTO> tasks = taskManager.findAll();
    Assert.assertEquals(tasks.size(), 3);
    Assert.assertEquals(tasks.get(0).getStatus(), TaskConstants.TaskStatus.WAITING);
    Assert.assertEquals(tasks.get(1).getStatus(), TaskConstants.TaskStatus.WAITING);
    Assert.assertEquals(tasks.get(2).getStatus(), TaskConstants.TaskStatus.WAITING);
    Assert.assertEquals(
        Stream.of(detection1, detection2, detection3)
            .map(x -> TaskConstants.TaskType.DETECTION.toString() + "_" + x)
            .collect(Collectors.toSet()),
        new HashSet<>(Arrays.asList(tasks.get(0).getJobName(), tasks.get(1).getJobName(), tasks.get(2).getJobName())));
  }

  @Test
  public void testNoReadyDetection() {
    List<Long> metrics1 = Arrays.asList(metricId1, metricId2);
    long detection1 = createDetection(1, metrics1, TEST_TIME, 0);
    long detection2 = createDetection(2, Collections.singletonList(metricId2), TEST_TIME, 0);
    createDataset(1, TEST_TIME + TimeUnit.HOURS.toMillis(1)); // updated dataset
    createDataset(2, TEST_TIME - TimeUnit.HOURS.toMillis(1)); // not updated dataset
    createDetectionTask(detection1, TEST_TIME - 60_000, TaskConstants.TaskStatus.COMPLETED);
    createDetectionTask(detection2, TEST_TIME - 60_000, TaskConstants.TaskStatus.COMPLETED);
    DetectionConfigManager detectionConfigManager = DAORegistry.getInstance().getDetectionConfigManager();
    List<DetectionConfigDTO> detectionConfigs = detectionConfigManager.findAll();
    Assert.assertEquals(detectionConfigs.size(), 2);
    dataAvailabilityTaskScheduler.run();
    TaskManager taskManager = DAORegistry.getInstance().getTaskDAO();
    List<TaskDTO> tasks = taskManager.findAll();
    Assert.assertEquals(tasks.size(), 2);
  }

  @Test
  public void testDetectionExceedNotRunThreshold() {
    List<Long> metrics1 = Arrays.asList(metricId1, metricId2);
    long oneDayAgo = TEST_TIME - TimeUnit.DAYS.toMillis(1);
    long detection1 = createDetection(1, metrics1, oneDayAgo, 0);
    long detection2 = createDetection(2, Collections.singletonList(metricId2), oneDayAgo, 0);
    long detection3 = createDetection(3, Collections.singletonList(metricId2), oneDayAgo, 2 * 24 * 60 * 60);
    createDataset(1, oneDayAgo - 60_000); // not updated dataset
    createDataset(2, oneDayAgo - 60_000); // not updated dataset
    createDetectionTask(detection1, oneDayAgo - 60_000, TaskConstants.TaskStatus.COMPLETED);
    createDetectionTask(detection2, oneDayAgo - 60_000, TaskConstants.TaskStatus.COMPLETED);
    createDetectionTask(detection3, oneDayAgo - 60_000, TaskConstants.TaskStatus.COMPLETED);
    dataAvailabilityTaskScheduler.run();
    TaskManager taskManager = DAORegistry.getInstance().getTaskDAO();
    List<TaskDTO> tasks = taskManager.findAll();
    Assert.assertEquals(tasks.size(), 5);
    Assert.assertEquals(tasks.stream().filter(t -> t.getStatus() == TaskConstants.TaskStatus.COMPLETED).count(), 3);
    Assert.assertEquals(tasks.stream().filter(t -> t.getStatus() == TaskConstants.TaskStatus.WAITING).count(), 2);
  }

  @Test
  public void testScheduleWithUnfinishedTask() {
    List<Long> metrics1 = Arrays.asList(metricId1, metricId2);
    long oneDayAgo = TEST_TIME - TimeUnit.DAYS.toMillis(1);
    long detection1 = createDetection(1, metrics1, oneDayAgo, 0);
    long detection2 = createDetection(2, metrics1, oneDayAgo, 0);
    List<Long> singleMetric = Collections.singletonList(metricId2);
    long detection3 = createDetection(3, singleMetric, oneDayAgo, 0);
    createDataset(1, TEST_TIME);
    createDataset(2, TEST_TIME);
    createDetectionTask(detection1, oneDayAgo, TaskConstants.TaskStatus.RUNNING);
    dataAvailabilityTaskScheduler.run();
    TaskManager taskManager = DAORegistry.getInstance().getTaskDAO();
    List<TaskDTO> tasks = taskManager.findAll();
    Assert.assertEquals(tasks.size(), 3);
    List<TaskDTO> waitingTasks = tasks.stream().filter(t -> t.getStatus() == TaskConstants.TaskStatus.WAITING).collect(
        Collectors.toList());
    Assert.assertEquals(tasks.stream().filter(t -> t.getStatus() == TaskConstants.TaskStatus.RUNNING).count(), 1);
    Assert.assertEquals(waitingTasks.size(), 2);
    Assert.assertEquals(
        Stream.of(detection2, detection3)
            .map(x -> TaskConstants.TaskType.DETECTION.toString() + "_" + x)
            .collect(Collectors.toSet()),
        new HashSet<>(Arrays.asList(tasks.get(1).getJobName(), tasks.get(2).getJobName())));
  }

  private long createDataset(int intSuffix, long refreshTime) {
    DatasetConfigManager datasetConfigDAO = DAORegistry.getInstance().getDatasetConfigDAO();
    DatasetConfigDTO ds1 = new DatasetConfigDTO();
    ds1.setDataset(TEST_DATASET_PREFIX + intSuffix);
    ds1.setLastRefreshTime(refreshTime);
    return datasetConfigDAO.save(ds1);
  }

  private long createDetection(int intSuffix, List<Long> metrics, long lastTimestamp, int notRunThreshold) {
    DetectionConfigManager detectionConfigManager = DAORegistry.getInstance().getDetectionConfigManager();
    final String TEST_DETECTION_PREFIX = "detection_trigger_listener_";
    DetectionConfigDTO detect = new DetectionConfigDTO();
    detect.setName(TEST_DETECTION_PREFIX + intSuffix);
    detect.setActive(true);
    Map<String, Object> props = new HashMap<>();
    List<String> metricUrns = metrics.stream().map(x -> "thirdeye:metric:" + x).collect(Collectors.toList());
    props.put("nestedMetricUrns", metricUrns);
    detect.setProperties(props);
    detect.setLastTimestamp(lastTimestamp);
    detect.setDataAvailabilitySchedule(true);
    detect.setTaskTriggerFallBackTimeInSec(notRunThreshold);
    return detectionConfigManager.save(detect);
  }

  private long createDetectionTask(long detectionId, long createTime, TaskConstants.TaskStatus status) {
    TaskManager taskManager = DAORegistry.getInstance().getTaskDAO();
    TaskDTO task = new TaskDTO();
    DetectionPipelineTaskInfo taskInfo = new DetectionPipelineTaskInfo(detectionId, createTime - 1, createTime);
    String taskInfoJson = null;
    try {
      taskInfoJson = OBJECT_MAPPER.writeValueAsString(taskInfo);
    } catch (JsonProcessingException e) {
      LOG.error("Exception when converting DetectionPipelineTaskInfo {} to jsonString", taskInfo, e);
    }
    task.setTaskType(TaskConstants.TaskType.DETECTION);
    task.setJobName(TaskConstants.TaskType.DETECTION.toString() + "_" + detectionId);
    task.setStatus(status);
    task.setTaskInfo(taskInfoJson);
    return taskManager.save(task);
  }

}
