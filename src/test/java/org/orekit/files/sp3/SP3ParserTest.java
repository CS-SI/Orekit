/* Licensed to CS Communication & Systèmes (CS) under one or more
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
package org.orekit.files.sp3;

import java.io.InputStream;
import java.util.List;

import junit.framework.Assert;

import org.apache.commons.math.geometry.euclidean.threed.Vector3D;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.errors.OrekitException;
import org.orekit.files.SatelliteTimeCoordinate;
import org.orekit.files.OrbitFile.TimeSystem;
import org.orekit.files.sp3.SP3File;
import org.orekit.files.sp3.SP3Parser;
import org.orekit.files.sp3.SP3File.OrbitType;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.PVCoordinates;

public class SP3ParserTest {

    @Test
    public void testParseSP3a1() throws OrekitException {
        // simple test for version sp3-a, only contains position entries
        String ex = "sp3_a_example1.txt";

        SP3Parser parser = new SP3Parser();
        InputStream inEntry = getClass().getResourceAsStream("/sp3/" + ex);
        SP3File file = parser.parse(inEntry);

        Assert.assertEquals(OrbitType.FIT, file.getOrbitType());
        Assert.assertEquals(TimeSystem.GPS, file.getTimeSystem());

        Assert.assertEquals(25, file.getSatelliteCount());

        List<SatelliteTimeCoordinate> coords = file
                .getSatelliteCoordinates("1");
        Assert.assertEquals(3, coords.size());

        SatelliteTimeCoordinate coord = coords.get(0);

        // 1994 12 17 0 0 0.00000000
        Assert.assertEquals(new AbsoluteDate(1994, 12, 17, 0, 0, 0,
                TimeScalesFactory.getGPS()), coord.getEpoch());

        // P 1 16258.524750 -3529.015750 -20611.427050 -62.540600
        // TODO: check the position entry
    }

    @Test
    public void testParseSP3a2() throws OrekitException {
        // simple test for version sp3-a, contains p/v entries
        String ex = "sp3_a_example2.txt";

        SP3Parser parser = new SP3Parser();
        InputStream inEntry = getClass().getResourceAsStream("/sp3/" + ex);
        SP3File file = parser.parse(inEntry);

        Assert.assertEquals(OrbitType.FIT, file.getOrbitType());
        Assert.assertEquals(TimeSystem.GPS, file.getTimeSystem());

        Assert.assertEquals(25, file.getSatelliteCount());

        List<SatelliteTimeCoordinate> coords = file
                .getSatelliteCoordinates("1");
        Assert.assertEquals(3, coords.size());

        SatelliteTimeCoordinate coord = coords.get(0);

        // 1994 12 17 0 0 0.00000000
        Assert.assertEquals(new AbsoluteDate(1994, 12, 17, 0, 0, 0,
                TimeScalesFactory.getGPS()), coord.getEpoch());

        // P 1 16258.524750 -3529.015750 -20611.427050 -62.540600
        // V  1  -6560.373522  25605.954994  -9460.427179     -0.024236
        // TODO: check the P/V entries
    }

    @Test
    public void testParseSP3c1() throws OrekitException {
        // simple test for version sp3-c, contains p/v entries
        String ex = "sp3_c_example1.txt";

        SP3Parser parser = new SP3Parser();
        InputStream inEntry = getClass().getResourceAsStream("/sp3/" + ex);
        SP3File file = parser.parse(inEntry);

        Assert.assertEquals(OrbitType.HLM, file.getOrbitType());
        Assert.assertEquals(TimeSystem.GPS, file.getTimeSystem());

        Assert.assertEquals(26, file.getSatelliteCount());

        List<SatelliteTimeCoordinate> coords = file
                .getSatelliteCoordinates("G01");
        Assert.assertEquals(2, coords.size());

        SatelliteTimeCoordinate coord = coords.get(0);

        // 2001  8  8  0  0  0.00000000
        Assert.assertEquals(new AbsoluteDate(2001, 8, 8, 0, 0, 0,
                TimeScalesFactory.getGPS()), coord.getEpoch());

        // TODO: check the P/V entries
    }

    @Before
    public void setUp() throws OrekitException {
        Utils.setDataRoot("regular-data");
    }
}
