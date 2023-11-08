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
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.OrekitMatchers;
import org.orekit.Utils;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;

import java.util.stream.Stream;

public class HelmertTransformationTest {

    @Test
    public void testHelmert20052008() {
        // for this test, we arbitrarily assume FramesFactory provides an ITRF 2008
        Frame itrf2008 = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
        HelmertTransformation.Predefined ht = HelmertTransformation.Predefined.ITRF_2008_TO_ITRF_2005;
        Assertions.assertEquals(ITRFVersion.ITRF_2008, ht.getOrigin());
        Assertions.assertEquals(ITRFVersion.ITRF_2005, ht.getDestination());
        Assertions.assertEquals(new AbsoluteDate(2000, 1, 1, 12, 0, 0, TimeScalesFactory.getTT()),
                            ht.getTransformation().getEpoch());
        Frame itrf2005 = ht.createTransformedITRF(itrf2008, "2005");
        Vector3D pos2005 = new Vector3D(1234567.8, 2345678.9, 3456789.0);

        // check the Helmert transformation as per http://itrf.ign.fr/ITRF_solutions/2008/tp_08-05.php
        AbsoluteDate date = new AbsoluteDate(2005, 1, 1, 12, 0, 0, TimeScalesFactory.getTT());
        Vector3D pos2008 = itrf2005.getTransformTo(itrf2008, date).transformPosition(pos2005);
        Vector3D generalOffset = pos2005.subtract(pos2008);
        Vector3D linearOffset  = computeOffsetLinearly(-0.5, -0.9, -4.7, 0.000, 0.000, 0.000,
                                                        0.3,  0.0,  0.0, 0.000, 0.000, 0.000,
                                                       pos2005, 0.0);
        Vector3D error         = generalOffset.subtract(linearOffset);
        Assertions.assertEquals(0.0, error.getNorm(), 2.0e-13 * pos2005.getNorm());
        MatcherAssert.assertThat(
                itrf2005.getStaticTransformTo(itrf2008, date).transformPosition(pos2005),
                OrekitMatchers.vectorCloseTo(pos2008, 0));

        date = date.shiftedBy(Constants.JULIAN_YEAR);
        pos2008 = itrf2005.getTransformTo(itrf2008, date).transformPosition(pos2005);
        generalOffset = pos2005.subtract(pos2008);
        linearOffset  = computeOffsetLinearly(-0.5, -0.9, -4.7, 0.000, 0.000, 0.000,
                                               0.3,  0.0,  0.0, 0.000, 0.000, 0.000,
                                              pos2005, 1.0);
        error         = generalOffset.subtract(linearOffset);
        Assertions.assertEquals(0.0, error.getNorm(), 2.0e-13 * pos2005.getNorm());
        MatcherAssert.assertThat(
                itrf2005.getStaticTransformTo(itrf2008, date).transformPosition(pos2005),
                OrekitMatchers.vectorCloseTo(pos2008, 0));

    }

    @Test
    public void testHelmert20002005() {
        // for this test, we arbitrarily assume FramesFactory provides an ITRF 2008
        Frame itrf2008 = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
        Frame itrf2000 =
                HelmertTransformation.Predefined.ITRF_2008_TO_ITRF_2000.createTransformedITRF(itrf2008, "2000");
        Frame itrf2005 =
                HelmertTransformation.Predefined.ITRF_2008_TO_ITRF_2005.createTransformedITRF(itrf2008, "2005");
        Vector3D pos2000 = new Vector3D(1234567.8, 2345678.9, 3456789.0);

        // check the Helmert transformation as per http://itrf.ign.fr/ITRF_solutions/2005/tp_05-00.php
        AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        Vector3D pos2005 = itrf2000.getTransformTo(itrf2005, date).transformPosition(pos2000);
        Vector3D generalOffset = pos2000.subtract(pos2005);
        Vector3D linearOffset  = computeOffsetLinearly( 0.1, -0.8, -5.8, 0.000, 0.000, 0.000,
                                                       -0.2,  0.1, -1.8, 0.000, 0.000, 0.000,
                                                       pos2000, 0.0);
        Vector3D error         = generalOffset.subtract(linearOffset);
        Assertions.assertEquals(0.0, error.getNorm(), FastMath.ulp(pos2000.getNorm()));

        date = date.shiftedBy(Constants.JULIAN_YEAR);
        pos2005 = itrf2000.getTransformTo(itrf2005, date).transformPosition(pos2000);
        generalOffset = pos2000.subtract(pos2005);
        linearOffset  = computeOffsetLinearly( 0.1, -0.8, -5.8, 0.000, 0.000, 0.000,
                                               -0.2,  0.1, -1.8, 0.000, 0.000, 0.000,
                                               pos2000, 1.0);
        error         = generalOffset.subtract(linearOffset);
        Assertions.assertEquals(0.0, error.getNorm(), FastMath.ulp(pos2000.getNorm()));

    }

    @Test
    public void testHelmert19972000() {
        // for this test, we arbitrarily assume FramesFactory provides an ITRF 2008
        Frame itrf2008 = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
        Frame itrf2000 =
                HelmertTransformation.Predefined.ITRF_2008_TO_ITRF_2000.createTransformedITRF(itrf2008, "2000");
        Frame itrf97 =
                HelmertTransformation.Predefined.ITRF_2008_TO_ITRF_1997.createTransformedITRF(itrf2008, "97");
        Vector3D pos97 = new Vector3D(1234567.8, 2345678.9, 3456789.0);

        // check the Helmert transformation as per ftp://itrf.ensg.ign.fr/pub/itrf/ITRF.TP
        AbsoluteDate date = new AbsoluteDate(1997, 1, 1, 12, 0, 0, TimeScalesFactory.getTT());
        Vector3D pos2000 = itrf97.getTransformTo(itrf2000, date).transformPosition(pos97);
        Vector3D generalOffset = pos97.subtract(pos2000);
        Vector3D linearOffset  = computeOffsetLinearly( 6.7,  6.1, -18.5, 0.000, 0.000, 0.000,
                                                        0.0, -0.6,  -1.4, 0.000, 0.000, 0.002,
                                                       pos2000, 0.0);
        Vector3D error         = generalOffset.subtract(linearOffset);
        Assertions.assertEquals(0.0, error.getNorm(), 2.0e-11 * pos97.getNorm());

        date = date.shiftedBy(Constants.JULIAN_YEAR);
        pos2000 = itrf97.getTransformTo(itrf2000, date).transformPosition(pos97);
        generalOffset = pos97.subtract(pos2000);
        linearOffset  = computeOffsetLinearly( 6.7,  6.1, -18.5, 0.000, 0.000, 0.000,
                                               0.0, -0.6,  -1.4, 0.000, 0.000, 0.002,
                                               pos2000, 1.0);
        error         = generalOffset.subtract(linearOffset);
        Assertions.assertEquals(0.0, error.getNorm(), 6.0e-11 * pos97.getNorm());

    }

    @Test
    public void testHelmert19932000() {
        // for this test, we arbitrarily assume FramesFactory provides an ITRF 2008
        Frame itrf2008 = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
        Frame itrf2000 =
                HelmertTransformation.Predefined.ITRF_2008_TO_ITRF_2000.createTransformedITRF(itrf2008, "2000");
        Frame itrf93 =
                HelmertTransformation.Predefined.ITRF_2008_TO_ITRF_1993.createTransformedITRF(itrf2008, "93");
        Vector3D pos93 = new Vector3D(1234567.8, 2345678.9, 3456789.0);

        // check the Helmert transformation as per ftp://itrf.ensg.ign.fr/pub/itrf/ITRF.TP
        AbsoluteDate date = new AbsoluteDate(1988, 1, 1, 12, 0, 0, TimeScalesFactory.getTT());
        Vector3D pos2000 = itrf93.getTransformTo(itrf2000, date).transformPosition(pos93);
        Vector3D generalOffset = pos93.subtract(pos2000);
        Vector3D linearOffset  = computeOffsetLinearly(12.7,  6.5, -20.9, -0.39,  0.80, -1.14,
                                                       -2.9, -0.2,  -0.6, -0.11, -0.19,  0.07,
                                                       pos2000, 0.0);
        Vector3D error         = generalOffset.subtract(linearOffset);
        Assertions.assertEquals(0.0, error.getNorm(), FastMath.ulp(pos93.getNorm()));

        date = date.shiftedBy(Constants.JULIAN_YEAR);
        pos2000 = itrf93.getTransformTo(itrf2000, date).transformPosition(pos93);
        generalOffset = pos93.subtract(pos2000);
        linearOffset  = computeOffsetLinearly(12.7,  6.5, -20.9, -0.39,  0.80, -1.14,
                                              -2.9, -0.2,  -0.6, -0.11, -0.19,  0.07,
                                              pos2000, 1.0);
        error         = generalOffset.subtract(linearOffset);
        Assertions.assertEquals(0.0, error.getNorm(), FastMath.ulp(pos93.getNorm()));

    }

    @Test
    public void test2020PivotVs2014Pivot() {
        doTestPivot(2020, 2014);
    }

    @Test
    public void test2020PivotVs2008Pivot() {
        doTestPivot(2020, 2008);
    }

    @Test
    public void test2014PivotVs2008Pivot() {
        doTestPivot(2014, 2008);
    }

    private void doTestPivot(final int year1, final int year2) {

        // for this test, we arbitrarily assume FramesFactory provides an ITRF year 1
        Frame itrfPivot1 = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
        Frame itrfPivot2 = HelmertTransformation.Predefined.selectPredefined(year1, year2).
                           createTransformedITRF(itrfPivot1, Integer.toString(year2));

        Stream.
        of(HelmertTransformation.Predefined.values()).
        filter(p -> p.getOrigin().getYear() == year1 && p.getDestination().getYear() != year2).
        forEach(p1 -> {
            HelmertTransformation.Predefined p2 =
                            HelmertTransformation.Predefined.selectPredefined(year2, p1.getDestination().getYear());
            if (p2 != null) {
                Frame itrfXFrom1 = p1.createTransformedITRF(itrfPivot1, "x-from-1");
                Frame itrfXFrom2 = p2.createTransformedITRF(itrfPivot2, "x-from-2");
                for (int year = 2000; year < 2007; ++year) {
                    AbsoluteDate date = new AbsoluteDate(year, 4, 17, 12, 0, 0, TimeScalesFactory.getTT());
                    Transform t = itrfXFrom2.getTransformTo(itrfXFrom1, date);
                    // the errors are not strictly zero (but they are very small) because
                    // Helmert transformations are a translation plus a rotation. If we do
                    // t1 -> r1 -> t2 -> r2, it is not the same as t1 -> t2 -> r1 -> r2
                    // which would correspond to simply add the offsets, velocities, rotations and rate,
                    // which is what is done in the reference documents. Anyway, the non-commutativity
                    // errors are well below models accuracy
                    Assertions.assertEquals(0, t.getTranslation().getNorm(),  6.0e-6);
                    Assertions.assertEquals(0, t.getVelocity().getNorm(),     2.0e-22);
                    Assertions.assertEquals(0, t.getRotation().getAngle(),    2.0e-12);
                    Assertions.assertEquals(0, t.getRotationRate().getNorm(), 2.0e-32);
                    final StaticTransform st = itrfXFrom2.getStaticTransformTo(itrfXFrom1, date);
                    MatcherAssert.assertThat(st.getTranslation(),
                                             OrekitMatchers.vectorCloseTo(t.getTranslation(), 0));
                    MatcherAssert.assertThat(Rotation.distance(st.getRotation(), t.getRotation()),
                                             OrekitMatchers.closeTo(0, 0));
                }
            }
        });
    }

    private Vector3D computeOffsetLinearly(final double t1,    final double t2,    final double t3,
                                           final double r1,    final double r2,    final double r3,
                                           final double t1Dot, final double t2Dot, final double t3Dot,
                                           final double r1Dot, final double r2Dot, final double r3Dot,
                                           final Vector3D p,   final double dt) {
        double t1U = (t1 + dt * t1Dot) * 1.0e-3;
        double t2U = (t2 + dt * t2Dot) * 1.0e-3;
        double t3U = (t3 + dt * t3Dot) * 1.0e-3;
        double r1U = FastMath.toRadians((r1 + dt * r1Dot) / 3.6e6);
        double r2U = FastMath.toRadians((r2 + dt * r2Dot) / 3.6e6);
        double r3U = FastMath.toRadians((r3 + dt * r3Dot) / 3.6e6);
        return new Vector3D(t1U - r3U * p.getY() + r2U * p.getZ(),
                            t2U + r3U * p.getX() - r1U * p.getZ(),
                            t3U - r2U * p.getX() + r1U * p.getY());
    }

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("compressed-data");
    }

}
