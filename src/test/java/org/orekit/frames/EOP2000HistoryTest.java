/* Copyright 2002-2010 CS Communication & Systèmes
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
package org.orekit.frames;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.errors.OrekitException;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;


public class EOP2000HistoryTest {

    @Test
    public void testRegular() throws OrekitException {
        AbsoluteDate date = new AbsoluteDate(2004, 1, 4, TimeScalesFactory.getUTC());
        double dt = FramesFactory.getEOP2000History().getUT1MinusUTC(date);
        Assert.assertEquals(-0.3906591, dt, 1.0e-10);
    }

    @Test
    public void testOutOfRange() throws OrekitException {
        EOP2000History history = FramesFactory.getEOP2000History();
        AbsoluteDate endDate = new AbsoluteDate(2006, 3, 5, TimeScalesFactory.getUTC());
        for (double t = -1000; t < 1000 ; t += 10) {
            AbsoluteDate date = new AbsoluteDate(endDate, t);
            double dt = history.getUT1MinusUTC(date);
            if (t <= 0) {
                Assert.assertTrue(dt < 0.29236);
                Assert.assertTrue(dt > 0.29233);
            } else {
                // no more data after end date
                Assert.assertEquals(0.0, dt, 1.0e-10);
            }
        }
    }

    @Before
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

}
