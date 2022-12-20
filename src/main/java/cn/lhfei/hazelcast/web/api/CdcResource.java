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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import cn.lhfei.hazelcast.component.cdc.CDCService;
import cn.lhfei.hazelcast.component.cdc.Cache;
import cn.lhfei.hazelcast.component.predictor.TrafficPredictor;
import cn.lhfei.hazelcast.component.window.session.SessionWindow;
import cn.lhfei.hazelcast.component.window.sliding.StockExchange;

/**
 * @version 1.4.0
 *
 * @author Hefei Li
 *
 * @created Sep 19, 2022
 */
@RestController
public class CdcResource {

  @GetMapping("/stream")
  public String consume(@RequestParam("topic") String topic) {
    cdcService.consumeMessage(topic);

    return "";
  }
  
  @GetMapping("/cancel")
  public void cancelWindow() {
    stockExchage.cancel();
  }
  
  @GetMapping("/tonN")
  public void tonN() {
    stockExchage.tonN();
  }
  
  @GetMapping("/window")
  public void slidingWindow() {
    stockExchage.startWindow();
  }
  
  @GetMapping("/mysql")
  public void cdc() {
    cache.run();
  }
  
  @GetMapping("/predictor")
  public void predict() {
    predictor.run("15-minute-counts-sorted.csv", "predictions");
  }
  
  
  
  
  
  @Autowired
  private CDCService cdcService;
  
  @Autowired
  private StockExchange stockExchage;
  
  @Autowired
  private SessionWindow sessionWindow;
  
  @Autowired
  private Cache cache;
  
  @Autowired
  private TrafficPredictor predictor;
}
