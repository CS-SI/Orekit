/* Copyright 2022-2025 Romain Serra
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
import org.orekit.utils.FieldAbsolutePVCoordinates;
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
        final ForceModel forceModel = new TestForce(acceleration, acceleration, date);
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
    void testResetStateDetectorIndependentOfState() {
        // GIVEN
        final AbsoluteDate date = AbsoluteDate.ARBITRARY_EPOCH;
        final Vector3D acceleration = Vector3D.MINUS_I;
        final ForceModel forceModel = new TestForce(Vector3D.ZERO, acceleration, date);
        final ForceModel forceWithDetectors = getForceModelWithWrappedDateDetectors(forceModel, date);
        final NumericalPropagationHarvester harvester = mockHarvester();
        final SwitchEventHandler switchEventHandler = buildSwitchEventHandler(forceWithDetectors, harvester,
                new ResetDerivativesOnEvent());
        final RealMatrix stm = MatrixUtils.createRealIdentityMatrix(7);
        final String jacobianName = "param0";
        when(harvester.getJacobiansColumnsNames()).thenReturn(Collections.singletonList(jacobianName));
        final double[][] transposedJacobian = new double[7][7];
        transposedJacobian[0] = new double[] {1, 2, 3, 4, 5, 6, 7};
        final SpacecraftState stateAtSwitch = buildAbsoluteState(date, harvester.toArray(stm.getData()))
                .addAdditionalData(jacobianName, transposedJacobian[0]);
        when(harvester.getParametersJacobian(stateAtSwitch)).thenReturn(MatrixUtils.createRealMatrix(transposedJacobian).transpose());
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
        final SpacecraftState stateAtSwitch = buildOrbitState(date, harvester.toArray(stm.getData()));
        final ForceModel forceWithDetectors = getForceModelWithoutSwitch(stateAtSwitch.getPVCoordinates());
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

    @Test
    void testResetState() {
        // GIVEN
        final AbsoluteDate date = AbsoluteDate.ARBITRARY_EPOCH;
        final Vector3D accelerationBefore = Vector3D.MINUS_I;
        final Vector3D accelerationAfter = new Vector3D(1, 2, 3);
        final RealMatrix originalStm = buildStm();
        final NumericalPropagationHarvester harvester = mockHarvester();
        final SpacecraftState stateAtSwitch = buildAbsoluteState(date, harvester.toArray(originalStm.getData()));
        final ForceModel forceWithDetectors = getForceModel(accelerationBefore, accelerationAfter, stateAtSwitch.getPVCoordinates());
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
        final FieldPTimeStampedVDetector<Gradient> fieldDetector = new FieldPTimeStampedVDetector<>(field, stateAtSwitch.getPVCoordinates());
        final double[] deltaDerivatives = new double[7];
        deltaDerivatives[3] = accelerationBefore.getX() - accelerationAfter.getX();
        deltaDerivatives[4] = accelerationBefore.getY() - accelerationAfter.getY();
        deltaDerivatives[5] = accelerationBefore.getZ() - accelerationAfter.getZ();
        final RealMatrix updateMatrix = computeUpdateMatrix(stateAtSwitch, deltaDerivatives, fieldDetector);
        final RealMatrix expectedStm = updateMatrix.multiply(originalStm);
        for (int i = 0; i < 7; i++) {
            assertArrayEquals(expectedStm.getRow(i), actualStm.getRow(i), 1e-6);
        }
    }

    private RealMatrix computeUpdateMatrix(final SpacecraftState state, final double[] deltaDerivatives,
                                           final FieldEventDetector<Gradient> switchFieldDetector) {
        final Gradient g = evaluateG(state, 0., switchFieldDetector);
        final RealMatrix matrixToInverse = MatrixUtils.createRealIdentityMatrix(8);
        matrixToInverse.setRow(7, g.getGradient());
        final RealMatrix inverted = new QRDecomposition(matrixToInverse).getSolver().getInverse();
        final RealMatrix lhs = MatrixUtils.createRealIdentityMatrix(8);
        for (int i = 0; i < 7; i++) {
            lhs.setEntry(i, 7, deltaDerivatives[i]);
        }
        final RealMatrix product = lhs.multiply(inverted);
        return product.getSubMatrix(0, 6, 0, 6);
    }

    private Gradient evaluateG(final SpacecraftState state, final double massRate,
                               final FieldEventDetector<Gradient> fieldEventDetector) {
        final int freeParameters = 8;
        final Gradient dt = Gradient.variable(freeParameters, 7, 0);
        final GradientField field = dt.getField();
        final Vector3D position = state.getPosition();
        final Vector3D velocity = state.getVelocity();
        final FieldVector3D<Gradient> fieldPosition = new FieldVector3D<>(Gradient.variable(freeParameters, 0, position.getX()),
                Gradient.variable(freeParameters, 1, position.getY()),
                Gradient.variable(freeParameters, 2, position.getZ())).add(new FieldVector3D<>(field,
                velocity).scalarMultiply(dt));
        final FieldVector3D<Gradient> fieldVelocity = new FieldVector3D<>(Gradient.variable(freeParameters, 3, velocity.getX()),
                Gradient.variable(freeParameters, 4, velocity.getY()),
                Gradient.variable(freeParameters, 5, velocity.getZ())).add(new FieldVector3D<>(field,
                state.getPVCoordinates().getAcceleration()).scalarMultiply(dt));
        final FieldAbsoluteDate<Gradient> fieldDate = new FieldAbsoluteDate<>(dt.getField(), state.getDate()).shiftedBy(dt);
        final FieldAbsolutePVCoordinates<Gradient> fieldAbsolutePVCoordinates = new FieldAbsolutePVCoordinates<>(
                state.getFrame(), fieldDate, fieldPosition, fieldVelocity);
        final Gradient fieldMass = Gradient.variable(freeParameters, 6, state.getMass()).add(dt.multiply(massRate));
        final FieldSpacecraftState<Gradient> fieldState = new FieldSpacecraftState<>(fieldAbsolutePVCoordinates)
                .withMass(fieldMass);
        return fieldEventDetector.g(fieldState);
    }

    private static ForceModel getForceModelWithoutSwitch(TimeStampedPVCoordinates pvCoordinates) {
        final Vector3D acceleration = new Vector3D(2, 1);
        return getForceModel(acceleration, acceleration, pvCoordinates);
    }

    private static ForceModel getForceModel(final Vector3D accelerationBefore, final Vector3D accelerationAfter,
                                            final TimeStampedPVCoordinates pvCoordinates) {
        final ForceModel forceModel = new TestForce(accelerationBefore, accelerationAfter, pvCoordinates.getDate());
        final TimeStampedPVCoordinates trivialPV = new TimeStampedPVCoordinates(pvCoordinates.getDate(), new PVCoordinates());
        return new ForceModelModifier() {
            @Override
            public ForceModel getUnderlyingModel() {
                return forceModel;
            }

            @Override
            public Stream<EventDetector> getEventDetectors() {
                return Stream.of(new TimeStampedPVDetector(trivialPV), new TimeStampedPVDetector(pvCoordinates));
            }

            @Override
            public <T extends CalculusFieldElement<T>> Stream<FieldEventDetector<T>> getFieldEventDetectors(Field<T> field) {
                return Stream.of(new FieldPTimeStampedVDetector<>(field, trivialPV),
                        new FieldPTimeStampedVDetector<>(field,  pvCoordinates));
            }
        };
    }

    private static void preprocessSwitchHandler(final SwitchEventHandler switchEventHandler, final SpacecraftState stateAtSwitch,
                                                final EventDetector eventDetector) {
        switchEventHandler.init(stateAtSwitch, AbsoluteDate.FUTURE_INFINITY, eventDetector);
        switchEventHandler.eventOccurred(stateAtSwitch, eventDetector, true);
    }

    private static SwitchEventHandler buildSwitchEventHandler(final ForceModel forceModel,
                                                              final NumericalPropagationHarvester harvester,
                                                              final EventHandler handler) {
        final NumericalTimeDerivativesEquations equations = new NumericalTimeDerivativesEquations(null,
                null, Collections.singletonList(forceModel));
        final AttitudeProvider attitudeProvider = new FrameAlignedProvider(Rotation.IDENTITY);
        return new SwitchEventHandler(handler, harvester, equations, attitudeProvider);
    }

    private static SpacecraftState buildAbsoluteState(final AbsoluteDate date, final double[] stmArray) {
        final PVCoordinates pvCoordinates = buildOrbitState(date, stmArray).getPVCoordinates();
        final AbsolutePVCoordinates absolutePVCoordinates = new AbsolutePVCoordinates(FramesFactory.getEME2000(), date,
                pvCoordinates);
        return new SpacecraftState(absolutePVCoordinates).addAdditionalData(STM_NAME, stmArray);
    }

    private static SpacecraftState buildOrbitState(final AbsoluteDate date, final double[] stmArray) {
        final PVCoordinates pvCoordinates = new PVCoordinates(new Vector3D(7e6, 3e3, -1e2), new Vector3D(-1e2, 7e3, 1e1));
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

        private final Vector3D accelerationBefore;
        private final Vector3D accelerationAfter;
        private final AbsoluteDate switchDate;

        TestForce(final Vector3D accelerationBefore, final Vector3D accelerationAfter,
                  final AbsoluteDate switchDate) {
            this.accelerationBefore = accelerationBefore;
            this.accelerationAfter = accelerationAfter;
            this.switchDate = switchDate;
        }

        @Override
        public boolean dependsOnPositionOnly() {
            return false;
        }

        @Override
        public Vector3D acceleration(SpacecraftState s, double[] parameters) {
            return s.getDate().isBeforeOrEqualTo(switchDate) ? accelerationBefore : accelerationAfter;
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
