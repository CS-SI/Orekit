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
package org.orekit.utils;

import org.hipparchus.util.MathUtils;

/** Set of useful physical constants.

 * @author Luc Maisonobe
 */
public interface Constants {


    /** Speed of light: 299792458.0 m/s. */
    double SPEED_OF_LIGHT = 299792458.0;

    /** Astronomical unit as a conventional unit of length since IAU 2012 resolution B2: 149597870700.0 m.
     * @see <a href="http://www.iau.org/static/resolutions/IAU2012_English.pdf">IAU 2012 resolution</a>
     */
    double IAU_2012_ASTRONOMICAL_UNIT = 149597870700.0;

    /** Duration of a mean solar day: 86400.0 s. */
    double JULIAN_DAY = 86400.0;

    /** Duration of a Julian year: 365.25 {@link #JULIAN_DAY}. */
    double JULIAN_YEAR = 31557600.0;

    /** Duration of a Julian century: 36525 {@link #JULIAN_DAY}. */
    double JULIAN_CENTURY = 36525 * JULIAN_DAY;


    /** Duration of a Besselian year: 365.242198781 {@link #JULIAN_DAY}. */
    double BESSELIAN_YEAR = 365.242198781 * JULIAN_DAY;


    /** Conversion factor from arc seconds to radians: 2*PI/(360*60*60). */
    double ARC_SECONDS_TO_RADIANS = MathUtils.TWO_PI / 1296000;


    /** Standard gravity constant, used in maneuvers definition: 9.80665 m/s². */
    double G0_STANDARD_GRAVITY = 9.80665;


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

    /** Earth gravitational constant from WGS84 model: 3.986004418e14 m³/s². */
    double WGS84_EARTH_MU = 3.986004418e14;

    /** Earth un-normalized second zonal coefficient from WGS84 model: . */
    double WGS84_EARTH_C20 = -1.08262668355315e-3;


    /** Earth equatorial radius from GRS80 model: 6378137.0 m. */
    double GRS80_EARTH_EQUATORIAL_RADIUS = 6378137.0;

    /** Earth flattening from GRS80 model: 1.0 / 298.257222101. */
    double GRS80_EARTH_FLATTENING = 1.0 / 298.257222101;

    /** Earth angular velocity from GRS80 model: 7.292115e-5 rad/s. */
    double GRS80_EARTH_ANGULAR_VELOCITY = 7.292115e-5;

    /** Earth gravitational constant from GRS80 model: 3.986005e14 m³/s². */
    double GRS80_EARTH_MU = 3.986005e14;

    /** Earth un-normalized second zonal coefficient from GRS80 model: -1.08263e-3. */
    double GRS80_EARTH_C20 = -1.08263e-3;


    /** Earth equatorial radius from EGM96 model: 6378136.3 m. */
    double EGM96_EARTH_EQUATORIAL_RADIUS = 6378136.3;

    /** Earth gravitational constant from EGM96 model: 3.986004415e14 m³/s². */
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

    /** Earth gravitational constant from GRIM5C1 model: 3.986004415e14 m³/s². */
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

    /** Earth gravitational constant from EIGEN5C model: 3.986004415e14 m³/s². */
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


    /** Gaussian gravitational constant: 0.01720209895 √(AU³/d²). */
    double JPL_SSD_GAUSSIAN_GRAVITATIONAL_CONSTANT = 0.01720209895;

    /** Astronomical Unit: 149597870691 m. */
    double JPL_SSD_ASTRONOMICAL_UNIT = 149597870691.0;

    /** Sun attraction coefficient (m³/s²). */
    double JPL_SSD_SUN_GM = JPL_SSD_GAUSSIAN_GRAVITATIONAL_CONSTANT * JPL_SSD_GAUSSIAN_GRAVITATIONAL_CONSTANT *
                            JPL_SSD_ASTRONOMICAL_UNIT * JPL_SSD_ASTRONOMICAL_UNIT * JPL_SSD_ASTRONOMICAL_UNIT /
                            (JULIAN_DAY * JULIAN_DAY);

    /** Sun/Mercury mass ratio: 6023600. */
    double JPL_SSD_SUN_MERCURY_MASS_RATIO = 6023600;

    /** Sun/Mercury attraction coefficient (m³/s²). */
    double JPL_SSD_MERCURY_GM = JPL_SSD_SUN_GM / JPL_SSD_SUN_MERCURY_MASS_RATIO;

    /** Sun/Venus mass ratio: 408523.71. */
    double JPL_SSD_SUN_VENUS_MASS_RATIO = 408523.71;

    /** Sun/Venus attraction coefficient (m³/s²). */
    double JPL_SSD_VENUS_GM = JPL_SSD_SUN_GM / JPL_SSD_SUN_VENUS_MASS_RATIO;

    /** Sun/(Earth + Moon) mass ratio: 328900.56. */
    double JPL_SSD_SUN_EARTH_PLUS_MOON_MASS_RATIO = 328900.56;

    /** Sun/(Earth + Moon) attraction coefficient (m³/s²). */
    double JPL_SSD_EARTH_PLUS_MOON_GM = JPL_SSD_SUN_GM / JPL_SSD_SUN_EARTH_PLUS_MOON_MASS_RATIO;

    /** Earth/Moon mass ratio: 81.30059. */
    double JPL_SSD_EARTH_MOON_MASS_RATIO = 81.300596;

    /** Moon attraction coefficient (m³/s²). */
    double JPL_SSD_MOON_GM = JPL_SSD_EARTH_PLUS_MOON_GM / (1.0 + JPL_SSD_EARTH_MOON_MASS_RATIO);

    /** Earth attraction coefficient (m³/s²). */
    double JPL_SSD_EARTH_GM = JPL_SSD_MOON_GM * JPL_SSD_EARTH_MOON_MASS_RATIO;

    /** Sun/(Mars system) mass ratio: 3098708.0. */
    double JPL_SSD_SUN_MARS_SYSTEM_MASS_RATIO = 3098708.0;

    /** Sun/(Mars system) attraction coefficient (m³/s²). */
    double JPL_SSD_MARS_SYSTEM_GM = JPL_SSD_SUN_GM / JPL_SSD_SUN_MARS_SYSTEM_MASS_RATIO;

    /** Sun/(Jupiter system) mass ratio: 1047.3486. */
    double JPL_SSD_SUN_JUPITER_SYSTEM_MASS_RATIO = 1047.3486;

    /** Sun/(Jupiter system) ttraction coefficient (m³/s²). */
    double JPL_SSD_JUPITER_SYSTEM_GM = JPL_SSD_SUN_GM / JPL_SSD_SUN_JUPITER_SYSTEM_MASS_RATIO;

    /** Sun/(Saturn system) mass ratio: 3497.898. */
    double JPL_SSD_SUN_SATURN_SYSTEM_MASS_RATIO = 3497.898;

    /** Sun/(Saturn system) attraction coefficient (m³/s²). */
    double JPL_SSD_SATURN_SYSTEM_GM = JPL_SSD_SUN_GM / JPL_SSD_SUN_SATURN_SYSTEM_MASS_RATIO;

    /** Sun/(Uranus system) mass ratio: 22902.98. */
    double JPL_SSD_SUN_URANUS_SYSTEM_MASS_RATIO = 22902.98;

    /** Sun/(Uranus system) attraction coefficient (m³/s²). */
    double JPL_SSD_URANUS_SYSTEM_GM = JPL_SSD_SUN_GM / JPL_SSD_SUN_URANUS_SYSTEM_MASS_RATIO;

    /** Sun/(Neptune system) mass ratio: 19412.24. */
    double JPL_SSD_SUN_NEPTUNE_SYSTEM_MASS_RATIO = 19412.24;

    /** Sun/(Neptune system) attraction coefficient (m³/s²). */
    double JPL_SSD_NEPTUNE_SYSTEM_GM = JPL_SSD_SUN_GM / JPL_SSD_SUN_NEPTUNE_SYSTEM_MASS_RATIO;

    /** Sun/(Pluto system) mass ratio: 1.35e8. */
    double JPL_SSD_SUN_PLUTO_SYSTEM_MASS_RATIO = 1.35e8;

    /** Sun/(Pluto system) ttraction coefficient (m³/s²). */
    double JPL_SSD_PLUTO_SYSTEM_GM = JPL_SSD_SUN_GM / JPL_SSD_SUN_PLUTO_SYSTEM_MASS_RATIO;

}
