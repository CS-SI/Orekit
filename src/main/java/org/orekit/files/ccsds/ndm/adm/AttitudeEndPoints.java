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
import org.orekit.files.ccsds.utils.CCSDSBodyFrame;
import org.orekit.files.ccsds.utils.CCSDSFrame;

/**
 * Container for a pair of frames acting as attitude end points.
 * <p>
 * One of the frame is an {@link CCSDSFrame external frame} suitable for
 * reference for Orekit {@link Attitude}, the other one is a {@link
 * CCSDSBodyFrame local spaceraft body frame}.
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
    private boolean external2Local;

    /** Get the external frame.
     * @return external frame suitable for reference for Orekit {@link Attitude}
     */
    public CCSDSFrame getExternalFrame() {
        return externalFrame;
    }

    /** Get the local spacecraft body frame.
     * @return local spacecraft body frame
     */
    public CCSDSBodyFrame getLocalFrame() {
        return localFrame;
    }

    /** Set reference frame A.
     * @param name name of reference frame A
     */
    public void setFrameA(final String name) {
        frameAIsExternal = setFrame(name);
    }

    /** Set reference frame B.
     * @param name name of reference frame B
     */
    public void setFrameB(final String name) {
        setFrame(name);
    }

    /** Check if a suffix corresponds to local frame.
     * @param suffix suffix to check
     * @return true if suffix corresponds to local frame
     */
    public boolean isLocalSuffix(final char suffix) {
        return frameAIsExternal ^ (suffix == 'A');
    }

    /** Set attitude direction.
     * @param direction attitude direction (from A to B or from B to A)
     */
    public void setDirection(final String direction) {
        external2Local = isLocalSuffix(direction.charAt(direction.length() - 1));
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
            externalFrame = CCSDSFrame.parse(name);
            return true;
        } catch (IllegalArgumentException iaeE) {
            localFrame    = CCSDSBodyFrame.parse(name);
            return false;
        }
    }

}
