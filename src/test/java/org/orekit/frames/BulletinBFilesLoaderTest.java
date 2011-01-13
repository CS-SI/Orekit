/* Copyright 2002-2011 CS Communication & Systèmes
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


import java.text.ParseException;

import org.junit.Assert;
import org.junit.Test;
import org.orekit.data.AbstractFilesLoaderTest;
import org.orekit.errors.OrekitException;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;


public class BulletinBFilesLoaderTest extends AbstractFilesLoaderTest {

    @Test
    public void testMissingMonths() throws OrekitException {
        setRoot("missing-months");
        EOP2000History history = new EOP2000History();
        new BulletinBFilesLoader(FramesFactory.BULLETINB_2000_FILENAME).fillHistory(history);
        Assert.assertTrue(getMaxGap(history) > 5);
    }

    @Test
    public void testStartDate() throws OrekitException, ParseException {
        setRoot("regular-data");
        EOP2000History history = new EOP2000History();
        new BulletinBFilesLoader(FramesFactory.BULLETINB_2000_FILENAME).fillHistory(history);
        Assert.assertEquals(new AbsoluteDate(2005, 12, 5, TimeScalesFactory.getUTC()),
                            history.getStartDate());
    }

    @Test
    public void testEndDate() throws OrekitException, ParseException {
        setRoot("regular-data");
        EOP2000History history = new EOP2000History();
        new BulletinBFilesLoader(FramesFactory.BULLETINB_2000_FILENAME).fillHistory(history);
        Assert.assertTrue(getMaxGap(history) < 5);
        Assert.assertEquals(new AbsoluteDate(2006, 3, 5, TimeScalesFactory.getUTC()),
                            history.getEndDate());
    }

    @Test
    public void testNewFormatNominal() throws OrekitException, ParseException {
        setRoot("new-bulletinB");
        EOP1980History history = new EOP1980History();
        new BulletinBFilesLoader("bulletinb.270").fillHistory(history);
        Assert.assertEquals(new AbsoluteDate(2010, 6, 2, TimeScalesFactory.getUTC()),
                            history.getStartDate());
        Assert.assertEquals(new AbsoluteDate(2010, 7, 1, TimeScalesFactory.getUTC()),
                            history.getEndDate());
    }

    @Test(expected=OrekitException.class)
    public void testNewFormatTruncated() throws OrekitException, ParseException {
        setRoot("new-bulletinB");
        EOP1980History history = new EOP1980History();
        new BulletinBFilesLoader("bulletinb-truncated.270").fillHistory(history);
    }

    @Test(expected=OrekitException.class)
    public void testNewFormatInconsistent() throws OrekitException, ParseException {
        setRoot("new-bulletinB");
        EOP1980History history = new EOP1980History();
        new BulletinBFilesLoader("bulletinb-inconsistent.270").fillHistory(history);
    }

}
