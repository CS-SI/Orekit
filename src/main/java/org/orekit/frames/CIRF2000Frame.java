/* Copyright 2002-2008 CS Communication & Systèmes
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
package org.orekit.frames;

import java.io.FileNotFoundException;
import java.io.PrintStream;

import org.apache.commons.math.geometry.Rotation;
import org.apache.commons.math.geometry.Vector3D;
import org.orekit.errors.OrekitException;
import org.orekit.frames.series.BodiesElements;
import org.orekit.frames.series.Development;
import org.orekit.time.AbsoluteDate;

/** Celestial Intermediate Reference Frame 2000.
 * <p>This frame includes both precession and nutation effects according to
 * the new IAU-2000 model. The single model replaces the two separate models
 * used before: IAU-76 precession (Lieske) and IAU-80 theory of nutation (Wahr).
 * It <strong>must</strong> be used with the Earth Rotation Angle (REA) defined by
 * Capitaine's model and <strong>not</strong> IAU-82 sidereal time which is
 * consistent with the previous models only.</p>
 * <p>Its parent frame is the GCRF frame.<p>
 * @version $Revision$ $Date$
 */
class CIRF2000Frame extends Frame {

    /** Serializable UID. */
    private static final long serialVersionUID = 7506358936983934856L;

    /** 2&pi;. */
    private static final double TWO_PI = 2.0 * Math.PI;

    /** Radians per arcsecond. */
    private static final double RADIANS_PER_ARC_SECOND = TWO_PI / 1296000;

    /** Julian century per second. */
    private static final double JULIAN_CENTURY_PER_SECOND = 1.0 / (36525.0 * 86400.0);

    // CHECKSTYLE: stop JavadocVariable check

    // lunisolar nutation elements
    private static final double F10 = Math.toRadians(134.96340251);
    private static final double F11 = 1717915923.217800  * RADIANS_PER_ARC_SECOND;
    private static final double F12 =         31.879200  * RADIANS_PER_ARC_SECOND;
    private static final double F13 =          0.051635  * RADIANS_PER_ARC_SECOND;
    private static final double F14 =         -0.0002447 * RADIANS_PER_ARC_SECOND;

    private static final double F20 = Math.toRadians(357.52910918);
    private static final double F21 = 129596581.048100   * RADIANS_PER_ARC_SECOND;
    private static final double F22 =        -0.553200   * RADIANS_PER_ARC_SECOND;
    private static final double F23 =         0.000136   * RADIANS_PER_ARC_SECOND;
    private static final double F24 =        -0.00001149 * RADIANS_PER_ARC_SECOND;

    private static final double F30 = Math.toRadians(93.27209062);
    private static final double F31 = 1739527262.847800   * RADIANS_PER_ARC_SECOND;
    private static final double F32 =        -12.751200   * RADIANS_PER_ARC_SECOND;
    private static final double F33 =         -0.001037   * RADIANS_PER_ARC_SECOND;
    private static final double F34 =          0.00000417 * RADIANS_PER_ARC_SECOND;

    private static final double F40 = Math.toRadians(297.85019547);
    private static final double F41 = 1602961601.209000   * RADIANS_PER_ARC_SECOND;
    private static final double F42 =         -6.370600   * RADIANS_PER_ARC_SECOND;
    private static final double F43 =          0.006593   * RADIANS_PER_ARC_SECOND;
    private static final double F44 =         -0.00003169 * RADIANS_PER_ARC_SECOND;

    private static final double F50 = Math.toRadians(125.04455501);
    private static final double F51 = -6962890.543100   * RADIANS_PER_ARC_SECOND;
    private static final double F52 =        7.472200   * RADIANS_PER_ARC_SECOND;
    private static final double F53 =        0.007702   * RADIANS_PER_ARC_SECOND;
    private static final double F54 =       -0.00005939 * RADIANS_PER_ARC_SECOND;

    // planetary nutation elements
    private static final double F60 = 4.402608842;
    private static final double F61 = 2608.7903141574;

    private static final double F70 = 3.176146697;
    private static final double F71 = 1021.3285546211;

    private static final double F80 = 1.753470314;
    private static final double F81 = 628.3075849991;

    private static final double F90 = 6.203480913;
    private static final double F91 = 334.0612426700;

    private static final double F100 = 0.599546497;
    private static final double F101 = 52.9690962641;

    private static final double F110 = 0.874016757;
    private static final double F111 = 21.3299104960;

    private static final double F120 = 5.481293872;
    private static final double F121 = 7.4781598567;

    private static final double F130 = 5.311886287;
    private static final double F131 = 3.8133035638;

    private static final double F141 = 0.024381750;
    private static final double F142 = 0.00000538691;

    // CHECKSTYLE: resume JavadocVariable check

    /** IERS conventions (2003) resources base directory. */
    private static final String IERS_2003_BASE = "/META-INF/IERS-conventions-2003/";

    /** Resources for IERS table 5.2a from IERS conventions (2003). */
    private static final String X_MODEL     = IERS_2003_BASE + "tab5.2a.txt";

    /** Resources for IERS table 5.2b from IERS conventions (2003). */
    private static final String Y_MODEL     = IERS_2003_BASE + "tab5.2b.txt";

    /** Resources for IERS table 5.2c from IERS conventions (2003). */
    private static final String S_XY2_MODEL = IERS_2003_BASE + "tab5.2c.txt";

    /** Pole position (X). */
    private final Development xDevelopment;

    /** Pole position (Y). */
    private final Development yDevelopment;

    /** Pole position (S + XY/2). */
    private final Development sxy2Development;

    /** Cached date to avoid useless computation. */
    private AbsoluteDate cachedDate;

    /** Simple constructor.
     * @param date the date.
     * @param name name of the frame
     * @exception OrekitException if the nutation model data embedded in the
     * library cannot be read.
     * @see Frame
     */
    protected CIRF2000Frame(final AbsoluteDate date, final String name)
        throws OrekitException {

        super(getGCRF(), null , name);

        // nutation models are in micro arcseconds
        final Class<CIRF2000Frame> c = CIRF2000Frame.class;
        xDevelopment =
            new Development(c.getResourceAsStream(X_MODEL), RADIANS_PER_ARC_SECOND * 1.0e-6, X_MODEL);
        yDevelopment =
            new Development(c.getResourceAsStream(Y_MODEL), RADIANS_PER_ARC_SECOND * 1.0e-6, Y_MODEL);
        sxy2Development =
            new Development(c.getResourceAsStream(S_XY2_MODEL), RADIANS_PER_ARC_SECOND * 1.0e-6, S_XY2_MODEL);

        // everything is in place, we can now synchronize the frame
        updateFrame(date);
    }

    /** Update the frame to the given date.
     * <p>The update considers the nutation and precession effects from IERS data.</p>
     * @param date new value of the date
     * @exception OrekitException if the nutation model data embedded in the
     * library cannot be read
     */
    protected void updateFrame(final AbsoluteDate date) throws OrekitException {

        if (cachedDate == null || cachedDate != date) {
            //    offset from J2000.0 epoch in julian centuries
            final double tts = date.durationFrom(AbsoluteDate.J2000_EPOCH);
            final double ttc =  tts * JULIAN_CENTURY_PER_SECOND;

            // precession and nutation effect (pole motion in celestial frame)
            final Rotation qRot = precessionNutationEffect(ttc);

            // combined effects
            final Rotation combined = qRot.revert();

            // set up the transform from parent GCRS to ITRF
            setTransform(new Transform(combined , Vector3D.ZERO));
            cachedDate = date;
        }
    }

    public static void main(String[] args) {
        try {
            CIRF2000Frame frame = new CIRF2000Frame(AbsoluteDate.J2000_EPOCH, "");
            final BodiesElements elements0 =
                new BodiesElements(F10, F20, F30, F40, F50, F60, F70, F80, F90, F100, F110, F120, F130, 0);
            // pole position
            final double x0 = frame.xDevelopment.value(0, elements0);
            final double y0 = frame.yDevelopment.value(0, elements0);
            final double s0 = frame.sxy2Development.value(0, elements0) - x0 * y0 / 2;
            Rotation r0 = frame.precessionNutationEffect(0);
            PrintStream p = new PrintStream("/tmp/x.dat");
            for (double tt = 0; tt < 1e7; tt += 900) {
                final double t = tt * JULIAN_CENTURY_PER_SECOND;
                final BodiesElements elements =
                    new BodiesElements((((F14 * t + F13) * t + F12) * t + F11) * t + F10, // mean anomaly of the Moon
                                       (((F24 * t + F23) * t + F22) * t + F21) * t + F20, // mean anomaly of the Sun
                                       (((F34 * t + F33) * t + F32) * t + F31) * t + F30, // L - &Omega; where L is the mean longitude of the Moon
                                       (((F44 * t + F43) * t + F42) * t + F41) * t + F40, // mean elongation of the Moon from the Sun
                                       (((F54 * t + F53) * t + F52) * t + F51) * t + F50, // mean longitude of the ascending node of the Moon
                                       F61  * t +  F60, // mean Mercury longitude
                                       F71  * t +  F70, // mean Venus longitude
                                       F81  * t +  F80, // mean Earth longitude
                                       F91  * t +  F90, // mean Mars longitude
                                       F101 * t + F100, // mean Jupiter longitude
                                       F111 * t + F110, // mean Saturn longitude
                                       F121 * t + F120, // mean Uranus longitude
                                       F131 * t + F130, // mean Neptune longitude
                                       (F142 * t + F141) * t); // general accumulated precession in longitude

                // pole position
                final double x =    frame.xDevelopment.value(t, elements);
                final double y =    frame.yDevelopment.value(t, elements);
                final double s = frame.sxy2Development.value(t, elements) - x * y / 2;

                final double x2 = x * x;
                final double y2 = y * y;
                final double r2 = x2 + y2;
                final double e = Math.atan2(y, x);
                final double d = Math.acos(Math.sqrt(1 - r2));
                final Rotation rpS = new Rotation(Vector3D.PLUS_K, -s);
                final Rotation rpE = new Rotation(Vector3D.PLUS_K, -e);
                final Rotation rmD = new Rotation(Vector3D.PLUS_J, +d);

                // combine the 4 rotations (rpE is used twice)
                // IERS conventions (2003), section 5.3, equation 6
                Rotation r = rpE.applyInverseTo(rmD.applyTo(rpE.applyTo(rpS)));
                p.println(tt + " " + (x - x0) + " " + (y - y0) + " " + (s - s0)
                          + " " + Rotation.distance(r, r0));
            }
        } catch (FileNotFoundException fnfe) {
            fnfe.printStackTrace(System.err);
        } catch (OrekitException oe) {
            oe.printStackTrace(System.err);
        }
    }

    /** Compute precession and nutation effects.
     * @param t offset from J2000.0 epoch in julian centuries
     * @return precession and nutation rotation
     */
    public Rotation precessionNutationEffect(final double t) {

        final BodiesElements elements =
            new BodiesElements((((F14 * t + F13) * t + F12) * t + F11) * t + F10, // mean anomaly of the Moon
                               (((F24 * t + F23) * t + F22) * t + F21) * t + F20, // mean anomaly of the Sun
                               (((F34 * t + F33) * t + F32) * t + F31) * t + F30, // L - &Omega; where L is the mean longitude of the Moon
                               (((F44 * t + F43) * t + F42) * t + F41) * t + F40, // mean elongation of the Moon from the Sun
                               (((F54 * t + F53) * t + F52) * t + F51) * t + F50, // mean longitude of the ascending node of the Moon
                               F61  * t +  F60, // mean Mercury longitude
                               F71  * t +  F70, // mean Venus longitude
                               F81  * t +  F80, // mean Earth longitude
                               F91  * t +  F90, // mean Mars longitude
                               F101 * t + F100, // mean Jupiter longitude
                               F111 * t + F110, // mean Saturn longitude
                               F121 * t + F120, // mean Uranus longitude
                               F131 * t + F130, // mean Neptune longitude
                               (F142 * t + F141) * t); // general accumulated precession in longitude

        // pole position
        final double x =    xDevelopment.value(t, elements);
        final double y =    yDevelopment.value(t, elements);
        final double s = sxy2Development.value(t, elements) - x * y / 2;

        final double x2 = x * x;
        final double y2 = y * y;
        final double r2 = x2 + y2;
        final double e = Math.atan2(y, x);
        final double d = Math.acos(Math.sqrt(1 - r2));
        final Rotation rpS = new Rotation(Vector3D.PLUS_K, -s);
        final Rotation rpE = new Rotation(Vector3D.PLUS_K, -e);
        final Rotation rmD = new Rotation(Vector3D.PLUS_J, +d);

        // combine the 4 rotations (rpE is used twice)
        // IERS conventions (2003), section 5.3, equation 6
        return rpE.applyInverseTo(rmD.applyTo(rpE.applyTo(rpS)));

    }

}
