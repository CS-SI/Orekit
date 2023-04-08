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
package org.orekit.files.ccsds.ndm.adm;

import org.orekit.bodies.CelestialBodies;
import org.orekit.bodies.CelestialBody;
import org.orekit.files.ccsds.definitions.BodyFacade;
import org.orekit.files.ccsds.section.Metadata;

/** This class gathers the meta-data present in the Attitude Data Message (ADM).
 * @author Bryan Cazabonne
 * @since 10.2
 */
public class AdmMetadata extends Metadata {

    /** Spacecraft name for which the attitude data are provided. */
    private String objectName;

    /** Object identifier of the object for which the attitude data are provided. */
    private String objectID;

    /** Body at origin of reference frame. */
    private BodyFacade center;

    /** Simple constructor.
     */
    public AdmMetadata() {
        super(null);
    }

    /** {@inheritDoc} */
    @Override
    public void validate(final double version) {
        super.validate(version);
        checkNotNull(objectName, AdmMetadataKey.OBJECT_NAME.name());
        checkNotNull(objectID,   AdmCommonMetadataKey.OBJECT_ID.name());
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
        refuseFurtherComments();
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
        refuseFurtherComments();
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

    /** Get the body at origin of reference frame.
     * @return the body at origin of reference frame.
     */
    public BodyFacade getCenter() {
        return center;
    }

    /** Set the body at origin of reference frame.
     * @param center body at origin of reference frame
     */
    public void setCenter(final BodyFacade center) {
        refuseFurtherComments();
        this.center = center;
    }

    /**
     * Get boolean testing whether the body corresponding to the centerName
     * attribute can be created through the {@link CelestialBodies}.
     * @return true if {@link CelestialBody} can be created from centerName
     *         false otherwise
     */
    public boolean getHasCreatableBody() {
        return center != null && center.getBody() != null;
    }

}
