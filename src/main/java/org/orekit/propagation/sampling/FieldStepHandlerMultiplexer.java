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
package org.orekit.propagation.sampling;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.hipparchus.CalculusFieldElement;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.time.FieldAbsoluteDate;

/** This class gathers several {@link OrekitStepHandler} instances into one.
 *
 * @author Luc Maisonobe
 * @param <T> type of the field elements
 */
public class FieldStepHandlerMultiplexer<T extends CalculusFieldElement<T>> implements FieldOrekitStepHandler<T> {

    /** Underlying step handlers. */
    private final List<FieldOrekitStepHandler<T>> handlers;

    /** Target time. */
    private FieldAbsoluteDate<T> target;

    /** Last known state. */
    private FieldSpacecraftState<T> last;

    /** Simple constructor.
     */
    public FieldStepHandlerMultiplexer() {
        handlers = new ArrayList<>();
    }

    /** Add a handler for variable size step.
     * <p>
     * If propagation is ongoing (i.e. global {@link #init(FieldSpacecraftState, FieldAbsoluteDate)
     * init} already called and global {@link #finish(FieldSpacecraftState) finish} not called
     * yet), then the local {@link FieldOrekitStepHandler#init(FieldSpacecraftState, FieldAbsoluteDate)
     * FieldOrekitStepHandler.init} method of the added handler will be called with the last
     * known state, so the handler starts properly.
     * </p>
     * @param handler step handler to add
     */
    public void add(final FieldOrekitStepHandler<T> handler) {
        handlers.add(handler);
        if (last != null) {
            // propagation is ongoing, we need to call init now for this handler
            handler.init(last, target);
        }
    }

    /** Add a handler for fixed size step.
     * <p>
     * If propagation is ongoing (i.e. global {@link #init(FieldSpacecraftState, FieldAbsoluteDate)
     * init} already called and global {@link #finish(FieldSpacecraftState) finish} not called
     * yet), then the local {@link FieldOrekitFixedStepHandler#init(FieldSpacecraftState, FieldAbsoluteDate,
     * CalculusFieldElement) FieldOrekitStepHandler.init} method of the added handler will be
     * called with the last known state, so the handler starts properly.
     * </p>
     * @param h fixed stepsize (s)
     * @param handler handler called at the end of each finalized step
     * @since 11.0
     */
    public void add(final T h, final FieldOrekitFixedStepHandler<T> handler) {
        final FieldOrekitStepHandler<T> normalized = new FieldOrekitStepNormalizer<>(h, handler);
        handlers.add(normalized);
        if (last != null) {
            // propagation is ongoing, we need to call init now for this handler
            normalized.init(last, target);
        }
    }

    /** Get an unmodifiable view of all handlers.
     * <p>
     * Note that if {@link FieldOrekitFixedStepHandler fixed step handlers} have
     * been {@link #add(CalculusFieldElement, FieldOrekitFixedStepHandler)}, then they will
     * show up wrapped within {@link FieldOrekitStepNormalizer step normalizers}.
     * </p>
     * @return an unmodifiable view of all handlers
     * @since 11.0
     */
    public List<FieldOrekitStepHandler<T>> getHandlers() {
        return Collections.unmodifiableList(handlers);
    }

    /** Remove a handler.
     * <p>
     * If propagation is ongoing (i.e. global {@link #init(FieldSpacecraftState, FieldAbsoluteDate)
     * init} already called and global {@link #finish(FieldSpacecraftState) finish} not called
     * yet), then the local {@link FieldOrekitStepHandler#finish(FieldSpacecraftState)
     * FieldOrekitStepHandler.finish} method of the removed handler will be called with the last
     * known state, so the handler stops properly.
     * </p>
     * @param handler step handler to remove
     * @since 11.0
     */
    public void remove(final FieldOrekitStepHandler<T> handler) {
        final Iterator<FieldOrekitStepHandler<T>> iterator = handlers.iterator();
        while (iterator.hasNext()) {
            if (iterator.next() == handler) {
                if (last != null) {
                    // propagation is ongoing, we need to call finish now for this handler
                    handler.finish(last);
                }
                iterator.remove();
                return;
            }
        }
    }

    /** Remove a handler.
     * <p>
     * If propagation is ongoing (i.e. global {@link #init(FieldSpacecraftState, FieldAbsoluteDate)
     * init} already called and global {@link #finish(FieldSpacecraftState) finish} not called
     * yet), then the local {@link FieldOrekitFixedStepHandler#finish(FieldSpacecraftState)
     * FieldOrekitFixedStepHandler.finish} method of the removed handler will be called with the last
     * known state, so the handler stops properly.
     * </p>
     * @param handler step handler to remove
     * @since 11.0
     */
    public void remove(final FieldOrekitFixedStepHandler<T> handler) {
        final Iterator<FieldOrekitStepHandler<T>> iterator = handlers.iterator();
        while (iterator.hasNext()) {
            final FieldOrekitStepHandler<T> current = iterator.next();
            if (current instanceof FieldOrekitStepNormalizer &&
                ((FieldOrekitStepNormalizer<T>) current).getFixedStepHandler() == handler) {
                if (last != null) {
                    // propagation is ongoing, we need to call finish now for this handler
                    current.finish(last);
                }
                iterator.remove();
                return;
            }
        }
    }

    /** Remove all handlers managed by this multiplexer.
     * <p>
     * If propagation is ongoing (i.e. global {@link #init(FieldSpacecraftState, FieldAbsoluteDate)
     * init} already called and global {@link #finish(FieldSpacecraftState) finish} not called
     * yet), then the local {@link FieldOrekitStepHandler#finish(FieldSpacecraftState)
     * FieldOrekitStepHandler.finish} and {@link FieldOrekitFixedStepHandler#finish(FieldSpacecraftState)
     * FieldOrekitFixedStepHandler.finish} methods of the removed handlers will be called with the last
     * known state, so the handlers stop properly.
     * </p>
     * @since 11.0
     */
    public void clear() {
        if (last != null) {
            // propagation is ongoing, we need to call finish now for all handlers
            handlers.forEach(h -> h.finish(last));
        }
        handlers.clear();
    }

    /** {@inheritDoc} */
    public void init(final FieldSpacecraftState<T> s0, final FieldAbsoluteDate<T> t) {
        this.target = t;
        this.last   = s0;
        for (final FieldOrekitStepHandler<T> handler : handlers) {
            handler.init(s0, t);
        }
    }

    /** {@inheritDoc} */
    public void handleStep(final FieldOrekitStepInterpolator<T> interpolator) {
        this.last = interpolator.getCurrentState();
        for (final FieldOrekitStepHandler<T> handler : handlers) {
            handler.handleStep(interpolator);
        }
    }

    /** {@inheritDoc} */
    public void finish(final FieldSpacecraftState<T> finalState) {
        this.target = null;
        this.last   = null;
        for (final FieldOrekitStepHandler<T> handler : handlers) {
            handler.finish(finalState);
        }
    }

}
