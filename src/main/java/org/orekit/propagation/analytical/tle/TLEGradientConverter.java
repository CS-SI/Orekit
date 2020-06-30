/* Copyright 2002-2020 CS GROUP
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

import java.util.ArrayList;
import java.util.List;

import org.hipparchus.analysis.differentiation.Gradient;
import org.hipparchus.util.FastMath;
import org.orekit.propagation.integration.AbstractGradientConverter;
import org.orekit.time.FieldAbsoluteDate;

/** Converter for TLE propagator.
 * @author Luc Maisonobe
 * @author Bryan Cazabonne
 * @author Thomas Paulet
 * @since 11.0
 */
public class TLEGradientConverter extends AbstractGradientConverter {

    /** Fixed dimension of the state. */
    private static final int FREE_STATE_PARAMETERS = 7;

    /** States with various number of additional parameters for force models. */
    private final List<FieldTLEPropagator<Gradient>> gPropagators;

    /** Simple constructor.
     */
    public TLEGradientConverter() {

        super(FREE_STATE_PARAMETERS);

        // initialize the list with the state having 0 force model parameters
        gPropagators = new ArrayList<>();
    }

    public FieldTLE<Gradient> getGradientTLE(final TLE tle) {

        final Gradient meanMotion   = Gradient.variable(FREE_STATE_PARAMETERS, 0, tle.getMeanMotion());
        final Gradient ge           = Gradient.variable(FREE_STATE_PARAMETERS, 1, tle.getE());
        final Gradient gi           = Gradient.variable(FREE_STATE_PARAMETERS, 2, tle.getI());
        final Gradient graan        = Gradient.variable(FREE_STATE_PARAMETERS, 3, tle.getRaan());
        final Gradient gpa          = Gradient.variable(FREE_STATE_PARAMETERS, 4, tle.getPerigeeArgument());
        final Gradient gMeanAnomaly = Gradient.variable(FREE_STATE_PARAMETERS, 5, tle.getMeanAnomaly());

        final FieldAbsoluteDate<Gradient> fieldDate = new FieldAbsoluteDate<>(ge.getField(), tle.getDate());
        final int satelliteNumber = tle.getSatelliteNumber();
        final char classification = tle.getClassification();
        final int launchYear = tle.getLaunchYear();
        final int launchNumber = tle.getLaunchNumber();
        final String launchPiece = tle.getLaunchPiece();
        final int ephemerisType = tle.getEphemerisType();
        final int elementNumber = tle.getElementNumber();
        final Gradient meanMotionFirstDerivative = Gradient.constant(FREE_STATE_PARAMETERS, tle.getMeanMotionFirstDerivative());
        final Gradient meanMotionSecondDerivative = Gradient.constant(FREE_STATE_PARAMETERS, tle.getMeanMotionSecondDerivative());
        final int revolutionNumberAtEpoch = tle.getRevolutionNumberAtEpoch();
        final Gradient bStar = Gradient.constant(FREE_STATE_PARAMETERS, tle.getBStar());
        final FieldTLE<Gradient> gtle = new FieldTLE<>(satelliteNumber, classification,
                        launchYear, launchNumber, launchPiece, ephemerisType, elementNumber, fieldDate,
                        meanMotion, meanMotionFirstDerivative, meanMotionSecondDerivative, ge, gi, gpa, graan, gMeanAnomaly,
                        revolutionNumberAtEpoch, bStar);

        return gtle;
    }

    public FieldTLEPropagator<Gradient> getPropagator() {

        return gPropagators.get(0);

    }

    public static Gradient computeA(final Gradient meanMotion) {
     // Compute semi-major axis from TLE with the 3rd Kepler's law.;
        final Gradient a = FastMath.pow(meanMotion.multiply(meanMotion).reciprocal().multiply(TLEPropagator.getMU()), 1. / 3);
        return a;
    }

}
