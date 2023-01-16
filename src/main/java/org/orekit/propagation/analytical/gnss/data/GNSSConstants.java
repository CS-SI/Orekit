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
package org.orekit.propagation.analytical.gnss.data;

/**
 * Set of useful physical constants used in Global Navigation Satellite Systems (GNSS).

 * @author Bryan Cazabonne
 * @author Pascal Parraud
 */
public interface GNSSConstants {

    /** Value of Pi for conversion from semicircles to radians in GNSS. */
    double GNSS_PI = 3.1415926535898;

    /** Duration of the GNSS week in seconds. */
    double GNSS_WEEK_IN_SECONDS = 604800.;

    /** Earth's universal gravitational parameter for Beidou user in m³/s². */
    double BEIDOU_MU = 3.986004418e+14;

    /** Number of weeks in the Beidou cycle. */
    int BEIDOU_WEEK_NB = 8192;

    /** Value of the earth's rotation rate in rad/s for Beidou user. */
    double BEIDOU_AV = 7.2921150e-5;

    /** Earth's universal gravitational parameter for Galileo user in m³/s². */
    double GALILEO_MU = 3.986004418e+14;

    /** Number of weeks in the Galileo cycle. */
    int GALILEO_WEEK_NB = 4096;

    /** Value of the earth's rotation rate in rad/s for Galileo user. */
    double GALILEO_AV = 7.2921151467e-5;

    /** Value of the Earth's universal gravitational parameter for GLONASS user in m³/s². */
    double GLONASS_MU = 3.986004418e+14;

    /** Value of Pi for conversion from semicircles to radian. */
    double GLONASS_PI = 3.14159265358979;

    /** WGS 84 value of the Earth's universal gravitational parameter for GPS user in m³/s². */
    double GPS_MU = 3.986005e+14;

    /** Number of weeks in the GPS cycle. */
    int GPS_WEEK_NB = 1024;

    /** Value of the earth's rotation rate in rad/s for GPS user. */
    double GPS_AV = 7.2921151467e-5;

    /** WGS 84 value of the Earth's universal gravitational parameter for IRNSS user in m³/s². */
    double IRNSS_MU = 3.986005e+14;

    /** Number of weeks in the IRNSS cycle. */
    int IRNSS_WEEK_NB = 1024;

    /** Value of the earth's rotation rate in rad/s for IRNSS user. */
    double IRNSS_AV = 7.2921151467e-5;

    /** WGS 84 value of the Earth's universal gravitational parameter for QZSS user in m³/s². */
    double QZSS_MU = 3.986005e+14;

    /** Number of weeks in the QZSS cycle. */
    int QZSS_WEEK_NB = 1024;

    /** Value of the earth's rotation rate in rad/s for QZSS user. */
    double QZSS_AV = 7.2921151467e-5;

    /** WGS 84 value of the Earth's universal gravitational parameter for SBAS user in m³/s². */
    double SBAS_MU = 3.986005e+14;

}
