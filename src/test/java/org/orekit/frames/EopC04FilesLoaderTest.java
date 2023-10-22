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
package org.orekit.frames;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.data.AbstractFilesLoaderTest;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.ChronologicalComparator;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;

import java.util.SortedSet;
import java.util.TreeSet;


public class EopC04FilesLoaderTest extends AbstractFilesLoaderTest {

    @Test
    public void testMissingMonths() {
        setRoot("missing-months");
        IERSConventions.NutationCorrectionConverter converter =
                IERSConventions.IERS_2010.getNutationCorrectionConverter();
        SortedSet<EOPEntry> history = new TreeSet<EOPEntry>(new ChronologicalComparator());
        new EopC04FilesLoader(FramesFactory.EOPC04_2000_FILENAME, manager, () -> utc).fillHistory(converter, history);
        Assertions.assertTrue(getMaxGap(history) > 5);
    }

    @Test
    public void testStartDate() {
        setRoot("regular-data");
        IERSConventions.NutationCorrectionConverter converter =
                IERSConventions.IERS_2010.getNutationCorrectionConverter();
        SortedSet<EOPEntry> history = new TreeSet<EOPEntry>(new ChronologicalComparator());
        new EopC04FilesLoader(FramesFactory.EOPC04_2000_FILENAME, manager, () -> utc).fillHistory(converter, history);
        Assertions.assertEquals(new AbsoluteDate(2003, 1, 1, TimeScalesFactory.getUTC()),
                            new EOPHistory(IERSConventions.IERS_2010, EOPHistory.DEFAULT_INTERPOLATION_DEGREE,
                                           history, true).getStartDate());
    }

    @Test
    public void testEndDate() {
        setRoot("regular-data");
        IERSConventions.NutationCorrectionConverter converter =
                IERSConventions.IERS_2010.getNutationCorrectionConverter();
        SortedSet<EOPEntry> history = new TreeSet<EOPEntry>(new ChronologicalComparator());
        new EopC04FilesLoader(FramesFactory.EOPC04_2000_FILENAME, manager, () -> utc).fillHistory(converter, history);
        Assertions.assertEquals(new AbsoluteDate(2005, 12, 31, TimeScalesFactory.getUTC()),
                            new EOPHistory(IERSConventions.IERS_2010, EOPHistory.DEFAULT_INTERPOLATION_DEGREE,
                                           history, true).getEndDate());
    }

    @Test
    public void testContent() {
        setRoot("regular-data");
        IERSConventions.NutationCorrectionConverter converter =
                IERSConventions.IERS_2010.getNutationCorrectionConverter();
        SortedSet<EOPEntry> data = new TreeSet<EOPEntry>(new ChronologicalComparator());
        new EopC04FilesLoader(FramesFactory.EOPC04_2000_FILENAME, manager, () -> utc).fillHistory(converter, data);
        EOPHistory history = new EOPHistory(IERSConventions.IERS_2010, EOPHistory.DEFAULT_INTERPOLATION_DEGREE,
                                            data, true);
        Assertions.assertEquals(IERSConventions.IERS_2010, history.getConventions());
        AbsoluteDate date = new AbsoluteDate(2003, 1, 7, 12, 0, 0, TimeScalesFactory.getUTC());
        Assertions.assertEquals(        (9 * ( 0.0007777 +  0.0008565) - ( 0.0005883 +  0.0008758)) / 16,  history.getLOD(date), 1.0e-10);
        Assertions.assertEquals(        (9 * (-0.2920264 + -0.2928461) - (-0.2913281 + -0.2937305)) / 16,  history.getUT1MinusUTC(date), 1.0e-10);
        Assertions.assertEquals(asToRad((9 * (-0.105933  + -0.108553)  - (-0.103513  + -0.111054))  / 16), history.getPoleCorrection(date).getXp(), 2.5e-12);
        Assertions.assertEquals(asToRad((9 * ( 0.201451  +  0.203596)  - ( 0.199545  +  0.205660))  / 16), history.getPoleCorrection(date).getYp(), 8.3e-11);
        Assertions.assertEquals(ITRFVersion.ITRF_2008, history.getITRFVersion(date));
    }

    @Test
    public void testMixedItrf() {
        setRoot("eopc04");
        IERSConventions.NutationCorrectionConverter converter =
                IERSConventions.IERS_2010.getNutationCorrectionConverter();
        SortedSet<EOPEntry> data = new TreeSet<EOPEntry>(new ChronologicalComparator());
        new EopC04FilesLoader(FramesFactory.EOPC04_2000_FILENAME, manager, () -> utc).fillHistory(converter, data);
        EOPHistory history = new EOPHistory(IERSConventions.IERS_2010, EOPHistory.DEFAULT_INTERPOLATION_DEGREE,
                                            data, true);
        Assertions.assertEquals(IERSConventions.IERS_2010, history.getConventions());

        // this entry is from eopc/eopc04.11
        AbsoluteDate date2011 = new AbsoluteDate(2011, 1, 9, 12, 0, 0, TimeScalesFactory.getUTC());
        Assertions.assertEquals(        (9 * ( 0.0005774 +  0.0005865) - ( 0.0005456 +  0.0005776)) / 16,  history.getLOD(date2011), 1.0e-10);
        Assertions.assertEquals(        (9 * (-0.1431393 + -0.1437380) - (-0.1425794 + -0.1443274)) / 16,  history.getUT1MinusUTC(date2011), 1.0e-10);
        Assertions.assertEquals(asToRad((9 * ( 0.103097  +  0.101098)  - ( 0.106313  +  0.099966))  / 16), history.getPoleCorrection(date2011).getXp(), 2.9e-11);
        Assertions.assertEquals(asToRad((9 * ( 0.197749  +  0.196801)  - ( 0.198821  +  0.196146))  / 16), history.getPoleCorrection(date2011).getYp(), 9.0e-12);
        Assertions.assertEquals(ITRFVersion.ITRF_2005, history.getITRFVersion(date2011));

        // this entry is from eopc/eopc04.12
        AbsoluteDate date2012 = new AbsoluteDate(2012, 1, 9, 12, 0, 0, TimeScalesFactory.getUTC());
        Assertions.assertEquals(        (9 * ( 0.0009020 +  0.0009794) - ( 0.0007866 +  0.0011310)) / 16,  history.getLOD(date2012), 1.0e-10);
        Assertions.assertEquals(        (9 * (-0.4263648 + -0.4272859) - (-0.4255338 + -0.4283517)) / 16,  history.getUT1MinusUTC(date2012), 1.0e-10);
        Assertions.assertEquals(asToRad((9 * ( 0.104225  +  0.101787)  - ( 0.106429  +  0.099783))  / 16), history.getPoleCorrection(date2012).getXp(), 5.2e-11);
        Assertions.assertEquals(asToRad((9 * ( 0.258041  +  0.257252)  - ( 0.258874  +  0.256002))  / 16), history.getPoleCorrection(date2012).getYp(), 1.4e-10);
        Assertions.assertEquals(ITRFVersion.ITRF_2008, history.getITRFVersion(date2012));

        // this entry is from eopc/eopc04.13
        AbsoluteDate date2013 = new AbsoluteDate(2013, 1, 9, 12, 0, 0, TimeScalesFactory.getUTC());
        Assertions.assertEquals(        (9 * (0.0008315 + 0.0008859) - (0.0008888 + 0.0010276)) / 16,  history.getLOD(date2013), 1.0e-10);
        Assertions.assertEquals(        (9 * (0.2686090 + 0.2677610) - (0.2694696 + 0.2668054)) / 16,  history.getUT1MinusUTC(date2013), 1.0e-10);
        Assertions.assertEquals(asToRad((9 * (0.065348  + 0.064418)  - (0.066251  + 0.063242))  / 16), history.getPoleCorrection(date2013).getXp(), 4.0e-11);
        Assertions.assertEquals(asToRad((9 * (0.291376  + 0.292248)  - (0.290552  + 0.293086))  / 16), history.getPoleCorrection(date2013).getYp(), 2.3e-11);
        Assertions.assertEquals(ITRFVersion.ITRF_2014, history.getITRFVersion(date2013));

        // this entry is from eopc/eopc04.14
        AbsoluteDate date2014 = new AbsoluteDate(2014, 1, 9, 12, 0, 0, TimeScalesFactory.getUTC());
        Assertions.assertEquals(        (9 * ( 0.0009514 +  0.0008041) - ( 0.0011181 +  0.0006787)) / 16,  history.getLOD(date2014), 1.0e-10);
        Assertions.assertEquals(        (9 * (-0.1072271 + -0.1081017) - (-0.1061938 + -0.1088416)) / 16,  history.getUT1MinusUTC(date2014), 1.0e-10);
        Assertions.assertEquals(asToRad((9 * ( 0.033902  +  0.033520)  - ( 0.034347  +  0.032766))  / 16), history.getPoleCorrection(date2014).getXp(), 5.4e-11);
        Assertions.assertEquals(asToRad((9 * ( 0.325031  +  0.326443)  - ( 0.323939  +  0.327836))  / 16), history.getPoleCorrection(date2014).getYp(), 8.0e-12);
        Assertions.assertEquals(ITRFVersion.ITRF_2020, history.getITRFVersion(date2014));

        // this entry is from eopc/eopc04.15 (beware, this file is sampled at 12h UTC, not 00h UTC)
        AbsoluteDate date2015 = new AbsoluteDate(2015, 1, 10, 0, 0, 0, TimeScalesFactory.getUTC());
        Assertions.assertEquals(        (9 * ( 0.0010180 +  0.0010925) - ( 0.0009043 +  0.0011262)) / 16,  history.getLOD(date2015), 1.0e-10);
        Assertions.assertEquals(        (9 * (-0.4665177 + -0.4675780) - (-0.4655555 + -0.4686919)) / 16,  history.getUT1MinusUTC(date2015), 1.0e-10);
        Assertions.assertEquals(asToRad((9 * ( 0.024041  +  0.023048)  - ( 0.025192  +  0.022026))  / 16), history.getPoleCorrection(date2015).getXp(), 6.5e-11);
        Assertions.assertEquals(asToRad((9 * ( 0.286304  +  0.287241)  - ( 0.285811  +  0.288345))  / 16), history.getPoleCorrection(date2015).getYp(), 2.2e-10);
        Assertions.assertEquals(ITRFVersion.ITRF_2020, history.getITRFVersion(date2015));

    }

    private double asToRad(double as) {
        return as * Constants.ARC_SECONDS_TO_RADIANS;
    }

}
