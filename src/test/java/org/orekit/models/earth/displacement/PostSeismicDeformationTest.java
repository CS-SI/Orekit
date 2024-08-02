/* Copyright 2002-2024 Thales Alenia Space
 * Licensed to CS Communication & Systèmes (CS) under one or more
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
package org.orekit.models.earth.displacement;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.data.DataSource;
import org.orekit.data.FundamentalNutationArguments;
import org.orekit.files.sinex.SinexLoader;
import org.orekit.files.sinex.Station;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;

import java.net.URISyntaxException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.net.URL;

class PostSeismicDeformationTest {

    @Test
    void testShintotsukawa() throws URISyntaxException {
        final URL url = PostSeismicDeformationTest.class.getClassLoader().
                        getResource("sinex/ITRF2020-psd-gnss.snx");
        final Station stk2 = new SinexLoader(new DataSource(url.toURI())).getStation("STK2");

        final IERSConventions              conventions = IERSConventions.IERS_2010;
        final TimeScale                    utc         = TimeScalesFactory.getUTC();
        final TimeScale                    ut1         = TimeScalesFactory.getUT1(conventions, false);
        final FundamentalNutationArguments fna         = conventions.getNutationArguments(ut1);
        final Frame                        earthFrame  = FramesFactory.getITRF(conventions, false);
        final OneAxisEllipsoid             earth       = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                                              Constants.WGS84_EARTH_FLATTENING,
                                                                              earthFrame);

        // https://earthquake.usgs.gov/earthquakes/eventpage/usp000c8kv/executive
        final AbsoluteDate date2003 =  new AbsoluteDate(2003, 9, 25, 19, 50, 6, utc);
        final GeodeticPoint base    = earth.transform(stk2.getPosition(), earthFrame, null);
        final PostSeismicDeformation psd = new PostSeismicDeformation(base, stk2.getPsdTimeSpanMap());

        for (double years = 0; years < 1.0; years += 0.01) {
            final Vector3D dp = psd.displacement(fna.evaluateAll( date2003.shiftedBy(years * Constants.JULIAN_YEAR)),
                                                 earthFrame, stk2.getPosition());

            // reference values from manual analysis of PSD data file
            final double refEast  = -3.12544483981059e-02 * (1 - FastMath.exp(-years / 3.90382962596289e-01)) +
                                     2.74994599757755e-02 * FastMath.log(1 + years / 6.84349501571032e-02);
            final double refNorth = -3.97046280518505e-02 * (1 - FastMath.exp(-years / 1.38871759192151e+00)) +
                                    -1.44921478188329e-02 * (1 - FastMath.exp(-years / 6.08619870701242e-02));
            final double refUp    = 0.0;

            assertEquals(refEast,  Vector3D.dotProduct(dp, base.getEast()),   1.0e-15);
            assertEquals(refNorth, Vector3D.dotProduct(dp, base.getNorth()),  1.0e-15);
            assertEquals(refUp,    Vector3D.dotProduct(dp, base.getZenith()), 1.0e-15);

        }

    }

    @BeforeEach
    void setUp() {
        Utils.setDataRoot("regular-data");
    }

}
