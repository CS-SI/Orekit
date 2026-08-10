/* Copyright 2002-2026 CS GROUP
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

import org.hipparchus.util.Binary64;
import org.hipparchus.util.Binary64Field;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;

public class EOPHistoryTest {

    @Test
    public void testRegular() {
        AbsoluteDate date = new AbsoluteDate(2004, 1, 4, TimeScalesFactory.getUTC());
        EOPHistory eopHistory = FramesFactory.getEOPHistory(IERSConventions.IERS_2010, true);
        double dt = eopHistory.getUT1MinusUTC(date);
        Assertions.assertEquals(EopDataType.FINAL, eopHistory.getEopDataType(date));
        Assertions.assertEquals(-0.3906070, dt, 1.0e-10);
    }

    @Test
    public void testOutOfRange() {
        EOPHistory history = FramesFactory.getEOPHistory(IERSConventions.IERS_2010, true);
        AbsoluteDate endDate = new AbsoluteDate(2006, 3, 5, TimeScalesFactory.getUTC());
        for (double t = -1000; t < 1000 ; t += 3) {
            AbsoluteDate date = endDate.shiftedBy(t);
            double dt = history.getUT1MinusUTC(date);
            if (t <= 0) {
                Assertions.assertEquals(EopDataType.FINAL, history.getEopDataType(date));
                Assertions.assertTrue(dt < 0.29236);
                Assertions.assertTrue(dt > 0.29233);
            } else {
                // no more data after end date
                Assertions.assertEquals(EopDataType.UNKNOWN, history.getEopDataType(date));
                Assertions.assertEquals(0.0, dt, 1.0e-10);
            }
        }
    }

    @Test
    public void testFieldOutOfRange() {
        EOPHistory history = FramesFactory.getEOPHistory(IERSConventions.IERS_2010, true);
        FieldAbsoluteDate<Binary64> endDate = new FieldAbsoluteDate<>(Binary64Field.getInstance(),
                                                                       2006, 3, 5, TimeScalesFactory.getUTC());
        for (double t = -1000; t < 1000 ; t += 3) {
            FieldAbsoluteDate<Binary64> date = endDate.shiftedBy(t);
            Binary64 dt = history.getUT1MinusUTC(date);
            if (t <= 0) {
                Assertions.assertTrue(dt.getReal() < 0.29236);
                Assertions.assertTrue(dt.getReal() > 0.29233);
            } else {
                // no more data after end date
                Assertions.assertEquals(0.0, dt.getReal(), 1.0e-10);
            }
        }
    }

    @Test
    public void testContinuityThreshold() {
        try {
            FramesFactory.setEOPContinuityThreshold(0.5 * Constants.JULIAN_DAY);
            AbsoluteDate date = new AbsoluteDate(2004, 1, 4, TimeScalesFactory.getUTC());
            FramesFactory.getEOPHistory(IERSConventions.IERS_2010, true).getUT1MinusUTC(date);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.MISSING_EARTH_ORIENTATION_PARAMETERS_BETWEEN_DATES_GAP,
                                oe.getSpecifier());
        }
    }

    @Test
    public void testUTCLeap() {
        EOPHistory history = FramesFactory.getEOPHistory(IERSConventions.IERS_2010, true);
        AbsoluteDate endLeap = new AbsoluteDate(2006, 1, 1, TimeScalesFactory.getUTC());
        for (double dt = -200; dt < 200; dt += 3) {
            final AbsoluteDate date = endLeap.shiftedBy(dt);
            double dtu1 = history.getUT1MinusUTC(date);
            if (dt <= 0) {
                Assertions.assertEquals(-0.6612, dtu1, 3.0e-5);
            } else {
                Assertions.assertEquals(0.3388, dtu1, 3.0e-5);
            }
        }
    }

    @Test
    public void testFieldUTCLeap() {
        EOPHistory history = FramesFactory.getEOPHistory(IERSConventions.IERS_2010, true);
        FieldAbsoluteDate<Binary64> endLeap = new FieldAbsoluteDate<>(Binary64Field.getInstance(),
                                                                       2006, 1, 1, TimeScalesFactory.getUTC());
        for (double dt = -200; dt < 200; dt += 3) {
            final FieldAbsoluteDate<Binary64> date = endLeap.shiftedBy(dt);
            Binary64 dtu1 = history.getUT1MinusUTC(date);
            if (dt <= 0) {
                Assertions.assertEquals(-0.6612, dtu1.getReal(), 3.0e-5);
            } else {
                Assertions.assertEquals(0.3388, dtu1.getReal(), 3.0e-5);
            }
        }
    }

    @Test
    public void testTidalInterpolationEffects() throws OrekitException {

        final EOPHistory h1 = FramesFactory.getEOPHistory(IERSConventions.IERS_2010, false);
        final EOPHistory h2 = h1.getEOPHistoryWithoutCachedTidalCorrection();
        final AbsoluteDate date0 = new AbsoluteDate(2004, 8, 16, 20, 0, 0, TimeScalesFactory.getUTC());

        for (double dt = 0; dt < Constants.JULIAN_DAY; dt += 10) {
            final AbsoluteDate date = date0.shiftedBy(dt);
            final double interpolationErrorUT1 = h1.getUT1MinusUTC(date) - h2.getUT1MinusUTC(date);
            final double interpolationErrorLOD = h1.getLOD(date)         - h2.getLOD(date);
            final PoleCorrection p1 = h1.getPoleCorrection(date);
            final PoleCorrection p2 = h2.getPoleCorrection(date);
            final double interpolationErrorXp  = (p1.getXp() - p2.getXp()) / Constants.ARC_SECONDS_TO_RADIANS;
            final double interpolationErrorYp  = (p1.getYp() - p2.getYp()) / Constants.ARC_SECONDS_TO_RADIANS;
            Assertions.assertEquals(0.0, interpolationErrorUT1, 1.2e-10); // seconds
            Assertions.assertEquals(0.0, interpolationErrorLOD, 1.5e-9);  // seconds
            Assertions.assertEquals(0.0, interpolationErrorXp,  2.3e-9);  // arcseconds
            Assertions.assertEquals(0.0, interpolationErrorYp,  1.5e-9);  // arcseconds
        }

    }

    @Test
    public void testCombineIncompatibleEntries() {
        final DateComponents dateA         = new DateComponents(DateComponents.MODIFIED_JULIAN_EPOCH, 56557);
        final DateComponents publicationAT = new DateComponents(DateComponents.MODIFIED_JULIAN_EPOCH, 56561);
        final DateComponents publicationAN = new DateComponents(DateComponents.MODIFIED_JULIAN_EPOCH, 56571);
        final DateComponents publicationAC = new DateComponents(DateComponents.MODIFIED_JULIAN_EPOCH, 56581);
        final DateComponents dateB         = new DateComponents(DateComponents.MODIFIED_JULIAN_EPOCH, 56562);
        final DateComponents publicationBT = new DateComponents(DateComponents.MODIFIED_JULIAN_EPOCH, 56568);
        final DateComponents publicationBN = new DateComponents(DateComponents.MODIFIED_JULIAN_EPOCH, 56578);
        final DateComponents publicationBC = new DateComponents(DateComponents.MODIFIED_JULIAN_EPOCH, 56588);
        try {
            final EOPEntry entryA =
                new EOPEntry(dateA.getMJD(), 1.0e-3, 1.0e-3,
                             1.0e-3, 1.0e-3, 1.0e-3, 1.0e-3,
                             1.0e-3, 1.0e-3, 1.0e-3, 1.0e-3,
                             ITRFVersion.ITRF_2020,
                             new AbsoluteDate(dateA, TimeScalesFactory.getUTC()), EopDataType.RAPID,
                             publicationAT.getMJD(), publicationAN.getMJD(), publicationAC.getMJD());
            final EOPEntry entryB =
                new EOPEntry(dateB.getMJD(), 2.0e-3, 2.0e-3,
                             2.0e-3, 2.0e-3, 2.0e-3, 2.0e-3,
                             2.0e-3, 2.0e-3, 2.0e-3, 2.0e-3,
                             ITRFVersion.ITRF_2014,
                             new AbsoluteDate(dateB, TimeScalesFactory.getUTC()), EopDataType.FINAL,
                             publicationBT.getMJD(), publicationBN.getMJD(), publicationBC.getMJD());
            new EOPEntry(entryA, entryB);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.INCOMPATIBLE_EARTH_ORIENTATION_PARAMETERS, oe.getSpecifier());
            Assertions.assertEquals(dateA, oe.getParts()[0]);
            Assertions.assertEquals(dateB, oe.getParts()[1]);
        }
    }

    @Test
    public void testCombineChronological() {
        final DateComponents date          = new DateComponents(DateComponents.MODIFIED_JULIAN_EPOCH, 56557);
        final DateComponents publicationAT = new DateComponents(DateComponents.MODIFIED_JULIAN_EPOCH, 56561);
        final DateComponents publicationAN = new DateComponents(DateComponents.MODIFIED_JULIAN_EPOCH, 56571);
        final DateComponents publicationAC = new DateComponents(DateComponents.MODIFIED_JULIAN_EPOCH, 56581);
        final DateComponents publicationBT = new DateComponents(DateComponents.MODIFIED_JULIAN_EPOCH, 56568);
        final DateComponents publicationBN = new DateComponents(DateComponents.MODIFIED_JULIAN_EPOCH, 56578);
        final DateComponents publicationBC = new DateComponents(DateComponents.MODIFIED_JULIAN_EPOCH, 56588);
        final EOPEntry entryA =
            new EOPEntry(date.getMJD(), 1.0e-3, 1.0e-3,
                         Double.NaN, 1.0e-3, Double.NaN, 1.0e-3,
                         1.0e-3, 1.0e-3, 1.0e-3, 1.0e-3,
                         ITRFVersion.ITRF_2020,
                         new AbsoluteDate(date, TimeScalesFactory.getUTC()), EopDataType.RAPID,
                         publicationAT.getMJD(), publicationAN.getMJD(), publicationAC.getMJD());
        final EOPEntry entryB =
            new EOPEntry(date.getMJD(), 2.0e-3, 2.0e-3,
                         2.0e-3, Double.NaN, Double.NaN, 2.0e-3,
                         2.0e-3, 2.0e-3, 2.0e-3, 2.0e-3,
                         ITRFVersion.ITRF_2014,
                         new AbsoluteDate(date, TimeScalesFactory.getUTC()), EopDataType.FINAL,
                         publicationBT.getMJD(), publicationBN.getMJD(), publicationBC.getMJD());
        final EOPEntry combined = new EOPEntry(entryA, entryB);
        Assertions.assertEquals(date.getMJD(), combined.getMjd());
        Assertions.assertEquals(new AbsoluteDate(date, TimeScalesFactory.getUTC()), combined.getDate());
        Assertions.assertEquals(entryB.getUT1MinusUTC(), combined.getUT1MinusUTC(), 1.0e-15);
        Assertions.assertEquals(entryB.getLOD(),         combined.getLOD(),         1.0e-15);
        Assertions.assertEquals(entryB.getX(),           combined.getX(),           1.0e-15);
        Assertions.assertEquals(entryA.getY(),           combined.getY(),           1.0e-15);
        Assertions.assertTrue(Double.isNaN(entryB.getXRate()));
        Assertions.assertEquals(entryB.getYRate(),       combined.getYRate(),       1.0e-15);
        Assertions.assertEquals(entryB.getDdPsi(),       combined.getDdPsi(),       1.0e-15);
        Assertions.assertEquals(entryB.getDdEps(),       combined.getDdEps(),       1.0e-15);
        Assertions.assertEquals(entryB.getDx(),          combined.getDx(),          1.0e-15);
        Assertions.assertEquals(entryB.getDy(),          combined.getDy(),          1.0e-15);
        Assertions.assertEquals(entryB.getITRFType(),    combined.getITRFType());
        Assertions.assertEquals(entryB.getEopDataType(), combined.getEopDataType());
        Assertions.assertEquals(publicationBT.getMJD(),  combined.getDtPub());
        Assertions.assertEquals(publicationBN.getMJD(),  combined.getNutPub());
        Assertions.assertEquals(publicationBC.getMJD(),  combined.getCipPub());
    }

    @Test
    public void testCombineReverseChronological() {
        final DateComponents date          = new DateComponents(DateComponents.MODIFIED_JULIAN_EPOCH, 56557);
        final DateComponents publicationAT = new DateComponents(DateComponents.MODIFIED_JULIAN_EPOCH, 56561);
        final DateComponents publicationAN = new DateComponents(DateComponents.MODIFIED_JULIAN_EPOCH, 56571);
        final DateComponents publicationAC = new DateComponents(DateComponents.MODIFIED_JULIAN_EPOCH, 56581);
        final DateComponents publicationBT = new DateComponents(DateComponents.MODIFIED_JULIAN_EPOCH, 56568);
        final DateComponents publicationBN = new DateComponents(DateComponents.MODIFIED_JULIAN_EPOCH, 56578);
        final DateComponents publicationBC = new DateComponents(DateComponents.MODIFIED_JULIAN_EPOCH, 56588);
        final EOPEntry entryA =
            new EOPEntry(date.getMJD(), 1.0e-3, 1.0e-3,
                         Double.NaN, 1.0e-3, Double.NaN, 1.0e-3,
                         1.0e-3, 1.0e-3, 1.0e-3, 1.0e-3,
                         ITRFVersion.ITRF_2020,
                         new AbsoluteDate(date, TimeScalesFactory.getUTC()), EopDataType.RAPID,
                         publicationAT.getMJD(), publicationAN.getMJD(), publicationAC.getMJD());
        final EOPEntry entryB =
            new EOPEntry(date.getMJD(), 2.0e-3, 2.0e-3,
                         2.0e-3, Double.NaN, Double.NaN, 2.0e-3,
                         2.0e-3, 2.0e-3, 2.0e-3, 2.0e-3,
                         ITRFVersion.ITRF_2014,
                         new AbsoluteDate(date, TimeScalesFactory.getUTC()), EopDataType.FINAL,
                         publicationBT.getMJD(), publicationBN.getMJD(), publicationBC.getMJD());
        final EOPEntry combined = new EOPEntry(entryB, entryA);
        Assertions.assertEquals(date.getMJD(), combined.getMjd());
        Assertions.assertEquals(new AbsoluteDate(date, TimeScalesFactory.getUTC()), combined.getDate());
        Assertions.assertEquals(entryB.getUT1MinusUTC(), combined.getUT1MinusUTC(), 1.0e-15);
        Assertions.assertEquals(entryB.getLOD(),         combined.getLOD(),         1.0e-15);
        Assertions.assertEquals(entryB.getX(),           combined.getX(),           1.0e-15);
        Assertions.assertEquals(entryA.getY(),           combined.getY(),           1.0e-15);
        Assertions.assertTrue(Double.isNaN(entryB.getXRate()));
        Assertions.assertEquals(entryB.getYRate(),       combined.getYRate(),       1.0e-15);
        Assertions.assertEquals(entryB.getDdPsi(),       combined.getDdPsi(),       1.0e-15);
        Assertions.assertEquals(entryB.getDdEps(),       combined.getDdEps(),       1.0e-15);
        Assertions.assertEquals(entryB.getDx(),          combined.getDx(),          1.0e-15);
        Assertions.assertEquals(entryB.getDy(),          combined.getDy(),          1.0e-15);
        Assertions.assertEquals(entryB.getITRFType(),    combined.getITRFType());
        Assertions.assertEquals(entryB.getEopDataType(), combined.getEopDataType());
        Assertions.assertEquals(publicationBT.getMJD(),  combined.getDtPub());
        Assertions.assertEquals(publicationBN.getMJD(),  combined.getNutPub());
        Assertions.assertEquals(publicationBC.getMJD(),  combined.getCipPub());
    }

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

}
