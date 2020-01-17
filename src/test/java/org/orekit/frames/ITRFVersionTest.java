/* Copyright 2002-2020 CS Group
 * Licensed to CS Group (CS) under one or more
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


import org.hipparchus.util.Decimal64;
import org.hipparchus.util.Decimal64Field;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.IERSConventions;

public class ITRFVersionTest {

    @Test
    public void testYears() {
        Assert.assertEquals(2014, ITRFVersion.ITRF_2014.getYear());
        Assert.assertEquals(2008, ITRFVersion.ITRF_2008.getYear());
        Assert.assertEquals(2005, ITRFVersion.ITRF_2005.getYear());
        Assert.assertEquals(2000, ITRFVersion.ITRF_2000.getYear());
        Assert.assertEquals(1997, ITRFVersion.ITRF_97.getYear());
        Assert.assertEquals(1996, ITRFVersion.ITRF_96.getYear());
        Assert.assertEquals(1994, ITRFVersion.ITRF_94.getYear());
        Assert.assertEquals(1993, ITRFVersion.ITRF_93.getYear());
        Assert.assertEquals(1992, ITRFVersion.ITRF_92.getYear());
        Assert.assertEquals(1991, ITRFVersion.ITRF_91.getYear());
        Assert.assertEquals(1990, ITRFVersion.ITRF_90.getYear());
        Assert.assertEquals(1989, ITRFVersion.ITRF_89.getYear());
        Assert.assertEquals(1988, ITRFVersion.ITRF_88.getYear());
    }

    @Test
    public void testNames() {
        Assert.assertEquals("ITRF-2014", ITRFVersion.ITRF_2014.getName());
        Assert.assertEquals("ITRF-2008", ITRFVersion.ITRF_2008.getName());
        Assert.assertEquals("ITRF-2005", ITRFVersion.ITRF_2005.getName());
        Assert.assertEquals("ITRF-2000", ITRFVersion.ITRF_2000.getName());
        Assert.assertEquals("ITRF-97",   ITRFVersion.ITRF_97.getName());
        Assert.assertEquals("ITRF-96",   ITRFVersion.ITRF_96.getName());
        Assert.assertEquals("ITRF-94",   ITRFVersion.ITRF_94.getName());
        Assert.assertEquals("ITRF-93",   ITRFVersion.ITRF_93.getName());
        Assert.assertEquals("ITRF-92",   ITRFVersion.ITRF_92.getName());
        Assert.assertEquals("ITRF-91",   ITRFVersion.ITRF_91.getName());
        Assert.assertEquals("ITRF-90",   ITRFVersion.ITRF_90.getName());
        Assert.assertEquals("ITRF-89",   ITRFVersion.ITRF_89.getName());
        Assert.assertEquals("ITRF-88",   ITRFVersion.ITRF_88.getName());
    }

    @Test
    public void testBuildFromYear() {
        Assert.assertEquals(ITRFVersion.ITRF_2014, ITRFVersion.getITRFVersion(2014));
        Assert.assertEquals(ITRFVersion.ITRF_2008, ITRFVersion.getITRFVersion(2008));
        Assert.assertEquals(ITRFVersion.ITRF_2005, ITRFVersion.getITRFVersion(2005));
        Assert.assertEquals(ITRFVersion.ITRF_2000, ITRFVersion.getITRFVersion(2000));
        Assert.assertEquals(ITRFVersion.ITRF_97,   ITRFVersion.getITRFVersion(1997));
        Assert.assertEquals(ITRFVersion.ITRF_96,   ITRFVersion.getITRFVersion(1996));
        Assert.assertEquals(ITRFVersion.ITRF_94,   ITRFVersion.getITRFVersion(1994));
        Assert.assertEquals(ITRFVersion.ITRF_93,   ITRFVersion.getITRFVersion(1993));
        Assert.assertEquals(ITRFVersion.ITRF_92,   ITRFVersion.getITRFVersion(1992));
        Assert.assertEquals(ITRFVersion.ITRF_91,   ITRFVersion.getITRFVersion(1991));
        Assert.assertEquals(ITRFVersion.ITRF_90,   ITRFVersion.getITRFVersion(1990));
        Assert.assertEquals(ITRFVersion.ITRF_89,   ITRFVersion.getITRFVersion(1989));
        Assert.assertEquals(ITRFVersion.ITRF_88,   ITRFVersion.getITRFVersion(1988));
    }

    @Test
    public void testInexistantYear() {
        try {
            ITRFVersion.getITRFVersion(1999);
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.NO_SUCH_ITRF_FRAME, oe.getSpecifier());
            Assert.assertEquals(1999, ((Integer) oe.getParts()[0]).intValue());
        }
    }

    @Test
    public void testBuildFromName() {
        Assert.assertEquals(ITRFVersion.ITRF_2014, ITRFVersion.getITRFVersion("itrf-2014"));
        Assert.assertEquals(ITRFVersion.ITRF_2008, ITRFVersion.getITRFVersion("itrf-2008"));
        Assert.assertEquals(ITRFVersion.ITRF_2005, ITRFVersion.getITRFVersion("itrf-2005"));
        Assert.assertEquals(ITRFVersion.ITRF_2000, ITRFVersion.getITRFVersion("itrf-2000"));
        Assert.assertEquals(ITRFVersion.ITRF_97,   ITRFVersion.getITRFVersion("itrf-97"));
        Assert.assertEquals(ITRFVersion.ITRF_96,   ITRFVersion.getITRFVersion("itrf-96"));
        Assert.assertEquals(ITRFVersion.ITRF_94,   ITRFVersion.getITRFVersion("itrf-94"));
        Assert.assertEquals(ITRFVersion.ITRF_93,   ITRFVersion.getITRFVersion("itrf-93"));
        Assert.assertEquals(ITRFVersion.ITRF_92,   ITRFVersion.getITRFVersion("itrf-92"));
        Assert.assertEquals(ITRFVersion.ITRF_91,   ITRFVersion.getITRFVersion("itrf-91"));
        Assert.assertEquals(ITRFVersion.ITRF_90,   ITRFVersion.getITRFVersion("itrf-90"));
        Assert.assertEquals(ITRFVersion.ITRF_89,   ITRFVersion.getITRFVersion("itrf-89"));
        Assert.assertEquals(ITRFVersion.ITRF_88,   ITRFVersion.getITRFVersion("itrf-88"));
    }

    @Test
    public void testInexistantName() {
        try {
            ITRFVersion.getITRFVersion("itrf-99");
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.NO_SUCH_ITRF_FRAME, oe.getSpecifier());
            Assert.assertEquals("itrf-99", oe.getParts()[0]);
        }
    }

    @Test
    public void testAllConverters() {

        // for this test, we arbitrarily assume FramesFactory provides an ITRF 2014
        Frame itrf2014 = FramesFactory.getITRF(IERSConventions.IERS_2010, true);

        for (final ITRFVersion origin : ITRFVersion.values()) {
            for (final ITRFVersion destination : ITRFVersion.values()) {
                ITRFVersion.Converter converter = ITRFVersion.getConverter(origin, destination);
                Assert.assertEquals(origin,      converter.getOrigin());
                Assert.assertEquals(destination, converter.getDestination());
                Frame originFrame      = origin == ITRFVersion.ITRF_2014 ?
                                         itrf2014 :
                                         from2014(origin.getYear()).createTransformedITRF(itrf2014, origin.toString());
                Frame destinationFrame = destination == ITRFVersion.ITRF_2014 ?
                                         itrf2014 :
                                         from2014(destination.getYear()).createTransformedITRF(itrf2014, destination.toString());
                for (int year = 2000; year < 2007; ++year) {

                    AbsoluteDate date = new AbsoluteDate(year, 4, 17, 12, 0, 0, TimeScalesFactory.getTT());
                    Transform looped =
                           new Transform(date,
                                         converter.getTransform(date),
                                         destinationFrame.getTransformTo(originFrame, date));
                    if (origin == ITRFVersion.ITRF_2008 || destination == ITRFVersion.ITRF_2008) {
                        // if we use ITRF 2008, as internally the pivot frame is ITRF 2014
                        // on side of the transform is computed as f -> 2008 -> 2014, and on
                        // the other side as f -> 2014 and we get some inversion in between.
                        // the errors are not strictly zero (but they are very small) because
                        // Helmert transformations are a translation plus a rotation. If we do
                        // t1 -> r1 -> t2 -> r2, it is not the same as t1 -> t2 -> r1 -> r2
                        // which would correspond to simply add the offsets, velocities, rotations and rate,
                        // which is what is done in the reference documents.
                        // Anyway, the non-commutativity errors are well below models accuracy
                        Assert.assertEquals(0, looped.getTranslation().getNorm(),  6.0e-06);
                        Assert.assertEquals(0, looped.getVelocity().getNorm(),     2.0e-22);
                        Assert.assertEquals(0, looped.getRotation().getAngle(),    2.0e-12);
                        Assert.assertEquals(0, looped.getRotationRate().getNorm(), 2.0e-32);
                    } else {
                        // if we always stay in the ITRF 2014 branch, we do the right conversions
                        // and errors are at numerical noise level
                        Assert.assertEquals(0, looped.getTranslation().getNorm(),  6.0e-17);
                        Assert.assertEquals(0, looped.getVelocity().getNorm(),     4.0e-26);
                        Assert.assertEquals(0, looped.getRotation().getAngle(),    1.0e-40);
                        Assert.assertEquals(0, looped.getRotationRate().getNorm(), 2.0e-32);
                    }

                    FieldAbsoluteDate<Decimal64> date64 = new FieldAbsoluteDate<>(Decimal64Field.getInstance(), date);
                    FieldTransform<Decimal64> looped64 =
                                    new FieldTransform<>(date64,
                                                         converter.getTransform(date64),
                                                         destinationFrame.getTransformTo(originFrame, date64));
                             if (origin == ITRFVersion.ITRF_2008 || destination == ITRFVersion.ITRF_2008) {
                                 // if we use ITRF 2008, as internally the pivot frame is ITRF 2014
                                 // on side of the transform is computed as f -> 2008 -> 2014, and on
                                 // the other side as f -> 2014 and we get some inversion in between.
                                 // the errors are not strictly zero (but they are very small) because
                                 // Helmert transformations are a translation plus a rotation. If we do
                                 // t1 -> r1 -> t2 -> r2, it is not the same as t1 -> t2 -> r1 -> r2
                                 // which would correspond to simply add the offsets, velocities, rotations and rate,
                                 // which is what is done in the reference documents.
                                 // Anyway, the non-commutativity errors are well below models accuracy
                                 Assert.assertEquals(0, looped64.getTranslation().getNorm().getReal(),  6.0e-06);
                                 Assert.assertEquals(0, looped64.getVelocity().getNorm().getReal(),     2.0e-22);
                                 Assert.assertEquals(0, looped64.getRotation().getAngle().getReal(),    2.0e-12);
                                 Assert.assertEquals(0, looped64.getRotationRate().getNorm().getReal(), 2.0e-32);
                             } else {
                                 // if we always stay in the ITRF 2014 branch, we do the right conversions
                                 // and errors are at numerical noise level
                                 Assert.assertEquals(0, looped64.getTranslation().getNorm().getReal(),  6.0e-17);
                                 Assert.assertEquals(0, looped64.getVelocity().getNorm().getReal(),     4.0e-26);
                                 Assert.assertEquals(0, looped64.getRotation().getAngle().getReal(),    1.0e-40);
                                 Assert.assertEquals(0, looped64.getRotationRate().getNorm().getReal(), 2.0e-32);
                             }
                }
            }
        }
    }

    private HelmertTransformation.Predefined from2014(final int year) {
        switch (year) {
            case 1988 : return HelmertTransformation.Predefined.ITRF_2014_TO_ITRF_88;
            case 1989 : return HelmertTransformation.Predefined.ITRF_2014_TO_ITRF_89;
            case 1990 : return HelmertTransformation.Predefined.ITRF_2014_TO_ITRF_90;
            case 1991 : return HelmertTransformation.Predefined.ITRF_2014_TO_ITRF_91;
            case 1992 : return HelmertTransformation.Predefined.ITRF_2014_TO_ITRF_92;
            case 1993 : return HelmertTransformation.Predefined.ITRF_2014_TO_ITRF_93;
            case 1994 : return HelmertTransformation.Predefined.ITRF_2014_TO_ITRF_94;
            case 1996 : return HelmertTransformation.Predefined.ITRF_2014_TO_ITRF_96;
            case 1997 : return HelmertTransformation.Predefined.ITRF_2014_TO_ITRF_97;
            case 2000 : return HelmertTransformation.Predefined.ITRF_2014_TO_ITRF_2000;
            case 2005 : return HelmertTransformation.Predefined.ITRF_2014_TO_ITRF_2005;
            case 2008 : return HelmertTransformation.Predefined.ITRF_2014_TO_ITRF_2008;
            default : return null;
        }
    }

    @Before
    public void setUp() {
        Utils.setDataRoot("compressed-data");
    }

}
