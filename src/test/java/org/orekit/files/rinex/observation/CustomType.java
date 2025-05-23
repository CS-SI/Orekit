/* Copyright 2022-2025 Thales Alenia Space
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
package org.orekit.files.rinex.observation;

import org.orekit.gnss.GnssSignal;
import org.orekit.gnss.MeasurementType;
import org.orekit.gnss.ObservationType;
import org.orekit.gnss.SatelliteSystem;
import org.orekit.gnss.SignalCode;

import java.util.Collections;

class CustomType implements ObservationType {

    private final String name;

    CustomType(final String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public MeasurementType getMeasurementType() {
        return name.charAt(0) == 'X' ? MeasurementType.TWO_WAY_TIME_TRANSFER : MeasurementType.CARRIER_PHASE;
    }

    @Override
    public SignalCode getSignalCode() {
        return SignalCode.CODELESS;
    }

    @Override
    public GnssSignal getSignal(final SatelliteSystem system) {
        return Collections.singletonMap(SatelliteSystem.USER_DEFINED_K, new CustomSignal()).get(system);
    }

    @Override
    public boolean equals(final Object type) {
        if (type instanceof CustomType) {
            return name.equals(((CustomType) type).name);
        }
        return false;
    }

}
