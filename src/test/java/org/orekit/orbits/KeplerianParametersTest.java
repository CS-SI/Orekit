/* Copyright 2002-2010 CS Communication & Systèmes
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


//$Id:KeplerianParametersTest.java 1665 2008-06-11 10:12:59Z luc $
public class KeplerianParametersTest {

    // Computation date 
    private AbsoluteDate date;
    
   // Body mu 
    private double mu;

    @Test
    public void testKeplerianToKeplerian() {

        // elliptic orbit
        KeplerianOrbit kep =
            new KeplerianOrbit(24464560.0, 0.7311, 0.122138, 3.10686, 1.00681,
                                    0.048363, KeplerianOrbit.MEAN_ANOMALY, 
                                    FramesFactory.getEME2000(), date, mu);

        Vector3D pos = kep.getPVCoordinates().getPosition();
        Vector3D vit = kep.getPVCoordinates().getVelocity();

        KeplerianOrbit param = new KeplerianOrbit(new PVCoordinates(pos,vit), 
                                                  FramesFactory.getEME2000(), date, mu);
        Assert.assertEquals(param.getA(), kep.getA(), Utils.epsilonTest * kep.getA());
        Assert.assertEquals(param.getE(), kep.getE(), Utils.epsilonE * FastMath.abs(kep.getE()));
        Assert.assertEquals(MathUtils.normalizeAngle(param.getI(), kep.getI()), kep.getI(), Utils.epsilonAngle * FastMath.abs(kep.getI()));
        Assert.assertEquals(MathUtils.normalizeAngle(param.getPerigeeArgument(), kep.getPerigeeArgument()), kep.getPerigeeArgument(), Utils.epsilonAngle * FastMath.abs(kep.getPerigeeArgument()));
        Assert.assertEquals(MathUtils.normalizeAngle(param.getRightAscensionOfAscendingNode(), kep.getRightAscensionOfAscendingNode()), kep.getRightAscensionOfAscendingNode(), Utils.epsilonAngle * FastMath.abs(kep.getRightAscensionOfAscendingNode()));
        Assert.assertEquals(MathUtils.normalizeAngle(param.getMeanAnomaly(), kep.getMeanAnomaly()), kep.getMeanAnomaly(), Utils.epsilonAngle * FastMath.abs(kep.getMeanAnomaly()));

        // circular orbit
        KeplerianOrbit kepCir =
            new KeplerianOrbit(24464560.0, 0.0, 0.122138, 3.10686, 1.00681,
                                    0.048363, KeplerianOrbit.MEAN_ANOMALY, 
                                    FramesFactory.getEME2000(), date, mu);

        Vector3D posCir = kepCir.getPVCoordinates().getPosition();
        Vector3D vitCir = kepCir.getPVCoordinates().getVelocity();

        KeplerianOrbit paramCir = new KeplerianOrbit(new PVCoordinates(posCir,vitCir),  
                                                     FramesFactory.getEME2000(), date, mu);
        Assert.assertEquals(paramCir.getA(), kepCir.getA(), Utils.epsilonTest * kepCir.getA());
        Assert.assertEquals(paramCir.getE(), kepCir.getE(), Utils.epsilonE * FastMath.max(1.,FastMath.abs(kepCir.getE())));
        Assert.assertEquals(MathUtils.normalizeAngle(paramCir.getI(), kepCir.getI()), kepCir.getI(), Utils.epsilonAngle * FastMath.abs(kepCir.getI()));
        Assert.assertEquals(MathUtils.normalizeAngle(paramCir.getLM(), kepCir.getLM()), kepCir.getLM(), Utils.epsilonAngle * FastMath.abs(kepCir.getLM()));
        Assert.assertEquals(MathUtils.normalizeAngle(paramCir.getLE(), kepCir.getLE()), kepCir.getLE(), Utils.epsilonAngle * FastMath.abs(kepCir.getLE()));
        Assert.assertEquals(MathUtils.normalizeAngle(paramCir.getLv(), kepCir.getLv()), kepCir.getLv(), Utils.epsilonAngle * FastMath.abs(kepCir.getLv()));

        // hyperbolic orbit
        KeplerianOrbit kepHyp =
            new KeplerianOrbit(-24464560.0, 1.7311, 0.122138, 3.10686, 1.00681,
                                    0.048363, KeplerianOrbit.MEAN_ANOMALY, 
                                    FramesFactory.getEME2000(), date, mu);

        Vector3D posHyp = kepHyp.getPVCoordinates().getPosition();
        Vector3D vitHyp = kepHyp.getPVCoordinates().getVelocity();

        KeplerianOrbit paramHyp = new KeplerianOrbit(new PVCoordinates(posHyp,vitHyp), 
                                                  FramesFactory.getEME2000(), date, mu);
        Assert.assertEquals(paramHyp.getA(), kepHyp.getA(), Utils.epsilonTest * FastMath.abs(kepHyp.getA()));
        Assert.assertEquals(paramHyp.getE(), kepHyp.getE(), Utils.epsilonE * FastMath.abs(kepHyp.getE()));
        Assert.assertEquals(MathUtils.normalizeAngle(paramHyp.getI(), kepHyp.getI()), kepHyp.getI(), Utils.epsilonAngle * FastMath.abs(kepHyp.getI()));
        Assert.assertEquals(MathUtils.normalizeAngle(paramHyp.getPerigeeArgument(), kepHyp.getPerigeeArgument()), kepHyp.getPerigeeArgument(), Utils.epsilonAngle * FastMath.abs(kepHyp.getPerigeeArgument()));
        Assert.assertEquals(MathUtils.normalizeAngle(paramHyp.getRightAscensionOfAscendingNode(), kepHyp.getRightAscensionOfAscendingNode()), kepHyp.getRightAscensionOfAscendingNode(), Utils.epsilonAngle * FastMath.abs(kepHyp.getRightAscensionOfAscendingNode()));
        Assert.assertEquals(MathUtils.normalizeAngle(paramHyp.getMeanAnomaly(), kepHyp.getMeanAnomaly()), kepHyp.getMeanAnomaly(), Utils.epsilonAngle * FastMath.abs(kepHyp.getMeanAnomaly()));

    }

    public void testKeplerianToCartesian() {

        KeplerianOrbit kep =
            new KeplerianOrbit(24464560.0, 0.7311, 0.122138, 3.10686, 1.00681,
                                    0.048363, KeplerianOrbit.MEAN_ANOMALY, 
                                    FramesFactory.getEME2000(), date, mu);

        Vector3D pos = kep.getPVCoordinates().getPosition();
        Vector3D vit = kep.getPVCoordinates().getVelocity();
        Assert.assertEquals(-0.107622532467967e+07, pos.getX(), Utils.epsilonTest * FastMath.abs(pos.getX()));
        Assert.assertEquals(-0.676589636432773e+07, pos.getY(), Utils.epsilonTest * FastMath.abs(pos.getY()));
        Assert.assertEquals(-0.332308783350379e+06, pos.getZ(), Utils.epsilonTest * FastMath.abs(pos.getZ()));

        Assert.assertEquals( 0.935685775154103e+04, vit.getX(), Utils.epsilonTest * FastMath.abs(vit.getX()));
        Assert.assertEquals(-0.331234775037644e+04, vit.getY(), Utils.epsilonTest * FastMath.abs(vit.getY()));
        Assert.assertEquals(-0.118801577532701e+04, vit.getZ(), Utils.epsilonTest * FastMath.abs(vit.getZ()));
    }

    @Test
    public void testKeplerianToEquinoctial() {

        KeplerianOrbit kep =
            new KeplerianOrbit(24464560.0, 0.7311, 0.122138, 3.10686, 1.00681,
                                    0.048363, KeplerianOrbit.MEAN_ANOMALY, 
                                    FramesFactory.getEME2000(), date, mu);

        Assert.assertEquals(24464560.0, kep.getA(), Utils.epsilonTest * kep.getA());
        Assert.assertEquals(-0.412036802887626, kep.getEquinoctialEx(), Utils.epsilonE * FastMath.abs(kep.getE()));
        Assert.assertEquals(-0.603931190671706, kep.getEquinoctialEy(), Utils.epsilonE * FastMath.abs(kep.getE()));
        Assert.assertEquals(MathUtils.normalizeAngle(2*FastMath.asin(FastMath.sqrt((FastMath.pow(0.652494417368829e-01,2)+FastMath.pow(0.103158450084864,2))/4.)),kep.getI()), kep.getI(), Utils.epsilonAngle * FastMath.abs(kep.getI()));
        Assert.assertEquals(MathUtils.normalizeAngle(0.416203300000000e+01,kep.getLM()), kep.getLM(),Utils.epsilonAngle * FastMath.abs(kep.getLM()));

    }

    @Test
    public void testAnomaly() {

        Vector3D position = new Vector3D(7.0e6, 1.0e6, 4.0e6);
        Vector3D velocity = new Vector3D(-500.0, 8000.0, 1000.0);
        double mu = 3.9860047e14;

        KeplerianOrbit p = new KeplerianOrbit(new PVCoordinates(position, velocity), 
                                              FramesFactory.getEME2000(), date, mu);

        // elliptic orbit
        double e = p.getE();
        double eRatio = FastMath.sqrt((1 - e) / (1 + e));

        double v = 1.1;
        // formulations for elliptic case
        double E = 2 * FastMath.atan(eRatio * FastMath.tan(v / 2));
        double M = E - e * FastMath.sin(E);

//      p.setTrueAnomaly(v);
        p = new KeplerianOrbit(p.getA(),p.getE(), p.getI(), p.getPerigeeArgument(),
                                    p.getRightAscensionOfAscendingNode(), v , 2, 
                                    p.getFrame(), p.getDate(), p.getMu());
        Assert.assertEquals(p.getTrueAnomaly(), v, Utils.epsilonAngle * FastMath.abs(v));
        Assert.assertEquals(p.getEccentricAnomaly(), E, Utils.epsilonAngle * FastMath.abs(E));
        Assert.assertEquals(p.getMeanAnomaly(), M, Utils.epsilonAngle * FastMath.abs(M));
//      p.setTrueAnomaly(0);
        p = new KeplerianOrbit(p.getA(),p.getE(), p.getI(), p.getPerigeeArgument(),
                                    p.getRightAscensionOfAscendingNode(), 0 , 2, 
                                    p.getFrame(), p.getDate(), p.getMu());

//      p.setEccentricAnomaly(E);
        p = new KeplerianOrbit(p.getA(),p.getE(), p.getI(), p.getPerigeeArgument(),
                                    p.getRightAscensionOfAscendingNode(), E , 1, 
                                    p.getFrame(), p.getDate(), p.getMu());
        Assert.assertEquals(p.getTrueAnomaly(), v, Utils.epsilonAngle * FastMath.abs(v));
        Assert.assertEquals(p.getEccentricAnomaly(), E, Utils.epsilonAngle * FastMath.abs(E));
        Assert.assertEquals(p.getMeanAnomaly(), M, Utils.epsilonAngle * FastMath.abs(M));
//      p.setTrueAnomaly(0);
        p = new KeplerianOrbit(p.getA(),p.getE(), p.getI(), p.getPerigeeArgument(),
                                    p.getRightAscensionOfAscendingNode(), 0 , 2, 
                                    p.getFrame(), p.getDate(), p.getMu());

//      p.setMeanAnomaly(M);
        p = new KeplerianOrbit(p.getA(),p.getE(), p.getI(), p.getPerigeeArgument(),
                                    p.getRightAscensionOfAscendingNode(), M , 0, 
                                    p.getFrame(), p.getDate(), p.getMu());
        Assert.assertEquals(p.getTrueAnomaly(), v, Utils.epsilonAngle * FastMath.abs(v));
        Assert.assertEquals(p.getEccentricAnomaly(), E, Utils.epsilonAngle * FastMath.abs(E));
        Assert.assertEquals(p.getMeanAnomaly(), M, Utils.epsilonAngle * FastMath.abs(M));

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
        Assert.assertEquals(p.getTrueAnomaly(), v, Utils.epsilonAngle * FastMath.abs(v));
        Assert.assertEquals(p.getEccentricAnomaly(), E, Utils.epsilonAngle * FastMath.abs(E));
        Assert.assertEquals(p.getMeanAnomaly(), M, Utils.epsilonAngle * FastMath.abs(M));
//      p.setTrueAnomaly(0);
        p = new KeplerianOrbit(p.getA(),p.getE(), p.getI(), p.getPerigeeArgument(),
                                    p.getRightAscensionOfAscendingNode(), 0 , 2, 
                                    p.getFrame(), p.getDate(), p.getMu());

//      p.setEccentricAnomaly(E);
        p = new KeplerianOrbit(p.getA(),p.getE(), p.getI(), p.getPerigeeArgument(),
                                    p.getRightAscensionOfAscendingNode(), E , 1, p.getFrame(), p.getDate(), p.getMu());
        Assert.assertEquals(p.getTrueAnomaly(), v, Utils.epsilonAngle * FastMath.abs(v));
        Assert.assertEquals(p.getEccentricAnomaly(), E, Utils.epsilonAngle * FastMath.abs(E));
        Assert.assertEquals(p.getMeanAnomaly(), M, Utils.epsilonAngle * FastMath.abs(M));
//      p.setTrueAnomaly(0);
        p = new KeplerianOrbit(p.getA(),p.getE(), p.getI(), p.getPerigeeArgument(),
                                    p.getRightAscensionOfAscendingNode(), 0 , 2, 
                                    p.getFrame(), p.getDate(), p.getMu());

//      p.setMeanAnomaly(M);
        p = new KeplerianOrbit(p.getA(),p.getE(), p.getI(), p.getPerigeeArgument(),
                                    p.getRightAscensionOfAscendingNode(), M , 0, 
                                    p.getFrame(), p.getDate(), p.getMu());
        Assert.assertEquals(p.getTrueAnomaly(), v, Utils.epsilonAngle * FastMath.abs(v));
        Assert.assertEquals(p.getEccentricAnomaly(), E, Utils.epsilonAngle * FastMath.abs(E));
        Assert.assertEquals(p.getMeanAnomaly(), M, Utils.epsilonAngle * FastMath.abs(M));

    }

    @Test
    public void testPositionVelocityNorms() {
        double mu = 3.9860047e14;

        // elliptic and non equatorial orbit
        KeplerianOrbit p =
            new KeplerianOrbit(24464560.0, 0.7311, 2.1, 3.10686, 1.00681,
                                    0.67, KeplerianOrbit.TRUE_ANOMALY, 
                                    FramesFactory.getEME2000(), date, mu);

        double e       = p.getE();
        double v       = p.getTrueAnomaly();
        double ksi     = 1 + e * FastMath.cos(v);
        double nu      = e * FastMath.sin(v);
        double epsilon = FastMath.sqrt((1 - e) * (1 + e));

        double a  = p.getA();
        double na = FastMath.sqrt(mu / a);

        // validation of: r = a .(1 - e2) / (1 + e.cos(v))
        Assert.assertEquals(a * epsilon * epsilon / ksi,
                     p.getPVCoordinates().getPosition().getNorm(),
                     Utils.epsilonTest * FastMath.abs(p.getPVCoordinates().getPosition().getNorm()));

        // validation of: V = sqrt(mu.(1+2e.cos(v)+e2)/a.(1-e2) )
        Assert.assertEquals(na * FastMath.sqrt(ksi * ksi + nu * nu) / epsilon,
                     p.getPVCoordinates().getVelocity().getNorm(),
                     Utils.epsilonTest * FastMath.abs(p.getPVCoordinates().getVelocity().getNorm()));


        //  circular and equatorial orbit
        KeplerianOrbit pCirEqua =
            new KeplerianOrbit(24464560.0, 0.1e-10, 0.1e-8, 3.10686, 1.00681,
                                    0.67, KeplerianOrbit.TRUE_ANOMALY, 
                                    FramesFactory.getEME2000(), date, mu);

        e       = pCirEqua.getE();
        v       = pCirEqua.getTrueAnomaly();
        ksi     = 1 + e * FastMath.cos(v);
        nu      = e * FastMath.sin(v);
        epsilon = FastMath.sqrt((1 - e) * (1 + e));

        a  = pCirEqua.getA();
        na = FastMath.sqrt(mu / a);

        // validation of: r = a .(1 - e2) / (1 + e.cos(v))
        Assert.assertEquals(a * epsilon * epsilon / ksi,
                     pCirEqua.getPVCoordinates().getPosition().getNorm(),
                     Utils.epsilonTest * FastMath.abs(pCirEqua.getPVCoordinates().getPosition().getNorm()));

        // validation of: V = sqrt(mu.(1+2e.cos(v)+e2)/a.(1-e2) )
        Assert.assertEquals(na * FastMath.sqrt(ksi * ksi + nu * nu) / epsilon,
                     pCirEqua.getPVCoordinates().getVelocity().getNorm(),
                     Utils.epsilonTest * FastMath.abs(pCirEqua.getPVCoordinates().getVelocity().getNorm()));
    }

    @Test
    public void testGeometry() {
        double mu = 3.9860047e14;

        // elliptic and non equatorial orbit
        KeplerianOrbit p =
            new KeplerianOrbit(24464560.0, 0.7311, 2.1, 3.10686, 1.00681,
                                    0.67, KeplerianOrbit.TRUE_ANOMALY, 
                                    FramesFactory.getEME2000(), date, mu);

        Vector3D position = p.getPVCoordinates().getPosition();
        Vector3D velocity = p.getPVCoordinates().getVelocity();
        Vector3D momentum = p.getPVCoordinates().getMomentum().normalize();

        double apogeeRadius  = p.getA() * (1 + p.getE());
        double perigeeRadius = p.getA() * (1 - p.getE());

        for (double lv = 0; lv <= 2 * FastMath.PI; lv += 2 * FastMath.PI/100.) {
//          p.setTrueAnomaly(lv);
            p = new KeplerianOrbit(p.getA(),p.getE(), p.getI(), p.getPerigeeArgument(),
                                        p.getRightAscensionOfAscendingNode(), lv , 2, 
                                        p.getFrame(), p.getDate(), p.getMu());
            position = p.getPVCoordinates().getPosition();

            // test if the norm of the position is in the range [perigee radius, apogee radius]
            Assert.assertTrue((position.getNorm() - apogeeRadius)  <= (  apogeeRadius * Utils.epsilonTest));
            Assert.assertTrue((position.getNorm() - perigeeRadius) >= (- perigeeRadius * Utils.epsilonTest));

            position = position.normalize();
            velocity = p.getPVCoordinates().getVelocity();
            velocity = velocity.normalize();

            // at this stage of computation, all the vectors (position, velocity and momemtum) are normalized here

            // test of orthogonality between position and momentum
            Assert.assertTrue(FastMath.abs(Vector3D.dotProduct(position, momentum)) < Utils.epsilonTest);
            // test of orthogonality between velocity and momentum
            Assert.assertTrue(FastMath.abs(Vector3D.dotProduct(velocity, momentum)) < Utils.epsilonTest);

        }

        // apsides
//      p.setTrueAnomaly(0);
        p = new KeplerianOrbit(p.getA(),p.getE(), p.getI(), p.getPerigeeArgument(),
                                    p.getRightAscensionOfAscendingNode(), 0 , 2, p.getFrame(), p.getDate(), p.getMu());
        Assert.assertEquals(p.getPVCoordinates().getPosition().getNorm(), perigeeRadius, perigeeRadius * Utils.epsilonTest);

//      p.setTrueAnomaly(FastMath.PI);
        p = new KeplerianOrbit(p.getA(),p.getE(), p.getI(), p.getPerigeeArgument(),
                                    p.getRightAscensionOfAscendingNode(), FastMath.PI , 2, p.getFrame(), p.getDate(), p.getMu());
        Assert.assertEquals(p.getPVCoordinates().getPosition().getNorm(), apogeeRadius, apogeeRadius * Utils.epsilonTest);

        // nodes
        // descending node
//      p.setTrueAnomaly(FastMath.PI - p.getPerigeeArgument());
        p = new KeplerianOrbit(p.getA(),p.getE(), p.getI(), p.getPerigeeArgument(),
                                    p.getRightAscensionOfAscendingNode(), FastMath.PI - p.getPerigeeArgument() , 2, 
                                    p.getFrame(), p.getDate(), p.getMu());
        Assert.assertTrue(FastMath.abs(p.getPVCoordinates().getPosition().getZ()) < p.getPVCoordinates().getPosition().getNorm() * Utils.epsilonTest);
        Assert.assertTrue(p.getPVCoordinates().getVelocity().getZ() < 0);

        // ascending node
//      p.setTrueAnomaly(2.0 * FastMath.PI - p.getPerigeeArgument());
        p = new KeplerianOrbit(p.getA(),p.getE(), p.getI(), p.getPerigeeArgument(),
                                    p.getRightAscensionOfAscendingNode(),2.0 * FastMath.PI - p.getPerigeeArgument() , 2, 
                                    p.getFrame(), p.getDate(), p.getMu());
        Assert.assertTrue(FastMath.abs(p.getPVCoordinates().getPosition().getZ()) < p.getPVCoordinates().getPosition().getNorm() * Utils.epsilonTest);
        Assert.assertTrue(p.getPVCoordinates().getVelocity().getZ() > 0);


        //  circular and equatorial orbit
        KeplerianOrbit pCirEqua =
            new KeplerianOrbit(24464560.0, 0.1e-10, 0.1e-8, 3.10686, 1.00681,
                                    0.67, KeplerianOrbit.TRUE_ANOMALY, FramesFactory.getEME2000(), date, mu);

        position = pCirEqua.getPVCoordinates().getPosition();
        velocity = pCirEqua.getPVCoordinates().getVelocity();
        momentum = Vector3D.crossProduct(position,velocity).normalize();

        apogeeRadius  = pCirEqua.getA() * (1 + pCirEqua.getE());
        perigeeRadius = pCirEqua.getA() * (1 - pCirEqua.getE());
        // test if apogee equals perigee
        Assert.assertEquals(perigeeRadius, apogeeRadius, 1.e+4 * Utils.epsilonTest * apogeeRadius);

        for (double lv = 0; lv <= 2 * FastMath.PI; lv += 2 * FastMath.PI/100.) {
//          pCirEqua.setTrueAnomaly(lv)
            pCirEqua = new KeplerianOrbit(pCirEqua.getA(),pCirEqua.getE(),pCirEqua.getI(), pCirEqua.getPerigeeArgument(),
                                               pCirEqua.getRightAscensionOfAscendingNode(), lv, 2, 
                                               pCirEqua.getFrame(), pCirEqua.getDate(), pCirEqua.getMu());
            position = pCirEqua.getPVCoordinates().getPosition();

            // test if the norm pf the position is in the range [perigee radius, apogee radius]
            // Warning: these tests are without absolute value by choice
            Assert.assertTrue((position.getNorm() - apogeeRadius)  <= (  apogeeRadius * Utils.epsilonTest));
            Assert.assertTrue((position.getNorm() - perigeeRadius) >= (- perigeeRadius * Utils.epsilonTest));

            position = position.normalize();
            velocity = pCirEqua.getPVCoordinates().getVelocity();
            velocity = velocity.normalize();

            // at this stage of computation, all the vectors (position, velocity and momemtum) are normalized here

            // test of orthogonality between position and momentum
            Assert.assertTrue(FastMath.abs(Vector3D.dotProduct(position, momentum)) < Utils.epsilonTest);
            // test of orthogonality between velocity and momentum
            Assert.assertTrue(FastMath.abs(Vector3D.dotProduct(velocity, momentum)) < Utils.epsilonTest);

        }
    }

    @Test
    public void testSymmetry() {

        // elliptic and non equatorial orbit
        Vector3D position = new Vector3D(-4947831., -3765382., -3708221.);
        Vector3D velocity = new Vector3D(-2079., 5291., -7842.);
        double mu = 3.9860047e14;

        KeplerianOrbit p = new KeplerianOrbit(new PVCoordinates(position, velocity),  
                                              FramesFactory.getEME2000(), date, mu);
        Vector3D positionOffset = p.getPVCoordinates().getPosition().subtract(position);
        Vector3D velocityOffset = p.getPVCoordinates().getVelocity().subtract(velocity);

        Assert.assertTrue(positionOffset.getNorm() < Utils.epsilonTest);
        Assert.assertTrue(velocityOffset.getNorm() < Utils.epsilonTest);

        // circular and equatorial orbit
        position = new Vector3D(1742382., -2.440243e7, -0.014517);
        velocity = new Vector3D(4026.2, 287.479, -3.e-6);


        p = new KeplerianOrbit(new PVCoordinates(position, velocity),  
                               FramesFactory.getEME2000(), date, mu);
        positionOffset = p.getPVCoordinates().getPosition().subtract(position);
        velocityOffset = p.getPVCoordinates().getVelocity().subtract(velocity);

        Assert.assertTrue(positionOffset.getNorm() < Utils.epsilonTest);
        Assert.assertTrue(velocityOffset.getNorm() < Utils.epsilonTest);

    }

    @Test(expected=IllegalArgumentException.class)
    public void testNonInertialFrame() throws IllegalArgumentException {

        Vector3D position = new Vector3D(-4947831., -3765382., -3708221.);
        Vector3D velocity = new Vector3D(-2079., 5291., -7842.);
        PVCoordinates pvCoordinates = new PVCoordinates( position, velocity);
        new KeplerianOrbit(pvCoordinates,
                           new Frame(FramesFactory.getEME2000(), Transform.IDENTITY, "non-inertial", false),
                           date, mu);
    }

    @Test
    public void testPeriod() {
        KeplerianOrbit orbit = new KeplerianOrbit(7654321.0, 0.1, 0.2, 0, 0, 0,
                                                  KeplerianOrbit.TRUE_ANOMALY,
                                                  FramesFactory.getEME2000(), AbsoluteDate.J2000_EPOCH,
                                                  mu);
        Assert.assertEquals(6664.5521723383589487, orbit.getKeplerianPeriod(), 1.0e-12);
        Assert.assertEquals(0.00094277682051291315229, orbit.getKeplerianMeanMotion(), 1.0e-16);
    }

    @Test
    public void testHyperbola() {
        KeplerianOrbit orbit = new KeplerianOrbit(-10000000.0, 2.5, 0.3, 0, 0, 0.0,
                                                  KeplerianOrbit.TRUE_ANOMALY,
                                                  FramesFactory.getEME2000(), AbsoluteDate.J2000_EPOCH,
                                                  mu);
        Vector3D perigeeP  = orbit.getPVCoordinates().getPosition();
        Vector3D u = perigeeP.normalize();
        Vector3D focus1 = Vector3D.ZERO;
        Vector3D focus2 = new Vector3D(-2 * orbit.getA() * orbit.getE(), u);
        for (double dt = -5000; dt < 5000; dt += 60) {
            PVCoordinates pv = orbit.shiftedBy(dt).getPVCoordinates();
            double d1 = Vector3D.distance(pv.getPosition(), focus1);
            double d2 = Vector3D.distance(pv.getPosition(), focus2);
            Assert.assertEquals(-2 * orbit.getA(), FastMath.abs(d1 - d2), 1.0e-6);
            KeplerianOrbit rebuilt =
                new KeplerianOrbit(pv, orbit.getFrame(), orbit.getDate().shiftedBy(dt), mu);
            Assert.assertEquals(-10000000.0, rebuilt.getA(), 1.0e-6);
            Assert.assertEquals(2.5, rebuilt.getE(), 1.0e-13);
        }
    }

    @Test
    public void testKeplerEquation() {

        for (double M = -6 * FastMath.PI; M < 6 * FastMath.PI; M += 0.01) {
            KeplerianOrbit pElliptic =
                new KeplerianOrbit(24464560.0, 0.7311, 2.1, 3.10686, 1.00681,
                                   M, KeplerianOrbit.MEAN_ANOMALY, 
                                   FramesFactory.getEME2000(), date, mu);
            double E = pElliptic.getEccentricAnomaly();
            double e = pElliptic.getE();
            Assert.assertEquals(M, E - e * FastMath.sin(E), 2.0e-14);
        }

        for (double M = -6 * FastMath.PI; M < 6 * FastMath.PI; M += 0.01) {
            KeplerianOrbit pAlmostParabolic =
                new KeplerianOrbit(24464560.0, 0.9999, 2.1, 3.10686, 1.00681,
                                   M, KeplerianOrbit.MEAN_ANOMALY, 
                                   FramesFactory.getEME2000(), date, mu);
            double E = pAlmostParabolic.getEccentricAnomaly();
            double e = pAlmostParabolic.getE();
            Assert.assertEquals(M, E - e * FastMath.sin(E), 3.0e-13);
        }

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
