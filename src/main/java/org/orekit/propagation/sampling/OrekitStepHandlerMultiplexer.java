/* Copyright 2002-2016 CS Systèmes d'Information
 * Licensed to CS Systèmes d'Information (CS) under one or more
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

import org.orekit.errors.OrekitException;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;

/** This class gathers several {@link OrekitStepHandler} instances into one.
 *
 * @author Luc Maisonobe
 */
public class OrekitStepHandlerMultiplexer implements OrekitStepHandler {

    /** Underlying step handlers. */
    private final List<OrekitStepHandler> handlers;

    /** Simple constructor.
     */
    public OrekitStepHandlerMultiplexer() {
        handlers = new ArrayList<OrekitStepHandler>();
    }

    /** Add a step handler.
     * @param handler step handler to add
     */
    public void add(final OrekitStepHandler handler) {
        handlers.add(handler);
    }

    /** {@inheritDoc} */
    public void init(final SpacecraftState s0, final AbsoluteDate t)
        throws OrekitException {
        for (final OrekitStepHandler handler : handlers) {
            handler.init(s0, t);
        }
    }

    /** {@inheritDoc} */
    public void handleStep(final OrekitStepInterpolator interpolator, final boolean isLast)
        throws OrekitException {
        for (final OrekitStepHandler handler : handlers) {
            handler.handleStep(interpolator, isLast);
        }
    }

}
