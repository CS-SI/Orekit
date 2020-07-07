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

import org.hipparchus.analysis.differentiation.Gradient;
import org.hipparchus.util.FastMath;
import org.orekit.propagation.integration.AbstractGradientConverter;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.ParameterDriver;

/** Converter for TLE propagator.
 * @author Luc Maisonobe
 * @author Bryan Cazabonne
 * @author Thomas Paulet
 * @since 11.0
 */
public class TLEGradientConverter extends AbstractGradientConverter {

    /** Fixed dimension of the state. */
    private static final int FREE_STATE_PARAMETERS = 6;

    /** Initial TLE. */
    private final TLE tle;

    /** Simple constructor.
     * @param tle initial TLE
     */
    public TLEGradientConverter(final TLE tle) {

        super(FREE_STATE_PARAMETERS);

        this.tle = tle;
    }

    /** Convert the initial TLE into a Gradient TLE.
     * @param nbParams number of model parameters
     * @return the gradient version of the initial TLE
     */
    public FieldTLE<Gradient> getGradientTLE(final int nbParams) {

        final int freeParameters = FREE_STATE_PARAMETERS + nbParams;
        final Gradient meanMotion   = Gradient.variable(freeParameters, 0, tle.getMeanMotion());
        final Gradient ge           = Gradient.variable(freeParameters, 1, tle.getE());
        final Gradient gi           = Gradient.variable(freeParameters, 2, tle.getI());
        final Gradient graan        = Gradient.variable(freeParameters, 3, tle.getRaan());
        final Gradient gpa          = Gradient.variable(freeParameters, 4, tle.getPerigeeArgument());
        final Gradient gMeanAnomaly = Gradient.variable(freeParameters, 5, tle.getMeanAnomaly());

        final FieldAbsoluteDate<Gradient> fieldDate = new FieldAbsoluteDate<>(ge.getField(), tle.getDate());
        final int satelliteNumber = tle.getSatelliteNumber();
        final char classification = tle.getClassification();
        final int launchYear = tle.getLaunchYear();
        final int launchNumber = tle.getLaunchNumber();
        final String launchPiece = tle.getLaunchPiece();
        final int ephemerisType = tle.getEphemerisType();
        final int elementNumber = tle.getElementNumber();
        final Gradient meanMotionFirstDerivative = Gradient.constant(freeParameters, tle.getMeanMotionFirstDerivative());
        final Gradient meanMotionSecondDerivative = Gradient.constant(freeParameters, tle.getMeanMotionSecondDerivative());
        final int revolutionNumberAtEpoch = tle.getRevolutionNumberAtEpoch();
        final Gradient bStar = Gradient.constant(freeParameters, tle.getBStar());
        final FieldTLE<Gradient> gtle = new FieldTLE<>(satelliteNumber, classification,
                        launchYear, launchNumber, launchPiece, ephemerisType, elementNumber, fieldDate,
                        meanMotion, meanMotionFirstDerivative, meanMotionSecondDerivative, ge, gi, gpa, graan, gMeanAnomaly,
                        revolutionNumberAtEpoch, bStar);

        return gtle;
    }

    /** Get the state with the number of parameters consistent with model.
     * @return state with the number of parameters consistent with force model
     */
    public FieldTLEPropagator<Gradient> getPropagator() {

        // count the required number of parameters
        int nbParams = 0;
        for (final ParameterDriver driver : tle.getParametersDrivers()) {
            if (driver.isSelected()) {
                ++nbParams;
            }
        }
        final FieldTLEPropagator<Gradient> p0 = FieldTLEPropagator.selectExtrapolator(getGradientTLE(nbParams));

        return p0;

    }

    /** Get the model parameters.
     * @param gPropagator Gradient propagator associated with parameters
     * @return force model parameters
     */
    public Gradient[] getParameters(final FieldTLEPropagator<Gradient> gPropagator) {
        final int freeParameters = gPropagator.getInitialState().getA().getFreeParameters();
        final ParameterDriver[] drivers = tle.getParametersDrivers();
        final Gradient[] parameters = new Gradient[drivers.length];
        int index = FREE_STATE_PARAMETERS;
        for (int i = 0; i < drivers.length; ++i) {
            parameters[i] = drivers[i].isSelected() ?
                            Gradient.variable(freeParameters, index++, drivers[i].getValue()) :
                            Gradient.constant(freeParameters, drivers[i].getValue());
        }
        return parameters;
    }

    public static Gradient computeA(final Gradient meanMotion) {
     // Compute semi-major axis from TLE with the 3rd Kepler's law.;
        final Gradient a = FastMath.pow(meanMotion.multiply(meanMotion).reciprocal().multiply(TLEPropagator.getMU()), 1. / 3);
        return a;
    }

}
