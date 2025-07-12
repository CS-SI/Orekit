package org.orekit.propagation.events.handlers;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.ode.events.Action;
import org.hipparchus.util.Binary64;
import org.hipparchus.util.Binary64Field;
import org.hipparchus.util.Pair;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.orekit.TestUtils;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.FieldOrbit;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.FieldEventDetector;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.AbsolutePVCoordinates;
import org.orekit.utils.FieldAbsolutePVCoordinates;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.FieldPVCoordinatesProvider;
import org.orekit.utils.PVCoordinates;

import java.util.List;

import static org.mockito.Mockito.mock;

class FieldSecondaryEventLoggerTest {

    private static final AbsoluteDate DEFAULT_DATE = AbsoluteDate.ARBITRARY_EPOCH;

    @Test
    void testGetter() {
        // GIVEN
        final FieldSpacecraftState<Binary64> state = buildState(DEFAULT_DATE);
        final FieldPVCoordinatesProvider<Binary64> pvCoordinatesProvider = state.getOrbit();
        final FieldSecondaryEventLogger<Binary64> secondaryEventLogger = new FieldSecondaryEventLogger<>(pvCoordinatesProvider);
        // WHEN
        final FieldPVCoordinatesProvider<Binary64> actualProvider = secondaryEventLogger.getSecondaryPVCoordinatesProvider();
        // THEN
        Assertions.assertEquals(pvCoordinatesProvider, actualProvider);
    }

    @Test
    @SuppressWarnings("unchecked")
    void testConstructor() {
        // GIVEN
        final FieldSpacecraftState<Binary64> state = buildState(DEFAULT_DATE);
        final FieldSecondaryEventLogger<Binary64> secondaryEventLogger = new FieldSecondaryEventLogger<>(state.getOrbit());
        final FieldEventDetector<Binary64> detector = mock();
        // WHEN
        final Action actualAction = secondaryEventLogger.eventOccurred(state, detector, true);
        // THEN
        Assertions.assertEquals(Action.CONTINUE, actualAction);
    }

    @Test
    void testInit() {
        // GIVEN
        final FieldSpacecraftState<Binary64> state = buildState(DEFAULT_DATE);
        final FieldSecondaryEventLogger<Binary64> secondaryEventLogger = new FieldSecondaryEventLogger<Binary64>(state.getOrbit());
        // WHEN
        secondaryEventLogger.init(state, null, null);
        // THEN
        final List<Pair<FieldAbsolutePVCoordinates<Binary64>, FieldPVCoordinates<Binary64>>> logs = secondaryEventLogger.copyLogs();
        Assertions.assertEquals(1, logs.size());
        final Pair<FieldAbsolutePVCoordinates<Binary64>, FieldPVCoordinates<Binary64>> log = logs.get(0);
        Assertions.assertEquals(state.getDate(), log.getKey().getDate());
    }

    @Test
    void testEventOccurred() {
        // GIVEN
        final AbsoluteDate date = DEFAULT_DATE;
        final PVCoordinates pvCoordinates = new PVCoordinates(Vector3D.MINUS_I.scalarMultiply(10), new Vector3D(1, 2, 3));
        final SpacecraftState state = new SpacecraftState(new AbsolutePVCoordinates(FramesFactory.getEME2000(),
                date, pvCoordinates));
        final FieldSpacecraftState<Binary64> primaryState = new FieldSpacecraftState<>(Binary64Field.getInstance(), state);
        final double shift = 100;
        final FieldSpacecraftState<Binary64> secondaryState = buildState(date.shiftedBy(shift));
        final FieldOrbit<Binary64> secondaryOrbit = secondaryState.getOrbit();
        final FieldSecondaryEventLogger<Binary64> secondaryEventLogger = new FieldSecondaryEventLogger<Binary64>(secondaryOrbit);
        // WHEN
        secondaryEventLogger.eventOccurred(primaryState, null, true);
        // THEN
        final Pair<FieldAbsolutePVCoordinates<Binary64>, FieldPVCoordinates<Binary64>> log = secondaryEventLogger.copyLogs().get(0);
        final FieldAbsolutePVCoordinates<Binary64> primaryPVCoordinates = log.getKey();
        final FieldPVCoordinates<Binary64> secondaryPVCoordinates = log.getValue();
        Assertions.assertEquals(primaryState.getDate(), primaryPVCoordinates.getDate());
        Assertions.assertEquals(primaryState.getFrame(), primaryPVCoordinates.getFrame());
        Assertions.assertEquals(pvCoordinates.getPosition(), primaryPVCoordinates.getPosition().toVector3D());
        Assertions.assertEquals(pvCoordinates.getVelocity(), primaryPVCoordinates.getVelocity().toVector3D());
        final FieldPVCoordinates<Binary64> shiftedOrbitPV = secondaryOrbit.shiftedBy(-shift).getPVCoordinates(primaryState.getFrame());
        Assertions.assertEquals(shiftedOrbitPV.getPosition(), secondaryPVCoordinates.getPosition());
        Assertions.assertEquals(shiftedOrbitPV.getVelocity(), secondaryPVCoordinates.getVelocity());
    }

    @ParameterizedTest
    @EnumSource(Action.class)
    @SuppressWarnings("unchecked")

    void testEventOccurredAction(final Action expectedAction) {
        // GIVEN
        final FieldSpacecraftState<Binary64> state = buildState(DEFAULT_DATE);
        final FieldSecondaryEventLogger<Binary64> secondaryEventLogger = new FieldSecondaryEventLogger<Binary64>(state.getOrbit(), expectedAction);
        final FieldEventDetector<Binary64> detector = mock();
        // WHEN
        final Action actualAction = secondaryEventLogger.eventOccurred(state, detector, true);
        // THEN
        Assertions.assertEquals(expectedAction, actualAction);
    }

    @Test
    @SuppressWarnings("unchecked")
    void testEventOccurredManyTimes() {
        // GIVEN
        final FieldSpacecraftState<Binary64> state = buildState(DEFAULT_DATE);
        final FieldSecondaryEventLogger<Binary64> secondaryEventLogger = new FieldSecondaryEventLogger<Binary64>(state.getOrbit());
        final int expectedSize = 10;
        final FieldEventDetector<Binary64> detector = mock();
        // WHEN
        for (int i = 0; i < expectedSize; i++) {
            secondaryEventLogger.eventOccurred(state.shiftedBy(i), detector, true);
        }
        // THEN
        final List<Pair<FieldAbsolutePVCoordinates<Binary64>, FieldPVCoordinates<Binary64>>> logs = secondaryEventLogger.copyLogs();
        Assertions.assertEquals(expectedSize, logs.size());
        for (int i = 0; i < expectedSize; i++) {
            Assertions.assertEquals(state.getDate().shiftedBy(i), logs.get(i).getKey().getDate());
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void testFinish() {
        // GIVEN
        final FieldSpacecraftState<Binary64> state = buildState(DEFAULT_DATE);
        final FieldSecondaryEventLogger<Binary64> secondaryEventLogger = new FieldSecondaryEventLogger<>(state.getOrbit());
        final FieldEventDetector<Binary64> detector = mock();
        // WHEN
        secondaryEventLogger.finish(state, detector);
        // THEN
        final List<Pair<FieldAbsolutePVCoordinates<Binary64>, FieldPVCoordinates<Binary64>>> logs = secondaryEventLogger.copyLogs();
        Assertions.assertEquals(1, logs.size());
        final Pair<FieldAbsolutePVCoordinates<Binary64>, FieldPVCoordinates<Binary64>> log = logs.get(0);
        Assertions.assertEquals(state.getDate(), log.getKey().getDate());
    }

    @Test
    void testClearLogs() {
        // GIVEN
        final FieldSpacecraftState<Binary64> state = buildState(DEFAULT_DATE);
        final FieldSecondaryEventLogger<Binary64> secondaryEventLogger = new FieldSecondaryEventLogger<Binary64>(state.getOrbit());
        // WHEN
        secondaryEventLogger.eventOccurred(state, null, false);
        secondaryEventLogger.clearLogs();
        // THEN
        final List<Pair<FieldAbsolutePVCoordinates<Binary64>, FieldPVCoordinates<Binary64>>> logs = secondaryEventLogger.copyLogs();
        Assertions.assertEquals(0, logs.size());
    }

    private FieldSpacecraftState<Binary64> buildState(final AbsoluteDate date) {
        final SpacecraftState state = new SpacecraftState(TestUtils.getDefaultOrbit(date));
        return new FieldSpacecraftState<>(Binary64Field.getInstance(), state);
    }
}
