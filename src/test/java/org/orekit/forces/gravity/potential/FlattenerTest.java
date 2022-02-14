/* Copyright 2002-2022 CS GROUP
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
package org.orekit.forces.gravity.potential;


import org.hipparchus.util.FastMath;
import org.junit.Assert;
import org.junit.Test;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;

public class FlattenerTest {

    @Test
    public void testFullTriangle() {
        final Flattener full = new Flattener(5, 5);
        Assert.assertEquals(5, full.getDegree());
        Assert.assertEquals(5, full.getOrder());
        Assert.assertEquals(21, full.arraySize());
        Assert.assertEquals( 0, full.index(5, 5));
        Assert.assertEquals( 1, full.index(4, 4));
        Assert.assertEquals( 2, full.index(5, 4));
        Assert.assertEquals( 3, full.index(3, 3));
        Assert.assertEquals( 4, full.index(4, 3));
        Assert.assertEquals( 5, full.index(5, 3));
        Assert.assertEquals( 6, full.index(2, 2));
        Assert.assertEquals( 7, full.index(3, 2));
        Assert.assertEquals( 8, full.index(4, 2));
        Assert.assertEquals( 9, full.index(5, 2));
        Assert.assertEquals(10, full.index(1, 1));
        Assert.assertEquals(11, full.index(2, 1));
        Assert.assertEquals(12, full.index(3, 1));
        Assert.assertEquals(13, full.index(4, 1));
        Assert.assertEquals(14, full.index(5, 1));
        Assert.assertEquals(15, full.index(0, 0));
        Assert.assertEquals(16, full.index(1, 0));
        Assert.assertEquals(17, full.index(2, 0));
        Assert.assertEquals(18, full.index(3, 0));
        Assert.assertEquals(19, full.index(4, 0));
        Assert.assertEquals(20, full.index(5, 0));
    }

    @Test
    public void testTruncatedTriangle() {
        final Flattener truncated = new Flattener(5, 3);
        Assert.assertEquals(5, truncated.getDegree());
        Assert.assertEquals(3, truncated.getOrder());
        Assert.assertEquals(18, truncated.arraySize());
        Assert.assertEquals( 0, truncated.index(3, 3));
        Assert.assertEquals( 1, truncated.index(4, 3));
        Assert.assertEquals( 2, truncated.index(5, 3));
        Assert.assertEquals( 3, truncated.index(2, 2));
        Assert.assertEquals( 4, truncated.index(3, 2));
        Assert.assertEquals( 5, truncated.index(4, 2));
        Assert.assertEquals( 6, truncated.index(5, 2));
        Assert.assertEquals( 7, truncated.index(1, 1));
        Assert.assertEquals( 8, truncated.index(2, 1));
        Assert.assertEquals( 9, truncated.index(3, 1));
        Assert.assertEquals(10, truncated.index(4, 1));
        Assert.assertEquals(11, truncated.index(5, 1));
        Assert.assertEquals(12, truncated.index(0, 0));
        Assert.assertEquals(13, truncated.index(1, 0));
        Assert.assertEquals(14, truncated.index(2, 0));
        Assert.assertEquals(15, truncated.index(3, 0));
        Assert.assertEquals(16, truncated.index(4, 0));
        Assert.assertEquals(17, truncated.index(5, 0));
    }

    @Test
    public void testFlatten() {
        final Flattener flattener = new Flattener(5, 3);
        double[][] triangular = {
            { 12 },
            { 13,  7 },
            { 14,  8, 3 },
            { 15,  9, 4, 0 },
            { 16, 10, 5, 1 },
            { 17, 11, 6, 2 }
        };
        final double[] flat = flattener.flatten(triangular);
        for (int i = 0; i < flat.length; ++i) {
            Assert.assertEquals(i, flat[i], 1.0e-15);
        }
    }

    @Test
    public void testSize() {
        for (int degree = 0; degree <= 60; ++degree) {
            for (int order = 0; order <= degree; ++order) {
                Assert.assertEquals(loop(degree, order, degree, 0) + 1,
                                    new Flattener(degree, order).arraySize());
            }
        }
    }

    @Test
    public void testIndices() {
        for (int degree = 0; degree <= 60; ++degree) {
            for (int order = 0; order <= degree; ++order) {
                final Flattener flattener = new Flattener(degree, order);
                for (int n = 0; n <= degree; ++n) {
                    for (int m = 0; m <= FastMath.min(n, order); ++m) {
                        Assert.assertEquals(loop(degree, order, n, m),
                                            flattener.index(n, m));
                    }
                }
            }
        }
    }

    @Test
    public void testLimits() {
        for (int degree = 0; degree <= 20; ++degree) {
            for (int order = 0; order <= degree; ++order) {
                final Flattener flattener = new Flattener(degree, order);
                Assert.assertEquals(degree, flattener.getDegree());
                Assert.assertEquals(order, flattener.getOrder());
                for (int n = -2; n < degree + 2; ++n) {
                    for (int m = -2; m <= FastMath.min(n, order) + 2; ++m) {
                        if (n < 0 || n > degree || m < 0 || m > FastMath.min(n, order)) {
                            try {
                                flattener.index(n, m);
                                Assert.fail("an exception should have been thrown");
                            } catch (OrekitException oe) {
                                Assert.assertEquals(OrekitMessages.WRONG_DEGREE_OR_ORDER, oe.getSpecifier());
                                Assert.assertEquals(n,      ((Integer) oe.getParts()[0]).intValue());
                                Assert.assertEquals(m,      ((Integer) oe.getParts()[1]).intValue());
                                Assert.assertEquals(degree, ((Integer) oe.getParts()[2]).intValue());
                                Assert.assertEquals(order,  ((Integer) oe.getParts()[3]).intValue());
                                Assert.assertFalse(flattener.withinRange(n, m));
                            }
                        } else {
                            Assert.assertTrue(flattener.index(n, m) >= 0);
                            Assert.assertTrue(flattener.withinRange(n, m));
                        }
                    }
                }
            }
        }
    }

    private int loop(final int degree, final int order, final int n, final int m) {
        int count = 0;
        for (int i = order; i > m; --i) {
            for (int j = i; j <= degree; ++j) {
                ++count;
            }
        }
        for (int j = m; j <= n; ++j) {
            ++count;
        }
        return count - 1;
    }

}
