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
package org.orekit.estimation.measurements.gnss;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.gnss.SatelliteSystem;

public class WindUpFactoryTest {

    @Test
    public void testDifferentFactories() {
        WindUp windUp1 = new WindUpFactory().getWindUp(SatelliteSystem.GALILEO, 1, Dipole.CANONICAL_I_J, "ABC123");
        Assertions.assertEquals(0, windUp1.getParametersDrivers().size());
        WindUp windUp2 = new WindUpFactory().getWindUp(SatelliteSystem.GALILEO, 1, Dipole.CANONICAL_I_J, "ABC123");
        Assertions.assertEquals(0, windUp2.getParametersDrivers().size());
        Assertions.assertNotSame(windUp1, windUp2);
    }

    @Test
    public void testSameFactory() {
        WindUpFactory factory = new WindUpFactory();
        WindUp windUp1 = factory.getWindUp(SatelliteSystem.GALILEO, 1, Dipole.CANONICAL_I_J, "ABC123");
        Assertions.assertEquals(0, windUp1.getParametersDrivers().size());
        WindUp windUp2 = factory.getWindUp(SatelliteSystem.GALILEO, 1, Dipole.CANONICAL_I_J, "ABC123");
        Assertions.assertEquals(0, windUp2.getParametersDrivers().size());
        Assertions.assertSame(windUp1, windUp2);
    }

    @Test
    public void testCachedInstances() {

        WindUpFactory factory = new WindUpFactory();
        WindUp[] windUp1 = {
            factory.getWindUp(SatelliteSystem.GALILEO, 1, Dipole.CANONICAL_I_J, "ABC123"),
            factory.getWindUp(SatelliteSystem.GLONASS, 1, Dipole.CANONICAL_I_J, "ABC123"),
            factory.getWindUp(SatelliteSystem.GALILEO, 2, Dipole.CANONICAL_I_J, "ABC123"),
            factory.getWindUp(SatelliteSystem.GLONASS, 2, Dipole.CANONICAL_I_J, "ABC123"),
            factory.getWindUp(SatelliteSystem.GALILEO, 1, Dipole.CANONICAL_I_J, "XYZ789"),
            factory.getWindUp(SatelliteSystem.GLONASS, 1, Dipole.CANONICAL_I_J, "XYZ789"),
            factory.getWindUp(SatelliteSystem.GALILEO, 2, Dipole.CANONICAL_I_J, "XYZ789"),
            factory.getWindUp(SatelliteSystem.GLONASS, 2, Dipole.CANONICAL_I_J, "XYZ789")
        };
        WindUp[] windUp2 = {
            factory.getWindUp(SatelliteSystem.GALILEO, 1, Dipole.CANONICAL_I_J, "ABC123"),
            factory.getWindUp(SatelliteSystem.GLONASS, 1, Dipole.CANONICAL_I_J, "ABC123"),
            factory.getWindUp(SatelliteSystem.GALILEO, 2, Dipole.CANONICAL_I_J, "ABC123"),
            factory.getWindUp(SatelliteSystem.GLONASS, 2, Dipole.CANONICAL_I_J, "ABC123"),
            factory.getWindUp(SatelliteSystem.GALILEO, 1, Dipole.CANONICAL_I_J, "XYZ789"),
            factory.getWindUp(SatelliteSystem.GLONASS, 1, Dipole.CANONICAL_I_J, "XYZ789"),
            factory.getWindUp(SatelliteSystem.GALILEO, 2, Dipole.CANONICAL_I_J, "XYZ789"),
            factory.getWindUp(SatelliteSystem.GLONASS, 2, Dipole.CANONICAL_I_J, "XYZ789")
        };

        for (int i = 0; i < windUp1.length; ++i) {
            for (int j = 0; j < windUp1.length; ++j) {
                if (i != j) {
                    Assertions.assertNotSame(windUp1[i], windUp1[j]);
                    Assertions.assertNotSame(windUp2[i], windUp2[j]);
                }
            }
            Assertions.assertSame(windUp1[i], windUp2[i]);
        }

    }

}
