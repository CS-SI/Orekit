/* Copyright 2002-2016 CS Systèmes d'Information
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


import org.hipparchus.analysis.differentiation.DerivativeStructure;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.junit.Assert;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.Frame;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.numerical.JacobiansMapper;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.propagation.numerical.PartialDerivativesEquations;
import org.orekit.propagation.numerical.TimeDerivativesEquations;
import org.orekit.propagation.sampling.OrekitStepHandler;
import org.orekit.propagation.sampling.OrekitStepInterpolator;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.ParameterDriver;


public abstract class AbstractForceModelTest {

    protected static class AccelerationRetriever implements TimeDerivativesEquations {

        private Vector3D acceleration;

        public AccelerationRetriever() {
            acceleration = Vector3D.ZERO;
        }

        public void addKeplerContribution(double mu) {
        }

        public void addXYZAcceleration(double x, double y, double z) {
            acceleration = new Vector3D(x, y, z);
        }

        public void addAcceleration(Vector3D gamma, Frame frame) {
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
        orbitType.mapOrbitToArray(state0.getOrbit(), angleType, a);
        a[index] += h;
        SpacecraftState shiftedState = new SpacecraftState(orbitType.mapArrayToOrbit(a, angleType, state0.getDate(),
                                                                                     state0.getMu(), state0.getFrame()),
                                                           state0.getAttitude(),
                                                           state0.getMass());
        propagator.setInitialState(shiftedState);
        SpacecraftState integratedState = propagator.propagate(targetDate);
        orbitType.mapOrbitToArray(integratedState.getOrbit(), angleType, a);
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


