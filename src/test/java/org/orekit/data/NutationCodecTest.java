/* Copyright 2002-2016 CS Systèmes d'Information
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
package org.orekit.data;


import java.util.Arrays;

import org.hipparchus.random.RandomGenerator;
import org.hipparchus.random.Well1024a;
import org.junit.Assert;
import org.junit.Test;


public class NutationCodecTest {

    @Test
    public void testKeySymmetry() {

        RandomGenerator random = new Well1024a(0x8fef7f6f99ad5d56l);
        int[] multipliers = new int[15];
        for (int i = 0; i < 100000; ++i) {
            Arrays.fill(multipliers, 0);
            int nb = 1 + random.nextInt(7);
            for (int k = 0; k < nb; ++k) {
                int index = random.nextInt(15);
                while (multipliers[index] == 0) {
                    multipliers[index] = random.nextInt(128) - 64;
                }
            }
            long key = NutationCodec.encode(multipliers);
            int[] rebuilt = NutationCodec.decode(key);
            for (int k = 0; k < multipliers.length; ++k) {
                Assert.assertEquals(multipliers[k], rebuilt[k]);
            }
        }

    }

}
