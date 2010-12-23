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
package org.orekit.bodies;

import org.apache.commons.math.geometry.Vector3D;
import org.apache.commons.math.util.FastMath;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;

/** Factory class for IAU poles.
 * <p>The pole models provided here come from <a
 * href="http://astrogeology.usgs.gov/Projects/WGCCRE/constants/iau2000_table1.html">
 * table 1</a> of the IAU/IAG Working Group on Cartographic Coordinates
 * and Rotational Elements of the Planets and Satellites (WGCCRE) site. The
 * constants have been retrieved on 2010-12-18. They seem to have last been updated
 * around 2005.
 *</p>
 * @author Luc Maisonobe
 * @version $Revision$ $Date$
 */
class IAUPoleFactory {

    /** Private constructor.
     * <p>This class is a utility class, it should neither have a public
     * nor a default constructor. This private constructor prevents
     * the compiler from generating one automatically.</p>
     */
    private IAUPoleFactory() {
    }

    /** Get an IAU pole.
     * @param body body for which the pole is requested
     * @return IAU pole for the body, or dummy EME2000 aligned pole
     * for barycenters
     */
    public static IAUPole getIAUPole(final JPLEphemeridesLoader.EphemerisType body) {
        switch (body) {
        case SUN:
            return new IAUPole() {

                /** Serializable UID. */
                private static final long serialVersionUID = 5715331729495237139L;

                /** {@inheritDoc }*/
                public Vector3D getPole(final AbsoluteDate date) {
                    return new Vector3D(FastMath.toRadians(286.13),
                                        FastMath.toRadians(63.87));
                }

                /** {@inheritDoc }*/
                public double getPrimeMeridianAngle(final AbsoluteDate date) {
                    return FastMath.toRadians(84.10 + 14.1844000 * d(date));
                }

            };
        case MERCURY:
            return new IAUPole() {

                /** Serializable UID. */
                private static final long serialVersionUID = -5769710119654037007L;

                /** {@inheritDoc }*/
                public Vector3D getPole(final AbsoluteDate date) {
                    final double t = t(date);
                    return new Vector3D(FastMath.toRadians(281.01 - 0.033 * t),
                                        FastMath.toRadians( 61.45 - 0.005 * t));
                }

                /** {@inheritDoc }*/
                public double getPrimeMeridianAngle(final AbsoluteDate date) {
                    return FastMath.toRadians(329.548 + 6.1385025 * d(date));
                }

            };
        case VENUS:
            return new IAUPole() {

                /** Serializable UID. */
                private static final long serialVersionUID = 7030506277976648896L;

                /** {@inheritDoc }*/
                public Vector3D getPole(final AbsoluteDate date) {
                    return new Vector3D(FastMath.toRadians(272.76),
                                        FastMath.toRadians(67.16));
                }

                /** {@inheritDoc }*/
                public double getPrimeMeridianAngle(final AbsoluteDate date) {
                    return FastMath.toRadians(160.20 - 1.4813688 * d(date));
                }

            };
        case EARTH:
            return new IAUPole() {

                /** Serializable UID. */
                private static final long serialVersionUID = 6912325697192667056L;

                /** {@inheritDoc }*/
                public Vector3D getPole(final AbsoluteDate date) {
                    final double t = t(date);
                    return new Vector3D(FastMath.toRadians( 0.00 - 0.641 * t),
                                        FastMath.toRadians(90.00 - 0.557 * t));
                }

                /** {@inheritDoc }*/
                public double getPrimeMeridianAngle(final AbsoluteDate date) {
                    return FastMath.toRadians(190.147 + 360.9856235 * d(date));
                }

            };
        case MOON:
            return new IAUPole() {


                /** Serializable UID. */
                private static final long serialVersionUID = -1310155975084976571L;

                /** {@inheritDoc }*/
                public Vector3D getPole(final AbsoluteDate date) {
                    final double[] e = computeEi(date);
                    final double t = t(date);
                    return new Vector3D(FastMath.toRadians(269.9949 + 0.0031 * t       - 3.8787 * FastMath.sin(e[0]) -
                                                           0.1204 * FastMath.sin(e[1]) + 0.0700 * FastMath.sin(e[2]) -
                                                           0.0172 * FastMath.sin(e[3]) + 0.0072 * FastMath.sin(e[5]) -
                                                           0.0052 * FastMath.sin(e[9]) + 0.0043 * FastMath.sin(e[12])),
                                        FastMath.toRadians( 66.5392 + 0.0130 * t       + 1.5419 * FastMath.cos(e[0]) +
                                                           0.0239 * FastMath.cos(e[1]) - 0.0278 * FastMath.cos(e[2]) +
                                                           0.0068 * FastMath.cos(e[3]) - 0.0029 * FastMath.cos(e[5]) +
                                                           0.0009 * FastMath.cos(e[6]) + 0.0008 * FastMath.cos(e[9]) -
                                                           0.0009 * FastMath.cos(e[12])));
                }

                /** {@inheritDoc }*/
                public double getPrimeMeridianAngle(final AbsoluteDate date) {
                    final double[] e = computeEi(date);
                    final double d = d(date);
                    return FastMath.toRadians(38.3213 + (13.17635815 - 1.4e-12 * d) * d + 3.5610 * FastMath.sin(e[0]) +
                                              0.1208 * FastMath.sin(e[1])  - 0.0642 * FastMath.sin(e[2])  +
                                              0.0158 * FastMath.sin(e[3])  + 0.0252 * FastMath.sin(e[4])  -
                                              0.0066 * FastMath.sin(e[5])  - 0.0047 * FastMath.sin(e[6])  -
                                              0.0046 * FastMath.sin(e[7])  + 0.0028 * FastMath.sin(e[8])  +
                                              0.0052 * FastMath.sin(e[9])  + 0.0040 * FastMath.sin(e[10]) +
                                              0.0019 * FastMath.sin(e[11]) - 0.0044 * FastMath.sin(e[12]));
                }

                /** Compute the Moon angles E<sub>i</sub>.
                 * @param date date
                 * @return array of Moon angles, with E<sub>i</sub> stored at index i-1
                 */
                private double[] computeEi(final AbsoluteDate date) {
                    final double d = d(date);
                    return new double[] {
                        FastMath.toRadians(125.045 -  0.0529921 * d), // E1
                        FastMath.toRadians(250.089 -  0.1059842 * d), // E2
                        FastMath.toRadians(260.008 + 13.0120009 * d), // E3
                        FastMath.toRadians(176.625 + 13.3407154 * d), // E4
                        FastMath.toRadians(357.529 +  0.9856003 * d), // E5
                        FastMath.toRadians(311.589 + 26.4057084 * d), // E6
                        FastMath.toRadians(134.963 + 13.0649930 * d), // E7
                        FastMath.toRadians(276.617 +  0.3287146 * d), // E8
                        FastMath.toRadians( 34.226 +  1.7484877 * d), // E9
                        FastMath.toRadians( 15.134 -  0.1589763 * d), // E10
                        FastMath.toRadians(119.743 +  0.0036096 * d), // E11
                        FastMath.toRadians(239.961 +  0.1643573 * d), // E12
                        FastMath.toRadians( 25.053 + 12.9590088 * d)  // E13
                    };
                }

            };
        case MARS:
            return new IAUPole() {

                /** Serializable UID. */
                private static final long serialVersionUID = 1471983418540015411L;

                /** {@inheritDoc }*/
                public Vector3D getPole(final AbsoluteDate date) {
                    final double t = t(date);
                    return new Vector3D(FastMath.toRadians(317.68143 - 0.1061 * t),
                                        FastMath.toRadians( 52.88650 - 0.0609 * t));
                }

                /** {@inheritDoc }*/
                public double getPrimeMeridianAngle(final AbsoluteDate date) {
                    return FastMath.toRadians(176.630 + 350.89198226 * d(date));
                }

            };
        case JUPITER:
            return new IAUPole() {

                /** Serializable UID. */
                private static final long serialVersionUID = 6959753758673537524L;

                /** {@inheritDoc }*/
                public Vector3D getPole(final AbsoluteDate date) {
                    final double t = t(date);
                    return new Vector3D(FastMath.toRadians(268.05 - 0.009 * t),
                                        FastMath.toRadians( 64.49 + 0.003 * t));
                }

                /** {@inheritDoc }*/
                public double getPrimeMeridianAngle(final AbsoluteDate date) {
                    return FastMath.toRadians(284.95 + 870.5366420 * d(date));
                }

            };
        case SATURN:
            return new IAUPole() {

                /** Serializable UID. */
                private static final long serialVersionUID = -1082211873912149774L;

                /** {@inheritDoc }*/
                public Vector3D getPole(final AbsoluteDate date) {
                    final double t = t(date);
                    return new Vector3D(FastMath.toRadians(40.589 - 0.036 * t),
                                        FastMath.toRadians(83.537 - 0.004 * t));
                }

                /** {@inheritDoc }*/
                public double getPrimeMeridianAngle(final AbsoluteDate date) {
                    return FastMath.toRadians(38.90 + 810.7939024 * d(date));
                }

            };
        case URANUS:
            return new IAUPole() {

                /** Serializable UID. */
                private static final long serialVersionUID = 362792230470085154L;

                /** {@inheritDoc }*/
                public Vector3D getPole(final AbsoluteDate date) {
                    return new Vector3D(FastMath.toRadians(257.311),
                                        FastMath.toRadians(-15.175));
                }

                /** {@inheritDoc }*/
                public double getPrimeMeridianAngle(final AbsoluteDate date) {
                    return FastMath.toRadians(203.81 - 501.1600928 * d(date));
                }

            };
        case NEPTUNE:
            return new IAUPole() {

                /** Serializable UID. */
                private static final long serialVersionUID = 560614555734665287L;

                /** {@inheritDoc }*/
                public Vector3D getPole(final AbsoluteDate date) {
                    final double n = FastMath.toRadians(357.85 + 52.316 * t(date));
                    return new Vector3D(FastMath.toRadians(299.36 + 0.70 * FastMath.sin(n)),
                                        FastMath.toRadians( 43.46 - 0.51 * FastMath.cos(n)));
                }

                /** {@inheritDoc }*/
                public double getPrimeMeridianAngle(final AbsoluteDate date) {
                    final double n = FastMath.toRadians(357.85 + 52.316 * t(date));
                    return FastMath.toRadians(253.18 + 536.3128492 * d(date) - 0.48 * FastMath.sin(n));
                }

            };
        case PLUTO:
            return new IAUPole() {

                /** Serializable UID. */
                private static final long serialVersionUID = -1277113129327018062L;

                /** {@inheritDoc }*/
                public Vector3D getPole(final AbsoluteDate date) {
                    return new Vector3D(FastMath.toRadians(313.02),
                                        FastMath.toRadians(9.09));
                }

                /** {@inheritDoc }*/
                public double getPrimeMeridianAngle(final AbsoluteDate date) {
                    return FastMath.toRadians(236.77 - 56.3623195 * d(date));
                }

            };
        default:
            return new EME2000Aligned();
        }
    }

    /** Compute the interval in julian centuries from standard epoch.
     * @param date date
     * @return interval between date and standard epoch in julian centuries 
     */
    private static double t(final AbsoluteDate date) {
        return date.durationFrom(AbsoluteDate.J2000_EPOCH) / Constants.JULIAN_CENTURY;
    }

    /** Compute the interval in julian days from standard epoch.
     * @param date date
     * @return interval between date and standard epoch in julian days 
     */
    private static double d(final AbsoluteDate date) {
        return date.durationFrom(AbsoluteDate.J2000_EPOCH) / Constants.JULIAN_DAY;
    }

    /** Default IAUPole implementation for barycenters.
     * <p>
     * This implementation defines directions such that the inertially oriented and body
     * oriented frames are identical and aligned with EME2000. It is used for example
     * to define the ICRF.
     * </p>
     */
    private static class EME2000Aligned implements IAUPole {

        /** Serializable UID. */
        private static final long serialVersionUID = 4148478144525077641L;

        /** {@inheritDoc }*/
        public Vector3D getPole(final AbsoluteDate date) {
            return Vector3D.PLUS_K;
        }

        /** {@inheritDoc }*/
        public double getPrimeMeridianAngle(final AbsoluteDate date) {
            return 0;
        }

    }

}
