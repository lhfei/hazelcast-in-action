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

package cn.lhfei.hazelcast.component.window.session;

import java.io.Serializable;

/**
 * @version 1.4.0
 *
 * @author Hefei Li
 *
 * @created Sep 22, 2022
 */
public class ProductEvent implements Serializable {
  private static final long serialVersionUID = 4203196325741340836L;
  private final long timestamp;
  private final String userId;
  private final String productId;
  private final ProductEventType productEventType;

  public ProductEvent(long timestamp, String userId, String productId,
      ProductEventType productEventType) {
    this.timestamp = timestamp;
    this.userId = userId;
    this.productId = productId;
    this.productEventType = productEventType;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public String getUserId() {
    return userId;
  }

  public String getProductId() {
    return productId;
  }

  public ProductEventType getProductEventType() {
    return productEventType;
  }

  @Override
  public String toString() {
    return "ProductEvent{timestamp=" + timestamp + ", userId='" + userId + '\'' + ", productId='"
        + productId + '\'' + ", productEventType=" + productEventType + '}';
  }
}
