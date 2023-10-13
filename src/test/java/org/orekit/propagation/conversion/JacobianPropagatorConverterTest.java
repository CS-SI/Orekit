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
package org.orekit.propagation.conversion;

import org.hipparchus.analysis.MultivariateVectorFunction;
import org.hipparchus.exception.LocalizedCoreFormats;
import org.hipparchus.exception.MathIllegalArgumentException;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.linear.ArrayRealVector;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.linear.RealVector;
import org.hipparchus.random.RandomGenerator;
import org.hipparchus.random.Well19937a;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.Pair;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.forces.ForceModel;
import org.orekit.forces.drag.DragForce;
import org.orekit.forces.drag.DragSensitive;
import org.orekit.forces.drag.IsotropicDrag;
import org.orekit.forces.gravity.HolmesFeatherstoneAttractionModel;
import org.orekit.forces.gravity.NewtonianAttraction;
import org.orekit.forces.gravity.potential.GravityFieldFactory;
import org.orekit.frames.FramesFactory;
import org.orekit.models.earth.atmosphere.Atmosphere;
import org.orekit.models.earth.atmosphere.SimpleExponentialAtmosphere;
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.ParameterDriver;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

public class JacobianPropagatorConverterTest {

    private double mu;
    private double dP;

    private Orbit orbit;
    private ForceModel gravity;
    private ForceModel drag;
    private Atmosphere atmosphere;
    private double crossSection;

    @Test
    public void testDerivativesNothing() {
        try {
            doTestDerivatives(1.0, 1.0);
            Assertions.fail("an exception should have been thrown");
        } catch (MathIllegalArgumentException miae) {
            Assertions.assertEquals(LocalizedCoreFormats.AT_LEAST_ONE_COLUMN, miae.getSpecifier());
        }
    }

    @Test
    public void testDerivativesOrbitOnly() {
        doTestDerivatives(4.8e-9, 3.5e-12,
                          "Px", "Py", "Pz", "Vx", "Vy", "Vz");
    }

    @Test
    public void testDerivativesPositionAndDrag() {
        doTestDerivatives(5.1e-9, 4.8e-12,
                          "Px", "Py", "Pz", DragSensitive.DRAG_COEFFICIENT);
    }

    @Test
    public void testDerivativesDrag() {
        doTestDerivatives(3.2e-9, 3.2e-12,
                          DragSensitive.DRAG_COEFFICIENT);
    }

    @Test
    public void testDerivativesCentralAttraction() {
        doTestDerivatives(3.6e-9, 4.0e-12,
                          NewtonianAttraction.CENTRAL_ATTRACTION_COEFFICIENT);
    }

    @Test
    public void testDerivativesAllParameters() {
        doTestDerivatives(1.1e-8, 1.1e-11,
                          "Px", "Py", "Pz", "Vx", "Vy", "Vz",
                          DragSensitive.DRAG_COEFFICIENT,
                          NewtonianAttraction.CENTRAL_ATTRACTION_COEFFICIENT);
    }

    private void doTestDerivatives(double tolP, double tolV, String... names) {

        // we use a fixed step integrator on purpose
        // as the test is based on external differentiation using finite differences,
        // an adaptive step size integrator would introduce *lots* of numerical noise
        NumericalPropagatorBuilder builder =
                        new NumericalPropagatorBuilder(OrbitType.CARTESIAN.convertType(orbit),
                                                       new LutherIntegratorBuilder(10.0),
                                                       PositionAngleType.TRUE, dP);
        builder.setMass(200.0);
        builder.addForceModel(drag);
        builder.addForceModel(gravity);

        // retrieve a state slightly different from the initial state,
        // using normalized values different from 0.0 for the sake of generality
        RandomGenerator random = new Well19937a(0xe67f19c1a678d037l);
        List<ParameterDriver> all = new ArrayList<ParameterDriver>();
        for (final ParameterDriver driver : builder.getOrbitalParametersDrivers().getDrivers()) {
            all.add(driver);
        }
        for (final ParameterDriver driver : builder.getPropagationParametersDrivers().getDrivers()) {
            all.add(driver);
        }
        double[] normalized = new double[names.length];
        List<ParameterDriver> selected = new ArrayList<ParameterDriver>(names.length);
        int index = 0;
        for (final ParameterDriver driver : all) {
            boolean found = false;
            for (final String name : names) {
                if (name.equals(driver.getName())) {
                    found = true;
                    normalized[index++] = driver.getNormalizedValue(new AbsoluteDate()) + (2 * random.nextDouble() - 1);
                    selected.add(driver);
                }
            }
            driver.setSelected(found);
        }

        // create a one hour sample that starts 10 minutes after initial state
        // the 10 minutes offset implies even the first point is influenced by model parameters
        final List<SpacecraftState> sample = new ArrayList<SpacecraftState>();
        Propagator propagator = builder.buildPropagator(normalized);
        propagator.setStepHandler(60.0, currentState -> sample.add(currentState));
        propagator.propagate(orbit.getDate().shiftedBy(600.0), orbit.getDate().shiftedBy(4200.0));

        JacobianPropagatorConverter  fitter = new JacobianPropagatorConverter(builder, 1.0e-3, 5000);
        try {
            Method setSample = AbstractPropagatorConverter.class.getDeclaredMethod("setSample", List.class);
            setSample.setAccessible(true);
            setSample.invoke(fitter, sample);
        } catch (NoSuchMethodException | SecurityException | IllegalAccessException |
                        IllegalArgumentException | InvocationTargetException e) {
            Assertions.fail(e.getLocalizedMessage());
        }

        MultivariateVectorFunction   f = fitter.getObjectiveFunction();
        Pair<RealVector, RealMatrix> p = fitter.getModel().value(new ArrayRealVector(normalized));

        // check derivatives
        // a h offset on normalized parameter represents a physical offset of h * scale
        RealMatrix m = p.getSecond();
        double h = 10.0;
        double[] shifted = normalized.clone();
        double maxErrorP = 0;
        double maxErrorV = 0;
        for (int j = 0; j < selected.size(); ++j) {
            shifted[j] = normalized[j] + 2.0 * h;
            double[] valueP2 = f.value(shifted);
            shifted[j] = normalized[j] + 1.0 * h;
            double[] valueP1 = f.value(shifted);
            shifted[j] = normalized[j] - 1.0 * h;
            double[] valueM1 = f.value(shifted);
            shifted[j] = normalized[j] - 2.0 * h;
            double[] valueM2 = f.value(shifted);
            shifted[j] = normalized[j];
            for (int i = 0; i < valueP2.length; ++i) {
                double d = (8 * (valueP1[i] - valueM1[i]) - (valueP2[i] - valueM2[i])) / (12 * h);
                if (i % 6 < 3) {
                    // position
                    maxErrorP = FastMath.max(maxErrorP, FastMath.abs(m.getEntry(i, j) - d));
                } else {
                    // velocity
                    maxErrorV = FastMath.max(maxErrorV, FastMath.abs(m.getEntry(i, j) - d));
                }
            }
        }
        Assertions.assertEquals(0.0, maxErrorP, tolP);
        Assertions.assertEquals(0.0, maxErrorV, tolV);

    }

    @BeforeEach
    public void setUp() throws IOException, ParseException {

        Utils.setDataRoot("regular-data:potential/shm-format");
        gravity = new HolmesFeatherstoneAttractionModel(FramesFactory.getITRF(IERSConventions.IERS_2010, true),
                                                        GravityFieldFactory.getNormalizedProvider(2, 0));
        mu = gravity.getParameterDriver(NewtonianAttraction.CENTRAL_ATTRACTION_COEFFICIENT).getValue();
        dP = 1.0;

        //use a orbit that comes close to Earth so the drag coefficient has an effect
        final Vector3D position     = new Vector3D(7.0e6, 1.0e6, 4.0e6).normalize()
                .scalarMultiply(Constants.WGS84_EARTH_EQUATORIAL_RADIUS + 300e3);
        final Vector3D velocity     = new Vector3D(-500.0, 8000.0, 1000.0);
        final AbsoluteDate initDate = new AbsoluteDate(2010, 10, 10, 10, 10, 10.0, TimeScalesFactory.getUTC());
        orbit = new EquinoctialOrbit(new PVCoordinates(position,  velocity),
                                     FramesFactory.getEME2000(), initDate, mu);

        final OneAxisEllipsoid earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                            Constants.WGS84_EARTH_FLATTENING,
                                                            FramesFactory.getITRF(IERSConventions.IERS_2010, true));
        earth.setAngularThreshold(1.e-7);
        atmosphere = new SimpleExponentialAtmosphere(earth, 0.0004, 42000.0, 7500.0);
        final double dragCoef = 2.0;
        crossSection = 25.0;
        drag = new DragForce(atmosphere, new IsotropicDrag(crossSection, dragCoef));

    }

}

