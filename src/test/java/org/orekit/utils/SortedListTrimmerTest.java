/* Contributed in the public domain.
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
package org.orekit.utils;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.errors.TimeStampedCacheException;
import org.orekit.time.AbsoluteDate;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import java.util.Collections;
import java.util.List;

/**
 * Unit tests for {@link SortedListTrimmer}.
 *
 * @author Evan Ward
 */
class SortedListTrimmerTest {

    /**
     * arbitrary date
     */
    private static final AbsoluteDate date = AbsoluteDate.CCSDS_EPOCH;

    /**
     * set Orekit data for useful debugging messages from dates.
     */
    @BeforeAll
    static void setUpBefore() {
        Utils.setDataRoot("regular-data");
    }

    /**
     * data provided to {@link #trimmer}
     */
    private List<AbsoluteDate> data;

    /**
     * subject under test
     */
    private SortedListTrimmer trimmer;

    /**
     * create {@link #trimmer} and {@link #data} with neighborsSize = 3
     */
    @BeforeEach
    void setUp() {
        data = Arrays.asList(date, date.shiftedBy(1), date.shiftedBy(2),
                             date.shiftedBy(3), date.shiftedBy(4),
                             date.shiftedBy(5));
        trimmer = new SortedListTrimmer(3);
    }

    /**
     * check
     * {@link SortedListTrimmer#getNeighborsSubList(AbsoluteDate, List)}
     */
    @Test
    void testGetNeighborsSubList() {
        // exception for neighborsSize > data.size()
        try {
            new SortedListTrimmer(data.size() + 1).getNeighborsSubList(date, data);
            fail("Expected Exception");
        } catch (OrekitException e) {
            assertEquals(OrekitMessages.NOT_ENOUGH_DATA, e.getSpecifier());
        }

        // exception for non-positive neighborsSize
        try {
            new SortedListTrimmer(0);
            fail("Expected Exception");
        } catch (IllegalArgumentException e) {
            // expected
        }

        // exception for null data
        try {
            new SortedListTrimmer(1).getNeighborsSubList(date, null);
            fail("Expected Exception");
        } catch (NullPointerException e) {
            // expected
        }

        // exception for zero data
        try {
            new SortedListTrimmer(1).getNeighborsSubList(date, Collections.emptyList());
            fail("Expected Exception");
        } catch (OrekitException e) {
            assertEquals(OrekitMessages.NOT_ENOUGH_DATA, e.getSpecifier());
        }
    }

    /**
     * check {@link SortedListTrimmer#getNeighborsSubList(AbsoluteDate, List)} at a
     * series of different dates designed to test all logic paths.
     */
    @Test
    void testGetNeighbors() {
        // setup
        int size = data.size();

        // actions + verify

        // before fist data
        try {
            trimmer.getNeighborsSubList(data.get(0).shiftedBy(-1), data);
            fail("Expected Exception");
        } catch (TimeStampedCacheException e) {
            // expected
            assertEquals(OrekitMessages.UNABLE_TO_GENERATE_NEW_DATA_BEFORE, e.getSpecifier());
        }

        // on fist date
        assertArrayEquals(trimmer.getNeighborsSubList(data.get(0), data).toArray(),
                                     data.subList(0, 3).toArray());
        // between fist and second date
        assertArrayEquals(trimmer.getNeighborsSubList(data.get(0).shiftedBy(0.5), data).toArray(),
                                     data.subList(0, 3).toArray());
        // in the middle on a date
        assertArrayEquals(trimmer.getNeighborsSubList(data.get(2), data).toArray(),
                                     data.subList(1, 4).toArray());
        // in the middle between dates
        assertArrayEquals(trimmer.getNeighborsSubList(data.get(2).shiftedBy(0.5), data).toArray(),
                                     data.subList(1, 4).toArray());
        // just before last date
        assertArrayEquals(trimmer.getNeighborsSubList(data.get(size - 1).shiftedBy(-0.5), data).toArray(),
                                     data.subList(size - 3, size).toArray());
        // on last date
        assertArrayEquals(trimmer.getNeighborsSubList(data.get(size - 1), data).toArray(),
                                     data.subList(size - 3, size).toArray());

        // after last date
        AbsoluteDate central = data.get(size - 1).shiftedBy(1);
        try {
            trimmer.getNeighborsSubList(central, data);
            fail("Expected Exception");
        } catch (TimeStampedCacheException e) {
            // expected
           assertEquals(OrekitMessages.UNABLE_TO_GENERATE_NEW_DATA_AFTER, e.getSpecifier());
        }
    }

    /** Check findIndex(...) returns results on a half closed interval. */
    @Test
    void testGetNeighborsSingle() {
        // setup
        trimmer = new SortedListTrimmer(1);
        int size = data.size();

        // actions + verify
        // on fist date
        assertArrayEquals(
                trimmer.getNeighborsSubList(data.get(0), data).toArray(),
                data.subList(0, 1).toArray());
        // between fist and second date
        assertArrayEquals(
                trimmer.getNeighborsSubList(data.get(0).shiftedBy(0.5), data).toArray(),
                data.subList(0, 1).toArray());
        // in the middle on a date
        assertArrayEquals(
                trimmer.getNeighborsSubList(data.get(2), data).toArray(),
                data.subList(2, 3).toArray());
        // in the middle between dates
        assertArrayEquals(
                trimmer.getNeighborsSubList(data.get(2).shiftedBy(0.1), data).toArray(),
                data.subList(2, 3).toArray());
        // just before last date
        assertArrayEquals(
                trimmer.getNeighborsSubList(data.get(size - 1).shiftedBy(-0.1), data).toArray(),
                data.subList(size - 2, size - 1).toArray());
        // on last date
        assertArrayEquals(
                trimmer.getNeighborsSubList(data.get(size - 1), data).toArray(),
                data.subList(size - 1, size).toArray());
    }

    /**
     * check {@link ImmutableTimeStampedCache#getMaxNeighborsSize()}
     */
    @Test
    void testGetNeighborsSize() {
        assertEquals(3, trimmer.getNeighborsSize());
    }

    @Test
    void testNonLinear() {
        final List<AbsoluteDate> nonLinearCache = Arrays.asList(date.shiftedBy(10),
                                                                date.shiftedBy(14),
                                                                date.shiftedBy(18),
                                                                date.shiftedBy(23),
                                                                date.shiftedBy(30),
                                                                date.shiftedBy(36),
                                                                date.shiftedBy(45),
                                                                date.shiftedBy(55),
                                                                date.shiftedBy(67),
                                                                date.shiftedBy(90),
                                                                date.shiftedBy(118));
        for (double dt = 10; dt < 118; dt += 0.01) {
            checkNeighbors(new SortedListTrimmer(2), nonLinearCache, dt);
        }
    }

    private void checkNeighbors(final SortedListTrimmer nonLinearTrimmer,
                                final List<AbsoluteDate> nonLinearCache,
                                final double offset) {
        List<AbsoluteDate> s = nonLinearTrimmer.getNeighborsSubList(date.shiftedBy(offset), nonLinearCache);
        assertEquals(2, s.size());
        assertTrue(s.get(0).durationFrom(date) <= offset);
        assertTrue(s.get(1).durationFrom(date) >  offset);
    }

}
