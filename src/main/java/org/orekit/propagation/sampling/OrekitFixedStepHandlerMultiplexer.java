/* Copyright 2002-2020 CS GROUP
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
package org.orekit.propagation.sampling;

import java.util.ArrayList;
import java.util.List;

import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;

/**
 * This class gathers several {@link OrekitFixedStepHandler} instances into one.
 * @author Bryan Cazabonne
 * @since 10.2
 */
public class OrekitFixedStepHandlerMultiplexer implements OrekitFixedStepHandler {

    /** Underlying step handlers. */
    private final List<OrekitFixedStepHandler> handlers;

    /**
     * Simple constructor.
     */
    public OrekitFixedStepHandlerMultiplexer() {
        handlers = new ArrayList<OrekitFixedStepHandler>();
    }

    /**
     * Add a step handler.
     * @param handler step handler to add
     */
    public void add(final OrekitFixedStepHandler handler) {
        handlers.add(handler);
    }

    /** {@inheritDoc} */
    @Override
    public void init(final SpacecraftState s0, final AbsoluteDate t, final double step) {
        for (final OrekitFixedStepHandler handler : handlers) {
            handler.init(s0, t, step);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void handleStep(final SpacecraftState currentState, final boolean isLast) {
        for (final OrekitFixedStepHandler handler : handlers) {
            handler.handleStep(currentState, isLast);
        }
    }

}
