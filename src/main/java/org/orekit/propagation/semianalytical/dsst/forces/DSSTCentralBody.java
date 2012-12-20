/* Copyright 2002-2012 CS Systèmes d'Information
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

import java.util.List;

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

        final int degree = Cnm.length - 1;
        final int order  = Cnm[degree].length - 1;

        // Check potential coefficients consistency
        if ((Cnm.length != Snm.length) || (Cnm[degree].length != Snm[degree].length)) {
            throw OrekitException.createIllegalArgumentException(OrekitMessages.POTENTIAL_ARRAYS_SIZES_MISMATCH,
                                                                 Cnm.length, Cnm[degree].length, Snm.length, Snm[degree].length);
        }
        if (degree < order) {
            throw OrekitException.createIllegalArgumentException(OrekitMessages.TOO_LARGE_ORDER_FOR_GRAVITY_FIELD, order, degree);
        }

        // Zonal harmonics contribution
        this.zonal = new ZonalContribution(equatorialRadius, mu, Cnm, Snm);

        // Tesseral harmonics contribution (only if order > 0)
        this.tesseral = (order == 0) ? null : new TesseralContribution(centralBodyRotationRate, equatorialRadius, mu, Cnm, Snm);

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

    /** Set the resonant Tesseral harmonic couple term.
     * <p>
     *  This parameter can be set to null or be an empty list.
     *  If so, the program will automatically determine the resonant couple to
     *  take in account. If not, only the resonant couple given by the user will
     *  be taken in account.
     *  </p>
     *
     * @param resonantTesseral Resonant Tesseral harmonic couple term
     */
    public void setResonantTesseral(final List<ResonantCouple> resonantTesseral) {
        if (tesseral != null) {
            tesseral.setResonantTesseral(resonantTesseral);
        }
    }

    /** Set the minimum period for analytically averaged high-order resonant
     *  central body spherical harmonics in seconds.
     *  <p>
     *  Set to 10 days by default.
     *  </p>
     * @param resonantMinPeriodInSec minimum period in seconds
     */
    public void setResonantMinPeriodInSec(final double resonantMinPeriodInSec) {
        if (tesseral != null) {
            tesseral.setResonantMinPeriodInSec(resonantMinPeriodInSec);
        }
    }

    /** Set the minimum period for analytically averaged high-order resonant
     *  central body spherical harmonics in number of satellite revolutions.
     *  <p>
     *  Set to 10 days by default.
     *  </p>
     * @param resonantMinPeriodInSatRev minimum period in satellite revolutions
     */
    public void setResonantMinPeriodInSatRev(final double resonantMinPeriodInSatRev) {
        if (tesseral != null) {
            tesseral.setResonantMinPeriodInSatRev(resonantMinPeriodInSatRev);
        }
    }

    /** Set the highest power of the eccentricity to appear in the truncated analytical
     *  power series expansion for the averaged central-body tesseral harmonic potential.
     *
     * @param tesseralMaxEccPower highest power of the eccentricity
     */
    public void setTesseralMaximumEccentricityPower(final int tesseralMaxEccPower) {
        if (tesseral != null) {
            tesseral.setTesseralMaximumEccentricityPower(tesseralMaxEccPower);
        }
    }

    /** Get the equatorial radius of the central body.
     *  @return the equatorial radius (m)
     */
    public double getEquatorialRadius() {
        return zonal.getEquatorialRadius();
    }

    /** Get the un-normalized coefficients array of the spherical harmonics (cosine part).
     *  @return Cnm
     */
    public double[][] getCnm() {
        return zonal.getCnm();
    }

    /** Get the un-normalized coefficients array of the spherical harmonics (sine part).
     *  @return Snm
     */
    public double[][] getSnm() {
        return zonal.getSnm();
    }

}
