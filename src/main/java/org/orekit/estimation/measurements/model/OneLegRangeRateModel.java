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
import org.orekit.frames.Frame;
import org.orekit.signal.FieldSignalTravelTimeAdjustableEmitter;
import org.orekit.signal.SignalTravelTimeAdjustableEmitter;
import org.orekit.signal.SignalTravelTimeModel;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.FieldPVCoordinatesProvider;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.PVCoordinatesProvider;
import org.orekit.utils.TimeStampedFieldPVCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;

/**
 * Class for one-leg range rate (a.k.a. Doppler measurement).
 * A signal is transmitted and received. There is no further assumption.
 * @since 14.0
 * @author Romain Serra
 */
public class OneLegRangeRateModel {

    /** Signal travel time model. */
    private final SignalTravelTimeModel signalTravelTimeModel;

    /**
     * Constructor.
     * @param signalTravelTimeModel signal travel time model
     */
    public OneLegRangeRateModel(final SignalTravelTimeModel signalTravelTimeModel) {
        this.signalTravelTimeModel = signalTravelTimeModel;
    }

    /**
     * Compute measurement without guess.
     *
     * @param frame            frame where position is given
     * @param receiverPV       receiver position-velocity (at reception)
     * @param receptionDate    signal reception date
     * @param emitter          signal initial emitter coordinates provider
     * @return ranges on both legs in chronological order (m)
     */
    public double value(final Frame frame, final PVCoordinates receiverPV, final AbsoluteDate receptionDate,
                        final PVCoordinatesProvider emitter) {
        return value(frame, receiverPV, receptionDate, emitter, receptionDate);
    }

    /**
     * Compute measurement.
     *
     * @param frame            frame where position is given
     * @param receiverPV       receiver position-velocity (at reception)
     * @param receptionDate    signal reception date
     * @param emitter          signal initial emitter coordinates provider
     * @param approxEmissionDate guess for the emission date
     * @return ranges on both legs in chronological order (m)
     */
    public double value(final Frame frame, final PVCoordinates receiverPV, final AbsoluteDate receptionDate,
                        final PVCoordinatesProvider emitter, final AbsoluteDate approxEmissionDate) {
        final SignalTravelTimeAdjustableEmitter adjustableEmitter = signalTravelTimeModel.getAdjustableEmitterComputer(emitter);
        final double delay = adjustableEmitter.computeDelay(approxEmissionDate, receiverPV.getPosition(), receptionDate, frame);
        final AbsoluteDate emissionDate = receptionDate.shiftedBy(-delay);
        final TimeStampedPVCoordinates emitterPV = emitter.getPVCoordinates(emissionDate, frame);
        final Vector3D relativePosition = receiverPV.getPosition().subtract(emitterPV.getPosition());
        final Vector3D relativeVelocity = receiverPV.getVelocity().subtract(emitterPV.getVelocity());
        return Vector3D.dotProduct(relativeVelocity, relativePosition.normalize());
    }

    /**
     * Compute measurement without guess.
     * @param <T> field type
     * @param frame            frame where position is given
     * @param receiverPV       receiver position and velocity (at reception)
     * @param receptionDate    signal reception date
     * @param emitter          signal initial emitter coordinates provider
     * @return range rate (m/s)
     */
    public <T extends CalculusFieldElement<T>> T value(final Frame frame, final FieldPVCoordinates<T> receiverPV,
                                                       final FieldAbsoluteDate<T> receptionDate,
                                                       final FieldPVCoordinatesProvider<T> emitter) {
        return value(frame, receiverPV, receptionDate, emitter, receptionDate);
    }
    /**
     * Compute measurement.
     * @param <T> field type
     * @param frame            frame where position is given
     * @param receiverPV       receiver position and velocity (at reception)
     * @param receptionDate    signal reception date
     * @param emitter          signal initial emitter coordinates provider
     * @param approxEmissionDate guess for the emission date
     * @return range rate (m/s)
     */
    public <T extends CalculusFieldElement<T>> T value(final Frame frame, final FieldPVCoordinates<T> receiverPV,
                                                       final FieldAbsoluteDate<T> receptionDate,
                                                       final FieldPVCoordinatesProvider<T> emitter,
                                                       final FieldAbsoluteDate<T> approxEmissionDate) {
        final FieldSignalTravelTimeAdjustableEmitter<T> adjustableEmitter = signalTravelTimeModel.getFieldAdjustableEmitterComputer(
                receptionDate.getField(), emitter);
        final T delay = adjustableEmitter.computeDelay(approxEmissionDate, receiverPV.getPosition(), receptionDate, frame);
        final FieldAbsoluteDate<T> emissionDate = receptionDate.shiftedBy(delay.negate());
        final TimeStampedFieldPVCoordinates<T> emitterPV = emitter.getPVCoordinates(emissionDate, frame);
        final FieldVector3D<T> relativePosition = receiverPV.getPosition().subtract(emitterPV.getPosition());
        final FieldVector3D<T> relativeVelocity = receiverPV.getVelocity().subtract(emitterPV.getVelocity());
        return FieldVector3D.dotProduct(relativeVelocity, relativePosition.normalize());
    }
}
