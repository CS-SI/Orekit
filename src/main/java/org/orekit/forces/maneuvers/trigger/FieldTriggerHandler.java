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

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.ode.events.Action;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.events.AbstractDetector;
import org.orekit.propagation.events.FieldEventDetector;
import org.orekit.propagation.events.handlers.FieldEventHandler;
import org.orekit.time.FieldAbsoluteDate;

/** Handler for triggering maneuvers.
 * <p>
 * This handler wraps the handler from a prototype detector and adds
 * firing events handling.
 * </p>
 * @param <D> type of the primitive double event detector
 * @param <T> type of the field event detector
 * @param <S> type of the field elements
 * @see AbstractManeuverTriggers
 * @author Luc Maisonobe
 * @since 11.1
 */
public class FieldTriggerHandler<D extends AbstractDetector<D>, T extends FieldEventDetector<S>, S extends CalculusFieldElement<S>> implements FieldEventHandler<T, S> {

    /** Prototype detector. */
    private final D prototypeDetector;

    /** Handler for the firing event. */
    private FiringHandler<S> firingHandler;

    /** Propagation direction. */
    private boolean forward;

    /** Simple constructor.
     * @param prototypeDetector prototype event detector
     * @param firingHandler handler for the firing event
     */
    FieldTriggerHandler(final D prototypeDetector, final FiringHandler<S> firingHandler) {
        this.prototypeDetector = prototypeDetector;
        this.firingHandler     = firingHandler;
        this.forward           = true;
    }

    /** {@inheritDoc} */
    @Override
    public void init(final FieldSpacecraftState<S> initialState, final FieldAbsoluteDate<S> target) {
        prototypeDetector.getHandler().init(initialState.toSpacecraftState(), target.toAbsoluteDate(), prototypeDetector);
        forward = target.isAfterOrEqualTo(initialState);
    }

    /** {@inheritDoc} */
    @Override
    public Action eventOccurred(final FieldSpacecraftState<S> s, final T detector, final boolean increasing) {
        return firingHandler.handleFiring(prototypeDetector.getHandler().eventOccurred(s.toSpacecraftState(), prototypeDetector, increasing),
                                          s, increasing, forward);
    }

    /** {@inheritDoc} */
    @Override
    public FieldSpacecraftState<S> resetState(final T detector, final FieldSpacecraftState<S> oldState) {
        return new FieldSpacecraftState<>(oldState.getDate().getField(),
                                          prototypeDetector.getHandler().resetState(prototypeDetector, oldState.toSpacecraftState()));
    }

    /** Handler for firing events.
     * @param <S> type of the field elements
     */
    public interface FiringHandler<S extends CalculusFieldElement<S>> {
        /** Handle a firing event.
         * @param prototypeAction action from the prototype event handler
         * @param state SpaceCraft state to be used in the evaluation
         * @param increasing with the event occurred in an "increasing" or "decreasing" slope direction
         * @param forward indicator for forward propagation
         * @return the Action that the calling detector should pass back to the evaluation system
         */
        Action handleFiring(Action prototypeAction, FieldSpacecraftState<S> state, boolean increasing, boolean forward);
    }

}
