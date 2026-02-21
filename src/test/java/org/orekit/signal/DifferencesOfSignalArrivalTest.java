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

class DifferencesOfSignalArrivalTest {

    @Test
    void testComputeNoVelocity() {
        // GIVEN
        final Frame frame = FramesFactory.getGCRF();
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        final DifferencesOfSignalArrival differencesOfSignalArrival = new DifferencesOfSignalArrival(new SignalTravelTimeModel());
        final Vector3D receiverPosition = Vector3D.ZERO;
        final PVCoordinates pvSecondary = new PVCoordinates(new Vector3D(1e5, 1e6, -1e4));
        final PVCoordinates pvEmitter = new PVCoordinates(new Vector3D(2e5, -3e4, 1e5));
        final AbsolutePVCoordinates secondary = new AbsolutePVCoordinates(frame, date, pvSecondary);
        final AbsolutePVCoordinates emitter = new AbsolutePVCoordinates(frame, date, pvEmitter);
        // WHEN
        final double[] delays = differencesOfSignalArrival.computeDelays(frame, receiverPosition, date, secondary,
                emitter);
        // THEN
        assertEquals(pvSecondary.getPosition().subtract(pvEmitter.getPosition()).getNorm2(), Constants.SPEED_OF_LIGHT * delays[1], 1e-9);
        assertEquals(receiverPosition.subtract(pvEmitter.getPosition()).getNorm2(), Constants.SPEED_OF_LIGHT * delays[0], 1e-9);
    }

    @Test
    void testComputeGuess() {
        // GIVEN
        final Frame frame = FramesFactory.getGCRF();
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        final DifferencesOfSignalArrival differencesOfSignalArrival = new DifferencesOfSignalArrival(new SignalTravelTimeModel());
        final Vector3D receiverPosition = Vector3D.ZERO;
        final PVCoordinates pvSecondary = new PVCoordinates(new Vector3D(1e5, 1e6, -1e4));
        final PVCoordinates pvEmitter = new PVCoordinates(new Vector3D(2e5, -3e4, 1e5));
        final AbsolutePVCoordinates secondary = new AbsolutePVCoordinates(frame, date, pvSecondary);
        final AbsolutePVCoordinates emitter = new AbsolutePVCoordinates(frame, date, pvEmitter);
        // WHEN
        final double[] delays = differencesOfSignalArrival.computeDelays(frame, receiverPosition, date, secondary, emitter);
        // THEN
        final double[] expectedDelays = differencesOfSignalArrival.computeDelays(frame, receiverPosition, date, secondary, date,
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
        final DifferencesOfSignalArrival differencesOfSignalArrival = new DifferencesOfSignalArrival(new SignalTravelTimeModel());
        final Vector3D receiverPosition = Vector3D.PLUS_I.scalarMultiply(position);
        final PVCoordinates pvSecondary = new PVCoordinates(new Vector3D(1e5, 1e6, -1e4), Vector3D.MINUS_J);
        final PVCoordinates pvEmitter = new PVCoordinates(new Vector3D(2e5, -3e4, 1e5), new Vector3D(1, 2, 3));
        final AbsolutePVCoordinates secondary = new AbsolutePVCoordinates(frame, date, pvSecondary);
        final AbsolutePVCoordinates emitter = new AbsolutePVCoordinates(frame, date, pvEmitter);
        final GradientField field = GradientField.getField(0);
        final FieldAbsoluteDate<Gradient> fieldDate = new FieldAbsoluteDate<>(field, date);
        // WHEN
        final Gradient[] fieldDelays = differencesOfSignalArrival.computeDelays(frame, new FieldVector3D<>(field, receiverPosition),
                fieldDate, new FieldAbsolutePVCoordinates<>(field, secondary), new FieldAbsolutePVCoordinates<>(field, emitter));
        // THEN
        final double[] delays = differencesOfSignalArrival.computeDelays(frame, receiverPosition, date, secondary, emitter);
        assertEquals(delays[0], fieldDelays[0].getValue());
        assertEquals(delays[1], fieldDelays[1].getValue());
    }
}
