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
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.PVCoordinates;


//$Id:KeplerianParametersTest.java 1665 2008-06-11 10:12:59Z luc $
public class KeplerianParametersTest extends TestCase {

    // Computation date 
    private AbsoluteDate date;
    
   // Body mu 
    private double mu;

    public KeplerianParametersTest(String name) {
        super(name);
    }

    public void testKeplerianToKeplerian() {

        // elliptic orbit
        KeplerianOrbit kep =
            new KeplerianOrbit(24464560.0, 0.7311, 0.122138, 3.10686, 1.00681,
                                    0.048363, KeplerianOrbit.MEAN_ANOMALY, 
                                    Frame.getJ2000(), date, mu);

        Vector3D pos = kep.getPVCoordinates().getPosition();
        Vector3D vit = kep.getPVCoordinates().getVelocity();

        KeplerianOrbit param = new KeplerianOrbit(new PVCoordinates(pos,vit), 
                                                  Frame.getJ2000(), date, mu);
        assertEquals(param.getA(), kep.getA(), Utils.epsilonTest * kep.getA());
        assertEquals(param.getE(), kep.getE(), Utils.epsilonE * Math.abs(kep.getE()));
        assertEquals(MathUtils.normalizeAngle(param.getI(), kep.getI()), kep.getI(), Utils.epsilonAngle * Math.abs(kep.getI()));
        assertEquals(MathUtils.normalizeAngle(param.getPerigeeArgument(), kep.getPerigeeArgument()), kep.getPerigeeArgument(), Utils.epsilonAngle * Math.abs(kep.getPerigeeArgument()));
        assertEquals(MathUtils.normalizeAngle(param.getRightAscensionOfAscendingNode(), kep.getRightAscensionOfAscendingNode()), kep.getRightAscensionOfAscendingNode(), Utils.epsilonAngle * Math.abs(kep.getRightAscensionOfAscendingNode()));
        assertEquals(MathUtils.normalizeAngle(param.getMeanAnomaly(), kep.getMeanAnomaly()), kep.getMeanAnomaly(), Utils.epsilonAngle * Math.abs(kep.getMeanAnomaly()));

        // circular orbit
        KeplerianOrbit kepCir =
            new KeplerianOrbit(24464560.0, 0.0, 0.122138, 3.10686, 1.00681,
                                    0.048363, KeplerianOrbit.MEAN_ANOMALY, 
                                    Frame.getJ2000(), date, mu);

        Vector3D posCir = kepCir.getPVCoordinates().getPosition();
        Vector3D vitCir = kepCir.getPVCoordinates().getVelocity();

        KeplerianOrbit paramCir = new KeplerianOrbit(new PVCoordinates(posCir,vitCir),  
                                                     Frame.getJ2000(), date, mu);
        assertEquals(paramCir.getA(), kepCir.getA(), Utils.epsilonTest * kepCir.getA());
        assertEquals(paramCir.getE(), kepCir.getE(), Utils.epsilonE * Math.max(1.,Math.abs(kepCir.getE())));
        assertEquals(MathUtils.normalizeAngle(paramCir.getI(), kepCir.getI()), kepCir.getI(), Utils.epsilonAngle * Math.abs(kepCir.getI()));
        assertEquals(MathUtils.normalizeAngle(paramCir.getLM(), kepCir.getLM()), kepCir.getLM(), Utils.epsilonAngle * Math.abs(kepCir.getLM()));
        assertEquals(MathUtils.normalizeAngle(paramCir.getLE(), kepCir.getLE()), kepCir.getLE(), Utils.epsilonAngle * Math.abs(kepCir.getLE()));
        assertEquals(MathUtils.normalizeAngle(paramCir.getLv(), kepCir.getLv()), kepCir.getLv(), Utils.epsilonAngle * Math.abs(kepCir.getLv()));

    }

    public void testKeplerianToCartesian() {

        KeplerianOrbit kep =
            new KeplerianOrbit(24464560.0, 0.7311, 0.122138, 3.10686, 1.00681,
                                    0.048363, KeplerianOrbit.MEAN_ANOMALY, 
                                    Frame.getJ2000(), date, mu);

        Vector3D pos = kep.getPVCoordinates().getPosition();
        Vector3D vit = kep.getPVCoordinates().getVelocity();
        assertEquals(-0.107622532467967e+07, pos.getX(), Utils.epsilonTest * Math.abs(pos.getX()));
        assertEquals(-0.676589636432773e+07, pos.getY(), Utils.epsilonTest * Math.abs(pos.getY()));
        assertEquals(-0.332308783350379e+06, pos.getZ(), Utils.epsilonTest * Math.abs(pos.getZ()));

        assertEquals( 0.935685775154103e+04, vit.getX(), Utils.epsilonTest * Math.abs(vit.getX()));
        assertEquals(-0.331234775037644e+04, vit.getY(), Utils.epsilonTest * Math.abs(vit.getY()));
        assertEquals(-0.118801577532701e+04, vit.getZ(), Utils.epsilonTest * Math.abs(vit.getZ()));
    }

    public void testKeplerianToEquinoctial() {

        KeplerianOrbit kep =
            new KeplerianOrbit(24464560.0, 0.7311, 0.122138, 3.10686, 1.00681,
                                    0.048363, KeplerianOrbit.MEAN_ANOMALY, 
                                    Frame.getJ2000(), date, mu);

        assertEquals(24464560.0, kep.getA(), Utils.epsilonTest * kep.getA());
        assertEquals(-0.412036802887626, kep.getEquinoctialEx(), Utils.epsilonE * Math.abs(kep.getE()));
        assertEquals(-0.603931190671706, kep.getEquinoctialEy(), Utils.epsilonE * Math.abs(kep.getE()));
        assertEquals(MathUtils.normalizeAngle(2*Math.asin(Math.sqrt((Math.pow(0.652494417368829e-01,2)+Math.pow(0.103158450084864,2))/4.)),kep.getI()), kep.getI(), Utils.epsilonAngle * Math.abs(kep.getI()));
        assertEquals(MathUtils.normalizeAngle(0.416203300000000e+01,kep.getLM()), kep.getLM(),Utils.epsilonAngle * Math.abs(kep.getLM()));

    }

    public void testAnomaly() {

        Vector3D position = new Vector3D(7.0e6, 1.0e6, 4.0e6);
        Vector3D velocity = new Vector3D(-500.0, 8000.0, 1000.0);
        double mu = 3.9860047e14;

        KeplerianOrbit p = new KeplerianOrbit(new PVCoordinates(position, velocity), 
                                              Frame.getJ2000(), date, mu);

        // elliptic orbit
        double e = p.getE();
        double eRatio = Math.sqrt((1 - e) / (1 + e));

        double v = 1.1;
        // formulations for elliptic case
        double E = 2 * Math.atan(eRatio * Math.tan(v / 2));
        double M = E - e * Math.sin(E);

//      p.setTrueAnomaly(v);
        p = new KeplerianOrbit(p.getA(),p.getE(), p.getI(), p.getPerigeeArgument(),
                                    p.getRightAscensionOfAscendingNode(), v , 2, 
                                    p.getFrame(), p.getDate(), p.getMu());
        assertEquals(p.getTrueAnomaly(), v, Utils.epsilonAngle * Math.abs(v));
        assertEquals(p.getEccentricAnomaly(), E, Utils.epsilonAngle * Math.abs(E));
        assertEquals(p.getMeanAnomaly(), M, Utils.epsilonAngle * Math.abs(M));
//      p.setTrueAnomaly(0);
        p = new KeplerianOrbit(p.getA(),p.getE(), p.getI(), p.getPerigeeArgument(),
                                    p.getRightAscensionOfAscendingNode(), 0 , 2, 
                                    p.getFrame(), p.getDate(), p.getMu());

//      p.setEccentricAnomaly(E);
        p = new KeplerianOrbit(p.getA(),p.getE(), p.getI(), p.getPerigeeArgument(),
                                    p.getRightAscensionOfAscendingNode(), E , 1, 
                                    p.getFrame(), p.getDate(), p.getMu());
        assertEquals(p.getTrueAnomaly(), v, Utils.epsilonAngle * Math.abs(v));
        assertEquals(p.getEccentricAnomaly(), E, Utils.epsilonAngle * Math.abs(E));
        assertEquals(p.getMeanAnomaly(), M, Utils.epsilonAngle * Math.abs(M));
//      p.setTrueAnomaly(0);
        p = new KeplerianOrbit(p.getA(),p.getE(), p.getI(), p.getPerigeeArgument(),
                                    p.getRightAscensionOfAscendingNode(), 0 , 2, 
                                    p.getFrame(), p.getDate(), p.getMu());

//      p.setMeanAnomaly(M);
        p = new KeplerianOrbit(p.getA(),p.getE(), p.getI(), p.getPerigeeArgument(),
                                    p.getRightAscensionOfAscendingNode(), M , 0, 
                                    p.getFrame(), p.getDate(), p.getMu());
        assertEquals(p.getTrueAnomaly(), v, Utils.epsilonAngle * Math.abs(v));
        assertEquals(p.getEccentricAnomaly(), E, Utils.epsilonAngle * Math.abs(E));
        assertEquals(p.getMeanAnomaly(), M, Utils.epsilonAngle * Math.abs(M));

        // circular orbit
//      p.setE(0);
        p = new KeplerianOrbit(p.getA(),0, p.getI(), p.getPerigeeArgument(),
                                    p.getRightAscensionOfAscendingNode(), p.getLv() , 2, 
                                    p.getFrame(), p.getDate(), p.getMu());

        E = v;
        M = E;

//      p.setTrueAnomaly(v);
        p = new KeplerianOrbit(p.getA(),p.getE(), p.getI(), p.getPerigeeArgument(),
                                    p.getRightAscensionOfAscendingNode(), v , 2, 
                                    p.getFrame(), p.getDate(), p.getMu());
        assertEquals(p.getTrueAnomaly(), v, Utils.epsilonAngle * Math.abs(v));
        assertEquals(p.getEccentricAnomaly(), E, Utils.epsilonAngle * Math.abs(E));
        assertEquals(p.getMeanAnomaly(), M, Utils.epsilonAngle * Math.abs(M));
//      p.setTrueAnomaly(0);
        p = new KeplerianOrbit(p.getA(),p.getE(), p.getI(), p.getPerigeeArgument(),
                                    p.getRightAscensionOfAscendingNode(), 0 , 2, 
                                    p.getFrame(), p.getDate(), p.getMu());

//      p.setEccentricAnomaly(E);
        p = new KeplerianOrbit(p.getA(),p.getE(), p.getI(), p.getPerigeeArgument(),
                                    p.getRightAscensionOfAscendingNode(), E , 1, p.getFrame(), p.getDate(), p.getMu());
        assertEquals(p.getTrueAnomaly(), v, Utils.epsilonAngle * Math.abs(v));
        assertEquals(p.getEccentricAnomaly(), E, Utils.epsilonAngle * Math.abs(E));
        assertEquals(p.getMeanAnomaly(), M, Utils.epsilonAngle * Math.abs(M));
//      p.setTrueAnomaly(0);
        p = new KeplerianOrbit(p.getA(),p.getE(), p.getI(), p.getPerigeeArgument(),
                                    p.getRightAscensionOfAscendingNode(), 0 , 2, 
                                    p.getFrame(), p.getDate(), p.getMu());

//      p.setMeanAnomaly(M);
        p = new KeplerianOrbit(p.getA(),p.getE(), p.getI(), p.getPerigeeArgument(),
                                    p.getRightAscensionOfAscendingNode(), M , 0, 
                                    p.getFrame(), p.getDate(), p.getMu());
        assertEquals(p.getTrueAnomaly(), v, Utils.epsilonAngle * Math.abs(v));
        assertEquals(p.getEccentricAnomaly(), E, Utils.epsilonAngle * Math.abs(E));
        assertEquals(p.getMeanAnomaly(), M, Utils.epsilonAngle * Math.abs(M));

    }

    public void testPositionVelocityNorms() {
        double mu = 3.9860047e14;

        // elliptic and non equatorial orbit
        KeplerianOrbit p =
            new KeplerianOrbit(24464560.0, 0.7311, 2.1, 3.10686, 1.00681,
                                    0.67, KeplerianOrbit.TRUE_ANOMALY, 
                                    Frame.getJ2000(), date, mu);

        double e       = p.getE();
        double v       = p.getTrueAnomaly();
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


        //  circular and equatorial orbit
        KeplerianOrbit pCirEqua =
            new KeplerianOrbit(24464560.0, 0.1e-10, 0.1e-8, 3.10686, 1.00681,
                                    0.67, KeplerianOrbit.TRUE_ANOMALY, 
                                    Frame.getJ2000(), date, mu);

        e       = pCirEqua.getE();
        v       = pCirEqua.getTrueAnomaly();
        ksi     = 1 + e * Math.cos(v);
        nu      = e * Math.sin(v);
        epsilon = Math.sqrt((1 - e) * (1 + e));

        a  = pCirEqua.getA();
        na = Math.sqrt(mu / a);

        // validation of: r = a .(1 - e2) / (1 + e.cos(v))
        assertEquals(a * epsilon * epsilon / ksi,
                     pCirEqua.getPVCoordinates().getPosition().getNorm(),
                     Utils.epsilonTest * Math.abs(pCirEqua.getPVCoordinates().getPosition().getNorm()));

        // validation of: V = sqrt(mu.(1+2e.cos(v)+e2)/a.(1-e2) )
        assertEquals(na * Math.sqrt(ksi * ksi + nu * nu) / epsilon,
                     pCirEqua.getPVCoordinates().getVelocity().getNorm(),
                     Utils.epsilonTest * Math.abs(pCirEqua.getPVCoordinates().getVelocity().getNorm()));
    }

    public void testGeometry() {
        double mu = 3.9860047e14;

        // elliptic and non equatorial orbit
        KeplerianOrbit p =
            new KeplerianOrbit(24464560.0, 0.7311, 2.1, 3.10686, 1.00681,
                                    0.67, KeplerianOrbit.TRUE_ANOMALY, 
                                    Frame.getJ2000(), date, mu);

        Vector3D position = p.getPVCoordinates().getPosition();
        Vector3D velocity = p.getPVCoordinates().getVelocity();
        Vector3D momentum = Vector3D.crossProduct(position,velocity).normalize();

        double apogeeRadius  = p.getA() * (1 + p.getE());
        double perigeeRadius = p.getA() * (1 - p.getE());

        for (double lv = 0; lv <= 2 * Math.PI; lv += 2 * Math.PI/100.) {
//          p.setTrueAnomaly(lv);
            p = new KeplerianOrbit(p.getA(),p.getE(), p.getI(), p.getPerigeeArgument(),
                                        p.getRightAscensionOfAscendingNode(), lv , 2, 
                                        p.getFrame(), p.getDate(), p.getMu());
            position = p.getPVCoordinates().getPosition();

            // test if the norm of the position is in the range [perigee radius, apogee radius]
            assertTrue((position.getNorm() - apogeeRadius)  <= (  apogeeRadius * Utils.epsilonTest));
            assertTrue((position.getNorm() - perigeeRadius) >= (- perigeeRadius * Utils.epsilonTest));

            position = position.normalize();
            velocity = p.getPVCoordinates().getVelocity();
            velocity = velocity.normalize();

            // at this stage of computation, all the vectors (position, velocity and momemtum) are normalized here

            // test of orthogonality between position and momentum
            assertTrue(Math.abs(Vector3D.dotProduct(position, momentum)) < Utils.epsilonTest);
            // test of orthogonality between velocity and momentum
            assertTrue(Math.abs(Vector3D.dotProduct(velocity, momentum)) < Utils.epsilonTest);

        }

        // apsides
//      p.setTrueAnomaly(0);
        p = new KeplerianOrbit(p.getA(),p.getE(), p.getI(), p.getPerigeeArgument(),
                                    p.getRightAscensionOfAscendingNode(), 0 , 2, p.getFrame(), p.getDate(), p.getMu());
        assertEquals(p.getPVCoordinates().getPosition().getNorm(), perigeeRadius, perigeeRadius * Utils.epsilonTest);

//      p.setTrueAnomaly(Math.PI);
        p = new KeplerianOrbit(p.getA(),p.getE(), p.getI(), p.getPerigeeArgument(),
                                    p.getRightAscensionOfAscendingNode(), Math.PI , 2, p.getFrame(), p.getDate(), p.getMu());
        assertEquals(p.getPVCoordinates().getPosition().getNorm(), apogeeRadius, apogeeRadius * Utils.epsilonTest);

        // nodes
        // descending node
//      p.setTrueAnomaly(Math.PI - p.getPerigeeArgument());
        p = new KeplerianOrbit(p.getA(),p.getE(), p.getI(), p.getPerigeeArgument(),
                                    p.getRightAscensionOfAscendingNode(), Math.PI - p.getPerigeeArgument() , 2, 
                                    p.getFrame(), p.getDate(), p.getMu());
        assertTrue(Math.abs(p.getPVCoordinates().getPosition().getZ()) < p.getPVCoordinates().getPosition().getNorm() * Utils.epsilonTest);
        assertTrue(p.getPVCoordinates().getVelocity().getZ() < 0);

        // ascending node
//      p.setTrueAnomaly(2.0 * Math.PI - p.getPerigeeArgument());
        p = new KeplerianOrbit(p.getA(),p.getE(), p.getI(), p.getPerigeeArgument(),
                                    p.getRightAscensionOfAscendingNode(),2.0 * Math.PI - p.getPerigeeArgument() , 2, 
                                    p.getFrame(), p.getDate(), p.getMu());
        assertTrue(Math.abs(p.getPVCoordinates().getPosition().getZ()) < p.getPVCoordinates().getPosition().getNorm() * Utils.epsilonTest);
        assertTrue(p.getPVCoordinates().getVelocity().getZ() > 0);


        //  circular and equatorial orbit
        KeplerianOrbit pCirEqua =
            new KeplerianOrbit(24464560.0, 0.1e-10, 0.1e-8, 3.10686, 1.00681,
                                    0.67, KeplerianOrbit.TRUE_ANOMALY, Frame.getJ2000(), date, mu);

        position = pCirEqua.getPVCoordinates().getPosition();
        velocity = pCirEqua.getPVCoordinates().getVelocity();
        momentum = Vector3D.crossProduct(position,velocity).normalize();

        apogeeRadius  = pCirEqua.getA() * (1 + pCirEqua.getE());
        perigeeRadius = pCirEqua.getA() * (1 - pCirEqua.getE());
        // test if apogee equals perigee
        assertEquals(perigeeRadius, apogeeRadius, 1.e+4 * Utils.epsilonTest * apogeeRadius);

        for (double lv = 0; lv <= 2 * Math.PI; lv += 2 * Math.PI/100.) {
//          pCirEqua.setTrueAnomaly(lv)
            pCirEqua = new KeplerianOrbit(pCirEqua.getA(),pCirEqua.getE(),pCirEqua.getI(), pCirEqua.getPerigeeArgument(),
                                               pCirEqua.getRightAscensionOfAscendingNode(), lv, 2, 
                                               pCirEqua.getFrame(), pCirEqua.getDate(), pCirEqua.getMu());
            position = pCirEqua.getPVCoordinates().getPosition();

            // test if the norm pf the position is in the range [perigee radius, apogee radius]
            // Warning: these tests are without absolute value by choice
            assertTrue((position.getNorm() - apogeeRadius)  <= (  apogeeRadius * Utils.epsilonTest));
            assertTrue((position.getNorm() - perigeeRadius) >= (- perigeeRadius * Utils.epsilonTest));

            position = position.normalize();
            velocity = pCirEqua.getPVCoordinates().getVelocity();
            velocity = velocity.normalize();

            // at this stage of computation, all the vectors (position, velocity and momemtum) are normalized here

            // test of orthogonality between position and momentum
            assertTrue(Math.abs(Vector3D.dotProduct(position, momentum)) < Utils.epsilonTest);
            // test of orthogonality between velocity and momentum
            assertTrue(Math.abs(Vector3D.dotProduct(velocity, momentum)) < Utils.epsilonTest);

        }
    }

    public void testSymmetry() {

        // elliptic and non equatorail orbit
        Vector3D position = new Vector3D(-4947831., -3765382., -3708221.);
        Vector3D velocity = new Vector3D(-2079., 5291., -7842.);
        double mu = 3.9860047e14;

        KeplerianOrbit p = new KeplerianOrbit(new PVCoordinates(position, velocity),  
                                              Frame.getJ2000(), date, mu);
        Vector3D positionOffset = p.getPVCoordinates().getPosition().subtract(position);
        Vector3D velocityOffset = p.getPVCoordinates().getVelocity().subtract(velocity);

        assertTrue(positionOffset.getNorm() < Utils.epsilonTest);
        assertTrue(velocityOffset.getNorm() < Utils.epsilonTest);

        // circular and equatorial orbit
        position = new Vector3D(1742382., -2.440243e7, -0.014517);
        velocity = new Vector3D(4026.2, 287.479, -3.e-6);


        p = new KeplerianOrbit(new PVCoordinates(position, velocity),  
                               Frame.getJ2000(), date, mu);
        positionOffset = p.getPVCoordinates().getPosition().subtract(position);
        velocityOffset = p.getPVCoordinates().getVelocity().subtract(velocity);

        assertTrue(positionOffset.getNorm() < Utils.epsilonTest);
        assertTrue(velocityOffset.getNorm() < Utils.epsilonTest);

    }

    public void testPeriod() {
        KeplerianOrbit orbit = new KeplerianOrbit(7654321.0, 0.1, 0.2, 0, 0, 0,
                                                  KeplerianOrbit.TRUE_ANOMALY,
                                                  Frame.getJ2000(), AbsoluteDate.J2000_EPOCH,
                                                  mu);
        assertEquals(6664.5521723383589487, orbit.getKeplerianPeriod(), 1.0e-12);
        assertEquals(0.00094277682051291315229, orbit.getKeplerianMeanMotion(), 1.0e-16);
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
        return new TestSuite(KeplerianParametersTest.class);
    }
}
