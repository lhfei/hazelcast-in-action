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

package cn.lhfei.hazelcast.component.cdc;

import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.google.gson.Gson;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.jet.Util;
import com.hazelcast.jet.kafka.KafkaSources;
import com.hazelcast.jet.pipeline.Pipeline;
import com.hazelcast.jet.pipeline.Sinks;

/**
 * @version 1.4.0
 *
 * @author Hefei Li
 *
 * @created Sep 20, 2022
 */
@Service("cdcService")
public class CDCServiceImpl implements CDCService {
  private static final Logger LOG = LoggerFactory.getLogger(CDCServiceImpl.class);
  private Gson gson = new Gson();
  @Override
  public void consumeMessage(String topic) {
    Pipeline p = Pipeline.create();
//    p.readFrom(KafkaSources.kafka(props, record -> {
//      LOG.info("Message Body: [{}]", gson.toJson(record));
//        HazelcastJsonValue key = new HazelcastJsonValue(record.key().toString());
//        HazelcastJsonValue value = new HazelcastJsonValue(record.value().toString());
//        return Util.entry(key, value);
//    }, topic))
//     .withoutTimestamps()
//     .peek()
//     .writeTo(Sinks.logger());

    p.readFrom(KafkaSources.kafka(kafkaProps, topic))
      .withNativeTimestamps(0)
      .peek()
      .map(e -> Util.entry(e.getKey(), e.getValue()))
//      .writeTo(Sinks.logger());
      .writeTo(Sinks.logger(wr -> {
        LOG.info("Record: key[{}] --  Value: [{}]", wr.getKey(), wr.getValue());
        return String.format("%nAt %s...%n%s", wr.getKey(), wr.getValue());}
      ));

    hazelcastInstance.getJet().newJob(p).join();
  }

  @Autowired
  private Properties kafkaProps;
  
  @Autowired
  private HazelcastInstance hazelcastInstance;
}
