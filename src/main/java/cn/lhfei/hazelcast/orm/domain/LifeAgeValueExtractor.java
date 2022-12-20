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

package cn.lhfei.hazelcast.orm.domain;

import java.time.temporal.ChronoUnit;
import com.hazelcast.query.extractor.ValueCollector;
import com.hazelcast.query.extractor.ValueExtractor;

/**
 * @version 1.4.0
 *
 * @author Hefei Li
 *
 * @created Sep 19, 2022
 */
public class LifeAgeValueExtractor implements ValueExtractor<LifeValue, Integer> {

  /**
   * <P>
   * Calculate age in years, an {@code int} not a {@code long}.
   * </P>
   *
   * @param lifeValue
   *            The original object
   * @param unused
   *            An arguments for the extractor
   * @param valueCollector
   *            To add the extracted value
   */
  @SuppressWarnings({ "rawtypes", "unchecked" })
  @Override
  public void extract(LifeValue lifeValue, Integer unused, ValueCollector valueCollector) {

      long age = ChronoUnit.YEARS.between(lifeValue.getDateOfBirth(), lifeValue.getDateOfDeath());

      valueCollector.addObject((int) age);
  }

}

