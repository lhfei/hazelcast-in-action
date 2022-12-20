/*
 * Copyright 2010-2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package cn.lhfei.hazelcast.component.window.sliding;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.google.gson.Gson;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.function.ComparatorEx;
import com.hazelcast.jet.Job;
import com.hazelcast.jet.aggregate.AggregateOperation1;
import com.hazelcast.jet.aggregate.AggregateOperations;
import com.hazelcast.jet.datamodel.KeyedWindowResult;
import com.hazelcast.jet.pipeline.Pipeline;
import com.hazelcast.jet.pipeline.Sinks;
import com.hazelcast.jet.pipeline.WindowDefinition;
import cn.lhfei.hazelcast.component.tradesource.Trade;
import cn.lhfei.hazelcast.component.tradesource.TradeSource;

/**
 * @version 1.4.0
 *
 * @author Hefei Li
 *
 * @created Sep 22, 2022
 */
@Service
public class StockExchangeImpl implements StockExchange {
  private static final Logger LOG = LoggerFactory.getLogger(StockExchangeImpl.class);
  private Gson gson = new Gson();
  
  private static final int SLIDING_WINDOW_LENGTH_MILLIS = 3_000;
  private static final int SLIDE_STEP_MILLIS = 500;
  private static final int TRADES_PER_SEC = 3_000;
  private static final int NUMBER_OF_TICKERS = 10;
  private static final int JOB_DURATION = 15;

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
    Pipeline p = Pipeline.create();

    p.readFrom(TradeSource.tradeStream(NUMBER_OF_TICKERS, TRADES_PER_SEC))
        .withNativeTimestamps(3000)
        .groupingKey(Trade::getTicker)
        .window(WindowDefinition.sliding(SLIDING_WINDOW_LENGTH_MILLIS, SLIDE_STEP_MILLIS))
        .aggregate(AggregateOperations.counting())
        .writeTo(Sinks.logger(
            wr -> String.format("%s %5s %4d", toLocalTime(wr.end()), wr.key(), wr.result())));

    try {
      Job job = hazelcastInstance.getJet().newJob(p);
      
      threadLocal = new ThreadLocal() {};
      threadLocal.set(job);
      
      TimeUnit.SECONDS.sleep(JOB_DURATION);
      job.join();
    } catch (InterruptedException e) {
      LOG.info("Error: {}", e.getMessage(), e);
    }
    
  }
  
  @Override
  public void tonN() {
    Pipeline p = Pipeline.create();

    ComparatorEx<KeyedWindowResult<String, Double>> comparingValue =
            ComparatorEx.comparing(KeyedWindowResult<String, Double>::result);
    // Apply two functions in a single step: top-n largest and top-n smallest values
    AggregateOperation1<KeyedWindowResult<String, Double>, ?, TopNResult> aggrOpTopN = AggregateOperations.allOf(
        AggregateOperations.topN(5, comparingValue),
        AggregateOperations.topN(5, comparingValue.reversed()),
            TopNResult::new);

    p.readFrom(TradeSource.tradeStream(500, 6_000))
     .withNativeTimestamps(1_000)
     .groupingKey(Trade::getTicker)
     .window(WindowDefinition.sliding(10_000, 1_000))
     // aggregate to create trend for each ticker
     .aggregate(AggregateOperations.linearTrend(Trade::getTime, Trade::getPrice))
     .window(WindowDefinition.tumbling(1_000))
     // 2nd aggregation: choose top-N trends from previous aggregation
     .aggregate(aggrOpTopN)
     .writeTo(Sinks.logger(wr -> String.format("%nAt %s...%n%s", toLocalTime(wr.end()), wr.result())));
  }

  private static LocalTime toLocalTime(long timestamp) {
    return Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalTime();
  }

  @Autowired
  private HazelcastInstance hazelcastInstance;
  
}


final class TopNResult {
  final List<KeyedWindowResult<String, Double>> topIncrease;
  final List<KeyedWindowResult<String, Double>> topDecrease;

  TopNResult(List<KeyedWindowResult<String, Double>> topIncrease,
      List<KeyedWindowResult<String, Double>> topDecrease) {
    this.topIncrease = topIncrease;
    this.topDecrease = topDecrease;
  }

  @Override
  public String toString() {
    return String.format("Top rising stocks:%n%s\nTop falling stocks:%n%s",
        topIncrease.stream()
            .map(kwr -> String.format("   %s by %.2f%%", kwr.key(), 100d * kwr.result()))
            .collect(Collectors.joining("\n")),
        topDecrease.stream()
            .map(kwr -> String.format("   %s by %.2f%%", kwr.key(), 100d * kwr.result()))
            .collect(Collectors.joining("\n")));
  }
}
