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
package org.orekit.forces.drag.atmosphere;

import org.hipparchus.RealFieldElement;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.Precision;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
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

    /** Default cosine exponent value. */
    private static final int N_DEFAULT = 4;

    /** Minimal value for calculating poxer of cosine. */
    private static final double MIN_COS = 1.e-12;

    /** Lag angle for diurnal bulge. */
    private static final double LAG = FastMath.toRadians(30.0);
    /** Lag angle cosine. */
    private static final double COSLAG = FastMath.cos(LAG);
    /** Lag angle sine. */
    private static final double SINLAG = FastMath.sin(LAG);

    // CHECKSTYLE: stop NoWhitespaceAfter
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
    // CHECKSTYLE: resume NoWhitespaceAfter

    /** Cosine exponent from 2 to 6 according to inclination. */
    private double n;

    /** Sun position. */
    private PVCoordinatesProvider sun;

    /** Earth body shape. */
    private OneAxisEllipsoid earth;

    /** Density table. */
    private double[][] tabAltRho;

    /** Simple constructor for Modified Harris-Priester atmosphere model.
     *  <p>The cosine exponent value is set to 4 by default.</p>
     *  <p>The default embedded density table is the one given in the referenced
     *  book from Montenbruck &amp; Gill. It is given for mean solar activity and
     *  spreads over 100 to 1000 km.</p>
     * @param sun the sun position
     * @param earth the earth body shape
     */
    public HarrisPriester(final PVCoordinatesProvider sun,
                          final OneAxisEllipsoid earth) {
        this(sun, earth, ALT_RHO, N_DEFAULT);
    }

    /** Constructor for Modified Harris-Priester atmosphere model.
     *  <p>Recommanded values for the cosine exponent spread over the range
     *  2, for low inclination orbits, to 6, for polar orbits.</p>
     *  <p> The default embedded density table is the one given in the referenced
     *  book from Montenbruck &amp; Gill. It is given for mean solar activity and
     *  spreads over 100 to 1000 km. </p>
     *  @param sun the sun position
     * @param earth the earth body shape
     * @param n the cosine exponent
     */
    public HarrisPriester(final PVCoordinatesProvider sun,
                          final OneAxisEllipsoid earth,
                          final double n) {
        this(sun, earth, ALT_RHO, n);
    }

    /** Constructor for Modified Harris-Priester atmosphere model.
     *  <p>The provided density table must be an array such as:
     *  <ul>
     *   <li>tabAltRho[][0] = altitude (m)</li>
     *   <li>tabAltRho[][1] = min density (kg/m³)</li>
     *   <li>tabAltRho[][2] = max density (kg/m³)</li>
     *  </ul>
     *  <p> The altitude must be increasing without limitation in range. The
     *  internal density table is a copy of the provided one.
     *
     *  <p>The cosine exponent value is set to 4 by default.</p>
     * @param sun the sun position
     * @param earth the earth body shape
     * @param tabAltRho the density table
     */
    public HarrisPriester(final PVCoordinatesProvider sun,
                          final OneAxisEllipsoid earth,
                          final double[][] tabAltRho) {
        this(sun, earth, tabAltRho, N_DEFAULT);
    }

    /** Constructor for Modified Harris-Priester atmosphere model.
     *  <p>Recommanded values for the cosine exponent spread over the range
     *  2, for low inclination orbits, to 6, for polar orbits.</p>
     *  <p>The provided density table must be an array such as:
     *  <ul>
     *   <li>tabAltRho[][0] = altitude (m)</li>
     *   <li>tabAltRho[][1] = min density (kg/m³)</li>
     *   <li>tabAltRho[][2] = max density (kg/m³)</li>
     *  </ul>
     *  <p> The altitude must be increasing without limitation in range. The
     *  internal density table is a copy of the provided one.
     *
     *  @param sun the sun position
     * @param earth the earth body shape
     * @param tabAltRho the density table
     * @param n the cosine exponent
     */
    public HarrisPriester(final PVCoordinatesProvider sun,
                          final OneAxisEllipsoid earth,
                          final double[][] tabAltRho,
                          final double n) {
        this.sun   = sun;
        this.earth = earth;
        setTabDensity(tabAltRho);
        setN(n);
    }

    /** {@inheritDoc} */
    public Frame getFrame() {
        return earth.getBodyFrame();
    }

    /** Set parameter N, the cosine exponent.
     *  @param n the cosine exponent
     */
    private void setN(final double n) {
        this.n = n;
    }

    /** Set a user define density table to deal with different solar activities.
     *  @param tab density vs. altitude table
     */
    private void setTabDensity(final double[][] tab) {
        this.tabAltRho = new double[tab.length][];
        for (int i = 0; i < tab.length; i++) {
            this.tabAltRho[i] = tab[i].clone();
        }
    }

    /** Get the current density table.
     *  <p>The density table is an array such as:
     *  <ul>
     *   <li>tabAltRho[][0] = altitude (m)</li>
     *   <li>tabAltRho[][1] = min density (kg/m³)</li>
     *   <li>tabAltRho[][2] = max density (kg/m³)</li>
     *  </ul>
     *  <p> The altitude must be increasing without limitation in range.
     *
     *  <p>
     *  The returned density table is a copy of the current one.
     *  </p>
     *  @return density vs. altitude table
     */
    public double[][] getTabDensity() {
        final double[][] copy = new double[tabAltRho.length][];
        for (int i = 0; i < tabAltRho.length; i++) {
            copy[i] = tabAltRho[i].clone();
        }
        return copy;
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
        return tabAltRho[tabAltRho.length - 1][0];
    }

    /** Get the local density.
     * @param sunInEarth position of the Sun in Earth frame (m)
     * @param posInEarth target position in Earth frame (m)
     * @return the local density (kg/m³)
     * @exception OrekitException if altitude is below the model minimal altitude
     */
    public double getDensity(final Vector3D sunInEarth, final Vector3D posInEarth)
        throws OrekitException {

        final double posAlt = getHeight(posInEarth);
        // Check for height boundaries
        if (posAlt < getMinAlt()) {
            throw new OrekitException(OrekitMessages.ALTITUDE_BELOW_ALLOWED_THRESHOLD, posAlt, getMinAlt());
        }
        if (posAlt > getMaxAlt()) {
            return 0.;
        }

        // Diurnal bulge apex direction
        final Vector3D sunDir = sunInEarth.normalize();
        final Vector3D bulDir = new Vector3D(sunDir.getX() * COSLAG - sunDir.getY() * SINLAG,
                                             sunDir.getX() * SINLAG + sunDir.getY() * COSLAG,
                                             sunDir.getZ());

        // Cosine of angle Psi between the diurnal bulge apex and the satellite
        final double cosPsi = bulDir.normalize().dotProduct(posInEarth.normalize());
        // (1 + cos(Psi))/2 = cos²(Psi/2)
        final double c2Psi2 = (1. + cosPsi) / 2.;
        final double cPsi2  = FastMath.sqrt(c2Psi2);
        final double cosPow = (cPsi2 > MIN_COS) ? c2Psi2 * FastMath.pow(cPsi2, n - 2) : 0.;

        // Search altitude index in density table
        int ia = 0;
        while (ia < tabAltRho.length - 2 && posAlt > tabAltRho[ia + 1][0]) {
            ia++;
        }

        // Fractional satellite height
        final double dH = (tabAltRho[ia][0] - posAlt) / (tabAltRho[ia][0] - tabAltRho[ia + 1][0]);

        // Min exponential density interpolation
        final double rhoMin = tabAltRho[ia][1] * FastMath.pow(tabAltRho[ia + 1][1] / tabAltRho[ia][1], dH);

        if (Precision.equals(cosPow, 0.)) {
            return rhoMin;
        } else {
            // Max exponential density interpolation
            final double rhoMax = tabAltRho[ia][2] * FastMath.pow(tabAltRho[ia + 1][2] / tabAltRho[ia][2], dH);
            return rhoMin + (rhoMax - rhoMin) * cosPow;
        }

    }

    /** Get the local density.
     * @param sunInEarth position of the Sun in Earth frame (m)
     * @param posInEarth target position in Earth frame (m)
     * @return the local density (kg/m³)
     * @param <T> instance of RealFieldElement<T>
     * @exception OrekitException if altitude is below the model minimal altitude
     */
    public <T extends RealFieldElement<T>> T getDensity(final Vector3D sunInEarth, final FieldVector3D<T> posInEarth)
        throws OrekitException {
        final T zero = posInEarth.getX().getField().getZero();
        final T posAlt = getHeight(posInEarth);
        // Check for height boundaries
        if (posAlt.getReal() < getMinAlt()) {
            throw new OrekitException(OrekitMessages.ALTITUDE_BELOW_ALLOWED_THRESHOLD, posAlt, getMinAlt());
        }
        if (posAlt.getReal() > getMaxAlt()) {
            return zero;
        }

        // Diurnal bulge apex direction
        final Vector3D sunDir = sunInEarth.normalize();
        final Vector3D bulDir = new Vector3D(sunDir.getX() * COSLAG - sunDir.getY() * SINLAG,
                                             sunDir.getX() * SINLAG + sunDir.getY() * COSLAG,
                                             sunDir.getZ());

        // Cosine of angle Psi between the diurnal bulge apex and the satellite
        final T cosPsi = posInEarth.normalize().dotProduct(bulDir.normalize());
        // (1 + cos(Psi))/2 = cos²(Psi/2)
        final T c2Psi2 = cosPsi.add(1.).divide(2);
        final T cPsi2  = c2Psi2.sqrt();
        final T cosPow = (cPsi2.getReal() > MIN_COS) ? c2Psi2.multiply(cPsi2.pow(n - 2)) : zero;

        // Search altitude index in density table
        int ia = 0;
        while (ia < tabAltRho.length - 2 && posAlt.getReal() > tabAltRho[ia + 1][0]) {
            ia++;
        }

        // Fractional satellite height
        final T dH = posAlt.negate().add(tabAltRho[ia][0]).divide(tabAltRho[ia][0] - tabAltRho[ia + 1][0]);

        // Min exponential density interpolation
        final T rhoMin = zero.add(tabAltRho[ia + 1][1] / tabAltRho[ia][1]).pow(dH).multiply(tabAltRho[ia][1]);

        if (Precision.equals(cosPow.getReal(), 0.)) {
            return zero.add(rhoMin);
        } else {
            // Max exponential density interpolation
            final T rhoMax = zero.add(tabAltRho[ia + 1][2] / tabAltRho[ia][2]).pow(dH).multiply(tabAltRho[ia][2]);
            return rhoMin.add(rhoMax.subtract(rhoMin).multiply(cosPow));
        }

    }

    /** Get the local density at some position.
     * @param date current date
     * @param position current position
     * @param frame the frame in which is defined the position
     * @return local density (kg/m³)
     * @exception OrekitException if some frame conversion cannot be performed
     *            or if altitude is below the model minimal altitude
     */
    public double getDensity(final AbsoluteDate date, final Vector3D position, final Frame frame)
        throws OrekitException {

        // Sun position in earth frame
        final Vector3D sunInEarth = sun.getPVCoordinates(date, earth.getBodyFrame()).getPosition();

        // Target position in earth frame
        final Vector3D posInEarth = frame.getTransformTo(earth.getBodyFrame(), date).transformPosition(position);

        return getDensity(sunInEarth, posInEarth);
    }

    /** Get the local density at some position.
     * @param date current date
     * @param position current position
     * @param <T> implements a RealFieldElement
     * @param frame the frame in which is defined the position
     * @return local density (kg/m³)
     * @exception OrekitException if some frame conversion cannot be performed
     *            or if altitude is below the model minimal altitude
     */
    public <T extends RealFieldElement<T>> T getDensity(final FieldAbsoluteDate<T> date,
                                                        final FieldVector3D<T> position,
                                                        final Frame frame)
            throws OrekitException {
        // Sun position in earth frame
        final Vector3D sunInEarth = sun.getPVCoordinates(date.toAbsoluteDate(), earth.getBodyFrame()).getPosition();

        // Target position in earth frame
        final FieldVector3D<T> posInEarth = frame.getTransformTo(earth.getBodyFrame(), date.toAbsoluteDate()).transformPosition(position);

        return getDensity(sunInEarth, posInEarth);
    }

    /** Get the height above the Earth for the given position.
     *  <p>
     *  The height computation is an approximation valid for the considered atmosphere.
     *  </p>
     *  @param position current position in Earth frame
     *  @return height (m)
     */
    private double getHeight(final Vector3D position) {
        final double a    = earth.getEquatorialRadius();
        final double f    = earth.getFlattening();
        final double e2   = f * (2. - f);
        final double r    = position.getNorm();
        final double sl   = position.getZ() / r;
        final double cl2  = 1. - sl * sl;
        final double coef = FastMath.sqrt((1. - e2) / (1. - e2 * cl2));

        return r - a * coef;
    }

    /** Get the height above the Earth for the given position.
     *  <p>
     *  The height computation is an approximation valid for the considered atmosphere.
     *  </p>
     *  @param position current position in Earth frame
     *  @param <T> instance of RealFieldElement<T>
     *  @return height (m)
     */
    private <T extends RealFieldElement<T>> T getHeight(final FieldVector3D<T> position) {
        final double a    = earth.getEquatorialRadius();
        final double f    = earth.getFlattening();
        final double e2   = f * (2. - f);
        final T r    = position.getNorm();
        final T sl   = position.getZ().divide(r);
        final T cl2  = sl.multiply(sl).negate().add(1.);
        final T coef = cl2.multiply(-e2).add(1.).reciprocal().multiply(1. - e2).sqrt();

        return r.subtract(coef.multiply(a));
    }

}
