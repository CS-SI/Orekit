/* Copyright 2002-2021 CS GROUP
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
import java.util.Iterator;
import java.util.List;

import org.hipparchus.CalculusFieldElement;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.time.FieldAbsoluteDate;

/** This class gathers several {@link OrekitStepHandler} instances into one.
 *
 * @author Luc Maisonobe
 */
public class FieldStepHandlerMultiplexer<T extends CalculusFieldElement<T>> implements FieldOrekitStepHandler<T> {

    /** Underlying step handlers. */
    private final List<FieldOrekitStepHandler<T>> handlers;

    /** Simple constructor.
     */
    public FieldStepHandlerMultiplexer() {
        handlers = new ArrayList<>();
    }

    /** Add a step handler.
     * @param handler step handler to add
     */
    public void add(final FieldOrekitStepHandler<T> handler) {
        handlers.add(handler);
    }

    /** Add a handler for fixed size step.
     * @param h fixed stepsize (s)
     * @param handler handler called at the end of each finalized step
     * @since 11.0
     */
    public void add(final T h, final FieldOrekitFixedStepHandler<T> handler) {
        handlers.add(new FieldOrekitStepNormalizer<>(h, handler));
    }

    /** Remove a handler.
     * @param handler step handler to remove
     * @since 11.0
     */
    public void remove(final FieldOrekitStepHandler<T> handler) {
        final Iterator<FieldOrekitStepHandler<T>> iterator = handlers.iterator();
        while (iterator.hasNext()) {
            if (iterator.next() == handler) {
                iterator.remove();
                return;
            }
        }
    }

    /** Remove a handler.
     * @param handler step handler to remove
     * @since 11.0
     */
    public void remove(final FieldOrekitFixedStepHandler<T> handler) {
        final Iterator<FieldOrekitStepHandler<T>> iterator = handlers.iterator();
        while (iterator.hasNext()) {
            final FieldOrekitStepHandler<T> current = iterator.next();
            if (current instanceof FieldOrekitStepNormalizer &&
                ((FieldOrekitStepNormalizer<T>) current).getFixedStepHandler() == handler) {
                iterator.remove();
                return;
            }
        }
    }

    /** Remove all handlers managed by this multiplexer.
     * @since 11.0
     */
    public void clear() {
        handlers.clear();
    }

    /** {@inheritDoc} */
    public void init(final FieldSpacecraftState<T> s0, final FieldAbsoluteDate<T> t) {
        for (final FieldOrekitStepHandler<T> handler : handlers) {
            handler.init(s0, t);
        }
    }

    /** {@inheritDoc} */
    public void handleStep(final FieldOrekitStepInterpolator<T> interpolator) {
        for (final FieldOrekitStepHandler<T> handler : handlers) {
            handler.handleStep(interpolator);
        }
    }

    /** {@inheritDoc} */
    public void finish(final FieldSpacecraftState<T> finalState) {
        for (final FieldOrekitStepHandler<T> handler : handlers) {
            handler.finish(finalState);
        }
    }

}
