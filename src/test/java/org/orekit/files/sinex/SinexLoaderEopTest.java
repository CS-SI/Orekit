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

package org.orekit.files.sinex;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.data.DataContext;
import org.orekit.frames.EOPEntry;
import org.orekit.frames.EOPHistory;
import org.orekit.frames.ITRFVersion;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.ChronologicalComparator;
import org.orekit.time.DateComponents;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.units.Unit;
import org.orekit.utils.units.UnitsConverter;

import java.util.Arrays;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

public class SinexLoaderEopTest {

    private TimeScale utc;

    @BeforeEach
    public void setUp() {
        // Sets the root of data to read
        Utils.setDataRoot("gnss:sinex");
        // Setup utc for defining dates
        utc = TimeScalesFactory.getUTC();
    }

    @Test
    // Check the behaviour for a simpl Sinex file containing EOP data
    public void testSmallIGSSinexEopFile() {

        // Setting up the Sinex Loader
        SinexLoader loader = new SinexLoader("cod20842-small.snx");
        loader.setITRFVersion(2014);

        // Extracting the data parsed in the Sinex loader to fill the history set
        IERSConventions.NutationCorrectionConverter converter =
                IERSConventions.IERS_2010.getNutationCorrectionConverter();
        SortedSet<EOPEntry> history = new TreeSet<EOPEntry>(new ChronologicalComparator());
        loader.fillHistory(converter, history);

        final UnitsConverter unitConvRad = new UnitsConverter(Unit.parse("mas"), Unit.RADIAN);
        AbsoluteDate date1 = new AbsoluteDate(new DateComponents(2019, 1, 1), utc).shiftedBy(Constants.JULIAN_DAY * (350));
        AbsoluteDate date2 = new AbsoluteDate(new DateComponents(2019, 1, 1), utc).shiftedBy(Constants.JULIAN_DAY * (352));

        // Check size of set
        Assertions.assertEquals(4, history.size());
        Assertions.assertEquals(4, loader.getParsedEop().size());

        // Test if the values are correctly extracted
        EOPEntry firstEntry = history.first();
        Assertions.assertEquals(unitConvRad.convert(0.101379061387836E+03), firstEntry.getX(), 1e-15);
        Assertions.assertEquals(unitConvRad.convert(0.274820464392703E+03), firstEntry.getY(), 1e-15);
        Assertions.assertEquals(-0.172036907064256E+03, firstEntry.getUT1MinusUTC() * 1000, 1e-15);

        // Test if a valid EOPHistory object can be built
        EOPHistory eopHistory = new EOPHistory(IERSConventions.IERS_2010, EOPHistory.DEFAULT_INTERPOLATION_DEGREE,
                                               history, true, DataContext.getDefault().getTimeScales());
        Assertions.assertEquals(-0.172046001405041, eopHistory.getUT1MinusUTC(date1), 1e-15);
        Assertions.assertEquals(unitConvRad.convert(0.994323310003336E+02), eopHistory.getPoleCorrection(date1).getXp(), 1e-15);
        Assertions.assertEquals(unitConvRad.convert(0.275001985187467E+03), eopHistory.getPoleCorrection(date1).getYp(), 1e-15);
        Assertions.assertEquals(0, eopHistory.getEquinoxNutationCorrection(date1)[0], 1e-15);
        Assertions.assertEquals(0, eopHistory.getEquinoxNutationCorrection(date1)[1], 1e-15);
        Assertions.assertEquals(0, eopHistory.getNonRotatinOriginNutationCorrection(date1)[0], 1e-15);
        Assertions.assertEquals(0, eopHistory.getNonRotatinOriginNutationCorrection(date1)[1], 1e-15);

        Assertions.assertEquals(-.172731650844468, eopHistory.getUT1MinusUTC(date2), 1e-15);
        Assertions.assertEquals(unitConvRad.convert(0.958035125590580E+02), eopHistory.getPoleCorrection(date2).getXp(), 1e-15);
        Assertions.assertEquals(unitConvRad.convert(0.275653571617736E+03), eopHistory.getPoleCorrection(date2).getYp(), 1e-15);
        Assertions.assertEquals(0, eopHistory.getEquinoxNutationCorrection(date2)[0], 1e-15);
        Assertions.assertEquals(0, eopHistory.getEquinoxNutationCorrection(date2)[1], 1e-15);
        Assertions.assertEquals(0, eopHistory.getNonRotatinOriginNutationCorrection(date2)[0], 1e-15);
        Assertions.assertEquals(0, eopHistory.getNonRotatinOriginNutationCorrection(date2)[1], 1e-15);

        // Checking start and end dates
        // 19:350:00000
        AbsoluteDate firstDate = new AbsoluteDate(new DateComponents(2019, 1, 1), utc).shiftedBy(Constants.JULIAN_DAY * (349));
        Assertions.assertEquals(firstDate, eopHistory.getStartDate());
        // 19:353:00000
        AbsoluteDate endDate = new AbsoluteDate(new DateComponents(2019, 1, 1), utc).shiftedBy(Constants.JULIAN_DAY * (352));
        Assertions.assertEquals(endDate, eopHistory.getEndDate());

    }


    @Test
    // Tests to go through the different branches of the IF-ELSE used to differentiate cases on the presence of
    // nutation data. (NUT_X, NUT_Y, NUT_LN, NUT_OB)
    // Case NUT_X, NUT_Y != null, NUT_LN, NUT_OB == null
    public void testSmallSinexEopSynth1File() {

        // Setting up the Sinex loader
        SinexLoader loader = new SinexLoader("cod20842-small-synthEOP.snx");
        loader.setITRFVersion(2014);

        // Extracting the data parsed in the Sinex loader to fill the history set
        IERSConventions.NutationCorrectionConverter converter =
                IERSConventions.IERS_2010.getNutationCorrectionConverter();
        SortedSet<EOPEntry> history = new TreeSet<EOPEntry>(new ChronologicalComparator());
        loader.fillHistory(converter, history);

        final UnitsConverter unitConvRad = new UnitsConverter(Unit.parse("mas"), Unit.RADIAN);

        // Setting up the date, and generating elements not present at first for check
        AbsoluteDate date = new AbsoluteDate(new DateComponents(2019, 1, 1), utc).shiftedBy(Constants.JULIAN_DAY * 350 + 43185.0);

        // Test if the values are correctly extracted
        EOPEntry firstEntry = history.first();
        double xPo = unitConvRad.convert(7.68783442726072E+01);
        double yPo = unitConvRad.convert(3.47286203337827E+02);
        double nutX = unitConvRad.convert(-1.10122731910265E+03);
        double nutY = unitConvRad.convert(-4.00387630903350E+03);
        double[] equinox = converter.toEquinox(date, nutX, nutY);
        Assertions.assertEquals(xPo, firstEntry.getX(), 1e-15);
        Assertions.assertEquals(yPo, firstEntry.getY(), 1e-15);
        Assertions.assertEquals(-3.17284190690589E+04, firstEntry.getUT1MinusUTC() * 1000, 1e-15);
        Assertions.assertEquals(1.32354538674901E+00, firstEntry.getLOD() * 1000, 1e-15);
        Assertions.assertEquals(nutX, firstEntry.getDx(), 1e-15);
        Assertions.assertEquals(nutY, firstEntry.getDy(), 1e-15);
        Assertions.assertEquals(equinox[0], firstEntry.getDdPsi(), 4.5e-11);
        Assertions.assertEquals(equinox[1], firstEntry.getDdEps(), 4.8e-12);

        // Test if a valid EOPHistory object can be built
        EOPHistory eopHistory = new EOPHistory(IERSConventions.IERS_2010, EOPHistory.DEFAULT_INTERPOLATION_DEGREE,
                                               history, true, DataContext.getDefault().getTimeScales());

        Assertions.assertEquals(unitConvRad.convert(7.68783442726072E+01), eopHistory.getPoleCorrection(date.shiftedBy(0)).getXp(), 1e-15);
        Assertions.assertEquals(unitConvRad.convert(3.47286203337827E+02), eopHistory.getPoleCorrection(date.shiftedBy(-1)).getYp(), 1e-15);
        Assertions.assertEquals(-3.17284190690589E+04, eopHistory.getUT1MinusUTC(date.shiftedBy(10)) * 1000, 1e-15);
        Assertions.assertEquals( 1.32354538674901E+00, eopHistory.getLOD(date.shiftedBy(10))* 1000, 1e-15);
        Assertions.assertEquals(unitConvRad.convert(-1.10122731910265E+03), eopHistory.getNonRotatinOriginNutationCorrection(date.shiftedBy(10))[0], 1e-15);
        Assertions.assertEquals(unitConvRad.convert(-4.00387630903350E+03), eopHistory.getNonRotatinOriginNutationCorrection(date.shiftedBy(10))[1], 1e-15);
        Assertions.assertEquals(equinox[0], eopHistory.getEquinoxNutationCorrection(date.shiftedBy(10))[0], 4.5e-11);
        Assertions.assertEquals(equinox[1], eopHistory.getEquinoxNutationCorrection(date.shiftedBy(10))[1], 4.8e-12);
    }

    @Test
    // Case NUT_X, NUT_Y != null, NUT_LN, NUT_OB != null
    public void testSmallSinexEopSynth2File() {
        // Setting up the Sinex loader
        SinexLoader loader = new SinexLoader("cod20842-small-synthEOP2.snx");
        loader.setITRFVersion(2014);

        // Extracting the data parsed in the Sinex loader to fill the history set
        IERSConventions.NutationCorrectionConverter converter =
                IERSConventions.IERS_2010.getNutationCorrectionConverter();
        SortedSet<EOPEntry> history = new TreeSet<EOPEntry>(new ChronologicalComparator());
        loader.fillHistory(converter, history);

        // Setting up the date
        AbsoluteDate date = new AbsoluteDate(new DateComponents(2019, 1, 1), utc).shiftedBy(Constants.JULIAN_DAY * (351 - 1)).shiftedBy(43185);
        final UnitsConverter unitConvRad = new UnitsConverter(Unit.parse("mas"), Unit.RADIAN);
        EOPEntry firstEntry = history.first();

        Assertions.assertEquals(unitConvRad.convert(7.68783442726072E+01), firstEntry.getX(), 1e-15);
        Assertions.assertEquals(unitConvRad.convert(3.47286203337827E+02), firstEntry.getY(), 1e-15);
        Assertions.assertEquals(-3.17284190690589E+04, firstEntry.getUT1MinusUTC() * 1000, 1e-15);
        Assertions.assertEquals(1.32354538674901E+00, firstEntry.getLOD() * 1000, 1e-15);
        Assertions.assertEquals(unitConvRad.convert(-1.10122731910265E+03), firstEntry.getDx(), 1e-15);
        Assertions.assertEquals(unitConvRad.convert(-4.00387630903350E+03), firstEntry.getDy(), 1e-15);
        Assertions.assertEquals(unitConvRad.convert(-1.10122731910265E+03), firstEntry.getDdPsi(), 1e-15);
        Assertions.assertEquals(unitConvRad.convert(-4.00387630903350E+03), firstEntry.getDdEps(), 1e-15);

        DataContext.getDefault().getFrames().addEOPHistoryLoader(IERSConventions.IERS_2010, loader);
        EOPHistory eopHistory =DataContext.getDefault().getFrames().getEOPHistory(IERSConventions.IERS_2010, true);

        Assertions.assertEquals(unitConvRad.convert(7.68783442726072E+01), eopHistory.getPoleCorrection(date.shiftedBy(10)).getXp(), 1e-15);
        Assertions.assertEquals(unitConvRad.convert(3.47286203337827E+02), eopHistory.getPoleCorrection(date.shiftedBy(10)).getYp(), 1e-15);
        Assertions.assertEquals(-3.17284190690589E+04, eopHistory.getUT1MinusUTC(date.shiftedBy(10)) * 1000, 1e-15);
        Assertions.assertEquals( 1.32354538674901E+00, eopHistory.getLOD(date.shiftedBy(10))* 1000, 1e-15);
        Assertions.assertEquals(unitConvRad.convert(-1.10122731910265E+03), eopHistory.getNonRotatinOriginNutationCorrection(date.shiftedBy(10))[0], 1e-15);
        Assertions.assertEquals(unitConvRad.convert(-4.00387630903350E+03), eopHistory.getNonRotatinOriginNutationCorrection(date.shiftedBy(10))[1], 1e-15);
        Assertions.assertEquals(unitConvRad.convert(-1.10122731910265E+03), eopHistory.getEquinoxNutationCorrection(date.shiftedBy(10))[0], 1e-15);
        Assertions.assertEquals(unitConvRad.convert(-4.00387630903350E+03), eopHistory.getEquinoxNutationCorrection(date.shiftedBy(10))[1], 1e-15);
    }

    @Test
    // Case NUT_X, NUT_OB != null, NUT_LN, NUT_Y == null
    public void testSmallSinexEopSynth3File() {

        // Setting up the Sinex loader
        SinexLoader loader = new SinexLoader("cod20842-small-synthEOP3.snx");
        loader.setITRFVersion(2014);

        IERSConventions.NutationCorrectionConverter converter =
                IERSConventions.IERS_2010.getNutationCorrectionConverter();
        SortedSet<EOPEntry> history = new TreeSet<EOPEntry>(new ChronologicalComparator());

        loader.fillHistory(converter, history);
        final UnitsConverter unitConvRad = new UnitsConverter(Unit.parse("mas"), Unit.RADIAN);

        AbsoluteDate date = new AbsoluteDate(new DateComponents(2019, 1, 1), utc).shiftedBy(Constants.JULIAN_DAY * (351 - 1)).shiftedBy(43185);
        double[] nro = converter.toNonRotating(date, unitConvRad.convert(-1.10122731910265E+03), unitConvRad.convert(-4.00387630903350E+03));

        EOPEntry firstEntry = history.first();

        Assertions.assertEquals(unitConvRad.convert(7.68783442726072E+01), firstEntry.getX(), 1e-15);
        Assertions.assertEquals(unitConvRad.convert(3.47286203337827E+02), firstEntry.getY(), 1e-15);
        Assertions.assertEquals(-3.17284190690589E+04, firstEntry.getUT1MinusUTC() * 1000, 1e-15);
        Assertions.assertEquals(1.32354538674901E+00, firstEntry.getLOD() * 1000, 1e-15);
        Assertions.assertEquals(unitConvRad.convert(-1.10122731910265E+03), firstEntry.getDdPsi(), 1e-15);
        Assertions.assertEquals(unitConvRad.convert(-4.00387630903350E+03), firstEntry.getDdEps(), 1e-15);
        Assertions.assertEquals(nro[0], firstEntry.getDx(), 1.8e-11);
        Assertions.assertEquals(nro[1], firstEntry.getDy(), 2.0e-12);

    }

    @Test
    // Test the number of EOP entries and epochs
    public void testEpochsInFile() {
        // Setting up the Sinex loader
        SinexLoader loader = new SinexLoader("cod_ifCloseEnd.snx");
        loader.setITRFVersion(2014);
        Assertions.assertEquals(ITRFVersion.ITRF_2014, loader.getITRFVersion());

        IERSConventions.NutationCorrectionConverter converter =
                IERSConventions.IERS_2010.getNutationCorrectionConverter();
        SortedSet<EOPEntry> history = new TreeSet<EOPEntry>(new ChronologicalComparator());
        loader.fillHistory(converter, history);

        AbsoluteDate dateStart = new AbsoluteDate(new DateComponents(2019, 1, 1), utc).shiftedBy(Constants.JULIAN_DAY * 350);
        AbsoluteDate dateStartPlusOne = dateStart.shiftedBy(+1.0);
        AbsoluteDate dateInFile = new AbsoluteDate(new DateComponents(2019, 1, 1), utc).shiftedBy(Constants.JULIAN_DAY * 350 + 45000.0);
        AbsoluteDate dateEnd = new AbsoluteDate(new DateComponents(2019, 1, 1), utc).shiftedBy(Constants.JULIAN_DAY * 351);
        AbsoluteDate dateEndMinusOne = dateEnd.shiftedBy(-1.0);

        List<AbsoluteDate> listDates = Arrays.asList(dateStart, dateStartPlusOne, dateInFile, dateEndMinusOne, dateEnd);

        int cpt = 0;
        for (EOPEntry entry : history) {
            Assertions.assertEquals(listDates.get(cpt), entry.getDate());
            cpt = cpt+1;
        }

    }

    @Test
    // Check the behaviour of the SinexLoader when given a regex leading to multiple files to parse, with consistent dates for EOP entries.
    public void testSmallSinexEopSynthMultiFile() {

        String supportedNames = "^(cod_test.+)";

        SinexLoader loader = new SinexLoader(supportedNames);
        loader.setITRFVersion(2014);
        Assertions.assertEquals(ITRFVersion.ITRF_2014, loader.getITRFVersion());

        IERSConventions.NutationCorrectionConverter converter =
                IERSConventions.IERS_2010.getNutationCorrectionConverter();
        SortedSet<EOPEntry> history = new TreeSet<EOPEntry>(new ChronologicalComparator());

        loader.fillHistory(converter, history);

        final UnitsConverter unitConvRad = new UnitsConverter(Unit.parse("mas"), Unit.RADIAN);

        EOPEntry firstEntry = history.first();

        Assertions.assertEquals(unitConvRad.convert(7.68783442726072E+01), firstEntry.getX(), 1e-15);
        Assertions.assertEquals(unitConvRad.convert(3.47286203337827E+02), firstEntry.getY(), 1e-15);
        Assertions.assertEquals(-3.17284190690589E+04, firstEntry.getUT1MinusUTC() * 1000, 1e-15);
        Assertions.assertEquals(1.32354538674901E+00, firstEntry.getLOD() * 1000, 1e-15);
        Assertions.assertEquals(unitConvRad.convert(-1.10122731910265E+03), firstEntry.getDx(), 1e-15);
        Assertions.assertEquals(unitConvRad.convert(-4.00387630903350E+03), firstEntry.getDy(), 1e-15);
    }


    @Test
    // Check the behaviour of the LazyLoadedEop class used in the Default DataContext case, with multiple Sinex files.
    // We suppose the dates are not overlapping, and are consistent with the header of each file.
    public void testSmallSinexEopSynthMultiLoader() {

        // Setting the loaders
        String sinex1 = "cod_test_1.snx";
        String sinex2 = "cod_test_2.snx";
        String sinex3 = "cod_test_3.snx";

        SinexLoader loader1 = new SinexLoader(sinex1);
        SinexLoader loader2 = new SinexLoader(sinex2);
        SinexLoader loader3 = new SinexLoader(sinex3);

        loader1.setITRFVersion(2014);
        loader2.setITRFVersion(2014);
        loader3.setITRFVersion(2014);

        // Setting the DataContext to extract the EOP data from the 3 SinexLoader objects
        DataContext.getDefault().getFrames().addEOPHistoryLoader(IERSConventions.IERS_2010, loader1);
        DataContext.getDefault().getFrames().addEOPHistoryLoader(IERSConventions.IERS_2010, loader2);
        DataContext.getDefault().getFrames().addEOPHistoryLoader(IERSConventions.IERS_2010, loader3);

        // Generate the EOPHistory
        EOPHistory eopHistory  = DataContext.getDefault().getFrames().getEOPHistory(IERSConventions.IERS_2010, true);

        // Setting up dates for further checks
        AbsoluteDate startDate = new AbsoluteDate(new DateComponents(2019, 1, 1), utc).shiftedBy(Constants.JULIAN_DAY * (350));
        AbsoluteDate startDatePlusOne = startDate.shiftedBy(+1.0);
        AbsoluteDate endDate = new AbsoluteDate(new DateComponents(2019, 1, 1), utc).shiftedBy(Constants.JULIAN_DAY * (353));
        AbsoluteDate endDateMinusOne = endDate.shiftedBy(-1.0);

        AbsoluteDate date  = new AbsoluteDate(new DateComponents(2019, 1, 1), utc).shiftedBy(Constants.JULIAN_DAY * 350 + 43185.0);
        AbsoluteDate date2 = new AbsoluteDate(new DateComponents(2019, 1, 1), utc).shiftedBy(Constants.JULIAN_DAY * 351 + 43185.0);
        AbsoluteDate date3 = new AbsoluteDate(new DateComponents(2019, 1, 1), utc).shiftedBy(Constants.JULIAN_DAY * 352 + 43185.0);

        // Intermediate shared date between two files
        AbsoluteDate dateI12 = new AbsoluteDate(new DateComponents(2019, 1, 1), utc).shiftedBy(Constants.JULIAN_DAY * (352 - 1)).shiftedBy(0);
        AbsoluteDate dateI12MinusOne = dateI12.shiftedBy(-1.0);
        AbsoluteDate dateI12PlusOne = dateI12.shiftedBy(+1.0);
        AbsoluteDate dateI23 = new AbsoluteDate(new DateComponents(2019, 1, 1), utc).shiftedBy(Constants.JULIAN_DAY * (353 - 1)).shiftedBy(0);
        AbsoluteDate dateI23MinusOne = dateI23.shiftedBy(-1.0);
        AbsoluteDate dateI23PlusOne = dateI23.shiftedBy(+1.0);

        List<AbsoluteDate> listDates = Arrays.asList(startDate, startDatePlusOne, date,
                                                     dateI12MinusOne, dateI12, dateI12PlusOne, date2,
                                                     dateI23MinusOne, dateI23, dateI23PlusOne, date3,
                                                     endDateMinusOne, endDate);
        // Simplify checks to stay in the units of Orekit
        final UnitsConverter unitConvRad = new UnitsConverter(Unit.parse("mas"), Unit.RADIAN);

        // Check dates
        int cpt = 0;
        for (EOPEntry entry : eopHistory.getEntries()) {
            Assertions.assertEquals(listDates.get(cpt), entry.getDate());
            cpt = cpt+1;
        }

        // First Entry
        double shift  = 0;
        Assertions.assertEquals(unitConvRad.convert(7.68783442726072E+01), eopHistory.getPoleCorrection(startDate).getXp(), 1e-15);
        Assertions.assertEquals(unitConvRad.convert(3.47286203337827E+02), eopHistory.getPoleCorrection(startDate).getYp(), 1e-15);
        Assertions.assertEquals(-3.17284190690589E+04, eopHistory.getUT1MinusUTC(startDate) * 1000, 1e-15);
        Assertions.assertEquals( 1.32354538674901E+00, eopHistory.getLOD(startDate)* 1000, 1e-15);
        Assertions.assertEquals(unitConvRad.convert(-1.10122731910265E+03), eopHistory.getNonRotatinOriginNutationCorrection(startDate)[0], 1e-15);
        Assertions.assertEquals(unitConvRad.convert(-4.00387630903350E+03), eopHistory.getNonRotatinOriginNutationCorrection(startDate)[1], 1e-15);

        Assertions.assertEquals(unitConvRad.convert(7.68783442726072E+01), eopHistory.getPoleCorrection(date.shiftedBy(shift)).getXp(), 1e-15);
        Assertions.assertEquals(unitConvRad.convert(3.47286203337827E+02), eopHistory.getPoleCorrection(date.shiftedBy(shift)).getYp(), 1e-15);
        Assertions.assertEquals(-3.17284190690589E+04, eopHistory.getUT1MinusUTC(date.shiftedBy(shift)) * 1000, 1e-15);
        Assertions.assertEquals( 1.32354538674901E+00, eopHistory.getLOD(date.shiftedBy(shift))* 1000, 1e-15);
        Assertions.assertEquals(unitConvRad.convert(-1.10122731910265E+03), eopHistory.getNonRotatinOriginNutationCorrection(date.shiftedBy(shift))[0], 1e-15);
        Assertions.assertEquals(unitConvRad.convert(-4.00387630903350E+03), eopHistory.getNonRotatinOriginNutationCorrection(date.shiftedBy(shift))[1], 1e-15);

        // Last entry for 1st file
        Assertions.assertEquals(unitConvRad.convert(7.68783442726072E+01), eopHistory.getPoleCorrection(dateI12).getXp(), 1e-15);
        Assertions.assertEquals(unitConvRad.convert(3.47286203337827E+02), eopHistory.getPoleCorrection(dateI12).getYp(), 1e-15);
        Assertions.assertEquals(-3.17284190690589E+04, eopHistory.getUT1MinusUTC(dateI12) * 1000, 1e-15);
        Assertions.assertEquals( 1.32354538674901E+00, eopHistory.getLOD(dateI12)* 1000, 1e-15);
        Assertions.assertEquals(unitConvRad.convert(-1.10122731910265E+03), eopHistory.getNonRotatinOriginNutationCorrection(dateI12)[0], 1e-15);
        Assertions.assertEquals(unitConvRad.convert(-4.00387630903350E+03), eopHistory.getNonRotatinOriginNutationCorrection(dateI12)[1], 1e-15);

        // Second Entry
        Assertions.assertEquals(unitConvRad.convert(6.68783442726072E+01), eopHistory.getPoleCorrection(date2).getXp(), 1e-15);
        Assertions.assertEquals(unitConvRad.convert(2.47286203337827E+02), eopHistory.getPoleCorrection(date2).getYp(), 1e-15);
        Assertions.assertEquals(-4.17284190690589E+04, eopHistory.getUT1MinusUTC(date2) * 1000, 1e-15);
        Assertions.assertEquals( 2.32354538674901E+00, eopHistory.getLOD(date2)* 1000, 1e-15);
        Assertions.assertEquals(unitConvRad.convert(-2.10122731910265E+03), eopHistory.getNonRotatinOriginNutationCorrection(date2)[0], 1e-15);
        Assertions.assertEquals(unitConvRad.convert(-5.00387630903350E+03), eopHistory.getNonRotatinOriginNutationCorrection(date2)[1], 1e-15);

        // Between second and third file
        Assertions.assertEquals(unitConvRad.convert(6.68783442726072E+01), eopHistory.getPoleCorrection(dateI23).getXp(), 1e-15);
        Assertions.assertEquals(unitConvRad.convert(2.47286203337827E+02), eopHistory.getPoleCorrection(dateI23).getYp(), 1e-15);
        Assertions.assertEquals(-4.17284190690589E+04, eopHistory.getUT1MinusUTC(dateI23) * 1000, 1e-15);
        Assertions.assertEquals( 2.32354538674901E+00, eopHistory.getLOD(dateI23)* 1000, 1e-15);
        Assertions.assertEquals(unitConvRad.convert(-2.10122731910265E+03), eopHistory.getNonRotatinOriginNutationCorrection(dateI23)[0], 1e-15);
        Assertions.assertEquals(unitConvRad.convert(-5.00387630903350E+03), eopHistory.getNonRotatinOriginNutationCorrection(dateI23)[1], 1e-15);

        // Third file main entry
        Assertions.assertEquals(unitConvRad.convert(5.68783442726072E+01), eopHistory.getPoleCorrection(date3).getXp(), 1e-15);
        Assertions.assertEquals(unitConvRad.convert(1.47286203337827E+02), eopHistory.getPoleCorrection(date3).getYp(), 1e-15);
        Assertions.assertEquals(-5.17284190690589E+04, eopHistory.getUT1MinusUTC(date3) * 1000, 1e-15);
        Assertions.assertEquals( 3.32354538674901E+00, eopHistory.getLOD(date3)* 1000, 1e-15);
        Assertions.assertEquals(unitConvRad.convert(-3.10122731910265E+03), eopHistory.getNonRotatinOriginNutationCorrection(date3)[0], 1e-15);
        Assertions.assertEquals(unitConvRad.convert(-6.00387630903350E+03), eopHistory.getNonRotatinOriginNutationCorrection(date3)[1], 1e-15);

        // Last entry
        Assertions.assertEquals(unitConvRad.convert(5.68783442726072E+01), eopHistory.getPoleCorrection(endDate).getXp(), 1e-15);
        Assertions.assertEquals(unitConvRad.convert(1.47286203337827E+02), eopHistory.getPoleCorrection(endDate).getYp(), 1e-15);
        Assertions.assertEquals(-5.17284190690589E+01, eopHistory.getUT1MinusUTC(endDate), 1e-14);
        Assertions.assertEquals( 3.32354538674901E+00, eopHistory.getLOD(endDate)* 1000, 1e-15);
        Assertions.assertEquals(unitConvRad.convert(-3.10122731910265E+03), eopHistory.getNonRotatinOriginNutationCorrection(endDate)[0], 1e-15);
        Assertions.assertEquals(unitConvRad.convert(-6.00387630903350E+03), eopHistory.getNonRotatinOriginNutationCorrection(endDate)[1], 1e-15);

    }

}
