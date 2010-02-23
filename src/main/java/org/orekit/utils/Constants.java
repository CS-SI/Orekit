/* Copyright 2002-2010 CS Communication & Systèmes
 * Licensed to CS Communication & Systèmes (CS) under one or more
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
package org.orekit.utils;

/** Set of useful physical constants.

 * @author Luc Maisonobe
 * @version $Revision$ $Date$
 */
public interface Constants {


    /** Speed of light: 299792458.0 m/s. */
    double SPEED_OF_LIGHT = 299792458.0;

    /** Sun radius: 695500000 m. */
    double SUN_RADIUS = 6.955e8;

    /** Moon equatorial radius: 1737400 m. */
    double MOON_EQUATORIAL_RADIUS = 1737400.0;

    /** Earth equatorial radius from WGS84 model: 6378137.0 m. */
    double WGS84_EARTH_EQUATORIAL_RADIUS = 6378137.0;

    /** Earth flattening from WGS84 model: 1.0 / 298.257223563. */
    double WGS84_EARTH_FLATTENING = 1.0 / 298.257223563;

    /** Earth angular velocity from WGS84 model: 7.292115e-5 rad/s. */
    double WGS84_EARTH_ANGULAR_VELOCITY = 7.292115e-5;

    /** Earth gravitational constant from WGS84 model: 3.986004418 m<sup>3</sup>/s<sup>2</sup>. */
    double WGS84_EARTH_MU = 3.986004418e14;

    /** Earth un-normalized second zonal coefficient from WGS84 model: . */
    double WGS84_EARTH_C20 = -1.08262668355315e-3;


    /** Earth equatorial radius from GRS80 model: 6378137.0 m. */
    double GRS80_EARTH_EQUATORIAL_RADIUS = 6378137.0;

    /** Earth flattening from GRS80 model: 1.0 / 298.257222101. */
    double GRS80_EARTH_FLATTENING = 1.0 / 298.257222101;

    /** Earth angular velocity from GRS80 model: 7.292115e-5 rad/s. */
    double GRS80_EARTH_ANGULAR_VELOCITY = 7.292115e-5;

    /** Earth gravitational constant from GRS80 model: 3.986005e14 m<sup>3</sup>/s<sup>2</sup>. */
    double GRS80_EARTH_MU = 3.986005e14;

    /** Earth un-normalized second zonal coefficient from GRS80 model: -1.08263e-3. */
    double GRS80_EARTH_C20 = -1.08263e-3;


    /** Earth equatorial radius from EGM96 model: 6378136.3 m. */
    double EGM96_EARTH_EQUATORIAL_RADIUS = 6378136.3;

    /** Earth gravitational constant from EGM96 model: 3.986004415 m<sup>3</sup>/s<sup>2</sup>. */
    double EGM96_EARTH_MU = 3.986004415e14;

    /** Earth un-normalized second zonal coefficient from EGM96 model: -1.08262668355315e-3. */
    double EGM96_EARTH_C20 = -1.08262668355315e-3;

    /** Earth un-normalized third zonal coefficient from EGM96 model: 2.53265648533224e-6. */
    double EGM96_EARTH_C30 = 2.53265648533224e-6;

    /** Earth un-normalized fourth zonal coefficient from EGM96 model: 1.619621591367e-6. */
    double EGM96_EARTH_C40 = 1.619621591367e-6;

    /** Earth un-normalized fifth zonal coefficient from EGM96 model: 2.27296082868698e-7. */
    double EGM96_EARTH_C50 = 2.27296082868698e-7;

    /** Earth un-normalized sixth zonal coefficient from EGM96 model: -5.40681239107085e-7. */
    double EGM96_EARTH_C60 = -5.40681239107085e-7;


    /** Earth equatorial radius from GRIM5C1 model: 6378136.46 m. */
    double GRIM5C1_EARTH_EQUATORIAL_RADIUS = 6378136.46;

    /** Earth flattening from GRIM5C1 model: 1.0 / 298.25765. */
    double GRIM5C1_EARTH_FLATTENING = 1.0 / 298.25765;

    /** Earth angular velocity from GRIM5C1 model: 7.292115e-5 rad/s. */
    double GRIM5C1_EARTH_ANGULAR_VELOCITY = 7.292115e-5;

    /** Earth gravitational constant from GRIM5C1 model: 3.986004415 m<sup>3</sup>/s<sup>2</sup>. */
    double GRIM5C1_EARTH_MU = 3.986004415e14;

    /** Earth un-normalized second zonal coefficient from GRIM5C1 model: -1.082626110612609e-3. */
    double GRIM5C1_EARTH_C20 = -1.082626110612609e-3;

    /** Earth un-normalized third zonal coefficient from GRIM5C1 model: 2.536150841690056e-6. */
    double GRIM5C1_EARTH_C30 = 2.536150841690056e-6;

    /** Earth un-normalized fourth zonal coefficient from GRIM5C1 model: 1.61936352497151e-6. */
    double GRIM5C1_EARTH_C40 = 1.61936352497151e-6;

    /** Earth un-normalized fifth zonal coefficient from GRIM5C1 model: 2.231013736607540e-7. */
    double GRIM5C1_EARTH_C50 = 2.231013736607540e-7;

    /** Earth un-normalized sixth zonal coefficient from GRIM5C1 model: -5.402895357302363e-7. */
    double GRIM5C1_EARTH_C60 = -5.402895357302363e-7;


    /** Earth equatorial radius from EIGEN5C model: 6378136.46 m. */
    double EIGEN5C_EARTH_EQUATORIAL_RADIUS = 6378136.46;

    /** Earth gravitational constant from EIGEN5C model: 3.986004415 m<sup>3</sup>/s<sup>2</sup>. */
    double EIGEN5C_EARTH_MU = 3.986004415e14;

    /** Earth un-normalized second zonal coefficient from EIGEN5C model: -1.082626457231767e-3. */
    double EIGEN5C_EARTH_C20 = -1.082626457231767e-3;

    /** Earth un-normalized third zonal coefficient from EIGEN5C model: 2.532547231862799e-6. */
    double EIGEN5C_EARTH_C30 = 2.532547231862799e-6;

    /** Earth un-normalized fourth zonal coefficient from EIGEN5C model: 1.619964434136e-6. */
    double EIGEN5C_EARTH_C40 = 1.619964434136e-6;

    /** Earth un-normalized fifth zonal coefficient from EIGEN5C model: 2.277928487005437e-7. */
    double EIGEN5C_EARTH_C50 = 2.277928487005437e-7;

    /** Earth un-normalized sixth zonal coefficient from EIGEN5C model: -5.406653715879098e-7. */
    double EIGEN5C_EARTH_C60 = -5.406653715879098e-7;

}
