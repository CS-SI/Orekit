/* Copyright 2002-2017 CS Systèmes d'Information
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.hipparchus.RealFieldElement;
import org.hipparchus.analysis.UnivariateVectorFunction;
import org.hipparchus.analysis.differentiation.DSFactory;
import org.hipparchus.analysis.differentiation.DerivativeStructure;
import org.hipparchus.analysis.differentiation.FiniteDifferencesDifferentiator;
import org.hipparchus.analysis.differentiation.UnivariateDifferentiableVectorFunction;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.Decimal64;
import org.hipparchus.util.Decimal64Field;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathArrays;
import org.hipparchus.util.MathUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitExceptionWrapper;
import org.orekit.errors.OrekitIllegalStateException;
import org.orekit.errors.OrekitMessages;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.ChronologicalComparator;
import org.orekit.time.DateComponents;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.AngularCoordinates;
import org.orekit.utils.AngularDerivativesFilter;
import org.orekit.utils.CartesianDerivativesFilter;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.IERSConventions.NutationCorrectionConverter;
import org.orekit.utils.PVCoordinates;

public class FramesFactoryTest {

    @Test
    public void testTreeRoot() throws OrekitException {
        Assert.assertNull(FramesFactory.getFrame(Predefined.GCRF).getParent());
    }

    @Test
    public void testWrongSupportedFileNames1980() throws OrekitException {
        FramesFactory.addDefaultEOP1980HistoryLoaders("wrong-rapidDataColumns-1980",
                                                      "wrong-rapidDataXML-1980",
                                                      "wrong-eopC04-1980",
                                                      "wrong-bulletinB-1980",
                                                      "wrong-bulletinA-1980");
        try {
            FramesFactory.getEOPHistory(IERSConventions.IERS_1996, true).getStartDate();
            Assert.fail("an exception should have been thrown");
        } catch (OrekitIllegalStateException oe) {
            Assert.assertEquals(OrekitMessages.NO_CACHED_ENTRIES, oe.getSpecifier());
        }
    }

    @Test
    public void testWrongSupportedFileNames2000() throws OrekitException {
        FramesFactory.addDefaultEOP2000HistoryLoaders("wrong-rapidDataColumns-2000",
                                                      "wrong-rapidDataXML-2000",
                                                      "wrong-eopC04-2000",
                                                      "wrong-bulletinB-2000",
                                                      "wrong-bulletinA-2000");
        try {
            FramesFactory.getEOPHistory(IERSConventions.IERS_2010, true).getStartDate();
            Assert.fail("an exception should have been thrown");
        } catch (OrekitIllegalStateException oe) {
            Assert.assertEquals(OrekitMessages.NO_CACHED_ENTRIES, oe.getSpecifier());
        }
    }

    @Test
    public void testWrongConventions() throws OrekitException {
        // set up only 1980 conventions
        FramesFactory.addDefaultEOP1980HistoryLoaders(null, null, null, null, null);
        try {
            // attempt to retrieve 2000 conventions
            FramesFactory.getEOPHistory(IERSConventions.IERS_2010, true).getStartDate();
            Assert.fail("an exception should have been thrown");
        } catch (OrekitIllegalStateException oe) {
            Assert.assertEquals(OrekitMessages.NO_CACHED_ENTRIES, oe.getSpecifier());
        }
    }

    @Test
    public void testEOPLoaderException() {
        final boolean[] flags = new boolean[2];
        try {
            FramesFactory.addEOPHistoryLoader(IERSConventions.IERS_2010, new EOPHistoryLoader() {
                @Override
                public void fillHistory(NutationCorrectionConverter converter, SortedSet<EOPEntry> history) {
                    // don't really fill history here
                    flags[0] = true;
                }
            });
            FramesFactory.addEOPHistoryLoader(IERSConventions.IERS_2010, new EOPHistoryLoader() {
                @Override
                public void fillHistory(NutationCorrectionConverter converter, SortedSet<EOPEntry> history)
                    throws OrekitException {
                    // generate exception
                    flags[1] = true;
                    throw new OrekitException(OrekitMessages.NO_DATA_GENERATED, AbsoluteDate.J2000_EPOCH);
                }
            });
            FramesFactory.getEOPHistory(IERSConventions.IERS_2010, true);
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertTrue(flags[0]);
            Assert.assertTrue(flags[1]);
            Assert.assertEquals(OrekitMessages.NO_DATA_GENERATED, oe.getSpecifier());
        }
    }

    @Test
    public void testUnwrapInterpolatingTransformProvider() throws OrekitException {
        TransformProvider raw = new TransformProvider() {
            private static final long serialVersionUID = 1L;
            public Transform getTransform(final AbsoluteDate date) {
                double dt = date.durationFrom(AbsoluteDate.J2000_EPOCH);
                double sin = FastMath.sin(dt * MathUtils.TWO_PI / Constants.JULIAN_DAY);
                return new Transform(date,
                                     new PVCoordinates(new Vector3D(sin, Vector3D.PLUS_I),
                                                       Vector3D.ZERO));
            }
            public <T extends RealFieldElement<T>> FieldTransform<T> getTransform(final FieldAbsoluteDate<T> date) {
                throw new UnsupportedOperationException("never called in this test");
            }
        };
        Frame parent = FramesFactory.getGCRF();
        Frame frame  = new Frame(parent,
                                 new InterpolatingTransformProvider(raw,
                                                                    CartesianDerivativesFilter.USE_P,
                                                                    AngularDerivativesFilter.USE_R,
                                                                    AbsoluteDate.PAST_INFINITY,
                                                                    AbsoluteDate.FUTURE_INFINITY,
                                                                    4, Constants.JULIAN_DAY, 10,
                                                                    Constants.JULIAN_YEAR, 2 * Constants.JULIAN_DAY),
                                 "sine");
        double maxErrorNonInterpolating = 0;
        double maxErrorInterpolating    = 0;
        for (double dt = 0; dt < Constants.JULIAN_DAY; dt += 60.0) {
            AbsoluteDate date            = AbsoluteDate.J2000_EPOCH.shiftedBy(dt);
            Transform reference          = raw.getTransform(date);
            Transform nonInterpolating   = FramesFactory.getNonInterpolatingTransform(parent, frame, date);
            Transform interpolating      = parent.getTransformTo(frame, date);
            double errorNonInterpolating = Vector3D.distance(reference.getTranslation(),
                                                             nonInterpolating.getTranslation());
            maxErrorNonInterpolating     = FastMath.max(maxErrorNonInterpolating, errorNonInterpolating);
            double errorInterpolating    = Vector3D.distance(reference.getTranslation(),
                                                             interpolating.getTranslation());
            maxErrorInterpolating        = FastMath.max(maxErrorInterpolating, errorInterpolating);
        }
        Assert.assertEquals(0.0, maxErrorNonInterpolating, 1.0e-15);
        Assert.assertEquals(1.0, maxErrorInterpolating,    1.0e-15);
    }

    @Test
    public void testUnwrapShiftingTransformProvider() throws OrekitException {
        TransformProvider raw = new TransformProvider() {
            private static final long serialVersionUID = 1L;
            public Transform getTransform(final AbsoluteDate date) {
                double dt = date.durationFrom(AbsoluteDate.J2000_EPOCH);
                double sin = FastMath.sin(dt * MathUtils.TWO_PI / Constants.JULIAN_DAY);
                return new Transform(date,
                                     new PVCoordinates(new Vector3D(sin, Vector3D.PLUS_I),
                                                       Vector3D.ZERO));
            }
            public <T extends RealFieldElement<T>> FieldTransform<T> getTransform(final FieldAbsoluteDate<T> date) {
                throw new UnsupportedOperationException("never called in this test");
            }
        };
        Frame parent = FramesFactory.getGCRF();
        Frame frame  = new Frame(parent,
                                 new ShiftingTransformProvider(raw,
                                                               CartesianDerivativesFilter.USE_P,
                                                               AngularDerivativesFilter.USE_R,
                                                               AbsoluteDate.PAST_INFINITY,
                                                               AbsoluteDate.FUTURE_INFINITY,
                                                               4, Constants.JULIAN_DAY, 10,
                                                               Constants.JULIAN_YEAR, 2 * Constants.JULIAN_DAY),
                                 "sine");
        double maxErrorNonShifting = 0;
        double maxErrorShifting    = 0;
        for (double dt = 0; dt < Constants.JULIAN_DAY; dt += 60.0) {
            AbsoluteDate date       = AbsoluteDate.J2000_EPOCH.shiftedBy(dt);
            Transform reference     = raw.getTransform(date);
            Transform nonShifting   = FramesFactory.getNonInterpolatingTransform(parent, frame, date);
            Transform shifting      = parent.getTransformTo(frame, date);
            double errorNonShifting = Vector3D.distance(reference.getTranslation(),
                                                        nonShifting.getTranslation());
            maxErrorNonShifting     = FastMath.max(maxErrorNonShifting, errorNonShifting);
            double errorShifting    = Vector3D.distance(reference.getTranslation(),
                                                        shifting.getTranslation());
            maxErrorShifting        = FastMath.max(maxErrorShifting, errorShifting);
        }
        Assert.assertEquals(0.0, maxErrorNonShifting, 1.0e-15);
        Assert.assertEquals(1.0, maxErrorShifting,    1.0e-15);
    }

    @Test
    public void testTreeICRF() throws OrekitException {
        Frame icrf = FramesFactory.getFrame(Predefined.ICRF);
        Transform t = icrf.getTransformTo(FramesFactory.getGCRF(),
                                          new AbsoluteDate(1969, 6, 25, TimeScalesFactory.getTT()));
        Assert.assertEquals(0.0, t.getRotation().getAngle(), 1.0e-15);
        Assert.assertEquals(CelestialBodyFactory.EARTH_MOON + "/inertial", icrf.getParent().getName());
        Assert.assertEquals(Predefined.GCRF.getName(), icrf.getParent().getParent().getName());
    }

    @Test
    public void testTree() throws OrekitException {
        Predefined[][] reference = new Predefined[][] {
            { Predefined.EME2000,                                Predefined.GCRF },
            { Predefined.ITRF_CIO_CONV_1996_ACCURATE_EOP,        Predefined.TIRF_CONVENTIONS_1996_ACCURATE_EOP },
            { Predefined.ITRF_CIO_CONV_1996_SIMPLE_EOP,          Predefined.TIRF_CONVENTIONS_1996_SIMPLE_EOP   },
            { Predefined.ITRF_CIO_CONV_2003_ACCURATE_EOP,        Predefined.TIRF_CONVENTIONS_2003_ACCURATE_EOP },
            { Predefined.ITRF_CIO_CONV_2003_SIMPLE_EOP,          Predefined.TIRF_CONVENTIONS_2003_SIMPLE_EOP   },
            { Predefined.ITRF_CIO_CONV_2010_ACCURATE_EOP,        Predefined.TIRF_CONVENTIONS_2010_ACCURATE_EOP },
            { Predefined.ITRF_CIO_CONV_2010_SIMPLE_EOP,          Predefined.TIRF_CONVENTIONS_2010_SIMPLE_EOP   },
            { Predefined.TIRF_CONVENTIONS_1996_ACCURATE_EOP,     Predefined.CIRF_CONVENTIONS_1996_ACCURATE_EOP },
            { Predefined.TIRF_CONVENTIONS_1996_SIMPLE_EOP,       Predefined.CIRF_CONVENTIONS_1996_SIMPLE_EOP   },
            { Predefined.TIRF_CONVENTIONS_2003_ACCURATE_EOP,     Predefined.CIRF_CONVENTIONS_2003_ACCURATE_EOP },
            { Predefined.TIRF_CONVENTIONS_2003_SIMPLE_EOP,       Predefined.CIRF_CONVENTIONS_2003_SIMPLE_EOP   },
            { Predefined.TIRF_CONVENTIONS_2010_ACCURATE_EOP,     Predefined.CIRF_CONVENTIONS_2010_ACCURATE_EOP },
            { Predefined.TIRF_CONVENTIONS_2010_SIMPLE_EOP,       Predefined.CIRF_CONVENTIONS_2010_SIMPLE_EOP   },
            { Predefined.CIRF_CONVENTIONS_1996_ACCURATE_EOP,     Predefined.GCRF },
            { Predefined.CIRF_CONVENTIONS_1996_SIMPLE_EOP,       Predefined.GCRF },
            { Predefined.CIRF_CONVENTIONS_2003_ACCURATE_EOP,     Predefined.GCRF },
            { Predefined.CIRF_CONVENTIONS_2003_SIMPLE_EOP,       Predefined.GCRF },
            { Predefined.CIRF_CONVENTIONS_2010_ACCURATE_EOP,     Predefined.GCRF },
            { Predefined.CIRF_CONVENTIONS_2010_SIMPLE_EOP,       Predefined.GCRF },
            { Predefined.VEIS_1950,                              Predefined.GTOD_WITHOUT_EOP_CORRECTIONS },
            { Predefined.ITRF_EQUINOX_CONV_1996_ACCURATE_EOP,    Predefined.GTOD_CONVENTIONS_1996_ACCURATE_EOP },
            { Predefined.ITRF_EQUINOX_CONV_1996_SIMPLE_EOP,      Predefined.GTOD_CONVENTIONS_1996_SIMPLE_EOP   },
            { Predefined.ITRF_EQUINOX_CONV_2003_ACCURATE_EOP,    Predefined.GTOD_CONVENTIONS_2003_ACCURATE_EOP },
            { Predefined.ITRF_EQUINOX_CONV_2003_SIMPLE_EOP,      Predefined.GTOD_CONVENTIONS_2003_SIMPLE_EOP   },
            { Predefined.ITRF_EQUINOX_CONV_2010_ACCURATE_EOP,    Predefined.GTOD_CONVENTIONS_2010_ACCURATE_EOP },
            { Predefined.ITRF_EQUINOX_CONV_2010_SIMPLE_EOP,      Predefined.GTOD_CONVENTIONS_2010_SIMPLE_EOP   },
            { Predefined.GTOD_WITHOUT_EOP_CORRECTIONS,           Predefined.TOD_WITHOUT_EOP_CORRECTIONS },
            { Predefined.GTOD_CONVENTIONS_1996_ACCURATE_EOP,     Predefined.TOD_CONVENTIONS_1996_ACCURATE_EOP },
            { Predefined.GTOD_CONVENTIONS_1996_SIMPLE_EOP,       Predefined.TOD_CONVENTIONS_1996_SIMPLE_EOP   },
            { Predefined.GTOD_CONVENTIONS_2003_ACCURATE_EOP,     Predefined.TOD_CONVENTIONS_2003_ACCURATE_EOP },
            { Predefined.GTOD_CONVENTIONS_2003_SIMPLE_EOP,       Predefined.TOD_CONVENTIONS_2003_SIMPLE_EOP   },
            { Predefined.GTOD_CONVENTIONS_2010_ACCURATE_EOP,     Predefined.TOD_CONVENTIONS_2010_ACCURATE_EOP },
            { Predefined.GTOD_CONVENTIONS_2010_SIMPLE_EOP,       Predefined.TOD_CONVENTIONS_2010_SIMPLE_EOP   },
            { Predefined.TOD_WITHOUT_EOP_CORRECTIONS,            Predefined.MOD_WITHOUT_EOP_CORRECTIONS },
            { Predefined.TOD_CONVENTIONS_1996_ACCURATE_EOP,      Predefined.MOD_CONVENTIONS_1996 },
            { Predefined.TOD_CONVENTIONS_1996_SIMPLE_EOP,        Predefined.MOD_CONVENTIONS_1996 },
            { Predefined.TOD_CONVENTIONS_2003_ACCURATE_EOP,      Predefined.MOD_CONVENTIONS_2003 },
            { Predefined.TOD_CONVENTIONS_2003_SIMPLE_EOP,        Predefined.MOD_CONVENTIONS_2003 },
            { Predefined.TOD_CONVENTIONS_2010_ACCURATE_EOP,      Predefined.MOD_CONVENTIONS_2010 },
            { Predefined.TOD_CONVENTIONS_2010_SIMPLE_EOP,        Predefined.MOD_CONVENTIONS_2010 },
            { Predefined.MOD_WITHOUT_EOP_CORRECTIONS,            Predefined.EME2000 },
            { Predefined.MOD_CONVENTIONS_1996,                   Predefined.GCRF    },
            { Predefined.MOD_CONVENTIONS_2003,                   Predefined.EME2000 },
            { Predefined.MOD_CONVENTIONS_2010,                   Predefined.EME2000 },
            { Predefined.TEME,                                   Predefined.TOD_WITHOUT_EOP_CORRECTIONS }
        };
        for (final Predefined[] pair : reference) {
            Frame child  = FramesFactory.getFrame(pair[0]);
            Frame parent = FramesFactory.getFrame(pair[1]);
            Assert.assertEquals("wrong parent for " + child.getName(),
                                parent.getName(), child.getParent().getName());
        }
    }

    @Test
    public void testSerialization()
            throws OrekitException, IOException, ClassNotFoundException {
        for (Predefined predefined : Predefined.values()) {

            Frame original = FramesFactory.getFrame(predefined);
            if (predefined == Predefined.ICRF) {
                Assert.assertEquals(CelestialBodyFactory.SOLAR_SYSTEM_BARYCENTER + "/inertial", original.getName());
            } else {
                Assert.assertEquals(predefined.getName(), original.getName());
            }

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream    oos = new ObjectOutputStream(bos);
            oos.writeObject(original);
             if (predefined == Predefined.GCRF) {
                Assert.assertTrue(bos.size() >  50);
                Assert.assertTrue(bos.size() < 100);
            } else if (predefined == Predefined.ICRF) {
                Assert.assertTrue(bos.size() > 430);
                Assert.assertTrue(bos.size() < 480);
            } else {
                Assert.assertTrue(bos.size() > 100);
                Assert.assertTrue(bos.size() < 160);
            }

            ByteArrayInputStream  bis = new ByteArrayInputStream(bos.toByteArray());
            ObjectInputStream     ois = new ObjectInputStream(bis);
            Frame deserialized  = (Frame) ois.readObject();
            Assert.assertTrue(original == deserialized);
        }
    }

    @Test
    public void testEOPConversionSymetry1980() throws OrekitException {
        Utils.setDataRoot("rapid-data-columns");
        IERSConventions.NutationCorrectionConverter converter =
                IERSConventions.IERS_1996.getNutationCorrectionConverter();
        SortedSet<EOPEntry> rawEquinox = new TreeSet<EOPEntry>(new ChronologicalComparator());
        new RapidDataAndPredictionColumnsLoader(false, "^finals\\.daily$").fillHistory(converter, rawEquinox);
        Assert.assertEquals(181, rawEquinox.size());
        for (final EOPEntry entry : rawEquinox) {
            final double[] rebuiltEquinox = converter.toEquinox(entry.getDate(),
                                                                entry.getDx(), entry.getDy());
            Assert.assertEquals(entry.getDdPsi(), rebuiltEquinox[0], 2.0e-22);
            Assert.assertEquals(entry.getDdEps(), rebuiltEquinox[1], 2.0e-23);
        }
    }

    @Test
    public void testEOPConversionSymetry2003() throws OrekitException {
        Utils.setDataRoot("rapid-data-columns");
        IERSConventions.NutationCorrectionConverter converter =
                IERSConventions.IERS_2003.getNutationCorrectionConverter();
        final SortedSet<EOPEntry> rawNRO = new TreeSet<EOPEntry>(new ChronologicalComparator());
        new RapidDataAndPredictionColumnsLoader(true, "^finals2000A\\.daily$").fillHistory(converter, rawNRO);
        Assert.assertEquals(181, rawNRO.size());
        for (final EOPEntry entry : rawNRO) {
            final double[] rebuiltNRO = converter.toNonRotating(entry.getDate(),
                                                                entry.getDdPsi(), entry.getDdEps());
            Assert.assertEquals(entry.getDx(), rebuiltNRO[0], 6.0e-23);
            Assert.assertEquals(entry.getDy(), rebuiltNRO[1], 2.0e-23);
        }
    }

    @Test
    public void testCIP2003() throws OrekitException {
        testCIP(IERSConventions.IERS_2003, 1.2e-11);
    }

    @Test
    public void testCIP2010() throws OrekitException {
        testCIP(IERSConventions.IERS_2010, 1.2e-11);
    }

    private void testCIP(IERSConventions conventions, double threshold) throws OrekitException {
        Utils.setLoaders(conventions, new ArrayList<EOPEntry>());
        Frame cirf = FramesFactory.getCIRF(conventions, false);
        Frame tod  = FramesFactory.getTOD(conventions, false);
        AbsoluteDate t0 = new AbsoluteDate(new DateComponents(2003, 06, 21), TimeComponents.H00,
                                           TimeScalesFactory.getUTC());
        for (double dt = -10 * Constants.JULIAN_DAY; dt < 10 * Constants.JULIAN_DAY; dt += 7200) {
            // CIRF and TOD should both have the Celestial Intermediate Pole as their Z axis
            AbsoluteDate date = t0.shiftedBy(dt);
            Transform t = FramesFactory.getNonInterpolatingTransform(tod, cirf, date);
            Vector3D z = t.transformVector(Vector3D.PLUS_K);
            Assert.assertEquals(0.0, Vector3D.angle(z, Vector3D.PLUS_K), threshold);
        }
    }

    @Test
    public void testEOPConversion() throws OrekitException {

        // real data from buletinb-298.txt

        // first use case: don't propagate the dx, dy correction to TOD, set dPsi, dEpsilon to 0.0
        final List<EOPEntry> forced = Utils.buildEOPList(IERSConventions.IERS_2010, new double[][] {
            { 56202, 0.3726886, 0.0008843, 0.168556, 0.332869, 0.0, 0.0, -0.000118, 0.000091 },
            { 56203, 0.3719108, 0.0007204, 0.168261, 0.331527, 0.0, 0.0, -0.000140, 0.000111 },
            { 56204, 0.3712561, 0.0006217, 0.168218, 0.330668, 0.0, 0.0, -0.000165, 0.000148 },
            { 56205, 0.3706736, 0.0005530, 0.167775, 0.329688, 0.0, 0.0, -0.000188, 0.000189 },
            { 56206, 0.3701593, 0.0005139, 0.166829, 0.328457, 0.0, 0.0, -0.000180, 0.000203 }
        });

        Utils.setLoaders(IERSConventions.IERS_2010, forced);
        Frame cirf            = FramesFactory.getCIRF(IERSConventions.IERS_2010, false);
        Frame todNoCorrection = FramesFactory.getTOD(IERSConventions.IERS_2010, false);

        // second use case: convert dx, dy data into dDPsi, dDEpsilon
        final List<EOPEntry> converted = Utils.buildEOPList(IERSConventions.IERS_2010, new double[][] {
            { 56202, 0.3726886, 0.0008843, 0.168556, 0.332869, Double.NaN, Double.NaN, -0.000118, 0.000091 },
            { 56203, 0.3719108, 0.0007204, 0.168261, 0.331527, Double.NaN, Double.NaN, -0.000140, 0.000111 },
            { 56204, 0.3712561, 0.0006217, 0.168218, 0.330668, Double.NaN, Double.NaN, -0.000165, 0.000148 },
            { 56205, 0.3706736, 0.0005530, 0.167775, 0.329688, Double.NaN, Double.NaN, -0.000188, 0.000189 },
            { 56206, 0.3701593, 0.0005139, 0.166829, 0.328457, Double.NaN, Double.NaN, -0.000180, 0.000203 }
        });
        Utils.setLoaders(IERSConventions.IERS_2010, converted);
        Frame todConvertedCorrection  = FramesFactory.getTOD(IERSConventions.IERS_2010, false);

        for (AbsoluteDate date = forced.get(0).getDate();
             date.compareTo(forced.get(forced.size() - 1).getDate()) < 0;
             date = date.shiftedBy(3600)) {
            Transform tNoCorrection =
                    FramesFactory.getNonInterpolatingTransform(todNoCorrection, cirf, date);

            // when we forget the correction on TOD,
            // its Z axis is slightly offset from CIRF Z axis
            Vector3D zNoCorrection  = tNoCorrection.transformVector(Vector3D.PLUS_K);
            Assert.assertTrue(Vector3D.angle(zNoCorrection, Vector3D.PLUS_K) > 7.2e-10);
            Transform tConverted =
                    FramesFactory.getNonInterpolatingTransform(todConvertedCorrection, cirf, date);
            // when we convert the correction and apply it to TOD,
            // its Z axis is much better aligned with CIRF Z axis
            Vector3D zConverted  = tConverted.transformVector(Vector3D.PLUS_K);
            Assert.assertTrue(Vector3D.angle(zConverted, Vector3D.PLUS_K) < 6e-12);

        }

    }

    @Test
    public void testEOPConversionUAI2000Package() throws OrekitException {
        // the reference value has been computed using the uai2000.package routines
        // provided by Ch. Bizouard on page http://hpiers.obspm.fr/eop-pc/models/models_fr.html
        // using the following main program
        //
        //        program test_EOP_conversion
        //        double precision dmjd,dpsi,deps,dx1,dy1,dx2,dy2
        //C       2004-02-14:00:00:00Z, MJD = 53049,
        //C                             UT1-UTC=-0.4093475, LOD = 0.4676,
        //C                             X = -0.076804, Y = 0.204671,
        //C                             dx  = -0.075, dy  = -0.189
        //C       values extracted from finals2000A.all file, bulletinA columns
        //        dmjd = 53049.0
        //        dx1  = -0.075
        //        dy1  = -0.189
        //        call DPSIDEPS2000_DXDY2000(dmjd,dx1,dy1,dpsi,deps)
        //        write (6, *) 'dPsi = ', dpsi, 'dEpsilon = ', deps
        //        call DXDY2000_DPSIDEPS2000(dmjd,dpsi,deps,dX2,dY2)
        //        write (6, *) 'dx = ', dx2, 'dy = ', dy2
        //      end
        //
        // the output of this test reads:
        //     dPsi =  -0.18810999708158463      dEpsilon =  -0.18906891450729962
        //     dx =   -7.5000002980232239E-002 dy =  -0.18899999558925629

        IERSConventions.NutationCorrectionConverter converter =
                IERSConventions.IERS_2003.getNutationCorrectionConverter();
        AbsoluteDate date =
                new AbsoluteDate(new DateComponents(DateComponents.MODIFIED_JULIAN_EPOCH, 53049),
                                 TimeScalesFactory.getUTC());
        double dx  = Constants.ARC_SECONDS_TO_RADIANS * -0.075;
        double dy  = Constants.ARC_SECONDS_TO_RADIANS * -0.189;
        double[] equinox = converter.toEquinox(date, dx, dy);

        // The code from uai2000.package uses sin(epsilon0), cos(epsilon0) in the
        // formula, whereas IERS conventions use sin(epsilonA), cos(epsilon0), i.e.
        // the sine is computed on current date instead of epoch. This explains why
        // the following threshold had to be raised to 2e-11.
        // We still decided to stick with sin(epsilonA) in our implementation, in
        // order to remain consistent with IERS conventions
        Assert.assertEquals(Constants.ARC_SECONDS_TO_RADIANS * -0.18810999708158463,
                            equinox[0], 2.0e-11);

        Assert.assertEquals(Constants.ARC_SECONDS_TO_RADIANS * -0.18906891450729962,
                            equinox[1], 2.2e-14);
        double[] nro = converter.toNonRotating(date, equinox[0], equinox[1]);
        Assert.assertEquals(dx, nro[0], 1.0e-20);
        Assert.assertEquals(dy, nro[1], 1.0e-20);

    }

    @Test
    public void testFieldConsistency() throws OrekitException {
        for (final Predefined predefined : Predefined.values()) {
            final Frame frame = FramesFactory.getFrame(predefined);
            final Frame parent = frame.getParent();
            if (parent != null) {
                double maxPositionError             = 0;
                double maxVelocityError             = 0;
                double maxAccelerationError         = 0;
                double maxRotationError             = 0;
                double maxRotationRateError         = 0;
                double maxRotationAccelerationError = 0;
                for (double dt = 0; dt < Constants.JULIAN_DAY; dt += 60.0) {
                    final AbsoluteDate date = AbsoluteDate.J2000_EPOCH.shiftedBy(dt);
                    final Transform transformDouble = parent.getTransformTo(frame, date);
                    final FieldTransform<Decimal64> transformD64 =
                                    parent.getTransformTo(frame, new FieldAbsoluteDate<>(Decimal64Field.getInstance(), date));
                    maxPositionError             = FastMath.max(maxPositionError,
                                                                Vector3D.distance(transformDouble.getTranslation(),
                                                                                  transformD64.getTranslation().toVector3D()));
                    maxVelocityError             = FastMath.max(maxVelocityError,
                                                                Vector3D.distance(transformDouble.getVelocity(),
                                                                                  transformD64.getVelocity().toVector3D()));
                    maxAccelerationError         = FastMath.max(maxAccelerationError,
                                                                Vector3D.distance(transformDouble.getAcceleration(),
                                                                                  transformD64.getAcceleration().toVector3D()));
                    maxRotationError             = FastMath.max(maxRotationError,
                                                               Rotation.distance(transformDouble.getRotation(),
                                                                                 transformD64.getRotation().toRotation()));
                    maxRotationRateError         = FastMath.max(maxRotationRateError,
                                                                Vector3D.distance(transformDouble.getRotationRate(),
                                                                                  transformD64.getRotationRate().toVector3D()));
                    maxRotationAccelerationError = FastMath.max(maxRotationAccelerationError,
                                                               Vector3D.distance(transformDouble.getRotationAcceleration(),
                                                                                 transformD64.getRotationAcceleration().toVector3D()));
                }
                Assert.assertEquals(0, maxPositionError,             1.0e-100);
                Assert.assertEquals(0, maxVelocityError,             1.0e-100);
                Assert.assertEquals(0, maxAccelerationError,         1.0e-100);
                Assert.assertEquals(0, maxRotationError,             2.0e-14);
                Assert.assertEquals(0, maxRotationRateError,         2.0e-18);
                Assert.assertEquals(0, maxRotationAccelerationError, 8.0e-22);
            }
        }
    }

    @Test
    public void testDerivatives2000WithInterpolation() throws OrekitException {
        doTestDerivatives(AbsoluteDate.J2000_EPOCH, Constants.JULIAN_DAY, 60.0, false,
                          8.0e-5, 2.0e-5, 3.0e-8, 4.0e-11, 7.0e-13, 2.0e-14);
    }

    @Test
    public void testDerivatives2000WithoutInterpolation() throws OrekitException {
        // when we forbid interpolation, the test is really slow (almost two hours
        // runtime on a very old machine for one day time span and one minute rate),
        // we drastically reduce sampling to circumvent this drawback
        doTestDerivatives(AbsoluteDate.J2000_EPOCH, Constants.JULIAN_DAY / 4, 3600, true,
                          8.0e-5, 2.0e-5, 3.0e-8, 4.0e-11, 7.0e-13, 2.0e-14);
    }

    @Test
    public void testDerivatives2003WithInterpolation() throws OrekitException {
        doTestDerivatives(AbsoluteDate.J2000_EPOCH.shiftedBy(3 * Constants.JULIAN_YEAR),
                          Constants.JULIAN_DAY, 60.0, false,
                          8.0e-5, 2.0e-5, 4.0e-8, 8.0e-12, 3.0e-13, 4.0e-15);
    }

    @Test
    public void testDerivatives2003WithoutInterpolation() throws OrekitException {
        // when we forbid interpolation, the test is really slow (almost two hours
        // runtime on a very old machine for one day time span and one minute rate),
        // we drastically reduce sampling to circumvent this drawback
        doTestDerivatives(AbsoluteDate.J2000_EPOCH.shiftedBy(3 * Constants.JULIAN_YEAR),
                          Constants.JULIAN_DAY / 4, 3600, true,
                          8.0e-5, 2.0e-5, 4.0e-8, 8.0e-12, 3.0e-13, 4.0e-15);
    }

    private void doTestDerivatives(AbsoluteDate ref,
                                   double duration, double step, boolean forbidInterpolation,
                                   double cartesianTolerance, double cartesianDotTolerance, double cartesianDotDotTolerance,
                                   double rodriguesTolerance, double rodriguesDotTolerance, double rodriguesDotDotTolerance)
        throws OrekitException {

        final DSFactory factory = new DSFactory(1, 2);
        final FieldAbsoluteDate<DerivativeStructure> refDS = new FieldAbsoluteDate<>(factory.getDerivativeField(), ref);
        FiniteDifferencesDifferentiator differentiator = new FiniteDifferencesDifferentiator(8, 60.0);

        for (final Predefined predefined : Predefined.values()) {
            final Frame frame = FramesFactory.getFrame(predefined);
            final Frame parent = frame.getParent();
            if (parent != null) {

                UnivariateDifferentiableVectorFunction dCartesian = differentiator.differentiate(new UnivariateVectorFunction() {
                    @Override
                    public double[] value(double t) {
                        try {
                            return forbidInterpolation ?
                                   FramesFactory.getNonInterpolatingTransform(parent, frame, ref.shiftedBy(t)).getTranslation().toArray() :
                                   parent.getTransformTo(frame, ref.shiftedBy(t)).getTranslation().toArray();
                        } catch (OrekitException oe) {
                            throw new OrekitExceptionWrapper(oe);
                        }
                    }
                });

                UnivariateDifferentiableVectorFunction dOrientation = differentiator.differentiate(new UnivariateVectorFunction() {
                    double sign = +1.0;
                    Rotation previous = Rotation.IDENTITY;
                    @Override
                    public double[] value(double t) {
                        try {
                            AngularCoordinates ac = forbidInterpolation ?
                                                    FramesFactory.getNonInterpolatingTransform(parent, frame, ref.shiftedBy(t)).getAngular() :
                                                    parent.getTransformTo(frame, ref.shiftedBy(t)).getAngular();
                            final double dot = MathArrays.linearCombination(ac.getRotation().getQ0(), previous.getQ0(),
                                                                            ac.getRotation().getQ1(), previous.getQ1(),
                                                                            ac.getRotation().getQ2(), previous.getQ2(),
                                                                            ac.getRotation().getQ3(), previous.getQ3());
                            sign = FastMath.copySign(1.0, dot * sign);
                            previous = ac.getRotation();
                            return ac.getModifiedRodrigues(sign)[0];
                        } catch (OrekitException oe) {
                            throw new OrekitExceptionWrapper(oe);
                        }
                    }
                });

                double maxCartesianError       = 0;
                double maxCartesianDotError    = 0;
                double maxCartesianDotDotError = 0;
                double maxRodriguesError       = 0;
                double maxRodriguesDotError    = 0;
                double maxRodriguesDotDotError = 0;
                for (double dt = 0; dt < duration; dt += step) {

                    final DerivativeStructure dtDS = factory.variable(0, dt);
                    final FieldTransform<DerivativeStructure> tDS = forbidInterpolation ?
                                                                    FramesFactory.getNonInterpolatingTransform(parent, frame, refDS.shiftedBy(dtDS)) :
                                                                    parent.getTransformTo(frame, refDS.shiftedBy(dtDS));

                    final DerivativeStructure[] refCart   = dCartesian.value(dtDS);
                    final DerivativeStructure[] fieldCart = tDS.getTranslation().toArray();
                    for (int i = 0; i < 3; ++i) {
                        maxCartesianError       = FastMath.max(maxCartesianError,       FastMath.abs(refCart[i].getValue()              - fieldCart[i].getValue()));
                        maxCartesianDotError    = FastMath.max(maxCartesianDotError,    FastMath.abs(refCart[i].getPartialDerivative(1) - fieldCart[i].getPartialDerivative(1)));
                        maxCartesianDotDotError = FastMath.max(maxCartesianDotDotError, FastMath.abs(refCart[i].getPartialDerivative(2) - fieldCart[i].getPartialDerivative(2)));
                    }

                    final DerivativeStructure[] refOr   = dOrientation.value(dtDS);
                    DerivativeStructure[] fieldOr = tDS.getAngular().getModifiedRodrigues(1.0)[0];
                    final double dot = refOr[0].linearCombination(refOr, fieldOr).getReal();
                    if (dot < 0 || Double.isNaN(dot)) {
                        fieldOr = tDS.getAngular().getModifiedRodrigues(-1.0)[0];
                    }
                    for (int i = 0; i < 3; ++i) {
                        maxRodriguesError       = FastMath.max(maxRodriguesError,       FastMath.abs(refOr[i].getValue()              - fieldOr[i].getValue()));
                        maxRodriguesDotError    = FastMath.max(maxRodriguesDotError,    FastMath.abs(refOr[i].getPartialDerivative(1) - fieldOr[i].getPartialDerivative(1)));
                        maxRodriguesDotDotError = FastMath.max(maxRodriguesDotDotError, FastMath.abs(refOr[i].getPartialDerivative(2) - fieldOr[i].getPartialDerivative(2)));
                    }

                }
                Assert.assertEquals(frame.getName(), 0, maxCartesianError,             cartesianTolerance);
                Assert.assertEquals(frame.getName(), 0, maxCartesianDotError,          cartesianDotTolerance);
                Assert.assertEquals(frame.getName(), 0, maxCartesianDotDotError,       cartesianDotDotTolerance);
                Assert.assertEquals(frame.getName(), 0, maxRodriguesError,             rodriguesTolerance);
                Assert.assertEquals(frame.getName(), 0, maxRodriguesDotError,          rodriguesDotTolerance);
                Assert.assertEquals(frame.getName(), 0, maxRodriguesDotDotError,       rodriguesDotDotTolerance);
            }
        }
    }

    @Before
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

}
