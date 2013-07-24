/* Copyright 2002-2013 CS Systèmes d'Information
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

import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;

/** Mean Equator, Mean Equinox Frame.
 * <p>This providers handles precession effects according to the IAU-76 model, which
 * is decribed in the Lieske paper: <a
 * href="http://articles.adsabs.harvard.edu/cgi-bin/nph-iarticle_query?1979A%26A....73..282L&defaultprint=YES&filetype=.pdf.">
 * Precession Matrix Based on IAU (1976) System of Astronomical Constants</a>, Astronomy and Astrophysics,
 * vol. 73, no. 3, Mar. 1979, p. 282-284.</p>
 * <p>Its parent frame is the GCRF frame.<p>
 * @author Pascal Parraud
 * @author Luc Maisonobe
 */
class MODProvider implements TransformProvider {

    /** Serializable UID. */
    private static final long serialVersionUID = 20130724L;

    /** Constant term of the 1st coefficient for ZETA precession angle. */
    private static final double ZETA_1_0  = 2306.2181 * Constants.ARC_SECONDS_TO_RADIANS;
    /** Linear term of the 1st coefficient for ZETA precession angle. */
    private static final double ZETA_1_1  =  1.39656  * Constants.ARC_SECONDS_TO_RADIANS;
    /** Quadratic term of the 1st coefficient for ZETA precession angle. */
    private static final double ZETA_1_2  = -0.000139 * Constants.ARC_SECONDS_TO_RADIANS;
    /** Constant term of the 2nd coefficient for ZETA precession angle. */
    private static final double ZETA_2_0  =  0.30188  * Constants.ARC_SECONDS_TO_RADIANS;
    /** Linear term of the 2nd coefficient for ZETA precession angle. */
    private static final double ZETA_2_1  = -0.000344 * Constants.ARC_SECONDS_TO_RADIANS;
    /** Constant term of the 3rd coefficient for ZETA precession angle. */
    private static final double ZETA_3_0  =  0.017998 * Constants.ARC_SECONDS_TO_RADIANS;

    /** Constant term of the 1st coefficient for THETA precession angle. */
    private static final double THETA_1_0 = 2004.3109 * Constants.ARC_SECONDS_TO_RADIANS;
    /** Linear term of the 1st coefficient for THETA precession angle. */
    private static final double THETA_1_1 = -0.85330 * Constants.ARC_SECONDS_TO_RADIANS;
    /** Quadratic term of the 1st coefficient for THETA precession angle. */
    private static final double THETA_1_2 = -0.000217 * Constants.ARC_SECONDS_TO_RADIANS;
    /** Constant term of the 2nd coefficient for THETA precession angle. */
    private static final double THETA_2_0 = -0.42665  * Constants.ARC_SECONDS_TO_RADIANS;
    /** Linear term of the 2nd coefficient for THETA precession angle. */
    private static final double THETA_2_1 = -0.000217 * Constants.ARC_SECONDS_TO_RADIANS;
    /** Constant term of the 3rd coefficient for THETA precession angle. */
    private static final double THETA_3_0 = -0.041833 * Constants.ARC_SECONDS_TO_RADIANS;

    /** Constant term of the 1st coefficient for Z precession angle. */
    private static final double Z_1_0     = 2306.2181 * Constants.ARC_SECONDS_TO_RADIANS;
    /** Linear term of the 1st coefficient for Z precession angle. */
    private static final double Z_1_1     =  1.39656  * Constants.ARC_SECONDS_TO_RADIANS;
    /** Quadratic term of the 1st coefficient for Z precession angle. */
    private static final double Z_1_2     = -0.000139 * Constants.ARC_SECONDS_TO_RADIANS;
    /** Constant term of the 2nd coefficient for Z precession angle. */
    private static final double Z_2_0     =  1.09468  * Constants.ARC_SECONDS_TO_RADIANS;
    /** Linear term of the 2nd coefficient for Z precession angle. */
    private static final double Z_2_1     =  0.000066 * Constants.ARC_SECONDS_TO_RADIANS;
    /** Constant term of the 3rd coefficient for Z precession angle. */
    private static final double Z_3_0     =  0.018203 * Constants.ARC_SECONDS_TO_RADIANS;

    /** Equinox epoch. */
    private final AbsoluteDate equinoxEpoch;

    /** 1st coefficient for ZETA precession angle. */
    private final double zeta1;
    /** 2nd coefficient for ZETA precession angle. */
    private final double zeta2;
    /** 3rd coefficient for ZETA precession angle. */
    private final double zeta3;

    /** 1st coefficient for THETA precession angle. */
    private final double theta1;
    /** 2nd coefficient for THETA precession angle. */
    private final double theta2;
    /** 3rd coefficient for THETA precession angle. */
    private final double theta3;

    /** 1st coefficient for Z precession angle. */
    private final double z1;
    /** 2nd coefficient for Z precession angle. */
    private final double z2;
    /** 3rd coefficient for Z precession angle. */
    private final double z3;

    /** Simple constructor.
     * @param equinoxEpoch reference epoch for equinox
     */
    public MODProvider(final AbsoluteDate equinoxEpoch) {

        this.equinoxEpoch = equinoxEpoch;

        // evaluate polynomials factors for the specified epoch
        final double bigT = equinoxEpoch.durationFrom(AbsoluteDate.J2000_EPOCH) / Constants.JULIAN_CENTURY;
        zeta1  = ZETA_1_0 + bigT * (ZETA_1_1 + bigT * ZETA_1_2);
        zeta2  = ZETA_2_0 + bigT * ZETA_2_1;
        zeta3  = ZETA_3_0;

        theta1 = THETA_1_0 + bigT * (THETA_1_1 + bigT * THETA_1_2);
        theta2 = THETA_2_0 + bigT * THETA_2_1;
        theta3 = THETA_3_0;

        z1     = Z_1_0 + bigT * (Z_1_1 + bigT * Z_1_2);
        z2     = Z_2_0 + bigT * Z_2_1;
        z3     = Z_3_0;

    }

    /** Get the transform from parent frame.
     * <p>The update considers the precession effects.</p>
     * @param date new value of the date
     * @return transform at the specified date
     */
    public Transform getTransform(final AbsoluteDate date) {

        // offset from equinox epoch in julian centuries
        final double tts = date.durationFrom(equinoxEpoch);
        final double ttc = tts / Constants.JULIAN_CENTURY;

        // compute the zeta precession angle
        final double zeta = ((zeta3 * ttc + zeta2) * ttc + zeta1) * ttc;

        // compute the theta precession angle
        final double theta = ((theta3 * ttc + theta2) * ttc + theta1) * ttc;

        // compute the z precession angle
        final double z = ((z3 * ttc + z2) * ttc + z1) * ttc;

        // elementary rotations for precession
        final Rotation r1 = new Rotation(Vector3D.PLUS_K,  z);
        final Rotation r2 = new Rotation(Vector3D.PLUS_J, -theta);
        final Rotation r3 = new Rotation(Vector3D.PLUS_K,  zeta);

        // complete precession
        final Rotation precession = r1.applyTo(r2.applyTo(r3));

        // set up the transform from parent GCRF
        return new Transform(date, precession);

    }

}
