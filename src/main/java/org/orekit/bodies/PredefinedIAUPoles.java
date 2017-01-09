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
        private final double w0 = FastMath.toRadians(84.176);

        /** Rate term of the prime meridian. */
        private final double wDot = FastMath.toRadians(14.1844000);

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
            return d(date) * wDot + w0;
        }

        /** {@inheritDoc} */
        public <T extends RealFieldElement<T>> T getPrimeMeridianAngle(final FieldAbsoluteDate<T> date) {
            return d(date).multiply(wDot).add(w0);
        }

    },

    /** IAU pole and prime meridian model for Mercury. */
    MERCURY {

        /** Constant term of the right ascension of the pole. */
        private final double alpha0 = FastMath.toRadians(281.0097);

        /** Rate term of the right ascension of the pole. */
        private final double alphaDot = FastMath.toRadians(-0.0328);

        /** Constant term of the declination of the pole. */
        private final double delta0 = FastMath.toRadians(281.0097);

        /** Rate term of the declination of the pole. */
        private final double deltaDot = FastMath.toRadians(-0.0328);

        /** Constant term of the prime meridian. */
        private final double w0 = FastMath.toRadians(329.5469);

        /** Rate term of the prime meridian. */
        private final double wDot = FastMath.toRadians(6.1385025);

        /** M1 coefficient of the prime meridian. */
        private final double m1Coeff = FastMath.toRadians(0.00993822);

        /** M2 coefficient of the prime meridian. */
        private final double m2Coeff = FastMath.toRadians(0.00104581);

        /** M3 coefficient of the prime meridian. */
        private final double m3Coeff = FastMath.toRadians(0.00010280);

        /** M4 coefficient of the prime meridian. */
        private final double m4Coeff = FastMath.toRadians(0.00002364);

        /** M5 coefficient of the prime meridian. */
        private final double m5Coeff = FastMath.toRadians(0.00000532);

        /** Constant term of the M1 angle. */
        private double M10   = FastMath.toRadians(174.791086);

        /** Rate term of the M1 angle. */
        private double M1Dot = FastMath.toRadians(4.092335);

        /** Constant term of the M2 angle. */
        private double M20   = FastMath.toRadians(349.582171);

        /** Rate term of the M1 angle. */
        private double M2Dot = FastMath.toRadians(8.184670);

        /** Constant term of the M3 angle. */
        private double M30   = FastMath.toRadians(164.373257);

        /** Rate term of the M1 angle. */
        private double M3Dot = FastMath.toRadians(12.277005);

        /** Constant term of the M4 angle. */
        private double M40   = FastMath.toRadians(339.164343);

        /** Rate term of the M1 angle. */
        private double M4Dot = FastMath.toRadians(16.369340);

        /** Constant term of the M5 angle. */
        private double M50   = FastMath.toRadians(153.955429);

        /** Rate term of the M1 angle. */
        private double M5Dot = FastMath.toRadians(20.461675);

        /** {@inheritDoc} */
        public Vector3D getPole(final AbsoluteDate date) {
            final double t = t(date);
            return new Vector3D(t * alphaDot + alpha0, t * deltaDot + delta0);
        }

        /** {@inheritDoc} */
        public <T extends RealFieldElement<T>> FieldVector3D<T> getPole(final FieldAbsoluteDate<T> date) {
            final T t = t(date);
            return new FieldVector3D<>(t.multiply(alphaDot).add(alpha0), t.multiply(deltaDot).add(delta0));
        }

        /** {@inheritDoc} */
        public double getPrimeMeridianAngle(final AbsoluteDate date) {
            final double d = d(date);
            return d(date) * wDot + w0 +
                   FastMath.sin(d * M10 + M1Dot) * m1Coeff +
                   FastMath.sin(d * M20 + M2Dot) * m2Coeff +
                   FastMath.sin(d * M30 + M3Dot) * m3Coeff +
                   FastMath.sin(d * M40 + M4Dot) * m4Coeff +
                   FastMath.sin(d * M50 + M5Dot) * m5Coeff;
        }

        /** {@inheritDoc} */
        public <T extends RealFieldElement<T>> T getPrimeMeridianAngle(final FieldAbsoluteDate<T> date) {
            final T d = d(date);
            return d(date).multiply(wDot).add(w0).
                   add(d.multiply(M10).add(M1Dot).sin().multiply(m1Coeff)).
                   add(d.multiply(M20).add(M2Dot).sin().multiply(m2Coeff)).
                   add(d.multiply(M30).add(M3Dot).sin().multiply(m3Coeff)).
                   add(d.multiply(M40).add(M4Dot).sin().multiply(m4Coeff)).
                   add(d.multiply(M50).add(M5Dot).sin().multiply(m5Coeff));
        }

    },

    /** IAU pole and prime meridian model for Venus. */
    VENUS {

        /** Fixed pole. */
        private final Vector3D pole = new Vector3D(FastMath.toRadians(272.76),
                                                   FastMath.toRadians(67.16));

        /** Constant term of the prime meridian. */
        private final double w0 = FastMath.toRadians(160.20);

        /** Rate term of the prime meridian. */
        private final double wDot = FastMath.toRadians(-1.4813688);

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
            return d(date) * wDot + w0;
        }

        /** {@inheritDoc} */
        public <T extends RealFieldElement<T>> T getPrimeMeridianAngle(final FieldAbsoluteDate<T> date) {
            return d(date).multiply(wDot).add(w0);
        }

    },

    /** IAU pole and prime meridian model for Earth. */
    EARTH {

        /** Constant term of the right ascension of the pole. */
        private final double alpha0 = FastMath.toRadians( 0.00);

        /** Rate term of the right ascension of the pole. */
        private final double alphaDot = FastMath.toRadians(-0.641);

        /** Constant term of the declination of the pole. */
        private final double delta0 = FastMath.toRadians(90.00);

        /** Rate term of the declination of the pole. */
        private final double deltaDot = FastMath.toRadians(-0.557);

        /** Constant term of the prime meridian. */
        private final double w0 = FastMath.toRadians(190.147);

        /** Rate term of the prime meridian. */
        private final double wDot = FastMath.toRadians(360.9856235);

        /** {@inheritDoc} */
        public Vector3D getPole(final AbsoluteDate date) {
            final double t = t(date);
            return new Vector3D(t * alphaDot + alpha0, t * deltaDot + delta0);
        }

        /** {@inheritDoc} */
        public <T extends RealFieldElement<T>> FieldVector3D<T> getPole(final FieldAbsoluteDate<T> date) {
            final T t = t(date);
            return new FieldVector3D<>(t.multiply(alphaDot).add(alpha0), t.multiply(deltaDot).add(delta0));
        }

        /** {@inheritDoc} */
        public double getPrimeMeridianAngle(final AbsoluteDate date) {
            return d(date) * wDot + w0;
        }

        /** {@inheritDoc} */
        public <T extends RealFieldElement<T>> T getPrimeMeridianAngle(final FieldAbsoluteDate<T> date) {
            return d(date).multiply(wDot).add(w0);
        }

    },

    /** IAU pole and prime meridian model for the Moon. */
    MOON {

        /** Constant term of the right ascension of the pole. */
        private final double alpha0 = FastMath.toRadians(269.9949);

        /** Rate term of the right ascension of the pole. */
        private final double alphaDot = FastMath.toRadians(0.0031);

        /** Constant term of the declination of the pole. */
        private final double delta0 = FastMath.toRadians(66.5392);

        /** Rate term of the declination of the pole. */
        private final double deltaDot = FastMath.toRadians( 0.0130);

        /** Constant term of the prime meridian. */
        private final double w0 = FastMath.toRadians(38.3213);

        /** Rate term of the prime meridian. */
        private final double wDot = FastMath.toRadians(13.17635815);

        /** Rate term of the prime meridian. */
        private final double wDotDot = FastMath.toRadians(-1.4e-12);

        /** Constant term of the E1 angle. */
        private final double e010    = FastMath.toRadians(125.045);

        /** Rate term of the E1 angle. */
        private final double e01Dot  = FastMath.toRadians( -0.0529921);

        /** Sine coefficient of the E1 angle. */
        private final double e01Sin  = FastMath.toRadians(-3.8787);

        /** Cosine coefficient of the E1 angle. */
        private final double e01Cos  = FastMath.toRadians( 1.5419);

        /** Sine coefficient of the E1 angle, for the prime meridian. */
        private final double e01WSin = FastMath.toRadians( 3.5610);

        /** Constant term of the E2 angle. */
        private final double e020    = FastMath.toRadians(250.089);

        /** Rate term of the E2 angle. */
        private final double e02Dot  = FastMath.toRadians( -0.1059842);

        /** Sine coefficient of the E2 angle. */
        private final double e02Sin  = FastMath.toRadians(-0.1204);

        /** Cosine coefficient of the E2 angle. */
        private final double e02Cos  = FastMath.toRadians( 0.0239);

        /** Sine coefficient of the E2 angle, for the prime meridian. */
        private final double e02WSin = FastMath.toRadians( 0.1208);

        /** Constant term of the E3 angle. */
        private final double e030    = FastMath.toRadians(260.008);

        /** Rate term of the E3 angle. */
        private final double e03Dot  = FastMath.toRadians( 13.0120009);

        /** Sine coefficient of the E3 angle. */
        private final double e03Sin  = FastMath.toRadians( 0.0700);

        /** Cosine coefficient of the E3 angle. */
        private final double e03Cos  = FastMath.toRadians(-0.0278);

        /** Sine coefficient of the E3 angle, for the prime meridian. */
        private final double e03WSin = FastMath.toRadians(-0.0642);

        /** Constant term of the E4 angle. */
        private final double e040    = FastMath.toRadians(176.625);

        /** Rate term of the E4 angle. */
        private final double e04Dot  = FastMath.toRadians( 13.3407154);

        /** Sine coefficient of the E4 angle. */
        private final double e04Sin  = FastMath.toRadians(-0.0172);

        /** Cosine coefficient of the E4 angle. */
        private final double e04Cos  = FastMath.toRadians( 0.0068);

        /** Sine coefficient of the E4 angle, for the prime meridian. */
        private final double e04WSin = FastMath.toRadians( 0.0158);

        /** Constant term of the E5 angle. */
        private final double e050    = FastMath.toRadians(357.529);

        /** Rate term of the E5 angle. */
        private final double e05Dot  = FastMath.toRadians(  0.9856003);

        /** Sine coefficient of the E5 angle, for the prime meridian. */
        private final double e05WSin = FastMath.toRadians( 0.0252);

        /** Constant term of the E6 angle. */
        private final double e060    = FastMath.toRadians(311.589);

        /** Rate term of the E6 angle. */
        private final double e06Dot  = FastMath.toRadians( 26.4057084);

        /** Sine coefficient of the E6 angle. */
        private final double e06Sin  = FastMath.toRadians(0.0072);

        /** Cosine coefficient of the E6 angle. */
        private final double e06Cos  = FastMath.toRadians(-0.0029);

        /** Sine coefficient of the E6 angle, for the prime meridian. */
        private final double e06WSin = FastMath.toRadians(-0.0066);

        /** Constant term of the E7 angle. */
        private final double e070    = FastMath.toRadians(134.963);

        /** Rate term of the E7 angle. */
        private final double e07Dot  = FastMath.toRadians( 13.0649930);

        /** Cosine coefficient of the E7 angle. */
        private final double e07Cos  = FastMath.toRadians( 0.0009);

        /** Sine coefficient of the E7 angle, for the prime meridian. */
        private final double e07WSin = FastMath.toRadians(-0.0047);

        /** Constant term of the E8 angle. */
        private final double e080    = FastMath.toRadians(276.617);

        /** Rate term of the E8 angle. */
        private final double e08Dot  = FastMath.toRadians(  0.3287146);

        /** Sine coefficient of the E8 angle, for the prime meridian. */
        private final double e08WSin = FastMath.toRadians(-0.0046);

        /** Constant term of the E9 angle. */
        private final double e090    = FastMath.toRadians( 34.226);

        /** Rate term of the E9 angle. */
        private final double e09Dot  = FastMath.toRadians(  1.7484877);

        /** Sine coefficient of the E9 angle, for the prime meridian. */
        private final double e09WSin = FastMath.toRadians( 0.0028);

        /** Constant term of the E10 angle. */
        private final double e100    = FastMath.toRadians( 15.134);

        /** Rate term of the E10 angle. */
        private final double e10Dot  = FastMath.toRadians( -0.1589763);

        /** Sine coefficient of the E10 angle. */
        private final double e10Sin  = FastMath.toRadians(-0.0052);

        /** Cosine coefficient of the E10 angle. */
        private final double e10Cos  = FastMath.toRadians(0.0008);

        /** Sine coefficient of the E10 angle, for the prime meridian. */
        private final double e10WSin = FastMath.toRadians( 0.0052);

        /** Constant term of the E11 angle. */
        private final double e110    = FastMath.toRadians(119.743);

        /** Rate term of the E11 angle. */
        private final double e11Dot  = FastMath.toRadians(  0.0036096);

        /** Sine coefficient of the E11 angle, for the prime meridian. */
        private final double e11WSin = FastMath.toRadians( 0.0040);

        /** Constant term of the E12 angle. */
        private final double e120    = FastMath.toRadians(239.961);

        /** Rate term of the E12 angle. */
        private final double e12Dot  = FastMath.toRadians(  0.1643573);

        /** Sine coefficient of the E12 angle, for the prime meridian. */
        private final double e12WSin = FastMath.toRadians( 0.0019);

        /** Constant term of the E13 angle. */
        private final double e130    = FastMath.toRadians( 25.053);

        /** Rate term of the E13 angle. */
        private final double e13Dot  = FastMath.toRadians( 12.9590088);

        /** Sine coefficient of the E13 angle. */
        private final double e13Sin  = FastMath.toRadians(0.0043);

        /** Cosine coefficient of the E13 angle. */
        private final double e13Cos  = FastMath.toRadians(-0.0009);

        /** Sine coefficient of the E13 angle, for the prime meridian. */
        private final double e13WSin = FastMath.toRadians(-0.0044);

        /** {@inheritDoc} */
        public Vector3D getPole(final AbsoluteDate date) {
            final double d = d(date);
            final double t = t(date);
            return new Vector3D(t * alphaDot + alpha0 +
                                FastMath.sin(d * e01Dot + e010) * e01Sin +
                                FastMath.sin(d * e02Dot + e020) * e02Sin +
                                FastMath.sin(d * e03Dot + e030) * e03Sin +
                                FastMath.sin(d * e04Dot + e040) * e04Sin +
                                FastMath.sin(d * e06Dot + e060) * e06Sin +
                                FastMath.sin(d * e10Dot + e100) * e10Sin +
                                FastMath.sin(d * e13Dot + e130) * e13Sin,
                                t * deltaDot + delta0 +
                                FastMath.cos(d * e01Dot + e010) * e01Cos +
                                FastMath.cos(d * e02Dot + e020) * e02Cos +
                                FastMath.cos(d * e03Dot + e030) * e03Cos +
                                FastMath.cos(d * e04Dot + e040) * e04Cos +
                                FastMath.cos(d * e06Dot + e060) * e06Cos +
                                FastMath.cos(d * e07Dot + e070) * e07Cos +
                                FastMath.cos(d * e10Dot + e100) * e10Cos +
                                FastMath.cos(d * e13Dot + e130) * e13Cos);
        }

        /** {@inheritDoc} */
        public <T extends RealFieldElement<T>> FieldVector3D<T> getPole(final FieldAbsoluteDate<T> date) {
            final T d = d(date);
            final T t = t(date);
            return new FieldVector3D<>(t.multiply(alphaDot).add(alpha0).
                                       add(d.multiply(e01Dot).add(e010).sin().multiply(e01Sin)).
                                       add(d.multiply(e02Dot).add(e020).sin().multiply(e02Sin)).
                                       add(d.multiply(e03Dot).add(e030).sin().multiply(e03Sin)).
                                       add(d.multiply(e04Dot).add(e040).sin().multiply(e04Sin)).
                                       add(d.multiply(e06Dot).add(e060).sin().multiply(e06Sin)).
                                       add(d.multiply(e10Dot).add(e100).sin().multiply(e10Sin)).
                                       add(d.multiply(e13Dot).add(e130).sin().multiply(e13Sin)),
                                       t.multiply(deltaDot).add(delta0).
                                       add(d.multiply(e01Dot).add(e010).cos().multiply(e01Cos)).
                                       add(d.multiply(e02Dot).add(e020).cos().multiply(e02Cos)).
                                       add(d.multiply(e03Dot).add(e030).cos().multiply(e03Cos)).
                                       add(d.multiply(e04Dot).add(e040).cos().multiply(e04Cos)).
                                       add(d.multiply(e06Dot).add(e060).cos().multiply(e06Cos)).
                                       add(d.multiply(e07Dot).add(e070).cos().multiply(e07Cos)).
                                       add(d.multiply(e10Dot).add(e100).cos().multiply(e10Cos)).
                                       add(d.multiply(e13Dot).add(e130).cos().multiply(e13Cos)));
        }

        /** {@inheritDoc} */
        public double getPrimeMeridianAngle(final AbsoluteDate date) {
            final double d = d(date);
            return d * (d * wDotDot + wDot) + w0 +
                   FastMath.sin(d * e01Dot + e010) * e01WSin +
                   FastMath.sin(d * e02Dot + e020) * e02WSin +
                   FastMath.sin(d * e03Dot + e030) * e03WSin +
                   FastMath.sin(d * e04Dot + e040) * e04WSin +
                   FastMath.sin(d * e05Dot + e050) * e05WSin +
                   FastMath.sin(d * e06Dot + e060) * e06WSin +
                   FastMath.sin(d * e07Dot + e070) * e07WSin +
                   FastMath.sin(d * e08Dot + e080) * e08WSin +
                   FastMath.sin(d * e09Dot + e090) * e09WSin +
                   FastMath.sin(d * e10Dot + e100) * e10WSin +
                   FastMath.sin(d * e11Dot + e110) * e11WSin +
                   FastMath.sin(d * e12Dot + e120) * e12WSin +
                   FastMath.sin(d * e13Dot + e130) * e13WSin;
        }

        /** {@inheritDoc} */
        public <T extends RealFieldElement<T>> T getPrimeMeridianAngle(final FieldAbsoluteDate<T> date) {
            final T d = d(date);
            return d.multiply(d.multiply(wDotDot).add(wDot)).add(w0).
                   add(d.multiply(e01Dot).add(e010).sin().multiply(e01WSin)).
                   add(d.multiply(e02Dot).add(e020).sin().multiply(e02WSin)).
                   add(d.multiply(e03Dot).add(e030).sin().multiply(e03WSin)).
                   add(d.multiply(e04Dot).add(e040).sin().multiply(e04WSin)).
                   add(d.multiply(e05Dot).add(e050).sin().multiply(e05WSin)).
                   add(d.multiply(e06Dot).add(e060).sin().multiply(e06WSin)).
                   add(d.multiply(e07Dot).add(e070).sin().multiply(e07WSin)).
                   add(d.multiply(e08Dot).add(e080).sin().multiply(e08WSin)).
                   add(d.multiply(e09Dot).add(e090).sin().multiply(e09WSin)).
                   add(d.multiply(e10Dot).add(e100).sin().multiply(e10WSin)).
                   add(d.multiply(e11Dot).add(e110).sin().multiply(e11WSin)).
                   add(d.multiply(e12Dot).add(e120).sin().multiply(e12WSin)).
                   add(d.multiply(e13Dot).add(e130).sin().multiply(e13WSin));
        }

    },

    /** IAU pole and prime meridian model for Mars. */
    MARS {

        /** Constant term of the right ascension of the pole. */
        private final double alpha0 = FastMath.toRadians(317.68143);

        /** Rate term of the right ascension of the pole. */
        private final double alphaDot = FastMath.toRadians(-0.1061);

        /** Constant term of the declination of the pole. */
        private final double delta0 = FastMath.toRadians( 52.88650);

        /** Rate term of the declination of the pole. */
        private final double deltaDot = FastMath.toRadians(-0.0609);

        /** Constant term of the prime meridian. */
        private final double w0 = FastMath.toRadians(176.630);

        /** Rate term of the prime meridian. */
        private final double wDot = FastMath.toRadians(350.89198226);

        /** {@inheritDoc} */
        public Vector3D getPole(final AbsoluteDate date) {
            final double t = t(date);
            return new Vector3D(t * alphaDot + alpha0, t * deltaDot + delta0);
        }

        /** {@inheritDoc} */
        public <T extends RealFieldElement<T>> FieldVector3D<T> getPole(final FieldAbsoluteDate<T> date) {
            final T t = t(date);
            return new FieldVector3D<>(t.multiply(alphaDot).add(alpha0), t.multiply(deltaDot).add(delta0));
        }

        /** {@inheritDoc} */
        public double getPrimeMeridianAngle(final AbsoluteDate date) {
            return d(date) * wDot + w0;
        }

        /** {@inheritDoc} */
        public <T extends RealFieldElement<T>> T getPrimeMeridianAngle(final FieldAbsoluteDate<T> date) {
            return d(date).multiply(wDot).add(w0);
        }

    },

    /** IAU pole and prime meridian model for Jupiter. */
    JUPITER {

        /** Constant term of the right ascension of the pole. */
        private final double alpha0 = FastMath.toRadians(268.056595);

        /** Rate term of the right ascension of the pole. */
        private final double alphaDot = FastMath.toRadians(-0.006499);

        /** Constant term of the declination of the pole. */
        private final double delta0 = FastMath.toRadians(64.495303);

        /** Rate term of the declination of the pole. */
        private final double deltaDot = FastMath.toRadians(0.002413);

        /** Constant term of the ja angle. */
        private final double ja0 = FastMath.toRadians( 99.360714);

        /** Rate term of the ja angle. */
        private final double jaDot = FastMath.toRadians(4850.4046);

        /** Sine coefficient of the ja angle. */
        private final double jaSin = FastMath.toRadians(0.000117);

        /** Cosine coefficient of the ja angle. */
        private final double jaCos = FastMath.toRadians(0.000050);

        /** Constant term of the jb angle. */
        private final double jb0 = FastMath.toRadians(175.895369);

        /** Rate term of the jb angle. */
        private final double jbDot = FastMath.toRadians(1191.9605);

        /** Sine coefficient of the jb angle. */
        private final double jbSin = FastMath.toRadians(0.000938);

        /** Cosine coefficient of the jb angle. */
        private final double jbCos = FastMath.toRadians(0.000404);

        /** Constant term of the jc angle. */
        private final double jc0 = FastMath.toRadians(300.323162);

        /** Rate term of the jc angle. */
        private final double jcDot = FastMath.toRadians(262.5475);

        /** Sine coefficient of the jc angle. */
        private final double jcSin = FastMath.toRadians(0.001432);

        /** Cosine coefficient of the jc angle. */
        private final double jcCos = FastMath.toRadians(0.000617);

        /** Constant term of the jd angle. */
        private final double jd0 = FastMath.toRadians(114.012305);

        /** Rate term of the jd angle. */
        private final double jdDot = FastMath.toRadians(6070.2476);

        /** Sine coefficient of the jd angle. */
        private final double jdSin = FastMath.toRadians(0.000030);

        /** Cosine coefficient of the jd angle. */
        private final double jdCos = FastMath.toRadians(-0.000013);

        /** Constant term of the je angle. */
        private final double je0 = FastMath.toRadians(49.511251);

        /** Rate term of the je angle. */
        private final double jeDot = FastMath.toRadians(64.3000);

        /** Sine coefficient of the je angle. */
        private final double jeSin = FastMath.toRadians(0.002150);

        /** Cosine coefficient of the je angle. */
        private final double jeCos = FastMath.toRadians(0.000926);

        /** Constant term of the prime meridian. */
        private final double w0 = FastMath.toRadians(284.95);

        /** Rate term of the prime meridian. */
        private final double wDot = FastMath.toRadians(870.5360000);

        /** {@inheritDoc} */
        public Vector3D getPole(final AbsoluteDate date) {

            final double t = t(date);
            final double ja = t * jaDot + ja0;
            final double jb = t * jbDot + jb0;
            final double jc = t * jcDot + jc0;
            final double jd = t * jdDot + jd0;
            final double je = t * jeDot + je0;

            return new Vector3D(t * alphaDot + alpha0 +
                                FastMath.sin(ja) * jaSin +
                                FastMath.sin(jb) * jbSin +
                                FastMath.sin(jc) * jcSin +
                                FastMath.sin(jd) * jdSin +
                                FastMath.sin(je) * jeSin,
                                t * deltaDot + delta0 +
                                FastMath.cos(ja) * jaCos +
                                FastMath.cos(jb) * jbCos +
                                FastMath.cos(jc) * jcCos +
                                FastMath.cos(jd) * jdCos +
                                FastMath.cos(je) * jeCos);
        }

        /** {@inheritDoc} */
        public <T extends RealFieldElement<T>> FieldVector3D<T> getPole(final FieldAbsoluteDate<T> date) {

            final T t = t(date);
            final T ja = t.multiply(jaDot).add(ja0);
            final T jb = t.multiply(jbDot).add(jb0);
            final T jc = t.multiply(jcDot).add(jc0);
            final T jd = t.multiply(jdDot).add(jd0);
            final T je = t.multiply(jeDot).add(je0);

            return new FieldVector3D<>(t.multiply(alphaDot).add(alpha0).
                                       add(ja.sin().multiply(jaSin)).
                                       add(jb.sin().multiply(jbSin)).
                                       add(jc.sin().multiply(jcSin)).
                                       add(jd.sin().multiply(jdSin)).
                                       add(je.sin().multiply(jeSin)),
                                       t.multiply(deltaDot).add(delta0).
                                       add(ja.cos().multiply(jaCos)).
                                       add(jb.cos().multiply(jbCos)).
                                       add(jc.cos().multiply(jcCos)).
                                       add(jd.cos().multiply(jdCos)).
                                       add(je.cos().multiply(jeCos)));

        }

        /** {@inheritDoc} */
        public double getPrimeMeridianAngle(final AbsoluteDate date) {
            return d(date) * wDot + w0;
        }

        /** {@inheritDoc} */
        public <T extends RealFieldElement<T>> T getPrimeMeridianAngle(final FieldAbsoluteDate<T> date) {
            return d(date).multiply(wDot).add(w0);
        }

    },

    /** IAU pole and prime meridian model for Saturn. */
    SATURN {

        /** Constant term of the right ascension of the pole. */
        private final double alpha0 = FastMath.toRadians(40.589);

        /** Rate term of the right ascension of the pole. */
        private final double alphaDot = FastMath.toRadians(-0.036);

        /** Constant term of the declination of the pole. */
        private final double delta0 = FastMath.toRadians(83.537);

        /** Rate term of the declination of the pole. */
        private final double deltaDot = FastMath.toRadians(-0.004);

        /** Constant term of the prime meridian. */
        private final double w0 = FastMath.toRadians(38.90);

        /** Rate term of the prime meridian. */
        private final double wDot = FastMath.toRadians(810.7939024);

        /** {@inheritDoc} */
        public Vector3D getPole(final AbsoluteDate date) {
            final double t = t(date);
            return new Vector3D(t * alphaDot + alpha0, t * deltaDot + delta0);
        }

        /** {@inheritDoc} */
        public <T extends RealFieldElement<T>> FieldVector3D<T> getPole(final FieldAbsoluteDate<T> date) {
            final T t = t(date);
            return new FieldVector3D<>(t.multiply(alphaDot).add(alpha0), t.multiply(deltaDot).add(delta0));
        }

        /** {@inheritDoc} */
        public double getPrimeMeridianAngle(final AbsoluteDate date) {
            return d(date) * wDot + w0;
        }

        /** {@inheritDoc} */
        public <T extends RealFieldElement<T>> T getPrimeMeridianAngle(final FieldAbsoluteDate<T> date) {
            return d(date).multiply(wDot).add(w0);
        }

    },

    /** IAU pole and prime meridian model for Uranus. */
    URANUS {

        /** Fixed pole. */
        private final Vector3D pole = new Vector3D(FastMath.toRadians(257.311),
                                                   FastMath.toRadians(-15.175));

        /** Constant term of the prime meridian. */
        private final double w0 = FastMath.toRadians(203.81);

        /** Rate term of the prime meridian. */
        private final double wDot = FastMath.toRadians(-501.1600928);

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
            return d(date) * wDot + w0;
        }

        /** {@inheritDoc} */
        public <T extends RealFieldElement<T>> T getPrimeMeridianAngle(final FieldAbsoluteDate<T> date) {
            return d(date).multiply(wDot).add(w0);
        }

    },

    /** IAU pole and prime meridian model for Neptune. */
    NEPTUNE {

        /** Constant term of the right ascension of the pole. */
        private final double alpha0 = FastMath.toRadians(299.36);

        /** Sine term of the right ascension of the pole. */
        private final double alphaSin = FastMath.toRadians(0.70);

        /** Constant term of the declination of the pole. */
        private final double delta0 = FastMath.toRadians(43.46);

        /** Cosine term of the declination of the pole. */
        private final double deltaCos = FastMath.toRadians(-0.51);

        /** Constant term of the prime meridian. */
        private final double w0 = FastMath.toRadians(253.18);

        /** Rate term of the prime meridian. */
        private final double wDot = FastMath.toRadians(536.3128492);

        /** Sine term of the prime meridian. */
        private final double wSin = FastMath.toRadians(-0.48);

        /** Constant term of the N angle. */
        private double N0   = FastMath.toRadians(357.85);

        /** Rate term of the M1 angle. */
        private double NDot = FastMath.toRadians(52.316);

        /** {@inheritDoc} */
        public Vector3D getPole(final AbsoluteDate date) {
            final double n = t(date) * NDot + N0;
            return new Vector3D(FastMath.sin(n) * alphaSin + alpha0, FastMath.cos(n) * deltaCos + delta0);
        }

        /** {@inheritDoc} */
        public <T extends RealFieldElement<T>> FieldVector3D<T> getPole(final FieldAbsoluteDate<T> date) {
            final T n = t(date).multiply(NDot).add(N0);
            return new FieldVector3D<>(n.sin().multiply(alphaSin).add(alpha0), n.cos().multiply(deltaCos).add(delta0));
        }

        /** {@inheritDoc} */
        public double getPrimeMeridianAngle(final AbsoluteDate date) {
            final double n = t(date) * NDot + N0;
            return d(date) * wDot + FastMath.sin(n) * wSin + w0;
        }

        /** {@inheritDoc} */
        public <T extends RealFieldElement<T>> T getPrimeMeridianAngle(final FieldAbsoluteDate<T> date) {
            final T n = t(date).multiply(NDot).add(N0);
            return d(date).multiply(wDot).add(n.sin().multiply(wSin)).add(w0);
        }

    },

    /** IAU pole and prime meridian model for Pluto. */
    PLUTO {

        /** Fixed pole. */
        private final Vector3D pole = new Vector3D(FastMath.toRadians(132.993),
                                                   FastMath.toRadians(-6.163));

        /** Constant term of the prime meridian. */
        private final double w0 = FastMath.toRadians(302.695);

        /** Rate term of the prime meridian. */
        private final double wDot = FastMath.toRadians(56.3625225);

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
            return d(date) * wDot + w0;
        }

        /** {@inheritDoc} */
        public <T extends RealFieldElement<T>> T getPrimeMeridianAngle(final FieldAbsoluteDate<T> date) {
            return d(date).multiply(wDot).add(w0);
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


}
