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
package org.orekit.orbits;

import org.hamcrest.MatcherAssert;
import org.hipparchus.analysis.UnivariateFunction;
import org.hipparchus.analysis.differentiation.DSFactory;
import org.hipparchus.analysis.differentiation.FiniteDifferencesDifferentiator;
import org.hipparchus.analysis.differentiation.UnivariateDifferentiableFunction;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.RealMatrixPreservingVisitor;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.errors.OrekitIllegalArgumentException;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.Transform;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.function.Function;

import static org.orekit.OrekitMatchers.relativelyCloseTo;


public class CircularOrbitTest {

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
            new CircularOrbit(42166712.0, 0.5, -0.5, i, raan,
                                   5.300 - raan, PositionAngleType.MEAN,
                                   FramesFactory.getEME2000(), date, mu);
        Vector3D pos = circ.getPosition();
        Vector3D vit = circ.getPVCoordinates().getVelocity();

        PVCoordinates pvCoordinates = new PVCoordinates( pos, vit);

        EquinoctialOrbit param = new EquinoctialOrbit(pvCoordinates, FramesFactory.getEME2000(), date, mu);
        Assertions.assertEquals(param.getA(),  circ.getA(), Utils.epsilonTest * circ.getA());
        Assertions.assertEquals(param.getEquinoctialEx(), circ.getEquinoctialEx(), Utils.epsilonE * FastMath.abs(circ.getE()));
        Assertions.assertEquals(param.getEquinoctialEy(), circ.getEquinoctialEy(), Utils.epsilonE * FastMath.abs(circ.getE()));
        Assertions.assertEquals(param.getHx(), circ.getHx(), Utils.epsilonAngle * FastMath.abs(circ.getI()));
        Assertions.assertEquals(param.getHy(), circ.getHy(), Utils.epsilonAngle * FastMath.abs(circ.getI()));
        Assertions.assertEquals(MathUtils.normalizeAngle(param.getLv(), circ.getLv()), circ.getLv(), Utils.epsilonAngle * FastMath.abs(circ.getLv()));

    }

    @Test
    public void testCircularToEquinoctialCirc() {

        double ix = 1.200e-04;
        double iy = -1.16e-04;
        double i  = 2 * FastMath.asin(FastMath.sqrt((ix * ix + iy * iy) / 4));
        double raan = FastMath.atan2(iy, ix);

        // circular orbit
        EquinoctialOrbit circCir =
            new EquinoctialOrbit(42166712.0, 0.1e-10, -0.1e-10, i, raan,
                                      5.300 - raan, PositionAngleType.MEAN,
                                      FramesFactory.getEME2000(), date, mu);
        Vector3D posCir = circCir.getPosition();
        Vector3D vitCir = circCir.getPVCoordinates().getVelocity();

        PVCoordinates pvCoordinates = new PVCoordinates( posCir, vitCir);

        EquinoctialOrbit paramCir = new EquinoctialOrbit(pvCoordinates, FramesFactory.getEME2000(), date, mu);
        Assertions.assertEquals(paramCir.getA(), circCir.getA(), Utils.epsilonTest * circCir.getA());
        Assertions.assertEquals(paramCir.getEquinoctialEx(), circCir.getEquinoctialEx(), Utils.epsilonEcir * FastMath.abs(circCir.getE()));
        Assertions.assertEquals(paramCir.getEquinoctialEy(), circCir.getEquinoctialEy(), Utils.epsilonEcir * FastMath.abs(circCir.getE()));
        Assertions.assertEquals(paramCir.getHx(), circCir.getHx(), Utils.epsilonAngle * FastMath.abs(circCir.getI()));
        Assertions.assertEquals(paramCir.getHy(), circCir.getHy(), Utils.epsilonAngle * FastMath.abs(circCir.getI()));
        Assertions.assertEquals(MathUtils.normalizeAngle(paramCir.getLv(), circCir.getLv()), circCir.getLv(), Utils.epsilonAngle * FastMath.abs(circCir.getLv()));

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
            new CircularOrbit(42166712.0, ex, ey, i, raan,
                                   5.300 - raan, PositionAngleType.MEAN,
                                   FramesFactory.getEME2000(), date, mu);
        Vector3D pos = circ.getPosition();
        Vector3D vel = circ.getPVCoordinates().getVelocity();

        // check 1/a = 2/r  - V2/mu
        double r = pos.getNorm();
        double v = vel.getNorm();
        Assertions.assertEquals(2 / r - v * v / mu, 1 / circ.getA(), 1.0e-7);

        Assertions.assertEquals( 0.233745668678733e+08, pos.getX(), Utils.epsilonTest * r);
        Assertions.assertEquals(-0.350998914352669e+08, pos.getY(), Utils.epsilonTest * r);
        Assertions.assertEquals(-0.150053723123334e+04, pos.getZ(), Utils.epsilonTest * r);

        Assertions.assertEquals(2558.7096558809967, vel.getX(), Utils.epsilonTest * v);
        Assertions.assertEquals(1704.1586039092576, vel.getY(), Utils.epsilonTest * v);
        Assertions.assertEquals(   0.5013093577879, vel.getZ(), Utils.epsilonTest * v);

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
            new CircularOrbit(42166712.0, ex, ey, i, raan,
                                   5.300 - raan, PositionAngleType.MEAN,
                                   FramesFactory.getEME2000(), date, mu);
        KeplerianOrbit kep = new KeplerianOrbit(circ);

        Assertions.assertEquals(42166712.000, circ.getA(), Utils.epsilonTest * kep.getA());
        Assertions.assertEquals(0.110283316961361e-03, kep.getE(), Utils.epsilonE * FastMath.abs(kep.getE()));
        Assertions.assertEquals(0.166901168553917e-03, kep.getI(),
                     Utils.epsilonAngle * FastMath.abs(kep.getI()));
        Assertions.assertEquals(MathUtils.normalizeAngle(-3.87224326008837, kep.getPerigeeArgument()),
                     kep.getPerigeeArgument(),
                     Utils.epsilonTest * FastMath.abs(kep.getPerigeeArgument()));
        Assertions.assertEquals(MathUtils.normalizeAngle(5.51473467358854, kep.getRightAscensionOfAscendingNode()),
                     kep.getRightAscensionOfAscendingNode(),
                     Utils.epsilonTest * FastMath.abs(kep.getRightAscensionOfAscendingNode()));
        Assertions.assertEquals(MathUtils.normalizeAngle(3.65750858649982, kep.getMeanAnomaly()),
                     kep.getMeanAnomaly(),
                     Utils.epsilonTest * FastMath.abs(kep.getMeanAnomaly()));

    }

    @Test
    public void testHyperbolic1() {
        try {
            new CircularOrbit(42166712.0, 0.9, 0.5, 0.01, -0.02, 5.300,
                              PositionAngleType.MEAN,  FramesFactory.getEME2000(), date, mu);
        } catch (OrekitIllegalArgumentException oe) {
            Assertions.assertEquals(OrekitMessages.HYPERBOLIC_ORBIT_NOT_HANDLED_AS, oe.getSpecifier());
        }
    }

    @Test
    public void testHyperbolic2() {
        Orbit orbit = new KeplerianOrbit(42166712.0, 0.9, 0.5, 0.01, -0.02, 5.300,
                                         PositionAngleType.MEAN,  FramesFactory.getEME2000(), date, mu);
        try {
            new CircularOrbit(orbit.getPVCoordinates(), orbit.getFrame(), orbit.getMu());
        } catch (OrekitIllegalArgumentException oe) {
            Assertions.assertEquals(OrekitMessages.HYPERBOLIC_ORBIT_NOT_HANDLED_AS, oe.getSpecifier());
        }
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
                                   p.getAlphaV(), lv - raan, PositionAngleType.TRUE, p.getFrame(), date, mu);
        Assertions.assertEquals(p.getAlphaV() + raan, lv, Utils.epsilonAngle * FastMath.abs(lv));
        Assertions.assertEquals(p.getAlphaE() + raan, lE, Utils.epsilonAngle * FastMath.abs(lE));
        Assertions.assertEquals(p.getAlphaM() + raan, lM, Utils.epsilonAngle * FastMath.abs(lM));
        p = new CircularOrbit(p.getA() , p.getCircularEx(), p.getCircularEy(),
                                   p.getRightAscensionOfAscendingNode(),
                                   p.getAlphaV(), 0, PositionAngleType.TRUE, p.getFrame(), date, mu);


        p = new CircularOrbit(p.getA() , p.getCircularEx(), p.getCircularEy(),
                                   p.getRightAscensionOfAscendingNode(),
                                   p.getAlphaV(), lE - raan, PositionAngleType.ECCENTRIC, p.getFrame(), date, mu);
        Assertions.assertEquals(p.getAlphaV() + raan, lv, Utils.epsilonAngle * FastMath.abs(lv));
        Assertions.assertEquals(p.getAlphaE() + raan, lE, Utils.epsilonAngle * FastMath.abs(lE));
        Assertions.assertEquals(p.getAlphaM() + raan, lM, Utils.epsilonAngle * FastMath.abs(lM));
        p = new CircularOrbit(p.getA() , p.getCircularEx(), p.getCircularEy(),
                                   p.getRightAscensionOfAscendingNode(),
                                   p.getAlphaV(), 0, PositionAngleType.TRUE, p.getFrame(), date, mu);

        p = new CircularOrbit(p.getA() , p.getCircularEx(), p.getCircularEy(),
                                   p.getRightAscensionOfAscendingNode(),
                                   p.getAlphaV(), lM - raan, PositionAngleType.MEAN, p.getFrame(), date, mu);
        Assertions.assertEquals(p.getAlphaV() + raan, lv, Utils.epsilonAngle * FastMath.abs(lv));
        Assertions.assertEquals(p.getAlphaE() + raan, lE, Utils.epsilonAngle * FastMath.abs(lE));
        Assertions.assertEquals(p.getAlphaM() + raan, lM, Utils.epsilonAngle * FastMath.abs(lM));

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
                                   p.getAlphaV(), p.getAlphaV(), PositionAngleType.TRUE, p.getFrame(), date, mu);

        double lv = 1.1;
        double lE = lv;
        double lM = lE;

        p = new CircularOrbit(p.getA() , p.getCircularEx(), p.getCircularEy(),
                                   p.getRightAscensionOfAscendingNode(),
                                   p.getAlphaV(), lv - raan, PositionAngleType.TRUE, p.getFrame(), date, mu);
        Assertions.assertEquals(p.getAlphaV() + raan, lv, Utils.epsilonAngle * FastMath.abs(lv));
        Assertions.assertEquals(p.getAlphaE() + raan, lE, Utils.epsilonAngle * FastMath.abs(lE));
        Assertions.assertEquals(p.getAlphaM() + raan, lM, Utils.epsilonAngle * FastMath.abs(lM));
        p = new CircularOrbit(p.getA() , p.getCircularEx(), p.getCircularEy(),
                                   p.getRightAscensionOfAscendingNode(),
                                   p.getAlphaV(), 0, PositionAngleType.TRUE, p.getFrame(), date, mu);

        p = new CircularOrbit(p.getA() , p.getCircularEx(), p.getCircularEy(),
                                   p.getRightAscensionOfAscendingNode(),
                                   p.getAlphaV(), lE - raan, PositionAngleType.ECCENTRIC, p.getFrame(), date, mu);

        Assertions.assertEquals(p.getAlphaV() + raan, lv, Utils.epsilonAngle * FastMath.abs(lv));
        Assertions.assertEquals(p.getAlphaE() + raan, lE, Utils.epsilonAngle * FastMath.abs(lE));
        Assertions.assertEquals(p.getAlphaM() + raan, lM, Utils.epsilonAngle * FastMath.abs(lM));
        p = new CircularOrbit(p.getA() , p.getCircularEx(), p.getCircularEy(),
                                   p.getRightAscensionOfAscendingNode(),
                                   p.getAlphaV(), 0, PositionAngleType.TRUE, p.getFrame(), date, mu);

        p = new CircularOrbit(p.getA() , p.getCircularEx(), p.getCircularEy(),
                                   p.getRightAscensionOfAscendingNode(),
                                   p.getAlphaV(), lM - raan, PositionAngleType.MEAN, p.getFrame(), date, mu);
        Assertions.assertEquals(p.getAlphaV() + raan, lv, Utils.epsilonAngle * FastMath.abs(lv));
        Assertions.assertEquals(p.getAlphaE() + raan, lE, Utils.epsilonAngle * FastMath.abs(lE));
        Assertions.assertEquals(p.getAlphaM() + raan, lM, Utils.epsilonAngle * FastMath.abs(lM));

    }

    @Test
    public void testPositionVelocityNormsEll() {

        // elliptic and non equatorial (i retrograde) orbit
        double hx =  1.2;
        double hy =  2.1;
        double i  = 2 * FastMath.atan(FastMath.sqrt(hx * hx + hy * hy));
        double raan = FastMath.atan2(hy, hx);
        CircularOrbit p =
            new CircularOrbit(42166712.0, 0.5, -0.5, i, raan,
                                   0.67 - raan, PositionAngleType.TRUE,
                                   FramesFactory.getEME2000(), date, mu);

        double ex = p.getEquinoctialEx();
        double ey = p.getEquinoctialEy();
        double lv = p.getLv();
        double ksi     = 1 + ex * FastMath.cos(lv) + ey * FastMath.sin(lv);
        double nu      = ex * FastMath.sin(lv) - ey * FastMath.cos(lv);
        double epsilon = FastMath.sqrt(1 - ex * ex - ey * ey);

        double a  = p.getA();
        double na = FastMath.sqrt(mu / a);

        Assertions.assertEquals(a * epsilon * epsilon / ksi,
                     p.getPosition().getNorm(),
                     Utils.epsilonTest * FastMath.abs(p.getPosition().getNorm()));
        Assertions.assertEquals(na * FastMath.sqrt(ksi * ksi + nu * nu) / epsilon,
                     p.getPVCoordinates().getVelocity().getNorm(),
                     Utils.epsilonTest * FastMath.abs(p.getPVCoordinates().getVelocity().getNorm()));

    }

    @Test
    public void testNumericalIssue25() {
        Vector3D position = new Vector3D(3782116.14107698, 416663.11924914, 5875541.62103057);
        Vector3D velocity = new Vector3D(-6349.7848910501, 288.4061811651, 4066.9366759691);
        CircularOrbit orbit = new CircularOrbit(new PVCoordinates(position, velocity),
                                                FramesFactory.getEME2000(),
                                                new AbsoluteDate("2004-01-01T23:00:00.000",
                                                                 TimeScalesFactory.getUTC()),
                                                                 3.986004415E14);
        Assertions.assertEquals(0.0, orbit.getE(), 2.0e-14);
    }

    @Test
    public void testPerfectlyEquatorial() {
        Vector3D position = new Vector3D(-7293947.695148368, 5122184.668436634, 0.0);
        Vector3D velocity = new Vector3D(-3890.4029433398, -5369.811285264604, 0.0);
        CircularOrbit orbit = new CircularOrbit(new PVCoordinates(position, velocity),
                                                FramesFactory.getEME2000(),
                                                new AbsoluteDate("2004-01-01T23:00:00.000",
                                                                 TimeScalesFactory.getUTC()),
                                                3.986004415E14);
        Assertions.assertEquals(0.0, orbit.getI(), 2.0e-14);
        Assertions.assertEquals(0.0, orbit.getRightAscensionOfAscendingNode(), 2.0e-14);
    }

    @Test
    public void testPositionVelocityNormsCirc() {

        // elliptic and non equatorial (i retrograde) orbit
        double hx =  0.1e-8;
        double hy =  0.1e-8;
        double i  = 2 * FastMath.atan(FastMath.sqrt(hx * hx + hy * hy));
        double raan = FastMath.atan2(hy, hx);
        CircularOrbit pCirEqua =
            new CircularOrbit(42166712.0, 0.1e-8, 0.1e-8, i, raan,
                                   0.67 - raan, PositionAngleType.TRUE,
                                   FramesFactory.getEME2000(), date, mu);

        double ex = pCirEqua.getEquinoctialEx();
        double ey = pCirEqua.getEquinoctialEy();
        double lv = pCirEqua.getLv();
        double ksi     = 1 + ex * FastMath.cos(lv) + ey * FastMath.sin(lv);
        double nu      = ex * FastMath.sin(lv) - ey * FastMath.cos(lv);
        double epsilon = FastMath.sqrt(1 - ex * ex - ey * ey);

        double a  = pCirEqua.getA();
        double na = FastMath.sqrt(mu / a);

        Assertions.assertEquals(a * epsilon * epsilon / ksi,
                     pCirEqua.getPosition().getNorm(),
                     Utils.epsilonTest * FastMath.abs(pCirEqua.getPosition().getNorm()));
        Assertions.assertEquals(na * FastMath.sqrt(ksi * ksi + nu * nu) / epsilon,
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
            new CircularOrbit(42166712.0, 0.5, -0.5, i, raan,
                                   0.67 - raan, PositionAngleType.TRUE,
                                   FramesFactory.getEME2000(), date, mu);

        Vector3D position = p.getPosition();
        Vector3D velocity = p.getPVCoordinates().getVelocity();
        Vector3D momentum = p.getPVCoordinates().getMomentum().normalize();

        double apogeeRadius  = p.getA() * (1 + p.getE());
        double perigeeRadius = p.getA() * (1 - p.getE());

        for (double alphaV = 0; alphaV <= 2 * FastMath.PI; alphaV += 2 * FastMath.PI/100.) {
            p = new CircularOrbit(p.getA() , p.getCircularEx(), p.getCircularEy(), p.getI(),
                                       p.getRightAscensionOfAscendingNode(),
                                       alphaV, PositionAngleType.TRUE, p.getFrame(), date, mu);
            position = p.getPosition();
            // test if the norm of the position is in the range [perigee radius, apogee radius]
            // Warning: these tests are without absolute value by choice
            Assertions.assertTrue((position.getNorm() - apogeeRadius)  <= (  apogeeRadius * Utils.epsilonTest));
            Assertions.assertTrue((position.getNorm() - perigeeRadius) >= (- perigeeRadius * Utils.epsilonTest));

            position= position.normalize();
            velocity = p.getPVCoordinates().getVelocity();
            velocity= velocity.normalize();

            // at this stage of computation, all the vectors (position, velocity and momemtum) are normalized here

            // test of orthogonality between position and momentum
            Assertions.assertTrue(FastMath.abs(Vector3D.dotProduct(position, momentum)) < Utils.epsilonTest);
            // test of orthogonality between velocity and momentum
            Assertions.assertTrue(FastMath.abs(Vector3D.dotProduct(velocity, momentum)) < Utils.epsilonTest);
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
            new CircularOrbit(42166712.0, 0.1e-8, 0.1e-8, i, raan,
                                   0.67 - raan, PositionAngleType.TRUE,
                                   FramesFactory.getEME2000(), date, mu);

        Vector3D position = pCirEqua.getPosition();
        Vector3D velocity = pCirEqua.getPVCoordinates().getVelocity();
        Vector3D momentum = pCirEqua.getPVCoordinates().getMomentum().normalize();

        double apogeeRadius  = pCirEqua.getA() * (1 + pCirEqua.getE());
        double perigeeRadius = pCirEqua.getA() * (1 - pCirEqua.getE());
        // test if apogee equals perigee
        Assertions.assertEquals(perigeeRadius, apogeeRadius, 1.e+4 * Utils.epsilonTest * apogeeRadius);

        for (double alphaV = 0; alphaV <= 2 * FastMath.PI; alphaV += 2 * FastMath.PI/100.) {
            pCirEqua = new CircularOrbit(pCirEqua.getA() , pCirEqua.getCircularEx(), pCirEqua.getCircularEy(), pCirEqua.getI(),
                                              pCirEqua.getRightAscensionOfAscendingNode(),
                                              alphaV, PositionAngleType.TRUE, pCirEqua.getFrame(), date, mu);
            position = pCirEqua.getPosition();

            // test if the norm pf the position is in the range [perigee radius, apogee radius]
            Assertions.assertTrue((position.getNorm() - apogeeRadius)  <= (  apogeeRadius * Utils.epsilonTest));
            Assertions.assertTrue((position.getNorm() - perigeeRadius) >= (- perigeeRadius * Utils.epsilonTest));

            position= position.normalize();
            velocity = pCirEqua.getPVCoordinates().getVelocity();
            velocity= velocity.normalize();

            // at this stage of computation, all the vectors (position, velocity and momemtum) are normalized here

            // test of orthogonality between position and momentum
            Assertions.assertTrue(FastMath.abs(Vector3D.dotProduct(position, momentum)) < Utils.epsilonTest);
            // test of orthogonality between velocity and momentum
            Assertions.assertTrue(FastMath.abs(Vector3D.dotProduct(velocity, momentum)) < Utils.epsilonTest);
        }
    }

    @Test
    public void testSymmetryEll() {

        // elliptic and non equatorail orbit
        Vector3D position = new Vector3D(4512.9, 18260., -5127.);
        Vector3D velocity = new Vector3D(134664.6, 90066.8, 72047.6);
        PVCoordinates pvCoordinates = new PVCoordinates(position, velocity);

        CircularOrbit p = new CircularOrbit(pvCoordinates, FramesFactory.getEME2000(), date, mu);

        Vector3D positionOffset = p.getPosition();
        Vector3D velocityOffset = p.getPVCoordinates().getVelocity();

        positionOffset = positionOffset.subtract(position);
        velocityOffset = velocityOffset.subtract(velocity);

        Assertions.assertEquals(0.0, positionOffset.getNorm(), position.getNorm() * Utils.epsilonTest);
        Assertions.assertEquals(0.0, velocityOffset.getNorm(), velocity.getNorm() * Utils.epsilonTest);

    }

    @Test
    public void testSymmetryCir() {
        // circular and equatorial orbit
        Vector3D position = new Vector3D(33051.2, 26184.9, -1.3E-5);
        Vector3D velocity = new Vector3D(-60376.2, 76208., 2.7E-4);
        PVCoordinates pvCoordinates = new PVCoordinates(position, velocity);

        CircularOrbit p = new CircularOrbit(pvCoordinates, FramesFactory.getEME2000(), date, mu);

        Vector3D positionOffset = p.getPosition().subtract(position);
        Vector3D velocityOffset = p.getPVCoordinates().getVelocity().subtract(velocity);

        Assertions.assertEquals(0.0, positionOffset.getNorm(), position.getNorm() * Utils.epsilonTest);
        Assertions.assertEquals(0.0, velocityOffset.getNorm(), velocity.getNorm() * Utils.epsilonTest);

    }

    @Test
    public void testNonInertialFrame() throws IllegalArgumentException {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Vector3D position = new Vector3D(33051.2, 26184.9, -1.3E-5);
            Vector3D velocity = new Vector3D(-60376.2, 76208., 2.7E-4);
            PVCoordinates pvCoordinates = new PVCoordinates( position, velocity);
            new CircularOrbit(pvCoordinates,
                    new Frame(FramesFactory.getEME2000(), Transform.IDENTITY, "non-inertial", false),
                    date, mu);
        });
    }

    @Test
    public void testJacobianReference() {

        AbsoluteDate dateTca = new AbsoluteDate(2000, 04, 01, 0, 0, 0.000, TimeScalesFactory.getUTC());
        double mu =  3.986004415e+14;
        CircularOrbit orbCir = new CircularOrbit(7000000.0, 0.01, -0.02, 1.2, 2.1,
                                                 0.7, PositionAngleType.MEAN,
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
        Assertions.assertEquals(0, pv.getPosition().subtract(pRef).getNorm(), 3.0e-16 * pRef.getNorm());
        Assertions.assertEquals(0, pv.getVelocity().subtract(vRef).getNorm(), 2.0e-16 * vRef.getNorm());

        double[][] jacobian = new double[6][6];
        orbCir.getJacobianWrtCartesian(PositionAngleType.MEAN, jacobian);

        for (int i = 0; i < jacobian.length; i++) {
            double[] row    = jacobian[i];
            double[] rowRef = jRef[i];
            for (int j = 0; j < row.length; j++) {
                Assertions.assertEquals(0, (row[j] - rowRef[j]) / rowRef[j], 5.0e-15);
            }
        }

    }

    @Test
    public void testJacobianFinitedifferences() {

        AbsoluteDate dateTca = new AbsoluteDate(2000, 04, 01, 0, 0, 0.000, TimeScalesFactory.getUTC());
        double mu =  3.986004415e+14;
        CircularOrbit orbCir = new CircularOrbit(7000000.0, 0.01, -0.02, 1.2, 2.1,
                                                 0.7, PositionAngleType.MEAN,
                                                 FramesFactory.getEME2000(), dateTca, mu);

        for (PositionAngleType type : PositionAngleType.values()) {
            double hP = 2.0;
            double[][] finiteDiffJacobian = finiteDifferencesJacobian(type, orbCir, hP);
            double[][] jacobian = new double[6][6];
            orbCir.getJacobianWrtCartesian(type, jacobian);

            for (int i = 0; i < jacobian.length; i++) {
                double[] row    = jacobian[i];
                double[] rowRef = finiteDiffJacobian[i];
                for (int j = 0; j < row.length; j++) {
                    Assertions.assertEquals(0, (row[j] - rowRef[j]) / rowRef[j], 8.0e-9);
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
                    Assertions.assertEquals(row == column ? 1.0 : 0.0, value, 4.0e-9);
                }

                public double end() {
                    return Double.NaN;
                }
            });

        }

    }

    private double[][] finiteDifferencesJacobian(PositionAngleType type, CircularOrbit orbit, double hP)
        {
        double[][] jacobian = new double[6][6];
        for (int i = 0; i < 6; ++i) {
            fillColumn(type, i, orbit, hP, jacobian);
        }
        return jacobian;
    }

    private void fillColumn(PositionAngleType type, int i, CircularOrbit orbit, double hP, double[][] jacobian) {

        // at constant energy (i.e. constant semi major axis), we have dV = -mu dP / (V * r^2)
        // we use this to compute a velocity step size from the position step size
        Vector3D p = orbit.getPosition();
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
    public void testSerialization()
      throws IOException, ClassNotFoundException {
        Vector3D position = new Vector3D(-29536113.0, 30329259.0, -100125.0);
        Vector3D velocity = new Vector3D(-2194.0, -2141.0, -8.0);
        PVCoordinates pvCoordinates = new PVCoordinates( position, velocity);
        CircularOrbit orbit = new CircularOrbit(pvCoordinates, FramesFactory.getEME2000(), date, mu);
        Assertions.assertEquals(42255170.003, orbit.getA(), 1.0e-3);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream    oos = new ObjectOutputStream(bos);
        oos.writeObject(orbit);

        Assertions.assertTrue(bos.size() > 350);
        Assertions.assertTrue(bos.size() < 400);

        ByteArrayInputStream  bis = new ByteArrayInputStream(bos.toByteArray());
        ObjectInputStream     ois = new ObjectInputStream(bis);
        CircularOrbit deserialized  = (CircularOrbit) ois.readObject();
        Assertions.assertEquals(orbit.getA(), deserialized.getA(), 1.0e-10);
        Assertions.assertEquals(orbit.getCircularEx(), deserialized.getCircularEx(), 1.0e-10);
        Assertions.assertEquals(orbit.getCircularEy(), deserialized.getCircularEy(), 1.0e-10);
        Assertions.assertEquals(orbit.getI(), deserialized.getI(), 1.0e-10);
        Assertions.assertEquals(orbit.getRightAscensionOfAscendingNode(), deserialized.getRightAscensionOfAscendingNode(), 1.0e-10);
        Assertions.assertEquals(orbit.getAlphaV(), deserialized.getAlphaV(), 1.0e-10);
        Assertions.assertTrue(Double.isNaN(orbit.getADot()) && Double.isNaN(deserialized.getADot()));
        Assertions.assertTrue(Double.isNaN(orbit.getCircularExDot()) && Double.isNaN(deserialized.getCircularExDot()));
        Assertions.assertTrue(Double.isNaN(orbit.getCircularEyDot()) && Double.isNaN(deserialized.getCircularEyDot()));
        Assertions.assertTrue(Double.isNaN(orbit.getIDot()) && Double.isNaN(deserialized.getIDot()));
        Assertions.assertTrue(Double.isNaN(orbit.getRightAscensionOfAscendingNodeDot()) && Double.isNaN(deserialized.getRightAscensionOfAscendingNodeDot()));
        Assertions.assertTrue(Double.isNaN(orbit.getAlphaVDot()) && Double.isNaN(deserialized.getAlphaVDot()));
        Assertions.assertEquals(orbit.getDate(), deserialized.getDate());
        Assertions.assertEquals(orbit.getMu(), deserialized.getMu(), 1.0e-10);
        Assertions.assertEquals(orbit.getFrame().getName(), deserialized.getFrame().getName());

    }

    @Test
    public void testSerializationWithDerivatives()
      throws IOException, ClassNotFoundException {
        Vector3D position = new Vector3D(-29536113.0, 30329259.0, -100125.0);
        Vector3D velocity = new Vector3D(-2194.0, -2141.0, -8.0);
        double r2 = position.getNormSq();
        double r  = FastMath.sqrt(r2);
        Vector3D acceleration = new Vector3D(-mu / (r * r2), position,
                                             1, new Vector3D(-0.1, 0.2, 0.3));
        PVCoordinates pvCoordinates = new PVCoordinates( position, velocity, acceleration);
        CircularOrbit orbit = new CircularOrbit(pvCoordinates, FramesFactory.getEME2000(), date, mu);
        Assertions.assertEquals(42255170.003, orbit.getA(), 1.0e-3);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream    oos = new ObjectOutputStream(bos);
        oos.writeObject(orbit);

        Assertions.assertTrue(bos.size() > 400);
        Assertions.assertTrue(bos.size() < 450);

        ByteArrayInputStream  bis = new ByteArrayInputStream(bos.toByteArray());
        ObjectInputStream     ois = new ObjectInputStream(bis);
        CircularOrbit deserialized  = (CircularOrbit) ois.readObject();
        Assertions.assertEquals(orbit.getA(), deserialized.getA(), 1.0e-10);
        Assertions.assertEquals(orbit.getCircularEx(), deserialized.getCircularEx(), 1.0e-10);
        Assertions.assertEquals(orbit.getCircularEy(), deserialized.getCircularEy(), 1.0e-10);
        Assertions.assertEquals(orbit.getI(), deserialized.getI(), 1.0e-10);
        Assertions.assertEquals(orbit.getRightAscensionOfAscendingNode(), deserialized.getRightAscensionOfAscendingNode(), 1.0e-10);
        Assertions.assertEquals(orbit.getAlphaV(), deserialized.getAlphaV(), 1.0e-10);
        Assertions.assertEquals(orbit.getADot(), deserialized.getADot(), 1.0e-10);
        Assertions.assertEquals(orbit.getCircularExDot(), deserialized.getCircularExDot(), 1.0e-10);
        Assertions.assertEquals(orbit.getCircularEyDot(), deserialized.getCircularEyDot(), 1.0e-10);
        Assertions.assertEquals(orbit.getIDot(), deserialized.getIDot(), 1.0e-10);
        Assertions.assertEquals(orbit.getRightAscensionOfAscendingNodeDot(), deserialized.getRightAscensionOfAscendingNodeDot(), 1.0e-10);
        Assertions.assertEquals(orbit.getAlphaVDot(), deserialized.getAlphaVDot(), 1.0e-10);
        Assertions.assertEquals(orbit.getDate(), deserialized.getDate());
        Assertions.assertEquals(orbit.getMu(), deserialized.getMu(), 1.0e-10);
        Assertions.assertEquals(orbit.getFrame().getName(), deserialized.getFrame().getName());

    }

    @Test
    public void testSerializationNoPVWithDerivatives()
      throws IOException, ClassNotFoundException {
        Vector3D position = new Vector3D(-29536113.0, 30329259.0, -100125.0);
        Vector3D velocity = new Vector3D(-2194.0, -2141.0, -8.0);
        double r2 = position.getNormSq();
        double r  = FastMath.sqrt(r2);
        Vector3D acceleration = new Vector3D(-mu / (r * r2), position,
                                             1, new Vector3D(-0.1, 0.2, 0.3));
        PVCoordinates pvCoordinates = new PVCoordinates( position, velocity, acceleration);
        CircularOrbit original = new CircularOrbit(pvCoordinates, FramesFactory.getEME2000(), date, mu);

        // rebuild the same orbit, preserving derivatives but removing Cartesian coordinates
        // (to check one specific path in serialization.deserialization)
        CircularOrbit orbit = new CircularOrbit(original.getA(), original.getCircularEx(), original.getCircularEy(),
                                                original.getI(), original.getRightAscensionOfAscendingNode(),
                                                original.getAlphaV(),
                                                original.getADot(), original.getCircularExDot(), original.getCircularEyDot(),
                                                original.getIDot(), original.getRightAscensionOfAscendingNodeDot(),
                                                original.getAlphaVDot(),
                                                PositionAngleType.TRUE, original.getFrame(),
                                                original.getDate(), original.getMu());
        Assertions.assertEquals(42255170.003, orbit.getA(), 1.0e-3);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream    oos = new ObjectOutputStream(bos);
        oos.writeObject(orbit);

        Assertions.assertTrue(bos.size() > 330);
        Assertions.assertTrue(bos.size() < 380);

        ByteArrayInputStream  bis = new ByteArrayInputStream(bos.toByteArray());
        ObjectInputStream     ois = new ObjectInputStream(bis);
        CircularOrbit deserialized  = (CircularOrbit) ois.readObject();
        Assertions.assertEquals(orbit.getA(), deserialized.getA(), 1.0e-10);
        Assertions.assertEquals(orbit.getCircularEx(), deserialized.getCircularEx(), 1.0e-10);
        Assertions.assertEquals(orbit.getCircularEy(), deserialized.getCircularEy(), 1.0e-10);
        Assertions.assertEquals(orbit.getI(), deserialized.getI(), 1.0e-10);
        Assertions.assertEquals(orbit.getRightAscensionOfAscendingNode(), deserialized.getRightAscensionOfAscendingNode(), 1.0e-10);
        Assertions.assertEquals(orbit.getAlphaV(), deserialized.getAlphaV(), 1.0e-10);
        Assertions.assertEquals(orbit.getADot(), deserialized.getADot(), 1.0e-10);
        Assertions.assertEquals(orbit.getCircularExDot(), deserialized.getCircularExDot(), 1.0e-10);
        Assertions.assertEquals(orbit.getCircularEyDot(), deserialized.getCircularEyDot(), 1.0e-10);
        Assertions.assertEquals(orbit.getIDot(), deserialized.getIDot(), 1.0e-10);
        Assertions.assertEquals(orbit.getRightAscensionOfAscendingNodeDot(), deserialized.getRightAscensionOfAscendingNodeDot(), 1.0e-10);
        Assertions.assertEquals(orbit.getAlphaVDot(), deserialized.getAlphaVDot(), 1.0e-10);
        Assertions.assertEquals(orbit.getDate(), deserialized.getDate());
        Assertions.assertEquals(orbit.getMu(), deserialized.getMu(), 1.0e-10);
        Assertions.assertEquals(orbit.getFrame().getName(), deserialized.getFrame().getName());

    }

    @Test
    public void testNonKeplerianDerivatives() {
        final AbsoluteDate date         = new AbsoluteDate("2003-05-01T00:00:20.000", TimeScalesFactory.getUTC());
        final Vector3D     position     = new Vector3D(6896874.444705,  1956581.072644,  -147476.245054);
        final Vector3D     velocity     = new Vector3D(166.816407662, -1106.783301861, -7372.745712770);
        final Vector3D     acceleration = new Vector3D(-7.466182457944, -2.118153357345,  0.160004048437);
        final TimeStampedPVCoordinates pv = new TimeStampedPVCoordinates(date, position, velocity, acceleration);
        final Frame frame = FramesFactory.getEME2000();
        final double mu   = Constants.EIGEN5C_EARTH_MU;
        final CircularOrbit orbit = new CircularOrbit(pv, frame, mu);

        Assertions.assertEquals(differentiate(pv, frame, mu, shifted -> shifted.getA()),
                            orbit.getADot(),
                            4.3e-8);
        Assertions.assertEquals(differentiate(pv, frame, mu, shifted -> shifted.getEquinoctialEx()),
                            orbit.getEquinoctialExDot(),
                            2.1e-15);
        Assertions.assertEquals(differentiate(pv, frame, mu, shifted -> shifted.getEquinoctialEy()),
                            orbit.getEquinoctialEyDot(),
                            5.4e-16);
        Assertions.assertEquals(differentiate(pv, frame, mu, shifted -> shifted.getHx()),
                            orbit.getHxDot(),
                            1.6e-15);
        Assertions.assertEquals(differentiate(pv, frame, mu, shifted -> shifted.getHy()),
                            orbit.getHyDot(),
                            7.3e-17);
        Assertions.assertEquals(differentiate(pv, frame, mu, shifted -> shifted.getLv()),
                            orbit.getLvDot(),
                            3.4e-16);
        Assertions.assertEquals(differentiate(pv, frame, mu, shifted -> shifted.getLE()),
                            orbit.getLEDot(),
                            3.5e-15);
        Assertions.assertEquals(differentiate(pv, frame, mu, shifted -> shifted.getLM()),
                            orbit.getLMDot(),
                            5.3e-15);
        Assertions.assertEquals(differentiate(pv, frame, mu, shifted -> shifted.getE()),
                            orbit.getEDot(),
                            6.8e-16);
        Assertions.assertEquals(differentiate(pv, frame, mu, shifted -> shifted.getI()),
                            orbit.getIDot(),
                            5.7e-16);
        Assertions.assertEquals(differentiate(pv, frame, mu, shifted -> shifted.getCircularEx()),
                            orbit.getCircularExDot(),
                            2.2e-15);
        Assertions.assertEquals(differentiate(pv, frame, mu, shifted -> shifted.getCircularEy()),
                            orbit.getCircularEyDot(),
                            5.3e-17);
        Assertions.assertEquals(differentiate(pv, frame, mu, shifted -> shifted.getAlphaV()),
                            orbit.getAlphaVDot(),
                            4.3e-15);
        Assertions.assertEquals(differentiate(pv, frame, mu, shifted -> shifted.getAlphaE()),
                            orbit.getAlphaEDot(),
                            1.2e-15);
        Assertions.assertEquals(differentiate(pv, frame, mu, shifted -> shifted.getAlphaM()),
                            orbit.getAlphaMDot(),
                            3.7e-15);
        Assertions.assertEquals(differentiate(pv, frame, mu, shifted -> shifted.getAlpha(PositionAngleType.TRUE)),
                            orbit.getAlphaDot(PositionAngleType.TRUE),
                            4.3e-15);
        Assertions.assertEquals(differentiate(pv, frame, mu, shifted -> shifted.getAlpha(PositionAngleType.ECCENTRIC)),
                            orbit.getAlphaDot(PositionAngleType.ECCENTRIC),
                            1.2e-15);
        Assertions.assertEquals(differentiate(pv, frame, mu, shifted -> shifted.getAlpha(PositionAngleType.MEAN)),
                            orbit.getAlphaDot(PositionAngleType.MEAN),
                            3.7e-15);

    }

    private <S extends Function<CircularOrbit, Double>>
    double differentiate(TimeStampedPVCoordinates pv, Frame frame, double mu, S picker) {
        final DSFactory factory = new DSFactory(1, 1);
        FiniteDifferencesDifferentiator differentiator = new FiniteDifferencesDifferentiator(8, 0.1);
        UnivariateDifferentiableFunction diff = differentiator.differentiate(new UnivariateFunction() {
            public double value(double dt) {
                return picker.apply(new CircularOrbit(pv.shiftedBy(dt), frame, mu));
            }
        });
        return diff.value(factory.variable(0, 0.0)).getPartialDerivative(1);
     }

    @Test
    public void testPositionAngleDerivatives() {
        final AbsoluteDate date         = new AbsoluteDate("2003-05-01T00:00:20.000", TimeScalesFactory.getUTC());
        final Vector3D     position     = new Vector3D(6896874.444705,  1956581.072644,  -147476.245054);
        final Vector3D     velocity     = new Vector3D(166.816407662, -1106.783301861, -7372.745712770);
        final Vector3D     acceleration = new Vector3D(-7.466182457944, -2.118153357345,  0.160004048437);
        final TimeStampedPVCoordinates pv = new TimeStampedPVCoordinates(date, position, velocity, acceleration);
        final Frame frame = FramesFactory.getEME2000();
        final double mu   = Constants.EIGEN5C_EARTH_MU;
        final CircularOrbit orbit = new CircularOrbit(pv, frame, mu);

        for (PositionAngleType type : PositionAngleType.values()) {
            final CircularOrbit rebuilt = new CircularOrbit(orbit.getA(),
                                                            orbit.getCircularEx(),
                                                            orbit.getCircularEy(),
                                                            orbit.getI(),
                                                            orbit.getRightAscensionOfAscendingNode(),
                                                            orbit.getAlpha(type),
                                                            orbit.getADot(),
                                                            orbit.getCircularExDot(),
                                                            orbit.getCircularEyDot(),
                                                            orbit.getIDot(),
                                                            orbit.getRightAscensionOfAscendingNodeDot(),
                                                            orbit.getAlphaDot(type),
                                                            type, orbit.getFrame(), orbit.getDate(), orbit.getMu());
            MatcherAssert.assertThat(rebuilt.getA(),                                relativelyCloseTo(orbit.getA(),                                1));
            MatcherAssert.assertThat(rebuilt.getCircularEx(),                       relativelyCloseTo(orbit.getCircularEx(),                       1));
            MatcherAssert.assertThat(rebuilt.getCircularEy(),                       relativelyCloseTo(orbit.getCircularEy(),                       1));
            MatcherAssert.assertThat(rebuilt.getE(),                                relativelyCloseTo(orbit.getE(),                                1));
            MatcherAssert.assertThat(rebuilt.getI(),                                relativelyCloseTo(orbit.getI(),                                1));
            MatcherAssert.assertThat(rebuilt.getRightAscensionOfAscendingNode(),    relativelyCloseTo(orbit.getRightAscensionOfAscendingNode(),    1));
            MatcherAssert.assertThat(rebuilt.getADot(),                             relativelyCloseTo(orbit.getADot(),                             1));
            MatcherAssert.assertThat(rebuilt.getCircularExDot(),                    relativelyCloseTo(orbit.getCircularExDot(),                    1));
            MatcherAssert.assertThat(rebuilt.getCircularEyDot(),                    relativelyCloseTo(orbit.getCircularEyDot(),                    1));
            MatcherAssert.assertThat(rebuilt.getEDot(),                             relativelyCloseTo(orbit.getEDot(),                             1));
            MatcherAssert.assertThat(rebuilt.getIDot(),                             relativelyCloseTo(orbit.getIDot(),                             1));
            MatcherAssert.assertThat(rebuilt.getRightAscensionOfAscendingNodeDot(), relativelyCloseTo(orbit.getRightAscensionOfAscendingNodeDot(), 1));
            for (PositionAngleType type2 : PositionAngleType.values()) {
                MatcherAssert.assertThat(rebuilt.getAlpha(type2),    relativelyCloseTo(orbit.getAlpha(type2),    1));
                MatcherAssert.assertThat(rebuilt.getAlphaDot(type2), relativelyCloseTo(orbit.getAlphaDot(type2), 1));
            }
        }

    }

    @Test
    public void testEquatorialRetrograde() {
        Vector3D position = new Vector3D(10000000.0, 0.0, 0.0);
        Vector3D velocity = new Vector3D(0.0, -6500.0, 1.0e-10);
        double r2 = position.getNormSq();
        double r  = FastMath.sqrt(r2);
        Vector3D acceleration = new Vector3D(-mu / (r * r2), position,
                                             1, new Vector3D(-0.1, 0.2, 0.3));
        PVCoordinates pvCoordinates = new PVCoordinates(position, velocity, acceleration);
        CircularOrbit orbit = new CircularOrbit(pvCoordinates, FramesFactory.getEME2000(), date, mu);
        Assertions.assertEquals(10637829.465, orbit.getA(), 1.0e-3);
        Assertions.assertEquals(-738.145, orbit.getADot(), 1.0e-3);
        Assertions.assertEquals(0.05995861, orbit.getE(), 1.0e-8);
        Assertions.assertEquals(-6.523e-5, orbit.getEDot(), 1.0e-8);
        Assertions.assertEquals(FastMath.PI, orbit.getI(), 2.0e-14);
        Assertions.assertEquals(-4.615e-5, orbit.getIDot(), 1.0e-8);
        Assertions.assertTrue(Double.isNaN(orbit.getHx()));
        Assertions.assertTrue(Double.isNaN(orbit.getHxDot()));
        Assertions.assertTrue(Double.isNaN(orbit.getHy()));
        Assertions.assertTrue(Double.isNaN(orbit.getHyDot()));
    }

    @Test
    public void testDerivativesConversionSymmetry() {
        final AbsoluteDate date = new AbsoluteDate("2003-05-01T00:01:20.000", TimeScalesFactory.getUTC());
        Vector3D position     = new Vector3D(6893443.400234382, 1886406.1073757345, -589265.1150359757);
        Vector3D velocity     = new Vector3D(-281.1261461082365, -1231.6165642450928, -7348.756363469432);
        Vector3D acceleration = new Vector3D(-7.460341170581685, -2.0415957334584527, 0.6393322823627762);
        PVCoordinates pvCoordinates = new PVCoordinates( position, velocity, acceleration);
        CircularOrbit orbit = new CircularOrbit(pvCoordinates, FramesFactory.getEME2000(),
                                                date, Constants.EIGEN5C_EARTH_MU);
        Assertions.assertTrue(orbit.hasDerivatives());
        double r2 = position.getNormSq();
        double r  = FastMath.sqrt(r2);
        Vector3D keplerianAcceleration = new Vector3D(-orbit.getMu() / (r2 * r), position);
        Assertions.assertEquals(0.0101, Vector3D.distance(keplerianAcceleration, acceleration), 1.0e-4);

        for (OrbitType type : OrbitType.values()) {
            Orbit converted = type.convertType(orbit);
            Assertions.assertTrue(converted.hasDerivatives());
            CircularOrbit rebuilt = (CircularOrbit) OrbitType.CIRCULAR.convertType(converted);
            Assertions.assertTrue(rebuilt.hasDerivatives());
            Assertions.assertEquals(orbit.getADot(),                             rebuilt.getADot(),                             3.0e-13);
            Assertions.assertEquals(orbit.getCircularExDot(),                    rebuilt.getCircularExDot(),                    1.0e-15);
            Assertions.assertEquals(orbit.getCircularEyDot(),                    rebuilt.getCircularEyDot(),                    1.0e-15);
            Assertions.assertEquals(orbit.getIDot(),                             rebuilt.getIDot(),                             1.0e-15);
            Assertions.assertEquals(orbit.getRightAscensionOfAscendingNodeDot(), rebuilt.getRightAscensionOfAscendingNodeDot(), 1.0e-15);
            Assertions.assertEquals(orbit.getAlphaVDot(),                        rebuilt.getAlphaVDot(),                        1.0e-15);
        }

    }

    @Test
    public void testToString() {
        Vector3D position = new Vector3D(-29536113.0, 30329259.0, -100125.0);
        Vector3D velocity = new Vector3D(-2194.0, -2141.0, -8.0);
        PVCoordinates pvCoordinates = new PVCoordinates(position, velocity);
        CircularOrbit orbit = new CircularOrbit(pvCoordinates, FramesFactory.getEME2000(), date, mu);
        Assertions.assertEquals("circular parameters: {a: 4.225517000282565E7, ex: 0.002082917137146049, ey: 5.173980074371024E-4, i: 0.20189257051515358, raan: -87.91788415673473, alphaV: -137.84099636616548;}",
                            orbit.toString());
    }

    @Test
    void testRemoveRates() {
        // GIVEN
        final Vector3D position = new Vector3D(-29536113.0, 30329259.0, -100125.0);
        final Vector3D velocity = new Vector3D(-2194.0, -2141.0, -8.0);
        final PVCoordinates pvCoordinates = new PVCoordinates(position, velocity);
        final CircularOrbit orbit = new CircularOrbit(pvCoordinates, FramesFactory.getGCRF(), date, mu);
        // WHEN
        final CircularOrbit orbitWithoutRates = orbit.removeRates();
        // THEN
        Assertions.assertFalse(orbitWithoutRates.hasRates());
        Assertions.assertTrue(Double.isNaN(orbitWithoutRates.getADot()));
        Assertions.assertEquals(orbit.getMu(), orbitWithoutRates.getMu());
        Assertions.assertEquals(orbit.getDate(), orbitWithoutRates.getDate());
        Assertions.assertEquals(orbit.getFrame(), orbitWithoutRates.getFrame());
        Assertions.assertEquals(orbit.getA(), orbitWithoutRates.getA());
        Assertions.assertEquals(orbit.getCircularEx(), orbitWithoutRates.getCircularEx());
        Assertions.assertEquals(orbit.getCircularEy(), orbitWithoutRates.getCircularEy());
        Assertions.assertEquals(orbit.getRightAscensionOfAscendingNode(),
                orbitWithoutRates.getRightAscensionOfAscendingNode());
        Assertions.assertEquals(orbit.getI(), orbitWithoutRates.getI());
        Assertions.assertEquals(orbit.getAlphaV(), orbitWithoutRates.getAlphaV());
    }

    @Test
    public void testCopyNonKeplerianAcceleration() {

        final Frame eme2000     = FramesFactory.getEME2000();

        // Define GEO satellite position
        final Vector3D position = new Vector3D(42164140, 0, 0);
        // Build PVCoodrinates starting from its position and computing the corresponding circular velocity
        final PVCoordinates pv  = new PVCoordinates(position,
                                       new Vector3D(0, FastMath.sqrt(mu / position.getNorm()), 0));
        // Build a KeplerianOrbit in eme2000
        final Orbit orbit = new CircularOrbit(pv, eme2000, date, mu);

        // Build another KeplerianOrbit as a copy of the first one
        final Orbit orbitCopy = new CircularOrbit(orbit);

        // Shift the orbit of a time-interval
        final Orbit shiftedOrbit = orbit.shiftedBy(10); // This works good
        final Orbit shiftedOrbitCopy = orbitCopy.shiftedBy(10); // This does not work

        Assertions.assertEquals(0.0,
                            Vector3D.distance(shiftedOrbit.getPosition(),
                                              shiftedOrbitCopy.getPosition()),
                            1.0e-10);
        Assertions.assertEquals(0.0,
                            Vector3D.distance(shiftedOrbit.getPVCoordinates().getVelocity(),
                                              shiftedOrbitCopy.getPVCoordinates().getVelocity()),
                            1.0e-10);

    }

    @Test
    public void testNormalize() {
        CircularOrbit withoutDerivatives =
                        new CircularOrbit(42166712.0, 0.005, -0.025, 1.6,
                                          1.25, 0.4, PositionAngleType.MEAN,
                                          FramesFactory.getEME2000(), date, mu);
        CircularOrbit ref =
                        new CircularOrbit(24000000.0, -0.012, 0.01, 0.2,
                                          -6.28, 6.28, PositionAngleType.MEAN,
                                          FramesFactory.getEME2000(), date, mu);

        CircularOrbit normalized1 = (CircularOrbit) OrbitType.CIRCULAR.normalize(withoutDerivatives, ref);
        Assertions.assertFalse(normalized1.hasDerivatives());
        Assertions.assertEquals(0.0, normalized1.getA()          - withoutDerivatives.getA(),          1.0e-6);
        Assertions.assertEquals(0.0, normalized1.getCircularEx() - withoutDerivatives.getCircularEx(), 1.0e-10);
        Assertions.assertEquals(0.0, normalized1.getCircularEy() - withoutDerivatives.getCircularEy(), 1.0e-10);
        Assertions.assertEquals(0.0, normalized1.getI()          - withoutDerivatives.getI(),          1.0e-10);
        Assertions.assertEquals(-MathUtils.TWO_PI, normalized1.getRightAscensionOfAscendingNode() - withoutDerivatives.getRightAscensionOfAscendingNode(), 1.0e-10);
        Assertions.assertEquals(+MathUtils.TWO_PI, normalized1.getAlphaV() - withoutDerivatives.getAlphaV(), 1.0e-10);
        Assertions.assertTrue(Double.isNaN(normalized1.getADot()));
        Assertions.assertTrue(Double.isNaN(normalized1.getCircularExDot()));
        Assertions.assertTrue(Double.isNaN(normalized1.getCircularEyDot()));
        Assertions.assertTrue(Double.isNaN(normalized1.getIDot()));
        Assertions.assertTrue(Double.isNaN(normalized1.getRightAscensionOfAscendingNodeDot()));
        Assertions.assertTrue(Double.isNaN(normalized1.getAlphaVDot()));

        double[] p = new double[6];
        OrbitType.CIRCULAR.mapOrbitToArray(withoutDerivatives, PositionAngleType.TRUE, p, null);
        CircularOrbit withDerivatives = (CircularOrbit) OrbitType.CIRCULAR.mapArrayToOrbit(p,
                                                                                           new double[] { 1.0, 2.0, 3.0, 4.0, 5.0, 6.0 },
                                                                                           PositionAngleType.TRUE,
                                                                                           withoutDerivatives.getDate(),
                                                                                           withoutDerivatives.getMu(),
                                                                                           withoutDerivatives.getFrame());
        CircularOrbit normalized2 = (CircularOrbit) OrbitType.CIRCULAR.normalize(withDerivatives, ref);
        Assertions.assertTrue(normalized2.hasDerivatives());
        Assertions.assertEquals(0.0, normalized2.getA()          - withDerivatives.getA(),          1.0e-6);
        Assertions.assertEquals(0.0, normalized2.getCircularEx() - withDerivatives.getCircularEx(), 1.0e-10);
        Assertions.assertEquals(0.0, normalized2.getCircularEy() - withDerivatives.getCircularEy(), 1.0e-10);
        Assertions.assertEquals(0.0, normalized2.getI()          - withDerivatives.getI(),          1.0e-10);
        Assertions.assertEquals(-MathUtils.TWO_PI, normalized2.getRightAscensionOfAscendingNode() - withDerivatives.getRightAscensionOfAscendingNode(), 1.0e-10);
        Assertions.assertEquals(+MathUtils.TWO_PI, normalized2.getAlphaV() - withDerivatives.getAlphaV(), 1.0e-10);
        Assertions.assertEquals(0.0, normalized2.getADot()          - withDerivatives.getADot(),          1.0e-10);
        Assertions.assertEquals(0.0, normalized2.getCircularExDot() - withDerivatives.getCircularExDot(), 1.0e-10);
        Assertions.assertEquals(0.0, normalized2.getCircularEyDot() - withDerivatives.getCircularEyDot(), 1.0e-10);
        Assertions.assertEquals(0.0, normalized2.getIDot()          - withDerivatives.getIDot(),          1.0e-10);
        Assertions.assertEquals(0.0, normalized2.getRightAscensionOfAscendingNodeDot() - withDerivatives.getRightAscensionOfAscendingNodeDot(), 1.0e-10);
        Assertions.assertEquals(0.0, normalized2.getAlphaVDot() - withDerivatives.getAlphaVDot(), 1.0e-10);

    }

    @BeforeEach
    public void setUp() {

        Utils.setDataRoot("regular-data");

        // Computation date
        date = AbsoluteDate.J2000_EPOCH;

        // Body mu
        mu = 3.9860047e14;
    }

    @AfterEach
    public void tearDown() {
        date = null;
    }

}
