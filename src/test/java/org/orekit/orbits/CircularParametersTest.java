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

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.commons.math.geometry.Vector3D;
import org.apache.commons.math.util.MathUtils;
import org.orekit.Utils;
import org.orekit.frames.Frame;
import org.orekit.orbits.CircularOrbit;
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.PVCoordinates;


public class CircularParametersTest extends TestCase {

    // Computation date 
    private AbsoluteDate date;
    
    // Body mu 
    private double mu;

    public CircularParametersTest(String name) {
        super(name);
    }

    public void testCircularToEquinoctialEll() {

        double ix = 1.200e-04;
        double iy = -1.16e-04;
        double i  = 2 * Math.asin(Math.sqrt((ix * ix + iy * iy) / 4));
        double raan = Math.atan2(iy, ix);

        // elliptic orbit
        CircularOrbit circ =
            new CircularOrbit(42166.712, 0.5, -0.5, i, raan,
                                   5.300 - raan, CircularOrbit.MEAN_LONGITUDE_ARGUMENT, 
                                   Frame.getJ2000(), date, mu);
        Vector3D pos = circ.getPVCoordinates().getPosition();
        Vector3D vit = circ.getPVCoordinates().getVelocity();

        PVCoordinates pvCoordinates = new PVCoordinates( pos, vit);

        EquinoctialOrbit param = new EquinoctialOrbit(pvCoordinates, Frame.getJ2000(), date, mu);
        assertEquals(param.getA(),  circ.getA(), Utils.epsilonTest * circ.getA());
        assertEquals(param.getEquinoctialEx(), circ.getEquinoctialEx(), Utils.epsilonE * Math.abs(circ.getE()));
        assertEquals(param.getEquinoctialEy(), circ.getEquinoctialEy(), Utils.epsilonE * Math.abs(circ.getE()));
        assertEquals(param.getHx(), circ.getHx(), Utils.epsilonAngle * Math.abs(circ.getI()));
        assertEquals(param.getHy(), circ.getHy(), Utils.epsilonAngle * Math.abs(circ.getI()));
        assertEquals(MathUtils.normalizeAngle(param.getLv(),circ.getLv()), circ.getLv(), Utils.epsilonAngle * Math.abs(circ.getLv()));

    }

    public void testCircularToEquinoctialCirc() {

        double ix = 1.200e-04;
        double iy = -1.16e-04;
        double i  = 2 * Math.asin(Math.sqrt((ix * ix + iy * iy) / 4));
        double raan = Math.atan2(iy, ix);

        // circular orbit
        EquinoctialOrbit circCir =
            new EquinoctialOrbit(42166.712, 0.1e-10, -0.1e-10, i, raan,
                                      5.300 - raan, CircularOrbit.MEAN_LONGITUDE_ARGUMENT, 
                                      Frame.getJ2000(), date, mu);
        Vector3D posCir = circCir.getPVCoordinates().getPosition();
        Vector3D vitCir = circCir.getPVCoordinates().getVelocity();

        PVCoordinates pvCoordinates = new PVCoordinates( posCir, vitCir);

        EquinoctialOrbit paramCir = new EquinoctialOrbit(pvCoordinates, Frame.getJ2000(), date, mu);
        assertEquals(paramCir.getA(), circCir.getA(), Utils.epsilonTest * circCir.getA());
        assertEquals(paramCir.getEquinoctialEx(), circCir.getEquinoctialEx(), Utils.epsilonEcir * Math.abs(circCir.getE()));
        assertEquals(paramCir.getEquinoctialEy(), circCir.getEquinoctialEy(), Utils.epsilonEcir * Math.abs(circCir.getE()));
        assertEquals(paramCir.getHx(), circCir.getHx(), Utils.epsilonAngle * Math.abs(circCir.getI()));
        assertEquals(paramCir.getHy(), circCir.getHy(), Utils.epsilonAngle * Math.abs(circCir.getI()));
        assertEquals(MathUtils.normalizeAngle(paramCir.getLv(),circCir.getLv()), circCir.getLv(), Utils.epsilonAngle * Math.abs(circCir.getLv()));

    }

    public void testCircularToCartesian() {

        double ix = 1.200e-04;
        double iy = -1.16e-04;
        double i  = 2 * Math.asin(Math.sqrt((ix * ix + iy * iy) / 4));
        double raan = Math.atan2(iy, ix);
        double cosRaan = Math.cos(raan);
        double sinRaan = Math.sin(raan);
        double exTilde = -7.900e-6;
        double eyTilde = 1.100e-4;
        double ex = exTilde * cosRaan + eyTilde * sinRaan;
        double ey = eyTilde * cosRaan - exTilde * sinRaan;

        CircularOrbit circ=
            new CircularOrbit(42166.712, ex, ey, i, raan,
                                   5.300 - raan, CircularOrbit.MEAN_LONGITUDE_ARGUMENT, 
                                   Frame.getJ2000(), date, mu);
        Vector3D pos = circ.getPVCoordinates().getPosition();
        Vector3D vel = circ.getPVCoordinates().getVelocity();

        // check 1/a = 2/r  - V2/mu
        double r = pos.getNorm();
        double v = vel.getNorm();
        assertEquals(2 / r - v * v / mu, 1 / circ.getA(), 1.0e-7);

        assertEquals( 0.233745668678733e+05, pos.getX(), Utils.epsilonTest * r);
        assertEquals(-0.350998914352669e+05, pos.getY(), Utils.epsilonTest * r);
        assertEquals(-0.150053723123334e+01, pos.getZ(), Utils.epsilonTest * r);

        assertEquals(0.809135038364960e+05, vel.getX(), Utils.epsilonTest * v);
        assertEquals(0.538902268252598e+05, vel.getY(), Utils.epsilonTest * v);
        assertEquals(0.158527938296630e+02, vel.getZ(), Utils.epsilonTest * v);

    }

    public void testCircularToKeplerian() {

        double ix   =  1.20e-4;
        double iy   = -1.16e-4;
        double i    = 2 * Math.asin(Math.sqrt((ix * ix + iy * iy) / 4));
        double raan = Math.atan2(iy, ix);
        double cosRaan = Math.cos(raan);
        double sinRaan = Math.sin(raan);
        double exTilde = -7.900e-6;
        double eyTilde = 1.100e-4;
        double ex = exTilde * cosRaan + eyTilde * sinRaan;
        double ey = eyTilde * cosRaan - exTilde * sinRaan;

        CircularOrbit circ=
            new CircularOrbit(42166.712, ex, ey, i, raan,
                                   5.300 - raan, CircularOrbit.MEAN_LONGITUDE_ARGUMENT, 
                                   Frame.getJ2000(), date, mu);
        KeplerianOrbit kep = new KeplerianOrbit(circ);

        assertEquals(42166.71200, circ.getA(), Utils.epsilonTest * kep.getA());
        assertEquals(0.110283316961361e-03, kep.getE(), Utils.epsilonE * Math.abs(kep.getE()));
        assertEquals(0.166901168553917e-03, kep.getI(),
                     Utils.epsilonAngle * Math.abs(kep.getI()));
        assertEquals(MathUtils.normalizeAngle(-3.87224326008837, kep.getPerigeeArgument()),
                     kep.getPerigeeArgument(),
                     Utils.epsilonTest * Math.abs(kep.getPerigeeArgument()));
        assertEquals(MathUtils.normalizeAngle(5.51473467358854, kep.getRightAscensionOfAscendingNode()),
                     kep.getRightAscensionOfAscendingNode(),
                     Utils.epsilonTest * Math.abs(kep.getRightAscensionOfAscendingNode()));
        assertEquals(MathUtils.normalizeAngle(3.65750858649982, kep.getMeanAnomaly()),
                     kep.getMeanAnomaly(),
                     Utils.epsilonTest * Math.abs(kep.getMeanAnomaly()));

    }

    public void testAnomalyEll() {

        // elliptic orbit
        Vector3D position = new Vector3D(7.0e6, 1.0e6, 4.0e6);
        Vector3D velocity = new Vector3D(-500.0, 8000.0, 1000.0);

        PVCoordinates pvCoordinates = new PVCoordinates( position, velocity);

        CircularOrbit  p   = new CircularOrbit(pvCoordinates, Frame.getJ2000(), date, mu);
        KeplerianOrbit kep = new KeplerianOrbit(p);

        double e       = p.getE();
        double eRatio  = Math.sqrt((1 - e) / (1 + e));
        double raan    = kep.getRightAscensionOfAscendingNode();
        double paPraan = kep.getPerigeeArgument() + raan;

        double lv = 1.1;
        // formulations for elliptic case
        double lE = 2 * Math.atan(eRatio * Math.tan((lv - paPraan) / 2)) + paPraan;
        double lM = lE - e * Math.sin(lE - paPraan);

//      p.setAlphaV(lv - raan);
        p = new CircularOrbit(p.getA() , p.getCircularEx(), p.getCircularEy(),
                                   p.getRightAscensionOfAscendingNode(),
                                   p.getAlphaV(), lv - raan, 2, p.getFrame(), date, mu);
        assertEquals(p.getAlphaV() + raan, lv, Utils.epsilonAngle * Math.abs(lv));
        assertEquals(p.getAlphaE() + raan, lE, Utils.epsilonAngle * Math.abs(lE));
        assertEquals(p.getAlphaM() + raan, lM, Utils.epsilonAngle * Math.abs(lM));
//      p.setAlphaV(0);
        p = new CircularOrbit(p.getA() , p.getCircularEx(), p.getCircularEy(),
                                   p.getRightAscensionOfAscendingNode(),
                                   p.getAlphaV(), 0, 2, p.getFrame(), date, mu);


//      p.setAlphaE(lE - raan);
        p = new CircularOrbit(p.getA() , p.getCircularEx(), p.getCircularEy(),
                                   p.getRightAscensionOfAscendingNode(),
                                   p.getAlphaV(), lE - raan, 1, p.getFrame(), date, mu);
        assertEquals(p.getAlphaV() + raan, lv, Utils.epsilonAngle * Math.abs(lv));
        assertEquals(p.getAlphaE() + raan, lE, Utils.epsilonAngle * Math.abs(lE));
        assertEquals(p.getAlphaM() + raan, lM, Utils.epsilonAngle * Math.abs(lM));
//      p.setAlphaV(0);
        p = new CircularOrbit(p.getA() , p.getCircularEx(), p.getCircularEy(),
                                   p.getRightAscensionOfAscendingNode(),
                                   p.getAlphaV(), 0, 2, p.getFrame(), date, mu);

//      p.setAlphaM(lM - raan);
        p = new CircularOrbit(p.getA() , p.getCircularEx(), p.getCircularEy(),
                                   p.getRightAscensionOfAscendingNode(),
                                   p.getAlphaV(), lM - raan, 0, p.getFrame(), date, mu);
        assertEquals(p.getAlphaV() + raan, lv, Utils.epsilonAngle * Math.abs(lv));
        assertEquals(p.getAlphaE() + raan, lE, Utils.epsilonAngle * Math.abs(lE));
        assertEquals(p.getAlphaM() + raan, lM, Utils.epsilonAngle * Math.abs(lM));

    }

    public void testAnomalyCirc() {

        Vector3D position = new Vector3D(7.0e6, 1.0e6, 4.0e6);
        Vector3D velocity = new Vector3D(-500.0, 8000.0, 1000.0);
        PVCoordinates pvCoordinates = new PVCoordinates( position, velocity);
        CircularOrbit  p = new CircularOrbit(pvCoordinates, Frame.getJ2000(), date, mu);
        double raan = p.getRightAscensionOfAscendingNode();

        // circular orbit
//      p.setCircularEx(0);
//      p.setCircularEy(0);

        p = new CircularOrbit(p.getA() , 0, 0, p.getRightAscensionOfAscendingNode(),
                                   p.getAlphaV(), p.getAlphaV(), 2, p.getFrame(), date, mu);

        double lv = 1.1;
        double lE = lv;
        double lM = lE;

//      p.setAlphaV(lv - raan);
        p = new CircularOrbit(p.getA() , p.getCircularEx(), p.getCircularEy(),
                                   p.getRightAscensionOfAscendingNode(),
                                   p.getAlphaV(), lv - raan, 2, p.getFrame(), date, mu);
        assertEquals(p.getAlphaV() + raan, lv, Utils.epsilonAngle * Math.abs(lv));
        assertEquals(p.getAlphaE() + raan, lE, Utils.epsilonAngle * Math.abs(lE));
        assertEquals(p.getAlphaM() + raan, lM, Utils.epsilonAngle * Math.abs(lM));
//      p.setAlphaV(0);
        p = new CircularOrbit(p.getA() , p.getCircularEx(), p.getCircularEy(),
                                   p.getRightAscensionOfAscendingNode(),
                                   p.getAlphaV(), 0, 2, p.getFrame(), date, mu);

//      p.setAlphaE(lE - raan);
        p = new CircularOrbit(p.getA() , p.getCircularEx(), p.getCircularEy(),
                                   p.getRightAscensionOfAscendingNode(),
                                   p.getAlphaV(), lE - raan, 1, p.getFrame(), date, mu);

        assertEquals(p.getAlphaV() + raan, lv, Utils.epsilonAngle * Math.abs(lv));
        assertEquals(p.getAlphaE() + raan, lE, Utils.epsilonAngle * Math.abs(lE));
        assertEquals(p.getAlphaM() + raan, lM, Utils.epsilonAngle * Math.abs(lM));
//      p.setAlphaV(0);
        p = new CircularOrbit(p.getA() , p.getCircularEx(), p.getCircularEy(),
                                   p.getRightAscensionOfAscendingNode(),
                                   p.getAlphaV(), 0, 2, p.getFrame(), date, mu);

//      p.setAlphaM(lM - raan);
        p = new CircularOrbit(p.getA() , p.getCircularEx(), p.getCircularEy(),
                                   p.getRightAscensionOfAscendingNode(),
                                   p.getAlphaV(), lM - raan, 0, p.getFrame(), date, mu);
        assertEquals(p.getAlphaV() + raan, lv, Utils.epsilonAngle * Math.abs(lv));
        assertEquals(p.getAlphaE() + raan, lE, Utils.epsilonAngle * Math.abs(lE));
        assertEquals(p.getAlphaM() + raan, lM, Utils.epsilonAngle * Math.abs(lM));

    }

    public void testPositionVelocityNormsEll() {

        // elliptic and non equatorial (i retrograde) orbit
        double hx =  1.2;
        double hy =  2.1;
        double i  = 2 * Math.atan(Math.sqrt(hx * hx + hy * hy));
        double raan = Math.atan2(hy, hx);
        CircularOrbit p =
            new CircularOrbit(42166.712, 0.5, -0.5, i, raan,
                                   0.67 - raan, CircularOrbit.TRUE_LONGITUDE_ARGUMENT, 
                                   Frame.getJ2000(), date, mu);

        double ex = p.getEquinoctialEx();
        double ey = p.getEquinoctialEy();
        double lv = p.getLv();
        double ksi     = 1 + ex * Math.cos(lv) + ey * Math.sin(lv);
        double nu      = ex * Math.sin(lv) - ey * Math.cos(lv);
        double epsilon = Math.sqrt(1 - ex * ex - ey * ey);

        double a  = p.getA();
        double na = Math.sqrt(mu / a);

        assertEquals(a * epsilon * epsilon / ksi,
                     p.getPVCoordinates().getPosition().getNorm(),
                     Utils.epsilonTest * Math.abs(p.getPVCoordinates().getPosition().getNorm()));
        assertEquals(na * Math.sqrt(ksi * ksi + nu * nu) / epsilon,
                     p.getPVCoordinates().getVelocity().getNorm(),
                     Utils.epsilonTest * Math.abs(p.getPVCoordinates().getVelocity().getNorm()));

    }

    public void testPositionVelocityNormsCirc() {

        // elliptic and non equatorial (i retrograde) orbit
        double hx =  0.1e-8;
        double hy =  0.1e-8;
        double i  = 2 * Math.atan(Math.sqrt(hx * hx + hy * hy));
        double raan = Math.atan2(hy, hx);
        CircularOrbit pCirEqua =
            new CircularOrbit(42166.712, 0.1e-8, 0.1e-8, i, raan,
                                   0.67 - raan, CircularOrbit.TRUE_LONGITUDE_ARGUMENT, 
                                   Frame.getJ2000(), date, mu);

        double ex = pCirEqua.getEquinoctialEx();
        double ey = pCirEqua.getEquinoctialEy();
        double lv = pCirEqua.getLv();
        double ksi     = 1 + ex * Math.cos(lv) + ey * Math.sin(lv);
        double nu      = ex * Math.sin(lv) - ey * Math.cos(lv);
        double epsilon = Math.sqrt(1 - ex * ex - ey * ey);

        double a  = pCirEqua.getA();
        double na = Math.sqrt(mu / a);

        assertEquals(a * epsilon * epsilon / ksi,
                     pCirEqua.getPVCoordinates().getPosition().getNorm(),
                     Utils.epsilonTest * Math.abs(pCirEqua.getPVCoordinates().getPosition().getNorm()));
        assertEquals(na * Math.sqrt(ksi * ksi + nu * nu) / epsilon,
                     pCirEqua.getPVCoordinates().getVelocity().getNorm(),
                     Utils.epsilonTest * Math.abs(pCirEqua.getPVCoordinates().getVelocity().getNorm()));
    }

    public void testGeometryEll() {

        // elliptic and non equatorial (i retrograde) orbit
        double hx =  1.2;
        double hy =  2.1;
        double i  = 2 * Math.atan(Math.sqrt(hx * hx + hy * hy));
        double raan = Math.atan2(hy, hx);
        CircularOrbit p =
            new CircularOrbit(42166.712, 0.5, -0.5, i, raan,
                                   0.67 - raan, CircularOrbit.TRUE_LONGITUDE_ARGUMENT, 
                                   Frame.getJ2000(), date, mu);

        Vector3D position = p.getPVCoordinates().getPosition();
        Vector3D velocity = p.getPVCoordinates().getVelocity();

        Vector3D momentum = Vector3D.crossProduct(position,velocity).normalize();

        double apogeeRadius  = p.getA() * (1 + p.getE());
        double perigeeRadius = p.getA() * (1 - p.getE());

        for (double alphaV = 0; alphaV <= 2 * Math.PI; alphaV += 2 * Math.PI/100.) {
//          p.setAlphaV(alphaV);
            p = new CircularOrbit(p.getA() , p.getCircularEx(), p.getCircularEy(), p.getI(),
                                       p.getRightAscensionOfAscendingNode(),
                                       alphaV, 2, p.getFrame(), date, mu);
            position = p.getPVCoordinates().getPosition();
            // test if the norm of the position is in the range [perigee radius, apogee radius]
            // Warning: these tests are without absolute value by choice
            assertTrue((position.getNorm() - apogeeRadius)  <= (  apogeeRadius * Utils.epsilonTest));
            assertTrue((position.getNorm() - perigeeRadius) >= (- perigeeRadius * Utils.epsilonTest));

            position= position.normalize();
            velocity = p.getPVCoordinates().getVelocity();
            velocity= velocity.normalize();

            // at this stage of computation, all the vectors (position, velocity and momemtum) are normalized here

            // test of orthogonality between position and momentum
            assertTrue(Math.abs(Vector3D.dotProduct(position, momentum)) < Utils.epsilonTest);
            // test of orthogonality between velocity and momentum
            assertTrue(Math.abs(Vector3D.dotProduct(velocity, momentum)) < Utils.epsilonTest);
        }

    }

    public void testGeometryCirc() {

        //  circular and equatorial orbit
        double hx =  0.1e-8;
        double hy =  0.1e-8;
        double i  = 2 * Math.atan(Math.sqrt(hx * hx + hy * hy));
        double raan = Math.atan2(hy, hx);
        CircularOrbit pCirEqua =
            new CircularOrbit(42166.712, 0.1e-8, 0.1e-8, i, raan,
                                   0.67 - raan, CircularOrbit.TRUE_LONGITUDE_ARGUMENT, 
                                   Frame.getJ2000(), date, mu);

        Vector3D position = pCirEqua.getPVCoordinates().getPosition();
        Vector3D velocity = pCirEqua.getPVCoordinates().getVelocity();

        Vector3D momentum = Vector3D.crossProduct(position,velocity).normalize();

        double apogeeRadius  = pCirEqua.getA() * (1 + pCirEqua.getE());
        double perigeeRadius = pCirEqua.getA() * (1 - pCirEqua.getE());
        // test if apogee equals perigee
        assertEquals(perigeeRadius, apogeeRadius, 1.e+4 * Utils.epsilonTest * apogeeRadius);

        for (double alphaV = 0; alphaV <= 2 * Math.PI; alphaV += 2 * Math.PI/100.) {
//          pCirEqua.setAlphaV(alphaV);
            pCirEqua = new CircularOrbit(pCirEqua.getA() , pCirEqua.getCircularEx(), pCirEqua.getCircularEy(), pCirEqua.getI(),
                                              pCirEqua.getRightAscensionOfAscendingNode(),
                                              alphaV, 2, pCirEqua.getFrame(), date, mu);
            position = pCirEqua.getPVCoordinates().getPosition();

            // test if the norm pf the position is in the range [perigee radius, apogee radius]
            assertTrue((position.getNorm() - apogeeRadius)  <= (  apogeeRadius * Utils.epsilonTest));
            assertTrue((position.getNorm() - perigeeRadius) >= (- perigeeRadius * Utils.epsilonTest));

            position= position.normalize();
            velocity = pCirEqua.getPVCoordinates().getVelocity();
            velocity= velocity.normalize();

            // at this stage of computation, all the vectors (position, velocity and momemtum) are normalized here

            // test of orthogonality between position and momentum
            assertTrue(Math.abs(Vector3D.dotProduct(position, momentum)) < Utils.epsilonTest);
            // test of orthogonality between velocity and momentum
            assertTrue(Math.abs(Vector3D.dotProduct(velocity, momentum)) < Utils.epsilonTest);
        }
    }


    public void testSymmetryEll() {

        // elliptic and non equatorail orbit
        Vector3D position = new Vector3D(4512.9, 18260., -5127.);
        Vector3D velocity = new Vector3D(134664.6, 90066.8, 72047.6);
        PVCoordinates pvCoordinates = new PVCoordinates(position, velocity);

        CircularOrbit p = new CircularOrbit(pvCoordinates, Frame.getJ2000(), date, mu);

        Vector3D positionOffset = p.getPVCoordinates().getPosition();
        Vector3D velocityOffset = p.getPVCoordinates().getVelocity();

        positionOffset = positionOffset.subtract(position);
        velocityOffset = velocityOffset.subtract(velocity);

        assertEquals(0.0, positionOffset.getNorm(), position.getNorm() * Utils.epsilonTest);
        assertEquals(0.0, velocityOffset.getNorm(), velocity.getNorm() * Utils.epsilonTest);

    }

    public void testSymmetryCir() {
        // circular and equatorial orbit
        Vector3D position = new Vector3D(33051.2, 26184.9, -1.3E-5);
        Vector3D velocity = new Vector3D(-60376.2, 76208., 2.7E-4);
        PVCoordinates pvCoordinates = new PVCoordinates(position, velocity);

        CircularOrbit p = new CircularOrbit(pvCoordinates, Frame.getJ2000(), date, mu);

        Vector3D positionOffset = p.getPVCoordinates().getPosition().subtract(position);
        Vector3D velocityOffset = p.getPVCoordinates().getVelocity().subtract(velocity);

        assertEquals(0.0, positionOffset.getNorm(), position.getNorm() * Utils.epsilonTest);
        assertEquals(0.0, velocityOffset.getNorm(), velocity.getNorm() * Utils.epsilonTest);

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
        return new TestSuite(CircularParametersTest.class);
    }

}
