/* Copyright 2002-2026 CS GROUP
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
package org.orekit.files.ilrs;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.orekit.files.ilrs.CRDConfiguration.LaserConfiguration;
import org.orekit.files.ilrs.CRDConfiguration.SystemConfiguration;

class CRDConfigurationTest {

    @Test
    @DisplayName("getLastSystemRecord returns null when no system record has been added")
    void testGetLastSystemRecordWhenEmpty() {
        // Given
        final CRDConfiguration config = new CRDConfiguration();

        // When
        final SystemConfiguration result = config.getLastSystemRecord();

        // Then
        Assertions.assertNull(result);
    }

    @Test
    @DisplayName("getLastSystemRecord returns the only record when exactly one has been added")
    void testGetLastSystemRecordWithSingleRecord() {
        // Given
        final CRDConfiguration config = new CRDConfiguration();
        final SystemConfiguration sys = new SystemConfiguration();
        sys.setSystemId("SYS-001");
        config.addConfigurationRecord(sys);

        // When
        final SystemConfiguration result = config.getLastSystemRecord();

        // Then
        Assertions.assertSame(sys, result);
    }

    @Test
    @DisplayName("getLastSystemRecord returns the last added record when multiple records exist")
    void testGetLastSystemRecordReturnsLastAdded() {
        // Given
        final CRDConfiguration config = new CRDConfiguration();
        final SystemConfiguration first = new SystemConfiguration();
        first.setSystemId("SYS-001");
        final SystemConfiguration last = new SystemConfiguration();
        last.setSystemId("SYS-002");
        config.addConfigurationRecord(first);
        config.addConfigurationRecord(last);

        // When
        final SystemConfiguration result = config.getLastSystemRecord();

        // Then
        Assertions.assertSame(last, result);
    }

    @Test
    @DisplayName("addConfigurationRecord does nothing when passed null")
    void testAddConfigurationRecordIgnoresNull() {
        // Given
        final CRDConfiguration config = new CRDConfiguration();

        // When
        config.addConfigurationRecord(null);

        // Then
        Assertions.assertTrue(config.getConfigurationRecordMap().isEmpty());
        Assertions.assertNull(config.getLastSystemRecord());
    }

    @Test
    @DisplayName("addConfigurationRecord adds a SystemConfiguration to both the map and the system list")
    void testAddConfigurationRecordDispatchesSystemConfigurationToBothCollections() {
        // Given
        final CRDConfiguration config = new CRDConfiguration();
        final SystemConfiguration sys = new SystemConfiguration();
        sys.setSystemId("SYS-A");

        // When
        config.addConfigurationRecord(sys);

        // Then
        Assertions.assertEquals(1, config.getConfigurationRecordMap().size());
        Assertions.assertSame(sys, config.getConfigurationRecordMap().get("SYS-A"));
        Assertions.assertEquals(1, config.getSystemConfigurationRecords().size());
        Assertions.assertSame(sys, config.getSystemConfigurationRecords().getFirst());
    }

    @Test
    @DisplayName("addConfigurationRecord adds a non-SystemConfiguration only to the map, not to the system list")
    void testAddConfigurationRecordDispatchesNonSystemConfigurationToMapOnly() {
        // Given
        final CRDConfiguration config = new CRDConfiguration();
        final LaserConfiguration laser = new LaserConfiguration();
        laser.setLaserId("LASER-1");

        // When
        config.addConfigurationRecord(laser);

        // Then
        Assertions.assertEquals(1, config.getConfigurationRecordMap().size());
        Assertions.assertSame(laser, config.getConfigurationRecordMap().get("LASER-1"));
        Assertions.assertTrue(config.getSystemConfigurationRecords().isEmpty());
        Assertions.assertNull(config.getLastSystemRecord());
    }
}
