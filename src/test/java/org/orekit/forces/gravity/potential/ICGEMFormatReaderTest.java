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
package org.orekit.forces.gravity.potential;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.hipparchus.exception.Localizable;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.data.DataContext;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.forces.gravity.potential.NormalizedSphericalHarmonicsProvider.NormalizedSphericalHarmonics;
import org.orekit.forces.gravity.potential.UnnormalizedSphericalHarmonicsProvider.UnnormalizedSphericalHarmonics;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.ChronologicalComparator;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.TimeSpanMap;

public class ICGEMFormatReaderTest {

    @Test
    public void testReadLimits() {
        Utils.setDataRoot("potential");
        GravityFieldFactory.addPotentialCoefficientsReader(new ICGEMFormatReader("g007_eigen_05c_coef", false));
        UnnormalizedSphericalHarmonicsProvider provider = GravityFieldFactory.getUnnormalizedProvider(3, 2);
        UnnormalizedSphericalHarmonics harmonics = provider.onDate(new AbsoluteDate(2004, 10, 1, 12, 0, 0.0, TimeScalesFactory.getTT()));
        Assertions.assertEquals(TideSystem.TIDE_FREE, provider.getTideSystem());
        try {
            harmonics.getUnnormalizedCnm(3, 3);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            // expected
        } catch (Exception e) {
            Assertions.fail("wrong exception caught: " + e.getLocalizedMessage());
        }
        try {
            harmonics.getUnnormalizedCnm(4, 2);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            // expected
        } catch (Exception e) {
            Assertions.fail("wrong exception caught: " + e.getLocalizedMessage());
        }
        harmonics.getUnnormalizedCnm(3, 2);
        Assertions.assertEquals(3, provider.getMaxDegree());
        Assertions.assertEquals(2, provider.getMaxOrder());
    }

    @Test
    public void testRegular05cNormalized() {
        Utils.setDataRoot("potential");
        GravityFieldFactory.addPotentialCoefficientsReader(new ICGEMFormatReader("g007_eigen_05c_coef", false));
        NormalizedSphericalHarmonicsProvider provider = GravityFieldFactory.getNormalizedProvider(5, 5);
        Assertions.assertEquals(TideSystem.TIDE_FREE, provider.getTideSystem());
        AbsoluteDate refDate = new AbsoluteDate("2004-10-01T12:00:00", TimeScalesFactory.getTT());
        AbsoluteDate date = new AbsoluteDate("2013-01-08T10:46:53", TimeScalesFactory.getTT());
        NormalizedSphericalHarmonics harmonics = provider.onDate(date);

        double offset     = date.durationFrom(refDate);
        double offsetYear = offset / Constants.JULIAN_YEAR;
        Assertions.assertEquals(0.957212879862e-06 + offsetYear * 0.490000000000e-11,
                            harmonics.getNormalizedCnm(3, 0), 1.0e-15);
        Assertions.assertEquals( 0.174804558032e-06, harmonics.getNormalizedCnm(5, 5), 1.0e-15);
        Assertions.assertEquals( 0.0,                harmonics.getNormalizedSnm(4, 0), 1.0e-15);
        Assertions.assertEquals( 0.308816581016e-06, harmonics.getNormalizedSnm(4, 4), 1.0e-15);
        Assertions.assertEquals(0.3986004415E+15, provider.getMu(), 1.0e-20);
        Assertions.assertEquals(0.6378136460E+07, provider.getAe(), 1.0e-20);
    }

    @Test
    public void testMoonGravityField() {
        Utils.setDataRoot("potential");
        GravityFieldFactory.addPotentialCoefficientsReader(new ICGEMFormatReader("GrazLGM300c.truncated", false));
        NormalizedSphericalHarmonicsProvider provider = GravityFieldFactory.getNormalizedProvider(12, 12);
        Assertions.assertEquals(TideSystem.TIDE_FREE, provider.getTideSystem());
        Assertions.assertEquals(4.9028010560e+12, provider.getMu(), 1.0e-20);
        Assertions.assertEquals(1.7380000000e+06, provider.getAe(), 1.0e-20);
    }

    @Test
    public void testVenusGravityField() {
        Utils.setDataRoot("potential");
        GravityFieldFactory.addPotentialCoefficientsReader(new ICGEMFormatReader("shgj180ua01.truncated", false));
        NormalizedSphericalHarmonicsProvider provider = GravityFieldFactory.getNormalizedProvider(12, 12);
        Assertions.assertEquals(TideSystem.TIDE_FREE, provider.getTideSystem());
        Assertions.assertEquals(3.248585920790e+14, provider.getMu(), 1.0e-20);
        Assertions.assertEquals(6.0510e+06,         provider.getAe(), 1.0e-20);
    }

    @Test
    public void testMarsGravityField() {
        Utils.setDataRoot("potential");
        GravityFieldFactory.addPotentialCoefficientsReader(new ICGEMFormatReader("jgm85f01.truncated", false));
        NormalizedSphericalHarmonicsProvider provider = GravityFieldFactory.getNormalizedProvider(12, 12);
        Assertions.assertEquals(TideSystem.TIDE_FREE, provider.getTideSystem());
        Assertions.assertEquals(4.28283763830e+13, provider.getMu(), 1.0e-20);
        Assertions.assertEquals(3.39420e+06,       provider.getAe(), 1.0e-20);
    }

    @Test
    public void testRegular05cUnnormalized() {
        Utils.setDataRoot("potential");
        GravityFieldFactory.addPotentialCoefficientsReader(new ICGEMFormatReader("g007_eigen_05c_coef", false));
        UnnormalizedSphericalHarmonicsProvider provider = GravityFieldFactory.getUnnormalizedProvider(5, 5);
        Assertions.assertEquals(TideSystem.TIDE_FREE, provider.getTideSystem());
        AbsoluteDate date = new AbsoluteDate("2013-01-08T10:46:53", TimeScalesFactory.getTT());
        UnnormalizedSphericalHarmonics harmonics = provider.onDate(date);
        int maxUlps = 2;
        checkValue(harmonics.getUnnormalizedCnm(3, 0),
                   date, 3, 0, 2004, 10, 1, 0.957212879862e-06, 0.490000000000e-11, 0, 0, 0, 0,
                   maxUlps);
        checkValue(harmonics.getUnnormalizedCnm(5, 5),
                   date, 5, 5, 2004, 10, 1, 0.174804558032e-06, 0, 0, 0, 0, 0,
                   maxUlps);
        checkValue(harmonics.getUnnormalizedSnm(4, 0),
                   date, 4, 0, 2004, 10, 1, 0, 0, 0, 0, 0, 0,
                   maxUlps);
        checkValue(harmonics.getUnnormalizedSnm(4, 4),
                   date, 4, 4, 2004, 10, 1, 0.308816581016e-06, 0, 0, 0, 0, 0,
                   maxUlps);
        Assertions.assertEquals(0.3986004415E+15, provider.getMu(), 1.0e-20);
        Assertions.assertEquals(0.6378136460E+07, provider.getAe(), 1.0e-20);
    }

    @Test
    public void testEigen06() {
        Utils.setDataRoot("potential");
        GravityFieldFactory.addPotentialCoefficientsReader(new ICGEMFormatReader("eigen-6s-truncated", false));
        UnnormalizedSphericalHarmonicsProvider provider = GravityFieldFactory.getUnnormalizedProvider(5, 5);
        Assertions.assertEquals(TideSystem.TIDE_FREE, provider.getTideSystem());
        AbsoluteDate date = new AbsoluteDate("2013-01-08T10:46:53", TimeScalesFactory.getTT());
        UnnormalizedSphericalHarmonics harmonics = provider.onDate(date);
        int maxUlps = 3;
        checkValue(harmonics.getUnnormalizedCnm(3, 0),
                   date, 3, 0, 2005, 1, 1, 9.57211326674e-07, -8.37191630994e-12,
                   -1.76087178236e-11, 9.47617140143e-11, 1.06252954726e-11, -9.12524501214e-12,
                   maxUlps);
        checkValue(harmonics.getUnnormalizedCnm(5, 5),
                   date, 5, 5, 2005, 1, 1, 1.74807033099e-07, -1.33498578664e-12,
                   -2.76043013690e-12, -8.28591865697e-12, 1.57898939101e-12, 2.90931436419e-12,
                   maxUlps);
        checkValue(harmonics.getUnnormalizedSnm(4, 0),
                   date, 4, 0, 2005, 1, 1, 0, 0, 0, 0, 0, 0,
                   maxUlps);
        checkValue(harmonics.getUnnormalizedSnm(4, 4),
                   date, 4, 4, 2005, 1, 1, 3.08820169866e-07, 4.35447782358e-13,
                   -1.21823769110e-11, 3.89722186321e-11, 7.28153817742e-12, -7.64506592459e-12,
                   maxUlps);
        Assertions.assertEquals(0.3986004415E+15, provider.getMu(), 1.0e-20);
        Assertions.assertEquals(0.6378136460E+07, provider.getAe(), 1.0e-20);
    }

    @Test
    public void testEigen06S4() throws OrekitException {
        Utils.setDataRoot("potential");
        TimeScale tt = DataContext.getDefault().getTimeScales().getTT();
        GravityFieldFactory.addPotentialCoefficientsReader(new ICGEMFormatReader("EIGEN-6S4-v2-truncated", true, tt));
        UnnormalizedSphericalHarmonicsProvider provider = GravityFieldFactory.getUnnormalizedProvider(3, 3);
        Assertions.assertEquals(TideSystem.TIDE_FREE, provider.getTideSystem());

        try {
            Field rawProviderField = WrappingUnnormalizedProvider.class.getDeclaredField("rawProvider");
            rawProviderField.setAccessible(true);
            PiecewiseSphericalHarmonics psh = (PiecewiseSphericalHarmonics) rawProviderField.get(provider);
            Field piecesField = PiecewiseSphericalHarmonics.class.getDeclaredField("pieces");
            piecesField.setAccessible(true);
            @SuppressWarnings("unchecked")
            TimeSpanMap<PiecewisePart> pieces = (TimeSpanMap<PiecewisePart>) piecesField.get(psh);

            List<AbsoluteDate> ref = new ArrayList<>();
            ref.add(new AbsoluteDate(1950, 1, 1, 0, 0, 0.0, tt));
            for (int year = 1986; year <= 2014; year++) {
                if (year != 2005 && year != 2010 && year != 2011) {
                    ref.add(new AbsoluteDate(year, 1, 1, 0, 0, 0.0, tt));
                }
            }
            ref.add(new AbsoluteDate(1985,  1,  9, 17, 51, 0.0, tt));
            ref.add(new AbsoluteDate(2002,  8, 15,  8, 17, 0.0, tt));
            ref.add(new AbsoluteDate(2004, 12, 26,  1,  0, 0.0, tt));
            ref.add(new AbsoluteDate(2010,  2, 27,  7, 35, 0.0, tt));
            ref.add(new AbsoluteDate(2011,  3, 11,  5, 15, 0.0, tt));
            ref.add(new AbsoluteDate(2014,  6, 15,  9, 17, 0.0, tt));
            ref.add(new AbsoluteDate(2050,  1,  1,  0,  0, 0.0, tt));
            ref.sort(new ChronologicalComparator());
            Assertions.assertEquals(35, pieces.getSpansNumber());
            TimeSpanMap.Transition<PiecewisePart> transition = pieces.getFirstTransition();
            for (final AbsoluteDate expected : ref) {
                Assertions.assertEquals(expected, transition.getDate());
                transition = transition.next();
            }
            Assertions.assertNull(transition);

        } catch (NoSuchFieldException | IllegalArgumentException | IllegalAccessException e) {
            Assertions.fail(e.getLocalizedMessage());
        }

        AbsoluteDate date = new AbsoluteDate("2013-01-08T10:46:53", TimeScalesFactory.getTT());
        UnnormalizedSphericalHarmonics harmonics = provider.onDate(date);
        int maxUlps = 2;
        checkValue(harmonics.getUnnormalizedCnm(3, 0),
                   date, 3, 0, 2013, 1, 1, 0, 0, 0.0,
                   9.57111813364E-07, 7.88508311993E-11,
                   -7.48797671617E-12, 1.07165711711E-10, -1.77995489691E-11, 8.19305927733E-12,
                   maxUlps);
        checkValue(harmonics.getUnnormalizedCnm(3, 2),
                   date, 3, 2, 2013, 1, 1, 0, 0, 0.0,
                   9.04721171104E-07, 3.61186647604E-11,
                   2.80429063599E-11, 2.70919929040E-11, -6.91647796355E-12, 2.03433003058E-11,
                   maxUlps);
        checkValue(harmonics.getUnnormalizedSnm(3, 2),
                   date, 3, 2, 2013, 1, 1, 0, 0, 0.0,
                   -6.18912728852E-07, -1.81446053580E-10,
                   -2.59785683825E-11, -2.89784167157E-11, 1.89089403607E-11, -3.61256979359E-11,
                   maxUlps);
        checkValue(harmonics.getUnnormalizedSnm(3, 3),
                   date, 3, 3, 2013, 1, 1, 0, 0, 0.0,
                   1.41428736507E-06, 3.11677919895E-10,
                   -8.67871982998E-11, 1.53591055907E-10, -4.45522488259E-11, 8.52872405198E-11,
                   maxUlps);
        Assertions.assertEquals(0.3986004415E+15, provider.getMu(), 1.0e-20);
        Assertions.assertEquals(0.6378136460E+07, provider.getAe(), 1.0e-20);
    }

    @Test
    public void testEigen06S4SplitBetween() throws OrekitException {
        Utils.setDataRoot("potential");
        TimeScale tai = DataContext.getDefault().getTimeScales().getTAI();
        GravityFieldFactory.addPotentialCoefficientsReader(new ICGEMFormatReader("eigen-6s4-split-between", true, tai));
        UnnormalizedSphericalHarmonicsProvider provider = GravityFieldFactory.getUnnormalizedProvider(1, 1);
        Assertions.assertEquals(TideSystem.TIDE_FREE, provider.getTideSystem());

        try {
            Field rawProviderField = WrappingUnnormalizedProvider.class.getDeclaredField("rawProvider");
            rawProviderField.setAccessible(true);
            PiecewiseSphericalHarmonics psh = (PiecewiseSphericalHarmonics) rawProviderField.get(provider);
            Field piecesField = PiecewiseSphericalHarmonics.class.getDeclaredField("pieces");
            piecesField.setAccessible(true);
            @SuppressWarnings("unchecked")
            TimeSpanMap<PiecewisePart> pieces = (TimeSpanMap<PiecewisePart>) piecesField.get(psh);

            List<AbsoluteDate> ref = new ArrayList<>();
            ref.add(new AbsoluteDate(1950,  1,  1,  0,  0, 0.0, tai));
            ref.add(new AbsoluteDate(2002,  8, 15,  8, 17, 0.0, tai));
            ref.add(new AbsoluteDate(2003,  1,  1,  0,  0, 0.0, tai));
            ref.add(new AbsoluteDate(2002,  1,  1,  0,  0, 0.0, tai));
            ref.add(new AbsoluteDate(2002,  6,  1,  0,  0, 0.0, tai));
            ref.add(new AbsoluteDate(2004,  1,  1,  0,  0, 0.0, tai));
            ref.sort(new ChronologicalComparator());
            Assertions.assertEquals(7, pieces.getSpansNumber());
            TimeSpanMap.Transition<PiecewisePart> transition = pieces.getFirstTransition();
            for (final AbsoluteDate expected : ref) {
                Assertions.assertEquals(expected, transition.getDate());
                transition = transition.next();
            }
            Assertions.assertNull(transition);

            AbsoluteDate previous = null;
            for (final AbsoluteDate current : ref) {
                if (previous != null) {
                    UnnormalizedSphericalHarmonics sh = provider.onDate(previous.shiftedBy(0.5 * current.durationFrom(previous)));
                    Assertions.assertEquals(1.0, sh.getUnnormalizedCnm(0, 0), 1.0e-15);
                    Assertions.assertEquals(0.0, sh.getUnnormalizedSnm(0, 0), 1.0e-15);
                }
                previous = current;
            }
        } catch (NoSuchFieldException | IllegalArgumentException | IllegalAccessException e) {
            Assertions.fail(e.getLocalizedMessage());
        }

    }

    @Test
    public void testCorruptedFile01() {
        doTestCorruptedFile("corrupted-01-g007_eigen_coef", 5, 5,
                            OrekitMessages.UNEXPECTED_FILE_FORMAT_ERROR_FOR_LOADER, -1);
    }

    @Test
    public void testCorruptedFile02() {
        doTestCorruptedFile("corrupted-02-g007_eigen_coef", 5, 5,
                            OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE, 45);
    }

    @Test
    public void testCorruptedFile03() {
        doTestCorruptedFile("corrupted-03-g007_eigen_coef", 5, 5,
                            OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE, 46);
    }

    @Test
    public void testCorruptedFile04() {
        doTestCorruptedFile("corrupted-04-g007_eigen_coef", 5, 5,
                            OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE, 51);
    }

    @Test
    public void testCorruptedFile05() {
        doTestCorruptedFile("corrupted-05-g007_eigen_coef", 5, 5,
                            OrekitMessages.MISSING_GRAVITY_FIELD_COEFFICIENT_IN_FILE, -1);
    }

    @Test
    public void testCorruptedFile06() {
        doTestCorruptedFile("corrupted-06-g007_eigen_coef", 5, 5,
                            OrekitMessages.SEVERAL_REFERENCE_DATES_IN_GRAVITY_FIELD, -1);
    }

    @Test
    public void testCorruptedFile07() {
        doTestCorruptedFile("corrupted-07-g007_eigen_coef", 5, 5,
                            OrekitMessages.DUPLICATED_GRAVITY_FIELD_COEFFICIENT_IN_FILE, -1);
    }

    @Test
    public void testCorruptedFile08() {
        doTestCorruptedFile("corrupted-08-g007_eigen_coef", 5, 5,
                            OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE, 107);
    }

    @Test
    public void testCorruptedFile09() {
        doTestCorruptedFile("corrupted-09-g007_eigen_coef", 5, 5,
                            OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE, 113);
    }

    @Test
    public void testCorruptedFile10() {
        doTestCorruptedFile("corrupted-10-eigen-6s4", 3, 3,
                            OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE, 77);
    }

    @Test
    public void testCorruptedFile11() {
        doTestCorruptedFile("corrupted-11-eigen-6s4", 3, 3,
                            OrekitMessages.DUPLICATED_GRAVITY_FIELD_COEFFICIENT_IN_FILE, -1);
    }

    @Test
    public void testCorruptedFile12() {
        doTestCorruptedFile("corrupted-12-eigen-6s4", 3, 3,
                            OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE, 53);
    }

    @Test
    public void testCorruptedFile13() throws OrekitException {
        doTestCorruptedFile("corrupted-13-eigen-6s4", 1, 1,
                            OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE, 27);
    }

    @Test
    public void testInvalidFormat() {
        doTestCorruptedFile("dummy_invalid_format_icgem", 3, 3,
                            OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE, 2);
    }

    @Test
    public void testUnsupportedFormat() {
        doTestCorruptedFile("dummy_unsupported_format_icgem", 3, 3,
                            OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE, 2);
    }

    @Test
    public void testInvalidProductType() {
        doTestCorruptedFile("dummy_invalid_product_type_icgem", 3, 3,
                            OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE, 3);
    }

    @Test
    public void testInvalidError() {
        doTestCorruptedFile("dummy_invalid_error_icgem", 3, 3,
                            OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE, 8);
    }

    @Test
    public void testInvalidNorm() {
        doTestCorruptedFile("dummy_invalid_norm_icgem", 3, 3,
                            OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE, 9);
    }

    @Test
    public void testInvalidTide() {
        doTestCorruptedFile("dummy_invalid_tide_icgem", 3, 3,
                            OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE, 10);
    }

    private void doTestCorruptedFile(final String name, final int degree, final int order,
                                     final Localizable specifier, int lineError) {
        Utils.setDataRoot("potential");
        GravityFieldFactory.addPotentialCoefficientsReader(new ICGEMFormatReader(name, false));
        try {
            GravityFieldFactory.getUnnormalizedProvider(degree, order);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(specifier, oe.getSpecifier());
            if (specifier.equals(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE)) {
                Assertions.assertEquals(lineError, ((Integer) oe.getParts()[0]).intValue());
            }
        }
    }


    @Test
    public void testZeroTide() {
        Utils.setDataRoot("potential");
        GravityFieldFactory.addPotentialCoefficientsReader(new ICGEMFormatReader("dummy_zero_tide_icgem", false));
        Assertions.assertEquals(TideSystem.ZERO_TIDE,
                            GravityFieldFactory.getUnnormalizedProvider(3, 3).getTideSystem());
    }

    @Test
    public void testUnknownTide() {
        Utils.setDataRoot("potential");
        GravityFieldFactory.addPotentialCoefficientsReader(new ICGEMFormatReader("dummy_unknown_tide_icgem", false));
        Assertions.assertEquals(TideSystem.UNKNOWN,
                            GravityFieldFactory.getUnnormalizedProvider(3, 3).getTideSystem());
    }

    /** Check numbers in the format 1.0d0 can be parsed. */
    @Test
    public void testLowercaseD() {
        Utils.setDataRoot("potential");
        GravityFieldFactory.addPotentialCoefficientsReader(new ICGEMFormatReader("dummy_small_d_icgem", false));
        Assertions.assertEquals(10.0,
                GravityFieldFactory
                        .getUnnormalizedProvider(3, 3)
                        .onDate(AbsoluteDate.J2000_EPOCH)
                        .getUnnormalizedCnm(2, 2),
                0.0);
    }

    /** check files without 1,0 and 1,1 can be parsed. */
    @Test
    public void testMissingDegree1() {
        Utils.setDataRoot("potential");
        GravityFieldFactory.addPotentialCoefficientsReader(new ICGEMFormatReader("dummy_missing_degree_1", false));
        UnnormalizedSphericalHarmonics harmonics = GravityFieldFactory
                .getUnnormalizedProvider(2, 2)
                .onDate(AbsoluteDate.J2000_EPOCH);
        //check coefficients not in the file are initialized correctly
        Assertions.assertEquals(0.0, harmonics.getUnnormalizedCnm(1, 0), 0.0);
        Assertions.assertEquals(0.0, harmonics.getUnnormalizedCnm(1, 1), 0.0);
        //check a coefficient is read correctly
        Assertions.assertEquals(10.0, harmonics.getUnnormalizedCnm(2, 2), 0.0);
    }

    private void checkValue(final double value,
                            final AbsoluteDate date, final int n, final int m,
                            final int refYear, final int refMonth, final int refDay,
                            final double constant, final double trend,
                            final double cosYear, final double sinYear,
                            final double cosHalfYear, final double sinHalfYear,
                            final int maxUlps) {
        checkValue(value, date, n, m,
                   refYear, refMonth, refDay, 12, 0, 0.0,
                   constant, trend,
                   cosYear, sinYear, cosHalfYear, sinHalfYear,
                   maxUlps);
    }

    private void checkValue(final double value,
                            final AbsoluteDate date, final int n, final int m,
                            final int refYear, final int refMonth, final int refDay,
                            final int refHour, final int refMin, final double refSec,
                            final double constant, final double trend,
                            final double cosYear, final double sinYear,
                            final double cosHalfYear, final double sinHalfYear,
                            final int maxUlps) {
        double factor = GravityFieldFactory.getUnnormalizationFactors(n, m)[n][m];
        AbsoluteDate refDate = new AbsoluteDate(refYear, refMonth, refDay, refHour, refMin, refSec,
                                                TimeScalesFactory.getTT());
        double dtYear = date.durationFrom(refDate) / Constants.JULIAN_YEAR;
        double unNormalized = factor * (constant +
                                        trend       * dtYear +
                                        cosYear     * FastMath.cos(MathUtils.TWO_PI * dtYear) +
                                        sinYear     * FastMath.sin(MathUtils.TWO_PI * dtYear) +
                                        cosHalfYear * FastMath.cos(MathUtils.TWO_PI * dtYear * 2) +
                                        sinHalfYear * FastMath.sin(MathUtils.TWO_PI * dtYear * 2));
        double epsilon = maxUlps * FastMath.ulp(unNormalized);
        Assertions.assertEquals(unNormalized, value, epsilon);
    }

}
