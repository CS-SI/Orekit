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
package org.orekit.forces.maneuvers.trigger;

import org.hipparchus.ode.events.Action;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.AbstractDetector;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.time.AbsoluteDate;

/** Handler for triggering maneuvers.
 * <p>
 * This handler wraps the handler from a prototype detector and adds
 * firing events handling.
 * </p>
 * @param <T> type of the prototype detector
 * @see AbstractManeuverTriggers
 * @author Luc Maisonobe
 * @since 11.1
 */
class TriggerHandler<T extends AbstractDetector<T>> implements EventHandler<T> {

    /** Prototype detector. */
    private final T prototypeDetector;

    /** Handler for the firing event. */
    private FiringHandler firingHandler;

    /** Propagation direction. */
    private boolean forward;

    /** Simple constructor.
     * @param prototypeDetector prototype event detector
     * @param firingHandler handler for the firing event
     */
    TriggerHandler(final T prototypeDetector, final FiringHandler firingHandler) {
        this.prototypeDetector = prototypeDetector;
        this.firingHandler     = firingHandler;
        this.forward           = true;
    }

    /** {@inheritDoc} */
    @Override
    public void init(final SpacecraftState initialState, final AbsoluteDate target, final T detector) {
        prototypeDetector.getHandler().init(initialState, target, prototypeDetector);
        forward = target.isAfterOrEqualTo(initialState);
    }

    /** {@inheritDoc} */
    @Override
    public Action eventOccurred(final SpacecraftState s, final T detector, final boolean increasing) {
        return firingHandler.handleFiring(prototypeDetector.getHandler().eventOccurred(s, prototypeDetector, increasing),
                                          s, increasing, forward);
    }

    /** {@inheritDoc} */
    @Override
    public SpacecraftState resetState(final T detector, final SpacecraftState oldState) {
        return prototypeDetector.getHandler().resetState(prototypeDetector, oldState);
    }

    /** Handler for firing events. */
    public interface FiringHandler {
        /** Handle a firing event.
         * @param prototypeAction action from the prototype event handler
         * @param state SpaceCraft state to be used in the evaluation
         * @param increasing with the event occurred in an "increasing" or "decreasing" slope direction
         * @param forward indicator for forward propagation
         * @return the Action that the calling detector should pass back to the evaluation system
         */
        Action handleFiring(Action prototypeAction, SpacecraftState state, boolean increasing, boolean forward);
    }

}
