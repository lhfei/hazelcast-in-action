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

package cn.lhfei.hazelcast.component.window.session;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.google.gson.Gson;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.jet.Job;
import com.hazelcast.jet.aggregate.AggregateOperation1;
import com.hazelcast.jet.aggregate.AggregateOperations;
import com.hazelcast.jet.core.ProcessorMetaSupplier;
import com.hazelcast.jet.datamodel.KeyedWindowResult;
import com.hazelcast.jet.datamodel.Tuple2;
import com.hazelcast.jet.pipeline.Pipeline;
import com.hazelcast.jet.pipeline.Sinks;
import com.hazelcast.jet.pipeline.Sources;
import com.hazelcast.jet.pipeline.StreamSource;
import com.hazelcast.jet.pipeline.WindowDefinition;

/**
 * @version 1.4.0
 *
 * @author Hefei Li
 *
 * @created Sep 22, 2022
 */
@Service
public class SessionWindowImpl implements SessionWindow {
  private static final Logger LOG = LoggerFactory.getLogger(SessionWindowImpl.class);
  private Gson gson = new Gson();

  private static final long JOB_DURATION_MS = 60_000;
  private static final int SESSION_TIMEOUT = 5_000;
  
  private ThreadLocal<Job> threadLocal;
  
  @Override
  public void cancel() {
    if(Optional.ofNullable(threadLocal).isPresent()) {
      try {
        threadLocal.get().cancel();
      } catch (Exception e) {
        LOG.info("Error: {}", e.getMessage(), e);
      }
    }
  }

  @Override
  public void startWindow() {
    // The composite aggregate operation computes two metrics for each user session:
    // 1. How many times the user opened a product page
    // 2. How many items the user purchased
    AggregateOperation1<ProductEvent, ?, Tuple2<Long, Set<String>>> aggrOp = AggregateOperations.allOf(
        AggregateOperations.summingLong(e -> e.getProductEventType() == ProductEventType.VIEW_LISTING ? 1 : 0),
        AggregateOperations.mapping(e -> e.getProductEventType() == ProductEventType.PURCHASE ? e.getProductId() : null, AggregateOperations.toSet())
    );

    Pipeline p = Pipeline.create();
    p.readFrom(eventsSource())
     .withTimestamps(ProductEvent::getTimestamp, 0)
     .groupingKey(ProductEvent::getUserId)
     .window(WindowDefinition.session(SESSION_TIMEOUT))
     .aggregate(aggrOp)
     .writeTo(Sinks.logger(SessionWindowImpl::sessionToString));
    
    try {
      Job job = hazelcastInstance.getJet().newJob(p);
      
      threadLocal = new ThreadLocal() {};
      threadLocal.set(job);
      
      TimeUnit.SECONDS.sleep(JOB_DURATION_MS);
      job.join();
    } catch (InterruptedException e) {
      LOG.info("Error: {}", e.getMessage(), e);
    }
  }

  private static StreamSource<ProductEvent> eventsSource() {
      return Sources.streamFromProcessor("generator", ProcessorMetaSupplier.preferLocalParallelismOne(GenerateEventsP::new));
  }

  @Nonnull
  private static String sessionToString(KeyedWindowResult<String, Tuple2<Long, Set<String>>> wr) {
    return String.format(
        "Session{userId=%s, start=%s, duration=%2ds, value={viewed=%2d, purchases=%s}", wr.key(), // userId
        Instant.ofEpochMilli(wr.start()).atZone(ZoneId.systemDefault()).toLocalTime(), // session
                                                                                       // start
        Duration.ofMillis(wr.end() - wr.start()).getSeconds(), // session duration
        wr.result().f0(), // number of viewed listings
        wr.result().f1()); // set of purchased products
  }
  
  @Autowired
  private HazelcastInstance hazelcastInstance;
}
