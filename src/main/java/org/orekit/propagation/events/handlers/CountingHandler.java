/* Copyright 2022-2025 Romain Serra
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
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.EventDetector;

/**
 * Abstract class for handlers counting event occurrences. The {@link Action} can be modified according to the count.
 * @author Romain Serra
 * @since 13.0
 */
public abstract class CountingHandler extends AbstractGenericCountingHandler implements EventHandler {

    /**
     * Constructor.
     *
     * @param startingCount value to initialize count
     * @param action        Action to initialize attribute
     */
    protected CountingHandler(final int startingCount, final Action action) {
        super(startingCount, action);
    }

    /** {@inheritDoc} */
    @Override
    public Action eventOccurred(final SpacecraftState s, final EventDetector detector, final boolean increasing) {
        if (doesCount(s, detector, increasing)) {
            increment();
        }
        return getAction();
    }

    /**
     * Method returning true if and only if the count needs to be incremented.
     * @param state state at detection
     * @param detector detector
     * @param increasing flag on direction of event function
     * @return flag on counting
     */
    protected abstract boolean doesCount(SpacecraftState state, EventDetector detector, boolean increasing);
}
