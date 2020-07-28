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
import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.QRDecomposition;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.linear.RealVector;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathArrays;
import org.hipparchus.util.MathUtils;
import org.orekit.attitudes.FieldAttitude;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.orbits.FieldKeplerianOrbit;
import org.orekit.orbits.FieldOrbit;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
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
public class TLEGradientConverter extends AbstractGradientConverter {

    /** Fixed dimension of the state. */
    private static final int FREE_STATE_PARAMETERS = 6;

    /** Initial TLE. */
    private final TLE tle;

    /** States with various number of additional parameters for force models. */
    private final List<FieldTLEPropagator<Gradient>> gPropagators;

    /** Simple constructor.
     * @param tle initial TLE
     */
    public TLEGradientConverter(final TLE tle) {

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

    /**
     * Convert Spacecraft State into TLE.
     * This converter uses Newton method to reverse SGP4 and SDP4 propagation algorithm
     * and generates a usable TLE estimation of a state.
     * @param state Spacecraft State to convert into TLE
     * @param templateTLE first guess used to get identification and estimate new TLE
     * @return TLE matching with Spacecraft State and template identification
     */
    public static TLE stateToTLE(final SpacecraftState state, final TLE templateTLE) {

        // get keplerian parameters from state
        final Orbit orbit = state.getOrbit();
        final KeplerianOrbit keplerianOrbit = (KeplerianOrbit) OrbitType.KEPLERIAN.convertType(orbit);

        double meanMotion  = keplerianOrbit.getKeplerianMeanMotion();
        double e           = keplerianOrbit.getE();
        double i           = keplerianOrbit.getI();
        double raan        = keplerianOrbit.getRightAscensionOfAscendingNode();
        double pa          = keplerianOrbit.getPerigeeArgument();
        double meanAnomaly = keplerianOrbit.getMeanAnomaly();

        // rough initialization of the TLE
        FieldTLE<Gradient> current = invTLE(meanMotion, e, i, raan, pa, meanAnomaly, templateTLE);

        final Gradient zero = current.getE().getField().getZero();

        // threshold for each parameter
        final double epsilon             = 1.0e-15;
        final double thresholdMeanMotion = epsilon * (1 + keplerianOrbit.getKeplerianMeanMotion());
        final double thresholdE          = epsilon * (1 + state.getE());
        final double thresholdI          = epsilon * (1 + state.getI());
        final double thresholdAngles     = epsilon * FastMath.PI;
        int k = 0;
        while (k++ < 100) {

            // recompute the state from the current TLE
            final Gradient[] parameters = new Gradient[1];
            //parameters[0] = Gradient.constant(FREE_STATE_PARAMETERS, current.getBStar());
            parameters[0] = zero.add(current.getBStar());
            final FieldTLEPropagator<Gradient> propagator = FieldTLEPropagator.selectExtrapolator(current, parameters);
            final FieldSpacecraftState<Gradient> recoveredState = propagator.getInitialState();
            final FieldOrbit<Gradient> recoveredOrbit = recoveredState.getOrbit();
            final FieldKeplerianOrbit<Gradient> recoveredKeplerianOrbit = (FieldKeplerianOrbit<Gradient>) OrbitType.KEPLERIAN.convertType(recoveredOrbit);

            // adapted parameters residuals
            final Gradient deltaMeanMotion  = recoveredKeplerianOrbit.getKeplerianMeanMotion().negate().add(keplerianOrbit.getKeplerianMeanMotion());
            final Gradient deltaE           = recoveredKeplerianOrbit.getE().negate().add(keplerianOrbit.getE());
            final Gradient deltaI           = recoveredKeplerianOrbit.getI().negate().add(keplerianOrbit.getI());
            final Gradient deltaRAAN        = MathUtils.normalizeAngle(recoveredKeplerianOrbit.getRightAscensionOfAscendingNode().negate()
                                                                                     .add(keplerianOrbit.getRightAscensionOfAscendingNode()), zero);
            final Gradient deltaPA          = MathUtils.normalizeAngle(recoveredKeplerianOrbit.getPerigeeArgument().negate()
                                                                                     .add(keplerianOrbit.getPerigeeArgument()), zero);
            final Gradient deltaMeanAnomaly = MathUtils.normalizeAngle(recoveredKeplerianOrbit.getMeanAnomaly().negate()
                                                                                     .add(keplerianOrbit.getMeanAnomaly()), zero);

            // check convergence
            if ((FastMath.abs(deltaMeanMotion.getValue()) < thresholdMeanMotion) &&
                (FastMath.abs(deltaE.getValue())          < thresholdE) &&
                (FastMath.abs(deltaI.getValue())          < thresholdI) &&
                (FastMath.abs(deltaPA.getValue())         < thresholdAngles) &&
                (FastMath.abs(deltaRAAN.getValue())       < thresholdAngles) &&
                (FastMath.abs(deltaMeanMotion.getValue()) < thresholdAngles)) {

                return current.toTLE();
            }

            // compute differencial correction according to Newton method
            final double[] vector = new double[6];
            vector[0] = -deltaMeanMotion.getReal();
            vector[1] = -deltaE.getReal();
            vector[2] = -deltaI.getReal();
            vector[3] = -deltaRAAN.getReal();
            vector[4] = -deltaPA.getReal();
            vector[5] = -deltaMeanAnomaly.getReal();
            final RealVector F = MatrixUtils.createRealVector(vector);
            final RealMatrix J = MatrixUtils.createRealMatrix(6, 6);
            J.setRow(0, deltaMeanMotion.getGradient());
            J.setRow(1, deltaE.getGradient());
            J.setRow(2, deltaI.getGradient());
            J.setRow(3, deltaRAAN.getGradient());
            J.setRow(4, deltaPA.getGradient());
            J.setRow(5, deltaMeanAnomaly.getGradient());
            final QRDecomposition decomp = new QRDecomposition(J);

            final RealVector deltaTLE = decomp.getSolver().solve(F);

            // update TLE
            meanMotion  += deltaTLE.getEntry(0);
            e           += deltaTLE.getEntry(1);
            i           += deltaTLE.getEntry(2);
            raan        += deltaTLE.getEntry(3);
            pa          += deltaTLE.getEntry(4);
            meanAnomaly += deltaTLE.getEntry(5);

            current = invTLE(meanMotion, e, i, raan, pa, meanAnomaly, templateTLE);

        }

        throw new OrekitException(OrekitMessages.UNABLE_TO_COMPUTE_TLE, k);
    }

    /**
     * Modifies TLE orbital parameters.
     * @param meanMotion Mean Motion (rad/s)
     * @param e excentricity
     * @param i inclination (rad)
     * @param raan right ascension of ascending node (rad)
     * @param pa perigee argument (rad)
     * @param meanAnomaly mean anomaly (rad)
     * @param templateTLE TLE used to get object identification
     * @return TLE with template identification and new orbital parameters
     */
    private static FieldTLE<Gradient> invTLE(final double meanMotion,
                                             final double e,
                                             final double i,
                                             final double raan,
                                             final double pa,
                                             final double meanAnomaly,
                                             final TLE templateTLE) {

        // Identification
        final int satelliteNumber = templateTLE.getSatelliteNumber();
        final char classification = templateTLE.getClassification();
        final int launchYear = templateTLE.getLaunchYear();
        final int launchNumber = templateTLE.getLaunchNumber();
        final String launchPiece = templateTLE.getLaunchPiece();
        final int ephemerisType = templateTLE.getEphemerisType();
        final int elementNumber = templateTLE.getElementNumber();
        final int revolutionNumberAtEpoch = templateTLE.getRevolutionNumberAtEpoch();

        final Gradient gMeanMotion  = Gradient.variable(FREE_STATE_PARAMETERS, 0, meanMotion);
        final Gradient ge           = Gradient.variable(FREE_STATE_PARAMETERS, 1, e);
        final Gradient gi           = Gradient.variable(FREE_STATE_PARAMETERS, 2, i);
        final Gradient graan        = Gradient.variable(FREE_STATE_PARAMETERS, 3, raan);
        final Gradient gpa          = Gradient.variable(FREE_STATE_PARAMETERS, 4, pa);
        final Gradient gMeanAnomaly = Gradient.variable(FREE_STATE_PARAMETERS, 5, meanAnomaly);
        // Epoch
        final FieldAbsoluteDate<Gradient> epoch = new FieldAbsoluteDate<>(gMeanMotion.getField(), templateTLE.getDate());

        //B*
        final double bStar = templateTLE.getBStar();

        // Mean Motion derivatives
        final Gradient gMeanMotionFirstDerivative = Gradient.constant(FREE_STATE_PARAMETERS, templateTLE.getMeanMotionFirstDerivative());
        final Gradient gMeanMotionSecondDerivative = Gradient.constant(FREE_STATE_PARAMETERS, templateTLE.getMeanMotionSecondDerivative());

        final FieldTLE<Gradient> newTLE = new FieldTLE<Gradient>(satelliteNumber, classification, launchYear, launchNumber, launchPiece, ephemerisType,
                       elementNumber, epoch, gMeanMotion, gMeanMotionFirstDerivative, gMeanMotionSecondDerivative,
                       ge, gi, gpa, graan, gMeanAnomaly, revolutionNumberAtEpoch, bStar, templateTLE.getUtc());

        for (int k = 0; k < newTLE.getParametersDrivers().length; ++k) {
            newTLE.getParametersDrivers()[k].setSelected(templateTLE.getParametersDrivers()[k].isSelected());
        }

        return newTLE;
    }
}
