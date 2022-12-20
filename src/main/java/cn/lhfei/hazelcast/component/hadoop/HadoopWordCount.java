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

package cn.lhfei.hazelcast.component.hadoop;

import static com.hazelcast.function.Functions.wholeItem;
import static com.hazelcast.jet.Traversers.traverseArray;
import static com.hazelcast.jet.aggregate.AggregateOperations.counting;
import static java.lang.System.nanoTime;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import java.util.regex.Pattern;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.TextInputFormat;
import org.apache.hadoop.mapred.TextOutputFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.jet.JetService;
import com.hazelcast.jet.hadoop.HadoopSinks;
import com.hazelcast.jet.hadoop.HadoopSources;
import com.hazelcast.jet.pipeline.Pipeline;


/**
 * @version 1.4.0
 *
 * @author Hefei Li
 *
 * @created Sep 23, 2022
 */
@Component
public class HadoopWordCount {
  private static final Logger LOG = LoggerFactory.getLogger(HadoopWordCount.class);
  private static final String OUTPUT_PATH = "hadoop-word-count";

  public void run() throws Exception {
    Path inputPath = new Path(HadoopWordCount.class.getResource("/data/books").getPath());
    Path outputPath = new Path(OUTPUT_PATH);
    // delete the output directory, if already exists
    FileSystem.get(new Configuration()).delete(outputPath, true);
    executeSample(new JobConf(), inputPath, outputPath);
  }

  public void executeSample(JobConf jobConf, Path inputPath, Path outputPath) {
    try {
      JetService jet = hazelcastInstance.getJet();
      LOG.info("Counting words from {}", inputPath);
      long start = nanoTime();
      Pipeline p = buildPipeline(jobConf, inputPath, outputPath);
      jet.newJob(p).join();
      LOG.info("Done in {}  milliseconds.", NANOSECONDS.toMillis(nanoTime() - start));
      LOG.info("Output written to {}", outputPath);
    } finally {
      hazelcastInstance.shutdown();
    }
  }

  private Pipeline buildPipeline(JobConf jobConf, Path inputPath, Path outputPath) {
    jobConf.setInputFormat(TextInputFormat.class);
    jobConf.setOutputFormat(TextOutputFormat.class);
    TextOutputFormat.setOutputPath(jobConf, outputPath);
    TextInputFormat.addInputPath(jobConf, inputPath);

    final Pattern regex = Pattern.compile("\\W+");
    Pipeline p = Pipeline.create();
    p.readFrom(HadoopSources.inputFormat(jobConf, (k, v) -> v.toString()))
     .flatMap(line -> traverseArray(regex.split(line.toLowerCase())).filter(w -> !w.isEmpty()))
     .groupingKey(wholeItem())
     .aggregate(counting())
     .writeTo(HadoopSinks.outputFormat(jobConf));
    return p;
  }

  @Autowired
  private HazelcastInstance hazelcastInstance;
}

