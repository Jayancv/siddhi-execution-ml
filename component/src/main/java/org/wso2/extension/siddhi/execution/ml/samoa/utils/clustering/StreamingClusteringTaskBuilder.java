/*
 * Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.extension.siddhi.execution.ml.samoa.utils.clustering;

import com.github.javacliparser.ClassOption;
import com.github.javacliparser.FlagOption;
import com.github.javacliparser.IntOption;
import com.github.javacliparser.Option;
import org.apache.samoa.moa.cluster.Clustering;
import org.apache.samoa.tasks.Task;
import org.apache.samoa.topology.impl.SimpleComponentFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.extension.siddhi.execution.ml.samoa.utils.TaskBuilder;
import org.wso2.siddhi.core.exception.ExecutionPlanRuntimeException;

import java.util.Queue;

public class StreamingClusteringTaskBuilder extends TaskBuilder {

    private static final Logger logger =
            LoggerFactory.getLogger(StreamingClusteringTaskBuilder.class);

    public Queue<Clustering> samoaClusters;
    public int numberOfClusters;
    private int parallelism;
    private int samplefrequency;
    private int interval;

    public StreamingClusteringTaskBuilder(int maxInstance, int numberofAttributes,
                                          int numberOfClusters, int parallel, int sampleFrequency, int interval,
                                          Queue<double[]> cepEvents, Queue<Clustering> samoaClusters) {
        this.maxEvents = maxInstance;
        this.numberOfAttributes = numberofAttributes;
        this.numberOfClusters = numberOfClusters;
        this.parallelism = parallel;
        this.samplefrequency = sampleFrequency;
        this.interval = interval;
        this.cepEvents = cepEvents;
        this.samoaClusters = samoaClusters;
    }

    public void initTask() {
        String query = "org.wso2.extension.siddhi.execution.ml.samoa.utils.clustering." +
                "StreamingClusteringTask -i " + maxEvents + " -s  (org.wso2.extension." +
                "siddhi.execution.ml.samoa.utils.clustering.StreamingClusteringStream -K "
                + numberOfClusters + " -a " + numberOfAttributes + ") -l " +
                "(org.wso2.extension.siddhi.execution.ml.samoa.utils.clustering.StreamingDistributor -F "
                + samplefrequency + " -I " + interval + " -P " + parallelism +
                " -l (org.apache.samoa.learners.clusterers.ClustreamClustererAdapter " +
                "-l (org.apache.samoa.moa.clusterers.clustream.WithKmeans -k " +numberOfClusters + ")))";
        logger.info("QUERY: " + query);
        String args[] = {query};
        this.initClusteringTask(args);
    }

    public void initClusteringTask(String[] args) {
        FlagOption suppressStatusOutOpt = new FlagOption("suppressStatusOut", 'S',
                SUPPRESS_STATUS_OUT_MSG);
        FlagOption suppressResultOutOpt = new FlagOption("suppressResultOut", 'R',
                SUPPRESS_RESULT_OUT_MSG);
        IntOption statusUpdateFreqOpt = new IntOption("statusUpdateFrequency", 'F',
                STATUS_UPDATE_FREQ_MSG, 1000, 0,
                Integer.MAX_VALUE);

        Option[] extraOptions = new Option[]{suppressStatusOutOpt, suppressResultOutOpt,
                statusUpdateFreqOpt};
        StringBuilder cliString = new StringBuilder();
        for (String arg : args) {
            cliString.append(" ").append(arg);
        }

        Task task;
        try {
            task = ClassOption.cliStringToObject(cliString.toString(), Task.class, extraOptions);
        } catch (Exception e) {
            throw new ExecutionPlanRuntimeException("Fail to initialize the task", e);
        }
        if (task instanceof StreamingClusteringTask) {
            StreamingClusteringTask clusteringTask = (StreamingClusteringTask) task;
            clusteringTask.setCepEvents(this.cepEvents);
            clusteringTask.setSamoaClusters(this.samoaClusters);
            clusteringTask.setNumberOfClusters(this.numberOfClusters);

        } else {
            throw new ExecutionPlanRuntimeException("Check the task: Not a StreamingClusteringTask");
        }

        task.setFactory(new SimpleComponentFactory());
        task.init();
        logger.info("Successfully initiated the streaming clustering task");
        topology = task.getTopology();
    }

}
