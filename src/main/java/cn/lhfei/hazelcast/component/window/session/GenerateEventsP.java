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

import static com.hazelcast.jet.Traversers.traverseStream;
import static java.lang.Math.max;
import java.util.Arrays;
import java.util.Random;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import com.hazelcast.jet.Traverser;
import com.hazelcast.jet.core.AbstractProcessor;

/**
 * @version 1.4.0
 *
 * @author Hefei Li
 *
 * @created Sep 22, 2022
 */
public class GenerateEventsP extends AbstractProcessor {
  private final Random random = new Random();
  private UserTracker[] userTrackers = new UserTracker[5];
  private Traverser<ProductEvent> traverser;

  @Override
  public boolean isCooperative() {
    // we are doing a blocking sleep so we aren't cooperative
    return false;
  }

  @Override
  protected void init(@Nonnull Context context) {
    Arrays.setAll(userTrackers, i -> randomTracker());
  }

  @Override
  public boolean complete() {
    initTraverserIfNeeded();
    emitFromTraverser(traverser);
    return false;
  }

  private void initTraverserIfNeeded() {
    if (traverser != null) {
      return;
    }

    // Generate one event for each user in userTrackers
    Stream<ProductEvent> productEventStream = IntStream.range(0, userTrackers.length)
       // randomly skip some events
       .filter(i -> random.nextInt(3) != 0)
       .mapToObj(idx -> {
           UserTracker track = userTrackers[idx];
           ProductEvent event;
           if (track.remainingListings > 0) {
               track.remainingListings--;
               event = randomEvent(track.userId, ProductEventType.VIEW_LISTING);
           } else {
               track.remainingPurchases--;
               event = randomEvent(track.userId, ProductEventType.PURCHASE);
           }
           if (track.remainingListings == 0 && track.remainingPurchases == 0) {
               // we are done with this userTracker, generate a new one
               userTrackers[idx] = randomTracker();
           }
           return event;
       });

    traverser = traverseStream(productEventStream)
            .onFirstNull(() -> traverser = null);

    try {
        Thread.sleep(100);
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
    }
  }

  private ProductEvent randomEvent(String userId, ProductEventType viewListing) {
    return new ProductEvent(System.currentTimeMillis(), userId, "product" + random.nextInt(20),
        viewListing);
  }

  private UserTracker randomTracker() {
    return new UserTracker(String.format("user%03d", random.nextInt(100)), random.nextInt(20),
        max(0, random.nextInt(20) - 16));
  }

  private static final class UserTracker {
    final String userId;
    int remainingListings;
    int remainingPurchases;

    private UserTracker(String userId, int numListings, int numPurchases) {
      this.userId = userId;
      this.remainingListings = numListings;
      this.remainingPurchases = numPurchases;
    }
  }
}
