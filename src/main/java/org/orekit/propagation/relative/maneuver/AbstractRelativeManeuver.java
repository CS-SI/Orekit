/* Copyright 2002-2026 CS GROUP
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
package org.orekit.propagation.relative.maneuver;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.ode.events.Action;
import org.orekit.annotation.DefaultDataContext;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.DateDetector;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.propagation.relative.RelativeProvider;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.DataDictionary;
import org.orekit.utils.DoubleArrayDictionary;
import org.orekit.utils.TimeStampedPVCoordinates;

import java.util.Objects;

/**
 * Abstract class for implementing an impulse maneuver for a chaser spacecraft.
 *
 * @param <P> type of the relative motion provider
 * @author Romain Cuvillon
 * @since 14.0
 */
public abstract class AbstractRelativeManeuver<P extends RelativeProvider> implements RelativeManeuver {

    /** Trigger event. */
    private final EventDetector trigger;

    /** ΔV vector in the target's LOF. */
    private final Vector3D deltaV;

    /** True if the propagation is forward in time. */
    private boolean forward;

    /** Relative Provider. */
    private final P relativeProvider;

    /** Event Handler. */
    private final EventHandler handler;

    /**
     * Creates a new {@link AbstractRelativeManeuver} from an event detector, a ΔV vector, and a
     * {@link RelativeProvider}.
     *
     * @param trigger          Triggering event detector.
     * @param deltaV           ΔV vector in the local orbital frame of the theory used by the given
     *                         {@link RelativeProvider}.
     * @param relativeProvider Relative motion equations provider.
     */
    public AbstractRelativeManeuver(final EventDetector trigger, final Vector3D deltaV, final P relativeProvider) {
        this.trigger          = trigger;
        this.deltaV           = deltaV;
        this.relativeProvider = relativeProvider;
        this.handler          = new Handler();
    }

    /** {@inheritDoc} */
    @Override
    public void init(final SpacecraftState s0, final AbsoluteDate t) {
        getDetector().init(s0, t);
        forward = t.durationFrom(s0.getDate()) >= 0;
    }

    /** {@inheritDoc} */
    @Override
    public EventDetector getDetector() {
        return trigger;
    }

    /** {@inheritDoc} */
    @Override
    public EventHandler getHandler() {
        return handler;
    }

    /** String representation in UTC.
     *
     * @return string representation with date in UTC timescale
     */
    @Override
    @DefaultDataContext
    public String toString() {
        return toString(TimeScalesFactory.getUTC());
    }

    /**
     * String representation with timescale.
     *
     * @param timeScale timescale to use for date formatting
     * @return string representation with timescale
     */
    public String toString(final TimeScale timeScale) {
        final String triggerString =
                        (getDetector() instanceof DateDetector) ?
                        ((DateDetector) getDetector()).getDate().toString(timeScale) :
                        getDetector().toString();
        return "[trigger = " + triggerString + " ; ΔV = " + deltaV + " ; |ΔV| = " + deltaV.getNorm() + "]";
    }

    /** Getter for the forward boolean.
     * @return forward boolean.
     */
    public boolean getForward() {
        return forward;
    }

    /** {@inheritDoc} */
    @Override
    public Vector3D getDeltaV() {
        return deltaV;
    }

    /** {@inheritDoc} */
    @Override
    public P getRelativeProvider() {
        return this.relativeProvider;
    }

    /** Local handler.
     * <p>
     * Apply the maneuver to the chaser S/C in the additional state.
     * </p>
     */
    private static class Handler implements EventHandler {

        /** {@inheritDoc} */
        public Action eventOccurred(final SpacecraftState s, final EventDetector detector, final boolean increasing) {
            final AbstractRelativeManeuver<?> im = (AbstractRelativeManeuver<?>) detector;
            final Action underlyingAction = im.getDetector().getHandler().eventOccurred(s, im.getDetector(), increasing);
            return (underlyingAction == Action.STOP) ? Action.RESET_STATE : Action.CONTINUE;
        }

        /** {@inheritDoc} */
        @Override
        public SpacecraftState resetState(final EventDetector detector, final SpacecraftState oldState) {
            final AbstractRelativeManeuver<?> maneuver = (AbstractRelativeManeuver<?>) detector;

            // Get velocity increment from maneuver
            final Vector3D deltaV = maneuver.getDeltaV();
            final double sign = maneuver.getForward() ? +1 : -1;

            // Apply increment to velocity of the chaser
            final double[] chaserPV = oldState.getAdditionalState(maneuver.getRelativeProvider().getName());

            chaserPV[3] += sign * deltaV.getX();
            chaserPV[4] += sign * deltaV.getY();
            chaserPV[5] += sign * deltaV.getZ();

            // Reset the equations provider with the new chaser state
            maneuver.getRelativeProvider().setInitialChaserPVTLof(
                            new TimeStampedPVCoordinates(oldState.getDate(),
                                                         new Vector3D(chaserPV[0],
                                                                      chaserPV[1],
                                                                      chaserPV[2]),
                                                         new Vector3D(chaserPV[3],
                                                                      chaserPV[4],
                                                                      chaserPV[5])));

            // Reset the TrueAnomaly of the spacecraft to the current true anomaly if YA is used / do nothing if CW.
            maneuver.resetTrueAnomalyAtManeuver(oldState.getOrbit());

            // Pack everything in a new state
            SpacecraftState newState = new SpacecraftState(oldState.getOrbit(), oldState.getAttitude()).withMass(
                            oldState.getMass());

            // Add additional equations
            for (final DataDictionary.Entry entry : oldState.getAdditionalDataValues().getData()) {
                if (!Objects.equals(entry.getKey(), maneuver.getRelativeProvider().getName())) {
                    newState = newState.addAdditionalData(entry.getKey(), entry.getValue());
                } else {
                    // Use modified chaser PV if the additional state is the currently used YA provider
                    newState = newState.addAdditionalData(maneuver.getRelativeProvider().getName(), chaserPV);
                }
            }

            // Add additional equations' derivatives
            for (final DoubleArrayDictionary.Entry entry : oldState.getAdditionalStatesDerivatives().getData()) {
                newState = newState.addAdditionalStateDerivative(entry.getKey(), entry.getValue());
            }

            return newState;
        }
    }
}
