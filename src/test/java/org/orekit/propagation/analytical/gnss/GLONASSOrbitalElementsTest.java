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
package org.orekit.propagation.analytical.gnss;

import org.hipparchus.util.Precision;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.propagation.analytical.gnss.data.GLONASSOrbitalElements;
import org.orekit.time.AbsoluteDate;

public class GLONASSOrbitalElementsTest {

    @Test
    public void testDefaultMethods() {
        GLONASSOrbitalElements goe = new GLONASSOrbitalElements() {
            public AbsoluteDate getDate() { return null; }
        };
        Assertions.assertEquals(0,   goe.getIOD());
        Assertions.assertEquals(0,   goe.getNa());
        Assertions.assertEquals(0,   goe.getN4());
        Assertions.assertEquals(0.0, goe.getTime(),      Precision.SAFE_MIN);
        Assertions.assertEquals(0.0, goe.getLambda(),    Precision.SAFE_MIN);
        Assertions.assertEquals(0.0, goe.getE(),         Precision.SAFE_MIN);
        Assertions.assertEquals(0.0, goe.getPa(),        Precision.SAFE_MIN);
        Assertions.assertEquals(0.0, goe.getDeltaI(),    Precision.SAFE_MIN);
        Assertions.assertEquals(0.0, goe.getDeltaT(),    Precision.SAFE_MIN);
        Assertions.assertEquals(0.0, goe.getDeltaTDot(), Precision.SAFE_MIN);
        Assertions.assertEquals(0.0, goe.getGammaN(),    Precision.SAFE_MIN);
        Assertions.assertEquals(0.0, goe.getTN(),        Precision.SAFE_MIN);
        Assertions.assertEquals(0.0, goe.getXDot(),      Precision.SAFE_MIN);
        Assertions.assertEquals(0.0, goe.getYDot(),      Precision.SAFE_MIN);
        Assertions.assertEquals(0.0, goe.getZDot(),      Precision.SAFE_MIN);
        Assertions.assertEquals(0.0, goe.getX(),         Precision.SAFE_MIN);
        Assertions.assertEquals(0.0, goe.getY(),         Precision.SAFE_MIN);
        Assertions.assertEquals(0.0, goe.getZ(),         Precision.SAFE_MIN);
        Assertions.assertEquals(0.0, goe.getXDotDot(),   Precision.SAFE_MIN);
        Assertions.assertEquals(0.0, goe.getYDotDot(),   Precision.SAFE_MIN);
        Assertions.assertEquals(0.0, goe.getZDotDot(),   Precision.SAFE_MIN);
    }

}
