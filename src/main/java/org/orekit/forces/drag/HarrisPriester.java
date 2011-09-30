/* Copyright 2002-2011 CS Communication & Systèmes
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
package org.orekit.forces.drag;

import org.apache.commons.math.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math.util.FastMath;
import org.orekit.bodies.BodyShape;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.Frame;
import org.orekit.frames.Transform;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.PVCoordinatesProvider;


/** This atmosphere model is the realization of the Modified Harris-Priester model.
 * <p>
 * This model is a static one that takes into account the diurnal density bulge.
 * It doesn't need any space weather data but a density vs. altitude table, which
 * depends on solar activity.
 * </p>
 * <p>
 * The implementation relies on the book:<br>
 * <b>Satellite Orbits</b><br>
 * <i>Oliver Montenbruck, Eberhard Gill</i><br>
 * Springer 2005
 * </p>
 * @author Pascal Parraud
 */
public class HarrisPriester implements Atmosphere {

    /** Serializable UID.*/
    private static final long serialVersionUID = 2772347498196369601L;

    // Constants :

    /** Lag angle in longitude. */
    private static final double LAG = FastMath.toRadians(30.0);

    /** Harris-Priester min-max density (kg/m3) vs. altitude (m) table.
     *  These data are valid for a mean solar activity. */
    private static final double[][] ALT_RHO = {
        {  100000.0, 4.974e-07, 4.974e-07 },
        {  120000.0, 2.490e-08, 2.490e-08 },
        {  130000.0, 8.377e-09, 8.710e-09 },
        {  140000.0, 3.899e-09, 4.059e-09 },
        {  150000.0, 2.122e-09, 2.215e-09 },
        {  160000.0, 1.263e-09, 1.344e-09 },
        {  170000.0, 8.008e-10, 8.758e-10 },
        {  180000.0, 5.283e-10, 6.010e-10 },
        {  190000.0, 3.617e-10, 4.297e-10 },
        {  200000.0, 2.557e-10, 3.162e-10 },
        {  210000.0, 1.839e-10, 2.396e-10 },
        {  220000.0, 1.341e-10, 1.853e-10 },
        {  230000.0, 9.949e-11, 1.455e-10 },
        {  240000.0, 7.488e-11, 1.157e-10 },
        {  250000.0, 5.709e-11, 9.308e-11 },
        {  260000.0, 4.403e-11, 7.555e-11 },
        {  270000.0, 3.430e-11, 6.182e-11 },
        {  280000.0, 2.697e-11, 5.095e-11 },
        {  290000.0, 2.139e-11, 4.226e-11 },
        {  300000.0, 1.708e-11, 3.526e-11 },
        {  320000.0, 1.099e-11, 2.511e-11 },
        {  340000.0, 7.214e-12, 1.819e-11 },
        {  360000.0, 4.824e-12, 1.337e-11 },
        {  380000.0, 3.274e-12, 9.955e-12 },
        {  400000.0, 2.249e-12, 7.492e-12 },
        {  420000.0, 1.558e-12, 5.684e-12 },
        {  440000.0, 1.091e-12, 4.355e-12 },
        {  460000.0, 7.701e-13, 3.362e-12 },
        {  480000.0, 5.474e-13, 2.612e-12 },
        {  500000.0, 3.916e-13, 2.042e-12 },
        {  520000.0, 2.819e-13, 1.605e-12 },
        {  540000.0, 2.042e-13, 1.267e-12 },
        {  560000.0, 1.488e-13, 1.005e-12 },
        {  580000.0, 1.092e-13, 7.997e-13 },
        {  600000.0, 8.070e-14, 6.390e-13 },
        {  620000.0, 6.012e-14, 5.123e-13 },
        {  640000.0, 4.519e-14, 4.121e-13 },
        {  660000.0, 3.430e-14, 3.325e-13 },
        {  680000.0, 2.632e-14, 2.691e-13 },
        {  700000.0, 2.043e-14, 2.185e-13 },
        {  720000.0, 1.607e-14, 1.779e-13 },
        {  740000.0, 1.281e-14, 1.452e-13 },
        {  760000.0, 1.036e-14, 1.190e-13 },
        {  780000.0, 8.496e-15, 9.776e-14 },
        {  800000.0, 7.069e-15, 8.059e-14 },
        {  840000.0, 4.680e-15, 5.741e-14 },
        {  880000.0, 3.200e-15, 4.210e-14 },
        {  920000.0, 2.210e-15, 3.130e-14 },
        {  960000.0, 1.560e-15, 2.360e-14 },
        { 1000000.0, 1.150e-15, 1.810e-14 }
    };

    /** Sun position. */
    private PVCoordinatesProvider sun;

    /** Earth body shape. */
    private BodyShape earth;

    /** Earth fixed frame. */
    private Frame bodyFrame;

    /** Density table. */
    private double[][] tabAltRho;

    /** Cosine exponent from 2 to 6 according to inclination. */
    private double n;

    /** Simple constructor for Modified Harris-Priester atmosphere model.
     * @param sun the sun position
     * @param earth the earth body shape
     * @param earthFixed the earth fixed frame
     */
    public HarrisPriester(final PVCoordinatesProvider sun,
                          final BodyShape earth, final Frame earthFixed) {
        this.sun       = sun;
        this.earth     = earth;
        this.bodyFrame = earthFixed;
        this.tabAltRho = ALT_RHO.clone();
        this.n = 4;
    }

    /** Get the minimal altitude for the model.
     * <p>No computation is possible below this altitude.</p>
     *  @return the minimal altitude (m)
     */
    public double getMinAlt() {
        return tabAltRho[0][0];
    }

    /** Get the maximal altitude for the model.
     * <p>Above this altitude, density is assumed to be zero.</p>
     *  @return the maximal altitude (m)
     */
    public double getMaxAlt() {
        return tabAltRho[tabAltRho.length-1][0];
    }

    /** Set parameter N, the cosine exponent.
     *  <p>Recommanded values spread over the range
     *  2, for low inclination orbits, to 6, for polar orbits.</p>
     *  <p>If this method is not called, the default value is set to 4.</p>
     *  </p>
     *  @param n the cosine exponent
     */
    public void setN(final double n) {
        this.n = n;
    }

    /** Set a user define density table to deal with different solar activities.
     *  <p>The density table must be an array such as:
     *  <ul>
     *   <li>tabAltRho[][0] = altitude (m)</li>
     *   <li>tabAltRho[][1] = min density (kg/m<sup>3</sup>)</li>
     *   <li>tabAltRho[][2] = max density (kg/m<sup>3</sup>)</li>
     *  </ul>
     *  The altitude must be increasing without limitation in range.
     *  </p>
     *  <p>
     *  If this method is not called, the default embedded table is the one given
     *  in the referenced book from Montenbruck & Gill. It deals with mean solar
     *  activity and spreads over 100 to 1000 km.
     *  </p>
     *  @param tabAltRho density vs. altitude table
     */
    public void setTabDensity(final double[][] tabAltRho) {
        this.tabAltRho = tabAltRho.clone();
    }

    /** Get the local density.
     * @param sunRAsc Right Ascension of Sun (radians)
     * @param sunDecl Declination of Sun (radians)
     * @param satPos  position of s/c in earth frame(m)
     * @param satAlt  height of s/c (m)
     * @return the local density (kg/m<sup>3</sup>)
     * @exception OrekitException if altitude is below the model minimal altitude 
     */
    public double getDensity(final double sunRAsc, final double sunDecl, final Vector3D satPos, final double satAlt)
        throws OrekitException {

        // Check for height boundaries
        if (satAlt < getMinAlt()) {
            throw new OrekitException(OrekitMessages.ALTITUDE_BELOW_ALLOWED_THRESHOLD, satAlt, getMinAlt());
        }
        if (satAlt > getMaxAlt()) {
            return 0.;
        }

        // Diurnal bulge apex direction
        final double cosDec = FastMath.cos(sunDecl);
        final Vector3D dDBA = new Vector3D(cosDec * FastMath.cos(sunRAsc + LAG),
                                           cosDec * FastMath.sin(sunRAsc + LAG),
                                           FastMath.sin(sunDecl));

        // Cosine of half angle between the diurnal bulge apex and the satellite
        double cosPsi2 = (0.5 + 0.5 * dDBA.normalize().dotProduct(satPos.normalize()));

        // Search altitude index in density table
        int ia = 0;
        while (ia < tabAltRho.length-1 && satAlt > tabAltRho[ia][0]) {  
            ia++ ;
        }

        // Exponential density interpolation 
        double altMin = (tabAltRho[ia][0] - tabAltRho[ia + 1][0]) / FastMath.log(tabAltRho[ia + 1][1] / tabAltRho[ia][1]);
        double altMax = (tabAltRho[ia][0] - tabAltRho[ia + 1][0]) / FastMath.log(tabAltRho[ia + 1][2] / tabAltRho[ia][2]);

        double rhoMin = tabAltRho[ia][1] * FastMath.exp((tabAltRho[ia][0] - satAlt) / altMin);
        double rhoMax = tabAltRho[ia][2] * FastMath.exp((tabAltRho[ia][0] - satAlt) / altMax);

        return rhoMin + (rhoMax - rhoMin) * FastMath.pow(cosPsi2, n/2);
    }

    /** Get the local density.
     * @param date current date
     * @param position current position in frame
     * @param frame the frame in which is defined the position
     * @return local density (kg/m<sup>3</sup>)
     * @exception OrekitException if some frame conversion cannot be performed 
     *            or if altitude is below the model minimal altitude
     */
    public double getDensity(final AbsoluteDate date, final Vector3D position, final Frame frame)
        throws OrekitException {

        // compute sun geodetic position
        final GeodeticPoint sunInBody = earth.transform(sun.getPVCoordinates(date, frame).getPosition(), frame, date);
        final double sunRAAN = sunInBody.getLongitude();
        final double sunDecl = sunInBody.getLatitude();

        // compute s/c position in earth frame
        final GeodeticPoint satInBody = earth.transform(position, frame, date);
        final double satAlt = satInBody.getAltitude();
        final Vector3D posInBody = earth.transform(satInBody);

        return getDensity(sunRAAN, sunDecl, posInBody, satAlt);
    }

    /** Get the inertial velocity of atmosphere molecules.
     * <p>
     * Here the case is simplified : atmosphere is supposed to have a null velocity
     * in earth frame.
     * </p>
     * @param date current date
     * @param position current position in frame
     * @param frame the frame in which is defined the position
     * @return velocity (m/s) (defined in the same frame as the position)
     * @exception OrekitException if some frame conversion cannot be performed
     */
    public Vector3D getVelocity(final AbsoluteDate date, final Vector3D position, final Frame frame)
        throws OrekitException {
        final Transform bodyToFrame = bodyFrame.getTransformTo(frame, date);
        final Vector3D posInBody = bodyToFrame.getInverse().transformPosition(position);
        final PVCoordinates pvBody = new PVCoordinates(posInBody, new Vector3D(0, 0, 0));
        final PVCoordinates pvFrame = bodyToFrame.transformPVCoordinates(pvBody);
        return pvFrame.getVelocity();
    }

}
