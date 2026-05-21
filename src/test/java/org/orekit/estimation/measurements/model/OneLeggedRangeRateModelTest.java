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

import org.hipparchus.analysis.differentiation.Gradient;
import org.hipparchus.analysis.differentiation.GradientField;
import org.hipparchus.analysis.differentiation.UnivariateDerivative2;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.signal.AdjustableEmitterSignalTimer;
import org.orekit.signal.FieldSignalReceptionCondition;
import org.orekit.signal.SignalReceptionCondition;
import org.orekit.signal.SignalTravelTimeModel;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.AbsolutePVCoordinates;
import org.orekit.utils.FieldAbsolutePVCoordinates;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.PVCoordinates;
import static org.junit.jupiter.api.Assertions.assertEquals;

class OneLeggedRangeRateModelTest {

    @Test
    void testValue() {
        // GIVEN
        final Frame frame = FramesFactory.getGCRF();
        final AbsoluteDate date = AbsoluteDate.ARBITRARY_EPOCH;
        final OneLeggedRangeRateModel rangeRateModel = new OneLeggedRangeRateModel(new SignalTravelTimeModel());
        final PVCoordinates pvEmitter = new PVCoordinates(new Vector3D(1e4, -1e3, 0.), new Vector3D(1, 2, 3));
        final AbsolutePVCoordinates emitter = new AbsolutePVCoordinates(frame, date, pvEmitter);
        final PVCoordinates receiverPV = new PVCoordinates();
        final SignalReceptionCondition receptionCondition = new SignalReceptionCondition(date, receiverPV.getPosition(), frame);
        // WHEN
        final double rangeRate = rangeRateModel.value(receptionCondition, receiverPV.getVelocity(), emitter);
        // THEN
        final FieldPVCoordinates<UnivariateDerivative2> fieldPV = pvEmitter.toUnivariateDerivative2PV();
        final double delay = new AdjustableEmitterSignalTimer(emitter).computeDelay(receptionCondition);
        assertEquals(fieldPV.shiftedBy(-delay).getPosition().getNorm2().getFirstDerivative(), rangeRate, 1e-12);
    }

    @Test
    void testValueRadial() {
        // GIVEN
        final Frame frame = FramesFactory.getGCRF();
        final AbsoluteDate date = AbsoluteDate.ARBITRARY_EPOCH;
        final OneLeggedRangeRateModel rangeRateModel = new OneLeggedRangeRateModel(new SignalTravelTimeModel());
        final PVCoordinates pvEmitter = new PVCoordinates(new Vector3D(1e5, 0., 0.), Vector3D.PLUS_I);
        final AbsolutePVCoordinates emitter = new AbsolutePVCoordinates(frame, date, pvEmitter);
        final PVCoordinates receiverPV = new PVCoordinates();
        final SignalReceptionCondition receptionCondition = new SignalReceptionCondition(date, receiverPV.getPosition(),
                frame);
        // WHEN
        final double rangeRate = rangeRateModel.value(receptionCondition, receiverPV.getVelocity(), emitter);
        // THEN
        assertEquals(1., rangeRate);
    }

    @Test
    void testValueNoVelocity() {
        // GIVEN
        final Frame frame = FramesFactory.getGCRF();
        final AbsoluteDate date = AbsoluteDate.ARBITRARY_EPOCH;
        final OneLeggedRangeRateModel rangeRateModel = new OneLeggedRangeRateModel(new SignalTravelTimeModel());
        final PVCoordinates pvEmitter = new PVCoordinates(new Vector3D(1e5, 0., 0.));
        final AbsolutePVCoordinates emitter = new AbsolutePVCoordinates(frame, date, pvEmitter);
        final PVCoordinates receiverPV = new PVCoordinates();
        final SignalReceptionCondition receptionCondition = new SignalReceptionCondition(date, receiverPV.getPosition(),
                frame);
        // WHEN
        final double rangeRate = rangeRateModel.value(receptionCondition, receiverPV.getVelocity(), emitter);
        // THEN
        assertEquals(0., rangeRate);
    }

    @Test
    void testValueGuess() {
        // GIVEN
        final Frame frame = FramesFactory.getGCRF();
        final AbsoluteDate date = AbsoluteDate.ARBITRARY_EPOCH;
        final OneLeggedRangeRateModel rangeRateModel = new OneLeggedRangeRateModel(new SignalTravelTimeModel());
        final PVCoordinates pvEmitter = new PVCoordinates(new Vector3D(1e5, 1e4,-1e6), Vector3D.PLUS_I);
        final AbsolutePVCoordinates emitter = new AbsolutePVCoordinates(frame, date, pvEmitter);
        final PVCoordinates receiverPV = new PVCoordinates();
        final SignalReceptionCondition receptionCondition = new SignalReceptionCondition(date, receiverPV.getPosition(),
                frame);
        // WHEN
        final double rangeRate = rangeRateModel.value(receptionCondition, receiverPV.getVelocity(), emitter, date);
        // THEN
        final double expected = rangeRateModel.value(receptionCondition, receiverPV.getVelocity(), emitter);
        assertEquals(expected, rangeRate);
    }

    @ParameterizedTest
    @ValueSource(doubles = {-1e3, 1e2, 1e4, 1e6})
    void testComputeField(final double position) {
        // GIVEN
        final Frame frame = FramesFactory.getGCRF();
        final AbsoluteDate date = AbsoluteDate.ARBITRARY_EPOCH;
        final Vector3D receiverPosition = Vector3D.PLUS_I.scalarMultiply(position);
        final PVCoordinates pvEmitter = new PVCoordinates(new Vector3D(2e5, -3e4, 1e5), new Vector3D(1, 2, 3));
        final AbsolutePVCoordinates emitter = new AbsolutePVCoordinates(frame, date, pvEmitter);
        final GradientField field = GradientField.getField(0);
        final FieldPVCoordinates<Gradient> receiverPV = new FieldPVCoordinates<>(field, new PVCoordinates(receiverPosition));
        final FieldAbsolutePVCoordinates<Gradient> fieldEmitter = new FieldAbsolutePVCoordinates<>(field, emitter);
        final FieldAbsoluteDate<Gradient> fieldDate = new FieldAbsoluteDate<>(field, date);
        final OneLeggedRangeRateModel rangeRateModel = new OneLeggedRangeRateModel(new SignalTravelTimeModel());
        final FieldSignalReceptionCondition<Gradient> receptionCondition = new FieldSignalReceptionCondition<>(fieldDate,
                receiverPV.getPosition(), frame);
        // WHEN
        final Gradient rateGradient = rangeRateModel.value(receptionCondition, receiverPV.getVelocity(), fieldEmitter);
        // THEN
        final double rate = rangeRateModel.value(new SignalReceptionCondition(date, receiverPosition, frame),
                receiverPV.getVelocity().toVector3D(), emitter);
        assertEquals(rate, rateGradient.getValue());
    }
}
