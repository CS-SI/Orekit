/* Copyright 2002-2025 CS GROUP
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
package org.orekit.time;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.TimeZone;

import java.util.concurrent.TimeUnit;
import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.analysis.differentiation.*;
import org.hipparchus.complex.Complex;
import org.hipparchus.complex.ComplexField;
import org.hipparchus.complex.FieldComplex;
import org.hipparchus.complex.FieldComplexField;
import org.hipparchus.dfp.Dfp;
import org.hipparchus.dfp.DfpField;
import org.hipparchus.util.Binary64;
import org.hipparchus.util.Binary64Field;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.FieldTuple;
import org.hipparchus.util.Precision;
import org.hipparchus.util.Tuple;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.OrekitMatchers;
import org.orekit.Utils;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitIllegalArgumentException;
import org.orekit.errors.OrekitMessages;
import org.orekit.utils.Constants;

class FieldAbsoluteDateTest {

    private TimeScale utc;

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("regular-data");
        utc = TimeScalesFactory.getUTC();
    }

    @Test
    void testStandardEpoch() {
        doTestStandardEpoch(Binary64Field.getInstance());
    }

    @Test
    void testStandardEpochStrings() {
        doTestStandardEpochStrings(Binary64Field.getInstance());
    }

    @Test
    void testJulianEpochRate() {
        doTestJulianEpochRate(Binary64Field.getInstance());
    }

    @Test
    void testBesselianEpochRate() {
        doTestBesselianEpochRate(Binary64Field.getInstance());
    }

    @Test
    void testLieske() {
        doTestLieske(Binary64Field.getInstance());
    }

    @Test
    void testParse() {
        doTestParse(Binary64Field.getInstance());
    }

    @Test
    void testLocalTimeParsing() {
        doTestLocalTimeParsing(Binary64Field.getInstance());
    }

    @Test
    void testTimeZoneDisplay() {
        doTestTimeZoneDisplay(Binary64Field.getInstance());
    }

    @Test
    void testLocalTimeLeapSecond() {
        doTestLocalTimeLeapSecond(Binary64Field.getInstance());
    }

    @Test
    void testTimeZoneLeapSecond() {
        doTestTimeZoneLeapSecond(Binary64Field.getInstance());
    }

    @Test
    void testParseLeap() {
        doTestParseLeap(Binary64Field.getInstance());
    }

    @Test
    void testOutput() {
        doTestOutput(Binary64Field.getInstance());
    }

    @Test
    void testJ2000() {
        doTestJ2000(Binary64Field.getInstance());
    }

    @Test
    void testFraction() {
        doTestFraction(Binary64Field.getInstance());
    }

    @Test
    void testScalesOffset() {
        doTestScalesOffset(Binary64Field.getInstance());
    }

    @Test
    void testUTC() {
        doTestUTC(Binary64Field.getInstance());
    }

    @Test
    void test1970() {
        doTest1970(Binary64Field.getInstance());
    }

    @Test
    void test1970Instant() {
        doTest1970Instant(Binary64Field.getInstance());
    }

    @Test
    void testInstantAccuracy() {
        doTestInstantAccuracy(Binary64Field.getInstance());
    }

    @Test
    void testToInstant() {
        doTestToInstant(Binary64Field.getInstance());
    }


    @Test
    void testUtcGpsOffset() {
        doTestUtcGpsOffset(Binary64Field.getInstance());
    }

    @Test
    void testGpsDate() {
        doTestGpsDate(Binary64Field.getInstance());
    }

    @Test
    void testMJDDate() {
        doTestMJDDate(Binary64Field.getInstance());
    }

    @Test
    void testJDDate() {
        doTestJDDate(Binary64Field.getInstance());
    }

    /** Test issue 1310: get a date from a JD using a pivot timescale. */
    @Test
    void testIssue1310JDDateInTDB() {

        // Given
        // -----
        final Field<Binary64> field = Binary64Field.getInstance();

        final TDBScale TDBscale = TimeScalesFactory.getTDB();
        final FieldAbsoluteDate<Binary64> refDate = new FieldAbsoluteDate<>(field,
                new AbsoluteDate("2023-08-01T00:00:00.000", TDBscale));

        // When
        // ----
        final FieldAbsoluteDate<Binary64> wrongDate  = FieldAbsoluteDate.createJDDate(2460157,
                field.getOne().multiply(Constants.JULIAN_DAY / 2.0d), TDBscale);
        final FieldAbsoluteDate<Binary64> properDate = FieldAbsoluteDate.createJDDate(2460157,
                field.getOne().multiply(Constants.JULIAN_DAY / 2.0d), TDBscale, TimeScalesFactory.getTT());

        // Then
        // ----

        // Wrong date is too far from reference date
        Assertions.assertEquals(0.0, wrongDate.durationFrom(refDate).getReal(), 1.270e-05);

        // Proper date is close enough from reference date
        Assertions.assertEquals(0.0, properDate.durationFrom(refDate).getReal(), 2.132e-13);
    }

    @Test
    void testOffsets() {
        doTestOffsets(Binary64Field.getInstance());
    }

    @Test
    void testBeforeAndAfterLeap() {
        doTestBeforeAndAfterLeap(Binary64Field.getInstance());
    }

    @Test
    void testSymmetry() {
        doTestSymmetry(Binary64Field.getInstance());
    }

    @Test
    void testEquals() {
        doTestEquals(Binary64Field.getInstance());
    }

    @Test
    void testEqualsAddendum() {
        // GIVEN
        final AbsoluteDate date = AbsoluteDate.ARBITRARY_EPOCH;
        final UnivariateDerivative1 derivative1 = new UnivariateDerivative1(0., 1);
        final UnivariateDerivative1Field field = UnivariateDerivative1Field.getInstance();
        final FieldAbsoluteDate<UnivariateDerivative1> fieldDate = new FieldAbsoluteDate<>(field,
                date).shiftedBy(derivative1);
        final FieldAbsoluteDate<UnivariateDerivative1> sameFieldDate = fieldDate.shiftedBy(0);
        // WHEN
        final boolean isEqual = fieldDate.equals(new FieldAbsoluteDate<>(field, date));
        // THEN
        Assertions.assertFalse(isEqual);
        Assertions.assertEquals(sameFieldDate.hashCode(), fieldDate.hashCode());
        Assertions.assertEquals(sameFieldDate, fieldDate);
    }

    @Test
    void testIsEqualTo() { doTestIsEqualTo(Binary64Field.getInstance()); }

    @Test
    void testIsCloseTo() { doTestIsCloseTo(Binary64Field.getInstance()); }

    @Test
    void testIsBefore() { doTestIsBefore(Binary64Field.getInstance()); }

    @Test
    void testIsAfter() { doTestIsAfter(Binary64Field.getInstance()); }

    @Test
    void testIsBeforeOrEqualTo() { doTestIsBeforeOrEqualTo(Binary64Field.getInstance()); }

    @Test
    void testIsAfterOrEqualTo() { doTestIsAfterOrEqualTo(Binary64Field.getInstance()); }

    @Test
    void testIsBetween() { doTestIsBetween(Binary64Field.getInstance()); }

    @Test
    void testIsBetweenOrEqualTo() { doTestIsBetweenOrEqualTo(Binary64Field.getInstance()); }

    @Test
    void testComponents() {
        doTestComponents(Binary64Field.getInstance());
    }

    @Test
    void testMonth() {
        doTestMonth(Binary64Field.getInstance());
    }

    @Test
    void testCCSDSUnsegmentedNoExtension() {
        doTestCCSDSUnsegmentedNoExtension(Binary64Field.getInstance());
    }

    @Test
    void testCCSDSUnsegmentedWithExtendedPreamble() {
        doTestCCSDSUnsegmentedWithExtendedPreamble(Binary64Field.getInstance());
    }

    @Test
    void testCCSDSDaySegmented() {
        doTestCCSDSDaySegmented(Binary64Field.getInstance());
    }

    @Test
    void testCCSDSCalendarSegmented() {
        doTestCCSDSCalendarSegmented(Binary64Field.getInstance());
    }

    @Test
    void testExpandedConstructors() {
        doTestExpandedConstructors(Binary64Field.getInstance());
    }

    @Test
    void testHashcode() {
        doTestHashcode(Binary64Field.getInstance());
    }

    @Test
    void testInfinity() {
        doTestInfinity(Binary64Field.getInstance());
    }

    @Test
    void testAccuracy() {
        doTestAccuracy(Binary64Field.getInstance());
    }

    @Test
    void testAccuracyIssue348() {
        doTestAccuracyIssue348(Binary64Field.getInstance());
    }

    @Test
    void testIterationAccuracy() {
        doTestIterationAccuracy(Binary64Field.getInstance());
    }

    @Test
    void testIssue142() {
        doTestIssue142(Binary64Field.getInstance());
    }

    @Test
    void testIssue148() {
        doTestIssue148(Binary64Field.getInstance());
    }

    @Test
    void testIssue149() {
        doTestIssue149(Binary64Field.getInstance());
    }

    @Test
    void testWrapAtMinuteEnd() {
        doTestWrapAtMinuteEnd(Binary64Field.getInstance());
    }

    @Test
    void testIssue508() {
        doTestIssue508(Binary64Field.getInstance());
    }

    @Test
    void testGetComponentsIssue681and676and694() {
        doTestGetComponentsIssue681and676and694(Binary64Field.getInstance());
    }

    @Test
    void testNegativeOffsetConstructor() {
        doTestNegativeOffsetConstructor(Binary64Field.getInstance());
    }

    @Test
    void testNegativeOffsetShift() {
        doTestNegativeOffsetShift(Binary64Field.getInstance());
    }

    @Test
    void testGetDayOfYear() {
        doTestGetDayOfYear(Binary64Field.getInstance());
    }
    /** Test for method {@link FieldAbsoluteDate#hasZeroField()}.*/
    @Test
    void testHasZeroField() {
                       
        // DerivativeStructure
        // ----------
        
        final DSFactory dsFactory  = new DSFactory(3, 2);
        final Field<DerivativeStructure> dsField = dsFactory.getDerivativeField();
        
        // Constant date returns true
        final FieldAbsoluteDate<DerivativeStructure> dsConstantDate = new FieldAbsoluteDate<>(dsField);
        Assertions.assertTrue(dsConstantDate.hasZeroField());
        Assertions.assertTrue(dsConstantDate.shiftedBy(dsField.getOne()).hasZeroField());
        
        // Variable date returns false
        final DerivativeStructure dsDt0 = dsFactory.variable(0, 10.);
        final DerivativeStructure dsDt1 = dsFactory.variable(1, -100.);
        final DerivativeStructure dsDt2 = dsFactory.variable(2, 100.);
        Assertions.assertFalse(dsConstantDate.shiftedBy(dsDt0).hasZeroField());
        Assertions.assertFalse(dsConstantDate.shiftedBy(dsDt1).hasZeroField());
        Assertions.assertFalse(dsConstantDate.shiftedBy(dsDt2).hasZeroField());
        Assertions.assertFalse(dsConstantDate.shiftedBy(dsDt0).shiftedBy(dsDt1).shiftedBy(dsDt2).hasZeroField());
        
        // UnivariateDerivative1
        // ---------------------
        
        final Field<UnivariateDerivative1> u1Field = UnivariateDerivative1Field.getInstance();
        
        // Constant date returns true
        final FieldAbsoluteDate<UnivariateDerivative1> u1ConstantDate = new FieldAbsoluteDate<>(u1Field);
        Assertions.assertTrue(u1ConstantDate.hasZeroField());
        Assertions.assertTrue(u1ConstantDate.shiftedBy(u1Field.getOne()).hasZeroField());
        Assertions.assertTrue(u1ConstantDate.shiftedBy(new UnivariateDerivative1(10., 0.)).hasZeroField());
        
        // Variable date returns false
        Assertions.assertFalse(u1ConstantDate.shiftedBy(new UnivariateDerivative1(10., 10.)).hasZeroField());
        
        // UnivariateDerivative1
        // ---------------------
        
        final Field<UnivariateDerivative2> u2Field = UnivariateDerivative2Field.getInstance();
        
        // Constant date returns true
        final FieldAbsoluteDate<UnivariateDerivative2> u2ConstantDate = new FieldAbsoluteDate<>(u2Field);
        Assertions.assertTrue(u2ConstantDate.hasZeroField());
        Assertions.assertTrue(u2ConstantDate.shiftedBy(u2Field.getOne()).hasZeroField());
        Assertions.assertTrue(u2ConstantDate.shiftedBy(new UnivariateDerivative2(10., 0., 0.)).hasZeroField());
        
        // Variable date returns false
        Assertions.assertFalse(u2ConstantDate.shiftedBy(new UnivariateDerivative2(10., 1., 2.)).hasZeroField());
        
        // Gradient
        // --------
        
        final Field<Gradient> gdField = GradientField.getField(2);
        
        // Constant date returns true
        final FieldAbsoluteDate<Gradient> gdConstantDate = new FieldAbsoluteDate<>(gdField);
        Assertions.assertTrue(gdConstantDate.hasZeroField());
        
        // Variable date returns false
        final Gradient gdDt0 = Gradient.variable(2, 0, 10.);
        final Gradient gdDt1 = Gradient.variable(2, 1, -100.);
        Assertions.assertFalse(gdConstantDate.shiftedBy(gdDt0).hasZeroField());
        Assertions.assertFalse(gdConstantDate.shiftedBy(gdDt1).hasZeroField());
        Assertions.assertFalse(gdConstantDate.shiftedBy(gdDt0).shiftedBy(gdDt1).hasZeroField());

        // SparseGradient
        final FieldAbsoluteDate<SparseGradient> sgdDate = new FieldAbsoluteDate<>(SparseGradient.createConstant(10.).getField());
        Assertions.assertTrue(sgdDate.hasZeroField());

        // Complex
        // -------
        
        final Field<Complex> cxField = ComplexField.getInstance();
        
        // Complex with no imaginary part returns true
        final FieldAbsoluteDate<Complex> cxConstantDate = new FieldAbsoluteDate<>(cxField);
        Assertions.assertTrue(cxConstantDate.hasZeroField());

        Assertions.assertTrue(cxConstantDate.shiftedBy(new Complex(10., 0.)).hasZeroField());                
        
        // Complex with imaginary part returns false
        Assertions.assertFalse(cxConstantDate.shiftedBy(new Complex(-100., 10.)).hasZeroField());

        // Binary64
        final Binary64Field b64Field = Binary64Field.getInstance();
        final FieldAbsoluteDate<Binary64> b64Date = new FieldAbsoluteDate<>(b64Field);
        Assertions.assertTrue(b64Date.hasZeroField());
        
        // Dfp
        final FieldAbsoluteDate<Dfp> dfpDate = new FieldAbsoluteDate<>(new DfpField(10));
        Assertions.assertTrue(dfpDate.hasZeroField());
        
        // FieldComplex
        final FieldAbsoluteDate<FieldComplex<Complex>> fcxDate = new FieldAbsoluteDate<>(FieldComplexField.getField(cxField));
        Assertions.assertTrue(fcxDate.hasZeroField());
        
        // FieldTuple
        final FieldAbsoluteDate<FieldTuple<DerivativeStructure>> ftpDate = new FieldAbsoluteDate<>(new FieldTuple<>(dsDt0, dsDt1).getField());
        Assertions.assertTrue(ftpDate.hasZeroField());

        // Tuple
        final FieldAbsoluteDate<Tuple> tpDate = new FieldAbsoluteDate<>(new Tuple(0., 1.).getField());
        Assertions.assertTrue(tpDate.hasZeroField());
        
        // FieldDerivativeStructure
        final FDSFactory<Binary64> fdsFactory = new FDSFactory<>(b64Field, 3, 1);
        final FieldAbsoluteDate<FieldDerivativeStructure<Binary64>> fdsDate = new FieldAbsoluteDate<>(fdsFactory.constant(1.).getField());
        Assertions.assertTrue(fdsDate.hasZeroField());
        
        // FieldGradient
        final FieldAbsoluteDate<FieldGradient<Binary64>> fgdDate =
                        new FieldAbsoluteDate<>(new FieldGradient<>(fdsFactory.constant(1.)).getField());
        Assertions.assertTrue(fgdDate.hasZeroField());
        
        // FieldUnivariateDerivative1
        final FieldAbsoluteDate<FieldUnivariateDerivative1<Binary64>> fu1Date = new FieldAbsoluteDate<>(
                FieldUnivariateDerivative1Field.getUnivariateDerivative1Field(Binary64Field.getInstance()));
        Assertions.assertTrue(fu1Date.hasZeroField());
        
        // FieldUnivariateDerivative2
        final FieldAbsoluteDate<FieldUnivariateDerivative2<Binary64>> fu2Date = new FieldAbsoluteDate<>(
                FieldUnivariateDerivative2Field.getUnivariateDerivative2Field(Binary64Field.getInstance()));
        Assertions.assertTrue(fu2Date.hasZeroField());
    }

    @Test
    void testDurationFromWithTimeUnit() {
        doTestDurationFromWithTimeUnit(Binary64Field.getInstance());
    }

    @Test
    void testConstructWithTimeUnitOffset() {
        doTestConstructWithTimeUnitOffset(Binary64Field.getInstance());
    }

    @Test
    void testShiftedByWithTimeUnit() {
        doTestShiftedByWithTimeUnit(Binary64Field.getInstance());
    }

    @Test
    void testToStringWithoutUtcOffset() {
        doTestToStringWithoutUtcOffset(Binary64Field.getInstance());
    }


    private <T extends CalculusFieldElement<T>> void doTestStandardEpoch(final Field<T> field) {

        TimeScale tai = TimeScalesFactory.getTAI();
        TimeScale tt  = TimeScalesFactory.getTT();

        FieldAbsoluteDate<T> JuEp  = FieldAbsoluteDate.getJulianEpoch(field);
        FieldAbsoluteDate<T> MJuEp = FieldAbsoluteDate.getModifiedJulianEpoch(field);
        FieldAbsoluteDate<T> FiEp  = FieldAbsoluteDate.getFiftiesEpoch(field);
        FieldAbsoluteDate<T> CCSDS = FieldAbsoluteDate.getCCSDSEpoch(field);
        FieldAbsoluteDate<T> GaEp  = FieldAbsoluteDate.getGalileoEpoch(field);
        FieldAbsoluteDate<T> GPSEp = FieldAbsoluteDate.getGPSEpoch(field);
        FieldAbsoluteDate<T> JTTEP = FieldAbsoluteDate.getJ2000Epoch(field);

        Assertions.assertEquals(-210866760000000L, JuEp.toDate(tt).getTime());
        Assertions.assertEquals(-3506716800000L, MJuEp.toDate(tt).getTime());
        Assertions.assertEquals(-631152000000L, FiEp.toDate(tt).getTime());
        Assertions.assertEquals(-378691200000L, CCSDS.toDate(tai).getTime());
        Assertions.assertEquals(935280019000L,  GaEp.toDate(tai).getTime());
        Assertions.assertEquals(315964819000L,  GPSEp.toDate(tai).getTime());
        Assertions.assertEquals(946728000000L,  JTTEP.toDate(tt).getTime());

    }

    private <T extends CalculusFieldElement<T>> void doTestStandardEpochStrings(final Field<T> field) {

        Assertions.assertEquals("-4712-01-01T12:00:00.000",
                            FieldAbsoluteDate.getJulianEpoch(field).toString(TimeScalesFactory.getTT()));
        Assertions.assertEquals("1858-11-17T00:00:00.000",
                            FieldAbsoluteDate.getModifiedJulianEpoch(field).toString(TimeScalesFactory.getTT()));
        Assertions.assertEquals("1950-01-01T00:00:00.000",
                            FieldAbsoluteDate.getFiftiesEpoch(field).toString(TimeScalesFactory.getTT()));
        Assertions.assertEquals("1958-01-01T00:00:00.000",
                            FieldAbsoluteDate.getCCSDSEpoch(field).toString(TimeScalesFactory.getTAI()));
        Assertions.assertEquals("1999-08-21T23:59:47.000",
                            FieldAbsoluteDate.getGalileoEpoch(field).toString(TimeScalesFactory.getUTC()));
        Assertions.assertEquals("1980-01-06T00:00:00.000",
                            FieldAbsoluteDate.getGPSEpoch(field).toString(TimeScalesFactory.getUTC()));
        Assertions.assertEquals("2000-01-01T12:00:00.000",
                            FieldAbsoluteDate.getJ2000Epoch(field).toString(TimeScalesFactory.getTT()));
        Assertions.assertEquals("1970-01-01T00:00:00.000",
                            FieldAbsoluteDate.getJavaEpoch(field).toString(TimeScalesFactory.getUTC()));
    }

    private <T extends CalculusFieldElement<T>> void doTestJulianEpochRate(final Field<T> field) {

        for (int i = 0; i < 10; ++i) {
            FieldAbsoluteDate<T> j200i = FieldAbsoluteDate.createJulianEpoch(field.getZero().add(2000.0+i));
            FieldAbsoluteDate<T> j2000 = FieldAbsoluteDate.getJ2000Epoch(field);
            double expected    = i * Constants.JULIAN_YEAR;
            Assertions.assertEquals(expected, j200i.durationFrom(j2000).getReal(), 4.0e-15 * expected);
        }

    }

    private <T extends CalculusFieldElement<T>> void doTestBesselianEpochRate(final Field<T> field) {

        for (int i = 0; i < 10; ++i) {
            FieldAbsoluteDate<T> b195i = FieldAbsoluteDate.createBesselianEpoch(field.getZero().add(1950.0 + i));
            FieldAbsoluteDate<T> b1950 = FieldAbsoluteDate.createBesselianEpoch(field.getZero().add(1950.0));
            double expected    = i * Constants.BESSELIAN_YEAR;
            Assertions.assertEquals(expected, b195i.durationFrom(b1950).getReal(), 4.0e-15 * expected);
        }

    }

    private <T extends CalculusFieldElement<T>> void doTestLieske(final Field<T> field) {

        // the following test values correspond to table 1 in the paper:
        // Precession Matrix Based on IAU (1976) System of Astronomical Constants,
        // Jay H. Lieske, Astronomy and Astrophysics, vol. 73, no. 3, Mar. 1979, p. 282-284
        // http://articles.adsabs.harvard.edu/cgi-bin/nph-iarticle_query?1979A%26A....73..282L&defaultprint=YES&filetype=.pdf

        // published table, with limited accuracy


        final double publishedEpsilon = 1.0e-6 * Constants.JULIAN_YEAR;
        checkEpochs(field, 1899.999142, 1900.000000, publishedEpsilon);
        checkEpochs(field, 1900.000000, 1900.000858, publishedEpsilon);
        checkEpochs(field, 1950.000000, 1949.999790, publishedEpsilon);
        checkEpochs(field, 1950.000210, 1950.000000, publishedEpsilon);
        checkEpochs(field, 2000.000000, 1999.998722, publishedEpsilon);
        checkEpochs(field, 2000.001278, 2000.000000, publishedEpsilon);

        // recomputed table, using directly Lieske formulas (i.e. *not* Orekit implementation) with high accuracy
        final double accurateEpsilon = 1.2e-13 * Constants.JULIAN_YEAR;
        checkEpochs(field, 1899.99914161068724704, 1900.00000000000000000, accurateEpsilon);
        checkEpochs(field, 1900.00000000000000000, 1900.00085837097878165, accurateEpsilon);
        checkEpochs(field, 1950.00000000000000000, 1949.99979044229979466, accurateEpsilon);
        checkEpochs(field, 1950.00020956217615449, 1950.00000000000000000, accurateEpsilon);
        checkEpochs(field, 2000.00000000000000000, 1999.99872251362080766, accurateEpsilon);
        checkEpochs(field, 2000.00127751366506194, 2000.00000000000000000, accurateEpsilon);

    }

    private <T extends CalculusFieldElement<T>> void checkEpochs(final Field<T> field, final double besselianEpoch, final double julianEpoch, final double epsilon) {
        final FieldAbsoluteDate<T> b = FieldAbsoluteDate.createBesselianEpoch(field.getZero().add(besselianEpoch));
        final FieldAbsoluteDate<T> j = FieldAbsoluteDate.createJulianEpoch(field.getZero().add(julianEpoch));
        Assertions.assertEquals(0.0, b.durationFrom(j).getReal(), epsilon);
    }

    private <T extends CalculusFieldElement<T>> void doTestParse(final Field<T> field) {

        Assertions.assertEquals(FieldAbsoluteDate.getModifiedJulianEpoch(field),
                            new FieldAbsoluteDate<>(field, "1858-W46-3", TimeScalesFactory.getTT()));
        Assertions.assertEquals(FieldAbsoluteDate.getJulianEpoch(field),
                            new FieldAbsoluteDate<>(field, "-4712-01-01T12:00:00.000", TimeScalesFactory.getTT()));
        Assertions.assertEquals(FieldAbsoluteDate.getFiftiesEpoch(field),
                            new FieldAbsoluteDate<>(field, "1950-01-01", TimeScalesFactory.getTT()));
        Assertions.assertEquals(FieldAbsoluteDate.getCCSDSEpoch(field),
                            new FieldAbsoluteDate<>(field, "1958-001", TimeScalesFactory.getTAI()));
    }

    private <T extends CalculusFieldElement<T>> void doTestLocalTimeParsing(final Field<T> field) {
        TimeScale utc = TimeScalesFactory.getUTC();
        Assertions.assertEquals(new FieldAbsoluteDate<>(field, "2011-12-31T23:00:00",       utc),
                            new FieldAbsoluteDate<>(field, "2012-01-01T03:30:00+04:30", utc));
        Assertions.assertEquals(new FieldAbsoluteDate<>(field, "2011-12-31T23:00:00",       utc),
                            new FieldAbsoluteDate<>(field, "2012-01-01T03:30:00+0430",  utc));
        Assertions.assertEquals(new FieldAbsoluteDate<>(field, "2011-12-31T23:30:00",       utc),
                            new FieldAbsoluteDate<>(field, "2012-01-01T03:30:00+04",    utc));
        Assertions.assertEquals(new FieldAbsoluteDate<>(field, "2012-01-01T05:17:00",       utc),
                            new FieldAbsoluteDate<>(field, "2011-12-31T22:17:00-07:00", utc));
        Assertions.assertEquals(new FieldAbsoluteDate<>(field, "2012-01-01T05:17:00",       utc),
                            new FieldAbsoluteDate<>(field, "2011-12-31T22:17:00-0700",  utc));
        Assertions.assertEquals(new FieldAbsoluteDate<>(field, "2012-01-01T05:17:00",       utc),
                            new FieldAbsoluteDate<>(field, "2011-12-31T22:17:00-07",    utc));
    }

    private <T extends CalculusFieldElement<T>> void doTestTimeZoneDisplay(final Field<T> field) {
        final TimeScale utc = TimeScalesFactory.getUTC();
        final FieldAbsoluteDate<T> date = new FieldAbsoluteDate<>(field, "2000-01-01T01:01:01.000", utc);
        Assertions.assertEquals("2000-01-01T01:01:01.000Z",      date.toString());
        Assertions.assertEquals("2000-01-01T11:01:01.000+10:00", date.toString( 600));
        Assertions.assertEquals("1999-12-31T23:01:01.000-02:00", date.toString(-120));

        // winter time, Europe is one hour ahead of UTC
        final TimeZone tz = TimeZone.getTimeZone("Europe/Paris");
        Assertions.assertEquals("2001-01-22T11:30:00.000+01:00",
                            new FieldAbsoluteDate<>(field, "2001-01-22T10:30:00", utc).toString(tz));

        // summer time, Europe is two hours ahead of UTC
        Assertions.assertEquals("2001-06-23T11:30:00.000+02:00",
                            new FieldAbsoluteDate<>(field, "2001-06-23T09:30:00", utc).toString(tz));

    }

    private <T extends CalculusFieldElement<T>> void doTestLocalTimeLeapSecond(final Field<T> field) {

        TimeScale utc = TimeScalesFactory.getUTC();
        FieldAbsoluteDate<T> beforeLeap = new FieldAbsoluteDate<>(field, "2012-06-30T23:59:59.8", utc);
        FieldAbsoluteDate<T> inLeap     = new FieldAbsoluteDate<>(field, "2012-06-30T23:59:60.5", utc);
        Assertions.assertEquals(0.7, inLeap.durationFrom(beforeLeap).getReal(), 1.0e-12);
       for (int minutesFromUTC = -1500; minutesFromUTC < -1499; ++minutesFromUTC) {
            DateTimeComponents dtcBeforeLeap = beforeLeap.getComponents(minutesFromUTC);
            DateTimeComponents dtcInsideLeap = inLeap.getComponents(minutesFromUTC);


            Assertions.assertEquals(dtcBeforeLeap.getDate(), dtcInsideLeap.getDate());

            Assertions.assertEquals(dtcBeforeLeap.getTime().getHour(), dtcInsideLeap.getTime().getHour());
            Assertions.assertEquals(dtcBeforeLeap.getTime().getMinute(), dtcInsideLeap.getTime().getMinute());
            Assertions.assertEquals(minutesFromUTC, dtcBeforeLeap.getTime().getMinutesFromUTC());
            Assertions.assertEquals(minutesFromUTC, dtcInsideLeap.getTime().getMinutesFromUTC());
            Assertions.assertEquals(59.8, dtcBeforeLeap.getTime().getSecond(), 1.0e-10);
            Assertions.assertEquals(60.5, dtcInsideLeap.getTime().getSecond(), 1.0e-10);
        }

    }

    private <T extends CalculusFieldElement<T>> void doTestTimeZoneLeapSecond(final Field<T> field) {

        TimeScale utc = TimeScalesFactory.getUTC();
        final TimeZone tz = TimeZone.getTimeZone("Europe/Paris");
        FieldAbsoluteDate<T> localBeforeMidnight = new FieldAbsoluteDate<>(field, "2012-06-30T21:59:59.800", utc);
        Assertions.assertEquals("2012-06-30T23:59:59.800+02:00",
                            localBeforeMidnight.toString(tz));
        Assertions.assertEquals("2012-07-01T00:00:00.800+02:00",
                            localBeforeMidnight.shiftedBy(1.0).toString(tz));

        FieldAbsoluteDate<T> beforeLeap = new FieldAbsoluteDate<>(field, "2012-06-30T23:59:59.8", utc);
        FieldAbsoluteDate<T> inLeap     = new FieldAbsoluteDate<>(field, "2012-06-30T23:59:60.5", utc);
        Assertions.assertEquals(0.7, inLeap.durationFrom(beforeLeap).getReal(), 1.0e-12);
        Assertions.assertEquals("2012-07-01T01:59:59.800+02:00", beforeLeap.toString(tz));
        Assertions.assertEquals("2012-07-01T01:59:60.500+02:00", inLeap.toString(tz));

    }

    private <T extends CalculusFieldElement<T>> void doTestParseLeap(final Field<T> field) {
        TimeScale utc = TimeScalesFactory.getUTC();
        FieldAbsoluteDate<T> beforeLeap = new FieldAbsoluteDate<>(field, "2012-06-30T23:59:59.8", utc);
        FieldAbsoluteDate<T> inLeap     = new FieldAbsoluteDate<>(field, "2012-06-30T23:59:60.5", utc);
        Assertions.assertEquals(0.7, inLeap.durationFrom(beforeLeap).getReal(), 1.0e-12);
        Assertions.assertEquals("2012-06-30T23:59:60.500", inLeap.toString(utc));
    }

    private <T extends CalculusFieldElement<T>> void doTestOutput(final Field<T> field) {
        TimeScale tt = TimeScalesFactory.getTT();
        Assertions.assertEquals("1950-01-01T01:01:01.000",
                            FieldAbsoluteDate.getFiftiesEpoch(field).shiftedBy(3661.0).toString(tt));
        Assertions.assertEquals("2000-01-01T13:01:01.000",
                            FieldAbsoluteDate.getJ2000Epoch(field).shiftedBy(3661.0).toString(tt));
    }

    private <T extends CalculusFieldElement<T>> void doTestJ2000(final Field<T> field) {
        FieldAbsoluteDate<T> FAD = new FieldAbsoluteDate<>(field);
        Assertions.assertEquals("2000-01-01T12:00:00.000",
                            FAD.toString(TimeScalesFactory.getTT()));
        Assertions.assertEquals("2000-01-01T11:59:27.816",
                            FAD.toString(TimeScalesFactory.getTAI()));
        Assertions.assertEquals("2000-01-01T11:58:55.816",
                            FAD.toString(utc));
        Assertions.assertEquals("2000-01-01T12:00:00.000",
                            FieldAbsoluteDate.getJ2000Epoch(field).toString(TimeScalesFactory.getTT()));
        Assertions.assertEquals("2000-01-01T11:59:27.816",
                            FieldAbsoluteDate.getJ2000Epoch(field).toString(TimeScalesFactory.getTAI()));
        Assertions.assertEquals("2000-01-01T11:58:55.816",
                            FieldAbsoluteDate.getJ2000Epoch(field).toString(utc));
    }

    private <T extends CalculusFieldElement<T>> void doTestFraction(final Field<T> field) {
        FieldAbsoluteDate<T> d =
            new FieldAbsoluteDate<>(field, new DateComponents(2000, 1, 1), new TimeComponents(11, 59, 27.816),
                             TimeScalesFactory.getTAI());
        Assertions.assertEquals(0, d.durationFrom(FieldAbsoluteDate.getJ2000Epoch(field)).getReal(), 1.0e-10);
    }

    private <T extends CalculusFieldElement<T>> void doTestScalesOffset(final Field<T> field) {
        FieldAbsoluteDate<T> date = new FieldAbsoluteDate<>(field, new DateComponents(2006, 2, 24),
                                                            new TimeComponents(15, 38, 0),
                                                            utc);
        Assertions.assertEquals(33,
                            date.timeScalesOffset(TimeScalesFactory.getTAI(), utc).getReal(),
                            1.0e-10);
    }

    private <T extends CalculusFieldElement<T>> void doTestUTC(final Field<T> field) {
        FieldAbsoluteDate<T> date = new FieldAbsoluteDate<>(field, new DateComponents(2002, 1, 1),
                                                            new TimeComponents(0, 0, 1),
                                                            utc);
        Assertions.assertEquals("2002-01-01T00:00:01.000Z", date.toString());
    }

    private <T extends CalculusFieldElement<T>> void doTest1970(final Field<T> field) {
        FieldAbsoluteDate<T> date = new FieldAbsoluteDate<>(field, new Date(0L), utc);
        Assertions.assertEquals("1970-01-01T00:00:00.000Z", date.toString());
    }

    private <T extends CalculusFieldElement<T>> void doTest1970Instant(final Field<T> field) {
        Assertions.assertEquals("1970-01-01T00:00:00.000Z", new FieldAbsoluteDate<>(field, Instant.EPOCH, utc).toString());
        Assertions.assertEquals("1970-01-01T00:00:00.000Z", new FieldAbsoluteDate<>(field, Instant.ofEpochMilli(0L), utc).toString());
        Assertions.assertEquals("1970-01-01T00:00:00.000Z", new FieldAbsoluteDate<>(field, Instant.EPOCH, (UTCScale) utc).toString());
        Assertions.assertEquals("1970-01-01T00:00:00.000Z", new FieldAbsoluteDate<>(field, Instant.ofEpochMilli(0L), (UTCScale) utc).toString());
    }

    private <T extends CalculusFieldElement<T>> void doTestInstantAccuracy(final Field<T> field) {
        Assertions.assertEquals("1970-01-02T00:16:40.123456789Z", new FieldAbsoluteDate<>(field, Instant.ofEpochSecond(87400, 123456789), utc).toString());
        Assertions.assertEquals("1970-01-07T00:10:00.123456789Z", new FieldAbsoluteDate<>(field, Instant.ofEpochSecond(519000, 123456789), utc).toString());
        Assertions.assertEquals("1970-01-02T00:16:40.123456789Z", new FieldAbsoluteDate<>(field, Instant.ofEpochSecond(87400, 123456789), (UTCScale) utc).toString());
        Assertions.assertEquals("1970-01-07T00:10:00.123456789Z", new FieldAbsoluteDate<>(field, Instant.ofEpochSecond(519000, 123456789), (UTCScale) utc).toString());
    }

    public <T extends CalculusFieldElement<T>> void doTestToInstant(final Field<T> field) {
        Assertions.assertEquals(Instant.ofEpochSecond(0), new FieldAbsoluteDate<>(field, "1970-01-01T00:00:00.000Z", utc).toInstant());
        Assertions.assertEquals(Instant.ofEpochSecond(0), new FieldAbsoluteDate<>(field, "1970-01-01T00:00:00.000Z", utc).toInstant(TimeScalesFactory.getTimeScales()));

        Instant expectedInstant = Instant.ofEpochSecond(519000, 123456789);
        Assertions.assertEquals(expectedInstant, new FieldAbsoluteDate<>(field, "1970-01-07T00:10:00.123456789Z", utc).toInstant());
        Assertions.assertEquals(expectedInstant, new FieldAbsoluteDate<>(field, "1970-01-07T00:10:00.123456789Z", utc).toInstant(TimeScalesFactory.getTimeScales()));

        Assertions.assertEquals(OffsetDateTime.parse("2024-05-15T09:32:36.123456789Z", DateTimeFormatter.ISO_DATE_TIME).toInstant(),
            new FieldAbsoluteDate<>(field,"2024-05-15T09:32:36.123456789Z", utc).toInstant());
        Assertions.assertEquals(OffsetDateTime.parse("2024-05-15T09:32:36.123456789Z", DateTimeFormatter.ISO_DATE_TIME).toInstant(),
            new FieldAbsoluteDate<>(field, "2024-05-15T09:32:36.123456789Z", utc).toInstant(TimeScalesFactory.getTimeScales()));

    }

    private <T extends CalculusFieldElement<T>> void doTestUtcGpsOffset(final Field<T> field) {
        FieldAbsoluteDate<T> date1   = new FieldAbsoluteDate<>(field, new DateComponents(2005, 8, 9),
                                                               new TimeComponents(16, 31, 17),
                                                               utc);
        FieldAbsoluteDate<T> date2   = new FieldAbsoluteDate<>(field, new DateComponents(2006, 8, 9),
                                                               new TimeComponents(16, 31, 17),
                                                               utc);
        FieldAbsoluteDate<T> dateRef = new FieldAbsoluteDate<>(field, new DateComponents(1980, 1, 6),
                                                               TimeComponents.H00,
                                                               utc);

        // 13 seconds offset between GPS time and UTC in 2005
        long noLeapGap = ((9347 * 24 + 16) * 60 + 31) * 60 + 17;
        long realGap   = (long) date1.durationFrom(dateRef).getReal();
        Assertions.assertEquals(13L, realGap - noLeapGap);

        // 14 seconds offset between GPS time and UTC in 2006
        noLeapGap = ((9712 * 24 + 16) * 60 + 31) * 60 + 17;
        realGap   = (long) date2.durationFrom(dateRef).getReal();
        Assertions.assertEquals(14L, realGap - noLeapGap);

    }

    private <T extends CalculusFieldElement<T>> void doTestGpsDate(final Field<T> field) {
        FieldAbsoluteDate<T> date = FieldAbsoluteDate.createGPSDate(1387, field.getZero().add(318677000.0));
        FieldAbsoluteDate<T> ref  = new FieldAbsoluteDate<>(field, new DateComponents(2006, 8, 9),
                                                            new TimeComponents(16, 31, 3),
                                                            utc);
        Assertions.assertEquals(0, date.durationFrom(ref).getReal(), 1.0e-15);
    }

    private <T extends CalculusFieldElement<T>> void doTestMJDDate(final Field<T> field) {
        FieldAbsoluteDate<T> dateA = FieldAbsoluteDate.createMJDDate(51544, field.getZero().add(0.5 * Constants.JULIAN_DAY),
                                                                     TimeScalesFactory.getTT());
        Assertions.assertEquals(0.0, FieldAbsoluteDate.getJ2000Epoch(field).durationFrom(dateA).getReal(), 1.0e-15);
        FieldAbsoluteDate<T> dateB = FieldAbsoluteDate.createMJDDate(53774, field.getZero(), TimeScalesFactory.getUTC());
        FieldAbsoluteDate<T> dateC = new FieldAbsoluteDate<>(field, "2006-02-08T00:00:00", TimeScalesFactory.getUTC());
        Assertions.assertEquals(0.0, dateC.durationFrom(dateB).getReal(), 1.0e-15);
    }

    private <T extends CalculusFieldElement<T>> void doTestJDDate(final Field<T> field) {
        final FieldAbsoluteDate<T> date = FieldAbsoluteDate.createJDDate(2400000, field.getZero().add(0.5 * Constants.JULIAN_DAY),
                                                                         TimeScalesFactory.getTT());
        Assertions.assertEquals(0.0, FieldAbsoluteDate.getModifiedJulianEpoch(field).durationFrom(date).getReal(), 1.0e-15);
    }

    private <T extends CalculusFieldElement<T>> void doTestOffsets(final Field<T> field) {
        final TimeScale tai = TimeScalesFactory.getTAI();
        FieldAbsoluteDate<T> leapStartUTC = new FieldAbsoluteDate<>(field, 1976, 12, 31, 23, 59, 59, utc);
        FieldAbsoluteDate<T> leapEndUTC   = new FieldAbsoluteDate<>(field, 1977,  1,  1,  0,  0,  0, utc);
        FieldAbsoluteDate<T> leapStartTAI = new FieldAbsoluteDate<>(field, 1977,  1,  1,  0,  0, 14, tai);
        FieldAbsoluteDate<T> leapEndTAI   = new FieldAbsoluteDate<>(field, 1977,  1,  1,  0,  0, 16, tai);
        Assertions.assertEquals(leapStartUTC, leapStartTAI);
        Assertions.assertEquals(leapEndUTC, leapEndTAI);
        Assertions.assertEquals(1, leapEndUTC.offsetFrom(leapStartUTC, utc).getReal(), 1.0e-10);
        Assertions.assertEquals(1, leapEndTAI.offsetFrom(leapStartTAI, utc).getReal(), 1.0e-10);
        Assertions.assertEquals(2, leapEndUTC.offsetFrom(leapStartUTC, tai).getReal(), 1.0e-10);
        Assertions.assertEquals(2, leapEndTAI.offsetFrom(leapStartTAI, tai).getReal(), 1.0e-10);
        Assertions.assertEquals(2, leapEndUTC.durationFrom(leapStartUTC).getReal(),    1.0e-10);
        Assertions.assertEquals(2, leapEndTAI.durationFrom(leapStartTAI).getReal(),    1.0e-10);
    }

    private <T extends CalculusFieldElement<T>> void doTestBeforeAndAfterLeap(final Field<T> field) {
        final TimeScale tai = TimeScalesFactory.getTAI();
        FieldAbsoluteDate<T> leapStart = new FieldAbsoluteDate<>(field, 1977,  1,  1,  0,  0, 14, tai);
        FieldAbsoluteDate<T> leapEnd   = new FieldAbsoluteDate<>(field, 1977,  1,  1,  0,  0, 16, tai);
        for (int i = -10; i < 10; ++i) {
            final double dt = 1.1 * (2 * i - 1);
            FieldAbsoluteDate<T> d1 = leapStart.shiftedBy(dt);
            FieldAbsoluteDate<T> d2 = new FieldAbsoluteDate<>(leapStart, dt, tai);
            FieldAbsoluteDate<T> d3 = new FieldAbsoluteDate<>(leapStart, dt, utc);
            FieldAbsoluteDate<T> d4 = new FieldAbsoluteDate<>(leapEnd,   dt, tai);
            FieldAbsoluteDate<T> d5 = new FieldAbsoluteDate<>(leapEnd,   dt, utc);
            Assertions.assertTrue(FastMath.abs(d1.durationFrom(d2).getReal()) < 1.0e-10);
            if (dt < 0) {
                Assertions.assertTrue(FastMath.abs(d2.durationFrom(d3).getReal()) < 1.0e-10);
                Assertions.assertTrue(d4.durationFrom(d5).getReal() > (1.0 - 1.0e-10));
            } else {
                Assertions.assertTrue(d2.durationFrom(d3).getReal() < (-1.0 + 1.0e-10));
                Assertions.assertTrue(FastMath.abs(d4.durationFrom(d5).getReal()) < 1.0e-10);
            }
        }
    }

    private <T extends CalculusFieldElement<T>> void doTestSymmetry(final Field<T> field) {
        final TimeScale tai = TimeScalesFactory.getTAI();
        FieldAbsoluteDate<T> leapStart = new FieldAbsoluteDate<>(field, 1977,  1,  1,  0,  0, 14, tai);
        for (int i = -10; i < 10; ++i) {
            final double dt = 1.1 * (2 * i - 1);
            Assertions.assertEquals(dt, new FieldAbsoluteDate<>(leapStart, dt, utc).offsetFrom(leapStart, utc).getReal(), 1.0e-10);
            Assertions.assertEquals(dt, new FieldAbsoluteDate<>(leapStart, dt, tai).offsetFrom(leapStart, tai).getReal(), 1.0e-10);
            Assertions.assertEquals(dt, leapStart.shiftedBy(dt).durationFrom(leapStart).getReal(), 1.0e-10);
        }
    }

    private <T extends CalculusFieldElement<T>> void doTestEquals(final Field<T> field) {
        FieldAbsoluteDate<T> d1 =
            new FieldAbsoluteDate<>(field, new DateComponents(2006, 2, 25),
                                    new TimeComponents(17, 10, 34),
                                    utc);
        FieldAbsoluteDate<T> d2 = new FieldAbsoluteDate<>(field, new DateComponents(2006, 2, 25),
                                                          new TimeComponents(17, 10, 0),
                                                          utc).shiftedBy(34);
        Assertions.assertEquals(d1, d2);
        Assertions.assertNotEquals(d1, this);
    }

    private <T extends CalculusFieldElement<T>> void doTestIsEqualTo(final Field<T> field) {
        TestDates<T> dates = new TestDates<>(field);
        FieldAbsoluteDate<T> present = dates.getPresentFieldAbsoluteDate();
        Assertions.assertTrue(present.isEqualTo(dates.present));
        Assertions.assertTrue(present.isEqualTo(dates.presentToo));
        Assertions.assertFalse(present.isEqualTo(dates.past));
        Assertions.assertFalse(present.isEqualTo(dates.future));
    }

    private <T extends CalculusFieldElement<T>> void doTestIsCloseTo(final Field<T> field) {
        double tolerance = 10;
        TestDates<T> dates = new TestDates<>(field);
        FieldAbsoluteDate<T> present = dates.getPresentFieldAbsoluteDate();
        FieldTimeStamped<T> closeToPresent = new AnyFieldTimeStamped<>(field, dates.presentDate.shiftedBy(5));
        Assertions.assertTrue(present.isCloseTo(present, tolerance));
        Assertions.assertTrue(present.isCloseTo(dates.presentToo, tolerance));
        Assertions.assertTrue(present.isCloseTo(closeToPresent, tolerance));
        Assertions.assertFalse(present.isCloseTo(dates.past, tolerance));
        Assertions.assertFalse(present.isCloseTo(dates.future, tolerance));
    }

    private <T extends CalculusFieldElement<T>> void doTestIsBefore(final Field<T> field) {
        TestDates<T> dates = new TestDates<>(field);
        FieldAbsoluteDate<T> present = dates.getPresentFieldAbsoluteDate();
        Assertions.assertFalse(present.isBefore(dates.past));
        Assertions.assertFalse(present.isBefore(present));
        Assertions.assertFalse(present.isBefore(dates.presentToo));
        Assertions.assertTrue(present.isBefore(dates.future));
    }

    private <T extends CalculusFieldElement<T>> void doTestIsAfter(final Field<T> field) {
        TestDates<T> dates = new TestDates<>(field);
        FieldAbsoluteDate<T> present = dates.getPresentFieldAbsoluteDate();
        Assertions.assertTrue(present.isAfter(dates.past));
        Assertions.assertFalse(present.isAfter(present));
        Assertions.assertFalse(present.isAfter(dates.presentToo));
        Assertions.assertFalse(present.isAfter(dates.future));
    }

    private <T extends CalculusFieldElement<T>> void doTestIsBeforeOrEqualTo(final Field<T> field) {
        TestDates<T> dates = new TestDates<>(field);
        FieldAbsoluteDate<T> present = dates.getPresentFieldAbsoluteDate();
        Assertions.assertFalse(present.isBeforeOrEqualTo(dates.past));
        Assertions.assertTrue(present.isBeforeOrEqualTo(present));
        Assertions.assertTrue(present.isBeforeOrEqualTo(dates.presentToo));
        Assertions.assertTrue(present.isBeforeOrEqualTo(dates.future));
    }

    private <T extends CalculusFieldElement<T>> void doTestIsAfterOrEqualTo(final Field<T> field) {
        TestDates<T> dates = new TestDates<>(field);
        FieldAbsoluteDate<T> present = dates.getPresentFieldAbsoluteDate();
        Assertions.assertTrue(present.isAfterOrEqualTo(dates.past));
        Assertions.assertTrue(present.isAfterOrEqualTo(present));
        Assertions.assertTrue(present.isAfterOrEqualTo(dates.presentToo));
        Assertions.assertFalse(present.isAfterOrEqualTo(dates.future));
    }

    private <T extends CalculusFieldElement<T>> void doTestIsBetween(final Field<T> field) {
        TestDates<T> dates = new TestDates<>(field);
        FieldAbsoluteDate<T> present = dates.getPresentFieldAbsoluteDate();
        Assertions.assertTrue(present.isBetween(dates.past, dates.future));
        Assertions.assertTrue(present.isBetween(dates.future, dates.past));
        Assertions.assertFalse(dates.past.getDate().isBetween(present, dates.future));
        Assertions.assertFalse(dates.past.getDate().isBetween(dates.future, present));
        Assertions.assertFalse(dates.future.getDate().isBetween(dates.past, present));
        Assertions.assertFalse(dates.future.getDate().isBetween(present, dates.past));
        Assertions.assertFalse(present.isBetween(present, dates.future));
        Assertions.assertFalse(present.isBetween(dates.past, present));
        Assertions.assertFalse(present.isBetween(dates.past, dates.past));
        Assertions.assertFalse(present.isBetween(present, present));
        Assertions.assertFalse(present.isBetween(present, dates.presentToo));
    }

    private <T extends CalculusFieldElement<T>> void doTestIsBetweenOrEqualTo(final Field<T> field) {
        TestDates<T> dates = new TestDates<>(field);
        FieldAbsoluteDate<T> present = dates.getPresentFieldAbsoluteDate();
        Assertions.assertTrue(present.isBetweenOrEqualTo(dates.past, dates.future));
        Assertions.assertTrue(present.isBetweenOrEqualTo(dates.future, dates.past));
        Assertions.assertFalse(dates.past.getDate().isBetweenOrEqualTo(present, dates.future));
        Assertions.assertFalse(dates.past.getDate().isBetweenOrEqualTo(dates.future, present));
        Assertions.assertFalse(dates.future.getDate().isBetweenOrEqualTo(dates.past, present));
        Assertions.assertFalse(dates.future.getDate().isBetweenOrEqualTo(present, dates.past));
        Assertions.assertTrue(present.isBetweenOrEqualTo(present, dates.future));
        Assertions.assertTrue(present.isBetweenOrEqualTo(dates.past, present));
        Assertions.assertFalse(present.isBetweenOrEqualTo(dates.past, dates.past));
        Assertions.assertTrue(present.isBetweenOrEqualTo(present, present));
        Assertions.assertTrue(present.isBetweenOrEqualTo(present, dates.presentToo));
    }

    private <T extends CalculusFieldElement<T>> void doTestComponents(final Field<T> field) {
        // this is NOT J2000.0,
        // it is either a few seconds before or after depending on time scale
        DateComponents date = new DateComponents(2000, 1, 1);
        TimeComponents time = new TimeComponents(11, 59, 10);
        TimeScale[] scales = {
            TimeScalesFactory.getTAI(), TimeScalesFactory.getUTC(),
            TimeScalesFactory.getTT(), TimeScalesFactory.getTCG()
        };
        for (int i = 0; i < scales.length; ++i) {
            FieldAbsoluteDate<T> in = new FieldAbsoluteDate<>(field, date, time, scales[i]);
            for (int j = 0; j < scales.length; ++j) {
                DateTimeComponents pair = in.getComponents(scales[j]);
                if (i == j) {
                    Assertions.assertEquals(date, pair.getDate());
                    Assertions.assertEquals(time, pair.getTime());
                } else {
                    Assertions.assertNotSame(date, pair.getDate());
                    Assertions.assertNotSame(time, pair.getTime());
                }
            }
        }
    }

    private <T extends CalculusFieldElement<T>> void doTestMonth(final Field<T> field) {
        TimeScale utc = TimeScalesFactory.getUTC();
        Assertions.assertEquals(new FieldAbsoluteDate<>(field, 2011, 2, 23, utc),
                            new FieldAbsoluteDate<>(field, 2011, Month.FEBRUARY, 23, utc));
        Assertions.assertEquals(new FieldAbsoluteDate<>(field, 2011, 2, 23, 1, 2, 3.4, utc),
                            new FieldAbsoluteDate<>(field, 2011, Month.FEBRUARY, 23, 1, 2, 3.4, utc));
    }

    private <T extends CalculusFieldElement<T>> void doTestCCSDSUnsegmentedNoExtension(final Field<T> field) {
        FieldAbsoluteDate<T> reference = new FieldAbsoluteDate<>(field, "2002-05-23T12:34:56.789", utc);
        double lsb = FastMath.pow(2.0, -24);

        byte[] timeCCSDSEpoch = new byte[] { 0x53, 0x7F, 0x40, -0x70, -0x37, -0x05, -0x19 };
        for (int preamble = 0x00; preamble < 0x80; ++preamble) {
            if (preamble == 0x1F) {
                // using CCSDS reference epoch
                FieldAbsoluteDate<T> ccsds1 =
                                FieldAbsoluteDate.parseCCSDSUnsegmentedTimeCode(field, (byte) preamble, (byte) 0x0, timeCCSDSEpoch, null);
                Assertions.assertEquals(0, ccsds1.durationFrom(reference).getReal(), lsb / 2);
            } else {
                try {
                    FieldAbsoluteDate.parseCCSDSUnsegmentedTimeCode(field, (byte) preamble, (byte) 0x0, timeCCSDSEpoch, null);
                    Assertions.fail("an exception should have been thrown");
                } catch (OrekitException iae) {
                    // expected
                }

            }
        }

        // missing epoch
        byte[] timeJ2000Epoch = new byte[] { 0x04, 0x7E, -0x0B, -0x10, -0x07, 0x16, -0x79 };
        try {
            FieldAbsoluteDate.parseCCSDSUnsegmentedTimeCode(field, (byte) 0x2F, (byte) 0x0, timeJ2000Epoch, null);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException iae) {
            // expected
        }

        // using J2000.0 epoch
        FieldAbsoluteDate<T> ccsds3 =
                        FieldAbsoluteDate.parseCCSDSUnsegmentedTimeCode(field, (byte) 0x2F, (byte) 0x0, timeJ2000Epoch,
                                                                        FieldAbsoluteDate.getJ2000Epoch(field));
        Assertions.assertEquals(0, ccsds3.durationFrom(reference).getReal(), lsb / 2);

    }

    private <T extends CalculusFieldElement<T>> void doTestCCSDSUnsegmentedWithExtendedPreamble(final Field<T> field) {

        FieldAbsoluteDate<T> reference = new FieldAbsoluteDate<>(field, "2095-03-03T22:02:45.789012345678901", utc);
        int leap = (int) FastMath.rint(utc.offsetFromTAI(reference.toAbsoluteDate()).toDouble());

        byte extendedPreamble = (byte) -0x80;
        byte identification   = (byte)  0x10;
        byte coarseLength1    = (byte)  0x0C; // four (3 + 1) bytes
        byte fineLength1      = (byte)  0x03; // 3 bytes
        byte coarseLength2    = (byte)  0x20; // 1 additional byte for coarse time
        byte fineLength2      = (byte)  0x10; // 4 additional bytes for fine time
        byte[] timeCCSDSEpoch = new byte[] {
             0x01,  0x02,  0x03,  0x04,  (byte)(0x05 - leap), // 5 bytes for coarse time (seconds)
            -0x37, -0x04, -0x4A, -0x74, -0x2C, -0x3C, -0x48   // 7 bytes for fine time (sub-seconds)
        };
        byte preamble1 = (byte) (extendedPreamble | identification | coarseLength1 | fineLength1);
        byte preamble2 = (byte) (coarseLength2 | fineLength2);
        FieldAbsoluteDate<T> ccsds1 =
                        FieldAbsoluteDate.parseCCSDSUnsegmentedTimeCode(field, preamble1, preamble2, timeCCSDSEpoch, null);

        // The 8 attoseconds difference comes from the fact unsegmented time is
        // in powers of 1/256 s, so it is not a whole number of attoseconds
        Assertions.assertEquals(-8.0e-18, ccsds1.durationFrom(reference).getReal(), 1.0e-18);

    }

    private <T extends CalculusFieldElement<T>> void doTestCCSDSDaySegmented(final Field<T> field) {
        FieldAbsoluteDate<T> reference = new FieldAbsoluteDate<>(field, "2002-05-23T12:34:56.789012345678", TimeScalesFactory.getUTC());
        double lsb = 1.0e-13;
        byte[] timeCCSDSEpoch = new byte[] { 0x3F, 0x55, 0x02, -0x4D, 0x2C, -0x6B, 0x00, -0x44, 0x61, 0x4E };

        for (int preamble = 0x00; preamble < 0x100; ++preamble) {
            if (preamble == 0x42) {
                // using CCSDS reference epoch

                FieldAbsoluteDate<T> ccsds1 =
                                FieldAbsoluteDate.parseCCSDSDaySegmentedTimeCode(field, (byte) preamble, timeCCSDSEpoch, null);
                Assertions.assertEquals(0, ccsds1.durationFrom(reference).getReal(), lsb / 2);
            } else {
                try {
                    FieldAbsoluteDate.parseCCSDSDaySegmentedTimeCode(field, (byte) preamble, timeCCSDSEpoch, null);
                    Assertions.fail("an exception should have been thrown");
                } catch (OrekitException iae) {
                    // expected
                }

            }
        }

        // missing epoch
        byte[] timeJ2000Epoch = new byte[] { 0x03, 0x69, 0x02, -0x4D, 0x2C, -0x6B, 0x00, -0x44, 0x61, 0x4E };
        try {
            FieldAbsoluteDate.parseCCSDSDaySegmentedTimeCode(field, (byte) 0x4A, timeJ2000Epoch, null);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException iae) {
            // expected
        }

        // using J2000.0 epoch
        FieldAbsoluteDate<T> ccsds3 =
                        FieldAbsoluteDate.parseCCSDSDaySegmentedTimeCode(field, (byte) 0x4A, timeJ2000Epoch, DateComponents.J2000_EPOCH);
        Assertions.assertEquals(0, ccsds3.durationFrom(reference).getReal(), lsb / 2);

        // limit to microsecond
        byte[] timeMicrosecond = new byte[] { 0x03, 0x69, 0x02, -0x4D, 0x2C, -0x6B, 0x00, 0x0C };
        FieldAbsoluteDate<T> ccsds4 =
                        FieldAbsoluteDate.parseCCSDSDaySegmentedTimeCode(field, (byte) 0x49, timeMicrosecond, DateComponents.J2000_EPOCH);
        Assertions.assertEquals(-0.345678e-6, ccsds4.durationFrom(reference).getReal(), lsb / 2);

    }

    private <T extends CalculusFieldElement<T>> void doTestCCSDSCalendarSegmented(final Field<T> field) {

        FieldAbsoluteDate<T> reference = new FieldAbsoluteDate<>(field, "2002-05-23T12:34:56.789012345678", TimeScalesFactory.getUTC());
        double lsb = 1.0e-13;
        FieldAbsoluteDate<T> FAD = new FieldAbsoluteDate<>(field);
        // month of year / day of month variation
        byte[] timeMonthDay = new byte[] { 0x07, -0x2E, 0x05, 0x17, 0x0C, 0x22, 0x38, 0x4E, 0x5A, 0x0C, 0x22, 0x38, 0x4E };
        for (int preamble = 0x00; preamble < 0x100; ++preamble) {
            if (preamble == 0x56) {
                FieldAbsoluteDate<T> ccsds1 =
                    FAD.parseCCSDSCalendarSegmentedTimeCode((byte) preamble, timeMonthDay);
                Assertions.assertEquals(0, ccsds1.durationFrom(reference).getReal(), lsb / 2);
            } else {
                try {
                    FAD.parseCCSDSCalendarSegmentedTimeCode((byte) preamble, timeMonthDay);
                    Assertions.fail("an exception should have been thrown");
                } catch (OrekitException iae) {
                    // expected
                } catch (IllegalArgumentException iae) {
                    // should happen when preamble specifies day of year variation
                    // since there is no day 1303 (= 5 * 256 + 23) in any year ...
                    Assertions.assertEquals(preamble & 0x08, 0x08);
                }

            }
        }

        // day of year variation
        byte[] timeDay = new byte[] { 0x07, -0x2E, 0x00, -0x71, 0x0C, 0x22, 0x38, 0x4E, 0x5A, 0x0C, 0x22, 0x38, 0x4E };
        for (int preamble = 0x00; preamble < 0x100; ++preamble) {
            if (preamble == 0x5E) {
                FieldAbsoluteDate<T> ccsds1 =
                    FAD.parseCCSDSCalendarSegmentedTimeCode((byte) preamble, timeDay);
                Assertions.assertEquals(0, ccsds1.durationFrom(reference).getReal(), lsb / 2);
            } else {
                try {
                    FAD.parseCCSDSCalendarSegmentedTimeCode((byte) preamble, timeDay);
                    Assertions.fail("an exception should have been thrown");
                } catch (OrekitException iae) {
                    // expected
                } catch (IllegalArgumentException iae) {
                    // should happen when preamble specifies month of year / day of month variation
                    // since there is no month 0 in any year ...
                    Assertions.assertEquals(preamble & 0x08, 0x00);
                }

            }
        }

        // limit to microsecond
        byte[] timeMicrosecond = new byte[] { 0x07, -0x2E, 0x00, -0x71, 0x0C, 0x22, 0x38, 0x4E, 0x5A, 0x0C };
        FieldAbsoluteDate<T> ccsds4 =
            FAD.parseCCSDSCalendarSegmentedTimeCode((byte) 0x5B, timeMicrosecond);
        Assertions.assertEquals(-0.345678e-6, ccsds4.durationFrom(reference).getReal(), lsb / 2);

    }

    private <T extends CalculusFieldElement<T>> void doTestExpandedConstructors(final Field<T> field) {
        Assertions.assertEquals(new FieldAbsoluteDate<>(field, new DateComponents(2002, 5, 28),
                                                    new TimeComponents(15, 30, 0),
                                                    TimeScalesFactory.getUTC()),
                     new FieldAbsoluteDate<>(field, 2002, 5, 28, 15, 30, 0, TimeScalesFactory.getUTC()));
        Assertions.assertEquals(new FieldAbsoluteDate<>(field, new DateComponents(2002, 5, 28), TimeComponents.H00,
                                                    TimeScalesFactory.getUTC()),
                     new FieldAbsoluteDate<>(field, 2002, 5, 28, TimeScalesFactory.getUTC()));
        try {
            new FieldAbsoluteDate<>(field, 2002, 5, 28, 25, 30, 0, TimeScalesFactory.getUTC());
        } catch (OrekitIllegalArgumentException oiae) {
            Assertions.assertEquals(OrekitMessages.NON_EXISTENT_HMS_TIME, oiae.getSpecifier());
            Assertions.assertEquals(25, ((Integer) oiae.getParts()[0]).intValue());
            Assertions.assertEquals(30, ((Integer) oiae.getParts()[1]).intValue());
            Assertions.assertEquals( 0, ((Double) oiae.getParts()[2]), 1.0e-15);
        }
    }

    private <T extends CalculusFieldElement<T>> void doTestHashcode(final Field<T> field) {
        FieldAbsoluteDate<T> d1 =
            new FieldAbsoluteDate<>(field, new DateComponents(2006, 2, 25),
                                    new TimeComponents(17, 10, 34),
                                    utc);
        FieldAbsoluteDate<T> d2 = new FieldAbsoluteDate<>(field, new DateComponents(2006, 2, 25),
                                                          new TimeComponents(17, 10, 0),
                                                          utc).shiftedBy(34);
        Assertions.assertEquals(d1.hashCode(), d2.hashCode());
        Assertions.assertTrue(d1.hashCode() != d1.shiftedBy(1.0e-3).hashCode());
    }

    private <T extends CalculusFieldElement<T>> void doTestInfinity(final Field<T> field) {
        Assertions.assertTrue(FieldAbsoluteDate.getJulianEpoch(field).compareTo(FieldAbsoluteDate.getPastInfinity(field)) > 0);
        Assertions.assertTrue(FieldAbsoluteDate.getJulianEpoch(field).compareTo(FieldAbsoluteDate.getFutureInfinity(field)) < 0);
        Assertions.assertTrue(FieldAbsoluteDate.getJ2000Epoch(field).compareTo(FieldAbsoluteDate.getPastInfinity(field)) > 0);
        Assertions.assertTrue(FieldAbsoluteDate.getJ2000Epoch(field).compareTo(FieldAbsoluteDate.getFutureInfinity(field)) < 0);
        Assertions.assertTrue(FieldAbsoluteDate.getPastInfinity(field).compareTo(FieldAbsoluteDate.getJulianEpoch(field)) < 0);
        Assertions.assertTrue(FieldAbsoluteDate.getPastInfinity(field).compareTo(FieldAbsoluteDate.getJ2000Epoch(field)) < 0);
        Assertions.assertTrue(FieldAbsoluteDate.getPastInfinity(field).compareTo(FieldAbsoluteDate.getFutureInfinity(field)) < 0);
        Assertions.assertTrue(FieldAbsoluteDate.getFutureInfinity(field).compareTo(FieldAbsoluteDate.getJulianEpoch(field)) > 0);
        Assertions.assertTrue(FieldAbsoluteDate.getFutureInfinity(field).compareTo(FieldAbsoluteDate.getJ2000Epoch(field)) > 0);
        Assertions.assertTrue(FieldAbsoluteDate.getFutureInfinity(field).compareTo(FieldAbsoluteDate.getPastInfinity(field)) > 0);
        Assertions.assertTrue(Double.isInfinite(FieldAbsoluteDate.getFutureInfinity(field).durationFrom(FieldAbsoluteDate.getJ2000Epoch(field)).getReal()));
        Assertions.assertTrue(Double.isInfinite(FieldAbsoluteDate.getFutureInfinity(field).durationFrom(FieldAbsoluteDate.getPastInfinity(field)).getReal()));
        Assertions.assertTrue(Double.isInfinite(FieldAbsoluteDate.getPastInfinity(field).durationFrom(FieldAbsoluteDate.getJ2000Epoch(field)).getReal()));
        Assertions.assertEquals("5881610-07-11T23:59:59.999Z",  FieldAbsoluteDate.getFutureInfinity(field).toString());
        Assertions.assertEquals("-5877490-03-03T00:00:00.000Z", FieldAbsoluteDate.getPastInfinity(field).toString());

        final FieldAbsoluteDate<T> j2000     = FieldAbsoluteDate.getJ2000Epoch(field);
        final FieldAbsoluteDate<T> arbitrary = FieldAbsoluteDate.getArbitraryEpoch(field);
        Assertions.assertEquals(Double.POSITIVE_INFINITY,
                                j2000.durationFrom(arbitrary.shiftedBy(Double.NEGATIVE_INFINITY)).getReal());
        Assertions.assertEquals(Double.NEGATIVE_INFINITY,
                                j2000.durationFrom(arbitrary.shiftedBy(Double.POSITIVE_INFINITY)).getReal());
        Assertions.assertEquals(Double.POSITIVE_INFINITY, j2000.durationFrom(arbitrary.shiftedBy(field.getZero().add(Double.NEGATIVE_INFINITY))).getReal());
        Assertions.assertEquals(Double.NEGATIVE_INFINITY,
                                j2000.durationFrom(arbitrary.shiftedBy(field.getZero().add(Double.POSITIVE_INFINITY))).getReal());

    }

    private <T extends CalculusFieldElement<T>> void doTestAccuracy(final Field<T> field) {
        TimeScale tai = TimeScalesFactory.getTAI();
        double sec = 0.281;
        FieldAbsoluteDate<T> t = new FieldAbsoluteDate<>(field, 2010, 6, 21, 18, 42, sec, tai);
        double recomputedSec = t.getComponents(tai).getTime().getSecond();
        Assertions.assertEquals(sec, recomputedSec, FastMath.ulp(sec));
    }

    private <T extends CalculusFieldElement<T>> void doTestAccuracyIssue348(final Field<T> field)
        {
        FieldAbsoluteDate<T> tF = new FieldAbsoluteDate<>(field,
                                                          new DateComponents(1970, 1, 1),
                                                          new TimeComponents(3, 25, 45.6789),
                                                          TimeScalesFactory.getUTC());
        AbsoluteDate tA = tF.toAbsoluteDate();
        double delta = -0.01;
        T recomputedDelta = tF.shiftedBy(delta).durationFrom(tA);
        Assertions.assertEquals(delta, recomputedDelta.getReal(), 1.0e-17);
    }

    private <T extends CalculusFieldElement<T>> void doTestIterationAccuracy(final Field<T> field) {

        final TimeScale tai = TimeScalesFactory.getTAI();
        final FieldAbsoluteDate<T> t0 = new FieldAbsoluteDate<>(field, 2010, 6, 21, 18, 42, 0.281, tai);

        // 0.1 is not representable exactly in double precision
        // we will accumulate error, between -0.5ULP and -3ULP at each iteration
        checkIteration(0.1, t0, 10000, 3.0, -0.3874, 1.0e-4);

        // 0.125 is representable exactly in double precision
        // error will be null
        checkIteration(0.125, t0, 10000, 1.0e-15, 0.0, 1.0e-15);
    }

    private <T extends CalculusFieldElement<T>> void checkIteration(final double step, final FieldAbsoluteDate<T> t0, final int nMax,
                                final double maxErrorFactor,
                                final double expectedMean, final double meanTolerance) {
        final double epsilon = FastMath.ulp(step);
        FieldAbsoluteDate<T> iteratedDate = t0;
        double mean = 0;
        for (int i = 1; i < nMax; ++i) {
            iteratedDate = iteratedDate.shiftedBy(step);
            FieldAbsoluteDate<T> directDate = t0.shiftedBy(i * step);
            final T error = iteratedDate.durationFrom(directDate);
            mean += error.getReal() / (i * epsilon);
            Assertions.assertEquals(0.0, iteratedDate.durationFrom(directDate).getReal(), maxErrorFactor * i * epsilon);
        }
        mean /= nMax;
        Assertions.assertEquals(expectedMean, mean, meanTolerance);
    }

    private <T extends CalculusFieldElement<T>> void doTestIssue142(final Field<T> field) {
        final FieldAbsoluteDate<T> epoch = FieldAbsoluteDate.getJavaEpoch(field);
        final TimeScale utc = TimeScalesFactory.getUTC();

        Assertions.assertEquals("1970-01-01T00:00:00.000", epoch.toString(utc));
        Assertions.assertEquals(0.0, epoch.durationFrom(new FieldAbsoluteDate<>(field, 1970, 1, 1, utc)).getReal(), 1.0e-15);
        Assertions.assertEquals(8.000082,
                            epoch.durationFrom(new FieldAbsoluteDate<>(field, DateComponents.JAVA_EPOCH, TimeScalesFactory.getTAI())).getReal(),
                            1.0e-15);

        //Milliseconds - April 1, 2006, in UTC
        long msOffset = 1143849600000L;
        final FieldAbsoluteDate<T> ad = new FieldAbsoluteDate<>(epoch, msOffset / 1000, TimeScalesFactory.getUTC());
        Assertions.assertEquals("2006-04-01T00:00:00.000", ad.toString(utc));
    }

    private <T extends CalculusFieldElement<T>> void doTestIssue148(final Field<T> field) {
        final TimeScale utc = TimeScalesFactory.getUTC();
        FieldAbsoluteDate<T> t0 = new FieldAbsoluteDate<>(field, 2012, 6, 30, 23, 59, 50.0, utc);
        DateTimeComponents components = t0.shiftedBy(11.0 - 200 * Precision.EPSILON).getComponents(utc);
        Assertions.assertEquals(2012, components.getDate().getYear());
        Assertions.assertEquals(   6, components.getDate().getMonth());
        Assertions.assertEquals(  30, components.getDate().getDay());
        Assertions.assertEquals(  23, components.getTime().getHour());
        Assertions.assertEquals(  59, components.getTime().getMinute());
        Assertions.assertEquals(  61 - 200 * Precision.EPSILON,
                            components.getTime().getSecond(), 1.0e-15);
    }

    private <T extends CalculusFieldElement<T>> void doTestIssue149(final Field<T> field) {
        final TimeScale utc = TimeScalesFactory.getUTC();
        FieldAbsoluteDate<T> t0 = new FieldAbsoluteDate<>(field, 2012, 6, 30, 23, 59, 59, utc);
        DateTimeComponents components = t0.shiftedBy(1.0 - Precision.EPSILON).getComponents(utc);
        Assertions.assertEquals(2012, components.getDate().getYear());
        Assertions.assertEquals(   6, components.getDate().getMonth());
        Assertions.assertEquals(  30, components.getDate().getDay());
        Assertions.assertEquals(  23, components.getTime().getHour());
        Assertions.assertEquals(  59, components.getTime().getMinute());
        Assertions.assertEquals(  60 - Precision.EPSILON,
                            components.getTime().getSecond(), 1.0e-15);
    }

    private <T extends CalculusFieldElement<T>> void doTestWrapAtMinuteEnd(final Field<T> field) {
        TimeScale tai = TimeScalesFactory.getTAI();
        TimeScale utc = TimeScalesFactory.getUTC();
        FieldAbsoluteDate<T> date0 = new FieldAbsoluteDate<>(field, DateComponents.J2000_EPOCH, TimeComponents.H12, tai);
        FieldAbsoluteDate<T> ref = date0.shiftedBy(new TimeOffset(496891466L, 732011406663332300L));
        FieldAbsoluteDate<T> date = ref.shiftedBy(new TimeOffset(597L, 900970042626200000L).negate().multiply(33));
        DateTimeComponents dtc = date.getComponents(utc);
        Assertions.assertEquals(                 2015, dtc.getDate().getYear());
        Assertions.assertEquals(                    9, dtc.getDate().getMonth());
        Assertions.assertEquals(                   30, dtc.getDate().getDay());
        Assertions.assertEquals(                    7, dtc.getTime().getHour());
        Assertions.assertEquals(                   54, dtc.getTime().getMinute());
        Assertions.assertEquals(                  59L, dtc.getTime().getSplitSecond().getSeconds());
        Assertions.assertEquals(  999999999998732300L, dtc.getTime().getSplitSecond().getAttoSeconds());
        Assertions.assertEquals("2015-09-30T07:54:59.9999999999987323",
                            date.toString(utc));
        FieldAbsoluteDate<T> beforeMidnight = new FieldAbsoluteDate<>(field, 2008, 2, 29, 23, 59,
                                                                      new TimeOffset(59L, 999400000000000000L), utc);
        FieldAbsoluteDate<T> stillBeforeMidnight = beforeMidnight.shiftedBy(2.0e-4);
        Assertions.assertEquals(59.9994, beforeMidnight.getComponents(utc).getTime().getSecond(), 1.0e-15);
        Assertions.assertEquals(59.9996, stillBeforeMidnight.getComponents(utc).getTime().getSecond(), 1.0e-15);
        Assertions.assertEquals("2008-02-29T23:59:59.9994", beforeMidnight.toString(utc));
        Assertions.assertEquals("2008-02-29T23:59:59.9996", stillBeforeMidnight.toString(utc));
    }

    private <T extends CalculusFieldElement<T>> void doTestIssue508(final Field<T> field) {
        AbsoluteDate date = new AbsoluteDate(2000, 2, 24, 17, 5, 30.047, TimeScalesFactory.getUTC());
        FieldAbsoluteDate<T> tA = new FieldAbsoluteDate<>(field, date);
        FieldAbsoluteDate<T> tB = new FieldAbsoluteDate<>(date, field.getZero());
        Assertions.assertEquals(0.0, tA.durationFrom(tB).getReal(), Precision.SAFE_MIN);
    }

    public <T extends CalculusFieldElement<T>> void doTestGetComponentsIssue681and676and694(
            final Field<T> field) {

        // setup
        FieldAbsoluteDate<T> date = new FieldAbsoluteDate<>(field, 2009, 1, 1, utc);
        double attoSecond = 1.0e-18;
        double zeroUlp = FastMath.nextUp(0.0);
        double oneUlp = FastMath.ulp(1.0);
        double sixtyUlp = FastMath.ulp(60.0);
        double one = FastMath.nextDown(1.0);
        double sixty = FastMath.nextDown(60.0);
        double sixtyOne = FastMath.nextDown(61.0);

        // actions + verify
        // translate back to AbsoluteDate has up to half an ULP of error,
        // except when truncated when the error can be up to 1 ULP.
        check(date, 2009, 1, 1, 0, 0, 0, 1, 0, 0);
        check(date.shiftedBy(attoSecond), 2009, 1, 1, 0, 0, attoSecond, 0.5, 0, 0);
        check(date.shiftedBy(one), 2009, 1, 1, 0, 0, one, 0.5, 0, 0);
        // I could also see rounding to a valid time as being reasonable here
        check(date.shiftedBy(59).shiftedBy(one), 2009, 1, 1, 0, 0, sixty, 1, 1, 0);
        check(date.shiftedBy(86399).shiftedBy(one), 2009, 1, 1, 23, 59, sixty, 1, 1, 0);
        check(date.shiftedBy(-zeroUlp), 2009, 1, 1, 0, 0, 0, 0.5, 0, 0);
        check(date.shiftedBy(-oneUlp), 2008, 12, 31, 23, 59, sixtyOne, 1, 1, 0);
        check(date.shiftedBy(-1).shiftedBy(zeroUlp), 2008, 12, 31, 23, 59, 60.0, 0.5, 0, 0);
        check(date.shiftedBy(-1).shiftedBy(-zeroUlp), 2008, 12, 31, 23, 59, 60.0, 0.5, 0, 0);
        check(date.shiftedBy(-1).shiftedBy(-oneUlp), 2008, 12, 31, 23, 59, 60.0, 0.5, 0, 0);
        check(date.shiftedBy(-1).shiftedBy(-sixtyUlp), 2008, 12, 31, 23, 59, sixty, 0.5, 0, 0);
        check(date.shiftedBy(-61).shiftedBy(attoSecond), 2008, 12, 31, 23, 59, attoSecond, 0.5, 0, 0);

        // check UTC weirdness.
        // These have more error because of additional multiplications and additions
        // up to 2 ULPs or ulp(60.0) of error.
        FieldAbsoluteDate<T> d = new FieldAbsoluteDate<>(field, 1966, 1, 1, utc);
        double ratePost = 0.0025920 / Constants.JULIAN_DAY;
        double factorPost = ratePost / (1 + ratePost);
        double ratePre = 0.0012960 / Constants.JULIAN_DAY;
        double factorPre = ratePre / (1 + ratePre);
        check(d, 1966, 1, 1, 0, 0, 0, 1, 0, 0);
        check(d.shiftedBy(zeroUlp), 1966, 1, 1, 0, 0, 0, 0.5, 0, 0);
        check(d.shiftedBy(attoSecond), 1966, 1, 1, 0, 0, attoSecond, 0.5, 0, 0);
        check(d.shiftedBy(one), 1966, 1, 1, 0, 0, one * (1 - factorPost), 1, 3, 0);
        check(d.shiftedBy(59).shiftedBy(one), 1966, 1, 1, 0, 0, sixty * (1 - factorPost), 1, 2, 0);
        check(d.shiftedBy(86399).shiftedBy(one), 1966, 1, 1, 23, 59, sixty - 86400 * factorPost, 1, 1, 0);
        check(d.shiftedBy(-zeroUlp), 1966, 1, 1, 0, 0, 0, 0.5, 0, 0);
        // actual leap is small ~1e-16, but during a leap rounding up to 60.0 is ok
        check(d.shiftedBy(-oneUlp), 1965, 12, 31, 23, 59, 60.0, 1, 0, 0);
        check(d.shiftedBy(-1).shiftedBy(zeroUlp), 1965, 12, 31, 23, 59, 59 + factorPre, 0.5, 0, 0);
        check(d.shiftedBy(-1).shiftedBy(-zeroUlp), 1965, 12, 31, 23, 59, 59 + factorPre, 0.5, 0, 0);
        check(d.shiftedBy(-1).shiftedBy(-oneUlp), 1965, 12, 31, 23, 59, 59 + factorPre, 0.5, 0, 0);
        check(d.shiftedBy(-1).shiftedBy(-sixtyUlp), 1965, 12, 31, 23, 59, 59 + (1 + sixtyUlp) * factorPre, 0.5, 1, 0);
        // since second ~= 0 there is significant cancellation
        check(d.shiftedBy(-60).shiftedBy(zeroUlp), 1965, 12, 31, 23, 59, 60 * factorPre, 0, 0, sixtyUlp);
        check(d.shiftedBy(-60).shiftedBy(oneUlp), 1965, 12, 31, 23, 59, (oneUlp - oneUlp * factorPre) + 60 * factorPre, 0.5, 0, sixtyUlp);

        // check first whole second leap
        FieldAbsoluteDate<T> d2 = new FieldAbsoluteDate<>(field, 1972, 7, 1, utc);
        check(d2, 1972, 7, 1, 0, 0, 0, 1, 0, 0);
        check(d2.shiftedBy(attoSecond), 1972, 7, 1, 0, 0, attoSecond, 0.5, 0, 0);
        check(d2.shiftedBy(one), 1972, 7, 1, 0, 0, one, 0.5, 0, 0);
        check(d2.shiftedBy(59).shiftedBy(one), 1972, 7, 1, 0, 0, sixty, 1, 1, 0);
        check(d2.shiftedBy(86399).shiftedBy(one), 1972, 7, 1, 23, 59, sixty, 1, 1, 0);
        check(d2.shiftedBy(-zeroUlp), 1972, 7, 1, 0, 0, 0, 0.5, 0, 0);
        check(d2.shiftedBy(-oneUlp), 1972, 6, 30, 23, 59, sixtyOne, 1, 1, 0);
        check(d2.shiftedBy(-1).shiftedBy(zeroUlp), 1972, 6, 30, 23, 59, 60.0, 0.5, 0, 0);
        check(d2.shiftedBy(-1).shiftedBy(-zeroUlp), 1972, 6, 30, 23, 59, 60.0, 0.5, 0, 0);
        check(d2.shiftedBy(-1).shiftedBy(-oneUlp), 1972, 6, 30, 23, 59, 60.0, 0.5, 0, 0);
        check(d2.shiftedBy(-1).shiftedBy(-sixtyUlp), 1972, 6, 30, 23, 59, sixty, 0.5, 0, 0);
        check(d2.shiftedBy(-61).shiftedBy(attoSecond), 1972, 6, 30, 23, 59, attoSecond, 0.5, 0, 0);

        // check first leap second, which was actually 1.422818 s.
        FieldAbsoluteDate<T> d3 = FieldAbsoluteDate.getArbitraryEpoch(field).shiftedBy(-1230724800);
        check(d3, 1960, 12, 31, 23, 59, 60, 0.5, 0, 0);
        FieldAbsoluteDate<T> d4 = new FieldAbsoluteDate<>(field, 1961, 1, 1, utc);
        check(d4, 1961, 1, 1, 0, 0, 0, 0.5, 0, 0);
        // FIXME something wrong because a date a smidgen before 1961-01-01 is not in a leap second
        //check(d4.shiftedBy(-oneUlp), 1960, 12, 31, 23, 59, 61.422818, 0.5, 0, 0);

        // check NaN, this is weird that NaNs have valid ymdhm, but not second.
        DateTimeComponents actual = date.shiftedBy(Double.NaN).getComponents(utc);
        DateComponents dc = actual.getDate();
        TimeComponents tc = actual.getTime();
        MatcherAssert.assertThat(dc.getYear(), CoreMatchers.is(2000));
        MatcherAssert.assertThat(dc.getMonth(), CoreMatchers.is(1));
        MatcherAssert.assertThat(dc.getDay(), CoreMatchers.is(1));
        MatcherAssert.assertThat(tc.getHour(), CoreMatchers.is(0));
        MatcherAssert.assertThat(tc.getMinute(), CoreMatchers.is(0));
        MatcherAssert.assertThat("second", tc.getSecond(), CoreMatchers.is(Double.NaN));
        MatcherAssert.assertThat(tc.getMinutesFromUTC(), CoreMatchers.is(0));
        final double difference = new FieldAbsoluteDate<>(field, actual, utc).durationFrom(date).getReal();
        MatcherAssert.assertThat(difference, CoreMatchers.is(Double.NaN));
    }

    private <T extends CalculusFieldElement<T>> void doTestNegativeOffsetConstructor(final Field<T> field) {
        FieldAbsoluteDate<T> date = new FieldAbsoluteDate<>(field,
                                                            2019, 10, 11, 20, 40,
                                                            FastMath.scalb(6629298651489277.0, -55),
                                                            TimeScalesFactory.getTT());
        FieldAbsoluteDate<T> after = date.shiftedBy(Precision.EPSILON);
        Assertions.assertEquals(624098367L, date.toAbsoluteDate().getSeconds());
        Assertions.assertEquals(FastMath.nextAfter(1.0, Double.NEGATIVE_INFINITY),
                                1.0e-18 * date.toAbsoluteDate().getAttoSeconds(), 2.4e-15);
        Assertions.assertEquals(Precision.EPSILON, after.durationFrom(date).getReal(), 1.0e-18);
    }

    private <T extends CalculusFieldElement<T>> void doTestNegativeOffsetShift(final Field<T> field) {
        FieldAbsoluteDate<T> reference = new FieldAbsoluteDate<>(field, 2019, 10, 11, 20, 40, 1.6667019180022178E-7,
                                                                 TimeScalesFactory.getTAI());
        T dt = field.getZero().newInstance(FastMath.scalb(6596520010750484.0, -39));
        FieldAbsoluteDate<T> shifted = reference.shiftedBy(dt);
        FieldAbsoluteDate<T> after = shifted.shiftedBy(Precision.EPSILON);
        Assertions.assertEquals(624110398L, shifted.toAbsoluteDate().getSeconds());
        Assertions.assertEquals((1.0 - 1.6922e-13) * 1.0e18, shifted.toAbsoluteDate().getAttoSeconds(), 1.0e-15);
        Assertions.assertEquals(Precision.EPSILON, after.durationFrom(shifted).getReal(), 1.0e-18);
    }

    private <T extends CalculusFieldElement<T>> void doTestDurationFromWithTimeUnit(final Field<T> field) {
        FieldAbsoluteDate<T> reference = new FieldAbsoluteDate<>(field, 2023, 1, 1, 12, 13, 59.12334567, utc);
        for (TimeUnit timeUnit : TimeUnit.values()) {
            Assertions.assertEquals(field.getZero(), reference.durationFrom(reference, timeUnit));

            long dayInTimeUnit = timeUnit.convert((long) Constants.JULIAN_DAY, TimeUnit.SECONDS);
            for (int i = 1; i <= 365; i++) {
                FieldAbsoluteDate<T> minusDays = reference.shiftedBy(-i * Constants.JULIAN_DAY);
                FieldAbsoluteDate<T> plusDays = reference.shiftedBy(i* Constants.JULIAN_DAY);


                Assertions.assertEquals(field.getZero().add(i * dayInTimeUnit), reference.durationFrom(minusDays, timeUnit));

                Assertions.assertEquals(field.getZero().add(-i * dayInTimeUnit), reference.durationFrom(plusDays, timeUnit));

                AbsoluteDate minusDaysA = minusDays.toAbsoluteDate();
                AbsoluteDate plusDaysA = plusDays.toAbsoluteDate();

                Assertions.assertEquals(field.getZero().add(i * dayInTimeUnit), reference.durationFrom(minusDaysA, timeUnit));

                Assertions.assertEquals(field.getZero().add(-i * dayInTimeUnit), reference.durationFrom(plusDaysA, timeUnit));
            }

            for (long ns = 1; ns <= 1_000_000_000; ns += 1_000_000) {
                FieldAbsoluteDate<T> minus = reference.shiftedBy(-1e-9 * ns);
                FieldAbsoluteDate<T> plus = reference.shiftedBy(1e-9 * ns);

                double deltaInTimeUnit = ns / (double) timeUnit.toNanos(1);
                Assertions.assertEquals(field.getZero().add(FastMath.round(deltaInTimeUnit)), reference.durationFrom(minus, timeUnit),
                    String.format("TimeUnit: %s, ns: %d", timeUnit, ns));

                Assertions.assertEquals(field.getZero().add(FastMath.round(-deltaInTimeUnit)), reference.durationFrom(plus, timeUnit),
                    String.format("TimeUnit: %s, ns: %d", timeUnit, ns));

                AbsoluteDate minusA = minus.toAbsoluteDate();
                AbsoluteDate plusA = plus.toAbsoluteDate();

                Assertions.assertEquals(field.getZero().add(FastMath.round(deltaInTimeUnit)), reference.durationFrom(minusA, timeUnit),
                    String.format("TimeUnit: %s, ns: %d", timeUnit, ns));

                Assertions.assertEquals(field.getZero().add(FastMath.round(-deltaInTimeUnit)), reference.durationFrom(plusA, timeUnit),
                    String.format("TimeUnit: %s, ns: %d", timeUnit, ns));
            }


        }
    }

    public <T extends CalculusFieldElement<T>> void doTestConstructWithTimeUnitOffset(final Field<T> field) {
        FieldAbsoluteDate<T> reference = new FieldAbsoluteDate<>(field, 2023, 1, 1, 12, 13, 59.12334567, utc);

        for (TimeUnit timeUnit : TimeUnit.values()) {
            Assertions.assertEquals(0,
                FastMath.abs(reference.durationFrom(new FieldAbsoluteDate<>(reference, 0, timeUnit)).getReal()), 1e-10);

            long dayInTimeUnit = timeUnit.convert((long) Constants.JULIAN_DAY, TimeUnit.SECONDS);
            for (int i = 1; i <= 365; i++) {
                FieldAbsoluteDate<T> minusDays = reference.shiftedBy(-i * Constants.JULIAN_DAY);
                FieldAbsoluteDate<T> plusDays = reference.shiftedBy(i* Constants.JULIAN_DAY);

                Assertions.assertEquals(0,
                    FastMath.abs(reference.durationFrom(new FieldAbsoluteDate<>(minusDays, i * dayInTimeUnit, timeUnit)).getReal()),
                    1e-10,
                    String.format("TimeUnit: %s", timeUnit));
                Assertions.assertEquals(0,
                    FastMath.abs(reference.durationFrom(new FieldAbsoluteDate<>(plusDays, -i * dayInTimeUnit, timeUnit)).getReal()),
                    1e-10,
                    String.format("TimeUnit: %s", timeUnit));

                Assertions.assertEquals(0,
                    FastMath.abs(reference.durationFrom(new FieldAbsoluteDate<>(minusDays.toAbsoluteDate(), i * dayInTimeUnit, timeUnit, field)).getReal()),
                    1e-10,
                    String.format("TimeUnit: %s", timeUnit));
                Assertions.assertEquals(0,
                    FastMath.abs(reference.durationFrom(new FieldAbsoluteDate<>(plusDays.toAbsoluteDate(), -i * dayInTimeUnit, timeUnit, field)).getReal()),
                    1e-10,
                    String.format("TimeUnit: %s", timeUnit));
            }

            for (long ns = 1; ns <= 1_000_000_000; ns += 1_000_000) {
                if (timeUnit.convert(1, TimeUnit.SECONDS) < 1) {
                    //Skip everything larger than one second
                    continue;
                }
                FieldAbsoluteDate<T> minus = reference.shiftedBy(-1e-9 * ns);
                FieldAbsoluteDate<T> plus = reference.shiftedBy(1e-9 * ns);

                double deltaInTimeUnit =  ns / (double) timeUnit.toNanos(1);
                Assertions.assertEquals(0,
                    FastMath.abs(reference.durationFrom(new FieldAbsoluteDate<>(minus, FastMath.round(deltaInTimeUnit), timeUnit)).getReal()),
                    1.0 / timeUnit.convert(1, TimeUnit.SECONDS),
                    String.format("TimeUnit: %s, ns: %d", timeUnit, ns));
                Assertions.assertEquals(0,
                    FastMath.abs(reference.durationFrom(new FieldAbsoluteDate<>(plus, FastMath.round(-deltaInTimeUnit), timeUnit)).getReal()),
                    1.0 / timeUnit.convert(1, TimeUnit.SECONDS),
                    String.format("TimeUnit: %s, ns: %d", timeUnit, ns));

                Assertions.assertEquals(0,
                    FastMath.abs(reference.durationFrom(new FieldAbsoluteDate<>(minus.toAbsoluteDate(), FastMath.round(deltaInTimeUnit), timeUnit, field)).getReal()),
                    1.0 / timeUnit.convert(1, TimeUnit.SECONDS),
                    String.format("TimeUnit: %s, ns: %d", timeUnit, ns));
                Assertions.assertEquals(0,
                    FastMath.abs(reference.durationFrom(new FieldAbsoluteDate<>(plus.toAbsoluteDate(), FastMath.round(-deltaInTimeUnit), timeUnit, field)).getReal()),
                    1.0 / timeUnit.convert(1, TimeUnit.SECONDS),
                    String.format("TimeUnit: %s, ns: %d", timeUnit, ns));
            }
        }
    }

    public <T extends CalculusFieldElement<T>> void doTestShiftedByWithTimeUnit(final Field<T> field) {
        FieldAbsoluteDate<T> reference = new FieldAbsoluteDate<>(field, 2023, 1, 1, 12, 13, 59.12334567, utc);

        for (TimeUnit timeUnit : TimeUnit.values()) {
            Assertions.assertEquals(0,
                FastMath.abs(reference.durationFrom(reference.shiftedBy(0, timeUnit)).getReal()), 1e-10);

            long dayInTimeUnit = timeUnit.convert((long) Constants.JULIAN_DAY, TimeUnit.SECONDS);
            for (int i = 1; i <= 365; i++) {
                FieldAbsoluteDate<T> minusDays = reference.shiftedBy(-i * Constants.JULIAN_DAY);
                FieldAbsoluteDate<T> plusDays = reference.shiftedBy(i* Constants.JULIAN_DAY);

                Assertions.assertEquals(0,
                    FastMath.abs(reference.durationFrom(minusDays.shiftedBy( i * dayInTimeUnit, timeUnit)).getReal()),
                    1e-10,
                    String.format("TimeUnit: %s", timeUnit));
                Assertions.assertEquals(0,
                    FastMath.abs(reference.durationFrom(plusDays.shiftedBy( -i * dayInTimeUnit, timeUnit)).getReal()),
                    1e-10,
                    String.format("TimeUnit: %s", timeUnit));

            }

            for (long ns = 1; ns <= 1_000_000_000; ns += 1_000_000) {
                if (timeUnit.convert(1, TimeUnit.SECONDS) < 1) {
                    //Skip everything larger than one second
                    continue;
                }
                FieldAbsoluteDate<T> minus = reference.shiftedBy(-1e-9 * ns);
                FieldAbsoluteDate<T> plus = reference.shiftedBy(1e-9 * ns);

                double deltaInTimeUnit =  ns / (double) timeUnit.toNanos(1);
                Assertions.assertEquals(0,
                    FastMath.abs(reference.durationFrom(minus.shiftedBy(FastMath.round(deltaInTimeUnit), timeUnit)).getReal()),
                    1.0 / timeUnit.convert(1, TimeUnit.SECONDS),
                    String.format("TimeUnit: %s, ns: %d", timeUnit, ns));
                Assertions.assertEquals(0,
                    FastMath.abs(reference.durationFrom(plus.shiftedBy(FastMath.round(-deltaInTimeUnit), timeUnit)).getReal()),
                    1.0 / timeUnit.convert(1, TimeUnit.SECONDS),
                    String.format("TimeUnit: %s, ns: %d", timeUnit, ns));
            }
        }
    }

    public <T extends CalculusFieldElement<T>> void doTestToStringWithoutUtcOffset(final Field<T> field) {
        // setup
        FieldAbsoluteDate<T> date = new FieldAbsoluteDate<>(field,2009, 1, 1, utc);
        double one = FastMath.nextDown(1.0);
        double zeroUlp = FastMath.nextUp(0.0);
        double oneUlp = FastMath.ulp(1.0);
        //double sixty = FastMath.nextDown(60.0);
        double sixtyUlp = FastMath.ulp(60.0);

        // action
        // test midnight
        checkToStringNoOffset(date, "2009-01-01T00:00:00.000");
        checkToStringNoOffset(date.shiftedBy(1), "2009-01-01T00:00:01.000");
        // test digits and rounding
        checkToStringNoOffset(date.shiftedBy(12.3456789123456789), "2009-01-01T00:00:12.346");
        checkToStringNoOffset(date.shiftedBy(0.0123456789123456789), "2009-01-01T00:00:00.012");
        // test min and max values
        checkToStringNoOffset(date.shiftedBy(zeroUlp), "2009-01-01T00:00:00.000");
        // Orekit 10.1 rounds up
        checkToStringNoOffset(date.shiftedBy(59.0).shiftedBy(one), "2009-01-01T00:01:00.000");
        // Orekit 10.1 rounds up
        checkToStringNoOffset(date.shiftedBy(86399).shiftedBy(one), "2009-01-02T00:00:00.000");
        checkToStringNoOffset(date.shiftedBy(oneUlp), "2009-01-01T00:00:00.000");
        checkToStringNoOffset(date.shiftedBy(one), "2009-01-01T00:00:01.000");
        checkToStringNoOffset(date.shiftedBy(-zeroUlp), "2009-01-01T00:00:00.000");
        // test leap
        // Orekit 10.1 throw OIAE, 10.2 rounds up
        checkToStringNoOffset(date.shiftedBy(-oneUlp), "2009-01-01T00:00:00.000");
        // Orekit 10.1 rounds up
        checkToStringNoOffset(date.shiftedBy(-1).shiftedBy(one), "2009-01-01T00:00:00.000");
        checkToStringNoOffset(date.shiftedBy(-0.5), "2008-12-31T23:59:60.500");
        checkToStringNoOffset(date.shiftedBy(-1).shiftedBy(zeroUlp), "2008-12-31T23:59:60.000");
        checkToStringNoOffset(date.shiftedBy(-1), "2008-12-31T23:59:60.000");
        checkToStringNoOffset(date.shiftedBy(-1).shiftedBy(-zeroUlp), "2008-12-31T23:59:60.000");
        checkToStringNoOffset(date.shiftedBy(-1).shiftedBy(-oneUlp), "2008-12-31T23:59:60.000");
        checkToStringNoOffset(date.shiftedBy(-2), "2008-12-31T23:59:59.000");
        // Orekit 10.1 rounds up
        checkToStringNoOffset(date.shiftedBy(-1).shiftedBy(-sixtyUlp), "2008-12-31T23:59:60.000");
        checkToStringNoOffset(date.shiftedBy(-61).shiftedBy(zeroUlp), "2008-12-31T23:59:00.000");
        checkToStringNoOffset(date.shiftedBy(-61).shiftedBy(oneUlp), "2008-12-31T23:59:00.000");
    }


    private <T extends CalculusFieldElement<T>> void checkToStringNoOffset(final FieldAbsoluteDate<T> d, final String s) {
        MatcherAssert.assertThat(d.toStringWithoutUtcOffset(utc, 3), CoreMatchers.is(s));
        MatcherAssert.assertThat(
            d.getComponents(utc).toStringWithoutUtcOffset(utc.minuteDuration(d), 3),
            CoreMatchers.is(s));
    }

    private <T extends CalculusFieldElement<T>> void check(FieldAbsoluteDate<T> date,
                                                           int year, int month, int day, int hour, int minute, double second,
                                                           double roundTripUlps, final int secondUlps, final double absTol) {
        DateTimeComponents actual = date.getComponents(utc);
        DateComponents d = actual.getDate();
        TimeComponents t = actual.getTime();
        MatcherAssert.assertThat(d.getYear(), CoreMatchers.is(year));
        MatcherAssert.assertThat(d.getMonth(), CoreMatchers.is(month));
        MatcherAssert.assertThat(d.getDay(), CoreMatchers.is(day));
        MatcherAssert.assertThat(t.getHour(), CoreMatchers.is(hour));
        MatcherAssert.assertThat(t.getMinute(), CoreMatchers.is(minute));
        MatcherAssert.assertThat("second", t.getSecond(),
                OrekitMatchers.numberCloseTo(second, absTol, secondUlps));
        MatcherAssert.assertThat(t.getMinutesFromUTC(), CoreMatchers.is(0));
        final double tol = FastMath.ulp(second) * roundTripUlps;
        final double difference = new FieldAbsoluteDate<>(date.getField(), actual, utc)
                .durationFrom(date).getReal();
        MatcherAssert.assertThat(difference,
                OrekitMatchers.closeTo(0, FastMath.max(absTol, tol)));
    }

    static class AnyFieldTimeStamped<T extends CalculusFieldElement<T>> implements FieldTimeStamped<T> {
        AbsoluteDate date;
        Field<T> field;

        public AnyFieldTimeStamped(Field<T> field, AbsoluteDate date) {
            this.date = date;
            this.field = field;
        }

        @Override
        public FieldAbsoluteDate<T> getDate() {
            return new FieldAbsoluteDate<>(field, date);
        }
    }

    static class TestDates<T extends CalculusFieldElement<T>> {
        private final AbsoluteDate presentDate;
        private final AnyFieldTimeStamped<T> present;
        private final AnyFieldTimeStamped<T> past;
        private final AnyFieldTimeStamped<T> presentToo;
        private final AnyFieldTimeStamped<T> future;

        public TestDates(Field<T> field) {
            presentDate = new AbsoluteDate(new DateComponents(2000, 1, 1),
                                                new TimeComponents(12, 0, 0),
                                                TimeScalesFactory.getUTC());
            present    = new AnyFieldTimeStamped<>(field, presentDate);
            presentToo = new AnyFieldTimeStamped<>(field, presentDate.shiftedBy(0));
            past       = new AnyFieldTimeStamped<>(field, presentDate.shiftedBy(-1000));
            future     = new AnyFieldTimeStamped<>(field, presentDate.shiftedBy(1000));
        }

        public FieldAbsoluteDate<T> getPresentFieldAbsoluteDate() {
            return present.getDate();
        }
    }

    @Test
    public void doTestGetJulianDatesWithBinar64() {
        // GIVEN
        final Field<Binary64> field = Binary64Field.getInstance();

        // WHEN & THEN
        doTestGetJulianDates(field);
    }

    public <T extends CalculusFieldElement<T>> void doTestGetJulianDates(Field<T> field) {
        // GIVEN a reference date
        final T one = field.getOne();
        final TimeScale utc = TimeScalesFactory.getUTC();

        FieldAbsoluteDate<T> reference = new FieldAbsoluteDate<>(field, 2024, 7, 4, 13, 0, 0, utc);
        FieldAbsoluteDate<T> referenceFromJDMethod =
                FieldAbsoluteDate.createJDDate(2460496, one.multiply(0.0416667 * Constants.JULIAN_DAY), utc);
        FieldAbsoluteDate<T> referenceFromMJDMethod =
                FieldAbsoluteDate.createMJDDate(60495, one.multiply(0.54166670 * Constants.JULIAN_DAY), utc);

        // WHEN converting it to Julian Date or Modified Julian Date
        T mjdDateDefaultData = reference.getMJD();
        T jdDateDefaultData  = reference.getJD();
        T mjdDate = reference.getMJD(utc);
        T jdDate  = reference.getJD(utc);

        // THEN
        // source : Time/Date Converter - HEASARC - NASA
        Assertions.assertEquals(2460496.0416667, jdDateDefaultData.getReal(), 1.0e-6);
        Assertions.assertEquals(60495.54166670, mjdDateDefaultData.getReal(), 1.0e-6);
        Assertions.assertEquals(jdDate, jdDateDefaultData);
        Assertions.assertEquals(mjdDate, mjdDateDefaultData);

        // Assert that static method are correct when creating date from JD or MJD
        Assertions.assertTrue(reference.isCloseTo(referenceFromJDMethod, 1e-2));
        Assertions.assertTrue(reference.isCloseTo(referenceFromMJDMethod, 1e-2));
    }

    @Test
    void testGetJD() {
        // GIVEN
        final AbsoluteDate date = AbsoluteDate.ARBITRARY_EPOCH;
        final FieldAbsoluteDate<UnivariateDerivative1> fieldDate = new FieldAbsoluteDate<>(UnivariateDerivative1Field.getInstance(),
                date).shiftedBy(new UnivariateDerivative1(0., 1));
        // WHEN
        final UnivariateDerivative1 jdField = fieldDate.getJD();
        // THEN
        final double shift = 10.;
        final FieldAbsoluteDate<UnivariateDerivative1> shiftedDate = fieldDate.shiftedBy(shift);
        final double expectedJdDerivative = (shiftedDate.getJD().getReal() - jdField.getReal()) / shift;
        Assertions.assertEquals(expectedJdDerivative, jdField.getFirstDerivative(), 1e-10);
    }

    @Test
    void testGetMJD() {
        // GIVEN
        final AbsoluteDate date = AbsoluteDate.ARBITRARY_EPOCH;
        final FieldAbsoluteDate<UnivariateDerivative1> fieldDate = new FieldAbsoluteDate<>(UnivariateDerivative1Field.getInstance(),
                date).shiftedBy(new UnivariateDerivative1(0., 1));
        // WHEN
        final UnivariateDerivative1 mjdField = fieldDate.getMJD();
        // THEN
        final double shift = 10.;
        final FieldAbsoluteDate<UnivariateDerivative1> shiftedDate = fieldDate.shiftedBy(shift);
        final double expectedMjdDerivative = (shiftedDate.getMJD().getReal() - mjdField.getReal()) / shift;
        Assertions.assertEquals(expectedMjdDerivative, mjdField.getFirstDerivative(), 1e-10);
    }

    @Test
    void testMedian() {
        final AbsoluteDate date1 = new AbsoluteDate(2003, 6, 13, 14, 15,
                                                    new TimeOffset(53, TimeOffset.SECOND, 12, TimeOffset.ATTOSECOND),
                                                    TimeScalesFactory.getTT());
        final FieldAbsoluteDate<UnivariateDerivative1> fieldDate1 =
            new FieldAbsoluteDate<>(UnivariateDerivative1Field.getInstance(), date1).
            shiftedBy(new UnivariateDerivative1(0.0, 1.0));
        final AbsoluteDate date2 = new AbsoluteDate(2003, 6, 13, 14, 17,
                                                    new TimeOffset(25, TimeOffset.SECOND, 120, TimeOffset.ATTOSECOND),
                                                    TimeScalesFactory.getTT());
        final FieldAbsoluteDate<UnivariateDerivative1> fieldDate2 =
            new FieldAbsoluteDate<>(UnivariateDerivative1Field.getInstance(), date2).
            shiftedBy(new UnivariateDerivative1(0.0, 2.0));
        final AbsoluteDate dateM = new AbsoluteDate(2003, 6, 13, 14, 16,
                                                 new TimeOffset(39, TimeOffset.SECOND, 66, TimeOffset.ATTOSECOND),
                                                 TimeScalesFactory.getTT());
        final FieldAbsoluteDate<UnivariateDerivative1> fieldDateM =
            new FieldAbsoluteDate<>(UnivariateDerivative1Field.getInstance(), dateM).
            shiftedBy(new UnivariateDerivative1(0.0, 1.5));
        Assertions.assertEquals(fieldDateM, FieldAbsoluteDate.createMedian(fieldDate1, fieldDate2));
        Assertions.assertEquals(fieldDateM, FieldAbsoluteDate.createMedian(fieldDate2, fieldDate1));
    }

    @Test
    void testMedianInfinite() {
        final FieldAbsoluteDate<Binary64> future    = FieldAbsoluteDate.getFutureInfinity(Binary64Field.getInstance());
        final FieldAbsoluteDate<Binary64> past      = FieldAbsoluteDate.getPastInfinity(Binary64Field.getInstance());
        final FieldAbsoluteDate<Binary64> arbitrary = FieldAbsoluteDate.getArbitraryEpoch(Binary64Field.getInstance());
        Assertions.assertEquals(future, FieldAbsoluteDate.createMedian(future, arbitrary));
        Assertions.assertEquals(past,   FieldAbsoluteDate.createMedian(past,   arbitrary));
    }

    @Test
    void testToFUD1Field() {
        // GIVEN
        final Field<Binary64> field = Binary64Field.getInstance();
        final FieldAbsoluteDate<Binary64> date = FieldAbsoluteDate.getArbitraryEpoch(field);
        // WHEN
        final FieldAbsoluteDate<FieldUnivariateDerivative1<Binary64>> ud1Date = date.toFUD1Field();
        // THEN
        Assertions.assertEquals(date.toAbsoluteDate(), ud1Date.toAbsoluteDate());
        final FieldUnivariateDerivative1<Binary64> shift = ud1Date.durationFrom(date.toAbsoluteDate());
        Assertions.assertEquals(field.getOne(), shift.getFirstDerivative());
    }

    @Test
    void testToFUD2Field() {
        // GIVEN
        final Field<Binary64> field = Binary64Field.getInstance();
        final FieldAbsoluteDate<Binary64> date = FieldAbsoluteDate.getArbitraryEpoch(field);
        // WHEN
        final FieldAbsoluteDate<FieldUnivariateDerivative2<Binary64>> ud2Date = date.toFUD2Field();
        // THEN
        Assertions.assertEquals(date.toAbsoluteDate(), ud2Date.toAbsoluteDate());
        final FieldUnivariateDerivative2<Binary64> shift = ud2Date.durationFrom(date.toAbsoluteDate());
        Assertions.assertEquals(field.getOne(), shift.getFirstDerivative());
        Assertions.assertEquals(field.getZero(), shift.getSecondDerivative());
    }

    private <T extends CalculusFieldElement<T>> void doTestGetDayOfYear(final Field<T> field) {
        Assertions.assertEquals(0.501,
                                new FieldAbsoluteDate<>(field,
                                                        new AbsoluteDate(2004,  1,  1,  0,  0,  0.001, utc)).
                                    getDayOfYear(utc).getReal(),
                                1.0e-3);
        Assertions.assertEquals(1.000,
                                new FieldAbsoluteDate<>(field,
                                                        new AbsoluteDate(2004,  1,  1, 12,  0,  0.000, utc)).
                                    getDayOfYear(utc).getReal(),
                                1.0e-3);
        Assertions.assertEquals(366.0,
                                new FieldAbsoluteDate<>(field,
                                                        new AbsoluteDate(2004, 12, 31, 12,  0,  0.000, utc)).
                                    getDayOfYear(utc).getReal(),
                                1.0e-3);
        Assertions.assertEquals(366.499999988426,
                                new FieldAbsoluteDate<>(field,
                                                        new AbsoluteDate(2004, 12, 31, 23, 59, 59.999, utc)).
                                    getDayOfYear(utc).getReal(),
                                1.0e-12);
        Assertions.assertEquals(0.500000011574,
                                new FieldAbsoluteDate<>(field,
                                                        new AbsoluteDate(2004, 12, 31, 23, 59, 59.999, utc).shiftedBy(0.002)).
                                    getDayOfYear(utc).getReal(),
                                1.0e-12);
    }

}
