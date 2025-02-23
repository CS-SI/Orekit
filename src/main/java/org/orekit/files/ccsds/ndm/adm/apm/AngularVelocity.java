/* Copyright 2022-2025 Luc Maisonobe
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
package org.orekit.files.ccsds.ndm.adm.apm;

import org.orekit.files.ccsds.definitions.FrameFacade;
import org.orekit.files.ccsds.ndm.adm.AttitudeEndpoints;
import org.orekit.files.ccsds.section.CommentsContainer;

/**
 * Container for Attitude Parameter Message data lines.
 * <p>
 * Beware that the Orekit getters and setters all rely on SI units. The parsers
 * and writers take care of converting these SI units into CCSDS mandatory units.
 * The {@link org.orekit.utils.units.Unit Unit} class provides useful
 * {@link org.orekit.utils.units.Unit#fromSI(double) fromSi} and
 * {@link org.orekit.utils.units.Unit#toSI(double) toSI} methods in case the callers
 * already use CCSDS units instead of the API SI units. The general-purpose
 * {@link org.orekit.utils.units.Unit Unit} class (without an 's') and the
 * CCSDS-specific {@link org.orekit.files.ccsds.definitions.Units Units} class
 * (with an 's') also provide some predefined units. These predefined units and the
 * {@link org.orekit.utils.units.Unit#fromSI(double) fromSi} and
 * {@link org.orekit.utils.units.Unit#toSI(double) toSI} conversion methods are indeed
 * what the parsers and writers use for the conversions.
 * </p>
 * @author Luc Maisonobe
 * @since 12.0
 */
public class AngularVelocity extends CommentsContainer {

    /** Endpoints (i.e. frames A, B and their relationship). */
    private final AttitudeEndpoints endpoints;

    /** The frame in which angular velocities are specified. */
    private FrameFacade frame;

    /** Angular velocity around X axis (rad/s). */
    private double angVelX;

    /** Angular velocity around Y axis (rad/s). */
    private double angVelY;

    /** Angular velocity around Z axis (rad/s). */
    private double angVelZ;

    /** Simple constructor.
     */
    public AngularVelocity() {
        endpoints = new AttitudeEndpoints();
        frame     = null;
        angVelX   = Double.NaN;
        angVelY   = Double.NaN;
        angVelZ   = Double.NaN;
    }

    /** {@inheritDoc} */
    @Override
    public void validate(final double version) {
        super.validate(version);
        endpoints.checkMandatoryEntriesExceptExternalFrame(version,
                                                           AngularVelocityKey.REF_FRAME_A,
                                                           AngularVelocityKey.REF_FRAME_B,
                                                           null);
        endpoints.checkExternalFrame(AngularVelocityKey.REF_FRAME_A, AngularVelocityKey.REF_FRAME_B);
        checkNotNull(frame, AngularVelocityKey.ANGVEL_FRAME.name());
        checkNotNaN(angVelX, AngularVelocityKey.ANGVEL_X.name());
        checkNotNaN(angVelY, AngularVelocityKey.ANGVEL_Y.name());
        checkNotNaN(angVelZ, AngularVelocityKey.ANGVEL_Z.name());
    }

    /** Get the endpoints (i.e. frames A, B and their relationship).
     * @return endpoints
     */
    public AttitudeEndpoints getEndpoints() {
        return endpoints;
    }

    /** Set frame in which angular velocities are specified.
     * @param frame frame in which angular velocities are specified
     */
    public void setFrame(final FrameFacade frame) {
        this.frame = frame;
    }

    /** Get frame in which angular velocities are specified.
     * @return frame in which angular velocities are specified
     */
    public FrameFacade getFrame() {
        return frame;
    }

    /** Get the angular velocity around X axis (rad/s).
     * @return angular velocity around X axis (rad/s)
     */
    public double getAngVelX() {
        return angVelX;
    }

    /** Set the angular velocity around X axis (rad/s).
     * @param angVelX angular velocity around X axis (rad/s)
     */
    public void setAngVelX(final double angVelX) {
        refuseFurtherComments();
        this.angVelX = angVelX;
    }

    /** Get the angular velocity around Y axis (rad/s).
     * @return angular velocity around Y axis (rad/s)
     */
    public double getAngVelY() {
        return angVelY;
    }

    /** Set the angular velocity around Z axis (rad/s).
     * @param angVelZ angular velocity around Z axis (rad/s)
     */
    public void setAngVelZ(final double angVelZ) {
        refuseFurtherComments();
        this.angVelZ = angVelZ;
    }

    /** Get the angular velocity around Z axis (rad/s).
     * @return angular velocity around Z axis (rad/s)
     */
    public double getAngVelZ() {
        return angVelZ;
    }

    /** Set the angular velocity around Y axis (rad/s).
     * @param angVelY angular velocity around Y axis (rad/s)
     */
    public void setAngVelY(final double angVelY) {
        refuseFurtherComments();
        this.angVelY = angVelY;
    }

}
