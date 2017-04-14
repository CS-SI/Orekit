/* Copyright 2002-2017 CS Systèmes d'Information
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
package org.orekit.bodies;

import org.hipparchus.RealFieldElement;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;

/** Enumerate for predefined IAU poles.
 * <p>The pole models provided here come from the <a
 * href="http://astropedia.astrogeology.usgs.gov/alfresco/d/d/workspace/SpacesStore/28fd9e81-1964-44d6-a58b-fbbf61e64e15/WGCCRE2009reprint.pdf">
 * 2009 report</a> and the <a href="http://astropedia.astrogeology.usgs.gov/alfresco/d/d/workspace/SpacesStore/04d348b0-eb2b-46a2-abe9-6effacb37763/WGCCRE-Erratum-2011reprint.pdf">
 * 2011 erratum</a> of the IAU/IAG Working Group on Cartographic Coordinates
 * and Rotational Elements of the Planets and Satellites (WGCCRE). Note that these value
 * differ from earliest reports (before 2005).
 *</p>
 * @author Luc Maisonobe
 * @since 9.0
 */
enum PredefinedIAUPoles implements IAUPole {

    /** IAU pole and prime meridian model for Sun. */
    SUN {

        /** Constant term of the prime meridian. */
        private static final double W0 = 84.176;

        /** Rate term of the prime meridian. */
        private static final double W_DOT = 14.1844000;

        /** Fixed pole. */
        private final Vector3D pole = new Vector3D(FastMath.toRadians(286.13),
                                                   FastMath.toRadians(63.87));

        /** {@inheritDoc} */
        public Vector3D getPole(final AbsoluteDate date) {
            return pole;
        }

        /** {@inheritDoc} */
        public <T extends RealFieldElement<T>> FieldVector3D<T> getPole(final FieldAbsoluteDate<T> date) {
            return new FieldVector3D<>(date.getField(), pole);
        }

        /** {@inheritDoc} */
        public double getPrimeMeridianAngle(final AbsoluteDate date) {
            return FastMath.toRadians(d(date) * W_DOT + W0);
        }

        /** {@inheritDoc} */
        public <T extends RealFieldElement<T>> T getPrimeMeridianAngle(final FieldAbsoluteDate<T> date) {
            return toRadians(d(date).multiply(W_DOT).add(W0));
        }

    },

    /** IAU pole and prime meridian model for Mercury. */
    MERCURY {

        /** Constant term of the right ascension of the pole. */
        private static final double ALPHA_0 = 281.0097;

        /** Rate term of the right ascension of the pole. */
        private static final double ALPHA_DOT = -0.0328;

        /** Constant term of the declination of the pole. */
        private static final double DELTA_0 = 61.4143;

        /** Rate term of the declination of the pole. */
        private static final double DELTA_DOT = -0.0049;

        /** Constant term of the prime meridian. */
        private static final double W_0 = 329.5469;

        /** Rate term of the prime meridian. */
        private static final double W_DOT = 6.1385025;

        /** M1 coefficient of the prime meridian. */
        private static final double M1_COEFF = 0.00993822;

        /** M2 coefficient of the prime meridian. */
        private static final double M2_COEFF = -0.00104581;

        /** M3 coefficient of the prime meridian. */
        private static final double M3_COEFF = -0.00010280;

        /** M4 coefficient of the prime meridian. */
        private static final double M4_COEFF = -0.00002364;

        /** M5 coefficient of the prime meridian. */
        private static final double M5_COEFF = -0.00000532;

        /** Constant term of the M1 angle. */
        private static final double M1_0   = 174.791086;

        /** Rate term of the M1 angle. */
        private static final double M1_DOT = 4.092335;

        /** Constant term of the M2 angle. */
        private static final double M2_0   = 349.582171;

        /** Rate term of the M1 angle. */
        private static final double M2_DOT = 8.184670;

        /** Constant term of the M3 angle. */
        private static final double M3_0   = 164.373257;

        /** Rate term of the M1 angle. */
        private static final double M3_DOT = 12.277005;

        /** Constant term of the M4 angle. */
        private static final double M4_0   = 339.164343;

        /** Rate term of the M1 angle. */
        private static final double M4_DOT = 16.369340;

        /** Constant term of the M5 angle. */
        private static final double M5_0   = 153.955429;

        /** Rate term of the M1 angle. */
        private static final double M5_DOT = 20.461675;

        /** {@inheritDoc} */
        public Vector3D getPole(final AbsoluteDate date) {
            final double t = t(date);
            return new Vector3D(FastMath.toRadians(t * ALPHA_DOT + ALPHA_0),
                                FastMath.toRadians(t * DELTA_DOT + DELTA_0));
        }

        /** {@inheritDoc} */
        public <T extends RealFieldElement<T>> FieldVector3D<T> getPole(final FieldAbsoluteDate<T> date) {
            final T t = t(date);
            return new FieldVector3D<>(toRadians(t.multiply(ALPHA_DOT).add(ALPHA_0)),
                                       toRadians(t.multiply(DELTA_DOT).add(DELTA_0)));
        }

        /** {@inheritDoc} */
        public double getPrimeMeridianAngle(final AbsoluteDate date) {
            final double d = d(date);
            return FastMath.toRadians(d(date) * W_DOT + W_0 +
                                      FastMath.sin(FastMath.toRadians(d * M1_DOT + M1_0)) * M1_COEFF +
                                      FastMath.sin(FastMath.toRadians(d * M2_DOT + M2_0)) * M2_COEFF +
                                      FastMath.sin(FastMath.toRadians(d * M3_DOT + M3_0)) * M3_COEFF +
                                      FastMath.sin(FastMath.toRadians(d * M4_DOT + M4_0)) * M4_COEFF +
                                      FastMath.sin(FastMath.toRadians(d * M5_DOT + M5_0)) * M5_COEFF);
        }

        /** {@inheritDoc} */
        public <T extends RealFieldElement<T>> T getPrimeMeridianAngle(final FieldAbsoluteDate<T> date) {
            final T d = d(date);
            return toRadians(d(date).multiply(W_DOT).add(W_0).
                             add(toRadians(d.multiply(M1_DOT).add(M1_0)).sin().multiply(M1_COEFF)).
                             add(toRadians(d.multiply(M2_DOT).add(M2_0)).sin().multiply(M2_COEFF)).
                             add(toRadians(d.multiply(M3_DOT).add(M3_0)).sin().multiply(M3_COEFF)).
                             add(toRadians(d.multiply(M4_DOT).add(M4_0)).sin().multiply(M4_COEFF)).
                             add(toRadians(d.multiply(M5_DOT).add(M5_0)).sin().multiply(M5_COEFF)));
        }

    },

    /** IAU pole and prime meridian model for Venus. */
    VENUS {

        /** Constant term of the prime meridian. */
        private static final double W_0 = 160.20;

        /** Rate term of the prime meridian. */
        private static final double W_DOT = -1.4813688;

        /** Fixed pole. */
        private final Vector3D pole = new Vector3D(FastMath.toRadians(272.76),
                                                   FastMath.toRadians(67.16));

        /** {@inheritDoc} */
        public Vector3D getPole(final AbsoluteDate date) {
            return pole;
        }

        /** {@inheritDoc} */
        public <T extends RealFieldElement<T>> FieldVector3D<T> getPole(final FieldAbsoluteDate<T> date) {
            return new FieldVector3D<>(date.getField(), pole);
        }

        /** {@inheritDoc} */
        public double getPrimeMeridianAngle(final AbsoluteDate date) {
            return FastMath.toRadians(d(date) * W_DOT + W_0);
        }

        /** {@inheritDoc} */
        public <T extends RealFieldElement<T>> T getPrimeMeridianAngle(final FieldAbsoluteDate<T> date) {
            return toRadians(d(date).multiply(W_DOT).add(W_0));
        }

    },

    /** IAU pole and prime meridian model for Earth. */
    EARTH {

        /** Constant term of the right ascension of the pole. */
        private static final double ALPHA_0 =  0.00;

        /** Rate term of the right ascension of the pole. */
        private static final double ALPHA_DOT = -0.641;

        /** Constant term of the declination of the pole. */
        private static final double DELTA_0 = 90.00;

        /** Rate term of the declination of the pole. */
        private static final double DELTA_DOT = -0.557;

        /** Constant term of the prime meridian. */
        private static final double W_0 = 190.147;

        /** Rate term of the prime meridian. */
        private static final double W_DOT = 360.9856235;

        /** {@inheritDoc} */
        public Vector3D getPole(final AbsoluteDate date) {
            final double t = t(date);
            return new Vector3D(FastMath.toRadians(t * ALPHA_DOT + ALPHA_0),
                                FastMath.toRadians(t * DELTA_DOT + DELTA_0));
        }

        /** {@inheritDoc} */
        public <T extends RealFieldElement<T>> FieldVector3D<T> getPole(final FieldAbsoluteDate<T> date) {
            final T t = t(date);
            return new FieldVector3D<>(toRadians(t.multiply(ALPHA_DOT).add(ALPHA_0)),
                                       toRadians(t.multiply(DELTA_DOT).add(DELTA_0)));
        }

        /** {@inheritDoc} */
        public double getPrimeMeridianAngle(final AbsoluteDate date) {
            return FastMath.toRadians(d(date) * W_DOT + W_0);
        }

        /** {@inheritDoc} */
        public <T extends RealFieldElement<T>> T getPrimeMeridianAngle(final FieldAbsoluteDate<T> date) {
            return toRadians(d(date).multiply(W_DOT).add(W_0));
        }

    },

    /** IAU pole and prime meridian model for the Moon. */
    MOON {

        /** Constant term of the right ascension of the pole. */
        private static final double ALPHA_0 = 269.9949;

        /** Rate term of the right ascension of the pole. */
        private static final double ALPHA_DOT = 0.0031;

        /** Constant term of the declination of the pole. */
        private static final double DELTA_0 = 66.5392;

        /** Rate term of the declination of the pole. */
        private static final double DELTA_DOT =  0.0130;

        /** Constant term of the prime meridian. */
        private static final double W_0 = 38.3213;

        /** Rate term of the prime meridian. */
        private static final double W_DOT = 13.17635815;

        /** Rate term of the prime meridian. */
        private static final double W_DOT_DOT = -1.4e-12;

        /** Constant term of the E1 angle. */
        private static final double E01_0    = 125.045;

        /** Rate term of the E1 angle. */
        private static final double E01_DOT  =  -0.0529921;

        /** Sine coefficient of the E1 angle. */
        private static final double E01_SIN  = -3.8787;

        /** Cosine coefficient of the E1 angle. */
        private static final double E01_COS  =  1.5419;

        /** Sine coefficient of the E1 angle, for the prime meridian. */
        private static final double E01_W_SIN =  3.5610;

        /** Constant term of the E2 angle. */
        private static final double E02_0    = 250.089;

        /** Rate term of the E2 angle. */
        private static final double E02_DOT  =  -0.1059842;

        /** Sine coefficient of the E2 angle. */
        private static final double E02_SIN  = -0.1204;

        /** Cosine coefficient of the E2 angle. */
        private static final double E02_COS  =  0.0239;

        /** Sine coefficient of the E2 angle, for the prime meridian. */
        private static final double E02_W_SIN =  0.1208;

        /** Constant term of the E3 angle. */
        private static final double E03_0    = 260.008;

        /** Rate term of the E3 angle. */
        private static final double E03_DOT  =  13.0120009;

        /** Sine coefficient of the E3 angle. */
        private static final double E03_SIN  =  0.0700;

        /** Cosine coefficient of the E3 angle. */
        private static final double E03_COS  = -0.0278;

        /** Sine coefficient of the E3 angle, for the prime meridian. */
        private static final double E03_W_SIN = -0.0642;

        /** Constant term of the E4 angle. */
        private static final double E04_0    = 176.625;

        /** Rate term of the E4 angle. */
        private static final double E04_DOT  =  13.3407154;

        /** Sine coefficient of the E4 angle. */
        private static final double E04_SIN  = -0.0172;

        /** Cosine coefficient of the E4 angle. */
        private static final double E04_COS  =  0.0068;

        /** Sine coefficient of the E4 angle, for the prime meridian. */
        private static final double E04_W_SIN =  0.0158;

        /** Constant term of the E5 angle. */
        private static final double E05_0    = 357.529;

        /** Rate term of the E5 angle. */
        private static final double E05_DOT  =   0.9856003;

        /** Sine coefficient of the E5 angle, for the prime meridian. */
        private static final double E05_W_SIN =  0.0252;

        /** Constant term of the E6 angle. */
        private static final double E06_0    = 311.589;

        /** Rate term of the E6 angle. */
        private static final double E06_DOT  =  26.4057084;

        /** Sine coefficient of the E6 angle. */
        private static final double E06_SIN  = 0.0072;

        /** Cosine coefficient of the E6 angle. */
        private static final double E06_COS  = -0.0029;

        /** Sine coefficient of the E6 angle, for the prime meridian. */
        private static final double E06_W_SIN = -0.0066;

        /** Constant term of the E7 angle. */
        private static final double E07_0    = 134.963;

        /** Rate term of the E7 angle. */
        private static final double E07_DOT  =  13.0649930;

        /** Cosine coefficient of the E7 angle. */
        private static final double E07_COS  =  0.0009;

        /** Sine coefficient of the E7 angle, for the prime meridian. */
        private static final double E07_W_SIN = -0.0047;

        /** Constant term of the E8 angle. */
        private static final double E08_0    = 276.617;

        /** Rate term of the E8 angle. */
        private static final double E08_DOT  =   0.3287146;

        /** Sine coefficient of the E8 angle, for the prime meridian. */
        private static final double E08_W_SIN = -0.0046;

        /** Constant term of the E9 angle. */
        private static final double E09_0    =  34.226;

        /** Rate term of the E9 angle. */
        private static final double E09_DOT  =   1.7484877;

        /** Sine coefficient of the E9 angle, for the prime meridian. */
        private static final double E09_W_SIN =  0.0028;

        /** Constant term of the E10 angle. */
        private static final double E10_0    =  15.134;

        /** Rate term of the E10 angle. */
        private static final double E10_DOT  =  -0.1589763;

        /** Sine coefficient of the E10 angle. */
        private static final double E10_SIN  = -0.0052;

        /** Cosine coefficient of the E10 angle. */
        private static final double E10_COS  = 0.0008;

        /** Sine coefficient of the E10 angle, for the prime meridian. */
        private static final double E10_W_SIN =  0.0052;

        /** Constant term of the E11 angle. */
        private static final double E11_0    = 119.743;

        /** Rate term of the E11 angle. */
        private static final double E11_DOT  =   0.0036096;

        /** Sine coefficient of the E11 angle, for the prime meridian. */
        private static final double E11_W_SIN =  0.0040;

        /** Constant term of the E12 angle. */
        private static final double E12_0    = 239.961;

        /** Rate term of the E12 angle. */
        private static final double E12_DOT  =   0.1643573;

        /** Sine coefficient of the E12 angle, for the prime meridian. */
        private static final double E12_W_SIN =  0.0019;

        /** Constant term of the E13 angle. */
        private static final double E13_0    =  25.053;

        /** Rate term of the E13 angle. */
        private static final double E13_DOT  =  12.9590088;

        /** Sine coefficient of the E13 angle. */
        private static final double E13_SIN  = 0.0043;

        /** Cosine coefficient of the E13 angle. */
        private static final double E13_COS  = -0.0009;

        /** Sine coefficient of the E13 angle, for the prime meridian. */
        private static final double E13_W_SIN = -0.0044;

        /** {@inheritDoc} */
        public Vector3D getPole(final AbsoluteDate date) {
            final double d = d(date);
            final double t = t(date);
            return new Vector3D(FastMath.toRadians(t * ALPHA_DOT + ALPHA_0 +
                                                   FastMath.sin(FastMath.toRadians(d * E01_DOT + E01_0)) * E01_SIN +
                                                   FastMath.sin(FastMath.toRadians(d * E02_DOT + E02_0)) * E02_SIN +
                                                   FastMath.sin(FastMath.toRadians(d * E03_DOT + E03_0)) * E03_SIN +
                                                   FastMath.sin(FastMath.toRadians(d * E04_DOT + E04_0)) * E04_SIN +
                                                   FastMath.sin(FastMath.toRadians(d * E06_DOT + E06_0)) * E06_SIN +
                                                   FastMath.sin(FastMath.toRadians(d * E10_DOT + E10_0)) * E10_SIN +
                                                   FastMath.sin(FastMath.toRadians(d * E13_DOT + E13_0)) * E13_SIN),
                                FastMath.toRadians(t * DELTA_DOT + DELTA_0 +
                                                   FastMath.cos(FastMath.toRadians(d * E01_DOT + E01_0)) * E01_COS +
                                                   FastMath.cos(FastMath.toRadians(d * E02_DOT + E02_0)) * E02_COS +
                                                   FastMath.cos(FastMath.toRadians(d * E03_DOT + E03_0)) * E03_COS +
                                                   FastMath.cos(FastMath.toRadians(d * E04_DOT + E04_0)) * E04_COS +
                                                   FastMath.cos(FastMath.toRadians(d * E06_DOT + E06_0)) * E06_COS +
                                                   FastMath.cos(FastMath.toRadians(d * E07_DOT + E07_0)) * E07_COS +
                                                   FastMath.cos(FastMath.toRadians(d * E10_DOT + E10_0)) * E10_COS +
                                                   FastMath.cos(FastMath.toRadians(d * E13_DOT + E13_0)) * E13_COS));
        }

        /** {@inheritDoc} */
        public <T extends RealFieldElement<T>> FieldVector3D<T> getPole(final FieldAbsoluteDate<T> date) {
            final T d = d(date);
            final T t = t(date);
            return new FieldVector3D<>(toRadians(t.multiply(ALPHA_DOT).add(ALPHA_0).
                                                 add(toRadians(d.multiply(E01_DOT).add(E01_0)).sin().multiply(E01_SIN)).
                                                 add(toRadians(d.multiply(E02_DOT).add(E02_0)).sin().multiply(E02_SIN)).
                                                 add(toRadians(d.multiply(E03_DOT).add(E03_0)).sin().multiply(E03_SIN)).
                                                 add(toRadians(d.multiply(E04_DOT).add(E04_0)).sin().multiply(E04_SIN)).
                                                 add(toRadians(d.multiply(E06_DOT).add(E06_0)).sin().multiply(E06_SIN)).
                                                 add(toRadians(d.multiply(E10_DOT).add(E10_0)).sin().multiply(E10_SIN)).
                                                 add(toRadians(d.multiply(E13_DOT).add(E13_0)).sin().multiply(E13_SIN))),
                                       toRadians(t.multiply(DELTA_DOT).add(DELTA_0).
                                                 add(toRadians(d.multiply(E01_DOT).add(E01_0)).cos().multiply(E01_COS)).
                                                 add(toRadians(d.multiply(E02_DOT).add(E02_0)).cos().multiply(E02_COS)).
                                                 add(toRadians(d.multiply(E03_DOT).add(E03_0)).cos().multiply(E03_COS)).
                                                 add(toRadians(d.multiply(E04_DOT).add(E04_0)).cos().multiply(E04_COS)).
                                                 add(toRadians(d.multiply(E06_DOT).add(E06_0)).cos().multiply(E06_COS)).
                                                 add(toRadians(d.multiply(E07_DOT).add(E07_0)).cos().multiply(E07_COS)).
                                                 add(toRadians(d.multiply(E10_DOT).add(E10_0)).cos().multiply(E10_COS)).
                                                 add(toRadians(d.multiply(E13_DOT).add(E13_0)).cos().multiply(E13_COS))));
        }

        /** {@inheritDoc} */
        public double getPrimeMeridianAngle(final AbsoluteDate date) {
            final double d = d(date);
            return FastMath.toRadians(d * (d * W_DOT_DOT + W_DOT) + W_0 +
                                      FastMath.sin(FastMath.toRadians(d * E01_DOT + E01_0)) * E01_W_SIN +
                                      FastMath.sin(FastMath.toRadians(d * E02_DOT + E02_0)) * E02_W_SIN +
                                      FastMath.sin(FastMath.toRadians(d * E03_DOT + E03_0)) * E03_W_SIN +
                                      FastMath.sin(FastMath.toRadians(d * E04_DOT + E04_0)) * E04_W_SIN +
                                      FastMath.sin(FastMath.toRadians(d * E05_DOT + E05_0)) * E05_W_SIN +
                                      FastMath.sin(FastMath.toRadians(d * E06_DOT + E06_0)) * E06_W_SIN +
                                      FastMath.sin(FastMath.toRadians(d * E07_DOT + E07_0)) * E07_W_SIN +
                                      FastMath.sin(FastMath.toRadians(d * E08_DOT + E08_0)) * E08_W_SIN +
                                      FastMath.sin(FastMath.toRadians(d * E09_DOT + E09_0)) * E09_W_SIN +
                                      FastMath.sin(FastMath.toRadians(d * E10_DOT + E10_0)) * E10_W_SIN +
                                      FastMath.sin(FastMath.toRadians(d * E11_DOT + E11_0)) * E11_W_SIN +
                                      FastMath.sin(FastMath.toRadians(d * E12_DOT + E12_0)) * E12_W_SIN +
                                      FastMath.sin(FastMath.toRadians(d * E13_DOT + E13_0)) * E13_W_SIN);
        }

        /** {@inheritDoc} */
        public <T extends RealFieldElement<T>> T getPrimeMeridianAngle(final FieldAbsoluteDate<T> date) {
            final T d = d(date);
            return toRadians(d.multiply(d.multiply(W_DOT_DOT).add(W_DOT)).add(W_0).
                             add(toRadians(d.multiply(E01_DOT).add(E01_0)).sin().multiply(E01_W_SIN)).
                             add(toRadians(d.multiply(E02_DOT).add(E02_0)).sin().multiply(E02_W_SIN)).
                             add(toRadians(d.multiply(E03_DOT).add(E03_0)).sin().multiply(E03_W_SIN)).
                             add(toRadians(d.multiply(E04_DOT).add(E04_0)).sin().multiply(E04_W_SIN)).
                             add(toRadians(d.multiply(E05_DOT).add(E05_0)).sin().multiply(E05_W_SIN)).
                             add(toRadians(d.multiply(E06_DOT).add(E06_0)).sin().multiply(E06_W_SIN)).
                             add(toRadians(d.multiply(E07_DOT).add(E07_0)).sin().multiply(E07_W_SIN)).
                             add(toRadians(d.multiply(E08_DOT).add(E08_0)).sin().multiply(E08_W_SIN)).
                             add(toRadians(d.multiply(E09_DOT).add(E09_0)).sin().multiply(E09_W_SIN)).
                             add(toRadians(d.multiply(E10_DOT).add(E10_0)).sin().multiply(E10_W_SIN)).
                             add(toRadians(d.multiply(E11_DOT).add(E11_0)).sin().multiply(E11_W_SIN)).
                             add(toRadians(d.multiply(E12_DOT).add(E12_0)).sin().multiply(E12_W_SIN)).
                             add(toRadians(d.multiply(E13_DOT).add(E13_0)).sin().multiply(E13_W_SIN)));
        }

    },

    /** IAU pole and prime meridian model for Mars. */
    MARS {

        /** Constant term of the right ascension of the pole. */
        private static final double ALPHA_0 = 317.68143;

        /** Rate term of the right ascension of the pole. */
        private static final double ALPHA_DOT = -0.1061;

        /** Constant term of the declination of the pole. */
        private static final double DELTA_0 =  52.88650;

        /** Rate term of the declination of the pole. */
        private static final double DELTA_DOT = -0.0609;

        /** Constant term of the prime meridian. */
        private static final double W_0 = 176.630;

        /** Rate term of the prime meridian. */
        private static final double W_DOT = 350.89198226;

        /** {@inheritDoc} */
        public Vector3D getPole(final AbsoluteDate date) {
            final double t = t(date);
            return new Vector3D(FastMath.toRadians(t * ALPHA_DOT + ALPHA_0),
                                FastMath.toRadians(t * DELTA_DOT + DELTA_0));
        }

        /** {@inheritDoc} */
        public <T extends RealFieldElement<T>> FieldVector3D<T> getPole(final FieldAbsoluteDate<T> date) {
            final T t = t(date);
            return new FieldVector3D<>(toRadians(t.multiply(ALPHA_DOT).add(ALPHA_0)),
                                       toRadians(t.multiply(DELTA_DOT).add(DELTA_0)));
        }

        /** {@inheritDoc} */
        public double getPrimeMeridianAngle(final AbsoluteDate date) {
            return FastMath.toRadians(d(date) * W_DOT + W_0);
        }

        /** {@inheritDoc} */
        public <T extends RealFieldElement<T>> T getPrimeMeridianAngle(final FieldAbsoluteDate<T> date) {
            return toRadians(d(date).multiply(W_DOT).add(W_0));
        }

    },

    /** IAU pole and prime meridian model for Jupiter. */
    JUPITER {

        /** Constant term of the right ascension of the pole. */
        private static final double ALPHA_0 = 268.056595;

        /** Rate term of the right ascension of the pole. */
        private static final double ALPHA_DOT = -0.006499;

        /** Constant term of the declination of the pole. */
        private static final double DELTA_0 = 64.495303;

        /** Rate term of the declination of the pole. */
        private static final double DELTA_DOT = 0.002413;

        /** Constant term of the ja angle. */
        private static final double JA_0 =  99.360714;

        /** Rate term of the ja angle. */
        private static final double JA_DOT = 4850.4046;

        /** Sine coefficient of the ja angle. */
        private static final double JA_SIN = 0.000117;

        /** Cosine coefficient of the ja angle. */
        private static final double JA_COS = 0.000050;

        /** Constant term of the jb angle. */
        private static final double JB_0 = 175.895369;

        /** Rate term of the jb angle. */
        private static final double JB_DOT = 1191.9605;

        /** Sine coefficient of the jb angle. */
        private static final double JB_SIN = 0.000938;

        /** Cosine coefficient of the jb angle. */
        private static final double JB_COS = 0.000404;

        /** Constant term of the jc angle. */
        private static final double JC_0 = 300.323162;

        /** Rate term of the jc angle. */
        private static final double JC_DOT = 262.5475;

        /** Sine coefficient of the jc angle. */
        private static final double JC_SIN = 0.001432;

        /** Cosine coefficient of the jc angle. */
        private static final double JC_COS = 0.000617;

        /** Constant term of the jd angle. */
        private static final double JD_0 = 114.012305;

        /** Rate term of the jd angle. */
        private static final double JD_DOT = 6070.2476;

        /** Sine coefficient of the jd angle. */
        private static final double JD_SIN = 0.000030;

        /** Cosine coefficient of the jd angle. */
        private static final double JD_COS = -0.000013;

        /** Constant term of the je angle. */
        private static final double JE_0 = 49.511251;

        /** Rate term of the je angle. */
        private static final double JE_DOT = 64.3000;

        /** Sine coefficient of the je angle. */
        private static final double JE_SIN = 0.002150;

        /** Cosine coefficient of the je angle. */
        private static final double JE_COS = 0.000926;

        /** Constant term of the prime meridian. */
        private static final double W_0 = 284.95;

        /** Rate term of the prime meridian. */
        private static final double W_DOT = 870.5360000;

        /** {@inheritDoc} */
        public Vector3D getPole(final AbsoluteDate date) {

            final double t = t(date);
            final double ja = FastMath.toRadians(t * JA_DOT + JA_0);
            final double jb = FastMath.toRadians(t * JB_DOT + JB_0);
            final double jc = FastMath.toRadians(t * JC_DOT + JC_0);
            final double jd = FastMath.toRadians(t * JD_DOT + JD_0);
            final double je = FastMath.toRadians(t * JE_DOT + JE_0);

            return new Vector3D(FastMath.toRadians(t * ALPHA_DOT + ALPHA_0 +
                                                   FastMath.sin(ja) * JA_SIN +
                                                   FastMath.sin(jb) * JB_SIN +
                                                   FastMath.sin(jc) * JC_SIN +
                                                   FastMath.sin(jd) * JD_SIN +
                                                   FastMath.sin(je) * JE_SIN),
                                FastMath.toRadians(t * DELTA_DOT + DELTA_0 +
                                                   FastMath.cos(ja) * JA_COS +
                                                   FastMath.cos(jb) * JB_COS +
                                                   FastMath.cos(jc) * JC_COS +
                                                   FastMath.cos(jd) * JD_COS +
                                                   FastMath.cos(je) * JE_COS));
        }

        /** {@inheritDoc} */
        public <T extends RealFieldElement<T>> FieldVector3D<T> getPole(final FieldAbsoluteDate<T> date) {

            final T t = t(date);
            final T ja = toRadians(t.multiply(JA_DOT).add(JA_0));
            final T jb = toRadians(t.multiply(JB_DOT).add(JB_0));
            final T jc = toRadians(t.multiply(JC_DOT).add(JC_0));
            final T jd = toRadians(t.multiply(JD_DOT).add(JD_0));
            final T je = toRadians(t.multiply(JE_DOT).add(JE_0));

            return new FieldVector3D<>(toRadians(t.multiply(ALPHA_DOT).add(ALPHA_0).
                                                 add(ja.sin().multiply(JA_SIN)).
                                                 add(jb.sin().multiply(JB_SIN)).
                                                 add(jc.sin().multiply(JC_SIN)).
                                                 add(jd.sin().multiply(JD_SIN)).
                                                 add(je.sin().multiply(JE_SIN))),
                                       toRadians(t.multiply(DELTA_DOT).add(DELTA_0).
                                                 add(ja.cos().multiply(JA_COS)).
                                                 add(jb.cos().multiply(JB_COS)).
                                                 add(jc.cos().multiply(JC_COS)).
                                                 add(jd.cos().multiply(JD_COS)).
                                                 add(je.cos().multiply(JE_COS))));

        }

        /** {@inheritDoc} */
        public double getPrimeMeridianAngle(final AbsoluteDate date) {
            return FastMath.toRadians(d(date) * W_DOT + W_0);
        }

        /** {@inheritDoc} */
        public <T extends RealFieldElement<T>> T getPrimeMeridianAngle(final FieldAbsoluteDate<T> date) {
            return toRadians(d(date).multiply(W_DOT).add(W_0));
        }

    },

    /** IAU pole and prime meridian model for Saturn. */
    SATURN {

        /** Constant term of the right ascension of the pole. */
        private static final double ALPHA_0 = 40.589;

        /** Rate term of the right ascension of the pole. */
        private static final double ALPHA_DOT = -0.036;

        /** Constant term of the declination of the pole. */
        private static final double DELTA_0 = 83.537;

        /** Rate term of the declination of the pole. */
        private static final double DELTA_DOT = -0.004;

        /** Constant term of the prime meridian. */
        private static final double W_0 = 38.90;

        /** Rate term of the prime meridian. */
        private static final double W_DOT = 810.7939024;

        /** {@inheritDoc} */
        public Vector3D getPole(final AbsoluteDate date) {
            final double t = t(date);
            return new Vector3D(FastMath.toRadians(t * ALPHA_DOT + ALPHA_0),
                                FastMath.toRadians(t * DELTA_DOT + DELTA_0));
        }

        /** {@inheritDoc} */
        public <T extends RealFieldElement<T>> FieldVector3D<T> getPole(final FieldAbsoluteDate<T> date) {
            final T t = t(date);
            return new FieldVector3D<>(toRadians(t.multiply(ALPHA_DOT).add(ALPHA_0)),
                                       toRadians(t.multiply(DELTA_DOT).add(DELTA_0)));
        }

        /** {@inheritDoc} */
        public double getPrimeMeridianAngle(final AbsoluteDate date) {
            return FastMath.toRadians(d(date) * W_DOT + W_0);
        }

        /** {@inheritDoc} */
        public <T extends RealFieldElement<T>> T getPrimeMeridianAngle(final FieldAbsoluteDate<T> date) {
            return toRadians(d(date).multiply(W_DOT).add(W_0));
        }

    },

    /** IAU pole and prime meridian model for Uranus. */
    URANUS {

        /** Constant term of the prime meridian. */
        private static final double W_0 = 203.81;

        /** Rate term of the prime meridian. */
        private static final double W_DOT = -501.1600928;

        /** Fixed pole. */
        private final Vector3D pole = new Vector3D(FastMath.toRadians(257.311),
                                                   FastMath.toRadians(-15.175));

        /** {@inheritDoc} */
        public Vector3D getPole(final AbsoluteDate date) {
            return pole;
        }

        /** {@inheritDoc} */
        public <T extends RealFieldElement<T>> FieldVector3D<T> getPole(final FieldAbsoluteDate<T> date) {
            return new FieldVector3D<>(date.getField(), pole);
        }

        /** {@inheritDoc} */
        public double getPrimeMeridianAngle(final AbsoluteDate date) {
            return FastMath.toRadians(d(date) * W_DOT + W_0);
        }

        /** {@inheritDoc} */
        public <T extends RealFieldElement<T>> T getPrimeMeridianAngle(final FieldAbsoluteDate<T> date) {
            return toRadians(d(date).multiply(W_DOT).add(W_0));
        }

    },

    /** IAU pole and prime meridian model for Neptune. */
    NEPTUNE {

        /** Constant term of the right ascension of the pole. */
        private static final double ALPHA_0 = 299.36;

        /** Sine term of the right ascension of the pole. */
        private static final double ALPHA_SIN = 0.70;

        /** Constant term of the declination of the pole. */
        private static final double DELTA_0 = 43.46;

        /** Cosine term of the declination of the pole. */
        private static final double DELTA_COS = -0.51;

        /** Constant term of the prime meridian. */
        private static final double W_0 = 253.18;

        /** Rate term of the prime meridian. */
        private static final double W_DOT = 536.3128492;

        /** Sine term of the prime meridian. */
        private static final double W_SIN = -0.48;

        /** Constant term of the N angle. */
        private static final double N_0   = 357.85;

        /** Rate term of the M1 angle. */
        private static final double N_DOT = 52.316;

        /** {@inheritDoc} */
        public Vector3D getPole(final AbsoluteDate date) {
            final double n = FastMath.toRadians(t(date) * N_DOT + N_0);
            return new Vector3D(FastMath.toRadians(FastMath.sin(n) * ALPHA_SIN + ALPHA_0),
                                FastMath.toRadians(FastMath.cos(n) * DELTA_COS + DELTA_0));
        }

        /** {@inheritDoc} */
        public <T extends RealFieldElement<T>> FieldVector3D<T> getPole(final FieldAbsoluteDate<T> date) {
            final T n = toRadians(t(date).multiply(N_DOT).add(N_0));
            return new FieldVector3D<>(toRadians(n.sin().multiply(ALPHA_SIN).add(ALPHA_0)),
                                       toRadians(n.cos().multiply(DELTA_COS).add(DELTA_0)));
        }

        /** {@inheritDoc} */
        public double getPrimeMeridianAngle(final AbsoluteDate date) {
            final double n = FastMath.toRadians(t(date) * N_DOT + N_0);
            return FastMath.toRadians(d(date) * W_DOT + FastMath.sin(n) * W_SIN + W_0);
        }

        /** {@inheritDoc} */
        public <T extends RealFieldElement<T>> T getPrimeMeridianAngle(final FieldAbsoluteDate<T> date) {
            final T n = toRadians(t(date).multiply(N_DOT).add(N_0));
            return toRadians(d(date).multiply(W_DOT).add(n.sin().multiply(W_SIN)).add(W_0));
        }

    },

    /** IAU pole and prime meridian model for Pluto. */
    PLUTO {

        /** Constant term of the prime meridian. */
        private static final double W_0 = 302.695;

        /** Rate term of the prime meridian. */
        private static final double W_DOT = 56.3625225;

        /** Fixed pole. */
        private final Vector3D pole = new Vector3D(FastMath.toRadians(132.993),
                                                   FastMath.toRadians(-6.163));

        /** {@inheritDoc} */
        public Vector3D getPole(final AbsoluteDate date) {
            return pole;
        }

        /** {@inheritDoc} */
        public <T extends RealFieldElement<T>> FieldVector3D<T> getPole(final FieldAbsoluteDate<T> date) {
            return new FieldVector3D<>(date.getField(), pole);
        }

        /** {@inheritDoc} */
        public double getPrimeMeridianAngle(final AbsoluteDate date) {
            return FastMath.toRadians(d(date) * W_DOT + W_0);
        }

        /** {@inheritDoc} */
        public <T extends RealFieldElement<T>> T getPrimeMeridianAngle(final FieldAbsoluteDate<T> date) {
            return toRadians(d(date).multiply(W_DOT).add(W_0));
        }

    },

    /** Default IAUPole implementation for barycenters.
     * <p>
     * This implementation defines directions such that the inertially oriented and body
     * oriented frames are identical and aligned with GCRF. It is used for example
     * to define the ICRF.
     * </p>
     */
    GCRF_ALIGNED {

        /** {@inheritDoc} */
        public Vector3D getPole(final AbsoluteDate date) {
            return Vector3D.PLUS_K;
        }

        /** {@inheritDoc} */
        public <T extends RealFieldElement<T>> FieldVector3D<T> getPole(final FieldAbsoluteDate<T> date) {
            return FieldVector3D.getPlusK(date.getField());
        }

        /** {@inheritDoc} */
        public double getPrimeMeridianAngle(final AbsoluteDate date) {
            return 0;
        }

        /** {@inheritDoc} */
        public <T extends RealFieldElement<T>> T getPrimeMeridianAngle(final FieldAbsoluteDate<T> date) {
            return date.getField().getZero();
        }

    };


    /** Get a predefined IAU pole.
     * @param body body identifier
     * @return predefined IAU pole
     */
    public static PredefinedIAUPoles getIAUPole(final JPLEphemeridesLoader.EphemerisType body) {
        switch(body) {
            case SUN :
                return SUN;
            case MERCURY :
                return MERCURY;
            case VENUS :
                return VENUS;
            case EARTH :
                return EARTH;
            case MOON :
                return MOON;
            case MARS :
                return MARS;
            case JUPITER :
                return JUPITER;
            case SATURN :
                return SATURN;
            case URANUS :
                return URANUS;
            case NEPTUNE :
                return NEPTUNE;
            case PLUTO :
                return PLUTO;
            default :
                return GCRF_ALIGNED;
        }
    }

    /** Compute the interval in julian centuries from standard epoch.
     * @param date date
     * @return interval between date and standard epoch in julian centuries
     */
    private static double t(final AbsoluteDate date) {
        return date.offsetFrom(AbsoluteDate.J2000_EPOCH, TimeScalesFactory.getTDB()) / Constants.JULIAN_CENTURY;
    }

    /** Compute the interval in julian centuries from standard epoch.
     * @param date date
     * @param <T> type of the filed elements
     * @return interval between date and standard epoch in julian centuries
     */
    private static <T extends RealFieldElement<T>> T t(final FieldAbsoluteDate<T> date) {
        return date.offsetFrom(FieldAbsoluteDate.getJ2000Epoch(date.getField()), TimeScalesFactory.getTDB()).divide(Constants.JULIAN_CENTURY);
    }

    /** Compute the interval in julian days from standard epoch.
     * @param date date
     * @return interval between date and standard epoch in julian days
     */
    private static double d(final AbsoluteDate date) {
        return date.offsetFrom(AbsoluteDate.J2000_EPOCH, TimeScalesFactory.getTDB()) / Constants.JULIAN_DAY;
    }

    /** Compute the interval in julian days from standard epoch.
     * @param date date
     * @param <T> type of the filed elements
     * @return interval between date and standard epoch in julian days
     */
    private static <T extends RealFieldElement<T>> T d(final FieldAbsoluteDate<T> date) {
        return date.offsetFrom(FieldAbsoluteDate.getJ2000Epoch(date.getField()), TimeScalesFactory.getTDB()).divide(Constants.JULIAN_DAY);
    }

    /** Convert an angle to radians.
     * @param angleInDegrees angle in degrees
     * @param <T> type of the filed elements
     * @return angle in radians
     */
    private static <T extends RealFieldElement<T>> T toRadians(final T angleInDegrees) {
        return angleInDegrees.multiply(FastMath.PI / 180);
    }

}
