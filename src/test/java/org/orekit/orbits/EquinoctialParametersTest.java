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
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.PVCoordinates;


public class EquinoctialParametersTest extends TestCase {

    // Computation date 
    private AbsoluteDate date;
    
    // Body mu 
    private double mu;

    public EquinoctialParametersTest(String name) {
        super(name);
    }

    public void testEquinoctialToEquinoctialEll() {

        double ix = 1.200e-04;
        double iy = -1.16e-04;
        double inc = 2 * Math.asin(Math.sqrt((ix * ix + iy * iy) / 4.));
        double hx = Math.tan(inc / 2.) * ix / (2 * Math.sin(inc / 2.));
        double hy = Math.tan(inc / 2.) * iy / (2 * Math.sin(inc / 2.));

        // elliptic orbit
        EquinoctialOrbit equi =
            new EquinoctialOrbit(42166.712, 0.5, -0.5, hx, hy,
                                      5.300, EquinoctialOrbit.MEAN_LATITUDE_ARGUMENT, 
                                      Frame.getJ2000(), date, mu);
        Vector3D pos = equi.getPVCoordinates().getPosition();
        Vector3D vit = equi.getPVCoordinates().getVelocity();

        PVCoordinates pvCoordinates = new PVCoordinates(pos,vit);

        EquinoctialOrbit param = new EquinoctialOrbit(pvCoordinates, Frame.getJ2000(), date, mu);
        assertEquals(param.getA(), equi.getA(), Utils.epsilonTest * equi.getA());
        assertEquals(param.getEquinoctialEx(), equi.getEquinoctialEx(),
                     Utils.epsilonE * Math.abs(equi.getE()));
        assertEquals(param.getEquinoctialEy(), equi.getEquinoctialEy(),
                     Utils.epsilonE * Math.abs(equi.getE()));
        assertEquals(param.getHx(), equi.getHx(), Utils.epsilonAngle
                     * Math.abs(equi.getI()));
        assertEquals(param.getHy(), equi.getHy(), Utils.epsilonAngle
                     * Math.abs(equi.getI()));
        assertEquals(MathUtils.normalizeAngle(param.getLv(), equi.getLv()), equi.getLv(),
                     Utils.epsilonAngle * Math.abs(equi.getLv()));

    }

    public void testEquinoctialToEquinoctialCirc() {

        double ix = 1.200e-04;
        double iy = -1.16e-04;
        double inc = 2 * Math.asin(Math.sqrt((ix * ix + iy * iy) / 4.));
        double hx = Math.tan(inc / 2.) * ix / (2 * Math.sin(inc / 2.));
        double hy = Math.tan(inc / 2.) * iy / (2 * Math.sin(inc / 2.));

        // circular orbit
        EquinoctialOrbit equiCir =
            new EquinoctialOrbit(42166.712, 0.1e-10, -0.1e-10, hx, hy,
                                      5.300, EquinoctialOrbit.MEAN_LATITUDE_ARGUMENT, 
                                      Frame.getJ2000(), date, mu);
        Vector3D posCir = equiCir.getPVCoordinates().getPosition();
        Vector3D vitCir = equiCir.getPVCoordinates().getVelocity();

        PVCoordinates pvCoordinates = new PVCoordinates(posCir,vitCir);

        EquinoctialOrbit paramCir = new EquinoctialOrbit(pvCoordinates, Frame.getJ2000(),
                                                         date, mu);
        assertEquals(paramCir.getA(), equiCir.getA(), Utils.epsilonTest
                     * equiCir.getA());
        assertEquals(paramCir.getEquinoctialEx(), equiCir.getEquinoctialEx(),
                     Utils.epsilonEcir * Math.abs(equiCir.getE()));
        assertEquals(paramCir.getEquinoctialEy(), equiCir.getEquinoctialEy(),
                     Utils.epsilonEcir * Math.abs(equiCir.getE()));
        assertEquals(paramCir.getHx(), equiCir.getHx(), Utils.epsilonAngle
                     * Math.abs(equiCir.getI()));
        assertEquals(paramCir.getHy(), equiCir.getHy(), Utils.epsilonAngle
                     * Math.abs(equiCir.getI()));
        assertEquals(MathUtils.normalizeAngle(paramCir.getLv(), equiCir.getLv()), equiCir
                     .getLv(), Utils.epsilonAngle * Math.abs(equiCir.getLv()));

    }

    public void testEquinoctialToCartesian() {

        double ix = 1.200e-04;
        double iy = -1.16e-04;
        double inc = 2 * Math.asin(Math.sqrt((ix * ix + iy * iy) / 4.));
        double hx = Math.tan(inc / 2.) * ix / (2 * Math.sin(inc / 2.));
        double hy = Math.tan(inc / 2.) * iy / (2 * Math.sin(inc / 2.));

        EquinoctialOrbit equi =
            new EquinoctialOrbit(42166.712, -7.900e-06, 1.100e-04, hx, hy,
                                      5.300, EquinoctialOrbit.MEAN_LATITUDE_ARGUMENT, 
                                      Frame.getJ2000(), date, mu);
        Vector3D pos = equi.getPVCoordinates().getPosition();
        Vector3D vit = equi.getPVCoordinates().getVelocity();

        // verif of 1/a = 2/X - V2/mu
        double oneovera = (2. / pos.getNorm()) - vit.getNorm() * vit.getNorm() / mu;
        assertEquals(oneovera, 1. / equi.getA(), 1.0e-7);

        assertEquals(0.233745668678733e+05, pos.getX(), Utils.epsilonTest
                     * Math.abs(pos.getX()));
        assertEquals(-0.350998914352669e+05, pos.getY(), Utils.epsilonTest
                     * Math.abs(pos.getY()));
        assertEquals(-0.150053723123334e+01, pos.getZ(), Utils.epsilonTest
                     * Math.abs(pos.getZ()));

        assertEquals(0.809135038364960e+05, vit.getX(), Utils.epsilonTest
                     * Math.abs(vit.getX()));
        assertEquals(0.538902268252598e+05, vit.getY(), Utils.epsilonTest
                     * Math.abs(vit.getY()));
        assertEquals(0.158527938296630e+02, vit.getZ(), Utils.epsilonTest
                     * Math.abs(vit.getZ()));

    }

    public void testEquinoctialToKeplerian() {

        double ix = 1.20e-4;
        double iy = -1.16e-4;
        double i = 2 * Math.asin(Math.sqrt((ix * ix + iy * iy) / 4));
        double hx = Math.tan(i / 2) * ix / (2 * Math.sin(i / 2));
        double hy = Math.tan(i / 2) * iy / (2 * Math.sin(i / 2));

        EquinoctialOrbit equi =
            new EquinoctialOrbit(42166.712, -7.900e-6, 1.100e-4, hx, hy,
                                      5.300, EquinoctialOrbit.MEAN_LATITUDE_ARGUMENT, 
                                      Frame.getJ2000(), date, mu);
        KeplerianOrbit kep = new KeplerianOrbit(equi);

        assertEquals(42166.71200, equi.getA(), Utils.epsilonTest * kep.getA());
        assertEquals(0.110283316961361e-03, kep.getE(), Utils.epsilonE
                     * Math.abs(kep.getE()));
        assertEquals(0.166901168553917e-03, kep.getI(), Utils.epsilonAngle
                     * Math.abs(kep.getI()));
        assertEquals(MathUtils.normalizeAngle(-3.87224326008837, kep.getPerigeeArgument()),
                     kep.getPerigeeArgument(), Utils.epsilonTest
                     * Math.abs(kep.getPerigeeArgument()));
        assertEquals(MathUtils.normalizeAngle(5.51473467358854, kep
                                     .getRightAscensionOfAscendingNode()), kep
                                     .getRightAscensionOfAscendingNode(), Utils.epsilonTest
                                     * Math.abs(kep.getRightAscensionOfAscendingNode()));
        assertEquals(MathUtils.normalizeAngle(3.65750858649982, kep.getMeanAnomaly()), kep
                     .getMeanAnomaly(), Utils.epsilonTest * Math.abs(kep.getMeanAnomaly()));

    }

    public void testAnomaly() {

        // elliptic orbit
        Vector3D position = new Vector3D(7.0e6, 1.0e6, 4.0e6);
        Vector3D velocity = new Vector3D(-500.0, 8000.0, 1000.0);

        EquinoctialOrbit p = new EquinoctialOrbit(new PVCoordinates(position, velocity), Frame.getJ2000(), date, mu);
        KeplerianOrbit kep = new KeplerianOrbit(p);

        double e = p.getE();
        double eRatio = Math.sqrt((1 - e) / (1 + e));
        double paPraan = kep.getPerigeeArgument()
        + kep.getRightAscensionOfAscendingNode();

        double lv = 1.1;
        // formulations for elliptic case
        double lE = 2 * Math.atan(eRatio * Math.tan((lv - paPraan) / 2)) + paPraan;
        double lM = lE - e * Math.sin(lE - paPraan);

//      p.setLv(lv);
        p = new EquinoctialOrbit(p.getA() ,p.getEquinoctialEx(),
                                      p.getEquinoctialEy() , p.getHx(), p.getHy() , lv , 2, 
                                      p.getFrame(), p.getDate(), p.getMu());
        assertEquals(p.getLv(), lv, Utils.epsilonAngle * Math.abs(lv));
        assertEquals(p.getLE(), lE, Utils.epsilonAngle * Math.abs(lE));
        assertEquals(p.getLM(), lM, Utils.epsilonAngle * Math.abs(lM));
//      p.setLv(0);
        p = new EquinoctialOrbit(p.getA() ,p.getEquinoctialEx(),
                                      p.getEquinoctialEy() , p.getHx(), p.getHy() , 0 , 2, 
                                      p.getFrame(), p.getDate(), p.getMu());

//      p.setLE(lE);
        p = new EquinoctialOrbit(p.getA() ,p.getEquinoctialEx(),
                                      p.getEquinoctialEy() , p.getHx(), p.getHy() , lE , 1, 
                                      p.getFrame(), p.getDate(), p.getMu());
        assertEquals(p.getLv(), lv, Utils.epsilonAngle * Math.abs(lv));
        assertEquals(p.getLE(), lE, Utils.epsilonAngle * Math.abs(lE));
        assertEquals(p.getLM(), lM, Utils.epsilonAngle * Math.abs(lM));
//      p.setLv(0);
        p = new EquinoctialOrbit(p.getA() ,p.getEquinoctialEx(),
                                      p.getEquinoctialEy() , p.getHx(), p.getHy() , 0 , 2, 
                                      p.getFrame(), p.getDate(), p.getMu());

//      p.setLM(lM);
        p = new EquinoctialOrbit(p.getA() ,p.getEquinoctialEx(),
                                      p.getEquinoctialEy() , p.getHx(), p.getHy() , lM , 0, 
                                      p.getFrame(), p.getDate(), p.getMu());
        assertEquals(p.getLv(), lv, Utils.epsilonAngle * Math.abs(lv));
        assertEquals(p.getLE(), lE, Utils.epsilonAngle * Math.abs(lE));
        assertEquals(p.getLM(), lM, Utils.epsilonAngle * Math.abs(lM));

        // circular orbit
        p = new EquinoctialOrbit(p.getA() ,0 ,
                                      0, p.getHx(), p.getHy() , p.getLv() , 2, 
                                      p.getFrame(), p.getDate(), p.getMu());

        lE = lv;
        lM = lE;

//      p.setLv(lv);
        p = new EquinoctialOrbit(p.getA() ,p.getEquinoctialEx(),
                                      p.getEquinoctialEy() , p.getHx(), p.getHy() , lv , 2, 
                                      p.getFrame(), p.getDate(), p.getMu());
        assertEquals(p.getLv(), lv, Utils.epsilonAngle * Math.abs(lv));
        assertEquals(p.getLE(), lE, Utils.epsilonAngle * Math.abs(lE));
        assertEquals(p.getLM(), lM, Utils.epsilonAngle * Math.abs(lM));
//      p.setLv(0);
        p = new EquinoctialOrbit(p.getA() ,p.getEquinoctialEx(),
                                      p.getEquinoctialEy() , p.getHx(), p.getHy() , 0 , 2, 
                                      p.getFrame(), p.getDate(), p.getMu());

//      p.setLE(lE);
        p = new EquinoctialOrbit(p.getA() ,p.getEquinoctialEx(),
                                      p.getEquinoctialEy() , p.getHx(), p.getHy() , lE , 1, 
                                      p.getFrame(), p.getDate(), p.getMu());
        assertEquals(p.getLv(), lv, Utils.epsilonAngle * Math.abs(lv));
        assertEquals(p.getLE(), lE, Utils.epsilonAngle * Math.abs(lE));
        assertEquals(p.getLM(), lM, Utils.epsilonAngle * Math.abs(lM));
//      p.setLv(0);
        p = new EquinoctialOrbit(p.getA() ,p.getEquinoctialEx(),
                                      p.getEquinoctialEy() , p.getHx(), p.getHy() , 0 , 2, 
                                      p.getFrame(), p.getDate(), p.getMu());

//      p.setLM(lM);
        p = new EquinoctialOrbit(p.getA() ,p.getEquinoctialEx(),
                                      p.getEquinoctialEy() , p.getHx(), p.getHy() , lM , 0, p.getFrame(), p.getDate(), p.getMu());
        assertEquals(p.getLv(), lv, Utils.epsilonAngle * Math.abs(lv));
        assertEquals(p.getLE(), lE, Utils.epsilonAngle * Math.abs(lE));
        assertEquals(p.getLM(), lM, Utils.epsilonAngle * Math.abs(lM));
    }

    public void testPositionVelocityNorms() {

        // elliptic and non equatorial (i retrograde) orbit
        EquinoctialOrbit p =
            new EquinoctialOrbit(42166.712, 0.5, -0.5, 1.200, 2.1,
                                      0.67, EquinoctialOrbit.TRUE_LATITUDE_ARGUMENT, 
                                      Frame.getJ2000(), date, mu);

        double ex = p.getEquinoctialEx();
        double ey = p.getEquinoctialEy();
        double lv = p.getLv();
        double ksi = 1 + ex * Math.cos(lv) + ey * Math.sin(lv);
        double nu = ex * Math.sin(lv) - ey * Math.cos(lv);
        double epsilon = Math.sqrt(1 - ex * ex - ey * ey);

        double a = p.getA();
        double na = Math.sqrt(p.getMu() / a);

        assertEquals(a * epsilon * epsilon / ksi, p.getPVCoordinates().getPosition().getNorm(),
                     Utils.epsilonTest * Math.abs(p.getPVCoordinates().getPosition().getNorm()));
        assertEquals(na * Math.sqrt(ksi * ksi + nu * nu) / epsilon, p
                     .getPVCoordinates().getVelocity().getNorm(), Utils.epsilonTest
                     * Math.abs(p.getPVCoordinates().getVelocity().getNorm()));

        // circular and equatorial orbit
        EquinoctialOrbit pCirEqua =
            new EquinoctialOrbit(42166.712, 0.1e-8, 0.1e-8, 0.1e-8, 0.1e-8,
                                      0.67, EquinoctialOrbit.TRUE_LATITUDE_ARGUMENT, 
                                      Frame.getJ2000(), date, mu);

        ex = pCirEqua.getEquinoctialEx();
        ey = pCirEqua.getEquinoctialEy();
        lv = pCirEqua.getLv();
        ksi = 1 + ex * Math.cos(lv) + ey * Math.sin(lv);
        nu = ex * Math.sin(lv) - ey * Math.cos(lv);
        epsilon = Math.sqrt(1 - ex * ex - ey * ey);

        a = pCirEqua.getA();
        na = Math.sqrt(pCirEqua.getMu() / a);

        assertEquals(a * epsilon * epsilon / ksi, pCirEqua.getPVCoordinates().getPosition()
                     .getNorm(), Utils.epsilonTest
                     * Math.abs(pCirEqua.getPVCoordinates().getPosition().getNorm()));
        assertEquals(na * Math.sqrt(ksi * ksi + nu * nu) / epsilon, pCirEqua
                     .getPVCoordinates().getVelocity().getNorm(), Utils.epsilonTest
                     * Math.abs(pCirEqua.getPVCoordinates().getVelocity().getNorm()));
    }

    public void testGeometry() {
 
        // elliptic and non equatorial (i retrograde) orbit
        EquinoctialOrbit p =
            new EquinoctialOrbit(42166.712, 0.5, -0.5, 1.200, 2.1,
                                      0.67, EquinoctialOrbit.TRUE_LATITUDE_ARGUMENT, 
                                      Frame.getJ2000(), date, mu);

        Vector3D position = p.getPVCoordinates().getPosition();
        Vector3D velocity = p.getPVCoordinates().getVelocity();

        Vector3D momentum = Vector3D.crossProduct(position, velocity).normalize();

        double apogeeRadius = p.getA() * (1 + p.getE());
        double perigeeRadius = p.getA() * (1 - p.getE());

        for (double lv = 0; lv <= 2 * Math.PI; lv += 2 * Math.PI / 100.) {
//          p.setLv(lv);
            p = new EquinoctialOrbit(p.getA() ,p.getEquinoctialEx(),
                                          p.getEquinoctialEy() , p.getHx(), p.getHy() , lv , 2, 
                                          p.getFrame(), p.getDate(), p.getMu());
            position = p.getPVCoordinates().getPosition();

            // test if the norm of the position is in the range [perigee radius,
            // apogee radius]
            // Warning: these tests are without absolute value by choice
            assertTrue((position.getNorm() - apogeeRadius) <= (apogeeRadius * Utils.epsilonTest));
            assertTrue((position.getNorm() - perigeeRadius) >= (-perigeeRadius * Utils.epsilonTest));

            position = position.normalize();
            velocity = p.getPVCoordinates().getVelocity();
            velocity = velocity.normalize();

            // at this stage of computation, all the vectors (position, velocity and
            // momemtum) are normalized here

            // test of orthogonality between position and momentum
            assertTrue(Math.abs(Vector3D.dotProduct(position, momentum)) < Utils.epsilonTest);
            // test of orthogonality between velocity and momentum
            assertTrue(Math.abs(Vector3D.dotProduct(velocity, momentum)) < Utils.epsilonTest);
        }

        // circular and equatorial orbit
        EquinoctialOrbit pCirEqua =
            new EquinoctialOrbit(42166.712, 0.1e-8, 0.1e-8, 0.1e-8, 0.1e-8,
                                      0.67, EquinoctialOrbit.TRUE_LATITUDE_ARGUMENT, 
                                      Frame.getJ2000(), date, mu);

        position = pCirEqua.getPVCoordinates().getPosition();
        velocity = pCirEqua.getPVCoordinates().getVelocity();

        momentum = Vector3D.crossProduct(position, velocity).normalize();

        apogeeRadius = pCirEqua.getA() * (1 + pCirEqua.getE());
        perigeeRadius = pCirEqua.getA() * (1 - pCirEqua.getE());
        // test if apogee equals perigee
        assertEquals(perigeeRadius, apogeeRadius, 1.e+4 * Utils.epsilonTest
                     * apogeeRadius);

        for (double lv = 0; lv <= 2 * Math.PI; lv += 2 * Math.PI / 100.) {
//          pCirEqua.setLv(lv);
            pCirEqua = new EquinoctialOrbit(pCirEqua.getA() ,pCirEqua.getEquinoctialEx(),
                                                 pCirEqua.getEquinoctialEy() , pCirEqua.getHx(), pCirEqua.getHy() , lv , 2, 
                                                 pCirEqua.getFrame(), p.getDate(), p.getMu());
            position = pCirEqua.getPVCoordinates().getPosition();

            // test if the norm pf the position is in the range [perigee radius,
            // apogee radius]
            assertTrue((position.getNorm() - apogeeRadius) <= (apogeeRadius * Utils.epsilonTest));
            assertTrue((position.getNorm() - perigeeRadius) >= (-perigeeRadius * Utils.epsilonTest));

            position = position.normalize();
            velocity = pCirEqua.getPVCoordinates().getVelocity();
            velocity = velocity.normalize();

            // at this stage of computation, all the vectors (position, velocity and
            // momemtum) are normalized here

            // test of orthogonality between position and momentum
            assertTrue(Math.abs(Vector3D.dotProduct(position, momentum)) < Utils.epsilonTest);
            // test of orthogonality between velocity and momentum
            assertTrue(Math.abs(Vector3D.dotProduct(velocity, momentum)) < Utils.epsilonTest);
        }
    }

    public void testSymmetry() {

        // elliptic and non equatorial orbit
        Vector3D position = new Vector3D(4512.9, 18260., -5127.);
        Vector3D velocity = new Vector3D(134664.6, 90066.8, 72047.6);

        EquinoctialOrbit p = new EquinoctialOrbit(new PVCoordinates(position, velocity), 
                                                  Frame.getJ2000(), date, mu);

        Vector3D positionOffset = p.getPVCoordinates().getPosition().subtract(position);
        Vector3D velocityOffset = p.getPVCoordinates().getVelocity().subtract(velocity);

        assertTrue(positionOffset.getNorm() < Utils.epsilonTest);
        assertTrue(velocityOffset.getNorm() < Utils.epsilonTest);

        // circular and equatorial orbit
        position = new Vector3D(33051.2, 26184.9, -1.3E-5);
        velocity = new Vector3D(-60376.2, 76208., 2.7E-4);

        p = new EquinoctialOrbit(new PVCoordinates(position, velocity), 
                                 Frame.getJ2000(), date, mu);

        positionOffset = p.getPVCoordinates().getPosition().subtract(position);
        velocityOffset = p.getPVCoordinates().getVelocity().subtract(velocity);

        assertTrue(positionOffset.getNorm() < Utils.epsilonTest);
        assertTrue(velocityOffset.getNorm() < Utils.epsilonTest);
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
        return new TestSuite(EquinoctialParametersTest.class);
    }

}
