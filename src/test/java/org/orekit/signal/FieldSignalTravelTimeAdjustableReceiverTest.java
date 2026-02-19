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
import org.hipparchus.analysis.differentiation.UnivariateDerivative2;
import org.hipparchus.analysis.differentiation.UnivariateDerivative2Field;
import org.hipparchus.analysis.solvers.AllowedSolution;
import org.hipparchus.analysis.solvers.BracketedRealFieldUnivariateSolver;
import org.hipparchus.analysis.solvers.FieldBracketingNthOrderBrentSolver;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FieldSignalTravelTimeAdjustableReceiverTest {

    @Test
    void testConstructorDefaultConvergenceChecker() {
        // GIVEN
        final FieldSignalTravelTimeAdjustableReceiver<Binary64> adjustableReceiver = new FieldSignalTravelTimeAdjustableReceiver<>(null);
        // WHEN
        final ConvergenceChecker<Binary64> convergenceChecker = adjustableReceiver.getConvergenceChecker();
        final Binary64 convergedValue = Binary64.ZERO;
        // THEN
        assertFalse(convergenceChecker.converged(0, convergedValue, convergedValue));  // enforces at least one iteration
        assertTrue(convergenceChecker.converged(1, convergedValue, convergedValue));
    }

    @Test
    void testComputeDelay() {
        // GIVEN
        final Orbit orbit = TestUtils.getDefaultOrbit(AbsoluteDate.ARBITRARY_EPOCH);
        final KeplerianExtendedPositionProvider positionProvider = new KeplerianExtendedPositionProvider(orbit);
        final Binary64Field field = Binary64Field.getInstance();
        final FieldSignalTravelTimeAdjustableReceiver<Binary64> fieldComputer = new FieldSignalTravelTimeAdjustableReceiver<>(positionProvider.toFieldPVCoordinatesProvider(field));
        final FieldAbsoluteDate<Binary64> fieldDate = FieldAbsoluteDate.getArbitraryEpoch(field);
        final Vector3D receiver = new Vector3D(-1e3, 2e2, 1e4);
        final FieldVector3D<Binary64> fieldReceiver = new FieldVector3D<>(field, receiver);
        // WHEN
        final Binary64 actual = fieldComputer.computeDelay(fieldReceiver, fieldDate, orbit.getFrame());
        // THEN
        final double expected = new SignalTravelTimeAdjustableReceiver(positionProvider).computeDelay(receiver,
                fieldDate.toAbsoluteDate(), orbit.getFrame());
        assertEquals(expected, actual.getReal());
    }

    @ParameterizedTest
    @ValueSource(doubles = {-1e2, 0., 1e3, 1e5})
    void testComputeDelayVersusBrent(final double speed) {
        // GIVEN
        final Frame frame = FramesFactory.getGCRF();
        final GradientField field = GradientField.getField(1);
        final FieldAbsoluteDate<Gradient> emissionDate = FieldAbsoluteDate.getArbitraryEpoch(field);
        final FieldVector3D<Gradient> emitterPosition = new FieldVector3D<>(field, new Vector3D(-1e4, 1e2, -1e3));
        final FieldVector3D<Gradient> receiverPosition = FieldVector3D.getMinusI(field)
                .scalarMultiply(new Gradient(0., 1));
        final FieldPVCoordinates<Gradient> receiverPV = new FieldPVCoordinates<>(receiverPosition,
                FieldVector3D.getPlusK(field).scalarMultiply(speed), FieldVector3D.getPlusJ(field));
        final FieldAbsolutePVCoordinates<Gradient> absolutePVCoordinates = new FieldAbsolutePVCoordinates<>(frame, emissionDate, receiverPV);
        final FieldSignalTravelTimeAdjustableReceiver<Gradient> signalTimeOfFlight = new FieldSignalTravelTimeAdjustableReceiver<>(absolutePVCoordinates);
        // WHEN
        final Gradient actual = signalTimeOfFlight.computeDelay(emitterPosition, emissionDate, frame);
        // THEN
        final BracketedRealFieldUnivariateSolver<Gradient> solver = new FieldBracketingNthOrderBrentSolver<>(new Gradient(1.0e-15, 0.),
                new Gradient(1e-12, 0.), new Gradient(1e-20, 0.), 5);
        final Gradient expected = solver.solve(1000, x -> {
            final Gradient d = FieldVector3D.distance(emitterPosition, absolutePVCoordinates.getPosition(emissionDate.shiftedBy(x), frame));
            return d.subtract(x.multiply(Constants.SPEED_OF_LIGHT));
        }, field.getOne().negate(), field.getOne(), AllowedSolution.ANY_SIDE);
        assertEquals(expected.getValue(), actual.getValue(), 1e-15);
        assertArrayEquals(expected.getGradient(), actual.getGradient(), 1e-22);
    }

    @Test
    void testComputeDelayWithGuess() {
        // GIVEN
        final Orbit orbit = TestUtils.getDefaultOrbit(AbsoluteDate.ARBITRARY_EPOCH);
        final KeplerianExtendedPositionProvider positionProvider = new KeplerianExtendedPositionProvider(orbit);
        final Binary64Field field = Binary64Field.getInstance();
        final FieldSignalTravelTimeAdjustableReceiver<Binary64> fieldComputer = new FieldSignalTravelTimeAdjustableReceiver<>(positionProvider.toFieldPVCoordinatesProvider(field));
        final FieldAbsoluteDate<Binary64> fieldDate = FieldAbsoluteDate.getArbitraryEpoch(field);
        final FieldAbsoluteDate<Binary64> guessDate = fieldDate.shiftedBy(1);
        final Vector3D receiver = new Vector3D(1e3, 2e4, 0);
        final FieldVector3D<Binary64> fieldReceiver = new FieldVector3D<>(field, receiver);
        // WHEN
        final Binary64 actual = fieldComputer.computeDelay(fieldReceiver, fieldDate, guessDate, orbit.getFrame());
        // THEN
        final double expected = new SignalTravelTimeAdjustableReceiver(positionProvider).computeDelay(receiver,
                fieldDate.toAbsoluteDate(), guessDate.toAbsoluteDate(), orbit.getFrame());
        assertEquals(expected, actual.getReal());
    }

    @Test
    void testComputeDelayInverse() {
        // GIVEN
        final Frame frame = FramesFactory.getGCRF();
        final UnivariateDerivative2Field field = UnivariateDerivative2Field.getInstance();
        final FieldAbsoluteDate<UnivariateDerivative2> emissionDate = FieldAbsoluteDate.getArbitraryEpoch(field)
                .shiftedBy(new UnivariateDerivative2(0., 1, 0.));
        final FieldVector3D<UnivariateDerivative2> emitterPosition = new FieldVector3D<>(field, new Vector3D(-1e4, 1e2, -1e3));
        final FieldPVCoordinates<UnivariateDerivative2> receiverPV = new FieldPVCoordinates<>(FieldVector3D.getMinusI(field),
                FieldVector3D.getPlusK(field));
        final FieldAbsolutePVCoordinates<UnivariateDerivative2> receiver = new FieldAbsolutePVCoordinates<>(frame, emissionDate, receiverPV);
        final FieldSignalTravelTimeAdjustableReceiver<UnivariateDerivative2> signalTimeOfFlight = new FieldSignalTravelTimeAdjustableReceiver<>(receiver);
        // WHEN
        final UnivariateDerivative2 delay = signalTimeOfFlight.computeDelay(emitterPosition, emissionDate, frame);
        final FieldAbsoluteDate<UnivariateDerivative2> receptionDate = emissionDate.shiftedBy(delay);
        // THEN
        final FieldSignalTravelTimeAdjustableEmitter<UnivariateDerivative2> inverseTravel = new FieldSignalTravelTimeAdjustableEmitter<>(
                new FieldAbsolutePVCoordinates<>(frame, emissionDate, emitterPosition, FieldVector3D.getZero(field)));
        final UnivariateDerivative2 inverseDelay = inverseTravel.computeDelay(receiver.getPosition(receptionDate, frame),
                receptionDate, frame);
        final FieldAbsoluteDate<UnivariateDerivative2> date = receptionDate.shiftedBy(inverseDelay.negate());
        assertEquals(emissionDate, date);
    }

}
