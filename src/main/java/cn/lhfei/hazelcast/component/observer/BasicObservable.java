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

package cn.lhfei.hazelcast.component.observer;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.function.ComparatorEx;
import com.hazelcast.jet.JetService;
import com.hazelcast.jet.Observable;
import com.hazelcast.jet.aggregate.AggregateOperations;
import com.hazelcast.jet.datamodel.WindowResult;
import com.hazelcast.jet.function.Observer;
import com.hazelcast.jet.pipeline.Pipeline;
import com.hazelcast.jet.pipeline.Sinks;
import com.hazelcast.jet.pipeline.WindowDefinition;
import com.hazelcast.jet.pipeline.test.TestSources;

/**
 * @version 1.4.0
 *
 * @author Hefei Li
 *
 * @created Sep 22, 2022
 */
@Component
public class BasicObservable {

  public static final int TOP = 3;

  public void run() {
    JetService jet = hazelcastInstance.getJet();
    Observable<List<Long>> observable = jet.newObservable();
    observable.addObserver(Observer.of(BasicObservable::printResults));

    Pipeline p = Pipeline.create();
    p.readFrom(TestSources.itemStream(100, (ts, seq) -> ThreadLocalRandom.current().nextLong()))
        .withIngestionTimestamps()
        .window(WindowDefinition.tumbling(1000))
        .aggregate(AggregateOperations.topN(TOP, ComparatorEx.comparingLong(l -> l)))
        .map(WindowResult::result)
        .writeTo(Sinks.observable(observable));

    jet.newJob(p).join();
  }

  private static void printResults(List<Long> topNumbers) {
    StringBuilder sb =
        new StringBuilder(String.format("====> \nTop %d random numbers in the latest window: ", TOP));
    for (int i = 0; i < topNumbers.size(); i++) {
      sb.append(String.format("\n\t%d. %,d", i + 1, topNumbers.get(i)));
    }
    System.out.println(sb.toString());
  }

  @Autowired
  private HazelcastInstance hazelcastInstance;
}

