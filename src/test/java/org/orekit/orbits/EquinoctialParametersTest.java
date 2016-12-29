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


public class EquinoctialParametersTest {

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
            new EquinoctialOrbit(42166.712, 0.5, -0.5, hx, hy,
                                      5.300, PositionAngle.MEAN,
                                      FramesFactory.getEME2000(), date, mu);
        Vector3D pos = equi.getPVCoordinates().getPosition();
        Vector3D vit = equi.getPVCoordinates().getVelocity();

        PVCoordinates pvCoordinates = new PVCoordinates(pos,vit);

        EquinoctialOrbit param = new EquinoctialOrbit(pvCoordinates, FramesFactory.getEME2000(), date, mu);
        Assert.assertEquals(param.getA(), equi.getA(), Utils.epsilonTest * equi.getA());
        Assert.assertEquals(param.getEquinoctialEx(), equi.getEquinoctialEx(),
                     Utils.epsilonE * FastMath.abs(equi.getE()));
        Assert.assertEquals(param.getEquinoctialEy(), equi.getEquinoctialEy(),
                     Utils.epsilonE * FastMath.abs(equi.getE()));
        Assert.assertEquals(param.getHx(), equi.getHx(), Utils.epsilonAngle
                     * FastMath.abs(equi.getI()));
        Assert.assertEquals(param.getHy(), equi.getHy(), Utils.epsilonAngle
                     * FastMath.abs(equi.getI()));
        Assert.assertEquals(MathUtils.normalizeAngle(param.getLv(), equi.getLv()), equi.getLv(),
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
            new EquinoctialOrbit(42166.712, 0.1e-10, -0.1e-10, hx, hy,
                                      5.300, PositionAngle.MEAN,
                                      FramesFactory.getEME2000(), date, mu);
        Vector3D posCir = equiCir.getPVCoordinates().getPosition();
        Vector3D vitCir = equiCir.getPVCoordinates().getVelocity();

        PVCoordinates pvCoordinates = new PVCoordinates(posCir,vitCir);

        EquinoctialOrbit paramCir = new EquinoctialOrbit(pvCoordinates, FramesFactory.getEME2000(),
                                                         date, mu);
        Assert.assertEquals(paramCir.getA(), equiCir.getA(), Utils.epsilonTest
                     * equiCir.getA());
        Assert.assertEquals(paramCir.getEquinoctialEx(), equiCir.getEquinoctialEx(),
                     Utils.epsilonEcir * FastMath.abs(equiCir.getE()));
        Assert.assertEquals(paramCir.getEquinoctialEy(), equiCir.getEquinoctialEy(),
                     Utils.epsilonEcir * FastMath.abs(equiCir.getE()));
        Assert.assertEquals(paramCir.getHx(), equiCir.getHx(), Utils.epsilonAngle
                     * FastMath.abs(equiCir.getI()));
        Assert.assertEquals(paramCir.getHy(), equiCir.getHy(), Utils.epsilonAngle
                     * FastMath.abs(equiCir.getI()));
        Assert.assertEquals(MathUtils.normalizeAngle(paramCir.getLv(), equiCir.getLv()), equiCir
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
            new EquinoctialOrbit(42166.712, -7.900e-06, 1.100e-04, hx, hy,
                                      5.300, PositionAngle.MEAN,
                                      FramesFactory.getEME2000(), date, mu);
        Vector3D pos = equi.getPVCoordinates().getPosition();
        Vector3D vit = equi.getPVCoordinates().getVelocity();

        // verif of 1/a = 2/X - V2/mu
        double oneovera = (2. / pos.getNorm()) - vit.getNorm() * vit.getNorm() / mu;
        Assert.assertEquals(oneovera, 1. / equi.getA(), 1.0e-7);

        Assert.assertEquals(0.233745668678733e+05, pos.getX(), Utils.epsilonTest
                     * FastMath.abs(pos.getX()));
        Assert.assertEquals(-0.350998914352669e+05, pos.getY(), Utils.epsilonTest
                     * FastMath.abs(pos.getY()));
        Assert.assertEquals(-0.150053723123334e+01, pos.getZ(), Utils.epsilonTest
                     * FastMath.abs(pos.getZ()));

        Assert.assertEquals(0.809135038364960e+05, vit.getX(), Utils.epsilonTest
                     * FastMath.abs(vit.getX()));
        Assert.assertEquals(0.538902268252598e+05, vit.getY(), Utils.epsilonTest
                     * FastMath.abs(vit.getY()));
        Assert.assertEquals(0.158527938296630e+02, vit.getZ(), Utils.epsilonTest
                     * FastMath.abs(vit.getZ()));

    }

    @Test
    public void testEquinoctialToKeplerian() {

        double ix = 1.20e-4;
        double iy = -1.16e-4;
        double i = 2 * FastMath.asin(FastMath.sqrt((ix * ix + iy * iy) / 4));
        double hx = FastMath.tan(i / 2) * ix / (2 * FastMath.sin(i / 2));
        double hy = FastMath.tan(i / 2) * iy / (2 * FastMath.sin(i / 2));

        EquinoctialOrbit equi =
            new EquinoctialOrbit(42166.712, -7.900e-6, 1.100e-4, hx, hy,
                                      5.300, PositionAngle.MEAN,
                                      FramesFactory.getEME2000(), date, mu);
        KeplerianOrbit kep = new KeplerianOrbit(equi);

        Assert.assertEquals(42166.71200, equi.getA(), Utils.epsilonTest * kep.getA());
        Assert.assertEquals(0.110283316961361e-03, kep.getE(), Utils.epsilonE
                     * FastMath.abs(kep.getE()));
        Assert.assertEquals(0.166901168553917e-03, kep.getI(), Utils.epsilonAngle
                     * FastMath.abs(kep.getI()));
        Assert.assertEquals(MathUtils.normalizeAngle(-3.87224326008837, kep.getPerigeeArgument()),
                     kep.getPerigeeArgument(), Utils.epsilonTest
                     * FastMath.abs(kep.getPerigeeArgument()));
        Assert.assertEquals(MathUtils.normalizeAngle(5.51473467358854, kep
                                     .getRightAscensionOfAscendingNode()), kep
                                     .getRightAscensionOfAscendingNode(), Utils.epsilonTest
                                     * FastMath.abs(kep.getRightAscensionOfAscendingNode()));
        Assert.assertEquals(MathUtils.normalizeAngle(3.65750858649982, kep.getMeanAnomaly()), kep
                     .getMeanAnomaly(), Utils.epsilonTest * FastMath.abs(kep.getMeanAnomaly()));

    }

    @Test(expected=IllegalArgumentException.class)
    public void testHyperbolic() {
        new EquinoctialOrbit(42166.712, 0.9, 0.5, 0.01, -0.02, 5.300,
                             PositionAngle.MEAN,  FramesFactory.getEME2000(), date, mu);
    }

    @Test
    public void testNumericalIssue25() throws OrekitException {
        Vector3D position = new Vector3D(3782116.14107698, 416663.11924914, 5875541.62103057);
        Vector3D velocity = new Vector3D(-6349.7848910501, 288.4061811651, 4066.9366759691);
        EquinoctialOrbit orbit = new EquinoctialOrbit(new PVCoordinates(position, velocity),
                                                      FramesFactory.getEME2000(),
                                                      new AbsoluteDate("2004-01-01T23:00:00.000",
                                                                       TimeScalesFactory.getUTC()),
                                                                       3.986004415E14);
        Assert.assertEquals(0.0, orbit.getE(), 2.0e-14);
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

        p = new EquinoctialOrbit(p.getA() ,p.getEquinoctialEx(),
                                      p.getEquinoctialEy() , p.getHx(), p.getHy() , lv , PositionAngle.TRUE,
                                      p.getFrame(), p.getDate(), p.getMu());
        Assert.assertEquals(p.getLv(), lv, Utils.epsilonAngle * FastMath.abs(lv));
        Assert.assertEquals(p.getLE(), lE, Utils.epsilonAngle * FastMath.abs(lE));
        Assert.assertEquals(p.getLM(), lM, Utils.epsilonAngle * FastMath.abs(lM));
        p = new EquinoctialOrbit(p.getA() ,p.getEquinoctialEx(),
                                      p.getEquinoctialEy() , p.getHx(), p.getHy() , 0 , PositionAngle.TRUE,
                                      p.getFrame(), p.getDate(), p.getMu());

        p = new EquinoctialOrbit(p.getA() ,p.getEquinoctialEx(),
                                      p.getEquinoctialEy() , p.getHx(), p.getHy() , lE , PositionAngle.ECCENTRIC,
                                      p.getFrame(), p.getDate(), p.getMu());
        Assert.assertEquals(p.getLv(), lv, Utils.epsilonAngle * FastMath.abs(lv));
        Assert.assertEquals(p.getLE(), lE, Utils.epsilonAngle * FastMath.abs(lE));
        Assert.assertEquals(p.getLM(), lM, Utils.epsilonAngle * FastMath.abs(lM));
        p = new EquinoctialOrbit(p.getA() ,p.getEquinoctialEx(),
                                      p.getEquinoctialEy() , p.getHx(), p.getHy() , 0 , PositionAngle.TRUE,
                                      p.getFrame(), p.getDate(), p.getMu());

        p = new EquinoctialOrbit(p.getA() ,p.getEquinoctialEx(),
                                      p.getEquinoctialEy() , p.getHx(), p.getHy() , lM , PositionAngle.MEAN,
                                      p.getFrame(), p.getDate(), p.getMu());
        Assert.assertEquals(p.getLv(), lv, Utils.epsilonAngle * FastMath.abs(lv));
        Assert.assertEquals(p.getLE(), lE, Utils.epsilonAngle * FastMath.abs(lE));
        Assert.assertEquals(p.getLM(), lM, Utils.epsilonAngle * FastMath.abs(lM));

        // circular orbit
        p = new EquinoctialOrbit(p.getA() ,0 ,
                                      0, p.getHx(), p.getHy() , p.getLv() , PositionAngle.TRUE,
                                      p.getFrame(), p.getDate(), p.getMu());

        lE = lv;
        lM = lE;

        p = new EquinoctialOrbit(p.getA() ,p.getEquinoctialEx(),
                                      p.getEquinoctialEy() , p.getHx(), p.getHy() , lv , PositionAngle.TRUE,
                                      p.getFrame(), p.getDate(), p.getMu());
        Assert.assertEquals(p.getLv(), lv, Utils.epsilonAngle * FastMath.abs(lv));
        Assert.assertEquals(p.getLE(), lE, Utils.epsilonAngle * FastMath.abs(lE));
        Assert.assertEquals(p.getLM(), lM, Utils.epsilonAngle * FastMath.abs(lM));
        p = new EquinoctialOrbit(p.getA() ,p.getEquinoctialEx(),
                                      p.getEquinoctialEy() , p.getHx(), p.getHy() , 0 , PositionAngle.TRUE,
                                      p.getFrame(), p.getDate(), p.getMu());

        p = new EquinoctialOrbit(p.getA() ,p.getEquinoctialEx(),
                                      p.getEquinoctialEy() , p.getHx(), p.getHy() , lE , PositionAngle.ECCENTRIC,
                                      p.getFrame(), p.getDate(), p.getMu());
        Assert.assertEquals(p.getLv(), lv, Utils.epsilonAngle * FastMath.abs(lv));
        Assert.assertEquals(p.getLE(), lE, Utils.epsilonAngle * FastMath.abs(lE));
        Assert.assertEquals(p.getLM(), lM, Utils.epsilonAngle * FastMath.abs(lM));
        p = new EquinoctialOrbit(p.getA() ,p.getEquinoctialEx(),
                                      p.getEquinoctialEy() , p.getHx(), p.getHy() , 0 , PositionAngle.TRUE,
                                      p.getFrame(), p.getDate(), p.getMu());

        p = new EquinoctialOrbit(p.getA() ,p.getEquinoctialEx(),
                                      p.getEquinoctialEy() , p.getHx(), p.getHy() , lM , PositionAngle.MEAN, p.getFrame(), p.getDate(), p.getMu());
        Assert.assertEquals(p.getLv(), lv, Utils.epsilonAngle * FastMath.abs(lv));
        Assert.assertEquals(p.getLE(), lE, Utils.epsilonAngle * FastMath.abs(lE));
        Assert.assertEquals(p.getLM(), lM, Utils.epsilonAngle * FastMath.abs(lM));
    }

    @Test
    public void testPositionVelocityNorms() {

        // elliptic and non equatorial (i retrograde) orbit
        EquinoctialOrbit p =
            new EquinoctialOrbit(42166.712, 0.5, -0.5, 1.200, 2.1,
                                      0.67, PositionAngle.TRUE,
                                      FramesFactory.getEME2000(), date, mu);

        double ex = p.getEquinoctialEx();
        double ey = p.getEquinoctialEy();
        double lv = p.getLv();
        double ksi = 1 + ex * FastMath.cos(lv) + ey * FastMath.sin(lv);
        double nu = ex * FastMath.sin(lv) - ey * FastMath.cos(lv);
        double epsilon = FastMath.sqrt(1 - ex * ex - ey * ey);

        double a = p.getA();
        double na = FastMath.sqrt(p.getMu() / a);

        Assert.assertEquals(a * epsilon * epsilon / ksi, p.getPVCoordinates().getPosition().getNorm(),
                     Utils.epsilonTest * FastMath.abs(p.getPVCoordinates().getPosition().getNorm()));
        Assert.assertEquals(na * FastMath.sqrt(ksi * ksi + nu * nu) / epsilon, p
                     .getPVCoordinates().getVelocity().getNorm(), Utils.epsilonTest
                     * FastMath.abs(p.getPVCoordinates().getVelocity().getNorm()));

        // circular and equatorial orbit
        EquinoctialOrbit pCirEqua =
            new EquinoctialOrbit(42166.712, 0.1e-8, 0.1e-8, 0.1e-8, 0.1e-8,
                                      0.67, PositionAngle.TRUE,
                                      FramesFactory.getEME2000(), date, mu);

        ex = pCirEqua.getEquinoctialEx();
        ey = pCirEqua.getEquinoctialEy();
        lv = pCirEqua.getLv();
        ksi = 1 + ex * FastMath.cos(lv) + ey * FastMath.sin(lv);
        nu = ex * FastMath.sin(lv) - ey * FastMath.cos(lv);
        epsilon = FastMath.sqrt(1 - ex * ex - ey * ey);

        a = pCirEqua.getA();
        na = FastMath.sqrt(pCirEqua.getMu() / a);

        Assert.assertEquals(a * epsilon * epsilon / ksi, pCirEqua.getPVCoordinates().getPosition()
                     .getNorm(), Utils.epsilonTest
                     * FastMath.abs(pCirEqua.getPVCoordinates().getPosition().getNorm()));
        Assert.assertEquals(na * FastMath.sqrt(ksi * ksi + nu * nu) / epsilon, pCirEqua
                     .getPVCoordinates().getVelocity().getNorm(), Utils.epsilonTest
                     * FastMath.abs(pCirEqua.getPVCoordinates().getVelocity().getNorm()));
    }

    @Test
    public void testGeometry() {

        // elliptic and non equatorial (i retrograde) orbit
        EquinoctialOrbit p =
            new EquinoctialOrbit(42166.712, 0.5, -0.5, 1.200, 2.1,
                                      0.67, PositionAngle.TRUE,
                                      FramesFactory.getEME2000(), date, mu);

        Vector3D position = p.getPVCoordinates().getPosition();
        Vector3D velocity = p.getPVCoordinates().getVelocity();
        Vector3D momentum = p.getPVCoordinates().getMomentum().normalize();

        double apogeeRadius = p.getA() * (1 + p.getE());
        double perigeeRadius = p.getA() * (1 - p.getE());

        for (double lv = 0; lv <= 2 * FastMath.PI; lv += 2 * FastMath.PI / 100.) {
            p = new EquinoctialOrbit(p.getA() ,p.getEquinoctialEx(),
                                          p.getEquinoctialEy() , p.getHx(), p.getHy() , lv , PositionAngle.TRUE,
                                          p.getFrame(), p.getDate(), p.getMu());
            position = p.getPVCoordinates().getPosition();

            // test if the norm of the position is in the range [perigee radius,
            // apogee radius]
            // Warning: these tests are without absolute value by choice
            Assert.assertTrue((position.getNorm() - apogeeRadius) <= (apogeeRadius * Utils.epsilonTest));
            Assert.assertTrue((position.getNorm() - perigeeRadius) >= (-perigeeRadius * Utils.epsilonTest));

            position = position.normalize();
            velocity = p.getPVCoordinates().getVelocity();
            velocity = velocity.normalize();

            // at this stage of computation, all the vectors (position, velocity and
            // momemtum) are normalized here

            // test of orthogonality between position and momentum
            Assert.assertTrue(FastMath.abs(Vector3D.dotProduct(position, momentum)) < Utils.epsilonTest);
            // test of orthogonality between velocity and momentum
            Assert.assertTrue(FastMath.abs(Vector3D.dotProduct(velocity, momentum)) < Utils.epsilonTest);
        }

        // circular and equatorial orbit
        EquinoctialOrbit pCirEqua =
            new EquinoctialOrbit(42166.712, 0.1e-8, 0.1e-8, 0.1e-8, 0.1e-8,
                                      0.67, PositionAngle.TRUE,
                                      FramesFactory.getEME2000(), date, mu);

        position = pCirEqua.getPVCoordinates().getPosition();
        velocity = pCirEqua.getPVCoordinates().getVelocity();

        momentum = Vector3D.crossProduct(position, velocity).normalize();

        apogeeRadius = pCirEqua.getA() * (1 + pCirEqua.getE());
        perigeeRadius = pCirEqua.getA() * (1 - pCirEqua.getE());
        // test if apogee equals perigee
        Assert.assertEquals(perigeeRadius, apogeeRadius, 1.e+4 * Utils.epsilonTest
                     * apogeeRadius);

        for (double lv = 0; lv <= 2 * FastMath.PI; lv += 2 * FastMath.PI / 100.) {
            pCirEqua = new EquinoctialOrbit(pCirEqua.getA() ,pCirEqua.getEquinoctialEx(),
                                                 pCirEqua.getEquinoctialEy() , pCirEqua.getHx(), pCirEqua.getHy() , lv , PositionAngle.TRUE,
                                                 pCirEqua.getFrame(), p.getDate(), p.getMu());
            position = pCirEqua.getPVCoordinates().getPosition();

            // test if the norm pf the position is in the range [perigee radius,
            // apogee radius]
            Assert.assertTrue((position.getNorm() - apogeeRadius) <= (apogeeRadius * Utils.epsilonTest));
            Assert.assertTrue((position.getNorm() - perigeeRadius) >= (-perigeeRadius * Utils.epsilonTest));

            position = position.normalize();
            velocity = pCirEqua.getPVCoordinates().getVelocity();
            velocity = velocity.normalize();

            // at this stage of computation, all the vectors (position, velocity and
            // momemtum) are normalized here

            // test of orthogonality between position and momentum
            Assert.assertTrue(FastMath.abs(Vector3D.dotProduct(position, momentum)) < Utils.epsilonTest);
            // test of orthogonality between velocity and momentum
            Assert.assertTrue(FastMath.abs(Vector3D.dotProduct(velocity, momentum)) < Utils.epsilonTest);
        }
    }


    @Test
    public void testRadiusOfCurvature() {

        // elliptic and non equatorial (i retrograde) orbit
        EquinoctialOrbit p =
            new EquinoctialOrbit(42166.712, 0.5, -0.5, 1.200, 2.1,
                                      0.67, PositionAngle.TRUE,
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
        Assert.assertEquals(rCart, rOrb, 1.0e-15 * p.getA());

        // at this place for such an eccentric orbit,
        // the radius of curvature is much smaller than semi major axis
        Assert.assertEquals(0.8477 * p.getA(), rCart, 1.0e-4 * p.getA());

    }

    @Test
    public void testSymmetry() {

        // elliptic and non equatorial orbit
        Vector3D position = new Vector3D(4512.9, 18260., -5127.);
        Vector3D velocity = new Vector3D(134664.6, 90066.8, 72047.6);

        EquinoctialOrbit p = new EquinoctialOrbit(new PVCoordinates(position, velocity),
                                                  FramesFactory.getEME2000(), date, mu);

        Vector3D positionOffset = p.getPVCoordinates().getPosition().subtract(position);
        Vector3D velocityOffset = p.getPVCoordinates().getVelocity().subtract(velocity);

        Assert.assertTrue(positionOffset.getNorm() < Utils.epsilonTest);
        Assert.assertTrue(velocityOffset.getNorm() < Utils.epsilonTest);

        // circular and equatorial orbit
        position = new Vector3D(33051.2, 26184.9, -1.3E-5);
        velocity = new Vector3D(-60376.2, 76208., 2.7E-4);

        p = new EquinoctialOrbit(new PVCoordinates(position, velocity),
                                 FramesFactory.getEME2000(), date, mu);

        positionOffset = p.getPVCoordinates().getPosition().subtract(position);
        velocityOffset = p.getPVCoordinates().getVelocity().subtract(velocity);

        Assert.assertTrue(positionOffset.getNorm() < Utils.epsilonTest);
        Assert.assertTrue(velocityOffset.getNorm() < Utils.epsilonTest);
    }

    @Test(expected=IllegalArgumentException.class)
    public void testNonInertialFrame() throws IllegalArgumentException {

        Vector3D position = new Vector3D(4512.9, 18260., -5127.);
        Vector3D velocity = new Vector3D(134664.6, 90066.8, 72047.6);
        PVCoordinates pvCoordinates = new PVCoordinates( position, velocity);
        new EquinoctialOrbit(pvCoordinates,
                             new Frame(FramesFactory.getEME2000(), Transform.IDENTITY, "non-inertial", false),
                             date, mu);
    }

    @Test
    public void testJacobianReference() throws OrekitException {

        AbsoluteDate dateTca = new AbsoluteDate(2000, 04, 01, 0, 0, 0.000, TimeScalesFactory.getUTC());
        double mu =  3.986004415e+14;
        EquinoctialOrbit orbEqu = new EquinoctialOrbit(7000000.0, 0.01, -0.02, 1.2, 2.1,
                                          FastMath.toRadians(40.), PositionAngle.MEAN,
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
        Assert.assertEquals(0, pv.getPosition().subtract(pRef).getNorm(), 2.0e-16 * pRef.getNorm());
        Assert.assertEquals(0, pv.getVelocity().subtract(vRef).getNorm(), 2.0e-16 * vRef.getNorm());

        double[][] jacobian = new double[6][6];
        orbEqu.getJacobianWrtCartesian(PositionAngle.MEAN, jacobian);

        for (int i = 0; i < jacobian.length; i++) {
            double[] row    = jacobian[i];
            double[] rowRef = jRef[i];
            for (int j = 0; j < row.length; j++) {
                Assert.assertEquals(0, (row[j] - rowRef[j]) / rowRef[j], 4.0e-15);
            }
        }

    }

    @Test
    public void testJacobianFinitedifferences() throws OrekitException {

        AbsoluteDate dateTca = new AbsoluteDate(2000, 04, 01, 0, 0, 0.000, TimeScalesFactory.getUTC());
        double mu =  3.986004415e+14;
        EquinoctialOrbit orbEqu = new EquinoctialOrbit(7000000.0, 0.01, -0.02, 1.2, 2.1,
                                                       FastMath.toRadians(40.), PositionAngle.MEAN,
                                                       FramesFactory.getEME2000(), dateTca, mu);

        for (PositionAngle type : PositionAngle.values()) {
            double hP = 2.0;
            double[][] finiteDiffJacobian = finiteDifferencesJacobian(type, orbEqu, hP);
            double[][] jacobian = new double[6][6];
            orbEqu.getJacobianWrtCartesian(type, jacobian);

            for (int i = 0; i < jacobian.length; i++) {
                double[] row    = jacobian[i];
                double[] rowRef = finiteDiffJacobian[i];
                for (int j = 0; j < row.length; j++) {
                    Assert.assertEquals(0, (row[j] - rowRef[j]) / rowRef[j], 4.0e-9);
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
                    Assert.assertEquals(row == column ? 1.0 : 0.0, value, 7.0e-9);
                }

                public double end() {
                    return Double.NaN;
                }
            });

        }

    }

    private double[][] finiteDifferencesJacobian(PositionAngle type, EquinoctialOrbit orbit, double hP)
        throws OrekitException {
        double[][] jacobian = new double[6][6];
        for (int i = 0; i < 6; ++i) {
            fillColumn(type, i, orbit, hP, jacobian);
        }
        return jacobian;
    }

    private void fillColumn(PositionAngle type, int i, EquinoctialOrbit orbit, double hP, double[][] jacobian) {

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
        final EquinoctialOrbit initialOrbit = new EquinoctialOrbit(new PVCoordinates(position, velocity),
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
        Assert.assertTrue(maxInterpolationError < 1.3);

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
        EquinoctialOrbit orbit = new EquinoctialOrbit(pvCoordinates, FramesFactory.getEME2000(), date, mu);
        Assert.assertEquals(42255170.003, orbit.getA(), 1.0e-3);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream    oos = new ObjectOutputStream(bos);
        oos.writeObject(orbit);

        Assert.assertTrue(bos.size() > 250);
        Assert.assertTrue(bos.size() < 350);

        ByteArrayInputStream  bis = new ByteArrayInputStream(bos.toByteArray());
        ObjectInputStream     ois = new ObjectInputStream(bis);
        EquinoctialOrbit deserialized  = (EquinoctialOrbit) ois.readObject();
        Assert.assertEquals(orbit.getA(), deserialized.getA(), 1.0e-10);
        Assert.assertEquals(orbit.getEquinoctialEx(), deserialized.getEquinoctialEx(), 1.0e-10);
        Assert.assertEquals(orbit.getEquinoctialEy(), deserialized.getEquinoctialEy(), 1.0e-10);
        Assert.assertEquals(orbit.getHx(), deserialized.getHx(), 1.0e-10);
        Assert.assertEquals(orbit.getHy(), deserialized.getHy(), 1.0e-10);
        Assert.assertEquals(orbit.getLv(), deserialized.getLv(), 1.0e-10);
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
