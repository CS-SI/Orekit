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
package org.orekit.data;


import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.hipparchus.analysis.UnivariateFunction;
import org.hipparchus.analysis.differentiation.DerivativeStructure;
import org.hipparchus.analysis.differentiation.FiniteDifferencesDifferentiator;
import org.hipparchus.analysis.differentiation.UnivariateDifferentiableFunction;
import org.hipparchus.util.FastMath;
import org.junit.Assert;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.FramesFactory;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;


public class PoissonSeriesParserTest {

    @Test(expected=OrekitException.class)
    public void testEmptyData() throws OrekitException {
        buildData("");
    }

    @Test(expected=OrekitException.class)
    public void testNoCoeffData() throws OrekitException {
        buildData("this is NOT an IERS nutation model file\n");
    }

    @Test(expected=OrekitException.class)
    public void testEmptyArrayData() throws OrekitException {
        buildData("  0.0 + 0.0 t - 0.0 t^2 - 0.0 t^3 - 0.0 t^4 + 0.0 t^5\n");
    }

    @Test(expected=OrekitException.class)
    public void testMissingTermData() throws OrekitException {
        buildData("  0.0 + 0.0 t - 0.0 t^2 - 0.0 t^3 - 0.0 t^4 + 0.0 t^5\n"
                  + "j = 0  Nb of terms = 1\n");
    }

    private PoissonSeries<DerivativeStructure> buildData(String data) throws OrekitException {
        return new PoissonSeriesParser<DerivativeStructure>(0).
                withPolynomialPart('t', PolynomialParser.Unit.NO_UNITS).
                parse(new ByteArrayInputStream(data.getBytes()),
                                                             "<file-content>" + data + "</file-content>");
    }

    @Test(expected=OrekitException.class)
    public void testNoFile() throws OrekitException {
        InputStream stream =
                PoissonSeriesParserTest.class.getResourceAsStream("/org/orekit/resources/missing");
        new PoissonSeriesParser<DerivativeStructure>(17).
            withPolynomialPart('t', PolynomialParser.Unit.NO_UNITS).
            withFirstDelaunay(4).
            withFirstPlanetary(9).
            withSinCos(0, 2, 1.0, 3, 1.0).
            parse(stream, "missing");
    }

    @Test
    public void testMissingSeries() throws OrekitException {
        try {
            String data =
                    "  0.0 + 0.0 x - 0.0 x^2 - 0.0 x^3 - 0.0 x^4 + 0.0 x^5\n"
                    + "j = 0  Nb of terms = 1\n"
                    + "1 1.0 0.0 0 0 0 0 1 0 0 0 0 0 0 0 0 0\n"
                    + "j = 1  Nb of terms = 1\n"
                    + "2 1.0 0.0 0 0 0 0 1 0 0 0 0 0 0 0 0 0\n"
                    + "j = 3  Nb of terms = 1\n"
                    + "3 1.0 0.0 0 0 0 0 1 0 0 0 0 0 0 0 0 0\n";
            new PoissonSeriesParser<DerivativeStructure>(17).
                withPolynomialPart('x', PolynomialParser.Unit.NO_UNITS).
                withFirstDelaunay(4).
                withFirstPlanetary(9).
                withSinCos(0, 2, 1.0, 3, 1.0).
                parse(new ByteArrayInputStream(data.getBytes()), "");
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.MISSING_SERIE_J_IN_FILE, oe.getSpecifier());
            Assert.assertEquals(2, oe.getParts()[0]);
            Assert.assertEquals(6, oe.getParts()[2]);
        }
    }

    @Test
    public void testMissingTerms() throws OrekitException {
        try {
            String data =
                    "  0.0 + 0.0 x - 0.0 x^2 - 0.0 x^3 - 0.0 x^4 + 0.0 x^5\n"
                    + "j = 0  Nb of terms = 1\n"
                    + "1 1.0 0.0 0 0 0 0 1 0 0 0 0 0 0 0 0 0\n"
                    + "j = 1  Nb of terms = 3\n"
                    + "2 1.0 0.0 0 0 0 0 1 0 0 0 0 0 0 0 0 0\n"
                    + "3 1.0 0.0 0 0 0 0 0 2 0 0 0 0 0 0 0 0\n"
                    + "j = 2  Nb of terms = 1\n"
                    + "4 1.0 0.0 0 0 0 0 1 0 0 0 0 0 0 0 0 0\n";
            new PoissonSeriesParser<DerivativeStructure>(17).
                withPolynomialPart('x', PolynomialParser.Unit.NO_UNITS).
                withFirstDelaunay(4).
                withFirstPlanetary(9).
                withSinCos(0, 2, 1.0, 3, 1.0).
                parse(new ByteArrayInputStream(data.getBytes()), "");
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.NOT_A_SUPPORTED_IERS_DATA_FILE, oe.getSpecifier());
        }
    }

    @Test
    public void testSmall() throws OrekitException {
        String data =
            "  0.0 + 0.0 x - 0.0 x^2 - 0.0 x^3 - 0.0 x^4 + 0.0 x^5\n"
            + "j = 0  Nb of terms = 1\n"
            + "1 1.0 0.0 0 0 0 0 1 0 0 0 0 0 0 0 0 0\n";
        PoissonSeries<DerivativeStructure> nd = new PoissonSeriesParser<DerivativeStructure>(17).
                               withPolynomialPart('x', PolynomialParser.Unit.NO_UNITS).
                               withFirstDelaunay(4).
                               withFirstPlanetary(9).
                               withSinCos(0, 2, 1.0, 3, 1.0).
                               parse(new ByteArrayInputStream(data.getBytes()), "");
        Assert.assertEquals(1, nd.getNonPolynomialSize());
    }

    @Test
    public void testSecondsMarkers() throws OrekitException {
        String data =
            "  0''.0 + 0''.0 t - 0''.0 t^2 - 0''.0 t^3 - 0''.0 t^4 + 0''.0 t^5\n"
            + "j = 0  Nb of terms = 1\n"
            + "1 1.0 0.0 0 0 0 0 1 0 0 0 0 0 0 0 0 0\n";
        PoissonSeries<DerivativeStructure> nd = new PoissonSeriesParser<DerivativeStructure>(17).
                               withFirstPlanetary(9).
                               withSinCos(0, 2, 1.0, 3, 1.0).
                               withPolynomialPart('t', PolynomialParser.Unit.NO_UNITS).
                               withFirstDelaunay(4).
                               parse(new ByteArrayInputStream(data.getBytes()), "");
        Assert.assertEquals(1, nd.getNonPolynomialSize());
    }

    @Test
    public void testExtract() throws OrekitException {
        String data =
            "Expression for the X coordinate of the CIP in the GCRS based on the IAU2000A\n"
            + "precession-nutation model\n"
            + "\n"
            + "\n"
            + "----------------------------------------------------------------------\n"
            + "\n"
            + "X = polynomial part + non-polynomial part\n"
            + "\n"
            + "----------------------------------------------------------------------\n"
            + "\n"
            + "Polynomial part (unit microarcsecond)\n"
            + "\n"
            + "  -16616.99 + 2004191742.88 t - 427219.05 t^2 - 198620.54 t^3 - 46.05 t^4 + 5.98 t^5\n"
            + "\n"
            + "----------------------------------------------------------------------\n"
            + "\n"
            + "Non-polynomial part (unit microarcsecond)\n"
            + "(ARG being for various combination of the fundamental arguments of the nutation theory)\n"
            + "\n"
            + "  Sum_i[a_{s,0})_i * sin(ARG) + a_{c,0})_i * cos(ARG)] \n"
            + "\n"
            + "+ Sum_i)j=1,4 [a_{s,j})_i * t^j * sin(ARG) + a_{c,j})_i * cos(ARG)] * t^j]\n"
            + "\n"
            + "The Table below provides the values for a_{s,j})_i and a_{c,j})_i\n"
            + "\n"
            + "The expressions for the fundamental arguments appearing in columns 4 to 8 (luni-solar part) \n"
            + "and in columns 6 to 17 (planetary part) are those of the IERS Conventions 2000\n"
            + "\n"
            + "----------------------------------------------------------------------\n"
            + "\n"
            + "    i    a_{s,j})_i      a_{c,j})_i    l    l'   F    D   Om L_Me L_Ve  L_E L_Ma  L_J L_Sa  L_U L_Ne  p_A\n"
            + "\n"
            + "----------------------------------------------------------------------\n"
            + "-16616.99 + 2004191742.88 t - 427219.05 t^2 - 198620.54 t^3 - 46.05 t^4 + 5.98 t^5\n"
            + "j = 0  Nb of terms = 2\n"
            + "\n"
            + "   1    -6844318.44        1328.67    0    0    0    0    1    0    0    0    0    0    0    0    0    0\n"
            + "   2           0.11           0.00    0    0    4   -4    4    0    0    0    0    0    0    0    0    0\n"
            + "\n"
            + "j = 1  Nb of terms = 2\n"
            + "\n"
            + "   3       -3328.48      205833.15    0    0    0    0    1    0    0    0    0    0    0    0    0    0\n"
            + "   4           0.00          -0.10    1   -1   -2   -2   -1    0    0    0    0    0    0    0    0    0\n"
            + "\n"
            + " j = 2  Nb of terms = 2\n"
            + "\n"
            + "   5        2038.00          82.26    0    0    0    0    1    0    0    0    0    0    0    0    0    0\n"
            + "   6          -0.12           0.00    1    0   -2   -2   -1    0    0    0    0    0    0    0    0    0\n"
            + "  \n"
            + " j = 3  Nb of terms = 2\n"
            + "\n"
            + "   7           1.76         -20.39    0    0    0    0    1    0    0    0    0    0    0    0    0    0\n"
            + "   8           0.00           0.20    0    0    0    0    2    0    0    0    0    0    0    0    0    0\n"
            + "\n"
            + " j = 4  Nb of terms = 1\n"
            + "       \n"
            + "   9          -0.10          -0.02    0    0    0    0    1    0    0    0    0    0    0    0    0    0\n";
        // despite there are 9 data lines above, there are only 5 different terms,
        // as some terms share the same Delaunay and planetary coefficients and are
        // therefore grouped together. The Delaunay arguments for the 5 terms are:
        // Ω, 4(F-D+Ω), l-l'-2(F+D)-Ω, l-2(F+D)-Ω and 2Ω
        Assert.assertEquals(5,
                            new PoissonSeriesParser<DerivativeStructure>(17).
                             withPolynomialPart('t', PolynomialParser.Unit.NO_UNITS).
                             withFirstDelaunay(4).
                             withFirstPlanetary(9).
                             withSinCos(0, 2, 1.0, 3, 1.0).
                             parse(new ByteArrayInputStream(data.getBytes()), "dummy").getNonPolynomialSize());
    }

    @Test
    public void testWrongIndex() throws OrekitException {
        String data =
            "Expression for the X coordinate of the CIP in the GCRS based on the IAU2000A\n"
            + "precession-nutation model\n"
            + "\n"
            + "\n"
            + "----------------------------------------------------------------------\n"
            + "\n"
            + "X = polynomial part + non-polynomial part\n"
            + "\n"
            + "----------------------------------------------------------------------\n"
            + "\n"
            + "Polynomial part (unit microarcsecond)\n"
            + "\n"
            + "  -16616.99 + 2004191742.88 t - 427219.05 t^2 - 198620.54 t^3 - 46.05 t^4 + 5.98 t^5\n"
            + "\n"
            + "----------------------------------------------------------------------\n"
            + "\n"
            + "Non-polynomial part (unit microarcsecond)\n"
            + "(ARG being for various combination of the fundamental arguments of the nutation theory)\n"
            + "\n"
            + "  Sum_i[a_{s,0})_i * sin(ARG) + a_{c,0})_i * cos(ARG)] \n"
            + "\n"
            + "+ Sum_i)j=1,4 [a_{s,j})_i * t^j * sin(ARG) + a_{c,j})_i * cos(ARG)] * t^j]\n"
            + "\n"
            + "The Table below provides the values for a_{s,j})_i and a_{c,j})_i\n"
            + "\n"
            + "The expressions for the fundamental arguments appearing in columns 4 to 8 (luni-solar part) \n"
            + "and in columns 6 to 17 (planetary part) are those of the IERS Conventions 2000\n"
            + "\n"
            + "----------------------------------------------------------------------\n"
            + "\n"
            + "    i    a_{s,j})_i      a_{c,j})_i    l    l'   F    D   Om L_Me L_Ve  L_E L_Ma  L_J L_Sa  L_U L_Ne  p_A\n"
            + "\n"
            + "----------------------------------------------------------------------\n"
            + "-16616.99 + 2004191742.88 t - 427219.05 t^2 - 198620.54 t^3 - 46.05 t^4 + 5.98 t^5\n"
            + "j = 0  Nb of terms = 2\n"
            + "\n"
            + "   1    -6844318.44        1328.67    0    0    0    0    1    0    0    0    0    0    0    0    0    0\n"
            + "   2           0.11           0.00    0    0    4   -4    4    0    0    0    0    0    0    0    0    0\n"
            + "\n"
            + "j = 1  Nb of terms = 2\n"
            + "\n"
            + "   3       -3328.48      205833.15    0    0    0    0    1    0    0    0    0    0    0    0    0    0\n"
            + "   4           0.00          -0.10    1   -1   -2   -2   -1    0    0    0    0    0    0    0    0    0\n"
            + "\n"
            + " j = 2  Nb of terms = 2\n"
            + "\n"
            + "   5        2038.00          82.26    0    0    0    0    1    0    0    0    0    0    0    0    0    0\n"
            + "   6          -0.12           0.00    1    0   -2   -2   -1    0    0    0    0    0    0    0    0    0\n"
            + "  \n"
            + " j = 3  Nb of terms = 2\n"
            + "\n"
            + "   7           1.76         -20.39    0    0    0    0    1    0    0    0    0    0    0    0    0    0\n"
            + " 999           0.00           0.20    0    0    0    0    2    0    0    0    0    0    0    0    0    0\n"
            + "\n"
            + " j = 4  Nb of terms = 1\n"
            + "       \n"
            + "   9          -0.10          -0.02    0    0    0    0    1    0    0    0    0    0    0    0    0    0\n";
        try {
            new PoissonSeriesParser<DerivativeStructure>(17).
                withPolynomialPart('t', PolynomialParser.Unit.NO_UNITS).
                withFirstDelaunay(4).
                withFirstPlanetary(9).
                withSinCos(0, 2, 1.0, 3, 1.0).
                parse(new ByteArrayInputStream(data.getBytes()), "dummy");
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE, oe.getSpecifier());
            Assert.assertEquals(53, oe.getParts()[0]);
            Assert.assertTrue(((String) oe.getParts()[2]).startsWith(" 999           0.00"));
        }
    }

    @Test
    public void testTruncated() throws OrekitException {
        String data =
            "Expression for the X coordinate of the CIP in the GCRS based on the IAU2000A\n"
            + "precession-nutation model\n"
            + "\n"
            + "\n"
            + "----------------------------------------------------------------------\n"
            + "\n"
            + "X = polynomial part + non-polynomial part\n"
            + "\n"
            + "----------------------------------------------------------------------\n"
            + "\n"
            + "Polynomial part (unit microarcsecond)\n"
            + "\n"
            + "  -16616.99 + 2004191742.88 t - 427219.05 t^2 - 198620.54 t^3 - 46.05 t^4 + 5.98 t^5\n"
            + "\n"
            + "----------------------------------------------------------------------\n"
            + "\n"
            + "Non-polynomial part (unit microarcsecond)\n"
            + "(ARG being for various combination of the fundamental arguments of the nutation theory)\n"
            + "\n"
            + "  Sum_i[a_{s,0})_i * sin(ARG) + a_{c,0})_i * cos(ARG)] \n"
            + "\n"
            + "+ Sum_i)j=1,4 [a_{s,j})_i * t^j * sin(ARG) + a_{c,j})_i * cos(ARG)] * t^j]\n"
            + "\n"
            + "The Table below provides the values for a_{s,j})_i and a_{c,j})_i\n"
            + "\n"
            + "The expressions for the fundamental arguments appearing in columns 4 to 8 (luni-solar part) \n"
            + "and in columns 6 to 17 (planetary part) are those of the IERS Conventions 2000\n"
            + "\n"
            + "----------------------------------------------------------------------\n"
            + "\n"
            + "    i    a_{s,j})_i      a_{c,j})_i    l    l'   F    D   Om L_Me L_Ve  L_E L_Ma  L_J L_Sa  L_U L_Ne  p_A\n"
            + "\n"
            + "----------------------------------------------------------------------\n"
            + "-16616.99 + 2004191742.88 t - 427219.05 t^2 - 198620.54 t^3 - 46.05 t^4 + 5.98 t^5\n"
            + "j = 0  Nb of terms = 2\n"
            + "\n"
            + "   1    -6844318.44        1328.67    0    0    0    0    1    0    0    0    0    0    0    0    0    0\n"
            + "   2           0.11           0.00    0    0    4   -4    4    0    0    0    0    0    0    0    0    0\n"
            + "\n"
            + "j = 1  Nb of terms = 2\n"
            + "\n"
            + "   3       -3328.48      205833.15    0    0    0    0    1    0    0    0    0    0    0    0    0    0\n";
        try {
            new PoissonSeriesParser<DerivativeStructure>(17).
                withPolynomialPart('t', PolynomialParser.Unit.NO_UNITS).
                withFirstDelaunay(4).
                withFirstPlanetary(9).
                withSinCos(0, 2, 1.0, 3, 1.0).
                parse(new ByteArrayInputStream(data.getBytes()), "dummy");
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.NOT_A_SUPPORTED_IERS_DATA_FILE, oe.getSpecifier());
        }
    }

    @Test
    public void testTrue1996Files() throws OrekitException {
        String directory = "/assets/org/orekit/IERS-conventions/";
        PoissonSeriesParser<DerivativeStructure> parser =
                new PoissonSeriesParser<DerivativeStructure>(10).
                    withFirstDelaunay(1).
                    withSinCos(0, 7, 1.0, -1, 1.0).
                    withSinCos(1, 8, 1.0, -1, 1.0);
        InputStream psiStream =
            getClass().getResourceAsStream(directory + "1996/tab5.1.txt");
        Assert.assertEquals(106,
                            parser.parse(psiStream, "1996/tab5.1.txt").getNonPolynomialSize());
        parser = parser.withSinCos(0, -1, 1.0, 9, 1.0).withSinCos(1, -1, 1.0, 10, 1.0);
        InputStream epsilonStream =
            getClass().getResourceAsStream(directory + "1996/tab5.1.txt");
        Assert.assertNotNull(parser.parse(epsilonStream, "1996/tab5.1.txt"));
    }

    @Test
    public void testTrue2003Files() throws OrekitException {
        String directory = "/assets/org/orekit/IERS-conventions/";
        PoissonSeriesParser<DerivativeStructure> parser =
                new PoissonSeriesParser<DerivativeStructure>(17).withPolynomialPart('t', PolynomialParser.Unit.NO_UNITS).
                    withFirstDelaunay(4).withFirstPlanetary(9).withSinCos(0, 2, 1.0, 3, 1.0);
        InputStream xStream =
            getClass().getResourceAsStream(directory + "2003/tab5.2a.txt");
        Assert.assertNotNull(parser.parse(xStream, "2003/tab5.2a.txt"));
        InputStream yStream =
            getClass().getResourceAsStream(directory + "2003/tab5.2b.txt");
        Assert.assertNotNull(parser.parse(yStream, "2003/tab5.2b.txt"));
        InputStream zStream =
            getClass().getResourceAsStream(directory + "2003/tab5.2c.txt");
        Assert.assertNotNull(parser.parse(zStream, "2003/tab5.2c.txt"));
    }

    @Test
    public void testTrue2010Files() throws OrekitException {
        String directory = "/assets/org/orekit/IERS-conventions/";
        PoissonSeriesParser<DerivativeStructure> parser =
                new PoissonSeriesParser<DerivativeStructure>(17).withPolynomialPart('t', PolynomialParser.Unit.NO_UNITS).
                    withFirstDelaunay(4).withFirstPlanetary(9).withSinCos(0, 2, 1.0, 3, 1.0);
        InputStream xStream =
            getClass().getResourceAsStream(directory + "2010/tab5.2a.txt");
        Assert.assertNotNull(parser.parse(xStream, "2010/tab5.2a.txt"));
        InputStream yStream =
            getClass().getResourceAsStream(directory + "2010/tab5.2b.txt");
        Assert.assertNotNull(parser.parse(yStream, "2010/tab5.2b.txt"));
        InputStream zStream =
                getClass().getResourceAsStream(directory + "2010/tab5.2d.txt");
        Assert.assertNotNull(parser.parse(zStream, "2010/tab5.2d.txt"));

        PoissonSeriesParser<DerivativeStructure> correctionParser =
                new PoissonSeriesParser<DerivativeStructure>(14).withFirstDelaunay(4).withSinCos(0, 11, 1.0, 12, 1.0);
        InputStream xCorrectionStream =
                getClass().getResourceAsStream(directory + "2010/tab5.1a.txt");
        Assert.assertNotNull(correctionParser.parse(xCorrectionStream, "2010/tab5.1a.txt"));
        correctionParser = correctionParser.withSinCos(0, 13, 1.0, 14, 1.0);
        InputStream yCorrectionStream =
                getClass().getResourceAsStream(directory + "2010/tab5.1a.txt");
        Assert.assertNotNull(correctionParser.parse(yCorrectionStream, "2010/tab5.1a.txt"));


    }

    @Test
    public void testCorruptedLDelaunayMultiplier() {
        checkCorrupted("/tides/tab6.5a-corrupted-l-Delaunay-multiplier.txt", "σ₁");
    }

    @Test
    public void testCorruptedLPrimeDelaunayMultiplier() {
        checkCorrupted("/tides/tab6.5a-corrupted-lPrime-Delaunay-multiplier.txt", "Q₁");
    }

    @Test
    public void testCorruptedFDelaunayMultiplier() {
        checkCorrupted("/tides/tab6.5a-corrupted-F-Delaunay-multiplier.txt", "Nτ₁");
    }

    @Test
    public void testCorruptedDDelaunayMultiplier() {
        checkCorrupted("/tides/tab6.5a-corrupted-D-Delaunay-multiplier.txt", "2Q₁");
    }

    @Test
    public void testCorruptedOmegaDelaunayMultiplier() {
        checkCorrupted("/tides/tab6.5a-corrupted-Omega-Delaunay-multiplier.txt", "τ₁");
    }

    @Test
    public void testCorruptedDoodsonMultiplier() {
        checkCorrupted("/tides/tab6.5a-corrupted-Doodson-multiplier.txt", "Lk₁");
    }

    @Test
    public void testCorruptedDoodsonNumber() {
        checkCorrupted("/tides/tab6.5a-corrupted-Doodson-number.txt", "No₁");
    }

    private void checkCorrupted(String resourceName, String lineStart) {
        try {
            PoissonSeriesParser<DerivativeStructure> parser =
                    new PoissonSeriesParser<DerivativeStructure>(18).
                    withOptionalColumn(1).
                    withDoodson(4, 3).
                    withFirstDelaunay(10).
                    withSinCos(0, 18, 1.0e-12, 17, 1.0e-12);
            parser.parse(getClass().getResourceAsStream(resourceName), resourceName);
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            if (lineStart == null) {
                Assert.assertEquals(OrekitMessages.NOT_A_SUPPORTED_IERS_DATA_FILE, oe.getSpecifier());
            } else {
                Assert.assertEquals(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE, oe.getSpecifier());
                Assert.assertTrue(((String) oe.getParts()[2]).trim().startsWith(lineStart));
            }
        } catch (Exception e) {
            Assert.fail("wrong exception caught: " + e);
        }
    }

    @Test
    public void testGammaTauForbidden() throws OrekitException {
        try {
            new PoissonSeriesParser<DerivativeStructure>(18).withGamma(4).withDoodson(4, 3);
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.CANNOT_PARSE_BOTH_TAU_AND_GAMMA, oe.getSpecifier());
        }
    }

    @Test
    public void testTauGammaForbidden() throws OrekitException {
        try {
            new PoissonSeriesParser<DerivativeStructure>(18).withDoodson(4, 3).withGamma(4);
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.CANNOT_PARSE_BOTH_TAU_AND_GAMMA, oe.getSpecifier());
        }
    }

    @Test
    public void testCompile() throws OrekitException, SecurityException, NoSuchMethodException, IllegalArgumentException, IllegalAccessException, InvocationTargetException {
        String directory = "/assets/org/orekit/IERS-conventions/";
        PoissonSeriesParser<DerivativeStructure> parser =
                new PoissonSeriesParser<DerivativeStructure>(17).withPolynomialPart('t', PolynomialParser.Unit.NO_UNITS).
                    withFirstDelaunay(4).withFirstPlanetary(9).withSinCos(0, 2, 1.0, 3, 1.0);
        InputStream xStream =
            getClass().getResourceAsStream(directory + "2010/tab5.2a.txt");
        PoissonSeries<DerivativeStructure> xSeries = parser.parse(xStream, "2010/tab5.2a.txt");
        InputStream yStream =
            getClass().getResourceAsStream(directory + "2010/tab5.2b.txt");
        PoissonSeries<DerivativeStructure> ySeries = parser.parse(yStream, "2010/tab5.2b.txt");
        InputStream zStream =
            getClass().getResourceAsStream(directory + "2010/tab5.2d.txt");
        PoissonSeries<DerivativeStructure> sSeries = parser.parse(zStream, "2010/tab5.2d.txt");
        PoissonSeries.CompiledSeries<DerivativeStructure> xysSeries =
                PoissonSeries.compile(xSeries, ySeries, sSeries);

        Method m = IERSConventions.class.getDeclaredMethod("getNutationArguments", TimeScale.class);
        m.setAccessible(true);
        FundamentalNutationArguments arguments =
                (FundamentalNutationArguments) m.invoke(IERSConventions.IERS_2010, (TimeScale) null);

        for (double dt = 0; dt < Constants.JULIAN_YEAR; dt += Constants.JULIAN_DAY) {
            AbsoluteDate date = AbsoluteDate.J2000_EPOCH.shiftedBy(dt);
            BodiesElements elements = arguments.evaluateAll(date);
            double x     = xSeries.value(elements);
            double y     = ySeries.value(elements);
            double s     = sSeries.value(elements);
            double[] xys = xysSeries.value(elements);
            Assert.assertEquals(x, xys[0], 1.0e-15 * FastMath.abs(x));
            Assert.assertEquals(y, xys[1], 1.0e-15 * FastMath.abs(y));
            Assert.assertEquals(s, xys[2], 1.0e-15 * FastMath.abs(s));
        }

    }

    @Test
    public void testDerivatives() throws OrekitException {

        Utils.setDataRoot("regular-data");
        String directory = "/assets/org/orekit/IERS-conventions/";
        PoissonSeriesParser<DerivativeStructure> parser =
                new PoissonSeriesParser<DerivativeStructure>(17).withPolynomialPart('t', PolynomialParser.Unit.NO_UNITS).
                    withFirstDelaunay(4).withFirstPlanetary(9).withSinCos(0, 2, 1.0, 3, 1.0);
        PoissonSeries<DerivativeStructure> xSeries =
                        parser.parse(getClass().getResourceAsStream(directory + "2010/tab5.2a.txt"), "2010/tab5.2a.txt");
        PoissonSeries<DerivativeStructure> ySeries =
                        parser.parse(getClass().getResourceAsStream(directory + "2010/tab5.2b.txt"), "2010/tab5.2b.txt");
        PoissonSeries<DerivativeStructure> zSeries =
                        parser.parse(getClass().getResourceAsStream(directory + "2010/tab5.2d.txt"), "2010/tab5.2d.txt");

        TimeScale ut1 = TimeScalesFactory.getUT1(FramesFactory.getEOPHistory(IERSConventions.IERS_2010, true));
        FundamentalNutationArguments arguments = IERSConventions.IERS_2010.getNutationArguments(ut1);

        Coordinate xCoordinate              = new Coordinate(xSeries, arguments);
        Coordinate yCoordinate              = new Coordinate(ySeries, arguments);
        Coordinate zCoordinate              = new Coordinate(zSeries, arguments);
        UnivariateDifferentiableFunction dx = new FiniteDifferencesDifferentiator(4, 0.4).differentiate(xCoordinate);
        UnivariateDifferentiableFunction dy = new FiniteDifferencesDifferentiator(4, 0.4).differentiate(yCoordinate);
        UnivariateDifferentiableFunction dz = new FiniteDifferencesDifferentiator(4, 0.4).differentiate(zCoordinate);

        for (double t = 0; t < Constants.JULIAN_DAY; t += 120) {

            final AbsoluteDate date = AbsoluteDate.J2000_EPOCH.shiftedBy(t);

            // direct computation of derivatives
            FieldBodiesElements<DerivativeStructure> elements = arguments.evaluateDerivative(date);
            Assert.assertEquals(0.0, elements.getDate().durationFrom(date), 1.0e-15);
            DerivativeStructure xDirect = xSeries.value(elements);
            DerivativeStructure yDirect = ySeries.value(elements);
            DerivativeStructure zDirect = zSeries.value(elements);

            // finite differences computation of derivatives
            DerivativeStructure zero = new DerivativeStructure(1, 1, 0, 0.0);
            xCoordinate.setDate(date);
            DerivativeStructure xFinite = dx.value(zero);
            yCoordinate.setDate(date);
            DerivativeStructure yFinite = dy.value(zero);
            zCoordinate.setDate(date);
            DerivativeStructure zFinite = dz.value(zero);

            Assert.assertEquals(xFinite.getValue(),              xDirect.getValue(),              FastMath.abs(7.0e-15 * xFinite.getValue()));
            Assert.assertEquals(xFinite.getPartialDerivative(1), xDirect.getPartialDerivative(1), FastMath.abs(2.0e-07 * xFinite.getPartialDerivative(1)));
            Assert.assertEquals(yFinite.getValue(),              yDirect.getValue(),              FastMath.abs(7.0e-15 * yFinite.getValue()));
            Assert.assertEquals(yFinite.getPartialDerivative(1), yDirect.getPartialDerivative(1), FastMath.abs(2.0e-07 * yFinite.getPartialDerivative(1)));
            Assert.assertEquals(zFinite.getValue(),              zDirect.getValue(),              FastMath.abs(7.0e-15 * zFinite.getValue()));
            Assert.assertEquals(zFinite.getPartialDerivative(1), zDirect.getPartialDerivative(1), FastMath.abs(2.0e-07 * zFinite.getPartialDerivative(1)));

        }

    }

    private static class Coordinate implements UnivariateFunction {
        private final PoissonSeries<DerivativeStructure> series;
        private final FundamentalNutationArguments arguments;
        private AbsoluteDate date;
        Coordinate(PoissonSeries<DerivativeStructure> series,
                   FundamentalNutationArguments arguments) {
            this.series    = series;
            this.arguments = arguments;
            this.date      = AbsoluteDate.J2000_EPOCH;
        }
        void setDate(AbsoluteDate date) {
            this.date = date;
        }
        public double value(double x) {
            return series.value(arguments.evaluateAll(date.shiftedBy(x)));
        }
    }

}
