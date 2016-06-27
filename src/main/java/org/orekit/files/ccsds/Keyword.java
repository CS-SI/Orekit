/* Copyright 2002-2016 CS Systèmes d'Information
 * Licensed to CS Systèmes d'Information (CS) under one or more
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

/** Keywords for CCSDS orbit data messages. Only these should be used.
 * @author sports
 * @since 6.1
 */
public enum Keyword {
    /** Comments specific to a ODM file. */
    COMMENT,
    /** CCSDS OPM format version. */
    CCSDS_OPM_VERS,
    /** CCSDS OMM format version. */
    CCSDS_OMM_VERS,
    /** File creation date in UTC. */
    CCSDS_OEM_VERS,
    /** CCSDS OEM format version. */
    CREATION_DATE,
    /** Creating agency or operator. */
    ORIGINATOR,
    /** Spacecraft name for which the orbit state is provided. */
    OBJECT_NAME,
    /** Object identifier of the object for which the orbit state is provided. */
    OBJECT_ID,
    /** Origin of reference frame. */
    CENTER_NAME,
    /** Name of the reference frame in which the state vector and optional Keplerian element data are given. */
    REF_FRAME,
    /** Epoch of reference frame, if not intrinsic to the definition of the reference frame. */
    REF_FRAME_EPOCH,
    /** Time system used for state vector, maneuver, and covariance data. */
    TIME_SYSTEM,
    /** Mean element theory. */
    MEAN_ELEMENT_THEORY,
    /** Epoch of state vector and optional Keplerian elements. */
    EPOCH,
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
    /** Covariance matrix [1,1] element. */
    CX_X,
    /** Covariance matrix [2,1] element. */
    CY_X,
    /** Covariance matrix [2,2] element. */
    CY_Y,
    /** Covariance matrix [3,1] element. */
    CZ_X,
    /** Covariance matrix [3,2] element. */
    CZ_Y,
    /** Covariance matrix [3,3] element. */
    CZ_Z,
    /** Covariance matrix [4,1] element. */
    CX_DOT_X,
    /** Covariance matrix [4,2] element. */
    CX_DOT_Y,
    /** Covariance matrix [4,3] element. */
    CX_DOT_Z,
    /** Covariance matrix [4,4] element. */
    CX_DOT_X_DOT,
    /** Covariance matrix [5,1] element. */
    CY_DOT_X,
    /** Covariance matrix [5,2] element. */
    CY_DOT_Y,
    /** Covariance matrix [5,3] element. */
    CY_DOT_Z,
    /** Covariance matrix [5,4] element. */
    CY_DOT_X_DOT,
    /** Covariance matrix [5,5] element. */
    CY_DOT_Y_DOT,
    /** Covariance matrix [6,1] element. */
    CZ_DOT_X,
    /** Covariance matrix [6,2] element. */
    CZ_DOT_Y,
    /** Covariance matrix [6,3] element. */
    CZ_DOT_Z,
    /** Covariance matrix [6,4] element. */
    CZ_DOT_X_DOT,
    /** Covariance matrix [6,5] element. */
    CZ_DOT_Y_DOT,
    /** Covariance matrix [6,6] element. */
    CZ_DOT_Z_DOT,
    /** Epoch of ignition. */
    MAN_EPOCH_IGNITION,
    /** Maneuver duration (If = 0, impulsive maneuver). */
    MAN_DURATION,
    /** Mass change during maneuver (value is &lt; 0). */
    MAN_DELTA_MASS,
    /** Coordinate system for velocity increment vector. Its value can either be RSW, RTN (both
     * indicating "Radial, Transverse, Normal") or TNW. */
    MAN_REF_FRAME,
    /** First component of the velocity increment. */
    MAN_DV_1,
    /** Second component of the velocity increment. */
    MAN_DV_2,
    /** Third component of the velocity increment. */
    MAN_DV_3,
    /** User defined parameter, where X is replaced by a variable length user specified character
     *  string. Any number of user defined parameters may be included, if necessary to provide
     *  essential information that cannot be conveyed in COMMENT statements. */
    USER_DEFINED_X,
    /** Keyword used to delineate the start of a metadata block. */
    META_START,
    /** Keyword used to delineate the end of a metadata block. */
    META_STOP,
    /** This keyword must appear before the first line of the covariance matrix data. */
    COVARIANCE_START,
    /** Start of total time span covered by ephemerides data and covariance data. */
    START_TIME,
    /** Start of useable time span covered by ephemerides data, it may be
     * necessary to allow for proper interpolation. */
    USEABLE_START_TIME,
    /** End of useable time span covered by ephemerides data, it may be
     * necessary to allow for proper interpolation. */
    USEABLE_STOP_TIME,
    /** End of total time span covered by ephemerides data and covariance data. */
    STOP_TIME,
    /** The interpolation method to be used. */
    INTERPOLATION,
    /** The interpolation degree. */
    INTERPOLATION_DEGREE,
    /** This keyword must appear after the last line of the covariance matrix data. */
    COVARIANCE_STOP;
}
