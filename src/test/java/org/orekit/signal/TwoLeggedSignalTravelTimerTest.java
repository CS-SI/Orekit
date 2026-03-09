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

import org.hipparchus.analysis.differentiation.Gradient;
import org.hipparchus.analysis.differentiation.GradientField;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.AbsolutePVCoordinates;
import org.orekit.utils.Constants;
import org.orekit.utils.FieldAbsolutePVCoordinates;
import org.orekit.utils.PVCoordinates;
import static org.junit.jupiter.api.Assertions.assertEquals;

class TwoLeggedSignalTravelTimerTest {

    @Test
    void testComputeNoVelocity() {
        // GIVEN
        final Frame frame = FramesFactory.getGCRF();
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        final TwoLeggedSignalTravelTimer twoLeggedSignalTravelTimer = new TwoLeggedSignalTravelTimer(new SignalTravelTimeModel());
        final Vector3D receiverPosition = Vector3D.ZERO;
        final PVCoordinates pvRelay = new PVCoordinates(new Vector3D(1e5, 1e6, -1e4));
        final PVCoordinates pvEmitter = new PVCoordinates(new Vector3D(2e5, -3e4, 1e5));
        final AbsolutePVCoordinates relay = new AbsolutePVCoordinates(frame, date, pvRelay);
        final AbsolutePVCoordinates emitter = new AbsolutePVCoordinates(frame, date, pvEmitter);
        final SignalReceptionCondition receptionCondition = new SignalReceptionCondition(date, receiverPosition, frame);
        // WHEN
        final double[] delays = twoLeggedSignalTravelTimer.computeDelays(receptionCondition, relay, emitter);
        // THEN
        assertEquals(pvRelay.getPosition().subtract(receiverPosition).getNorm2(), Constants.SPEED_OF_LIGHT * delays[1], 1e-9);
        assertEquals(pvRelay.getPosition().subtract(pvEmitter.getPosition()).getNorm2(), Constants.SPEED_OF_LIGHT * delays[0], 1e-9);
    }

    @Test
    void testComputeGuess() {
        // GIVEN
        final Frame frame = FramesFactory.getGCRF();
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        final TwoLeggedSignalTravelTimer twoLeggedSignalTravelTimer = new TwoLeggedSignalTravelTimer(new SignalTravelTimeModel());
        final Vector3D receiverPosition = Vector3D.ZERO;
        final PVCoordinates pvRelay = new PVCoordinates(new Vector3D(1e5, 1e6, -1e4));
        final PVCoordinates pvEmitter = new PVCoordinates(new Vector3D(2e5, -3e4, 1e5));
        final AbsolutePVCoordinates relay = new AbsolutePVCoordinates(frame, date, pvRelay);
        final AbsolutePVCoordinates emitter = new AbsolutePVCoordinates(frame, date, pvEmitter);
        final SignalReceptionCondition receptionCondition = new SignalReceptionCondition(date, receiverPosition, frame);
        // WHEN
        final double[] delays = twoLeggedSignalTravelTimer.computeDelays(receptionCondition, relay,
                emitter);
        // THEN
        final double[] expectedDelays = twoLeggedSignalTravelTimer.computeDelays(receptionCondition, relay, date,
                emitter, date);
        assertEquals(expectedDelays[0], delays[0]);
        assertEquals(expectedDelays[1], delays[1]);
    }

    @ParameterizedTest
    @ValueSource(doubles = {-1e3, 1e2, 1e4, 1e6})
    void testComputeField(final double position) {
        // GIVEN
        final Frame frame = FramesFactory.getGCRF();
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        final TwoLeggedSignalTravelTimer twoLeggedSignalTravelTimer = new TwoLeggedSignalTravelTimer(new SignalTravelTimeModel());
        final Vector3D receiverPosition = Vector3D.PLUS_I.scalarMultiply(position);
        final PVCoordinates pvRelay = new PVCoordinates(new Vector3D(1e5, 1e6, -1e4), Vector3D.MINUS_J);
        final PVCoordinates pvEmitter = new PVCoordinates(new Vector3D(2e5, -3e4, 1e5), new Vector3D(1, 2, 3));
        final AbsolutePVCoordinates relay = new AbsolutePVCoordinates(frame, date, pvRelay);
        final AbsolutePVCoordinates emitter = new AbsolutePVCoordinates(frame, date, pvEmitter);
        final GradientField field = GradientField.getField(0);
        final FieldAbsoluteDate<Gradient> fieldDate = new FieldAbsoluteDate<>(field, date);
        final FieldSignalReceptionCondition<Gradient> fieldCondition = new FieldSignalReceptionCondition<>(fieldDate,
                new FieldVector3D<>(field, receiverPosition), frame);
        // WHEN
        final Gradient[] fieldDelays = twoLeggedSignalTravelTimer.computeDelays(fieldCondition,
                new FieldAbsolutePVCoordinates<>(field, relay), new FieldAbsolutePVCoordinates<>(field, emitter));
        // THEN
        final SignalReceptionCondition receptionCondition = new SignalReceptionCondition(date, receiverPosition, frame);
        final double[] delays = twoLeggedSignalTravelTimer.computeDelays(receptionCondition, relay, emitter);
        assertEquals(delays[0], fieldDelays[0].getValue());
        assertEquals(delays[1], fieldDelays[1].getValue());
    }
}
