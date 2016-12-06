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

import org.hipparchus.analysis.UnivariateFunction;
import org.hipparchus.analysis.differentiation.DerivativeStructure;
import org.hipparchus.analysis.differentiation.FiniteDifferencesDifferentiator;
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
import org.orekit.utils.Constants;
import org.orekit.utils.PVCoordinates;


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
                                    0.048363, PositionAngle.MEAN,
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
                                    0.048363, PositionAngle.MEAN,
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
                                    0.048363, PositionAngle.MEAN,
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
                                    0.048363, PositionAngle.MEAN,
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
                                    0.048363, PositionAngle.MEAN,
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

        p = new KeplerianOrbit(p.getA(),p.getE(), p.getI(), p.getPerigeeArgument(),
                                    p.getRightAscensionOfAscendingNode(), v , PositionAngle.TRUE,
                                    p.getFrame(), p.getDate(), p.getMu());
        Assert.assertEquals(p.getTrueAnomaly(), v, Utils.epsilonAngle * FastMath.abs(v));
        Assert.assertEquals(p.getEccentricAnomaly(), E, Utils.epsilonAngle * FastMath.abs(E));
        Assert.assertEquals(p.getMeanAnomaly(), M, Utils.epsilonAngle * FastMath.abs(M));
        p = new KeplerianOrbit(p.getA(),p.getE(), p.getI(), p.getPerigeeArgument(),
                                    p.getRightAscensionOfAscendingNode(), 0 , PositionAngle.TRUE,
                                    p.getFrame(), p.getDate(), p.getMu());

        p = new KeplerianOrbit(p.getA(),p.getE(), p.getI(), p.getPerigeeArgument(),
                                    p.getRightAscensionOfAscendingNode(), E , PositionAngle.ECCENTRIC,
                                    p.getFrame(), p.getDate(), p.getMu());
        Assert.assertEquals(p.getTrueAnomaly(), v, Utils.epsilonAngle * FastMath.abs(v));
        Assert.assertEquals(p.getEccentricAnomaly(), E, Utils.epsilonAngle * FastMath.abs(E));
        Assert.assertEquals(p.getMeanAnomaly(), M, Utils.epsilonAngle * FastMath.abs(M));
        p = new KeplerianOrbit(p.getA(),p.getE(), p.getI(), p.getPerigeeArgument(),
                                    p.getRightAscensionOfAscendingNode(), 0 , PositionAngle.TRUE,
                                    p.getFrame(), p.getDate(), p.getMu());

        p = new KeplerianOrbit(p.getA(),p.getE(), p.getI(), p.getPerigeeArgument(),
                                    p.getRightAscensionOfAscendingNode(), M, PositionAngle.MEAN,
                                    p.getFrame(), p.getDate(), p.getMu());
        Assert.assertEquals(p.getTrueAnomaly(), v, Utils.epsilonAngle * FastMath.abs(v));
        Assert.assertEquals(p.getEccentricAnomaly(), E, Utils.epsilonAngle * FastMath.abs(E));
        Assert.assertEquals(p.getMeanAnomaly(), M, Utils.epsilonAngle * FastMath.abs(M));

        // circular orbit
        p = new KeplerianOrbit(p.getA(),0, p.getI(), p.getPerigeeArgument(),
                                    p.getRightAscensionOfAscendingNode(), p.getLv() , PositionAngle.TRUE,
                                    p.getFrame(), p.getDate(), p.getMu());

        E = v;
        M = E;

        p = new KeplerianOrbit(p.getA(),p.getE(), p.getI(), p.getPerigeeArgument(),
                                    p.getRightAscensionOfAscendingNode(), v , PositionAngle.TRUE,
                                    p.getFrame(), p.getDate(), p.getMu());
        Assert.assertEquals(p.getTrueAnomaly(), v, Utils.epsilonAngle * FastMath.abs(v));
        Assert.assertEquals(p.getEccentricAnomaly(), E, Utils.epsilonAngle * FastMath.abs(E));
        Assert.assertEquals(p.getMeanAnomaly(), M, Utils.epsilonAngle * FastMath.abs(M));
        p = new KeplerianOrbit(p.getA(),p.getE(), p.getI(), p.getPerigeeArgument(),
                                    p.getRightAscensionOfAscendingNode(), 0 , PositionAngle.TRUE,
                                    p.getFrame(), p.getDate(), p.getMu());

        p = new KeplerianOrbit(p.getA(),p.getE(), p.getI(), p.getPerigeeArgument(),
                                    p.getRightAscensionOfAscendingNode(), E , PositionAngle.ECCENTRIC, p.getFrame(), p.getDate(), p.getMu());
        Assert.assertEquals(p.getTrueAnomaly(), v, Utils.epsilonAngle * FastMath.abs(v));
        Assert.assertEquals(p.getEccentricAnomaly(), E, Utils.epsilonAngle * FastMath.abs(E));
        Assert.assertEquals(p.getMeanAnomaly(), M, Utils.epsilonAngle * FastMath.abs(M));
        p = new KeplerianOrbit(p.getA(),p.getE(), p.getI(), p.getPerigeeArgument(),
                                    p.getRightAscensionOfAscendingNode(), 0 , PositionAngle.TRUE,
                                    p.getFrame(), p.getDate(), p.getMu());

        p = new KeplerianOrbit(p.getA(),p.getE(), p.getI(), p.getPerigeeArgument(),
                                    p.getRightAscensionOfAscendingNode(), M, PositionAngle.MEAN,
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
                                    0.67, PositionAngle.TRUE,
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
                                    0.67, PositionAngle.TRUE,
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
                                    0.67, PositionAngle.TRUE,
                                    FramesFactory.getEME2000(), date, mu);

        Vector3D position = p.getPVCoordinates().getPosition();
        Vector3D velocity = p.getPVCoordinates().getVelocity();
        Vector3D momentum = p.getPVCoordinates().getMomentum().normalize();

        double apogeeRadius  = p.getA() * (1 + p.getE());
        double perigeeRadius = p.getA() * (1 - p.getE());

        for (double lv = 0; lv <= 2 * FastMath.PI; lv += 2 * FastMath.PI/100.) {
            p = new KeplerianOrbit(p.getA(),p.getE(), p.getI(), p.getPerigeeArgument(),
                                        p.getRightAscensionOfAscendingNode(), lv , PositionAngle.TRUE,
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
        p = new KeplerianOrbit(p.getA(),p.getE(), p.getI(), p.getPerigeeArgument(),
                                    p.getRightAscensionOfAscendingNode(), 0 , PositionAngle.TRUE, p.getFrame(), p.getDate(), p.getMu());
        Assert.assertEquals(p.getPVCoordinates().getPosition().getNorm(), perigeeRadius, perigeeRadius * Utils.epsilonTest);

        p = new KeplerianOrbit(p.getA(),p.getE(), p.getI(), p.getPerigeeArgument(),
                                    p.getRightAscensionOfAscendingNode(), FastMath.PI , PositionAngle.TRUE, p.getFrame(), p.getDate(), p.getMu());
        Assert.assertEquals(p.getPVCoordinates().getPosition().getNorm(), apogeeRadius, apogeeRadius * Utils.epsilonTest);

        // nodes
        // descending node
        p = new KeplerianOrbit(p.getA(),p.getE(), p.getI(), p.getPerigeeArgument(),
                                    p.getRightAscensionOfAscendingNode(), FastMath.PI - p.getPerigeeArgument() , PositionAngle.TRUE,
                                    p.getFrame(), p.getDate(), p.getMu());
        Assert.assertTrue(FastMath.abs(p.getPVCoordinates().getPosition().getZ()) < p.getPVCoordinates().getPosition().getNorm() * Utils.epsilonTest);
        Assert.assertTrue(p.getPVCoordinates().getVelocity().getZ() < 0);

        // ascending node
        p = new KeplerianOrbit(p.getA(),p.getE(), p.getI(), p.getPerigeeArgument(),
                                    p.getRightAscensionOfAscendingNode(),2.0 * FastMath.PI - p.getPerigeeArgument() , PositionAngle.TRUE,
                                    p.getFrame(), p.getDate(), p.getMu());
        Assert.assertTrue(FastMath.abs(p.getPVCoordinates().getPosition().getZ()) < p.getPVCoordinates().getPosition().getNorm() * Utils.epsilonTest);
        Assert.assertTrue(p.getPVCoordinates().getVelocity().getZ() > 0);


        //  circular and equatorial orbit
        KeplerianOrbit pCirEqua =
            new KeplerianOrbit(24464560.0, 0.1e-10, 0.1e-8, 3.10686, 1.00681,
                                    0.67, PositionAngle.TRUE, FramesFactory.getEME2000(), date, mu);

        position = pCirEqua.getPVCoordinates().getPosition();
        velocity = pCirEqua.getPVCoordinates().getVelocity();
        momentum = Vector3D.crossProduct(position,velocity).normalize();

        apogeeRadius  = pCirEqua.getA() * (1 + pCirEqua.getE());
        perigeeRadius = pCirEqua.getA() * (1 - pCirEqua.getE());
        // test if apogee equals perigee
        Assert.assertEquals(perigeeRadius, apogeeRadius, 1.e+4 * Utils.epsilonTest * apogeeRadius);

        for (double lv = 0; lv <= 2 * FastMath.PI; lv += 2 * FastMath.PI/100.) {
            pCirEqua = new KeplerianOrbit(pCirEqua.getA(),pCirEqua.getE(),pCirEqua.getI(), pCirEqua.getPerigeeArgument(),
                                               pCirEqua.getRightAscensionOfAscendingNode(), lv, PositionAngle.TRUE,
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
                                                  PositionAngle.TRUE,
                                                  FramesFactory.getEME2000(), AbsoluteDate.J2000_EPOCH,
                                                  mu);
        Assert.assertEquals(6664.5521723383589487, orbit.getKeplerianPeriod(), 1.0e-12);
        Assert.assertEquals(0.00094277682051291315229, orbit.getKeplerianMeanMotion(), 1.0e-16);
    }

    @Test
    public void testHyperbola() {
        KeplerianOrbit orbit = new KeplerianOrbit(-10000000.0, 2.5, 0.3, 0, 0, 0.0,
                                                  PositionAngle.TRUE,
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
        public void testVeryLargeEccentricity() {

            final Frame eme2000 = FramesFactory.getEME2000();
            final double meanAnomaly = 1.;
            final KeplerianOrbit orb0 = new KeplerianOrbit(42600e3, 0.9, 0.00001, 0, 0,
                                                           FastMath.toRadians(meanAnomaly),
                                                           PositionAngle.MEAN, eme2000, date, mu);

            // big dV along Y
            final Vector3D deltaV = new Vector3D(0.0, 110000.0, 0.0);
            final PVCoordinates pv1 = new PVCoordinates(orb0.getPVCoordinates().getPosition(),
                                                        orb0.getPVCoordinates().getVelocity().add(deltaV));
            final KeplerianOrbit orb1 = new KeplerianOrbit(pv1, eme2000, date, mu);

            // Despite large eccentricity, the conversion of mean anomaly to hyperbolic eccentric anomaly
            // converges in less than 50 iterations (issue #114)
            final PVCoordinates pvTested    = orb1.shiftedBy(0).getPVCoordinates();
            final Vector3D      pTested     = pvTested.getPosition();
            final Vector3D      vTested     = pvTested.getVelocity();

            final PVCoordinates pvReference = orb1.getPVCoordinates();
            final Vector3D      pReference  = pvReference.getPosition();
            final Vector3D      vReference  = pvReference.getVelocity();

            final double threshold = 1.e-15;
            Assert.assertEquals(0, pTested.subtract(pReference).getNorm(), threshold * pReference.getNorm());
            Assert.assertEquals(0, vTested.subtract(vReference).getNorm(), threshold * vReference.getNorm());

        }

    @Test
    public void testKeplerEquation() {

        for (double M = -6 * FastMath.PI; M < 6 * FastMath.PI; M += 0.01) {
            KeplerianOrbit pElliptic =
                new KeplerianOrbit(24464560.0, 0.7311, 2.1, 3.10686, 1.00681,
                                   M, PositionAngle.MEAN,
                                   FramesFactory.getEME2000(), date, mu);
            double E = pElliptic.getEccentricAnomaly();
            double e = pElliptic.getE();
            Assert.assertEquals(M, E - e * FastMath.sin(E), 2.0e-14);
        }

        for (double M = -6 * FastMath.PI; M < 6 * FastMath.PI; M += 0.01) {
            KeplerianOrbit pAlmostParabolic =
                new KeplerianOrbit(24464560.0, 0.9999, 2.1, 3.10686, 1.00681,
                                   M, PositionAngle.MEAN,
                                   FramesFactory.getEME2000(), date, mu);
            double E = pAlmostParabolic.getEccentricAnomaly();
            double e = pAlmostParabolic.getE();
            Assert.assertEquals(M, E - e * FastMath.sin(E), 3.0e-13);
        }

    }

    @Test(expected=IllegalArgumentException.class)
    public void testOutOfRangeV() throws OrekitException {
        new KeplerianOrbit(-7000434.460140012, 1.1999785407363386, 1.3962787004479158,
                           1.3962320168955138, 0.3490728321331678, -2.55593407037698,
                           PositionAngle.TRUE, FramesFactory.getEME2000(),
                           new AbsoluteDate("2000-01-01T12:00:00.391", TimeScalesFactory.getUTC()),
                           3.986004415E14);
    }

    @Test
    public void testNumericalIssue25() throws OrekitException {
        Vector3D position = new Vector3D(3782116.14107698, 416663.11924914, 5875541.62103057);
        Vector3D velocity = new Vector3D(-6349.7848910501, 288.4061811651, 4066.9366759691);
        KeplerianOrbit orbit = new KeplerianOrbit(new PVCoordinates(position, velocity),
                                                  FramesFactory.getEME2000(),
                                                  new AbsoluteDate("2004-01-01T23:00:00.000",
                                                                   TimeScalesFactory.getUTC()),
                                                  3.986004415E14);
        Assert.assertEquals(0.0, orbit.getE(), 2.0e-14);
    }

    @Test
    public void testPerfectlyEquatorial() throws OrekitException {
        Vector3D position = new Vector3D(6957904.3624652653594, 766529.11411558074507, 0);
        Vector3D velocity = new Vector3D(-7538.2817012412102845, 342.38751001881413381, 0.);
        KeplerianOrbit orbit = new KeplerianOrbit(new PVCoordinates(position, velocity),
                                                  FramesFactory.getEME2000(),
                                                  new AbsoluteDate("2004-01-01T23:00:00.000",
                                                                   TimeScalesFactory.getUTC()),
                                                  3.986004415E14);
        Assert.assertEquals(0.0, orbit.getI(), 2.0e-14);
        Assert.assertEquals(0.0, orbit.getRightAscensionOfAscendingNode(), 2.0e-14);
    }

    @Test
    public void testJacobianReferenceEllipse() throws OrekitException {

        AbsoluteDate dateTca = new AbsoluteDate(2000, 04, 01, 0, 0, 0.000, TimeScalesFactory.getUTC());
        double mu =  3.986004415e+14;
        KeplerianOrbit orbKep = new KeplerianOrbit(7000000.0, 0.01, FastMath.toRadians(80.), FastMath.toRadians(80.), FastMath.toRadians(20.),
                                          FastMath.toRadians(40.), PositionAngle.MEAN,
                                          FramesFactory.getEME2000(), dateTca, mu);

        // the following reference values have been computed using the free software
        // version 6.2 of the MSLIB fortran library by the following program:
        //        program kep_jacobian
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
        //        type(tm_orb_kep)::kep
        //        real(pm_reel), dimension(6,6)::jacob
        //        real(pm_reel)::norme
        //
        //        kep%a=7000000_pm_reel
        //        kep%e=0.01_pm_reel
        //        kep%i=80_pm_reel*pm_deg_rad
        //        kep%pom=80_pm_reel*pm_deg_rad
        //        kep%gom=20_pm_reel*pm_deg_rad
        //        kep%M=40_pm_reel*pm_deg_rad
        //
        //        call mv_kep_car(mu,kep,pos_car,vit_car,code_retour)
        //        write(*,*)code_retour%valeur
        //        write(*,1000)pos_car,vit_car
        //
        //
        //        call mu_norme(pos_car,norme,code_retour)
        //        write(*,*)norme
        //
        //        call mv_car_kep (mu, pos_car, vit_car, kep, code_retour, jacob)
        //        write(*,*)code_retour%valeur
        //
        //        write(*,*)"kep = ", kep%a, kep%e, kep%i*pm_rad_deg,&
        //                            kep%pom*pm_rad_deg, kep%gom*pm_rad_deg, kep%M*pm_rad_deg
        //
        //        do i = 1,6
        //           write(*,*) " ",(jacob(i,j),j=1,6)
        //        end do
        //
        //        1000 format (6(f24.15,1x))
        //        end program kep_jacobian
        Vector3D pRef = new Vector3D(-3691555.569874833337963, -240330.253992714860942, 5879700.285850423388183);
        Vector3D vRef = new Vector3D(-5936.229884450408463, -2871.067660163344044, -3786.209549192726627);
        double[][] jRef = {
            { -1.0792090588217809,       -7.02594292049818631E-002,  1.7189029642216496,       -1459.4829009393857,       -705.88138246206040,       -930.87838644776593       },
            { -1.31195762636625214E-007, -3.90087231593959271E-008,  4.65917592901869866E-008, -2.02467187867647177E-004, -7.89767994436215424E-005, -2.81639203329454407E-005 },
            {  4.18334478744371316E-008, -1.14936453412947957E-007,  2.15670500707930151E-008, -2.26450325965329431E-005,  6.22167157217876380E-005, -1.16745469637130306E-005 },
            {  3.52735168061691945E-006,  3.82555734454450974E-006,  1.34715077236557634E-005, -8.06586262922115264E-003, -6.13725651685311825E-003, -1.71765290503914092E-002 },
            {  2.48948022169790885E-008, -6.83979069529389238E-008,  1.28344057971888544E-008,  3.86597661353874888E-005, -1.06216834498373629E-004,  1.99308724078785540E-005 },
            { -3.41911705254704525E-006, -3.75913623359912437E-006, -1.34013845492518465E-005,  8.19851888816422458E-003,  6.16449264680494959E-003,  1.69495878276556648E-002 }
        };

        PVCoordinates pv = orbKep.getPVCoordinates();
        Assert.assertEquals(0, pv.getPosition().subtract(pRef).getNorm(), 1.0e-15 * pRef.getNorm());
        Assert.assertEquals(0, pv.getVelocity().subtract(vRef).getNorm(), 1.0e-16 * vRef.getNorm());

        double[][] jacobian = new double[6][6];
        orbKep.getJacobianWrtCartesian(PositionAngle.MEAN, jacobian);

        for (int i = 0; i < jacobian.length; i++) {
            double[] row    = jacobian[i];
            double[] rowRef = jRef[i];
            for (int j = 0; j < row.length; j++) {
                Assert.assertEquals(0, (row[j] - rowRef[j]) / rowRef[j], 2.0e-12);
            }
        }

    }

    @Test
    public void testJacobianFinitedifferencesEllipse() throws OrekitException {

        AbsoluteDate dateTca = new AbsoluteDate(2000, 04, 01, 0, 0, 0.000, TimeScalesFactory.getUTC());
        double mu =  3.986004415e+14;
        KeplerianOrbit orbKep = new KeplerianOrbit(7000000.0, 0.01, FastMath.toRadians(80.), FastMath.toRadians(80.), FastMath.toRadians(20.),
                                          FastMath.toRadians(40.), PositionAngle.MEAN,
                                          FramesFactory.getEME2000(), dateTca, mu);

        for (PositionAngle type : PositionAngle.values()) {
            double hP = 2.0;
            double[][] finiteDiffJacobian = finiteDifferencesJacobian(type, orbKep, hP);
            double[][] jacobian = new double[6][6];
            orbKep.getJacobianWrtCartesian(type, jacobian);

            for (int i = 0; i < jacobian.length; i++) {
                double[] row    = jacobian[i];
                double[] rowRef = finiteDiffJacobian[i];
                for (int j = 0; j < row.length; j++) {
                    Assert.assertEquals(0, (row[j] - rowRef[j]) / rowRef[j], 2.0e-7);
                }
            }

            double[][] invJacobian = new double[6][6];
            orbKep.getJacobianWrtParameters(type, invJacobian);
            MatrixUtils.createRealMatrix(jacobian).
                            multiply(MatrixUtils.createRealMatrix(invJacobian)).
            walkInRowOrder(new RealMatrixPreservingVisitor() {
                public void start(int rows, int columns,
                                  int startRow, int endRow, int startColumn, int endColumn) {
                }

                public void visit(int row, int column, double value) {
                    Assert.assertEquals(row == column ? 1.0 : 0.0, value, 5.0e-9);
                }

                public double end() {
                    return Double.NaN;
                }
            });

        }

    }

    @Test
    public void testJacobianReferenceHyperbola() throws OrekitException {

        AbsoluteDate dateTca = new AbsoluteDate(2000, 04, 01, 0, 0, 0.000, TimeScalesFactory.getUTC());
        double mu =  3.986004415e+14;
        KeplerianOrbit orbKep = new KeplerianOrbit(-7000000.0, 1.2, FastMath.toRadians(80.), FastMath.toRadians(80.), FastMath.toRadians(20.),
                                          FastMath.toRadians(40.), PositionAngle.MEAN,
                                          FramesFactory.getEME2000(), dateTca, mu);

        // the following reference values have been computed using the free software
        // version 6.2 of the MSLIB fortran library by the following program:
        //        program kep_hyperb_jacobian
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
        //        type(tm_orb_kep)::kep
        //        real(pm_reel), dimension(6,6)::jacob
        //        real(pm_reel)::norme
        //
        //        kep%a=7000000_pm_reel
        //        kep%e=1.2_pm_reel
        //        kep%i=80_pm_reel*pm_deg_rad
        //        kep%pom=80_pm_reel*pm_deg_rad
        //        kep%gom=20_pm_reel*pm_deg_rad
        //        kep%M=40_pm_reel*pm_deg_rad
        //
        //        call mv_kep_car(mu,kep,pos_car,vit_car,code_retour)
        //        write(*,*)code_retour%valeur
        //        write(*,1000)pos_car,vit_car
        //
        //
        //        call mu_norme(pos_car,norme,code_retour)
        //        write(*,*)norme
        //
        //        call mv_car_kep (mu, pos_car, vit_car, kep, code_retour, jacob)
        //        write(*,*)code_retour%valeur
        //
        //        write(*,*)"kep = ", kep%a, kep%e, kep%i*pm_rad_deg,&
        //                            kep%pom*pm_rad_deg, kep%gom*pm_rad_deg, kep%M*pm_rad_deg
        //
        //        ! convert the sign of da row since mslib uses a > 0 for all orbits
        //        ! whereas we use a < 0 for hyperbolic orbits
        //        write(*,*) " ",(-jacob(1,j),j=1,6)
        //        do i = 2,6
        //           write(*,*) " ",(jacob(i,j),j=1,6)
        //        end do
        //
        //        1000 format (6(f24.15,1x))
        //        end program kep_hyperb_jacobian
        Vector3D pRef = new Vector3D(-7654711.206549182534218, -3460171.872979687992483, -3592374.514463655184954);
        Vector3D vRef = new Vector3D(   -7886.368091820805603,    -4359.739012331759113,    -7937.060044548694350);
        double[][] jRef = {
            {  -0.98364725131848019,      -0.44463970750901238,      -0.46162803814668391,       -1938.9443476028839,       -1071.8864775981751,       -1951.4074832397598      },
            {  -1.10548813242982574E-007, -2.52906747183730431E-008,  7.96500937398593591E-008, -9.70479823470940108E-006, -2.93209076428001017E-005, -1.37434463892791042E-004 },
            {   8.55737680891616672E-008, -2.35111995522618220E-007,  4.41171797903162743E-008, -8.05235180390949802E-005,  2.21236547547460423E-004, -4.15135455876865407E-005 },
            {  -1.52641427784095578E-007,  1.10250447958827901E-008,  1.21265251605359894E-007,  7.63347077200903542E-005, -3.54738331412232378E-005, -2.31400737283033359E-004 },
            {   7.86711766048035274E-008, -2.16147281283624453E-007,  4.05585791077187359E-008, -3.56071805267582894E-005,  9.78299244677127374E-005, -1.83571253224293247E-005 },
            {  -2.41488884881911384E-007, -1.00119615610276537E-007, -6.51494225096757969E-008, -2.43295075073248163E-004, -1.43273725071890463E-004, -2.91625510452094873E-004 }
        };

        PVCoordinates pv = orbKep.getPVCoordinates();
        Assert.assertEquals(0, pv.getPosition().subtract(pRef).getNorm() / pRef.getNorm(), 1.0e-16);
//        Assert.assertEquals(0, pv.getPosition().subtract(pRef).getNorm() / pRef.getNorm(), 2.0e-15);
        Assert.assertEquals(0, pv.getVelocity().subtract(vRef).getNorm() / vRef.getNorm(), 3.0e-16);

        double[][] jacobian = new double[6][6];
        orbKep.getJacobianWrtCartesian(PositionAngle.MEAN, jacobian);

        for (int i = 0; i < jacobian.length; i++) {
            double[] row    = jacobian[i];
            double[] rowRef = jRef[i];
            for (int j = 0; j < row.length; j++) {
                Assert.assertEquals(0, (row[j] - rowRef[j]) / rowRef[j], 1.0e-14);
            }
        }

    }

    @Test
    public void testJacobianFinitedifferencesHyperbola() throws OrekitException {

        AbsoluteDate dateTca = new AbsoluteDate(2000, 04, 01, 0, 0, 0.000, TimeScalesFactory.getUTC());
        double mu =  3.986004415e+14;
        KeplerianOrbit orbKep = new KeplerianOrbit(-7000000.0, 1.2, FastMath.toRadians(80.), FastMath.toRadians(80.), FastMath.toRadians(20.),
                                                   FastMath.toRadians(40.), PositionAngle.MEAN,
                                                   FramesFactory.getEME2000(), dateTca, mu);

        for (PositionAngle type : PositionAngle.values()) {
            double hP = 2.0;
            double[][] finiteDiffJacobian = finiteDifferencesJacobian(type, orbKep, hP);
            double[][] jacobian = new double[6][6];
            orbKep.getJacobianWrtCartesian(type, jacobian);
            for (int i = 0; i < jacobian.length; i++) {
                double[] row    = jacobian[i];
                double[] rowRef = finiteDiffJacobian[i];
                for (int j = 0; j < row.length; j++) {
                    Assert.assertEquals(0, (row[j] - rowRef[j]) / rowRef[j], 3.0e-8);
                }
            }

            double[][] invJacobian = new double[6][6];
            orbKep.getJacobianWrtParameters(type, invJacobian);
            MatrixUtils.createRealMatrix(jacobian).
                            multiply(MatrixUtils.createRealMatrix(invJacobian)).
            walkInRowOrder(new RealMatrixPreservingVisitor() {
                public void start(int rows, int columns,
                                  int startRow, int endRow, int startColumn, int endColumn) {
                }

                public void visit(int row, int column, double value) {
                    Assert.assertEquals(row == column ? 1.0 : 0.0, value, 2.0e-8);
                }

                public double end() {
                    return Double.NaN;
                }
            });

        }

    }

    private double[][] finiteDifferencesJacobian(PositionAngle type, KeplerianOrbit orbit, double hP)
        throws OrekitException {
        double[][] jacobian = new double[6][6];
        for (int i = 0; i < 6; ++i) {
            fillColumn(type, i, orbit, hP, jacobian);
        }
        return jacobian;
    }

    private void fillColumn(PositionAngle type, int i, KeplerianOrbit orbit, double hP, double[][] jacobian) {

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

        KeplerianOrbit oM4h = new KeplerianOrbit(new PVCoordinates(new Vector3D(1, p, -4, dP), new Vector3D(1, v, -4, dV)),
                                                 orbit.getFrame(), orbit.getDate(), orbit.getMu());
        KeplerianOrbit oM3h = new KeplerianOrbit(new PVCoordinates(new Vector3D(1, p, -3, dP), new Vector3D(1, v, -3, dV)),
                                                 orbit.getFrame(), orbit.getDate(), orbit.getMu());
        KeplerianOrbit oM2h = new KeplerianOrbit(new PVCoordinates(new Vector3D(1, p, -2, dP), new Vector3D(1, v, -2, dV)),
                                                 orbit.getFrame(), orbit.getDate(), orbit.getMu());
        KeplerianOrbit oM1h = new KeplerianOrbit(new PVCoordinates(new Vector3D(1, p, -1, dP), new Vector3D(1, v, -1, dV)),
                                                 orbit.getFrame(), orbit.getDate(), orbit.getMu());
        KeplerianOrbit oP1h = new KeplerianOrbit(new PVCoordinates(new Vector3D(1, p, +1, dP), new Vector3D(1, v, +1, dV)),
                                                 orbit.getFrame(), orbit.getDate(), orbit.getMu());
        KeplerianOrbit oP2h = new KeplerianOrbit(new PVCoordinates(new Vector3D(1, p, +2, dP), new Vector3D(1, v, +2, dV)),
                                                 orbit.getFrame(), orbit.getDate(), orbit.getMu());
        KeplerianOrbit oP3h = new KeplerianOrbit(new PVCoordinates(new Vector3D(1, p, +3, dP), new Vector3D(1, v, +3, dV)),
                                                 orbit.getFrame(), orbit.getDate(), orbit.getMu());
        KeplerianOrbit oP4h = new KeplerianOrbit(new PVCoordinates(new Vector3D(1, p, +4, dP), new Vector3D(1, v, +4, dV)),
                                                 orbit.getFrame(), orbit.getDate(), orbit.getMu());

        jacobian[0][i] = (-3 * (oP4h.getA()                             - oM4h.getA()) +
                          32 * (oP3h.getA()                             - oM3h.getA()) -
                         168 * (oP2h.getA()                             - oM2h.getA()) +
                         672 * (oP1h.getA()                             - oM1h.getA())) / (840 * h);
        jacobian[1][i] = (-3 * (oP4h.getE()                             - oM4h.getE()) +
                          32 * (oP3h.getE()                             - oM3h.getE()) -
                         168 * (oP2h.getE()                             - oM2h.getE()) +
                         672 * (oP1h.getE()                             - oM1h.getE())) / (840 * h);
        jacobian[2][i] = (-3 * (oP4h.getI()                             - oM4h.getI()) +
                          32 * (oP3h.getI()                             - oM3h.getI()) -
                         168 * (oP2h.getI()                             - oM2h.getI()) +
                         672 * (oP1h.getI()                             - oM1h.getI())) / (840 * h);
        jacobian[3][i] = (-3 * (oP4h.getPerigeeArgument()               - oM4h.getPerigeeArgument()) +
                          32 * (oP3h.getPerigeeArgument()               - oM3h.getPerigeeArgument()) -
                         168 * (oP2h.getPerigeeArgument()               - oM2h.getPerigeeArgument()) +
                         672 * (oP1h.getPerigeeArgument()               - oM1h.getPerigeeArgument())) / (840 * h);
        jacobian[4][i] = (-3 * (oP4h.getRightAscensionOfAscendingNode() - oM4h.getRightAscensionOfAscendingNode()) +
                          32 * (oP3h.getRightAscensionOfAscendingNode() - oM3h.getRightAscensionOfAscendingNode()) -
                         168 * (oP2h.getRightAscensionOfAscendingNode() - oM2h.getRightAscensionOfAscendingNode()) +
                         672 * (oP1h.getRightAscensionOfAscendingNode() - oM1h.getRightAscensionOfAscendingNode())) / (840 * h);
        jacobian[5][i] = (-3 * (oP4h.getAnomaly(type)                   - oM4h.getAnomaly(type)) +
                          32 * (oP3h.getAnomaly(type)                   - oM3h.getAnomaly(type)) -
                         168 * (oP2h.getAnomaly(type)                   - oM2h.getAnomaly(type)) +
                         672 * (oP1h.getAnomaly(type)                   - oM1h.getAnomaly(type))) / (840 * h);

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
        final KeplerianOrbit initialOrbit = new KeplerianOrbit(new PVCoordinates(position, velocity),
                                                               FramesFactory.getEME2000(), date, ehMu);

        EcksteinHechlerPropagator propagator =
                new EcksteinHechlerPropagator(initialOrbit, ae, ehMu, c20, c30, c40, c50, c60);

        // set up a 5 points sample
        List<Orbit> sample = new ArrayList<Orbit>();
        for (double dt = 0; dt < 300.0; dt += 60.0) {
            sample.add(propagator.propagate(date.shiftedBy(dt)).getOrbit());
        }

        // well inside the sample, interpolation should be slightly better than Keplerian shift
        // the relative bad behaviour here is due to eccentricity, which cannot be
        // accurately interpolated with a polynomial in this case
        double maxShiftPositionError = 0;
        double maxInterpolationPositionError = 0;
        double maxShiftEccentricityError = 0;
        double maxInterpolationEccentricityError = 0;
        for (double dt = 0; dt < 241.0; dt += 1.0) {
            AbsoluteDate t         = initialOrbit.getDate().shiftedBy(dt);
            Vector3D shiftedP      = initialOrbit.shiftedBy(dt).getPVCoordinates().getPosition();
            Vector3D interpolatedP = initialOrbit.interpolate(t, sample).getPVCoordinates().getPosition();
            Vector3D propagatedP   = propagator.propagate(t).getPVCoordinates().getPosition();
            double shiftedE        = initialOrbit.shiftedBy(dt).getE();
            double interpolatedE   = initialOrbit.interpolate(t, sample).getE();
            double propagatedE     = propagator.propagate(t).getE();
            maxShiftPositionError = FastMath.max(maxShiftPositionError, shiftedP.subtract(propagatedP).getNorm());
            maxInterpolationPositionError = FastMath.max(maxInterpolationPositionError, interpolatedP.subtract(propagatedP).getNorm());
            maxShiftEccentricityError = FastMath.max(maxShiftEccentricityError, FastMath.abs(shiftedE - propagatedE));
            maxInterpolationEccentricityError = FastMath.max(maxInterpolationEccentricityError, FastMath.abs(interpolatedE - propagatedE));
        }
        Assert.assertTrue(maxShiftPositionError             > 390.0);
        Assert.assertTrue(maxInterpolationPositionError     < 62.0);
        Assert.assertTrue(maxShiftEccentricityError         > 4.5e-4);
        Assert.assertTrue(maxInterpolationEccentricityError < 2.6e-5);

        // slightly past sample end, bad eccentricity interpolation shows up
        // (in this case, interpolated eccentricity exceeds 1.0 btween 1900
        // and 1910s, while semi-majaxis remains positive, so this is not
        // even a proper hyperbolic orbit...)
        maxShiftPositionError = 0;
        maxInterpolationPositionError = 0;
        maxShiftEccentricityError = 0;
        maxInterpolationEccentricityError = 0;
        for (double dt = 240; dt < 600; dt += 1.0) {
            AbsoluteDate t         = initialOrbit.getDate().shiftedBy(dt);
            Vector3D shiftedP      = initialOrbit.shiftedBy(dt).getPVCoordinates().getPosition();
            Vector3D interpolatedP = initialOrbit.interpolate(t, sample).getPVCoordinates().getPosition();
            Vector3D propagatedP   = propagator.propagate(t).getPVCoordinates().getPosition();
            double shiftedE        = initialOrbit.shiftedBy(dt).getE();
            double interpolatedE   = initialOrbit.interpolate(t, sample).getE();
            double propagatedE     = propagator.propagate(t).getE();
            maxShiftPositionError = FastMath.max(maxShiftPositionError, shiftedP.subtract(propagatedP).getNorm());
            maxInterpolationPositionError = FastMath.max(maxInterpolationPositionError, interpolatedP.subtract(propagatedP).getNorm());
            maxShiftEccentricityError = FastMath.max(maxShiftEccentricityError, FastMath.abs(shiftedE - propagatedE));
            maxInterpolationEccentricityError = FastMath.max(maxInterpolationEccentricityError, FastMath.abs(interpolatedE - propagatedE));
        }
        Assert.assertTrue(maxShiftPositionError             <  2200.0);
        Assert.assertTrue(maxInterpolationPositionError     > 72000.0);
        Assert.assertTrue(maxShiftEccentricityError         <  1.2e-3);
        Assert.assertTrue(maxInterpolationEccentricityError >  3.8e-3);

    }

    @Test
    public void testPerfectlyEquatorialConversion() throws OrekitException {
        KeplerianOrbit initial = new KeplerianOrbit(13378000.0, 0.05, 0.0, 0.0, FastMath.PI,
                                                    0.0, PositionAngle.MEAN,
                                                    FramesFactory.getEME2000(), date,
                                                    Constants.EIGEN5C_EARTH_MU);
        EquinoctialOrbit equ = (EquinoctialOrbit) OrbitType.EQUINOCTIAL.convertType(initial);
        KeplerianOrbit converted = (KeplerianOrbit) OrbitType.KEPLERIAN.convertType(equ);
        Assert.assertEquals(FastMath.PI,
                            MathUtils.normalizeAngle(converted.getRightAscensionOfAscendingNode() +
                                                     converted.getPerigeeArgument(), FastMath.PI),
                            1.0e-10);
    }

    @Test
    public void testKeplerianDerivatives() {
        final KeplerianOrbit o = new KeplerianOrbit(new PVCoordinates(new Vector3D(-4947831., -3765382., -3708221.),
                                                                      new Vector3D(-2079., 5291., -7842.)),
                                                    FramesFactory.getEME2000(), date, 3.9860047e14);
        final Vector3D p = o.getPVCoordinates().getPosition();
        final Vector3D v = o.getPVCoordinates().getVelocity();
        final Vector3D a = o.getPVCoordinates().getAcceleration();

        // check that despite we did not provide acceleration, it got recomputed
        Assert.assertEquals(7.605422, a.getNorm(), 1.0e-6);

        FiniteDifferencesDifferentiator differentiator = new FiniteDifferencesDifferentiator(8, 0.1);

        // check velocity is the derivative of position
        double vx = differentiator.differentiate(new UnivariateFunction() {
            public double value(double dt) {
                return o.shiftedBy(dt).getPVCoordinates().getPosition().getX();
            }
        }).value(new DerivativeStructure(1, 1, 0, 0.0)).getPartialDerivative(1);
        Assert.assertEquals(o.getPVCoordinates().getVelocity().getX(), vx, 3.0e-12 * v.getNorm());
        double vy = differentiator.differentiate(new UnivariateFunction() {
            public double value(double dt) {
                return o.shiftedBy(dt).getPVCoordinates().getPosition().getY();
            }
        }).value(new DerivativeStructure(1, 1, 0, 0.0)).getPartialDerivative(1);
        Assert.assertEquals(o.getPVCoordinates().getVelocity().getY(), vy, 3.0e-12 * v.getNorm());
        double vz = differentiator.differentiate(new UnivariateFunction() {
            public double value(double dt) {
                return o.shiftedBy(dt).getPVCoordinates().getPosition().getZ();
            }
        }).value(new DerivativeStructure(1, 1, 0, 0.0)).getPartialDerivative(1);
        Assert.assertEquals(o.getPVCoordinates().getVelocity().getZ(), vz, 3.0e-12 * v.getNorm());

        // check acceleration is the derivative of velocity
        double ax = differentiator.differentiate(new UnivariateFunction() {
            public double value(double dt) {
                return o.shiftedBy(dt).getPVCoordinates().getVelocity().getX();
            }
        }).value(new DerivativeStructure(1, 1, 0, 0.0)).getPartialDerivative(1);
        Assert.assertEquals(o.getPVCoordinates().getAcceleration().getX(), ax, 3.0e-12 * a.getNorm());
        double ay = differentiator.differentiate(new UnivariateFunction() {
            public double value(double dt) {
                return o.shiftedBy(dt).getPVCoordinates().getVelocity().getY();
            }
        }).value(new DerivativeStructure(1, 1, 0, 0.0)).getPartialDerivative(1);
        Assert.assertEquals(o.getPVCoordinates().getAcceleration().getY(), ay, 3.0e-12 * a.getNorm());
        double az = differentiator.differentiate(new UnivariateFunction() {
            public double value(double dt) {
                return o.shiftedBy(dt).getPVCoordinates().getVelocity().getZ();
            }
        }).value(new DerivativeStructure(1, 1, 0, 0.0)).getPartialDerivative(1);
        Assert.assertEquals(o.getPVCoordinates().getAcceleration().getZ(), az, 3.0e-12 * a.getNorm());

        // check jerk is the derivative of acceleration
        final double r2 = p.getNormSq();
        final double r  = FastMath.sqrt(r2);
        Vector3D keplerianJerk = new Vector3D(-3 * Vector3D.dotProduct(p, v) / r2, a, -a.getNorm() / r, v);
        double jx = differentiator.differentiate(new UnivariateFunction() {
            public double value(double dt) {
                return o.shiftedBy(dt).getPVCoordinates().getAcceleration().getX();
            }
        }).value(new DerivativeStructure(1, 1, 0, 0.0)).getPartialDerivative(1);
        Assert.assertEquals(keplerianJerk.getX(), jx, 3.0e-12 * keplerianJerk.getNorm());
        double jy = differentiator.differentiate(new UnivariateFunction() {
            public double value(double dt) {
                return o.shiftedBy(dt).getPVCoordinates().getAcceleration().getY();
            }
        }).value(new DerivativeStructure(1, 1, 0, 0.0)).getPartialDerivative(1);
        Assert.assertEquals(keplerianJerk.getY(), jy, 3.0e-12 * keplerianJerk.getNorm());
        double jz = differentiator.differentiate(new UnivariateFunction() {
            public double value(double dt) {
                return o.shiftedBy(dt).getPVCoordinates().getAcceleration().getZ();
            }
        }).value(new DerivativeStructure(1, 1, 0, 0.0)).getPartialDerivative(1);
        Assert.assertEquals(keplerianJerk.getZ(), jz, 3.0e-12 * keplerianJerk.getNorm());

    }

    @Test
    public void testSerialization()
            throws IOException, ClassNotFoundException, NoSuchFieldException, IllegalAccessException {
        Vector3D position = new Vector3D(-29536113.0, 30329259.0, -100125.0);
        Vector3D velocity = new Vector3D(-2194.0, -2141.0, -8.0);
        PVCoordinates pvCoordinates = new PVCoordinates( position, velocity);
        KeplerianOrbit orbit = new KeplerianOrbit(pvCoordinates, FramesFactory.getEME2000(), date, mu);
        Assert.assertEquals(42255170.003, orbit.getA(), 1.0e-3);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream    oos = new ObjectOutputStream(bos);
        oos.writeObject(orbit);

        Assert.assertTrue(bos.size() > 250);
        Assert.assertTrue(bos.size() < 350);

        ByteArrayInputStream  bis = new ByteArrayInputStream(bos.toByteArray());
        ObjectInputStream     ois = new ObjectInputStream(bis);
        KeplerianOrbit deserialized  = (KeplerianOrbit) ois.readObject();
        Assert.assertEquals(orbit.getA(), deserialized.getA(), 1.0e-10);
        Assert.assertEquals(orbit.getE(), deserialized.getE(), 1.0e-10);
        Assert.assertEquals(orbit.getI(), deserialized.getI(), 1.0e-10);
        Assert.assertEquals(orbit.getPerigeeArgument(), deserialized.getPerigeeArgument(), 1.0e-10);
        Assert.assertEquals(orbit.getRightAscensionOfAscendingNode(), deserialized.getRightAscensionOfAscendingNode(), 1.0e-10);
        Assert.assertEquals(orbit.getTrueAnomaly(), deserialized.getTrueAnomaly(), 1.0e-10);
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
