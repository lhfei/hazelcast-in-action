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

import java.io.Serializable;
import com.hazelcast.partition.PartitionAware;

/**
 * @version 1.4.0
 *
 * @author Hefei Li
 *
 * @created Sep 19, 2022
 */
@SuppressWarnings("serial")
public class PersonKey implements PartitionAware<Byte>, Serializable {

  private String firstName;
  private String lastName;

  /**
   * <p>
   * Routing of keys to partitions is based on the first letter of the last name.
   * Assumes ASCII names.
   * </P>
   *
   * @return 'A' for 'Apple', etc.
   */
  @Override
  public Byte getPartitionKey() {
      return (byte) this.lastName.charAt(0);
  }

  public String getFirstName() {
      return firstName;
  }

  public void setFirstName(String firstName) {
      this.firstName = firstName;
  }

  public String getLastName() {
      return lastName;
  }

  public void setLastName(String lastName) {
      this.lastName = lastName;
  }
}
