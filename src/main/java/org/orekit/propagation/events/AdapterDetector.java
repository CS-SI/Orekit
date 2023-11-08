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
package org.orekit.propagation.events;

import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.time.AbsoluteDate;

/** Base class for adapting an existing detector.
 * <p>
 * This class is intended to be a base class for changing behaviour
 * of a wrapped existing detector. This base class delegates all
 * its methods to the wrapped detector. Classes extending it can
 * therefore override only the methods they want to change.
 * </p>
 * @author Luc Maisonobe
 * @since 9.3
 */
public class AdapterDetector implements EventDetector {

    /** Wrapped detector. */
    private final EventDetector detector;

    /** Build an adaptor wrapping an existing detector.
     * @param detector detector to wrap
     */
    public AdapterDetector(final EventDetector detector) {
        this.detector = detector;
    }

    /** Get the wrapped detector.
     * @return wrapped detector
     */
    public EventDetector getDetector() {
        return detector;
    }

    /** {@inheritDoc} */
    @Override
    public void init(final SpacecraftState s0, final AbsoluteDate t) {
        detector.init(s0, t);
    }

    /** {@inheritDoc} */
    @Override
    public double g(final SpacecraftState s) {
        return detector.g(s);
    }

    /** {@inheritDoc} */
    @Override
    public double getThreshold() {
        return detector.getThreshold();
    }

    /** {@inheritDoc} */
    @Override
    public AdaptableInterval getMaxCheckInterval() {
        return detector.getMaxCheckInterval();
    }

    /** {@inheritDoc} */
    @Override
    public int getMaxIterationCount() {
        return detector.getMaxIterationCount();
    }

    /** {@inheritDoc} */
    @Override
    public EventHandler getHandler() {
        return detector.getHandler();
    }

}
