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

package org.orekit.files.ccsds.ndm.odm;

import org.orekit.bodies.CelestialBodies;
import org.orekit.bodies.CelestialBody;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.ccsds.utils.CcsdsFrame;
import org.orekit.files.ccsds.utils.CcsdsModifiedFrame;
import org.orekit.files.ccsds.utils.CenterName;
import org.orekit.files.ccsds.utils.ParsingContext;
import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;

/** Common metadata for Orbit Parameter/Ephemeris/Mean Message files.
 * @author Luc Maisonobe
 * @since 11.0
 */
public class CommonMetadata extends OdmMetadata {

    /** Object identifier of the object for which the orbit state is provided. */
    private String objectID;

    /** Origin of reference frame. */
    private String centerName;

    /** Celestial body corresponding to the center name. */
    private CelestialBody centerBody;

    /** Reference frame in which data are given: used for state vector
     * and Keplerian elements data (and for the covariance reference frame if none is given). */
    private Frame referenceFrame;

    /** Reference frame in which data are given: used for state vector
     * and Keplerian elements data (and for the covariance reference frame if none is given). */
    private CcsdsFrame referenceCCSDSFrame;

    /** Epoch of reference frame, if not intrinsic to the definition of the
     * reference frame. */
    private String frameEpochString;

    /** Epoch of reference frame, if not intrinsic to the definition of the
     * reference frame. */
    private AbsoluteDate frameEpoch;

    /** Simple constructor.
     */
    public CommonMetadata() {
        super(null);
    }

    /** {@inheritDoc} */
    @Override
    public void checkMandatoryEntries() {
        super.checkMandatoryEntries();
        checkNotNull(objectID,   CommonMetadataKey.OBJECT_ID);
        checkNotNull(centerName, CommonMetadataKey.CENTER_NAME);
        checkNotNull(referenceFrame,   CommonMetadataKey.REF_FRAME);
    }

    /** Finalize the metadata.
     * <p>
     * ODM standard enforces {@code TIME_SYSTEM} to appear *after*
     * {@code REF_FRAME_EPOCH}, despite it is needed to interpret it.
     * We have to wait until parsing end to finalize this date.
     * <p>
     * @param context parsing context
     */
    public void finalizeMetadata(final ParsingContext context) {
        if (frameEpochString != null) {
            frameEpoch = context.getTimeScale().parseDate(frameEpochString,
                                                          context.getConventions(),
                                                          context.getMissionReferenceDate(),
                                                          context.getDataContext().getTimeScales());
        }
    }

    /** Get the spacecraft ID for which the orbit state is provided.
     * @return the spacecraft ID
     */
    public String getObjectID() {
        return objectID;
    }

    /** Set the spacecraft ID for which the orbit state is provided.
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

    /** Get the origin of reference frame.
     * @return the origin of reference frame.
     */
    public String getCenterName() {
        return centerName;
    }

    /** Set the origin of reference frame.
     * @param name the origin of reference frame to be set
     * @param celestialBodies factory for celestial bodies
     */
    public void setCenterName(final String name, final CelestialBodies celestialBodies) {

        refuseFurtherComments();

        // store the name itself
        this.centerName = name;

        // change the name to a canonical one in some cases
        final String canonicalValue;
        if ("SOLAR SYSTEM BARYCENTER".equals(centerName) || "SSB".equals(centerName)) {
            canonicalValue = "SOLAR_SYSTEM_BARYCENTER";
        } else if ("EARTH MOON BARYCENTER".equals(centerName) || "EARTH-MOON BARYCENTER".equals(centerName) ||
                   "EARTH BARYCENTER".equals(centerName) || "EMB".equals(centerName)) {
            canonicalValue = "EARTH_MOON";
        } else {
            canonicalValue = centerName;
        }

        final CenterName c;
        try {
            c = CenterName.valueOf(canonicalValue);
        } catch (IllegalArgumentException iae) {
            centerBody = null;
            return;
        }

        centerBody = c.getCelestialBody(celestialBodies);

    }

    /** Get the {@link CelestialBody} corresponding to the center name.
     * @return the center body
     */
    public CelestialBody getCenterBody() {
        return centerBody;
    }

    /**
     * Get the reference frame in which data are given: used for state vector and
     * Keplerian elements data (and for the covariance reference frame if none is given).
     *
     * @return the reference frame
     */
    public Frame getFrame() {
        if (centerBody == null) {
            throw new OrekitException(OrekitMessages.NO_DATA_LOADED_FOR_CELESTIAL_BODY, centerName);
        }
        // Just return frame if we don't need to shift the center based on CENTER_NAME
        // MCI and ICRF are the only non-Earth centered frames specified in Annex A.
        final boolean isMci  = referenceCCSDSFrame == CcsdsFrame.MCI;
        final boolean isIcrf = referenceCCSDSFrame == CcsdsFrame.ICRF;
        final boolean isSolarSystemBarycenter =
                CelestialBodyFactory.SOLAR_SYSTEM_BARYCENTER.equals(centerBody.getName());
        if ((!(isMci || isIcrf) && CelestialBodyFactory.EARTH.equals(centerBody.getName())) ||
            (isMci && CelestialBodyFactory.MARS.equals(centerBody.getName())) ||
            (isIcrf && isSolarSystemBarycenter)) {
            return referenceFrame;
        }
        // else, translate frame to specified center.
        return new CcsdsModifiedFrame(referenceFrame, referenceCCSDSFrame, centerBody, centerName);
    }

    /**
     * Get the value of {@code REF_FRAME} as an Orekit {@link Frame}. The {@code
     * CENTER_NAME} key word has not been applied yet, so the returned frame may not
     * correspond to the reference frame of the data in the file.
     *
     * @return The reference frame specified by the {@code REF_FRAME} keyword.
     * @see #getFrame()
     */
    public Frame getRefFrame() {
        return referenceFrame;
    }

    /**
     * Get the value of {@code REF_FRAME} as an Orekit {@link Frame}. The {@code
     * CENTER_NAME} key word has not been applied yet, so the returned frame may not
     * correspond to the reference frame of the data in the file.
     *
     * @return The reference frame specified by the {@code REF_FRAME} keyword.
     * @see #getFrame()
     */
    public CcsdsFrame getRefCCSDSFrame() {
        return referenceCCSDSFrame;
    }

    /** Set the reference frame in which data are given: used for state vector
     * and Keplerian elements data (and for the covariance reference frame if none is given).
     * @param frame the reference frame to be set
     * @param ccsdsFrame the reference frame to be set
     */
    public void setRefFrame(final Frame frame, final CcsdsFrame ccsdsFrame) {
        refuseFurtherComments();
        this.referenceFrame      = frame;
        this.referenceCCSDSFrame = ccsdsFrame;
    }

    /** Get epoch of reference frame, if not intrinsic to the definition of the
     * reference frame.
     * @return epoch of reference frame
     */
    String getFrameEpochString() {
        return frameEpochString;
    }

    /** Set epoch of reference frame, if not intrinsic to the definition of the
     * reference frame.
     * @param frameEpochString the epoch of reference frame to be set
     */
    void setFrameEpochString(final String frameEpochString) {
        refuseFurtherComments();
        this.frameEpochString = frameEpochString;
    }

    /** Get epoch of reference frame, if not intrinsic to the definition of the
     * reference frame.
     * @return epoch of reference frame
     */
    public AbsoluteDate getFrameEpoch() {
        return frameEpoch;
    }

    /** Set epoch of reference frame, if not intrinsic to the definition of the
     * reference frame.
     * @param frameEpoch the epoch of reference frame to be set
     */
    public void setFrameEpoch(final AbsoluteDate frameEpoch) {
        refuseFurtherComments();
        this.frameEpoch = frameEpoch;
    }

}
