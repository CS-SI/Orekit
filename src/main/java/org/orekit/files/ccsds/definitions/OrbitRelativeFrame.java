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
package org.orekit.files.ccsds.definitions;

import org.orekit.frames.LOFType;

/** Frames used in CCSDS Orbit Data Messages.
 * @author Luc Maisonobe
 * @since 11.0
 */
public enum OrbitRelativeFrame {

    /** Equinoctial coordinate system (X towards ascending node, Z towards momentum). */
    EQW_INERTIAL(LOFType.EQW, true),

    /** Local vertical, Local Horizontal (Z towards nadir, Y opposite to momentum). */
    LVLH_ROTATING(LOFType.LVLH_CCSDS, false),

    /** Local vertical, Local Horizontal (Z towards nadir, Y opposite to momentum). */
    LVLH_INERTIAL(LOFType.LVLH_CCSDS_INERTIAL, true),

    /** Local vertical, Local Horizontal (Z towards nadir, Y opposite to momentum). */
    LVLH(LOFType.LVLH_CCSDS, false),

    /** Nadir, Sun, Normal (X towards nadir, Y as close to Sun as possible). */
    NSW_ROTATING(null, false),

    /** Nadir, Sun, Normal (X towards nadir, Y as close to Sun as possible). */
    NSW_INERTIAL(null, true),

    /** Transverse Velocity Normal coordinate system (Y towards velocity, Z towards momentum). */
    NTW_ROTATING(LOFType.NTW, false),

    /** Transverse Velocity Normal coordinate system (Y towards velocity, Z towards momentum). */
    NTW_INERTIAL(LOFType.NTW_INERTIAL, true),

    /** Perifocal coordinate system (X towards periapsis, Z towards momentum). */
    PQW_INERTIAL(null, true),

    /** Another name for Radial, Transverse (along-track) and Normal (X towards zenith, Z towards momentum). */
    RSW_ROTATING(LOFType.QSW, false),

    /** Another name for Radial, Transverse (along-track) and Normal (X towards zenith, Z towards momentum). */
    RSW_INERTIAL(LOFType.QSW_INERTIAL, true),

    /** Another name for Radial, Transverse (along-track) and Normal. */
    RSW(LOFType.QSW, false),

    /** Another name for Radial, Transverse (along-track) and Normal (X towards zenith, Z towards momentum). */
    RIC(LOFType.QSW, false),

    /** Radial, Transverse (along-track) and Normal (X towards zenith, Z towards momentum). */
    RTN(LOFType.QSW, false),

    /** Another name for Radial, Transverse (along-track) and Normal (X towards zenith, Z towards momentum). */
    QSW(LOFType.QSW, false),

    /** Tangential, Normal, Cross-track coordinate system (X towards velocity, Z towards momentum). */
    TNW_ROTATING(LOFType.TNW, false),

    /** Tangential, Normal, Cross-track coordinate system (X towards velocity, Z towards momentum). */
    TNW_INERTIAL(LOFType.TNW_INERTIAL, true),

    /** TNW : x-axis along the velocity vector, W along the orbital angular momentum vector and
    N completes the right-handed system. */
    TNW(LOFType.TNW, false),

    /** South, East, Zenith coordinate system. */
    SEZ_ROTATING(null, false),

    /** South, East, Zenith coordinate system. */
    SEZ_INERTIAL(null, true),

    /** Velocity, Normal, Co-normal coordinate system (X towards velocity, Y towards momentum). */
    VNC_ROTATING(LOFType.VNC, false),

    /** Velocity, Normal, Co-normal coordinate system (X towards velocity, Y towards momentum). */
    VNC_INERTIAL(LOFType.VNC_INERTIAL, true);

    /** Type of Local Orbital Frame (may-be null). */
    private final LOFType lofType;

    /** Flag for inertial orientation. */
    private final boolean quasiInertial;

    /** Simple constructor.
     * @param lofType type of Local Orbital Frame (null if frame is not a Local Orbital Frame)
     * @param quasiInertial if true, frame should be treated as an inertial coordinate system
     */
    OrbitRelativeFrame(final LOFType lofType, final boolean quasiInertial) {
        this.lofType       = lofType;
        this.quasiInertial = quasiInertial;
    }

    /** Get the type of Local Orbital frame.
     * @return type of Local Orbital Frame, or null if the frame is not a local orbital frame
     */
    public LOFType getLofType() {
        return lofType;
    }

    /** Check if frame should be treated as inertial.
     * <p>
     * A frame treated as an inertial coordinate system if it
     * is considered to be redefined at each time of interest
     * </p>
     * @return true if frame should be treated as inertial
     */
    public boolean isQuasiInertial() {
        return quasiInertial;
    }

}
