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

import org.orekit.attitudes.Attitude;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.ccsds.ndm.adm.aem.AEMMetadataKey;
import org.orekit.files.ccsds.utils.CCSDSBodyFrame;
import org.orekit.files.ccsds.utils.CCSDSFrame;

/**
 * Container for a pair of frames acting as attitude end points.
 * <p>
 * There are two different perspectives implemented by this class.
 * </p>
 * <p>
 * In the Orekit perspective, one of the frame is an {@link CCSDSFrame external
 * frame} suitable for reference for Orekit {@link Attitude}, the other one
 * is a {@link CCSDSBodyFrame local spaceraft body frame}. The suitable
 * setters to be used in this perspective are {@link #setExternalFrame(CCSDSFrame)},
 * {@link #setLocalFrame(CCSDSBodyFrame)} and {@link #setExternal2Local(boolean)}.
 * </p>
 * <p>
 * In the CCSDS perspective, the frames are simply labeled as 'A' and 'B', there
 * are no conditions on which frame is which and on the direction of the attitude.
 * The suitable setters to be used in this perspective are {@link #setFrameA(String)},
 * {@link #setFrameB(String)} and {@link #setDirection(String)}.
 * </p>
 * <p>
 * When populating an instance, it is recommended to use only one perspective
 * and therefore to use consistent setters. Mixing setters from the two different
 * perspectives may lead to inconsistent results.
 * </p>
 * @author Luc Maisonobe
 * @since 11.0
 */
public class AttitudeEndPoints {

    /** External frame. */
    private CCSDSFrame externalFrame;

    /** Local spacecraft body frame. */
    private CCSDSBodyFrame localFrame;

    /** Indicator for frame A. */
    private boolean frameAIsExternal;

    /** Indicator for attitude direction in CCSDS ADM file. */
    private Boolean external2Local;

    /** Complain if a field is null.
     * @param field field to check
     * @param key key associated with the field
     */
    private void checkNotNull(final Object field, final Enum<?> key) {
        if (field == null) {
            throw new OrekitException(OrekitMessages.UNINITIALIZED_VALUE_FOR_KEY, key.name());
        }
    }

    /** Check is all mandatory entries have been initialized.
     * <p>
     * This method should throw an exception if some mandatory entry is missing
     * </p>
     */
    public void checkMandatoryEntries() {
        checkNotNull(externalFrame, isLocalSuffix('B') ? AEMMetadataKey.REF_FRAME_A : AEMMetadataKey.REF_FRAME_B);
        checkNotNull(localFrame,    isLocalSuffix('A') ? AEMMetadataKey.REF_FRAME_A : AEMMetadataKey.REF_FRAME_B);
        checkNotNull(isExternal2Local(), AEMMetadataKey.ATTITUDE_DIR);
    }

    /** Set the external frame.
     * @param externalFrame external frame suitable for reference for Orekit {@link Attitude}
     */
    public void setExternalFrame(final CCSDSFrame externalFrame) {
        this.externalFrame = externalFrame;
    }

    /** Get the external frame.
     * @return external frame suitable for reference for Orekit {@link Attitude}
     */
    public CCSDSFrame getExternalFrame() {
        return externalFrame;
    }

    /** Set the local spacecraft body frame.
     * @param localFrame local spacecraft body frame
     */
    public void setLocalFrame(final CCSDSBodyFrame localFrame) {
        this.localFrame = localFrame;
    }

    /** Get the local spacecraft body frame.
     * @return local spacecraft body frame
     */
    public CCSDSBodyFrame getLocalFrame() {
        return localFrame;
    }

    /** Set reference frame A.
     * @param name name of reference frame A, as a normalized string
     */
    public void setFrameA(final String name) {
        frameAIsExternal = setFrame(name);
    }

    /** Set reference frame B.
     * @param name name of reference frame B, as a normalized string
     */
    public void setFrameB(final String name) {
        setFrame(name);
    }

    /** Check if a suffix corresponds to local frame.
     * @param suffix suffix to check
     * @return true if suffix corresponds to local frame, null if direction not set
     */
    public boolean isLocalSuffix(final char suffix) {
        return frameAIsExternal ^ (suffix == 'A');
    }

    /** Set attitude direction.
     * @param direction attitude direction (from A to B or from B to A)
     */
    public void setDirection(final String direction) {
        setExternal2Local(isLocalSuffix(direction.charAt(direction.length() - 1)));
    }

    /** Set if attitude is from external frame to local frame.
     * @param external2Local if true, attitude is from external frame to local frame
     */
    public void setExternal2Local(final boolean external2Local) {
        this.external2Local = external2Local;
    }

    /** Check if attitude is from external frame to local frame.
     * @return true if attitude is from external frame to local frame
     */
    public boolean isExternal2Local() {
        return external2Local;
    }

    /** Set a frame.
     * @param name frame name
     * @return true if frame is an external frame
     */
    private boolean setFrame(final String name) {
        try {
            setExternalFrame(CCSDSFrame.parse(name));
            return true;
        } catch (IllegalArgumentException iaeE) {
            setLocalFrame(CCSDSBodyFrame.parse(name));
            return false;
        }
    }

}
