/* Copyright 2002-2020 CS GROUP
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

package org.orekit.files.ccsds;

import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;

/**
 * The ODMFile (Orbit Data Message) class represents any of the three orbit messages used by the CCSDS,
 * i.e. the Orbit Parameter Message (OPM), the Mean-Elements Message (OMM) and the Orbit Ephemeris Message (OEM).
 * It contains the information of the message's header and configuration data (set in the parser).
 * @author sports
 * @since 6.1
 */
public abstract class ODMFile extends NDMFile {

    /** Gravitational coefficient set by the user in the parser. */
    private double muSet;

    /** Gravitational coefficient parsed in the ODM File. */
    private double muParsed;

    /** Gravitational coefficient created from the knowledge of the central body. */
    private double muCreated;

    /** Final gravitational coefficient (used for the public methods that need such a parameter, ex: generateCartesianOrbit).
     * In order of decreasing priority, finalMU is equal to: the coefficient parsed in the file, the coefficient set by the
     * user with the parser's method setMu, the coefficient created from the knowledge of the central body.
     */
    private double muUsed;

    /** ODMFile constructor. */
    public ODMFile() {
        muSet     = Double.NaN;
        muParsed  = Double.NaN;
        muCreated = Double.NaN;
        muUsed    = Double.NaN;
    }

    /**
     * Get the gravitational coefficient set by the user.
     * @return the coefficient
     */
    public double getMuSet() {
        return muSet;
    }

    /**
     * Set the gravitational coefficient set by the user.
     * @param muSet the coefficient to be set
     */
    public void setMuSet(final double muSet) {
        this.muSet = muSet;
    }

    /**
     * Get the gravitational coefficient parsed in the ODM File.
     * @return the coefficient
     */
    public double getMuParsed() {
        return muParsed;
    }

    /**
     * Set the gravitational coefficient parsed in the ODM File.
     * @param muParsed the coefficient to be set
     */
    void setMuParsed(final double muParsed) {
        this.muParsed = muParsed;
    }

    /**
     * Get the gravitational coefficient created from the knowledge of the central body.
     * @return the coefficient
     */
    public double getMuCreated() {
        return muCreated;
    }

    /**
     * Set the gravitational coefficient created from the knowledge of the central body.
     * @param muCreated the coefficient to be set
     */
    void setMuCreated(final double muCreated) {
        this.muCreated = muCreated;
    }

    /**
     * Get the used gravitational coefficient.
     * @return the coefficient
     */
    public double getMuUsed() {
        return muUsed;
    }

    /**
     * Set the gravitational coefficient created from the knowledge of the central body.
     * In order of decreasing priority, finalMU is set equal to:
     * <ol>
     *   <li>the coefficient parsed in the file,</li>
     *   <li>the coefficient set by the user with the parser's method setMu,</li>
     *   <li>the coefficient created from the knowledge of the central body.</li>
     * </ol>
     */
    protected void setMuUsed() {
        if (!Double.isNaN(muParsed)) {
            muUsed = muParsed;
        } else if (!Double.isNaN(muSet)) {
            muUsed = muSet;
        } else if (!Double.isNaN(muCreated)) {
            muUsed = muCreated;
        } else {
            throw new OrekitException(OrekitMessages.CCSDS_UNKNOWN_GM);
        }
    }

}

