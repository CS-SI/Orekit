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

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.ode.events.Action;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.events.FieldEventDetector;
import org.orekit.propagation.events.handlers.FieldEventHandler;
import org.orekit.propagation.relative.FieldRelativeProvider;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.FieldArrayDictionary;
import org.orekit.utils.FieldDataDictionary;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.TimeStampedFieldPVCoordinates;

import java.util.Objects;

/**
 * Abstract class for implementing an impulse maneuver for a chaser spacecraft.
 *
 * @param <T> type of the field element
 * @param <P> type of the relative motion provider
 * @author Romain Cuvillon
 * @since 14.0
 */
public abstract class FieldAbstractRelativeManeuver<T extends CalculusFieldElement<T>, P extends FieldRelativeProvider<T>>
                implements FieldRelativeManeuver<T> {

    /** Trigger event. */
    private final FieldEventDetector<T> trigger;

    /** ΔV vector in the target's LOF. */
    private final FieldVector3D<T> deltaV;

    /** True if the propagation is forward in time. */
    private boolean forward;

    /** Relative Provider. */
    private final P relativeProvider;

    /** Event Handler. */
    private final FieldEventHandler<T> handler;

    /**
     * Creates a new {@link FieldAbstractRelativeManeuver} from an event detector, a ΔV vector, and a
     * {@link FieldRelativeProvider}.
     *
     * @param trigger          Triggering event detector.
     * @param deltaV           ΔV vector in the local orbital frame of the theory used by the given
     *                         {@link FieldRelativeProvider}.
     * @param relativeProvider Relative motion equations provider.
     */
    public FieldAbstractRelativeManeuver(final FieldEventDetector<T> trigger, final FieldVector3D<T> deltaV,
                                         final P relativeProvider) {
        this.trigger          = trigger;
        this.deltaV           = deltaV;
        this.relativeProvider = relativeProvider;
        this.handler          = new Handler<>();
    }

    /** {@inheritDoc} */
    @Override
    public void init(final FieldSpacecraftState<T> s0, final FieldAbsoluteDate<T> t) {
        forward = t.durationFrom(s0.getDate()).getReal() >= 0;
        getDetector().init(s0, t);
    }

    /** {@inheritDoc} */
    @Override
    public FieldEventDetector<T> getDetector() {
        return trigger;
    }

    /**
     * Getter for the forward boolean.
     *
     * @return forward boolean
     */
    public boolean getForward() {
        return forward;
    }

    /** {@inheritDoc} */
    @Override
    public FieldVector3D<T> getDeltaV() {
        return deltaV;
    }

    /** {@inheritDoc} */
    @Override
    public P getRelativeProvider() {
        return this.relativeProvider;
    }

    /** {@inheritDoc} */
    @Override
    public FieldEventHandler<T> getHandler() {
        return handler;
    }

    /**
     * Local handler.
     * <p>
     * Apply the maneuver to the chaser S/C in the additional state.
     * </p>
     */
    private static class Handler<T extends CalculusFieldElement<T>> implements FieldEventHandler<T> {

        /** {@inheritDoc} */
        @Override
        public Action eventOccurred(final FieldSpacecraftState<T> s, final FieldEventDetector<T> detector,
                                    final boolean increasing) {
            final FieldAbstractRelativeManeuver<T, ?> im = (FieldAbstractRelativeManeuver<T, ?>) detector;
            final Action underlyingAction =
                            im.getDetector().getHandler().eventOccurred(s, im.getDetector(), increasing);
            return (underlyingAction == Action.STOP) ? Action.RESET_STATE : Action.CONTINUE;
        }

        /** {@inheritDoc} */
        @Override
        public FieldSpacecraftState<T> resetState(final FieldEventDetector<T> detector,
                                                  final FieldSpacecraftState<T> oldState) {
            final FieldAbstractRelativeManeuver<T, ?> maneuver = (FieldAbstractRelativeManeuver<T, ?>) detector;

            // Get velocity increment from maneuver
            final FieldVector3D<T> deltaV = maneuver.getDeltaV();
            final double sign = maneuver.getForward() ? +1 : -1;

            // Apply increment to velocity of the chaser
            final T[] chaserPV = oldState.getAdditionalState(maneuver.getRelativeProvider().getName());
            chaserPV[3] = chaserPV[3].add(deltaV.getX().multiply(sign));
            chaserPV[4] = chaserPV[4].add(deltaV.getY().multiply(sign));
            chaserPV[5] = chaserPV[5].add(deltaV.getZ().multiply(sign));

            // Reset the equations provider with the new chaser state
            maneuver.getRelativeProvider().setInitialChaserPVTLof(
                            new TimeStampedFieldPVCoordinates<>(oldState.getDate(), new FieldPVCoordinates<>(
                                            new FieldVector3D<>(chaserPV[0], chaserPV[1], chaserPV[2]),
                                            new FieldVector3D<>(chaserPV[3], chaserPV[4], chaserPV[5]))));

            // Pack everything in a new state
            FieldSpacecraftState<T> newState =
                            new FieldSpacecraftState<>(oldState.getOrbit(), oldState.getAttitude()).withMass(
                                            oldState.getMass());

            // Reset the TrueAnomaly of the spacecraft to the current true anomaly if YA is used / do nothing if CW.
            maneuver.resetTrueAnomalyAtManeuver(oldState.getOrbit());

            // Add additional equations
            for (final FieldDataDictionary<T>.Entry entry : oldState.getAdditionalDataValues().getData()) {
                if (!Objects.equals(entry.getKey(), maneuver.getRelativeProvider().getName())) {
                    newState = newState.addAdditionalData(entry.getKey(), entry.getValue());
                } else {
                    // Use modified chaser PV if the additional state is the currently used provider
                    newState = newState.addAdditionalData(maneuver.getRelativeProvider().getName(), chaserPV);
                }
            }

            // Add additional equations' derivatives
            for (final FieldArrayDictionary<T>.Entry entry : oldState.getAdditionalStatesDerivatives().getData()) {
                newState = newState.addAdditionalStateDerivative(entry.getKey(), entry.getValue());
            }

            return newState;
        }
    }
}
