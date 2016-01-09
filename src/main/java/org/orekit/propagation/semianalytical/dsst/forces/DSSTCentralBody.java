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

import org.orekit.attitudes.AttitudeProvider;
import org.orekit.errors.OrekitException;
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
