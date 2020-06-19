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
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.Constants;

/** Converter for TLE propagator and parameters arrays.
 * @author Luc Maisonobe
 * @author Bryan Cazabonne
 * @since 10.2
 */
class TLEGradientConverter extends AbstractGradientConverter {

    /** Fixed dimension of the state. */
    private static final int FREE_STATE_PARAMETERS = 6;

    /** States with various number of additional parameters for force models. */
    private final List<FieldTLEPropagator<Gradient>> gPropagators;

    /** Simple constructor.
     * @param propagator TLE propagator to convert.
     */
    TLEGradientConverter(final TLEPropagator propagator) {

        super(FREE_STATE_PARAMETERS);

        // Compute semi-major axis from TLE with the 3rd Kepler's law.
        final double revolutionPeriod = Constants.JULIAN_DAY / propagator.getTLE().getMeanMotion();
        final double a = FastMath.pow(TLEPropagator.getMU() * revolutionPeriod / (4 * FastMath.PI * FastMath.PI), 1. / 3);

        // Keplerian parameters always has derivatives
        final Gradient ga           = Gradient.variable(FREE_STATE_PARAMETERS, 0, a);
        final Gradient ge           = Gradient.variable(FREE_STATE_PARAMETERS, 1, propagator.getTLE().getE());
        final Gradient gi           = Gradient.variable(FREE_STATE_PARAMETERS, 2, propagator.getTLE().getI());
        final Gradient graan        = Gradient.variable(FREE_STATE_PARAMETERS, 3, propagator.getTLE().getRaan());
        final Gradient gpa          = Gradient.variable(FREE_STATE_PARAMETERS, 4, propagator.getTLE().getPerigeeArgument());
        final Gradient gMeanAnomaly = Gradient.variable(FREE_STATE_PARAMETERS, 5, propagator.getTLE().getMeanAnomaly());

        final Gradient gMu = Gradient.constant(FREE_STATE_PARAMETERS, TLEPropagator.getMU());

         // date
        final AbsoluteDate date = propagator.getTLE().getDate();
        final FieldAbsoluteDate<Gradient> dateField = new FieldAbsoluteDate<>(ga.getField(), date);

        // mass never has derivatives
        final Gradient gM = Gradient.constant(FREE_STATE_PARAMETERS, propagator.getMass(date));


        //Retrieving original TLE constants
        final int satelliteNumber = propagator.getTLE().getSatelliteNumber();
        final char classification = propagator.getTLE().getClassification();
        final int launchYear = propagator.getTLE().getLaunchYear();
        final int launchNumber = propagator.getTLE().getLaunchNumber();
        final String launchPiece = propagator.getTLE().getLaunchPiece();
        final int ephemerisType = propagator.getTLE().getEphemerisType();
        final int elementNumber = propagator.getTLE().getElementNumber();
        final Gradient meanMotion = ga.getField().getZero().add(propagator.getTLE().getMeanMotion());
        final Gradient meanMotionFirstDerivative = ga.getField().getZero().add(propagator.getTLE().getMeanMotionFirstDerivative());
        final Gradient meanMotionSecondDerivative = ga.getField().getZero().add(propagator.getTLE().getMeanMotionSecondDerivative());
        final int revolutionNumberAtEpoch = propagator.getTLE().getRevolutionNumberAtEpoch();
        final Gradient bStar = ga.getField().getZero().add(propagator.getTLE().getBStar());


        final FieldTLE<Gradient> gtle = new FieldTLE<>(satelliteNumber, classification,
                        launchYear, launchNumber, launchPiece, ephemerisType, elementNumber, dateField,
                        meanMotion, meanMotionFirstDerivative, meanMotionSecondDerivative, ge, gi, gpa, graan, gMeanAnomaly,
                        revolutionNumberAtEpoch, bStar);

        final FieldTLEPropagator<Gradient> gPropagator = FieldTLEPropagator.selectExtrapolator(gtle);

        // initialize the list with the state having 0 force model parameters
        gPropagators = new ArrayList<>();
        gPropagators.add(gPropagator);

    }
    public FieldTLEPropagator<Gradient> getPropagator() {

        // no force model in analytical method
        final int nbParams = 0;

        // cf DSSTGradientConverter si ajout des paramètres mu et B*

        return gPropagators.get(nbParams);

    }

}
