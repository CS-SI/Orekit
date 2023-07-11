/* Copyright 2002-2023 CS GROUP
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
package org.orekit.files.general;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.OrekitMatchers;
import org.orekit.Utils;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.general.EphemerisFile.EphemerisSegment;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.propagation.BoundedPropagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.CartesianDerivativesFilter;
import org.orekit.utils.Constants;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;

/**
 * Unit tests for {@link EphemerisSegmentPropagator}.
 *
 * @author Evan Ward
 */
public class EphemerisSegmentPropagatorTest {

    /** Set Orekit data. */
    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }


    /**
     * Check {@link EphemerisSegmentPropagator} and {@link EphemerisSegment#getPropagator()}.
     *
     * @throws Exception on error.
     */
    @Test
    public void testPropagator() throws Exception {
        // setup
        AbsoluteDate start = AbsoluteDate.J2000_EPOCH, end = start.shiftedBy(60);
        Frame frame = FramesFactory.getEME2000();
        List<TimeStampedPVCoordinates> coordinates = Arrays.asList(
                new TimeStampedPVCoordinates(start, new Vector3D(6778137, 0, 0), new Vector3D(0, 7.5e3, 0)),
                new TimeStampedPVCoordinates(start.shiftedBy(30), new Vector3D(6778137 + 1, 0, 0), new Vector3D(0, 7.5e3, 0)),
                new TimeStampedPVCoordinates(end, new Vector3D(6778137 + 3, 0, 0), new Vector3D(0, 7.5e3, 0)));
        EphemerisSegment<TimeStampedPVCoordinates> ephemeris = new EphemerisSegment<TimeStampedPVCoordinates>() {
            @Override
            public double getMu() {
                return Constants.EGM96_EARTH_MU;
            }

            @Override
            public Frame getFrame() {
                return frame;
            }

            @Override
            public int getInterpolationSamples() {
                return 2;
            }

            @Override
            public CartesianDerivativesFilter getAvailableDerivatives() {
                return CartesianDerivativesFilter.USE_P;
            }

            @Override
            public List<TimeStampedPVCoordinates> getCoordinates() {
                return coordinates;
            }

            @Override
            public AbsoluteDate getStart() {
                return start;
            }

            @Override
            public AbsoluteDate getStop() {
                return end;
            }
        };

        // action
        BoundedPropagator propagator = ephemeris.getPropagator();

        //verify
        MatcherAssert.assertThat(propagator.getMinDate(), CoreMatchers.is(start));
        MatcherAssert.assertThat(propagator.getMaxDate(), CoreMatchers.is(end));
        MatcherAssert.assertThat(propagator.getFrame(), CoreMatchers.is(frame));
        int ulps = 0;
        PVCoordinates expected = new PVCoordinates(
                new Vector3D(6778137, 0, 0),
                new Vector3D(1.0 / 30, 0, 0));
        MatcherAssert.assertThat(
                propagator.propagate(start).getPVCoordinates(),
                OrekitMatchers.pvCloseTo(expected, ulps));
        MatcherAssert.assertThat(
                propagator.getPVCoordinates(start, frame),
                OrekitMatchers.pvCloseTo(expected, ulps));
        expected = new PVCoordinates(
                new Vector3D(6778137 + 2, 0, 0),
                new Vector3D(2 / 30.0, 0, 0));
        MatcherAssert.assertThat(
                propagator.propagate(start.shiftedBy(45)).getPVCoordinates(),
                OrekitMatchers.pvCloseTo(expected, ulps));
        MatcherAssert.assertThat(
                propagator.getPVCoordinates(start.shiftedBy(45), frame),
                OrekitMatchers.pvCloseTo(expected, ulps));
        expected = new PVCoordinates(
                new Vector3D(6778137 + 3, 0, 0),
                new Vector3D(2 / 30.0, 0, 0));
        MatcherAssert.assertThat(
                propagator.propagate(end).getPVCoordinates(),
                OrekitMatchers.pvCloseTo(expected, ulps));
        MatcherAssert.assertThat(
                propagator.getPVCoordinates(end, frame),
                OrekitMatchers.pvCloseTo(expected, ulps));
        // check reset state is prohibited
        SpacecraftState ic = propagator.propagate(start);
        try {
            propagator.resetInitialState(ic);
            Assertions.fail("Expected Exception");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.NON_RESETABLE_STATE, oe.getSpecifier());
        }
        try {
            Method reset = EphemerisSegmentPropagator.class.getDeclaredMethod("resetIntermediateState",
                                                                              SpacecraftState.class,
                                                                              Boolean.TYPE);
            reset.setAccessible(true);
            reset.invoke(propagator, ic, true);
            Assertions.fail("Expected Exception");
        } catch (InvocationTargetException ite) {
            OrekitException oe = (OrekitException) ite.getCause();
            Assertions.assertEquals(OrekitMessages.NON_RESETABLE_STATE, oe.getSpecifier());
        }

    }

}
