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
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.TimeSpanMap;

/**
 * Base class for triggers.
 * @author Luc Maisonobe
 * @since 11.1
 */
public abstract class AbstractManeuverTriggers implements ManeuverTriggers {

    /** Firing time spans. */
    private TimeSpanMap<Boolean> firings;

    /** Propagation direction. */
    private boolean forward;

    /** Simple constructor.
     */
    protected AbstractManeuverTriggers() {
        this.firings = new TimeSpanMap<>(Boolean.FALSE);
    }

    /** {@inheritDoc} */
    @Override
    public void init(final SpacecraftState initialState, final AbsoluteDate target) {

        forward = target.isAfterOrEqualTo(initialState);
        firings = new TimeSpanMap<>(Boolean.FALSE);

        if (isFiringOnInitialState(initialState, forward)) {
            if (forward) {
                firings.addValidAfter(Boolean.TRUE, initialState.getDate());
            } else {
                firings.addValidBefore(Boolean.TRUE, initialState.getDate());
            }
        }

    }

    /**
     * Method to check if the thruster is firing on initialization. can be called by
     * sub classes
     *
     * @param initialState initial spacecraft state
     * @param isForward if true, propagation will be in the forward direction
     * @return true if firing
     */
    protected abstract boolean isFiringOnInitialState(SpacecraftState initialState, boolean isForward);

    /** {@inheritDoc} */
    @Override
    public boolean isFiring(final AbsoluteDate date, final double[] parameters) {
        return firings.get(date);
    }

    /** {@inheritDoc} */
    @Override
    public <S extends CalculusFieldElement<S>> boolean isFiring(final FieldAbsoluteDate<S> date, final S[] parameters) {
        return firings.get(date.toAbsoluteDate());
    }

    /** Get the firings detected during last propagation.
     * @return firings detected during last propagation
     */
    public TimeSpanMap<Boolean> getFirings() {
        return firings;
    }

}
