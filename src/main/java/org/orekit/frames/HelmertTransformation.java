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
package org.orekit.frames;

import java.util.Optional;
import java.util.stream.Stream;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.geometry.euclidean.threed.FieldRotation;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.RotationConvention;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.Precision;
import org.orekit.annotation.DefaultDataContext;
import org.orekit.data.DataContext;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateTimeComponents;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.utils.Constants;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.PVCoordinates;


/** Transformation class for geodetic systems.
 *
 * <p>The Helmert transformation is mainly used to convert between various
 * realizations of geodetic frames, for example in the ITRF family.</p>
 *
 * <p>The original Helmert transformation is a 14 parameters transform that
 * includes translation, velocity, rotation, rotation rate and scale factor.
 * The scale factor is useful for coordinates near Earth surface, but it
 * cannot be extended to outer space as it would correspond to a non-unitary
 * transform. Therefore, the scale factor is <em>not</em> used here.
 *
 * <p>Instances of this class are guaranteed to be immutable.</p>
 *
 * @author Luc Maisonobe
 * @since 5.1
 */
public class HelmertTransformation implements TransformProvider {

    /** serializable UID. */
    private static final long serialVersionUID = 20220419L;

    /** Enumerate for predefined Helmert transformations. */
    public enum Predefined {

        // see https://itrf.ign.fr/docs/solutions/itrf2020/Transfo-ITRF2020_TRFs.txt
        // SOLUTION         Tx       Ty       Tz        D        Rx        Ry        Rz      EPOCH
        // UNITS----------> mm       mm       mm       ppb       .001"     .001"     .001"
        //                  .        .        .         .        .         .         .
        //        RATES     Tx       Ty       Tz        D        Rx        Ry        Rz
        // UNITS----------> mm/y     mm/y     mm/y     ppb/y    .001"/y   .001"/y   .001"/y
        // -----------------------------------------------------------------------------------------
        //   ITRF2014       -1.4     -0.9      1.4     -0.42      0.00      0.00      0.00    2015.0
        //        rates      0.0     -0.1      0.2      0.00      0.00      0.00      0.00
        //   ITRF2008        0.2      1.0      3.3     -0.29      0.00      0.00      0.00    2015.0
        //        rates      0.0     -0.1      0.1      0.03      0.00      0.00      0.00
        //   ITRF2005        2.7      0.1     -1.4      0.65      0.00      0.00      0.00    2015.0
        //        rates      0.3     -0.1      0.1      0.03      0.00      0.00      0.00
        //   ITRF2000       -0.2      0.8    -34.2      2.25      0.00      0.00      0.00    2015.0
        //        rates      0.1      0.0     -1.7      0.11      0.00      0.00      0.00
        //   ITRF97          6.5     -3.9    -77.9      3.98      0.00      0.00      0.36    2015.0
        //        rates      0.1     -0.6     -3.1      0.12      0.00      0.00      0.02
        //   ITRF96          6.5     -3.9    -77.9      3.98      0.00      0.00      0.36    2015.0
        //        rates      0.1     -0.6     -3.1      0.12      0.00      0.00      0.02
        //   ITRF94          6.5     -3.9    -77.9      3.98      0.00      0.00      0.36    2015.0
        //        rates      0.1     -0.6     -3.1      0.12      0.00      0.00      0.02
        //   ITRF93        -65.8      1.9    -71.3      4.47     -3.36     -4.33      0.75    2015.0
        //        rates     -2.8     -0.2     -2.3      0.12     -0.11     -0.19      0.07
        //   ITRF92         14.5     -1.9    -85.9      3.27      0.00      0.00      0.36    2015.0
        //        rates      0.1     -0.6     -3.1      0.12      0.00      0.00      0.02
        //   ITRF91         26.5     12.1    -91.9      4.67      0.00      0.00      0.36    2015.0
        //        rates      0.1     -0.6     -3.1      0.12      0.00      0.00      0.02
        //   ITRF90         24.5      8.1   -107.9      4.97      0.00      0.00      0.36    2015.0
        //        rates      0.1     -0.6     -3.1      0.12      0.00      0.00      0.02
        //   ITRF89         29.5     32.1   -145.9      8.37      0.00      0.00      0.36    2015.0
        //        rates      0.1     -0.6     -3.1      0.12      0.00      0.00      0.02
        //   ITRF88         24.5     -3.9   -169.9     11.47      0.10      0.00      0.36    2015.0
        //        rates      0.1     -0.6     -3.1      0.12      0.00      0.00      0.02
        // _________________________________________________________________________________________

        /** Transformation from ITRF 2020 To ITRF 2014. */
        ITRF_2020_TO_ITRF_2014(ITRFVersion.ITRF_2020, ITRFVersion.ITRF_2014, 2015,
                               -1.4, -0.9,   1.4,  0.00,  0.00,  0.00,
                                0.0, -0.1,   0.2,  0.00,  0.00,  0.00),

        /** Transformation from ITRF 2020 To ITRF 2008. */
        ITRF_2020_TO_ITRF_2008(ITRFVersion.ITRF_2020, ITRFVersion.ITRF_2008, 2015,
                               0.2,  1.0,   3.3,  0.00,  0.00,  0.00,
                               0.0, -0.1,   0.1,  0.00,  0.00,  0.00),

        /** Transformation from ITRF 2020 To ITRF 2005. */
        ITRF_2020_TO_ITRF_2005(ITRFVersion.ITRF_2020, ITRFVersion.ITRF_2005, 2015,
                               2.7,  0.1,  -1.4,  0.00,  0.00,  0.00,
                               0.3, -0.1,   0.1,  0.00,  0.00,  0.00),

        /** Transformation from ITRF 2020 To ITRF 2000. */
        ITRF_2020_TO_ITRF_2000(ITRFVersion.ITRF_2020, ITRFVersion.ITRF_2000, 2015,
                               -0.2,  0.8, -34.2,  0.00,  0.00,  0.00,
                                0.1,  0.0,  -1.7,  0.00,  0.00,  0.00),

        /** Transformation from ITRF 2020 To ITRF 97. */
        ITRF_2020_TO_ITRF_1997(ITRFVersion.ITRF_2020, ITRFVersion.ITRF_1997, 2015,
                               6.5, -3.9, -77.9,  0.00,  0.00,  0.36,
                               0.1, -0.6,  -3.1,  0.00,  0.00,  0.02),

        /** Transformation from ITRF 2020 To ITRF 96. */
        ITRF_2020_TO_ITRF_1996(ITRFVersion.ITRF_2020, ITRFVersion.ITRF_1996, 2015,
                               6.5, -3.9, -77.9,  0.00,  0.00,  0.36,
                               0.1, -0.6,  -3.1,  0.00,  0.00,  0.02),

        /** Transformation from ITRF 2020 To ITRF 94. */
        ITRF_2020_TO_ITRF_1994(ITRFVersion.ITRF_2020, ITRFVersion.ITRF_1994, 2015,
                               6.5, -3.9, -77.9,  0.00,  0.00,  0.36,
                               0.1, -0.6,  -3.1,  0.00,  0.00,  0.02),

        /** Transformation from ITRF 2020 To ITRF 93. */
        ITRF_2020_TO_ITRF_1993(ITRFVersion.ITRF_2020, ITRFVersion.ITRF_1993, 2015,
                               -65.8,  1.9, -71.3, -3.36, -4.33,  0.75,
                                -2.8, -0.2,  -2.3, -0.11, -0.19,  0.07),

        /** Transformation from ITRF 2020 To ITRF 92. */
        ITRF_2020_TO_ITRF_1992(ITRFVersion.ITRF_2020, ITRFVersion.ITRF_1992, 2015,
                               14.5, -1.9, -85.9,  0.00,  0.00,  0.36,
                                0.1, -0.6,  -3.1,  0.00,  0.00,  0.02),

        /** Transformation from ITRF 2020 To ITRF 91. */
        ITRF_2020_TO_ITRF_1991(ITRFVersion.ITRF_2020, ITRFVersion.ITRF_1991, 2015,
                               26.5, 12.1, -91.9,  0.00,  0.00,  0.36,
                                0.1, -0.6,  -3.1,  0.00,  0.00,  0.02),

        /** Transformation from ITRF 2020 To ITRF 90. */
        ITRF_2020_TO_ITRF_1990(ITRFVersion.ITRF_2020, ITRFVersion.ITRF_1990, 2015,
                               24.5,  8.1, -107.9,  0.00,  0.00,  0.36,
                                0.1, -0.6,   -3.1,  0.00,  0.00,  0.02),

        /** Transformation from ITRF 2020 To ITRF 89. */
        ITRF_2020_TO_ITRF_1989(ITRFVersion.ITRF_2020, ITRFVersion.ITRF_1989, 2015,
                               29.5, 32.1, -145.9,  0.00,  0.00,  0.36,
                                0.1, -0.6,   -3.1,  0.00,  0.00,  0.02),

        /** Transformation from ITRF 2020 To ITRF 88. */
        ITRF_2020_TO_ITRF_1988(ITRFVersion.ITRF_2020, ITRFVersion.ITRF_1988, 2015,
                               24.5, -3.9, -169.9,  0.10,  0.00,  0.36,
                                0.1, -0.6,   -3.1,  0.00,  0.00,  0.02),

        // see http://itrf.ign.fr/doc_ITRF/Transfo-ITRF2014_ITRFs.txt
        // SOLUTION         Tx       Ty       Tz        D        Rx        Ry        Rz      EPOCH
        // UNITS----------> mm       mm       mm       ppb       .001"     .001"     .001"
        //                  .        .        .         .        .         .         .
        //        RATES     Tx       Ty       Tz        D        Rx        Ry        Rz
        // UNITS----------> mm/y     mm/y     mm/y     ppb/y    .001"/y   .001"/y   .001"/y
        // -----------------------------------------------------------------------------------------
        //   ITRF2008        1.6      1.9      2.4     -0.02      0.00      0.00      0.00    2010.0
        //        rates      0.0      0.0     -0.1      0.03      0.00      0.00      0.00
        //   ITRF2005        2.6      1.0     -2.3      0.92      0.00      0.00      0.00    2010.0
        //        rates      0.3      0.0     -0.1      0.03      0.00      0.00      0.00
        //   ITRF2000        0.7      1.2    -26.1      2.12      0.00      0.00      0.00    2010.0
        //        rates      0.1      0.1     -1.9      0.11      0.00      0.00      0.00
        //   ITRF97          7.4     -0.5    -62.8      3.80      0.00      0.00      0.26    2010.0
        //        rates      0.1     -0.5     -3.3      0.12      0.00      0.00      0.02
        //   ITRF96          7.4     -0.5    -62.8      3.80      0.00      0.00      0.26    2010.0
        //        rates      0.1     -0.5     -3.3      0.12      0.00      0.00      0.02
        //   ITRF94          7.4     -0.5    -62.8      3.80      0.00      0.00      0.26    2010.0
        //        rates      0.1     -0.5     -3.3      0.12      0.00      0.00      0.02
        //   ITRF93        -50.4      3.3    -60.2      4.29     -2.81     -3.38      0.40    2010.0
        //        rates     -2.8     -0.1     -2.5      0.12     -0.11     -0.19      0.07
        //   ITRF92         15.4      1.5    -70.8      3.09      0.00      0.00      0.26    2010.0
        //        rates      0.1     -0.5     -3.3      0.12      0.00      0.00      0.02
        //   ITRF91         27.4     15.5    -76.8      4.49      0.00      0.00      0.26    2010.0
        //        rates      0.1     -0.5     -3.3      0.12      0.00      0.00      0.02
        //   ITRF90         25.4     11.5    -92.8      4.79      0.00      0.00      0.26    2010.0
        //        rates      0.1     -0.5     -3.3      0.12      0.00      0.00      0.02
        //   ITRF89         30.4     35.5   -130.8      8.19      0.00      0.00      0.26    2010.0
        //        rates      0.1     -0.5     -3.3      0.12      0.00      0.00      0.02
        //   ITRF88         25.4     -0.5   -154.8     11.29      0.10      0.00      0.26    2010.0
        //        rates      0.1     -0.5     -3.3      0.12      0.00      0.00      0.02
        // _________________________________________________________________________________________

        /** Transformation from ITRF 2014 To ITRF 2008. */
        ITRF_2014_TO_ITRF_2008(ITRFVersion.ITRF_2014, ITRFVersion.ITRF_2008, 2010,
                                1.6, 1.9,     2.4, 0.00, 0.00, 0.00,
                                0.0, 0.0,    -0.1, 0.00, 0.00, 0.00),

        /** Transformation from ITRF 2014 To ITRF 2005. */
        ITRF_2014_TO_ITRF_2005(ITRFVersion.ITRF_2014, ITRFVersion.ITRF_2005, 2010,
                                2.6, 1.0,    -2.3, 0.00, 0.00, 0.00,
                                0.3, 0.0,    -0.1, 0.00, 0.00, 0.00),

        /** Transformation from ITRF 2014 To ITRF 2000. */
        ITRF_2014_TO_ITRF_2000(ITRFVersion.ITRF_2014, ITRFVersion.ITRF_2000, 2010,
                               0.7, 1.2,   -26.1, 0.00, 0.00, 0.00,
                               0.1, 0.1,    -1.9, 0.00, 0.00, 0.00),

        /** Transformation from ITRF 2014 To ITRF 97. */
        ITRF_2014_TO_ITRF_1997(ITRFVersion.ITRF_2014, ITRFVersion.ITRF_1997, 2010,
                               7.4, -0.5,  -62.8, 0.00, 0.00, 0.26,
                               0.1, -0.5,   -3.3, 0.00, 0.00, 0.02),

        /** Transformation from ITRF 2014 To ITRF 96. */
        ITRF_2014_TO_ITRF_1996(ITRFVersion.ITRF_2014, ITRFVersion.ITRF_1996, 2010,
                               7.4, -0.5,  -62.8, 0.00, 0.00, 0.26,
                               0.1, -0.5,  -3.3, 0.00, 0.00, 0.02),

        /** Transformation from ITRF 2014 To ITRF 94. */
        ITRF_2014_TO_ITRF_1994(ITRFVersion.ITRF_2014, ITRFVersion.ITRF_1994, 2010,
                               7.4, -0.5,  -62.8, 0.00, 0.00, 0.26,
                               0.1, -0.5,   -3.3, 0.00, 0.00, 0.02),

        /** Transformation from ITRF 2014 To ITRF 93. */
        ITRF_2014_TO_ITRF_1993(ITRFVersion.ITRF_2014, ITRFVersion.ITRF_1993, 2010,
                               -50.4,  3.3,  -60.2, -2.81, -3.38, 0.40,
                               -2.8, -0.1,   -2.5, -0.11, -0.19, 0.07),

        /** Transformation from ITRF 2014 To ITRF 92. */
        ITRF_2014_TO_ITRF_1992(ITRFVersion.ITRF_2014, ITRFVersion.ITRF_1992, 2010,
                               15.4,  1.5,  -70.8, 0.00, 0.00, 0.26,
                               0.1, -0.5,   -3.3, 0.00, 0.00, 0.02),

        /** Transformation from ITRF 2014 To ITRF 91. */
        ITRF_2014_TO_ITRF_1991(ITRFVersion.ITRF_2014, ITRFVersion.ITRF_1991, 2010,
                               27.4, 15.5,  -76.8, 0.00, 0.00, 0.26,
                               0.1, -0.5,   -3.3, 0.00, 0.00, 0.02),

        /** Transformation from ITRF 2014 To ITRF 90. */
        ITRF_2014_TO_ITRF_1990(ITRFVersion.ITRF_2014, ITRFVersion.ITRF_1990, 2010,
                               25.4, 11.5,  -92.8, 0.00, 0.00, 0.26,
                               0.1, -0.5,   -3.3, 0.00, 0.00, 0.02),

        /** Transformation from ITRF 2014 To ITRF 89. */
        ITRF_2014_TO_ITRF_1989(ITRFVersion.ITRF_2014, ITRFVersion.ITRF_1989, 2010,
                               30.4, 35.5, -130.8, 0.00, 0.00, 0.26,
                               0.1, -0.5,   -3.3, 0.00, 0.00, 0.02),

        /** Transformation from ITRF 2014 To ITRF 88. */
        ITRF_2014_TO_ITRF_1988(ITRFVersion.ITRF_2014, ITRFVersion.ITRF_1988, 2010,
                               25.4, -0.5, -154.8, 0.10, 0.00, 0.26,
                               0.1, -0.5,   -3.3, 0.00, 0.00, 0.02),

        // see http://itrf.ensg.ign.fr/doc_ITRF/Transfo-ITRF2008_ITRFs.txt
        // SOLUTION         Tx       Ty       Tz        D        Rx        Ry        Rz      EPOCH
        // UNITS----------> mm       mm       mm       ppb       .001"     .001"     .001"
        //                         .        .        .         .        .         .         .
        //        RATES     Tx       Ty       Tz        D        Rx        Ry        Rz
        // UNITS----------> mm/y     mm/y     mm/y     ppb/y    .001"/y   .001"/y   .001"/y
        // -----------------------------------------------------------------------------------------
        //   ITRF2005       -2.0     -0.9     -4.7      0.94      0.00      0.00      0.00    2000.0
        //        rates      0.3      0.0      0.0      0.00      0.00      0.00      0.00
        //   ITRF2000       -1.9     -1.7    -10.5      1.34      0.00      0.00      0.00    2000.0
        //        rates      0.1      0.1     -1.8      0.08      0.00      0.00      0.00
        //   ITRF97          4.8      2.6    -33.2      2.92      0.00      0.00      0.06    2000.0
        //        rates      0.1     -0.5     -3.2      0.09      0.00      0.00      0.02
        //   ITRF96          4.8      2.6    -33.2      2.92      0.00      0.00      0.06    2000.0
        //        rates      0.1     -0.5     -3.2      0.09      0.00      0.00      0.02
        //   ITRF94          4.8      2.6    -33.2      2.92      0.00      0.00      0.06    2000.0
        //        rates      0.1     -0.5     -3.2      0.09      0.00      0.00      0.02
        //   ITRF93        -24.0      2.4    -38.6      3.41     -1.71     -1.48     -0.30    2000.0
        //        rates     -2.8     -0.1     -2.4      0.09     -0.11     -0.19      0.07
        //   ITRF92         12.8      4.6    -41.2      2.21      0.00      0.00      0.06    2000.0
        //        rates      0.1     -0.5     -3.2      0.09      0.00      0.00      0.02
        //   ITRF91         24.8     18.6    -47.2      3.61      0.00      0.00      0.06    2000.0
        //        rates      0.1     -0.5     -3.2      0.09      0.00      0.00      0.02
        //   ITRF90         22.8     14.6    -63.2      3.91      0.00      0.00      0.06    2000.0
        //        rates      0.1     -0.5     -3.2      0.09      0.00      0.00      0.02
        //   ITRF89         27.8     38.6   -101.2      7.31      0.00      0.00      0.06    2000.0
        //        rates      0.1     -0.5     -3.2      0.09      0.00      0.00      0.02
        //   ITRF88         22.8      2.6   -125.2     10.41      0.10      0.00      0.06    2000.0
        //        rates      0.1     -0.5     -3.2      0.09      0.00      0.00      0.02
        // _________________________________________________________________________________________

        /** Transformation from ITRF 2008 To ITRF 2005. */
        ITRF_2008_TO_ITRF_2005(ITRFVersion.ITRF_2008, ITRFVersion.ITRF_2005, 2000,
                               -2.0, -0.9,   -4.7,  0.00,  0.00,  0.00,
                                0.3,  0.0,    0.0,  0.00,  0.00,  0.00),

        /** Transformation from ITRF 2008 To ITRF 2000. */
        ITRF_2008_TO_ITRF_2000(ITRFVersion.ITRF_2008, ITRFVersion.ITRF_2000, 2000,
                               -1.9, -1.7,  -10.5,  0.00,  0.00,  0.00,
                                0.1,  0.1,   -1.8,  0.00,  0.00,  0.00),

        /** Transformation from ITRF 2008 To ITRF 97. */
        ITRF_2008_TO_ITRF_1997(ITRFVersion.ITRF_2008, ITRFVersion.ITRF_1997, 2000,
                               4.8,  2.6,  -33.2,  0.00,  0.00,  0.06,
                               0.1, -0.5,   -3.2,  0.00,  0.00,  0.02),

        /** Transformation from ITRF 2008 To ITRF 96. */
        ITRF_2008_TO_ITRF_1996(ITRFVersion.ITRF_2008, ITRFVersion.ITRF_1996, 2000,
                               4.8,  2.6,  -33.2,  0.00,  0.00,  0.06,
                               0.1, -0.5,   -3.2,  0.00,  0.00,  0.02),

        /** Transformation from ITRF 2008 To ITRF 94. */
        ITRF_2008_TO_ITRF_1994(ITRFVersion.ITRF_2008, ITRFVersion.ITRF_1994, 2000,
                               4.8,  2.6,  -33.2,  0.00,  0.00,  0.06,
                               0.1, -0.5,   -3.2,  0.00,  0.00,  0.02),

        /** Transformation from ITRF 2008 To ITRF 93. */
        ITRF_2008_TO_ITRF_1993(ITRFVersion.ITRF_2008, ITRFVersion.ITRF_1993, 2000,
                               -24.0,  2.4,  -38.6, -1.71, -1.48, -0.30,
                               -2.8, -0.1,   -2.4, -0.11, -0.19,  0.07),

        /** Transformation from ITRF 2008 To ITRF 92. */
        ITRF_2008_TO_ITRF_1992(ITRFVersion.ITRF_2008, ITRFVersion.ITRF_1992, 2000,
                               12.8,  4.6,  -41.2,  0.00,  0.00,  0.06,
                               0.1, -0.5,   -3.2,  0.00,  0.00,  0.02),

        /** Transformation from ITRF 2008 To ITRF 91. */
        ITRF_2008_TO_ITRF_1991(ITRFVersion.ITRF_2008, ITRFVersion.ITRF_1991, 2000,
                               24.8, 18.6,  -47.2,  0.00,  0.00,  0.06,
                               0.1, -0.5,   -3.2,  0.00,  0.00,  0.02),

        /** Transformation from ITRF 2008 To ITRF 90. */
        ITRF_2008_TO_ITRF_1990(ITRFVersion.ITRF_2008, ITRFVersion.ITRF_1990, 2000,
                               22.8, 14.6,  -63.2,  0.00,  0.00,  0.06,
                               0.1, -0.5,   -3.2,  0.00,  0.00,  0.02),

        /** Transformation from ITRF 2008 To ITRF 89. */
        ITRF_2008_TO_ITRF_1989(ITRFVersion.ITRF_2008, ITRFVersion.ITRF_1989, 2000,
                               27.8, 38.6, -101.2,  0.00,  0.00,  0.06,
                               0.1, -0.5,   -3.2,  0.00,  0.00,  0.02),

        /** Transformation from ITRF 2008 To ITRF 88. */
        ITRF_2008_TO_ITRF_1988(ITRFVersion.ITRF_2008, ITRFVersion.ITRF_1988, 2000,
                               22.8,  2.6, -125.2,  0.10,  0.00,  0.06,
                               0.1, -0.5,   -3.2,  0.00,  0.00,  0.02);

        /** Origin ITRF. */
        private final ITRFVersion origin;

        /** Destination ITRF. */
        private final ITRFVersion destination;

        /** Transformation. */
        private final transient HelmertTransformationWithoutTimeScale transformation;

        /** Simple constructor.
         * @param origin origin ITRF
         * @param destination destination ITRF
         * @param refYear reference year for the epoch of the transform
         * @param t1 translation parameter along X axis (BEWARE, this is in mm)
         * @param t2 translation parameter along Y axis (BEWARE, this is in mm)
         * @param t3 translation parameter along Z axis (BEWARE, this is in mm)
         * @param r1 rotation parameter around X axis (BEWARE, this is in mas)
         * @param r2 rotation parameter around Y axis (BEWARE, this is in mas)
         * @param r3 rotation parameter around Z axis (BEWARE, this is in mas)
         * @param t1Dot rate of translation parameter along X axis (BEWARE, this is in mm/y)
         * @param t2Dot rate of translation parameter along Y axis (BEWARE, this is in mm/y)
         * @param t3Dot rate of translation parameter along Z axis (BEWARE, this is in mm/y)
         * @param r1Dot rate of rotation parameter around X axis (BEWARE, this is in mas/y)
         * @param r2Dot rate of rotation parameter around Y axis (BEWARE, this is in mas/y)
         * @param r3Dot rate of rotation parameter around Z axis (BEWARE, this is in mas/y)
         */
        Predefined(final ITRFVersion origin, final ITRFVersion destination, final int refYear,
                   final double t1, final double t2, final double t3,
                   final double r1, final double r2, final double r3,
                   final double t1Dot, final double t2Dot, final double t3Dot,
                   final double r1Dot, final double r2Dot, final double r3Dot) {
            this.origin         = origin;
            this.destination    = destination;
            this.transformation =
                    new HelmertTransformationWithoutTimeScale(new DateTimeComponents(refYear, 1, 1, 12, 0, 0),
                                              t1, t2, t3, r1, r2, r3, t1Dot, t2Dot, t3Dot, r1Dot, r2Dot, r3Dot);
        }

        /** Get the origin ITRF.
         * @return origin ITRF
         * @since 9.2
         */
        public ITRFVersion getOrigin() {
            return origin;
        }

        /** Get the destination ITRF.
         * @return destination ITRF
         * @since 9.2
         */
        public ITRFVersion getDestination() {
            return destination;
        }

        /** Get the underlying {@link HelmertTransformation}.
         *
         * <p>This method uses the {@link DataContext#getDefault() default data context}.
         *
         * @return underlying {@link HelmertTransformation}
         * @since 9.2
         * @see #getTransformation(TimeScale)
         */
        @DefaultDataContext
        public HelmertTransformation getTransformation() {
            return getTransformation(DataContext.getDefault().getTimeScales().getTT());
        }

        /** Get the underlying {@link HelmertTransformation}.
         * @return underlying {@link HelmertTransformation}
         * @param tt TT time scale.
         * @since 10.1
         */
        public HelmertTransformation getTransformation(final TimeScale tt) {
            return transformation.withTimeScale(tt);
        }

        /** Create an ITRF frame by transforming another ITRF frame.
         *
         * <p>This method uses the {@link DataContext#getDefault() default data context}.
         *
         * @param parent parent ITRF frame
         * @param name name of the frame to create
         * @return new ITRF frame
         * @see #createTransformedITRF(Frame, String, TimeScale)
         */
        @DefaultDataContext
        public Frame createTransformedITRF(final Frame parent, final String name) {
            return createTransformedITRF(parent, name,
                    DataContext.getDefault().getTimeScales().getTT());
        }

        /** Create an ITRF frame by transforming another ITRF frame.
         * @param parent parent ITRF frame
         * @param name name of the frame to create
         * @param tt TT time scale.
         * @return new ITRF frame
         * @since 10.1
         */
        public Frame createTransformedITRF(final Frame parent,
                                           final String name,
                                           final TimeScale tt) {
            return new Frame(parent, getTransformation(tt), name);
        }

        /** Select a predefined transform between two years.
         * @param origin origin year
         * @param destination destination year
         * @return predefined transform from origin to destination, or null if no such predefined transform exist
         * @since 11.2
         */
        public static Predefined selectPredefined(final int origin, final int destination) {
            final Optional<HelmertTransformation.Predefined> optional =
                            Stream.
                            of(HelmertTransformation.Predefined.values()).
                            filter(p -> p.getOrigin().getYear() == origin && p.getDestination().getYear() == destination).
                            findFirst();
            return optional.isPresent() ? optional.get() : null;
        }

    }

    /**
     * A {@link HelmertTransformation} without reference to a {@link TimeScale}. This
     * class is needed to maintain compatibility with Orekit 10.0 since {@link Predefined}
     * is an enum and it had a reference to the TT time scale.
     */
    private static class HelmertTransformationWithoutTimeScale {

        /** Cartesian part of the transform. */
        private final PVCoordinates cartesian;

        /** Global rotation vector (applying rotation is done by computing cross product). */
        private final Vector3D rotationVector;

        /** First time derivative of the rotation (norm representing angular rate). */
        private final Vector3D rotationRate;

        /** Reference epoch of the transform. */
        private final DateTimeComponents epoch;

        /** Build a transform from its primitive operations.
         * @param epoch reference epoch of the transform
         * @param t1 translation parameter along X axis (BEWARE, this is in mm)
         * @param t2 translation parameter along Y axis (BEWARE, this is in mm)
         * @param t3 translation parameter along Z axis (BEWARE, this is in mm)
         * @param r1 rotation parameter around X axis (BEWARE, this is in mas)
         * @param r2 rotation parameter around Y axis (BEWARE, this is in mas)
         * @param r3 rotation parameter around Z axis (BEWARE, this is in mas)
         * @param t1Dot rate of translation parameter along X axis (BEWARE, this is in mm/y)
         * @param t2Dot rate of translation parameter along Y axis (BEWARE, this is in mm/y)
         * @param t3Dot rate of translation parameter along Z axis (BEWARE, this is in mm/y)
         * @param r1Dot rate of rotation parameter around X axis (BEWARE, this is in mas/y)
         * @param r2Dot rate of rotation parameter around Y axis (BEWARE, this is in mas/y)
         * @param r3Dot rate of rotation parameter around Z axis (BEWARE, this is in mas/y)
         */
        HelmertTransformationWithoutTimeScale(
                final DateTimeComponents epoch,
                final double t1, final double t2, final double t3,
                final double r1, final double r2, final double r3,
                final double t1Dot, final double t2Dot, final double t3Dot,
                final double r1Dot, final double r2Dot, final double r3Dot) {

            // conversion parameters to SI units
            final double mmToM    = 1.0e-3;
            final double masToRad = 1.0e-3 * Constants.ARC_SECONDS_TO_RADIANS;

            this.epoch          = epoch;
            this.cartesian = new PVCoordinates(new Vector3D(t1 * mmToM,
                    t2 * mmToM,
                    t3 * mmToM),
                    new Vector3D(t1Dot * mmToM / Constants.JULIAN_YEAR,
                            t2Dot * mmToM / Constants.JULIAN_YEAR,
                            t3Dot * mmToM / Constants.JULIAN_YEAR));
            this.rotationVector = new Vector3D(r1 * masToRad,
                    r2 * masToRad,
                    r3 * masToRad);
            this.rotationRate   = new Vector3D(r1Dot * masToRad / Constants.JULIAN_YEAR,
                    r2Dot * masToRad / Constants.JULIAN_YEAR,
                    r3Dot * masToRad / Constants.JULIAN_YEAR);

        }

        /**
         * Get the Helmert transformation with reference to the given time scale.
         *
         * @param tt TT time scale.
         * @return Helmert transformation.
         */
        public HelmertTransformation withTimeScale(final TimeScale tt) {
            return new HelmertTransformation(cartesian, rotationVector, rotationRate,
                    new AbsoluteDate(epoch, tt));
        }

    }

    /** Cartesian part of the transform. */
    private final PVCoordinates cartesian;

    /** Global rotation vector (applying rotation is done by computing cross product). */
    private final Vector3D rotationVector;

    /** First time derivative of the rotation (norm representing angular rate). */
    private final Vector3D rotationRate;

    /** Reference epoch of the transform. */
    private final AbsoluteDate epoch;

    /** Build a transform from its primitive operations.
     * @param epoch reference epoch of the transform
     * @param t1 translation parameter along X axis (BEWARE, this is in mm)
     * @param t2 translation parameter along Y axis (BEWARE, this is in mm)
     * @param t3 translation parameter along Z axis (BEWARE, this is in mm)
     * @param r1 rotation parameter around X axis (BEWARE, this is in mas)
     * @param r2 rotation parameter around Y axis (BEWARE, this is in mas)
     * @param r3 rotation parameter around Z axis (BEWARE, this is in mas)
     * @param t1Dot rate of translation parameter along X axis (BEWARE, this is in mm/y)
     * @param t2Dot rate of translation parameter along Y axis (BEWARE, this is in mm/y)
     * @param t3Dot rate of translation parameter along Z axis (BEWARE, this is in mm/y)
     * @param r1Dot rate of rotation parameter around X axis (BEWARE, this is in mas/y)
     * @param r2Dot rate of rotation parameter around Y axis (BEWARE, this is in mas/y)
     * @param r3Dot rate of rotation parameter around Z axis (BEWARE, this is in mas/y)
     */
    public HelmertTransformation(final AbsoluteDate epoch,
                                 final double t1, final double t2, final double t3,
                                 final double r1, final double r2, final double r3,
                                 final double t1Dot, final double t2Dot, final double t3Dot,
                                 final double r1Dot, final double r2Dot, final double r3Dot) {

        // conversion parameters to SI units
        final double mmToM    = 1.0e-3;
        final double masToRad = 1.0e-3 * Constants.ARC_SECONDS_TO_RADIANS;

        this.epoch          = epoch;
        this.cartesian = new PVCoordinates(new Vector3D(t1 * mmToM,
                                                        t2 * mmToM,
                                                        t3 * mmToM),
                                           new Vector3D(t1Dot * mmToM / Constants.JULIAN_YEAR,
                                                        t2Dot * mmToM / Constants.JULIAN_YEAR,
                                                        t3Dot * mmToM / Constants.JULIAN_YEAR));
        this.rotationVector = new Vector3D(r1 * masToRad,
                                           r2 * masToRad,
                                           r3 * masToRad);
        this.rotationRate   = new Vector3D(r1Dot * masToRad / Constants.JULIAN_YEAR,
                                           r2Dot * masToRad / Constants.JULIAN_YEAR,
                                           r3Dot * masToRad / Constants.JULIAN_YEAR);

    }

    /**
     * Private constructor.
     *
     * @param cartesian      part of the transform.
     * @param rotationVector global rotation vector.
     * @param rotationRate   time derivative of rotation.
     * @param epoch          of transform.
     */
    private HelmertTransformation(final PVCoordinates cartesian,
                                  final Vector3D rotationVector,
                                  final Vector3D rotationRate,
                                  final AbsoluteDate epoch) {
        this.cartesian = cartesian;
        this.rotationVector = rotationVector;
        this.rotationRate = rotationRate;
        this.epoch = epoch;
    }

    /** Get the reference epoch of the transform.
     * @return reference epoch of the transform
     */
    public AbsoluteDate getEpoch() {
        return epoch;
    }

    /** {@inheritDoc} */
    @Override
    public Transform getTransform(final AbsoluteDate date) {

        // compute parameters evolution since reference epoch
        final double dt = date.durationFrom(epoch);
        final Vector3D dR = new Vector3D(1, rotationVector, dt, rotationRate);

        // build translation part
        final Transform translationTransform = new Transform(date, cartesian.shiftedBy(dt));

        // build rotation part
        final double angle = dR.getNorm();
        final Transform rotationTransform =
                new Transform(date,
                              (angle < Precision.SAFE_MIN) ?
                              Rotation.IDENTITY :
                              new Rotation(dR, angle, RotationConvention.VECTOR_OPERATOR),
                              rotationRate);

        // combine both parts
        return new Transform(date, translationTransform, rotationTransform);

    }

    /** {@inheritDoc} */
    @Override
    public StaticTransform getStaticTransform(final AbsoluteDate date) {

        // compute parameters evolution since reference epoch
        final double dt = date.durationFrom(epoch);
        final Vector3D dR = new Vector3D(1, rotationVector, dt, rotationRate);

        // build translation part
        final Vector3D translation = cartesian.shiftedBy(dt).getPosition();

        // build rotation part
        final double angle = dR.getNorm();
        final Rotation rotation = (angle < Precision.SAFE_MIN) ?
                Rotation.IDENTITY :
                new Rotation(dR, angle, RotationConvention.VECTOR_OPERATOR);

        // combine both parts
        return StaticTransform.of(date, translation, rotation);

    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> FieldTransform<T> getTransform(final FieldAbsoluteDate<T> date) {

        // compute parameters evolution since reference epoch
        final T dt = date.durationFrom(epoch);
        final FieldVector3D<T> dR = new FieldVector3D<>(date.getField().getOne(), rotationVector,
                                                        dt, rotationRate);

        // build translation part
        final FieldTransform<T> translationTransform =
                        new FieldTransform<>(date,
                                             new FieldPVCoordinates<>(date.getField(), cartesian).shiftedBy(dt));

        // build rotation part
        final T angle = dR.getNorm();
        final FieldTransform<T> rotationTransform =
                new FieldTransform<>(date,
                                    (angle.getReal() < Precision.SAFE_MIN) ?
                                     FieldRotation.getIdentity(date.getField()) :
                                    new FieldRotation<>(dR, angle, RotationConvention.VECTOR_OPERATOR),
                                    new FieldVector3D<>(date.getField(), rotationRate));

        // combine both parts
        return new FieldTransform<>(date, translationTransform, rotationTransform);

    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> FieldStaticTransform<T> getStaticTransform(final FieldAbsoluteDate<T> date) {

        // field
        final Field<T> field = date.getField();

        // compute parameters evolution since reference epoch
        final T dt = date.durationFrom(epoch);
        final FieldVector3D<T> dR = new FieldVector3D<>(field.getOne(), rotationVector, dt, rotationRate);

        // build translation part
        final FieldVector3D<T> translation = new FieldPVCoordinates<>(date.getField(), cartesian).shiftedBy(dt).getPosition();

        // build rotation part
        final T angle = dR.getNorm();
        final FieldRotation<T> rotation = (angle.getReal() < Precision.SAFE_MIN) ?
                FieldRotation.getIdentity(field) :
                new FieldRotation<>(dR, angle, RotationConvention.VECTOR_OPERATOR);

        // combine both parts
        return FieldStaticTransform.of(date, translation, rotation);

    }

}
