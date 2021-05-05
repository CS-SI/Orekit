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
package org.orekit.files.ccsds.definitions;

import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;

/** Frames used in CCSDS Attitude Data Messages for the spacecraft body.
 * @author Luc Maisonobe
 * @since 11.0
 */
public class SpacecraftBodyFrame {

    /** Equipment on which the frame is located. */
    public enum BaseEquipment {

        /** Accelerometer. */
        ACC,

        /** Actuator: could denote reaction wheels, solar arrays, thrusters, etc.. */
        ACTUATOR,

        /** Autonomous Star Tracker. */
        AST,

        /** Coarse Sun Sensor. */
        CSS,

        /** Digital Sun Sensor. */
        DSS,

        /** Earth Sensor Assembly. */
        ESA,

        /** Gyro reference frame (this name was used in ADM V1.0 (CCSDS 504.0-B-1). */
        GYRO,

        /** Gyro reference frame (this name is used in SANA registry https://sanaregistry.org/r/spacecraft_body_reference_frames/). */
        GYRO_FRAME,

        /** Inertial Measurement Unit. */
        IMU_FRAME,

        /** Instrument. */
        INSTRUMENT,

        /** Magnetic Torque Assembly. */
        MTA,

        /** Reaction Wheel. */
        RW,

        /** Solar Array. */
        SA,

        /** Spacecraft Body. */
        SC_BODY,

        /** Sensor. */
        SENSOR,

        /** Star Tracker. */
        STARTRACKER,

        /** Three Axis Magnetometer. */
        TAM;

    }

    /** Equipment on which the frame is located. */
    private final BaseEquipment baseEquipment;

    /** Frame label. */
    private final String label;

    /** Simple constructor.
     * @param baseEquipment equipment on which the frame is located
     * @param label frame label
     */
    public SpacecraftBodyFrame(final BaseEquipment baseEquipment, final String label) {
        this.baseEquipment = baseEquipment;
        this.label         = label;
    }

    /** Get the quipment on which the frame is located.
     * @return equipment on which the frame is located
     */
    public BaseEquipment getBaseEquipment() {
        return baseEquipment;
    }

    /** Get the frame label.
     * @return frame label
     */
    public String getLabel() {
        return label;
    }

    /** {@inheritDoc}
     * <p>
     * The CCSDS composite name combines the {@link #getBaseEquipment() base equipment}
     * and the {@link #getLabel()}
     * </p>
     * @return CCSDS composite name
     */
    @Override
    public String toString() {
        return getBaseEquipment().name() + "_" + getLabel();
    }

    /** Build an instance from a normalized descriptor.
     * <p>
     * Normalized strings have '_' characters replaced by spaces,
     * and multiple spaces collapsed as one space only.
     * </p>
     * @param descriptor normalized descriptor
     * @return parsed body frame
     */
    public static SpacecraftBodyFrame parse(final String descriptor) {
        final int separatorIndex = descriptor.lastIndexOf('_');
        if (separatorIndex >= 0) {
            try {
                final String equipmentName = descriptor.substring(0, separatorIndex);
                return new SpacecraftBodyFrame(BaseEquipment.valueOf(equipmentName),
                                          descriptor.substring(separatorIndex + 1));
            } catch (IllegalArgumentException iae) {
                // ignored, errors are handled below
            }
        }
        throw new OrekitException(OrekitMessages.CCSDS_INVALID_FRAME, descriptor);
    }

}
