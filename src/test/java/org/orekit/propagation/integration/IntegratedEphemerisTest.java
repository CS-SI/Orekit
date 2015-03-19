/* Copyright 2002-2015 CS Systèmes d'Information
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
package org.orekit.propagation.integration;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.ode.nonstiff.AdaptiveStepsizeIntegrator;
import org.apache.commons.math3.ode.nonstiff.DormandPrince853Integrator;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.errors.OrekitException;
import org.orekit.errors.PropagationException;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.propagation.BoundedPropagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.KeplerianPropagator;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.numerical.JacobiansMapper;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.propagation.numerical.PartialDerivativesEquations;
import org.orekit.propagation.sampling.OrekitStepHandler;
import org.orekit.propagation.sampling.OrekitStepInterpolator;
import org.orekit.propagation.semianalytical.dsst.DSSTPropagator;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTForceModel;
import org.orekit.propagation.semianalytical.dsst.utilities.AuxiliaryElements;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.PVCoordinates;


public class IntegratedEphemerisTest {

    @Test
    public void testNormalKeplerIntegration() throws OrekitException {

        // Keplerian propagator definition
        KeplerianPropagator keplerEx = new KeplerianPropagator(initialOrbit);

        // Integrated ephemeris

        // Propagation
        AbsoluteDate finalDate = initialOrbit.getDate().shiftedBy(Constants.JULIAN_DAY);
        numericalPropagator.setEphemerisMode();
        numericalPropagator.setInitialState(new SpacecraftState(initialOrbit));
        numericalPropagator.propagate(finalDate);
        Assert.assertTrue(numericalPropagator.getCalls() < 3200);
        BoundedPropagator ephemeris = numericalPropagator.getGeneratedEphemeris();

        // tests
        for (int i = 1; i <= Constants.JULIAN_DAY; i++) {
            AbsoluteDate intermediateDate = initialOrbit.getDate().shiftedBy(i);
            SpacecraftState keplerIntermediateOrbit = keplerEx.propagate(intermediateDate);
            SpacecraftState numericIntermediateOrbit = ephemeris.propagate(intermediateDate);
            Vector3D kepPosition = keplerIntermediateOrbit.getPVCoordinates().getPosition();
            Vector3D numPosition = numericIntermediateOrbit.getPVCoordinates().getPosition();
            Assert.assertEquals(0, kepPosition.subtract(numPosition).getNorm(), 0.06);
        }

        // test inv
        AbsoluteDate intermediateDate = initialOrbit.getDate().shiftedBy(41589);
        SpacecraftState keplerIntermediateOrbit = keplerEx.propagate(intermediateDate);
        SpacecraftState state = keplerEx.propagate(finalDate);
        numericalPropagator.setInitialState(state);
        numericalPropagator.setEphemerisMode();
        numericalPropagator.propagate(initialOrbit.getDate());
        BoundedPropagator invEphemeris = numericalPropagator.getGeneratedEphemeris();
        SpacecraftState numericIntermediateOrbit = invEphemeris.propagate(intermediateDate);
        Vector3D kepPosition = keplerIntermediateOrbit.getPVCoordinates().getPosition();
        Vector3D numPosition = numericIntermediateOrbit.getPVCoordinates().getPosition();
        Assert.assertEquals(0, kepPosition.subtract(numPosition).getNorm(), 10e-2);

    }

    @Test
    public void testPartialDerivativesIssue16() throws OrekitException {

        final String eqName = "derivatives";
        numericalPropagator.setEphemerisMode();
        numericalPropagator.setOrbitType(OrbitType.CARTESIAN);
        final PartialDerivativesEquations derivatives =
            new PartialDerivativesEquations(eqName, numericalPropagator);
        final SpacecraftState initialState =
                derivatives.setInitialJacobians(new SpacecraftState(initialOrbit), 6, 0);
        final JacobiansMapper mapper = derivatives.getMapper();
        numericalPropagator.setInitialState(initialState);
        numericalPropagator.propagate(initialOrbit.getDate().shiftedBy(3600.0));
        BoundedPropagator ephemeris = numericalPropagator.getGeneratedEphemeris();
        ephemeris.setMasterMode(new OrekitStepHandler() {

            private final Array2DRowRealMatrix dYdY0 = new Array2DRowRealMatrix(6, 6);

            public void init(SpacecraftState s0, AbsoluteDate t) {
            }

            public void handleStep(OrekitStepInterpolator interpolator, boolean isLast)
            throws PropagationException {
                try {
                    SpacecraftState state = interpolator.getInterpolatedState();
                    Assert.assertEquals(mapper.getAdditionalStateDimension(),
                                        state.getAdditionalState(eqName).length);
                    mapper.getStateJacobian(state, dYdY0.getDataRef());
                    mapper.getParametersJacobian(state, null); // no parameters, this is a no-op and should work
                    RealMatrix deltaId = dYdY0.subtract(MatrixUtils.createRealIdentityMatrix(6));
                    Assert.assertTrue(deltaId.getNorm() >  100);
                    Assert.assertTrue(deltaId.getNorm() < 3100);
                } catch (OrekitException oe) {
                    throw new PropagationException(oe);
                }
            }

        });

        ephemeris.propagate(initialOrbit.getDate().shiftedBy(1800.0));

    }
    
    @Test
    public void testGetFrame() throws PropagationException, OrekitException {
        // setup
        AbsoluteDate finalDate = initialOrbit.getDate().shiftedBy(Constants.JULIAN_DAY);
        numericalPropagator.setEphemerisMode();
        numericalPropagator.setInitialState(new SpacecraftState(initialOrbit));
        numericalPropagator.propagate(finalDate);
        Assert.assertTrue(numericalPropagator.getCalls() < 3200);
        BoundedPropagator ephemeris = numericalPropagator.getGeneratedEphemeris();
        
        //action
        Assert.assertNotNull(ephemeris.getFrame());
        Assert.assertSame(ephemeris.getFrame(), numericalPropagator.getFrame());
    }

    @Test
    public void testSerializationNumerical() throws PropagationException, OrekitException, IOException, ClassNotFoundException {

        AbsoluteDate finalDate = initialOrbit.getDate().shiftedBy(Constants.JULIAN_DAY);
        numericalPropagator.setEphemerisMode();
        numericalPropagator.setInitialState(new SpacecraftState(initialOrbit));
        numericalPropagator.propagate(finalDate);
        IntegratedEphemeris ephemeris = (IntegratedEphemeris) numericalPropagator.getGeneratedEphemeris();
        
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream    oos = new ObjectOutputStream(bos);
        oos.writeObject(ephemeris);

        Assert.assertTrue(bos.size() > 192000);
        Assert.assertTrue(bos.size() < 193000);

        Assert.assertNotNull(ephemeris.getFrame());
        Assert.assertSame(ephemeris.getFrame(), numericalPropagator.getFrame());
        ByteArrayInputStream  bis = new ByteArrayInputStream(bos.toByteArray());
        ObjectInputStream     ois = new ObjectInputStream(bis);
        IntegratedEphemeris deserialized  = (IntegratedEphemeris) ois.readObject();
        Assert.assertEquals(deserialized.getMinDate(), deserialized.getMinDate());
        Assert.assertEquals(deserialized.getMaxDate(), deserialized.getMaxDate());

    }

    @Test
    public void testSerializationDSST() throws PropagationException, OrekitException, IOException, ClassNotFoundException {

        AbsoluteDate finalDate = initialOrbit.getDate().shiftedBy(Constants.JULIAN_DAY);
        final double[][] tol = DSSTPropagator.tolerances(1.0, initialOrbit);
        AdaptiveStepsizeIntegrator integrator = new DormandPrince853Integrator(10, Constants.JULIAN_DAY, tol[0], tol[1]);
        DSSTPropagator dsstProp = new DSSTPropagator(integrator);
        dsstProp.setInitialState(new SpacecraftState(initialOrbit), false);
        dsstProp.setEphemerisMode();
        dsstProp.propagate(finalDate);
        IntegratedEphemeris ephemeris = (IntegratedEphemeris) dsstProp.getGeneratedEphemeris();
        
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream    oos = new ObjectOutputStream(bos);
        oos.writeObject(ephemeris);

        Assert.assertTrue(bos.size() > 8000);
        Assert.assertTrue(bos.size() < 9000);

        Assert.assertNotNull(ephemeris.getFrame());
        Assert.assertSame(ephemeris.getFrame(), dsstProp.getFrame());
        ByteArrayInputStream  bis = new ByteArrayInputStream(bos.toByteArray());
        ObjectInputStream     ois = new ObjectInputStream(bis);
        IntegratedEphemeris deserialized  = (IntegratedEphemeris) ois.readObject();
        Assert.assertEquals(deserialized.getMinDate(), deserialized.getMinDate());
        Assert.assertEquals(deserialized.getMaxDate(), deserialized.getMaxDate());

    }

    @Test(expected=NotSerializableException.class)
    public void testSerializationDSSTNotSerializableForceModel() throws PropagationException, OrekitException, IOException, ClassNotFoundException {

        AbsoluteDate finalDate = initialOrbit.getDate().shiftedBy(Constants.JULIAN_DAY);
        final double[][] tol = DSSTPropagator.tolerances(1.0, initialOrbit);
        AdaptiveStepsizeIntegrator integrator = new DormandPrince853Integrator(10, Constants.JULIAN_DAY, tol[0], tol[1]);
        DSSTPropagator dsstProp = new DSSTPropagator(integrator);
        dsstProp.setInitialState(new SpacecraftState(initialOrbit), false);

        // set up a dummy not serializable force model
        dsstProp.addForceModel(new DSSTForceModel() {

            public void initializeStep(AuxiliaryElements aux) {
            }

            public void initialize(AuxiliaryElements aux, boolean meanOnly) {
            }

            public double[] getShortPeriodicVariations(AbsoluteDate date,
                                                       double[] meanElements) {
                return new double[6];
            }

            public double[] getMeanElementRate(SpacecraftState state) {
                return new double[6];
            }

            public EventDetector[] getEventsDetectors() {
                return null;
            }

            @Override
            public void registerAttitudeProvider(AttitudeProvider provider) {
            }

            public void computeShortPeriodicsCoefficients(SpacecraftState state) {
            }

            @Override
            public void resetShortPeriodicsCoefficients() {
            }
        });

        dsstProp.setEphemerisMode();
        dsstProp.propagate(finalDate);
        IntegratedEphemeris ephemeris = (IntegratedEphemeris) dsstProp.getGeneratedEphemeris();
        
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream    oos = new ObjectOutputStream(bos);
        oos.writeObject(ephemeris);

    }

    @Before
    public void setUp() {
        // Definition of initial conditions with position and velocity
        Vector3D position = new Vector3D(7.0e6, 1.0e6, 4.0e6);
        Vector3D velocity = new Vector3D(-500.0, 8000.0, 1000.0);
        double mu = 3.9860047e14;

        AbsoluteDate initDate = AbsoluteDate.J2000_EPOCH.shiftedBy(584.);
        initialOrbit =
            new EquinoctialOrbit(new PVCoordinates(position, velocity),
                                 FramesFactory.getEME2000(), initDate, mu);

        // Numerical propagator definition
        double[] absTolerance = {
            0.0001, 1.0e-11, 1.0e-11, 1.0e-8, 1.0e-8, 1.0e-8, 0.001
        };
        double[] relTolerance = {
            1.0e-8, 1.0e-8, 1.0e-8, 1.0e-9, 1.0e-9, 1.0e-9, 1.0e-7
        };
        AdaptiveStepsizeIntegrator integrator =
            new DormandPrince853Integrator(0.001, 500, absTolerance, relTolerance);
        integrator.setInitialStepSize(100);
        numericalPropagator = new NumericalPropagator(integrator);

    }

    private Orbit initialOrbit;
    private NumericalPropagator numericalPropagator;

}
