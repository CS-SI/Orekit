/* Copyright 2023 Thales Alenia Space
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

import java.net.URISyntaxException;
import java.net.URL;
import java.util.Map;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.data.BodiesElements;
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
import org.orekit.utils.units.Unit;

public class TectonicsDisplacementTest {

    @Test
    public void testLviv() throws URISyntaxException {
        final URL url = TectonicsDisplacementTest.class.getClassLoader().
                        getResource("sinex/SLRF2008_150928_2015.09.28.snx");
        final Map<String, Station>         stations    = new SinexLoader(new DataSource(url.toURI())).getStations();
        final IERSConventions              conventions = IERSConventions.IERS_2010;
        final TimeScale                    utc         = TimeScalesFactory.getUTC();
        final TimeScale                    ut1         = TimeScalesFactory.getUT1(conventions, false);
        final FundamentalNutationArguments fna         = conventions.getNutationArguments(ut1);
        final Frame                        earthFrame  = FramesFactory.getITRF(conventions, false);

        // 1831  A 12368S001 L Lviv       LVIV         23 57 15.8  49 55  3.2   359.8     18318501
        // ...
        // 13 STAX   1831  A    1 05:001:00000 m    2 0.376067473563698E+07 0.32330E-02
        // 14 STAY   1831  A    1 05:001:00000 m    2 0.167077643037227E+07 0.36276E-02
        // 15 STAZ   1831  A    1 05:001:00000 m    2 0.485716543779447E+07 0.26569E-02
        // 16 VELX   1831  A    1 05:001:00000 m/y  2 -.228163272262724E-02 0.28320E-02
        // 17 VELY   1831  A    1 05:001:00000 m/y  2 0.175681714423597E-01 0.34733E-02
        // 18 VELZ   1831  A    1 05:001:00000 m/y  2 0.408218739007309E-01 0.24574E-02
        final Station lviv = stations.get("1831");
        Assertions.assertEquals(0.0,
                                Vector3D.distance(new Vector3D(0.376067473563698E+07,
                                                               0.167077643037227E+07,
                                                               0.485716543779447E+07),
                                                  lviv.getPosition()),
                                1.0e-8);
        final Unit mPy = Unit.parse("m/yr");
        Assertions.assertEquals(0.0,
                                Vector3D.distance(new Vector3D(mPy.toSI(-.228163272262724E-02),
                                                               mPy.toSI(0.175681714423597E-01),
                                                               mPy.toSI(0.408218739007309E-01)),
                                                  lviv.getVelocity()),
                                1.0e-20);
        Assertions.assertEquals(0.0,
                                lviv.getEpoch().durationFrom(new AbsoluteDate(2005, 1, 1, utc)),
                                1.0e-15);

        final TectonicsDisplacement displacement = new TectonicsDisplacement(lviv.getEpoch(),
                                                                             lviv.getVelocity());
        final AbsoluteDate          sixMonthsLater = lviv.getEpoch().shiftedBy(0.5 * Constants.JULIAN_YEAR);
        final BodiesElements        elements       = fna.evaluateAll(sixMonthsLater);
        final Vector3D              dP             = displacement.displacement(elements, earthFrame, lviv.getPosition());
        Assertions.assertEquals(0.5 * -.228163272262724E-02, dP.getX(), 1.0e-16);
        Assertions.assertEquals(0.5 * 0.175681714423597E-01, dP.getY(), 1.0e-16);
        Assertions.assertEquals(0.5 * 0.408218739007309E-01, dP.getZ(), 1.0e-16);
        

    }

    @BeforeEach
    public void setUp() throws Exception {
        Utils.setDataRoot("regular-data");
    }

}
