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
package org.orekit.forces.gravity.potential;

import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;

public class FlattenerTest {

    @Test
    public void testFullTriangle() {
        final Flattener full = new Flattener(5, 5);
        Assertions.assertEquals(5, full.getDegree());
        Assertions.assertEquals(5, full.getOrder());
        Assertions.assertEquals(21, full.arraySize());
        Assertions.assertEquals( 0, full.index(5, 5));
        Assertions.assertEquals( 1, full.index(4, 4));
        Assertions.assertEquals( 2, full.index(5, 4));
        Assertions.assertEquals( 3, full.index(3, 3));
        Assertions.assertEquals( 4, full.index(4, 3));
        Assertions.assertEquals( 5, full.index(5, 3));
        Assertions.assertEquals( 6, full.index(2, 2));
        Assertions.assertEquals( 7, full.index(3, 2));
        Assertions.assertEquals( 8, full.index(4, 2));
        Assertions.assertEquals( 9, full.index(5, 2));
        Assertions.assertEquals(10, full.index(1, 1));
        Assertions.assertEquals(11, full.index(2, 1));
        Assertions.assertEquals(12, full.index(3, 1));
        Assertions.assertEquals(13, full.index(4, 1));
        Assertions.assertEquals(14, full.index(5, 1));
        Assertions.assertEquals(15, full.index(0, 0));
        Assertions.assertEquals(16, full.index(1, 0));
        Assertions.assertEquals(17, full.index(2, 0));
        Assertions.assertEquals(18, full.index(3, 0));
        Assertions.assertEquals(19, full.index(4, 0));
        Assertions.assertEquals(20, full.index(5, 0));
    }

    @Test
    public void testTruncatedTriangle() {
        final Flattener truncated = new Flattener(5, 3);
        Assertions.assertEquals(5, truncated.getDegree());
        Assertions.assertEquals(3, truncated.getOrder());
        Assertions.assertEquals(18, truncated.arraySize());
        Assertions.assertEquals( 0, truncated.index(3, 3));
        Assertions.assertEquals( 1, truncated.index(4, 3));
        Assertions.assertEquals( 2, truncated.index(5, 3));
        Assertions.assertEquals( 3, truncated.index(2, 2));
        Assertions.assertEquals( 4, truncated.index(3, 2));
        Assertions.assertEquals( 5, truncated.index(4, 2));
        Assertions.assertEquals( 6, truncated.index(5, 2));
        Assertions.assertEquals( 7, truncated.index(1, 1));
        Assertions.assertEquals( 8, truncated.index(2, 1));
        Assertions.assertEquals( 9, truncated.index(3, 1));
        Assertions.assertEquals(10, truncated.index(4, 1));
        Assertions.assertEquals(11, truncated.index(5, 1));
        Assertions.assertEquals(12, truncated.index(0, 0));
        Assertions.assertEquals(13, truncated.index(1, 0));
        Assertions.assertEquals(14, truncated.index(2, 0));
        Assertions.assertEquals(15, truncated.index(3, 0));
        Assertions.assertEquals(16, truncated.index(4, 0));
        Assertions.assertEquals(17, truncated.index(5, 0));
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
            Assertions.assertEquals(i, flat[i], 1.0e-15);
        }
    }

    @Test
    public void testSize() {
        for (int degree = 0; degree <= 60; ++degree) {
            for (int order = 0; order <= degree; ++order) {
                Assertions.assertEquals(loop(degree, order, degree, 0) + 1,
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
                        Assertions.assertEquals(loop(degree, order, n, m),
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
                Assertions.assertEquals(degree, flattener.getDegree());
                Assertions.assertEquals(order, flattener.getOrder());
                for (int n = -2; n < degree + 2; ++n) {
                    for (int m = -2; m <= FastMath.min(n, order) + 2; ++m) {
                        if (n < 0 || n > degree || m < 0 || m > FastMath.min(n, order)) {
                            try {
                                flattener.index(n, m);
                                Assertions.fail("an exception should have been thrown");
                            } catch (OrekitException oe) {
                                Assertions.assertEquals(OrekitMessages.WRONG_DEGREE_OR_ORDER, oe.getSpecifier());
                                Assertions.assertEquals(n,      ((Integer) oe.getParts()[0]).intValue());
                                Assertions.assertEquals(m,      ((Integer) oe.getParts()[1]).intValue());
                                Assertions.assertEquals(degree, ((Integer) oe.getParts()[2]).intValue());
                                Assertions.assertEquals(order,  ((Integer) oe.getParts()[3]).intValue());
                                Assertions.assertFalse(flattener.withinRange(n, m));
                            }
                        } else {
                            Assertions.assertTrue(flattener.index(n, m) >= 0);
                            Assertions.assertTrue(flattener.withinRange(n, m));
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
