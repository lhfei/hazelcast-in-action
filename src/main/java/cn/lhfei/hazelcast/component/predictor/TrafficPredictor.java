/*
 * Copyright 2010-2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.lhfei.hazelcast.component.predictor;

import static com.hazelcast.jet.Util.entry;
import static com.hazelcast.jet.aggregate.AggregateOperations.linearTrend;
import static com.hazelcast.jet.impl.util.Util.toLocalDateTime;
import static com.hazelcast.jet.pipeline.WindowDefinition.sliding;
import static java.util.concurrent.TimeUnit.DAYS;
import static java.util.concurrent.TimeUnit.MINUTES;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.jet.JetService;
import com.hazelcast.jet.datamodel.KeyedWindowResult;
import com.hazelcast.jet.pipeline.Pipeline;
import com.hazelcast.jet.pipeline.ServiceFactories;
import com.hazelcast.jet.pipeline.Sinks;
import com.hazelcast.jet.pipeline.Sources;
import com.hazelcast.jet.pipeline.StreamStage;
import com.hazelcast.jet.pipeline.file.CsvFileFormat;
import com.hazelcast.jet.pipeline.file.FileSources;

/**
 * 
 * This application reads the traffic data from file. Each input record contains timestamp, location and
 * respective car count. After ingestion, the records are routed to two separate computations.
 * First computation uses the car count to train the model. Jet collects data from last two hours
 * and computes the trend using the linear regression algorithm. This trend is updated every 15 minutes
 * and stored into an IMap. Sliding Windows are used to select two hour blocks from the time series.
 * Second computation combines current car count with the trend from previous week to predict car count
 * in next two hours. Prediction results are stored in a file in predictions/ directory.
 *
 * The training data is obtained from:
 * https://catalog.data.gov/dataset/nys-thruway-origin-and-destination-points-for-all-vehicles-15-minute-intervals-2016-q1
 * <p>
 * We've grouped the data by date and entry location and sorted them.
 *
 * The DAG used to model traffic predictor can be seen below :
 *
 *                           ┌────────────────────────┐
 *                           │Read historic car counts│
 *                           │       from file        │
 *                           └──┬──────────────┬──────┘
 *                              │              │
 *                              │              └─────────┐
 *                              v                        │
 *                      ┌──────────────┐                 │
 *                      │Add timestamps│                 │
 *                      └───────┬──────┘                 │
 *                              │                        │
 *                              v                        │
 *                     ┌─────────────────┐               │
 *                     │Group by location│               │
 *                     └─────────┬───────┘               │
 *                               │                       │
 *                               v                       │
 *                  ┌─────────────────────────┐          │
 *                  │Calculate linear trend of│          │
 *                  │ counts in 2hour windows │          │
 *                  │     slides by 15mins    │          │
 *                  └────────────┬────────────┘          │
 *                               │                       │
 *                 ┌─────────────┘                       │
 *                 │                                     │
 *                 v                                     v
 *   ┌──────────────────────────┐ ┌────────────────────────────────────────────┐
 *   │Format linear trend output│ │Make predictions based on the historic trend│
 *   └────────────────┬─────────┘ └──────────────────────────┬─────────────────┘
 *                    │                                      │
 *                    v                                      v
 *   ┌────────────────────────────────┐ ┌────────────────────────────────────────┐
 *   │Write results to an IMap(trends)│ │Write results to a file(targetDirectory)│
 *   └────────────────────────────────┘ └────────────────────────────────────────┘
 *
 * @version 1.4.0
 *
 * @author Hefei Li
 *
 * @created Sep 27, 2022
 */
@Component
public class TrafficPredictor {
  private static final Logger LOG = LoggerFactory.getLogger(TrafficPredictor.class);
  private static final long GRANULARITY_STEP_MS = MINUTES.toMillis(15);
  private static final int NUM_PREDICTIONS = 8;
  static final String DATA_PATH = "src/main/resources/data/";

  static {
    System.setProperty("hazelcast.multicast.group", "224.18.19.21");
  }

  public void run(String source, String destination) {
    Path sourceFile = Paths.get(DATA_PATH + source);
    final String targetDirectory = DATA_PATH + destination;
    if (!Files.isReadable(sourceFile)) {
      LOG.info("Source file does not exist or is not readable (" + sourceFile + ")");
      return;
    }

    JetService jet = hazelcastInstance.getJet();

    Pipeline pipeline = buildPipeline(sourceFile, targetDirectory);
    try {
      jet.newJob(pipeline).join();
    } finally {
      hazelcastInstance.shutdown();
    }
  }
  /**
   * Builds and returns the Pipeline which represents the actual computation.
   */
  private static Pipeline buildPipeline(Path sourceFile, String targetDirectory) {
    Pipeline pipeline = Pipeline.create();

    // Calculate car counts from the file.
    StreamStage<CarCount> carCounts =
        pipeline.readFrom(Sources.filesBuilder(sourceFile.getParent().toString())
            .glob(sourceFile.getFileName().toString())
                .build((filename, line) -> {
                    String[] split = line.split(",");
                    long time = LocalDateTime.parse(split[0])
                                             .atZone(ZoneId.systemDefault())
                                             .toInstant()
                                             .toEpochMilli();
                    return new CarCount(split[1], time, Integer.parseInt(split[2]));
                }
            )
    ).addTimestamps(CarCount::getTime, MINUTES.toMillis(300));
    
    // Calculate linear trends of car counts and writes them into an IMap
    // in 2 hour windows sliding by 15 minutes.
    carCounts
        .groupingKey(CarCount::getLocation)
        .window(sliding(MINUTES.toMillis(120), MINUTES.toMillis(15)))
        .aggregate(linearTrend(CarCount::getTime, CarCount::getCount))
        .map((KeyedWindowResult<String, Double> e) ->
                entry(new TrendKey(e.getKey(), e.end()), e.getValue()))
        .writeTo(Sinks.map("trends"));

    // Makes predictions using the trends calculated above from an IMap and writes them to a file
    carCounts
        .mapUsingService(ServiceFactories.<TrendKey, Double>iMapService("trends"),
            (trendMap, cc) -> {
                int[] counts = new int[NUM_PREDICTIONS];
                double trend = 0.0;
                for (int i = 0; i < NUM_PREDICTIONS; i++) {
                    Double newTrend = trendMap.get(new TrendKey(cc.location, cc.time - DAYS.toMillis(7)));
                    if (newTrend != null) {
                        trend = newTrend;
                    }
                    double prediction = cc.count + i * GRANULARITY_STEP_MS * trend;
                    counts[i] = (int) Math.round(prediction);
                }
                return new Prediction(cc.location, cc.time + GRANULARITY_STEP_MS, counts);
            })
        .writeTo(Sinks.files(targetDirectory));
    return pipeline;
  }

  /**
   * Composite key object of time and location which used on IMap
   */
  private static class TrendKey implements Serializable {
    private final String location;
    private final long time;

    private TrendKey(String location, long time) {
      this.location = location;
      this.time = time;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      TrendKey trendKey = (TrendKey) o;
      return time == trendKey.time && Objects.equals(location, trendKey.location);
    }

    @Override
    public int hashCode() {
      return Objects.hash(location, time);
    }

    @Override
    public String toString() {
      return "TrendKey{" +
          "location='" + location + '\'' +
          ", time=" + toLocalDateTime(time) +
          '}';
    }
  }

  /**
   * DTO for predictions
   */
  private static class Prediction implements Serializable {
    private final String location;
    // time of the first prediction
    private final long time;
    // predictions, one for each minute
    private final int[] predictedCounts;

    private Prediction(String location, long time, int[] predictedCounts) {
      this.location = location;
      this.time = time;
      this.predictedCounts = predictedCounts;
    }

    public String getLocation() {
      return location;
    }

    public long getTime() {
      return time;
    }

    public int[] getPredictedCounts() {
      return predictedCounts;
    }

    @Override
    public String toString() {
      return "Prediction{" +
          "location='" + location + '\'' +
          ", time=" + toLocalDateTime(time) + " (" + time + ")" +
          ", predictedCounts=" + Arrays.toString(predictedCounts) +
          '}';
    }
  }

  /**
   * DTO for car counts of a location on a specific time
   */
  private static class CarCount {
    private final String location;
    private final long time;
    private final int count;

    private CarCount(String location, long time, int count) {
      this.location = location;
      this.time = time;
      this.count = count;
    }

    public String getLocation() {
      return location;
    }

    public long getTime() {
      return time;
    }

    public int getCount() {
      return count;
    }

    @Override
    public String toString() {
      return "CarCount{" + "location='" + location + '\'' + ", time=" + toLocalDateTime(time)
          + ", count=" + count + '}';
    }
  }
  
  @Autowired
  private HazelcastInstance hazelcastInstance;
}
