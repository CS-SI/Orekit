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
package org.orekit.frames;

import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.RotationConvention;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.Precision;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
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
    private static final long serialVersionUID = -1900615992141291146L;

    /** Enumerate for predefined Helmert transformations. */
    public enum Predefined {

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
        ITRF_2008_TO_ITRF_2005(2000,
                                -2.0, -0.9,   -4.7,  0.00,  0.00,  0.00,
                                 0.3,  0.0,    0.0,  0.00,  0.00,  0.00),

        /** Transformation from ITRF 2008 To ITRF 2000. */
        ITRF_2008_TO_ITRF_2000(2000,
                                -1.9, -1.7,  -10.5,  0.00,  0.00,  0.00,
                                 0.1,  0.1,   -1.8,  0.00,  0.00,  0.00),

        /** Transformation from ITRF 2008 To ITRF 97. */
        ITRF_2008_TO_ITRF_97  (2000,
                                 4.8,  2.6,  -33.2,  0.00,  0.00,  0.06,
                                 0.1, -0.5,   -3.2,  0.00,  0.00,  0.02),

        /** Transformation from ITRF 2008 To ITRF 96. */
        ITRF_2008_TO_ITRF_96  (2000,
                                 4.8,  2.6,  -33.2,  0.00,  0.00,  0.06,
                                 0.1, -0.5,   -3.2,  0.00,  0.00,  0.02),

        /** Transformation from ITRF 2008 To ITRF 94. */
        ITRF_2008_TO_ITRF_94  (2000,
                                 4.8,  2.6,  -33.2,  0.00,  0.00,  0.06,
                                 0.1, -0.5,   -3.2,  0.00,  0.00,  0.02),

        /** Transformation from ITRF 2008 To ITRF 93. */
        ITRF_2008_TO_ITRF_93  (2000,
                               -24.0,  2.4,  -38.6, -1.71, -1.48, -0.30,
                                -2.8, -0.1,   -2.4, -0.11, -0.19,  0.07),

        /** Transformation from ITRF 2008 To ITRF 92. */
        ITRF_2008_TO_ITRF_92  (2000,
                                12.8,  4.6,  -41.2,  0.00,  0.00,  0.06,
                                 0.1, -0.5,   -3.2,  0.00,  0.00,  0.02),

        /** Transformation from ITRF 2008 To ITRF 91. */
        ITRF_2008_TO_ITRF_91  (2000,
                                24.8, 18.6,  -47.2,  0.00,  0.00,  0.06,
                                 0.1, -0.5,   -3.2,  0.00,  0.00,  0.02),

        /** Transformation from ITRF 2008 To ITRF 90. */
        ITRF_2008_TO_ITRF_90  (2000,
                                22.8, 14.6,  -63.2,  0.00,  0.00,  0.06,
                                 0.1, -0.5,   -3.2,  0.00,  0.00,  0.02),

        /** Transformation from ITRF 2008 To ITRF 89. */
        ITRF_2008_TO_ITRF_89  (2000,
                                27.8, 38.6, -101.2,  0.00,  0.00,  0.06,
                                 0.1, -0.5,   -3.2,  0.00,  0.00,  0.02),

        /** Transformation from ITRF 2008 To ITRF 88. */
        ITRF_2008_TO_ITRF_88  (2000,
                                22.8,  2.6, -125.2,  0.10,  0.00,  0.06,
                                 0.1, -0.5,   -3.2,  0.00,  0.00,  0.02);

        /** Transformation. */
        private final HelmertTransformation transformation;

        /** Simple constructor.
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
        Predefined(final int refYear,
                           final double t1, final double t2, final double t3,
                           final double r1, final double r2, final double r3,
                           final double t1Dot, final double t2Dot, final double t3Dot,
                           final double r1Dot, final double r2Dot, final double r3Dot) {
            transformation =
                    new HelmertTransformation(new AbsoluteDate(refYear, 1, 1, 12, 0, 0, TimeScalesFactory.getTT()),
                                              t1, t2, t3, r1, r2, r3, t1Dot, t2Dot, t3Dot, r1Dot, r2Dot, r3Dot);
        }

        /** Create an ITRF frame by transforming another ITRF frame.
         * @param parent parent ITRF frame
         * @param name name of the frame to create
         * @return new ITRF frame
         */
        public Frame createTransformedITRF(final Frame parent, final String name) {
            return new Frame(parent, transformation, name);
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

    /** Get the reference epoch of the transform.
     * @return reference epoch of the transform
     */
    public AbsoluteDate getEpoch() {
        return epoch;
    }

    /** Compute the transform at some date.
     * @param date date at which the transform is desired
     * @return computed transform at specified date
     */
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

}
