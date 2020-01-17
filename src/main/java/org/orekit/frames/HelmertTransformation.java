/* Copyright 2002-2020 CS Group
 * Licensed to CS Group (CS) under one or more
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

import org.hipparchus.RealFieldElement;
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
    private static final long serialVersionUID = -1900615992141291146L;

    /** Enumerate for predefined Helmert transformations. */
    public enum Predefined {

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
        ITRF_2014_TO_ITRF_97  (ITRFVersion.ITRF_2014, ITRFVersion.ITRF_97, 2010,
                                7.4, -0.5,  -62.8, 0.00, 0.00, 0.26,
                                0.1, -0.5,   -3.3, 0.00, 0.00, 0.02),

        /** Transformation from ITRF 2014 To ITRF 96. */
        ITRF_2014_TO_ITRF_96  (ITRFVersion.ITRF_2014, ITRFVersion.ITRF_96, 2010,
                                7.4, -0.5,  -62.8, 0.00, 0.00, 0.26,
                                0.1, -0.5,  -3.3, 0.00, 0.00, 0.02),

        /** Transformation from ITRF 2014 To ITRF 94. */
        ITRF_2014_TO_ITRF_94  (ITRFVersion.ITRF_2014, ITRFVersion.ITRF_94, 2010,
                                7.4, -0.5,  -62.8, 0.00, 0.00, 0.26,
                                0.1, -0.5,   -3.3, 0.00, 0.00, 0.02),

        /** Transformation from ITRF 2014 To ITRF 93. */
        ITRF_2014_TO_ITRF_93  (ITRFVersion.ITRF_2014, ITRFVersion.ITRF_93, 2010,
                              -50.4,  3.3,  -60.2, -2.81, -3.38, 0.40,
                               -2.8, -0.1,   -2.5, -0.11, -0.19, 0.07),

        /** Transformation from ITRF 2014 To ITRF 92. */
        ITRF_2014_TO_ITRF_92  (ITRFVersion.ITRF_2014, ITRFVersion.ITRF_92, 2010,
                               15.4,  1.5,  -70.8, 0.00, 0.00, 0.26,
                                0.1, -0.5,   -3.3, 0.00, 0.00, 0.02),

        /** Transformation from ITRF 2014 To ITRF 91. */
        ITRF_2014_TO_ITRF_91  (ITRFVersion.ITRF_2014, ITRFVersion.ITRF_91, 2010,
                               27.4, 15.5,  -76.8, 0.00, 0.00, 0.26,
                                0.1, -0.5,   -3.3, 0.00, 0.00, 0.02),

        /** Transformation from ITRF 2014 To ITRF 90. */
        ITRF_2014_TO_ITRF_90  (ITRFVersion.ITRF_2014, ITRFVersion.ITRF_90, 2010,
                               25.4, 11.5,  -92.8, 0.00, 0.00, 0.26,
                                0.1, -0.5,   -3.3, 0.00, 0.00, 0.02),

        /** Transformation from ITRF 2014 To ITRF 89. */
        ITRF_2014_TO_ITRF_89  (ITRFVersion.ITRF_2014, ITRFVersion.ITRF_89, 2010,
                               30.4, 35.5, -130.8, 0.00, 0.00, 0.26,
                                0.1, -0.5,   -3.3, 0.00, 0.00, 0.02),

        /** Transformation from ITRF 2014 To ITRF 88. */
        ITRF_2014_TO_ITRF_88  (ITRFVersion.ITRF_2014, ITRFVersion.ITRF_88, 2010,
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
        ITRF_2008_TO_ITRF_97  (ITRFVersion.ITRF_2008, ITRFVersion.ITRF_97, 2000,
                                4.8,  2.6,  -33.2,  0.00,  0.00,  0.06,
                                0.1, -0.5,   -3.2,  0.00,  0.00,  0.02),

        /** Transformation from ITRF 2008 To ITRF 96. */
        ITRF_2008_TO_ITRF_96  (ITRFVersion.ITRF_2008, ITRFVersion.ITRF_96, 2000,
                                4.8,  2.6,  -33.2,  0.00,  0.00,  0.06,
                                0.1, -0.5,   -3.2,  0.00,  0.00,  0.02),

        /** Transformation from ITRF 2008 To ITRF 94. */
        ITRF_2008_TO_ITRF_94  (ITRFVersion.ITRF_2008, ITRFVersion.ITRF_94, 2000,
                                4.8,  2.6,  -33.2,  0.00,  0.00,  0.06,
                                0.1, -0.5,   -3.2,  0.00,  0.00,  0.02),

        /** Transformation from ITRF 2008 To ITRF 93. */
        ITRF_2008_TO_ITRF_93  (ITRFVersion.ITRF_2008, ITRFVersion.ITRF_93, 2000,
                              -24.0,  2.4,  -38.6, -1.71, -1.48, -0.30,
                               -2.8, -0.1,   -2.4, -0.11, -0.19,  0.07),

        /** Transformation from ITRF 2008 To ITRF 92. */
        ITRF_2008_TO_ITRF_92  (ITRFVersion.ITRF_2008, ITRFVersion.ITRF_92, 2000,
                               12.8,  4.6,  -41.2,  0.00,  0.00,  0.06,
                                0.1, -0.5,   -3.2,  0.00,  0.00,  0.02),

        /** Transformation from ITRF 2008 To ITRF 91. */
        ITRF_2008_TO_ITRF_91  (ITRFVersion.ITRF_2008, ITRFVersion.ITRF_91, 2000,
                               24.8, 18.6,  -47.2,  0.00,  0.00,  0.06,
                                0.1, -0.5,   -3.2,  0.00,  0.00,  0.02),

        /** Transformation from ITRF 2008 To ITRF 90. */
        ITRF_2008_TO_ITRF_90  (ITRFVersion.ITRF_2008, ITRFVersion.ITRF_90, 2000,
                               22.8, 14.6,  -63.2,  0.00,  0.00,  0.06,
                                0.1, -0.5,   -3.2,  0.00,  0.00,  0.02),

        /** Transformation from ITRF 2008 To ITRF 89. */
        ITRF_2008_TO_ITRF_89  (ITRFVersion.ITRF_2008, ITRFVersion.ITRF_89, 2000,
                               27.8, 38.6, -101.2,  0.00,  0.00,  0.06,
                                0.1, -0.5,   -3.2,  0.00,  0.00,  0.02),

        /** Transformation from ITRF 2008 To ITRF 88. */
        ITRF_2008_TO_ITRF_88  (ITRFVersion.ITRF_2008, ITRFVersion.ITRF_88, 2000,
                               22.8,  2.6, -125.2,  0.10,  0.00,  0.06,
                                0.1, -0.5,   -3.2,  0.00,  0.00,  0.02);

        /** Origin ITRF. */
        private final ITRFVersion origin;

        /** Destination ITRF. */
        private final ITRFVersion destination;

        /** Transformation. */
        private final HelmertTransformationWithoutTimeScale transformation;

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
    public <T extends RealFieldElement<T>> FieldTransform<T> getTransform(final FieldAbsoluteDate<T> date) {

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

}
