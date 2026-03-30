/* Copyright 2022-2026 Romain Serra
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
package org.orekit.estimation.measurements.model;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.signal.FieldSignalReceptionCondition;
import org.orekit.signal.FieldSignalTravelTimeAdjustableEmitter;
import org.orekit.signal.SignalReceptionCondition;
import org.orekit.signal.SignalTravelTimeAdjustableEmitter;
import org.orekit.signal.SignalTravelTimeModel;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.FieldPVCoordinatesProvider;
import org.orekit.utils.PVCoordinatesProvider;
import org.orekit.utils.TimeStampedFieldPVCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;

/**
 * Class for one-legged range rate (a.k.a. Doppler measurement).
 * A signal is transmitted and received. There is no further assumption.
 * @since 14.0
 * @author Romain Serra
 */
public class OneLeggedRangeRateModel extends AbstractSignalBasedModel {

    /**
     * Constructor.
     * @param signalTravelTimeModel signal travel time model
     */
    public OneLeggedRangeRateModel(final SignalTravelTimeModel signalTravelTimeModel) {
        super(signalTravelTimeModel);
    }

    /**
     * Compute measurement without guess.
     *
     * @param receptionCondition signal reception condition
     * @param receiverVelocity receiver's velocity vector at reception
     * @param emitter          signal initial emitter coordinates provider
     * @return range rate (m/s)
     */
    public double value(final SignalReceptionCondition receptionCondition, final Vector3D receiverVelocity,
                        final PVCoordinatesProvider emitter) {
        return value(receptionCondition, receiverVelocity, emitter, receptionCondition.getReceptionDate());
    }

    /**
     * Compute measurement.
     *
     * @param receptionCondition signal reception condition
     * @param receiverVelocity receiver's velocity vector at reception
     * @param emitter          signal initial emitter coordinates provider
     * @param approxEmissionDate guess for the emission date
     * @return range rate (m/s)
     */
    public double value(final SignalReceptionCondition receptionCondition, final Vector3D receiverVelocity,
                        final PVCoordinatesProvider emitter, final AbsoluteDate approxEmissionDate) {
        final SignalTravelTimeAdjustableEmitter adjustableEmitter = getSignalTravelTimeModel().getAdjustableEmitterComputer(emitter);
        final AbsoluteDate receptionDate = receptionCondition.getReceptionDate();
        final double delay = adjustableEmitter.computeDelay(receptionCondition, approxEmissionDate);
        final AbsoluteDate emissionDate = receptionDate.shiftedBy(-delay);
        final TimeStampedPVCoordinates emitterPV = emitter.getPVCoordinates(emissionDate, receptionCondition.getReferenceFrame());
        final Vector3D relativePosition = receptionCondition.getReceiverPosition().subtract(emitterPV.getPosition());
        final Vector3D relativeVelocity = receiverVelocity.subtract(emitterPV.getVelocity());
        return Vector3D.dotProduct(relativeVelocity, relativePosition.normalize());
    }

    /**
     * Compute measurement without guess.
     * @param <T> field type
     * @param receptionCondition signal reception condition
     * @param receiverVelocity receiver's velocity vector at reception
     * @param emitter          signal initial emitter coordinates provider
     * @return range rate (m/s)
     */
    public <T extends CalculusFieldElement<T>> T value(final FieldSignalReceptionCondition<T> receptionCondition,
                                                       final FieldVector3D<T> receiverVelocity,
                                                       final FieldPVCoordinatesProvider<T> emitter) {
        return value(receptionCondition, receiverVelocity, emitter, receptionCondition.getReceptionDate());
    }
    /**
     * Compute measurement.
     * @param <T> field type
     * @param receptionCondition signal reception condition
     * @param receiverVelocity receiver's velocity vector at reception
     * @param emitter          signal initial emitter coordinates provider
     * @param approxEmissionDate guess for the emission date
     * @return range rate (m/s)
     */
    public <T extends CalculusFieldElement<T>> T value(final FieldSignalReceptionCondition<T> receptionCondition,
                                                       final FieldVector3D<T> receiverVelocity,
                                                       final FieldPVCoordinatesProvider<T> emitter,
                                                       final FieldAbsoluteDate<T> approxEmissionDate) {
        final FieldAbsoluteDate<T> receptionDate = receptionCondition.getReceptionDate();
        final FieldSignalTravelTimeAdjustableEmitter<T> adjustableEmitter = getSignalTravelTimeModel().getFieldAdjustableEmitterComputer(
                receptionDate.getField(), emitter);
        final T delay = adjustableEmitter.computeDelay(receptionCondition, approxEmissionDate);
        final FieldAbsoluteDate<T> emissionDate = receptionDate.shiftedBy(delay.negate());
        final TimeStampedFieldPVCoordinates<T> emitterPV = emitter.getPVCoordinates(emissionDate, receptionCondition.getReferenceFrame());
        final FieldVector3D<T> relativePosition = receptionCondition.getReceiverPosition().subtract(emitterPV.getPosition());
        final FieldVector3D<T> relativeVelocity = receiverVelocity.subtract(emitterPV.getVelocity());
        return FieldVector3D.dotProduct(relativeVelocity, relativePosition.normalize());
    }
}
