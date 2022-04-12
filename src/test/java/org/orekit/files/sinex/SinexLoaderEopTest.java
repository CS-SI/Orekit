/* Copyright 2002-2022 CS GROUP
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

import java.util.SortedSet;
import java.util.TreeSet;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
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

public class SinexLoaderEopTest {
    
    private TimeScale utc;
    
    @Before
    public void setUp() {
        // Sets the root of data to read
        Utils.setDataRoot("gnss:sinex");
        // Setup utc for defining dates
        utc = TimeScalesFactory.getUTC();
    }
    
    @Test
    public void testSmallIGSSinexEopFile() {

        SinexLoader loader = new SinexLoader("cod20842-small.snx");
        loader.setITRFVersion(2014);
        
        IERSConventions.NutationCorrectionConverter converter =
                IERSConventions.IERS_2010.getNutationCorrectionConverter();
        SortedSet<EOPEntry> history = new TreeSet<EOPEntry>(new ChronologicalComparator());

        loader.fillHistory(converter, history);
        final UnitsConverter unitConvRad = new UnitsConverter(Unit.parse("mas"), Unit.RADIAN);
        
        Assert.assertEquals(4, history.size());
        
        history.forEach(entry -> System.out.println(entry.getDate()));
        
        AbsoluteDate date = new AbsoluteDate(new DateComponents(2019, 1, 1), utc).shiftedBy(Constants.JULIAN_DAY * (351 - 1)).shiftedBy(0);
        
        // Test if the values are correctly extracted
        EOPEntry firstEntry = history.first();
        Assert.assertEquals(unitConvRad.convert(0.101379061387836E+03), firstEntry.getX(), 1e-15);
        Assert.assertEquals(unitConvRad.convert(0.274820464392703E+03), firstEntry.getY(), 1e-15);
        Assert.assertEquals(-0.172036907064256E+03, firstEntry.getUT1MinusUTC() * 1000, 1e-15);
        
        // Test if a valid EOPHistory object can be built
        EOPHistory eopHistory = new EOPHistory(IERSConventions.IERS_2010, history, true, DataContext.getDefault().getTimeScales());
        Assert.assertEquals(-0.172046001405041E+03, eopHistory.getUT1MinusUTC(date)*1000, 1e-15);
        Assert.assertEquals(unitConvRad.convert(0.994323310003336E+02), eopHistory.getPoleCorrection(date).getXp(), 1e-15);
        Assert.assertEquals(unitConvRad.convert(0.275001985187467E+03), eopHistory.getPoleCorrection(date).getYp(), 1e-15);
        Assert.assertEquals(0, eopHistory.getEquinoxNutationCorrection(date)[0], 1e-15);
        Assert.assertEquals(0, eopHistory.getEquinoxNutationCorrection(date)[1], 1e-15);
        Assert.assertEquals(0, eopHistory.getNonRotatinOriginNutationCorrection(date)[0], 1e-15);
        Assert.assertEquals(0, eopHistory.getNonRotatinOriginNutationCorrection(date)[1], 1e-15);
        
        AbsoluteDate firstDate = new AbsoluteDate(new DateComponents(2019, 1, 1), utc).shiftedBy(Constants.JULIAN_DAY * (350 - 1)).shiftedBy(0);
        Assert.assertEquals(firstDate, eopHistory.getStartDate());
        AbsoluteDate endDate = new AbsoluteDate(new DateComponents(2019, 1, 1), utc).shiftedBy(Constants.JULIAN_DAY * (353 - 1)).shiftedBy(0);
        Assert.assertEquals(endDate, eopHistory.getEndDate());
        
        
    }
    
    
    @Test
    public void testSmallSinexEopSynth1File() {

        SinexLoader loader = new SinexLoader("cod20842-small-synthEOP.snx");
        loader.setITRFVersion(2014);
        
        IERSConventions.NutationCorrectionConverter converter =
                IERSConventions.IERS_2010.getNutationCorrectionConverter();
        SortedSet<EOPEntry> history = new TreeSet<EOPEntry>(new ChronologicalComparator());

        loader.fillHistory(converter, history);
        
        final UnitsConverter unitConvRad = new UnitsConverter(Unit.parse("mas"), Unit.RADIAN);

        AbsoluteDate date = new AbsoluteDate(new DateComponents(2019, 1, 1), utc).shiftedBy(Constants.JULIAN_DAY * (351 - 1)).shiftedBy(43185);
        double[] equinox = converter.toEquinox(date, unitConvRad.convert(-1.10122731910265E+03), unitConvRad.convert(-4.00387630903350E+03));
        
        history.forEach(entry -> System.out.println(entry.getDate()));

        // Test if the values are correctly extracted
        EOPEntry firstEntry = history.first();
        Assert.assertEquals(unitConvRad.convert(7.68783442726072E+01), firstEntry.getX(), 1e-15);
        Assert.assertEquals(unitConvRad.convert(3.47286203337827E+02), firstEntry.getY(), 1e-15);
        Assert.assertEquals(-3.17284190690589E+04, firstEntry.getUT1MinusUTC() * 1000, 1e-15);
        Assert.assertEquals(1.32354538674901E+00, firstEntry.getLOD() * 1000, 1e-15);
        Assert.assertEquals(unitConvRad.convert(-1.10122731910265E+03), firstEntry.getDx(), 1e-15);
        Assert.assertEquals(unitConvRad.convert(-4.00387630903350E+03), firstEntry.getDy(), 1e-15);
        Assert.assertEquals(equinox[0], firstEntry.getDdPsi(), 1e-15);
        Assert.assertEquals(equinox[1], firstEntry.getDdEps(), 1e-15);
        
        // Test if a valid EOPHistory object can be built
        EOPHistory eopHistory = new EOPHistory(IERSConventions.IERS_2010, history, true, DataContext.getDefault().getTimeScales());
        
        //Assert.assertEquals(2, eopHistory.getEntries().size());
        
        Assert.assertEquals(unitConvRad.convert(7.68783442726072E+01), eopHistory.getPoleCorrection(date.shiftedBy(0)).getXp(), 1e-15);
        Assert.assertEquals(unitConvRad.convert(3.47286203337827E+02), eopHistory.getPoleCorrection(date.shiftedBy(-1)).getYp(), 1e-15);
        Assert.assertEquals(-3.17284190690589E+04, eopHistory.getUT1MinusUTC(date.shiftedBy(10)) * 1000, 1e-15);
        Assert.assertEquals( 1.32354538674901E+00, eopHistory.getLOD(date.shiftedBy(10))* 1000, 1e-15);
        Assert.assertEquals(unitConvRad.convert(-1.10122731910265E+03), eopHistory.getNonRotatinOriginNutationCorrection(date.shiftedBy(10))[0], 1e-15);
        Assert.assertEquals(unitConvRad.convert(-4.00387630903350E+03), eopHistory.getNonRotatinOriginNutationCorrection(date.shiftedBy(10))[1], 1e-15);
        Assert.assertEquals(equinox[0], eopHistory.getEquinoxNutationCorrection(date.shiftedBy(10))[0], 1e-15);
        Assert.assertEquals(equinox[1], eopHistory.getEquinoxNutationCorrection(date.shiftedBy(10))[1], 1e-15);
    }
    
    @Test
    public void testSmallSinexEopSynth2File() {

        SinexLoader loader = new SinexLoader("cod20842-small-synthEOP2.snx");
        loader.setITRFVersion(2014);
        
        IERSConventions.NutationCorrectionConverter converter =
                IERSConventions.IERS_2010.getNutationCorrectionConverter();
        SortedSet<EOPEntry> history = new TreeSet<EOPEntry>(new ChronologicalComparator());

        loader.fillHistory(converter, history);
        
        AbsoluteDate date = new AbsoluteDate(new DateComponents(2019, 1, 1), utc).shiftedBy(Constants.JULIAN_DAY * (351 - 1)).shiftedBy(43185);
        final UnitsConverter unitConvRad = new UnitsConverter(Unit.parse("mas"), Unit.RADIAN);

        history.forEach(entry -> System.out.println(entry.getDate()));

        EOPEntry firstEntry = history.first();
        
        //Assert.assertEquals(2, history.size());
        
        Assert.assertEquals(unitConvRad.convert(7.68783442726072E+01), firstEntry.getX(), 1e-15);
        Assert.assertEquals(unitConvRad.convert(3.47286203337827E+02), firstEntry.getY(), 1e-15);
        Assert.assertEquals(-3.17284190690589E+04, firstEntry.getUT1MinusUTC() * 1000, 1e-15);
        Assert.assertEquals(1.32354538674901E+00, firstEntry.getLOD() * 1000, 1e-15);
        Assert.assertEquals(unitConvRad.convert(-1.10122731910265E+03), firstEntry.getDx(), 1e-15);
        Assert.assertEquals(unitConvRad.convert(-4.00387630903350E+03), firstEntry.getDy(), 1e-15);
        Assert.assertEquals(unitConvRad.convert(-1.10122731910265E+03), firstEntry.getDdPsi(), 1e-15);
        Assert.assertEquals(unitConvRad.convert(-4.00387630903350E+03), firstEntry.getDdEps(), 1e-15);
        
        EOPHistory eopHistory = new EOPHistory(IERSConventions.IERS_2010, history, true, DataContext.getDefault().getTimeScales());

        //Assert.assertEquals(2, eopHistory.getEntries().size());

        Assert.assertEquals(unitConvRad.convert(7.68783442726072E+01), eopHistory.getPoleCorrection(date.shiftedBy(10)).getXp(), 1e-15);
        Assert.assertEquals(unitConvRad.convert(3.47286203337827E+02), eopHistory.getPoleCorrection(date.shiftedBy(10)).getYp(), 1e-15);
        Assert.assertEquals(-3.17284190690589E+04, eopHistory.getUT1MinusUTC(date.shiftedBy(10)) * 1000, 1e-15);
        Assert.assertEquals( 1.32354538674901E+00, eopHistory.getLOD(date.shiftedBy(10))* 1000, 1e-15);
        Assert.assertEquals(unitConvRad.convert(-1.10122731910265E+03), eopHistory.getNonRotatinOriginNutationCorrection(date.shiftedBy(10))[0], 1e-15);
        Assert.assertEquals(unitConvRad.convert(-4.00387630903350E+03), eopHistory.getNonRotatinOriginNutationCorrection(date.shiftedBy(10))[1], 1e-15);
        Assert.assertEquals(unitConvRad.convert(-1.10122731910265E+03), eopHistory.getEquinoxNutationCorrection(date.shiftedBy(10))[0], 1e-15);
        Assert.assertEquals(unitConvRad.convert(-4.00387630903350E+03), eopHistory.getEquinoxNutationCorrection(date.shiftedBy(10))[1], 1e-15);
    }
    
    @Test
    public void testSmallSinexEopSynth3File() {

        SinexLoader loader = new SinexLoader("cod20842-small-synthEOP3.snx");
        loader.setITRFVersion(2014);
        
        IERSConventions.NutationCorrectionConverter converter =
                IERSConventions.IERS_2010.getNutationCorrectionConverter();
        SortedSet<EOPEntry> history = new TreeSet<EOPEntry>(new ChronologicalComparator());

        loader.fillHistory(converter, history);
        final UnitsConverter unitConvRad = new UnitsConverter(Unit.parse("mas"), Unit.RADIAN);

        history.forEach(entry -> System.out.println(entry.getDate()));

        AbsoluteDate date = new AbsoluteDate(new DateComponents(2019, 1, 1), utc).shiftedBy(Constants.JULIAN_DAY * (351 - 1)).shiftedBy(43185);
        double[] nro = converter.toNonRotating(date, unitConvRad.convert(-1.10122731910265E+03), unitConvRad.convert(-4.00387630903350E+03));
        
        EOPEntry firstEntry = history.first();

        Assert.assertEquals(unitConvRad.convert(7.68783442726072E+01), firstEntry.getX(), 1e-15);
        Assert.assertEquals(unitConvRad.convert(3.47286203337827E+02), firstEntry.getY(), 1e-15);
        Assert.assertEquals(-3.17284190690589E+04, firstEntry.getUT1MinusUTC() * 1000, 1e-15);
        Assert.assertEquals(1.32354538674901E+00, firstEntry.getLOD() * 1000, 1e-15);
        Assert.assertEquals(unitConvRad.convert(-1.10122731910265E+03), firstEntry.getDdPsi(), 1e-15);
        Assert.assertEquals(unitConvRad.convert(-4.00387630903350E+03), firstEntry.getDdEps(), 1e-15);
        Assert.assertEquals(nro[0], firstEntry.getDx(), 1e-15);
        Assert.assertEquals(nro[1], firstEntry.getDy(), 1e-15);

    }
    
    @Test
    public void testSmallSinexEopSynth4File() {

        SinexLoader loader = new SinexLoader("cod20842-small-synthEOP4.snx");
        loader.setITRFVersion(2014);
        Assert.assertEquals(ITRFVersion.ITRF_2014, loader.getITRFVersion());
        
        IERSConventions.NutationCorrectionConverter converter =
                IERSConventions.IERS_2010.getNutationCorrectionConverter();
        SortedSet<EOPEntry> history = new TreeSet<EOPEntry>(new ChronologicalComparator());

        loader.fillHistory(converter, history);
        
        AbsoluteDate date = new AbsoluteDate(new DateComponents(2019, 1, 1), utc).shiftedBy(Constants.JULIAN_DAY * (351 - 1)).shiftedBy(43185);
        final UnitsConverter unitConvRad = new UnitsConverter(Unit.parse("mas"), Unit.RADIAN);
        
        history.forEach(entry -> System.out.println(entry.getDate()));
        
        EOPEntry firstEntry = history.first();

        Assert.assertEquals(unitConvRad.convert(7.68783442726072E+01), firstEntry.getX(), 1e-15);
        Assert.assertEquals(unitConvRad.convert(3.47286203337827E+02), firstEntry.getY(), 1e-15);
        Assert.assertEquals(-3.17284190690589E+04, firstEntry.getUT1MinusUTC() * 1000, 1e-15);
        Assert.assertEquals(1.32354538674901E+00, firstEntry.getLOD() * 1000, 1e-15);
        Assert.assertEquals(unitConvRad.convert(-1.10122731910265E+03), firstEntry.getDx(), 1e-15);
        Assert.assertEquals(unitConvRad.convert(0), firstEntry.getDy(), 1e-15);
        Assert.assertEquals(unitConvRad.convert(0), firstEntry.getDdPsi(), 1e-15);
        Assert.assertEquals(unitConvRad.convert(-4.00387630903350E+03), firstEntry.getDdEps(), 1e-15);

    }
}
