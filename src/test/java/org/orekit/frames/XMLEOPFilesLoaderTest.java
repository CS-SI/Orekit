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


public class XMLEOPFilesLoaderTest extends AbstractFilesLoaderTest {

    @Test
    public void testStartDateDaily1980() throws OrekitException, ParseException {
        setRoot("xml-daily-data");
        EOP1980History history = new EOP1980History();
        new XMLEOPFilesLoader("^finals\\.daily\\.xml$").fillHistory(history);
        Assert.assertEquals(new AbsoluteDate(2010, 7, 1, TimeScalesFactory.getUTC()),
                            history.getStartDate());
    }

    @Test
    public void testEndDateDaily1980() throws OrekitException, ParseException {
        setRoot("xml-daily-data");
        EOP1980History history = new EOP1980History();
        new XMLEOPFilesLoader("^finals\\.daily\\.xml$").fillHistory(history);
        Assert.assertEquals(new AbsoluteDate(2010, 8, 1, TimeScalesFactory.getUTC()),
                            history.getEndDate());
    }

    @Test
    public void testStartDateDaily2000() throws OrekitException, ParseException {
        setRoot("xml-daily-data");
        EOP2000History history = new EOP2000History();
        new XMLEOPFilesLoader("^finals2000A\\.daily\\.xml$").fillHistory(history);
        Assert.assertEquals(new AbsoluteDate(2010, 5, 11, TimeScalesFactory.getUTC()),
                            history.getStartDate());
    }

    @Test
    public void testEndDateDaily2000() throws OrekitException, ParseException {
        setRoot("xml-daily-data");
        EOP2000History history = new EOP2000History();
        new XMLEOPFilesLoader("^finals2000A\\.daily\\.xml$").fillHistory(history);
        Assert.assertEquals(new AbsoluteDate(2010, 7, 24, TimeScalesFactory.getUTC()),
                            history.getEndDate());
    }

    @Test
    public void testStartDateFinals1980() throws OrekitException, ParseException {
        setRoot("compressed-data");
        EOP1980History history = new EOP1980History();
        new XMLEOPFilesLoader("^finals\\.1999\\.xml$").fillHistory(history);
        Assert.assertEquals(new AbsoluteDate(1999, 1, 1, TimeScalesFactory.getUTC()),
                            history.getStartDate());
    }

    @Test
    public void testEndDateFinals1980() throws OrekitException, ParseException {
        setRoot("compressed-data");
        EOP1980History history = new EOP1980History();
        new XMLEOPFilesLoader("^finals\\.1999\\.xml$").fillHistory(history);
        Assert.assertEquals(new AbsoluteDate(1999, 12, 31, TimeScalesFactory.getUTC()),
                            history.getEndDate());
    }

    @Test
    public void testStartDateFinals2000() throws OrekitException, ParseException {
        setRoot("regular-data");
        EOP2000History history = new EOP2000History();
        new XMLEOPFilesLoader("^finals2000A\\.2002\\.xml$").fillHistory(history);
        Assert.assertEquals(new AbsoluteDate(2002, 1, 1, TimeScalesFactory.getUTC()),
                            history.getStartDate());
    }

    @Test
    public void testEndDateFinals2000() throws OrekitException, ParseException {
        setRoot("regular-data");
        EOP2000History history = new EOP2000History();
        new XMLEOPFilesLoader("^finals2000A\\.2002\\.xml$").fillHistory(history);
        Assert.assertEquals(new AbsoluteDate(2002, 12, 31, TimeScalesFactory.getUTC()),
                            history.getEndDate());
    }

}
