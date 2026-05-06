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
import org.hipparchus.Field;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.signal.AdjustableEmitterSignalTimer;
import org.orekit.signal.FieldAdjustableEmitterSignalTimer;
import org.orekit.signal.FieldSignalEmissionCondition;
import org.orekit.signal.FieldSignalReceptionCondition;
import org.orekit.signal.SignalEmissionCondition;
import org.orekit.signal.SignalReceptionCondition;
import org.orekit.signal.SignalTravelTimeModel;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.FieldPVCoordinatesProvider;
import org.orekit.utils.PVCoordinatesProvider;

/**
 * Abstract class for angular measurement model.
 * @since 14.0
 * @author Romain Serra
 */
public abstract class AbstractAngularMeasurementModel extends AbstractSignalBasedModel {

    /**
     * Constructor.
     * @param signalTravelTimeModel time delay computer
     */
    protected AbstractAngularMeasurementModel(final SignalTravelTimeModel signalTravelTimeModel) {
        super(signalTravelTimeModel);
    }

    /**
     * Compute emitter-to-receiver vector.
     * @param receptionCondition signal reception conditions
     * @param emitter signal emitter coordinates provider
     * @param approxEmissionDate guess for the emission date (shall be adjusted by signal travel time computer)
     * @return emitter-to-receiver vector
     */
    protected Vector3D getEmitterToReceiverVector(final SignalReceptionCondition receptionCondition,
                                                  final PVCoordinatesProvider emitter,
                                                  final AbsoluteDate approxEmissionDate) {
        final AdjustableEmitterSignalTimer signalTravelTime = getSignalTravelTimeModel().getAdjustableEmitterComputer(emitter);
        final SignalEmissionCondition emissionCondition = signalTravelTime.computeEmissionCondition(receptionCondition,
                approxEmissionDate);
        final Vector3D observedPosition = emissionCondition.getEmitterPosition();
        return observedPosition.subtract(receptionCondition.getReceiverPosition()).normalize();
    }

    /**
     * Compute emitter-to-receiver vector with FIeld.
     * @param <T> field type
     * @param receptionCondition signal reception conditions
     * @param emitter signal emitter coordinates provider
     * @param approxEmissionDate guess for the emission date (shall be adjusted by signal travel time computer)
     * @return emitter-to-receiver vector
     */
    protected <T extends CalculusFieldElement<T>> FieldVector3D<T> getEmitterToReceiverVector(final FieldSignalReceptionCondition<T> receptionCondition,
                                                                                              final FieldPVCoordinatesProvider<T> emitter,
                                                                                              final FieldAbsoluteDate<T> approxEmissionDate) {
        final Field<T> field = receptionCondition.getReceptionDate().getField();
        final FieldAdjustableEmitterSignalTimer<T> signalTravelTime = getSignalTravelTimeModel()
                .getFieldAdjustableEmitterComputer(field, emitter);
        final FieldSignalEmissionCondition<T> emissionCondition = signalTravelTime.computeEmissionCondition(receptionCondition,
                approxEmissionDate);
        final FieldVector3D<T> observedPosition = emissionCondition.getEmitterPosition();
        return observedPosition.subtract(receptionCondition.getReceiverPosition());
    }
}
