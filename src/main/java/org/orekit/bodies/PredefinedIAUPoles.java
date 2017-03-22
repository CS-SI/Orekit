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

        /** Fixed pole. */
        private final Vector3D pole = new Vector3D(FastMath.toRadians(286.13),
                                                   FastMath.toRadians(63.87));

        /** Constant term of the prime meridian. */
        private final double w0 = 84.176;

        /** Rate term of the prime meridian. */
        private final double wDot = 14.1844000;

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
            return FastMath.toRadians(d(date) * wDot + w0);
        }

        /** {@inheritDoc} */
        public <T extends RealFieldElement<T>> T getPrimeMeridianAngle(final FieldAbsoluteDate<T> date) {
            return toRadians(d(date).multiply(wDot).add(w0));
        }

    },

    /** IAU pole and prime meridian model for Mercury. */
    MERCURY {

        /** Constant term of the right ascension of the pole. */
        private final double alpha0 = 281.0097;

        /** Rate term of the right ascension of the pole. */
        private final double alphaDot = -0.0328;

        /** Constant term of the declination of the pole. */
        private final double delta0 = 61.4143;

        /** Rate term of the declination of the pole. */
        private final double deltaDot = -0.0049;

        /** Constant term of the prime meridian. */
        private final double w0 = 329.5469;

        /** Rate term of the prime meridian. */
        private final double wDot = 6.1385025;

        /** M1 coefficient of the prime meridian. */
        private final double m1Coeff = 0.00993822;

        /** M2 coefficient of the prime meridian. */
        private final double m2Coeff = -0.00104581;

        /** M3 coefficient of the prime meridian. */
        private final double m3Coeff = -0.00010280;

        /** M4 coefficient of the prime meridian. */
        private final double m4Coeff = -0.00002364;

        /** M5 coefficient of the prime meridian. */
        private final double m5Coeff = -0.00000532;

        /** Constant term of the M1 angle. */
        private double M10   = 174.791086;

        /** Rate term of the M1 angle. */
        private double M1Dot = 4.092335;

        /** Constant term of the M2 angle. */
        private double M20   = 349.582171;

        /** Rate term of the M1 angle. */
        private double M2Dot = 8.184670;

        /** Constant term of the M3 angle. */
        private double M30   = 164.373257;

        /** Rate term of the M1 angle. */
        private double M3Dot = 12.277005;

        /** Constant term of the M4 angle. */
        private double M40   = 339.164343;

        /** Rate term of the M1 angle. */
        private double M4Dot = 16.369340;

        /** Constant term of the M5 angle. */
        private double M50   = 153.955429;

        /** Rate term of the M1 angle. */
        private double M5Dot = 20.461675;

        /** {@inheritDoc} */
        public Vector3D getPole(final AbsoluteDate date) {
            final double t = t(date);
            return new Vector3D(FastMath.toRadians(t * alphaDot + alpha0),
                                FastMath.toRadians(t * deltaDot + delta0));
        }

        /** {@inheritDoc} */
        public <T extends RealFieldElement<T>> FieldVector3D<T> getPole(final FieldAbsoluteDate<T> date) {
            final T t = t(date);
            return new FieldVector3D<>(toRadians(t.multiply(alphaDot).add(alpha0)),
                                       toRadians(t.multiply(deltaDot).add(delta0)));
        }

        /** {@inheritDoc} */
        public double getPrimeMeridianAngle(final AbsoluteDate date) {
            final double d = d(date);
            return FastMath.toRadians(d(date) * wDot + w0 +
                                      FastMath.sin(FastMath.toRadians(d * M1Dot + M10)) * m1Coeff +
                                      FastMath.sin(FastMath.toRadians(d * M2Dot + M20)) * m2Coeff +
                                      FastMath.sin(FastMath.toRadians(d * M3Dot + M30)) * m3Coeff +
                                      FastMath.sin(FastMath.toRadians(d * M4Dot + M40)) * m4Coeff +
                                      FastMath.sin(FastMath.toRadians(d * M5Dot + M50)) * m5Coeff);
        }

        /** {@inheritDoc} */
        public <T extends RealFieldElement<T>> T getPrimeMeridianAngle(final FieldAbsoluteDate<T> date) {
            final T d = d(date);
            return toRadians(d(date).multiply(wDot).add(w0).
                             add(toRadians(d.multiply(M1Dot).add(M10)).sin().multiply(m1Coeff)).
                             add(toRadians(d.multiply(M2Dot).add(M20)).sin().multiply(m2Coeff)).
                             add(toRadians(d.multiply(M3Dot).add(M30)).sin().multiply(m3Coeff)).
                             add(toRadians(d.multiply(M4Dot).add(M40)).sin().multiply(m4Coeff)).
                             add(toRadians(d.multiply(M5Dot).add(M50)).sin().multiply(m5Coeff)));
        }

    },

    /** IAU pole and prime meridian model for Venus. */
    VENUS {

        /** Fixed pole. */
        private final Vector3D pole = new Vector3D(FastMath.toRadians(272.76),
                                                   FastMath.toRadians(67.16));

        /** Constant term of the prime meridian. */
        private final double w0 = 160.20;

        /** Rate term of the prime meridian. */
        private final double wDot = -1.4813688;

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
            return FastMath.toRadians(d(date) * wDot + w0);
        }

        /** {@inheritDoc} */
        public <T extends RealFieldElement<T>> T getPrimeMeridianAngle(final FieldAbsoluteDate<T> date) {
            return toRadians(d(date).multiply(wDot).add(w0));
        }

    },

    /** IAU pole and prime meridian model for Earth. */
    EARTH {

        /** Constant term of the right ascension of the pole. */
        private final double alpha0 =  0.00;

        /** Rate term of the right ascension of the pole. */
        private final double alphaDot = -0.641;

        /** Constant term of the declination of the pole. */
        private final double delta0 = 90.00;

        /** Rate term of the declination of the pole. */
        private final double deltaDot = -0.557;

        /** Constant term of the prime meridian. */
        private final double w0 = 190.147;

        /** Rate term of the prime meridian. */
        private final double wDot = 360.9856235;

        /** {@inheritDoc} */
        public Vector3D getPole(final AbsoluteDate date) {
            final double t = t(date);
            return new Vector3D(FastMath.toRadians(t * alphaDot + alpha0),
                                FastMath.toRadians(t * deltaDot + delta0));
        }

        /** {@inheritDoc} */
        public <T extends RealFieldElement<T>> FieldVector3D<T> getPole(final FieldAbsoluteDate<T> date) {
            final T t = t(date);
            return new FieldVector3D<>(toRadians(t.multiply(alphaDot).add(alpha0)),
                                       toRadians(t.multiply(deltaDot).add(delta0)));
        }

        /** {@inheritDoc} */
        public double getPrimeMeridianAngle(final AbsoluteDate date) {
            return FastMath.toRadians(d(date) * wDot + w0);
        }

        /** {@inheritDoc} */
        public <T extends RealFieldElement<T>> T getPrimeMeridianAngle(final FieldAbsoluteDate<T> date) {
            return toRadians(d(date).multiply(wDot).add(w0));
        }

    },

    /** IAU pole and prime meridian model for the Moon. */
    MOON {

        /** Constant term of the right ascension of the pole. */
        private final double alpha0 = 269.9949;

        /** Rate term of the right ascension of the pole. */
        private final double alphaDot = 0.0031;

        /** Constant term of the declination of the pole. */
        private final double delta0 = 66.5392;

        /** Rate term of the declination of the pole. */
        private final double deltaDot =  0.0130;

        /** Constant term of the prime meridian. */
        private final double w0 = 38.3213;

        /** Rate term of the prime meridian. */
        private final double wDot = 13.17635815;

        /** Rate term of the prime meridian. */
        private final double wDotDot = -1.4e-12;

        /** Constant term of the E1 angle. */
        private final double e010    = 125.045;

        /** Rate term of the E1 angle. */
        private final double e01Dot  =  -0.0529921;

        /** Sine coefficient of the E1 angle. */
        private final double e01Sin  = -3.8787;

        /** Cosine coefficient of the E1 angle. */
        private final double e01Cos  =  1.5419;

        /** Sine coefficient of the E1 angle, for the prime meridian. */
        private final double e01WSin =  3.5610;

        /** Constant term of the E2 angle. */
        private final double e020    = 250.089;

        /** Rate term of the E2 angle. */
        private final double e02Dot  =  -0.1059842;

        /** Sine coefficient of the E2 angle. */
        private final double e02Sin  = -0.1204;

        /** Cosine coefficient of the E2 angle. */
        private final double e02Cos  =  0.0239;

        /** Sine coefficient of the E2 angle, for the prime meridian. */
        private final double e02WSin =  0.1208;

        /** Constant term of the E3 angle. */
        private final double e030    = 260.008;

        /** Rate term of the E3 angle. */
        private final double e03Dot  =  13.0120009;

        /** Sine coefficient of the E3 angle. */
        private final double e03Sin  =  0.0700;

        /** Cosine coefficient of the E3 angle. */
        private final double e03Cos  = -0.0278;

        /** Sine coefficient of the E3 angle, for the prime meridian. */
        private final double e03WSin = -0.0642;

        /** Constant term of the E4 angle. */
        private final double e040    = 176.625;

        /** Rate term of the E4 angle. */
        private final double e04Dot  =  13.3407154;

        /** Sine coefficient of the E4 angle. */
        private final double e04Sin  = -0.0172;

        /** Cosine coefficient of the E4 angle. */
        private final double e04Cos  =  0.0068;

        /** Sine coefficient of the E4 angle, for the prime meridian. */
        private final double e04WSin =  0.0158;

        /** Constant term of the E5 angle. */
        private final double e050    = 357.529;

        /** Rate term of the E5 angle. */
        private final double e05Dot  =   0.9856003;

        /** Sine coefficient of the E5 angle, for the prime meridian. */
        private final double e05WSin =  0.0252;

        /** Constant term of the E6 angle. */
        private final double e060    = 311.589;

        /** Rate term of the E6 angle. */
        private final double e06Dot  =  26.4057084;

        /** Sine coefficient of the E6 angle. */
        private final double e06Sin  = 0.0072;

        /** Cosine coefficient of the E6 angle. */
        private final double e06Cos  = -0.0029;

        /** Sine coefficient of the E6 angle, for the prime meridian. */
        private final double e06WSin = -0.0066;

        /** Constant term of the E7 angle. */
        private final double e070    = 134.963;

        /** Rate term of the E7 angle. */
        private final double e07Dot  =  13.0649930;

        /** Cosine coefficient of the E7 angle. */
        private final double e07Cos  =  0.0009;

        /** Sine coefficient of the E7 angle, for the prime meridian. */
        private final double e07WSin = -0.0047;

        /** Constant term of the E8 angle. */
        private final double e080    = 276.617;

        /** Rate term of the E8 angle. */
        private final double e08Dot  =   0.3287146;

        /** Sine coefficient of the E8 angle, for the prime meridian. */
        private final double e08WSin = -0.0046;

        /** Constant term of the E9 angle. */
        private final double e090    =  34.226;

        /** Rate term of the E9 angle. */
        private final double e09Dot  =   1.7484877;

        /** Sine coefficient of the E9 angle, for the prime meridian. */
        private final double e09WSin =  0.0028;

        /** Constant term of the E10 angle. */
        private final double e100    =  15.134;

        /** Rate term of the E10 angle. */
        private final double e10Dot  =  -0.1589763;

        /** Sine coefficient of the E10 angle. */
        private final double e10Sin  = -0.0052;

        /** Cosine coefficient of the E10 angle. */
        private final double e10Cos  = 0.0008;

        /** Sine coefficient of the E10 angle, for the prime meridian. */
        private final double e10WSin =  0.0052;

        /** Constant term of the E11 angle. */
        private final double e110    = 119.743;

        /** Rate term of the E11 angle. */
        private final double e11Dot  =   0.0036096;

        /** Sine coefficient of the E11 angle, for the prime meridian. */
        private final double e11WSin =  0.0040;

        /** Constant term of the E12 angle. */
        private final double e120    = 239.961;

        /** Rate term of the E12 angle. */
        private final double e12Dot  =   0.1643573;

        /** Sine coefficient of the E12 angle, for the prime meridian. */
        private final double e12WSin =  0.0019;

        /** Constant term of the E13 angle. */
        private final double e130    =  25.053;

        /** Rate term of the E13 angle. */
        private final double e13Dot  =  12.9590088;

        /** Sine coefficient of the E13 angle. */
        private final double e13Sin  = 0.0043;

        /** Cosine coefficient of the E13 angle. */
        private final double e13Cos  = -0.0009;

        /** Sine coefficient of the E13 angle, for the prime meridian. */
        private final double e13WSin = -0.0044;

        /** {@inheritDoc} */
        public Vector3D getPole(final AbsoluteDate date) {
            final double d = d(date);
            final double t = t(date);
            return new Vector3D(FastMath.toRadians(t * alphaDot + alpha0 +
                                                   FastMath.sin(FastMath.toRadians(d * e01Dot + e010)) * e01Sin +
                                                   FastMath.sin(FastMath.toRadians(d * e02Dot + e020)) * e02Sin +
                                                   FastMath.sin(FastMath.toRadians(d * e03Dot + e030)) * e03Sin +
                                                   FastMath.sin(FastMath.toRadians(d * e04Dot + e040)) * e04Sin +
                                                   FastMath.sin(FastMath.toRadians(d * e06Dot + e060)) * e06Sin +
                                                   FastMath.sin(FastMath.toRadians(d * e10Dot + e100)) * e10Sin +
                                                   FastMath.sin(FastMath.toRadians(d * e13Dot + e130)) * e13Sin),
                                FastMath.toRadians(t * deltaDot + delta0 +
                                                   FastMath.cos(FastMath.toRadians(d * e01Dot + e010)) * e01Cos +
                                                   FastMath.cos(FastMath.toRadians(d * e02Dot + e020)) * e02Cos +
                                                   FastMath.cos(FastMath.toRadians(d * e03Dot + e030)) * e03Cos +
                                                   FastMath.cos(FastMath.toRadians(d * e04Dot + e040)) * e04Cos +
                                                   FastMath.cos(FastMath.toRadians(d * e06Dot + e060)) * e06Cos +
                                                   FastMath.cos(FastMath.toRadians(d * e07Dot + e070)) * e07Cos +
                                                   FastMath.cos(FastMath.toRadians(d * e10Dot + e100)) * e10Cos +
                                                   FastMath.cos(FastMath.toRadians(d * e13Dot + e130)) * e13Cos));
        }

        /** {@inheritDoc} */
        public <T extends RealFieldElement<T>> FieldVector3D<T> getPole(final FieldAbsoluteDate<T> date) {
            final T d = d(date);
            final T t = t(date);
            return new FieldVector3D<>(toRadians(t.multiply(alphaDot).add(alpha0).
                                                 add(toRadians(d.multiply(e01Dot).add(e010)).sin().multiply(e01Sin)).
                                                 add(toRadians(d.multiply(e02Dot).add(e020)).sin().multiply(e02Sin)).
                                                 add(toRadians(d.multiply(e03Dot).add(e030)).sin().multiply(e03Sin)).
                                                 add(toRadians(d.multiply(e04Dot).add(e040)).sin().multiply(e04Sin)).
                                                 add(toRadians(d.multiply(e06Dot).add(e060)).sin().multiply(e06Sin)).
                                                 add(toRadians(d.multiply(e10Dot).add(e100)).sin().multiply(e10Sin)).
                                                 add(toRadians(d.multiply(e13Dot).add(e130)).sin().multiply(e13Sin))),
                                       toRadians(t.multiply(deltaDot).add(delta0).
                                                 add(toRadians(d.multiply(e01Dot).add(e010)).cos().multiply(e01Cos)).
                                                 add(toRadians(d.multiply(e02Dot).add(e020)).cos().multiply(e02Cos)).
                                                 add(toRadians(d.multiply(e03Dot).add(e030)).cos().multiply(e03Cos)).
                                                 add(toRadians(d.multiply(e04Dot).add(e040)).cos().multiply(e04Cos)).
                                                 add(toRadians(d.multiply(e06Dot).add(e060)).cos().multiply(e06Cos)).
                                                 add(toRadians(d.multiply(e07Dot).add(e070)).cos().multiply(e07Cos)).
                                                 add(toRadians(d.multiply(e10Dot).add(e100)).cos().multiply(e10Cos)).
                                                 add(toRadians(d.multiply(e13Dot).add(e130)).cos().multiply(e13Cos))));
        }

        /** {@inheritDoc} */
        public double getPrimeMeridianAngle(final AbsoluteDate date) {
            final double d = d(date);
            return FastMath.toRadians(d * (d * wDotDot + wDot) + w0 +
                                      FastMath.sin(FastMath.toRadians(d * e01Dot + e010)) * e01WSin +
                                      FastMath.sin(FastMath.toRadians(d * e02Dot + e020)) * e02WSin +
                                      FastMath.sin(FastMath.toRadians(d * e03Dot + e030)) * e03WSin +
                                      FastMath.sin(FastMath.toRadians(d * e04Dot + e040)) * e04WSin +
                                      FastMath.sin(FastMath.toRadians(d * e05Dot + e050)) * e05WSin +
                                      FastMath.sin(FastMath.toRadians(d * e06Dot + e060)) * e06WSin +
                                      FastMath.sin(FastMath.toRadians(d * e07Dot + e070)) * e07WSin +
                                      FastMath.sin(FastMath.toRadians(d * e08Dot + e080)) * e08WSin +
                                      FastMath.sin(FastMath.toRadians(d * e09Dot + e090)) * e09WSin +
                                      FastMath.sin(FastMath.toRadians(d * e10Dot + e100)) * e10WSin +
                                      FastMath.sin(FastMath.toRadians(d * e11Dot + e110)) * e11WSin +
                                      FastMath.sin(FastMath.toRadians(d * e12Dot + e120)) * e12WSin +
                                      FastMath.sin(FastMath.toRadians(d * e13Dot + e130)) * e13WSin);
        }

        /** {@inheritDoc} */
        public <T extends RealFieldElement<T>> T getPrimeMeridianAngle(final FieldAbsoluteDate<T> date) {
            final T d = d(date);
            return toRadians(d.multiply(d.multiply(wDotDot).add(wDot)).add(w0).
                             add(toRadians(d.multiply(e01Dot).add(e010)).sin().multiply(e01WSin)).
                             add(toRadians(d.multiply(e02Dot).add(e020)).sin().multiply(e02WSin)).
                             add(toRadians(d.multiply(e03Dot).add(e030)).sin().multiply(e03WSin)).
                             add(toRadians(d.multiply(e04Dot).add(e040)).sin().multiply(e04WSin)).
                             add(toRadians(d.multiply(e05Dot).add(e050)).sin().multiply(e05WSin)).
                             add(toRadians(d.multiply(e06Dot).add(e060)).sin().multiply(e06WSin)).
                             add(toRadians(d.multiply(e07Dot).add(e070)).sin().multiply(e07WSin)).
                             add(toRadians(d.multiply(e08Dot).add(e080)).sin().multiply(e08WSin)).
                             add(toRadians(d.multiply(e09Dot).add(e090)).sin().multiply(e09WSin)).
                             add(toRadians(d.multiply(e10Dot).add(e100)).sin().multiply(e10WSin)).
                             add(toRadians(d.multiply(e11Dot).add(e110)).sin().multiply(e11WSin)).
                             add(toRadians(d.multiply(e12Dot).add(e120)).sin().multiply(e12WSin)).
                             add(toRadians(d.multiply(e13Dot).add(e130)).sin().multiply(e13WSin)));
        }

    },

    /** IAU pole and prime meridian model for Mars. */
    MARS {

        /** Constant term of the right ascension of the pole. */
        private final double alpha0 = 317.68143;

        /** Rate term of the right ascension of the pole. */
        private final double alphaDot = -0.1061;

        /** Constant term of the declination of the pole. */
        private final double delta0 =  52.88650;

        /** Rate term of the declination of the pole. */
        private final double deltaDot = -0.0609;

        /** Constant term of the prime meridian. */
        private final double w0 = 176.630;

        /** Rate term of the prime meridian. */
        private final double wDot = 350.89198226;

        /** {@inheritDoc} */
        public Vector3D getPole(final AbsoluteDate date) {
            final double t = t(date);
            return new Vector3D(FastMath.toRadians(t * alphaDot + alpha0),
                                FastMath.toRadians(t * deltaDot + delta0));
        }

        /** {@inheritDoc} */
        public <T extends RealFieldElement<T>> FieldVector3D<T> getPole(final FieldAbsoluteDate<T> date) {
            final T t = t(date);
            return new FieldVector3D<>(toRadians(t.multiply(alphaDot).add(alpha0)),
                                       toRadians(t.multiply(deltaDot).add(delta0)));
        }

        /** {@inheritDoc} */
        public double getPrimeMeridianAngle(final AbsoluteDate date) {
            return FastMath.toRadians(d(date) * wDot + w0);
        }

        /** {@inheritDoc} */
        public <T extends RealFieldElement<T>> T getPrimeMeridianAngle(final FieldAbsoluteDate<T> date) {
            return toRadians(d(date).multiply(wDot).add(w0));
        }

    },

    /** IAU pole and prime meridian model for Jupiter. */
    JUPITER {

        /** Constant term of the right ascension of the pole. */
        private final double alpha0 = 268.056595;

        /** Rate term of the right ascension of the pole. */
        private final double alphaDot = -0.006499;

        /** Constant term of the declination of the pole. */
        private final double delta0 = 64.495303;

        /** Rate term of the declination of the pole. */
        private final double deltaDot = 0.002413;

        /** Constant term of the ja angle. */
        private final double ja0 =  99.360714;

        /** Rate term of the ja angle. */
        private final double jaDot = 4850.4046;

        /** Sine coefficient of the ja angle. */
        private final double jaSin = 0.000117;

        /** Cosine coefficient of the ja angle. */
        private final double jaCos = 0.000050;

        /** Constant term of the jb angle. */
        private final double jb0 = 175.895369;

        /** Rate term of the jb angle. */
        private final double jbDot = 1191.9605;

        /** Sine coefficient of the jb angle. */
        private final double jbSin = 0.000938;

        /** Cosine coefficient of the jb angle. */
        private final double jbCos = 0.000404;

        /** Constant term of the jc angle. */
        private final double jc0 = 300.323162;

        /** Rate term of the jc angle. */
        private final double jcDot = 262.5475;

        /** Sine coefficient of the jc angle. */
        private final double jcSin = 0.001432;

        /** Cosine coefficient of the jc angle. */
        private final double jcCos = 0.000617;

        /** Constant term of the jd angle. */
        private final double jd0 = 114.012305;

        /** Rate term of the jd angle. */
        private final double jdDot = 6070.2476;

        /** Sine coefficient of the jd angle. */
        private final double jdSin = 0.000030;

        /** Cosine coefficient of the jd angle. */
        private final double jdCos = -0.000013;

        /** Constant term of the je angle. */
        private final double je0 = 49.511251;

        /** Rate term of the je angle. */
        private final double jeDot = 64.3000;

        /** Sine coefficient of the je angle. */
        private final double jeSin = 0.002150;

        /** Cosine coefficient of the je angle. */
        private final double jeCos = 0.000926;

        /** Constant term of the prime meridian. */
        private final double w0 = 284.95;

        /** Rate term of the prime meridian. */
        private final double wDot = 870.5360000;

        /** {@inheritDoc} */
        public Vector3D getPole(final AbsoluteDate date) {

            final double t = t(date);
            final double ja = FastMath.toRadians(t * jaDot + ja0);
            final double jb = FastMath.toRadians(t * jbDot + jb0);
            final double jc = FastMath.toRadians(t * jcDot + jc0);
            final double jd = FastMath.toRadians(t * jdDot + jd0);
            final double je = FastMath.toRadians(t * jeDot + je0);

            return new Vector3D(FastMath.toRadians(t * alphaDot + alpha0 +
                                                   FastMath.sin(ja) * jaSin +
                                                   FastMath.sin(jb) * jbSin +
                                                   FastMath.sin(jc) * jcSin +
                                                   FastMath.sin(jd) * jdSin +
                                                   FastMath.sin(je) * jeSin),
                                FastMath.toRadians(t * deltaDot + delta0 +
                                                   FastMath.cos(ja) * jaCos +
                                                   FastMath.cos(jb) * jbCos +
                                                   FastMath.cos(jc) * jcCos +
                                                   FastMath.cos(jd) * jdCos +
                                                   FastMath.cos(je) * jeCos));
        }

        /** {@inheritDoc} */
        public <T extends RealFieldElement<T>> FieldVector3D<T> getPole(final FieldAbsoluteDate<T> date) {

            final T t = t(date);
            final T ja = toRadians(t.multiply(jaDot).add(ja0));
            final T jb = toRadians(t.multiply(jbDot).add(jb0));
            final T jc = toRadians(t.multiply(jcDot).add(jc0));
            final T jd = toRadians(t.multiply(jdDot).add(jd0));
            final T je = toRadians(t.multiply(jeDot).add(je0));

            return new FieldVector3D<>(toRadians(t.multiply(alphaDot).add(alpha0).
                                                 add(ja.sin().multiply(jaSin)).
                                                 add(jb.sin().multiply(jbSin)).
                                                 add(jc.sin().multiply(jcSin)).
                                                 add(jd.sin().multiply(jdSin)).
                                                 add(je.sin().multiply(jeSin))),
                                       toRadians(t.multiply(deltaDot).add(delta0).
                                                 add(ja.cos().multiply(jaCos)).
                                                 add(jb.cos().multiply(jbCos)).
                                                 add(jc.cos().multiply(jcCos)).
                                                 add(jd.cos().multiply(jdCos)).
                                                 add(je.cos().multiply(jeCos))));

        }

        /** {@inheritDoc} */
        public double getPrimeMeridianAngle(final AbsoluteDate date) {
            return FastMath.toRadians(d(date) * wDot + w0);
        }

        /** {@inheritDoc} */
        public <T extends RealFieldElement<T>> T getPrimeMeridianAngle(final FieldAbsoluteDate<T> date) {
            return toRadians(d(date).multiply(wDot).add(w0));
        }

    },

    /** IAU pole and prime meridian model for Saturn. */
    SATURN {

        /** Constant term of the right ascension of the pole. */
        private final double alpha0 = 40.589;

        /** Rate term of the right ascension of the pole. */
        private final double alphaDot = -0.036;

        /** Constant term of the declination of the pole. */
        private final double delta0 = 83.537;

        /** Rate term of the declination of the pole. */
        private final double deltaDot = -0.004;

        /** Constant term of the prime meridian. */
        private final double w0 = 38.90;

        /** Rate term of the prime meridian. */
        private final double wDot = 810.7939024;

        /** {@inheritDoc} */
        public Vector3D getPole(final AbsoluteDate date) {
            final double t = t(date);
            return new Vector3D(FastMath.toRadians(t * alphaDot + alpha0),
                                FastMath.toRadians(t * deltaDot + delta0));
        }

        /** {@inheritDoc} */
        public <T extends RealFieldElement<T>> FieldVector3D<T> getPole(final FieldAbsoluteDate<T> date) {
            final T t = t(date);
            return new FieldVector3D<>(toRadians(t.multiply(alphaDot).add(alpha0)),
                                       toRadians(t.multiply(deltaDot).add(delta0)));
        }

        /** {@inheritDoc} */
        public double getPrimeMeridianAngle(final AbsoluteDate date) {
            return FastMath.toRadians(d(date) * wDot + w0);
        }

        /** {@inheritDoc} */
        public <T extends RealFieldElement<T>> T getPrimeMeridianAngle(final FieldAbsoluteDate<T> date) {
            return toRadians(d(date).multiply(wDot).add(w0));
        }

    },

    /** IAU pole and prime meridian model for Uranus. */
    URANUS {

        /** Fixed pole. */
        private final Vector3D pole = new Vector3D(FastMath.toRadians(257.311),
                                                   FastMath.toRadians(-15.175));

        /** Constant term of the prime meridian. */
        private final double w0 = 203.81;

        /** Rate term of the prime meridian. */
        private final double wDot = -501.1600928;

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
            return FastMath.toRadians(d(date) * wDot + w0);
        }

        /** {@inheritDoc} */
        public <T extends RealFieldElement<T>> T getPrimeMeridianAngle(final FieldAbsoluteDate<T> date) {
            return toRadians(d(date).multiply(wDot).add(w0));
        }

    },

    /** IAU pole and prime meridian model for Neptune. */
    NEPTUNE {

        /** Constant term of the right ascension of the pole. */
        private final double alpha0 = 299.36;

        /** Sine term of the right ascension of the pole. */
        private final double alphaSin = 0.70;

        /** Constant term of the declination of the pole. */
        private final double delta0 = 43.46;

        /** Cosine term of the declination of the pole. */
        private final double deltaCos = -0.51;

        /** Constant term of the prime meridian. */
        private final double w0 = 253.18;

        /** Rate term of the prime meridian. */
        private final double wDot = 536.3128492;

        /** Sine term of the prime meridian. */
        private final double wSin = -0.48;

        /** Constant term of the N angle. */
        private double N0   = 357.85;

        /** Rate term of the M1 angle. */
        private double NDot = 52.316;

        /** {@inheritDoc} */
        public Vector3D getPole(final AbsoluteDate date) {
            final double n = FastMath.toRadians(t(date) * NDot + N0);
            return new Vector3D(FastMath.toRadians(FastMath.sin(n) * alphaSin + alpha0),
                                FastMath.toRadians(FastMath.cos(n) * deltaCos + delta0));
        }

        /** {@inheritDoc} */
        public <T extends RealFieldElement<T>> FieldVector3D<T> getPole(final FieldAbsoluteDate<T> date) {
            final T n = toRadians(t(date).multiply(NDot).add(N0));
            return new FieldVector3D<>(toRadians(n.sin().multiply(alphaSin).add(alpha0)),
                                       toRadians(n.cos().multiply(deltaCos).add(delta0)));
        }

        /** {@inheritDoc} */
        public double getPrimeMeridianAngle(final AbsoluteDate date) {
            final double n = FastMath.toRadians(t(date) * NDot + N0);
            return FastMath.toRadians(d(date) * wDot + FastMath.sin(n) * wSin + w0);
        }

        /** {@inheritDoc} */
        public <T extends RealFieldElement<T>> T getPrimeMeridianAngle(final FieldAbsoluteDate<T> date) {
            final T n = toRadians(t(date).multiply(NDot).add(N0));
            return toRadians(d(date).multiply(wDot).add(n.sin().multiply(wSin)).add(w0));
        }

    },

    /** IAU pole and prime meridian model for Pluto. */
    PLUTO {

        /** Fixed pole. */
        private final Vector3D pole = new Vector3D(FastMath.toRadians(132.993),
                                                   FastMath.toRadians(-6.163));

        /** Constant term of the prime meridian. */
        private final double w0 = 302.695;

        /** Rate term of the prime meridian. */
        private final double wDot = 56.3625225;

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
            return FastMath.toRadians(d(date) * wDot + w0);
        }

        /** {@inheritDoc} */
        public <T extends RealFieldElement<T>> T getPrimeMeridianAngle(final FieldAbsoluteDate<T> date) {
            return toRadians(d(date).multiply(wDot).add(w0));
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
