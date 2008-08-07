/* Copyright 2002-2008 CS Communication & Systèmes
 * Licensed to CS Communication & Systèmes (CS) under one or more
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
package org.orekit.time;

import junit.framework.*;

public class TCGScaleTest
extends TestCase {

    public TCGScaleTest(String name) {
        super(name);
    }

    public void testRatio() {
        TimeScale scale = TCGScale.getInstance();
        final double dtTT = 1e6;
        final AbsoluteDate t1 = AbsoluteDate.J2000_EPOCH;
        final AbsoluteDate t2 = new AbsoluteDate(t1, dtTT);
        final double dtTCG = dtTT + scale.offsetFromTAI(t2) - scale.offsetFromTAI(t1);
        assertEquals(1 - 6.969290134e-10, dtTT / dtTCG, 1.0e-15);
    }

    public void testSymmetry() {
        TimeScale scale = TCGScale.getInstance();
        for (double dt = -10000; dt < 10000; dt += 123.456789) {
            AbsoluteDate date = new AbsoluteDate(AbsoluteDate.J2000_EPOCH, dt * 86400);
            double dt1 = scale.offsetFromTAI(date);
            ChunksPair chunks = date.getChunks(scale);
            double dt2 = scale.offsetToTAI(chunks.getDate(), chunks.getTime());
            assertEquals( 0.0, dt1 + dt2, 1.0e-10);
        }
    }

    public void testReference() {
        ChunkedDate referenceDate = new ChunkedDate(1977, 01, 01);
        ChunkedTime thirtyTwo     = new ChunkedTime(0, 0, 32.184);
        AbsoluteDate ttRef  = new AbsoluteDate(referenceDate, thirtyTwo, TTScale.getInstance());
        AbsoluteDate tcgRef = new AbsoluteDate(referenceDate, thirtyTwo, TCGScale.getInstance());
        AbsoluteDate taiRef = new AbsoluteDate(referenceDate, ChunkedTime.H00, TAIScale.getInstance());
        assertEquals(0, ttRef.minus(tcgRef), 1.0e-15);
        assertEquals(0, ttRef.minus(taiRef), 1.0e-15);
    }

    public static Test suite() {
        return new TestSuite(TCGScaleTest.class);
    }

}
