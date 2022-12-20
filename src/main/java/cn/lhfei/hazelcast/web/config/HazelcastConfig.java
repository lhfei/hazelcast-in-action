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

package cn.lhfei.hazelcast.web.config;

import java.util.Properties;
import org.apache.kafka.connect.json.JsonDeserializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.hazelcast.config.ClasspathYamlConfig;
import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;

/**
 * @version 1.4.0
 *
 * @author Hefei Li
 *
 * @created Sep 19, 2022
 */
@Configuration
public class HazelcastConfig {
  /**
   * <P>
   * Create a Jet instance, which in turn will create a Hazelcast IMDG instance.
   * Use the supplied IMDG configuration but let the Jet configuration be the
   * default.
   * </P>
   *
   * @return Hazelcast Jet instance
   */
  @Bean
  public HazelcastInstance jetInstance() {
//      Config imdgConfig = new ClasspathXmlConfig("hazelcast.xml");
      Config imdgConfig = new ClasspathYamlConfig("hazelcast.yaml");
      HazelcastInstance hazelcast = Hazelcast.newHazelcastInstance(imdgConfig);
      return hazelcast;
  }
  
  @Bean(name = "kafkaProps")
  public Properties initKafkaProps() {
    Properties props = new Properties();
    props.setProperty("group.id", kafkaConfig.getGroupId());
    props.setProperty("bootstrap.servers", kafkaConfig.getBootstrapServers());
    props.setProperty("key.deserializer", JsonDeserializer.class.getCanonicalName());
    props.setProperty("value.deserializer", JsonDeserializer.class.getCanonicalName());
    props.setProperty("auto.offset.reset", kafkaConfig.getAutoOffsetReset());
    
    return props;
  }
  
  
  @Autowired
  private KafkaConfig kafkaConfig;
}
