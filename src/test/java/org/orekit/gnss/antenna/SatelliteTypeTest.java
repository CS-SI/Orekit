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
package org.orekit.gnss.antenna;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.frames.FramesFactory;
import org.orekit.gnss.attitude.BeidouGeo;
import org.orekit.gnss.attitude.BeidouIGSO;
import org.orekit.gnss.attitude.BeidouMeo;
import org.orekit.gnss.attitude.GPSBlockIIA;
import org.orekit.gnss.attitude.GPSBlockIIF;
import org.orekit.gnss.attitude.GPSBlockIIR;
import org.orekit.gnss.attitude.Galileo;
import org.orekit.gnss.attitude.GenericGNSS;
import org.orekit.gnss.attitude.Glonass;
import org.orekit.time.AbsoluteDate;


public class SatelliteTypeTest {

    @Before
    public void setUp() {
        // Sets the root of data to read
        Utils.setDataRoot("regular-data");
    }

    @Test
    public void testAttitudeTypes() {
        for (final SatelliteType type : SatelliteType.values()) {
            AttitudeProvider provider = type.buildAttitudeProvider(AbsoluteDate.J2000_EPOCH,
                                                                   AbsoluteDate.J2000_EPOCH.shiftedBy(3600),
                                                                   CelestialBodyFactory.getSun(),
                                                                   FramesFactory.getEME2000(),
                                                                   1);

            if (type == SatelliteType.BEIDOU_3I  ||
                type == SatelliteType.GALILEO_0A ||
                type == SatelliteType.GALILEO_0B ||
                type == SatelliteType.GALILEO_1  ||
                type == SatelliteType.GALILEO_2) {
                Assert.assertEquals(Galileo.class, provider.getClass());
            } else if (type == SatelliteType.BEIDOU_2G) {
                Assert.assertEquals(BeidouGeo.class, provider.getClass());
            } else if (type == SatelliteType.BEIDOU_2I) {
                Assert.assertEquals(BeidouIGSO.class, provider.getClass());
            } else if (type == SatelliteType.BEIDOU_2M) {
                Assert.assertEquals(BeidouMeo.class, provider.getClass());
            } else if (type == SatelliteType.BLOCK_I   ||
                       type == SatelliteType.BLOCK_II ||
                       type == SatelliteType.BLOCK_IIA) {
                Assert.assertEquals(GPSBlockIIA.class, provider.getClass());
            } else if (type == SatelliteType.BLOCK_IIR_A ||
                       type == SatelliteType.BLOCK_IIR_B ||
                       type == SatelliteType.BLOCK_IIR_M) {
                Assert.assertEquals(GPSBlockIIR.class, provider.getClass());
            } else if (type == SatelliteType.BLOCK_IIF ||
                       type == SatelliteType.BLOCK_IIIA) {
                Assert.assertEquals(GPSBlockIIF.class, provider.getClass());
            } else if (type == SatelliteType.GLONASS ||
                       type == SatelliteType.GLONASS_M ||
                       type == SatelliteType.GLONASS_K1 ||
                       type == SatelliteType.GLONASS_K2) {
                Assert.assertEquals(Glonass.class, provider.getClass());
            } else {
                Assert.assertEquals(GenericGNSS.class, provider.getClass());
            }
        }
    }

}
