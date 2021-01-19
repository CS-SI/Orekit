/* Copyright 2002-2021 CS GROUP
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
package org.orekit.files.ccsds.ndm.adm;

import org.orekit.bodies.CelestialBodies;
import org.orekit.bodies.CelestialBody;
import org.orekit.data.DataContext;
import org.orekit.files.ccsds.ndm.NDMMetadata;
import org.orekit.utils.IERSConventions;

/** This class gathers the meta-data present in the Attitude Data Message (ADM).
 * @author Bryan Cazabonne
 * @since 10.2
 */
public class ADMMetadata extends NDMMetadata {

    /** Spacecraft name for which the attitude data are provided. */
    private String objectName;

    /** Object identifier of the object for which the attitude data are provided. */
    private String objectID;

    /** Origin of reference frame. */
    private String centerName;

    /** Celestial body corresponding to the center name. */
    private CelestialBody centerBody;

    /** Tests whether the body corresponding to the center name can be
     * created through {@link CelestialBodies} in order to obtain theO
     * corresponding gravitational coefficient. */
    private boolean hasCreatableBody;

    /** Create a new meta-data.
     * @param conventions IERS conventions to use
     * @param simpleEOP if true, tidal effects are ignored when interpolating EOP
     * @param dataContext data context to use
     */
    public ADMMetadata(final IERSConventions conventions, final boolean simpleEOP, final DataContext dataContext) {
        super(conventions, simpleEOP, dataContext);
    }

    /**
     * Get the spacecraft name for which the attitude data are provided.
     * @return the spacecraft name
     */
    public String getObjectName() {
        return objectName;
    }

    /**
     * Set the spacecraft name for which the attitude data are provided.
     * @param objectName the spacecraft name to be set
     */
    public void setObjectName(final String objectName) {
        this.objectName = objectName;
    }

    /**
     * Get the spacecraft ID for which the attitude data are provided.
     * @return the spacecraft ID
     */
    public String getObjectID() {
        return objectID;
    }

    /**
     * Set the spacecraft ID for which the attitude data are provided.
     * @param objectID the spacecraft ID to be set
     */
    public void setObjectID(final String objectID) {
        this.objectID = objectID;
    }

    /** Get the launch year.
     * @return launch year
     */
    public int getLaunchYear() {
        return getLaunchYear(objectID);
    }

    /** Get the launch number.
     * @return launch number
     */
    public int getLaunchNumber() {
        return getLaunchNumber(objectID);
    }

    /** Get the piece of launch.
     * @return piece of launch
     */
    public String getLaunchPiece() {
        return getLaunchPiece(objectID);
    }

    /** Get the origin of reference frame.
     * @return the origin of reference frame.
     */
    public String getCenterName() {
        return centerName;
    }

    /** Set the origin of reference frame.
     * @param centerName the origin of reference frame to be set
     */
    public void setCenterName(final String centerName) {
        this.centerName = centerName;
    }

    /**
     * Get the {@link CelestialBody} corresponding to the center name.
     * @return the center body
     */
    public CelestialBody getCenterBody() {
        return centerBody;
    }

    /**
     * Set the {@link CelestialBody} corresponding to the center name.
     * @param centerBody the {@link CelestialBody} to be set
     */
    public void setCenterBody(final CelestialBody centerBody) {
        this.centerBody = centerBody;
    }

    /**
     * Get boolean testing whether the body corresponding to the centerName
     * attribute can be created through the {@link CelestialBodies}.
     * @return true if {@link CelestialBody} can be created from centerName
     *         false otherwise
     */
    public boolean getHasCreatableBody() {
        return hasCreatableBody;
    }

    /**
     * Set boolean testing whether the body corresponding to the centerName
     * attribute can be created through the {@link CelestialBodies}.
     * @param hasCreatableBody the boolean to be set.
     */
    public void setHasCreatableBody(final boolean hasCreatableBody) {
        this.hasCreatableBody = hasCreatableBody;
    }

}
