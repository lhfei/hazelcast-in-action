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

package cn.lhfei.hazelcast.orm.data;

/**
 * @version 1.4.0
 *
 * @author Hefei Li
 *
 * @created Sep 19, 2022
 */
public class TestData {
  // People and dates of birth
  public static final Object[][] BIRTHS =
      new Object[][] {{"Curly", "Howard", "1903-10-22"}, {"Larry", "Fine", "1902-10-05"},
          {"Moe", "Howard", "1897-06-19"}, {"Shemp", "Howard", "1895-03-11"},
          {"Joe", "Besser", "1907-08-12"}, {"Joe", "DeRita", "1909-07-12"},};

  // Deaths for some of the above
  public static final Object[][] DEATHS = new Object[][] {{"Curly", "1952-01-18"},
      {"Larry", "1975-01-24"}, {"Moe", "1975-05-04"}, {"Shemp", "1955-11-22"},};
}
