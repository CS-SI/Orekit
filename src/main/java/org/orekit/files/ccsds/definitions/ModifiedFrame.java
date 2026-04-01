/* Contributed in the public domain.
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
package org.orekit.files.ccsds.definitions;

import org.orekit.bodies.CelestialBody;
import org.orekit.frames.AngularTransformProvider;
import org.orekit.frames.Frame;

/**
 * A reference frame created from the {@code REF_FRAME} and {@code CENTER_NAME} is a CCSDS
 * OPM, OMM, or OEM file.
 *
 * @author Evan Ward
 */
public class ModifiedFrame extends Frame {

    /** Reference frame used to create this frame. */
    private final CelestialBodyFrame refFrame;

    /** Value of the CENTER_NAME keyword in the ODM file used to create this frame. */
    private final String centerName;

    /**
     * Create a CCSDS reference frame that is centered on {@code body} and
     * aligned with {@code frame}.
     *
     * <p>Callers should check that the requested frame is not already stored
     * somewhere else. For example, Earth-centered ICRF is
     * {@code Frames.getGCRF()}. {@link OrekitCcsdsFrameMapper} performs the
     * checking for CCSDS frames that Orekit implements.
     *
     * @param frame      the existing frame that specifies the orientation.
     * @param refFrame   the reference frame used to create {@code frame}.
     * @param body       the origin.
     * @param centerName the value of the {@code CENTER_NAME} key word used to
     *                   create {@code body}.
     * @see #ModifiedFrame(Frame, CelestialBodyFrame, CelestialBody, String,
     * String)
     */
    public ModifiedFrame(final Frame frame,
                         final CelestialBodyFrame refFrame,
                         final CelestialBody body,
                         final String centerName) {
        this(frame, refFrame, body, centerName,
                body.getName() + "/" + frame.getName());
    }

    /**
     * Create a CCSDS reference frame that is centered on {@code body} and
     * aligned with {@code frame}.
     *
     * <p>Callers should check that the requested frame is not already stored
     * somewhere else. For example, Earth-centered ICRF is
     * {@code Frames.getGCRF()}. {@link OrekitCcsdsFrameMapper} performs the
     * checking for CCSDS frames that Orekit implements.
     *
     * @param frame      the existing frame that specifies the orientation.
     * @param refFrame   the reference frame used to create {@code frame}.
     * @param body       the origin.
     * @param centerName the value of the {@code CENTER_NAME} key word used to
     *                   create {@code body}.
     * @param frameName  the name of this frame, returned by
     *                   {@link #getName()}.
     * @see #ModifiedFrame(Frame, CelestialBodyFrame, CelestialBody, String)
     * @since 14.0
     */
    public ModifiedFrame(final Frame frame,
                         final CelestialBodyFrame refFrame,
                         final CelestialBody body,
                         final String centerName,
                         final String frameName) {
        // unit tests reveal there is a tradeoff in translation vs. rotation
        // error, but that the error is often lower when translation is
        // performed first, then rotation.
        super(body.getIcrfAlignedFrame(),
                new AngularTransformProvider(body.getIcrfAlignedFrame(), frame),
                frameName,
                frame.isPseudoInertial());
        this.refFrame = refFrame;
        this.centerName = centerName;
    }

    /**
     * Get the CCSDS reference frame.
     *
     * @return the reference frame used to create this frame.
     */
    public CelestialBodyFrame getRefFrame() {
        return refFrame;
    }

    /**
     * Get the CCSDS center name.
     *
     * @return the value of the {@code CENTER_NAME} key word used to specify the origin of
     * this frame.
     */
    public String getCenterName() {
        return centerName;
    }

}
