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

package cn.lhfei.hazelcast.web.api;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.google.gson.Gson;
import com.hazelcast.map.IMap;
import cn.lhfei.hazelcast.component.cdc.IMapService;
import cn.lhfei.hazelcast.orm.data.TestData;
import cn.lhfei.hazelcast.orm.domain.PersonValue;

/**
 * @version 1.4.0
 *
 * @author Hefei Li
 *
 * @created Sep 19, 2022
 */
@RestController
public class InvoiceResource extends AbstractResource {
  private static final Gson gson = new Gson();

  @GetMapping("/version")
  public String getVersion() {
    return "v1.4.0";
  }
  
  @GetMapping("/load")
  public Map<String, Object> load() {
    Map<String, Object> result = new HashMap<>();

    mapService.load();

    result.put("success", true);
    result.put("size", TestData.BIRTHS.length);

    return result;
  }


  @GetMapping("/list")
  public List<IMap<?, ?>> list() {

    return mapService.list();
  }


  @GetMapping("/howard/{schema}")
  public Collection<PersonValue> howard(@PathVariable("schema") String schema,
      @RequestParam("key") String key) {

    return mapService.query(schema, key);
  }
  
  @Autowired
  private IMapService mapService;
}
