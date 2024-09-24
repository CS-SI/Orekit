/* Copyright 2002-2024 CS GROUP
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

package org.orekit.files.sinex;

import org.orekit.gnss.SatInSystem;

/**
 * Class based on DSB, used to store the data parsed in {@link SinexBiasParser}
 * for Differential Signal Biases computed for satellites.
 * <p>
 * Satellites and stations have differentiated classes as stations might have multiple satellite systems.
 * The data are stored in a single DSB object.
 * </p>
 * @author Louis Aucouturier
 * @since 12.0
 */
public class SatelliteDifferentialSignalBias {

    /** Satellite identifier. */
    private final SatInSystem satellite;

    /** DSB solution data. */
    private final DifferentialSignalBias dsb;

    /** Constructor for the DSBSatellite class.
     * @param satellite satellite identifier
     */
    public SatelliteDifferentialSignalBias(final SatInSystem satellite) {
        this.satellite = satellite;
        this.dsb       = new DifferentialSignalBias();
    }

    /** Return the satellite identifier.
     * @return the satellite identifier
     */
    public SatInSystem getSatellite() {
        return satellite;
    }

    /** Get the DSB data for the current satellite.
     * @return the DSB data for the current satellite
     */
    public DifferentialSignalBias getDsb() {
        return dsb;
    }

}
