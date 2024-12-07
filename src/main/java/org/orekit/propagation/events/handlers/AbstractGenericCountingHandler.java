/* Copyright 2022-2024 Romain Serra
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
package org.orekit.propagation.events.handlers;

import org.hipparchus.ode.events.Action;

/**
 * Abstract, Orekit-internal class for standard and fielded handlers counting event occurrences.
 * The {@link Action} can be modified according to the count.
 * @author Romain Serra
 * @since 13.0
 */
abstract class AbstractGenericCountingHandler {

    /** Action to return. */
    private Action action;

    /** Number of event occurrences. */
    private int count;

    /** Constructor.
     * @param startingCount value to initialize count
     * @param action Action to initialize attribute
     */
    protected AbstractGenericCountingHandler(final int startingCount, final Action action) {
        this.count = startingCount;
        this.action = action;
    }

    /**
     * Getter for count.
     * @return count
     */
    public int getCount() {
        return count;
    }

    /**
     * Protected getter for the action to return.
     * @return action
     */
    protected Action getAction() {
        return action;
    }

    /**
     * Protected setter for action.
     * @param action new action
     */
    protected void setAction(final Action action) {
        this.action = action;
    }

    /**
     * Reset count.
     */
    public void reset() {
        count = 0;
    }

    /**
     * Increment count.
     */
    protected void increment() {
        count++;
    }
}
