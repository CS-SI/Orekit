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
package org.orekit.estimation.measurements.gnss;

import org.junit.Assert;
import org.junit.Test;
import org.orekit.gnss.SatelliteSystem;

public class WindUpFactoryTest {

    @Test
    public void testDifferentFactories() {
        WindUp windUp1 = new WindUpFactory().getWindUp(SatelliteSystem.GALILEO, 1, "ABC123");
        Assert.assertEquals(0, windUp1.getParametersDrivers().size());
        WindUp windUp2 = new WindUpFactory().getWindUp(SatelliteSystem.GALILEO, 1, "ABC123");
        Assert.assertEquals(0, windUp2.getParametersDrivers().size());
        Assert.assertNotSame(windUp1, windUp2);
    }

    @Test
    public void testSameFactory() {
        WindUpFactory factory = new WindUpFactory();
        WindUp windUp1 = factory.getWindUp(SatelliteSystem.GALILEO, 1, "ABC123");
        Assert.assertEquals(0, windUp1.getParametersDrivers().size());
        WindUp windUp2 = factory.getWindUp(SatelliteSystem.GALILEO, 1, "ABC123");
        Assert.assertEquals(0, windUp2.getParametersDrivers().size());
        Assert.assertSame(windUp1, windUp2);
    }

    @Test
    public void testCachedInstances() {

        WindUpFactory factory = new WindUpFactory();
        WindUp[] windUp1 = {
            factory.getWindUp(SatelliteSystem.GALILEO, 1, "ABC123"),
            factory.getWindUp(SatelliteSystem.GLONASS, 1, "ABC123"),
            factory.getWindUp(SatelliteSystem.GALILEO, 2, "ABC123"),
            factory.getWindUp(SatelliteSystem.GLONASS, 2, "ABC123"),
            factory.getWindUp(SatelliteSystem.GALILEO, 1, "XYZ789"),
            factory.getWindUp(SatelliteSystem.GLONASS, 1, "XYZ789"),
            factory.getWindUp(SatelliteSystem.GALILEO, 2, "XYZ789"),
            factory.getWindUp(SatelliteSystem.GLONASS, 2, "XYZ789")
        };
        WindUp[] windUp2 = {
            factory.getWindUp(SatelliteSystem.GALILEO, 1, "ABC123"),
            factory.getWindUp(SatelliteSystem.GLONASS, 1, "ABC123"),
            factory.getWindUp(SatelliteSystem.GALILEO, 2, "ABC123"),
            factory.getWindUp(SatelliteSystem.GLONASS, 2, "ABC123"),
            factory.getWindUp(SatelliteSystem.GALILEO, 1, "XYZ789"),
            factory.getWindUp(SatelliteSystem.GLONASS, 1, "XYZ789"),
            factory.getWindUp(SatelliteSystem.GALILEO, 2, "XYZ789"),
            factory.getWindUp(SatelliteSystem.GLONASS, 2, "XYZ789")
        };

        for (int i = 0; i < windUp1.length; ++i) {
            for (int j = 0; j < windUp1.length; ++j) {
                if (i != j) {
                    Assert.assertNotSame(windUp1[i], windUp1[j]);
                    Assert.assertNotSame(windUp2[i], windUp2[j]);
                }
            }
            Assert.assertSame(windUp1[i], windUp2[i]);
        }

    }

}
