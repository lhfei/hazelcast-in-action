/*
 * Copyright 2010-2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package cn.lhfei.hazelcast.component.tradesource;

import java.io.Serializable;
import java.util.Objects;
import javax.annotation.Nonnull;

/**
 * @version 1.4.0
 *
 * @author Hefei Li
 *
 * @created Sep 22, 2022
 */
public class Trade implements Serializable {

  private static final long serialVersionUID = 8193596854524156791L;
  
  private final long time;
  private final String ticker;
  private final long quantity;
  private final long price;

  Trade(long time, @Nonnull String ticker, long quantity, long price) {
    this.time = time;
    this.ticker = Objects.requireNonNull(ticker);
    this.quantity = quantity;
    this.price = price;
  }

  /**
   * Event time of the trade.
   */
  public long getTime() {
    return time;
  }

  /**
   * Name of the instrument being traded.
   */
  @Nonnull
  public String getTicker() {
    return ticker;
  }

  /**
   * Quantity of the trade, the amount of the instrument that has been traded.
   */
  public long getQuantity() {
    return quantity;
  }

  /**
   * Price at which the transaction took place.
   */
  public long getPrice() {
    return price;
  }

  @Override
  public String toString() {
    return "Trade{time=" + time + ", ticker='" + ticker + '\'' + ", quantity=" + quantity
        + ", price=" + price + '}';
  }
}

