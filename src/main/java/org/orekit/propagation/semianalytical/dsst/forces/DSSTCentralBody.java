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
package org.orekit.propagation.semianalytical.dsst.forces;

import java.util.List;

import org.apache.commons.math3.util.FastMath;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitInternalError;
import org.orekit.forces.gravity.potential.UnnormalizedSphericalHarmonicsProvider;
import org.orekit.frames.Frame;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.semianalytical.dsst.utilities.AuxiliaryElements;

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
 *   @deprecated as of 7.2, replaced by {@link DSSTZonal} and {@link DSSTTesseral}
 */
@Deprecated
public class DSSTCentralBody implements DSSTForceModel {

    /** Zonal harmonics contribution. */
    private final DSSTZonal    zonal;

    /** Tesseral harmonics contribution. */
    private final DSSTTesseral tesseral;

    /** DSST Central body constructor.
     * <p>
     * This constructor limits the zonal short periods to degree 12,
     * the tesseral short periods to degree and order 8,
     * and the tesseral m-dailies to degree and order 12.
     * </p>
     * @param centralBodyFrame rotating body frame
     * @param centralBodyRotationRate central body rotation rate (rad/s)
     * @param provider provider for spherical harmonics
     * @param mDailiesOnly if true only M-dailies tesseral harmonics are taken into account for short periodics
     * @deprecated since 7.1, replaced with {@link #DSSTCentralBody(Frame, double,
     * UnnormalizedSphericalHarmonicsProvider, int, int, int, int, int)}
     */
    @Deprecated
    public DSSTCentralBody(final Frame centralBodyFrame,
                           final double centralBodyRotationRate,
                           final UnnormalizedSphericalHarmonicsProvider provider,
                           final boolean mDailiesOnly) {
        this(centralBodyFrame, centralBodyRotationRate, provider,
             12, mDailiesOnly ? -1 : 8, mDailiesOnly ? -1 : 8, 12, 12);
    }

    /** DSST Central body constructor.
     * <p>
     *  All harmonics are considered for the short-periodic tesseral contribution
     * </p>
     * @param centralBodyFrame rotating body frame
     * @param centralBodyRotationRate central body rotation rate (rad/s)
     * @param provider provider for spherical harmonics
     * @deprecated since 7.1, replaced with {@link #DSSTCentralBody(Frame, double,
     * UnnormalizedSphericalHarmonicsProvider, int, int, int, int, int)}
     */
    @Deprecated
    public DSSTCentralBody(final Frame centralBodyFrame,
                           final double centralBodyRotationRate,
                           final UnnormalizedSphericalHarmonicsProvider provider) {
        this(centralBodyFrame, centralBodyRotationRate, provider, false);
    }

    /** DSST Central body constructor.
     * @param centralBodyFrame rotating body frame
     * @param centralBodyRotationRate central body rotation rate (rad/s)
     * @param provider provider for spherical harmonics
     * @param maxDegreeZonalSP maximal degree to consider for short periodics zonal harmonics potential
     *  (the real degree used may be smaller if the provider does not provide enough terms)
     * @param maxDegreeTesseralSP maximal degree to consider for short periodics tesseral harmonics potential
     *  (the real degree used may be smaller if the provider does not provide enough terms)
     * @param maxOrderTesseralSP maximal order to consider for short periodics tesseral harmonics potential
     *  (the real order used may be smaller if the provider does not provide enough terms)
     * @param maxDegreeMdailyTesseralSP maximal degree to consider for short periodics m-daily tesseral harmonics potential
     *  (the real degree used may be smaller if the provider does not provide enough terms)
     * @param maxOrderMdailyTesseralSP maximal order to consider for short periodics m-daily tesseral harmonics potential
     *  (the real order used may be smaller if the provider does not provide enough terms)
     * @since 7.1
     */
    public DSSTCentralBody(final Frame centralBodyFrame,
                           final double centralBodyRotationRate,
                           final UnnormalizedSphericalHarmonicsProvider provider,
                           final int maxDegreeZonalSP,
                           final int maxDegreeTesseralSP, final int maxOrderTesseralSP,
                           final int maxDegreeMdailyTesseralSP, final int maxOrderMdailyTesseralSP) {
        try {

            // Zonal harmonics contribution
            final int maxDegreeZonalShortPeriodics = FastMath.min(provider.getMaxDegree(), maxDegreeZonalSP);
            final int maxEccPowZonalShortPeriodics = FastMath.min(maxDegreeZonalShortPeriodics - 1, 4);
            this.zonal = new DSSTZonal(provider,
                                       maxDegreeZonalShortPeriodics,
                                       maxEccPowZonalShortPeriodics,
                                       2 * maxDegreeZonalShortPeriodics + 1);

            // Tesseral harmonics contribution (only if order > 0)
            final int maxDegreeTesseralShortPeriodics       = FastMath.min(provider.getMaxDegree(), maxDegreeTesseralSP);
            final int maxOrderTesseralShortPeriodics        = FastMath.min(provider.getMaxOrder(), maxOrderTesseralSP);
            final int maxEccPowTesseralShortPeriodics       = FastMath.min(maxDegreeTesseralShortPeriodics - 1, 4);
            final int maxFrequencyShortPeriodics            = FastMath.min(maxDegreeTesseralShortPeriodics +
                                                                           maxEccPowTesseralShortPeriodics,
                                                                           12);
            final int maxDegreeMdailyTesseralShortPeriodics = FastMath.min(provider.getMaxDegree(), maxDegreeMdailyTesseralSP);
            final int maxOrderMdailyTesseralShortPeriodics  = FastMath.min(provider.getMaxOrder(), maxOrderMdailyTesseralSP);
            final int maxEccPowMdailyTesseralShortPeriodics = FastMath.min(maxDegreeMdailyTesseralShortPeriodics - 2, 4);
            this.tesseral = (provider.getMaxOrder() == 0) ?
                            null : new DSSTTesseral(centralBodyFrame,
                                                    centralBodyRotationRate,
                                                    provider,
                                                    maxDegreeTesseralShortPeriodics, maxOrderTesseralShortPeriodics,
                                                    maxEccPowTesseralShortPeriodics, maxFrequencyShortPeriodics,
                                                    maxDegreeMdailyTesseralShortPeriodics,
                                                    maxOrderMdailyTesseralShortPeriodics,
                                                    maxEccPowMdailyTesseralShortPeriodics);
        } catch (OrekitException oe) {
            // this should never happen
            throw new OrekitInternalError(oe);
        }
    }

    /** {@inheritDoc} */
    public List<ShortPeriodTerms> initialize(final AuxiliaryElements aux, final boolean meanOnly)
        throws OrekitException {

        // Initialize zonal contribution
        final List<ShortPeriodTerms> list = zonal.initialize(aux, meanOnly);

        // Initialize tesseral contribution if needed
        if (tesseral != null) {
            list.addAll(tesseral.initialize(aux, meanOnly));
        }

        return list;

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
    public EventDetector[] getEventsDetectors() {
        return null;
    }

    /** {@inheritDoc} */
    public void updateShortPeriodTerms(final SpacecraftState ... state)
        throws OrekitException {
        //relay the call to the Zonal and Tesseral contributions
        zonal.updateShortPeriodTerms(state);
        if (tesseral != null) {
            tesseral.updateShortPeriodTerms(state);
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
