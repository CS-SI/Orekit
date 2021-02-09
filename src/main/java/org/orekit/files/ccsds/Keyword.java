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
package org.orekit.files.ccsds;

import org.orekit.files.ccsds.ndm.odm.ocm.OrbitCategory;

/** Keywords for CCSDS Navigation Data Messages.<p>
 * Only these should be used.<p>
 *  The enumeration is split in 3 parts:<p>
 *  - Common NDM keywords;<p>
 *  - Orbit Data Messages (ODM) specific keywords;<p>
 *  - Tracking Data Messages (TDM) specific keywords.<p>
 * References:<p>
 * - <a href="https://public.ccsds.org/Pubs/502x0b2c1.pdf">CCSDS 502.0-B-2 recommended standard</a> ("Orbit Data Messages", Blue Book, Issue 2.0, November 2009).<p>
 * - <a href="https://public.ccsds.org/Pubs/503x0b1c1.pdf">CCSDS 503.0-B-1 recommended standard</a> ("Tracking Data Message", Blue Book, Issue 1.0, November 2007).
 * @author sports
 * @author Maxime Journot
 * @since 6.1
 */
public enum Keyword {

    // ---------------------------------------------------
    // Common NDM (Navigation Data Message) CCSDS keywords
    // ---------------------------------------------------

    /** Comments specific to ODM and ADM file. */
    COMMENT,
    /** File creation date in UTC. */
    CREATION_DATE,
    /** Creating agency or operator. */
    ORIGINATOR,
    /** Unique ID identifying a message from a given originator.
     * @since 11.0
     */
    MESSAGE_ID,
    /** Classification of this message.
     * @since 11.0
     */
    CLASSIFICATION,
    /** Spacecraft name for which the orbit state is provided. */
    OBJECT_NAME,
    /** Alternate names fir this space object.
     * @since 11.0
     */
    ALTERNATE_NAMES,
    /** Unique satellite identification designator for the object.
     * @since 11.0
     */
    OBJECT_DESIGNATOR,
    /** Object identifier of the object for which the orbit state is provided. */
    OBJECT_ID,
    /** Origin of reference frame. */
    CENTER_NAME,
    /** Time system used for state vector, maneuver, and covariance data. */
    TIME_SYSTEM,
    /** Epoch of state vector and optional Keplerian elements.
     *  Or epoch of a TDM observation.
     */
    EPOCH,
    /** Start of total time span covered by: <p>
     * - Ephemerides data and covariance data;<p>
     * - Tracking data session.
     */
    START_TIME,
    /** End of total time span covered by: <p>
     * - Ephemerides data and covariance data;<p>
     * - Tracking data session.
     */
    STOP_TIME,
    /** User defined parameter, where X is replaced by a variable length user specified character
     *  string. Any number of user defined parameters may be included, if necessary to provide
     *  essential information that cannot be conveyed in COMMENT statements. */
    USER_DEFINED_X,
    /** Keyword used to delineate the start of a Meta-data block. */
    META_START,
    /** Keyword used to delineate the end of a Meta-data block. */
    META_STOP,
    /** Maneuver duration (If = 0, impulsive maneuver). */
    MAN_DURATION,
    /** Coordinate system for velocity increment vector. Its value can either be RSW, RTN (both
     * indicating "Radial, Transverse, Normal") or TNW. */
    MAN_REF_FRAME,
    /** Start of useable time span covered by ephemerides data, it may be
     * necessary to allow for proper interpolation. */
    USEABLE_START_TIME,
    /** End of useable time span covered by ephemerides data, it may be
     * necessary to allow for proper interpolation. */
    USEABLE_STOP_TIME,
    /** The interpolation degree. */
    INTERPOLATION_DEGREE,

    // -------------------------------------------
    // Orbit Data Messages (ODM) specific keywords
    // -------------------------------------------

    /** CCSDS OPM format version. */
    CCSDS_OPM_VERS,
    /** CCSDS OMM format version. */
    CCSDS_OMM_VERS,
    /** CCSDS OEM format version. */
    CCSDS_OEM_VERS,
    /** CCSDS OCM format version.
     * @since 11.0
     */
    CCSDS_OCM_VERS,
    /** Start of orbit data section.
     * @since 11.0
     */
    ORB_START,
    /** Stop of orbit data section.
     * @since 11.0
     */
    ORB_STOP,
    /** Orbit identification number.
     * @since 11.0
     */
    ORB_ID,
    /** Identification number of previous orbit.
     * @since 11.0
     */
    ORB_PREV_ID,
    /** Identification number of next orbit.
     * @since 11.0
     */
    ORB_NEXT_ID,
    /** Basis of this orbit state time history data.
     * @see CCSDSOrbitBasis
     * @since 11.0
     */
    ORB_BASIS,
    /** Identification number of the orbit determination or simulation upon which this orbit is based.
     * @since 11.0
     */
    ORB_BASIS_ID,
    /** Type of averaging (Osculating, mean Brouwer, other...).
     * @since 11.0
     */
    ORB_AVERAGING,
    /** Reference epoch for all relative times in the orbit state block.
     * @since 11.0
     */
    ORB_EPOCH_TZERO,
    /** Time system for {@link #ORB_EPOCH_TZERO}.
     * @since 11.0
     */
    ORB_TIME_SYSTEM,
    /** Reference frame of the orbit.
     * @since 11.0
     */
    ORB_REF_FRAME,
    /** Epoch of the {@link #ORB_REF_FRAME orbit reference frame}.
     * @since 11.0
     */
    ORB_FRAME_EPOCH,
    /** Orbit element set type.
     * @see CCSDSElementsType
     * @since 11.0
     */
    ORB_TYPE,
    /** Number of elements (excluding time) contain in the element set.
     * @since 11.0
     */
    ORB_N,
    /** Definition of orbit elements.
     * @since 11.0
     */
    ORB_ELEMENTS,
    /** Start of object physical characteristics data section.
     * @since 11.0
     */
    PHYS_START,
    /** Stop of object physical characteristics data section.
     * @since 11.0
     */
    PHYS_STOP,
    /** Start of covariance data section.
     * @since 11.0
     */
    COV_START,
    /** Stop of covariance data section.
     * @since 11.0
     */
    COV_STOP,
    /** Start of state transition matrix data section.
     * @since 11.0
     */
    STM_START,
    /** Stop of state transition matrix data section.
     * @since 11.0
     */
    STM_STOP,
    /** Start of maneuver data section.
     * @since 11.0
     */
    MAN_START,
    /** Stop of maneuver data section.
     * @since 11.0
     */
    MAN_STOP,
    /** Start of perturbations data section.
     * @since 11.0
     */
    PERT_START,
    /** Stop of perturbations data section.
     * @since 11.0
     */
    PERT_STOP,
    /** Start of orbit determination data section.
     * @since 11.0
     */
    OD_START,
    /** Stop of orbit determination data section.
     * @since 11.0
     */
    OD_STOP,
    /** Start of user-defined parameters data section.
     * @since 11.0
     */
    USER_START,
    /** Stop of user-defined parameters data section.
     * @since 11.0
     */
    USER_STOP,
    /** Name of the reference frame in which the state vector and optional Keplerian element data are given. */
    REF_FRAME,
    /** Epoch of reference frame, if not intrinsic to the definition of the reference frame. */
    REF_FRAME_EPOCH,
    /** Mean element theory. */
    MEAN_ELEMENT_THEORY,
    /** Position vector X-component. */
    X,
    /** Position vector Y-component. */
    Y,
    /** Position vector Z-component. */
    Z,
    /** Velocity vector X-component. */
    X_DOT,
    /** Velocity vector Y-component. */
    Y_DOT,
    /** Velocity vector Z-component. */
    Z_DOT,
    /** Orbit semi-major axis. */
    SEMI_MAJOR_AXIS,
    /** Mean Motion. */
    MEAN_MOTION,
    /** Orbit eccentricity. */
    ECCENTRICITY,
    /** Orbit inclination. */
    INCLINATION,
    /** Orbit right ascension of ascending node. */
    RA_OF_ASC_NODE,
    /** Orbit argument of pericenter. */
    ARG_OF_PERICENTER,
    /** Orbit true anomaly. */
    TRUE_ANOMALY,
    /** Orbit mean anomaly.*/
    MEAN_ANOMALY,
    /** Gravitational coefficient. */
    GM,
    /** Spacecraft mass. */
    MASS,
    /** Solar radiation pressure area. */
    SOLAR_RAD_AREA,
    /** Solar radiation pressure coefficient. */
    SOLAR_RAD_COEFF,
    /** Drag area. */
    DRAG_AREA,
    /** Drag coefficient. */
    DRAG_COEFF,
    /** Ephemeris type. */
    EPHEMERIS_TYPE,
    /** Classification type. */
    CLASSIFICATION_TYPE,
    /** NORAD catalogue number. */
    NORAD_CAT_ID,
    /** Element set number of the satellite. */
    ELEMENT_SET_NO,
    /** Revolution Number. */
    REV_AT_EPOCH,
    /** SGP/SGP4 drag-like coefficient. */
    BSTAR,
    /** First Time Derivative of the Mean Motion. */
    MEAN_MOTION_DOT,
    /** Second Time Derivative of the Mean Motion. */
    MEAN_MOTION_DDOT,
    /** Coordinate system for covariance matrix. Its value can either be RSW, RTN (both indicating
    /* "Radial, Transverse, Normal") or TNW. */
    COV_REF_FRAME,
    /** Covariance matrix [1, 1] element. */
    CX_X,
    /** Covariance matrix [2, 1] element. */
    CY_X,
    /** Covariance matrix [2, 2] element. */
    CY_Y,
    /** Covariance matrix [3, 1] element. */
    CZ_X,
    /** Covariance matrix [3, 2] element. */
    CZ_Y,
    /** Covariance matrix [3, 3] element. */
    CZ_Z,
    /** Covariance matrix [4, 1] element. */
    CX_DOT_X,
    /** Covariance matrix [4, 2] element. */
    CX_DOT_Y,
    /** Covariance matrix [4, 3] element. */
    CX_DOT_Z,
    /** Covariance matrix [4, 4] element. */
    CX_DOT_X_DOT,
    /** Covariance matrix [5, 1] element. */
    CY_DOT_X,
    /** Covariance matrix [5, 2] element. */
    CY_DOT_Y,
    /** Covariance matrix [5, 3] element. */
    CY_DOT_Z,
    /** Covariance matrix [5, 4] element. */
    CY_DOT_X_DOT,
    /** Covariance matrix [5, 5] element. */
    CY_DOT_Y_DOT,
    /** Covariance matrix [6, 1] element. */
    CZ_DOT_X,
    /** Covariance matrix [6, 2] element. */
    CZ_DOT_Y,
    /** Covariance matrix [6, 3] element. */
    CZ_DOT_Z,
    /** Covariance matrix [6, 4] element. */
    CZ_DOT_X_DOT,
    /** Covariance matrix [6, 5] element. */
    CZ_DOT_Y_DOT,
    /** Covariance matrix [6, 6] element. */
    CZ_DOT_Z_DOT,
    /** Epoch of ignition. */
    MAN_EPOCH_IGNITION,
    /** Mass change during maneuver (value is &lt; 0). */
    MAN_DELTA_MASS,
    /** First component of the velocity increment. */
    MAN_DV_1,
    /** Second component of the velocity increment. */
    MAN_DV_2,
    /** Third component of the velocity increment. */
    MAN_DV_3,
    /** This keyword must appear before the first line of the covariance matrix data. */
    COVARIANCE_START,
    /** The interpolation method to be used. */
    INTERPOLATION,
    /** This keyword must appear after the last line of the covariance matrix data. */
    COVARIANCE_STOP,

    // -------------------------------------------
    // Attitude Data Messages (ADM) specific keywords
    // -------------------------------------------

    /** CCSDS AEM format version. */
    CCSDS_AEM_VERS,
    /** Rotation order of the EULER_FRAME_A to EULER_FRAME_B or vice versa. */
    EULER_ROT_SEQ,
    /** The value of this keyword expresses the relevant keyword to use that denotes the
     *  frame of reference in which the X_RATE, Y_RATE and Z_RATE are expressed. */
    RATE_FRAME,
    /** Name of the reference frame specifying one frame of the transformation. */
    REF_FRAME_A,
    /** Name of the reference frame specifying the second portion of the transformation. */
    REF_FRAME_B,
    /** Rotation direction of the attitude. */
    ATTITUDE_DIR,
    /** The format of the data lines in the message. */
    ATTITUDE_TYPE,
    /** The placement of the scalar portion of the quaternion (QC) in the attitude data. */
    QUATERNION_TYPE,
    /** Recommended interpolation method for attitude ephemeris data. */
    INTERPOLATION_METHOD,

    // Miscellaneous KEYVALUE keywords
    /** Keyword used to delineate the start of a Data block in Keyvalue files. */
    DATA_START,
    /** Keyword used to delineate the end of a Data block in Keyvalue files.. */
    DATA_STOP;

}
