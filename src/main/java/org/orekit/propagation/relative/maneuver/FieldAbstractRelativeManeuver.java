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
import org.orekit.propagation.events.FieldEventDetectionSettings;
import org.orekit.propagation.events.FieldEventDetector;
import org.orekit.propagation.events.handlers.FieldEventHandler;
import org.orekit.propagation.events.intervals.FieldAdaptableInterval;
import org.orekit.propagation.relative.FieldRelativeProvider;
import org.orekit.propagation.relative.RelativeProvider;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.FieldArrayDictionary;
import org.orekit.utils.FieldDataDictionary;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.TimeStampedFieldPVCoordinates;

import java.util.Objects;

/**
 * Abstract class for implementing a maneuver for a chaser spacecraft.
 *
 * @author Romain Cuvillon
 * @since 14.0
 * @param <T> Any scalar field.
 */
public abstract class FieldAbstractRelativeManeuver <T extends CalculusFieldElement<T>> implements FieldRelativeManeuver<T> {
    /**
     * Trigger event.
     */
    private final FieldEventDetector<T> trigger;

    /**
     * ΔV vector in the target's LOF.
     */
    private final FieldVector3D<T> deltaV;

    /**
     * True if the propagation is forward in time.
     */
    private boolean forward;

    /**
     * Relative Provider.
     */
    private final FieldRelativeProvider<T> relativeProvider;

    /**
     * Event Handler.
     */
    private final FieldEventHandler<T> handler;

    /**
     * Creates a new {@link AbstractRelativeManeuver} from an event detector, a ΔV vector, and a {@link RelativeProvider}.
     * @param trigger Triggering event detector.
     * @param deltaV ΔV vector in the local orbital frame of the theory used by the given {@link RelativeProvider}.
     * @param relativeProvider Relative motion equations provider.
     */
    public FieldAbstractRelativeManeuver(final FieldEventDetector<T> trigger, final FieldVector3D<T> deltaV, final FieldRelativeProvider<T> relativeProvider) {
        this.trigger = trigger;
        this.deltaV = deltaV;
        this.relativeProvider = relativeProvider;
        this.handler = new Handler<>();
    }

    /**
     * {@inheritDoc}
     */
    public void init(final FieldSpacecraftState<T> s0, final FieldAbsoluteDate<T> t) {
        forward = t.durationFrom(s0.getDate()).getReal() >= 0;
        // Initialize the triggering event
        trigger.init(s0, t);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public T g(final FieldSpacecraftState<T> s) {
        return trigger.g(s);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public T getThreshold() {
        return trigger.getThreshold();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FieldAdaptableInterval<T> getMaxCheckInterval() {
        return trigger.getMaxCheckInterval();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getMaxIterationCount() {
        return trigger.getMaxIterationCount();
    }

    /**
     * @return The triggering event.
     */
    @Override
    public FieldEventDetector<T> getTrigger() {
        return trigger;
    }

    /**
     * @return forward boolean.
     */
    public boolean getForward() {
        return forward;
    }

    /**
     * @return The ΔV vector of the maneuver, expressed in the target spacecraft's LOF.
     */
    @Override
    public FieldVector3D<T> getDeltaV() {
        return deltaV;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FieldEventDetectionSettings<T> getDetectionSettings() {
        return trigger.getDetectionSettings();
    }

    /**
     * @return relative provider.
     */
    @Override
    public FieldRelativeProvider<T> getRelativeProvider() {
        return this.relativeProvider;
    }

    /**
     * {@inheritDoc}
     */
    public FieldEventHandler<T> getHandler() {
        return handler;
    }

    /**
     * Local handler.
     */
    private static class Handler<T extends CalculusFieldElement<T>> implements FieldEventHandler<T> {
        /**
         * {@inheritDoc}
         */
        public Action eventOccurred(final FieldSpacecraftState<T> s, final FieldEventDetector<T> detector, final boolean increasing) {
            // filter underlying event
            final FieldAbstractRelativeManeuver<T> im = (FieldAbstractRelativeManeuver<T>) detector;
            final Action underlyingAction = im.getTrigger().getHandler().eventOccurred(s, im.getTrigger(), increasing);
            return (underlyingAction == Action.STOP) ? Action.RESET_STATE : Action.CONTINUE;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public FieldSpacecraftState<T> resetState(final FieldEventDetector<T> detector, final FieldSpacecraftState<T> oldState) {
            // Cast the detector to a FieldYamanakaAnkersenManeuver
            final FieldAbstractRelativeManeuver<T> maneuver = (FieldAbstractRelativeManeuver<T>) detector;
            //Get velocity increment from maneuver
            final FieldVector3D<T> deltaV = maneuver.getDeltaV();
            final double sign = maneuver.getForward() ? +1 : -1;
            //Apply increment to velocity of the chaser
            final T[] chaserPV = oldState.getAdditionalState(maneuver.getRelativeProvider().getName());
            chaserPV[3] = chaserPV[3].add(deltaV.getX().multiply(sign));
            chaserPV[4] = chaserPV[4].add(deltaV.getY().multiply(sign));
            chaserPV[5] = chaserPV[5].add(deltaV.getZ().multiply(sign));
            // Reset the Yamanaka-Ankersen equations provider with the new chaser state
            maneuver.getRelativeProvider().setInitialChaserPVTLof(new TimeStampedFieldPVCoordinates<>(
                    oldState.getDate(),
                    new FieldPVCoordinates<>(new FieldVector3D<>(chaserPV[0], chaserPV[1], chaserPV[2]),
                            new FieldVector3D<>(chaserPV[3], chaserPV[4], chaserPV[5]))));
            // Pack everything in a new state
            FieldSpacecraftState<T> newState = new FieldSpacecraftState<>(oldState.getOrbit(), oldState.getAttitude()).withMass(oldState.getMass());
            // Reset the TrueAnomaly of the spacecraft to the current true anomaly if YA is used / do nothing if CW.
            maneuver.resetTrueAnomalyAtManeuver(oldState.getOrbit());
            // Add additional equations
            for (final FieldDataDictionary<T>.Entry entry : oldState.getAdditionalDataValues().getData()) {
                if (!Objects.equals(entry.getKey(), maneuver.getRelativeProvider().getName())) {
                    newState = newState.addAdditionalData(entry.getKey(), entry.getValue());
                } else {
                    // Use modified chaser PV if the additional state is the currently used CW provider
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
