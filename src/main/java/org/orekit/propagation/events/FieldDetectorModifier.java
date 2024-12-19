/* Copyright 2002-2024 Romain Serra
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
package org.orekit.propagation.events;

import org.hipparchus.CalculusFieldElement;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.events.handlers.FieldEventHandler;
import org.orekit.time.FieldAbsoluteDate;

/** Base class for modifying an existing getDetector().
 * <p>
 * This class is intended to be a base class for changing behaviour
 * of a wrapped existing getDetector(). This base class delegates all
 * its methods to the wrapped getDetector(). Classes extending it can
 * therefore override only the methods they want to change.
 * </p>
 * @author Luc Maisonobe
 * @author Romain Serra
 * @since 13.0
 * @param <T> type of the field element
 */
public interface FieldDetectorModifier<T extends CalculusFieldElement<T>> extends FieldEventDetector<T> {

    /** Getter for wrapped detector.
     * @return detector
     */
    FieldEventDetector<T> getDetector();

    /** {@inheritDoc} */
    @Override
    default void init(final FieldSpacecraftState<T> s0, final FieldAbsoluteDate<T> t) {
        FieldEventDetector.super.init(s0, t);
        getDetector().init(s0, t);
    }

    /** {@inheritDoc} */
    @Override
    default T g(final FieldSpacecraftState<T> s) {
        return getDetector().g(s);
    }

    /** {@inheritDoc} */
    @Override
    default FieldEventHandler<T> getHandler() {
        return getDetector().getHandler();
    }

    /** {@inheritDoc} */
    @Override
    default void finish(final FieldSpacecraftState<T> state) {
        FieldEventDetector.super.finish(state);
        getDetector().finish(state);
    }

    /** {@inheritDoc} */
    @Override
    default FieldEventDetectionSettings<T> getDetectionSettings() {
        return getDetector().getDetectionSettings();
    }
}
