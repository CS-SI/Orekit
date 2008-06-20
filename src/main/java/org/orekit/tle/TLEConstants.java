/* Copyright 2002-2008 CS Communication & Systèmes
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
package org.orekit.tle;


/** Constants necessary to TLE propagation.
 *
 * This constants are used in the WGS-72 model, compliant with NORAD implementations.
 *
 * @author Fabien Maussion
 * @version $Revision:1665 $ $Date:2008-06-11 12:12:59 +0200 (mer., 11 juin 2008) $
 */
final class TLEConstants {

    /** Constant 1.0 / 3.0. */
    public static final double ONE_THIRD = 1.0 / 3.0;

    /** Constant 2.0 / 3.0. */
    public static final double TWO_THIRD = 2.0 / 3.0;

    /** Earth radius in km. */
    public static final double EARTH_RADIUS = 6378.135;

    /** Equatorial radius redimensioned (1.0). */
    public static final double NORMALIZED_EQUATORIAL_RADIUS = 1.0;

    /** Time units per julian day. */
    public static final double MINUTES_PER_DAY = 1440.0;

    // CHECKSTYLE: stop JavadocVariable check
    // Potential perturbation coefficients
    public static final double XKE = 0.0743669161331734132; // mu = 3.986008e+14;
    public static final double XJ3 = -2.53881e-6;
    public static final double XJ2 = 1.082616e-3;
    public static final double XJ4 = -1.65597e-6;
    public static final double CK2 =
        0.5 * XJ2 * NORMALIZED_EQUATORIAL_RADIUS * NORMALIZED_EQUATORIAL_RADIUS;
    public static final double CK4 =
        -0.375 * XJ4 * NORMALIZED_EQUATORIAL_RADIUS * NORMALIZED_EQUATORIAL_RADIUS *
        NORMALIZED_EQUATORIAL_RADIUS * NORMALIZED_EQUATORIAL_RADIUS;
    public static final double S = NORMALIZED_EQUATORIAL_RADIUS * (1. + 78. / EARTH_RADIUS);
    public static final double QOMS2T = 1.880279159015270643865e-9;
    public static final double A3OVK2 =
        -XJ3 / CK2 * NORMALIZED_EQUATORIAL_RADIUS * NORMALIZED_EQUATORIAL_RADIUS *
        NORMALIZED_EQUATORIAL_RADIUS;
    // CHECKSTYLE: resume JavadocVariable check

}
