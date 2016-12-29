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

import org.hipparchus.RealFieldElement;
import org.orekit.errors.OrekitException;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.time.FieldAbsoluteDate;

/** This class gathers several {@link OrekitStepHandler} instances into one.
 *
 * @author Luc Maisonobe
 */
public class FieldOrekitStepHandlerMultiplexer<T extends RealFieldElement<T>> implements FieldOrekitStepHandler<T> {

    /** Underlying step handlers. */
    private final List<FieldOrekitStepHandler<T>> handlers;

    /** Simple constructor.
     */
    public FieldOrekitStepHandlerMultiplexer() {
        handlers = new ArrayList<FieldOrekitStepHandler<T>>();
    }

    /** Add a step handler.
     * @param handler step handler to add
     */
    public void add(final FieldOrekitStepHandler<T> handler) {
        handlers.add(handler);
    }

    /** {@inheritDoc} */
    public void init(final FieldSpacecraftState<T> s0, final FieldAbsoluteDate<T> t)
        throws OrekitException {
        for (final FieldOrekitStepHandler<T> handler : handlers) {
            handler.init(s0, t);
        }
    }

    /** {@inheritDoc} */
    public void handleStep(final FieldOrekitStepInterpolator<T> interpolator, final boolean isLast)
        throws OrekitException {
        for (final FieldOrekitStepHandler<T> handler : handlers) {
            handler.handleStep(interpolator, isLast);
        }
    }

}
