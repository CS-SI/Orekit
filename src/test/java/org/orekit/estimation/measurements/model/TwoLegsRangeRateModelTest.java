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
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.junit.jupiter.api.Test;
import org.orekit.estimation.measurements.signal.SignalTravelTimeModel;
import org.orekit.estimation.measurements.signal.TwoLegsSignalTravelTimer;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.AbsolutePVCoordinates;
import org.orekit.utils.FieldAbsolutePVCoordinates;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.PVCoordinates;
import static org.junit.jupiter.api.Assertions.assertEquals;

class TwoLegsRangeRateModelTest {

    @Test
    void testValueNoVelocity() {
        // GIVEN
        final Frame frame = FramesFactory.getGCRF();
        final AbsoluteDate date = AbsoluteDate.ARBITRARY_EPOCH;
        final TwoLegsSignalTravelTimer twoLegsSignalTravelTimer = new TwoLegsSignalTravelTimer(new SignalTravelTimeModel());
        final TwoLegsRangeRateModel rangeRateModel = new TwoLegsRangeRateModel(twoLegsSignalTravelTimer);
        final PVCoordinates pvRelay = new PVCoordinates(new Vector3D(1e5, 1e6, -1e4));
        final PVCoordinates pvEmitter = new PVCoordinates(new Vector3D(2e5, -3e4, 1e5));
        final AbsolutePVCoordinates relay = new AbsolutePVCoordinates(frame, date, pvRelay);
        final AbsolutePVCoordinates emitter = new AbsolutePVCoordinates(frame, date, pvEmitter);
        // WHEN
        final double rangeRate = rangeRateModel.value(frame, new PVCoordinates(), date, relay, emitter);
        // THEN
        assertEquals(0., rangeRate);
    }

    @Test
    void testValueRadialVelocity() {
        // GIVEN
        final Frame frame = FramesFactory.getGCRF();
        final AbsoluteDate date = AbsoluteDate.ARBITRARY_EPOCH;
        final TwoLegsSignalTravelTimer twoLegsSignalTravelTimer = new TwoLegsSignalTravelTimer(new SignalTravelTimeModel());
        final TwoLegsRangeRateModel rangeRateModel = new TwoLegsRangeRateModel(twoLegsSignalTravelTimer);
        final PVCoordinates pvRelay = new PVCoordinates(new Vector3D(1e5, 0., 0.), Vector3D.PLUS_I);
        final PVCoordinates pvEmitter = new PVCoordinates();
        final AbsolutePVCoordinates relay = new AbsolutePVCoordinates(frame, date, pvRelay);
        final AbsolutePVCoordinates emitter = new AbsolutePVCoordinates(frame, date, pvEmitter);
        // WHEN
        final double rangeRate = rangeRateModel.value(frame, pvEmitter, date, relay, emitter);
        // THEN
        assertEquals(pvRelay.getPosition().normalize().dotProduct(pvRelay.getVelocity()) * 2., rangeRate);
    }

    @Test
    void testValueGuess() {
        // GIVEN
        final Frame frame = FramesFactory.getGCRF();
        final AbsoluteDate date = AbsoluteDate.ARBITRARY_EPOCH;
        final TwoLegsSignalTravelTimer twoLegsSignalTravelTimer = new TwoLegsSignalTravelTimer(new SignalTravelTimeModel());
        final TwoLegsRangeRateModel rangeRateModel = new TwoLegsRangeRateModel(twoLegsSignalTravelTimer);
        final PVCoordinates pvRelay = new PVCoordinates(new Vector3D(1e5, 1e4,-1e6), Vector3D.PLUS_I);
        final PVCoordinates pvEmitter = new PVCoordinates();
        final AbsolutePVCoordinates relay = new AbsolutePVCoordinates(frame, date, pvRelay);
        final AbsolutePVCoordinates emitter = new AbsolutePVCoordinates(frame, date, pvEmitter);
        // WHEN
        final double rangeRate = rangeRateModel.value(frame, pvEmitter, date, relay, date, emitter, date);
        // THEN
        final double expected = rangeRateModel.value(frame, pvEmitter, date, relay, emitter);
        assertEquals(expected, rangeRate);
    }

    @Test
    void testValueField() {
        // GIVEN
        final Frame frame = FramesFactory.getGCRF();
        final AbsoluteDate date = AbsoluteDate.ARBITRARY_EPOCH;
        final TwoLegsSignalTravelTimer twoLegsSignalTravelTimer = new TwoLegsSignalTravelTimer(new SignalTravelTimeModel());
        final TwoLegsRangeRateModel rangeRateModel = new TwoLegsRangeRateModel(twoLegsSignalTravelTimer);
        final PVCoordinates receiverPV = new PVCoordinates(Vector3D.PLUS_I.scalarMultiply(1e4), Vector3D.MINUS_K);
        final PVCoordinates pvRelay = new PVCoordinates(new Vector3D(1e5, 1e6, -1e4), Vector3D.MINUS_J);
        final PVCoordinates pvEmitter = new PVCoordinates(new Vector3D(2e5, -3e4, 1e5), new Vector3D(1, 2, 3));
        final AbsolutePVCoordinates relay = new AbsolutePVCoordinates(frame, date, pvRelay);
        final AbsolutePVCoordinates emitter = new AbsolutePVCoordinates(frame, date, pvEmitter);
        final GradientField field = GradientField.getField(0);
        final FieldAbsoluteDate<Gradient> fieldDate = new FieldAbsoluteDate<>(field, date);
        // WHEN
        final Gradient fieldRangeRate = rangeRateModel.value(frame, new FieldPVCoordinates<>(field, receiverPV),
                fieldDate, new FieldAbsolutePVCoordinates<>(field, relay), new FieldAbsolutePVCoordinates<>(field, emitter));
        // THEN
        final double rangeRate = rangeRateModel.value(frame, receiverPV, date, relay, date, emitter, date);
        assertEquals(rangeRate, fieldRangeRate.getValue());
    }
}
