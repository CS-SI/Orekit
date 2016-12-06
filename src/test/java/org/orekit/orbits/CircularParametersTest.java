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
package org.orekit.orbits;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.RealMatrixPreservingVisitor;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.Transform;
import org.orekit.propagation.analytical.EcksteinHechlerPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.PVCoordinates;


public class CircularParametersTest {

    // Computation date
    private AbsoluteDate date;

    // Body mu
    private double mu;

    @Test
    public void testCircularToEquinoctialEll() {

        double ix = 1.200e-04;
        double iy = -1.16e-04;
        double i  = 2 * FastMath.asin(FastMath.sqrt((ix * ix + iy * iy) / 4));
        double raan = FastMath.atan2(iy, ix);

        // elliptic orbit
        CircularOrbit circ =
            new CircularOrbit(42166.712, 0.5, -0.5, i, raan,
                                   5.300 - raan, PositionAngle.MEAN,
                                   FramesFactory.getEME2000(), date, mu);
        Vector3D pos = circ.getPVCoordinates().getPosition();
        Vector3D vit = circ.getPVCoordinates().getVelocity();

        PVCoordinates pvCoordinates = new PVCoordinates( pos, vit);

        EquinoctialOrbit param = new EquinoctialOrbit(pvCoordinates, FramesFactory.getEME2000(), date, mu);
        Assert.assertEquals(param.getA(),  circ.getA(), Utils.epsilonTest * circ.getA());
        Assert.assertEquals(param.getEquinoctialEx(), circ.getEquinoctialEx(), Utils.epsilonE * FastMath.abs(circ.getE()));
        Assert.assertEquals(param.getEquinoctialEy(), circ.getEquinoctialEy(), Utils.epsilonE * FastMath.abs(circ.getE()));
        Assert.assertEquals(param.getHx(), circ.getHx(), Utils.epsilonAngle * FastMath.abs(circ.getI()));
        Assert.assertEquals(param.getHy(), circ.getHy(), Utils.epsilonAngle * FastMath.abs(circ.getI()));
        Assert.assertEquals(MathUtils.normalizeAngle(param.getLv(),circ.getLv()), circ.getLv(), Utils.epsilonAngle * FastMath.abs(circ.getLv()));

    }

    @Test
    public void testCircularToEquinoctialCirc() {

        double ix = 1.200e-04;
        double iy = -1.16e-04;
        double i  = 2 * FastMath.asin(FastMath.sqrt((ix * ix + iy * iy) / 4));
        double raan = FastMath.atan2(iy, ix);

        // circular orbit
        EquinoctialOrbit circCir =
            new EquinoctialOrbit(42166.712, 0.1e-10, -0.1e-10, i, raan,
                                      5.300 - raan, PositionAngle.MEAN,
                                      FramesFactory.getEME2000(), date, mu);
        Vector3D posCir = circCir.getPVCoordinates().getPosition();
        Vector3D vitCir = circCir.getPVCoordinates().getVelocity();

        PVCoordinates pvCoordinates = new PVCoordinates( posCir, vitCir);

        EquinoctialOrbit paramCir = new EquinoctialOrbit(pvCoordinates, FramesFactory.getEME2000(), date, mu);
        Assert.assertEquals(paramCir.getA(), circCir.getA(), Utils.epsilonTest * circCir.getA());
        Assert.assertEquals(paramCir.getEquinoctialEx(), circCir.getEquinoctialEx(), Utils.epsilonEcir * FastMath.abs(circCir.getE()));
        Assert.assertEquals(paramCir.getEquinoctialEy(), circCir.getEquinoctialEy(), Utils.epsilonEcir * FastMath.abs(circCir.getE()));
        Assert.assertEquals(paramCir.getHx(), circCir.getHx(), Utils.epsilonAngle * FastMath.abs(circCir.getI()));
        Assert.assertEquals(paramCir.getHy(), circCir.getHy(), Utils.epsilonAngle * FastMath.abs(circCir.getI()));
        Assert.assertEquals(MathUtils.normalizeAngle(paramCir.getLv(),circCir.getLv()), circCir.getLv(), Utils.epsilonAngle * FastMath.abs(circCir.getLv()));

    }

    @Test
    public void testCircularToCartesian() {

        double ix = 1.200e-04;
        double iy = -1.16e-04;
        double i  = 2 * FastMath.asin(FastMath.sqrt((ix * ix + iy * iy) / 4));
        double raan = FastMath.atan2(iy, ix);
        double cosRaan = FastMath.cos(raan);
        double sinRaan = FastMath.sin(raan);
        double exTilde = -7.900e-6;
        double eyTilde = 1.100e-4;
        double ex = exTilde * cosRaan + eyTilde * sinRaan;
        double ey = eyTilde * cosRaan - exTilde * sinRaan;

        CircularOrbit circ=
            new CircularOrbit(42166.712, ex, ey, i, raan,
                                   5.300 - raan, PositionAngle.MEAN,
                                   FramesFactory.getEME2000(), date, mu);
        Vector3D pos = circ.getPVCoordinates().getPosition();
        Vector3D vel = circ.getPVCoordinates().getVelocity();

        // check 1/a = 2/r  - V2/mu
        double r = pos.getNorm();
        double v = vel.getNorm();
        Assert.assertEquals(2 / r - v * v / mu, 1 / circ.getA(), 1.0e-7);

        Assert.assertEquals( 0.233745668678733e+05, pos.getX(), Utils.epsilonTest * r);
        Assert.assertEquals(-0.350998914352669e+05, pos.getY(), Utils.epsilonTest * r);
        Assert.assertEquals(-0.150053723123334e+01, pos.getZ(), Utils.epsilonTest * r);

        Assert.assertEquals(0.809135038364960e+05, vel.getX(), Utils.epsilonTest * v);
        Assert.assertEquals(0.538902268252598e+05, vel.getY(), Utils.epsilonTest * v);
        Assert.assertEquals(0.158527938296630e+02, vel.getZ(), Utils.epsilonTest * v);

    }

    @Test
    public void testCircularToKeplerian() {

        double ix   =  1.20e-4;
        double iy   = -1.16e-4;
        double i    = 2 * FastMath.asin(FastMath.sqrt((ix * ix + iy * iy) / 4));
        double raan = FastMath.atan2(iy, ix);
        double cosRaan = FastMath.cos(raan);
        double sinRaan = FastMath.sin(raan);
        double exTilde = -7.900e-6;
        double eyTilde = 1.100e-4;
        double ex = exTilde * cosRaan + eyTilde * sinRaan;
        double ey = eyTilde * cosRaan - exTilde * sinRaan;

        CircularOrbit circ=
            new CircularOrbit(42166.712, ex, ey, i, raan,
                                   5.300 - raan, PositionAngle.MEAN,
                                   FramesFactory.getEME2000(), date, mu);
        KeplerianOrbit kep = new KeplerianOrbit(circ);

        Assert.assertEquals(42166.71200, circ.getA(), Utils.epsilonTest * kep.getA());
        Assert.assertEquals(0.110283316961361e-03, kep.getE(), Utils.epsilonE * FastMath.abs(kep.getE()));
        Assert.assertEquals(0.166901168553917e-03, kep.getI(),
                     Utils.epsilonAngle * FastMath.abs(kep.getI()));
        Assert.assertEquals(MathUtils.normalizeAngle(-3.87224326008837, kep.getPerigeeArgument()),
                     kep.getPerigeeArgument(),
                     Utils.epsilonTest * FastMath.abs(kep.getPerigeeArgument()));
        Assert.assertEquals(MathUtils.normalizeAngle(5.51473467358854, kep.getRightAscensionOfAscendingNode()),
                     kep.getRightAscensionOfAscendingNode(),
                     Utils.epsilonTest * FastMath.abs(kep.getRightAscensionOfAscendingNode()));
        Assert.assertEquals(MathUtils.normalizeAngle(3.65750858649982, kep.getMeanAnomaly()),
                     kep.getMeanAnomaly(),
                     Utils.epsilonTest * FastMath.abs(kep.getMeanAnomaly()));

    }

    @Test(expected=IllegalArgumentException.class)
    public void testHyperbolic() {
        new CircularOrbit(42166.712, 0.9, 0.5, 0.01, -0.02, 5.300,
                          PositionAngle.MEAN,  FramesFactory.getEME2000(), date, mu);
    }

    @Test
    public void testAnomalyEll() {

        // elliptic orbit
        Vector3D position = new Vector3D(7.0e6, 1.0e6, 4.0e6);
        Vector3D velocity = new Vector3D(-500.0, 8000.0, 1000.0);

        PVCoordinates pvCoordinates = new PVCoordinates( position, velocity);

        CircularOrbit  p   = new CircularOrbit(pvCoordinates, FramesFactory.getEME2000(), date, mu);
        KeplerianOrbit kep = new KeplerianOrbit(p);

        double e       = p.getE();
        double eRatio  = FastMath.sqrt((1 - e) / (1 + e));
        double raan    = kep.getRightAscensionOfAscendingNode();
        double paPraan = kep.getPerigeeArgument() + raan;

        double lv = 1.1;
        // formulations for elliptic case
        double lE = 2 * FastMath.atan(eRatio * FastMath.tan((lv - paPraan) / 2)) + paPraan;
        double lM = lE - e * FastMath.sin(lE - paPraan);

        p = new CircularOrbit(p.getA() , p.getCircularEx(), p.getCircularEy(),
                                   p.getRightAscensionOfAscendingNode(),
                                   p.getAlphaV(), lv - raan, PositionAngle.TRUE, p.getFrame(), date, mu);
        Assert.assertEquals(p.getAlphaV() + raan, lv, Utils.epsilonAngle * FastMath.abs(lv));
        Assert.assertEquals(p.getAlphaE() + raan, lE, Utils.epsilonAngle * FastMath.abs(lE));
        Assert.assertEquals(p.getAlphaM() + raan, lM, Utils.epsilonAngle * FastMath.abs(lM));
        p = new CircularOrbit(p.getA() , p.getCircularEx(), p.getCircularEy(),
                                   p.getRightAscensionOfAscendingNode(),
                                   p.getAlphaV(), 0, PositionAngle.TRUE, p.getFrame(), date, mu);


        p = new CircularOrbit(p.getA() , p.getCircularEx(), p.getCircularEy(),
                                   p.getRightAscensionOfAscendingNode(),
                                   p.getAlphaV(), lE - raan, PositionAngle.ECCENTRIC, p.getFrame(), date, mu);
        Assert.assertEquals(p.getAlphaV() + raan, lv, Utils.epsilonAngle * FastMath.abs(lv));
        Assert.assertEquals(p.getAlphaE() + raan, lE, Utils.epsilonAngle * FastMath.abs(lE));
        Assert.assertEquals(p.getAlphaM() + raan, lM, Utils.epsilonAngle * FastMath.abs(lM));
        p = new CircularOrbit(p.getA() , p.getCircularEx(), p.getCircularEy(),
                                   p.getRightAscensionOfAscendingNode(),
                                   p.getAlphaV(), 0, PositionAngle.TRUE, p.getFrame(), date, mu);

        p = new CircularOrbit(p.getA() , p.getCircularEx(), p.getCircularEy(),
                                   p.getRightAscensionOfAscendingNode(),
                                   p.getAlphaV(), lM - raan, PositionAngle.MEAN, p.getFrame(), date, mu);
        Assert.assertEquals(p.getAlphaV() + raan, lv, Utils.epsilonAngle * FastMath.abs(lv));
        Assert.assertEquals(p.getAlphaE() + raan, lE, Utils.epsilonAngle * FastMath.abs(lE));
        Assert.assertEquals(p.getAlphaM() + raan, lM, Utils.epsilonAngle * FastMath.abs(lM));

    }

    @Test
    public void testAnomalyCirc() {

        Vector3D position = new Vector3D(7.0e6, 1.0e6, 4.0e6);
        Vector3D velocity = new Vector3D(-500.0, 8000.0, 1000.0);
        PVCoordinates pvCoordinates = new PVCoordinates( position, velocity);
        CircularOrbit  p = new CircularOrbit(pvCoordinates, FramesFactory.getEME2000(), date, mu);
        double raan = p.getRightAscensionOfAscendingNode();

        // circular orbit
        p = new CircularOrbit(p.getA() , 0, 0, p.getRightAscensionOfAscendingNode(),
                                   p.getAlphaV(), p.getAlphaV(), PositionAngle.TRUE, p.getFrame(), date, mu);

        double lv = 1.1;
        double lE = lv;
        double lM = lE;

        p = new CircularOrbit(p.getA() , p.getCircularEx(), p.getCircularEy(),
                                   p.getRightAscensionOfAscendingNode(),
                                   p.getAlphaV(), lv - raan, PositionAngle.TRUE, p.getFrame(), date, mu);
        Assert.assertEquals(p.getAlphaV() + raan, lv, Utils.epsilonAngle * FastMath.abs(lv));
        Assert.assertEquals(p.getAlphaE() + raan, lE, Utils.epsilonAngle * FastMath.abs(lE));
        Assert.assertEquals(p.getAlphaM() + raan, lM, Utils.epsilonAngle * FastMath.abs(lM));
        p = new CircularOrbit(p.getA() , p.getCircularEx(), p.getCircularEy(),
                                   p.getRightAscensionOfAscendingNode(),
                                   p.getAlphaV(), 0, PositionAngle.TRUE, p.getFrame(), date, mu);

        p = new CircularOrbit(p.getA() , p.getCircularEx(), p.getCircularEy(),
                                   p.getRightAscensionOfAscendingNode(),
                                   p.getAlphaV(), lE - raan, PositionAngle.ECCENTRIC, p.getFrame(), date, mu);

        Assert.assertEquals(p.getAlphaV() + raan, lv, Utils.epsilonAngle * FastMath.abs(lv));
        Assert.assertEquals(p.getAlphaE() + raan, lE, Utils.epsilonAngle * FastMath.abs(lE));
        Assert.assertEquals(p.getAlphaM() + raan, lM, Utils.epsilonAngle * FastMath.abs(lM));
        p = new CircularOrbit(p.getA() , p.getCircularEx(), p.getCircularEy(),
                                   p.getRightAscensionOfAscendingNode(),
                                   p.getAlphaV(), 0, PositionAngle.TRUE, p.getFrame(), date, mu);

        p = new CircularOrbit(p.getA() , p.getCircularEx(), p.getCircularEy(),
                                   p.getRightAscensionOfAscendingNode(),
                                   p.getAlphaV(), lM - raan, PositionAngle.MEAN, p.getFrame(), date, mu);
        Assert.assertEquals(p.getAlphaV() + raan, lv, Utils.epsilonAngle * FastMath.abs(lv));
        Assert.assertEquals(p.getAlphaE() + raan, lE, Utils.epsilonAngle * FastMath.abs(lE));
        Assert.assertEquals(p.getAlphaM() + raan, lM, Utils.epsilonAngle * FastMath.abs(lM));

    }

    @Test
    public void testPositionVelocityNormsEll() {

        // elliptic and non equatorial (i retrograde) orbit
        double hx =  1.2;
        double hy =  2.1;
        double i  = 2 * FastMath.atan(FastMath.sqrt(hx * hx + hy * hy));
        double raan = FastMath.atan2(hy, hx);
        CircularOrbit p =
            new CircularOrbit(42166.712, 0.5, -0.5, i, raan,
                                   0.67 - raan, PositionAngle.TRUE,
                                   FramesFactory.getEME2000(), date, mu);

        double ex = p.getEquinoctialEx();
        double ey = p.getEquinoctialEy();
        double lv = p.getLv();
        double ksi     = 1 + ex * FastMath.cos(lv) + ey * FastMath.sin(lv);
        double nu      = ex * FastMath.sin(lv) - ey * FastMath.cos(lv);
        double epsilon = FastMath.sqrt(1 - ex * ex - ey * ey);

        double a  = p.getA();
        double na = FastMath.sqrt(mu / a);

        Assert.assertEquals(a * epsilon * epsilon / ksi,
                     p.getPVCoordinates().getPosition().getNorm(),
                     Utils.epsilonTest * FastMath.abs(p.getPVCoordinates().getPosition().getNorm()));
        Assert.assertEquals(na * FastMath.sqrt(ksi * ksi + nu * nu) / epsilon,
                     p.getPVCoordinates().getVelocity().getNorm(),
                     Utils.epsilonTest * FastMath.abs(p.getPVCoordinates().getVelocity().getNorm()));

    }

    @Test
    public void testNumericalIssue25() throws OrekitException {
        Vector3D position = new Vector3D(3782116.14107698, 416663.11924914, 5875541.62103057);
        Vector3D velocity = new Vector3D(-6349.7848910501, 288.4061811651, 4066.9366759691);
        CircularOrbit orbit = new CircularOrbit(new PVCoordinates(position, velocity),
                                                FramesFactory.getEME2000(),
                                                new AbsoluteDate("2004-01-01T23:00:00.000",
                                                                 TimeScalesFactory.getUTC()),
                                                                 3.986004415E14);
        Assert.assertEquals(0.0, orbit.getE(), 2.0e-14);
    }

    @Test
    public void testPerfectlyEquatorial() throws OrekitException {
        Vector3D position = new Vector3D(-7293947.695148368, 5122184.668436634, 0.0);
        Vector3D velocity = new Vector3D(-3890.4029433398, -5369.811285264604, 0.0);
        CircularOrbit orbit = new CircularOrbit(new PVCoordinates(position, velocity),
                                                FramesFactory.getEME2000(),
                                                new AbsoluteDate("2004-01-01T23:00:00.000",
                                                                 TimeScalesFactory.getUTC()),
                                                3.986004415E14);
        Assert.assertEquals(0.0, orbit.getI(), 2.0e-14);
        Assert.assertEquals(0.0, orbit.getRightAscensionOfAscendingNode(), 2.0e-14);
    }

    @Test
    public void testPositionVelocityNormsCirc() {

        // elliptic and non equatorial (i retrograde) orbit
        double hx =  0.1e-8;
        double hy =  0.1e-8;
        double i  = 2 * FastMath.atan(FastMath.sqrt(hx * hx + hy * hy));
        double raan = FastMath.atan2(hy, hx);
        CircularOrbit pCirEqua =
            new CircularOrbit(42166.712, 0.1e-8, 0.1e-8, i, raan,
                                   0.67 - raan, PositionAngle.TRUE,
                                   FramesFactory.getEME2000(), date, mu);

        double ex = pCirEqua.getEquinoctialEx();
        double ey = pCirEqua.getEquinoctialEy();
        double lv = pCirEqua.getLv();
        double ksi     = 1 + ex * FastMath.cos(lv) + ey * FastMath.sin(lv);
        double nu      = ex * FastMath.sin(lv) - ey * FastMath.cos(lv);
        double epsilon = FastMath.sqrt(1 - ex * ex - ey * ey);

        double a  = pCirEqua.getA();
        double na = FastMath.sqrt(mu / a);

        Assert.assertEquals(a * epsilon * epsilon / ksi,
                     pCirEqua.getPVCoordinates().getPosition().getNorm(),
                     Utils.epsilonTest * FastMath.abs(pCirEqua.getPVCoordinates().getPosition().getNorm()));
        Assert.assertEquals(na * FastMath.sqrt(ksi * ksi + nu * nu) / epsilon,
                     pCirEqua.getPVCoordinates().getVelocity().getNorm(),
                     Utils.epsilonTest * FastMath.abs(pCirEqua.getPVCoordinates().getVelocity().getNorm()));
    }

    @Test
    public void testGeometryEll() {

        // elliptic and non equatorial (i retrograde) orbit
        double hx =  1.2;
        double hy =  2.1;
        double i  = 2 * FastMath.atan(FastMath.sqrt(hx * hx + hy * hy));
        double raan = FastMath.atan2(hy, hx);
        CircularOrbit p =
            new CircularOrbit(42166.712, 0.5, -0.5, i, raan,
                                   0.67 - raan, PositionAngle.TRUE,
                                   FramesFactory.getEME2000(), date, mu);

        Vector3D position = p.getPVCoordinates().getPosition();
        Vector3D velocity = p.getPVCoordinates().getVelocity();
        Vector3D momentum = p.getPVCoordinates().getMomentum().normalize();

        double apogeeRadius  = p.getA() * (1 + p.getE());
        double perigeeRadius = p.getA() * (1 - p.getE());

        for (double alphaV = 0; alphaV <= 2 * FastMath.PI; alphaV += 2 * FastMath.PI/100.) {
            p = new CircularOrbit(p.getA() , p.getCircularEx(), p.getCircularEy(), p.getI(),
                                       p.getRightAscensionOfAscendingNode(),
                                       alphaV, PositionAngle.TRUE, p.getFrame(), date, mu);
            position = p.getPVCoordinates().getPosition();
            // test if the norm of the position is in the range [perigee radius, apogee radius]
            // Warning: these tests are without absolute value by choice
            Assert.assertTrue((position.getNorm() - apogeeRadius)  <= (  apogeeRadius * Utils.epsilonTest));
            Assert.assertTrue((position.getNorm() - perigeeRadius) >= (- perigeeRadius * Utils.epsilonTest));

            position= position.normalize();
            velocity = p.getPVCoordinates().getVelocity();
            velocity= velocity.normalize();

            // at this stage of computation, all the vectors (position, velocity and momemtum) are normalized here

            // test of orthogonality between position and momentum
            Assert.assertTrue(FastMath.abs(Vector3D.dotProduct(position, momentum)) < Utils.epsilonTest);
            // test of orthogonality between velocity and momentum
            Assert.assertTrue(FastMath.abs(Vector3D.dotProduct(velocity, momentum)) < Utils.epsilonTest);
        }

    }

    @Test
    public void testGeometryCirc() {

        //  circular and equatorial orbit
        double hx =  0.1e-8;
        double hy =  0.1e-8;
        double i  = 2 * FastMath.atan(FastMath.sqrt(hx * hx + hy * hy));
        double raan = FastMath.atan2(hy, hx);
        CircularOrbit pCirEqua =
            new CircularOrbit(42166.712, 0.1e-8, 0.1e-8, i, raan,
                                   0.67 - raan, PositionAngle.TRUE,
                                   FramesFactory.getEME2000(), date, mu);

        Vector3D position = pCirEqua.getPVCoordinates().getPosition();
        Vector3D velocity = pCirEqua.getPVCoordinates().getVelocity();
        Vector3D momentum = pCirEqua.getPVCoordinates().getMomentum().normalize();

        double apogeeRadius  = pCirEqua.getA() * (1 + pCirEqua.getE());
        double perigeeRadius = pCirEqua.getA() * (1 - pCirEqua.getE());
        // test if apogee equals perigee
        Assert.assertEquals(perigeeRadius, apogeeRadius, 1.e+4 * Utils.epsilonTest * apogeeRadius);

        for (double alphaV = 0; alphaV <= 2 * FastMath.PI; alphaV += 2 * FastMath.PI/100.) {
            pCirEqua = new CircularOrbit(pCirEqua.getA() , pCirEqua.getCircularEx(), pCirEqua.getCircularEy(), pCirEqua.getI(),
                                              pCirEqua.getRightAscensionOfAscendingNode(),
                                              alphaV, PositionAngle.TRUE, pCirEqua.getFrame(), date, mu);
            position = pCirEqua.getPVCoordinates().getPosition();

            // test if the norm pf the position is in the range [perigee radius, apogee radius]
            Assert.assertTrue((position.getNorm() - apogeeRadius)  <= (  apogeeRadius * Utils.epsilonTest));
            Assert.assertTrue((position.getNorm() - perigeeRadius) >= (- perigeeRadius * Utils.epsilonTest));

            position= position.normalize();
            velocity = pCirEqua.getPVCoordinates().getVelocity();
            velocity= velocity.normalize();

            // at this stage of computation, all the vectors (position, velocity and momemtum) are normalized here

            // test of orthogonality between position and momentum
            Assert.assertTrue(FastMath.abs(Vector3D.dotProduct(position, momentum)) < Utils.epsilonTest);
            // test of orthogonality between velocity and momentum
            Assert.assertTrue(FastMath.abs(Vector3D.dotProduct(velocity, momentum)) < Utils.epsilonTest);
        }
    }

    @Test
    public void testSymmetryEll() {

        // elliptic and non equatorail orbit
        Vector3D position = new Vector3D(4512.9, 18260., -5127.);
        Vector3D velocity = new Vector3D(134664.6, 90066.8, 72047.6);
        PVCoordinates pvCoordinates = new PVCoordinates(position, velocity);

        CircularOrbit p = new CircularOrbit(pvCoordinates, FramesFactory.getEME2000(), date, mu);

        Vector3D positionOffset = p.getPVCoordinates().getPosition();
        Vector3D velocityOffset = p.getPVCoordinates().getVelocity();

        positionOffset = positionOffset.subtract(position);
        velocityOffset = velocityOffset.subtract(velocity);

        Assert.assertEquals(0.0, positionOffset.getNorm(), position.getNorm() * Utils.epsilonTest);
        Assert.assertEquals(0.0, velocityOffset.getNorm(), velocity.getNorm() * Utils.epsilonTest);

    }

    @Test
    public void testSymmetryCir() {
        // circular and equatorial orbit
        Vector3D position = new Vector3D(33051.2, 26184.9, -1.3E-5);
        Vector3D velocity = new Vector3D(-60376.2, 76208., 2.7E-4);
        PVCoordinates pvCoordinates = new PVCoordinates(position, velocity);

        CircularOrbit p = new CircularOrbit(pvCoordinates, FramesFactory.getEME2000(), date, mu);

        Vector3D positionOffset = p.getPVCoordinates().getPosition().subtract(position);
        Vector3D velocityOffset = p.getPVCoordinates().getVelocity().subtract(velocity);

        Assert.assertEquals(0.0, positionOffset.getNorm(), position.getNorm() * Utils.epsilonTest);
        Assert.assertEquals(0.0, velocityOffset.getNorm(), velocity.getNorm() * Utils.epsilonTest);

    }

    @Test(expected=IllegalArgumentException.class)
    public void testNonInertialFrame() throws IllegalArgumentException {

        Vector3D position = new Vector3D(33051.2, 26184.9, -1.3E-5);
        Vector3D velocity = new Vector3D(-60376.2, 76208., 2.7E-4);
        PVCoordinates pvCoordinates = new PVCoordinates( position, velocity);
        new CircularOrbit(pvCoordinates,
                          new Frame(FramesFactory.getEME2000(), Transform.IDENTITY, "non-inertial", false),
                          date, mu);
    }

    @Test
    public void testJacobianReference() throws OrekitException {

        AbsoluteDate dateTca = new AbsoluteDate(2000, 04, 01, 0, 0, 0.000, TimeScalesFactory.getUTC());
        double mu =  3.986004415e+14;
        CircularOrbit orbCir = new CircularOrbit(7000000.0, 0.01, -0.02, 1.2, 2.1,
                                                 0.7, PositionAngle.MEAN,
                                                 FramesFactory.getEME2000(), dateTca, mu);

        // the following reference values have been computed using the free software
        // version 6.2 of the MSLIB fortran library by the following program:
        //        program cir_jacobian
        //
        //        use mslib
        //        implicit none
        //
        //        integer, parameter :: nb = 11
        //        integer :: i,j
        //        type(tm_code_retour)      ::  code_retour
        //
        //        real(pm_reel), parameter :: mu= 3.986004415e+14_pm_reel
        //        real(pm_reel),dimension(3)::vit_car,pos_car
        //        type(tm_orb_cir)::cir
        //        real(pm_reel), dimension(6,6)::jacob
        //        real(pm_reel)::norme
        //
        //
        //        cir%a=7000000_pm_reel
        //        cir%ex=0.01_pm_reel
        //        cir%ey=-0.02_pm_reel
        //        cir%i=1.2_pm_reel
        //        cir%gom=2.1_pm_reel
        //        cir%pso_M=0.7_pm_reel
        //
        //        call mv_cir_car(mu,cir,pos_car,vit_car,code_retour)
        //        write(*,*)code_retour%valeur
        //        write(*,1000)pos_car,vit_car
        //
        //
        //        call mu_norme(pos_car,norme,code_retour)
        //        write(*,*)norme
        //
        //        call mv_car_cir (mu, pos_car, vit_car, cir, code_retour, jacob)
        //        write(*,*)code_retour%valeur
        //
        //        write(*,*)"circular = ", cir%a, cir%ex, cir%ey, cir%i, cir%gom, cir%pso_M
        //
        //        do i = 1,6
        //           write(*,*) " ",(jacob(i,j),j=1,6)
        //        end do
        //
        //        1000 format (6(f24.15,1x))
        //        end program cir_jacobian
        Vector3D pRef = new Vector3D(-4106905.105389204807580, 3603162.539798960555345, 4439730.167038885876536);
        Vector3D vRef = new Vector3D(740.132407342422994, -5308.773280141396754, 5250.338353483879473);
        double[][] jRef = {
            { -1.1535467596325562,        1.0120556393573172,        1.2470306024626943,        181.96913090864561,       -1305.2162699469984,        1290.8494448855752      },
            { -5.07367368325471104E-008, -1.27870567070456834E-008,  1.31544531338558113E-007, -3.09332106417043592E-005, -9.60781276304445404E-005,  1.91506964883791605E-004 },
            { -6.59428471712402018E-008,  1.24561703203882533E-007, -1.41907027322388158E-008,  7.63442601186485441E-005, -1.77446722746170009E-004,  5.99464401287846734E-005 },
            {  7.55079920652274275E-008,  4.41606835295069131E-008,  3.40079310688458225E-008,  7.89724635377817962E-005,  4.61868720707717372E-005,  3.55682891687782599E-005 },
            { -9.20788748896973282E-008, -5.38521280004949642E-008, -4.14712660805579618E-008,  7.78626692360739821E-005,  4.55378113077967091E-005,  3.50684505810897702E-005 },
            {  1.85082436324531617E-008,  1.20506219457886855E-007, -8.31277842285972640E-008,  1.27364008345789645E-004, -1.54770720974742483E-004, -1.78589436862677754E-004 }
        };

        PVCoordinates pv = orbCir.getPVCoordinates();
        Assert.assertEquals(0, pv.getPosition().subtract(pRef).getNorm(), 3.0e-16 * pRef.getNorm());
        Assert.assertEquals(0, pv.getVelocity().subtract(vRef).getNorm(), 2.0e-16 * vRef.getNorm());

        double[][] jacobian = new double[6][6];
        orbCir.getJacobianWrtCartesian(PositionAngle.MEAN, jacobian);

        for (int i = 0; i < jacobian.length; i++) {
            double[] row    = jacobian[i];
            double[] rowRef = jRef[i];
            for (int j = 0; j < row.length; j++) {
                Assert.assertEquals(0, (row[j] - rowRef[j]) / rowRef[j], 5.0e-15);
            }
        }

    }

    @Test
    public void testJacobianFinitedifferences() throws OrekitException {

        AbsoluteDate dateTca = new AbsoluteDate(2000, 04, 01, 0, 0, 0.000, TimeScalesFactory.getUTC());
        double mu =  3.986004415e+14;
        CircularOrbit orbCir = new CircularOrbit(7000000.0, 0.01, -0.02, 1.2, 2.1,
                                                 0.7, PositionAngle.MEAN,
                                                 FramesFactory.getEME2000(), dateTca, mu);

        for (PositionAngle type : PositionAngle.values()) {
            double hP = 2.0;
            double[][] finiteDiffJacobian = finiteDifferencesJacobian(type, orbCir, hP);
            double[][] jacobian = new double[6][6];
            orbCir.getJacobianWrtCartesian(type, jacobian);

            for (int i = 0; i < jacobian.length; i++) {
                double[] row    = jacobian[i];
                double[] rowRef = finiteDiffJacobian[i];
                for (int j = 0; j < row.length; j++) {
                    Assert.assertEquals(0, (row[j] - rowRef[j]) / rowRef[j], 8.0e-9);
                }
            }

            double[][] invJacobian = new double[6][6];
            orbCir.getJacobianWrtParameters(type, invJacobian);
            MatrixUtils.createRealMatrix(jacobian).
                            multiply(MatrixUtils.createRealMatrix(invJacobian)).
            walkInRowOrder(new RealMatrixPreservingVisitor() {
                public void start(int rows, int columns,
                                  int startRow, int endRow, int startColumn, int endColumn) {
                }

                public void visit(int row, int column, double value) {
                    Assert.assertEquals(row == column ? 1.0 : 0.0, value, 4.0e-9);
                }

                public double end() {
                    return Double.NaN;
                }
            });

        }

    }

    private double[][] finiteDifferencesJacobian(PositionAngle type, CircularOrbit orbit, double hP)
        throws OrekitException {
        double[][] jacobian = new double[6][6];
        for (int i = 0; i < 6; ++i) {
            fillColumn(type, i, orbit, hP, jacobian);
        }
        return jacobian;
    }

    private void fillColumn(PositionAngle type, int i, CircularOrbit orbit, double hP, double[][] jacobian) {

        // at constant energy (i.e. constant semi major axis), we have dV = -mu dP / (V * r^2)
        // we use this to compute a velocity step size from the position step size
        Vector3D p = orbit.getPVCoordinates().getPosition();
        Vector3D v = orbit.getPVCoordinates().getVelocity();
        double hV = orbit.getMu() * hP / (v.getNorm() * p.getNormSq());

        double h;
        Vector3D dP = Vector3D.ZERO;
        Vector3D dV = Vector3D.ZERO;
        switch (i) {
        case 0:
            h = hP;
            dP = new Vector3D(hP, 0, 0);
            break;
        case 1:
            h = hP;
            dP = new Vector3D(0, hP, 0);
            break;
        case 2:
            h = hP;
            dP = new Vector3D(0, 0, hP);
            break;
        case 3:
            h = hV;
            dV = new Vector3D(hV, 0, 0);
            break;
        case 4:
            h = hV;
            dV = new Vector3D(0, hV, 0);
            break;
        default:
            h = hV;
            dV = new Vector3D(0, 0, hV);
            break;
        }

        CircularOrbit oM4h = new CircularOrbit(new PVCoordinates(new Vector3D(1, p, -4, dP), new Vector3D(1, v, -4, dV)),
                                               orbit.getFrame(), orbit.getDate(), orbit.getMu());
        CircularOrbit oM3h = new CircularOrbit(new PVCoordinates(new Vector3D(1, p, -3, dP), new Vector3D(1, v, -3, dV)),
                                               orbit.getFrame(), orbit.getDate(), orbit.getMu());
        CircularOrbit oM2h = new CircularOrbit(new PVCoordinates(new Vector3D(1, p, -2, dP), new Vector3D(1, v, -2, dV)),
                                               orbit.getFrame(), orbit.getDate(), orbit.getMu());
        CircularOrbit oM1h = new CircularOrbit(new PVCoordinates(new Vector3D(1, p, -1, dP), new Vector3D(1, v, -1, dV)),
                                               orbit.getFrame(), orbit.getDate(), orbit.getMu());
        CircularOrbit oP1h = new CircularOrbit(new PVCoordinates(new Vector3D(1, p, +1, dP), new Vector3D(1, v, +1, dV)),
                                               orbit.getFrame(), orbit.getDate(), orbit.getMu());
        CircularOrbit oP2h = new CircularOrbit(new PVCoordinates(new Vector3D(1, p, +2, dP), new Vector3D(1, v, +2, dV)),
                                               orbit.getFrame(), orbit.getDate(), orbit.getMu());
        CircularOrbit oP3h = new CircularOrbit(new PVCoordinates(new Vector3D(1, p, +3, dP), new Vector3D(1, v, +3, dV)),
                                               orbit.getFrame(), orbit.getDate(), orbit.getMu());
        CircularOrbit oP4h = new CircularOrbit(new PVCoordinates(new Vector3D(1, p, +4, dP), new Vector3D(1, v, +4, dV)),
                                               orbit.getFrame(), orbit.getDate(), orbit.getMu());

        jacobian[0][i] = (-3 * (oP4h.getA()                             - oM4h.getA()) +
                          32 * (oP3h.getA()                             - oM3h.getA()) -
                         168 * (oP2h.getA()                             - oM2h.getA()) +
                         672 * (oP1h.getA()                             - oM1h.getA())) / (840 * h);
        jacobian[1][i] = (-3 * (oP4h.getCircularEx()                    - oM4h.getCircularEx()) +
                          32 * (oP3h.getCircularEx()                    - oM3h.getCircularEx()) -
                         168 * (oP2h.getCircularEx()                    - oM2h.getCircularEx()) +
                         672 * (oP1h.getCircularEx()                    - oM1h.getCircularEx())) / (840 * h);
        jacobian[2][i] = (-3 * (oP4h.getCircularEy()                    - oM4h.getCircularEy()) +
                          32 * (oP3h.getCircularEy()                    - oM3h.getCircularEy()) -
                         168 * (oP2h.getCircularEy()                    - oM2h.getCircularEy()) +
                         672 * (oP1h.getCircularEy()                    - oM1h.getCircularEy())) / (840 * h);
        jacobian[3][i] = (-3 * (oP4h.getI()                             - oM4h.getI()) +
                          32 * (oP3h.getI()                             - oM3h.getI()) -
                         168 * (oP2h.getI()                             - oM2h.getI()) +
                         672 * (oP1h.getI()                             - oM1h.getI())) / (840 * h);
        jacobian[4][i] = (-3 * (oP4h.getRightAscensionOfAscendingNode() - oM4h.getRightAscensionOfAscendingNode()) +
                          32 * (oP3h.getRightAscensionOfAscendingNode() - oM3h.getRightAscensionOfAscendingNode()) -
                         168 * (oP2h.getRightAscensionOfAscendingNode() - oM2h.getRightAscensionOfAscendingNode()) +
                         672 * (oP1h.getRightAscensionOfAscendingNode() - oM1h.getRightAscensionOfAscendingNode())) / (840 * h);
        jacobian[5][i] = (-3 * (oP4h.getAlpha(type)                     - oM4h.getAlpha(type)) +
                          32 * (oP3h.getAlpha(type)                     - oM3h.getAlpha(type)) -
                         168 * (oP2h.getAlpha(type)                     - oM2h.getAlpha(type)) +
                         672 * (oP1h.getAlpha(type)                     - oM1h.getAlpha(type))) / (840 * h);

    }

    @Test
    public void testInterpolation() throws OrekitException {

        final double ehMu  = 3.9860047e14;
        final double ae  = 6.378137e6;
        final double c20 = -1.08263e-3;
        final double c30 = 2.54e-6;
        final double c40 = 1.62e-6;
        final double c50 = 2.3e-7;
        final double c60 = -5.5e-7;

        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH.shiftedBy(584.);
        final Vector3D position = new Vector3D(3220103., 69623., 6449822.);
        final Vector3D velocity = new Vector3D(6414.7, -2006., -3180.);
        final CircularOrbit initialOrbit = new CircularOrbit(new PVCoordinates(position, velocity),
                                                             FramesFactory.getEME2000(), date, ehMu);

        EcksteinHechlerPropagator propagator =
                new EcksteinHechlerPropagator(initialOrbit, ae, ehMu, c20, c30, c40, c50, c60);

        // set up a 5 points sample
        List<Orbit> sample = new ArrayList<Orbit>();
        for (double dt = 0; dt < 300.0; dt += 60.0) {
            sample.add(propagator.propagate(date.shiftedBy(dt)).getOrbit());
        }

        // well inside the sample, interpolation should be much better than Keplerian shift
        double maxShiftError = 0;
        double maxInterpolationError = 0;
        for (double dt = 0; dt < 241.0; dt += 1.0) {
            AbsoluteDate t        = initialOrbit.getDate().shiftedBy(dt);
            Vector3D shifted      = initialOrbit.shiftedBy(dt).getPVCoordinates().getPosition();
            Vector3D interpolated = initialOrbit.interpolate(t, sample).getPVCoordinates().getPosition();
            Vector3D propagated   = propagator.propagate(t).getPVCoordinates().getPosition();
            maxShiftError = FastMath.max(maxShiftError, shifted.subtract(propagated).getNorm());
            maxInterpolationError = FastMath.max(maxInterpolationError, interpolated.subtract(propagated).getNorm());
        }
        Assert.assertTrue(maxShiftError         > 390.0);
        Assert.assertTrue(maxInterpolationError < 0.04);

        // slightly past sample end, interpolation should quickly increase, but remain reasonable
        maxShiftError = 0;
        maxInterpolationError = 0;
        for (double dt = 240; dt < 300.0; dt += 1.0) {
            AbsoluteDate t        = initialOrbit.getDate().shiftedBy(dt);
            Vector3D shifted      = initialOrbit.shiftedBy(dt).getPVCoordinates().getPosition();
            Vector3D interpolated = initialOrbit.interpolate(t, sample).getPVCoordinates().getPosition();
            Vector3D propagated   = propagator.propagate(t).getPVCoordinates().getPosition();
            maxShiftError = FastMath.max(maxShiftError, shifted.subtract(propagated).getNorm());
            maxInterpolationError = FastMath.max(maxInterpolationError, interpolated.subtract(propagated).getNorm());
        }
        Assert.assertTrue(maxShiftError         <  610.0);
        Assert.assertTrue(maxInterpolationError <    1.3);

        // far past sample end, interpolation should become really wrong
        // (in this test case, break even occurs at around 863 seconds, with a 3.9 km error)
        maxShiftError = 0;
        maxInterpolationError = 0;
        for (double dt = 300; dt < 1000; dt += 1.0) {
            AbsoluteDate t        = initialOrbit.getDate().shiftedBy(dt);
            Vector3D shifted      = initialOrbit.shiftedBy(dt).getPVCoordinates().getPosition();
            Vector3D interpolated = initialOrbit.interpolate(t, sample).getPVCoordinates().getPosition();
            Vector3D propagated   = propagator.propagate(t).getPVCoordinates().getPosition();
            maxShiftError = FastMath.max(maxShiftError, shifted.subtract(propagated).getNorm());
            maxInterpolationError = FastMath.max(maxInterpolationError, interpolated.subtract(propagated).getNorm());
        }
        Assert.assertTrue(maxShiftError         < 5000.0);
        Assert.assertTrue(maxInterpolationError > 8800.0);

    }

    @Test
    public void testSerialization()
      throws IOException, ClassNotFoundException, NoSuchFieldException, IllegalAccessException {
        Vector3D position = new Vector3D(-29536113.0, 30329259.0, -100125.0);
        Vector3D velocity = new Vector3D(-2194.0, -2141.0, -8.0);
        PVCoordinates pvCoordinates = new PVCoordinates( position, velocity);
        CircularOrbit orbit = new CircularOrbit(pvCoordinates, FramesFactory.getEME2000(), date, mu);
        Assert.assertEquals(42255170.003, orbit.getA(), 1.0e-3);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream    oos = new ObjectOutputStream(bos);
        oos.writeObject(orbit);

        Assert.assertTrue(bos.size() > 350);
        Assert.assertTrue(bos.size() < 400);

        ByteArrayInputStream  bis = new ByteArrayInputStream(bos.toByteArray());
        ObjectInputStream     ois = new ObjectInputStream(bis);
        CircularOrbit deserialized  = (CircularOrbit) ois.readObject();
        Assert.assertEquals(orbit.getA(), deserialized.getA(), 1.0e-10);
        Assert.assertEquals(orbit.getCircularEx(), deserialized.getCircularEx(), 1.0e-10);
        Assert.assertEquals(orbit.getCircularEy(), deserialized.getCircularEy(), 1.0e-10);
        Assert.assertEquals(orbit.getRightAscensionOfAscendingNode(), deserialized.getRightAscensionOfAscendingNode(), 1.0e-10);
        Assert.assertEquals(orbit.getAlphaV(), deserialized.getAlphaV(), 1.0e-10);
        Assert.assertEquals(orbit.getDate(), deserialized.getDate());
        Assert.assertEquals(orbit.getMu(), deserialized.getMu(), 1.0e-10);
        Assert.assertEquals(orbit.getFrame().getName(), deserialized.getFrame().getName());

    }

    @Before
    public void setUp() {

        Utils.setDataRoot("regular-data");

        // Computation date
        date = AbsoluteDate.J2000_EPOCH;

        // Body mu
        mu = 3.9860047e14;
    }

    @After
    public void tearDown() {
        date = null;
    }

}
