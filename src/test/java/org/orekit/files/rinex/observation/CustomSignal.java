/* Copyright 2002-2024 Thales Alenia Space
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
import org.orekit.gnss.SatelliteSystem;

class CustomSignal implements GnssSignal {

    @Override
    public double getRatio() {
        return 1;
    }

    @Override
    public String getName() {
        return "U09";
    }

    @Override
    public SatelliteSystem getSatelliteSystem() {
        return SatelliteSystem.USER_DEFINED_K;
    }

    @Override
    public double getFrequency() {
        return GnssSignal.F0;
    }

}
