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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitIllegalStateException;
import org.orekit.errors.OrekitMessages;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.ChronologicalComparator;
import org.orekit.time.DateComponents;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.AngularDerivativesFilter;
import org.orekit.utils.CartesianDerivativesFilter;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.IERSConventions.NutationCorrectionConverter;

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

    @Before
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

}
