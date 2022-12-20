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

package cn.lhfei.hazelcast.jet;

import java.time.LocalDate;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Map.Entry;
import com.hazelcast.function.Functions;
import com.hazelcast.jet.Util;
import com.hazelcast.jet.datamodel.Tuple3;
import com.hazelcast.jet.pipeline.BatchStage;
import com.hazelcast.jet.pipeline.JoinClause;
import com.hazelcast.jet.pipeline.Pipeline;
import com.hazelcast.jet.pipeline.Sinks;
import com.hazelcast.jet.pipeline.Sources;
import cn.lhfei.hazelcast.orm.domain.LifeValue;
import cn.lhfei.hazelcast.orm.domain.PersonKey;
import cn.lhfei.hazelcast.orm.domain.PersonValue;

/**
 * @version 1.4.0
 *
 * @author Hefei Li
 *
 * @created Sep 19, 2022
 */
public class MyJoinPipeline {

  public static Pipeline build() {
      Pipeline pipeline = Pipeline.create();
      // 1 - read a map
      BatchStage<Entry<String, LocalDate>> births = pipeline
              .readFrom(Sources.<PersonKey, PersonValue>map("person"))
              .map(entry -> Util.entry(entry.getKey().getFirstName(), entry.getValue().getDateOfBirth()));

      // 2 - read another map
      BatchStage<Entry<String, LocalDate>> deaths = pipeline.readFrom(Sources.map("deaths"));

      // 5 - join output from steps 2 and 4 (Tuple2 are map entries) on key
      BatchStage<Tuple3<String, LocalDate, LocalDate>> stage5 = deaths.hashJoin(births,
          JoinClause.joinMapEntries(Functions.entryKey()),
          (nameAndBirth, death) -> Tuple3.tuple3(nameAndBirth.getKey(), nameAndBirth.getValue(), death));

      // 6 - filter out unjoined
      BatchStage<Tuple3<String, LocalDate, LocalDate>> stage6 = stage5
              .filter(tuple2 -> tuple2.f1() != null);

      // 7 - create a map entry from step 6 output
      BatchStage<SimpleImmutableEntry<String, LifeValue>> stage7 = stage6.map(trio -> {
          // Tuple2<Tuple2< key, date-of-birth>, date-of-death>
          String key = trio.f0();
          LocalDate dob = trio.f1();
          LocalDate dod = trio.f2();

          LifeValue value = new LifeValue();
          value.setDateOfBirth(dob);
          value.setDateOfDeath(dod);

          // Create a Map.Entry
          return new SimpleImmutableEntry<>(key, value);
      });

      // 8 - save the map entry
      stage7.writeTo(Sinks.map("life"));

      // Return the query execution plan
      return pipeline;
  }

}
