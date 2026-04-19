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
import org.hipparchus.analysis.solvers.AllowedSolution;
import org.hipparchus.analysis.solvers.BracketedRealFieldUnivariateSolver;
import org.hipparchus.analysis.solvers.FieldBracketingNthOrderBrentSolver;
import org.hipparchus.complex.Complex;
import org.hipparchus.complex.ComplexField;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.optim.ConvergenceChecker;
import org.hipparchus.util.Binary64;
import org.hipparchus.util.Binary64Field;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.orekit.TestUtils;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.KeplerianExtendedPositionProvider;
import org.orekit.orbits.Orbit;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.FieldAbsolutePVCoordinates;
import org.orekit.utils.FieldPVCoordinates;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class FieldAdjustableEmitterSignalTimerTest {

    @Test
    void testGetter() {
        // GIVEN
        final ConvergenceChecker<Binary64> convergenceChecker = (iteration, previous, current) -> true;
        // WHEN
        final FieldAdjustableEmitterSignalTimer<Binary64> emitter = new FieldAdjustableEmitterSignalTimer<>(null, convergenceChecker);
        // THEN
        assertEquals(convergenceChecker, emitter.getConvergenceChecker());
    }

    @Test
    void testComputeDelayVersusReverse() {
        // GIVEN
        final Frame frame = FramesFactory.getGCRF();
        final ComplexField field = ComplexField.getInstance();
        final FieldAbsoluteDate<Complex> receptionDate = FieldAbsoluteDate.getArbitraryEpoch(field);
        final Vector3D emitterPositionDouble = Vector3D.MINUS_I.scalarMultiply(1e5);
        final FieldVector3D<Complex> emitterPosition = new FieldVector3D<>(field, emitterPositionDouble).scalarMultiply(Complex.I);
        final FieldPVCoordinates<Complex> fieldPVCoordinates = new FieldPVCoordinates<>(emitterPosition, FieldVector3D.getZero(field));
        final FieldAbsolutePVCoordinates<Complex> absolutePVCoordinates = new FieldAbsolutePVCoordinates<>(frame, receptionDate, fieldPVCoordinates);
        final Vector3D receiverPositionDouble = new Vector3D(1e2, 1e3, 1e4);
        final FieldVector3D<Complex> receiverPosition = new FieldVector3D<>(field, receiverPositionDouble);
        final FieldAdjustableEmitterSignalTimer<Complex> signalTimeOfFlight = new FieldAdjustableEmitterSignalTimer<>(absolutePVCoordinates);
        final FieldSignalReceptionCondition<Complex> receptionCondition = new FieldSignalReceptionCondition<>(receptionDate, receiverPosition, frame);
        // WHEN
        final Complex actual = signalTimeOfFlight.computeDelay(receptionCondition);
        // THEN
        final FieldAbsolutePVCoordinates<Complex> reversed = new FieldAbsolutePVCoordinates<>(frame, receptionDate,
                new FieldPVCoordinates<>(receiverPosition, FieldVector3D.getZero(field)));
        final FieldAdjustableReceiverSignalTimer<Complex> signalTimeOfFlightAdjustableReceiver = new FieldAdjustableReceiverSignalTimer<>(reversed);
        final FieldAbsoluteDate<Complex> emissionDate = receptionDate.shiftedBy(actual.negate());
        final Complex expected = signalTimeOfFlightAdjustableReceiver.computeDelay(new FieldSignalEmissionCondition<>(emissionDate,
                emitterPosition, frame));
        assertEquals(expected, actual);
    }

    @Test
    void testComputeDelay() {
        // GIVEN
        final Orbit orbit = TestUtils.getDefaultOrbit(AbsoluteDate.ARBITRARY_EPOCH);
        final KeplerianExtendedPositionProvider positionProvider = new KeplerianExtendedPositionProvider(orbit);
        final Binary64Field field = Binary64Field.getInstance();
        final FieldAdjustableEmitterSignalTimer<Binary64> fieldComputer = new FieldAdjustableEmitterSignalTimer<>(positionProvider.toFieldPVCoordinatesProvider(field));
        final FieldAbsoluteDate<Binary64> fieldDate = FieldAbsoluteDate.getArbitraryEpoch(field);
        final Vector3D receiver = new Vector3D(-1e3, 2e2, 2e4);
        final FieldVector3D<Binary64> fieldReceiver = new FieldVector3D<>(field, receiver);
        final FieldSignalReceptionCondition<Binary64> fieldCondition = new FieldSignalReceptionCondition<>(fieldDate, fieldReceiver, orbit.getFrame());
        // WHEN
        final Binary64 actual = fieldComputer.computeDelay(fieldCondition);
        // THEN
        final SignalReceptionCondition receptionCondition = new SignalReceptionCondition(fieldDate.toAbsoluteDate(),
                receiver, orbit.getFrame());
        final double expected = new AdjustableEmitterSignalTimer(positionProvider).computeDelay(receptionCondition);
        assertEquals(expected, actual.getReal());
    }

    @ParameterizedTest
    @ValueSource(doubles = {-1e2, 0., 1e3, 1e5})
    void testComputeDelayVersusBrent(final double speed) {
        // GIVEN
        final Frame frame = FramesFactory.getGCRF();
        final GradientField field = GradientField.getField(1);
        final FieldAbsoluteDate<Gradient> emissionDate = FieldAbsoluteDate.getArbitraryEpoch(field);
        final FieldVector3D<Gradient> receiverPosition = new FieldVector3D<>(field, new Vector3D(-1e4, 1e2, -1e3));
        final FieldPVCoordinates<Gradient> emitterrPV = new FieldPVCoordinates<>(FieldVector3D.getMinusI(field)
                .scalarMultiply(new Gradient(0., 1)), FieldVector3D.getPlusK(field).scalarMultiply(speed));
        final FieldAbsolutePVCoordinates<Gradient> absolutePVCoordinates = new FieldAbsolutePVCoordinates<>(frame, emissionDate, emitterrPV);
        final FieldAdjustableEmitterSignalTimer<Gradient> signalTimeOfFlight = new FieldAdjustableEmitterSignalTimer<>(absolutePVCoordinates);
        final FieldSignalReceptionCondition<Gradient> receptionCondition = new FieldSignalReceptionCondition<>(emissionDate, receiverPosition, frame);
        // WHEN
        final Gradient actual = signalTimeOfFlight.computeDelay(receptionCondition);
        // THEN
        final BracketedRealFieldUnivariateSolver<Gradient> solver = new FieldBracketingNthOrderBrentSolver<>(new Gradient(1.0e-15, 0.),
                new Gradient(1e-12, 0.), new Gradient(1e-20, 0.), 5);
        final Gradient expected = solver.solve(1000, x -> {
            final Gradient d = FieldVector3D.distance(receiverPosition, absolutePVCoordinates.getPosition(emissionDate.shiftedBy(x.negate()), frame));
            return d.subtract(x.multiply(Constants.SPEED_OF_LIGHT));
        }, field.getOne().negate(), field.getOne(), AllowedSolution.ANY_SIDE);
        assertEquals(expected.getValue(), actual.getValue(), 1e-15);
        assertArrayEquals(expected.getGradient(), actual.getGradient(), 1e-20);
    }

    @Test
    void testComputeDelayWithGuess() {
        // GIVEN
        final Orbit orbit = TestUtils.getDefaultOrbit(AbsoluteDate.ARBITRARY_EPOCH);
        final KeplerianExtendedPositionProvider positionProvider = new KeplerianExtendedPositionProvider(orbit);
        final Binary64Field field = Binary64Field.getInstance();
        final FieldAdjustableEmitterSignalTimer<Binary64> fieldComputer = new FieldAdjustableEmitterSignalTimer<>(positionProvider.toFieldPVCoordinatesProvider(field));
        final FieldAbsoluteDate<Binary64> fieldDate = FieldAbsoluteDate.getArbitraryEpoch(field);
        final FieldAbsoluteDate<Binary64> guessDate = fieldDate.shiftedBy(1);
        final Vector3D receiver = new Vector3D(1e3, 2e4, 0);
        final FieldVector3D<Binary64> fieldReceiver = new FieldVector3D<>(field, receiver);
        final FieldSignalReceptionCondition<Binary64> fieldCondition = new FieldSignalReceptionCondition<>(fieldDate, fieldReceiver, orbit.getFrame());
        // WHEN
        final Binary64 actual = fieldComputer.computeDelay(fieldCondition, guessDate);
        // THEN
        final SignalReceptionCondition receptionCondition = new SignalReceptionCondition(fieldDate.toAbsoluteDate(),
                receiver, orbit.getFrame());
        final double expected = new AdjustableEmitterSignalTimer(positionProvider).computeDelay(receptionCondition,
                guessDate.toAbsoluteDate());
        assertEquals(expected, actual.getReal());
    }

}
