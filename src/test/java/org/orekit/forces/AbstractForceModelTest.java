/* Copyright 2002-2017 CS Systèmes d'Information
 * Licensed to CS Systèmes d'Information (CS) under one or more
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
import org.hipparchus.geometry.euclidean.threed.FieldRotation;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.Precision;
import org.junit.Assert;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.attitudes.FieldAttitude;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.FieldCartesianOrbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.numerical.JacobiansMapper;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.propagation.numerical.PartialDerivativesEquations;
import org.orekit.propagation.numerical.TimeDerivativesEquations;
import org.orekit.propagation.sampling.OrekitStepHandler;
import org.orekit.propagation.sampling.OrekitStepInterpolator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.Differentiation;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.TimeStampedFieldAngularCoordinates;
import org.orekit.utils.TimeStampedFieldPVCoordinates;


public abstract class AbstractForceModelTest {

    protected static class AccelerationRetriever implements TimeDerivativesEquations {

        private Vector3D acceleration;

        public AccelerationRetriever() {
            acceleration = Vector3D.ZERO;
        }

        public void addKeplerContribution(double mu) {
        }

        public void addNonKeplerianAcceleration(Vector3D gamma) {
            acceleration = gamma;
        }

        public void addMassDerivative(double q) {
        }

        public Vector3D getAcceleration() {
            return acceleration;
        }

    }

    protected void checkParameterDerivative(SpacecraftState state,
                                            ForceModel forceModel, String name,
                                            double hFactor, double tol)
        throws OrekitException {

        try {
            forceModel.accelerationDerivatives(state, "not a parameter");
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            // expected
            Assert.assertEquals(OrekitMessages.UNSUPPORTED_PARAMETER_NAME, oe.getSpecifier());
        }
        FieldVector3D<DerivativeStructure> accDer = forceModel.accelerationDerivatives(state, name);
        Vector3D derivative = new Vector3D(accDer.getX().getPartialDerivative(1),
                                           accDer.getY().getPartialDerivative(1),
                                           accDer.getZ().getPartialDerivative(1));

        AccelerationRetriever accelerationRetriever = new AccelerationRetriever();
        ParameterDriver driver = null;
        for (ParameterDriver d : forceModel.getParametersDrivers()) {
            if (d.getName().equals(name)) {
                driver = d;
            }
        }
        double p0 = driver.getValue();
        double hParam = hFactor * p0;
        driver.setValue(p0 - 1 * hParam);
        Assert.assertEquals(p0 - 1 * hParam, driver.getValue(), 1.0e-10);
        forceModel.addContribution(state, accelerationRetriever);
        final Vector3D gammaM1h = accelerationRetriever.getAcceleration();
        driver.setValue(p0 + 1 * hParam);
        Assert.assertEquals(p0 + 1 * hParam, driver.getValue(), 1.0e-10);
        forceModel.addContribution(state, accelerationRetriever);
        final Vector3D gammaP1h = accelerationRetriever.getAcceleration();

        final Vector3D reference = new Vector3D(  1 / (2 * hParam), gammaP1h.subtract(gammaM1h));
        final Vector3D delta = derivative.subtract(reference);
        Assert.assertEquals(0, delta.getNorm(), tol * reference.getNorm());

    }

    protected FieldVector3D<DerivativeStructure> accelerationDerivatives(ForceModel forceModel, FieldSpacecraftState<DerivativeStructure> fState)
        throws OrekitException {
        // TODO: should be removed from interface and pushed into test classes
        return forceModel.accelerationDerivatives(fState.getDate().toAbsoluteDate(), FramesFactory.getEME2000(),
                                                  fState.getPVCoordinates().getPosition(),
                                                  fState.getPVCoordinates().getVelocity(),
                                                  fState.getAttitude().getRotation(),
                                                  fState.getMass());
    }

    protected void checkStateJacobianVs80Implementation(final SpacecraftState state, final ForceModel forceModel,
                                                        final double checkTolerance, final boolean print)
        throws OrekitException {
        final Vector3D p = state.getPVCoordinates().getPosition();
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
        final TimeStampedFieldAngularCoordinates<DerivativeStructure> fAC =
                        new TimeStampedFieldAngularCoordinates<>(fDate,
                                        new FieldRotation<>(field, state.getAttitude().getRotation()),
                                        new FieldVector3D<>(field, state.getAttitude().getSpin()),
                                        new FieldVector3D<>(field, state.getAttitude().getRotationAcceleration()));
        final FieldSpacecraftState<DerivativeStructure> fState =
                        new FieldSpacecraftState<>(new FieldCartesianOrbit<>(fPVA, state.getFrame(), state.getMu()),
                                                   new FieldAttitude<>(state.getFrame(), fAC),
                                                   field.getZero().add(state.getMass()));
        FieldVector3D<DerivativeStructure> dsNew = forceModel.acceleration(fState);
        FieldVector3D<DerivativeStructure> dsOld = accelerationDerivatives(forceModel, fState);
        Vector3D dFdPXRef = new Vector3D(dsOld.getX().getPartialDerivative(1, 0, 0, 0, 0, 0),
                                         dsOld.getY().getPartialDerivative(1, 0, 0, 0, 0, 0),
                                         dsOld.getZ().getPartialDerivative(1, 0, 0, 0, 0, 0));
        Vector3D dFdPXRes = new Vector3D(dsNew.getX().getPartialDerivative(1, 0, 0, 0, 0, 0),
                                         dsNew.getY().getPartialDerivative(1, 0, 0, 0, 0, 0),
                                         dsNew.getZ().getPartialDerivative(1, 0, 0, 0, 0, 0));
        Vector3D dFdPYRef = new Vector3D(dsOld.getX().getPartialDerivative(0, 1, 0, 0, 0, 0),
                                         dsOld.getY().getPartialDerivative(0, 1, 0, 0, 0, 0),
                                         dsOld.getZ().getPartialDerivative(0, 1, 0, 0, 0, 0));
        Vector3D dFdPYRes = new Vector3D(dsNew.getX().getPartialDerivative(0, 1, 0, 0, 0, 0),
                                         dsNew.getY().getPartialDerivative(0, 1, 0, 0, 0, 0),
                                         dsNew.getZ().getPartialDerivative(0, 1, 0, 0, 0, 0));
        Vector3D dFdPZRef = new Vector3D(dsOld.getX().getPartialDerivative(0, 0, 1, 0, 0, 0),
                                         dsOld.getY().getPartialDerivative(0, 0, 1, 0, 0, 0),
                                         dsOld.getZ().getPartialDerivative(0, 0, 1, 0, 0, 0));
        Vector3D dFdPZRes = new Vector3D(dsNew.getX().getPartialDerivative(0, 0, 1, 0, 0, 0),
                                         dsNew.getY().getPartialDerivative(0, 0, 1, 0, 0, 0),
                                         dsNew.getZ().getPartialDerivative(0, 0, 1, 0, 0, 0));
        Vector3D dFdVXRef = new Vector3D(dsOld.getX().getPartialDerivative(0, 0, 0, 1, 0, 0),
                                         dsOld.getY().getPartialDerivative(0, 0, 0, 1, 0, 0),
                                         dsOld.getZ().getPartialDerivative(0, 0, 0, 1, 0, 0));
        Vector3D dFdVXRes = new Vector3D(dsNew.getX().getPartialDerivative(0, 0, 0, 1, 0, 0),
                                         dsNew.getY().getPartialDerivative(0, 0, 0, 1, 0, 0),
                                         dsNew.getZ().getPartialDerivative(0, 0, 0, 1, 0, 0));
        Vector3D dFdVYRef = new Vector3D(dsOld.getX().getPartialDerivative(0, 0, 0, 0, 1, 0),
                                         dsOld.getY().getPartialDerivative(0, 0, 0, 0, 1, 0),
                                         dsOld.getZ().getPartialDerivative(0, 0, 0, 0, 1, 0));
        Vector3D dFdVYRes = new Vector3D(dsNew.getX().getPartialDerivative(0, 0, 0, 0, 1, 0),
                                         dsNew.getY().getPartialDerivative(0, 0, 0, 0, 1, 0),
                                         dsNew.getZ().getPartialDerivative(0, 0, 0, 0, 1, 0));
        Vector3D dFdVZRef = new Vector3D(dsOld.getX().getPartialDerivative(0, 0, 0, 0, 0, 1),
                                         dsOld.getY().getPartialDerivative(0, 0, 0, 0, 0, 1),
                                         dsOld.getZ().getPartialDerivative(0, 0, 0, 0, 0, 1));
        Vector3D dFdVZRes = new Vector3D(dsNew.getX().getPartialDerivative(0, 0, 0, 0, 0, 1),
                                         dsNew.getY().getPartialDerivative(0, 0, 0, 0, 0, 1),
                                         dsNew.getZ().getPartialDerivative(0, 0, 0, 0, 0, 1));
        if (print) {
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

    protected void checkStateJacobianVsFiniteDifferences(final  SpacecraftState state0, final ForceModel forceModel,
                                                         final AttitudeProvider provider, final double dP,
                                                         final double checkTolerance, final boolean print)
        throws OrekitException {

        double[][] finiteDifferencesJacobian =
                        Differentiation.differentiate(state -> forceModel.acceleration(state).toArray(),
                                                      3, provider, OrbitType.CARTESIAN, PositionAngle.MEAN,
                                                      dP, 5).
                        value(state0);

        DSFactory factory = new DSFactory(6, 1);
        Field<DerivativeStructure> field = factory.getDerivativeField();
        final FieldAbsoluteDate<DerivativeStructure> fDate = new FieldAbsoluteDate<>(field, state0.getDate());
        final Vector3D p = state0.getPVCoordinates().getPosition();
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
                        new FieldSpacecraftState<>(new FieldCartesianOrbit<>(fPVA, state0.getFrame(), state0.getMu()),
                                                   new FieldAttitude<>(state0.getFrame(), fAC),
                                                   field.getZero().add(state0.getMass()));
        FieldVector3D<DerivativeStructure> dsJacobian = forceModel.acceleration(fState);

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
            Assert.assertEquals(0, result.getNorm(), Precision.SAFE_MIN);
        } else {
            Assert.assertEquals(0, Vector3D.distance(reference, result), checkTolerance * reference.getNorm());
        }
    }

    protected void checkStateJacobian(NumericalPropagator propagator, SpacecraftState state0,
                                      AbsoluteDate targetDate, double hFactor,
                                      double[] integratorAbsoluteTolerances, double checkTolerance)
        throws OrekitException {

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

        final String name = "pde";
        PartialDerivativesEquations pde = new PartialDerivativesEquations(name, propagator);
        propagator.setInitialState(pde.setInitialJacobians(state0, 6));
        final JacobiansMapper mapper = pde.getMapper();
        final double[][] dYdY0 = new double[6][6];
        propagator.setMasterMode(new OrekitStepHandler() {

            public void handleStep(OrekitStepInterpolator interpolator, boolean isLast)
                throws OrekitException {
                if (isLast) {
                    // pick up final Jacobian
                    mapper.getStateJacobian(interpolator.getCurrentState(), dYdY0);
                }
            }

        });
        propagator.propagate(targetDate);

        for (int j = 0; j < 6; ++j) {
            for (int k = 0; k < 6; ++k) {
                double scale = integratorAbsoluteTolerances[j] / integratorAbsoluteTolerances[k];
                Assert.assertEquals(reference[j][k], dYdY0[j][k], checkTolerance * scale);
            }
        }

    }

    private double[] jacobianColumn(final NumericalPropagator propagator, final SpacecraftState state0,
                                    final AbsoluteDate targetDate, final int index,
                                    final double h)
                                            throws OrekitException {
        return differential4(integrateShiftedState(propagator, state0, targetDate, index, -2 * h),
                             integrateShiftedState(propagator, state0, targetDate, index, -1 * h),
                             integrateShiftedState(propagator, state0, targetDate, index, +1 * h),
                             integrateShiftedState(propagator, state0, targetDate, index, +2 * h),
                             h);
    }

    private double[] integrateShiftedState(final NumericalPropagator propagator,
                                           final SpacecraftState state0,
                                           final AbsoluteDate targetDate,
                                           final int index, final double h)
        throws OrekitException {
        OrbitType orbitType = propagator.getOrbitType();
        PositionAngle angleType = propagator.getPositionAngleType();
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


