/* Copyright 2002-2022 CS GROUP
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
package org.orekit.propagation.analytical.tle;


/** Constants necessary to TLE propagation.
 *
 * This constants are used in the WGS-72 model, compliant with NORAD implementations.
 *
 * @author Fabien Maussion
 */
public class TLEConstants {

    /** Constant 1.0 / 3.0. */
    public static final double ONE_THIRD = 1.0 / 3.0;

    /** Constant 2.0 / 3.0. */
    public static final double TWO_THIRD = 2.0 / 3.0;

    /** Earth radius in km. */
    public static final double EARTH_RADIUS = 6378.135;

    /** Equatorial radius rescaled (1.0). */
    public static final double NORMALIZED_EQUATORIAL_RADIUS = 1.0;

    /** Time units per julian day. */
    public static final double MINUTES_PER_DAY = 1440.0;

    // CHECKSTYLE: stop JavadocVariable check
    // Potential perturbation coefficients
    public static final double XKE    = 0.0743669161331734132; // mu = 3.986008e+14;
    public static final double XJ3    = -2.53881e-6;
    public static final double XJ2    = 1.082616e-3;
    public static final double XJ4    = -1.65597e-6;
    public static final double CK2    = 0.5 * XJ2 * NORMALIZED_EQUATORIAL_RADIUS * NORMALIZED_EQUATORIAL_RADIUS;
    public static final double CK4    = -0.375 * XJ4 * NORMALIZED_EQUATORIAL_RADIUS * NORMALIZED_EQUATORIAL_RADIUS *
                                        NORMALIZED_EQUATORIAL_RADIUS * NORMALIZED_EQUATORIAL_RADIUS;
    public static final double S      = NORMALIZED_EQUATORIAL_RADIUS * (1. + 78. / EARTH_RADIUS);
    public static final double QOMS2T = 1.880279159015270643865e-9;
    public static final double A3OVK2 = -XJ3 / CK2 * NORMALIZED_EQUATORIAL_RADIUS * NORMALIZED_EQUATORIAL_RADIUS *
                                        NORMALIZED_EQUATORIAL_RADIUS;

    // Deep SDP4 constants
    public static final double ZNS      = 1.19459E-5;
    public static final double ZES      = 0.01675;
    public static final double ZNL      = 1.5835218E-4;
    public static final double ZEL      = 0.05490;
    public static final double THDT     = 4.3752691E-3;
    public static final double C1SS     =  2.9864797E-6;
    public static final double C1L      = 4.7968065E-7;

    public static final double ROOT22   = 1.7891679E-6;
    public static final double ROOT32   = 3.7393792E-7;
    public static final double ROOT44   = 7.3636953E-9;
    public static final double ROOT52   = 1.1428639E-7;
    public static final double ROOT54   = 2.1765803E-9;

    public static final double Q22      =  1.7891679E-6;
    public static final double Q31      =  2.1460748E-6;
    public static final double Q33      =  2.2123015E-7;

    public static final double C_FASX2  =  0.99139134268488593;
    public static final double S_FASX2  =  0.13093206501640101;
    public static final double C_2FASX4 =  0.87051638752972937;
    public static final double S_2FASX4 = -0.49213943048915526;
    public static final double C_3FASX6 =  0.43258117585763334;
    public static final double S_3FASX6 =  0.90159499016666422;

    public static final double C_G22    =  0.87051638752972937;
    public static final double S_G22    = -0.49213943048915526;
    public static final double C_G32    =  0.57972190187001149;
    public static final double S_G32    =  0.81481440616389245;
    public static final double C_G44    = -0.22866241528815548;
    public static final double S_G44    =  0.97350577801807991;
    public static final double C_G52    =  0.49684831179884198;
    public static final double S_G52    =  0.86783740128127729;
    public static final double C_G54    = -0.29695209575316894;
    public static final double S_G54    = -0.95489237761529999;

    // CHECKSTYLE: resume JavadocVariable check

    /** Earth gravity coefficient in m³/s². */
    public static final double MU = XKE * XKE * EARTH_RADIUS * EARTH_RADIUS * EARTH_RADIUS * (1000 * 1000 * 1000) / (60 * 60);

    /** Private constructor for a utility class.
     */
    private TLEConstants() {
        // nothin to do
    }

}
