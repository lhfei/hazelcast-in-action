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

import static com.hazelcast.function.Functions.wholeItem;
import static com.hazelcast.jet.Traversers.traverseArray;
import static com.hazelcast.jet.aggregate.AggregateOperations.counting;
import static java.util.Comparator.comparingLong;
import static java.util.stream.Collectors.toMap;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.jet.JetService;
import com.hazelcast.jet.Observable;
import com.hazelcast.jet.pipeline.Pipeline;
import com.hazelcast.jet.pipeline.Sinks;
import com.hazelcast.jet.pipeline.Sources;

/**
 * @version 1.4.0
 *
 * @author Hefei Li
 *
 * @created Sep 23, 2022
 */
@Component
public class FutureResults {

  private static final String BOOK_LINES = "bookLines";

  public void run() {
      JetService jet = hazelcastInstance.getJet();

      loadLines(jet);

      Observable<Map.Entry<String, Long>> observable = jet.newObservable();
      Pipeline p = buildPipeline(observable);
      observable.toFuture(s -> s.collect(toMap(Map.Entry::getKey, Map.Entry::getValue)))
        .thenAccept(map -> {
          this.printResults(map);
        })
//        .thenAccept(FutureResults::printResults)
        .thenAccept(v -> hazelcastInstance.shutdown());

      jet.newJob(p).join();
  }

  private void loadLines(JetService jet) {
    try {
      long[] lineNum = {0};
      Map<Long, String> bookLines = new HashMap<>();
      InputStream stream =
          FutureResults.class.getResourceAsStream("/data/books/shakespeare-complete-works.txt");
      try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
        reader.lines().forEach(line -> bookLines.put(++lineNum[0], line));
      }
      hazelcastInstance.getMap(BOOK_LINES).putAll(bookLines);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private Pipeline buildPipeline(Observable<Map.Entry<String, Long>> observable) {
    Pattern delimiter = Pattern.compile("\\W+");
    Pipeline p = Pipeline.create();
    p.readFrom(Sources.<Long, String>map(BOOK_LINES))
      .flatMap(e -> traverseArray(delimiter.split(e.getValue().toLowerCase())))
      .filter(word -> !word.isEmpty())
      .groupingKey(wholeItem())
      .aggregate(counting())
      .writeTo(Sinks.observable(observable));
    return p;
  }

  private void printResults(Map<String, Long> counts) {
    final int limit = 25;

    StringBuilder sb = new StringBuilder(String.format(" Top %d entries are:%n", limit));
    sb.append("+-------+---------+\n");
    sb.append("| Count | Word    |\n");
    sb.append("|-------+---------|\n");
    counts.entrySet().stream()
            .sorted(comparingLong(Map.Entry<String, Long>::getValue).reversed())
            .limit(limit)
            .forEach(e -> sb.append(String.format("|%6d | %-8s|%n", e.getValue(), e.getKey())));
    sb.append("+-------+---------+\n");

    System.out.println(sb.toString());
  }
  
  @Autowired
  private HazelcastInstance hazelcastInstance;

}
