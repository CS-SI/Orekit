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
package org.orekit.propagation.integration;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.linear.Array2DRowRealMatrix;
import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.ode.nonstiff.AdaptiveStepsizeIntegrator;
import org.hipparchus.ode.nonstiff.DormandPrince853Integrator;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.bodies.CelestialBody;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.errors.OrekitException;
import org.orekit.forces.gravity.HolmesFeatherstoneAttractionModel;
import org.orekit.forces.gravity.ThirdBodyAttraction;
import org.orekit.forces.gravity.potential.GravityFieldFactory;
import org.orekit.forces.gravity.potential.ICGEMFormatReader;
import org.orekit.forces.gravity.potential.NormalizedSphericalHarmonicsProvider;
import org.orekit.forces.gravity.potential.UnnormalizedSphericalHarmonicsProvider;
import org.orekit.forces.radiation.IsotropicRadiationSingleCoefficient;
import org.orekit.forces.radiation.RadiationSensitive;
import org.orekit.forces.radiation.SolarRadiationPressure;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.propagation.BoundedPropagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.KeplerianPropagator;
import org.orekit.propagation.numerical.JacobiansMapper;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.propagation.numerical.PartialDerivativesEquations;
import org.orekit.propagation.sampling.OrekitStepHandler;
import org.orekit.propagation.sampling.OrekitStepInterpolator;
import org.orekit.propagation.semianalytical.dsst.DSSTPropagator;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTSolarRadiationPressure;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTTesseral;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTThirdBody;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTZonal;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
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
                derivatives.setInitialJacobians(new SpacecraftState(initialOrbit), 6);
        final JacobiansMapper mapper = derivatives.getMapper();
        numericalPropagator.setInitialState(initialState);
        numericalPropagator.propagate(initialOrbit.getDate().shiftedBy(3600.0));
        BoundedPropagator ephemeris = numericalPropagator.getGeneratedEphemeris();
        ephemeris.setMasterMode(new OrekitStepHandler() {

            private final Array2DRowRealMatrix dYdY0 = new Array2DRowRealMatrix(6, 6);

            public void handleStep(OrekitStepInterpolator interpolator, boolean isLast)
                throws OrekitException {
                SpacecraftState state = interpolator.getCurrentState();
                Assert.assertEquals(mapper.getAdditionalStateDimension(),
                                    state.getAdditionalState(eqName).length);
                mapper.getStateJacobian(state, dYdY0.getDataRef());
                mapper.getParametersJacobian(state, null); // no parameters, this is a no-op and should work
                RealMatrix deltaId = dYdY0.subtract(MatrixUtils.createRealIdentityMatrix(6));
                Assert.assertTrue(deltaId.getNorm() >  100);
                Assert.assertTrue(deltaId.getNorm() < 3100);
            }

        });

        ephemeris.propagate(initialOrbit.getDate().shiftedBy(1800.0));

    }

    @Test
    public void testGetFrame() throws OrekitException {
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
    public void testSerializationNumerical() throws OrekitException, IOException, ClassNotFoundException {

        AbsoluteDate finalDate = initialOrbit.getDate().shiftedBy(Constants.JULIAN_DAY);
        numericalPropagator.setEphemerisMode();
        numericalPropagator.setInitialState(new SpacecraftState(initialOrbit));

        final Frame itrf = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
        final NormalizedSphericalHarmonicsProvider gravity =
                        GravityFieldFactory.getNormalizedProvider(8, 8);
        final CelestialBody sun  = CelestialBodyFactory.getSun();
        final CelestialBody moon = CelestialBodyFactory.getMoon();
        final RadiationSensitive spacecraft = new IsotropicRadiationSingleCoefficient(20.0, 2.0);
        numericalPropagator.addForceModel(new HolmesFeatherstoneAttractionModel(itrf, gravity));
        numericalPropagator.addForceModel(new ThirdBodyAttraction(sun));
        numericalPropagator.addForceModel(new ThirdBodyAttraction(moon));
        numericalPropagator.addForceModel(new SolarRadiationPressure(sun,
                                                                     Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                                     spacecraft));

        numericalPropagator.propagate(finalDate);
        IntegratedEphemeris ephemeris = (IntegratedEphemeris) numericalPropagator.getGeneratedEphemeris();

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream    oos = new ObjectOutputStream(bos);
        oos.writeObject(ephemeris);

        int expectedSize = 258223;
        Assert.assertTrue("size = " + bos.size (), bos.size () >  9 * expectedSize / 10);
        Assert.assertTrue("size = " + bos.size (), bos.size () < 11 * expectedSize / 10);

        Assert.assertNotNull(ephemeris.getFrame());
        Assert.assertSame(ephemeris.getFrame(), numericalPropagator.getFrame());
        ByteArrayInputStream  bis = new ByteArrayInputStream(bos.toByteArray());
        ObjectInputStream     ois = new ObjectInputStream(bis);
        IntegratedEphemeris deserialized  = (IntegratedEphemeris) ois.readObject();
        Assert.assertEquals(deserialized.getMinDate(), deserialized.getMinDate());
        Assert.assertEquals(deserialized.getMaxDate(), deserialized.getMaxDate());

    }

    @Test
    public void testSerializationDSSTMean()
        throws OrekitException, IOException, ClassNotFoundException {
        doTestSerializationDSST(true, 36703);
    }

    @Test
    public void testSerializationDSSTOsculating()
        throws OrekitException, IOException, ClassNotFoundException {
        doTestSerializationDSST(false, 618025);
    }

    private void doTestSerializationDSST(boolean meanOnly, int expectedSize)
        throws OrekitException, IOException, ClassNotFoundException {

        AbsoluteDate finalDate = initialOrbit.getDate().shiftedBy(Constants.JULIAN_DAY);
        final double[][] tol = DSSTPropagator.tolerances(1.0, initialOrbit);
        AdaptiveStepsizeIntegrator integrator = new DormandPrince853Integrator(10, Constants.JULIAN_DAY, tol[0], tol[1]);
        DSSTPropagator dsstProp = new DSSTPropagator(integrator, meanOnly);
        dsstProp.setInitialState(new SpacecraftState(initialOrbit), false);
        dsstProp.setEphemerisMode();

        final Frame itrf = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
        final UnnormalizedSphericalHarmonicsProvider gravity =
                        GravityFieldFactory.getUnnormalizedProvider(8, 8);
        final CelestialBody sun  = CelestialBodyFactory.getSun();
        final CelestialBody moon = CelestialBodyFactory.getMoon();
        final RadiationSensitive spacecraft = new IsotropicRadiationSingleCoefficient(20.0, 2.0);
        dsstProp.addForceModel(new DSSTZonal(gravity, 8, 7, 17));
        dsstProp.addForceModel(new DSSTTesseral(itrf, Constants.WGS84_EARTH_ANGULAR_VELOCITY,
                                                gravity, 8, 8, 4, 12, 8, 8, 4));
        dsstProp.addForceModel(new DSSTThirdBody(sun));
        dsstProp.addForceModel(new DSSTThirdBody(moon));
        dsstProp.addForceModel(new DSSTSolarRadiationPressure(sun, Constants.WGS84_EARTH_EQUATORIAL_RADIUS, spacecraft));

        dsstProp.propagate(finalDate);
        IntegratedEphemeris ephemeris = (IntegratedEphemeris) dsstProp.getGeneratedEphemeris();

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream    oos = new ObjectOutputStream(bos);
        oos.writeObject(ephemeris);

        Assert.assertTrue("size = " + bos.size (), bos.size () >  9 * expectedSize / 10);
        Assert.assertTrue("size = " + bos.size (), bos.size () < 11 * expectedSize / 10);


        Assert.assertNotNull(ephemeris.getFrame());
        Assert.assertSame(ephemeris.getFrame(), dsstProp.getFrame());
        ByteArrayInputStream  bis = new ByteArrayInputStream(bos.toByteArray());
        ObjectInputStream     ois = new ObjectInputStream(bis);
        IntegratedEphemeris deserialized  = (IntegratedEphemeris) ois.readObject();
        Assert.assertEquals(deserialized.getMinDate(), deserialized.getMinDate());
        Assert.assertEquals(deserialized.getMaxDate(), deserialized.getMaxDate());

    }

    @Before
    public void setUp() {

        Utils.setDataRoot("regular-data:potential/icgem-format");
        GravityFieldFactory.addPotentialCoefficientsReader(new ICGEMFormatReader("eigen-6s-truncated", true));

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
