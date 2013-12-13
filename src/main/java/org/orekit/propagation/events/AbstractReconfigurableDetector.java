/* Copyright 2002-2013 CS Systèmes d'Information
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
package org.orekit.propagation.events;

import org.orekit.errors.OrekitException;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.handlers.EventHandler;

/** Builder reconfiguration API for detectors.
 * <p>
 * This class is only a temporary one introduced with Orekit 6.1 between the
 * legacy {@link AbstractDetector} and all the Orekit provided detectors. It
 * will be removed in 7.0, when all its methods will be pushed up in the
 * hierarchy, with declarations at the {@link EventDetector} interface level
 * and implementation at the {@link AbstractDetector} abstract class level.
 * In other words, the methods will stay, but the intermediate class will
 * disappear.
 * </p>
 * @author Luc Maisonobe
 * @param <T> class type for the generic version
 * @since 6.1
 */
public abstract class AbstractReconfigurableDetector<T extends EventDetector> extends AbstractDetector {

    /** Serializable UID. */
    private static final long serialVersionUID = 20131202L;

    /** Default handler for event overrides. */
    private final EventHandler<T> handler;

    /** Build a new instance.
     * @param maxCheck maximum checking interval (s)
     * @param threshold convergence threshold (s)
     * @param maxIter maximum number of iterations in the event time search
     * @param handler event handler to call at event occurrences
     */
    protected AbstractReconfigurableDetector(final double maxCheck, final double threshold,
                                             final int maxIter, final EventHandler<T> handler) {
        super(maxCheck, threshold, maxIter);
        this.handler = handler;
    }

    /** Build a new instance.
     * @param newMaxCheck maximum checking interval (s)
     * @param newThreshold convergence threshold (s)
     * @param newMaxIter maximum number of iterations in the event time search
     * @param newHandler event handler to call at event occurrences
     * @return a new instance of the appropriate sub-type
     */
    protected abstract T create(final double newMaxCheck, final double newThreshold,
                                final int newMaxIter, final EventHandler<T> newHandler);

    /**
     * Setup the maximum checking interval.
     * <p>
     * This will override a maximum checking interval if it has been configured previously.
     * </p>
     * @param newMaxCheck maximum checking interval (s)
     * @return a new detector with updated configuration (the instance is not changed)
     * @since 6.1
     */
    public T withMaxCheck(final double newMaxCheck) {
        return create(newMaxCheck, getThreshold(), getMaxIterationCount(), getHandler());
    }

    /**
     * Setup the maximum number of iterations in the event time search.
     * <p>
     * This will override a number of iterations if it has been configured previously.
     * </p>
     * @param newMaxIter maximum number of iterations in the event time search
     * @return a new detector with updated configuration (the instance is not changed)
     * @since 6.1
     */
    public T withMaxIter(final int newMaxIter) {
        return create(getMaxCheckInterval(), getThreshold(), newMaxIter,  getHandler());
    }

    /**
     * Setup the convergence threshold.
     * <p>
     * This will override a convergence threshold if it has been configured previously.
     * </p>
     * @param newThreshold convergence threshold (s)
     * @return a new detector with updated configuration (the instance is not changed)
     * @since 6.1
     */
    public T withThreshold(final double newThreshold) {
        return create(getMaxCheckInterval(), newThreshold, getMaxIterationCount(),  getHandler());
    }

    /**
     * Setup the event handler to call at event occurrences.
     * <p>
     * This will override a handler if it has been configured previously.
     * </p>
     * @param newHandler event handler to call at event occurrences
     * @return a new detector with updated configuration (the instance is not changed)
     * @since 6.1
     */
    public T withHandler(final EventHandler<T> newHandler) {
        return create(getMaxCheckInterval(), getThreshold(), getMaxIterationCount(), newHandler);
    }

    /** Get the handler.
     * @return event handler to call at event occurrences
     */
    public EventHandler<T> getHandler() {
        return handler;
    }

    /** {@inheritDoc}
     * @deprecated as of 6.1 replaced by {@link
     * EventHandler#eventOccurred(SpacecraftState, EventDetector, boolean)}
     */
    @Deprecated
    public Action eventOccurred(final SpacecraftState s, final boolean increasing)
        throws OrekitException {
        @SuppressWarnings("unchecked")
        final T self = (T) this;
        return convert(handler.eventOccurred(s, self, increasing));
    }

    /** Conversion between pre-6.1 EventDetector.Action and post-6.1 EventHandler.Action.
     * @param action action to convert
     * @return converted action
     */
    @SuppressWarnings("deprecation")
    public static EventHandler.Action convert(final EventDetector.Action action) {
        switch (action) {
        case STOP :
            return EventHandler.Action.STOP;
        case RESET_STATE :
            return EventHandler.Action.RESET_STATE;
        case RESET_DERIVATIVES :
            return EventHandler.Action.RESET_DERIVATIVES;
        case CONTINUE :
            return EventHandler.Action.CONTINUE;
        default :
            // this should never occur
            throw OrekitException.createInternalError(null);
        }
    }

    /** Conversion between post 6.1 EventHandler.Action and pre-6.1 EventDetector.Action.
     * @param action action to convert
     * @return converted action
     */
    @SuppressWarnings("deprecation")
    public static EventDetector.Action convert(final EventHandler.Action action) {
        switch (action) {
        case STOP :
            return EventDetector.Action.STOP;
        case RESET_STATE :
            return EventDetector.Action.RESET_STATE;
        case RESET_DERIVATIVES :
            return EventDetector.Action.RESET_DERIVATIVES;
        case CONTINUE :
            return EventDetector.Action.CONTINUE;
        default :
            // this should never occur
            throw OrekitException.createInternalError(null);
        }
    }

    /** {@inheritDoc}
     * @deprecated as of 6.1 replaced by {@link EventHandler#resetState(EventDetector, SpacecraftState)}
     */
    @Deprecated
    public SpacecraftState resetState(final SpacecraftState oldState)
        throws OrekitException {
        @SuppressWarnings("unchecked")
        final T self = (T) this;
        return handler.resetState(self, oldState);
    }

}
