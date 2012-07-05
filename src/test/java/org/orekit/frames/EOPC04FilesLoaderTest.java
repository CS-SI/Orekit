/* Copyright 2002-2012 CS Systèmes d'Information
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


import java.text.ParseException;

import org.junit.Assert;
import org.junit.Test;
import org.orekit.data.AbstractFilesLoaderTest;
import org.orekit.errors.OrekitException;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;


public class EOPC04FilesLoaderTest extends AbstractFilesLoaderTest {

    @Test
    public void testMissingMonths() throws OrekitException {
        setRoot("missing-months");
        EOP2000History history = new EOP2000History();
        new EOP05C04FilesLoader(FramesFactory.EOPC04_2000_FILENAME).fillHistory(history);
        Assert.assertTrue(getMaxGap(history) > 5);
    }

    @Test
    public void testStartDate() throws OrekitException, ParseException {
        setRoot("regular-data");
        EOP2000History history = new EOP2000History();
        new EOP05C04FilesLoader(FramesFactory.EOPC04_2000_FILENAME).fillHistory(history);
        Assert.assertEquals(new AbsoluteDate(2003, 1, 1, TimeScalesFactory.getUTC()),
                            history.getStartDate());
    }

    @Test
    public void testEndDate() throws OrekitException, ParseException {
        setRoot("regular-data");
        EOP2000History history = new EOP2000History();
        new EOP05C04FilesLoader(FramesFactory.EOPC04_2000_FILENAME).fillHistory(history);
        Assert.assertEquals(new AbsoluteDate(2005, 12, 31, TimeScalesFactory.getUTC()),
                            history.getEndDate());
    }

    @Test
    public void testContent() throws OrekitException, ParseException {
        setRoot("regular-data");
        EOP2000History history = new EOP2000History();
        new EOP05C04FilesLoader(FramesFactory.EOPC04_2000_FILENAME).fillHistory(history);
        AbsoluteDate date = new AbsoluteDate(2003, 1, 7, 12, 0, 0, TimeScalesFactory.getUTC());
        Assert.assertEquals(        (-3 *  0.0006026 + 27 *  0.0007776 + 27 *  0.0008613 - 3 *  0.0008817) / 48,  history.getLOD(date), 1.0e-10);
        Assert.assertEquals(        (-3 * -0.2913617 + 27 * -0.2920235 + 27 * -0.2928453 - 3 * -0.2937273) / 48,  history.getUT1MinusUTC(date), 1.0e-10);
        Assert.assertEquals(asToRad((-3 * -0.103535  + 27 * -0.106053  + 27 * -0.108629  - 3 * -0.111086)  / 48), history.getPoleCorrection(date).getXp(), 1.0e-10);
        Assert.assertEquals(asToRad((-3 *  0.199584  + 27 *  0.201512  + 27 *  0.203603  - 3 *  0.205778)  / 48), history.getPoleCorrection(date).getYp(), 1.0e-10);
    }

    private double asToRad(double mas) {
        return mas * Constants.ARC_SECONDS_TO_RADIANS;
    }

}
