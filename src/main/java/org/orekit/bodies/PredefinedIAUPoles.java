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
package org.orekit.bodies;

import java.util.ArrayList;
import java.util.List;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.FieldSinCos;
import org.hipparchus.util.SinCos;
import org.orekit.bodies.JPLEphemeridesLoader.EphemerisType;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeScales;
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
abstract class PredefinedIAUPoles implements IAUPole {

    /** Serializable UID. */
    private static final long serialVersionUID = 20200130L;

    /** Time scales. */
    private final TimeScales timeScales;

    /**
     * Simple constructor.
     *
     * @param timeScales to use when computing the pole, including TDB and J2000.0.
     */
    PredefinedIAUPoles(final TimeScales timeScales) {
        this.timeScales = timeScales;
    }

    /** IAU pole and prime meridian model for Sun. */
    private static class Sun extends PredefinedIAUPoles {

        /** Serializable UID. */
        private static final long serialVersionUID = 20200130L;

        /** Constant term of the prime meridian. */
        private static final double W0 = 84.176;

        /** Rate term of the prime meridian. */
        private static final double W_DOT = 14.1844000;

        /** Fixed pole. */
        private final Vector3D pole = new Vector3D(FastMath.toRadians(286.13),
                                                   FastMath.toRadians(63.87));

        /**
         * Simple constructor.
         *
         * @param timeScales to use when computing the pole, including TDB and J2000.0.
         */
        Sun(final TimeScales timeScales) {
            super(timeScales);
        }

        /** {@inheritDoc} */
        public Vector3D getPole(final AbsoluteDate date) {
            return pole;
        }

        /** {@inheritDoc} */
        public <T extends CalculusFieldElement<T>> FieldVector3D<T> getPole(final FieldAbsoluteDate<T> date) {
            return new FieldVector3D<>(date.getField(), pole);
        }

        /** {@inheritDoc} */
        public double getPrimeMeridianAngle(final AbsoluteDate date) {
            return FastMath.toRadians(d(date) * W_DOT + W0);
        }

        /** {@inheritDoc} */
        public <T extends CalculusFieldElement<T>> T getPrimeMeridianAngle(final FieldAbsoluteDate<T> date) {
            return FastMath.toRadians(d(date).multiply(W_DOT).add(W0));
        }

    }

    /** IAU pole and prime meridian model for Mercury. */
    private static class Mercury extends PredefinedIAUPoles {

        /** Serializable UID. */
        private static final long serialVersionUID = 20200130L;

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

        /**
         * Simple constructor.
         *
         * @param timeScales to use when computing the pole, including TDB and J2000.0.
         */
        Mercury(final TimeScales timeScales) {
            super(timeScales);
        }

        /** {@inheritDoc} */
        public Vector3D getPole(final AbsoluteDate date) {
            final double t = t(date);
            return new Vector3D(FastMath.toRadians(t * ALPHA_DOT + ALPHA_0),
                                FastMath.toRadians(t * DELTA_DOT + DELTA_0));
        }

        /** {@inheritDoc} */
        public <T extends CalculusFieldElement<T>> FieldVector3D<T> getPole(final FieldAbsoluteDate<T> date) {
            final T t = t(date);
            return new FieldVector3D<>(FastMath.toRadians(t.multiply(ALPHA_DOT).add(ALPHA_0)),
                                       FastMath.toRadians(t.multiply(DELTA_DOT).add(DELTA_0)));
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
        public <T extends CalculusFieldElement<T>> T getPrimeMeridianAngle(final FieldAbsoluteDate<T> date) {
            final T d = d(date);
            return FastMath.toRadians(d(date).multiply(W_DOT).add(W_0).
                             add(FastMath.toRadians(d.multiply(M1_DOT).add(M1_0)).sin().multiply(M1_COEFF)).
                             add(FastMath.toRadians(d.multiply(M2_DOT).add(M2_0)).sin().multiply(M2_COEFF)).
                             add(FastMath.toRadians(d.multiply(M3_DOT).add(M3_0)).sin().multiply(M3_COEFF)).
                             add(FastMath.toRadians(d.multiply(M4_DOT).add(M4_0)).sin().multiply(M4_COEFF)).
                             add(FastMath.toRadians(d.multiply(M5_DOT).add(M5_0)).sin().multiply(M5_COEFF)));
        }

    }

    /** IAU pole and prime meridian model for Venus. */
    private static class Venus extends PredefinedIAUPoles {

        /** Serializable UID. */
        private static final long serialVersionUID = 20200130L;

        /** Constant term of the prime meridian. */
        private static final double W_0 = 160.20;

        /** Rate term of the prime meridian. */
        private static final double W_DOT = -1.4813688;

        /** Fixed pole. */
        private final Vector3D pole = new Vector3D(FastMath.toRadians(272.76),
                                                   FastMath.toRadians(67.16));

        /**
         * Simple constructor.
         *
         * @param timeScales to use when computing the pole, including TDB and J2000.0.
         */
        Venus(final TimeScales timeScales) {
            super(timeScales);
        }

        /** {@inheritDoc} */
        public Vector3D getPole(final AbsoluteDate date) {
            return pole;
        }

        /** {@inheritDoc} */
        public <T extends CalculusFieldElement<T>> FieldVector3D<T> getPole(final FieldAbsoluteDate<T> date) {
            return new FieldVector3D<>(date.getField(), pole);
        }

        /** {@inheritDoc} */
        public double getPrimeMeridianAngle(final AbsoluteDate date) {
            return FastMath.toRadians(d(date) * W_DOT + W_0);
        }

        /** {@inheritDoc} */
        public <T extends CalculusFieldElement<T>> T getPrimeMeridianAngle(final FieldAbsoluteDate<T> date) {
            return FastMath.toRadians(d(date).multiply(W_DOT).add(W_0));
        }

    }

    /** IAU pole and prime meridian model for Earth. */
    private static class Earth extends PredefinedIAUPoles {

        /** Serializable UID. */
        private static final long serialVersionUID = 20200130L;

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

        /**
         * Simple constructor.
         *
         * @param timeScales to use when computing the pole, including TDB and J2000.0.
         */
        Earth(final TimeScales timeScales) {
            super(timeScales);
        }

        /** {@inheritDoc} */
        public Vector3D getPole(final AbsoluteDate date) {
            final double t = t(date);
            return new Vector3D(FastMath.toRadians(t * ALPHA_DOT + ALPHA_0),
                                FastMath.toRadians(t * DELTA_DOT + DELTA_0));
        }

        /** {@inheritDoc} */
        public <T extends CalculusFieldElement<T>> FieldVector3D<T> getPole(final FieldAbsoluteDate<T> date) {
            final T t = t(date);
            return new FieldVector3D<>(FastMath.toRadians(t.multiply(ALPHA_DOT).add(ALPHA_0)),
                                       FastMath.toRadians(t.multiply(DELTA_DOT).add(DELTA_0)));
        }

        /** {@inheritDoc} */
        @Override
        public Vector3D getNode(final AbsoluteDate date) {
            final double t = t(date);
            return new Vector3D(FastMath.toRadians(t * ALPHA_DOT + ALPHA_0 + 90.0),
                                0.0);
        }

        /** {@inheritDoc} */
        @Override
        public <T extends CalculusFieldElement<T>> FieldVector3D<T> getNode(final FieldAbsoluteDate<T> date) {
            final T t = t(date);
            return new FieldVector3D<>(FastMath.toRadians(t.multiply(ALPHA_DOT).add(ALPHA_0 + 90.0)),
                                       date.getField().getZero());
        }

        /** {@inheritDoc} */
        public double getPrimeMeridianAngle(final AbsoluteDate date) {
            return FastMath.toRadians(d(date) * W_DOT + W_0);
        }

        /** {@inheritDoc} */
        public <T extends CalculusFieldElement<T>> T getPrimeMeridianAngle(final FieldAbsoluteDate<T> date) {
            return FastMath.toRadians(d(date).multiply(W_DOT).add(W_0));
        }

    }

    /** IAU pole and prime meridian model for the Moon. */
    private static class Moon extends PredefinedIAUPoles {

        /** Serializable UID. */
        private static final long serialVersionUID = 20200130L;

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

        /**
         * Simple constructor.
         *
         * @param timeScales to use when computing the pole, including TDB and J2000.0.
         */
        Moon(final TimeScales timeScales) {
            super(timeScales);
        }

        /** {@inheritDoc} */
        public Vector3D getPole(final AbsoluteDate date) {
            final double d = d(date);
            final double t = t(date);

            final SinCos scE01 = FastMath.sinCos(FastMath.toRadians(d * E01_DOT + E01_0));
            final SinCos scE02 = FastMath.sinCos(FastMath.toRadians(d * E02_DOT + E02_0));
            final SinCos scE03 = FastMath.sinCos(FastMath.toRadians(d * E03_DOT + E03_0));
            final SinCos scE04 = FastMath.sinCos(FastMath.toRadians(d * E04_DOT + E04_0));
            final SinCos scE06 = FastMath.sinCos(FastMath.toRadians(d * E06_DOT + E06_0));
            final SinCos scE10 = FastMath.sinCos(FastMath.toRadians(d * E10_DOT + E10_0));
            final SinCos scE13 = FastMath.sinCos(FastMath.toRadians(d * E13_DOT + E13_0));

            return new Vector3D(FastMath.toRadians(t * ALPHA_DOT + ALPHA_0 +
                                                   scE01.sin() * E01_SIN +
                                                   scE02.sin() * E02_SIN +
                                                   scE03.sin() * E03_SIN +
                                                   scE04.sin() * E04_SIN +
                                                   scE06.sin() * E06_SIN +
                                                   scE10.sin() * E10_SIN +
                                                   scE13.sin() * E13_SIN),
                                FastMath.toRadians(t * DELTA_DOT + DELTA_0 +
                                                   scE01.cos() * E01_COS +
                                                   scE02.cos() * E02_COS +
                                                   scE03.cos() * E03_COS +
                                                   scE04.cos() * E04_COS +
                                                   scE06.cos() * E06_COS +
                                                   FastMath.cos(FastMath.toRadians(d * E07_DOT + E07_0)) * E07_COS +  // only the cosine is needed
                                                   scE10.cos() * E10_COS +
                                                   scE13.cos() * E13_COS));
        }

        /** {@inheritDoc} */
        public <T extends CalculusFieldElement<T>> FieldVector3D<T> getPole(final FieldAbsoluteDate<T> date) {
            final T d = d(date);
            final T t = t(date);

            final FieldSinCos<T> scE01 = FastMath.sinCos(FastMath.toRadians(d.multiply(E01_DOT).add(E01_0)));
            final FieldSinCos<T> scE02 = FastMath.sinCos(FastMath.toRadians(d.multiply(E02_DOT).add(E02_0)));
            final FieldSinCos<T> scE03 = FastMath.sinCos(FastMath.toRadians(d.multiply(E03_DOT).add(E03_0)));
            final FieldSinCos<T> scE04 = FastMath.sinCos(FastMath.toRadians(d.multiply(E04_DOT).add(E04_0)));
            final FieldSinCos<T> scE06 = FastMath.sinCos(FastMath.toRadians(d.multiply(E06_DOT).add(E06_0)));
            final FieldSinCos<T> scE10 = FastMath.sinCos(FastMath.toRadians(d.multiply(E10_DOT).add(E10_0)));
            final FieldSinCos<T> scE13 = FastMath.sinCos(FastMath.toRadians(d.multiply(E13_DOT).add(E13_0)));

            return new FieldVector3D<>(FastMath.toRadians(t.multiply(ALPHA_DOT).add(ALPHA_0).
                                                 add(scE01.sin().multiply(E01_SIN)).
                                                 add(scE02.sin().multiply(E02_SIN)).
                                                 add(scE03.sin().multiply(E03_SIN)).
                                                 add(scE04.sin().multiply(E04_SIN)).
                                                 add(scE06.sin().multiply(E06_SIN)).
                                                 add(scE10.sin().multiply(E10_SIN)).
                                                 add(scE13.sin().multiply(E13_SIN))),
                                       FastMath.toRadians(t.multiply(DELTA_DOT).add(DELTA_0).
                                                 add(scE01.cos().multiply(E01_COS)).
                                                 add(scE02.cos().multiply(E02_COS)).
                                                 add(scE03.cos().multiply(E03_COS)).
                                                 add(scE04.cos().multiply(E04_COS)).
                                                 add(scE06.cos().multiply(E06_COS)).
                                                 add(FastMath.toRadians(d.multiply(E07_DOT).add(E07_0)).cos().multiply(E07_COS)).// only the cosine is needed
                                                 add(scE10.cos().multiply(E10_COS)).
                                                 add(scE13.cos().multiply(E13_COS))));
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
        public <T extends CalculusFieldElement<T>> T getPrimeMeridianAngle(final FieldAbsoluteDate<T> date) {
            final T d = d(date);
            return FastMath.toRadians(d.multiply(d.multiply(W_DOT_DOT).add(W_DOT)).add(W_0).
                                      add(FastMath.toRadians(d.multiply(E01_DOT).add(E01_0)).sin().multiply(E01_W_SIN)).
                                      add(FastMath.toRadians(d.multiply(E02_DOT).add(E02_0)).sin().multiply(E02_W_SIN)).
                                      add(FastMath.toRadians(d.multiply(E03_DOT).add(E03_0)).sin().multiply(E03_W_SIN)).
                                      add(FastMath.toRadians(d.multiply(E04_DOT).add(E04_0)).sin().multiply(E04_W_SIN)).
                                      add(FastMath.toRadians(d.multiply(E05_DOT).add(E05_0)).sin().multiply(E05_W_SIN)).
                                      add(FastMath.toRadians(d.multiply(E06_DOT).add(E06_0)).sin().multiply(E06_W_SIN)).
                                      add(FastMath.toRadians(d.multiply(E07_DOT).add(E07_0)).sin().multiply(E07_W_SIN)).
                                      add(FastMath.toRadians(d.multiply(E08_DOT).add(E08_0)).sin().multiply(E08_W_SIN)).
                                      add(FastMath.toRadians(d.multiply(E09_DOT).add(E09_0)).sin().multiply(E09_W_SIN)).
                                      add(FastMath.toRadians(d.multiply(E10_DOT).add(E10_0)).sin().multiply(E10_W_SIN)).
                                      add(FastMath.toRadians(d.multiply(E11_DOT).add(E11_0)).sin().multiply(E11_W_SIN)).
                                      add(FastMath.toRadians(d.multiply(E12_DOT).add(E12_0)).sin().multiply(E12_W_SIN)).
                                      add(FastMath.toRadians(d.multiply(E13_DOT).add(E13_0)).sin().multiply(E13_W_SIN)));
        }

    }

    /** IAU pole and prime meridian model for Mars. */
    private static class Mars extends PredefinedIAUPoles {

        /** Serializable UID. */
        private static final long serialVersionUID = 20200130L;

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

        /**
         * Simple constructor.
         *
         * @param timeScales to use when computing the pole, including TDB and J2000.0.
         */
        Mars(final TimeScales timeScales) {
            super(timeScales);
        }

        /** {@inheritDoc} */
        public Vector3D getPole(final AbsoluteDate date) {
            final double t = t(date);
            return new Vector3D(FastMath.toRadians(t * ALPHA_DOT + ALPHA_0),
                                FastMath.toRadians(t * DELTA_DOT + DELTA_0));
        }

        /** {@inheritDoc} */
        public <T extends CalculusFieldElement<T>> FieldVector3D<T> getPole(final FieldAbsoluteDate<T> date) {
            final T t = t(date);
            return new FieldVector3D<>(FastMath.toRadians(t.multiply(ALPHA_DOT).add(ALPHA_0)),
                                       FastMath.toRadians(t.multiply(DELTA_DOT).add(DELTA_0)));
        }

        /** {@inheritDoc} */
        public double getPrimeMeridianAngle(final AbsoluteDate date) {
            return FastMath.toRadians(d(date) * W_DOT + W_0);
        }

        /** {@inheritDoc} */
        public <T extends CalculusFieldElement<T>> T getPrimeMeridianAngle(final FieldAbsoluteDate<T> date) {
            return FastMath.toRadians(d(date).multiply(W_DOT).add(W_0));
        }

    }

    /** IAU pole and prime meridian model for Jupiter. */
    private static class Jupiter extends PredefinedIAUPoles {

        /** Serializable UID. */
        private static final long serialVersionUID = 20200130L;

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

        /**
         * Simple constructor.
         *
         * @param timeScales to use when computing the pole, including TDB and J2000.0.
         */
        Jupiter(final TimeScales timeScales) {
            super(timeScales);
        }

        /** {@inheritDoc} */
        public Vector3D getPole(final AbsoluteDate date) {

            final double t = t(date);
            final double ja = FastMath.toRadians(t * JA_DOT + JA_0);
            final double jb = FastMath.toRadians(t * JB_DOT + JB_0);
            final double jc = FastMath.toRadians(t * JC_DOT + JC_0);
            final double jd = FastMath.toRadians(t * JD_DOT + JD_0);
            final double je = FastMath.toRadians(t * JE_DOT + JE_0);

            final SinCos scJa = FastMath.sinCos(ja);
            final SinCos scJb = FastMath.sinCos(jb);
            final SinCos scJc = FastMath.sinCos(jc);
            final SinCos scJd = FastMath.sinCos(jd);
            final SinCos scJe = FastMath.sinCos(je);

            return new Vector3D(FastMath.toRadians(t * ALPHA_DOT + ALPHA_0 +
                                                   scJa.sin() * JA_SIN +
                                                   scJb.sin() * JB_SIN +
                                                   scJc.sin() * JC_SIN +
                                                   scJd.sin() * JD_SIN +
                                                   scJe.sin() * JE_SIN),
                                FastMath.toRadians(t * DELTA_DOT + DELTA_0 +
                                                   scJa.cos() * JA_COS +
                                                   scJb.cos() * JB_COS +
                                                   scJc.cos() * JC_COS +
                                                   scJd.cos() * JD_COS +
                                                   scJe.cos() * JE_COS));
        }

        /** {@inheritDoc} */
        public <T extends CalculusFieldElement<T>> FieldVector3D<T> getPole(final FieldAbsoluteDate<T> date) {

            final T t = t(date);
            final T ja = FastMath.toRadians(t.multiply(JA_DOT).add(JA_0));
            final T jb = FastMath.toRadians(t.multiply(JB_DOT).add(JB_0));
            final T jc = FastMath.toRadians(t.multiply(JC_DOT).add(JC_0));
            final T jd = FastMath.toRadians(t.multiply(JD_DOT).add(JD_0));
            final T je = FastMath.toRadians(t.multiply(JE_DOT).add(JE_0));

            final FieldSinCos<T> scJa = FastMath.sinCos(ja);
            final FieldSinCos<T> scJb = FastMath.sinCos(jb);
            final FieldSinCos<T> scJc = FastMath.sinCos(jc);
            final FieldSinCos<T> scJd = FastMath.sinCos(jd);
            final FieldSinCos<T> scJe = FastMath.sinCos(je);

            return new FieldVector3D<>(FastMath.toRadians(t.multiply(ALPHA_DOT).add(ALPHA_0).
                                                 add(scJa.sin().multiply(JA_SIN)).
                                                 add(scJb.sin().multiply(JB_SIN)).
                                                 add(scJc.sin().multiply(JC_SIN)).
                                                 add(scJd.sin().multiply(JD_SIN)).
                                                 add(scJe.sin().multiply(JE_SIN))),
                                       FastMath.toRadians(t.multiply(DELTA_DOT).add(DELTA_0).
                                                 add(scJa.cos().multiply(JA_COS)).
                                                 add(scJb.cos().multiply(JB_COS)).
                                                 add(scJc.cos().multiply(JC_COS)).
                                                 add(scJd.cos().multiply(JD_COS)).
                                                 add(scJe.cos().multiply(JE_COS))));

        }

        /** {@inheritDoc} */
        public double getPrimeMeridianAngle(final AbsoluteDate date) {
            return FastMath.toRadians(d(date) * W_DOT + W_0);
        }

        /** {@inheritDoc} */
        public <T extends CalculusFieldElement<T>> T getPrimeMeridianAngle(final FieldAbsoluteDate<T> date) {
            return FastMath.toRadians(d(date).multiply(W_DOT).add(W_0));
        }

    }

    /** IAU pole and prime meridian model for Saturn. */
    private static class Saturn extends PredefinedIAUPoles {

        /** Serializable UID. */
        private static final long serialVersionUID = 20200130L;

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

        /**
         * Simple constructor.
         *
         * @param timeScales to use when computing the pole, including TDB and J2000.0.
         */
        Saturn(final TimeScales timeScales) {
            super(timeScales);
        }

        /** {@inheritDoc} */
        public Vector3D getPole(final AbsoluteDate date) {
            final double t = t(date);
            return new Vector3D(FastMath.toRadians(t * ALPHA_DOT + ALPHA_0),
                                FastMath.toRadians(t * DELTA_DOT + DELTA_0));
        }

        /** {@inheritDoc} */
        public <T extends CalculusFieldElement<T>> FieldVector3D<T> getPole(final FieldAbsoluteDate<T> date) {
            final T t = t(date);
            return new FieldVector3D<>(FastMath.toRadians(t.multiply(ALPHA_DOT).add(ALPHA_0)),
                                       FastMath.toRadians(t.multiply(DELTA_DOT).add(DELTA_0)));
        }

        /** {@inheritDoc} */
        public double getPrimeMeridianAngle(final AbsoluteDate date) {
            return FastMath.toRadians(d(date) * W_DOT + W_0);
        }

        /** {@inheritDoc} */
        public <T extends CalculusFieldElement<T>> T getPrimeMeridianAngle(final FieldAbsoluteDate<T> date) {
            return FastMath.toRadians(d(date).multiply(W_DOT).add(W_0));
        }

    }

    /** IAU pole and prime meridian model for Uranus. */
    private static class Uranus extends PredefinedIAUPoles {

        /** Serializable UID. */
        private static final long serialVersionUID = 20200130L;

        /** Constant term of the prime meridian. */
        private static final double W_0 = 203.81;

        /** Rate term of the prime meridian. */
        private static final double W_DOT = -501.1600928;

        /** Fixed pole. */
        private final Vector3D pole = new Vector3D(FastMath.toRadians(257.311),
                                                   FastMath.toRadians(-15.175));

        /**
         * Simple constructor.
         *
         * @param timeScales to use when computing the pole, including TDB and J2000.0.
         */
        Uranus(final TimeScales timeScales) {
            super(timeScales);
        }

        /** {@inheritDoc} */
        public Vector3D getPole(final AbsoluteDate date) {
            return pole;
        }

        /** {@inheritDoc} */
        public <T extends CalculusFieldElement<T>> FieldVector3D<T> getPole(final FieldAbsoluteDate<T> date) {
            return new FieldVector3D<>(date.getField(), pole);
        }

        /** {@inheritDoc} */
        public double getPrimeMeridianAngle(final AbsoluteDate date) {
            return FastMath.toRadians(d(date) * W_DOT + W_0);
        }

        /** {@inheritDoc} */
        public <T extends CalculusFieldElement<T>> T getPrimeMeridianAngle(final FieldAbsoluteDate<T> date) {
            return FastMath.toRadians(d(date).multiply(W_DOT).add(W_0));
        }

    }

    /** IAU pole and prime meridian model for Neptune. */
    private static class Neptune extends PredefinedIAUPoles {

        /** Serializable UID. */
        private static final long serialVersionUID = 20200130L;

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

        /**
         * Simple constructor.
         *
         * @param timeScales to use when computing the pole, including TDB and J2000.0.
         */
        Neptune(final TimeScales timeScales) {
            super(timeScales);
        }

        /** {@inheritDoc} */
        public Vector3D getPole(final AbsoluteDate date) {
            final double n  = FastMath.toRadians(t(date) * N_DOT + N_0);
            final SinCos sc = FastMath.sinCos(n);
            return new Vector3D(FastMath.toRadians(sc.sin() * ALPHA_SIN + ALPHA_0),
                                FastMath.toRadians(sc.cos() * DELTA_COS + DELTA_0));
        }

        /** {@inheritDoc} */
        public <T extends CalculusFieldElement<T>> FieldVector3D<T> getPole(final FieldAbsoluteDate<T> date) {
            final T n = FastMath.toRadians(t(date).multiply(N_DOT).add(N_0));
            final FieldSinCos<T> sc = FastMath.sinCos(n);
            return new FieldVector3D<>(FastMath.toRadians(sc.sin().multiply(ALPHA_SIN).add(ALPHA_0)),
                                       FastMath.toRadians(sc.cos().multiply(DELTA_COS).add(DELTA_0)));
        }

        /** {@inheritDoc} */
        public double getPrimeMeridianAngle(final AbsoluteDate date) {
            final double n = FastMath.toRadians(t(date) * N_DOT + N_0);
            return FastMath.toRadians(d(date) * W_DOT + FastMath.sin(n) * W_SIN + W_0);
        }

        /** {@inheritDoc} */
        public <T extends CalculusFieldElement<T>> T getPrimeMeridianAngle(final FieldAbsoluteDate<T> date) {
            final T n = FastMath.toRadians(t(date).multiply(N_DOT).add(N_0));
            return FastMath.toRadians(d(date).multiply(W_DOT).add(n.sin().multiply(W_SIN)).add(W_0));
        }

    }

    /** IAU pole and prime meridian model for Pluto. */
    private static class Pluto extends PredefinedIAUPoles {

        /** Serializable UID. */
        private static final long serialVersionUID = 20200130L;

        /** Constant term of the prime meridian. */
        private static final double W_0 = 302.695;

        /** Rate term of the prime meridian. */
        private static final double W_DOT = 56.3625225;

        /** Fixed pole. */
        private final Vector3D pole = new Vector3D(FastMath.toRadians(132.993),
                                                   FastMath.toRadians(-6.163));

        /**
         * Simple constructor.
         *
         * @param timeScales to use when computing the pole, including TDB and J2000.0.
         */
        Pluto(final TimeScales timeScales) {
            super(timeScales);
        }

        /** {@inheritDoc} */
        public Vector3D getPole(final AbsoluteDate date) {
            return pole;
        }

        /** {@inheritDoc} */
        public <T extends CalculusFieldElement<T>> FieldVector3D<T> getPole(final FieldAbsoluteDate<T> date) {
            return new FieldVector3D<>(date.getField(), pole);
        }

        /** {@inheritDoc} */
        public double getPrimeMeridianAngle(final AbsoluteDate date) {
            return FastMath.toRadians(d(date) * W_DOT + W_0);
        }

        /** {@inheritDoc} */
        public <T extends CalculusFieldElement<T>> T getPrimeMeridianAngle(final FieldAbsoluteDate<T> date) {
            return FastMath.toRadians(d(date).multiply(W_DOT).add(W_0));
        }

    }

    /** Default IAUPole implementation for barycenters.
     * <p>
     * This implementation defines directions such that the inertially oriented and body
     * oriented frames are identical and aligned with GCRF. It is used for example
     * to define the ICRF.
     * </p>
     */
    private static class GcrfAligned extends PredefinedIAUPoles {

        /** Serializable UID. */
        private static final long serialVersionUID = 20200130L;

        /**
         * Simple constructor.
         *
         * @param timeScales to use when computing the pole, including TDB and J2000.0.
         */
        GcrfAligned(final TimeScales timeScales) {
            super(timeScales);
        }

        /** {@inheritDoc} */
        public Vector3D getPole(final AbsoluteDate date) {
            return Vector3D.PLUS_K;
        }

        /** {@inheritDoc} */
        public <T extends CalculusFieldElement<T>> FieldVector3D<T> getPole(final FieldAbsoluteDate<T> date) {
            return FieldVector3D.getPlusK(date.getField());
        }

        /** {@inheritDoc} */
        @Override
        public Vector3D getNode(final AbsoluteDate date) {
            return Vector3D.PLUS_I;
        }

        /** {@inheritDoc} */
        @Override
        public <T extends CalculusFieldElement<T>> FieldVector3D<T> getNode(final FieldAbsoluteDate<T> date) {
            return FieldVector3D.getPlusI(date.getField());
        }

        /** {@inheritDoc} */
        public double getPrimeMeridianAngle(final AbsoluteDate date) {
            return 0;
        }

        /** {@inheritDoc} */
        public <T extends CalculusFieldElement<T>> T getPrimeMeridianAngle(final FieldAbsoluteDate<T> date) {
            return date.getField().getZero();
        }

    }


    /** Get a predefined IAU pole.
     * @param body body identifier
     * @param timeScales to use when computing the pole, including TDB and J2000.0.
     * @return predefined IAU pole
     */
    public static PredefinedIAUPoles getIAUPole(final EphemerisType body,
                                                final TimeScales timeScales) {

        switch (body) {
            case SUN :
                return new Sun(timeScales);
            case MERCURY :
                return new Mercury(timeScales);
            case VENUS :
                return new Venus(timeScales);
            case EARTH :
                return new Earth(timeScales);
            case MOON :
                return new Moon(timeScales);
            case MARS :
                return new Mars(timeScales);
            case JUPITER :
                return new Jupiter(timeScales);
            case SATURN :
                return new Saturn(timeScales);
            case URANUS :
                return new Uranus(timeScales);
            case NEPTUNE :
                return new Neptune(timeScales);
            case PLUTO :
                return new Pluto(timeScales);
            default :
                return new GcrfAligned(timeScales);
        }
    }

    /**
     * List of predefined IAU poles.
     *
     * @param timeScales to use when computing the pole, including TDB and J2000.0.
     * @return the poles.
     */
    static List<PredefinedIAUPoles> values(final TimeScales timeScales) {
        final List<PredefinedIAUPoles> values = new ArrayList<>(12);
        values.add(new Sun(timeScales));
        values.add(new Mercury(timeScales));
        values.add(new Venus(timeScales));
        values.add(new Earth(timeScales));
        values.add(new Moon(timeScales));
        values.add(new Mars(timeScales));
        values.add(new Jupiter(timeScales));
        values.add(new Saturn(timeScales));
        values.add(new Uranus(timeScales));
        values.add(new Neptune(timeScales));
        values.add(new Pluto(timeScales));
        values.add(new GcrfAligned(timeScales));
        return values;
    }

    /** Compute the interval in julian centuries from standard epoch.
     * @param date date
     * @return interval between date and standard epoch in julian centuries
     */
    protected double t(final AbsoluteDate date) {
        return date.offsetFrom(timeScales.getJ2000Epoch(), timeScales.getTDB()) /
                Constants.JULIAN_CENTURY;
    }

    /** Compute the interval in julian centuries from standard epoch.
     * @param date date
     * @param <T> type of the filed elements
     * @return interval between date and standard epoch in julian centuries
     */
    protected <T extends CalculusFieldElement<T>> T t(final FieldAbsoluteDate<T> date) {
        final FieldAbsoluteDate<T> j2000Epoch =
                new FieldAbsoluteDate<>(date.getField(), timeScales.getJ2000Epoch());
        return date.offsetFrom(j2000Epoch, timeScales.getTDB()).divide(Constants.JULIAN_CENTURY);
    }

    /** Compute the interval in julian days from standard epoch.
     * @param date date
     * @return interval between date and standard epoch in julian days
     */
    protected double d(final AbsoluteDate date) {
        return date.offsetFrom(timeScales.getJ2000Epoch(), timeScales.getTDB()) /
                Constants.JULIAN_DAY;
    }

    /** Compute the interval in julian days from standard epoch.
     * @param date date
     * @param <T> type of the filed elements
     * @return interval between date and standard epoch in julian days
     */
    protected <T extends CalculusFieldElement<T>> T d(final FieldAbsoluteDate<T> date) {
        final FieldAbsoluteDate<T> j2000Epoch =
                new FieldAbsoluteDate<>(date.getField(), timeScales.getJ2000Epoch());
        return date.offsetFrom(j2000Epoch, timeScales.getTDB()).divide(Constants.JULIAN_DAY);
    }

}
