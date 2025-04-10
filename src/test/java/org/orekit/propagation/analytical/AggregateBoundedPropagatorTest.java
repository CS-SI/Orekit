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
package org.orekit.propagation.analytical;

import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.orekit.OrekitMatchers;
import org.orekit.Utils;
import org.orekit.attitudes.FrameAlignedProvider;
import org.orekit.attitudes.LofOffset;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.LOFType;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.BoundedPropagator;
import org.orekit.propagation.EphemerisGenerator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.TimeSpanMap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;

/**
 * Tests for {@link AggregateBoundedPropagator}.
 *
 * @author Evan Ward
 */
public class AggregateBoundedPropagatorTest {

    public static final Frame frame = FramesFactory.getGCRF();

    /** Set Orekit data. */
    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

    /**
     * Check {@link AbstractAnalyticalPropagator#propagateOrbit(AbsoluteDate)} when the
     * constituent propagators are exactly adjacent.
     */
    @Test
    public void testAdjacent() {
        // setup
        AbsoluteDate date = AbsoluteDate.CCSDS_EPOCH;
        BoundedPropagator p1 = createPropagator(date, date.shiftedBy(10), 0);
        BoundedPropagator p2 = createPropagator(date.shiftedBy(10), date.shiftedBy(20), 1);

        // action
        AggregateBoundedPropagator actual = new AggregateBoundedPropagator(Arrays.asList(p1, p2));

        //verify
        int ulps = 0;
        MatcherAssert.assertThat(actual.getFrame(), CoreMatchers.is(p1.getFrame()));
        MatcherAssert.assertThat(actual.getMinDate(), CoreMatchers.is(date));
        MatcherAssert.assertThat(actual.getMaxDate(), CoreMatchers.is(date.shiftedBy(20)));
        MatcherAssert.assertThat(
                actual.propagate(date).getPVCoordinates(),
                OrekitMatchers.pvCloseTo(p1.propagate(date).getPVCoordinates(), ulps));
        MatcherAssert.assertThat(
                actual.propagate(date.shiftedBy(5)).getPVCoordinates(),
                OrekitMatchers.pvCloseTo(p1.propagate(date.shiftedBy(5)).getPVCoordinates(), ulps));
        MatcherAssert.assertThat(
                actual.propagate(date.shiftedBy(10)).getPVCoordinates(),
                OrekitMatchers.pvCloseTo(p2.propagate(date.shiftedBy(10)).getPVCoordinates(), ulps));
        MatcherAssert.assertThat(
                actual.propagate(date.shiftedBy(15)).getPVCoordinates(),
                OrekitMatchers.pvCloseTo(p2.propagate(date.shiftedBy(15)).getPVCoordinates(), ulps));
        MatcherAssert.assertThat(
                actual.propagate(date.shiftedBy(20)).getPVCoordinates(),
                OrekitMatchers.pvCloseTo(p2.propagate(date.shiftedBy(20)).getPVCoordinates(), ulps));

        for (TimeSpanMap.Span<BoundedPropagator> span = actual.getPropagatorsMap().getFirstNonNullSpan();
             span != null;
             span = span.next()) {
            Assertions.assertEquals(span.getStart(), span.getData().getMinDate());
        }

    }

    /**
     * Check {@link AbstractAnalyticalPropagator#propagateOrbit(AbsoluteDate)} when the
     * constituent propagators overlap.
     */
    @Test
    public void testOverlap() {
        // setup
        AbsoluteDate date = AbsoluteDate.CCSDS_EPOCH;
        BoundedPropagator p1 = createPropagator(date, date.shiftedBy(25), 0);
        BoundedPropagator p2 = createPropagator(date.shiftedBy(10), date.shiftedBy(20), 1);

        // action
        BoundedPropagator actual = new AggregateBoundedPropagator(Arrays.asList(p1, p2));

        //verify
        int ulps = 0;
        MatcherAssert.assertThat(actual.getFrame(), CoreMatchers.is(p1.getFrame()));
        MatcherAssert.assertThat(actual.getMinDate(), CoreMatchers.is(date));
        MatcherAssert.assertThat(actual.getMaxDate(), CoreMatchers.is(date.shiftedBy(20)));
        MatcherAssert.assertThat(
                actual.propagate(date).getPVCoordinates(),
                OrekitMatchers.pvCloseTo(p1.propagate(date).getPVCoordinates(), ulps));
        MatcherAssert.assertThat(
                actual.propagate(date.shiftedBy(5)).getPVCoordinates(),
                OrekitMatchers.pvCloseTo(p1.propagate(date.shiftedBy(5)).getPVCoordinates(), ulps));
        MatcherAssert.assertThat(
                actual.propagate(date.shiftedBy(10)).getPVCoordinates(),
                OrekitMatchers.pvCloseTo(p2.propagate(date.shiftedBy(10)).getPVCoordinates(), ulps));
        MatcherAssert.assertThat(
                actual.propagate(date.shiftedBy(15)).getPVCoordinates(),
                OrekitMatchers.pvCloseTo(p2.propagate(date.shiftedBy(15)).getPVCoordinates(), ulps));
        MatcherAssert.assertThat(
                actual.propagate(date.shiftedBy(20)).getPVCoordinates(),
                OrekitMatchers.pvCloseTo(p2.propagate(date.shiftedBy(20)).getPVCoordinates(), ulps));
    }

    /**
     * Check {@link AbstractAnalyticalPropagator#propagateOrbit(AbsoluteDate)} with a gap
     * between the constituent propagators.
     */
    @Test
    public void testGap() {
        // setup
        AbsoluteDate date = AbsoluteDate.CCSDS_EPOCH;
        BoundedPropagator p1 = createPropagator(date, date.shiftedBy(1), 0);
        BoundedPropagator p2 = createPropagator(date.shiftedBy(10), date.shiftedBy(20), 1);

        // action
        BoundedPropagator actual = new AggregateBoundedPropagator(Arrays.asList(p1, p2));

        //verify
        int ulps = 0;
        MatcherAssert.assertThat(actual.getFrame(), CoreMatchers.is(p1.getFrame()));
        MatcherAssert.assertThat(actual.getMinDate(), CoreMatchers.is(date));
        MatcherAssert.assertThat(actual.getMaxDate(), CoreMatchers.is(date.shiftedBy(20)));
        MatcherAssert.assertThat(
                actual.propagate(date).getPVCoordinates(),
                OrekitMatchers.pvCloseTo(p1.propagate(date).getPVCoordinates(), ulps));
        MatcherAssert.assertThat(
                actual.propagate(date.shiftedBy(10)).getPVCoordinates(),
                OrekitMatchers.pvCloseTo(p2.propagate(date.shiftedBy(10)).getPVCoordinates(), ulps));
        MatcherAssert.assertThat(
                actual.propagate(date.shiftedBy(15)).getPVCoordinates(),
                OrekitMatchers.pvCloseTo(p2.propagate(date.shiftedBy(15)).getPVCoordinates(), ulps));
        MatcherAssert.assertThat(
                actual.propagate(date.shiftedBy(20)).getPVCoordinates(),
                OrekitMatchers.pvCloseTo(p2.propagate(date.shiftedBy(20)).getPVCoordinates(), ulps));
        try {
            // may or may not throw an exception depending on the type of propagator.
            MatcherAssert.assertThat(
                    actual.propagate(date.shiftedBy(5)).getPVCoordinates(),
                    OrekitMatchers.pvCloseTo(p1.propagate(date.shiftedBy(5)).getPVCoordinates(), ulps));
        } catch (OrekitException e) {
            // expected
        }
    }

    @Test
    public void testOutsideBounds() {
        // setup
        AbsoluteDate date = AbsoluteDate.CCSDS_EPOCH;
        BoundedPropagator p1 = createPropagator(date, date.shiftedBy(10), 0);
        BoundedPropagator p2 = createPropagator(date.shiftedBy(10), date.shiftedBy(20), 1);

        // action
        BoundedPropagator actual = new AggregateBoundedPropagator(Arrays.asList(p1, p2));

        // verify
        int ulps = 0;
        // before bound of first propagator
        try {
            // may or may not throw an exception depending on the type of propagator.
            MatcherAssert.assertThat(
                    actual.propagate(date.shiftedBy(-60)).getPVCoordinates(),
                    OrekitMatchers.pvCloseTo(p1.propagate(date.shiftedBy(-60)).getPVCoordinates(), ulps));
        } catch (OrekitException e) {
            // expected
        }
        try {
            // may or may not throw an exception depending on the type of propagator.
            MatcherAssert.assertThat(
                    actual.getPVCoordinates(date.shiftedBy(-60), frame),
                    OrekitMatchers.pvCloseTo(p1.propagate(date.shiftedBy(-60)).getPVCoordinates(), ulps));
        } catch (OrekitException e) {
            // expected
        }
        // after bound of last propagator
        try {
            // may or may not throw an exception depending on the type of propagator.
            MatcherAssert.assertThat(
                    actual.propagate(date.shiftedBy(60)).getPVCoordinates(),
                    OrekitMatchers.pvCloseTo(p2.propagate(date.shiftedBy(60)).getPVCoordinates(), ulps));
        } catch (OrekitException e) {
            // expected
        }
        try {
            // may or may not throw an exception depending on the type of propagator.
            MatcherAssert.assertThat(
                    actual.getPVCoordinates(date.shiftedBy(60), frame),
                    OrekitMatchers.pvCloseTo(p2.propagate(date.shiftedBy(60)).getPVCoordinates(), ulps));
        } catch (OrekitException e) {
            // expected
        }

    }

    /**
     * Check that resetting the state is prohibited.
     */
    @Test
    public void testResetState() {
        // setup
        AbsoluteDate date = AbsoluteDate.CCSDS_EPOCH;
        BoundedPropagator p1 = createPropagator(date, date.shiftedBy(10), 0);
        BoundedPropagator p2 = createPropagator(date.shiftedBy(10), date.shiftedBy(20), 1);
        SpacecraftState ic = p2.getInitialState();

        // action
        BoundedPropagator actual = new AggregateBoundedPropagator(Arrays.asList(p1, p2));

        // verify
        try {
            actual.resetInitialState(ic);
            Assertions.fail("Expected Exception");
        } catch (OrekitException e) {
            // expected
        }
    }

    /**
     * Check that creating an aggregate propagator from an empty list of propagators is
     * prohibited.
     */
    @Test
    public void testEmptyList() {
        // action + verify
        try {
            new AggregateBoundedPropagator(Collections.emptyList());
            Assertions.fail("Expected Exception");
        } catch (OrekitException e) {
            // expected
        }
    }

    /**
     * Check
     * {@link
     * AggregateBoundedPropagator#AggregateBoundedPropagator(NavigableMap,
     * AbsoluteDate, AbsoluteDate)}.
     */
    @Test
    public void testAggregateBoundedPropagator() {
        // setup
        NavigableMap<AbsoluteDate, BoundedPropagator> map = new TreeMap<>();
        AbsoluteDate date = AbsoluteDate.ARBITRARY_EPOCH;
        AbsoluteDate end = date.shiftedBy(20);
        BoundedPropagator p1 = createPropagator(date, end, 0);
        BoundedPropagator p2 = createPropagator(date.shiftedBy(10), end, 0);
        map.put(date, p1);
        map.put(date.shiftedBy(10), p2);
        AbsoluteDate min = date.shiftedBy(-10);
        AbsoluteDate max = end.shiftedBy(10);

        // action
        final BoundedPropagator actual =
                new AggregateBoundedPropagator(map, min, max);

        // verify
        MatcherAssert.assertThat(actual.getMinDate(), CoreMatchers.is(min));
        MatcherAssert.assertThat(actual.getMaxDate(), CoreMatchers.is(max));
        MatcherAssert.assertThat(actual.propagate(date).getPVCoordinates(),
                OrekitMatchers.pvCloseTo(p1.propagate(date).getPVCoordinates(), 0));
        MatcherAssert.assertThat(actual.propagate(end).getPVCoordinates(),
                OrekitMatchers.pvCloseTo(p2.propagate(end).getPVCoordinates(), 0));
        MatcherAssert.assertThat(actual.propagate(min).getPVCoordinates(),
                OrekitMatchers.pvCloseTo(p1.propagate(min).getPVCoordinates(), 0));
    }

    /**
     * Create a propagator with the given dates.
     *
     * @param start date.
     * @param end   date.
     * @param v     true anomaly.
     * @return a bound propagator with the given dates.
     */
    private BoundedPropagator createPropagator(AbsoluteDate start,
                                               AbsoluteDate end,
                                               double v) {
        double gm = Constants.EGM96_EARTH_MU;
        KeplerianPropagator propagator = new KeplerianPropagator(new KeplerianOrbit(
                6778137, 0, 0, 0, 0, v, PositionAngleType.TRUE, frame, start, gm));
        propagator.setAttitudeProvider(new LofOffset(frame, LOFType.LVLH_CCSDS));
        final EphemerisGenerator generator = propagator.getEphemerisGenerator();
        propagator.propagate(start, end);
        return generator.getGeneratedEphemeris();
    }

    @Test
    void testAttitude() {
        AbsoluteDate date = AbsoluteDate.ARBITRARY_EPOCH;
        AbsoluteDate end = date.shiftedBy(20);
        BoundedPropagator p1 = createPropagator(date, end, 0);
        BoundedPropagator p2 = createPropagator(date.shiftedBy(10), end, 0);
        final BoundedPropagator actual = new AggregateBoundedPropagator(Arrays.asList(p1, p2));

        // using the attitude providers from the underlying propagator
        final SpacecraftState s0 = actual.propagate(date.shiftedBy(15));
        Assertions.assertEquals(0,
                                Vector3D.angle(s0.getPosition(),
                                               s0.getAttitude().getRotation().applyInverseTo(Vector3D.MINUS_K)),
                                3.0e-16);

        // overriding explicitly the global attitude provider
        actual.setAttitudeProvider(new FrameAlignedProvider(p1.getInitialState().getFrame()));
        final SpacecraftState s1 = actual.propagate(date.shiftedBy(15));
        Assertions.assertEquals(0,
                                Vector3D.angle(Vector3D.MINUS_K,
                                               s1.getAttitude().getRotation().applyInverseTo(Vector3D.MINUS_K)),
                                3.0e-16);
        Assertions.assertEquals(1.570796,
                                Vector3D.angle(s1.getPosition(),
                                               s1.getAttitude().getRotation().applyInverseTo(Vector3D.MINUS_K)),
                                1.0e-6);

    }

    @Test
    void testPropagateOrbit() {
        // GIVEN
        final AbsoluteDate date = AbsoluteDate.ARBITRARY_EPOCH;
        final SpacecraftState mockedState = Mockito.mock(SpacecraftState.class);
        final Orbit expectedOrbit = Mockito.mock(Orbit.class);
        Mockito.when(mockedState.getOrbit()).thenReturn(expectedOrbit);
        final BoundedPropagator mockedBoundedPropagator = mockBoundedPropagator(date, mockedState);
        final List<BoundedPropagator> boundedPropagatorList = new ArrayList<>();
        boundedPropagatorList.add(mockedBoundedPropagator);
        final AggregateBoundedPropagator propagator = new AggregateBoundedPropagator(boundedPropagatorList);

        // WHEN
        final Orbit actualOrbit = propagator.propagateOrbit(date);

        // THEN
        Assertions.assertEquals(expectedOrbit, actualOrbit);
    }

    @Test
    void testGetPosition() {
        // GIVEN
        final AbsoluteDate date = AbsoluteDate.ARBITRARY_EPOCH;
        final Frame mockedFrame = Mockito.mock(Frame.class);
        final SpacecraftState mockedState = Mockito.mock(SpacecraftState.class);
        final Vector3D expectedPosition = new Vector3D(1, 2, 3);
        Mockito.when(mockedState.getPosition(mockedFrame)).thenReturn(expectedPosition);
        final BoundedPropagator mockedBoundedPropagator = mockBoundedPropagator(date, mockedState);
        final List<BoundedPropagator> boundedPropagatorList = new ArrayList<>();
        boundedPropagatorList.add(mockedBoundedPropagator);
        final AggregateBoundedPropagator propagator = new AggregateBoundedPropagator(boundedPropagatorList);

        // WHEN
        final Vector3D actualPosition = propagator.getPosition(date, mockedFrame);

        // THEN
        Assertions.assertEquals(expectedPosition, actualPosition);
    }

    private BoundedPropagator mockBoundedPropagator(final AbsoluteDate date, final SpacecraftState state) {
        final BoundedPropagator mockedBoundedPropagator = Mockito.mock(BoundedPropagator.class);
        Mockito.when(mockedBoundedPropagator.getMinDate()).thenReturn(AbsoluteDate.PAST_INFINITY);
        Mockito.when(mockedBoundedPropagator.getMinDate()).thenReturn(AbsoluteDate.FUTURE_INFINITY);
        Mockito.when(mockedBoundedPropagator.propagate(date)).thenReturn(state);
        Mockito.when(mockedBoundedPropagator.getInitialState()).thenReturn(state);
        return mockedBoundedPropagator;
    }

}
