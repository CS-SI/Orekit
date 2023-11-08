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
package org.orekit.models.earth.displacement;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.hipparchus.analysis.UnivariateFunction;
import org.hipparchus.analysis.interpolation.SplineInterpolator;
import org.hipparchus.analysis.polynomials.PolynomialSplineFunction;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.SinCos;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.data.BodiesElements;
import org.orekit.frames.Frame;

/**
 * Modeling of displacement of reference points due to ocean loading.
 * <p>
 * This class implements the same model as IERS HARDIP.F program. For a
 * given site, this model uses a set of amplitudes and phases for the 11
 * main tides (M₂, S₂, N₂, K₂, K₁, O₁, P₁, Q₁, Mf, Mm, and Ssa) in BLQ
 * format as provided by the <a
 * href="http://holt.oso.chalmers.se/loading/">Bos-Scherneck web site</a>
 * at Onsala Space Observatory. From these elements, additional admittances
 * are derived using spline interpolation based on tides frequencies for
 * a total of 342 tides, including the 11 main tides.
 * </p>
 * <p>
 * This implementation is a complete rewrite of the original HARDISP.F program
 * developed by Duncan Agnew and copyright 2008 IERS Conventions center. This
 * derived work is not endorsed by the IERS conventions center. What remains
 * from the original program is the model (spline interpolation and coefficients).
 * The code by itself is completely different, using the underlying mathematical
 * library for spline interpolation and the existing Orekit features for nutation
 * arguments, time and time scales handling, tides modeling...
 * </p>
 * <p>
 * Instances of this class are guaranteed to be immutable
 * </p>
 * <p>
 * The original HARDISP.F program is distributed with the following notice:
 * </p>
 * <pre>
 *  Copyright (C) 2008
 *  IERS Conventions Center
 *
 *  ==================================
 *  IERS Conventions Software License
 *  ==================================
 *
 *  NOTICE TO USER:
 *
 *  BY USING THIS SOFTWARE YOU ACCEPT THE FOLLOWING TERMS AND CONDITIONS
 *  WHICH APPLY TO ITS USE.
 *
 *  1. The Software is provided by the IERS Conventions Center ("the
 *     Center").
 *
 *  2. Permission is granted to anyone to use the Software for any
 *     purpose, including commercial applications, free of charge,
 *     subject to the conditions and restrictions listed below.
 *
 *  3. You (the user) may adapt the Software and its algorithms for your
 *     own purposes and you may distribute the resulting "derived work"
 *     to others, provided that the derived work complies with the
 *     following requirements:
 *
 *     a) Your work shall be clearly identified so that it cannot be
 *        mistaken for IERS Conventions software and that it has been
 *        neither distributed by nor endorsed by the Center.
 *
 *     b) Your work (including source code) must contain descriptions of
 *        how the derived work is based upon and/or differs from the
 *        original Software.
 *
 *     c) The name(s) of all modified routine(s) that you distribute
 *        shall be changed.
 *
 *     d) The origin of the IERS Conventions components of your derived
 *        work must not be misrepresented; you must not claim that you
 *        wrote the original Software.
 *
 *     e) The source code must be included for all routine(s) that you
 *        distribute.  This notice must be reproduced intact in any
 *        source distribution.
 *
 *  4. In any published work produced by the user and which includes
 *     results achieved by using the Software, you shall acknowledge
 *     that the Software was used in obtaining those results.
 *
 *  5. The Software is provided to the user "as is" and the Center makes
 *     no warranty as to its use or performance.   The Center does not
 *     and cannot warrant the performance or results which the user may
 *     obtain by using the Software.  The Center makes no warranties,
 *     express or implied, as to non-infringement of third party rights,
 *     merchantability, or fitness for any particular purpose.  In no
 *     event will the Center be liable to the user for any consequential,
 *     incidental, or special damages, including any lost profits or lost
 *     savings, even if a Center representative has been advised of such
 *     damages, or for any claim by any third party.
 *
 *  Correspondence concerning IERS Conventions software should be
 *  addressed as follows:
 *
 *                     Gerard Petit
 *     Internet email: gpetit[at]bipm.org
 *     Postal address: IERS Conventions Center
 *                     Time, frequency and gravimetry section, BIPM
 *                     Pavillon de Breteuil
 *                     92312 Sevres  FRANCE
 *
 *     or
 *
 *                     Brian Luzum
 *     Internet email: brian.luzum[at]usno.navy.mil
 *     Postal address: IERS Conventions Center
 *                     Earth Orientation Department
 *                     3450 Massachusetts Ave, NW
 *                     Washington, DC 20392
 * </pre>
 * @see org.orekit.estimation.measurements.GroundStation
 * @since 9.1
 * @author Luc Maisonobe
 */
public class OceanLoading implements StationDisplacement {

    // CHECKSTYLE: stop Indentation check
    /** Amplitudes of all tides used. */
    private static final double[] CARTWRIGHT_EDDEN_AMPLITUDE = {
        0.632208,  0.294107,  0.121046,  0.079915,  0.023818, -0.023589,  0.022994,
        0.019333, -0.017871,  0.017192,  0.016018,  0.004671, -0.004662, -0.004519,
        0.004470,  0.004467,  0.002589, -0.002455, -0.002172,  0.001972,  0.001947,
        0.001914, -0.001898,  0.001802,  0.001304,  0.001170,  0.001130,  0.001061,
       -0.001022, -0.001017,  0.001014,  0.000901, -0.000857,  0.000855,  0.000855,
        0.000772,  0.000741,  0.000741, -0.000721,  0.000698,  0.000658,  0.000654,
       -0.000653,  0.000633,  0.000626, -0.000598,  0.000590,  0.000544,  0.000479,
       -0.000464,  0.000413, -0.000390,  0.000373,  0.000366,  0.000366, -0.000360,
       -0.000355,  0.000354,  0.000329,  0.000328,  0.000319,  0.000302,  0.000279,
       -0.000274, -0.000272,  0.000248, -0.000225,  0.000224, -0.000223, -0.000216,
        0.000211,  0.000209,  0.000194,  0.000185, -0.000174, -0.000171,  0.000159,
        0.000131,  0.000127,  0.000120,  0.000118,  0.000117,  0.000108,  0.000107,
        0.000105, -0.000102,  0.000102,  0.000099, -0.000096,  0.000095, -0.000089,
       -0.000085, -0.000084, -0.000081, -0.000077, -0.000072, -0.000067,  0.000066,
        0.000064,  0.000063,  0.000063,  0.000063,  0.000062,  0.000062, -0.000060,
        0.000056,  0.000053,  0.000051,  0.000050,  0.368645, -0.262232, -0.121995,
       -0.050208,  0.050031, -0.049470,  0.020620,  0.020613,  0.011279, -0.009530,
       -0.009469, -0.008012,  0.007414, -0.007300,  0.007227, -0.007131, -0.006644,
        0.005249,  0.004137,  0.004087,  0.003944,  0.003943,  0.003420,  0.003418,
        0.002885,  0.002884,  0.002160, -0.001936,  0.001934, -0.001798,  0.001690,
        0.001689,  0.001516,  0.001514, -0.001511,  0.001383,  0.001372,  0.001371,
       -0.001253, -0.001075,  0.001020,  0.000901,  0.000865, -0.000794,  0.000788,
        0.000782, -0.000747, -0.000745,  0.000670, -0.000603, -0.000597,  0.000542,
        0.000542, -0.000541, -0.000469, -0.000440,  0.000438,  0.000422,  0.000410,
       -0.000374, -0.000365,  0.000345,  0.000335, -0.000321, -0.000319,  0.000307,
        0.000291,  0.000290, -0.000289,  0.000286,  0.000275,  0.000271,  0.000263,
       -0.000245,  0.000225,  0.000225,  0.000221, -0.000202, -0.000200, -0.000199,
        0.000192,  0.000183,  0.000183,  0.000183, -0.000170,  0.000169,  0.000168,
        0.000162,  0.000149, -0.000147, -0.000141,  0.000138,  0.000136,  0.000136,
        0.000127,  0.000127, -0.000126, -0.000121, -0.000121,  0.000117, -0.000116,
       -0.000114, -0.000114, -0.000114,  0.000114,  0.000113,  0.000109,  0.000108,
        0.000106, -0.000106, -0.000106,  0.000105,  0.000104, -0.000103, -0.000100,
       -0.000100, -0.000100,  0.000099, -0.000098,  0.000093,  0.000093,  0.000090,
       -0.000088,  0.000083, -0.000083, -0.000082, -0.000081, -0.000079, -0.000077,
       -0.000075, -0.000075, -0.000075,  0.000071,  0.000071, -0.000071,  0.000068,
        0.000068,  0.000065,  0.000065,  0.000064,  0.000064,  0.000064, -0.000064,
       -0.000060,  0.000056,  0.000056,  0.000053,  0.000053,  0.000053, -0.000053,
        0.000053,  0.000053,  0.000052,  0.000050, -0.066607, -0.035184, -0.030988,
        0.027929, -0.027616, -0.012753, -0.006728, -0.005837, -0.005286, -0.004921,
       -0.002884, -0.002583, -0.002422,  0.002310,  0.002283, -0.002037,  0.001883,
       -0.001811, -0.001687, -0.001004, -0.000925, -0.000844,  0.000766,  0.000766,
       -0.000700, -0.000495, -0.000492,  0.000491,  0.000483,  0.000437, -0.000416,
       -0.000384,  0.000374, -0.000312, -0.000288, -0.000273,  0.000259,  0.000245,
       -0.000232,  0.000229, -0.000216,  0.000206, -0.000204, -0.000202,  0.000200,
        0.000195, -0.000190,  0.000187,  0.000180, -0.000179,  0.000170,  0.000153,
       -0.000137, -0.000119, -0.000119, -0.000112, -0.000110, -0.000110,  0.000107,
       -0.000095, -0.000095, -0.000091, -0.000090, -0.000081, -0.000079, -0.000079,
        0.000077, -0.000073,  0.000069, -0.000067, -0.000066,  0.000065,  0.000064,
       -0.000062,  0.000060,  0.000059, -0.000056,  0.000055, -0.000051
    };
    // CHECKSTYLE: resume Indentation check

    // CHECKSTYLE: stop NoWhitespaceAfter check
    /** Doodson arguments for all tides used. */
    private static final int[][] DOODSON_ARGUMENTS = {
        { 2,  0,  0,  0,  0,  0 }, { 2,  2, -2,  0,  0,  0 }, { 2, -1,  0,  1,  0,  0 },
        { 2,  2,  0,  0,  0,  0 }, { 2,  2,  0,  0,  1,  0 }, { 2,  0,  0,  0, -1,  0 },
        { 2, -1,  2, -1,  0,  0 }, { 2, -2,  2,  0,  0,  0 }, { 2,  1,  0, -1,  0,  0 },
        { 2,  2, -3,  0,  0,  1 }, { 2, -2,  0,  2,  0,  0 }, { 2, -3,  2,  1,  0,  0 },
        { 2,  1, -2,  1,  0,  0 }, { 2, -1,  0,  1, -1,  0 }, { 2,  3,  0, -1,  0,  0 },
        { 2,  1,  0,  1,  0,  0 }, { 2,  2,  0,  0,  2,  0 }, { 2,  2, -1,  0,  0, -1 },
        { 2,  0, -1,  0,  0,  1 }, { 2,  1,  0,  1,  1,  0 }, { 2,  3,  0, -1,  1,  0 },
        { 2,  0,  1,  0,  0, -1 }, { 2,  0, -2,  2,  0,  0 }, { 2, -3,  0,  3,  0,  0 },
        { 2, -2,  3,  0,  0, -1 }, { 2,  4,  0,  0,  0,  0 }, { 2, -1,  1,  1,  0, -1 },
        { 2, -1,  3, -1,  0, -1 }, { 2,  2,  0,  0, -1,  0 }, { 2, -1, -1,  1,  0,  1 },
        { 2,  4,  0,  0,  1,  0 }, { 2, -3,  4, -1,  0,  0 }, { 2, -1,  2, -1, -1,  0 },
        { 2,  3, -2,  1,  0,  0 }, { 2,  1,  2, -1,  0,  0 }, { 2, -4,  2,  2,  0,  0 },
        { 2,  4, -2,  0,  0,  0 }, { 2,  0,  2,  0,  0,  0 }, { 2, -2,  2,  0, -1,  0 },
        { 2,  2, -4,  0,  0,  2 }, { 2,  2, -2,  0, -1,  0 }, { 2,  1,  0, -1, -1,  0 },
        { 2, -1,  1,  0,  0,  0 }, { 2,  2, -1,  0,  0,  1 }, { 2,  2,  1,  0,  0, -1 },
        { 2, -2,  0,  2, -1,  0 }, { 2, -2,  4, -2,  0,  0 }, { 2,  2,  2,  0,  0,  0 },
        { 2, -4,  4,  0,  0,  0 }, { 2, -1,  0, -1, -2,  0 }, { 2,  1,  2, -1,  1,  0 },
        { 2, -1, -2,  3,  0,  0 }, { 2,  3, -2,  1,  1,  0 }, { 2,  4,  0, -2,  0,  0 },
        { 2,  0,  0,  2,  0,  0 }, { 2,  0,  2, -2,  0,  0 }, { 2,  0,  2,  0,  1,  0 },
        { 2, -3,  3,  1,  0, -1 }, { 2,  0,  0,  0, -2,  0 }, { 2,  4,  0,  0,  2,  0 },
        { 2,  4, -2,  0,  1,  0 }, { 2,  0,  0,  0,  0,  2 }, { 2,  1,  0,  1,  2,  0 },
        { 2,  0, -2,  0, -2,  0 }, { 2, -2,  1,  0,  0,  1 }, { 2, -2,  1,  2,  0, -1 },
        { 2, -1,  1, -1,  0,  1 }, { 2,  5,  0, -1,  0,  0 }, { 2,  1, -3,  1,  0,  1 },
        { 2, -2, -1,  2,  0,  1 }, { 2,  3,  0, -1,  2,  0 }, { 2,  1, -2,  1, -1,  0 },
        { 2,  5,  0, -1,  1,  0 }, { 2, -4,  0,  4,  0,  0 }, { 2, -3,  2,  1, -1,  0 },
        { 2, -2,  1,  1,  0,  0 }, { 2,  4,  0, -2,  1,  0 }, { 2,  0,  0,  2,  1,  0 },
        { 2, -5,  4,  1,  0,  0 }, { 2,  0,  2,  0,  2,  0 }, { 2, -1,  2,  1,  0,  0 },
        { 2,  5, -2, -1,  0,  0 }, { 2,  1, -1,  0,  0,  0 }, { 2,  2, -2,  0,  0,  2 },
        { 2, -5,  2,  3,  0,  0 }, { 2, -1, -2,  1, -2,  0 }, { 2, -3,  5, -1,  0, -1 },
        { 2, -1,  0,  0,  0,  1 }, { 2, -2,  0,  0, -2,  0 }, { 2,  0, -1,  1,  0,  0 },
        { 2, -3,  1,  1,  0,  1 }, { 2,  3,  0, -1, -1,  0 }, { 2,  1,  0,  1, -1,  0 },
        { 2, -1,  2,  1,  1,  0 }, { 2,  0, -3,  2,  0,  1 }, { 2,  1, -1, -1,  0,  1 },
        { 2, -3,  0,  3, -1,  0 }, { 2,  0, -2,  2, -1,  0 }, { 2, -4,  3,  2,  0, -1 },
        { 2, -1,  0,  1, -2,  0 }, { 2,  5,  0, -1,  2,  0 }, { 2, -4,  5,  0,  0, -1 },
        { 2, -2,  4,  0,  0, -2 }, { 2, -1,  0,  1,  0,  2 }, { 2, -2, -2,  4,  0,  0 },
        { 2,  3, -2, -1, -1,  0 }, { 2, -2,  5, -2,  0, -1 }, { 2,  0, -1,  0, -1,  1 },
        { 2,  5, -2, -1,  1,  0 }, { 1,  1,  0,  0,  0,  0 }, { 1, -1,  0,  0,  0,  0 },
        { 1,  1, -2,  0,  0,  0 }, { 1, -2,  0,  1,  0,  0 }, { 1,  1,  0,  0,  1,  0 },
        { 1, -1,  0,  0, -1,  0 }, { 1,  2,  0, -1,  0,  0 }, { 1,  0,  0,  1,  0,  0 },
        { 1,  3,  0,  0,  0,  0 }, { 1, -2,  2, -1,  0,  0 }, { 1, -2,  0,  1, -1,  0 },
        { 1, -3,  2,  0,  0,  0 }, { 1,  0,  0, -1,  0,  0 }, { 1,  1,  0,  0, -1,  0 },
        { 1,  3,  0,  0,  1,  0 }, { 1,  1, -3,  0,  0,  1 }, { 1, -3,  0,  2,  0,  0 },
        { 1,  1,  2,  0,  0,  0 }, { 1,  0,  0,  1,  1,  0 }, { 1,  2,  0, -1,  1,  0 },
        { 1,  0,  2, -1,  0,  0 }, { 1,  2, -2,  1,  0,  0 }, { 1,  3, -2,  0,  0,  0 },
        { 1, -1,  2,  0,  0,  0 }, { 1,  1,  1,  0,  0, -1 }, { 1,  1, -1,  0,  0,  1 },
        { 1,  4,  0, -1,  0,  0 }, { 1, -4,  2,  1,  0,  0 }, { 1,  0, -2,  1,  0,  0 },
        { 1, -2,  2, -1, -1,  0 }, { 1,  3,  0, -2,  0,  0 }, { 1, -1,  0,  2,  0,  0 },
        { 1, -1,  0,  0, -2,  0 }, { 1,  3,  0,  0,  2,  0 }, { 1, -3,  2,  0, -1,  0 },
        { 1,  4,  0, -1,  1,  0 }, { 1,  0,  0, -1, -1,  0 }, { 1,  1, -2,  0, -1,  0 },
        { 1, -3,  0,  2, -1,  0 }, { 1,  1,  0,  0,  2,  0 }, { 1,  1, -1,  0,  0, -1 },
        { 1, -1, -1,  0,  0,  1 }, { 1,  0,  2, -1,  1,  0 }, { 1, -1,  1,  0,  0, -1 },
        { 1, -1, -2,  2,  0,  0 }, { 1,  2, -2,  1,  1,  0 }, { 1, -4,  0,  3,  0,  0 },
        { 1, -1,  2,  0,  1,  0 }, { 1,  3, -2,  0,  1,  0 }, { 1,  2,  0, -1, -1,  0 },
        { 1,  0,  0,  1, -1,  0 }, { 1, -2,  2,  1,  0,  0 }, { 1,  4, -2, -1,  0,  0 },
        { 1, -3,  3,  0,  0, -1 }, { 1, -2,  1,  1,  0, -1 }, { 1, -2,  3, -1,  0, -1 },
        { 1,  0, -2,  1, -1,  0 }, { 1, -2, -1,  1,  0,  1 }, { 1,  4, -2,  1,  0,  0 },
        { 1, -4,  4, -1,  0,  0 }, { 1, -4,  2,  1, -1,  0 }, { 1,  5, -2,  0,  0,  0 },
        { 1,  3,  0, -2,  1,  0 }, { 1, -5,  2,  2,  0,  0 }, { 1,  2,  0,  1,  0,  0 },
        { 1,  1,  3,  0,  0, -1 }, { 1, -2,  0,  1, -2,  0 }, { 1,  4,  0, -1,  2,  0 },
        { 1,  1, -4,  0,  0,  2 }, { 1,  5,  0, -2,  0,  0 }, { 1, -1,  0,  2,  1,  0 },
        { 1, -2,  1,  0,  0,  0 }, { 1,  4, -2,  1,  1,  0 }, { 1, -3,  4, -2,  0,  0 },
        { 1, -1,  3,  0,  0, -1 }, { 1,  3, -3,  0,  0,  1 }, { 1,  5, -2,  0,  1,  0 },
        { 1,  1,  2,  0,  1,  0 }, { 1,  2,  0,  1,  1,  0 }, { 1, -5,  4,  0,  0,  0 },
        { 1, -2,  0, -1, -2,  0 }, { 1,  5,  0, -2,  1,  0 }, { 1,  1,  2, -2,  0,  0 },
        { 1,  1, -2,  2,  0,  0 }, { 1, -2,  2,  1,  1,  0 }, { 1,  0,  3, -1,  0, -1 },
        { 1,  2, -3,  1,  0,  1 }, { 1, -2, -2,  3,  0,  0 }, { 1, -1,  2, -2,  0,  0 },
        { 1, -4,  3,  1,  0, -1 }, { 1, -4,  0,  3, -1,  0 }, { 1, -1, -2,  2, -1,  0 },
        { 1, -2,  0,  3,  0,  0 }, { 1,  4,  0, -3,  0,  0 }, { 1,  0,  1,  1,  0, -1 },
        { 1,  2, -1, -1,  0,  1 }, { 1,  2, -2,  1, -1,  0 }, { 1,  0,  0, -1, -2,  0 },
        { 1,  2,  0,  1,  2,  0 }, { 1,  2, -2, -1, -1,  0 }, { 1,  0,  0,  1,  2,  0 },
        { 1,  0,  1,  0,  0,  0 }, { 1,  2, -1,  0,  0,  0 }, { 1,  0,  2, -1, -1,  0 },
        { 1, -1, -2,  0, -2,  0 }, { 1, -3,  1,  0,  0,  1 }, { 1,  3, -2,  0, -1,  0 },
        { 1, -1, -1,  0, -1,  1 }, { 1,  4, -2, -1,  1,  0 }, { 1,  2,  1, -1,  0, -1 },
        { 1,  0, -1,  1,  0,  1 }, { 1, -2,  4, -1,  0,  0 }, { 1,  4, -4,  1,  0,  0 },
        { 1, -3,  1,  2,  0, -1 }, { 1, -3,  3,  0, -1, -1 }, { 1,  1,  2,  0,  2,  0 },
        { 1,  1, -2,  0, -2,  0 }, { 1,  3,  0,  0,  3,  0 }, { 1, -1,  2,  0, -1,  0 },
        { 1, -2,  1, -1,  0,  1 }, { 1,  0, -3,  1,  0,  1 }, { 1, -3, -1,  2,  0,  1 },
        { 1,  2,  0, -1,  2,  0 }, { 1,  6, -2, -1,  0,  0 }, { 1,  2,  2, -1,  0,  0 },
        { 1, -1,  1,  0, -1, -1 }, { 1, -2,  3, -1, -1, -1 }, { 1, -1,  0,  0,  0,  2 },
        { 1, -5,  0,  4,  0,  0 }, { 1,  1,  0,  0,  0, -2 }, { 1, -2,  1,  1, -1, -1 },
        { 1,  1, -1,  0,  1,  1 }, { 1,  1,  2,  0,  0, -2 }, { 1, -3,  1,  1,  0,  0 },
        { 1, -4,  4, -1, -1,  0 }, { 1,  1,  0, -2, -1,  0 }, { 1, -2, -1,  1, -1,  1 },
        { 1, -3,  2,  2,  0,  0 }, { 1,  5, -2, -2,  0,  0 }, { 1,  3, -4,  2,  0,  0 },
        { 1,  1, -2,  0,  0,  2 }, { 1, -1,  4, -2,  0,  0 }, { 1,  2,  2, -1,  1,  0 },
        { 1, -5,  2,  2, -1,  0 }, { 1,  1, -3,  0, -1,  1 }, { 1,  1,  1,  0,  1, -1 },
        { 1,  6, -2, -1,  1,  0 }, { 1, -2,  2, -1, -2,  0 }, { 1,  4, -2,  1,  2,  0 },
        { 1, -6,  4,  1,  0,  0 }, { 1,  5, -4,  0,  0,  0 }, { 1, -3,  4,  0,  0,  0 },
        { 1,  1,  2, -2,  1,  0 }, { 1, -2,  1,  0, -1,  0 }, { 0,  2,  0,  0,  0,  0 },
        { 0,  1,  0, -1,  0,  0 }, { 0,  0,  2,  0,  0,  0 }, { 0,  0,  0,  0,  1,  0 },
        { 0,  2,  0,  0,  1,  0 }, { 0,  3,  0, -1,  0,  0 }, { 0,  1, -2,  1,  0,  0 },
        { 0,  2, -2,  0,  0,  0 }, { 0,  3,  0, -1,  1,  0 }, { 0,  0,  1,  0,  0, -1 },
        { 0,  2,  0, -2,  0,  0 }, { 0,  2,  0,  0,  2,  0 }, { 0,  3, -2,  1,  0,  0 },
        { 0,  1,  0, -1, -1,  0 }, { 0,  1,  0, -1,  1,  0 }, { 0,  4, -2,  0,  0,  0 },
        { 0,  1,  0,  1,  0,  0 }, { 0,  0,  3,  0,  0, -1 }, { 0,  4,  0, -2,  0,  0 },
        { 0,  3, -2,  1,  1,  0 }, { 0,  3, -2, -1,  0,  0 }, { 0,  4, -2,  0,  1,  0 },
        { 0,  0,  2,  0,  1,  0 }, { 0,  1,  0,  1,  1,  0 }, { 0,  4,  0, -2,  1,  0 },
        { 0,  3,  0, -1,  2,  0 }, { 0,  5, -2, -1,  0,  0 }, { 0,  1,  2, -1,  0,  0 },
        { 0,  1, -2,  1, -1,  0 }, { 0,  1, -2,  1,  1,  0 }, { 0,  2, -2,  0, -1,  0 },
        { 0,  2, -3,  0,  0,  1 }, { 0,  2, -2,  0,  1,  0 }, { 0,  0,  2, -2,  0,  0 },
        { 0,  1, -3,  1,  0,  1 }, { 0,  0,  0,  0,  2,  0 }, { 0,  0,  1,  0,  0,  1 },
        { 0,  1,  2, -1,  1,  0 }, { 0,  3,  0, -3,  0,  0 }, { 0,  2,  1,  0,  0, -1 },
        { 0,  1, -1, -1,  0,  1 }, { 0,  1,  0,  1,  2,  0 }, { 0,  5, -2, -1,  1,  0 },
        { 0,  2, -1,  0,  0,  1 }, { 0,  2,  2, -2,  0,  0 }, { 0,  1, -1,  0,  0,  0 },
        { 0,  5,  0, -3,  0,  0 }, { 0,  2,  0, -2,  1,  0 }, { 0,  1,  1, -1,  0, -1 },
        { 0,  3, -4,  1,  0,  0 }, { 0,  0,  2,  0,  2,  0 }, { 0,  2,  0, -2, -1,  0 },
        { 0,  4, -3,  0,  0,  1 }, { 0,  3, -1, -1,  0,  1 }, { 0,  0,  2,  0,  0, -2 },
        { 0,  3, -3,  1,  0,  1 }, { 0,  2, -4,  2,  0,  0 }, { 0,  4, -2, -2,  0,  0 },
        { 0,  3,  1, -1,  0, -1 }, { 0,  5, -4,  1,  0,  0 }, { 0,  3, -2, -1, -1,  0 },
        { 0,  3, -2,  1,  2,  0 }, { 0,  4, -4,  0,  0,  0 }, { 0,  6, -2, -2,  0,  0 },
        { 0,  5,  0, -3,  1,  0 }, { 0,  4, -2,  0,  2,  0 }, { 0,  2,  2, -2,  1,  0 },
        { 0,  0,  4,  0,  0, -2 }, { 0,  3, -1,  0,  0,  0 }, { 0,  3, -3, -1,  0,  1 },
        { 0,  4,  0, -2,  2,  0 }, { 0,  1, -2, -1, -1,  0 }, { 0,  2, -1,  0,  0, -1 },
        { 0,  4, -4,  2,  0,  0 }, { 0,  2,  1,  0,  1, -1 }, { 0,  3, -2, -1,  1,  0 },
        { 0,  4, -3,  0,  1,  1 }, { 0,  2,  0,  0,  3,  0 }, { 0,  6, -4,  0,  0,  0 }
    };
    // CHECKSTYLE: resume NoWhitespaceAfter check

    /** Cartwright-Edden amplitudes for all tides. */
    private static final Map<Tide, Double> CARTWRIGHT_EDDEN_AMPLITUDE_MAP;

    static {
        CARTWRIGHT_EDDEN_AMPLITUDE_MAP = new HashMap<>(CARTWRIGHT_EDDEN_AMPLITUDE.length);
        for (int i = 0; i < CARTWRIGHT_EDDEN_AMPLITUDE.length; ++i) {
            CARTWRIGHT_EDDEN_AMPLITUDE_MAP.put(new Tide(DOODSON_ARGUMENTS[i][0], DOODSON_ARGUMENTS[i][1], DOODSON_ARGUMENTS[i][2],
                                                        DOODSON_ARGUMENTS[i][3], DOODSON_ARGUMENTS[i][4], DOODSON_ARGUMENTS[i][5]),
                                               CARTWRIGHT_EDDEN_AMPLITUDE[i]);
        }
    }

    /** Earth shape. */
    private final OneAxisEllipsoid earth;

    /** Data for main tides, for which we have ocean loading coefficients. */
    private final MainTideData[][] mainTides;

    /** Simple constructor.
     * @param earth Earth shape
     * @param coefficients coefficients for the considered site
     * @see OceanLoadingCoefficientsBLQFactory
     */
    public OceanLoading(final OneAxisEllipsoid earth, final OceanLoadingCoefficients coefficients) {

        this.earth = earth;

        // set up complex admittances, scaled to Cartwright-Edden amplitudes
        // and grouped by species (0: long period, 1: diurnal, 2: semi-diurnal)
        mainTides  = new MainTideData[coefficients.getNbSpecies()][];
        for (int i = 0; i < mainTides.length; ++i) {
            mainTides[i] = new MainTideData[coefficients.getNbTides(i)];
            for (int j = 0; j < mainTides[i].length; ++j) {
                final double amplitude = CARTWRIGHT_EDDEN_AMPLITUDE_MAP.get(coefficients.getTide(i, j));
                mainTides[i][j] = new MainTideData(coefficients, i, j, FastMath.abs(amplitude));
            }
        }

    }

    /** {@inheritDoc} */
    @Override
    public Vector3D displacement(final BodiesElements elements, final Frame earthFrame,
                                 final Vector3D referencePoint) {

        // allocate arrays for each species splines
        final UnivariateFunction[] realZSpline      = new UnivariateFunction[mainTides.length];
        final UnivariateFunction[] imaginaryZSpline = new UnivariateFunction[mainTides.length];
        final UnivariateFunction[] realWSpline      = new UnivariateFunction[mainTides.length];
        final UnivariateFunction[] imaginaryWSpline = new UnivariateFunction[mainTides.length];
        final UnivariateFunction[] realSSpline      = new UnivariateFunction[mainTides.length];
        final UnivariateFunction[] imaginarySSpline = new UnivariateFunction[mainTides.length];

        // prepare splines for each species
        for (int i = 0; i < mainTides.length; ++i) {

            // compute current rates
            final double[] rates = new double[mainTides[i].length];
            for (int j = 0; j < rates.length; ++j) {
                rates[j] = mainTides[i][j].tide.getRate(elements);
            }

            // set up splines for the current rates
            realZSpline[i]      = spline(rates, mainTides[i], d -> d.realZ);
            imaginaryZSpline[i] = spline(rates, mainTides[i], d -> d.imaginaryZ);
            realWSpline[i]      = spline(rates, mainTides[i], d -> d.realW);
            imaginaryWSpline[i] = spline(rates, mainTides[i], d -> d.imaginaryW);
            realSSpline[i]      = spline(rates, mainTides[i], d -> d.realS);
            imaginarySSpline[i] = spline(rates, mainTides[i], d -> d.imaginaryS);

        }

        // evaluate all harmonics using interpolated admittances
        double dz = 0;
        double dw = 0;
        double ds = 0;
        for (final Map.Entry<Tide, Double> entry : CARTWRIGHT_EDDEN_AMPLITUDE_MAP.entrySet()) {

            final Tide   tide      = entry.getKey();
            final double amplitude = entry.getValue();
            final int    i         = tide.getTauMultiplier();
            final double rate      = tide.getRate(elements);

            // apply splines for the current rate of this tide
            final double rZ = realZSpline[i].value(rate);
            final double iZ = imaginaryZSpline[i].value(rate);
            final double rW = realWSpline[i].value(rate);
            final double iW = imaginaryWSpline[i].value(rate);
            final double rS = realSSpline[i].value(rate);
            final double iS = imaginarySSpline[i].value(rate);

            // phase for the current tide, including corrections
            final double correction;
            if (tide.getTauMultiplier() == 0) {
                correction = FastMath.PI;
            } else if (tide.getTauMultiplier() == 1) {
                correction = 0.5 * FastMath.PI;
            } else {
                correction = 0.0;
            }
            final double phase = tide.getPhase(elements) + correction;

            dz += amplitude * FastMath.hypot(rZ, iZ) * FastMath.cos(phase + FastMath.atan2(iZ, rZ));
            dw += amplitude * FastMath.hypot(rW, iW) * FastMath.cos(phase + FastMath.atan2(iW, rW));
            ds += amplitude * FastMath.hypot(rS, iS) * FastMath.cos(phase + FastMath.atan2(iS, rS));

        }

        // convert to proper frame
        final GeodeticPoint gp = earth.transform(referencePoint, earthFrame, elements.getDate());
        return new Vector3D(dz, gp.getZenith(),
                            dw, gp.getWest(),
                            ds, gp.getSouth());

    }

    /** Get a spline function for interpolating between main tide data.
     * @param rates rates for the tides species
     * @param data data for the tides species
     * @param selector data selector
     * @return spline function for interpolating the selected data
     */
    private UnivariateFunction spline(final double[] rates, final MainTideData[] data,
                                      final Function<MainTideData, Double> selector) {
        final double[] y = new double[data.length];
        for (int i = 0; i < y.length; ++i) {
            y[i] = selector.apply(data[i]);
        }
        final PolynomialSplineFunction psf = new SplineInterpolator().interpolate(rates, y);

        // as per HARDISP program EVAL subroutine, if spline evaluation is outside of range,
        // the closest value is used. This occurs for example for long period tides.
        // The main tides have rates 0.0821°/h, 0.5444°/h and 1.0980°/h. However,
        // tide 55565 has rate 0.0022°/h, which is below the min rate and tide 75565 has
        // rate 1.1002°/h, which is above max rate
        final double[] knots          = psf.getKnots();
        final double   minRate        = knots[0];
        final double   valueAtMinRate = psf.value(minRate);
        final double   maxRate        = knots[knots.length - 1];
        final double   valueAtMaxRate = psf.value(maxRate);
        return t -> (t < minRate) ? valueAtMinRate : (t > maxRate) ? valueAtMaxRate : psf.value(t);

    }

    /** Container for main tide data. */
    private static class MainTideData {

        /** Tide for which we have ocean loading coefficients. */
        private final Tide tide;

        /** Scaled real part of admittance along zenith axis. */
        private final double realZ;

        /** Scaled imaginary part of admittance along zenith axis. */
        private final double imaginaryZ;

        /** Scaled real part of admittance along west axis. */
        private final double realW;

        /** Scaled imaginary part of admittance along west axis. */
        private final double imaginaryW;

        /** Scaled real part of admittance along south axis. */
        private final double realS;

        /** Scaled imaginary part of admittance south axis. */
        private final double imaginaryS;

        /** Simple constructor.
         * @param coefficients coefficients for the considered site
         * @param i tide species
         * @param j tide index in the species
         * @param absAmplitude absolute value of the Cartwright-Edden amplitude of the tide
         */
        MainTideData(final OceanLoadingCoefficients coefficients, final int i, final int j, final double absAmplitude) {
            // Sine and Cosine of difference angles
            final SinCos scZenith = FastMath.sinCos(coefficients.getZenithPhase(i, j));
            final SinCos scWest   = FastMath.sinCos(coefficients.getWestPhase(i, j));
            final SinCos scSouth  = FastMath.sinCos(coefficients.getSouthPhase(i, j));
            // Initialize attributes
            tide       = coefficients.getTide(i, j);
            realZ      = coefficients.getZenithAmplitude(i, j) * scZenith.cos() / absAmplitude;
            imaginaryZ = coefficients.getZenithAmplitude(i, j) * scZenith.sin() / absAmplitude;
            realW      = coefficients.getWestAmplitude(i, j)   * scWest.cos()   / absAmplitude;
            imaginaryW = coefficients.getWestAmplitude(i, j)   * scWest.sin()   / absAmplitude;
            realS      = coefficients.getSouthAmplitude(i, j)  * scSouth.cos()  / absAmplitude;
            imaginaryS = coefficients.getSouthAmplitude(i, j)  * scSouth.sin()  / absAmplitude;
        }

    }

}

