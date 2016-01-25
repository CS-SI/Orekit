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
package org.orekit.frames;


import java.util.SortedSet;
import java.util.TreeSet;

import org.junit.Assert;
import org.junit.Test;
import org.orekit.data.AbstractFilesLoaderTest;
import org.orekit.errors.OrekitException;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.ChronologicalComparator;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;


public class EOP08C04FilesLoaderTest extends AbstractFilesLoaderTest {

    @Test
    public void testMissingMonths() throws OrekitException {
        setRoot("missing-months");
        IERSConventions.NutationCorrectionConverter converter =
                IERSConventions.IERS_2010.getNutationCorrectionConverter();
        SortedSet<EOPEntry> history = new TreeSet<EOPEntry>(new ChronologicalComparator());
        new EOP08C04FilesLoader(FramesFactory.EOPC04_2000_FILENAME).fillHistory(converter, history);
        Assert.assertTrue(getMaxGap(history) > 5);
    }

    @Test
    public void testStartDate() throws OrekitException {
        setRoot("regular-data");
        IERSConventions.NutationCorrectionConverter converter =
                IERSConventions.IERS_2010.getNutationCorrectionConverter();
        SortedSet<EOPEntry> history = new TreeSet<EOPEntry>(new ChronologicalComparator());
        new EOP08C04FilesLoader(FramesFactory.EOPC04_2000_FILENAME).fillHistory(converter, history);
        Assert.assertEquals(new AbsoluteDate(2003, 1, 1, TimeScalesFactory.getUTC()),
                            new EOPHistory(IERSConventions.IERS_2010, history, true).getStartDate());
    }

    @Test
    public void testEndDate() throws OrekitException {
        setRoot("regular-data");
        IERSConventions.NutationCorrectionConverter converter =
                IERSConventions.IERS_2010.getNutationCorrectionConverter();
        SortedSet<EOPEntry> history = new TreeSet<EOPEntry>(new ChronologicalComparator());
        new EOP08C04FilesLoader(FramesFactory.EOPC04_2000_FILENAME).fillHistory(converter, history);
        Assert.assertEquals(new AbsoluteDate(2005, 12, 31, TimeScalesFactory.getUTC()),
                            new EOPHistory(IERSConventions.IERS_2010, history, true).getEndDate());
    }

    @Test
    public void testContent() throws OrekitException {
        setRoot("regular-data");
        IERSConventions.NutationCorrectionConverter converter =
                IERSConventions.IERS_2010.getNutationCorrectionConverter();
        SortedSet<EOPEntry> data = new TreeSet<EOPEntry>(new ChronologicalComparator());
        new EOP08C04FilesLoader(FramesFactory.EOPC04_2000_FILENAME).fillHistory(converter, data);
        EOPHistory history = new EOPHistory(IERSConventions.IERS_2010, data, true);
        AbsoluteDate date = new AbsoluteDate(2003, 1, 7, 12, 0, 0, TimeScalesFactory.getUTC());
        Assert.assertEquals(        (9 * ( 0.0007777 +  0.0008565) - ( 0.0005883 +  0.0008758)) / 16,  history.getLOD(date), 1.0e-10);
        Assert.assertEquals(        (9 * (-0.2920264 + -0.2928461) - (-0.2913281 + -0.2937305)) / 16,  history.getUT1MinusUTC(date), 1.0e-10);
        Assert.assertEquals(asToRad((9 * (-0.105933  + -0.108553)  - (-0.103513  + -0.111054))  / 16), history.getPoleCorrection(date).getXp(), 1.0e-10);
        Assert.assertEquals(asToRad((9 * ( 0.201451  +  0.203596)  - ( 0.199545  +  0.205660))  / 16), history.getPoleCorrection(date).getYp(), 1.0e-10);
    }

    private double asToRad(double as) {
        return as * Constants.ARC_SECONDS_TO_RADIANS;
    }

}
