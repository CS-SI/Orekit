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


public class EquinoctialOrbitTest {

    // Computation date
    private AbsoluteDate date;

    // Body mu
    private double mu;

    @Test
    public void testEquinoctialToEquinoctialEll() {

        double ix = 1.200e-04;
        double iy = -1.16e-04;
        double inc = 2 * FastMath.asin(FastMath.sqrt((ix * ix + iy * iy) / 4.));
        double hx = FastMath.tan(inc / 2.) * ix / (2 * FastMath.sin(inc / 2.));
        double hy = FastMath.tan(inc / 2.) * iy / (2 * FastMath.sin(inc / 2.));

        // elliptic orbit
        EquinoctialOrbit equi =
            new EquinoctialOrbit(42166712.0, 0.5, -0.5, hx, hy,
                                      5.300, PositionAngleType.MEAN,
                                      FramesFactory.getEME2000(), date, mu);
        Vector3D pos = equi.getPosition();
        Vector3D vit = equi.getPVCoordinates().getVelocity();

        PVCoordinates pvCoordinates = new PVCoordinates(pos, vit);

        EquinoctialOrbit param = new EquinoctialOrbit(pvCoordinates, FramesFactory.getEME2000(), date, mu);
        Assertions.assertEquals(param.getA(), equi.getA(), Utils.epsilonTest * equi.getA());
        Assertions.assertEquals(param.getEquinoctialEx(), equi.getEquinoctialEx(),
                     Utils.epsilonE * FastMath.abs(equi.getE()));
        Assertions.assertEquals(param.getEquinoctialEy(), equi.getEquinoctialEy(),
                     Utils.epsilonE * FastMath.abs(equi.getE()));
        Assertions.assertEquals(param.getHx(), equi.getHx(), Utils.epsilonAngle
                     * FastMath.abs(equi.getI()));
        Assertions.assertEquals(param.getHy(), equi.getHy(), Utils.epsilonAngle
                     * FastMath.abs(equi.getI()));
        Assertions.assertEquals(MathUtils.normalizeAngle(param.getLv(), equi.getLv()), equi.getLv(),
                     Utils.epsilonAngle * FastMath.abs(equi.getLv()));

    }

    @Test
    public void testEquinoctialToEquinoctialCirc() {

        double ix = 1.200e-04;
        double iy = -1.16e-04;
        double inc = 2 * FastMath.asin(FastMath.sqrt((ix * ix + iy * iy) / 4.));
        double hx = FastMath.tan(inc / 2.) * ix / (2 * FastMath.sin(inc / 2.));
        double hy = FastMath.tan(inc / 2.) * iy / (2 * FastMath.sin(inc / 2.));

        // circular orbit
        EquinoctialOrbit equiCir =
            new EquinoctialOrbit(42166712.0, 0.1e-10, -0.1e-10, hx, hy,
                                      5.300, PositionAngleType.MEAN,
                                      FramesFactory.getEME2000(), date, mu);
        Vector3D posCir = equiCir.getPosition();
        Vector3D vitCir = equiCir.getPVCoordinates().getVelocity();

        PVCoordinates pvCoordinates = new PVCoordinates(posCir, vitCir);

        EquinoctialOrbit paramCir = new EquinoctialOrbit(pvCoordinates, FramesFactory.getEME2000(),
                                                         date, mu);
        Assertions.assertEquals(paramCir.getA(), equiCir.getA(), Utils.epsilonTest
                     * equiCir.getA());
        Assertions.assertEquals(paramCir.getEquinoctialEx(), equiCir.getEquinoctialEx(),
                     Utils.epsilonEcir * FastMath.abs(equiCir.getE()));
        Assertions.assertEquals(paramCir.getEquinoctialEy(), equiCir.getEquinoctialEy(),
                     Utils.epsilonEcir * FastMath.abs(equiCir.getE()));
        Assertions.assertEquals(paramCir.getHx(), equiCir.getHx(), Utils.epsilonAngle
                     * FastMath.abs(equiCir.getI()));
        Assertions.assertEquals(paramCir.getHy(), equiCir.getHy(), Utils.epsilonAngle
                     * FastMath.abs(equiCir.getI()));
        Assertions.assertEquals(MathUtils.normalizeAngle(paramCir.getLv(), equiCir.getLv()), equiCir
                     .getLv(), Utils.epsilonAngle * FastMath.abs(equiCir.getLv()));

    }

    @Test
    public void testEquinoctialToCartesian() {

        double ix = 1.200e-04;
        double iy = -1.16e-04;
        double inc = 2 * FastMath.asin(FastMath.sqrt((ix * ix + iy * iy) / 4.));
        double hx = FastMath.tan(inc / 2.) * ix / (2 * FastMath.sin(inc / 2.));
        double hy = FastMath.tan(inc / 2.) * iy / (2 * FastMath.sin(inc / 2.));

        EquinoctialOrbit equi =
            new EquinoctialOrbit(42166712.0, -7.900e-06, 1.100e-04, hx, hy,
                                      5.300, PositionAngleType.MEAN,
                                      FramesFactory.getEME2000(), date, mu);
        Vector3D pos = equi.getPosition();
        Vector3D vit = equi.getPVCoordinates().getVelocity();

        // verif of 1/a = 2/X - V2/mu
        double oneovera = (2. / pos.getNorm()) - vit.getNorm() * vit.getNorm() / mu;
        Assertions.assertEquals(oneovera, 1. / equi.getA(), 1.0e-7);

        Assertions.assertEquals( 0.233745668678733e+08, pos.getX(), Utils.epsilonTest * FastMath.abs(pos.getX()));
        Assertions.assertEquals(-0.350998914352669e+08, pos.getY(), Utils.epsilonTest * FastMath.abs(pos.getY()));
        Assertions.assertEquals(-0.150053723123334e+04, pos.getZ(), Utils.epsilonTest * FastMath.abs(pos.getZ()));

        Assertions.assertEquals(2558.7096558809967, vit.getX(), Utils.epsilonTest * FastMath.abs(vit.getX()));
        Assertions.assertEquals(1704.1586039092576, vit.getY(), Utils.epsilonTest * FastMath.abs(vit.getY()));
        Assertions.assertEquals(   0.5013093577879, vit.getZ(), Utils.epsilonTest * FastMath.abs(vit.getZ()));

    }

    @Test
    public void testEquinoctialToKeplerian() {

        double ix = 1.20e-4;
        double iy = -1.16e-4;
        double i = 2 * FastMath.asin(FastMath.sqrt((ix * ix + iy * iy) / 4));
        double hx = FastMath.tan(i / 2) * ix / (2 * FastMath.sin(i / 2));
        double hy = FastMath.tan(i / 2) * iy / (2 * FastMath.sin(i / 2));

        EquinoctialOrbit equi =
            new EquinoctialOrbit(42166712.0, -7.900e-6, 1.100e-4, hx, hy,
                                      5.300, PositionAngleType.MEAN,
                                      FramesFactory.getEME2000(), date, mu);
        KeplerianOrbit kep = new KeplerianOrbit(equi);

        Assertions.assertEquals(42166712.000, equi.getA(), Utils.epsilonTest * kep.getA());
        Assertions.assertEquals(0.110283316961361e-03, kep.getE(), Utils.epsilonE
                     * FastMath.abs(kep.getE()));
        Assertions.assertEquals(0.166901168553917e-03, kep.getI(), Utils.epsilonAngle
                     * FastMath.abs(kep.getI()));
        Assertions.assertEquals(MathUtils.normalizeAngle(-3.87224326008837, kep.getPerigeeArgument()),
                     kep.getPerigeeArgument(), Utils.epsilonTest
                     * FastMath.abs(kep.getPerigeeArgument()));
        Assertions.assertEquals(MathUtils.normalizeAngle(5.51473467358854, kep
                                     .getRightAscensionOfAscendingNode()), kep
                                     .getRightAscensionOfAscendingNode(), Utils.epsilonTest
                                     * FastMath.abs(kep.getRightAscensionOfAscendingNode()));
        Assertions.assertEquals(MathUtils.normalizeAngle(3.65750858649982, kep.getMeanAnomaly()), kep
                     .getMeanAnomaly(), Utils.epsilonTest * FastMath.abs(kep.getMeanAnomaly()));

    }

    @Test
    public void testHyperbolic() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            new EquinoctialOrbit(42166712.0, 0.9, 0.5, 0.01, -0.02, 5.300,
                    PositionAngleType.MEAN,  FramesFactory.getEME2000(), date, mu);
        });
    }

    @Test
    public void testNumericalIssue25() {
        Vector3D position = new Vector3D(3782116.14107698, 416663.11924914, 5875541.62103057);
        Vector3D velocity = new Vector3D(-6349.7848910501, 288.4061811651, 4066.9366759691);
        EquinoctialOrbit orbit = new EquinoctialOrbit(new PVCoordinates(position, velocity),
                                                      FramesFactory.getEME2000(),
                                                      new AbsoluteDate("2004-01-01T23:00:00.000",
                                                                       TimeScalesFactory.getUTC()),
                                                                       3.986004415E14);
        Assertions.assertEquals(0.0, orbit.getE(), 2.0e-14);
    }


    @Test
    public void testAnomaly() {

        // elliptic orbit
        Vector3D position = new Vector3D(7.0e6, 1.0e6, 4.0e6);
        Vector3D velocity = new Vector3D(-500.0, 8000.0, 1000.0);

        EquinoctialOrbit p = new EquinoctialOrbit(new PVCoordinates(position, velocity), FramesFactory.getEME2000(), date, mu);
        KeplerianOrbit kep = new KeplerianOrbit(p);

        double e = p.getE();
        double eRatio = FastMath.sqrt((1 - e) / (1 + e));
        double paPraan = kep.getPerigeeArgument()
        + kep.getRightAscensionOfAscendingNode();

        double lv = 1.1;
        // formulations for elliptic case
        double lE = 2 * FastMath.atan(eRatio * FastMath.tan((lv - paPraan) / 2)) + paPraan;
        double lM = lE - e * FastMath.sin(lE - paPraan);

        p = new EquinoctialOrbit(p.getA(), p.getEquinoctialEx(),
                                 p.getEquinoctialEy() , p.getHx(), p.getHy() , lv , PositionAngleType.TRUE,
                                 p.getFrame(), p.getDate(), p.getMu());
        Assertions.assertEquals(p.getLv(), lv, Utils.epsilonAngle * FastMath.abs(lv));
        Assertions.assertEquals(p.getLE(), lE, Utils.epsilonAngle * FastMath.abs(lE));
        Assertions.assertEquals(p.getLM(), lM, Utils.epsilonAngle * FastMath.abs(lM));
        p = new EquinoctialOrbit(p.getA(), p.getEquinoctialEx(),
                                 p.getEquinoctialEy() , p.getHx(), p.getHy() , 0 , PositionAngleType.TRUE,
                                 p.getFrame(), p.getDate(), p.getMu());

        p = new EquinoctialOrbit(p.getA(), p.getEquinoctialEx(),
                                 p.getEquinoctialEy() , p.getHx(), p.getHy() , lE , PositionAngleType.ECCENTRIC,
                                 p.getFrame(), p.getDate(), p.getMu());
        Assertions.assertEquals(p.getLv(), lv, Utils.epsilonAngle * FastMath.abs(lv));
        Assertions.assertEquals(p.getLE(), lE, Utils.epsilonAngle * FastMath.abs(lE));
        Assertions.assertEquals(p.getLM(), lM, Utils.epsilonAngle * FastMath.abs(lM));
        p = new EquinoctialOrbit(p.getA(), p.getEquinoctialEx(),
                                 p.getEquinoctialEy() , p.getHx(), p.getHy() , 0 , PositionAngleType.TRUE,
                                 p.getFrame(), p.getDate(), p.getMu());

        p = new EquinoctialOrbit(p.getA(), p.getEquinoctialEx(),
                                 p.getEquinoctialEy() , p.getHx(), p.getHy() , lM , PositionAngleType.MEAN,
                                 p.getFrame(), p.getDate(), p.getMu());
        Assertions.assertEquals(p.getLv(), lv, Utils.epsilonAngle * FastMath.abs(lv));
        Assertions.assertEquals(p.getLE(), lE, Utils.epsilonAngle * FastMath.abs(lE));
        Assertions.assertEquals(p.getLM(), lM, Utils.epsilonAngle * FastMath.abs(lM));

        // circular orbit
        p = new EquinoctialOrbit(p.getA(), 0 ,
                                 0, p.getHx(), p.getHy() , p.getLv() , PositionAngleType.TRUE,
                                 p.getFrame(), p.getDate(), p.getMu());

        lE = lv;
        lM = lE;

        p = new EquinoctialOrbit(p.getA(), p.getEquinoctialEx(),
                                 p.getEquinoctialEy() , p.getHx(), p.getHy() , lv , PositionAngleType.TRUE,
                                 p.getFrame(), p.getDate(), p.getMu());
        Assertions.assertEquals(p.getLv(), lv, Utils.epsilonAngle * FastMath.abs(lv));
        Assertions.assertEquals(p.getLE(), lE, Utils.epsilonAngle * FastMath.abs(lE));
        Assertions.assertEquals(p.getLM(), lM, Utils.epsilonAngle * FastMath.abs(lM));
        p = new EquinoctialOrbit(p.getA(), p.getEquinoctialEx(),
                                 p.getEquinoctialEy() , p.getHx(), p.getHy() , 0 , PositionAngleType.TRUE,
                                 p.getFrame(), p.getDate(), p.getMu());

        p = new EquinoctialOrbit(p.getA(), p.getEquinoctialEx(),
                                 p.getEquinoctialEy() , p.getHx(), p.getHy() , lE , PositionAngleType.ECCENTRIC,
                                 p.getFrame(), p.getDate(), p.getMu());
        Assertions.assertEquals(p.getLv(), lv, Utils.epsilonAngle * FastMath.abs(lv));
        Assertions.assertEquals(p.getLE(), lE, Utils.epsilonAngle * FastMath.abs(lE));
        Assertions.assertEquals(p.getLM(), lM, Utils.epsilonAngle * FastMath.abs(lM));
        p = new EquinoctialOrbit(p.getA(), p.getEquinoctialEx(),
                                 p.getEquinoctialEy() , p.getHx(), p.getHy() , 0 , PositionAngleType.TRUE,
                                 p.getFrame(), p.getDate(), p.getMu());

        p = new EquinoctialOrbit(p.getA(), p.getEquinoctialEx(),
                                 p.getEquinoctialEy() , p.getHx(), p.getHy() , lM , PositionAngleType.MEAN, p.getFrame(), p.getDate(), p.getMu());
        Assertions.assertEquals(p.getLv(), lv, Utils.epsilonAngle * FastMath.abs(lv));
        Assertions.assertEquals(p.getLE(), lE, Utils.epsilonAngle * FastMath.abs(lE));
        Assertions.assertEquals(p.getLM(), lM, Utils.epsilonAngle * FastMath.abs(lM));
    }

    @Test
    public void testPositionVelocityNorms() {

        // elliptic and non equatorial (i retrograde) orbit
        EquinoctialOrbit p =
            new EquinoctialOrbit(42166712.0, 0.5, -0.5, 1.200, 2.1,
                                 0.67, PositionAngleType.TRUE,
                                 FramesFactory.getEME2000(), date, mu);

        double ex = p.getEquinoctialEx();
        double ey = p.getEquinoctialEy();
        double lv = p.getLv();
        double ksi = 1 + ex * FastMath.cos(lv) + ey * FastMath.sin(lv);
        double nu = ex * FastMath.sin(lv) - ey * FastMath.cos(lv);
        double epsilon = FastMath.sqrt(1 - ex * ex - ey * ey);

        double a = p.getA();
        double na = FastMath.sqrt(p.getMu() / a);

        Assertions.assertEquals(a * epsilon * epsilon / ksi, p.getPosition().getNorm(),
                     Utils.epsilonTest * FastMath.abs(p.getPosition().getNorm()));
        Assertions.assertEquals(na * FastMath.sqrt(ksi * ksi + nu * nu) / epsilon, p
                     .getPVCoordinates().getVelocity().getNorm(), Utils.epsilonTest
                     * FastMath.abs(p.getPVCoordinates().getVelocity().getNorm()));

        // circular and equatorial orbit
        EquinoctialOrbit pCirEqua =
            new EquinoctialOrbit(42166712.0, 0.1e-8, 0.1e-8, 0.1e-8, 0.1e-8,
                                 0.67, PositionAngleType.TRUE,
                                 FramesFactory.getEME2000(), date, mu);

        ex = pCirEqua.getEquinoctialEx();
        ey = pCirEqua.getEquinoctialEy();
        lv = pCirEqua.getLv();
        ksi = 1 + ex * FastMath.cos(lv) + ey * FastMath.sin(lv);
        nu = ex * FastMath.sin(lv) - ey * FastMath.cos(lv);
        epsilon = FastMath.sqrt(1 - ex * ex - ey * ey);

        a = pCirEqua.getA();
        na = FastMath.sqrt(pCirEqua.getMu() / a);

        Assertions.assertEquals(a * epsilon * epsilon / ksi, pCirEqua.getPosition()
                     .getNorm(), Utils.epsilonTest
                     * FastMath.abs(pCirEqua.getPosition().getNorm()));
        Assertions.assertEquals(na * FastMath.sqrt(ksi * ksi + nu * nu) / epsilon, pCirEqua
                     .getPVCoordinates().getVelocity().getNorm(), Utils.epsilonTest
                     * FastMath.abs(pCirEqua.getPVCoordinates().getVelocity().getNorm()));
    }

    @Test
    public void testGeometry() {

        // elliptic and non equatorial (i retrograde) orbit
        EquinoctialOrbit p =
            new EquinoctialOrbit(42166712.0, 0.5, -0.5, 1.200, 2.1,
                                 0.67, PositionAngleType.TRUE,
                                 FramesFactory.getEME2000(), date, mu);

        Vector3D position = p.getPosition();
        Vector3D velocity = p.getPVCoordinates().getVelocity();
        Vector3D momentum = p.getPVCoordinates().getMomentum().normalize();

        double apogeeRadius = p.getA() * (1 + p.getE());
        double perigeeRadius = p.getA() * (1 - p.getE());

        for (double lv = 0; lv <= 2 * FastMath.PI; lv += 2 * FastMath.PI / 100.) {
            p = new EquinoctialOrbit(p.getA(), p.getEquinoctialEx(),
                                     p.getEquinoctialEy() , p.getHx(), p.getHy() , lv , PositionAngleType.TRUE,
                                     p.getFrame(), p.getDate(), p.getMu());
            position = p.getPosition();

            // test if the norm of the position is in the range [perigee radius,
            // apogee radius]
            // Warning: these tests are without absolute value by choice
            Assertions.assertTrue((position.getNorm() - apogeeRadius) <= (apogeeRadius * Utils.epsilonTest));
            Assertions.assertTrue((position.getNorm() - perigeeRadius) >= (-perigeeRadius * Utils.epsilonTest));

            position = position.normalize();
            velocity = p.getPVCoordinates().getVelocity();
            velocity = velocity.normalize();

            // at this stage of computation, all the vectors (position, velocity and
            // momemtum) are normalized here

            // test of orthogonality between position and momentum
            Assertions.assertTrue(FastMath.abs(Vector3D.dotProduct(position, momentum)) < Utils.epsilonTest);
            // test of orthogonality between velocity and momentum
            Assertions.assertTrue(FastMath.abs(Vector3D.dotProduct(velocity, momentum)) < Utils.epsilonTest);
        }

        // circular and equatorial orbit
        EquinoctialOrbit pCirEqua =
            new EquinoctialOrbit(42166712.0, 0.1e-8, 0.1e-8, 0.1e-8, 0.1e-8,
                                 0.67, PositionAngleType.TRUE,
                                 FramesFactory.getEME2000(), date, mu);

        position = pCirEqua.getPosition();
        velocity = pCirEqua.getPVCoordinates().getVelocity();

        momentum = Vector3D.crossProduct(position, velocity).normalize();

        apogeeRadius = pCirEqua.getA() * (1 + pCirEqua.getE());
        perigeeRadius = pCirEqua.getA() * (1 - pCirEqua.getE());
        // test if apogee equals perigee
        Assertions.assertEquals(perigeeRadius, apogeeRadius, 1.e+4 * Utils.epsilonTest
                     * apogeeRadius);

        for (double lv = 0; lv <= 2 * FastMath.PI; lv += 2 * FastMath.PI / 100.) {
            pCirEqua = new EquinoctialOrbit(pCirEqua.getA(), pCirEqua.getEquinoctialEx(),
                                            pCirEqua.getEquinoctialEy() , pCirEqua.getHx(), pCirEqua.getHy() , lv , PositionAngleType.TRUE,
                                            pCirEqua.getFrame(), p.getDate(), p.getMu());
            position = pCirEqua.getPosition();

            // test if the norm pf the position is in the range [perigee radius,
            // apogee radius]
            Assertions.assertTrue((position.getNorm() - apogeeRadius) <= (apogeeRadius * Utils.epsilonTest));
            Assertions.assertTrue((position.getNorm() - perigeeRadius) >= (-perigeeRadius * Utils.epsilonTest));

            position = position.normalize();
            velocity = pCirEqua.getPVCoordinates().getVelocity();
            velocity = velocity.normalize();

            // at this stage of computation, all the vectors (position, velocity and
            // momemtum) are normalized here

            // test of orthogonality between position and momentum
            Assertions.assertTrue(FastMath.abs(Vector3D.dotProduct(position, momentum)) < Utils.epsilonTest);
            // test of orthogonality between velocity and momentum
            Assertions.assertTrue(FastMath.abs(Vector3D.dotProduct(velocity, momentum)) < Utils.epsilonTest);
        }
    }


    @Test
    public void testRadiusOfCurvature() {

        // elliptic and non equatorial (i retrograde) orbit
        EquinoctialOrbit p =
            new EquinoctialOrbit(42166712.0, 0.5, -0.5, 1.200, 2.1,
                                 0.67, PositionAngleType.TRUE,
                                 FramesFactory.getEME2000(), date, mu);

        // arbitrary orthogonal vectors in the orbital plane
        Vector3D u = p.getPVCoordinates().getMomentum().orthogonal();
        Vector3D v = Vector3D.crossProduct(p.getPVCoordinates().getMomentum(), u).normalize();

        // compute radius of curvature in the orbital plane from Cartesian coordinates
        double xDot    = Vector3D.dotProduct(p.getPVCoordinates().getVelocity(),     u);
        double yDot    = Vector3D.dotProduct(p.getPVCoordinates().getVelocity(),     v);
        double xDotDot = Vector3D.dotProduct(p.getPVCoordinates().getAcceleration(), u);
        double yDotDot = Vector3D.dotProduct(p.getPVCoordinates().getAcceleration(), v);
        double dot2    = xDot * xDot + yDot * yDot;
        double rCart   = dot2 * FastMath.sqrt(dot2) /
                         FastMath.abs(xDot * yDotDot - yDot * xDotDot);

        // compute radius of curvature in the orbital plane from orbital parameters
        double ex   = p.getEquinoctialEx();
        double ey   = p.getEquinoctialEy();
        double f    = ex * FastMath.cos(p.getLE()) + ey * FastMath.sin(p.getLE());
        double oMf2 = 1 - f * f;
        double rOrb = p.getA() * oMf2 * FastMath.sqrt(oMf2 / (1 - (ex * ex + ey * ey)));

        // both methods to compute radius of curvature should match
        Assertions.assertEquals(rCart, rOrb, 1.0e-15 * p.getA());

        // at this place for such an eccentric orbit,
        // the radius of curvature is much smaller than semi major axis
        Assertions.assertEquals(0.8477 * p.getA(), rCart, 1.0e-4 * p.getA());

    }

    @Test
    public void testSymmetry() {

        // elliptic and non equatorial orbit
        Vector3D position = new Vector3D(4512.9, 18260., -5127.);
        Vector3D velocity = new Vector3D(134664.6, 90066.8, 72047.6);

        EquinoctialOrbit p = new EquinoctialOrbit(new PVCoordinates(position, velocity),
                                                  FramesFactory.getEME2000(), date, mu);

        Vector3D positionOffset = p.getPosition().subtract(position);
        Vector3D velocityOffset = p.getPVCoordinates().getVelocity().subtract(velocity);

        Assertions.assertEquals(0, positionOffset.getNorm(), 7.5e-12);
        Assertions.assertEquals(0, velocityOffset.getNorm(), 1.0e-15);

        // circular and equatorial orbit
        position = new Vector3D(33051.2, 26184.9, -1.3E-5);
        velocity = new Vector3D(-60376.2, 76208., 2.7E-4);

        p = new EquinoctialOrbit(new PVCoordinates(position, velocity),
                                 FramesFactory.getEME2000(), date, mu);

        positionOffset = p.getPosition().subtract(position);
        velocityOffset = p.getPVCoordinates().getVelocity().subtract(velocity);

        Assertions.assertEquals(0, positionOffset.getNorm(), 1.1e-11);
        Assertions.assertEquals(0, velocityOffset.getNorm(), 1.0e-15);
    }

    @Test
    public void testNonInertialFrame() throws IllegalArgumentException {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Vector3D position = new Vector3D(4512.9, 18260., -5127.);
            Vector3D velocity = new Vector3D(134664.6, 90066.8, 72047.6);
            PVCoordinates pvCoordinates = new PVCoordinates( position, velocity);
            new EquinoctialOrbit(pvCoordinates,
                    new Frame(FramesFactory.getEME2000(), Transform.IDENTITY, "non-inertial", false),
                    date, mu);
        });
    }

    @Test
    public void testJacobianReference() {

        AbsoluteDate dateTca = new AbsoluteDate(2000, 04, 01, 0, 0, 0.000, TimeScalesFactory.getUTC());
        double mu =  3.986004415e+14;
        EquinoctialOrbit orbEqu = new EquinoctialOrbit(7000000.0, 0.01, -0.02, 1.2, 2.1,
                                          FastMath.toRadians(40.), PositionAngleType.MEAN,
                                          FramesFactory.getEME2000(), dateTca, mu);

        // the following reference values have been computed using the free software
        // version 6.2 of the MSLIB fortran library by the following program:
        //         program equ_jacobian
        //
        //         use mslib
        //         implicit none
        //
        //         integer, parameter :: nb = 11
        //         integer :: i,j
        //         type(tm_code_retour)      ::  code_retour
        //
        //         real(pm_reel), parameter :: mu= 3.986004415e+14_pm_reel
        //         real(pm_reel),dimension(3)::vit_car,pos_car
        //         type(tm_orb_cir_equa)::cir_equa
        //         real(pm_reel), dimension(6,6)::jacob
        //         real(pm_reel)::norme,hx,hy,f,dix,diy
        //         intrinsic sqrt
        //
        //         cir_equa%a=7000000_pm_reel
        //         cir_equa%ex=0.01_pm_reel
        //         cir_equa%ey=-0.02_pm_reel
        //
        //         ! mslib cir-equ parameters use ix = 2 sin(i/2) cos(gom) and iy = 2 sin(i/2) sin(gom)
        //         ! equinoctial parameters use hx = tan(i/2) cos(gom) and hy = tan(i/2) sin(gom)
        //         ! the conversions between these parameters and their differentials can be computed
        //         ! from the ratio f = 2cos(i/2) which can be found either from (ix, iy) or (hx, hy):
        //         !   f = sqrt(4 - ix^2 - iy^2) =  2 / sqrt(1 + hx^2 + hy^2)
        //         !  hx = ix / f,  hy = iy / f
        //         !  ix = hx * f, iy = hy *f
        //         ! dhx = ((1 + hx^2) / f) dix + (hx hy / f) diy, dhy = (hx hy / f) dix + ((1 + hy^2) /f) diy
        //         ! dix = ((1 - ix^2 / 4) f dhx - (ix iy / 4) f dhy, diy = -(ix iy / 4) f dhx + (1 - iy^2 / 4) f dhy
        //         hx=1.2_pm_reel
        //         hy=2.1_pm_reel
        //         f=2_pm_reel/sqrt(1+hx*hx+hy*hy)
        //         cir_equa%ix=hx*f
        //         cir_equa%iy=hy*f
        //
        //         cir_equa%pso_M=40_pm_reel*pm_deg_rad
        //
        //         call mv_cir_equa_car(mu,cir_equa,pos_car,vit_car,code_retour)
        //         write(*,*)code_retour%valeur
        //         write(*,1000)pos_car,vit_car
        //
        //
        //         call mu_norme(pos_car,norme,code_retour)
        //         write(*,*)norme
        //
        //         call mv_car_cir_equa (mu, pos_car, vit_car, cir_equa, code_retour, jacob)
        //         write(*,*)code_retour%valeur
        //
        //         f=sqrt(4_pm_reel-cir_equa%ix*cir_equa%ix-cir_equa%iy*cir_equa%iy)
        //         hx=cir_equa%ix/f
        //         hy=cir_equa%iy/f
        //         write(*,*)"ix = ", cir_equa%ix, ", iy = ", cir_equa%iy
        //         write(*,*)"equinoctial = ", cir_equa%a, cir_equa%ex, cir_equa%ey, hx, hy, cir_equa%pso_M*pm_rad_deg
        //
        //         do j = 1,6
        //           dix=jacob(4,j)
        //           diy=jacob(5,j)
        //           jacob(4,j)=((1_pm_reel+hx*hx)*dix+(hx*hy)*diy)/f
        //           jacob(5,j)=((hx*hy)*dix+(1_pm_reel+hy*hy)*diy)/f
        //         end do
        //
        //         do i = 1,6
        //            write(*,*) " ",(jacob(i,j),j=1,6)
        //         end do
        //
        //         1000 format (6(f24.15,1x))
        //         end program equ_jacobian
        Vector3D pRef = new Vector3D(2004367.298657628707588, 6575317.978060320019722, -1518024.843913963763043);
        Vector3D vRef = new Vector3D(5574.048661495634406, -368.839015744295409, 5009.529487849066754);
        double[][] jRef = {
            {  0.56305379787310628,        1.8470954710993663,      -0.42643364527246025,        1370.4369387322224,       -90.682848736736688 ,       1231.6441195141242      },
            {  9.52434720041122055E-008,  9.49704503778007296E-008,  4.46607520107935678E-008,  1.69704446323098610E-004,  7.05603505855828105E-005,  1.14825140460141970E-004 },
            { -5.41784097802642701E-008,  9.54903765833015538E-008, -8.95815777332234450E-008,  1.01864980963344096E-004, -1.03194262242761416E-004,  1.40668700715197768E-004 },
            {  1.96680305426455816E-007, -1.12388745957974467E-007, -2.27118924123407353E-007,  2.06472886488132167E-004, -1.17984506564646906E-004, -2.38427023682723818E-004 },
            { -2.24382495052235118E-007,  1.28218568601277626E-007,  2.59108357381747656E-007,  1.89034327703662092E-004, -1.08019615830663994E-004, -2.18289640324466583E-004 },
            { -3.04001022071876804E-007,  1.22214683774559989E-007,  1.35141804810132761E-007, -1.34034616931480536E-004, -2.14283975204169379E-004,  1.29018773893081404E-004 }
        };

        PVCoordinates pv = orbEqu.getPVCoordinates();
        Assertions.assertEquals(0, pv.getPosition().subtract(pRef).getNorm(), 2.0e-16 * pRef.getNorm());
        Assertions.assertEquals(0, pv.getVelocity().subtract(vRef).getNorm(), 2.0e-16 * vRef.getNorm());

        double[][] jacobian = new double[6][6];
        orbEqu.getJacobianWrtCartesian(PositionAngleType.MEAN, jacobian);

        for (int i = 0; i < jacobian.length; i++) {
            double[] row    = jacobian[i];
            double[] rowRef = jRef[i];
            for (int j = 0; j < row.length; j++) {
                Assertions.assertEquals(0, (row[j] - rowRef[j]) / rowRef[j], 4.0e-15);
            }
        }

    }

    @Test
    public void testJacobianFinitedifferences() {

        AbsoluteDate dateTca = new AbsoluteDate(2000, 04, 01, 0, 0, 0.000, TimeScalesFactory.getUTC());
        double mu =  3.986004415e+14;
        EquinoctialOrbit orbEqu = new EquinoctialOrbit(7000000.0, 0.01, -0.02, 1.2, 2.1,
                                                       FastMath.toRadians(40.), PositionAngleType.MEAN,
                                                       FramesFactory.getEME2000(), dateTca, mu);

        for (PositionAngleType type : PositionAngleType.values()) {
            double hP = 2.0;
            double[][] finiteDiffJacobian = finiteDifferencesJacobian(type, orbEqu, hP);
            double[][] jacobian = new double[6][6];
            orbEqu.getJacobianWrtCartesian(type, jacobian);

            for (int i = 0; i < jacobian.length; i++) {
                double[] row    = jacobian[i];
                double[] rowRef = finiteDiffJacobian[i];
                for (int j = 0; j < row.length; j++) {
                    Assertions.assertEquals(0, (row[j] - rowRef[j]) / rowRef[j], 4.0e-9);
                }
            }

            double[][] invJacobian = new double[6][6];
            orbEqu.getJacobianWrtParameters(type, invJacobian);
            MatrixUtils.createRealMatrix(jacobian).
                            multiply(MatrixUtils.createRealMatrix(invJacobian)).
            walkInRowOrder(new RealMatrixPreservingVisitor() {
                public void start(int rows, int columns,
                                  int startRow, int endRow, int startColumn, int endColumn) {
                }

                public void visit(int row, int column, double value) {
                    Assertions.assertEquals(row == column ? 1.0 : 0.0, value, 7.0e-9);
                }

                public double end() {
                    return Double.NaN;
                }
            });

        }

    }

    private double[][] finiteDifferencesJacobian(PositionAngleType type, EquinoctialOrbit orbit, double hP)
        {
        double[][] jacobian = new double[6][6];
        for (int i = 0; i < 6; ++i) {
            fillColumn(type, i, orbit, hP, jacobian);
        }
        return jacobian;
    }

    private void fillColumn(PositionAngleType type, int i, EquinoctialOrbit orbit, double hP, double[][] jacobian) {

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

        EquinoctialOrbit oM4h = new EquinoctialOrbit(new PVCoordinates(new Vector3D(1, p, -4, dP), new Vector3D(1, v, -4, dV)),
                                                     orbit.getFrame(), orbit.getDate(), orbit.getMu());
        EquinoctialOrbit oM3h = new EquinoctialOrbit(new PVCoordinates(new Vector3D(1, p, -3, dP), new Vector3D(1, v, -3, dV)),
                                                     orbit.getFrame(), orbit.getDate(), orbit.getMu());
        EquinoctialOrbit oM2h = new EquinoctialOrbit(new PVCoordinates(new Vector3D(1, p, -2, dP), new Vector3D(1, v, -2, dV)),
                                                     orbit.getFrame(), orbit.getDate(), orbit.getMu());
        EquinoctialOrbit oM1h = new EquinoctialOrbit(new PVCoordinates(new Vector3D(1, p, -1, dP), new Vector3D(1, v, -1, dV)),
                                                     orbit.getFrame(), orbit.getDate(), orbit.getMu());
        EquinoctialOrbit oP1h = new EquinoctialOrbit(new PVCoordinates(new Vector3D(1, p, +1, dP), new Vector3D(1, v, +1, dV)),
                                                     orbit.getFrame(), orbit.getDate(), orbit.getMu());
        EquinoctialOrbit oP2h = new EquinoctialOrbit(new PVCoordinates(new Vector3D(1, p, +2, dP), new Vector3D(1, v, +2, dV)),
                                                     orbit.getFrame(), orbit.getDate(), orbit.getMu());
        EquinoctialOrbit oP3h = new EquinoctialOrbit(new PVCoordinates(new Vector3D(1, p, +3, dP), new Vector3D(1, v, +3, dV)),
                                                     orbit.getFrame(), orbit.getDate(), orbit.getMu());
        EquinoctialOrbit oP4h = new EquinoctialOrbit(new PVCoordinates(new Vector3D(1, p, +4, dP), new Vector3D(1, v, +4, dV)),
                                                     orbit.getFrame(), orbit.getDate(), orbit.getMu());

        jacobian[0][i] = (-3 * (oP4h.getA()             - oM4h.getA()) +
                          32 * (oP3h.getA()             - oM3h.getA()) -
                         168 * (oP2h.getA()             - oM2h.getA()) +
                         672 * (oP1h.getA()             - oM1h.getA())) / (840 * h);
        jacobian[1][i] = (-3 * (oP4h.getEquinoctialEx() - oM4h.getEquinoctialEx()) +
                          32 * (oP3h.getEquinoctialEx() - oM3h.getEquinoctialEx()) -
                         168 * (oP2h.getEquinoctialEx() - oM2h.getEquinoctialEx()) +
                         672 * (oP1h.getEquinoctialEx() - oM1h.getEquinoctialEx())) / (840 * h);
        jacobian[2][i] = (-3 * (oP4h.getEquinoctialEy() - oM4h.getEquinoctialEy()) +
                          32 * (oP3h.getEquinoctialEy() - oM3h.getEquinoctialEy()) -
                         168 * (oP2h.getEquinoctialEy() - oM2h.getEquinoctialEy()) +
                         672 * (oP1h.getEquinoctialEy() - oM1h.getEquinoctialEy())) / (840 * h);
        jacobian[3][i] = (-3 * (oP4h.getHx()            - oM4h.getHx()) +
                          32 * (oP3h.getHx()            - oM3h.getHx()) -
                         168 * (oP2h.getHx()            - oM2h.getHx()) +
                         672 * (oP1h.getHx()            - oM1h.getHx())) / (840 * h);
        jacobian[4][i] = (-3 * (oP4h.getHy()            - oM4h.getHy()) +
                          32 * (oP3h.getHy()            - oM3h.getHy()) -
                         168 * (oP2h.getHy()            - oM2h.getHy()) +
                         672 * (oP1h.getHy()            - oM1h.getHy())) / (840 * h);
        jacobian[5][i] = (-3 * (oP4h.getL(type)         - oM4h.getL(type)) +
                          32 * (oP3h.getL(type)         - oM3h.getL(type)) -
                         168 * (oP2h.getL(type)         - oM2h.getL(type)) +
                         672 * (oP1h.getL(type)         - oM1h.getL(type))) / (840 * h);

    }

    @Test
    public void testSerialization()
      throws IOException, ClassNotFoundException {
        Vector3D position = new Vector3D(-29536113.0, 30329259.0, -100125.0);
        Vector3D velocity = new Vector3D(-2194.0, -2141.0, -8.0);
        PVCoordinates pvCoordinates = new PVCoordinates( position, velocity);
        EquinoctialOrbit orbit = new EquinoctialOrbit(pvCoordinates, FramesFactory.getEME2000(), date, mu);
        Assertions.assertEquals(42255170.003, orbit.getA(), 1.0e-3);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream    oos = new ObjectOutputStream(bos);
        oos.writeObject(orbit);

        Assertions.assertTrue(bos.size() > 280);
        Assertions.assertTrue(bos.size() < 330);

        ByteArrayInputStream  bis = new ByteArrayInputStream(bos.toByteArray());
        ObjectInputStream     ois = new ObjectInputStream(bis);
        EquinoctialOrbit deserialized  = (EquinoctialOrbit) ois.readObject();
        Assertions.assertEquals(orbit.getA(), deserialized.getA(), 1.0e-10);
        Assertions.assertEquals(orbit.getEquinoctialEx(), deserialized.getEquinoctialEx(), 1.0e-10);
        Assertions.assertEquals(orbit.getEquinoctialEy(), deserialized.getEquinoctialEy(), 1.0e-10);
        Assertions.assertEquals(orbit.getHx(), deserialized.getHx(), 1.0e-10);
        Assertions.assertEquals(orbit.getHy(), deserialized.getHy(), 1.0e-10);
        Assertions.assertEquals(orbit.getLv(), deserialized.getLv(), 1.0e-10);
        Assertions.assertTrue(Double.isNaN(orbit.getADot()) && Double.isNaN(deserialized.getADot()));
        Assertions.assertTrue(Double.isNaN(orbit.getEquinoctialExDot()) && Double.isNaN(deserialized.getEquinoctialExDot()));
        Assertions.assertTrue(Double.isNaN(orbit.getEquinoctialEyDot()) && Double.isNaN(deserialized.getEquinoctialEyDot()));
        Assertions.assertTrue(Double.isNaN(orbit.getHxDot()) && Double.isNaN(deserialized.getHxDot()));
        Assertions.assertTrue(Double.isNaN(orbit.getHyDot()) && Double.isNaN(deserialized.getHyDot()));
        Assertions.assertTrue(Double.isNaN(orbit.getLvDot()) && Double.isNaN(deserialized.getLvDot()));
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
        PVCoordinates pvCoordinates = new PVCoordinates(position, velocity, acceleration);
        EquinoctialOrbit orbit = new EquinoctialOrbit(pvCoordinates, FramesFactory.getEME2000(), date, mu);
        Assertions.assertEquals(42255170.003, orbit.getA(), 1.0e-3);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream    oos = new ObjectOutputStream(bos);
        oos.writeObject(orbit);

        Assertions.assertTrue(bos.size() > 330);
        Assertions.assertTrue(bos.size() < 380);

        ByteArrayInputStream  bis = new ByteArrayInputStream(bos.toByteArray());
        ObjectInputStream     ois = new ObjectInputStream(bis);
        EquinoctialOrbit deserialized  = (EquinoctialOrbit) ois.readObject();
        Assertions.assertEquals(orbit.getA(), deserialized.getA(), 1.0e-10);
        Assertions.assertEquals(orbit.getEquinoctialEx(), deserialized.getEquinoctialEx(), 1.0e-10);
        Assertions.assertEquals(orbit.getEquinoctialEy(), deserialized.getEquinoctialEy(), 1.0e-10);
        Assertions.assertEquals(orbit.getHx(), deserialized.getHx(), 1.0e-10);
        Assertions.assertEquals(orbit.getHy(), deserialized.getHy(), 1.0e-10);
        Assertions.assertEquals(orbit.getLv(), deserialized.getLv(), 1.0e-10);
        Assertions.assertEquals(orbit.getADot(), deserialized.getADot(), 1.0e-10);
        Assertions.assertEquals(orbit.getEquinoctialExDot(), deserialized.getEquinoctialExDot(), 1.0e-10);
        Assertions.assertEquals(orbit.getEquinoctialEyDot(), deserialized.getEquinoctialEyDot(), 1.0e-10);
        Assertions.assertEquals(orbit.getHxDot(), deserialized.getHxDot(), 1.0e-10);
        Assertions.assertEquals(orbit.getHyDot(), deserialized.getHyDot(), 1.0e-10);
        Assertions.assertEquals(orbit.getLvDot(), deserialized.getLvDot(), 1.0e-10);
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
        final EquinoctialOrbit orbit = new EquinoctialOrbit(pv, frame, mu);

        Assertions.assertEquals(differentiate(pv, frame, mu, shifted -> shifted.getA()),
                            orbit.getADot(),
                            4.3e-8);
        Assertions.assertEquals(differentiate(pv, frame, mu, shifted -> shifted.getEquinoctialEx()),
                            orbit.getEquinoctialExDot(),
                            2.1e-15);
        Assertions.assertEquals(differentiate(pv, frame, mu, shifted -> shifted.getEquinoctialEy()),
                            orbit.getEquinoctialEyDot(),
                            5.3e-16);
        Assertions.assertEquals(differentiate(pv, frame, mu, shifted -> shifted.getHx()),
                            orbit.getHxDot(),
                            4.4e-15);
        Assertions.assertEquals(differentiate(pv, frame, mu, shifted -> shifted.getHy()),
                            orbit.getHyDot(),
                            1.5e-15);
        Assertions.assertEquals(differentiate(pv, frame, mu, shifted -> shifted.getLv()),
                            orbit.getLvDot(),
                            1.2e-15);
        Assertions.assertEquals(differentiate(pv, frame, mu, shifted -> shifted.getLE()),
                            orbit.getLEDot(),
                            7.7e-16);
        Assertions.assertEquals(differentiate(pv, frame, mu, shifted -> shifted.getLM()),
                            orbit.getLMDot(),
                            8.8e-16);
        Assertions.assertEquals(differentiate(pv, frame, mu, shifted -> shifted.getE()),
                            orbit.getEDot(),
                            6.9e-16);
        Assertions.assertEquals(differentiate(pv, frame, mu, shifted -> shifted.getI()),
                            orbit.getIDot(),
                            3.5e-15);
        Assertions.assertEquals(differentiate(pv, frame, mu, shifted -> shifted.getL(PositionAngleType.TRUE)),
                            orbit.getLDot(PositionAngleType.TRUE),
                            1.2e-15);
        Assertions.assertEquals(differentiate(pv, frame, mu, shifted -> shifted.getL(PositionAngleType.ECCENTRIC)),
                            orbit.getLDot(PositionAngleType.ECCENTRIC),
                            7.7e-16);
        Assertions.assertEquals(differentiate(pv, frame, mu, shifted -> shifted.getL(PositionAngleType.MEAN)),
                            orbit.getLDot(PositionAngleType.MEAN),
                            8.8e-16);

    }

    private <S extends Function<EquinoctialOrbit, Double>>
    double differentiate(TimeStampedPVCoordinates pv, Frame frame, double mu, S picker) {
        final DSFactory factory = new DSFactory(1, 1);
        FiniteDifferencesDifferentiator differentiator = new FiniteDifferencesDifferentiator(8, 0.1);
        UnivariateDifferentiableFunction diff = differentiator.differentiate(new UnivariateFunction() {
            public double value(double dt) {
                return picker.apply(new EquinoctialOrbit(pv.shiftedBy(dt), frame, mu));
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
        final EquinoctialOrbit orbit = new EquinoctialOrbit(pv, frame, mu);

        for (PositionAngleType type : PositionAngleType.values()) {
            final EquinoctialOrbit rebuilt = new EquinoctialOrbit(orbit.getA(),
                                                            orbit.getEquinoctialEx(),
                                                            orbit.getEquinoctialEy(),
                                                            orbit.getHx(),
                                                            orbit.getHy(),
                                                            orbit.getL(type),
                                                            orbit.getADot(),
                                                            orbit.getEquinoctialExDot(),
                                                            orbit.getEquinoctialEyDot(),
                                                            orbit.getHxDot(),
                                                            orbit.getHyDot(),
                                                            orbit.getLDot(type),
                                                            type, orbit.getFrame(), orbit.getDate(), orbit.getMu());
            MatcherAssert.assertThat(rebuilt.getA(),                                relativelyCloseTo(orbit.getA(),                1));
            MatcherAssert.assertThat(rebuilt.getEquinoctialEx(),                    relativelyCloseTo(orbit.getEquinoctialEx(),    1));
            MatcherAssert.assertThat(rebuilt.getEquinoctialEy(),                    relativelyCloseTo(orbit.getEquinoctialEy(),    1));
            MatcherAssert.assertThat(rebuilt.getHx(),                               relativelyCloseTo(orbit.getHx(),               1));
            MatcherAssert.assertThat(rebuilt.getHy(),                               relativelyCloseTo(orbit.getHy(),               1));
            MatcherAssert.assertThat(rebuilt.getADot(),                             relativelyCloseTo(orbit.getADot(),             1));
            MatcherAssert.assertThat(rebuilt.getEquinoctialExDot(),                 relativelyCloseTo(orbit.getEquinoctialExDot(), 1));
            MatcherAssert.assertThat(rebuilt.getEquinoctialEyDot(),                 relativelyCloseTo(orbit.getEquinoctialEyDot(), 1));
            MatcherAssert.assertThat(rebuilt.getHxDot(),                            relativelyCloseTo(orbit.getHxDot(),            1));
            MatcherAssert.assertThat(rebuilt.getHyDot(),                            relativelyCloseTo(orbit.getHyDot(),            1));
            for (PositionAngleType type2 : PositionAngleType.values()) {
                MatcherAssert.assertThat(rebuilt.getL(type2),    relativelyCloseTo(orbit.getL(type2),    1));
                MatcherAssert.assertThat(rebuilt.getLDot(type2), relativelyCloseTo(orbit.getLDot(type2), 1));
            }
        }

    }

    @Test
    public void testEquatorialRetrograde() {
        Vector3D position = new Vector3D(10000000.0, 0.0, 0.0);
        Vector3D velocity = new Vector3D(0.0, -6500.0, 0.0);
        double r2 = position.getNormSq();
        double r  = FastMath.sqrt(r2);
        Vector3D acceleration = new Vector3D(-mu / (r * r2), position,
                                             1, new Vector3D(-0.1, 0.2, 0.3));
        PVCoordinates pvCoordinates = new PVCoordinates(position, velocity, acceleration);
        // we use an intermediate Keplerian orbit so eccentricity can be computed
        // when using directly PV, eccentricity ends up in NaN, due to the way computation is organized
        // this is not really considered a problem as anyway retrograde equatorial cannot be fully supported
        EquinoctialOrbit orbit = new EquinoctialOrbit(new KeplerianOrbit(pvCoordinates, FramesFactory.getEME2000(), date, mu));
        Assertions.assertEquals(10637829.465, orbit.getA(), 1.0e-3);
        Assertions.assertEquals(-738.145, orbit.getADot(), 1.0e-3);
        Assertions.assertEquals(0.05995861, orbit.getE(), 1.0e-8);
        Assertions.assertEquals(-6.523e-5, orbit.getEDot(), 1.0e-8);
        Assertions.assertTrue(Double.isNaN(orbit.getI()));
        Assertions.assertTrue(Double.isNaN(orbit.getIDot()));
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
        EquinoctialOrbit orbit = new EquinoctialOrbit(pvCoordinates, FramesFactory.getEME2000(),
                                                      date, Constants.EIGEN5C_EARTH_MU);
        Assertions.assertTrue(orbit.hasDerivatives());
        double r2 = position.getNormSq();
        double r  = FastMath.sqrt(r2);
        Vector3D keplerianAcceleration = new Vector3D(-orbit.getMu() / (r2 * r), position);
        Assertions.assertEquals(0.0101, Vector3D.distance(keplerianAcceleration, acceleration), 1.0e-4);

        for (OrbitType type : OrbitType.values()) {
            Orbit converted = type.convertType(orbit);
            Assertions.assertTrue(converted.hasDerivatives());
            EquinoctialOrbit rebuilt = (EquinoctialOrbit) OrbitType.EQUINOCTIAL.convertType(converted);
            Assertions.assertTrue(rebuilt.hasDerivatives());
            Assertions.assertEquals(orbit.getADot(),             rebuilt.getADot(),             3.0e-13);
            Assertions.assertEquals(orbit.getEquinoctialExDot(), rebuilt.getEquinoctialExDot(), 1.0e-15);
            Assertions.assertEquals(orbit.getEquinoctialEyDot(), rebuilt.getEquinoctialEyDot(), 1.0e-15);
            Assertions.assertEquals(orbit.getHxDot(),            rebuilt.getHxDot(),            1.0e-15);
            Assertions.assertEquals(orbit.getHyDot(),            rebuilt.getHyDot(),            1.0e-15);
            Assertions.assertEquals(orbit.getLvDot(),            rebuilt.getLvDot(),            1.0e-15);
        }

    }

    @Test
    public void testToString() {
        Vector3D position = new Vector3D(-29536113.0, 30329259.0, -100125.0);
        Vector3D velocity = new Vector3D(-2194.0, -2141.0, -8.0);
        PVCoordinates pvCoordinates = new PVCoordinates(position, velocity);
        EquinoctialOrbit orbit = new EquinoctialOrbit(pvCoordinates, FramesFactory.getEME2000(), date, mu);
        Assertions.assertEquals("equinoctial parameters: {a: 4.225517000282565E7; ex: 5.927324978565528E-4; ey: -0.002062743969643666; hx: 6.401103130239252E-5; hy: -0.0017606836670756732; lv: 134.24111947709974;}",
                            orbit.toString());
    }

    @Test
    void testRemoveRates() {
        // GIVEN
        final Vector3D position = new Vector3D(-29536113.0, 30329259.0, -100125.0);
        final Vector3D velocity = new Vector3D(-2194.0, -2141.0, -8.0);
        final PVCoordinates pvCoordinates = new PVCoordinates(position, velocity);
        final EquinoctialOrbit orbit = new EquinoctialOrbit(pvCoordinates, FramesFactory.getGCRF(), date, mu);
        // WHEN
        final EquinoctialOrbit orbitWithoutRates = orbit.removeRates();
        // THEN
        Assertions.assertFalse(orbitWithoutRates.hasRates());
        Assertions.assertTrue(Double.isNaN(orbitWithoutRates.getADot()));
        Assertions.assertEquals(orbit.getMu(), orbitWithoutRates.getMu());
        Assertions.assertEquals(orbit.getDate(), orbitWithoutRates.getDate());
        Assertions.assertEquals(orbit.getFrame(), orbitWithoutRates.getFrame());
        Assertions.assertEquals(orbit.getA(), orbitWithoutRates.getA());
        Assertions.assertEquals(orbit.getEquinoctialEx(), orbitWithoutRates.getEquinoctialEx());
        Assertions.assertEquals(orbit.getEquinoctialEy(), orbitWithoutRates.getEquinoctialEy());
        Assertions.assertEquals(orbit.getHx(), orbitWithoutRates.getHx());
        Assertions.assertEquals(orbit.getHy(), orbitWithoutRates.getHy());
        Assertions.assertEquals(orbit.getLv(), orbitWithoutRates.getLv());
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
        final Orbit orbit = new EquinoctialOrbit(pv, eme2000, date, mu);

        // Build another KeplerianOrbit as a copy of the first one
        final Orbit orbitCopy = new EquinoctialOrbit(orbit);

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
        EquinoctialOrbit withoutDerivatives =
                        new EquinoctialOrbit(42166712.0, 0.005, -0.025, 0.17, 0.34,
                                             0.4, PositionAngleType.MEAN,
                                             FramesFactory.getEME2000(), date, mu);
        EquinoctialOrbit ref =
                        new EquinoctialOrbit(24000000.0, -0.012, 0.01, 0.2, 0.1,
                                             -6.28, PositionAngleType.MEAN,
                                             FramesFactory.getEME2000(), date, mu);

        EquinoctialOrbit normalized1 = (EquinoctialOrbit) OrbitType.EQUINOCTIAL.normalize(withoutDerivatives, ref);
        Assertions.assertFalse(normalized1.hasDerivatives());
        Assertions.assertEquals(0.0, normalized1.getA()             - withoutDerivatives.getA(),             1.0e-6);
        Assertions.assertEquals(0.0, normalized1.getEquinoctialEx() - withoutDerivatives.getEquinoctialEx(), 1.0e-10);
        Assertions.assertEquals(0.0, normalized1.getEquinoctialEy() - withoutDerivatives.getEquinoctialEy(), 1.0e-10);
        Assertions.assertEquals(0.0, normalized1.getHx()            - withoutDerivatives.getHx(),            1.0e-10);
        Assertions.assertEquals(0.0, normalized1.getHy()            - withoutDerivatives.getHy(),            1.0e-10);
        Assertions.assertEquals(-MathUtils.TWO_PI, normalized1.getLv() - withoutDerivatives.getLv(),         1.0e-10);
        Assertions.assertTrue(Double.isNaN(normalized1.getADot()));
        Assertions.assertTrue(Double.isNaN(normalized1.getEquinoctialExDot()));
        Assertions.assertTrue(Double.isNaN(normalized1.getEquinoctialEyDot()));
        Assertions.assertTrue(Double.isNaN(normalized1.getHxDot()));
        Assertions.assertTrue(Double.isNaN(normalized1.getHyDot()));
        Assertions.assertTrue(Double.isNaN(normalized1.getLvDot()));

        double[] p = new double[6];
        OrbitType.EQUINOCTIAL.mapOrbitToArray(withoutDerivatives, PositionAngleType.TRUE, p, null);
        EquinoctialOrbit withDerivatives = (EquinoctialOrbit) OrbitType.EQUINOCTIAL.mapArrayToOrbit(p,
                                                                                                    new double[] { 1.0, 2.0, 3.0, 4.0, 5.0, 6.0 },
                                                                                                    PositionAngleType.TRUE,
                                                                                                    withoutDerivatives.getDate(),
                                                                                                    withoutDerivatives.getMu(),
                                                                                                    withoutDerivatives.getFrame());
        EquinoctialOrbit normalized2 = (EquinoctialOrbit) OrbitType.EQUINOCTIAL.normalize(withDerivatives, ref);
        Assertions.assertTrue(normalized2.hasDerivatives());
        Assertions.assertFalse(normalized1.hasDerivatives());
        Assertions.assertEquals(0.0, normalized1.getA()                - withoutDerivatives.getA(),             1.0e-6);
        Assertions.assertEquals(0.0, normalized1.getEquinoctialEx()    - withoutDerivatives.getEquinoctialEx(), 1.0e-10);
        Assertions.assertEquals(0.0, normalized1.getEquinoctialEy()    - withoutDerivatives.getEquinoctialEy(), 1.0e-10);
        Assertions.assertEquals(0.0, normalized1.getHx()               - withoutDerivatives.getHx(),            1.0e-10);
        Assertions.assertEquals(0.0, normalized1.getHy()               - withoutDerivatives.getHy(),            1.0e-10);
        Assertions.assertEquals(-MathUtils.TWO_PI, normalized1.getLv() - withoutDerivatives.getLv(),            1.0e-10);
        Assertions.assertEquals(0.0, normalized2.getADot()             - withDerivatives.getADot(),             1.0e-10);
        Assertions.assertEquals(0.0, normalized2.getEquinoctialExDot() - withDerivatives.getEquinoctialExDot(), 1.0e-10);
        Assertions.assertEquals(0.0, normalized2.getEquinoctialEyDot() - withDerivatives.getEquinoctialEyDot(), 1.0e-10);
        Assertions.assertEquals(0.0, normalized2.getHxDot()            - withDerivatives.getHxDot(),            1.0e-10);
        Assertions.assertEquals(0.0, normalized2.getHyDot()            - withDerivatives.getHyDot(),            1.0e-10);
        Assertions.assertEquals(0.0, normalized2.getLvDot()            - withDerivatives.getLvDot(),            1.0e-10);

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
