/* Copyright 2022-2026 Romain Serra.
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

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.ode.events.Action;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.events.FieldEventDetector;
import org.orekit.time.FieldAbsoluteDate;


/** An interface defining an event handler wrapping another one, possibly modifying the outputs or extracting information.
 * By default, all methods delegate to the original handler.
 *
 * @author Romain Serra
 *
 * @see EventHandlerModifier
 * @since 14.0
 */
public interface FieldEventHandlerModifier<T extends CalculusFieldElement<T>> extends FieldEventHandler<T> {

    /**
     * Getter for the original event handler.
     * @return underlying handler
     */
    FieldEventHandler<T> getOriginalHandler();

    /** {@inheritDoc} */
    @Override
    default void init(final FieldSpacecraftState<T> initialState, final FieldAbsoluteDate<T> target, final FieldEventDetector<T> detector) {
        getOriginalHandler().init(initialState, target, detector);
    }

    /** {@inheritDoc} */
    @Override
    default Action eventOccurred(final FieldSpacecraftState<T> s, final FieldEventDetector<T> detector, final boolean increasing) {
        return getOriginalHandler().eventOccurred(s, detector, increasing);
    }

    /** {@inheritDoc} */
    @Override
    default FieldSpacecraftState<T> resetState(final FieldEventDetector<T> detector, final FieldSpacecraftState<T> oldState) {
        return getOriginalHandler().resetState(detector, oldState);
    }

    /** {@inheritDoc} */
    @Override
    default void finish(final FieldSpacecraftState<T> finalState, final FieldEventDetector<T> detector) {
        getOriginalHandler().finish(finalState, detector);
    }

}
