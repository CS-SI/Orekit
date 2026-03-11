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

import org.hipparchus.analysis.solvers.BracketingNthOrderBrentSolver;
import org.hipparchus.analysis.solvers.UnivariateSolver;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.optim.ConvergenceChecker;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.AbsolutePVCoordinates;
import org.orekit.utils.Constants;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.PVCoordinatesProvider;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class SignalTravelTimeAdjustableEmitterTest {

    @Test
    void testGetter() {
        // GIVEN
        final ConvergenceChecker<Double> convergenceChecker = (iteration, previous, current) -> true;
        // WHEN
        final SignalTravelTimeAdjustableEmitter emitter = new SignalTravelTimeAdjustableEmitter(mock(), convergenceChecker);
        // THEN
        assertEquals(convergenceChecker, emitter.getConvergenceChecker());
    }

    @Test
    void testComputeDelayInstantaneous() {
        // GIVEN
        final Frame frame = FramesFactory.getGCRF();
        final AbsoluteDate receptionDate = AbsoluteDate.ARBITRARY_EPOCH;
        final AbsolutePVCoordinates absolutePVCoordinates = new AbsolutePVCoordinates(frame, receptionDate,
                new PVCoordinates(Vector3D.ZERO, Vector3D.MINUS_J.scalarMultiply(1e4)));
        final Vector3D receiverPosition = new Vector3D(-1e2, 1e3, -1e4);
        final SignalTravelTimeAdjustableEmitter signalTimeOfFlight = new SignalTravelTimeAdjustableEmitter(absolutePVCoordinates,
                ((iteration, previous, current) -> FastMath.abs(previous - current) == 0.));
        final SignalReceptionCondition receptionCondition = new SignalReceptionCondition(receptionDate, receiverPosition,
                frame);
        // WHEN
        final double actual = signalTimeOfFlight.computeDelay(receptionCondition, receptionDate);
        // THEN
        final double expectedDelay = 0.;
        assertEquals(expectedDelay, actual);
    }

    @ParameterizedTest
    @ValueSource(doubles = {-1e2, 0., 1e3, 1e5})
    void testComputeDelayVersusBrent(final double speed) {
        // GIVEN
        final Frame frame = FramesFactory.getGCRF();
        final AbsoluteDate receptionDate = AbsoluteDate.ARBITRARY_EPOCH;
        final Vector3D emitterPosition = Vector3D.MINUS_I.scalarMultiply(1e5);
        final AbsolutePVCoordinates absolutePVCoordinates = new AbsolutePVCoordinates(frame, receptionDate,
                new PVCoordinates(emitterPosition, Vector3D.MINUS_J.scalarMultiply(speed), Vector3D.PLUS_J));
        final Vector3D receiverPosition = new Vector3D(-1e1, 1e2, -1e3);
        final SignalTravelTimeAdjustableEmitter signalTimeOfFlight = new SignalTravelTimeAdjustableEmitter(absolutePVCoordinates);
        final SignalReceptionCondition receptionCondition = new SignalReceptionCondition(receptionDate, receiverPosition, frame);
        // WHEN
        final double actual = signalTimeOfFlight.computeDelay(receptionCondition);
        // THEN
        final UnivariateSolver solver = new BracketingNthOrderBrentSolver(1.0e-12, 5);
        final double expected = solver.solve(1000, x -> {
            final double d = Vector3D.distance(receiverPosition, absolutePVCoordinates.getPosition(receptionDate.shiftedBy(-x), frame));
            return d - x * Constants.SPEED_OF_LIGHT;
        }, -1.0, 1.0);
        assertEquals(expected, actual, 1e-15);
    }

    @Test
    void testComputeDelayNonRegression() {
        // GIVEN
        final Frame frame = FramesFactory.getGCRF();
        final AbsoluteDate receptionDate = AbsoluteDate.ARBITRARY_EPOCH;
        final Vector3D emitterPosition = Vector3D.MINUS_I.scalarMultiply(1e5);
        final AbsolutePVCoordinates absolutePVCoordinates = new AbsolutePVCoordinates(frame, receptionDate,
                new PVCoordinates(emitterPosition, Vector3D.MINUS_J.scalarMultiply(1e4)));
        final Vector3D receiverPosition = new Vector3D(-1e2, 1e3, -1e4);
        final SignalTravelTimeAdjustableEmitter signalTimeOfFlight = new SignalTravelTimeAdjustableEmitter(absolutePVCoordinates);
        final SignalReceptionCondition receptionCondition = new SignalReceptionCondition(receptionDate, receiverPosition, frame);
        // WHEN
        final double actual = signalTimeOfFlight.computeDelay(receptionCondition);
        // THEN
        assertEquals(3.3491236019669596E-4, actual);
    }

    @Test
    void testComputeDelayVersusReverse() {
        // GIVEN
        final Frame frame = FramesFactory.getGCRF();
        final AbsoluteDate receptionDate = AbsoluteDate.ARBITRARY_EPOCH;
        final Vector3D emitterPosition = Vector3D.MINUS_I.scalarMultiply(1e5);
        final AbsolutePVCoordinates absolutePVCoordinates = new AbsolutePVCoordinates(frame, receptionDate, new PVCoordinates(emitterPosition));
        final Vector3D receiverPosition = new Vector3D(1e2, 1e3, 1e4);
        final SignalTravelTimeAdjustableEmitter signalTimeOfFlight = new SignalTravelTimeAdjustableEmitter(absolutePVCoordinates);
        final SignalReceptionCondition receptionCondition = new SignalReceptionCondition(receptionDate, receiverPosition, frame);
        // WHEN
        final double actual = signalTimeOfFlight.computeDelay(receptionCondition);
        // THEN
        final AbsolutePVCoordinates reversed = new AbsolutePVCoordinates(frame, receptionDate, new PVCoordinates(receiverPosition));
        final SignalTravelTimeAdjustableReceiver signalTravelTimeAdjustableReceiver = new SignalTravelTimeAdjustableReceiver(reversed);
        final AbsoluteDate emissionDate = receptionDate.shiftedBy(actual);
        final SignalEmissionCondition emissionCondition = new SignalEmissionCondition(emissionDate, emitterPosition, frame);
        final double expected = signalTravelTimeAdjustableReceiver.computeDelay(emissionCondition);
        assertEquals(expected, actual);
    }

    @Test
    void testComputeDelayStatic() {
        // GIVEN
        final Frame frame = FramesFactory.getGCRF();
        final AbsoluteDate receptionDate = AbsoluteDate.ARBITRARY_EPOCH;
        final AbsolutePVCoordinates absolutePVCoordinates = new AbsolutePVCoordinates(frame, receptionDate, new PVCoordinates());
        final Vector3D receiverPosition = new Vector3D(1e2, 1e3, 1e4);
        final SignalTravelTimeAdjustableEmitter signalTimeOfFlight = new SignalTravelTimeAdjustableEmitter(absolutePVCoordinates);
        final SignalReceptionCondition receptionCondition = new SignalReceptionCondition(receptionDate, receiverPosition, frame);
        // WHEN
        final double actual = signalTimeOfFlight.computeDelay(receptionCondition);
        // THEN
        final double expected = receiverPosition.getNorm() / Constants.SPEED_OF_LIGHT;
        assertEquals(expected, actual);
    }

    @ParameterizedTest
    @ValueSource(doubles = {-1e3, -1e1, 0., 1e1, 1e2, 1e4, 1e4})
    void testComputeDelay(final double speedFactor) {
        // GIVEN
        final Frame frame = FramesFactory.getGCRF();
        final AbsoluteDate receptionDate = AbsoluteDate.ARBITRARY_EPOCH;
        final PVCoordinates pvCoordinates = new PVCoordinates(Vector3D.MINUS_I, new Vector3D(1, -2, 3).scalarMultiply(speedFactor));
        final AbsolutePVCoordinates absolutePVCoordinates = new AbsolutePVCoordinates(frame, receptionDate, pvCoordinates);
        final Vector3D receiverPosition = new Vector3D(1e2, 1e3, 1e4);
        final SignalTravelTimeAdjustableEmitter signalTimeOfFlight = new SignalTravelTimeAdjustableEmitter(absolutePVCoordinates);
        final SignalReceptionCondition receptionCondition = new SignalReceptionCondition(receptionDate, receiverPosition, frame);
        // WHEN
        final double actual = signalTimeOfFlight.computeDelay(receptionCondition);
        // THEN
        final AbsoluteDate emittingDate = receptionDate.shiftedBy(-actual);
        final double expected = signalTimeOfFlight.computeDelay(receptionCondition, emittingDate);
        assertEquals(expected, actual);
    }

    @ParameterizedTest
    @ValueSource(doubles = {-1e4, -1e3, -1e1, 0., 1e1, 1e2, 1e3, 1e4})
    void testComputeDelayVersusSimple(final double speedFactor) {
        // GIVEN
        final Frame frame = FramesFactory.getGCRF();
        final AbsoluteDate receptionDate = AbsoluteDate.ARBITRARY_EPOCH;
        final PVCoordinates pvCoordinates = new PVCoordinates(new Vector3D(1e4, 1e6, -1e5), Vector3D.PLUS_J.scalarMultiply(speedFactor));
        final AbsolutePVCoordinates absolutePVCoordinates = new AbsolutePVCoordinates(frame, receptionDate, pvCoordinates);
        final Vector3D receiverPosition = Vector3D.ZERO;
        final SignalTravelTimeAdjustableEmitter signalTimeOfFlight = new SignalTravelTimeAdjustableEmitter(absolutePVCoordinates,
                ((iteration, previous, current) -> iteration >= 1));
        final SignalReceptionCondition receptionCondition = new SignalReceptionCondition(receptionDate, receiverPosition, frame);
        // WHEN
        final double actual = signalTimeOfFlight.computeDelay(receptionCondition);
        // THEN
        final double guess = pvCoordinates.getPosition().getNorm2() / Constants.SPEED_OF_LIGHT;
        final double expected = iterateOnce(receiverPosition, receptionDate, frame, absolutePVCoordinates, -guess);
        assertEquals(expected, actual, 1e-12);
    }

    protected double iterateOnce(final Vector3D position, final AbsoluteDate date, final Frame frame,
                                 final PVCoordinatesProvider pvCoordinatesProvider, final double travelTimeGuess) {
        final Vector3D shiftedEmitter = pvCoordinatesProvider.getPosition(date.shiftedBy(travelTimeGuess), frame);
        return shiftedEmitter.subtract(position).getNorm2() / Constants.SPEED_OF_LIGHT;
    }
}
