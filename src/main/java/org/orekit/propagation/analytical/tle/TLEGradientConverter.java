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
import org.hipparchus.util.MathArrays;
import org.orekit.attitudes.FieldAttitude;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.integration.AbstractGradientConverter;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.FieldAngularCoordinates;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.TimeStampedFieldAngularCoordinates;

/** Converter for TLE propagator.
 * @author Luc Maisonobe
 * @author Bryan Cazabonne
 * @author Thomas Paulet
 * @since 11.0
 */
class TLEGradientConverter extends AbstractGradientConverter {

    /** Fixed dimension of the state. */
    private static final int FREE_STATE_PARAMETERS = 6;

    /** Initial TLE. */
    private final TLE tle;

    /** States with various number of additional parameters for force models. */
    private final List<FieldTLEPropagator<Gradient>> gPropagators;

    /** Simple constructor.
     * @param tle initial TLE
     */
    TLEGradientConverter(final TLE tle) {

        super(FREE_STATE_PARAMETERS);
        this.tle = tle;
        final FieldTLE<Gradient> gTLE = getGradientTLE();
        final Gradient[] parameters;
        parameters = MathArrays.buildArray(gTLE.getE().getField(), 1);
        parameters[0].add(gTLE.getBStar());
        gPropagators = new ArrayList<>();
        gPropagators.add(FieldTLEPropagator.selectExtrapolator(gTLE, parameters));
    }

    /** Convert the initial TLE into a Gradient TLE.
     * @return the gradient version of the initial TLE
     */
    public FieldTLE<Gradient> getGradientTLE() {

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
        final double bStar = tle.getBStar();

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

        // fill in intermediate slots
        while (gPropagators.size() < nbParams + 1) {
            gPropagators.add(null);
        }

        if (gPropagators.get(nbParams) == null) {
            // it is the first time we need this number of parameters
            // we need to create the state
            final int freeParameters = FREE_STATE_PARAMETERS + nbParams;
            final FieldTLEPropagator<Gradient> p0 = gPropagators.get(0);

            // TLE
            final FieldTLE<Gradient> tle0 = p0.getTLE();
            final Gradient gMeanMotion  = extend(tle0.getMeanMotion(), freeParameters);
            final Gradient ge           = extend(tle0.getE(), freeParameters);
            final Gradient gi           = extend(tle0.getI(), freeParameters);
            final Gradient graan        = extend(tle0.getRaan(), freeParameters);
            final Gradient gpa          = extend(tle0.getPerigeeArgument(), freeParameters);
            final Gradient gMeanAnomaly = extend(tle0.getMeanAnomaly(), freeParameters);

            final FieldAbsoluteDate<Gradient> fieldDate = new FieldAbsoluteDate<>(gMeanMotion.getField(), tle.getDate());
            final int satelliteNumber = tle.getSatelliteNumber();
            final char classification = tle.getClassification();
            final int launchYear = tle.getLaunchYear();
            final int launchNumber = tle.getLaunchNumber();
            final String launchPiece = tle.getLaunchPiece();
            final int ephemerisType = tle.getEphemerisType();
            final int elementNumber = tle.getElementNumber();
            final Gradient meanMotionFirstDerivative = extend(tle0.getMeanMotionFirstDerivative(), freeParameters);
            final Gradient meanMotionSecondDerivative = extend(tle0.getMeanMotionSecondDerivative(), freeParameters);
            final int revolutionNumberAtEpoch = tle.getRevolutionNumberAtEpoch();
            final double bStar = tle.getBStar();

            final FieldTLE<Gradient> gTLE = new FieldTLE<>(satelliteNumber, classification,
                            launchYear, launchNumber, launchPiece, ephemerisType, elementNumber, fieldDate,
                            gMeanMotion, meanMotionFirstDerivative, meanMotionSecondDerivative, ge, gi, gpa, graan, gMeanAnomaly,
                            revolutionNumberAtEpoch, bStar);

            final FieldTLEPropagator<Gradient> p1 = FieldTLEPropagator.selectExtrapolator(gTLE, getParameters(gTLE));

            // attitude
            final FieldAngularCoordinates<Gradient> ac1 = p1.getInitialState().getAttitude().getOrientation();
            final FieldAttitude<Gradient> gAttitude =
                            new FieldAttitude<>(p1.getInitialState().getAttitude().getReferenceFrame(),
                                                new TimeStampedFieldAngularCoordinates<>(p1.getInitialState().getOrbit().getDate(),
                                                                                         extend(ac1.getRotation(), freeParameters),
                                                                                         extend(ac1.getRotationRate(), freeParameters),
                                                                                         extend(ac1.getRotationAcceleration(), freeParameters)));
            // mass
            final Gradient gM = extend(p1.getInitialState().getMass(), freeParameters);

            final FieldSpacecraftState<Gradient> s1 = new FieldSpacecraftState<>(p1.getInitialState().getOrbit(), gAttitude, gM);
            p1.resetInitialState(s1);
            gPropagators.set(nbParams, p1);

        }

        return gPropagators.get(nbParams);

    }

    /** Get the model parameters.
     * @param gTLE gradient TLE compliant with parameter drivers
     * @return force model parameters
     */
    public Gradient[] getParameters(final FieldTLE<Gradient> gTLE) {
        final int freeParameters = gTLE.getE().getFreeParameters();
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
