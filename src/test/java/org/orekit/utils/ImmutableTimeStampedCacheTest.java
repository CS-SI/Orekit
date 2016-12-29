/* Contributed in the public domain.
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
package org.orekit.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.errors.TimeStampedCacheException;
import org.orekit.time.AbsoluteDate;

/**
 * Unit tests for {@link ImmutableTimeStampedCache}.
 *
 * @author Evan Ward
 */
public class ImmutableTimeStampedCacheTest {

    /**
     * arbitrary date
     */
    private static final AbsoluteDate date = AbsoluteDate.CCSDS_EPOCH;

    /**
     * set Orekit data for useful debugging messages from dates.
     */
    @BeforeClass
    public static void setUpBefore() {
        Utils.setDataRoot("regular-data");
    }

    /**
     * data provided to {@link #cache}
     */
    private List<AbsoluteDate> data;

    /**
     * subject under test
     */
    private ImmutableTimeStampedCache<AbsoluteDate> cache;

    /**
     * create {@link #cache} and {@link #data} with neighborsSize = 3
     */
    @Before
    public void setUp() {
        data = Arrays.asList(date, date.shiftedBy(1), date.shiftedBy(2),
                             date.shiftedBy(3), date.shiftedBy(4),
                             date.shiftedBy(5));
        cache = new ImmutableTimeStampedCache<AbsoluteDate>(3, data);
    }

    /**
     * check
     * {@link ImmutableTimeStampedCache#ImmutableTimeStampedCache(int, java.util.Collection)}
     */
    @Test
    public void testImmutableTimeStampedCache() {
        // exception for neighborsSize > data.size()
        try {
            new ImmutableTimeStampedCache<AbsoluteDate>(data.size() + 1, data);
            Assert.fail("Expected Exception");
        } catch (IllegalArgumentException e) {
            // expected
        }

        // exception for non-positive neighborsSize
        try {
            new ImmutableTimeStampedCache<AbsoluteDate>(0, data);
            Assert.fail("Expected Exception");
        } catch (IllegalArgumentException e) {
            // expected
        }

        // exception for null data
        try {
            new ImmutableTimeStampedCache<AbsoluteDate>(1, null);
            Assert.fail("Expected Exception");
        } catch (NullPointerException e) {
            // expected
        }

        // exception for zero data
        try {
            new ImmutableTimeStampedCache<AbsoluteDate>(
                                                        1,
                                                        Collections
                                                            .<AbsoluteDate> emptyList());
            Assert.fail("Expected Exception");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    /**
     * check {@link ImmutableTimeStampedCache#getNeighbors(AbsoluteDate)} at a
     * series of different dates designed to test all logic paths.
     *
     * @throws TimeStampedCacheException
     */
    @Test
    public void testGetNeighbors()
        throws TimeStampedCacheException {
        // setup
        int size = data.size();

        // actions + verify

        // before fist data
        try {
            cache.getNeighbors(data.get(0).shiftedBy(-1));
            Assert.fail("Expected Exception");
        } catch (TimeStampedCacheException e) {
            // expected
        }

        // on fist date
        Assert.assertArrayEquals(cache.getNeighbors(data.get(0)).toArray(), data
            .subList(0, 3).toArray());
        // between fist and second date
        Assert.assertArrayEquals(cache.getNeighbors(data.get(0).shiftedBy(0.5))
            .toArray(), data.subList(0, 3).toArray());
        // in the middle on a date
        Assert.assertArrayEquals(cache.getNeighbors(data.get(2)).toArray(), data
            .subList(1, 4).toArray());
        // in the middle between dates
        Assert.assertArrayEquals(cache.getNeighbors(data.get(2).shiftedBy(0.5))
            .toArray(), data.subList(1, 4).toArray());
        // just before last date
        Assert.assertArrayEquals(cache
            .getNeighbors(data.get(size - 1).shiftedBy(-0.5)).toArray(), data
            .subList(size - 3, size).toArray());
        // on last date
        Assert.assertArrayEquals(cache.getNeighbors(data.get(size - 1)).toArray(),
                          data.subList(size - 3, size).toArray());

        // after last date
        try {
            cache.getNeighbors(data.get(size - 1).shiftedBy(1));
            Assert.fail("Expected Exception");
        } catch (TimeStampedCacheException e) {
            // expected
        }
    }

    /**
     * check {@link ImmutableTimeStampedCache#getNeighborsSize()}
     */
    @Test
    public void testGetNeighborsSize() {
        Assert.assertEquals(cache.getNeighborsSize(), 3);
    }

    /**
     * check {@link ImmutableTimeStampedCache#getEarliest()}
     */
    @Test
    public void testGetEarliest() {
        Assert.assertEquals(cache.getEarliest(), data.get(0));
    }

    /**
     * check {@link ImmutableTimeStampedCache#getLatest()}
     */
    @Test
    public void testGetLatest() {
        Assert.assertEquals(cache.getLatest(), data.get(data.size() - 1));
    }

    /**
     * check {@link ImmutableTimeStampedCache#getAll()}
     */
    @Test
    public void testGetAll() {
        Assert.assertArrayEquals(cache.getAll().toArray(), data.toArray());
    }

    /**
     * check that the cache is immutable by changing the data passed in the
     * constructor and returned from methods.
     *
     * @throws TimeStampedCacheException
     */
    @Test
    public void testImmutable()
        throws TimeStampedCacheException {
        // setup
        List<AbsoluteDate> actuals;
        List<AbsoluteDate> expecteds = new ArrayList<AbsoluteDate>(data);
        AbsoluteDate different = date.shiftedBy(-50);

        // actions + verify

        // check constructor
        data.set(0, different);
        Assert.assertArrayEquals(cache.getAll().toArray(), expecteds.toArray());

        // check getAll
        actuals = cache.getAll();
        try {
            actuals.set(0, different);
        } catch (UnsupportedOperationException e) {
            // exception ok
        }
        Assert.assertArrayEquals(cache.getAll().toArray(), expecteds.toArray());

        // check getNeighbors
        actuals = cache.getNeighbors(date);
        try {
            actuals.set(0, different);
        } catch (UnsupportedOperationException e) {
            // exception ok
        }
        Assert.assertArrayEquals(cache.getAll().toArray(), expecteds.toArray());
    }

    /**
     * check {@link ImmutableTimeStampedCache#emptyCache()}.
     */
    @Test
    public void testEmptyCache() {
        // setup
        cache = ImmutableTimeStampedCache.emptyCache();

        // actions + verify
        try {
            cache.getNeighbors(date);
            Assert.fail("Expected Exception");
        } catch (TimeStampedCacheException e) {
            // expected
        }
        try {
            cache.getEarliest();
            Assert.fail("Expected Exception");
        } catch (IllegalStateException e) {
            // expected
        }
        try {
            cache.getLatest();
            Assert.fail("Expected Exception");
        } catch (IllegalStateException e) {
            // expected
        }
        Assert.assertEquals(cache.getAll().size(), 0);
        Assert.assertEquals(cache.getNeighborsSize(), 0);
    }
}
