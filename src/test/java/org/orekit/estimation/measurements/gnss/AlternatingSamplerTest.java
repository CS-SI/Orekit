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
package org.orekit.estimation.measurements.gnss;

import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class AlternatingSamplerTest {

    @Test
    public void testCanonicalA001057() {
        check(new AlternatingSampler(0, 7.5),
              0, 1, -1, 2, -2, 3, -3, 4, -4, 5, -5, 6, -6, 7, -7);
    }

    @Test
    public void testNegatedA001057() {
        check(new AlternatingSampler(-0.1, 7.5),
              0, -1, 1, -2, 2, -3, 3, -4, 4, -5, 5, -6, 6, -7, 7);
    }

    @Test
    public void testJavadocExample() {
        check(new AlternatingSampler(17.3, 5.2),
              17, 18, 16, 19, 15, 20, 14, 21, 13, 22);
    }

    @Test
    public void testIntegerLimitsOdd() {
        check(new AlternatingSampler(12.0, 3.0),
              12, 13, 11, 14, 10, 15, 9);
    }

    @Test
    public void testIntegerLimitsEven() {
        check(new AlternatingSampler(12.0, 4.0),
              12, 13, 11, 14, 10, 15, 9, 16, 8);
    }

    @Test
    public void testNoIntegerInRange() {
        Assertions.assertFalse(new AlternatingSampler(12.4, 0.25).inRange());
    }

    @Test
    public void testLimits() {
        for (double a = -5.5; a <= 5.5; a += 0.125) {
              for (double r = 0.0; r <= 3.5; r += 0.125) {
                  final AlternatingSampler sampler = new AlternatingSampler(a, r);

                  while (sampler.inRange()) {
                      Assertions.assertTrue(sampler.getCurrent() >= a - r);
                      Assertions.assertTrue(sampler.getCurrent() <= a + r);
                      sampler.generateNext();
                  };

                  // once range has been exceeded, all generated numbers
                  // are out of range
                  Assertions.assertFalse(sampler.inRange());
                  sampler.generateNext();
                  Assertions.assertFalse(sampler.inRange());
                  sampler.generateNext();
                  Assertions.assertFalse(sampler.inRange());
                  sampler.generateNext();
                  Assertions.assertFalse(sampler.inRange());

              }
        }
    }

    @Test
    public void testNoMiss() {
        for (double a = -5.5; a <= 5.5; a += 0.125) {
              for (double r = 0.0; r <= 3.5; r += 0.125) {
                  final int min = (int) FastMath.ceil(a - r);
                  final int max = (int) FastMath.floor(a + r);
                  final boolean[] seen = new boolean[max - min + 1];
                  final AlternatingSampler sampler = new AlternatingSampler(a, r);

                  while (sampler.inRange()) {
                      final int k = (int) (sampler.getCurrent() - min);
                      Assertions.assertFalse(seen[k]);
                      seen[k] = true;
                      Assertions.assertTrue(sampler.getCurrent() >= a - r);
                      Assertions.assertTrue(sampler.getCurrent() <= a + r);
                      sampler.generateNext();
                  };

                  // once range has been exceeded, all generated numbers
                  // are out of range
                  Assertions.assertFalse(sampler.inRange());
                  sampler.generateNext();
                  Assertions.assertFalse(sampler.inRange());
                  sampler.generateNext();
                  Assertions.assertFalse(sampler.inRange());
                  sampler.generateNext();
                  Assertions.assertFalse(sampler.inRange());

                  // all the integers in the [min; max] range
                  // should have been generated exactly once
                  for (final boolean s : seen) {
                      Assertions.assertTrue(s);
                  }

              }
        }
    }

    private void check(final AlternatingSampler sampler, final int... expected) {

        for (int i = 0; i < expected.length; ++i) {
            Assertions.assertTrue(sampler.inRange());
            Assertions.assertEquals(expected[i], sampler.getCurrent());
            sampler.generateNext();
        }

        // once range has been exceeded, all generated numbers
        // are out of range
        Assertions.assertFalse(sampler.inRange());
        sampler.generateNext();
        Assertions.assertFalse(sampler.inRange());
        sampler.generateNext();
        Assertions.assertFalse(sampler.inRange());
        sampler.generateNext();
        Assertions.assertFalse(sampler.inRange());

    }

}

