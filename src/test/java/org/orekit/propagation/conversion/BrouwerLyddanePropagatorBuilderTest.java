/* Copyright 2002-2022 CS GROUP
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

import org.hipparchus.util.FastMath;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.forces.gravity.potential.GravityFieldFactory;
import org.orekit.forces.gravity.potential.TideSystem;
import org.orekit.forces.gravity.potential.UnnormalizedSphericalHarmonicsProvider;
import org.orekit.forces.gravity.potential.UnnormalizedSphericalHarmonicsProvider.UnnormalizedSphericalHarmonics;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.analytical.BrouwerLyddanePropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.ParameterDriver;

public class BrouwerLyddanePropagatorBuilderTest {

    private Orbit orbit;
    private UnnormalizedSphericalHarmonicsProvider provider;

    @Test
    public void doTestBuildPropagator() {
    	
    	final double eps  = 2.0e-10;
        
    	// Define initial state and BrouwerLyddane Propagator
    	AbsoluteDate initDate = AbsoluteDate.J2000_EPOCH.shiftedBy(583.);
    	BrouwerLyddanePropagator propagator = new BrouwerLyddanePropagator(orbit, provider, BrouwerLyddanePropagator.M2);
        // We propagate using directly the propagator of the set up
        final Orbit orbitWithPropagator = propagator.propagate(initDate.shiftedBy(60000)).getOrbit();
        
        // Convert povider to normalized provider to be able to build a Brouwer Lyddane propagator
        UnnormalizedSphericalHarmonics harmonics = provider.onDate(orbit.getDate());
        
        // We propagate using a build version of the propagator
        // We shall have the same results than before
        BrouwerLyddanePropagatorBuilder builder = new BrouwerLyddanePropagatorBuilder(orbit,
                                                                                      provider.getAe(),
                                                                                      provider.getMu(),
                                                                                      provider.getTideSystem(),
                                                                                      harmonics.getUnnormalizedCnm(2, 0),
                                                                                      harmonics.getUnnormalizedCnm(3, 0),
                                                                                      harmonics.getUnnormalizedCnm(4, 0),
                                                                                      harmonics.getUnnormalizedCnm(5, 0),
                                                                                      OrbitType.KEPLERIAN,
                                                                                      PositionAngle.TRUE,
                                                                                      1.0,
                                                                                      BrouwerLyddanePropagator.M2);
        
        final BrouwerLyddanePropagator prop = builder.buildPropagator(builder.getSelectedNormalizedParameters());
        final Orbit orbitWithBuilder = prop.propagate(initDate.shiftedBy(60000)).getOrbit();
        
        // Verify
        Assert.assertEquals(orbitWithPropagator.getA(),             orbitWithBuilder.getA(), 1.e-1);
        Assert.assertEquals(orbitWithPropagator.getEquinoctialEx(), orbitWithBuilder.getEquinoctialEx(), eps);
        Assert.assertEquals(orbitWithPropagator.getEquinoctialEy(), orbitWithBuilder.getEquinoctialEy(), eps);
        Assert.assertEquals(orbitWithPropagator.getHx(),            orbitWithBuilder.getHx(), eps);
        Assert.assertEquals(orbitWithPropagator.getHy(),            orbitWithBuilder.getHy(), eps);
        Assert.assertEquals(orbitWithPropagator.getLM(),            orbitWithBuilder.getLM(), 8.0e-10);

    }

    @Test
    public void doTestBuildPropagatorWithDrag() {

        // M2
        final double M2 = 1.0e-15;

        // Convert provider to normalized provider to be able to build a Brouwer Lyddane propagator
        UnnormalizedSphericalHarmonics harmonics = provider.onDate(orbit.getDate());
        
        // Initialize propagator builder
        BrouwerLyddanePropagatorBuilder builder = new BrouwerLyddanePropagatorBuilder(orbit,
                                                                                      provider.getAe(),
                                                                                      provider.getMu(),
                                                                                      provider.getTideSystem(),
                                                                                      harmonics.getUnnormalizedCnm(2, 0),
                                                                                      harmonics.getUnnormalizedCnm(3, 0),
                                                                                      harmonics.getUnnormalizedCnm(4, 0),
                                                                                      harmonics.getUnnormalizedCnm(5, 0),
                                                                                      OrbitType.KEPLERIAN,
                                                                                      PositionAngle.TRUE,
                                                                                      1.0,
                                                                                      M2);

        // Set the M2 parameter to selected
        for (ParameterDriver driver : builder.getPropagationParametersDrivers().getDrivers()) {
            if (BrouwerLyddanePropagator.M2_NAME.equals(driver.getName())) {
                driver.setSelected(true);
            }
        }
        
        // Build the propagator
        final BrouwerLyddanePropagator prop = builder.buildPropagator(builder.getSelectedNormalizedParameters());

        // Verify
        Assert.assertEquals(M2, prop.getM2(), Double.MIN_VALUE);
        Assert.assertTrue(prop.getParametersDrivers().get(0).isSelected());

    }

    @Before
    public void setUp() {
        Utils.setDataRoot("regular-data");

        AbsoluteDate initDate = AbsoluteDate.J2000_EPOCH.shiftedBy(583.);
        final Frame inertialFrame = FramesFactory.getEME2000();

        // Provider definition
        double mu  = 3.9860047e14;
        double ae  = 6.378137e6;
        double[][] cnm = new double[][] {
            { 0 }, { 0 }, { -1.08263e-3 }, { 2.54e-6 }, { 1.62e-6 }, { 2.3e-7 }
           };
        double[][] snm = new double[][] {
            { 0 }, { 0 }, { 0 }, { 0 }, { 0 }, { 0 }
           };
        provider = GravityFieldFactory.getUnnormalizedProvider(ae, mu, TideSystem.UNKNOWN, cnm, snm);

        // Initial orbit
        final double a = 24396159; // semi major axis in meters
        final double e = 0.01; // eccentricity
        final double i = FastMath.toRadians(47.); // inclination
        final double omega = FastMath.toRadians(180); // perigee argument
        final double raan = FastMath.toRadians(261); // right ascention of ascending node
        final double lM = 0; // mean anomaly
        orbit = new KeplerianOrbit(a, e, i, omega, raan, lM, PositionAngle.TRUE, inertialFrame, initDate, mu);
    }

}
