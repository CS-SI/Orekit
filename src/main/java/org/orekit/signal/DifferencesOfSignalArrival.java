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
package org.orekit.signal;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.MathArrays;
import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.FieldPVCoordinatesProvider;
import org.orekit.utils.PVCoordinatesProvider;

/**
 * Class for differences of arrival.
 * A signal is emitted and received by two different receivers: the primary one defining reference reception time and the secondary one.
 * @since 14.0
 * @author Romain Serra
 */
public class DifferencesOfSignalArrival {

    /** Signal travel time model. */
    private final SignalTravelTimeModel signalTravelTimeModel;

    /**
     * Constructor.
     * @param signalTravelTimeModel time delay computer
     */
    public DifferencesOfSignalArrival(final SignalTravelTimeModel signalTravelTimeModel) {
        this.signalTravelTimeModel = signalTravelTimeModel;
    }

    /**
     * Compute (positive) delays from emission.
     *
     * @param primaryReceptionCondition signal reception conditions at primary observer
     * @param secondaryReceiver signal secondary receiver coordinates provider
     * @param approxSecondaryReception guess for the secondary reception date
     * @param emitter          signal initial emitter coordinates provider
     * @param approxEmissionDate guess for the emission date
     * @return delays to primary and secondary respectively (s)
     */
    public double[] computeDelays(final SignalReceptionCondition primaryReceptionCondition,
                                  final PVCoordinatesProvider secondaryReceiver, final AbsoluteDate approxSecondaryReception,
                                  final PVCoordinatesProvider emitter, final AbsoluteDate approxEmissionDate) {
        final double primaryDelay = signalTravelTimeModel.getAdjustableEmitterComputer(emitter)
                .computeDelay(primaryReceptionCondition, approxEmissionDate);
        final AbsoluteDate emissionDate = primaryReceptionCondition.receptionDate().shiftedBy(-primaryDelay);
        final AdjustableReceiverSignalTimer adjustableReceiverSignalTimer = signalTravelTimeModel
                .getAdjustableReceiverComputer(secondaryReceiver);
        final Frame frame = primaryReceptionCondition.referenceFrame();
        final Vector3D emissionPosition = emitter.getPosition(emissionDate, frame);
        final SignalEmissionCondition emissionCondition = new SignalEmissionCondition(emissionDate, emissionPosition,
                frame);
        final double secondaryDelay = adjustableReceiverSignalTimer.computeDelay(emissionCondition,
                approxSecondaryReception);
        return new double[] { primaryDelay, secondaryDelay };
    }

    /**
     * Compute (positive) delays from emission without guesses.
     *
     * @param primaryReceptionCondition signal reception conditions at primary observer
     * @param secondaryReceiver signal secondary receiver coordinates provider
     * @param emitter          signal initial emitter coordinates provider
     * @return delays to primary and secondary respectively (s)
     */
    public double[] computeDelays(final SignalReceptionCondition primaryReceptionCondition,
                                  final PVCoordinatesProvider secondaryReceiver,
                                  final PVCoordinatesProvider emitter) {
        final double primaryDelay = signalTravelTimeModel.getAdjustableEmitterComputer(emitter)
                .computeDelay(primaryReceptionCondition);
        final AbsoluteDate emissionDate = primaryReceptionCondition.receptionDate().shiftedBy(-primaryDelay);
        final AdjustableReceiverSignalTimer adjustableReceiverSignalTimer = signalTravelTimeModel
                .getAdjustableReceiverComputer(secondaryReceiver);
        final Frame frame = primaryReceptionCondition.referenceFrame();
        final Vector3D emissionPosition = emitter.getPosition(emissionDate, frame);
        final SignalEmissionCondition emissionCondition = new SignalEmissionCondition(emissionDate, emissionPosition,
                frame);
        final double secondaryDelay = adjustableReceiverSignalTimer.computeDelay(emissionCondition);
        return new double[] { primaryDelay, secondaryDelay };
    }

    /**
     * Compute (positive) delays from emission.
     * @param <T> field type
     * @param primaryReceptionCondition signal reception conditions at primary observer
     * @param secondaryReceiver signal secondary receiver coordinates provider
     * @param approxSecondaryReception guess for the secondary reception date
     * @param emitter          signal initial emitter coordinates provider
     * @param approxEmissionDate guess for the emission date
     * @return delays to primary and secondary respectively (s)
     */
    public <T extends CalculusFieldElement<T>> T[] computeDelays(final FieldSignalReceptionCondition<T> primaryReceptionCondition,
                                                                 final FieldPVCoordinatesProvider<T> secondaryReceiver,
                                                                 final FieldAbsoluteDate<T> approxSecondaryReception,
                                                                 final FieldPVCoordinatesProvider<T> emitter,
                                                                 final FieldAbsoluteDate<T> approxEmissionDate) {
        final FieldAbsoluteDate<T> receptionDate = primaryReceptionCondition.receptionDate();
        final Field<T> field = receptionDate.getField();
        final Frame frame = primaryReceptionCondition.referenceFrame();
        final T primaryDelay = signalTravelTimeModel.getFieldAdjustableEmitterComputer(field, emitter)
            .computeDelay(primaryReceptionCondition, approxEmissionDate);
        final FieldAbsoluteDate<T> emissionDate = receptionDate.shiftedBy(primaryDelay.negate());
        final FieldAdjustableReceiverSignalTimer<T> signalTravelTimeAdjustableReceiver = signalTravelTimeModel
                .getFieldAdjustableReceiverComputer(field, secondaryReceiver);
        final FieldVector3D<T> emissionPosition = emitter.getPosition(emissionDate, frame);
        final FieldSignalEmissionCondition<T> emissionCondition = new FieldSignalEmissionCondition<>(emissionDate,
                emissionPosition, frame);
        final T secondaryDelay = signalTravelTimeAdjustableReceiver.computeDelay(emissionCondition, approxSecondaryReception);
        final T[] output = MathArrays.buildArray(field, 2);
        output[0] = primaryDelay;
        output[1] = secondaryDelay;
        return output;
    }

    /**
     * Compute (positive) delays from emission without guesses.
     * @param <T> field type
     * @param primaryReceptionCondition signal reception conditions at primary observer
     * @param secondaryReceiver signal secondary receiver coordinates provider
     * @param emitter          signal initial emitter coordinates provider
     * @return delays to primary and secondary respectively (s)
     */
    public <T extends CalculusFieldElement<T>> T[] computeDelays(final FieldSignalReceptionCondition<T> primaryReceptionCondition,
                                                                 final FieldPVCoordinatesProvider<T> secondaryReceiver,
                                                                 final FieldPVCoordinatesProvider<T> emitter) {
        final FieldAbsoluteDate<T> receptionDate = primaryReceptionCondition.receptionDate();
        final Frame frame = primaryReceptionCondition.referenceFrame();
        final Field<T> field = receptionDate.getField();
        final T primaryDelay = signalTravelTimeModel.getFieldAdjustableEmitterComputer(field, emitter)
                .computeDelay(primaryReceptionCondition);
        final FieldAbsoluteDate<T> emissionDate = receptionDate.shiftedBy(primaryDelay.negate());
        final FieldAdjustableReceiverSignalTimer<T> signalTravelTimeAdjustableReceiver = signalTravelTimeModel
                .getFieldAdjustableReceiverComputer(field, secondaryReceiver);
        final FieldVector3D<T> emissionPosition = emitter.getPosition(emissionDate, frame);
        final FieldSignalEmissionCondition<T> emissionCondition = new FieldSignalEmissionCondition<>(emissionDate,
                emissionPosition, frame);
        final T secondaryDelay = signalTravelTimeAdjustableReceiver.computeDelay(emissionCondition);
        final T[] output = MathArrays.buildArray(field, 2);
        output[0] = primaryDelay;
        output[1] = secondaryDelay;
        return output;
    }
}
