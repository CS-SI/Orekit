/* Copyright 2002-2023 CS GROUP
 * Licensed to CS GROUP (CS) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * CS licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.orekit.gnss.metric.ntrip;

import org.orekit.gnss.metric.messages.ParsedMessage;

import java.util.concurrent.Phaser;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;



public class CountingObserver implements MessageObserver {

    private Function<ParsedMessage, Boolean> filter;
    private AtomicInteger received = new AtomicInteger(0);
    private Phaser phaser = new Phaser(1);

    public CountingObserver(final Function<ParsedMessage, Boolean> filter) {
        this.filter = filter;
    }

    public void messageAvailable(String mountPoint, ParsedMessage message) {
        if (filter.apply(message)) {
            received.incrementAndGet();
            phaser.arrive();
        }
    }

    /**
     * Wait for a certain number of messages to be received.
     *
     * @param count   number of messages to wait for.
     * @param timeout when waiting in ms.
     * @throws InterruptedException if interrupted while waiting.
     * @throws TimeoutException     if timeout is reached while waiting.
     */
    public void awaitCount(int count, long timeout) throws InterruptedException, TimeoutException {
        final long start = System.currentTimeMillis();
        final long end = start + timeout;
        int phase = phaser.getPhase();
        while (received.get() < count && (timeout = end - System.currentTimeMillis()) > 0) {
            phase = phaser.awaitAdvanceInterruptibly(phase, timeout, TimeUnit.MILLISECONDS);
        }
    }

}

