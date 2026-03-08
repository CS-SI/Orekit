package org.orekit.signal;

import org.hipparchus.analysis.solvers.BracketingNthOrderBrentSolver;
import org.hipparchus.analysis.solvers.UnivariateSolver;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.optim.ConvergenceChecker;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class SignalTravelTimeAdjustableReceiverTest {

    @Test
    void testConstructorDefaultConvergenceChecker() {
        // GIVEN
        final SignalTravelTimeAdjustableReceiver adjustableReceiver = new SignalTravelTimeAdjustableReceiver(mock());
        // WHEN
        final ConvergenceChecker<Double> convergenceChecker = adjustableReceiver.getConvergenceChecker();
        final double convergedValue = 0.;
        // THEN
        assertFalse(convergenceChecker.converged(0, convergedValue, convergedValue));  // enforces at least one iteration
        assertTrue(convergenceChecker.converged(1, convergedValue, convergedValue));
    }

    @Test
    void testComputeDelayStatic() {
        // GIVEN
        final Frame frame = FramesFactory.getGCRF();
        final AbsoluteDate emissionDate = AbsoluteDate.ARBITRARY_EPOCH;
        final AbsolutePVCoordinates absolutePVCoordinates = new AbsolutePVCoordinates(frame, emissionDate, new PVCoordinates());
        final Vector3D emitterPosition = new Vector3D(1e2, 1e3, 1e4);
        final SignalTravelTimeAdjustableReceiver signalTimeOfFlight = new SignalTravelTimeAdjustableReceiver(absolutePVCoordinates);
        final SignalEmissionCondition emissionCondition = new SignalEmissionCondition(emissionDate, emitterPosition, frame);
        // WHEN
        final double actual = signalTimeOfFlight.computeDelay(emissionCondition);
        // THEN
        final double expected = emitterPosition.getNorm() / Constants.SPEED_OF_LIGHT;
        assertEquals(expected, actual);
    }

    @ParameterizedTest
    @ValueSource(doubles = {-1e2, 0., 1e3, 1e5})
    void testComputeDelayVersusBrent(final double speed) {
        // GIVEN
        final Frame frame = FramesFactory.getGCRF();
        final AbsoluteDate emissionDate = AbsoluteDate.ARBITRARY_EPOCH;
        final Vector3D emitterPosition = new Vector3D(-1e1, 1e2, -1e3);
        final AbsolutePVCoordinates absolutePVCoordinates = new AbsolutePVCoordinates(frame, emissionDate,
                new PVCoordinates(emitterPosition, Vector3D.MINUS_J.scalarMultiply(speed), Vector3D.PLUS_J));
        final SignalTravelTimeAdjustableReceiver signalTimeOfFlight = new SignalTravelTimeAdjustableReceiver(absolutePVCoordinates);
        final SignalEmissionCondition emissionCondition = new SignalEmissionCondition(emissionDate, emitterPosition, frame);
        // WHEN
        final double actual = signalTimeOfFlight.computeDelay(emissionCondition);
        // THEN
        final UnivariateSolver solver = new BracketingNthOrderBrentSolver(1.0e-12, 5);
        final double expected = solver.solve(1000, x -> {
            final double d = Vector3D.distance(emitterPosition, absolutePVCoordinates.getPosition(emissionDate.shiftedBy(x), frame));
            return d - x * Constants.SPEED_OF_LIGHT;
        }, -1.0, 1.0);
        assertEquals(expected, actual);
    }

    @ParameterizedTest
    @ValueSource(doubles = {-1e3, -1e1, 0., 1e1, 1e2, 1e3, 1e4})
    void testComputeDelay(final double speedFactor) {
        // GIVEN
        final Frame frame = FramesFactory.getGCRF();
        final AbsoluteDate emissionDate = AbsoluteDate.ARBITRARY_EPOCH;
        final PVCoordinates pvCoordinates = new PVCoordinates(Vector3D.MINUS_I, new Vector3D(1, -2, 3).scalarMultiply(speedFactor));
        final AbsolutePVCoordinates absolutePVCoordinates = new AbsolutePVCoordinates(frame, emissionDate, pvCoordinates);
        final Vector3D emitterPosition = new Vector3D(1e2, 1e3, 1e4);
        final SignalTravelTimeAdjustableReceiver signalTimeOfFlight = new SignalTravelTimeAdjustableReceiver(absolutePVCoordinates);
        final SignalEmissionCondition emissionCondition = new SignalEmissionCondition(emissionDate, emitterPosition, frame);
        // WHEN
        final double actual = signalTimeOfFlight.computeDelay(emissionCondition);
        // THEN
        final AbsoluteDate receptionDate = emissionDate.shiftedBy(actual);
        final double expected = signalTimeOfFlight.computeDelay(emissionCondition, receptionDate);
        assertEquals(expected, actual);
    }

    @ParameterizedTest
    @ValueSource(doubles = {-1e4, -1e3, -1e1, 0., 1e1, 1e2, 1e3, 1e4})
    void testComputeDelayVersusSimple(final double speedFactor) {
        // GIVEN
        final Frame frame = FramesFactory.getGCRF();
        final AbsoluteDate emissionDate = AbsoluteDate.ARBITRARY_EPOCH;
        final PVCoordinates pvCoordinates = new PVCoordinates(new Vector3D(1e4, 1e6, -1e5), Vector3D.PLUS_J.scalarMultiply(speedFactor));
        final AbsolutePVCoordinates absolutePVCoordinates = new AbsolutePVCoordinates(frame, emissionDate, pvCoordinates);
        final Vector3D emitterPosition = Vector3D.ZERO;
        final SignalTravelTimeAdjustableReceiver signalTimeOfFlight = new SignalTravelTimeAdjustableReceiver(absolutePVCoordinates,
                ((iteration, previous, current) -> iteration >= 1));
        final SignalEmissionCondition emissionCondition = new SignalEmissionCondition(emissionDate, emitterPosition, frame);
        // WHEN
        final double actual = signalTimeOfFlight.computeDelay(emissionCondition);
        // THEN
        final double guess = pvCoordinates.getPosition().getNorm2() / Constants.SPEED_OF_LIGHT;
        final double expected = iterateOnce(emitterPosition, emissionDate, frame, absolutePVCoordinates, guess);
        assertEquals(expected, actual, 1e-12);
    }

    protected double iterateOnce(final Vector3D position, final AbsoluteDate date, final Frame frame,
                                 final PVCoordinatesProvider pvCoordinatesProvider, final double travelTimeGuess) {
        final Vector3D shiftedEmitter = pvCoordinatesProvider.getPosition(date.shiftedBy(travelTimeGuess), frame);
        return shiftedEmitter.subtract(position).getNorm2() / Constants.SPEED_OF_LIGHT;
    }
}
