/* Copyright 2002-2021 CS GROUP
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
package org.orekit.attitudes;


import org.hamcrest.MatcherAssert;
import org.hipparchus.Field;
import org.hipparchus.CalculusFieldElement;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.RotationConvention;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.Decimal64;
import org.hipparchus.util.Decimal64Field;
import org.hipparchus.util.FastMath;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.Transform;
import org.orekit.orbits.FieldCartesianOrbit;
import org.orekit.orbits.FieldOrbit;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.KeplerianPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.AngularCoordinates;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.FieldPVCoordinatesProvider;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.orekit.OrekitMatchers.attitudeIs;
import static org.orekit.OrekitMatchers.closeTo;
import static org.orekit.OrekitMatchers.distanceIs;


public class InertialAttitudeTest {

    private AbsoluteDate t0;
    private Orbit        orbit0;

    @Test
    public void testIsInertial() {
        InertialProvider law = new InertialProvider(new Rotation(new Vector3D(0.6, 0.48, 0.64), 0.9, RotationConvention.VECTOR_OPERATOR));
        KeplerianPropagator propagator = new KeplerianPropagator(orbit0, law);
        Attitude initial = propagator.propagate(t0).getAttitude();
        for (double t = 0; t < 10000.0; t += 100) {
            SpacecraftState state = propagator.propagate(t0.shiftedBy(t));
            checkField(Decimal64Field.getInstance(), law, state.getOrbit(), state.getDate(), state.getFrame());
            Attitude attitude = state.getAttitude();
            Rotation evolution = attitude.getRotation().compose(initial.getRotation().revert(),
                                                                RotationConvention.VECTOR_OPERATOR);
            Assert.assertEquals(0, evolution.getAngle(), 1.0e-10);
            Assert.assertEquals(FramesFactory.getEME2000(), attitude.getReferenceFrame());
        }
    }

    @Test
    public void testCompensateMomentum() {
        InertialProvider law = new InertialProvider(new Rotation(new Vector3D(-0.64, 0.6, 0.48), 0.2, RotationConvention.VECTOR_OPERATOR));
        KeplerianPropagator propagator = new KeplerianPropagator(orbit0, law);
        Attitude initial = propagator.propagate(t0).getAttitude();
        for (double t = 0; t < 10000.0; t += 100) {
            Attitude attitude = propagator.propagate(t0.shiftedBy(t)).getAttitude();
            Rotation evolution = attitude.getRotation().compose(initial.getRotation().revert(),
                                                                RotationConvention.VECTOR_OPERATOR);
            Assert.assertEquals(0, evolution.getAngle(), 1.0e-10);
            Assert.assertEquals(FramesFactory.getEME2000(), attitude.getReferenceFrame());
        }
    }

    @Test
    public void testSpin() {

        AbsoluteDate date = new AbsoluteDate(new DateComponents(1970, 01, 01),
                                             new TimeComponents(3, 25, 45.6789),
                                             TimeScalesFactory.getUTC());

        AttitudeProvider law = new InertialProvider(new Rotation(new Vector3D(-0.64, 0.6, 0.48), 0.2, RotationConvention.VECTOR_OPERATOR));

        KeplerianOrbit orbit =
            new KeplerianOrbit(7178000.0, 1.e-4, FastMath.toRadians(50.),
                              FastMath.toRadians(10.), FastMath.toRadians(20.),
                              FastMath.toRadians(30.), PositionAngle.MEAN,
                              FramesFactory.getEME2000(), date, 3.986004415e14);

        Propagator propagator = new KeplerianPropagator(orbit, law);

        double h = 100.0;
        SpacecraftState sMinus = propagator.propagate(date.shiftedBy(-h));
        SpacecraftState s0     = propagator.propagate(date);
        SpacecraftState sPlus  = propagator.propagate(date.shiftedBy(h));

        // check spin is consistent with attitude evolution
        double errorAngleMinus     = Rotation.distance(sMinus.shiftedBy(h).getAttitude().getRotation(),
                                                       s0.getAttitude().getRotation());
        double evolutionAngleMinus = Rotation.distance(sMinus.getAttitude().getRotation(),
                                                       s0.getAttitude().getRotation());
        Assert.assertEquals(0.0, errorAngleMinus, 1.0e-6 * evolutionAngleMinus);
        double errorAnglePlus      = Rotation.distance(s0.getAttitude().getRotation(),
                                                       sPlus.shiftedBy(-h).getAttitude().getRotation());
        double evolutionAnglePlus  = Rotation.distance(s0.getAttitude().getRotation(),
                                                       sPlus.getAttitude().getRotation());
        Assert.assertEquals(0.0, errorAnglePlus, 1.0e-6 * evolutionAnglePlus);

        // compute spin axis using finite differences
        Rotation rMinus = sMinus.getAttitude().getRotation();
        Rotation rPlus  = sPlus.getAttitude().getRotation();
        Rotation dr     = rPlus.compose(rMinus.revert(), RotationConvention.VECTOR_OPERATOR);
        Assert.assertEquals(0, dr.getAngle(), 1.0e-10);

        Vector3D spin0 = s0.getAttitude().getSpin();
        Assert.assertEquals(0, spin0.getNorm(), 1.0e-10);

    }

    private <T extends CalculusFieldElement<T>> void checkField(final Field<T> field, final AttitudeProvider provider,
                                                            final Orbit orbit, final AbsoluteDate date,
                                                            final Frame frame)
        {
        Attitude attitudeD = provider.getAttitude(orbit, date, frame);
        final FieldOrbit<T> orbitF = new FieldSpacecraftState<>(field, new SpacecraftState(orbit)).getOrbit();
        final FieldAbsoluteDate<T> dateF = new FieldAbsoluteDate<>(field, date);
        FieldAttitude<T> attitudeF = provider.getAttitude(orbitF, dateF, frame);
        Assert.assertEquals(0.0, Rotation.distance(attitudeD.getRotation(), attitudeF.getRotation().toRotation()), 1.0e-15);
        Assert.assertEquals(0.0, Vector3D.distance(attitudeD.getSpin(), attitudeF.getSpin().toVector3D()), 1.0e-15);
        Assert.assertEquals(0.0, Vector3D.distance(attitudeD.getRotationAcceleration(), attitudeF.getRotationAcceleration().toVector3D()), 1.0e-15);
    }

    @Test
    public void testGetAttitude() {
        // expected
        Frame eci = orbit0.getFrame();
        Attitude expected = new Attitude(t0, eci, AngularCoordinates.IDENTITY);
        AttitudeProvider law = InertialProvider.of(eci);

        // action + verify
        Attitude actual = law.getAttitude(orbit0, t0, eci);
        MatcherAssert.assertThat(actual.getReferenceFrame(), is(eci));
        MatcherAssert.assertThat(actual, attitudeIs(expected));
        actual = law.getAttitude(orbit0.shiftedBy(1e3), t0.shiftedBy(1e3), eci);
        MatcherAssert.assertThat(actual.getReferenceFrame(), is(eci));
        MatcherAssert.assertThat(actual, attitudeIs(expected));
        // create new frame for testing frame transforms
        Rotation rotation = new Rotation(
                Vector3D.PLUS_K,
                FastMath.PI / 2.0,
                RotationConvention.FRAME_TRANSFORM);
        Transform angular = new Transform(
                t0,
                rotation,
                new Vector3D(1, 2, 3),
                new Vector3D(-4, 5, 6));
        Transform translation = new Transform(
                t0,
                new Vector3D(-1, 2, -3),
                new Vector3D(4, -5, 6),
                new Vector3D(7, 8, -9));
        Frame other = new Frame(eci, new Transform(t0, angular, translation), "other");
        actual = law.getAttitude(orbit0.shiftedBy(1e3), t0.shiftedBy(1e3), other);
        MatcherAssert.assertThat(actual.getReferenceFrame(), is(other));
        MatcherAssert.assertThat(actual, attitudeIs(expected));
        // check not identity rotation
        MatcherAssert.assertThat(actual.getRotation(),
                not(distanceIs(Rotation.IDENTITY, closeTo(0.0, 1e-1))));
    }


    /**
     * Unit tests for {@link FrameAligned#getAttitude(FieldPVCoordinatesProvider,
     * FieldAbsoluteDate, Frame)}.
     */
    @Test
    public void testGetAttitudeField() {
        // expected
        Frame eci = orbit0.getFrame();
        Attitude expected = new Attitude(t0, eci, AngularCoordinates.IDENTITY);
        AttitudeProvider law = InertialProvider.of(eci);
        Decimal64 one = Decimal64.ONE;
        FieldAbsoluteDate<Decimal64> date = new FieldAbsoluteDate<>(one.getField(), t0);
        FieldOrbit<Decimal64> orbit = new FieldCartesianOrbit<>(
                new FieldPVCoordinates<>(one, this.orbit0.getPVCoordinates()),
                eci,
                date,
                one.multiply(orbit0.getMu()));

        // action + verify
        FieldAttitude<Decimal64> actual = law.getAttitude(orbit, date, eci);
        MatcherAssert.assertThat(actual.getReferenceFrame(), is(eci));
        MatcherAssert.assertThat(actual.toAttitude(), attitudeIs(expected));
        actual = law.getAttitude(orbit.shiftedBy(1e3), date.shiftedBy(1e3), eci);
        MatcherAssert.assertThat(actual.getReferenceFrame(), is(eci));
        MatcherAssert.assertThat(actual.toAttitude(), attitudeIs(expected));
        // create new frame for testing frame transforms
        Rotation rotation = new Rotation(
                Vector3D.PLUS_K,
                FastMath.PI / 2.0,
                RotationConvention.FRAME_TRANSFORM);
        Transform angular = new Transform(
                t0,
                rotation,
                new Vector3D(1, 2, 3),
                new Vector3D(-4, 5, 6));
        Transform translation = new Transform(
                t0,
                new Vector3D(-1, 2, -3),
                new Vector3D(4, -5, 6),
                new Vector3D(7, 8, -9));
        Frame other = new Frame(eci, new Transform(t0, angular, translation), "other");
        actual = law.getAttitude(orbit.shiftedBy(1e3), date.shiftedBy(1e3), other);
        MatcherAssert.assertThat(actual.getReferenceFrame(), is(other));
        MatcherAssert.assertThat(actual.toAttitude(), attitudeIs(expected));
        // check not identity rotation
        MatcherAssert.assertThat(actual.getRotation().toRotation(),
                not(distanceIs(Rotation.IDENTITY, closeTo(0.0, 1e-1))));
    }



    @Before
    public void setUp() {
        try {
            Utils.setDataRoot("regular-data");

            t0 = new AbsoluteDate(new DateComponents(2008, 06, 03), TimeComponents.H12,
                                  TimeScalesFactory.getUTC());
            orbit0 =
                new KeplerianOrbit(12345678.9, 0.001, 2.3, 0.1, 3.04, 2.4,
                                   PositionAngle.TRUE, FramesFactory.getEME2000(),
                                   t0, 3.986004415e14);
        } catch (OrekitException oe) {
            Assert.fail(oe.getMessage());
        }
    }

    @After
    public void tearDown() {
        t0     = null;
        orbit0 = null;
    }

}

