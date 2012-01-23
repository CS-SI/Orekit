/* Copyright 2002-2011 CS Communication & Systèmes
 * Licensed to CS Communication & Systèmes (CS) under one or more
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
package org.orekit.propagation.semianalytical.dsst.dsstforcemodel;


import org.apache.commons.math.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math.util.FastMath;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.errors.OrekitException;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.PVCoordinatesProvider;


public class DSSTSolarRadiationPressureTest {

    private final static double D_REF = 149597870000.0;
    private final static double P_REF = 4.56e-6;

    @Test
    public void testMeanElementRateWithShadow() throws OrekitException {
        final PVCoordinatesProvider sun = CelestialBodyFactory.getSun();
        final double cR = 1.;
        final double aC = 5.;
        final DSSTSolarRadiationPressure force = new DSSTSolarRadiationPressure(cR, aC, sun, Constants.WGS84_EARTH_EQUATORIAL_RADIUS);

        // LEO orbit with shadow
        AbsoluteDate date = new AbsoluteDate(new DateComponents(2003, 03, 21), new TimeComponents(1, 0, 0.), TimeScalesFactory.getUTC());
        final double mu   = 3.9860047e14;
        final Vector3D position  = new Vector3D(-6142438.668, 3492467.560, -25767.25680);
        final Vector3D velocity  = new Vector3D(505.8479685, 942.7809215, 7435.922231);
        final Orbit orbit = new EquinoctialOrbit(new PVCoordinates(position,  velocity),
                                                 FramesFactory.getEME2000(), date, mu);
        final SpacecraftState state = new SpacecraftState(orbit);

        final double[] daidt    = force.getMeanElementRate(state);
        final double[] daidtRef = getDirectMeanElementRate(state, cR, aC, sun);
        
//        System.out.println(daidtRef[0] + " " + daidt[0] + " " + (daidtRef[0] - daidt[0]));
//        System.out.println(daidtRef[1] + " " + daidt[1] + " " + (daidtRef[1] - daidt[1]));
//        System.out.println(daidtRef[2] + " " + daidt[2] + " " + (daidtRef[2] - daidt[2]));
//        System.out.println(daidtRef[3] + " " + daidt[3] + " " + (daidtRef[3] - daidt[3]));
//        System.out.println(daidtRef[4] + " " + daidt[4] + " " + (daidtRef[4] - daidt[4]));
//        System.out.println(daidtRef[5] + " " + daidt[5] + " " + (daidtRef[5] - daidt[5]));

        Assert.assertEquals(daidtRef[0], daidt[0], 5.e-9);
        Assert.assertEquals(daidtRef[1], daidt[1], 1.e-12);
        Assert.assertEquals(daidtRef[2], daidt[2], 1.e-12);
        Assert.assertEquals(daidtRef[3], daidt[3], 5.e-13);
        Assert.assertEquals(daidtRef[4], daidt[4], 5.e-13);
        Assert.assertEquals(daidtRef[5], daidt[5], 2.e-12);

    }

    @Test
    public void testMeanElementRateWithoutShadow() throws OrekitException {
        final PVCoordinatesProvider sun = CelestialBodyFactory.getSun();
        final double cR = 2.;
        final double aC = 5.;
        final DSSTSolarRadiationPressure force = new DSSTSolarRadiationPressure(cR, aC, sun, Constants.WGS84_EARTH_EQUATORIAL_RADIUS);

        // GEO orbit without shadow (shadow on 1970-03-23)
        AbsoluteDate date = new AbsoluteDate(new DateComponents(1970,1,23),
                                             new TimeComponents(0, 59, 28.812),
                                             TimeScalesFactory.getUTC());
        final Vector3D position  = new Vector3D(-42338319.88, 4113188.92, 120810.3);
        final Vector3D velocity  = new Vector3D(-328.07, -3029.81, 6.28);
        final double mu = 3.9860047e14;
        final Orbit orbit = new EquinoctialOrbit(new PVCoordinates(position,  velocity),
                                                 FramesFactory.getEME2000(), date, mu);
        final SpacecraftState state = new SpacecraftState(orbit);

        final double[] daidt    = force.getMeanElementRate(state);
        final double[] daidtRef = getDirectMeanElementRate(state, cR, aC, sun);

//        System.out.println(daidtRef[0] + " " + daidt[0] + " " + (daidtRef[0] - daidt[0]));
//        System.out.println(daidtRef[1] + " " + daidt[1] + " " + (daidtRef[1] - daidt[1]));
//        System.out.println(daidtRef[2] + " " + daidt[2] + " " + (daidtRef[2] - daidt[2]));
//        System.out.println(daidtRef[3] + " " + daidt[3] + " " + (daidtRef[3] - daidt[3]));
//        System.out.println(daidtRef[4] + " " + daidt[4] + " " + (daidtRef[4] - daidt[4]));
//        System.out.println(daidtRef[5] + " " + daidt[5] + " " + (daidtRef[5] - daidt[5]));

        Assert.assertEquals(daidtRef[0], daidt[0], 5.e-21);
        Assert.assertEquals(daidtRef[1], daidt[1], 9.e-12);
        Assert.assertEquals(daidtRef[2], daidt[2], 6.5e-12);
        Assert.assertEquals(daidtRef[3], daidt[3], 2.e-14);
        Assert.assertEquals(daidtRef[4], daidt[4], 5.e-14);
        Assert.assertEquals(daidtRef[5], daidt[5], 4.5e-14);

    }

    /** Direct computation of the mean element rates without shadow 
     *  @param state current state information: date, kinematics, attitude
     *  @param cr satellite radiation pressure coefficient (assuming total specular reflection)
     *  @param area cross sectionnal area of satellite
     *  @param sun Sun provider
     *  @return the mean element rates dai/dt
     *  @exception OrekitException if some specific error occurs
     */
    public double[] getDirectMeanElementRate(final SpacecraftState state,
                                             final double cr, final double area,
                                             final PVCoordinatesProvider sun) throws OrekitException {
        final double[] meanElementRate = new double[6];
 
        // Equinoctial elements
        double[] stateVector = new double[6];
        OrbitType.EQUINOCTIAL.mapOrbitToArray(state.getOrbit(), PositionAngle.MEAN, stateVector);
        final double a = stateVector[0];
        final double k = stateVector[1];
        final double h = stateVector[2];
        final double q = stateVector[3];
        final double p = stateVector[4];
        final double I = +1;

        // Factors
        final double k2 = k * k;
        final double h2 = h * h;
        final double q2 = q * q;
        final double p2 = p * p;

        // Computation of A, B, C (equinoctial coefficients)
        final double A = FastMath.sqrt(state.getMu() * a);
        final double B = FastMath.sqrt(1 - k2 - h2);
        final double C = 1 + q2 + p2;

        // Direction cosines
        final Vector3D f = new Vector3D( (1 - p2 + q2) / C,        2. * p * q / C,       -2. * I * p / C);
        final Vector3D g = new Vector3D(2. * I * p * q / C, I * (1 + p2 - q2) / C,            2. * q / C);
        final Vector3D w = new Vector3D(        2. * p / C,           -2. * q / C, I * (1 - p2 - q2) / C);

        // Direction cosines
        final Vector3D sunPos = sun.getPVCoordinates(state.getDate(), state.getFrame()).getPosition().normalize();
        final double alpha = sunPos.dotProduct(f);
        final double beta  = sunPos.dotProduct(g);
        final double gamma = sunPos.dotProduct(w);

        // Compute T coefficient
        final double kRef = 0.5 * P_REF * D_REF * D_REF * cr * area;
        final double T = kRef / state.getMass();
        // Compute R3^2
        final double R32 = sun.getPVCoordinates(state.getDate(), state.getFrame()).getPosition().getNormSq();
        // Compute V coefficient
        final double V =  (3. * T * a) / (2. * R32);
        // Compute mean elements rate
        final double ab = A * B;
        final double bvoa = B * V / A;
        final double kpihq = k * p - I * h * q;
        final double vgoab = V * gamma / ab;
        final double cvgo2ab = C * vgoab / 2.;
        final double kpihqvgoab = kpihq * vgoab;
        // da/dt =  0
        meanElementRate[0] = 0.;
        // dk/dt = −B*V*β/A + (V*h*γ/A*B)*(k*p − I*h*q)
        meanElementRate[1] = -bvoa * beta + kpihqvgoab * h;
        // dh/dt = B*V*α/A − (V*k*γ/A*B)*(k*p − I*h*q)
        meanElementRate[2] =  bvoa * alpha - kpihqvgoab * k;
        // dq/dt = I*C*V*k*γ/2*A*B
        meanElementRate[3] =  I * cvgo2ab * k;
        // dp/dt = C*V*h*γ/2*A*B
        meanElementRate[4] =  cvgo2ab * h;
        // dλ/dt = −(2 + B)*V*(k*α + h*β)/A*(1 + B) − (V*γ/A*B)*(k*p − I*h*q)
        meanElementRate[5] = -(kpihqvgoab + (2. + B) * V * (k * alpha + h * beta) / (A * (1. + B)));
    
        return meanElementRate;
    }

    @Before
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

}
