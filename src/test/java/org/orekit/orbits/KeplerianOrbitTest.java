/* Copyright 2002-2025 CS GROUP
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
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.orekit.Utils;
import org.orekit.errors.OrekitException;
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

import java.util.function.Function;

import static org.orekit.OrekitMatchers.relativelyCloseTo;

class KeplerianOrbitTest {

    // Computation date
    private AbsoluteDate date;

   // Body mu
    private double mu;

    @ParameterizedTest
    @EnumSource(PositionAngleType.class)
    void testWithCachedPositionAngleType(final PositionAngleType positionAngleType) {
        // GIVEN
        final Vector3D position = new Vector3D(-29536113.0, 30329259.0, -100125.0);
        final Vector3D velocity = new Vector3D(-2194.0, -2141.0, -8.0);
        final PVCoordinates pvCoordinates = new PVCoordinates(position, velocity);
        final double muEarth = 3.9860047e14;
        final CartesianOrbit cartesianOrbit = new CartesianOrbit(pvCoordinates, FramesFactory.getEME2000(), date, muEarth);
        final KeplerianOrbit keplerianOrbit = new KeplerianOrbit(cartesianOrbit);
        // WHEN
        final KeplerianOrbit orbit = keplerianOrbit.withCachedPositionAngleType(positionAngleType);
        // THEN
        Assertions.assertEquals(keplerianOrbit.getFrame(), orbit.getFrame());
        Assertions.assertEquals(keplerianOrbit.getDate(), orbit.getDate());
        Assertions.assertEquals(keplerianOrbit.getMu(), orbit.getMu());
        final Vector3D relativePosition = keplerianOrbit.getPosition(orbit.getFrame()).subtract(
                orbit.getPosition());
        Assertions.assertEquals(0., relativePosition.getNorm(), 1e-6);
        Assertions.assertEquals(keplerianOrbit.hasNonKeplerianAcceleration(),
                orbit.hasNonKeplerianAcceleration());
    }

    @ParameterizedTest
    @EnumSource(PositionAngleType.class)
    void testInFrameKeplerian(final PositionAngleType positionAngleType) {
        testTemplateInFrame(Vector3D.ZERO, positionAngleType);
    }

    @Test
    void testInFrameNonKeplerian() {
        testTemplateInFrame(Vector3D.PLUS_K, PositionAngleType.TRUE);
    }

    private void testTemplateInFrame(final Vector3D acceleration, final PositionAngleType positionAngleType) {
        // GIVEN
        final Vector3D position = new Vector3D(-29536113.0, 30329259.0, -100125.0);
        final Vector3D velocity = new Vector3D(-2194.0, -2141.0, -8.0);
        final PVCoordinates pvCoordinates = new PVCoordinates(position, velocity, acceleration);
        final double muEarth = 3.9860047e14;
        final CartesianOrbit cartesianOrbit = new CartesianOrbit(pvCoordinates, FramesFactory.getEME2000(), date, muEarth);
        final KeplerianOrbit keplerianOrbit = new KeplerianOrbit(cartesianOrbit).withCachedPositionAngleType(positionAngleType);
        // WHEN
        final KeplerianOrbit orbitWithOtherFrame = keplerianOrbit.inFrame(FramesFactory.getGCRF());
        // THEN
        Assertions.assertNotEquals(keplerianOrbit.getFrame(), orbitWithOtherFrame.getFrame());
        Assertions.assertEquals(keplerianOrbit.getDate(), orbitWithOtherFrame.getDate());
        Assertions.assertEquals(keplerianOrbit.getMu(), orbitWithOtherFrame.getMu());
        final Vector3D relativePosition = keplerianOrbit.getPosition(orbitWithOtherFrame.getFrame()).subtract(
                orbitWithOtherFrame.getPosition());
        Assertions.assertEquals(0., relativePosition.getNorm(), 1e-6);
        Assertions.assertEquals(keplerianOrbit.hasNonKeplerianAcceleration(),
                orbitWithOtherFrame.hasNonKeplerianAcceleration());
    }

    @Test
    void testKeplerianToKeplerian() {

        // elliptic orbit
        KeplerianOrbit kep =
            new KeplerianOrbit(24464560.0, 0.7311, 0.122138, 3.10686, 1.00681,
                                    0.048363, PositionAngleType.MEAN,
                                    FramesFactory.getEME2000(), date, mu);

        Vector3D pos = kep.getPosition();
        Vector3D vit = kep.getVelocity();

        KeplerianOrbit param = new KeplerianOrbit(new PVCoordinates(pos, vit),
                                                  FramesFactory.getEME2000(), date, mu);
        Assertions.assertEquals(param.getA(), kep.getA(), Utils.epsilonTest * kep.getA());
        Assertions.assertEquals(param.getE(), kep.getE(), Utils.epsilonE * FastMath.abs(kep.getE()));
        Assertions.assertEquals(MathUtils.normalizeAngle(param.getI(), kep.getI()), kep.getI(), Utils.epsilonAngle * FastMath.abs(kep.getI()));
        Assertions.assertEquals(MathUtils.normalizeAngle(param.getPerigeeArgument(), kep.getPerigeeArgument()), kep.getPerigeeArgument(), Utils.epsilonAngle * FastMath.abs(kep.getPerigeeArgument()));
        Assertions.assertEquals(MathUtils.normalizeAngle(param.getRightAscensionOfAscendingNode(), kep.getRightAscensionOfAscendingNode()), kep.getRightAscensionOfAscendingNode(), Utils.epsilonAngle * FastMath.abs(kep.getRightAscensionOfAscendingNode()));
        Assertions.assertEquals(MathUtils.normalizeAngle(param.getMeanAnomaly(), kep.getMeanAnomaly()), kep.getMeanAnomaly(), Utils.epsilonAngle * FastMath.abs(kep.getMeanAnomaly()));

        // circular orbit
        KeplerianOrbit kepCir =
            new KeplerianOrbit(24464560.0, 0.0, 0.122138, 3.10686, 1.00681,
                                    0.048363, PositionAngleType.MEAN,
                                    FramesFactory.getEME2000(), date, mu);

        Vector3D posCir = kepCir.getPosition();
        Vector3D vitCir = kepCir.getVelocity();

        KeplerianOrbit paramCir = new KeplerianOrbit(new PVCoordinates(posCir, vitCir),
                                                     FramesFactory.getEME2000(), date, mu);
        Assertions.assertEquals(paramCir.getA(), kepCir.getA(), Utils.epsilonTest * kepCir.getA());
        Assertions.assertEquals(paramCir.getE(), kepCir.getE(), Utils.epsilonE * FastMath.max(1., FastMath.abs(kepCir.getE())));
        Assertions.assertEquals(MathUtils.normalizeAngle(paramCir.getI(), kepCir.getI()), kepCir.getI(), Utils.epsilonAngle * FastMath.abs(kepCir.getI()));
        Assertions.assertEquals(MathUtils.normalizeAngle(paramCir.getLM(), kepCir.getLM()), kepCir.getLM(), Utils.epsilonAngle * FastMath.abs(kepCir.getLM()));
        Assertions.assertEquals(MathUtils.normalizeAngle(paramCir.getLE(), kepCir.getLE()), kepCir.getLE(), Utils.epsilonAngle * FastMath.abs(kepCir.getLE()));
        Assertions.assertEquals(MathUtils.normalizeAngle(paramCir.getLv(), kepCir.getLv()), kepCir.getLv(), Utils.epsilonAngle * FastMath.abs(kepCir.getLv()));

        // hyperbolic orbit
        KeplerianOrbit kepHyp =
            new KeplerianOrbit(-24464560.0, 1.7311, 0.122138, 3.10686, 1.00681,
                                    0.048363, PositionAngleType.MEAN,
                                    FramesFactory.getEME2000(), date, mu);

        Vector3D posHyp = kepHyp.getPosition();
        Vector3D vitHyp = kepHyp.getVelocity();

        KeplerianOrbit paramHyp = new KeplerianOrbit(new PVCoordinates(posHyp, vitHyp),
                                                  FramesFactory.getEME2000(), date, mu);
        Assertions.assertEquals(paramHyp.getA(), kepHyp.getA(), Utils.epsilonTest * FastMath.abs(kepHyp.getA()));
        Assertions.assertEquals(paramHyp.getE(), kepHyp.getE(), Utils.epsilonE * FastMath.abs(kepHyp.getE()));
        Assertions.assertEquals(MathUtils.normalizeAngle(paramHyp.getI(), kepHyp.getI()), kepHyp.getI(), Utils.epsilonAngle * FastMath.abs(kepHyp.getI()));
        Assertions.assertEquals(MathUtils.normalizeAngle(paramHyp.getPerigeeArgument(), kepHyp.getPerigeeArgument()), kepHyp.getPerigeeArgument(), Utils.epsilonAngle * FastMath.abs(kepHyp.getPerigeeArgument()));
        Assertions.assertEquals(MathUtils.normalizeAngle(paramHyp.getRightAscensionOfAscendingNode(), kepHyp.getRightAscensionOfAscendingNode()), kepHyp.getRightAscensionOfAscendingNode(), Utils.epsilonAngle * FastMath.abs(kepHyp.getRightAscensionOfAscendingNode()));
        Assertions.assertEquals(MathUtils.normalizeAngle(paramHyp.getMeanAnomaly(), kepHyp.getMeanAnomaly()), kepHyp.getMeanAnomaly(), Utils.epsilonAngle * FastMath.abs(kepHyp.getMeanAnomaly()));

    }

    @Test 
    void testKeplerianToCartesian() {

        KeplerianOrbit kep =
            new KeplerianOrbit(24464560.0, 0.7311, 0.122138, 3.10686, 1.00681,
                                    0.048363, PositionAngleType.MEAN,
                                    FramesFactory.getEME2000(), date, mu);

        Vector3D pos = kep.getPosition();
        Vector3D vit = kep.getVelocity();
        Assertions.assertEquals(-0.107622532467967e+07, pos.getX(), Utils.epsilonTest * FastMath.abs(pos.getX()));
        Assertions.assertEquals(-0.676589636432773e+07, pos.getY(), Utils.epsilonTest * FastMath.abs(pos.getY()));
        Assertions.assertEquals(-0.332308783350379e+06, pos.getZ(), Utils.epsilonTest * FastMath.abs(pos.getZ()));

        Assertions.assertEquals( 0.935685775154103e+04, vit.getX(), Utils.epsilonTest * FastMath.abs(vit.getX()));
        Assertions.assertEquals(-0.331234775037644e+04, vit.getY(), Utils.epsilonTest * FastMath.abs(vit.getY()));
        Assertions.assertEquals(-0.118801577532701e+04, vit.getZ(), Utils.epsilonTest * FastMath.abs(vit.getZ()));
    }

    @Test
    void testKeplerianToEquinoctial() {

        KeplerianOrbit kep =
            new KeplerianOrbit(24464560.0, 0.7311, 0.122138, 3.10686, 1.00681,
                                    0.048363, PositionAngleType.MEAN,
                                    FramesFactory.getEME2000(), date, mu);

        Assertions.assertEquals(24464560.0, kep.getA(), Utils.epsilonTest * kep.getA());
        Assertions.assertEquals(-0.412036802887626, kep.getEquinoctialEx(), Utils.epsilonE * FastMath.abs(kep.getE()));
        Assertions.assertEquals(-0.603931190671706, kep.getEquinoctialEy(), Utils.epsilonE * FastMath.abs(kep.getE()));
        Assertions.assertEquals(MathUtils.normalizeAngle(2*FastMath.asin(FastMath.sqrt((FastMath.pow(0.652494417368829e-01, 2)+FastMath.pow(0.103158450084864, 2))/4.)), kep.getI()), kep.getI(), Utils.epsilonAngle * FastMath.abs(kep.getI()));
        Assertions.assertEquals(MathUtils.normalizeAngle(0.416203300000000e+01, kep.getLM()), kep.getLM(), Utils.epsilonAngle * FastMath.abs(kep.getLM()));

    }

    @Test
    void testAnomaly() {

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
                                    p.getRightAscensionOfAscendingNode(), v , PositionAngleType.TRUE,
                                    p.getFrame(), p.getDate(), p.getMu());
        Assertions.assertEquals(p.getTrueAnomaly(), v, Utils.epsilonAngle * FastMath.abs(v));
        Assertions.assertEquals(p.getEccentricAnomaly(), E, Utils.epsilonAngle * FastMath.abs(E));
        Assertions.assertEquals(p.getMeanAnomaly(), M, Utils.epsilonAngle * FastMath.abs(M));
        p = new KeplerianOrbit(p.getA(), p.getE(), p.getI(), p.getPerigeeArgument(),
                                    p.getRightAscensionOfAscendingNode(), 0 , PositionAngleType.TRUE,
                                    p.getFrame(), p.getDate(), p.getMu());

        p = new KeplerianOrbit(p.getA(), p.getE(), p.getI(), p.getPerigeeArgument(),
                                    p.getRightAscensionOfAscendingNode(), E , PositionAngleType.ECCENTRIC,
                                    p.getFrame(), p.getDate(), p.getMu());
        Assertions.assertEquals(p.getTrueAnomaly(), v, Utils.epsilonAngle * FastMath.abs(v));
        Assertions.assertEquals(p.getEccentricAnomaly(), E, Utils.epsilonAngle * FastMath.abs(E));
        Assertions.assertEquals(p.getMeanAnomaly(), M, Utils.epsilonAngle * FastMath.abs(M));
        p = new KeplerianOrbit(p.getA(), p.getE(), p.getI(), p.getPerigeeArgument(),
                                    p.getRightAscensionOfAscendingNode(), 0 , PositionAngleType.TRUE,
                                    p.getFrame(), p.getDate(), p.getMu());

        p = new KeplerianOrbit(p.getA(), p.getE(), p.getI(), p.getPerigeeArgument(),
                                    p.getRightAscensionOfAscendingNode(), M, PositionAngleType.MEAN,
                                    p.getFrame(), p.getDate(), p.getMu());
        Assertions.assertEquals(p.getTrueAnomaly(), v, Utils.epsilonAngle * FastMath.abs(v));
        Assertions.assertEquals(p.getEccentricAnomaly(), E, Utils.epsilonAngle * FastMath.abs(E));
        Assertions.assertEquals(p.getMeanAnomaly(), M, Utils.epsilonAngle * FastMath.abs(M));

        // circular orbit
        p = new KeplerianOrbit(p.getA(), 0, p.getI(), p.getPerigeeArgument(),
                                    p.getRightAscensionOfAscendingNode(), p.getLv() , PositionAngleType.TRUE,
                                    p.getFrame(), p.getDate(), p.getMu());

        E = v;
        M = E;

        p = new KeplerianOrbit(p.getA(), p.getE(), p.getI(), p.getPerigeeArgument(),
                                    p.getRightAscensionOfAscendingNode(), v , PositionAngleType.TRUE,
                                    p.getFrame(), p.getDate(), p.getMu());
        Assertions.assertEquals(p.getTrueAnomaly(), v, Utils.epsilonAngle * FastMath.abs(v));
        Assertions.assertEquals(p.getEccentricAnomaly(), E, Utils.epsilonAngle * FastMath.abs(E));
        Assertions.assertEquals(p.getMeanAnomaly(), M, Utils.epsilonAngle * FastMath.abs(M));
        p = new KeplerianOrbit(p.getA(), p.getE(), p.getI(), p.getPerigeeArgument(),
                                    p.getRightAscensionOfAscendingNode(), 0 , PositionAngleType.TRUE,
                                    p.getFrame(), p.getDate(), p.getMu());

        p = new KeplerianOrbit(p.getA(), p.getE(), p.getI(), p.getPerigeeArgument(),
                                    p.getRightAscensionOfAscendingNode(), E , PositionAngleType.ECCENTRIC, p.getFrame(), p.getDate(), p.getMu());
        Assertions.assertEquals(p.getTrueAnomaly(), v, Utils.epsilonAngle * FastMath.abs(v));
        Assertions.assertEquals(p.getEccentricAnomaly(), E, Utils.epsilonAngle * FastMath.abs(E));
        Assertions.assertEquals(p.getMeanAnomaly(), M, Utils.epsilonAngle * FastMath.abs(M));
        p = new KeplerianOrbit(p.getA(), p.getE(), p.getI(), p.getPerigeeArgument(),
                                    p.getRightAscensionOfAscendingNode(), 0 , PositionAngleType.TRUE,
                                    p.getFrame(), p.getDate(), p.getMu());

        p = new KeplerianOrbit(p.getA(), p.getE(), p.getI(), p.getPerigeeArgument(),
                                    p.getRightAscensionOfAscendingNode(), M, PositionAngleType.MEAN,
                                    p.getFrame(), p.getDate(), p.getMu());
        Assertions.assertEquals(p.getTrueAnomaly(), v, Utils.epsilonAngle * FastMath.abs(v));
        Assertions.assertEquals(p.getEccentricAnomaly(), E, Utils.epsilonAngle * FastMath.abs(E));
        Assertions.assertEquals(p.getMeanAnomaly(), M, Utils.epsilonAngle * FastMath.abs(M));

    }

    @Test
    void testPositionVelocityNorms() {
        double mu = 3.9860047e14;

        // elliptic and non equatorial orbit
        KeplerianOrbit p =
            new KeplerianOrbit(24464560.0, 0.7311, 2.1, 3.10686, 1.00681,
                                    0.67, PositionAngleType.TRUE,
                                    FramesFactory.getEME2000(), date, mu);

        double e       = p.getE();
        double v       = p.getTrueAnomaly();
        double ksi     = 1 + e * FastMath.cos(v);
        double nu      = e * FastMath.sin(v);
        double epsilon = FastMath.sqrt((1 - e) * (1 + e));

        double a  = p.getA();
        double na = FastMath.sqrt(mu / a);

        // validation of: r = a .(1 - e2) / (1 + e.cos(v))
        Assertions.assertEquals(a * epsilon * epsilon / ksi,
                     p.getPosition().getNorm(),
                     Utils.epsilonTest * FastMath.abs(p.getPosition().getNorm()));

        // validation of: V = sqrt(mu.(1+2e.cos(v)+e2)/a.(1-e2) )
        Assertions.assertEquals(na * FastMath.sqrt(ksi * ksi + nu * nu) / epsilon,
                     p.getVelocity().getNorm(),
                     Utils.epsilonTest * FastMath.abs(p.getVelocity().getNorm()));


        //  circular and equatorial orbit
        KeplerianOrbit pCirEqua =
            new KeplerianOrbit(24464560.0, 0.1e-10, 0.1e-8, 3.10686, 1.00681,
                                    0.67, PositionAngleType.TRUE,
                                    FramesFactory.getEME2000(), date, mu);

        e       = pCirEqua.getE();
        v       = pCirEqua.getTrueAnomaly();
        ksi     = 1 + e * FastMath.cos(v);
        nu      = e * FastMath.sin(v);
        epsilon = FastMath.sqrt((1 - e) * (1 + e));

        a  = pCirEqua.getA();
        na = FastMath.sqrt(mu / a);

        // validation of: r = a .(1 - e2) / (1 + e.cos(v))
        Assertions.assertEquals(a * epsilon * epsilon / ksi,
                     pCirEqua.getPosition().getNorm(),
                     Utils.epsilonTest * FastMath.abs(pCirEqua.getPosition().getNorm()));

        // validation of: V = sqrt(mu.(1+2e.cos(v)+e2)/a.(1-e2) )
        Assertions.assertEquals(na * FastMath.sqrt(ksi * ksi + nu * nu) / epsilon,
                     pCirEqua.getVelocity().getNorm(),
                     Utils.epsilonTest * FastMath.abs(pCirEqua.getVelocity().getNorm()));
    }

    @Test
    void testGeometry() {
        double mu = 3.9860047e14;

        // elliptic and non equatorial orbit
        KeplerianOrbit p =
            new KeplerianOrbit(24464560.0, 0.7311, 2.1, 3.10686, 1.00681,
                                    0.67, PositionAngleType.TRUE,
                                    FramesFactory.getEME2000(), date, mu);

        Vector3D position = p.getPosition();
        Vector3D velocity = p.getVelocity();
        Vector3D momentum = p.getPVCoordinates().getMomentum().normalize();

        double apogeeRadius  = p.getA() * (1 + p.getE());
        double perigeeRadius = p.getA() * (1 - p.getE());

        for (double lv = 0; lv <= 2 * FastMath.PI; lv += 2 * FastMath.PI/100.) {
            p = new KeplerianOrbit(p.getA(), p.getE(), p.getI(), p.getPerigeeArgument(),
                                   p.getRightAscensionOfAscendingNode(), lv , PositionAngleType.TRUE,
                                   p.getFrame(), p.getDate(), p.getMu());
            position = p.getPosition();

            // test if the norm of the position is in the range [perigee radius, apogee radius]
            Assertions.assertTrue((position.getNorm() - apogeeRadius)  <= (  apogeeRadius * Utils.epsilonTest));
            Assertions.assertTrue((position.getNorm() - perigeeRadius) >= (- perigeeRadius * Utils.epsilonTest));

            position = position.normalize();
            velocity = p.getVelocity();
            velocity = velocity.normalize();

            // at this stage of computation, all the vectors (position, velocity and momemtum) are normalized here

            // test of orthogonality between position and momentum
            Assertions.assertTrue(FastMath.abs(Vector3D.dotProduct(position, momentum)) < Utils.epsilonTest);
            // test of orthogonality between velocity and momentum
            Assertions.assertTrue(FastMath.abs(Vector3D.dotProduct(velocity, momentum)) < Utils.epsilonTest);

        }

        // apsides
        p = new KeplerianOrbit(p.getA(), p.getE(), p.getI(), p.getPerigeeArgument(),
                               p.getRightAscensionOfAscendingNode(), 0 , PositionAngleType.TRUE, p.getFrame(), p.getDate(), p.getMu());
        Assertions.assertEquals(p.getPosition().getNorm(), perigeeRadius, perigeeRadius * Utils.epsilonTest);

        p = new KeplerianOrbit(p.getA(), p.getE(), p.getI(), p.getPerigeeArgument(),
                               p.getRightAscensionOfAscendingNode(), FastMath.PI , PositionAngleType.TRUE, p.getFrame(), p.getDate(), p.getMu());
        Assertions.assertEquals(p.getPosition().getNorm(), apogeeRadius, apogeeRadius * Utils.epsilonTest);

        // nodes
        // descending node
        p = new KeplerianOrbit(p.getA(), p.getE(), p.getI(), p.getPerigeeArgument(),
                               p.getRightAscensionOfAscendingNode(), FastMath.PI - p.getPerigeeArgument() , PositionAngleType.TRUE,
                               p.getFrame(), p.getDate(), p.getMu());
        Assertions.assertTrue(FastMath.abs(p.getPosition().getZ()) < p.getPosition().getNorm() * Utils.epsilonTest);
        Assertions.assertTrue(p.getVelocity().getZ() < 0);

        // ascending node
        p = new KeplerianOrbit(p.getA(), p.getE(), p.getI(), p.getPerigeeArgument(),
                               p.getRightAscensionOfAscendingNode(), 2.0 * FastMath.PI - p.getPerigeeArgument() , PositionAngleType.TRUE,
                               p.getFrame(), p.getDate(), p.getMu());
        Assertions.assertTrue(FastMath.abs(p.getPosition().getZ()) < p.getPosition().getNorm() * Utils.epsilonTest);
        Assertions.assertTrue(p.getVelocity().getZ() > 0);


        //  circular and equatorial orbit
        KeplerianOrbit pCirEqua =
            new KeplerianOrbit(24464560.0, 0.1e-10, 0.1e-8, 3.10686, 1.00681,
                               0.67, PositionAngleType.TRUE, FramesFactory.getEME2000(), date, mu);

        position = pCirEqua.getPosition();
        velocity = pCirEqua.getVelocity();
        momentum = Vector3D.crossProduct(position, velocity).normalize();

        apogeeRadius  = pCirEqua.getA() * (1 + pCirEqua.getE());
        perigeeRadius = pCirEqua.getA() * (1 - pCirEqua.getE());
        // test if apogee equals perigee
        Assertions.assertEquals(perigeeRadius, apogeeRadius, 1.e+4 * Utils.epsilonTest * apogeeRadius);

        for (double lv = 0; lv <= 2 * FastMath.PI; lv += 2 * FastMath.PI/100.) {
            pCirEqua = new KeplerianOrbit(pCirEqua.getA(), pCirEqua.getE(), pCirEqua.getI(), pCirEqua.getPerigeeArgument(),
                                          pCirEqua.getRightAscensionOfAscendingNode(), lv, PositionAngleType.TRUE,
                                          pCirEqua.getFrame(), pCirEqua.getDate(), pCirEqua.getMu());
            position = pCirEqua.getPosition();

            // test if the norm pf the position is in the range [perigee radius, apogee radius]
            // Warning: these tests are without absolute value by choice
            Assertions.assertTrue((position.getNorm() - apogeeRadius)  <= (  apogeeRadius * Utils.epsilonTest));
            Assertions.assertTrue((position.getNorm() - perigeeRadius) >= (- perigeeRadius * Utils.epsilonTest));

            position = position.normalize();
            velocity = pCirEqua.getVelocity();
            velocity = velocity.normalize();

            // at this stage of computation, all the vectors (position, velocity and momemtum) are normalized here

            // test of orthogonality between position and momentum
            Assertions.assertTrue(FastMath.abs(Vector3D.dotProduct(position, momentum)) < Utils.epsilonTest);
            // test of orthogonality between velocity and momentum
            Assertions.assertTrue(FastMath.abs(Vector3D.dotProduct(velocity, momentum)) < Utils.epsilonTest);

        }
    }

    @Test
    void testSymmetry() {

        // elliptic and non equatorial orbit
        Vector3D position = new Vector3D(-4947831., -3765382., -3708221.);
        Vector3D velocity = new Vector3D(-2079., 5291., -7842.);
        double mu = 3.9860047e14;

        KeplerianOrbit p = new KeplerianOrbit(new PVCoordinates(position, velocity),
                                              FramesFactory.getEME2000(), date, mu);
        Vector3D positionOffset = p.getPosition().subtract(position);
        Vector3D velocityOffset = p.getVelocity().subtract(velocity);

        Assertions.assertEquals(0.0, positionOffset.getNorm(), 2.9e-9);
        Assertions.assertEquals(0.0, velocityOffset.getNorm(), 1.0e-15);

        // circular and equatorial orbit
        position = new Vector3D(1742382., -2.440243e7, -0.014517);
        velocity = new Vector3D(4026.2, 287.479, -3.e-6);


        p = new KeplerianOrbit(new PVCoordinates(position, velocity),
                               FramesFactory.getEME2000(), date, mu);
        positionOffset = p.getPosition().subtract(position);
        velocityOffset = p.getVelocity().subtract(velocity);

        Assertions.assertEquals(0.0, positionOffset.getNorm(), 4.8e-9);
        Assertions.assertEquals(0.0, velocityOffset.getNorm(), 1.0e-100);

    }

    @Test
    void testNonInertialFrame() throws IllegalArgumentException {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Vector3D position = new Vector3D(-4947831., -3765382., -3708221.);
            Vector3D velocity = new Vector3D(-2079., 5291., -7842.);
            PVCoordinates pvCoordinates = new PVCoordinates( position, velocity);
            new KeplerianOrbit(pvCoordinates,
                    new Frame(FramesFactory.getEME2000(), Transform.IDENTITY, "non-inertial", false),
                    date, mu);
        });
    }

    @Test
    void testPeriod() {
        KeplerianOrbit orbit = new KeplerianOrbit(7654321.0, 0.1, 0.2, 0, 0, 0,
                                                  PositionAngleType.TRUE,
                                                  FramesFactory.getEME2000(), AbsoluteDate.J2000_EPOCH,
                                                  mu);
        Assertions.assertEquals(6664.5521723383589487, orbit.getKeplerianPeriod(), 1.0e-12);
        Assertions.assertEquals(0.00094277682051291315229, orbit.getKeplerianMeanMotion(), 1.0e-16);
    }

    @Test
    void testHyperbola1() {
        KeplerianOrbit orbit = new KeplerianOrbit(-10000000.0, 2.5, 0.3, 0, 0, 0.0,
                                                  PositionAngleType.TRUE,
                                                  FramesFactory.getEME2000(), AbsoluteDate.J2000_EPOCH,
                                                  mu);
        Vector3D perigeeP  = orbit.getPosition();
        Vector3D u = perigeeP.normalize();
        Vector3D focus1 = Vector3D.ZERO;
        Vector3D focus2 = new Vector3D(-2 * orbit.getA() * orbit.getE(), u);
        for (double dt = -5000; dt < 5000; dt += 60) {
            PVCoordinates pv = orbit.shiftedBy(dt).getPVCoordinates();
            double d1 = Vector3D.distance(pv.getPosition(), focus1);
            double d2 = Vector3D.distance(pv.getPosition(), focus2);
            Assertions.assertEquals(-2 * orbit.getA(), FastMath.abs(d1 - d2), 1.0e-6);
            KeplerianOrbit rebuilt =
                new KeplerianOrbit(pv, orbit.getFrame(), orbit.getDate().shiftedBy(dt), mu);
            Assertions.assertEquals(-10000000.0, rebuilt.getA(), 1.0e-6);
            Assertions.assertEquals(2.5, rebuilt.getE(), 1.0e-13);
        }
    }

    @Test
    void testHyperbola2() {
        KeplerianOrbit orbit = new KeplerianOrbit(-10000000.0, 1.2, 0.3, 0, 0, -1.75,
                                                  PositionAngleType.MEAN,
                                                  FramesFactory.getEME2000(), AbsoluteDate.J2000_EPOCH,
                                                  mu);
        Vector3D perigeeP  = new KeplerianOrbit(orbit.getA(), orbit.getE(), orbit.getI(),
                                                orbit.getPerigeeArgument(), orbit.getRightAscensionOfAscendingNode(),
                                                0.0, PositionAngleType.TRUE, orbit.getFrame(),
                                                orbit.getDate(), orbit.getMu()).getPosition();
        Vector3D u = perigeeP.normalize();
        Vector3D focus1 = Vector3D.ZERO;
        Vector3D focus2 = new Vector3D(-2 * orbit.getA() * orbit.getE(), u);
        for (double dt = -5000; dt < 5000; dt += 60) {
            PVCoordinates pv = orbit.shiftedBy(dt).getPVCoordinates();
            double d1 = Vector3D.distance(pv.getPosition(), focus1);
            double d2 = Vector3D.distance(pv.getPosition(), focus2);
            Assertions.assertEquals(-2 * orbit.getA(), FastMath.abs(d1 - d2), 1.0e-6);
            KeplerianOrbit rebuilt =
                new KeplerianOrbit(pv, orbit.getFrame(), orbit.getDate().shiftedBy(dt), mu);
            Assertions.assertEquals(-10000000.0, rebuilt.getA(), 1.0e-6);
            Assertions.assertEquals(1.2, rebuilt.getE(), 1.0e-13);
        }
    }

    @Test
    void testInconsistentHyperbola() {
        try {
            new KeplerianOrbit(+10000000.0, 2.5, 0.3, 0, 0, 0.0,
                                                  PositionAngleType.TRUE,
                                                  FramesFactory.getEME2000(), AbsoluteDate.J2000_EPOCH,
                                                  mu);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitIllegalArgumentException oe) {
            Assertions.assertEquals(OrekitMessages.ORBIT_A_E_MISMATCH_WITH_CONIC_TYPE, oe.getSpecifier());
            Assertions.assertEquals(+10000000.0, ((Double) oe.getParts()[0]).doubleValue(), 1.0e-3);
            Assertions.assertEquals(2.5,         ((Double) oe.getParts()[1]).doubleValue(), 1.0e-15);
        }
    }

    @Test
    void testVeryLargeEccentricity() {

        final Frame eme2000 = FramesFactory.getEME2000();
        final double meanAnomaly = 1.;
        final KeplerianOrbit orb0 = new KeplerianOrbit(42600e3, 0.9, 0.00001, 0, 0,
                                                       FastMath.toRadians(meanAnomaly),
                                                       PositionAngleType.MEAN, eme2000, date, mu);

        // big dV along Y
        final Vector3D deltaV = new Vector3D(0.0, 110000.0, 0.0);
        final PVCoordinates pv1 = new PVCoordinates(orb0.getPosition(),
                                                    orb0.getVelocity().add(deltaV));
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
        Assertions.assertEquals(0, pTested.subtract(pReference).getNorm(), threshold * pReference.getNorm());
        Assertions.assertEquals(0, vTested.subtract(vReference).getNorm(), threshold * vReference.getNorm());

    }

    @Test
    void testKeplerEquation() {

        for (double M = -6 * FastMath.PI; M < 6 * FastMath.PI; M += 0.01) {
            KeplerianOrbit pElliptic =
                new KeplerianOrbit(24464560.0, 0.7311, 2.1, 3.10686, 1.00681,
                                   M, PositionAngleType.MEAN,
                                   FramesFactory.getEME2000(), date, mu);
            double E = pElliptic.getEccentricAnomaly();
            double e = pElliptic.getE();
            Assertions.assertEquals(M, E - e * FastMath.sin(E), 2.0e-14);
        }

        for (double M = -6 * FastMath.PI; M < 6 * FastMath.PI; M += 0.01) {
            KeplerianOrbit pAlmostParabolic =
                new KeplerianOrbit(24464560.0, 0.9999, 2.1, 3.10686, 1.00681,
                                   M, PositionAngleType.MEAN,
                                   FramesFactory.getEME2000(), date, mu);
            double E = pAlmostParabolic.getEccentricAnomaly();
            double e = pAlmostParabolic.getE();
            Assertions.assertEquals(M, E - e * FastMath.sin(E), 4.0e-13);
        }

    }

    @Test
    void testOutOfRangeV() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            new KeplerianOrbit(-7000434.460140012, 1.1999785407363386, 1.3962787004479158,
                    1.3962320168955138, 0.3490728321331678, -2.55593407037698,
                    PositionAngleType.TRUE, FramesFactory.getEME2000(),
                    new AbsoluteDate("2000-01-01T12:00:00.391", TimeScalesFactory.getUTC()),
                    3.986004415E14);
        });
    }

    @Test
    void testNumericalIssue25() {
        Vector3D position = new Vector3D(3782116.14107698, 416663.11924914, 5875541.62103057);
        Vector3D velocity = new Vector3D(-6349.7848910501, 288.4061811651, 4066.9366759691);
        KeplerianOrbit orbit = new KeplerianOrbit(new PVCoordinates(position, velocity),
                                                  FramesFactory.getEME2000(),
                                                  new AbsoluteDate("2004-01-01T23:00:00.000",
                                                                   TimeScalesFactory.getUTC()),
                                                  3.986004415E14);
        Assertions.assertEquals(0.0, orbit.getE(), 2.0e-14);
    }

    @Test
    void testPerfectlyEquatorial() {
        Vector3D position = new Vector3D(6957904.3624652653594, 766529.11411558074507, 0);
        Vector3D velocity = new Vector3D(-7538.2817012412102845, 342.38751001881413381, 0.);
        KeplerianOrbit orbit = new KeplerianOrbit(new PVCoordinates(position, velocity),
                                                  FramesFactory.getEME2000(),
                                                  new AbsoluteDate("2004-01-01T23:00:00.000",
                                                                   TimeScalesFactory.getUTC()),
                                                  3.986004415E14);
        Assertions.assertEquals(0.0, orbit.getI(), 2.0e-14);
        Assertions.assertEquals(0.0, orbit.getRightAscensionOfAscendingNode(), 2.0e-14);
    }

    @Test
    void testJacobianReferenceEllipse() {

        AbsoluteDate dateTca = new AbsoluteDate(2000, 04, 01, 0, 0, 0.000, TimeScalesFactory.getUTC());
        double mu =  3.986004415e+14;
        KeplerianOrbit orbKep = new KeplerianOrbit(7000000.0, 0.01, FastMath.toRadians(80.), FastMath.toRadians(80.), FastMath.toRadians(20.),
                                          FastMath.toRadians(40.), PositionAngleType.MEAN,
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
        Assertions.assertEquals(0, pv.getPosition().subtract(pRef).getNorm(), 1.0e-15 * pRef.getNorm());
        Assertions.assertEquals(0, pv.getVelocity().subtract(vRef).getNorm(), 1.0e-16 * vRef.getNorm());

        double[][] jacobian = new double[6][6];
        orbKep.getJacobianWrtCartesian(PositionAngleType.MEAN, jacobian);

        for (int i = 0; i < jacobian.length; i++) {
            double[] row    = jacobian[i];
            double[] rowRef = jRef[i];
            for (int j = 0; j < row.length; j++) {
                Assertions.assertEquals(0, (row[j] - rowRef[j]) / rowRef[j], 2.0e-12);
            }
        }

    }

    @Test
    void testJacobianFinitedifferencesEllipse() {

        AbsoluteDate dateTca = new AbsoluteDate(2000, 04, 01, 0, 0, 0.000, TimeScalesFactory.getUTC());
        double mu =  3.986004415e+14;
        KeplerianOrbit orbKep = new KeplerianOrbit(7000000.0, 0.01, FastMath.toRadians(80.), FastMath.toRadians(80.), FastMath.toRadians(20.),
                                          FastMath.toRadians(40.), PositionAngleType.MEAN,
                                          FramesFactory.getEME2000(), dateTca, mu);

        for (PositionAngleType type : PositionAngleType.values()) {
            double hP = 2.0;
            double[][] finiteDiffJacobian = finiteDifferencesJacobian(type, orbKep, hP);
            double[][] jacobian = new double[6][6];
            orbKep.getJacobianWrtCartesian(type, jacobian);

            for (int i = 0; i < jacobian.length; i++) {
                double[] row    = jacobian[i];
                double[] rowRef = finiteDiffJacobian[i];
                for (int j = 0; j < row.length; j++) {
                    Assertions.assertEquals(0, (row[j] - rowRef[j]) / rowRef[j], 2.0e-7);
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
                    Assertions.assertEquals(row == column ? 1.0 : 0.0, value, 5.0e-9);
                }

                public double end() {
                    return Double.NaN;
                }
            });

        }

    }

    @Test
    void testJacobianReferenceHyperbola() {

        AbsoluteDate dateTca = new AbsoluteDate(2000, 04, 01, 0, 0, 0.000, TimeScalesFactory.getUTC());
        double mu =  3.986004415e+14;
        KeplerianOrbit orbKep = new KeplerianOrbit(-7000000.0, 1.2, FastMath.toRadians(80.), FastMath.toRadians(80.), FastMath.toRadians(20.),
                                          FastMath.toRadians(40.), PositionAngleType.MEAN,
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
        Assertions.assertEquals(0, pv.getPosition().subtract(pRef).getNorm() / pRef.getNorm(), 1.0e-16);
//        Assertions.assertEquals(0, pv.getPosition().subtract(pRef).getNorm() / pRef.getNorm(), 2.0e-15);
        Assertions.assertEquals(0, pv.getVelocity().subtract(vRef).getNorm() / vRef.getNorm(), 3.0e-16);

        double[][] jacobian = new double[6][6];
        orbKep.getJacobianWrtCartesian(PositionAngleType.MEAN, jacobian);

        for (int i = 0; i < jacobian.length; i++) {
            double[] row    = jacobian[i];
            double[] rowRef = jRef[i];
            for (int j = 0; j < row.length; j++) {
                Assertions.assertEquals(0, (row[j] - rowRef[j]) / rowRef[j], 1.0e-14);
            }
        }

    }

    @Test
    void testJacobianFinitedifferencesHyperbola() {

        AbsoluteDate dateTca = new AbsoluteDate(2000, 04, 01, 0, 0, 0.000, TimeScalesFactory.getUTC());
        double mu =  3.986004415e+14;
        KeplerianOrbit orbKep = new KeplerianOrbit(-7000000.0, 1.2, FastMath.toRadians(80.), FastMath.toRadians(80.), FastMath.toRadians(20.),
                                                   FastMath.toRadians(40.), PositionAngleType.MEAN,
                                                   FramesFactory.getEME2000(), dateTca, mu);

        for (PositionAngleType type : PositionAngleType.values()) {
            double hP = 2.0;
            double[][] finiteDiffJacobian = finiteDifferencesJacobian(type, orbKep, hP);
            double[][] jacobian = new double[6][6];
            orbKep.getJacobianWrtCartesian(type, jacobian);
            for (int i = 0; i < jacobian.length; i++) {
                double[] row    = jacobian[i];
                double[] rowRef = finiteDiffJacobian[i];
                for (int j = 0; j < row.length; j++) {
                    Assertions.assertEquals(0, (row[j] - rowRef[j]) / rowRef[j], 3.0e-8);
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
                    Assertions.assertEquals(row == column ? 1.0 : 0.0, value, 2.0e-8);
                }

                public double end() {
                    return Double.NaN;
                }
            });

        }

    }

    private double[][] finiteDifferencesJacobian(PositionAngleType type, KeplerianOrbit orbit, double hP)
        {
        double[][] jacobian = new double[6][6];
        for (int i = 0; i < 6; ++i) {
            fillColumn(type, i, orbit, hP, jacobian);
        }
        return jacobian;
    }

    private void fillColumn(PositionAngleType type, int i, KeplerianOrbit orbit, double hP, double[][] jacobian) {

        // at constant energy (i.e. constant semi major axis), we have dV = -mu dP / (V * r^2)
        // we use this to compute a velocity step size from the position step size
        Vector3D p = orbit.getPosition();
        Vector3D v = orbit.getVelocity();
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
    void testPerfectlyEquatorialConversion() {
        KeplerianOrbit initial = new KeplerianOrbit(13378000.0, 0.05, 0.0, 0.0, FastMath.PI,
                                                    0.0, PositionAngleType.MEAN,
                                                    FramesFactory.getEME2000(), date,
                                                    Constants.EIGEN5C_EARTH_MU);
        EquinoctialOrbit equ = (EquinoctialOrbit) OrbitType.EQUINOCTIAL.convertType(initial);
        KeplerianOrbit converted = (KeplerianOrbit) OrbitType.KEPLERIAN.convertType(equ);
        Assertions.assertEquals(FastMath.PI,
                            MathUtils.normalizeAngle(converted.getRightAscensionOfAscendingNode() +
                                                     converted.getPerigeeArgument(), FastMath.PI),
                            1.0e-10);
    }

    @Test
    void testKeplerianDerivatives() {

        final KeplerianOrbit orbit = new KeplerianOrbit(new PVCoordinates(new Vector3D(-4947831., -3765382., -3708221.),
                                                                          new Vector3D(-2079., 5291., -7842.)),
                                                        FramesFactory.getEME2000(), date, 3.9860047e14);
        final Vector3D p = orbit.getPosition();
        final Vector3D v = orbit.getVelocity();
        final Vector3D a = orbit.getPVCoordinates().getAcceleration();

        // check that despite we did not provide acceleration, it got recomputed
        Assertions.assertEquals(7.605422, a.getNorm(), 1.0e-6);

        // check velocity is the derivative of position
        final double tolerance = 3e-10;
        Assertions.assertEquals(differentiate(orbit, shifted -> shifted.getPosition().getX()),
                            orbit.getVelocity().getX(),
                            tolerance * v.getNorm());
        Assertions.assertEquals(differentiate(orbit, shifted -> shifted.getPosition().getY()),
                            orbit.getVelocity().getY(),
                            tolerance * v.getNorm());
        Assertions.assertEquals(differentiate(orbit, shifted -> shifted.getPosition().getZ()),
                            orbit.getVelocity().getZ(),
                            tolerance * v.getNorm());

        // check acceleration is the derivative of velocity
        Assertions.assertEquals(differentiate(orbit, shifted -> shifted.getVelocity().getX()),
                            orbit.getPVCoordinates().getAcceleration().getX(),
                            tolerance * a.getNorm());
        Assertions.assertEquals(differentiate(orbit, shifted -> shifted.getVelocity().getY()),
                            orbit.getPVCoordinates().getAcceleration().getY(),
                            tolerance * a.getNorm());
        Assertions.assertEquals(differentiate(orbit, shifted -> shifted.getVelocity().getZ()),
                            orbit.getPVCoordinates().getAcceleration().getZ(),
                            tolerance * a.getNorm());

        // check jerk is the derivative of acceleration
        final double r2 = p.getNormSq();
        final double r  = FastMath.sqrt(r2);
        Vector3D keplerianJerk = new Vector3D(-3 * Vector3D.dotProduct(p, v) / r2, a, -a.getNorm() / r, v);
        Assertions.assertEquals(differentiate(orbit, shifted -> shifted.getPVCoordinates().getAcceleration().getX()),
                            keplerianJerk.getX(),
                            3.0e-12 * keplerianJerk.getNorm());
        Assertions.assertEquals(differentiate(orbit, shifted -> shifted.getPVCoordinates().getAcceleration().getY()),
                            keplerianJerk.getY(),
                            3.0e-12 * keplerianJerk.getNorm());
        Assertions.assertEquals(differentiate(orbit, shifted -> shifted.getPVCoordinates().getAcceleration().getZ()),
                            keplerianJerk.getZ(),
                            3.0e-12 * keplerianJerk.getNorm());

        Assertions.assertEquals(0., orbit.getADot());
        Assertions.assertEquals(0., orbit.getEquinoctialExDot());
        Assertions.assertEquals(0., orbit.getEquinoctialEyDot());
        Assertions.assertEquals(0., orbit.getHxDot());
        Assertions.assertEquals(0., orbit.getHyDot());
        Assertions.assertEquals(0., orbit.getEDot());
        Assertions.assertEquals(0., orbit.getIDot());
        Assertions.assertEquals(0., orbit.getPerigeeArgumentDot());
        Assertions.assertEquals(0., orbit.getRightAscensionOfAscendingNodeDot());

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
    void testNonKeplerianEllipticDerivatives() {
        final AbsoluteDate date         = new AbsoluteDate("2003-05-01T00:00:20.000", TimeScalesFactory.getUTC());
        final Vector3D     position     = new Vector3D(6896874.444705,  1956581.072644,  -147476.245054);
        final Vector3D     velocity     = new Vector3D(166.816407662, -1106.783301861, -7372.745712770);
        final Vector3D     acceleration = new Vector3D(-7.466182457944, -2.118153357345,  0.160004048437);
        final TimeStampedPVCoordinates pv = new TimeStampedPVCoordinates(date, position, velocity, acceleration);
        final Frame frame = FramesFactory.getEME2000();
        final double mu   = Constants.EIGEN5C_EARTH_MU;
        final KeplerianOrbit orbit = new KeplerianOrbit(pv, frame, mu);

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
                            1.6e-15);
        Assertions.assertEquals(differentiate(pv, frame, mu, shifted -> shifted.getHy()),
                            orbit.getHyDot(),
                            7.3e-17);
        Assertions.assertEquals(differentiate(pv, frame, mu, shifted -> shifted.getLv()),
                            orbit.getLvDot(),
                            1.1e-14);
        Assertions.assertEquals(differentiate(pv, frame, mu, shifted -> shifted.getLE()),
                            orbit.getLEDot(),
                            7.2e-15);
        Assertions.assertEquals(differentiate(pv, frame, mu, shifted -> shifted.getLM()),
                            orbit.getLMDot(),
                            4.7e-15);
        Assertions.assertEquals(differentiate(pv, frame, mu, shifted -> shifted.getE()),
                            orbit.getEDot(),
                            6.9e-16);
        Assertions.assertEquals(differentiate(pv, frame, mu, shifted -> shifted.getI()),
                            orbit.getIDot(),
                            5.8e-16);
        Assertions.assertEquals(differentiate(pv, frame, mu, shifted -> shifted.getPerigeeArgument()),
                            orbit.getPerigeeArgumentDot(),
                            1.5e-12);
        Assertions.assertEquals(differentiate(pv, frame, mu, shifted -> shifted.getRightAscensionOfAscendingNode()),
                            orbit.getRightAscensionOfAscendingNodeDot(),
                            1.5e-15);
        Assertions.assertEquals(differentiate(pv, frame, mu, shifted -> shifted.getTrueAnomaly()),
                            orbit.getTrueAnomalyDot(),
                            1.5e-12);
        Assertions.assertEquals(differentiate(pv, frame, mu, shifted -> shifted.getEccentricAnomaly()),
                            orbit.getEccentricAnomalyDot(),
                            1.5e-12);
        Assertions.assertEquals(differentiate(pv, frame, mu, shifted -> shifted.getMeanAnomaly()),
                            orbit.getMeanAnomalyDot(),
                            1.5e-12);
        Assertions.assertEquals(differentiate(pv, frame, mu, shifted -> shifted.getAnomaly(PositionAngleType.TRUE)),
                            orbit.getAnomalyDot(PositionAngleType.TRUE),
                            1.5e-12);
        Assertions.assertEquals(differentiate(pv, frame, mu, shifted -> shifted.getAnomaly(PositionAngleType.ECCENTRIC)),
                            orbit.getAnomalyDot(PositionAngleType.ECCENTRIC),
                            1.5e-12);
        Assertions.assertEquals(differentiate(pv, frame, mu, shifted -> shifted.getAnomaly(PositionAngleType.MEAN)),
                            orbit.getAnomalyDot(PositionAngleType.MEAN),
                            1.5e-12);

    }

    @Test
    void testNonKeplerianHyperbolicDerivatives() {
        final AbsoluteDate date         = new AbsoluteDate("2003-05-01T00:00:20.000", TimeScalesFactory.getUTC());
        final Vector3D     position     = new Vector3D(224267911.905821, 290251613.109399, 45534292.777492);
        final Vector3D     velocity     = new Vector3D(-1494.068165293, 1124.771027677, 526.915286134);
        final Vector3D     acceleration = new Vector3D(-0.001295920501, -0.002233045187, -0.000349906292);
        final TimeStampedPVCoordinates pv = new TimeStampedPVCoordinates(date, position, velocity, acceleration);
        final Frame frame = FramesFactory.getEME2000();
        final double mu   = Constants.EIGEN5C_EARTH_MU;
        final KeplerianOrbit orbit = new KeplerianOrbit(pv, frame, mu);

        Assertions.assertEquals(differentiate(pv, frame, mu, shifted -> shifted.getA()),
                            orbit.getADot(),
                            9.6e-8);
        Assertions.assertEquals(differentiate(pv, frame, mu, shifted -> shifted.getEquinoctialEx()),
                            orbit.getEquinoctialExDot(),
                            2.8e-16);
        Assertions.assertEquals(differentiate(pv, frame, mu, shifted -> shifted.getEquinoctialEy()),
                            orbit.getEquinoctialEyDot(),
                            3.6e-15);
        Assertions.assertEquals(differentiate(pv, frame, mu, shifted -> shifted.getHx()),
                            orbit.getHxDot(),
                            1.4e-15);
        Assertions.assertEquals(differentiate(pv, frame, mu, shifted -> shifted.getHy()),
                            orbit.getHyDot(),
                            9.4e-16);
        Assertions.assertEquals(differentiate(pv, frame, mu, shifted -> shifted.getLv()),
                            orbit.getLvDot(),
                            5.6e-16);
        Assertions.assertEquals(differentiate(pv, frame, mu, shifted -> shifted.getLE()),
                            orbit.getLEDot(),
                            9.0e-16);
        Assertions.assertEquals(differentiate(pv, frame, mu, shifted -> shifted.getLM()),
                            orbit.getLMDot(),
                            1.8e-15);
        Assertions.assertEquals(differentiate(pv, frame, mu, shifted -> shifted.getE()),
                            orbit.getEDot(),
                            1.8e-15);
        Assertions.assertEquals(differentiate(pv, frame, mu, shifted -> shifted.getI()),
                            orbit.getIDot(),
                            3.6e-15);
        Assertions.assertEquals(differentiate(pv, frame, mu, shifted -> shifted.getPerigeeArgument()),
                            orbit.getPerigeeArgumentDot(),
                            9.4e-16);
        Assertions.assertEquals(differentiate(pv, frame, mu, shifted -> shifted.getRightAscensionOfAscendingNode()),
                            orbit.getRightAscensionOfAscendingNodeDot(),
                            1.1e-15);
        Assertions.assertEquals(differentiate(pv, frame, mu, shifted -> shifted.getTrueAnomaly()),
                            orbit.getTrueAnomalyDot(),
                            1.4e-15);
        Assertions.assertEquals(differentiate(pv, frame, mu, shifted -> shifted.getEccentricAnomaly()),
                            orbit.getEccentricAnomalyDot(),
                            9.2e-16);
        Assertions.assertEquals(differentiate(pv, frame, mu, shifted -> shifted.getMeanAnomaly()),
                            orbit.getMeanAnomalyDot(),
                            1.4e-15);
        Assertions.assertEquals(differentiate(pv, frame, mu, shifted -> shifted.getAnomaly(PositionAngleType.TRUE)),
                            orbit.getAnomalyDot(PositionAngleType.TRUE),
                            1.4e-15);
        Assertions.assertEquals(differentiate(pv, frame, mu, shifted -> shifted.getAnomaly(PositionAngleType.ECCENTRIC)),
                            orbit.getAnomalyDot(PositionAngleType.ECCENTRIC),
                            9.2e-16);
        Assertions.assertEquals(differentiate(pv, frame, mu, shifted -> shifted.getAnomaly(PositionAngleType.MEAN)),
                            orbit.getAnomalyDot(PositionAngleType.MEAN),
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
    void testPositionAngleDerivatives() {
        final AbsoluteDate date         = new AbsoluteDate("2003-05-01T00:00:20.000", TimeScalesFactory.getUTC());
        final Vector3D     position     = new Vector3D(6896874.444705,  1956581.072644,  -147476.245054);
        final Vector3D     velocity     = new Vector3D(166.816407662, -1106.783301861, -7372.745712770);
        final Vector3D     acceleration = new Vector3D(-7.466182457944, -2.118153357345,  0.160004048437);
        final TimeStampedPVCoordinates pv = new TimeStampedPVCoordinates(date, position, velocity, acceleration);
        final Frame frame = FramesFactory.getEME2000();
        final double mu   = Constants.EIGEN5C_EARTH_MU;
        final KeplerianOrbit orbit = new KeplerianOrbit(pv, frame, mu);

        for (PositionAngleType type : PositionAngleType.values()) {
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
            MatcherAssert.assertThat(rebuilt.getA(),                                relativelyCloseTo(orbit.getA(),                                1));
            MatcherAssert.assertThat(rebuilt.getE(),                                relativelyCloseTo(orbit.getE(),                                1));
            MatcherAssert.assertThat(rebuilt.getI(),                                relativelyCloseTo(orbit.getI(),                                1));
            MatcherAssert.assertThat(rebuilt.getPerigeeArgument(),                  relativelyCloseTo(orbit.getPerigeeArgument(),                  1));
            MatcherAssert.assertThat(rebuilt.getRightAscensionOfAscendingNode(),    relativelyCloseTo(orbit.getRightAscensionOfAscendingNode(),    1));
            MatcherAssert.assertThat(rebuilt.getADot(),                             relativelyCloseTo(orbit.getADot(),                             1));
            MatcherAssert.assertThat(rebuilt.getEDot(),                             relativelyCloseTo(orbit.getEDot(),                             1));
            MatcherAssert.assertThat(rebuilt.getIDot(),                             relativelyCloseTo(orbit.getIDot(),                             1));
            MatcherAssert.assertThat(rebuilt.getPerigeeArgumentDot(),               relativelyCloseTo(orbit.getPerigeeArgumentDot(),               1));
            MatcherAssert.assertThat(rebuilt.getRightAscensionOfAscendingNodeDot(), relativelyCloseTo(orbit.getRightAscensionOfAscendingNodeDot(), 1));
            for (PositionAngleType type2 : PositionAngleType.values()) {
                MatcherAssert.assertThat(rebuilt.getAnomaly(type2),    relativelyCloseTo(orbit.getAnomaly(type2),    1));
                MatcherAssert.assertThat(rebuilt.getAnomalyDot(type2), relativelyCloseTo(orbit.getAnomalyDot(type2), 1));
            }
        }

    }

    @Test
    void testPositionAngleHyperbolicDerivatives() {
        final AbsoluteDate date         = new AbsoluteDate("2003-05-01T00:00:20.000", TimeScalesFactory.getUTC());
        final Vector3D     position     = new Vector3D(224267911.905821, 290251613.109399, 45534292.777492);
        final Vector3D     velocity     = new Vector3D(-1494.068165293, 1124.771027677, 526.915286134);
        final Vector3D     acceleration = new Vector3D(-0.001295920501, -0.002233045187, -0.000349906292);
        final TimeStampedPVCoordinates pv = new TimeStampedPVCoordinates(date, position, velocity, acceleration);
        final Frame frame = FramesFactory.getEME2000();
        final double mu   = Constants.EIGEN5C_EARTH_MU;
        final KeplerianOrbit orbit = new KeplerianOrbit(pv, frame, mu);

        for (PositionAngleType type : PositionAngleType.values()) {
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
            MatcherAssert.assertThat(rebuilt.getA(),                                relativelyCloseTo(orbit.getA(),                                1));
            MatcherAssert.assertThat(rebuilt.getE(),                                relativelyCloseTo(orbit.getE(),                                1));
            MatcherAssert.assertThat(rebuilt.getI(),                                relativelyCloseTo(orbit.getI(),                                1));
            MatcherAssert.assertThat(rebuilt.getPerigeeArgument(),                  relativelyCloseTo(orbit.getPerigeeArgument(),                  1));
            MatcherAssert.assertThat(rebuilt.getRightAscensionOfAscendingNode(),    relativelyCloseTo(orbit.getRightAscensionOfAscendingNode(),    1));
            MatcherAssert.assertThat(rebuilt.getADot(),                             relativelyCloseTo(orbit.getADot(),                             1));
            MatcherAssert.assertThat(rebuilt.getEDot(),                             relativelyCloseTo(orbit.getEDot(),                             1));
            MatcherAssert.assertThat(rebuilt.getIDot(),                             relativelyCloseTo(orbit.getIDot(),                             1));
            MatcherAssert.assertThat(rebuilt.getPerigeeArgumentDot(),               relativelyCloseTo(orbit.getPerigeeArgumentDot(),               1));
            MatcherAssert.assertThat(rebuilt.getRightAscensionOfAscendingNodeDot(), relativelyCloseTo(orbit.getRightAscensionOfAscendingNodeDot(), 1));
            for (PositionAngleType type2 : PositionAngleType.values()) {
                MatcherAssert.assertThat(rebuilt.getAnomaly(type2),    relativelyCloseTo(orbit.getAnomaly(type2),    3));
                MatcherAssert.assertThat(rebuilt.getAnomalyDot(type2), relativelyCloseTo(orbit.getAnomalyDot(type2), 4));
            }
        }

    }

    @Test
    void testEquatorialRetrograde() {
        Vector3D position = new Vector3D(10000000.0, 0.0, 0.0);
        Vector3D velocity = new Vector3D(0.0, -6500.0, 1.0e-10);
        double r2 = position.getNormSq();
        double r  = FastMath.sqrt(r2);
        Vector3D acceleration = new Vector3D(-mu / (r * r2), position,
                                             1, new Vector3D(-0.1, 0.2, 0.3));
        PVCoordinates pvCoordinates = new PVCoordinates(position, velocity, acceleration);
        KeplerianOrbit orbit = new KeplerianOrbit(pvCoordinates, FramesFactory.getEME2000(), date, mu);
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
    void testDerivativesConversionSymmetry() {
        final AbsoluteDate date = new AbsoluteDate("2003-05-01T00:01:20.000", TimeScalesFactory.getUTC());
        Vector3D position     = new Vector3D(6893443.400234382, 1886406.1073757345, -589265.1150359757);
        Vector3D velocity     = new Vector3D(-281.1261461082365, -1231.6165642450928, -7348.756363469432);
        Vector3D acceleration = new Vector3D(-7.460341170581685, -2.0415957334584527, 0.6393322823627762);
        PVCoordinates pvCoordinates = new PVCoordinates( position, velocity, acceleration);
        KeplerianOrbit orbit = new KeplerianOrbit(pvCoordinates, FramesFactory.getEME2000(),
                                                  date, Constants.EIGEN5C_EARTH_MU);
        Assertions.assertTrue(orbit.hasNonKeplerianAcceleration());
        double r2 = position.getNormSq();
        double r  = FastMath.sqrt(r2);
        Vector3D keplerianAcceleration = new Vector3D(-orbit.getMu() / (r2 * r), position);
        Assertions.assertEquals(0.0101, Vector3D.distance(keplerianAcceleration, acceleration), 1.0e-4);

        for (OrbitType type : OrbitType.values()) {
            Orbit converted = type.convertType(orbit);
            Assertions.assertTrue(converted.hasNonKeplerianAcceleration());
            KeplerianOrbit rebuilt = (KeplerianOrbit) OrbitType.KEPLERIAN.convertType(converted);
            Assertions.assertTrue(rebuilt.hasNonKeplerianAcceleration());
            Assertions.assertEquals(orbit.getADot(),                             rebuilt.getADot(),                             3.0e-13);
            Assertions.assertEquals(orbit.getEDot(),                             rebuilt.getEDot(),                             1.0e-15);
            Assertions.assertEquals(orbit.getIDot(),                             rebuilt.getIDot(),                             1.0e-15);
            Assertions.assertEquals(orbit.getPerigeeArgumentDot(),               rebuilt.getPerigeeArgumentDot(),               1.0e-15);
            Assertions.assertEquals(orbit.getRightAscensionOfAscendingNodeDot(), rebuilt.getRightAscensionOfAscendingNodeDot(), 1.0e-15);
            Assertions.assertEquals(orbit.getTrueAnomalyDot(),                   rebuilt.getTrueAnomalyDot(),                   1.0e-15);
        }

    }

    @Test
    void testDerivativesConversionSymmetryHyperbolic() {
        final AbsoluteDate date         = new AbsoluteDate("2003-05-01T00:00:20.000", TimeScalesFactory.getUTC());
        final Vector3D     position     = new Vector3D(224267911.905821, 290251613.109399, 45534292.777492);
        final Vector3D     velocity     = new Vector3D(-1494.068165293, 1124.771027677, 526.915286134);
        final Vector3D     acceleration = new Vector3D(-0.001295920501, -0.002233045187, -0.000349906292);
        PVCoordinates pvCoordinates = new PVCoordinates( position, velocity, acceleration);
        KeplerianOrbit orbit = new KeplerianOrbit(pvCoordinates, FramesFactory.getEME2000(),
                                                  date, Constants.EIGEN5C_EARTH_MU);
        Assertions.assertTrue(orbit.hasNonKeplerianAcceleration());
        double r2 = position.getNormSq();
        double r  = FastMath.sqrt(r2);
        Vector3D keplerianAcceleration = new Vector3D(-orbit.getMu() / (r2 * r), position);
        Assertions.assertEquals(4.78e-4, Vector3D.distance(keplerianAcceleration, acceleration), 1.0e-6);

        OrbitType type = OrbitType.CARTESIAN;
        Orbit converted = type.convertType(orbit);
        Assertions.assertTrue(converted.hasNonKeplerianAcceleration());
        KeplerianOrbit rebuilt = (KeplerianOrbit) OrbitType.KEPLERIAN.convertType(converted);
        Assertions.assertTrue(rebuilt.hasNonKeplerianAcceleration());
        Assertions.assertEquals(orbit.getADot(),                             rebuilt.getADot(),                             3.0e-13);
        Assertions.assertEquals(orbit.getEDot(),                             rebuilt.getEDot(),                             1.0e-15);
        Assertions.assertEquals(orbit.getIDot(),                             rebuilt.getIDot(),                             1.0e-15);
        Assertions.assertEquals(orbit.getPerigeeArgumentDot(),               rebuilt.getPerigeeArgumentDot(),               1.0e-15);
        Assertions.assertEquals(orbit.getRightAscensionOfAscendingNodeDot(), rebuilt.getRightAscensionOfAscendingNodeDot(), 1.0e-15);
        Assertions.assertEquals(orbit.getTrueAnomalyDot(),                   rebuilt.getTrueAnomalyDot(),                   1.0e-15);

    }

    @Test
    void testNonKeplerianAcceleration() {
        // GIVEN
        final PVCoordinates pvCoordinates = new PVCoordinates(new Vector3D(1, 2, 3),
                Vector3D.MINUS_K.scalarMultiply(0.1), Vector3D.MINUS_I);
        final CartesianOrbit cartesianOrbit = new CartesianOrbit(pvCoordinates, FramesFactory.getEME2000(),
                AbsoluteDate.ARBITRARY_EPOCH, 1.);
        final KeplerianOrbit keplerianOrbit = (KeplerianOrbit) OrbitType.KEPLERIAN.convertType(cartesianOrbit);
        // WHEN
        final Vector3D nonKeplerianAcceleration = keplerianOrbit.nonKeplerianAcceleration();
        // THEN
        final Vector3D expectedVector = computeLegacyNonKeplerianAcceleration(keplerianOrbit);
        Assertions.assertArrayEquals(expectedVector.toArray(), nonKeplerianAcceleration.toArray(), 1e-10);
    }

    private Vector3D computeLegacyNonKeplerianAcceleration(final Orbit orbit) {
        final double[][] dCdP = new double[6][6];
        final PositionAngleType positionAngleType = PositionAngleType.MEAN;
        orbit.getJacobianWrtParameters(positionAngleType, dCdP);

        final double[] derivatives = new double[6];
        orbit.getType().mapOrbitToArray(orbit, positionAngleType, new double[6], derivatives);
        derivatives[5] -= orbit.getKeplerianMeanMotion();
        final double nonKeplerianAx = dCdP[3][0] * derivatives[0]  + dCdP[3][1] * derivatives[1] + dCdP[3][2] * derivatives[2] +
                dCdP[3][3] * derivatives[3] + dCdP[3][4] * derivatives[4] + dCdP[3][5] * derivatives[5];
        final double nonKeplerianAy = dCdP[4][0] * derivatives[0]  + dCdP[4][1] * derivatives[1] + dCdP[4][2] * derivatives[2] +
                dCdP[4][3] * derivatives[3] + dCdP[4][4] * derivatives[4] + dCdP[4][5] * derivatives[5];
        final double nonKeplerianAz = dCdP[5][0] * derivatives[0]  + dCdP[5][1] * derivatives[1] + dCdP[5][2] * derivatives[2] +
                dCdP[5][3] * derivatives[3] + dCdP[5][4] * derivatives[4] + dCdP[5][5] * derivatives[5];

        return new Vector3D(nonKeplerianAx, nonKeplerianAy, nonKeplerianAz);
    }

    @Test
    void testToString() {
        Vector3D position = new Vector3D(-29536113.0, 30329259.0, -100125.0);
        Vector3D velocity = new Vector3D(-2194.0, -2141.0, -8.0);
        PVCoordinates pvCoordinates = new PVCoordinates(position, velocity);
        KeplerianOrbit orbit = new KeplerianOrbit(pvCoordinates, FramesFactory.getEME2000(), date, mu);
        Assertions.assertEquals("Keplerian parameters: {a: 4.225517000282565E7; e: 0.002146216321416967; i: 0.20189257051515358; pa: 13.949966363606599; raan: -87.91788415673473; v: -151.79096272977213;}",
                            orbit.toString());
    }

    @Test
    void testWithKeplerianRates() {
        // GIVEN
        final Vector3D position = new Vector3D(-29536113.0, 30329259.0, -100125.0);
        final Vector3D velocity = new Vector3D(-2194.0, -2141.0, -8.0);
        final PVCoordinates pvCoordinates = new PVCoordinates(position, velocity);
        final KeplerianOrbit orbit = new KeplerianOrbit(pvCoordinates, FramesFactory.getGCRF(), date, mu);
        // WHEN
        final KeplerianOrbit orbitWithoutRates = orbit.withKeplerianRates();
        // THEN
        Assertions.assertFalse(orbitWithoutRates.hasNonKeplerianRates());
        Assertions.assertEquals(orbit.getMu(), orbitWithoutRates.getMu());
        Assertions.assertEquals(orbit.getDate(), orbitWithoutRates.getDate());
        Assertions.assertEquals(orbit.getFrame(), orbitWithoutRates.getFrame());
        Assertions.assertEquals(orbit.getA(), orbitWithoutRates.getA());
        Assertions.assertEquals(orbit.getE(), orbitWithoutRates.getE());
        Assertions.assertEquals(orbit.getI(), orbitWithoutRates.getI());
        Assertions.assertEquals(orbit.getRightAscensionOfAscendingNode(),
                orbitWithoutRates.getRightAscensionOfAscendingNode());
        Assertions.assertEquals(orbit.getPerigeeArgument(), orbitWithoutRates.getPerigeeArgument());
        Assertions.assertEquals(orbit.getTrueAnomaly(), orbitWithoutRates.getTrueAnomaly());
    }

    @Test
    void testCopyNonKeplerianAcceleration() {

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

        Assertions.assertEquals(0.0,
                            Vector3D.distance(shiftedOrbit.getPosition(),
                                              shiftedOrbitCopy.getPosition()),
                            1.0e-10);
        Assertions.assertEquals(0.0,
                            Vector3D.distance(shiftedOrbit.getVelocity(),
                                              shiftedOrbitCopy.getVelocity()),
                            1.0e-10);

    }

    @Test
    void testIssue674() {
        try {
            new KeplerianOrbit(24464560.0, -0.7311, 0.122138, 3.10686, 1.00681,
                               0.048363, PositionAngleType.MEAN,
                               FramesFactory.getEME2000(), date, mu);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.INVALID_PARAMETER_RANGE, oe.getSpecifier());
            Assertions.assertEquals("eccentricity", oe.getParts()[0]);
            Assertions.assertEquals(-0.7311, ((Double) oe.getParts()[1]).doubleValue(), Double.MIN_VALUE);
            Assertions.assertEquals(0.0, ((Double) oe.getParts()[2]).doubleValue(), Double.MIN_VALUE);
            Assertions.assertEquals(Double.POSITIVE_INFINITY, ((Double) oe.getParts()[3]).doubleValue(), Double.MIN_VALUE);
        }
    }

    @Test
    void testNormalize() {
        KeplerianOrbit withoutDerivatives =
                        new KeplerianOrbit(42166712.0, 0.005, 1.6,
                                           -0.3, 1.25, 0.4, PositionAngleType.MEAN,
                                           FramesFactory.getEME2000(), date, mu);
        KeplerianOrbit ref =
                        new KeplerianOrbit(24000000.0, 0.012, 1.9,
                                          -6.28, 6.28, 12.56, PositionAngleType.MEAN,
                                          FramesFactory.getEME2000(), date, mu);

        KeplerianOrbit normalized1 = (KeplerianOrbit) OrbitType.KEPLERIAN.normalize(withoutDerivatives, ref);
        Assertions.assertFalse(normalized1.hasNonKeplerianAcceleration());
        Assertions.assertEquals(0.0, normalized1.getA() - withoutDerivatives.getA(), 1.0e-6);
        Assertions.assertEquals(0.0, normalized1.getE() - withoutDerivatives.getE(), 1.0e-10);
        Assertions.assertEquals(0.0, normalized1.getI() - withoutDerivatives.getI(), 1.0e-10);
        Assertions.assertEquals(-MathUtils.TWO_PI, normalized1.getPerigeeArgument()               - withoutDerivatives.getPerigeeArgument(),               1.0e-10);
        Assertions.assertEquals(+MathUtils.TWO_PI, normalized1.getRightAscensionOfAscendingNode() - withoutDerivatives.getRightAscensionOfAscendingNode(), 1.0e-10);
        Assertions.assertEquals(2 * MathUtils.TWO_PI, normalized1.getTrueAnomaly()                - withoutDerivatives.getTrueAnomaly(),                   1.0e-10);
        Assertions.assertEquals(withoutDerivatives.getADot(), normalized1.getADot());
        Assertions.assertEquals(withoutDerivatives.getEDot(), normalized1.getEDot());
        Assertions.assertEquals(withoutDerivatives.getIDot(), normalized1.getIDot());
        Assertions.assertEquals(withoutDerivatives.getPerigeeArgumentDot(), normalized1.getPerigeeArgumentDot());
        Assertions.assertEquals(withoutDerivatives.getRightAscensionOfAscendingNodeDot(), normalized1.getRightAscensionOfAscendingNodeDot());
        Assertions.assertEquals(withoutDerivatives.getTrueAnomalyDot(), normalized1.getTrueAnomalyDot());

        double[] p = new double[6];
        OrbitType.KEPLERIAN.mapOrbitToArray(withoutDerivatives, PositionAngleType.TRUE, p, null);
        KeplerianOrbit withDerivatives = (KeplerianOrbit) OrbitType.KEPLERIAN.mapArrayToOrbit(p,
                                                                                              new double[] { 1.0, 2.0, 3.0, 4.0, 5.0, 6.0 },
                                                                                              PositionAngleType.TRUE,
                                                                                              withoutDerivatives.getDate(),
                                                                                              withoutDerivatives.getMu(),
                                                                                              withoutDerivatives.getFrame());
        KeplerianOrbit normalized2 = (KeplerianOrbit) OrbitType.KEPLERIAN.normalize(withDerivatives, ref);
        Assertions.assertTrue(normalized2.hasNonKeplerianAcceleration());
        Assertions.assertEquals(0.0, normalized2.getA() - withoutDerivatives.getA(), 1.0e-6);
        Assertions.assertEquals(0.0, normalized2.getE() - withoutDerivatives.getE(), 1.0e-10);
        Assertions.assertEquals(0.0, normalized2.getI() - withoutDerivatives.getI(), 1.0e-10);
        Assertions.assertEquals(-MathUtils.TWO_PI, normalized2.getPerigeeArgument()               - withoutDerivatives.getPerigeeArgument(),               1.0e-10);
        Assertions.assertEquals(+MathUtils.TWO_PI, normalized2.getRightAscensionOfAscendingNode() - withoutDerivatives.getRightAscensionOfAscendingNode(), 1.0e-10);
        Assertions.assertEquals(2 * MathUtils.TWO_PI, normalized2.getTrueAnomaly()                - withoutDerivatives.getTrueAnomaly(),                   1.0e-10);
        Assertions.assertEquals(0.0, normalized2.getADot() - withDerivatives.getADot(), 1.0e-10);
        Assertions.assertEquals(0.0, normalized2.getEDot() - withDerivatives.getEDot(), 1.0e-10);
        Assertions.assertEquals(0.0, normalized2.getIDot() - withDerivatives.getIDot(), 1.0e-10);
        Assertions.assertEquals(0.0, normalized2.getPerigeeArgumentDot()               - withDerivatives.getPerigeeArgumentDot(),               1.0e-10);
        Assertions.assertEquals(0.0, normalized2.getRightAscensionOfAscendingNodeDot() - withDerivatives.getRightAscensionOfAscendingNodeDot(), 1.0e-10);
        Assertions.assertEquals(0.0, normalized2.getTrueAnomalyDot()                   - withDerivatives.getTrueAnomalyDot(),                   1.0e-10);

    }

    @Test
    void testKeplerianToPvToKeplerian() {
        // setup
        Frame eci = FramesFactory.getGCRF();
        AbsoluteDate date = AbsoluteDate.ARBITRARY_EPOCH;
        double mu = Constants.EGM96_EARTH_MU;
        KeplerianOrbit orbit = new KeplerianOrbit(6878e3, 0, 0, 0, 0, 0,
                PositionAngleType.TRUE, eci, date, mu);

        // action
        KeplerianOrbit pvOrbit = new KeplerianOrbit(orbit.getPVCoordinates(), eci, mu);

        // verify
        Assertions.assertEquals(orbit.shiftedBy(1).getA(), pvOrbit.shiftedBy(1).getA(), 1e-6);
        Assertions.assertEquals(orbit.shiftedBy(1).getE(), pvOrbit.shiftedBy(1).getE(), 1e-14);
        Assertions.assertEquals(orbit.shiftedBy(1).getI(), pvOrbit.shiftedBy(1).getI(), 1e-10);
        Assertions.assertEquals(orbit.shiftedBy(1).getPerigeeArgument(), pvOrbit.shiftedBy(1).getPerigeeArgument(), 1e-10);
        Assertions.assertEquals(orbit.shiftedBy(1).getRightAscensionOfAscendingNode(), pvOrbit.shiftedBy(1).getRightAscensionOfAscendingNode(), 1e-10);
        Assertions.assertEquals(orbit.shiftedBy(1).getTrueAnomaly(), pvOrbit.shiftedBy(1).getTrueAnomaly(), 1e-10);

    }

    @Test
    void testCoverageCachedPositionAngleTypeElliptic() {
        testCoverageCachedPositionAngleType(1e4, 0.5);
    }

    @Test
    void testCoverageCachedPositionAngleTypeHyperbolic() {
        testCoverageCachedPositionAngleType(-1e4, 2);
    }

    private void testCoverageCachedPositionAngleType(final double a, final double e) {
        // GIVEN
        final double expectedAnomaly = 0.;
        // WHEN & THEN
        for (final PositionAngleType inputPositionAngleType: PositionAngleType.values()) {
            for (final PositionAngleType cachedPositionAngleType: PositionAngleType.values()) {
                final KeplerianOrbit keplerianOrbit = new KeplerianOrbit(a, e, 0., 0., 0.,
                        expectedAnomaly, inputPositionAngleType, cachedPositionAngleType, FramesFactory.getGCRF(), date, mu);
                Assertions.assertEquals(expectedAnomaly, keplerianOrbit.getTrueAnomaly());
                Assertions.assertEquals(expectedAnomaly, keplerianOrbit.getEccentricAnomaly());
                Assertions.assertEquals(expectedAnomaly, keplerianOrbit.getMeanAnomaly());
            }
        }
    }

    @Test
    void testCoverageCachedPositionAngleTypeWithRates() {
        // GIVEN
        final double semiMajorAxis = 1e4;
        final double eccentricity = 0.;
        final double expectedAnomaly = 0.;
        final double expectedAnomalyDot = 0.;
        // WHEN & THEN
        for (final PositionAngleType inputPositionAngleType: PositionAngleType.values()) {
            for (final PositionAngleType cachedPositionAngleType: PositionAngleType.values()) {
                final KeplerianOrbit keplerianOrbit = new KeplerianOrbit(semiMajorAxis, eccentricity, 0., 0., 0.,
                        expectedAnomaly, 0., 0., 0., 0., 0., expectedAnomalyDot,
                        inputPositionAngleType, cachedPositionAngleType, FramesFactory.getGCRF(), date, mu);
                Assertions.assertEquals(expectedAnomaly, keplerianOrbit.getTrueAnomaly());
                Assertions.assertEquals(expectedAnomaly, keplerianOrbit.getEccentricAnomaly());
                Assertions.assertEquals(expectedAnomaly, keplerianOrbit.getMeanAnomaly());
                Assertions.assertEquals(expectedAnomalyDot, keplerianOrbit.getTrueAnomalyDot());
                Assertions.assertEquals(expectedAnomalyDot, keplerianOrbit.getEccentricAnomalyDot());
                Assertions.assertEquals(expectedAnomalyDot, keplerianOrbit.getMeanAnomalyDot());
            }
        }
    }

    @Test
    void testCoverageCachedPositionAngleTypeWithRatesHyperbolic() {
        // GIVEN
        final double semiMajorAxis = -1e4;
        final double eccentricity = 2.;
        final double expectedAnomaly = 0.;
        final double expectedAnomalyDot = 0.;
        // WHEN & THEN
        for (final PositionAngleType inputPositionAngleType: PositionAngleType.values()) {
            for (final PositionAngleType cachedPositionAngleType: PositionAngleType.values()) {
                final KeplerianOrbit keplerianOrbit = new KeplerianOrbit(semiMajorAxis, eccentricity, 0., 0., 0.,
                        expectedAnomaly, 0., 0., 0., 0., 0., expectedAnomalyDot,
                        inputPositionAngleType, cachedPositionAngleType, FramesFactory.getGCRF(), date, mu);
                Assertions.assertEquals(expectedAnomaly, keplerianOrbit.getTrueAnomaly());
                Assertions.assertEquals(expectedAnomaly, keplerianOrbit.getEccentricAnomaly());
                Assertions.assertEquals(expectedAnomaly, keplerianOrbit.getMeanAnomaly());
                Assertions.assertEquals(expectedAnomalyDot, keplerianOrbit.getTrueAnomalyDot());
                Assertions.assertEquals(expectedAnomalyDot, keplerianOrbit.getEccentricAnomalyDot());
                Assertions.assertEquals(expectedAnomalyDot, keplerianOrbit.getMeanAnomalyDot());
            }
        }
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
