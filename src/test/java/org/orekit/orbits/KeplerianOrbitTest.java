/* Copyright 2002-2020 CS Group
 * Licensed to CS Group (CS) under one or more
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
import java.util.function.Function;

import org.hipparchus.analysis.UnivariateFunction;
import org.hipparchus.analysis.differentiation.DSFactory;
import org.hipparchus.analysis.differentiation.FiniteDifferencesDifferentiator;
import org.hipparchus.analysis.differentiation.UnivariateDifferentiableFunction;
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
import org.orekit.errors.OrekitIllegalArgumentException;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.Transform;
import org.orekit.propagation.analytical.EcksteinHechlerPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;

import static org.orekit.OrekitMatchers.relativelyCloseTo;

public class KeplerianOrbitTest {

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

        KeplerianOrbit param = new KeplerianOrbit(new PVCoordinates(pos, vit),
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

        KeplerianOrbit paramCir = new KeplerianOrbit(new PVCoordinates(posCir, vitCir),
                                                     FramesFactory.getEME2000(), date, mu);
        Assert.assertEquals(paramCir.getA(), kepCir.getA(), Utils.epsilonTest * kepCir.getA());
        Assert.assertEquals(paramCir.getE(), kepCir.getE(), Utils.epsilonE * FastMath.max(1., FastMath.abs(kepCir.getE())));
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

        KeplerianOrbit paramHyp = new KeplerianOrbit(new PVCoordinates(posHyp, vitHyp),
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
        Assert.assertEquals(MathUtils.normalizeAngle(2*FastMath.asin(FastMath.sqrt((FastMath.pow(0.652494417368829e-01, 2)+FastMath.pow(0.103158450084864, 2))/4.)), kep.getI()), kep.getI(), Utils.epsilonAngle * FastMath.abs(kep.getI()));
        Assert.assertEquals(MathUtils.normalizeAngle(0.416203300000000e+01, kep.getLM()), kep.getLM(), Utils.epsilonAngle * FastMath.abs(kep.getLM()));

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

        p = new KeplerianOrbit(p.getA(), p.getE(), p.getI(), p.getPerigeeArgument(),
                                    p.getRightAscensionOfAscendingNode(), v , PositionAngle.TRUE,
                                    p.getFrame(), p.getDate(), p.getMu());
        Assert.assertEquals(p.getTrueAnomaly(), v, Utils.epsilonAngle * FastMath.abs(v));
        Assert.assertEquals(p.getEccentricAnomaly(), E, Utils.epsilonAngle * FastMath.abs(E));
        Assert.assertEquals(p.getMeanAnomaly(), M, Utils.epsilonAngle * FastMath.abs(M));
        p = new KeplerianOrbit(p.getA(), p.getE(), p.getI(), p.getPerigeeArgument(),
                                    p.getRightAscensionOfAscendingNode(), 0 , PositionAngle.TRUE,
                                    p.getFrame(), p.getDate(), p.getMu());

        p = new KeplerianOrbit(p.getA(), p.getE(), p.getI(), p.getPerigeeArgument(),
                                    p.getRightAscensionOfAscendingNode(), E , PositionAngle.ECCENTRIC,
                                    p.getFrame(), p.getDate(), p.getMu());
        Assert.assertEquals(p.getTrueAnomaly(), v, Utils.epsilonAngle * FastMath.abs(v));
        Assert.assertEquals(p.getEccentricAnomaly(), E, Utils.epsilonAngle * FastMath.abs(E));
        Assert.assertEquals(p.getMeanAnomaly(), M, Utils.epsilonAngle * FastMath.abs(M));
        p = new KeplerianOrbit(p.getA(), p.getE(), p.getI(), p.getPerigeeArgument(),
                                    p.getRightAscensionOfAscendingNode(), 0 , PositionAngle.TRUE,
                                    p.getFrame(), p.getDate(), p.getMu());

        p = new KeplerianOrbit(p.getA(), p.getE(), p.getI(), p.getPerigeeArgument(),
                                    p.getRightAscensionOfAscendingNode(), M, PositionAngle.MEAN,
                                    p.getFrame(), p.getDate(), p.getMu());
        Assert.assertEquals(p.getTrueAnomaly(), v, Utils.epsilonAngle * FastMath.abs(v));
        Assert.assertEquals(p.getEccentricAnomaly(), E, Utils.epsilonAngle * FastMath.abs(E));
        Assert.assertEquals(p.getMeanAnomaly(), M, Utils.epsilonAngle * FastMath.abs(M));

        // circular orbit
        p = new KeplerianOrbit(p.getA(), 0, p.getI(), p.getPerigeeArgument(),
                                    p.getRightAscensionOfAscendingNode(), p.getLv() , PositionAngle.TRUE,
                                    p.getFrame(), p.getDate(), p.getMu());

        E = v;
        M = E;

        p = new KeplerianOrbit(p.getA(), p.getE(), p.getI(), p.getPerigeeArgument(),
                                    p.getRightAscensionOfAscendingNode(), v , PositionAngle.TRUE,
                                    p.getFrame(), p.getDate(), p.getMu());
        Assert.assertEquals(p.getTrueAnomaly(), v, Utils.epsilonAngle * FastMath.abs(v));
        Assert.assertEquals(p.getEccentricAnomaly(), E, Utils.epsilonAngle * FastMath.abs(E));
        Assert.assertEquals(p.getMeanAnomaly(), M, Utils.epsilonAngle * FastMath.abs(M));
        p = new KeplerianOrbit(p.getA(), p.getE(), p.getI(), p.getPerigeeArgument(),
                                    p.getRightAscensionOfAscendingNode(), 0 , PositionAngle.TRUE,
                                    p.getFrame(), p.getDate(), p.getMu());

        p = new KeplerianOrbit(p.getA(), p.getE(), p.getI(), p.getPerigeeArgument(),
                                    p.getRightAscensionOfAscendingNode(), E , PositionAngle.ECCENTRIC, p.getFrame(), p.getDate(), p.getMu());
        Assert.assertEquals(p.getTrueAnomaly(), v, Utils.epsilonAngle * FastMath.abs(v));
        Assert.assertEquals(p.getEccentricAnomaly(), E, Utils.epsilonAngle * FastMath.abs(E));
        Assert.assertEquals(p.getMeanAnomaly(), M, Utils.epsilonAngle * FastMath.abs(M));
        p = new KeplerianOrbit(p.getA(), p.getE(), p.getI(), p.getPerigeeArgument(),
                                    p.getRightAscensionOfAscendingNode(), 0 , PositionAngle.TRUE,
                                    p.getFrame(), p.getDate(), p.getMu());

        p = new KeplerianOrbit(p.getA(), p.getE(), p.getI(), p.getPerigeeArgument(),
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
            p = new KeplerianOrbit(p.getA(), p.getE(), p.getI(), p.getPerigeeArgument(),
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
        p = new KeplerianOrbit(p.getA(), p.getE(), p.getI(), p.getPerigeeArgument(),
                               p.getRightAscensionOfAscendingNode(), 0 , PositionAngle.TRUE, p.getFrame(), p.getDate(), p.getMu());
        Assert.assertEquals(p.getPVCoordinates().getPosition().getNorm(), perigeeRadius, perigeeRadius * Utils.epsilonTest);

        p = new KeplerianOrbit(p.getA(), p.getE(), p.getI(), p.getPerigeeArgument(),
                               p.getRightAscensionOfAscendingNode(), FastMath.PI , PositionAngle.TRUE, p.getFrame(), p.getDate(), p.getMu());
        Assert.assertEquals(p.getPVCoordinates().getPosition().getNorm(), apogeeRadius, apogeeRadius * Utils.epsilonTest);

        // nodes
        // descending node
        p = new KeplerianOrbit(p.getA(), p.getE(), p.getI(), p.getPerigeeArgument(),
                               p.getRightAscensionOfAscendingNode(), FastMath.PI - p.getPerigeeArgument() , PositionAngle.TRUE,
                               p.getFrame(), p.getDate(), p.getMu());
        Assert.assertTrue(FastMath.abs(p.getPVCoordinates().getPosition().getZ()) < p.getPVCoordinates().getPosition().getNorm() * Utils.epsilonTest);
        Assert.assertTrue(p.getPVCoordinates().getVelocity().getZ() < 0);

        // ascending node
        p = new KeplerianOrbit(p.getA(), p.getE(), p.getI(), p.getPerigeeArgument(),
                               p.getRightAscensionOfAscendingNode(), 2.0 * FastMath.PI - p.getPerigeeArgument() , PositionAngle.TRUE,
                               p.getFrame(), p.getDate(), p.getMu());
        Assert.assertTrue(FastMath.abs(p.getPVCoordinates().getPosition().getZ()) < p.getPVCoordinates().getPosition().getNorm() * Utils.epsilonTest);
        Assert.assertTrue(p.getPVCoordinates().getVelocity().getZ() > 0);


        //  circular and equatorial orbit
        KeplerianOrbit pCirEqua =
            new KeplerianOrbit(24464560.0, 0.1e-10, 0.1e-8, 3.10686, 1.00681,
                               0.67, PositionAngle.TRUE, FramesFactory.getEME2000(), date, mu);

        position = pCirEqua.getPVCoordinates().getPosition();
        velocity = pCirEqua.getPVCoordinates().getVelocity();
        momentum = Vector3D.crossProduct(position, velocity).normalize();

        apogeeRadius  = pCirEqua.getA() * (1 + pCirEqua.getE());
        perigeeRadius = pCirEqua.getA() * (1 - pCirEqua.getE());
        // test if apogee equals perigee
        Assert.assertEquals(perigeeRadius, apogeeRadius, 1.e+4 * Utils.epsilonTest * apogeeRadius);

        for (double lv = 0; lv <= 2 * FastMath.PI; lv += 2 * FastMath.PI/100.) {
            pCirEqua = new KeplerianOrbit(pCirEqua.getA(), pCirEqua.getE(), pCirEqua.getI(), pCirEqua.getPerigeeArgument(),
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
    public void testHyperbola1() {
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
    public void testHyperbola2() {
        KeplerianOrbit orbit = new KeplerianOrbit(-10000000.0, 1.2, 0.3, 0, 0, -1.75,
                                                  PositionAngle.MEAN,
                                                  FramesFactory.getEME2000(), AbsoluteDate.J2000_EPOCH,
                                                  mu);
        Vector3D perigeeP  = new KeplerianOrbit(orbit.getA(), orbit.getE(), orbit.getI(),
                                                orbit.getPerigeeArgument(), orbit.getRightAscensionOfAscendingNode(),
                                                0.0, PositionAngle.TRUE, orbit.getFrame(),
                                                orbit.getDate(), orbit.getMu()).getPVCoordinates().getPosition();
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
            Assert.assertEquals(1.2, rebuilt.getE(), 1.0e-13);
        }
    }

    @Test
    public void testInconsistentHyperbola() {
        try {
            new KeplerianOrbit(+10000000.0, 2.5, 0.3, 0, 0, 0.0,
                                                  PositionAngle.TRUE,
                                                  FramesFactory.getEME2000(), AbsoluteDate.J2000_EPOCH,
                                                  mu);
            Assert.fail("an exception should have been thrown");
        } catch (OrekitIllegalArgumentException oe) {
            Assert.assertEquals(OrekitMessages.ORBIT_A_E_MISMATCH_WITH_CONIC_TYPE, oe.getSpecifier());
            Assert.assertEquals(+10000000.0, ((Double) oe.getParts()[0]).doubleValue(), 1.0e-3);
            Assert.assertEquals(2.5,         ((Double) oe.getParts()[1]).doubleValue(), 1.0e-15);
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
            Assert.assertEquals(M, E - e * FastMath.sin(E), 4.0e-13);
        }

    }

    @Test(expected=IllegalArgumentException.class)
    public void testOutOfRangeV() {
        new KeplerianOrbit(-7000434.460140012, 1.1999785407363386, 1.3962787004479158,
                           1.3962320168955138, 0.3490728321331678, -2.55593407037698,
                           PositionAngle.TRUE, FramesFactory.getEME2000(),
                           new AbsoluteDate("2000-01-01T12:00:00.391", TimeScalesFactory.getUTC()),
                           3.986004415E14);
    }

    @Test
    public void testNumericalIssue25() {
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
    public void testPerfectlyEquatorial() {
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
    public void testJacobianReferenceEllipse() {

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
    public void testJacobianFinitedifferencesEllipse() {

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
    public void testJacobianReferenceHyperbola() {

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
    public void testJacobianFinitedifferencesHyperbola() {

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
        {
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
    public void testInterpolationWithDerivatives() {
        doTestInterpolation(true,
                            397, 4.01, 4.75e-4, 1.28e-7,
                            2159, 1.05e7, 1.19e-3, 0.773);
    }

    @Test
    public void testInterpolationWithoutDerivatives() {
        doTestInterpolation(false,
                            397, 62.0, 4.75e-4, 2.87e-6,
                            2159, 79365, 1.19e-3, 3.89e-3);
    }

    private void doTestInterpolation(boolean useDerivatives,
                                     double shiftPositionErrorWithin, double interpolationPositionErrorWithin,
                                     double shiftEccentricityErrorWithin, double interpolationEccentricityErrorWithin,
                                     double shiftPositionErrorSlightlyPast, double interpolationPositionErrorSlightlyPast,
                                     double shiftEccentricityErrorSlightlyPast, double interpolationEccentricityErrorSlightlyPast)
        {

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
            Orbit orbit = propagator.propagate(date.shiftedBy(dt)).getOrbit();
            if (!useDerivatives) {
                // remove derivatives
                double[] stateVector = new double[6];
                orbit.getType().mapOrbitToArray(orbit, PositionAngle.TRUE, stateVector, null);
                orbit = orbit.getType().mapArrayToOrbit(stateVector, null, PositionAngle.TRUE,
                                                        orbit.getDate(), orbit.getMu(), orbit.getFrame());
            }
            sample.add(orbit);
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
        Assert.assertEquals(shiftPositionErrorWithin,             maxShiftPositionError,             0.01 * shiftPositionErrorWithin);
        Assert.assertEquals(interpolationPositionErrorWithin,     maxInterpolationPositionError,     0.01 * interpolationPositionErrorWithin);
        Assert.assertEquals(shiftEccentricityErrorWithin,         maxShiftEccentricityError,         0.01 * shiftEccentricityErrorWithin);
        Assert.assertEquals(interpolationEccentricityErrorWithin, maxInterpolationEccentricityError, 0.01 * interpolationEccentricityErrorWithin);

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
        Assert.assertEquals(shiftPositionErrorSlightlyPast,             maxShiftPositionError,             0.01 * shiftPositionErrorSlightlyPast);
        Assert.assertEquals(interpolationPositionErrorSlightlyPast,     maxInterpolationPositionError,     0.01 * interpolationPositionErrorSlightlyPast);
        Assert.assertEquals(shiftEccentricityErrorSlightlyPast,         maxShiftEccentricityError,         0.01 * shiftEccentricityErrorSlightlyPast);
        Assert.assertEquals(interpolationEccentricityErrorSlightlyPast, maxInterpolationEccentricityError, 0.01 * interpolationEccentricityErrorSlightlyPast);

    }

    @Test
    public void testPerfectlyEquatorialConversion() {
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

        final KeplerianOrbit orbit = new KeplerianOrbit(new PVCoordinates(new Vector3D(-4947831., -3765382., -3708221.),
                                                                          new Vector3D(-2079., 5291., -7842.)),
                                                        FramesFactory.getEME2000(), date, 3.9860047e14);
        final Vector3D p = orbit.getPVCoordinates().getPosition();
        final Vector3D v = orbit.getPVCoordinates().getVelocity();
        final Vector3D a = orbit.getPVCoordinates().getAcceleration();

        // check that despite we did not provide acceleration, it got recomputed
        Assert.assertEquals(7.605422, a.getNorm(), 1.0e-6);

        // check velocity is the derivative of position
        Assert.assertEquals(differentiate(orbit, shifted -> shifted.getPVCoordinates().getPosition().getX()),
                            orbit.getPVCoordinates().getVelocity().getX(),
                            3.0e-12 * v.getNorm());
        Assert.assertEquals(differentiate(orbit, shifted -> shifted.getPVCoordinates().getPosition().getY()),
                            orbit.getPVCoordinates().getVelocity().getY(),
                            3.0e-12 * v.getNorm());
        Assert.assertEquals(differentiate(orbit, shifted -> shifted.getPVCoordinates().getPosition().getZ()),
                            orbit.getPVCoordinates().getVelocity().getZ(),
                            3.0e-12 * v.getNorm());

        // check acceleration is the derivative of velocity
        Assert.assertEquals(differentiate(orbit, shifted -> shifted.getPVCoordinates().getVelocity().getX()),
                            orbit.getPVCoordinates().getAcceleration().getX(),
                            3.0e-12 * a.getNorm());
        Assert.assertEquals(differentiate(orbit, shifted -> shifted.getPVCoordinates().getVelocity().getY()),
                            orbit.getPVCoordinates().getAcceleration().getY(),
                            3.0e-12 * a.getNorm());
        Assert.assertEquals(differentiate(orbit, shifted -> shifted.getPVCoordinates().getVelocity().getZ()),
                            orbit.getPVCoordinates().getAcceleration().getZ(),
                            3.0e-12 * a.getNorm());

        // check jerk is the derivative of acceleration
        final double r2 = p.getNormSq();
        final double r  = FastMath.sqrt(r2);
        Vector3D keplerianJerk = new Vector3D(-3 * Vector3D.dotProduct(p, v) / r2, a, -a.getNorm() / r, v);
        Assert.assertEquals(differentiate(orbit, shifted -> shifted.getPVCoordinates().getAcceleration().getX()),
                            keplerianJerk.getX(),
                            3.0e-12 * keplerianJerk.getNorm());
        Assert.assertEquals(differentiate(orbit, shifted -> shifted.getPVCoordinates().getAcceleration().getY()),
                            keplerianJerk.getY(),
                            3.0e-12 * keplerianJerk.getNorm());
        Assert.assertEquals(differentiate(orbit, shifted -> shifted.getPVCoordinates().getAcceleration().getZ()),
                            keplerianJerk.getZ(),
                            3.0e-12 * keplerianJerk.getNorm());

        Assert.assertTrue(Double.isNaN(orbit.getADot()));
        Assert.assertTrue(Double.isNaN(orbit.getEquinoctialExDot()));
        Assert.assertTrue(Double.isNaN(orbit.getEquinoctialEyDot()));
        Assert.assertTrue(Double.isNaN(orbit.getHxDot()));
        Assert.assertTrue(Double.isNaN(orbit.getHyDot()));
        Assert.assertTrue(Double.isNaN(orbit.getLvDot()));
        Assert.assertTrue(Double.isNaN(orbit.getLEDot()));
        Assert.assertTrue(Double.isNaN(orbit.getLMDot()));
        Assert.assertTrue(Double.isNaN(orbit.getEDot()));
        Assert.assertTrue(Double.isNaN(orbit.getIDot()));
        Assert.assertTrue(Double.isNaN(orbit.getPerigeeArgumentDot()));
        Assert.assertTrue(Double.isNaN(orbit.getRightAscensionOfAscendingNodeDot()));
        Assert.assertTrue(Double.isNaN(orbit.getTrueAnomalyDot()));
        Assert.assertTrue(Double.isNaN(orbit.getEccentricAnomalyDot()));
        Assert.assertTrue(Double.isNaN(orbit.getMeanAnomalyDot()));
        Assert.assertTrue(Double.isNaN(orbit.getAnomalyDot(PositionAngle.TRUE)));
        Assert.assertTrue(Double.isNaN(orbit.getAnomalyDot(PositionAngle.ECCENTRIC)));
        Assert.assertTrue(Double.isNaN(orbit.getAnomalyDot(PositionAngle.MEAN)));

    }

    private <S extends Function<KeplerianOrbit, Double>>
    double differentiate(KeplerianOrbit orbit, S picker) {
        final DSFactory factory = new DSFactory(1, 1);
        FiniteDifferencesDifferentiator differentiator = new FiniteDifferencesDifferentiator(8, 0.1);
        UnivariateDifferentiableFunction diff = differentiator.differentiate(new UnivariateFunction() {
            public double value(double dt) {
                return picker.apply(orbit.shiftedBy(dt));
            }
        });
        return diff.value(factory.variable(0, 0.0)).getPartialDerivative(1);
     }

    @Test
    public void testNonKeplerianEllipticDerivatives() {
        final AbsoluteDate date         = new AbsoluteDate("2003-05-01T00:00:20.000", TimeScalesFactory.getUTC());
        final Vector3D     position     = new Vector3D(6896874.444705,  1956581.072644,  -147476.245054);
        final Vector3D     velocity     = new Vector3D(166.816407662, -1106.783301861, -7372.745712770);
        final Vector3D     acceleration = new Vector3D(-7.466182457944, -2.118153357345,  0.160004048437);
        final TimeStampedPVCoordinates pv = new TimeStampedPVCoordinates(date, position, velocity, acceleration);
        final Frame frame = FramesFactory.getEME2000();
        final double mu   = Constants.EIGEN5C_EARTH_MU;
        final KeplerianOrbit orbit = new KeplerianOrbit(pv, frame, mu);

        Assert.assertEquals(differentiate(pv, frame, mu, shifted -> shifted.getA()),
                            orbit.getADot(),
                            4.3e-8);
        Assert.assertEquals(differentiate(pv, frame, mu, shifted -> shifted.getEquinoctialEx()),
                            orbit.getEquinoctialExDot(),
                            2.1e-15);
        Assert.assertEquals(differentiate(pv, frame, mu, shifted -> shifted.getEquinoctialEy()),
                            orbit.getEquinoctialEyDot(),
                            5.3e-16);
        Assert.assertEquals(differentiate(pv, frame, mu, shifted -> shifted.getHx()),
                            orbit.getHxDot(),
                            1.6e-15);
        Assert.assertEquals(differentiate(pv, frame, mu, shifted -> shifted.getHy()),
                            orbit.getHyDot(),
                            7.3e-17);
        Assert.assertEquals(differentiate(pv, frame, mu, shifted -> shifted.getLv()),
                            orbit.getLvDot(),
                            1.1e-14);
        Assert.assertEquals(differentiate(pv, frame, mu, shifted -> shifted.getLE()),
                            orbit.getLEDot(),
                            7.2e-15);
        Assert.assertEquals(differentiate(pv, frame, mu, shifted -> shifted.getLM()),
                            orbit.getLMDot(),
                            4.7e-15);
        Assert.assertEquals(differentiate(pv, frame, mu, shifted -> shifted.getE()),
                            orbit.getEDot(),
                            6.9e-16);
        Assert.assertEquals(differentiate(pv, frame, mu, shifted -> shifted.getI()),
                            orbit.getIDot(),
                            5.8e-16);
        Assert.assertEquals(differentiate(pv, frame, mu, shifted -> shifted.getPerigeeArgument()),
                            orbit.getPerigeeArgumentDot(),
                            1.5e-12);
        Assert.assertEquals(differentiate(pv, frame, mu, shifted -> shifted.getRightAscensionOfAscendingNode()),
                            orbit.getRightAscensionOfAscendingNodeDot(),
                            1.5e-15);
        Assert.assertEquals(differentiate(pv, frame, mu, shifted -> shifted.getTrueAnomaly()),
                            orbit.getTrueAnomalyDot(),
                            1.5e-12);
        Assert.assertEquals(differentiate(pv, frame, mu, shifted -> shifted.getEccentricAnomaly()),
                            orbit.getEccentricAnomalyDot(),
                            1.5e-12);
        Assert.assertEquals(differentiate(pv, frame, mu, shifted -> shifted.getMeanAnomaly()),
                            orbit.getMeanAnomalyDot(),
                            1.5e-12);
        Assert.assertEquals(differentiate(pv, frame, mu, shifted -> shifted.getAnomaly(PositionAngle.TRUE)),
                            orbit.getAnomalyDot(PositionAngle.TRUE),
                            1.5e-12);
        Assert.assertEquals(differentiate(pv, frame, mu, shifted -> shifted.getAnomaly(PositionAngle.ECCENTRIC)),
                            orbit.getAnomalyDot(PositionAngle.ECCENTRIC),
                            1.5e-12);
        Assert.assertEquals(differentiate(pv, frame, mu, shifted -> shifted.getAnomaly(PositionAngle.MEAN)),
                            orbit.getAnomalyDot(PositionAngle.MEAN),
                            1.5e-12);

    }

    @Test
    public void testNonKeplerianHyperbolicDerivatives() {
        final AbsoluteDate date         = new AbsoluteDate("2003-05-01T00:00:20.000", TimeScalesFactory.getUTC());
        final Vector3D     position     = new Vector3D(224267911.905821, 290251613.109399, 45534292.777492);
        final Vector3D     velocity     = new Vector3D(-1494.068165293, 1124.771027677, 526.915286134);
        final Vector3D     acceleration = new Vector3D(-0.001295920501, -0.002233045187, -0.000349906292);
        final TimeStampedPVCoordinates pv = new TimeStampedPVCoordinates(date, position, velocity, acceleration);
        final Frame frame = FramesFactory.getEME2000();
        final double mu   = Constants.EIGEN5C_EARTH_MU;
        final KeplerianOrbit orbit = new KeplerianOrbit(pv, frame, mu);

        Assert.assertEquals(differentiate(pv, frame, mu, shifted -> shifted.getA()),
                            orbit.getADot(),
                            9.6e-8);
        Assert.assertEquals(differentiate(pv, frame, mu, shifted -> shifted.getEquinoctialEx()),
                            orbit.getEquinoctialExDot(),
                            2.8e-16);
        Assert.assertEquals(differentiate(pv, frame, mu, shifted -> shifted.getEquinoctialEy()),
                            orbit.getEquinoctialEyDot(),
                            3.6e-15);
        Assert.assertEquals(differentiate(pv, frame, mu, shifted -> shifted.getHx()),
                            orbit.getHxDot(),
                            1.4e-15);
        Assert.assertEquals(differentiate(pv, frame, mu, shifted -> shifted.getHy()),
                            orbit.getHyDot(),
                            9.4e-16);
        Assert.assertEquals(differentiate(pv, frame, mu, shifted -> shifted.getLv()),
                            orbit.getLvDot(),
                            5.6e-16);
        Assert.assertEquals(differentiate(pv, frame, mu, shifted -> shifted.getLE()),
                            orbit.getLEDot(),
                            9.0e-16);
        Assert.assertEquals(differentiate(pv, frame, mu, shifted -> shifted.getLM()),
                            orbit.getLMDot(),
                            1.8e-15);
        Assert.assertEquals(differentiate(pv, frame, mu, shifted -> shifted.getE()),
                            orbit.getEDot(),
                            1.8e-15);
        Assert.assertEquals(differentiate(pv, frame, mu, shifted -> shifted.getI()),
                            orbit.getIDot(),
                            3.6e-15);
        Assert.assertEquals(differentiate(pv, frame, mu, shifted -> shifted.getPerigeeArgument()),
                            orbit.getPerigeeArgumentDot(),
                            9.4e-16);
        Assert.assertEquals(differentiate(pv, frame, mu, shifted -> shifted.getRightAscensionOfAscendingNode()),
                            orbit.getRightAscensionOfAscendingNodeDot(),
                            1.1e-15);
        Assert.assertEquals(differentiate(pv, frame, mu, shifted -> shifted.getTrueAnomaly()),
                            orbit.getTrueAnomalyDot(),
                            1.4e-15);
        Assert.assertEquals(differentiate(pv, frame, mu, shifted -> shifted.getEccentricAnomaly()),
                            orbit.getEccentricAnomalyDot(),
                            9.2e-16);
        Assert.assertEquals(differentiate(pv, frame, mu, shifted -> shifted.getMeanAnomaly()),
                            orbit.getMeanAnomalyDot(),
                            1.4e-15);
        Assert.assertEquals(differentiate(pv, frame, mu, shifted -> shifted.getAnomaly(PositionAngle.TRUE)),
                            orbit.getAnomalyDot(PositionAngle.TRUE),
                            1.4e-15);
        Assert.assertEquals(differentiate(pv, frame, mu, shifted -> shifted.getAnomaly(PositionAngle.ECCENTRIC)),
                            orbit.getAnomalyDot(PositionAngle.ECCENTRIC),
                            9.2e-16);
        Assert.assertEquals(differentiate(pv, frame, mu, shifted -> shifted.getAnomaly(PositionAngle.MEAN)),
                            orbit.getAnomalyDot(PositionAngle.MEAN),
                            1.4e-15);

    }

    private <S extends Function<KeplerianOrbit, Double>>
    double differentiate(TimeStampedPVCoordinates pv, Frame frame, double mu, S picker) {
        final DSFactory factory = new DSFactory(1, 1);
        FiniteDifferencesDifferentiator differentiator = new FiniteDifferencesDifferentiator(8, 0.1);
        UnivariateDifferentiableFunction diff = differentiator.differentiate(new UnivariateFunction() {
            public double value(double dt) {
                return picker.apply(new KeplerianOrbit(pv.shiftedBy(dt), frame, mu));
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
        final KeplerianOrbit orbit = new KeplerianOrbit(pv, frame, mu);

        for (PositionAngle type : PositionAngle.values()) {
            final KeplerianOrbit rebuilt = new KeplerianOrbit(orbit.getA(),
                                                              orbit.getE(),
                                                              orbit.getI(),
                                                              orbit.getPerigeeArgument(),
                                                              orbit.getRightAscensionOfAscendingNode(),
                                                              orbit.getAnomaly(type),
                                                              orbit.getADot(),
                                                              orbit.getEDot(),
                                                              orbit.getIDot(),
                                                              orbit.getPerigeeArgumentDot(),
                                                              orbit.getRightAscensionOfAscendingNodeDot(),
                                                              orbit.getAnomalyDot(type),
                                                              type, orbit.getFrame(), orbit.getDate(), orbit.getMu());
            Assert.assertThat(rebuilt.getA(),                                relativelyCloseTo(orbit.getA(),                                1));
            Assert.assertThat(rebuilt.getE(),                                relativelyCloseTo(orbit.getE(),                                1));
            Assert.assertThat(rebuilt.getI(),                                relativelyCloseTo(orbit.getI(),                                1));
            Assert.assertThat(rebuilt.getPerigeeArgument(),                  relativelyCloseTo(orbit.getPerigeeArgument(),                  1));
            Assert.assertThat(rebuilt.getRightAscensionOfAscendingNode(),    relativelyCloseTo(orbit.getRightAscensionOfAscendingNode(),    1));
            Assert.assertThat(rebuilt.getADot(),                             relativelyCloseTo(orbit.getADot(),                             1));
            Assert.assertThat(rebuilt.getEDot(),                             relativelyCloseTo(orbit.getEDot(),                             1));
            Assert.assertThat(rebuilt.getIDot(),                             relativelyCloseTo(orbit.getIDot(),                             1));
            Assert.assertThat(rebuilt.getPerigeeArgumentDot(),               relativelyCloseTo(orbit.getPerigeeArgumentDot(),               1));
            Assert.assertThat(rebuilt.getRightAscensionOfAscendingNodeDot(), relativelyCloseTo(orbit.getRightAscensionOfAscendingNodeDot(), 1));
            for (PositionAngle type2 : PositionAngle.values()) {
                Assert.assertThat(rebuilt.getAnomaly(type2),    relativelyCloseTo(orbit.getAnomaly(type2),    1));
                Assert.assertThat(rebuilt.getAnomalyDot(type2), relativelyCloseTo(orbit.getAnomalyDot(type2), 1));
            }
        }

    }

    @Test
    public void testPositionAngleHyperbolicDerivatives() {
        final AbsoluteDate date         = new AbsoluteDate("2003-05-01T00:00:20.000", TimeScalesFactory.getUTC());
        final Vector3D     position     = new Vector3D(224267911.905821, 290251613.109399, 45534292.777492);
        final Vector3D     velocity     = new Vector3D(-1494.068165293, 1124.771027677, 526.915286134);
        final Vector3D     acceleration = new Vector3D(-0.001295920501, -0.002233045187, -0.000349906292);
        final TimeStampedPVCoordinates pv = new TimeStampedPVCoordinates(date, position, velocity, acceleration);
        final Frame frame = FramesFactory.getEME2000();
        final double mu   = Constants.EIGEN5C_EARTH_MU;
        final KeplerianOrbit orbit = new KeplerianOrbit(pv, frame, mu);

        for (PositionAngle type : PositionAngle.values()) {
            final KeplerianOrbit rebuilt = new KeplerianOrbit(orbit.getA(),
                                                              orbit.getE(),
                                                              orbit.getI(),
                                                              orbit.getPerigeeArgument(),
                                                              orbit.getRightAscensionOfAscendingNode(),
                                                              orbit.getAnomaly(type),
                                                              orbit.getADot(),
                                                              orbit.getEDot(),
                                                              orbit.getIDot(),
                                                              orbit.getPerigeeArgumentDot(),
                                                              orbit.getRightAscensionOfAscendingNodeDot(),
                                                              orbit.getAnomalyDot(type),
                                                              type, orbit.getFrame(), orbit.getDate(), orbit.getMu());
            Assert.assertThat(rebuilt.getA(),                                relativelyCloseTo(orbit.getA(),                                1));
            Assert.assertThat(rebuilt.getE(),                                relativelyCloseTo(orbit.getE(),                                1));
            Assert.assertThat(rebuilt.getI(),                                relativelyCloseTo(orbit.getI(),                                1));
            Assert.assertThat(rebuilt.getPerigeeArgument(),                  relativelyCloseTo(orbit.getPerigeeArgument(),                  1));
            Assert.assertThat(rebuilt.getRightAscensionOfAscendingNode(),    relativelyCloseTo(orbit.getRightAscensionOfAscendingNode(),    1));
            Assert.assertThat(rebuilt.getADot(),                             relativelyCloseTo(orbit.getADot(),                             1));
            Assert.assertThat(rebuilt.getEDot(),                             relativelyCloseTo(orbit.getEDot(),                             1));
            Assert.assertThat(rebuilt.getIDot(),                             relativelyCloseTo(orbit.getIDot(),                             1));
            Assert.assertThat(rebuilt.getPerigeeArgumentDot(),               relativelyCloseTo(orbit.getPerigeeArgumentDot(),               1));
            Assert.assertThat(rebuilt.getRightAscensionOfAscendingNodeDot(), relativelyCloseTo(orbit.getRightAscensionOfAscendingNodeDot(), 1));
            for (PositionAngle type2 : PositionAngle.values()) {
                Assert.assertThat(rebuilt.getAnomaly(type2),    relativelyCloseTo(orbit.getAnomaly(type2),    2));
                Assert.assertThat(rebuilt.getAnomalyDot(type2), relativelyCloseTo(orbit.getAnomalyDot(type2), 4));
            }
        }

    }

    @Test
    public void testSerialization()
            throws IOException, ClassNotFoundException {
        Vector3D position = new Vector3D(-29536113.0, 30329259.0, -100125.0);
        Vector3D velocity = new Vector3D(-2194.0, -2141.0, -8.0);
        PVCoordinates pvCoordinates = new PVCoordinates(position, velocity);
        KeplerianOrbit orbit = new KeplerianOrbit(pvCoordinates, FramesFactory.getEME2000(), date, mu);
        Assert.assertEquals(42255170.003, orbit.getA(), 1.0e-3);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream    oos = new ObjectOutputStream(bos);
        oos.writeObject(orbit);

        Assert.assertTrue(bos.size() > 280);
        Assert.assertTrue(bos.size() < 330);

        ByteArrayInputStream  bis = new ByteArrayInputStream(bos.toByteArray());
        ObjectInputStream     ois = new ObjectInputStream(bis);
        KeplerianOrbit deserialized  = (KeplerianOrbit) ois.readObject();
        Assert.assertEquals(orbit.getA(), deserialized.getA(), 1.0e-10);
        Assert.assertEquals(orbit.getE(), deserialized.getE(), 1.0e-10);
        Assert.assertEquals(orbit.getI(), deserialized.getI(), 1.0e-10);
        Assert.assertEquals(orbit.getPerigeeArgument(), deserialized.getPerigeeArgument(), 1.0e-10);
        Assert.assertEquals(orbit.getRightAscensionOfAscendingNode(), deserialized.getRightAscensionOfAscendingNode(), 1.0e-10);
        Assert.assertEquals(orbit.getTrueAnomaly(), deserialized.getTrueAnomaly(), 1.0e-10);
        Assert.assertTrue(Double.isNaN(orbit.getADot()) && Double.isNaN(deserialized.getADot()));
        Assert.assertTrue(Double.isNaN(orbit.getEDot()) && Double.isNaN(deserialized.getEDot()));
        Assert.assertTrue(Double.isNaN(orbit.getIDot()) && Double.isNaN(deserialized.getIDot()));
        Assert.assertTrue(Double.isNaN(orbit.getPerigeeArgumentDot()) && Double.isNaN(deserialized.getPerigeeArgumentDot()));
        Assert.assertTrue(Double.isNaN(orbit.getRightAscensionOfAscendingNodeDot()) && Double.isNaN(deserialized.getRightAscensionOfAscendingNodeDot()));
        Assert.assertTrue(Double.isNaN(orbit.getTrueAnomalyDot()) && Double.isNaN(deserialized.getTrueAnomalyDot()));
        Assert.assertEquals(orbit.getDate(), deserialized.getDate());
        Assert.assertEquals(orbit.getMu(), deserialized.getMu(), 1.0e-10);
        Assert.assertEquals(orbit.getFrame().getName(), deserialized.getFrame().getName());

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
        KeplerianOrbit orbit = new KeplerianOrbit(pvCoordinates, FramesFactory.getEME2000(), date, mu);
        Assert.assertEquals(42255170.003, orbit.getA(), 1.0e-3);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream    oos = new ObjectOutputStream(bos);
        oos.writeObject(orbit);

        Assert.assertTrue(bos.size() > 330);
        Assert.assertTrue(bos.size() < 380);

        ByteArrayInputStream  bis = new ByteArrayInputStream(bos.toByteArray());
        ObjectInputStream     ois = new ObjectInputStream(bis);
        KeplerianOrbit deserialized  = (KeplerianOrbit) ois.readObject();
        Assert.assertEquals(orbit.getA(), deserialized.getA(), 1.0e-10);
        Assert.assertEquals(orbit.getE(), deserialized.getE(), 1.0e-10);
        Assert.assertEquals(orbit.getI(), deserialized.getI(), 1.0e-10);
        Assert.assertEquals(orbit.getPerigeeArgument(), deserialized.getPerigeeArgument(), 1.0e-10);
        Assert.assertEquals(orbit.getRightAscensionOfAscendingNode(), deserialized.getRightAscensionOfAscendingNode(), 1.0e-10);
        Assert.assertEquals(orbit.getTrueAnomaly(), deserialized.getTrueAnomaly(), 1.0e-10);
        Assert.assertEquals(orbit.getADot(), deserialized.getADot(), 1.0e-10);
        Assert.assertEquals(orbit.getEDot(), deserialized.getEDot(), 1.0e-10);
        Assert.assertEquals(orbit.getIDot(), deserialized.getIDot(), 1.0e-10);
        Assert.assertEquals(orbit.getPerigeeArgumentDot(), deserialized.getPerigeeArgumentDot(), 1.0e-10);
        Assert.assertEquals(orbit.getRightAscensionOfAscendingNodeDot(), deserialized.getRightAscensionOfAscendingNodeDot(), 1.0e-10);
        Assert.assertEquals(orbit.getTrueAnomalyDot(), deserialized.getTrueAnomalyDot(), 1.0e-10);
        Assert.assertEquals(orbit.getDate(), deserialized.getDate());
        Assert.assertEquals(orbit.getMu(), deserialized.getMu(), 1.0e-10);
        Assert.assertEquals(orbit.getFrame().getName(), deserialized.getFrame().getName());

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
        KeplerianOrbit orbit = new KeplerianOrbit(pvCoordinates, FramesFactory.getEME2000(), date, mu);
        Assert.assertEquals(10637829.465, orbit.getA(), 1.0e-3);
        Assert.assertEquals(-738.145, orbit.getADot(), 1.0e-3);
        Assert.assertEquals(0.05995861, orbit.getE(), 1.0e-8);
        Assert.assertEquals(-6.523e-5, orbit.getEDot(), 1.0e-8);
        Assert.assertEquals(FastMath.PI, orbit.getI(), 1.0e-15);
        Assert.assertEquals(-4.615e-5, orbit.getIDot(), 1.0e-8);
        Assert.assertTrue(Double.isNaN(orbit.getHx()));
        Assert.assertTrue(Double.isNaN(orbit.getHxDot()));
        Assert.assertTrue(Double.isNaN(orbit.getHy()));
        Assert.assertTrue(Double.isNaN(orbit.getHyDot()));
    }

    @Test
    public void testDerivativesConversionSymmetry() {
        final AbsoluteDate date = new AbsoluteDate("2003-05-01T00:01:20.000", TimeScalesFactory.getUTC());
        Vector3D position     = new Vector3D(6893443.400234382, 1886406.1073757345, -589265.1150359757);
        Vector3D velocity     = new Vector3D(-281.1261461082365, -1231.6165642450928, -7348.756363469432);
        Vector3D acceleration = new Vector3D(-7.460341170581685, -2.0415957334584527, 0.6393322823627762);
        PVCoordinates pvCoordinates = new PVCoordinates( position, velocity, acceleration);
        KeplerianOrbit orbit = new KeplerianOrbit(pvCoordinates, FramesFactory.getEME2000(),
                                                  date, Constants.EIGEN5C_EARTH_MU);
        Assert.assertTrue(orbit.hasDerivatives());
        double r2 = position.getNormSq();
        double r  = FastMath.sqrt(r2);
        Vector3D keplerianAcceleration = new Vector3D(-orbit.getMu() / (r2 * r), position);
        Assert.assertEquals(0.0101, Vector3D.distance(keplerianAcceleration, acceleration), 1.0e-4);

        for (OrbitType type : OrbitType.values()) {
            Orbit converted = type.convertType(orbit);
            Assert.assertTrue(converted.hasDerivatives());
            KeplerianOrbit rebuilt = (KeplerianOrbit) OrbitType.KEPLERIAN.convertType(converted);
            Assert.assertTrue(rebuilt.hasDerivatives());
            Assert.assertEquals(orbit.getADot(),                             rebuilt.getADot(),                             3.0e-13);
            Assert.assertEquals(orbit.getEDot(),                             rebuilt.getEDot(),                             1.0e-15);
            Assert.assertEquals(orbit.getIDot(),                             rebuilt.getIDot(),                             1.0e-15);
            Assert.assertEquals(orbit.getPerigeeArgumentDot(),               rebuilt.getPerigeeArgumentDot(),               1.0e-15);
            Assert.assertEquals(orbit.getRightAscensionOfAscendingNodeDot(), rebuilt.getRightAscensionOfAscendingNodeDot(), 1.0e-15);
            Assert.assertEquals(orbit.getTrueAnomalyDot(),                   rebuilt.getTrueAnomalyDot(),                   1.0e-15);
        }

    }

    @Test
    public void testDerivativesConversionSymmetryHyperbolic() {
        final AbsoluteDate date         = new AbsoluteDate("2003-05-01T00:00:20.000", TimeScalesFactory.getUTC());
        final Vector3D     position     = new Vector3D(224267911.905821, 290251613.109399, 45534292.777492);
        final Vector3D     velocity     = new Vector3D(-1494.068165293, 1124.771027677, 526.915286134);
        final Vector3D     acceleration = new Vector3D(-0.001295920501, -0.002233045187, -0.000349906292);
        PVCoordinates pvCoordinates = new PVCoordinates( position, velocity, acceleration);
        KeplerianOrbit orbit = new KeplerianOrbit(pvCoordinates, FramesFactory.getEME2000(),
                                                  date, Constants.EIGEN5C_EARTH_MU);
        Assert.assertTrue(orbit.hasDerivatives());
        double r2 = position.getNormSq();
        double r  = FastMath.sqrt(r2);
        Vector3D keplerianAcceleration = new Vector3D(-orbit.getMu() / (r2 * r), position);
        Assert.assertEquals(4.78e-4, Vector3D.distance(keplerianAcceleration, acceleration), 1.0e-6);

        OrbitType type = OrbitType.CARTESIAN;
        Orbit converted = type.convertType(orbit);
        Assert.assertTrue(converted.hasDerivatives());
        KeplerianOrbit rebuilt = (KeplerianOrbit) OrbitType.KEPLERIAN.convertType(converted);
        Assert.assertTrue(rebuilt.hasDerivatives());
        Assert.assertEquals(orbit.getADot(),                             rebuilt.getADot(),                             3.0e-13);
        Assert.assertEquals(orbit.getEDot(),                             rebuilt.getEDot(),                             1.0e-15);
        Assert.assertEquals(orbit.getIDot(),                             rebuilt.getIDot(),                             1.0e-15);
        Assert.assertEquals(orbit.getPerigeeArgumentDot(),               rebuilt.getPerigeeArgumentDot(),               1.0e-15);
        Assert.assertEquals(orbit.getRightAscensionOfAscendingNodeDot(), rebuilt.getRightAscensionOfAscendingNodeDot(), 1.0e-15);
        Assert.assertEquals(orbit.getTrueAnomalyDot(),                   rebuilt.getTrueAnomalyDot(),                   1.0e-15);

    }

    @Test
    public void testToString() {
        Vector3D position = new Vector3D(-29536113.0, 30329259.0, -100125.0);
        Vector3D velocity = new Vector3D(-2194.0, -2141.0, -8.0);
        PVCoordinates pvCoordinates = new PVCoordinates(position, velocity);
        KeplerianOrbit orbit = new KeplerianOrbit(pvCoordinates, FramesFactory.getEME2000(), date, mu);
        Assert.assertEquals("Keplerian parameters: {a: 4.225517000282565E7; e: 0.002146216321416967; i: 0.20189257051515358; pa: 13.949966363606599; raan: -87.91788415673473; v: -151.79096272977213;}",
                            orbit.toString());
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
        final Orbit orbit = new KeplerianOrbit(pv, eme2000, date, mu);

        // Build another KeplerianOrbit as a copy of the first one
        final Orbit orbitCopy = new KeplerianOrbit(orbit);

        // Shift the orbit of a time-interval
        final Orbit shiftedOrbit = orbit.shiftedBy(10); // This works good
        final Orbit shiftedOrbitCopy = orbitCopy.shiftedBy(10); // This does not work

        Assert.assertEquals(0.0,
                            Vector3D.distance(shiftedOrbit.getPVCoordinates().getPosition(),
                                              shiftedOrbitCopy.getPVCoordinates().getPosition()),
                            1.0e-10);
        Assert.assertEquals(0.0,
                            Vector3D.distance(shiftedOrbit.getPVCoordinates().getVelocity(),
                                              shiftedOrbitCopy.getPVCoordinates().getVelocity()),
                            1.0e-10);

    }

    @Test
    public void testIssue544() {
        // Initial parameters
        // In order to test the issue, we volontary set the anomaly at Double.NaN.
        double e = 0.7311;
        double anomaly = Double.NaN;
        // Computes the elliptic eccentric anomaly 
        double E = KeplerianOrbit.meanToEllipticEccentric(anomaly, e);
        // Verify that an infinite loop did not occur
        Assert.assertTrue(Double.isNaN(E));  
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
