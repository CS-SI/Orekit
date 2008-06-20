/* Copyright 2002-2008 CS Communication & Systèmes
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
package org.orekit.orbits;

import junit.framework.*;

import org.apache.commons.math.geometry.Vector3D;
import org.apache.commons.math.util.MathUtils;
import org.orekit.Utils;
import org.orekit.frames.Frame;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.PVCoordinates;


public class CartesianParametersTest extends TestCase {

    // Computation date 
    private AbsoluteDate date;
    
    // Body mu 
    private double mu;

    public CartesianParametersTest(String name) {
        super(name);
    }

    public void testCartesianToCartesian() {

        Vector3D position = new Vector3D(-29536113.0, 30329259.0, -100125.0);
        Vector3D velocity = new Vector3D(-2194.0, -2141.0, -8.0);
        PVCoordinates pvCoordinates = new PVCoordinates( position, velocity);
        double mu = 3.9860047e14;

        CartesianOrbit p = new CartesianOrbit(pvCoordinates, Frame.getJ2000(), date, mu);

        assertEquals(p.getPVCoordinates().getPosition().getX(), pvCoordinates.getPosition().getX(), Utils.epsilonTest * Math.abs(pvCoordinates.getPosition().getX()));
        assertEquals(p.getPVCoordinates().getPosition().getY(), pvCoordinates.getPosition().getY(), Utils.epsilonTest * Math.abs(pvCoordinates.getPosition().getY()));
        assertEquals(p.getPVCoordinates().getPosition().getZ(), pvCoordinates.getPosition().getZ(), Utils.epsilonTest * Math.abs(pvCoordinates.getPosition().getZ()));
        assertEquals(p.getPVCoordinates().getVelocity().getX(), pvCoordinates.getVelocity().getX(), Utils.epsilonTest * Math.abs(pvCoordinates.getVelocity().getX()));
        assertEquals(p.getPVCoordinates().getVelocity().getY(), pvCoordinates.getVelocity().getY(), Utils.epsilonTest * Math.abs(pvCoordinates.getVelocity().getY()));
        assertEquals(p.getPVCoordinates().getVelocity().getZ(), pvCoordinates.getVelocity().getZ(), Utils.epsilonTest * Math.abs(pvCoordinates.getVelocity().getZ()));
    }

    public void testCartesianToEquinoctial() {

        Vector3D position = new Vector3D(-29536113.0, 30329259.0, -100125.0);
        Vector3D velocity = new Vector3D(-2194.0, -2141.0, -8.0);
        PVCoordinates pvCoordinates = new PVCoordinates( position, velocity);

        CartesianOrbit p = new CartesianOrbit(pvCoordinates, Frame.getJ2000(), date, mu);

        assertEquals(42255170.0028257,  p.getA(), Utils.epsilonTest * p.getA());
        assertEquals(0.592732497856475e-03,  p.getEquinoctialEx(), Utils.epsilonE * Math.abs(p.getE()));
        assertEquals(-0.206274396964359e-02, p.getEquinoctialEy(), Utils.epsilonE * Math.abs(p.getE()));
        assertEquals(Math.sqrt(Math.pow(0.592732497856475e-03,2)+Math.pow(-0.206274396964359e-02,2)), p.getE(), Utils.epsilonAngle * Math.abs(p.getE()));
        assertEquals(MathUtils.normalizeAngle(2*Math.asin(Math.sqrt((Math.pow(0.128021863908325e-03,2)+Math.pow(-0.352136186881817e-02,2))/4.)),p.getI()), p.getI(), Utils.epsilonAngle * Math.abs(p.getI()));
        assertEquals(MathUtils.normalizeAngle(0.234498139679291e+01,p.getLM()), p.getLM(), Utils.epsilonAngle * Math.abs(p.getLM()));
    }

    public void testCartesianToKeplerian(){

        Vector3D position = new Vector3D(-26655470.0, 29881667.0,-113657.0);
        Vector3D velocity = new Vector3D(-1125.0,-1122.0,195.0);
        PVCoordinates pvCoordinates = new PVCoordinates( position, velocity);
        double mu = 3.9860047e14;

        CartesianOrbit p = new CartesianOrbit(pvCoordinates, Frame.getJ2000(), date, mu);
        KeplerianOrbit kep = new KeplerianOrbit(p);

        assertEquals(22979265.3030773,  p.getA(), Utils.epsilonTest  * p.getA());
        assertEquals(0.743502611664700, p.getE(), Utils.epsilonE     * Math.abs(p.getE()));
        assertEquals(0.122182096220906, p.getI(), Utils.epsilonAngle * Math.abs(p.getI()));
        double pa = kep.getPerigeeArgument();
        assertEquals(MathUtils.normalizeAngle(3.09909041016672, pa), pa,
                     Utils.epsilonAngle * Math.abs(pa));
        double raan = kep.getRightAscensionOfAscendingNode();
        assertEquals(MathUtils.normalizeAngle(2.32231010979999, raan), raan,
                     Utils.epsilonAngle * Math.abs(raan));
        double m = kep.getMeanAnomaly();
        assertEquals(MathUtils.normalizeAngle(3.22888977629034, m), m,
                     Utils.epsilonAngle * Math.abs(Math.abs(m)));
    }

    public void testPositionVelocityNorms(){

        Vector3D position = new Vector3D(-29536113.0, 30329259.0, -100125.0);
        Vector3D velocity = new Vector3D(-2194.0, -2141.0, -8.0);
        PVCoordinates pvCoordinates = new PVCoordinates( position, velocity);

        CartesianOrbit p = new CartesianOrbit(pvCoordinates, Frame.getJ2000(), date, mu);

        double e       = p.getE();
        double v       = new KeplerianOrbit(p).getTrueAnomaly();
        double ksi     = 1 + e * Math.cos(v);
        double nu      = e * Math.sin(v);
        double epsilon = Math.sqrt((1 - e) * (1 + e));

        double a  = p.getA();
        double na = Math.sqrt(mu / a);

        // validation of: r = a .(1 - e2) / (1 + e.cos(v))
        assertEquals(a * epsilon * epsilon / ksi,
                     p.getPVCoordinates().getPosition().getNorm(),
                     Utils.epsilonTest * Math.abs(p.getPVCoordinates().getPosition().getNorm()));

        // validation of: V = sqrt(mu.(1+2e.cos(v)+e2)/a.(1-e2) )
        assertEquals(na * Math.sqrt(ksi * ksi + nu * nu) / epsilon,
                     p.getPVCoordinates().getVelocity().getNorm(),
                     Utils.epsilonTest * Math.abs(p.getPVCoordinates().getVelocity().getNorm()));

    }

    public void testGeometry() {

        Vector3D position = new Vector3D(-29536113.0, 30329259.0, -100125.0);
        Vector3D velocity = new Vector3D(-2194.0, -2141.0, -8.0);
        PVCoordinates pvCoordinates = new PVCoordinates( position, velocity);

        Vector3D momentum = Vector3D.crossProduct(position, velocity).normalize();

        EquinoctialOrbit p = new EquinoctialOrbit(pvCoordinates, Frame.getJ2000(), date, mu);

        double apogeeRadius  = p.getA() * (1 + p.getE());
        double perigeeRadius = p.getA() * (1 - p.getE());

        for (double lv = 0; lv <= 2 * Math.PI; lv += 2 * Math.PI/100.) {
            p = new EquinoctialOrbit(p.getA(), p.getEquinoctialEx(), p.getEquinoctialEy(),
                                          p.getHx(), p.getHy(), lv, 2, p.getFrame(), date, mu);
            position = p.getPVCoordinates().getPosition();

            // test if the norm of the position is in the range [perigee radius, apogee radius]
            // Warning: these tests are without absolute value by choice
            assertTrue((position.getNorm() - apogeeRadius)  <= (  apogeeRadius * Utils.epsilonTest));
            assertTrue((position.getNorm() - perigeeRadius) >= (- perigeeRadius * Utils.epsilonTest));
            // assertTrue(position.getNorm() <= apogeeRadius);
            // assertTrue(position.getNorm() >= perigeeRadius);

            position= position.normalize();
            velocity = p.getPVCoordinates().getVelocity().normalize();

            // at this stage of computation, all the vectors (position, velocity and momemtum) are normalized here

            // test of orthogonality between position and momentum
            assertTrue(Math.abs(Vector3D.dotProduct(position, momentum)) < Utils.epsilonTest);
            // test of orthogonality between velocity and momentum
            assertTrue(Math.abs(Vector3D.dotProduct(velocity, momentum)) < Utils.epsilonTest);
        }
    }


    public void setUp() {

        // Computation date
        date = AbsoluteDate.J2000_EPOCH;

        // Body mu
        mu = 3.9860047e14;
    }

    public void tearDown() {
        date = null;
    }

    public static Test suite() {
        return new TestSuite(CartesianParametersTest.class);
    }
}

