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
import org.orekit.estimation.measurements.signal.SignalTravelTimeModel;
import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.FieldPVCoordinatesProvider;
import org.orekit.utils.PVCoordinatesProvider;

/**
 * Abstract class for angular measurement model.
 * @since 14.0
 * @author Romain Serra
 */
public abstract class AbstractAngularMeasurementModel {

    /** Signal travel time model. */
    private final SignalTravelTimeModel signalTravelTimeModel;

    /**
     * Constructor.
     * @param signalTravelTimeModel time delay computer
     */
    protected AbstractAngularMeasurementModel(final SignalTravelTimeModel signalTravelTimeModel) {
        this.signalTravelTimeModel = signalTravelTimeModel;
    }

    /**
     * Getter for signal travel time model.
     * @return model
     */
    public SignalTravelTimeModel getSignalTravelTimeModel() {
        return signalTravelTimeModel;
    }

    /**
     * Compute emitter-to-receiver vector.
     * @param frame frame where receiver position is given
     * @param receiverPosition receiver position in input frame at reception time
     * @param receptionDate signal reception date
     * @param emitter signal emitter coordinates provider
     * @param approxEmissionDate guess for the emission date (shall be adjusted by signal travel time computer)
     * @return emitter-to-receiver vector
     */
    protected Vector3D getEmitterToReceiverVector(final Frame frame, final Vector3D receiverPosition,
                                                  final AbsoluteDate receptionDate, final PVCoordinatesProvider emitter,
                                                  final AbsoluteDate approxEmissionDate) {
        // Refine time delay
        final double signalTravelTime = signalTravelTimeModel.getAdjustableEmitterComputer(emitter)
                .computeDelay(approxEmissionDate, receiverPosition, receptionDate, frame);
        final AbsoluteDate emissionDate = receptionDate.shiftedBy(-signalTravelTime);

        final Vector3D observedPosition = emitter.getPosition(emissionDate, frame);
        return observedPosition.subtract(receiverPosition).normalize();
    }

    /**
     * Compute emitter-to-receiver vector with FIeld.
     * @param <T> field type
     * @param frame frame where receiver position is given
     * @param receiverPosition receiver position in input frame at reception time
     * @param receptionDate signal reception date
     * @param emitter signal emitter coordinates provider
     * @param approxEmissionDate guess for the emission date (shall be adjusted by signal travel time computer)
     * @return emitter-to-receiver vector
     */
    protected <T extends CalculusFieldElement<T>> FieldVector3D<T> getEmitterToReceiverVector(final Frame frame, final FieldVector3D<T> receiverPosition,
                                                                                              final FieldAbsoluteDate<T> receptionDate,
                                                                                              final FieldPVCoordinatesProvider<T> emitter,
                                                                                              final FieldAbsoluteDate<T> approxEmissionDate) {
        // Refine time delay
        final T signalTravelTime = signalTravelTimeModel.getFieldAdjustableEmitterComputer(receptionDate.getField(), emitter)
                .computeDelay(approxEmissionDate, receiverPosition, receptionDate, frame);
        final FieldAbsoluteDate<T> emissionDate = receptionDate.shiftedBy(signalTravelTime.negate());

        final FieldVector3D<T> observedPosition = emitter.getPosition(emissionDate, frame);
        return observedPosition.subtract(receiverPosition);
    }
}
