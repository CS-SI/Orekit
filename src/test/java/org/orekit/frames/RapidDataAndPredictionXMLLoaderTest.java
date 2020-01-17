/* Copyright 2002-2020 CS Group
 * Licensed to CS Group (CS) under one or more
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


import java.net.MalformedURLException;
import java.util.Collections;
import java.util.SortedSet;
import java.util.TreeSet;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;
import org.orekit.data.AbstractFilesLoaderTest;
import org.orekit.errors.OrekitException;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.ChronologicalComparator;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.IERSConventions;


public class RapidDataAndPredictionXMLLoaderTest extends AbstractFilesLoaderTest {

    private static final ChronologicalComparator COMP = new ChronologicalComparator();

    @Test
    public void testExternalResourcesAreIgnoredIssue368() {
        // setup
        setRoot("external-resources");
        IERSConventions.NutationCorrectionConverter converter =
                IERSConventions.IERS_1996.getNutationCorrectionConverter();
        SortedSet<EOPEntry> history = new TreeSet<>(new ChronologicalComparator());
        RapidDataAndPredictionXMLLoader loader =
                new RapidDataAndPredictionXMLLoader("^finals2000A\\..*\\.xml$", manager, () -> utc);

        // action
        try {
            loader.fillHistory(converter, history);

            // verify
            Assert.fail("Expected Exception");
        } catch (OrekitException e) {
            // Malformed URL exception indicates external resource was disabled
            // file not found exception indicates parser tried to load the resource
            Assert.assertThat(e.getCause(),
                    CoreMatchers.instanceOf(MalformedURLException.class));
        }

        // problem if any EOP data is loaded
        Assert.assertEquals(0, history.size());
    }

    @Test
    public void testStartDateDaily1980() {
        setRoot("rapid-data-xml");
        IERSConventions.NutationCorrectionConverter converter =
                IERSConventions.IERS_1996.getNutationCorrectionConverter();
        SortedSet<EOPEntry> history = new TreeSet<EOPEntry>(new ChronologicalComparator());
        new RapidDataAndPredictionXMLLoader("^finals\\.daily\\.xml$", manager, () -> utc).fillHistory(converter, history);
        Assert.assertEquals(new AbsoluteDate(2010, 7, 1, TimeScalesFactory.getUTC()),
                            new EOPHistory(IERSConventions.IERS_1996, history, true).getStartDate());
    }

    @Test
    public void testEndDateDaily1980() {
        setRoot("rapid-data-xml");
        IERSConventions.NutationCorrectionConverter converter =
                IERSConventions.IERS_1996.getNutationCorrectionConverter();
        SortedSet<EOPEntry> history = new TreeSet<EOPEntry>(new ChronologicalComparator());
        new RapidDataAndPredictionXMLLoader("^finals\\.daily\\.xml$", manager, () -> utc).fillHistory(converter, history);
        Assert.assertEquals(new AbsoluteDate(2010, 8, 1, TimeScalesFactory.getUTC()),
                            new EOPHistory(IERSConventions.IERS_1996, history, true).getEndDate());
    }

    @Test
    public void testStartDateDaily2000() {
        setRoot("rapid-data-xml");
        IERSConventions.NutationCorrectionConverter converter =
                IERSConventions.IERS_2003.getNutationCorrectionConverter();
        SortedSet<EOPEntry> history = new TreeSet<EOPEntry>(new ChronologicalComparator());
        new RapidDataAndPredictionXMLLoader("^finals2000A\\.daily\\.xml$", manager, () -> utc).fillHistory(converter, history);
        Assert.assertEquals(new AbsoluteDate(2010, 5, 11, TimeScalesFactory.getUTC()),
                            Collections.min(history, COMP).getDate());
    }

    @Test
    public void testEndDateDaily2000() {
        setRoot("rapid-data-xml");
        IERSConventions.NutationCorrectionConverter converter =
                IERSConventions.IERS_2003.getNutationCorrectionConverter();
        SortedSet<EOPEntry> history = new TreeSet<EOPEntry>(new ChronologicalComparator());
        new RapidDataAndPredictionXMLLoader("^finals2000A\\.daily\\.xml$", manager, () -> utc).fillHistory(converter, history);
        Assert.assertEquals(new AbsoluteDate(2010, 7, 24, TimeScalesFactory.getUTC()),
                            Collections.max(history, COMP).getDate());
    }

    @Test
    public void testStartDateFinals1980() {
        setRoot("compressed-data");
        IERSConventions.NutationCorrectionConverter converter =
                IERSConventions.IERS_1996.getNutationCorrectionConverter();
        SortedSet<EOPEntry> history = new TreeSet<EOPEntry>(new ChronologicalComparator());
        new RapidDataAndPredictionXMLLoader("^finals\\.1999\\.xml$", manager, () -> utc).fillHistory(converter, history);
        Assert.assertEquals(new AbsoluteDate(1999, 1, 1, TimeScalesFactory.getUTC()),
                            new EOPHistory(IERSConventions.IERS_1996, history, true).getStartDate());
    }

    @Test
    public void testEndDateFinals1980() {
        setRoot("compressed-data");
        IERSConventions.NutationCorrectionConverter converter =
                IERSConventions.IERS_1996.getNutationCorrectionConverter();
        SortedSet<EOPEntry> history = new TreeSet<EOPEntry>(new ChronologicalComparator());
        new RapidDataAndPredictionXMLLoader("^finals\\.1999\\.xml$", manager, () -> utc).fillHistory(converter, history);
        Assert.assertEquals(new AbsoluteDate(1999, 12, 31, TimeScalesFactory.getUTC()),
                            new EOPHistory(IERSConventions.IERS_1996, history, true).getEndDate());
    }

    @Test
    public void testStartDateFinals2000() {
        setRoot("regular-data");
        IERSConventions.NutationCorrectionConverter converter =
                IERSConventions.IERS_2003.getNutationCorrectionConverter();
        SortedSet<EOPEntry> history = new TreeSet<EOPEntry>(new ChronologicalComparator());
        new RapidDataAndPredictionXMLLoader("^finals2000A\\.2002\\.xml$", manager, () -> utc).fillHistory(converter, history);
        Assert.assertEquals(new AbsoluteDate(2002, 1, 1, TimeScalesFactory.getUTC()),
                            new EOPHistory(IERSConventions.IERS_2003, history, true).getStartDate());
    }

    @Test
    public void testEndDateFinals2000() {
        setRoot("regular-data");
        IERSConventions.NutationCorrectionConverter converter =
                IERSConventions.IERS_2003.getNutationCorrectionConverter();
        SortedSet<EOPEntry> history = new TreeSet<EOPEntry>(new ChronologicalComparator());
        new RapidDataAndPredictionXMLLoader("^finals2000A\\.2002\\.xml$", manager, () -> utc).fillHistory(converter, history);
        Assert.assertEquals(new AbsoluteDate(2002, 12, 31, TimeScalesFactory.getUTC()),
                            new EOPHistory(IERSConventions.IERS_2003, history, true).getEndDate());
    }

    @Test
    public void testIssue139() {
        setRoot("zipped-data");
        IERSConventions.NutationCorrectionConverter converter =
                IERSConventions.IERS_1996.getNutationCorrectionConverter();
        SortedSet<EOPEntry> history = new TreeSet<EOPEntry>(new ChronologicalComparator());
        new RapidDataAndPredictionXMLLoader("^finals\\.daily\\.xml$", manager, () -> utc).fillHistory(converter, history);
        Assert.assertEquals(new AbsoluteDate(2010, 7, 1, TimeScalesFactory.getUTC()),
                            new EOPHistory(IERSConventions.IERS_1996, history, true).getStartDate());
    }

}
