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
package org.orekit.orbits;

import org.apache.commons.math.geometry.Vector3D;
import org.apache.commons.math.util.FastMath;
import org.apache.commons.math.util.MathUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.Transform;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.PVCoordinates;


public class EquinoctialParametersTest {

    // Computation date 
    private AbsoluteDate date;
    
    // Body mu 
    private double mu;

    @Test
    @Deprecated
    public void testOldConstructors() {

        double ix = 1.200e-04;
        double iy = -1.16e-04;
        double inc = 2 * FastMath.asin(FastMath.sqrt((ix * ix + iy * iy) / 4.));
        double hx = FastMath.tan(inc / 2.) * ix / (2 * FastMath.sin(inc / 2.));
        double hy = FastMath.tan(inc / 2.) * iy / (2 * FastMath.sin(inc / 2.));

        int inexistantType = 17;
        int[] types = {
            EquinoctialOrbit.MEAN_LATITUDE_ARGUMENT,
            EquinoctialOrbit.ECCENTRIC_LATITUDE_ARGUMENT,
            EquinoctialOrbit.TRUE_LATITUDE_ARGUMENT,
            inexistantType
        };

        for (int type : types) {
            try {

                EquinoctialOrbit equi =
                    new EquinoctialOrbit(42166.712, 0.5, -0.5, hx, hy,
                                              5.300, PositionAngle.MEAN, 
                                              FramesFactory.getEME2000(), date, mu);

                PVCoordinates pvCoordinates = equi.getPVCoordinates();
                KeplerianOrbit param = new KeplerianOrbit(pvCoordinates, FramesFactory.getEME2000(), date, mu);
                Assert.assertEquals(param.getA(),  equi.getA(), Utils.epsilonTest * equi.getA());
                Assert.assertEquals(param.getEquinoctialEx(), equi.getEquinoctialEx(), Utils.epsilonE * FastMath.abs(equi.getE()));
                Assert.assertEquals(param.getEquinoctialEy(), equi.getEquinoctialEy(), Utils.epsilonE * FastMath.abs(equi.getE()));
                Assert.assertEquals(param.getHx(), equi.getHx(), Utils.epsilonAngle * FastMath.abs(equi.getI()));
                Assert.assertEquals(param.getHy(), equi.getHy(), Utils.epsilonAngle * FastMath.abs(equi.getI()));
                Assert.assertEquals(MathUtils.normalizeAngle(param.getLv(),equi.getLv()), equi.getLv(), Utils.epsilonAngle * FastMath.abs(equi.getLv()));
            } catch (IllegalArgumentException iae) {
                Assert.assertEquals(inexistantType, type);
            }
        }
    }

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

    @Before
    public void setUp() {

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
