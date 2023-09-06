/* Copyright 2002-2023 CS GROUP
 * Licensed to CS GROUP (CS) under one or more
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
package org.orekit.propagation.analytical.tle;

import java.util.List;

import org.hipparchus.analysis.differentiation.Gradient;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.frames.Frame;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.analytical.AbstractAnalyticalGradientConverter;
import org.orekit.time.TimeScale;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.ParameterDriversProvider;

/** Converter for TLE propagator.
 * @author Luc Maisonobe
 * @author Bryan Cazabonne
 * @author Thomas Paulet
 * @since 11.0
 */
class TLEGradientConverter extends AbstractAnalyticalGradientConverter implements ParameterDriversProvider {

    /** Fixed dimension of the state. */
    public static final int FREE_STATE_PARAMETERS = 6;

    /** Current TLE. */
    private final TLE tle;

    /** UTC time scale. */
    private final TimeScale utc;

    /** TEME frame. */
    private final Frame teme;

    /** Attitude provider. */
    private final AttitudeProvider provider;

    /** Simple constructor.
     * @param propagator TLE propagator used to access initial orbit
     */
    TLEGradientConverter(final TLEPropagator propagator) {
        super(propagator, TLEConstants.MU, FREE_STATE_PARAMETERS);
        // TLE and related parameters
        this.tle      = propagator.getTLE();
        this.teme     = propagator.getFrame();
        this.utc      = tle.getUtc();
        this.provider = propagator.getAttitudeProvider();
    }

    /** {@inheritDoc} */
    @Override
    public FieldTLEPropagator<Gradient> getPropagator(final FieldSpacecraftState<Gradient> state,
                                                      final Gradient[] parameters) {

        // Zero
        final Gradient zero = state.getA().getField().getZero();

        // Template TLE
        final int satelliteNumber         = tle.getSatelliteNumber();
        final char classification         = tle.getClassification();
        final int launchYear              = tle.getLaunchYear();
        final int launchNumber            = tle.getLaunchNumber();
        final String launchPiece          = tle.getLaunchPiece();
        final int ephemerisType           = tle.getEphemerisType();
        final int elementNumber           = tle.getElementNumber();
        final int revolutionNumberAtEpoch = tle.getRevolutionNumberAtEpoch();
        final double bStar                = tle.getBStar(state.getDate().toAbsoluteDate());

        // Initialize the new TLE
        final FieldTLE<Gradient> templateTLE = new FieldTLE<>(satelliteNumber, classification,
                        launchYear, launchNumber, launchPiece, ephemerisType, elementNumber, state.getDate(),
                        zero, zero, zero, zero, zero, zero, zero, zero,
                        revolutionNumberAtEpoch, bStar, utc);

        // TLE
        final FieldTLE<Gradient> gTLE = TLEPropagator.getDefaultTleGenerationAlgorithm(utc, teme).generate(state, templateTLE);

        // Return the "Field" propagator
        return FieldTLEPropagator.selectExtrapolator(gTLE, provider, state.getMass(), teme, parameters);

    }

    /** {@inheritDoc} */
    @Override
    public List<ParameterDriver> getParametersDrivers() {
        return tle.getParametersDrivers();
    }

}
