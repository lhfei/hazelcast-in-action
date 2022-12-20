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

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import com.hazelcast.query.Predicate;
import com.hazelcast.query.impl.predicates.SqlPredicate;
import cn.lhfei.hazelcast.orm.data.TestData;
import cn.lhfei.hazelcast.orm.domain.LifeValue;
import cn.lhfei.hazelcast.orm.domain.PersonKey;
import cn.lhfei.hazelcast.orm.domain.PersonValue;
import jline.internal.Log;

/**
 * @version 1.4.0
 *
 * @author Hefei Li
 *
 * @created Sep 19, 2022
 */
@Service("mapService")
public class MapServiceImpl implements IMapService {

  @Override
  public void load() {
    IMap<PersonKey, PersonValue> personMap = this.hazelcastInstance.getMap("person");
    IMap<String, LocalDate> deathsMap = this.hazelcastInstance.getMap("deaths");
    Map<String, Object> result = new HashMap<>();

    Arrays.stream(TestData.BIRTHS).forEach((Object[] datum) -> {
        PersonKey personKey = new PersonKey();
        personKey.setFirstName(datum[0].toString());
        personKey.setLastName(datum[1].toString());

        PersonValue personValue = new PersonValue();
        personValue.setDateOfBirth(LocalDate.parse(datum[2].toString()));

        personMap.put(personKey, personValue);
    });

    Arrays.stream(TestData.DEATHS).forEach((Object[] datum) -> {
        String firstName = datum[0].toString();
        LocalDate dateOfDeath = LocalDate.parse(datum[1].toString());

        deathsMap.put(firstName, dateOfDeath);
    });
  }

  @Override
  public List<IMap<?, ?>> list() {
    IMap<PersonKey, PersonValue> personMap = this.hazelcastInstance.getMap("person");
    IMap<String, LifeValue> lifeMap = this.hazelcastInstance.getMap("life");
    
    List<IMap<?, ?>> result = new ArrayList<>();

    if (personMap.isEmpty()) {
        System.out.println("Map 'person' is empty, run 'load' first");
    }
    if (lifeMap.isEmpty()) {
        System.out.println("Map 'life' is empty, run 'join' first");
    }

    String[] mapNames = {"person", "deaths", "life"};

    for (String mapName : mapNames) {
        IMap<?, ?> map = this.hazelcastInstance.getMap(mapName);
        result.add(map);
    }
    
    return result;
  }

  @Override
  public Collection<PersonValue> query(String schema, String feed) {
    IMap<PersonKey, PersonValue> personMap = this.hazelcastInstance.getMap(schema);

    Predicate<PersonKey, PersonValue> predicate =
        new SqlPredicate("__key.lastName = '" + feed + "' or __key.firstName = '" + feed + "'");

    Log.info("{}", predicate.toString());
    return personMap.values(predicate);
  }

  @Autowired
  private HazelcastInstance hazelcastInstance;
}
