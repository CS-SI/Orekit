package org.orekit.propagation.numerical;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.analysis.differentiation.Gradient;
import org.hipparchus.analysis.differentiation.GradientField;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.QRDecomposition;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.ode.events.Action;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mockito;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.attitudes.FrameAlignedProvider;
import org.orekit.forces.ForceModel;
import org.orekit.forces.ForceModelModifier;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.OrbitType;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.DateDetector;
import org.orekit.propagation.events.DetectorModifier;
import org.orekit.propagation.events.EventDetectionSettings;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.events.FieldDateDetector;
import org.orekit.propagation.events.FieldDetectorModifier;
import org.orekit.propagation.events.FieldEventDetectionSettings;
import org.orekit.propagation.events.FieldEventDetector;
import org.orekit.propagation.events.handlers.ContinueOnEvent;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.propagation.events.handlers.FieldContinueOnEvent;
import org.orekit.propagation.events.handlers.FieldEventHandler;
import org.orekit.propagation.events.handlers.ResetDerivativesOnEvent;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.AbsolutePVCoordinates;
import org.orekit.utils.Constants;
import org.orekit.utils.DerivativeStateUtils;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.TimeStampedPVCoordinates;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SwitchEventHandlerTest {

    private static final String STM_NAME = "stm";

    @ParameterizedTest
    @EnumSource(value = Action.class)
    void testEventOccurred(final Action action) {
        // GIVEN
        final AbsoluteDate date = AbsoluteDate.ARBITRARY_EPOCH;
        final NumericalPropagationHarvester harvester = mockHarvester();
        final EventHandler handler = (s, e, i) -> action;
        final SwitchEventHandler switchEventHandler = buildSwitchEventHandler(mock(ForceModel.class), harvester, handler);
        final SpacecraftState stateAtSwitch = buildAbsoluteState(date, new double[0]);
        // WHEN
        final Action wrappedAction = switchEventHandler.eventOccurred(stateAtSwitch, new DateDetector(), true);
        // THEN
        assertEquals(action, wrappedAction);
    }

    @Test
    void testEventOccurredResetDerivativesAndMatchingDetector() {
        // GIVEN
        final AbsoluteDate date = AbsoluteDate.ARBITRARY_EPOCH;
        final Vector3D acceleration = new Vector3D(1, 2, 3);
        final ForceModel forceModel = new TestForce(acceleration, date);
        final ForceModel forceWithDetectors = getForceModelWithWrappedDateDetectors(forceModel, date);
        final NumericalPropagationHarvester harvester = mockHarvester();
        final SwitchEventHandler switchEventHandler = buildSwitchEventHandler(forceWithDetectors, harvester,
                new ResetDerivativesOnEvent());
        final SpacecraftState stateAtSwitch = buildAbsoluteState(date, new double[0]);
        final DateDetector dateDetector = new DateDetector(date);
        // WHEN
        final Action action = switchEventHandler.eventOccurred(stateAtSwitch, dateDetector, true);
        // THEN
        assertEquals(Action.RESET_STATE, action);
    }

    @Test
    void testResetStateTrivial() {
        // GIVEN
        final NumericalPropagationHarvester harvester = mockHarvester();
        final SwitchEventHandler switchEventHandler = buildSwitchEventHandler(mock(ForceModel.class), harvester, new ContinueOnEvent());
        final SpacecraftState stateAtSwitch = buildAbsoluteState(AbsoluteDate.ARBITRARY_EPOCH, new double[0]);
        // WHEN
        final SpacecraftState resetState = switchEventHandler.resetState(null, stateAtSwitch);
        // THEN
        compareStates(stateAtSwitch, resetState);
    }

    private static void compareStatesWithoutAdditionalVariables(final SpacecraftState expectedState,
                                                                final SpacecraftState actualState) {
        assertEquals(expectedState.getDate(), actualState.getDate());
        assertEquals(expectedState.getPosition(), actualState.getPosition());
        assertEquals(expectedState.getMass(), actualState.getMass());
        assertEquals(expectedState.getAttitude(), actualState.getAttitude());
    }

    private static void compareStates(final SpacecraftState expectedState, final SpacecraftState actualState) {
        compareStatesWithoutAdditionalVariables(expectedState, actualState);
        assertArrayEquals(expectedState.getAdditionalState(STM_NAME), actualState.getAdditionalState(STM_NAME));
    }

    @Test
    void testResetStateDetectorIndependentOfTime() {
        // GIVEN
        final AbsoluteDate date = AbsoluteDate.ARBITRARY_EPOCH;
        final Vector3D acceleration = Vector3D.MINUS_I;
        final ForceModel forceModel = new TestForce(acceleration, date);
        final ForceModel forceWithDetectors = getForceModelWithWrappedDateDetectors(forceModel, date);
        final NumericalPropagationHarvester harvester = mockHarvester();
        final SwitchEventHandler switchEventHandler = buildSwitchEventHandler(forceWithDetectors, harvester,
                new ResetDerivativesOnEvent());
        final RealMatrix stm = MatrixUtils.createRealIdentityMatrix(7);
        final String jacobianName = "param0";
        when(harvester.getJacobiansColumnsNames()).thenReturn(Collections.singletonList(jacobianName));
        final SpacecraftState stateAtSwitch = buildAbsoluteState(date, harvester.toArray(stm.getData()))
                .addAdditionalData(jacobianName, new double[] {1, 2, 3, 4, 5, 6, 7});
        final EventDetector eventDetector = forceWithDetectors.getEventDetectors().collect(Collectors.toList()).get(0);
        // WHEN
        preprocessSwitchHandler(switchEventHandler, stateAtSwitch, eventDetector);
        final SpacecraftState resetState = switchEventHandler.resetState(eventDetector, stateAtSwitch);
        // THEN
        compareStates(stateAtSwitch, resetState);
        assertArrayEquals(stateAtSwitch.getAdditionalState(jacobianName), resetState.getAdditionalState(jacobianName));
    }

    @Test
    void testResetStateNoSwitch() {
        // GIVEN
        final AbsoluteDate date = AbsoluteDate.ARBITRARY_EPOCH;
        final RealMatrix stm = buildStm();
        final NumericalPropagationHarvester harvester = mockHarvester();
        final SpacecraftState stateAtSwitch = buildAbsoluteState(date, harvester.toArray(stm.getData()));
        final ForceModel forceWithDetectors = getForceModelWithoutSwitch(date, stateAtSwitch);
        final SwitchEventHandler switchEventHandler = buildSwitchEventHandler(forceWithDetectors, harvester,
                new ResetDerivativesOnEvent());
        final EventDetector eventDetector = forceWithDetectors.getEventDetectors().collect(Collectors.toList()).get(0);
        // WHEN
        preprocessSwitchHandler(switchEventHandler, stateAtSwitch, eventDetector);
        final SpacecraftState resetState = switchEventHandler.resetState(mock(EventDetector.class), stateAtSwitch);
        // THEN
        compareStates(stateAtSwitch, resetState);
    }

    private static RealMatrix buildStm() {
        final RealMatrix matrix = MatrixUtils.createRealIdentityMatrix(7);
        for (int i = 0; i < 7; i++) {
            for (int j = 0; j < 7; j++) {
                if (i != j) {
                    matrix.setEntry(i, j, i + j);
                }
            }
        }
        return matrix;
    }

    private static ForceModel getForceModelWithoutSwitch(AbsoluteDate date, SpacecraftState stateAtSwitch) {
        return getForceModel(date, Vector3D.ZERO, stateAtSwitch);
    }

    private static ForceModel getForceModel(final AbsoluteDate date,  final Vector3D acceleration,
                                            final SpacecraftState stateAtSwitch) {
        final ForceModel forceModel = new TestForce(acceleration, date);
        final TimeStampedPVCoordinates trivialPV = new TimeStampedPVCoordinates(date, new PVCoordinates());
        return new ForceModelModifier() {
            @Override
            public ForceModel getUnderlyingModel() {
                return forceModel;
            }

            @Override
            public Stream<EventDetector> getEventDetectors() {
                return Stream.of(new TimeStampedPVDetector(trivialPV), new TimeStampedPVDetector(stateAtSwitch.getPVCoordinates()));
            }

            @Override
            public <T extends CalculusFieldElement<T>> Stream<FieldEventDetector<T>> getFieldEventDetectors(Field<T> field) {
                return Stream.of(new FieldPTimeStampedVDetector<>(field, trivialPV),
                        new FieldPTimeStampedVDetector<>(field, stateAtSwitch.getPVCoordinates()));
            }
        };
    }

    @Test
    void testResetState() {
        // GIVEN
        final AbsoluteDate date = AbsoluteDate.ARBITRARY_EPOCH;
        final Vector3D acceleration = new Vector3D(1, 2, 3);
        final RealMatrix stm = buildStm();
        final NumericalPropagationHarvester harvester = mockHarvester();
        final SpacecraftState stateAtSwitch = buildOrbitState(date, harvester.toArray(stm.getData()));
        final ForceModel forceWithDetectors = getForceModel(date, acceleration, stateAtSwitch);
        final SwitchEventHandler switchEventHandler = buildSwitchEventHandler(forceWithDetectors, harvester,
                new ResetDerivativesOnEvent());
        final EventDetector eventDetector = new TimeStampedPVDetector(stateAtSwitch.getPVCoordinates());
        // WHEN
        preprocessSwitchHandler(switchEventHandler, stateAtSwitch, eventDetector);
        final SpacecraftState resetState = switchEventHandler.resetState(eventDetector, stateAtSwitch);
        // THEN
        compareStatesWithoutAdditionalVariables(stateAtSwitch, resetState);
        final GradientField field = GradientField.getField(8);
        final RealMatrix actualStm = harvester.toSquareMatrix(resetState.getAdditionalState(STM_NAME));
        final RealMatrix expectedStm = computeExpectedStm(stateAtSwitch,
                new FieldPTimeStampedVDetector<>(field, stateAtSwitch.getPVCoordinates()), acceleration);
        for (int i = 0; i < 7; i++) {
            assertArrayEquals(expectedStm.getRow(i), actualStm.getRow(i), 5e-2);
        }
    }

    private static void preprocessSwitchHandler(final SwitchEventHandler switchEventHandler, final SpacecraftState stateAtSwitch,
                                                final EventDetector eventDetector) {
        switchEventHandler.init(stateAtSwitch, AbsoluteDate.FUTURE_INFINITY, eventDetector);
        switchEventHandler.eventOccurred(stateAtSwitch, eventDetector, true);
    }

    private static RealMatrix computeExpectedStm(final SpacecraftState state,
                                                 final FieldEventDetector<Gradient> fieldDetector,
                                                 final Vector3D acceleration) {
        final NumericalPropagationHarvester harvester = mockHarvester();
        final Gradient dt = Gradient.variable(8, 7, 0);
        final RealMatrix stm = harvester.toSquareMatrix(state.getAdditionalState(STM_NAME));
        final RealMatrix augmentedStm = MatrixUtils.createRealMatrix(8, 8);
        augmentedStm.setSubMatrix(stm.getData(), 0, 0);
        final FieldSpacecraftState<Gradient> fieldState = DerivativeStateUtils.buildSpacecraftStateTransitionGradient(state,
                augmentedStm, null).shiftedBy(dt);
        final Gradient g = fieldDetector.g(fieldState);
        final RealMatrix matrixToInverse = MatrixUtils.createRealIdentityMatrix(8);
        matrixToInverse.setRow(7, g.getGradient());
        final RealMatrix inverted = new QRDecomposition(matrixToInverse).getSolver().getInverse();
        final RealMatrix lhs = MatrixUtils.createRealMatrix(8, 8);
        lhs.setSubMatrix(stm.getData(), 0, 0);
        lhs.setEntry(3, 7, acceleration.getX());
        lhs.setEntry(4, 7, acceleration.getY());
        lhs.setEntry(5, 7, acceleration.getZ());
        final RealMatrix product = lhs.multiply(inverted);
        return product.getSubMatrix(0, 6, 0, 6);
    }

    private static SwitchEventHandler buildSwitchEventHandler(final ForceModel forceModel,
                                                              final NumericalPropagationHarvester harvester,
                                                              final EventHandler handler) {
        final NumericalTimeDerivativesEquations equations = new NumericalTimeDerivativesEquations(OrbitType.CARTESIAN,
                null, Collections.singletonList(forceModel));
        final AttitudeProvider attitudeProvider = new FrameAlignedProvider(Rotation.IDENTITY);
        return new SwitchEventHandler(handler, harvester, equations, attitudeProvider);
    }

    private static SpacecraftState buildAbsoluteState(final AbsoluteDate date, final double[] stmArray) {
        final PVCoordinates pvCoordinates = new PVCoordinates();
        final AbsolutePVCoordinates absolutePVCoordinates = new AbsolutePVCoordinates(FramesFactory.getEME2000(), date,
                pvCoordinates);
        return new SpacecraftState(absolutePVCoordinates).addAdditionalData(STM_NAME, stmArray);
    }

    private static SpacecraftState buildOrbitState(final AbsoluteDate date, final double[] stmArray) {
        final PVCoordinates pvCoordinates = new PVCoordinates(Vector3D.PLUS_I.scalarMultiply(7e6), Vector3D.MINUS_K.scalarMultiply(7e3));
        final CartesianOrbit cartesianOrbit = new CartesianOrbit(new TimeStampedPVCoordinates(date, pvCoordinates),
                FramesFactory.getEME2000(), Constants.EGM96_EARTH_MU);
        return new SpacecraftState(cartesianOrbit).addAdditionalData(STM_NAME, stmArray);
    }

    private static NumericalPropagationHarvester mockHarvester() {
        final NumericalPropagationHarvester harvester = mock();
        when(harvester.getStateDimension()).thenReturn(7);
        when(harvester.toArray(Mockito.any())).thenCallRealMethod();
        when(harvester.toSquareMatrix(Mockito.any())).thenCallRealMethod();
        when(harvester.getStmName()).thenReturn(STM_NAME);
        return harvester;
    }

    private static class TimeStampedPVDetector implements EventDetector {
        private final TimeStampedPVCoordinates pvCoordinates;

        TimeStampedPVDetector(final TimeStampedPVCoordinates pvCoordinates) {
            this.pvCoordinates = pvCoordinates;
        }

        @Override
        public double g(SpacecraftState s) {
            final PVCoordinates relativePV = new PVCoordinates(s.getPVCoordinates(), pvCoordinates);
            return s.durationFrom(pvCoordinates) + relativePV.getPosition().getX() + relativePV.getPosition().getY()
                    + relativePV.getPosition().getZ() + relativePV.getVelocity().getX() + relativePV.getVelocity().getY()
                    + relativePV.getVelocity().getZ();
        }

        @Override
        public EventHandler getHandler() {
            return new ContinueOnEvent();
        }
    }

    private static class FieldPTimeStampedVDetector<T extends CalculusFieldElement<T>> implements FieldEventDetector<T> {
        private final TimeStampedPVCoordinates pvCoordinates;
        private final Field<T> field;

        FieldPTimeStampedVDetector(final Field<T> field, final TimeStampedPVCoordinates pvCoordinates) {
            this.field = field;
            this.pvCoordinates = pvCoordinates;
        }

        @Override
        public T g(FieldSpacecraftState<T> s) {
            final FieldPVCoordinates<T> relativePV = new FieldPVCoordinates<>(s.getPosition().subtract(pvCoordinates.getPosition()),
                    s.getPVCoordinates().getVelocity().subtract(pvCoordinates.getVelocity()));
            final FieldVector3D<T> relativePosition = relativePV.getPosition();
            final FieldVector3D<T> relativeVelocity = relativePV.getVelocity();
            return s.durationFrom(new FieldAbsoluteDate<>(s.getDate().getField(), pvCoordinates.getDate()))
                    .add(relativePosition.getX()).add(relativePosition.getY()).add(relativePosition.getZ())
                    .add(relativeVelocity.getX()).add(relativeVelocity.getY()).add(relativeVelocity.getZ());
        }

        @Override
        public FieldEventHandler<T> getHandler() {
            return new FieldContinueOnEvent<>();
        }

        @Override
        public FieldEventDetectionSettings<T> getDetectionSettings() {
            return new FieldEventDetectionSettings<>(field, EventDetectionSettings.getDefaultEventDetectionSettings());
        }
    }

    private static ForceModel getForceModelWithWrappedDateDetectors(final ForceModel forceModel, final AbsoluteDate date) {
        return new ForceModelModifier() {
            @Override
            public ForceModel getUnderlyingModel() {
                return forceModel;
            }

            @Override
            public Stream<EventDetector> getEventDetectors() {
                return Stream.of(new DetectorModifier() {
                    @Override
                    public boolean dependsOnTimeOnly() {
                        return false;
                    }

                    @Override
                    public EventDetector getDetector() {
                        return new DateDetector(date);
                    }
                });
            }

            @Override
            public <T extends CalculusFieldElement<T>> Stream<FieldEventDetector<T>> getFieldEventDetectors(Field<T> field) {
                return Stream.of(new FieldDetectorModifier<T>() {
                    @Override
                    public boolean dependsOnTimeOnly() {
                        return false;
                    }

                    @Override
                    public FieldEventDetector<T> getDetector() {
                        return new FieldDateDetector<>(new FieldAbsoluteDate<>(field, date));
                    }
                });
            }
        };
    }

    private static class TestForce implements ForceModel {

        private final Vector3D switchAcceleration;
        private final AbsoluteDate switchDate;

        TestForce(final Vector3D switchAcceleration, final AbsoluteDate switchDate) {
            this.switchAcceleration = switchAcceleration;
            this.switchDate = switchDate;
        }

        @Override
        public boolean dependsOnPositionOnly() {
            return false;
        }

        @Override
        public Vector3D acceleration(SpacecraftState s, double[] parameters) {
            return s.getDate().isBefore(switchDate) ? Vector3D.ZERO : switchAcceleration;
        }

        @Override
        public <T extends CalculusFieldElement<T>> FieldVector3D<T> acceleration(FieldSpacecraftState<T> s, T[] parameters) {
            return null;
        }

        @Override
        public List<ParameterDriver> getParametersDrivers() {
            return Collections.emptyList();
        }
    }
}
