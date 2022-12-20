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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.jet.JetService;
import com.hazelcast.jet.cdc.ChangeRecord;
import com.hazelcast.jet.cdc.mysql.MySqlCdcSources;
import com.hazelcast.jet.config.JobConfig;
import com.hazelcast.jet.pipeline.Pipeline;
import com.hazelcast.jet.pipeline.Sinks;
import com.hazelcast.jet.pipeline.StreamSource;

/**
 * @version 1.4.0
 *
 * @author Hefei Li
 *
 * @created Sep 23, 2022
 */
@Component
public class Cache {

  public void run() {
    StreamSource<ChangeRecord> source = MySqlCdcSources.mysql("223345")
      .setDatabaseAddress("10.170.232.199")
      .setDatabasePort(3306)
      .setDatabaseUser("root")
      .setDatabasePassword("Lhfeilaile@01")
      .setClusterName("dbserver1")
      .setDatabaseWhitelist("gt4_poc")
      .setTableWhitelist("student_score")
      .build();

    Pipeline pipeline = Pipeline.create();
    pipeline.readFrom(source)
      .withoutTimestamps()
      .peek()
      .writeTo(Sinks.logger());
//      .writeTo(CdcSinks.map("students",
//              r -> r.key().toMap().get("id"),
//              r -> r.value().toObject(Student.class).toString())
//      );

    JobConfig cfg = new JobConfig().setName("mysql-[28]-cdc");
    
    JetService jet = hazelcastInstance.getJet();
    
    jet.newJob(pipeline, cfg).join();
    
  }

  
  @Autowired
  private HazelcastInstance hazelcastInstance;

}
