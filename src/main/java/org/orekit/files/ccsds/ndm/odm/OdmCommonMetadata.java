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

package org.orekit.files.ccsds.ndm.odm;

import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.ccsds.definitions.BodyFacade;
import org.orekit.files.ccsds.definitions.CelestialBodyFrame;
import org.orekit.files.ccsds.definitions.FrameFacade;
import org.orekit.files.ccsds.definitions.ModifiedFrame;
import org.orekit.files.ccsds.utils.ContextBinding;
import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;

/** Common metadata for Orbit Parameter/Ephemeris/Mean Messages.
 * @author Luc Maisonobe
 * @since 11.0
 */
public class OdmCommonMetadata extends OdmMetadata {

    /** Object identifier of the object for which the orbit state is provided. */
    private String objectID;

    /** Origin of reference frame. */
    private BodyFacade center;

    /** Reference frame in which data are given: used for state vector
     * and Keplerian elements data (and for the covariance reference frame if none is given). */
    private FrameFacade referenceFrame;

    /** Epoch of reference frame, if not intrinsic to the definition of the
     * reference frame. */
    private String frameEpochString;

    /** Epoch of reference frame, if not intrinsic to the definition of the
     * reference frame. */
    private AbsoluteDate frameEpoch;

    /** Simple constructor.
     */
    public OdmCommonMetadata() {
        super(null);
    }

    /** {@inheritDoc} */
    @Override
    public void validate(final double version) {
        super.validate(version);
        checkNotNull(getObjectName(), OdmMetadataKey.OBJECT_NAME.name());
        checkNotNull(objectID,        CommonMetadataKey.OBJECT_ID.name());
        checkNotNull(center,          CommonMetadataKey.CENTER_NAME.name());
        checkNotNull(referenceFrame,  CommonMetadataKey.REF_FRAME.name());
    }

    /** Finalize the metadata.
     * <p>
     * ODM standard enforces {@code TIME_SYSTEM} to appear *after*
     * {@code REF_FRAME_EPOCH}, despite it is needed to interpret it.
     * We have to wait until parsing end to finalize this date.
     * </p>
     * @param context context binding
     */
    public void finalizeMetadata(final ContextBinding context) {
        if (frameEpochString != null) {
            frameEpoch = context.getTimeSystem().getConverter(context).parse(frameEpochString);
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
    public BodyFacade getCenter() {
        return center;
    }

    /** Set the origin of reference frame.
     * @param center origin of reference frame to be set
     */
    public void setCenter(final BodyFacade center) {
        refuseFurtherComments();
        this.center = center;
    }

    /**
     * Get the reference frame in which data are given: used for state vector and
     * Keplerian elements data (and for the covariance reference frame if none is given).
     *
     * @return the reference frame
     */
    public Frame getFrame() {
        if (center.getBody() == null) {
            throw new OrekitException(OrekitMessages.NO_DATA_LOADED_FOR_CELESTIAL_BODY, center.getName());
        }
        if (referenceFrame.asFrame() == null) {
            throw new OrekitException(OrekitMessages.CCSDS_INVALID_FRAME, referenceFrame.getName());
        }
        // Just return frame if we don't need to shift the center based on CENTER_NAME
        // MCI and ICRF are the only non-Earth centered frames specified in Annex A.
        final boolean isMci  = referenceFrame.asCelestialBodyFrame() == CelestialBodyFrame.MCI;
        final boolean isIcrf = referenceFrame.asCelestialBodyFrame() == CelestialBodyFrame.ICRF;
        final boolean isSolarSystemBarycenter =
                CelestialBodyFactory.SOLAR_SYSTEM_BARYCENTER.equals(center.getBody().getName());
        if (!(isMci || isIcrf) && CelestialBodyFactory.EARTH.equals(center.getBody().getName()) ||
            isMci && CelestialBodyFactory.MARS.equals(center.getBody().getName()) ||
            isIcrf && isSolarSystemBarycenter) {
            return referenceFrame.asFrame();
        }
        // else, translate frame to specified center.
        return new ModifiedFrame(referenceFrame.asFrame(), referenceFrame.asCelestialBodyFrame(),
                                 center.getBody(), center.getName());
    }

    /**
     * Get the value of {@code REF_FRAME} as an Orekit {@link Frame}. The {@code
     * CENTER_NAME} key word has not been applied yet, so the returned frame may not
     * correspond to the reference frame of the data in the file.
     *
     * @return The reference frame specified by the {@code REF_FRAME} keyword.
     * @see #getFrame()
     */
    public FrameFacade getReferenceFrame() {
        return referenceFrame;
    }

    /** Set the reference frame in which data are given: used for state vector
     * and Keplerian elements data (and for the covariance reference frame if none is given).
     * @param referenceFrame the reference frame to be set
     */
    public void setReferenceFrame(final FrameFacade referenceFrame) {
        refuseFurtherComments();
        this.referenceFrame = referenceFrame;
    }

    /** Set epoch of reference frame, if not intrinsic to the definition of the
     * reference frame.
     * @param frameEpochString the epoch of reference frame to be set
     */
    public void setFrameEpochString(final String frameEpochString) {
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
