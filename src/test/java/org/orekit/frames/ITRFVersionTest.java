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

import org.hamcrest.MatcherAssert;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.util.Binary64;
import org.hipparchus.util.Binary64Field;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.OrekitMatchers;
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
        Assertions.assertEquals(2014, ITRFVersion.ITRF_2014.getYear());
        Assertions.assertEquals(2008, ITRFVersion.ITRF_2008.getYear());
        Assertions.assertEquals(2005, ITRFVersion.ITRF_2005.getYear());
        Assertions.assertEquals(2000, ITRFVersion.ITRF_2000.getYear());
        Assertions.assertEquals(1997, ITRFVersion.ITRF_1997.getYear());
        Assertions.assertEquals(1996, ITRFVersion.ITRF_1996.getYear());
        Assertions.assertEquals(1994, ITRFVersion.ITRF_1994.getYear());
        Assertions.assertEquals(1993, ITRFVersion.ITRF_1993.getYear());
        Assertions.assertEquals(1992, ITRFVersion.ITRF_1992.getYear());
        Assertions.assertEquals(1991, ITRFVersion.ITRF_1991.getYear());
        Assertions.assertEquals(1990, ITRFVersion.ITRF_1990.getYear());
        Assertions.assertEquals(1989, ITRFVersion.ITRF_1989.getYear());
        Assertions.assertEquals(1988, ITRFVersion.ITRF_1988.getYear());
    }

    @Test
    public void testNames() {
        Assertions.assertEquals("ITRF-2014", ITRFVersion.ITRF_2014.getName());
        Assertions.assertEquals("ITRF-2008", ITRFVersion.ITRF_2008.getName());
        Assertions.assertEquals("ITRF-2005", ITRFVersion.ITRF_2005.getName());
        Assertions.assertEquals("ITRF-2000", ITRFVersion.ITRF_2000.getName());
        Assertions.assertEquals("ITRF-1997", ITRFVersion.ITRF_1997.getName());
        Assertions.assertEquals("ITRF-1996", ITRFVersion.ITRF_1996.getName());
        Assertions.assertEquals("ITRF-1994", ITRFVersion.ITRF_1994.getName());
        Assertions.assertEquals("ITRF-1993", ITRFVersion.ITRF_1993.getName());
        Assertions.assertEquals("ITRF-1992", ITRFVersion.ITRF_1992.getName());
        Assertions.assertEquals("ITRF-1991", ITRFVersion.ITRF_1991.getName());
        Assertions.assertEquals("ITRF-1990", ITRFVersion.ITRF_1990.getName());
        Assertions.assertEquals("ITRF-1989", ITRFVersion.ITRF_1989.getName());
        Assertions.assertEquals("ITRF-1988", ITRFVersion.ITRF_1988.getName());
    }

    @Test
    public void testBuildFromYear() {
        Assertions.assertEquals(ITRFVersion.ITRF_2014, ITRFVersion.getITRFVersion(2014));
        Assertions.assertEquals(ITRFVersion.ITRF_2008, ITRFVersion.getITRFVersion(2008));
        Assertions.assertEquals(ITRFVersion.ITRF_2005, ITRFVersion.getITRFVersion(2005));
        Assertions.assertEquals(ITRFVersion.ITRF_2000, ITRFVersion.getITRFVersion(2000));
        Assertions.assertEquals(ITRFVersion.ITRF_1997, ITRFVersion.getITRFVersion(1997));
        Assertions.assertEquals(ITRFVersion.ITRF_1996, ITRFVersion.getITRFVersion(1996));
        Assertions.assertEquals(ITRFVersion.ITRF_1994, ITRFVersion.getITRFVersion(1994));
        Assertions.assertEquals(ITRFVersion.ITRF_1993, ITRFVersion.getITRFVersion(1993));
        Assertions.assertEquals(ITRFVersion.ITRF_1992, ITRFVersion.getITRFVersion(1992));
        Assertions.assertEquals(ITRFVersion.ITRF_1991, ITRFVersion.getITRFVersion(1991));
        Assertions.assertEquals(ITRFVersion.ITRF_1990, ITRFVersion.getITRFVersion(1990));
        Assertions.assertEquals(ITRFVersion.ITRF_1989, ITRFVersion.getITRFVersion(1989));
        Assertions.assertEquals(ITRFVersion.ITRF_1988, ITRFVersion.getITRFVersion(1988));
        Assertions.assertEquals(ITRFVersion.ITRF_1997, ITRFVersion.getITRFVersion(  97));
        Assertions.assertEquals(ITRFVersion.ITRF_1996, ITRFVersion.getITRFVersion(  96));
        Assertions.assertEquals(ITRFVersion.ITRF_1994, ITRFVersion.getITRFVersion(  94));
        Assertions.assertEquals(ITRFVersion.ITRF_1993, ITRFVersion.getITRFVersion(  93));
        Assertions.assertEquals(ITRFVersion.ITRF_1992, ITRFVersion.getITRFVersion(  92));
        Assertions.assertEquals(ITRFVersion.ITRF_1991, ITRFVersion.getITRFVersion(  91));
        Assertions.assertEquals(ITRFVersion.ITRF_1990, ITRFVersion.getITRFVersion(  90));
        Assertions.assertEquals(ITRFVersion.ITRF_1989, ITRFVersion.getITRFVersion(  89));
        Assertions.assertEquals(ITRFVersion.ITRF_1988, ITRFVersion.getITRFVersion(  88));
    }

    @Test
    public void testInexistantYear() {
        try {
            ITRFVersion.getITRFVersion(1999);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.NO_SUCH_ITRF_FRAME, oe.getSpecifier());
            Assertions.assertEquals(1999, ((Integer) oe.getParts()[0]).intValue());
        }
    }

    @Test
    public void testBuildFromName() {
        Assertions.assertEquals(ITRFVersion.ITRF_2014, ITRFVersion.getITRFVersion("ITRF-2014"));
        Assertions.assertEquals(ITRFVersion.ITRF_2008, ITRFVersion.getITRFVersion("ItRf-2008"));
        Assertions.assertEquals(ITRFVersion.ITRF_2005, ITRFVersion.getITRFVersion("iTrF-2005"));
        Assertions.assertEquals(ITRFVersion.ITRF_2000, ITRFVersion.getITRFVersion("itrf_2000"));
        Assertions.assertEquals(ITRFVersion.ITRF_1997, ITRFVersion.getITRFVersion("itrf 1997"));
        Assertions.assertEquals(ITRFVersion.ITRF_1996, ITRFVersion.getITRFVersion("itrf1996"));
        Assertions.assertEquals(ITRFVersion.ITRF_1994, ITRFVersion.getITRFVersion("itrf-1994"));
        Assertions.assertEquals(ITRFVersion.ITRF_1993, ITRFVersion.getITRFVersion("itrf-1993"));
        Assertions.assertEquals(ITRFVersion.ITRF_1992, ITRFVersion.getITRFVersion("itrf-1992"));
        Assertions.assertEquals(ITRFVersion.ITRF_1991, ITRFVersion.getITRFVersion("itrf-1991"));
        Assertions.assertEquals(ITRFVersion.ITRF_1990, ITRFVersion.getITRFVersion("itrf-1990"));
        Assertions.assertEquals(ITRFVersion.ITRF_1989, ITRFVersion.getITRFVersion("itrf-1989"));
        Assertions.assertEquals(ITRFVersion.ITRF_1988, ITRFVersion.getITRFVersion("itrf-1988"));
        Assertions.assertEquals(ITRFVersion.ITRF_1997, ITRFVersion.getITRFVersion("ITRF97"));
        Assertions.assertEquals(ITRFVersion.ITRF_1996, ITRFVersion.getITRFVersion("itrf-96"));
        Assertions.assertEquals(ITRFVersion.ITRF_1994, ITRFVersion.getITRFVersion("itrf-94"));
        Assertions.assertEquals(ITRFVersion.ITRF_1993, ITRFVersion.getITRFVersion("itrf-93"));
        Assertions.assertEquals(ITRFVersion.ITRF_1992, ITRFVersion.getITRFVersion("itrf-92"));
        Assertions.assertEquals(ITRFVersion.ITRF_1991, ITRFVersion.getITRFVersion("itrf-91"));
        Assertions.assertEquals(ITRFVersion.ITRF_1990, ITRFVersion.getITRFVersion("itrf-90"));
        Assertions.assertEquals(ITRFVersion.ITRF_1989, ITRFVersion.getITRFVersion("itrf-89"));
        Assertions.assertEquals(ITRFVersion.ITRF_1988, ITRFVersion.getITRFVersion("itrf-88"));
    }

    @Test
    public void testInexistantName() {
        try {
            ITRFVersion.getITRFVersion("itrf-99");
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.NO_SUCH_ITRF_FRAME, oe.getSpecifier());
            Assertions.assertEquals("itrf-99", oe.getParts()[0]);
        }
    }

    @Test
    public void testMalformedName() {
        try {
            ITRFVersion.getITRFVersion("YTRF-2014");
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.NO_SUCH_ITRF_FRAME, oe.getSpecifier());
            Assertions.assertEquals("YTRF-2014", oe.getParts()[0]);
        }
    }

    @Test
    public void testAllConverters() {

        // select the last supported ITRF version
        ITRFVersion last = ITRFVersion.getLast();

        // for this test, we arbitrarily assume FramesFactory provides an ITRF in last supported version
        Frame itrfLast = FramesFactory.getITRF(IERSConventions.IERS_2010, true);

        for (final ITRFVersion origin : ITRFVersion.values()) {
            for (final ITRFVersion destination : ITRFVersion.values()) {
                ITRFVersion.Converter converter = ITRFVersion.getConverter(origin, destination);
                Assertions.assertEquals(origin,      converter.getOrigin());
                Assertions.assertEquals(destination, converter.getDestination());
                Frame originFrame      = origin == last ?
                                         itrfLast :
                                         HelmertTransformation.Predefined.selectPredefined(last.getYear(), origin.getYear()).
                                         createTransformedITRF(itrfLast, origin.toString());
                Frame destinationFrame = destination == last ?
                                         itrfLast :
                                         HelmertTransformation.Predefined.selectPredefined(last.getYear(), destination.getYear()).
                                         createTransformedITRF(itrfLast, destination.toString());
                for (int year = 2000; year < 2007; ++year) {

                    AbsoluteDate date = new AbsoluteDate(year, 4, 17, 12, 0, 0, TimeScalesFactory.getTT());
                    Transform looped =
                           new Transform(date,
                                         converter.getTransform(date),
                                         destinationFrame.getTransformTo(originFrame, date));
                    StaticTransform sLooped = StaticTransform.compose(
                                    date,
                                    converter.getStaticTransform(date),
                                    destinationFrame.getStaticTransformTo(originFrame, date));
                    if (origin != last && destination != last) {
                        // if we use two old ITRF, as internally the pivot frame is the last one
                        // one side of the transform is computed as f -> itrf-1 -> itrf-last, and on
                        // the other side as f -> itrf-last and we get some inversion in between.
                        // the errors are not strictly zero (but they are very small) because
                        // Helmert transformations are a translation plus a rotation. If we do
                        // t1 -> r1 -> t2 -> r2, it is not the same as t1 -> t2 -> r1 -> r2
                        // which would correspond to simply add the offsets, velocities, rotations and rate,
                        // which is what is done in the reference documents.
                        // Anyway, the non-commutativity errors are well below models accuracy
                        Assertions.assertEquals(0, looped.getTranslation().getNorm(),  3.0e-06);
                        Assertions.assertEquals(0, looped.getVelocity().getNorm(),     9.0e-23);
                        Assertions.assertEquals(0, looped.getRotation().getAngle(),    8.0e-13);
                        Assertions.assertEquals(0, looped.getRotationRate().getNorm(), 2.0e-32);
                    } else {
                        // if we always stay in the ITRF last branch, we do the right conversions
                        // and errors are at numerical noise level
                        Assertions.assertEquals(0, looped.getTranslation().getNorm(),  2.0e-17);
                        Assertions.assertEquals(0, looped.getVelocity().getNorm(),     4.0e-26);
                        Assertions.assertEquals(0, looped.getRotation().getAngle(),    1.0e-50);
                        Assertions.assertEquals(0, looped.getRotationRate().getNorm(), 2.0e-32);
                    }
                    MatcherAssert.assertThat(
                            sLooped.getTranslation(),
                            OrekitMatchers.vectorCloseTo(looped.getTranslation(), 0));
                    MatcherAssert.assertThat(
                            Rotation.distance(sLooped.getRotation(), looped.getRotation()),
                            OrekitMatchers.closeTo(0, 0));

                    FieldAbsoluteDate<Binary64> date64 = new FieldAbsoluteDate<>(Binary64Field.getInstance(), date);
                    FieldTransform<Binary64> looped64 =
                                    new FieldTransform<>(date64,
                                                         converter.getTransform(date64),
                                                         destinationFrame.getTransformTo(originFrame, date64));
                             if (origin != last && destination != last) {
                                 // if we use two old ITRF, as internally the pivot frame is the last one
                                 // one side of the transform is computed as f -> itrf-1 -> itrf-last, and on
                                 // the other side as f -> itrf-last and we get some inversion in between.
                                 // the errors are not strictly zero (but they are very small) because
                                 // Helmert transformations are a translation plus a rotation. If we do
                                 // t1 -> r1 -> t2 -> r2, it is not the same as t1 -> t2 -> r1 -> r2
                                 // which would correspond to simply add the offsets, velocities, rotations and rate,
                                 // which is what is done in the reference documents.
                                 // Anyway, the non-commutativity errors are well below models accuracy
                                 Assertions.assertEquals(0, looped64.getTranslation().getNorm().getReal(),  3.0e-06);
                                 Assertions.assertEquals(0, looped64.getVelocity().getNorm().getReal(),     9.0e-23);
                                 Assertions.assertEquals(0, looped64.getRotation().getAngle().getReal(),    8.0e-13);
                                 Assertions.assertEquals(0, looped64.getRotationRate().getNorm().getReal(), 2.0e-32);
                             } else {
                                 // if we always stay in the ITRF last branch, we do the right conversions
                                 // and errors are at numerical noise level
                                 Assertions.assertEquals(0, looped64.getTranslation().getNorm().getReal(),  2.0e-17);
                                 Assertions.assertEquals(0, looped64.getVelocity().getNorm().getReal(),     4.0e-26);
                                 Assertions.assertEquals(0, looped64.getRotation().getAngle().getReal(),    1.0e-50);
                                 Assertions.assertEquals(0, looped64.getRotationRate().getNorm().getReal(), 2.0e-32);
                             }
                }
            }
        }
    }

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("compressed-data");
    }

}
