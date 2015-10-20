/* Copyright 2002-2015 CS Systèmes d'Information
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

import java.util.Map;
import java.util.Set;

import org.orekit.attitudes.AttitudeProvider;
import org.orekit.errors.OrekitException;
import org.orekit.forces.gravity.potential.UnnormalizedSphericalHarmonicsProvider;
import org.orekit.frames.Frame;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.semianalytical.dsst.utilities.AuxiliaryElements;
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
     * @param centralBodyFrame rotating body frame
     * @param centralBodyRotationRate central body rotation rate (rad/s)
     * @param provider provider for spherical harmonics
     * @param mDailiesOnly if true only M-dailies tesseral harmonics are taken into account for short periodics
     */
    public DSSTCentralBody(final Frame centralBodyFrame,
                           final double centralBodyRotationRate,
                           final UnnormalizedSphericalHarmonicsProvider provider,
                           final boolean mDailiesOnly) {

        // Zonal harmonics contribution
        this.zonal = new ZonalContribution(provider);

        // Tesseral harmonics contribution (only if order > 0)
        this.tesseral = (provider.getMaxOrder() == 0) ?
                        null : new TesseralContribution(centralBodyFrame,
                                                        centralBodyRotationRate,
                                                        provider,
                                                        mDailiesOnly);

    }

    /** DSST Central body constructor.
     * <p>
     *  All harmonics are considered for the short-periodic tesseral contribution
     * </p>
     * @param centralBodyFrame rotating body frame
     * @param centralBodyRotationRate central body rotation rate (rad/s)
     * @param provider provider for spherical harmonics
     */
    public DSSTCentralBody(final Frame centralBodyFrame,
                           final double centralBodyRotationRate,
                           final UnnormalizedSphericalHarmonicsProvider provider) {
        this(centralBodyFrame, centralBodyRotationRate, provider, false);
    }

    /** {@inheritDoc} */
    public void initialize(final AuxiliaryElements aux, final boolean meanOnly)
        throws OrekitException {

        // Initialize zonal contribution
        zonal.initialize(aux, meanOnly);

        // Initialize tesseral contribution if needed
        if (tesseral != null) {
            tesseral.initialize(aux, meanOnly);
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

    /** {@inheritDoc} */
    @Override
    public String getCoefficientsKeyPrefix() {
        return "DSST-central-body-";
    }

    /** {@inheritDoc}
     * <p>
     * For central body attraction forces,there are zonal coefficients
     * optionally followed by tesseral coefficients
     * </p>
     * <p>
     * For zonal terms contributions,there are 6 * maxJ cZij coefficients,
     * 6 * maxJ sZij coefficients and 12 dZij coefficients, where maxJ depends
     * on the orbit. The i index ranges from 0 to 5 and corresponds to the
     * orbital parameter (0 for semi-major axis, ...).The j index is the
     * integer multiplier for the true longitude argument in the cZij and sZij
     * coefficients.
     * </p>
     * <p>
     * For tesseral terms contributions,there are 6 * maxOrderMdailyTesseralSP
     * m-daily cMim coefficients, 6 * maxOrderMdailyTesseralSP m-daily sMim
     * coefficients, 6 * nbNonResonant cijm coefficients and 6 * nbNonResonant
     * sijm coefficients, where maxOrderMdailyTesseralSP and nbNonResonant both
     * depend on the orbit. To these raw coefficients, we add the two integers
     * maxOrderMdailyTesseralSP and nbNonResonant as well as the 2 * nbNonResonant
     * indices j and m that correspond to non-resonant pairs. The i index ranges
     * from 0 to 5 and corresponds to the orbital parameter (0 for semi-major axis,
     * ...). The j index is the integer multiplier for the true longitude argument
     * and the m index is the integer multiplier for m-dailies.
     * </p>
     */
    @Override
    public Map<String, double[]> getShortPeriodicCoefficients(final AbsoluteDate date, final Set<String> selected)
        throws OrekitException {
        final Map<String, double[]> coefficients = zonal.getShortPeriodicCoefficients(date, selected);
        if (tesseral != null) {
            coefficients.putAll(tesseral.getShortPeriodicCoefficients(date, selected));
        }
        return coefficients;
    }

    /** {@inheritDoc} */
    public EventDetector[] getEventsDetectors() {
        return null;
    }

    /** {@inheritDoc} */
    public void computeShortPeriodicsCoefficients(final SpacecraftState state) throws OrekitException {
        //relay the call to the Zonal and Tesseral contributions
        zonal.computeShortPeriodicsCoefficients(state);
        if (tesseral != null) {
            tesseral.computeShortPeriodicsCoefficients(state);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void resetShortPeriodicsCoefficients() {
      //relay the call to the Zonal and Tesseral contributions
        zonal.resetShortPeriodicsCoefficients();
        if (tesseral != null) {
            tesseral.resetShortPeriodicsCoefficients();
        }
    }

    /** Get the spherical harmonics provider.
     *  @return the spherical harmonics provider
     */
    public UnnormalizedSphericalHarmonicsProvider getProvider() {
        return zonal.getProvider();
    }

    /** {@inheritDoc} */
    @Override
    public void registerAttitudeProvider(final AttitudeProvider provider) {
        //nothing is done since this contribution is not sensitive to attitude
    }
}
