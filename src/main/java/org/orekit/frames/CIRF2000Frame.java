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
package org.orekit.frames;

import java.io.InputStream;

import org.apache.commons.math.geometry.Rotation;
import org.apache.commons.math.geometry.Vector3D;
import org.orekit.data.BodiesElements;
import org.orekit.data.PoissonSeries;
import org.orekit.errors.OrekitException;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;

/** Celestial Intermediate Reference Frame 2000.
 * <p>This frame includes both precession and nutation effects according to
 * the new IAU-2000 model. The single model replaces the two separate models
 * used before: IAU-76 precession (Lieske) and IAU-80 theory of nutation (Wahr).
 * It <strong>must</strong> be used with the Earth Rotation Angle (REA) defined by
 * Capitaine's model and <strong>not</strong> IAU-82 sidereal time which is
 * consistent with the previous models only.</p>
 * <p>Its parent frame is the GCRF frame.<p>
 * <p>This implementation includes a caching/interpolation feature to
 * tremendously improve efficiency. The IAU-2000 model involves lots of terms
 * (1600 components for x, 1275 components for y and 66 components for s). Recomputing
 * all these components for each point is really slow. The shortest period for these
 * components is about 5.5 days (one fifth of the moon revolution period), hence the
 * pole motion is smooth at the day or week scale. This implies that these motions can
 * be computed accurately using a few reference points per day or week and interpolated
 * between these points. This implementation uses 12 points separated by 1/2 day
 * (43200 seconds) each, the resulting maximal interpolation error on the frame is about
 * 1.3&times;10<sup>-10</sup> arcseconds.</p>
 * @version $Revision$ $Date$
 */
class CIRF2000Frame extends Frame {

    /** Serializable UID. */
    private static final long serialVersionUID = 4112885226125872560L;

    /** Radians per arcsecond. */
    private static final double RADIANS_PER_ARC_SECOND = Math.PI / 648000;

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
    private final PoissonSeries xDevelopment;

    /** Pole position (Y). */
    private final PoissonSeries yDevelopment;

    /** Pole position (S + XY/2). */
    private final PoissonSeries sxy2Development;

    /** "Left-central" date of the interpolation array. */
    private double tCenter;

    /** Step size of the interpolation array. */
    private final double h;

    /** X coordinate of current pole. */
    private double xCurrent;

    /** Y coordinate of current pole. */
    private double yCurrent;

    /** S coordinate of current pole. */
    private double sCurrent;

    /** X coordinate of reference poles. */
    private final double[] xRef;

    /** Y coordinate of reference poles. */
    private final double[] yRef;

    /** S coordinate of reference poles. */
    private final double[] sRef;

    /** Neville interpolation array for X coordinate. */
    private final double[] xNeville;

    /** Neville interpolation array for Y coordinate. */
    private final double[] yNeville;

    /** Neville interpolation array for S coordinate. */
    private final double[] sNeville;

    /** Simple constructor.
     * @param date the date.
     * @param name name of the frame
     * @exception OrekitException if the nutation model data embedded in the
     * library cannot be read.
     * @see Frame
     */
    protected CIRF2000Frame(final AbsoluteDate date, final String name)
        throws OrekitException {

        super(FramesFactory.getGCRF(), null , name, true);

        // set up an interpolation model on 12 points with a 1/2 day step
        // this leads to an interpolation error of about 1.1e-10 arcseconds
        final int n = 12;
        tCenter  = Double.NaN;
        h        = 43200.0;
        xRef     = new double[n];
        yRef     = new double[n];
        sRef     = new double[n];
        xNeville = new double[n];
        yNeville = new double[n];
        sNeville = new double[n];

        // load the nutation model
        xDevelopment    = loadModel(X_MODEL);
        yDevelopment    = loadModel(Y_MODEL);
        sxy2Development = loadModel(S_XY2_MODEL);

        // everything is in place, we can now synchronize the frame
        updateFrame(date);

    }

    /** Load a series development model.
     * @param name file name of the series development
     * @return series development model
     * @exception OrekitException if table cannot be loaded
     */
    private static PoissonSeries loadModel(final String name)
        throws OrekitException {

        // get the table data
        final InputStream stream = CIRF2000Frame.class.getResourceAsStream(name);

        // nutation models are in micro arcseconds in the data files
        // we store and use them in radians
        return new PoissonSeries(stream, RADIANS_PER_ARC_SECOND * 1.0e-6, name);

    }

    /** Update the frame to the given date.
     * <p>The update considers the nutation and precession effects from IERS data.</p>
     * @param date new value of the date
     * @exception OrekitException if the nutation model data embedded in the
     * library cannot be read
     */
    protected void updateFrame(final AbsoluteDate date) throws OrekitException {

        //    offset from J2000.0 epoch
        final double t = date.durationFrom(AbsoluteDate.J2000_EPOCH);

        // evaluate pole motion in celestial frame
        setInterpolatedPoleCoordinates(t);

        // set up the bias, precession and nutation rotation
        final double x2Py2  = xCurrent * xCurrent + yCurrent * yCurrent;
        final double zP1    = 1 + Math.sqrt(1 - x2Py2);
        final double r      = Math.sqrt(x2Py2);
        final double sPe2   = 0.5 * (sCurrent + Math.atan2(yCurrent, xCurrent));
        final double cos    = Math.cos(sPe2);
        final double sin    = Math.sin(sPe2);
        final double xPr    = xCurrent + r;
        final double xPrCos = xPr * cos;
        final double xPrSin = xPr * sin;
        final double yCos   = yCurrent * cos;
        final double ySin   = yCurrent * sin;
        final Rotation bpn  = new Rotation(zP1 * (xPrCos + ySin), -r * (yCos + xPrSin),
                                           r * (xPrCos - ySin), zP1 * (yCos - xPrSin),
                                           true);

        // set up the transform from parent GCRF
        setTransform(new Transform(bpn, Vector3D.ZERO));

    }

    /** Set the interpolated pole coordinates.
     * @param t offset from J2000.0 epoch in seconds
     */
    protected void setInterpolatedPoleCoordinates(final double t) {

        final int n    = xRef.length;
        final int nM12 = (n - 1) / 2;
        if (Double.isNaN(tCenter) || (t < tCenter) || (t > tCenter + h)) {
            // recompute interpolation array
            setReferencePoints(t);
        }

        // interpolate pole coordinates using Neville's algorithm
        System.arraycopy(xRef, 0, xNeville, 0, n);
        System.arraycopy(yRef, 0, yNeville, 0, n);
        System.arraycopy(sRef, 0, sNeville, 0, n);
        final double theta = (t - tCenter) / h;
        for (int j = 1; j < n; ++j) {
            for (int i = n - 1; i >= j; --i) {
                final double c1 = (theta + nM12 - i + j) / j;
                final double c2 = (theta + nM12 - i) / j;
                xNeville[i] = c1 * xNeville[i] - c2 * xNeville[i - 1];
                yNeville[i] = c1 * yNeville[i] - c2 * yNeville[i - 1];
                sNeville[i] = c1 * sNeville[i] - c2 * sNeville[i - 1];
            }
        }

        xCurrent = xNeville[n - 1];
        yCurrent = yNeville[n - 1];
        sCurrent = sNeville[n - 1];

    }

    /** Set the reference points array.
     * @param t offset from J2000.0 epoch in seconds
     */
    private void setReferencePoints(final double t) {

        final int n    = xRef.length;
        final int nM12 = (n - 1) / 2;

        // evaluate new location of center interval
        final double newTCenter = h * Math.floor(t / h);

        // shift reusable reference points
        int iMin = 0;
        int iMax = n;
        final int shift = (int) Math.rint((newTCenter - tCenter) / h);
        if (!Double.isNaN(tCenter) && (Math.abs(shift) < n)) {
            if (shift >= 0) {
                System.arraycopy(xRef, shift, xRef, 0, n - shift);
                System.arraycopy(yRef, shift, yRef, 0, n - shift);
                System.arraycopy(sRef, shift, sRef, 0, n - shift);
                iMin = n - shift;
            } else {
                System.arraycopy(xRef, 0, xRef, -shift, n + shift);
                System.arraycopy(yRef, 0, yRef, -shift, n + shift);
                System.arraycopy(sRef, 0, sRef, -shift, n + shift);
                iMax = -shift;
            }
        }

        // compute new reference points
        tCenter = newTCenter;
        for (int i = iMin; i < iMax; ++i) {
            computePoleCoordinates(tCenter + (i - nM12) * h);
            xRef[i] = xCurrent;
            yRef[i] = yCurrent;
            sRef[i] = sCurrent;
        }

    }

    /** Compute pole coordinates from precession and nutation effects.
     * <p>This method applies the complete IAU-2000 model and hence is
     * extremely slow. It is called by the {@link
     * #getInterpolatedPoleCoordinates(double)} on a small number of reference
     * points only.</p>
     * @param t offset from J2000.0 epoch in seconds
     */
    protected void computePoleCoordinates(final double t) {

        // offset in julian centuries
        final double tc =  t / Constants.JULIAN_CENTURY;

        final BodiesElements elements =
            new BodiesElements((((F14 * tc + F13) * tc + F12) * tc + F11) * tc + F10, // mean anomaly of the Moon
                               (((F24 * tc + F23) * tc + F22) * tc + F21) * tc + F20, // mean anomaly of the Sun
                               (((F34 * tc + F33) * tc + F32) * tc + F31) * tc + F30, // L - &Omega; where L is the mean longitude of the Moon
                               (((F44 * tc + F43) * tc + F42) * tc + F41) * tc + F40, // mean elongation of the Moon from the Sun
                               (((F54 * tc + F53) * tc + F52) * tc + F51) * tc + F50, // mean longitude of the ascending node of the Moon
                               F61  * tc +  F60, // mean Mercury longitude
                               F71  * tc +  F70, // mean Venus longitude
                               F81  * tc +  F80, // mean Earth longitude
                               F91  * tc +  F90, // mean Mars longitude
                               F101 * tc + F100, // mean Jupiter longitude
                               F111 * tc + F110, // mean Saturn longitude
                               F121 * tc + F120, // mean Uranus longitude
                               F131 * tc + F130, // mean Neptune longitude
                               (F142 * tc + F141) * tc); // general accumulated precession in longitude

        // pole position
        xCurrent =    xDevelopment.value(tc, elements);
        yCurrent =    yDevelopment.value(tc, elements);
        sCurrent = sxy2Development.value(tc, elements) - xCurrent * yCurrent / 2;

    }

}
