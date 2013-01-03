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
package org.orekit.propagation.semianalytical.dsst.forces;

import java.util.Set;

import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.semianalytical.dsst.utilities.AuxiliaryElements;
import org.orekit.propagation.semianalytical.dsst.utilities.ResonantCouple;
import org.orekit.time.AbsoluteDate;

/** Central body gravitational contribution to the
 *  {@link org.orekit.propagation.semianalytical.dsst.DSSTPropagator DSSTPropagator}.
 *  <p>
 *  Central body gravitational contribution is made of:
 *  <ol>
 *  <li>zonal harmonics contribution</li>
 *  <li>tesseral harmonics contribution</li>
 *  </ol>
 *  </p>
 *
 *   @author Pascal Parraud
 */
public class DSSTCentralBody implements DSSTForceModel {

    /** Equatorial radius of the central body (m). */
    private final double equatorialRadius;

    /** Un-normalized coefficients array (cosine part). */
    private final double[][] C;

    /** Un-normalized coefficients array (sine part). */
    private final double[][] S;

    /** Degree <i>n</i> of potential. */
    private final int degree;

    /** Order <i>m</i> of potential. */
    private final int order;

    /** Zonal harmonics contribution. */
    private final ZonalContribution    zonal;

    /** Tesseral harmonics contribution. */
    private final TesseralContribution tesseral;

    /** DSST Central body constructor.
     * @param centralBodyRotationRate central body rotation rate (rad/s)
     * @param equatorialRadius equatorial radius of the central body (m)
     * @param mu central body attraction coefficient (m<sup>3</sup>/s<sup>2</sup>)
     * @param Cnm un-normalized coefficients array of the spherical harmonics (cosine part)
     * @param Snm un-normalized coefficients array of the spherical harmonics (sine part)
     */
    public DSSTCentralBody(final double centralBodyRotationRate,
                           final double equatorialRadius,
                           final double mu,
                           final double[][] Cnm,
                           final double[][] Snm) {

        this.degree = Cnm.length - 1;
        this.order  = Cnm[degree].length - 1;

        // Check potential coefficients consistency
        if ((Cnm.length != Snm.length) || (Cnm[degree].length != Snm[degree].length)) {
            throw OrekitException.createIllegalArgumentException(OrekitMessages.POTENTIAL_ARRAYS_SIZES_MISMATCH,
                                                                 Cnm.length, Cnm[degree].length, Snm.length, Snm[degree].length);
        }
        if (degree < order) {
            throw OrekitException.createIllegalArgumentException(OrekitMessages.TOO_LARGE_ORDER_FOR_GRAVITY_FIELD, order, degree);
        }

        // Equatorial radius of the central body
        this.equatorialRadius = equatorialRadius;

        // Potential coefficients
        this.C = Cnm.clone();
        this.S = Snm.clone();

        // Zonal harmonics contribution
        this.zonal = new ZonalContribution(this, mu);

        // Tesseral harmonics contribution (only if order > 0)
        this.tesseral = (order == 0) ? null : new TesseralContribution(this, centralBodyRotationRate, mu);

    }

    /** {@inheritDoc} */
    public void initialize(final AuxiliaryElements aux)
        throws OrekitException {

        // Initialize zonal contribution
        zonal.initialize(aux);

        // Initialize tesseral contribution if needed
        if (tesseral != null) {
            tesseral.initialize(aux);
        }
    }

    /** {@inheritDoc} */
    public void initializeStep(final AuxiliaryElements aux)
        throws OrekitException {

        // Initialize zonal contribution
        zonal.initializeStep(aux);

        // Initialize tesseral contribution if needed
        if (tesseral != null) {
            tesseral.initializeStep(aux);
        }
    }

    /** {@inheritDoc} */
    public double[] getMeanElementRate(final SpacecraftState spacecraftState)
        throws OrekitException {

        // Get zonal harmonics contribution to mean elements
        final double[] meanElementRate = zonal.getMeanElementRate(spacecraftState);

        // Get tesseral resonant harmonics contribution to mean elements
        if (tesseral != null) {
            final double[] tesseralMeanRate = tesseral.getMeanElementRate(spacecraftState);
            for (int i = 0; i < 6; i++) {
                meanElementRate[i] += tesseralMeanRate[i];
            }
        }

        return meanElementRate;
    }

    /** {@inheritDoc} */
    public double[] getShortPeriodicVariations(final AbsoluteDate date, final double[] meanElements)
        throws OrekitException {

        // Get zonal harmonics contribution to short periodic variations
        final double[] shortPeriodics = zonal.getShortPeriodicVariations(date, meanElements);

        // Get tesseral resonant harmonics contribution to short periodic variations
        if (tesseral != null) {
            final double[] tesseralShort = tesseral.getShortPeriodicVariations(date, meanElements);
            for (int i = 0; i < 6; i++) {
                shortPeriodics[i] += tesseralShort[i];
            }
        }

        return shortPeriodics;
    }

    /** Set the resonant harmonic couples.
     *  <p>
     *  If the set is null or empty, the resonant couples will be automatically computed.
     *  If it is not null nor empty, only these resonant couples will be taken in account.
     *  </p>
     *  @param resonantTesseral Set of resonant terms
     */
    public void setResonantTesseral(final Set<ResonantCouple> resonantTesseral) {
        if (tesseral != null) {
            tesseral.setResonantTesseralTerms(resonantTesseral);
        }
    }

    /** Get the equatorial radius of the central body.
     *  @return the equatorial radius (m)
     */
    public double getEquatorialRadius() {
        return equatorialRadius;
    }

    /** Get the un-normalized coefficients array of the spherical harmonics (cosine part).
     *  @return the Cnm array
     */
    public double[][] getCnm() {
        return C.clone();
    }

    /** Get the un-normalized coefficients array of the spherical harmonics (sine part).
     *  @return the Snm array
     */
    public double[][] getSnm() {
        return S.clone();
    }

    /** Get the maximum degree of the spherical harmonics.
     *  @return the maximum degree
     */
    public int getMaxDegree() {
        return degree;
    }

    /** Get the maximum order of the spherical harmonics.
     *  @return the maximum order
     */
    public int getMaxOrder() {
        return order;
    }

    /** Get one Cnm coefficient of the spherical harmonics.
     *  @param n the degree
     *  @param m the order
     *  @return the Cnm coefficient
     */
    public double getCnm(final int n, final int m) {
        return C[n][m];
    }

    /** Get one Jn coefficient (i.e. -C[n][0]) of the spherical harmonics.
     *  @param n the degree
     *  @return the Jn coefficient
     */
    public double getJn(final int n) {
        return -C[n][0];
    }

    /** Get one Snm coefficient of the spherical harmonics.
     *  @param n the degree
     *  @param m the order
     *  @return the Snm coefficient
     */
    public double getSnm(final int n, final int m) {
        return S[n][m];
    }

}
