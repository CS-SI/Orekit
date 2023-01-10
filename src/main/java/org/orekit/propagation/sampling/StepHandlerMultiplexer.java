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

import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;

/** This class gathers several {@link OrekitStepHandler} instances into one.
 *
 * @author Luc Maisonobe
 */
public class StepHandlerMultiplexer implements OrekitStepHandler {

    /** Underlying step handlers. */
    private final List<OrekitStepHandler> handlers;

    /** Target time. */
    private AbsoluteDate target;

    /** Last known state. */
    private SpacecraftState last;

    /** Simple constructor.
     */
    public StepHandlerMultiplexer() {
        handlers = new ArrayList<>();
    }

    /** Add a handler for variable size step.
     * <p>
     * If propagation is ongoing (i.e. global {@link #init(SpacecraftState, AbsoluteDate)
     * init} already called and global {@link #finish(SpacecraftState) finish} not called
     * yet), then the local {@link OrekitStepHandler#init(SpacecraftState, AbsoluteDate)
     * OrekitStepHandler.init} method of the added handler will be called with the last
     * known state, so the handler starts properly.
     * </p>
     * @param handler step handler to add
     */
    public void add(final OrekitStepHandler handler) {
        handlers.add(handler);
        if (last != null) {
            // propagation is ongoing, we need to call init now for this handler
            handler.init(last, target);
        }
    }

    /** Add a handler for fixed size step.
     * <p>
     * If propagation is ongoing (i.e. global {@link #init(SpacecraftState, AbsoluteDate)
     * init} already called and global {@link #finish(SpacecraftState) finish} not called
     * yet), then the local {@link OrekitFixedStepHandler#init(SpacecraftState, AbsoluteDate, double)
     * OrekitFixedStepHandler.init} method of the added handler will be called with the
     * last known state, so the handler starts properly.
     * </p>
     * @param h fixed stepsize (s)
     * @param handler handler called at the end of each finalized step
     * @since 11.0
     */
    public void add(final double h, final OrekitFixedStepHandler handler) {
        final OrekitStepHandler normalized = new OrekitStepNormalizer(h, handler);
        handlers.add(normalized);
        if (last != null) {
            // propagation is ongoing, we need to call init now for this handler
            normalized.init(last, target);
        }
    }

    /** Get an unmodifiable view of all handlers.
     * <p>
     * Note that if {@link OrekitFixedStepHandler fixed step handlers} have
     * been {@link #add(double, OrekitFixedStepHandler)}, then they will
     * show up wrapped within {@link OrekitStepNormalizer step normalizers}.
     * </p>
     * @return an unmodifiable view of all handlers
     * @since 11.0
     */
    public List<OrekitStepHandler> getHandlers() {
        return Collections.unmodifiableList(handlers);
    }

    /** Remove a handler.
     * <p>
     * If propagation is ongoing (i.e. global {@link #init(SpacecraftState, AbsoluteDate)
     * init} already called and global {@link #finish(SpacecraftState) finish} not called
     * yet), then the local {@link OrekitStepHandler#finish(SpacecraftState)
     * OrekitStepHandler.finish} method of the removed handler will be called with the last
     * known state, so the handler stops properly.
     * </p>
     * @param handler step handler to remove
     * @since 11.0
     */
    public void remove(final OrekitStepHandler handler) {
        final Iterator<OrekitStepHandler> iterator = handlers.iterator();
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
     * If propagation is ongoing (i.e. global {@link #init(SpacecraftState, AbsoluteDate)
     * init} already called and global {@link #finish(SpacecraftState) finish} not called
     * yet), then the local {@link OrekitFixedStepHandler#finish(SpacecraftState)
     * OrekitFixedStepHandler.finish} method of the removed handler will be called with the
     * last known state, so the handler stops properly.
     * </p>
     * @param handler step handler to remove
     * @since 11.0
     */
    public void remove(final OrekitFixedStepHandler handler) {
        final Iterator<OrekitStepHandler> iterator = handlers.iterator();
        while (iterator.hasNext()) {
            final OrekitStepHandler current = iterator.next();
            if (current instanceof OrekitStepNormalizer &&
                ((OrekitStepNormalizer) current).getFixedStepHandler() == handler) {
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
     * If propagation is ongoing (i.e. global {@link #init(SpacecraftState, AbsoluteDate)
     * init} already called and global {@link #finish(SpacecraftState) finish} not called
     * yet), then the local {@link OrekitStepHandler#finish(SpacecraftState)
     * OrekitStepHandler.finish} and {@link OrekitFixedStepHandler#finish(SpacecraftState)
     * OrekitFixedStepHandler.finish} methods of the removed handlers will be called with the
     * last known state, so the handlers stop properly.
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
    public void init(final SpacecraftState s0, final AbsoluteDate t) {
        this.target = t;
        this.last   = s0;
        for (final OrekitStepHandler handler : handlers) {
            handler.init(s0, t);
        }
    }

    /** {@inheritDoc} */
    public void handleStep(final OrekitStepInterpolator interpolator) {
        this.last = interpolator.getCurrentState();
        for (final OrekitStepHandler handler : handlers) {
            handler.handleStep(interpolator);
        }
    }

    /** {@inheritDoc} */
    public void finish(final SpacecraftState finalState) {
        this.target = null;
        this.last   = null;
        for (final OrekitStepHandler handler : handlers) {
            handler.finish(finalState);
        }
    }

}
