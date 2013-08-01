/* Copyright 2002-2013 CS Systèmes d'Information
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
package org.orekit.frames;


import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.errors.OrekitException;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;


public class ITRFEquinoxProviderTest {

    @Test
    public void testEquinoxVersusCIO() throws OrekitException {
        Frame itrfEquinox  = FramesFactory.getITRFEquinox(IERSConventions.IERS_1996);
        Frame itrfCIO      = FramesFactory.getITRF2005();
        AbsoluteDate start = new AbsoluteDate(2011, 4, 10, TimeScalesFactory.getUTC());
        AbsoluteDate end   = new AbsoluteDate(2011, 7,  4, TimeScalesFactory.getUTC());
        for (AbsoluteDate date = start; date.compareTo(end) < 0; date = date.shiftedBy(10000)) {
            double angularOffset =
                    itrfEquinox.getTransformTo(itrfCIO, date).getRotation().getAngle();
            Assert.assertEquals(0, angularOffset / Constants.ARC_SECONDS_TO_RADIANS, 0.07);
        }
    }

    @Before
    public void setUp() {
        Utils.setDataRoot("rapid-data-columns");
    }

}
