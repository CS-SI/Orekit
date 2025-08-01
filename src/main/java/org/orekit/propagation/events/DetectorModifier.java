/* Copyright 2022-2025 Romain Serra
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

import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.time.AbsoluteDate;

/** Base class for modifying an existing detector.
 * <p>
 * This class is intended to be a base class for changing behaviour
 * of a wrapped existing detector. This base class delegates all
 * its methods to the wrapped detector. Classes extending it can
 * therefore override only the methods they want to change.
 * </p>
 * @author Luc Maisonobe
 * @author Romain Serra
 * @since 13.0
 */
public interface DetectorModifier extends EventDetector {

    /** Get the wrapped detector.
     * @return wrapped detector
     */
    EventDetector getDetector();

    /** {@inheritDoc} */
    @Override
    default void init(final SpacecraftState s0, final AbsoluteDate t) {
        EventDetector.super.init(s0, t);
        getDetector().init(s0, t);
    }

    /** {@inheritDoc} */
    @Override
    default void reset(final SpacecraftState state, final AbsoluteDate target) {
        EventDetector.super.reset(state, target);
        getDetector().reset(state, target);
    }

    /** {@inheritDoc} */
    @Override
    default boolean dependsOnTimeOnly() {
        return getDetector().dependsOnTimeOnly();
    }

    /** {@inheritDoc} */
    @Override
    default double g(final SpacecraftState s) {
        return getDetector().g(s);
    }

    /** {@inheritDoc} */
    @Override
    default EventHandler getHandler() {
        return getDetector().getHandler();
    }

    /** {@inheritDoc} */
    @Override
    default void finish(final SpacecraftState state) {
        EventDetector.super.finish(state);
        getDetector().finish(state);
    }

    /** {@inheritDoc} */
    @Override
    default EventDetectionSettings getDetectionSettings() {
        return getDetector().getDetectionSettings();
    }
}
