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

package org.orekit.files.sinex;

import org.orekit.gnss.SatelliteSystem;

/**
 * Class based on DCB, used to store the data parsed in {@link SinexLoader}
 * for Differential Code Biases computed for satellites.
 * <p>
 * Satellites and stations have differentiated classes as stations might have multiple satellite systems.
 * The data are stored in a single DCB object.
 * </p>
 * @author Louis Aucouturier
 * @since 12.0
 */
public class DcbSatellite {

    /** Satellite PRN identifier.
     * <p>
     * Satellite PRN and station id are present in order to allow stations to be associated with
     * a satellite system stored in the PRN, as done in the Sinex file.
     * </p>
     */
    private String prn;

    /** DCB description container. */
    private DcbDescription description;

    /** DCB solution data. */
    private Dcb dcb;

    /**
     * Constructor for the DCBSatellite class.
     *
     * @param prn satellite PRN identifier
     */
    public DcbSatellite(final String prn) {
        this.prn         = prn;
        this.description = null;
        this.dcb         = new Dcb();
    }

    /**
     * Get the data contained in "DCB/DESCRIPTION" block of the Sinex file.
     * <p>
     * This block gives important parameters from the analysis and defines
     * the fields in the block ’BIAS/SOLUTION’
     * </p>
     * @return the "DCB/DESCRIPTION" parameters.
     */
    public DcbDescription getDescription() {
        return description;
    }

    /**
     * Set the data contained in "DCB/DESCRIPTION" block of the Sinex file.
     *
     * @param description the "DCB/DESCRIPTION" parameters to set
     */
    public void setDescription(final DcbDescription description) {
        this.description = description;
    }

    /**
     * Get the DCB data for the current satellite.
     *
     * @return the DCB data for the current satellite
     */
    public Dcb getDcbData() {
        return dcb;
    }

    /**
     * Return the satellite PRN, as a String.
     * <p>
     * Example of satellite PRN: "G01"
     * </p>
     * @return the satellite PRN
     */
    public String getPRN() {
        return prn;
    }

    /**
     * Get the satellite sytem corresponding to the satellite.
     * <p>
     * Satellite system is extracted from the first letter of the PRN.
     * </p>
     * @return the satellite from which the DCB are extracted.
     */
    public SatelliteSystem getSatelliteSytem() {
        return SatelliteSystem.parseSatelliteSystem(prn);
    }

}
