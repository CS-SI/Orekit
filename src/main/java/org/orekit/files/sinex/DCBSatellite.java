/* Copyright 2002-2022 CS GROUP
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

public class DCBSatellite {

    /** Satellite PRN identifier.
     * Satellite PRN and station id are present in order to allow stations to be associated with
     * a satellite system stored in the PRN, as done in the Sinex file.
     */
    private String satPRN;

    /** */
    private DCBDescription dcbDescription;

    /** */
    private DCB dcbSat;

    /**
     * @param satPRN
     */
    public DCBSatellite(final String satPRN) {
        this.dcbDescription = null;
        this.satPRN = satPRN;
        this.dcbSat = new DCB();
    }


    /**
     * Get the DCB Description data stored in the DCB Satellite object.
     * @return dcbDescription : DCBDescription object.
     */
    public DCBDescription getDcbDescription() {
        return dcbDescription;
    }

    /**
     * Set the DCB description variable stored in the object.
     *
     * @param dcbDesc
     */
    public void setDCBDescription(final DCBDescription dcbDesc) {
        this.dcbDescription = dcbDesc;
    }

    /**
     * Get the DCB object for this particular satellite.
     *
     * @return dcb : DCB object.
     */
    public DCB getDcbObject() {
        return dcbSat;
    }

    /**
     * Return the satellite PRN, as a String.
     *
     * @return String object corresponding to the PRN identifier of the satellite.
     */
    public String getPRN() {
        return satPRN;
    }

    /**
     * Return the SatelliteSytem object corresponding to the satellite.
     *
     * @return SatelliteSystem object corresponding to the satellite
     * from which the DCB are extracted.
     */
    public SatelliteSystem getSatelliteSytem() {
        return SatelliteSystem.parseSatelliteSystem(satPRN);
    }
}
