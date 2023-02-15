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
import org.orekit.propagation.analytical.gnss.data.SBASOrbitalElements;
import org.orekit.time.AbsoluteDate;

public class SBASOrbitalElementsTest {

    @Test
    public void testDefaultMethods() {
        SBASOrbitalElements soe = new SBASOrbitalElements() {
            public AbsoluteDate getDate() { return null; }
            public int    getWeek()       { return 0; }
            public double getTime()       { return 0; }
            public int    getPRN()        { return 0; }
            public double getX()          { return 0; }
            public double getXDot()       { return 0; }
            public double getXDotDot()    { return 0; }
            public double getY()          { return 0; }
            public double getYDot()       { return 0; }
            public double getYDotDot()    { return 0; }
            public double getZ()          { return 0; }
            public double getZDot()       { return 0; }
            public double getZDotDot()    { return 0; }
        };
        Assertions.assertEquals(0,   soe.getIODN());
        Assertions.assertEquals(0.0, soe.getAGf0(), Precision.SAFE_MIN);
        Assertions.assertEquals(0.0, soe.getAGf1(), Precision.SAFE_MIN);
        Assertions.assertEquals(0.0, soe.getToc(), Precision.SAFE_MIN);

    }

}
