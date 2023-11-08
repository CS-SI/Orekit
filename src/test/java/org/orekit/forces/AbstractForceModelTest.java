/* Copyright 2002-2023 CS GROUP
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
package org.orekit.forces;

import org.hipparchus.Field;
import org.hipparchus.analysis.differentiation.DSFactory;
import org.hipparchus.analysis.differentiation.DerivativeStructure;
import org.hipparchus.analysis.differentiation.Gradient;
import org.hipparchus.analysis.differentiation.GradientField;
import org.hipparchus.geometry.euclidean.threed.FieldRotation;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.random.GaussianRandomGenerator;
import org.hipparchus.random.RandomGenerator;
import org.hipparchus.random.UncorrelatedRandomVectorGenerator;
import org.hipparchus.random.Well19937a;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.Precision;
import org.junit.jupiter.api.Assertions;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.attitudes.FieldAttitude;
import org.orekit.orbits.FieldCartesianOrbit;
import org.orekit.orbits.FieldKeplerianOrbit;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.MatricesHarvester;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.numerical.FieldNumericalPropagator;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.propagation.sampling.OrekitStepHandler;
import org.orekit.propagation.sampling.OrekitStepInterpolator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.Differentiation;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.TimeStampedFieldAngularCoordinates;
import org.orekit.utils.TimeStampedFieldPVCoordinates;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;


public abstract class AbstractForceModelTest {

    protected void checkParameterDerivative(SpacecraftState state,
                                            ForceModel forceModel, String name,
                                            double hFactor, double tol) {

        final DSFactory factory11 = new DSFactory(1, 1);
        final Field<DerivativeStructure> field = factory11.getDerivativeField();
        final FieldSpacecraftState<DerivativeStructure> stateF = new FieldSpacecraftState<DerivativeStructure>(field, state);
        final List<ParameterDriver> drivers = forceModel.getParametersDrivers();
        final DerivativeStructure[] parametersDS = new DerivativeStructure[drivers.size()];
        for (int i = 0; i < parametersDS.length; ++i) {
            if (drivers.get(i).getName().equals(name)) {
                parametersDS[i] = factory11.variable(0, drivers.get(i).getValue(state.getDate()));
            } else {
                parametersDS[i] = factory11.constant(drivers.get(i).getValue(state.getDate()));
            }
        }
        FieldVector3D<DerivativeStructure> accDer = forceModel.acceleration(stateF, parametersDS);
        Vector3D derivative = new Vector3D(accDer.getX().getPartialDerivative(1),
                                           accDer.getY().getPartialDerivative(1),
                                           accDer.getZ().getPartialDerivative(1));

        int selected = -1;
        final double[] parameters = new double[drivers.size()];
        for (int i = 0; i < drivers.size(); ++i) {
            parameters[i] = drivers.get(i).getValue(state.getDate());
            if (drivers.get(i).getName().equals(name)) {
                selected = i;
            }
        }
        double p0 = parameters[selected];
        double hParam = hFactor * p0;
        drivers.get(selected).setValue(p0 - 1 * hParam, state.getDate());
        parameters[selected] = drivers.get(selected).getValue(state.getDate());
        Assertions.assertEquals(p0 - 1 * hParam, parameters[selected], 1.0e-10);
        final Vector3D gammaM1h = forceModel.acceleration(state, parameters);
        drivers.get(selected).setValue(p0 + 1 * hParam, state.getDate());
        parameters[selected] = drivers.get(selected).getValue(state.getDate());
        Assertions.assertEquals(p0 + 1 * hParam, parameters[selected], 1.0e-10);
        final Vector3D gammaP1h = forceModel.acceleration(state, parameters);
        drivers.get(selected).setValue(p0, state.getDate());

        final Vector3D reference = new Vector3D(  1 / (2 * hParam), gammaP1h.subtract(gammaM1h));
        final Vector3D delta = derivative.subtract(reference);
        Assertions.assertEquals(0., delta.getNorm(), tol * reference.getNorm());

    }

    protected void checkParameterDerivativeGradient(SpacecraftState state,
                                                    ForceModel forceModel, String name,
                                                    double hFactor, double tol) {

        final int freeParameters = 1;
        final Field<Gradient> field = GradientField.getField(freeParameters);
        final FieldSpacecraftState<Gradient> stateF = new FieldSpacecraftState<>(field, state);
        final List<ParameterDriver> drivers = forceModel.getParametersDrivers();
        final Gradient[] parametersDS = new Gradient[drivers.size()];
        for (int i = 0; i < parametersDS.length; ++i) {
            if (drivers.get(i).getName().equals(name)) {
                parametersDS[i] = Gradient.variable(freeParameters, 0, drivers.get(i).getValue(state.getDate()));
            } else {
                parametersDS[i] = Gradient.constant(freeParameters, drivers.get(i).getValue(state.getDate()));
            }
        }
        FieldVector3D<Gradient> accDer = forceModel.acceleration(stateF, parametersDS);
        Vector3D derivative = new Vector3D(accDer.getX().getPartialDerivative(0),
                                           accDer.getY().getPartialDerivative(0),
                                           accDer.getZ().getPartialDerivative(0));

        int selected = -1;
        final double[] parameters = new double[drivers.size()];
        for (int i = 0; i < drivers.size(); ++i) {
            parameters[i] = drivers.get(i).getValue(state.getDate());
            if (drivers.get(i).getName().equals(name)) {
                selected = i;
            }
        }
        double p0 = parameters[selected];
        double hParam = hFactor * p0;
        drivers.get(selected).setValue(p0 - 1 * hParam, state.getDate());
        parameters[selected] = drivers.get(selected).getValue(state.getDate());
        Assertions.assertEquals(p0 - 1 * hParam, parameters[selected], 1.0e-10);
        final Vector3D gammaM1h = forceModel.acceleration(state, parameters);
        drivers.get(selected).setValue(p0 + 1 * hParam, state.getDate());
        parameters[selected] = drivers.get(selected).getValue(state.getDate());
        Assertions.assertEquals(p0 + 1 * hParam, parameters[selected], 1.0e-10);
        final Vector3D gammaP1h = forceModel.acceleration(state, parameters);
        drivers.get(selected).setValue(p0, state.getDate());

        final Vector3D reference = new Vector3D(  1 / (2 * hParam), gammaP1h.subtract(gammaM1h));
        final Vector3D delta = derivative.subtract(reference);
        Assertions.assertEquals(0, delta.getNorm(), tol * reference.getNorm());

    }

    protected FieldSpacecraftState<DerivativeStructure> toDS(final SpacecraftState state,
                                                             final AttitudeProvider attitudeProvider) {

        final Vector3D p = state.getPosition();
        final Vector3D v = state.getPVCoordinates().getVelocity();
        final Vector3D a = state.getPVCoordinates().getAcceleration();
        DSFactory factory = new DSFactory(6, 1);
        Field<DerivativeStructure> field = factory.getDerivativeField();
        final FieldAbsoluteDate<DerivativeStructure> fDate = new FieldAbsoluteDate<>(field, state.getDate());
        final TimeStampedFieldPVCoordinates<DerivativeStructure> fPVA =
                        new TimeStampedFieldPVCoordinates<>(fDate,
                                        new FieldVector3D<>(factory.variable(0, p.getX()),
                                                            factory.variable(1, p.getY()),
                                                            factory.variable(2, p.getZ())),
                                        new FieldVector3D<>(factory.variable(3, v.getX()),
                                                            factory.variable(4, v.getY()),
                                                            factory.variable(5, v.getZ())),
                                        new FieldVector3D<>(factory.constant(a.getX()),
                                                            factory.constant(a.getY()),
                                                            factory.constant(a.getZ())));
        final FieldCartesianOrbit<DerivativeStructure> orbit =
                        new FieldCartesianOrbit<>(fPVA, state.getFrame(), field.getZero().add(state.getMu()));
        final FieldAttitude<DerivativeStructure> attitude =
                        attitudeProvider.getAttitude(orbit, orbit.getDate(), orbit.getFrame());

        return new FieldSpacecraftState<>(orbit, attitude, field.getZero().add(state.getMass()));

    }

    protected FieldSpacecraftState<Gradient> toGradient(final SpacecraftState state,
                                                        final AttitudeProvider attitudeProvider) {

        final Vector3D p = state.getPosition();
        final Vector3D v = state.getPVCoordinates().getVelocity();
        final Vector3D a = state.getPVCoordinates().getAcceleration();
        final int freeParameters = 6;
        Field<Gradient> field = GradientField.getField(freeParameters);
        final FieldAbsoluteDate<Gradient> fDate = new FieldAbsoluteDate<>(field, state.getDate());
        final TimeStampedFieldPVCoordinates<Gradient> fPVA =
                        new TimeStampedFieldPVCoordinates<>(fDate,
                                        new FieldVector3D<>(Gradient.variable(freeParameters, 0, p.getX()),
                                                        Gradient.variable(freeParameters, 1, p.getY()),
                                                        Gradient.variable(freeParameters, 2, p.getZ())),
                                        new FieldVector3D<>(Gradient.variable(freeParameters, 3, v.getX()),
                                                        Gradient.variable(freeParameters, 4, v.getY()),
                                                        Gradient.variable(freeParameters, 5, v.getZ())),
                                        new FieldVector3D<>(Gradient.constant(freeParameters, a.getX()),
                                                        Gradient.constant(freeParameters, a.getY()),
                                                        Gradient.constant(freeParameters, a.getZ())));
        final FieldCartesianOrbit<Gradient> orbit =
                        new FieldCartesianOrbit<>(fPVA, state.getFrame(), field.getZero().add(state.getMu()));
        final FieldAttitude<Gradient> attitude =
                        attitudeProvider.getAttitude(orbit, orbit.getDate(), orbit.getFrame());

        return new FieldSpacecraftState<>(orbit, attitude, field.getZero().add(state.getMass()));

    }

    protected void checkStateJacobianVsFiniteDifferences(final  SpacecraftState state0, final ForceModel forceModel,
                                                         final AttitudeProvider provider, final double dP,
                                                         final double checkTolerance, final boolean print) {

        double[][] finiteDifferencesJacobian =
                        Differentiation.differentiate(state -> forceModel.acceleration(state, forceModel.getParameters(state0.getDate())).toArray(),
                                                      3, provider, OrbitType.CARTESIAN, PositionAngleType.MEAN,
                                                      dP, 5).
                        value(state0);

        DSFactory factory = new DSFactory(6, 1);
        Field<DerivativeStructure> field = factory.getDerivativeField();
        final FieldAbsoluteDate<DerivativeStructure> fDate = new FieldAbsoluteDate<>(field, state0.getDate());
        final Vector3D p = state0.getPosition();
        final Vector3D v = state0.getPVCoordinates().getVelocity();
        final Vector3D a = state0.getPVCoordinates().getAcceleration();
        final TimeStampedFieldPVCoordinates<DerivativeStructure> fPVA =
                        new TimeStampedFieldPVCoordinates<>(fDate,
                                        new FieldVector3D<>(factory.variable(0, p.getX()),
                                                            factory.variable(1, p.getY()),
                                                            factory.variable(2, p.getZ())),
                                        new FieldVector3D<>(factory.variable(3, v.getX()),
                                                            factory.variable(4, v.getY()),
                                                            factory.variable(5, v.getZ())),
                                        new FieldVector3D<>(factory.constant(a.getX()),
                                                            factory.constant(a.getY()),
                                                            factory.constant(a.getZ())));
        final TimeStampedFieldAngularCoordinates<DerivativeStructure> fAC =
                        new TimeStampedFieldAngularCoordinates<>(fDate,
                                        new FieldRotation<>(field, state0.getAttitude().getRotation()),
                                        new FieldVector3D<>(field, state0.getAttitude().getSpin()),
                                        new FieldVector3D<>(field, state0.getAttitude().getRotationAcceleration()));
        final FieldSpacecraftState<DerivativeStructure> fState =
                        new FieldSpacecraftState<>(new FieldCartesianOrbit<>(fPVA, state0.getFrame(), field.getZero().add(state0.getMu())),
                                                   new FieldAttitude<>(state0.getFrame(), fAC),
                                                   field.getZero().add(state0.getMass()));
        FieldVector3D<DerivativeStructure> dsJacobian = forceModel.acceleration(fState,
                                                                                forceModel.getParameters(fState.getDate().getField(), fState.getDate()));

        Vector3D dFdPXRef = new Vector3D(finiteDifferencesJacobian[0][0],
                                         finiteDifferencesJacobian[1][0],
                                         finiteDifferencesJacobian[2][0]);
        Vector3D dFdPXRes = new Vector3D(dsJacobian.getX().getPartialDerivative(1, 0, 0, 0, 0, 0),
                                         dsJacobian.getY().getPartialDerivative(1, 0, 0, 0, 0, 0),
                                         dsJacobian.getZ().getPartialDerivative(1, 0, 0, 0, 0, 0));
        Vector3D dFdPYRef = new Vector3D(finiteDifferencesJacobian[0][1],
                                         finiteDifferencesJacobian[1][1],
                                         finiteDifferencesJacobian[2][1]);
        Vector3D dFdPYRes = new Vector3D(dsJacobian.getX().getPartialDerivative(0, 1, 0, 0, 0, 0),
                                         dsJacobian.getY().getPartialDerivative(0, 1, 0, 0, 0, 0),
                                         dsJacobian.getZ().getPartialDerivative(0, 1, 0, 0, 0, 0));
        Vector3D dFdPZRef = new Vector3D(finiteDifferencesJacobian[0][2],
                                         finiteDifferencesJacobian[1][2],
                                         finiteDifferencesJacobian[2][2]);
        Vector3D dFdPZRes = new Vector3D(dsJacobian.getX().getPartialDerivative(0, 0, 1, 0, 0, 0),
                                         dsJacobian.getY().getPartialDerivative(0, 0, 1, 0, 0, 0),
                                         dsJacobian.getZ().getPartialDerivative(0, 0, 1, 0, 0, 0));
        Vector3D dFdVXRef = new Vector3D(finiteDifferencesJacobian[0][3],
                                         finiteDifferencesJacobian[1][3],
                                         finiteDifferencesJacobian[2][3]);
        Vector3D dFdVXRes = new Vector3D(dsJacobian.getX().getPartialDerivative(0, 0, 0, 1, 0, 0),
                                         dsJacobian.getY().getPartialDerivative(0, 0, 0, 1, 0, 0),
                                         dsJacobian.getZ().getPartialDerivative(0, 0, 0, 1, 0, 0));
        Vector3D dFdVYRef = new Vector3D(finiteDifferencesJacobian[0][4],
                                         finiteDifferencesJacobian[1][4],
                                         finiteDifferencesJacobian[2][4]);
        Vector3D dFdVYRes = new Vector3D(dsJacobian.getX().getPartialDerivative(0, 0, 0, 0, 1, 0),
                                         dsJacobian.getY().getPartialDerivative(0, 0, 0, 0, 1, 0),
                                         dsJacobian.getZ().getPartialDerivative(0, 0, 0, 0, 1, 0));
        Vector3D dFdVZRef = new Vector3D(finiteDifferencesJacobian[0][5],
                                         finiteDifferencesJacobian[1][5],
                                         finiteDifferencesJacobian[2][5]);
        Vector3D dFdVZRes = new Vector3D(dsJacobian.getX().getPartialDerivative(0, 0, 0, 0, 0, 1),
                                         dsJacobian.getY().getPartialDerivative(0, 0, 0, 0, 0, 1),
                                         dsJacobian.getZ().getPartialDerivative(0, 0, 0, 0, 0, 1));
        if (print) {
            System.out.println("dF/dPX ref: " + dFdPXRef.getX() + " " + dFdPXRef.getY() + " " + dFdPXRef.getZ());
            System.out.println("dF/dPX res: " + dFdPXRes.getX() + " " + dFdPXRes.getY() + " " + dFdPXRes.getZ());
            System.out.println("dF/dPY ref: " + dFdPYRef.getX() + " " + dFdPYRef.getY() + " " + dFdPYRef.getZ());
            System.out.println("dF/dPY res: " + dFdPYRes.getX() + " " + dFdPYRes.getY() + " " + dFdPYRes.getZ());
            System.out.println("dF/dPZ ref: " + dFdPZRef.getX() + " " + dFdPZRef.getY() + " " + dFdPZRef.getZ());
            System.out.println("dF/dPZ res: " + dFdPZRes.getX() + " " + dFdPZRes.getY() + " " + dFdPZRes.getZ());
            System.out.println("dF/dPX ref norm: " + dFdPXRef.getNorm() + ", abs error: " + Vector3D.distance(dFdPXRef, dFdPXRes) + ", rel error: " + (Vector3D.distance(dFdPXRef, dFdPXRes) / dFdPXRef.getNorm()));
            System.out.println("dF/dPY ref norm: " + dFdPYRef.getNorm() + ", abs error: " + Vector3D.distance(dFdPYRef, dFdPYRes) + ", rel error: " + (Vector3D.distance(dFdPYRef, dFdPYRes) / dFdPYRef.getNorm()));
            System.out.println("dF/dPZ ref norm: " + dFdPZRef.getNorm() + ", abs error: " + Vector3D.distance(dFdPZRef, dFdPZRes) + ", rel error: " + (Vector3D.distance(dFdPZRef, dFdPZRes) / dFdPZRef.getNorm()));
            System.out.println("dF/dVX ref norm: " + dFdVXRef.getNorm() + ", abs error: " + Vector3D.distance(dFdVXRef, dFdVXRes) + ", rel error: " + (Vector3D.distance(dFdVXRef, dFdVXRes) / dFdVXRef.getNorm()));
            System.out.println("dF/dVY ref norm: " + dFdVYRef.getNorm() + ", abs error: " + Vector3D.distance(dFdVYRef, dFdVYRes) + ", rel error: " + (Vector3D.distance(dFdVYRef, dFdVYRes) / dFdVYRef.getNorm()));
            System.out.println("dF/dVZ ref norm: " + dFdVZRef.getNorm() + ", abs error: " + Vector3D.distance(dFdVZRef, dFdVZRes) + ", rel error: " + (Vector3D.distance(dFdVZRef, dFdVZRes) / dFdVZRef.getNorm()));
        }
        checkdFdP(dFdPXRef, dFdPXRes, checkTolerance);
        checkdFdP(dFdPYRef, dFdPYRes, checkTolerance);
        checkdFdP(dFdPZRef, dFdPZRes, checkTolerance);
        checkdFdP(dFdVXRef, dFdVXRes, checkTolerance);
        checkdFdP(dFdVYRef, dFdVYRes, checkTolerance);
        checkdFdP(dFdVZRef, dFdVZRes, checkTolerance);
    }

    protected void checkStateJacobianVsFiniteDifferencesGradient(final  SpacecraftState state0, final ForceModel forceModel,
                                                                 final AttitudeProvider provider, final double dP,
                                                                 final double checkTolerance, final boolean print) {

        double[][] finiteDifferencesJacobian =
                        Differentiation.differentiate(state -> forceModel.acceleration(state, forceModel.getParameters(state0.getDate())).toArray(),
                                                      3, provider, OrbitType.CARTESIAN, PositionAngleType.MEAN,
                                                      dP, 5).
                        value(state0);

        final int freePrameters = 6;
        Field<Gradient> field = GradientField.getField(freePrameters);
        final FieldAbsoluteDate<Gradient> fDate = new FieldAbsoluteDate<>(field, state0.getDate());
        final Vector3D p = state0.getPosition();
        final Vector3D v = state0.getPVCoordinates().getVelocity();
        final Vector3D a = state0.getPVCoordinates().getAcceleration();
        final TimeStampedFieldPVCoordinates<Gradient> fPVA =
                        new TimeStampedFieldPVCoordinates<>(fDate,
                                        new FieldVector3D<>(Gradient.variable(freePrameters, 0, p.getX()),
                                                        Gradient.variable(freePrameters, 1, p.getY()),
                                                        Gradient.variable(freePrameters, 2, p.getZ())),
                                        new FieldVector3D<>(Gradient.variable(freePrameters, 3, v.getX()),
                                                        Gradient.variable(freePrameters, 4, v.getY()),
                                                        Gradient.variable(freePrameters, 5, v.getZ())),
                                        new FieldVector3D<>(Gradient.constant(freePrameters, a.getX()),
                                                        Gradient.constant(freePrameters, a.getY()),
                                                        Gradient.constant(freePrameters, a.getZ())));
        final TimeStampedFieldAngularCoordinates<Gradient> fAC =
                        new TimeStampedFieldAngularCoordinates<>(fDate,
                                        new FieldRotation<>(field, state0.getAttitude().getRotation()),
                                        new FieldVector3D<>(field, state0.getAttitude().getSpin()),
                                        new FieldVector3D<>(field, state0.getAttitude().getRotationAcceleration()));
        final FieldSpacecraftState<Gradient> fState =
                        new FieldSpacecraftState<>(new FieldCartesianOrbit<>(fPVA, state0.getFrame(), field.getZero().add(state0.getMu())),
                                                   new FieldAttitude<>(state0.getFrame(), fAC),
                                                   field.getZero().add(state0.getMass()));
        FieldVector3D<Gradient> gJacobian = forceModel.acceleration(fState,
                                                                     forceModel.getParameters(fState.getDate().getField(), fState.getDate()));

        Vector3D dFdPXRef = new Vector3D(finiteDifferencesJacobian[0][0],
                                         finiteDifferencesJacobian[1][0],
                                         finiteDifferencesJacobian[2][0]);
        Vector3D dFdPXRes = new Vector3D(gJacobian.getX().getPartialDerivative(0),
                                         gJacobian.getY().getPartialDerivative(0),
                                         gJacobian.getZ().getPartialDerivative(0));
        Vector3D dFdPYRef = new Vector3D(finiteDifferencesJacobian[0][1],
                                         finiteDifferencesJacobian[1][1],
                                         finiteDifferencesJacobian[2][1]);
        Vector3D dFdPYRes = new Vector3D(gJacobian.getX().getPartialDerivative(1),
                                         gJacobian.getY().getPartialDerivative(1),
                                         gJacobian.getZ().getPartialDerivative(1));
        Vector3D dFdPZRef = new Vector3D(finiteDifferencesJacobian[0][2],
                                         finiteDifferencesJacobian[1][2],
                                         finiteDifferencesJacobian[2][2]);
        Vector3D dFdPZRes = new Vector3D(gJacobian.getX().getPartialDerivative(2),
                                         gJacobian.getY().getPartialDerivative(2),
                                         gJacobian.getZ().getPartialDerivative(2));
        Vector3D dFdVXRef = new Vector3D(finiteDifferencesJacobian[0][3],
                                         finiteDifferencesJacobian[1][3],
                                         finiteDifferencesJacobian[2][3]);
        Vector3D dFdVXRes = new Vector3D(gJacobian.getX().getPartialDerivative(3),
                                         gJacobian.getY().getPartialDerivative(3),
                                         gJacobian.getZ().getPartialDerivative(3));
        Vector3D dFdVYRef = new Vector3D(finiteDifferencesJacobian[0][4],
                                         finiteDifferencesJacobian[1][4],
                                         finiteDifferencesJacobian[2][4]);
        Vector3D dFdVYRes = new Vector3D(gJacobian.getX().getPartialDerivative(4),
                                         gJacobian.getY().getPartialDerivative(4),
                                         gJacobian.getZ().getPartialDerivative(4));
        Vector3D dFdVZRef = new Vector3D(finiteDifferencesJacobian[0][5],
                                         finiteDifferencesJacobian[1][5],
                                         finiteDifferencesJacobian[2][5]);
        Vector3D dFdVZRes = new Vector3D(gJacobian.getX().getPartialDerivative(5),
                                         gJacobian.getY().getPartialDerivative(5),
                                         gJacobian.getZ().getPartialDerivative(5));
        if (print) {
            System.out.println("dF/dPX ref: " + dFdPXRef.getX() + " " + dFdPXRef.getY() + " " + dFdPXRef.getZ());
            System.out.println("dF/dPX res: " + dFdPXRes.getX() + " " + dFdPXRes.getY() + " " + dFdPXRes.getZ());
            System.out.println("dF/dPY ref: " + dFdPYRef.getX() + " " + dFdPYRef.getY() + " " + dFdPYRef.getZ());
            System.out.println("dF/dPY res: " + dFdPYRes.getX() + " " + dFdPYRes.getY() + " " + dFdPYRes.getZ());
            System.out.println("dF/dPZ ref: " + dFdPZRef.getX() + " " + dFdPZRef.getY() + " " + dFdPZRef.getZ());
            System.out.println("dF/dPZ res: " + dFdPZRes.getX() + " " + dFdPZRes.getY() + " " + dFdPZRes.getZ());
            System.out.println("dF/dPX ref norm: " + dFdPXRef.getNorm() + ", abs error: " + Vector3D.distance(dFdPXRef, dFdPXRes) + ", rel error: " + (Vector3D.distance(dFdPXRef, dFdPXRes) / dFdPXRef.getNorm()));
            System.out.println("dF/dPY ref norm: " + dFdPYRef.getNorm() + ", abs error: " + Vector3D.distance(dFdPYRef, dFdPYRes) + ", rel error: " + (Vector3D.distance(dFdPYRef, dFdPYRes) / dFdPYRef.getNorm()));
            System.out.println("dF/dPZ ref norm: " + dFdPZRef.getNorm() + ", abs error: " + Vector3D.distance(dFdPZRef, dFdPZRes) + ", rel error: " + (Vector3D.distance(dFdPZRef, dFdPZRes) / dFdPZRef.getNorm()));
            System.out.println("dF/dVX ref norm: " + dFdVXRef.getNorm() + ", abs error: " + Vector3D.distance(dFdVXRef, dFdVXRes) + ", rel error: " + (Vector3D.distance(dFdVXRef, dFdVXRes) / dFdVXRef.getNorm()));
            System.out.println("dF/dVY ref norm: " + dFdVYRef.getNorm() + ", abs error: " + Vector3D.distance(dFdVYRef, dFdVYRes) + ", rel error: " + (Vector3D.distance(dFdVYRef, dFdVYRes) / dFdVYRef.getNorm()));
            System.out.println("dF/dVZ ref norm: " + dFdVZRef.getNorm() + ", abs error: " + Vector3D.distance(dFdVZRef, dFdVZRes) + ", rel error: " + (Vector3D.distance(dFdVZRef, dFdVZRes) / dFdVZRef.getNorm()));
        }
        checkdFdP(dFdPXRef, dFdPXRes, checkTolerance);
        checkdFdP(dFdPYRef, dFdPYRes, checkTolerance);
        checkdFdP(dFdPZRef, dFdPZRes, checkTolerance);
        checkdFdP(dFdVXRef, dFdVXRes, checkTolerance);
        checkdFdP(dFdVYRef, dFdVYRes, checkTolerance);
        checkdFdP(dFdVZRef, dFdVZRes, checkTolerance);
    }

    private void checkdFdP(final Vector3D reference, final Vector3D result, final double checkTolerance) {
        if (reference.getNorm() == 0) {
            // if dF/dP is exactly zero (i.e. no dependency between F and P),
            // then the result should also be exactly zero
            Assertions.assertEquals(0, result.getNorm(), Precision.SAFE_MIN);
        } else {
            Assertions.assertEquals(0, Vector3D.distance(reference, result), checkTolerance * reference.getNorm());
        }
    }

    protected void checkStateJacobian(NumericalPropagator propagator, SpacecraftState state0,
                                      AbsoluteDate targetDate, double hFactor,
                                      double[] integratorAbsoluteTolerances, double checkTolerance) {

        propagator.setInitialState(state0);
        double[][] reference = new double[][] {
            jacobianColumn(propagator, state0, targetDate, 0, hFactor * integratorAbsoluteTolerances[0]),
            jacobianColumn(propagator, state0, targetDate, 1, hFactor * integratorAbsoluteTolerances[1]),
            jacobianColumn(propagator, state0, targetDate, 2, hFactor * integratorAbsoluteTolerances[2]),
            jacobianColumn(propagator, state0, targetDate, 3, hFactor * integratorAbsoluteTolerances[3]),
            jacobianColumn(propagator, state0, targetDate, 4, hFactor * integratorAbsoluteTolerances[4]),
            jacobianColumn(propagator, state0, targetDate, 5, hFactor * integratorAbsoluteTolerances[5])
        };
        for (int j = 0; j < 6; ++j) {
            for (int k = j + 1; k < 6; ++k) {
                double tmp = reference[j][k];
                reference[j][k] = reference[k][j];
                reference[k][j] = tmp;
            }
        }

        propagator.setInitialState(state0);
        MatricesHarvester harvester = propagator.setupMatricesComputation("stm", null, null);
        final AtomicReference<RealMatrix> dYdY0 = new AtomicReference<>();
        propagator.setStepHandler(new OrekitStepHandler() {
            public void handleStep(OrekitStepInterpolator interpolator) {
            }
            public void finish(SpacecraftState finalState) {
                // pick up final Jacobian
                dYdY0.set(harvester.getStateTransitionMatrix(finalState));
            }

        });
        propagator.propagate(targetDate);

        for (int j = 0; j < 6; ++j) {
            for (int k = 0; k < 6; ++k) {
                double scale = integratorAbsoluteTolerances[j] / integratorAbsoluteTolerances[k];
                Assertions.assertEquals(reference[j][k], dYdY0.get().getEntry(j, k), checkTolerance * scale);
            }
        }

    }


    /** Compare field numerical propagation with numerical propagation.
     *  - First compare the positions after a given propagation duration
     *  - Then shift initial state of numerical propagator and compare its final propagated state
     *    with a state obtained through Taylor expansion of the field numerical propagated "real" initial state
     * @param initialOrbit initial Keplerian orbit
     * @param positionAngleType position angle for the anomaly
     * @param duration duration of the propagation
     * @param propagator numerical propagator
     * @param fieldpropagator field numerical propagator
     * @param positionRelativeTolerancePropag relative tolerance for final position check
     * @param positionRelativeToleranceTaylor relative tolerance for position check with Taylor expansion
     * @param velocityRelativeToleranceTaylor relative tolerance for velocity check with Taylor expansion
     * @param accelerationRelativeToleranceTaylor relative tolerance for acceleration check with Taylor expansion
     * @param print if true, print the results of the test
     */
    protected void checkRealFieldPropagation(final FieldKeplerianOrbit<DerivativeStructure> initialOrbit,
                                             final PositionAngleType positionAngleType,
                                             final double duration,
                                             final NumericalPropagator propagator,
                                             final FieldNumericalPropagator<DerivativeStructure> fieldpropagator,
                                             final double positionRelativeTolerancePropag,
                                             final double positionRelativeToleranceTaylor,
                                             final double velocityRelativeToleranceTaylor,
                                             final double accelerationRelativeToleranceTaylor,
                                             final int nbTests,
                                             final boolean print) {

        // First test: Check the position after integration are the same for numerical and
        // field numerical propagators
        // ---------------------------

        // Propagate numerical and field numerical propagators
        FieldAbsoluteDate<DerivativeStructure> target = initialOrbit.getDate().shiftedBy(duration);
        FieldSpacecraftState<DerivativeStructure> finalState_DS = fieldpropagator.propagate(target);
        SpacecraftState finalState_R = propagator.propagate(target.toAbsoluteDate());
        FieldPVCoordinates<DerivativeStructure> finPVC_DS = finalState_DS.getPVCoordinates();
        PVCoordinates finPVC_R = finalState_R.getPVCoordinates();

        // Compare final positions
        final Vector3D finPosition_DS   = finPVC_DS.toPVCoordinates().getPosition();
        final Vector3D finPosition_R    = finPVC_R.getPosition();
        final double   finPositionDelta = Vector3D.distance(finPosition_DS, finPosition_R);
        final double   propagPosTol     = finPosition_R.getNorm() * positionRelativeTolerancePropag;
        if (print) {
            System.out.println("1 - Check ΔP after propagation");
            System.out.println("\t         Pf_DS = " + finPosition_DS.getX() + " " + finPosition_DS.getY() + " " + finPosition_DS.getZ());
            System.out.println("\t          Pf_R = " + finPosition_R.getX() + " " + finPosition_R.getY() + " " + finPosition_R.getZ());
            System.out.println("\t           ΔPf = " + finPositionDelta);
            System.out.println("\t            εP = " + propagPosTol);
            System.out.println("\tΔPf / ||Pf_R|| = " + finPositionDelta / finPosition_R.getNorm());
        }
        Assertions.assertEquals(0.,  finPositionDelta, propagPosTol);

        // Second test: Compare
        // - A spacecraft state (pos, vel, acc) obtained with classical numerical propagation with a randomly shifted initial state
        // - With the Taylor expansion of the field spacecraft state obtained after propagation with the field numerical propagator
        // ----------------------------

        // Set up random generator
        long number = 23091991;
        RandomGenerator RG = new Well19937a(number);
        GaussianRandomGenerator NGG = new GaussianRandomGenerator(RG);
        UncorrelatedRandomVectorGenerator URVG = new UncorrelatedRandomVectorGenerator(new double[] {0.0 , 0.0 , 0.0 , 0.0 , 0.0 , 0.0 },
                                                                                       new double[] {1e3, 0.005, 0.005, 0.01, 0.01, 0.01},
                                                                                       NGG);
        // Initial orbit values
        double a_R = initialOrbit.getA().getReal();
        double e_R = initialOrbit.getE().getReal();
        double i_R = initialOrbit.getI().getReal();
        double R_R = initialOrbit.getPerigeeArgument().getReal();
        double O_R = initialOrbit.getRightAscensionOfAscendingNode().getReal();
        double n_R = initialOrbit.getAnomaly(positionAngleType).getReal();

        // Max P, V, A values initialization
        double maxP = 0.;
        double maxV = 0.;
        double maxA = 0.;

        // Loop on the number of tests
        for (int ii = 0; ii < nbTests; ii++){

            // Shift Keplerian parameters
            double[] rand_next = URVG.nextVector();
            double a_shift = a_R + rand_next[0];
            double e_shift = e_R + rand_next[1];
            double i_shift = i_R + rand_next[2];
            double R_shift = R_R + rand_next[3];
            double O_shift = O_R + rand_next[4];
            double n_shift = n_R + rand_next[5];

            KeplerianOrbit shiftedOrb = new KeplerianOrbit(a_shift, e_shift, i_shift, R_shift, O_shift, n_shift,
                                                           PositionAngleType.MEAN,
                                                           initialOrbit.getFrame(),
                                                           initialOrbit.getDate().toAbsoluteDate(),
                                                           Constants.EIGEN5C_EARTH_MU
                                                           );
            // Shifted initial spacecraft state
            SpacecraftState shift_iSR = new SpacecraftState(shiftedOrb);

            // Propagate to duration
            propagator.setInitialState(shift_iSR);
            SpacecraftState finalState_shift = propagator.propagate(target.toAbsoluteDate());
            PVCoordinates finPVC_shift = finalState_shift.getPVCoordinates();


            // Position check
            // --------------

            // Taylor expansion of position
            FieldVector3D<DerivativeStructure> pos_DS = finPVC_DS.getPosition();
            double x_DS = pos_DS.getX().taylor(rand_next[0], rand_next[1], rand_next[2], rand_next[3], rand_next[4], rand_next[5]);
            double y_DS = pos_DS.getY().taylor(rand_next[0], rand_next[1], rand_next[2], rand_next[3], rand_next[4], rand_next[5]);
            double z_DS = pos_DS.getZ().taylor(rand_next[0], rand_next[1], rand_next[2], rand_next[3], rand_next[4], rand_next[5]);

            // Reference position
            double x = finPVC_shift.getPosition().getX();
            double y = finPVC_shift.getPosition().getY();
            double z = finPVC_shift.getPosition().getZ();

            // Compute maximum delta pos
            maxP = FastMath.max(maxP, FastMath.abs((x_DS - x) / (x - pos_DS.getX().getReal())));
            maxP = FastMath.max(maxP, FastMath.abs((y_DS - y) / (y - pos_DS.getY().getReal())));
            maxP = FastMath.max(maxP, FastMath.abs((z_DS - z) / (z - pos_DS.getZ().getReal())));


            // Velocity check
            // --------------

            // Taylor expansion of velocity
            FieldVector3D<DerivativeStructure> vel_DS = finPVC_DS.getVelocity();
            double vx_DS = vel_DS.getX().taylor(rand_next[0], rand_next[1], rand_next[2], rand_next[3], rand_next[4], rand_next[5]);
            double vy_DS = vel_DS.getY().taylor(rand_next[0], rand_next[1], rand_next[2], rand_next[3], rand_next[4], rand_next[5]);
            double vz_DS = vel_DS.getZ().taylor(rand_next[0], rand_next[1], rand_next[2], rand_next[3], rand_next[4], rand_next[5]);

            // Reference velocity
            double vx = finPVC_shift.getVelocity().getX();
            double vy = finPVC_shift.getVelocity().getY();
            double vz = finPVC_shift.getVelocity().getZ();

            // Compute maximum delta vel
            maxV = FastMath.max(maxV, FastMath.abs((vx_DS - vx) / vx));
            maxV = FastMath.max(maxV, FastMath.abs((vy_DS - vy) / vy));
            maxV = FastMath.max(maxV, FastMath.abs((vz_DS - vz) / vz));


            // Acceleration check
            // ------------------

            // Taylor expansion of acceleration
            FieldVector3D<DerivativeStructure> acc_DS = finPVC_DS.getAcceleration();
            double ax_DS = acc_DS.getX().taylor(rand_next[0], rand_next[1], rand_next[2], rand_next[3], rand_next[4], rand_next[5]);
            double ay_DS = acc_DS.getY().taylor(rand_next[0], rand_next[1], rand_next[2], rand_next[3], rand_next[4], rand_next[5]);
            double az_DS = acc_DS.getZ().taylor(rand_next[0], rand_next[1], rand_next[2], rand_next[3], rand_next[4], rand_next[5]);

            // Reference acceleration
            double ax = finPVC_shift.getAcceleration().getX();
            double ay = finPVC_shift.getAcceleration().getY();
            double az = finPVC_shift.getAcceleration().getZ();

            // Compute max accelerations
            maxA = FastMath.max(maxA, FastMath.abs((ax_DS - ax) / ax));
            maxA = FastMath.max(maxA, FastMath.abs((ay_DS - ay) / ay));
            maxA = FastMath.max(maxA, FastMath.abs((az_DS - az) / az));
        }

        // Do the checks
        if (print) {
            System.out.println("\n2 - Check P, V, A with Taylor expansion");
            System.out.println("\tMax ΔP = " + maxP);
            System.out.println("\tMax ΔV = " + maxV);
            System.out.println("\tMax ΔA = " + maxA);
        }
        Assertions.assertEquals(0, maxP, positionRelativeToleranceTaylor);
        Assertions.assertEquals(0, maxV, velocityRelativeToleranceTaylor);
        Assertions.assertEquals(0, maxA, accelerationRelativeToleranceTaylor);
    }

    /** Compare field numerical propagation with numerical propagation.
     *  - First compare the positions after a given propagation duration
     *  - Then shift initial state of numerical propagator and compare its final propagated state
     *    with a state obtained through Taylor expansion of the field numerical propagated "real" initial state
     * @param initialOrbit initial Keplerian orbit
     * @param positionAngleType position angle for the anomaly
     * @param duration duration of the propagation
     * @param propagator numerical propagator
     * @param fieldpropagator field numerical propagator
     * @param positionRelativeTolerancePropag relative tolerance for final position check
     * @param positionRelativeToleranceTaylor relative tolerance for position check with Taylor expansion
     * @param velocityRelativeToleranceTaylor relative tolerance for velocity check with Taylor expansion
     * @param accelerationRelativeToleranceTaylor relative tolerance for acceleration check with Taylor expansion
     * @param print if true, print the results of the test
     */
    protected void checkRealFieldPropagationGradient(final FieldKeplerianOrbit<Gradient> initialOrbit,
                                                     final PositionAngleType positionAngleType,
                                                     final double duration,
                                                     final NumericalPropagator propagator,
                                                     final FieldNumericalPropagator<Gradient> fieldpropagator,
                                                     final double positionRelativeTolerancePropag,
                                                     final double positionRelativeToleranceTaylor,
                                                     final double velocityRelativeToleranceTaylor,
                                                     final double accelerationRelativeToleranceTaylor,
                                                     final int nbTests,
                                                     final boolean print) {

        // First test: Check the position after integration are the same for numerical and
        // field numerical propagators
        // ---------------------------

        // Propagate numerical and field numerical propagators
        FieldAbsoluteDate<Gradient> target = initialOrbit.getDate().shiftedBy(duration);
        FieldSpacecraftState<Gradient> finalState_G = fieldpropagator.propagate(target);
        SpacecraftState finalState_R = propagator.propagate(target.toAbsoluteDate());
        FieldPVCoordinates<Gradient> finPVC_G = finalState_G.getPVCoordinates();
        PVCoordinates finPVC_R = finalState_R.getPVCoordinates();

        // Compare final positions
        final Vector3D finPosition_G   = finPVC_G.toPVCoordinates().getPosition();
        final Vector3D finPosition_R    = finPVC_R.getPosition();
        final double   finPositionDelta = Vector3D.distance(finPosition_G, finPosition_R);
        final double   propagPosTol     = finPosition_R.getNorm() * positionRelativeTolerancePropag;
        if (print) {
            System.out.println("1 - Check ΔP after propagation");
            System.out.println("\t         Pf_DS = " + finPosition_G.getX() + " " + finPosition_G.getY() + " " + finPosition_G.getZ());
            System.out.println("\t          Pf_R = " + finPosition_R.getX() + " " + finPosition_R.getY() + " " + finPosition_R.getZ());
            System.out.println("\t           ΔPf = " + finPositionDelta);
            System.out.println("\t            εP = " + propagPosTol);
            System.out.println("\tΔPf / ||Pf_R|| = " + finPositionDelta / finPosition_R.getNorm());
        }
        Assertions.assertEquals(0.,  finPositionDelta, propagPosTol);

        // Second test: Compare
        // - A spacecraft state (pos, vel, acc) obtained with classical numerical propagation with a randomly shifted initial state
        // - With the Taylor expansion of the field spacecraft state obtained after propagation with the field numerical propagator
        // ----------------------------

        // Set up random generator
        long number = 23091991;
        RandomGenerator RG = new Well19937a(number);
        GaussianRandomGenerator NGG = new GaussianRandomGenerator(RG);
        UncorrelatedRandomVectorGenerator URVG = new UncorrelatedRandomVectorGenerator(new double[] {0.0 , 0.0 , 0.0 , 0.0 , 0.0 , 0.0 },
                                                                                       new double[] {1e3, 0.005, 0.005, 0.01, 0.01, 0.01},
                                                                                       NGG);
        // Initial orbit values
        double a_R = initialOrbit.getA().getReal();
        double e_R = initialOrbit.getE().getReal();
        double i_R = initialOrbit.getI().getReal();
        double R_R = initialOrbit.getPerigeeArgument().getReal();
        double O_R = initialOrbit.getRightAscensionOfAscendingNode().getReal();
        double n_R = initialOrbit.getAnomaly(positionAngleType).getReal();

        // Max P, V, A values initialization
        double maxP = 0.;
        double maxV = 0.;
        double maxA = 0.;

        // Loop on the number of tests
        for (int ii = 0; ii < nbTests; ii++){

            // Shift Keplerian parameters
            double[] rand_next = URVG.nextVector();
            double a_shift = a_R + rand_next[0];
            double e_shift = e_R + rand_next[1];
            double i_shift = i_R + rand_next[2];
            double R_shift = R_R + rand_next[3];
            double O_shift = O_R + rand_next[4];
            double n_shift = n_R + rand_next[5];

            KeplerianOrbit shiftedOrb = new KeplerianOrbit(a_shift, e_shift, i_shift, R_shift, O_shift, n_shift,
                                                           PositionAngleType.MEAN,
                                                           initialOrbit.getFrame(),
                                                           initialOrbit.getDate().toAbsoluteDate(),
                                                           Constants.EIGEN5C_EARTH_MU
                                                           );
            // Shifted initial spacecraft state
            SpacecraftState shift_iSR = new SpacecraftState(shiftedOrb);

            // Propagate to duration
            propagator.setInitialState(shift_iSR);
            SpacecraftState finalState_shift = propagator.propagate(target.toAbsoluteDate());
            PVCoordinates finPVC_shift = finalState_shift.getPVCoordinates();


            // Position check
            // --------------

            // Taylor expansion of position
            FieldVector3D<Gradient> pos_DS = finPVC_G.getPosition();
            double x_DS = pos_DS.getX().taylor(rand_next[0], rand_next[1], rand_next[2], rand_next[3], rand_next[4], rand_next[5]);
            double y_DS = pos_DS.getY().taylor(rand_next[0], rand_next[1], rand_next[2], rand_next[3], rand_next[4], rand_next[5]);
            double z_DS = pos_DS.getZ().taylor(rand_next[0], rand_next[1], rand_next[2], rand_next[3], rand_next[4], rand_next[5]);

            // Reference position
            double x = finPVC_shift.getPosition().getX();
            double y = finPVC_shift.getPosition().getY();
            double z = finPVC_shift.getPosition().getZ();

            // Compute maximum delta pos
            maxP = FastMath.max(maxP, FastMath.abs((x_DS - x) / (x - pos_DS.getX().getReal())));
            maxP = FastMath.max(maxP, FastMath.abs((y_DS - y) / (y - pos_DS.getY().getReal())));
            maxP = FastMath.max(maxP, FastMath.abs((z_DS - z) / (z - pos_DS.getZ().getReal())));


            // Velocity check
            // --------------

            // Taylor expansion of velocity
            FieldVector3D<Gradient> vel_G = finPVC_G.getVelocity();
            double vx_G = vel_G.getX().taylor(rand_next[0], rand_next[1], rand_next[2], rand_next[3], rand_next[4], rand_next[5]);
            double vy_G = vel_G.getY().taylor(rand_next[0], rand_next[1], rand_next[2], rand_next[3], rand_next[4], rand_next[5]);
            double vz_G = vel_G.getZ().taylor(rand_next[0], rand_next[1], rand_next[2], rand_next[3], rand_next[4], rand_next[5]);

            // Reference velocity
            double vx = finPVC_shift.getVelocity().getX();
            double vy = finPVC_shift.getVelocity().getY();
            double vz = finPVC_shift.getVelocity().getZ();

            // Compute maximum delta vel
            maxV = FastMath.max(maxV, FastMath.abs((vx_G - vx) / vx));
            maxV = FastMath.max(maxV, FastMath.abs((vy_G - vy) / vy));
            maxV = FastMath.max(maxV, FastMath.abs((vz_G - vz) / vz));


            // Acceleration check
            // ------------------

            // Taylor expansion of acceleration
            FieldVector3D<Gradient> acc_G = finPVC_G.getAcceleration();
            double ax_G = acc_G.getX().taylor(rand_next[0], rand_next[1], rand_next[2], rand_next[3], rand_next[4], rand_next[5]);
            double ay_G = acc_G.getY().taylor(rand_next[0], rand_next[1], rand_next[2], rand_next[3], rand_next[4], rand_next[5]);
            double az_G = acc_G.getZ().taylor(rand_next[0], rand_next[1], rand_next[2], rand_next[3], rand_next[4], rand_next[5]);

            // Reference acceleration
            double ax = finPVC_shift.getAcceleration().getX();
            double ay = finPVC_shift.getAcceleration().getY();
            double az = finPVC_shift.getAcceleration().getZ();

            // Compute max accelerations
            maxA = FastMath.max(maxA, FastMath.abs((ax_G - ax) / ax));
            maxA = FastMath.max(maxA, FastMath.abs((ay_G - ay) / ay));
            maxA = FastMath.max(maxA, FastMath.abs((az_G - az) / az));
        }

        // Do the checks
        if (print) {
            System.out.println("\n2 - Check P, V, A with Taylor expansion");
            System.out.println("\tMax ΔP = " + maxP);
            System.out.println("\tMax ΔV = " + maxV);
            System.out.println("\tMax ΔA = " + maxA);
        }
        Assertions.assertEquals(0, maxP, positionRelativeToleranceTaylor);
        Assertions.assertEquals(0, maxV, velocityRelativeToleranceTaylor);
        Assertions.assertEquals(0, maxA, accelerationRelativeToleranceTaylor);
    }

    private double[] jacobianColumn(final NumericalPropagator propagator, final SpacecraftState state0,
                                    final AbsoluteDate targetDate, final int index,
                                    final double h)
                                            {
        return differential4(integrateShiftedState(propagator, state0, targetDate, index, -2 * h),
                             integrateShiftedState(propagator, state0, targetDate, index, -1 * h),
                             integrateShiftedState(propagator, state0, targetDate, index, +1 * h),
                             integrateShiftedState(propagator, state0, targetDate, index, +2 * h),
                             h);
    }

    private double[] integrateShiftedState(final NumericalPropagator propagator,
                                           final SpacecraftState state0,
                                           final AbsoluteDate targetDate,
                                           final int index, final double h) {
        OrbitType orbitType = propagator.getOrbitType();
        PositionAngleType angleType = propagator.getPositionAngleType();
        double[] a = new double[6];
        double[] aDot = new double[6];
        orbitType.mapOrbitToArray(state0.getOrbit(), angleType, a, aDot);
        a[index] += h;
        SpacecraftState shiftedState = new SpacecraftState(orbitType.mapArrayToOrbit(a, aDot, angleType, state0.getDate(),
                                                                                     state0.getMu(), state0.getFrame()),
                                                           state0.getAttitude(),
                                                           state0.getMass());
        propagator.setInitialState(shiftedState);
        SpacecraftState integratedState = propagator.propagate(targetDate);
        orbitType.mapOrbitToArray(integratedState.getOrbit(), angleType, a, null);
        return a;
    }

    protected double[] differential8(final double[] fM4h, final double[] fM3h, final double[] fM2h, final double[] fM1h,
                                     final double[] fP1h, final double[] fP2h, final double[] fP3h, final double[] fP4h,
                                     final double h) {

        double[] a = new double[fM4h.length];
        for (int i = 0; i < a.length; ++i) {
            a[i] = differential8(fM4h[i], fM3h[i], fM2h[i], fM1h[i], fP1h[i], fP2h[i], fP3h[i], fP4h[i], h);
        }
        return a;
    }

    protected double differential8(final double fM4h, final double fM3h, final double fM2h, final double fM1h,
                                   final double fP1h, final double fP2h, final double fP3h, final double fP4h,
                                   final double h) {

        // eight-points finite differences
        // the remaining error is -h^8/630 d^9f/dx^9 + O(h^10)
        return (-3 * (fP4h - fM4h) + 32 * (fP3h - fM3h) - 168 * (fP2h - fM2h) + 672 * (fP1h - fM1h)) / (840 * h);

    }

    protected double[] differential4(final double[] fM2h, final double[] fM1h,
                                     final double[] fP1h, final double[] fP2h,
                                     final double h) {

        double[] a = new double[fM2h.length];
        for (int i = 0; i < a.length; ++i) {
            a[i] = differential4(fM2h[i], fM1h[i], fP1h[i], fP2h[i], h);
        }
        return a;
    }

    protected double differential4(final double fM2h, final double fM1h,
                                   final double fP1h, final double fP2h,
                                   final double h) {

        // four-points finite differences
        // the remaining error is -2h^4/5 d^5f/dx^5 + O(h^6)
        return (-1 * (fP2h - fM2h) + 8 * (fP1h - fM1h)) / (12 * h);

    }

}


