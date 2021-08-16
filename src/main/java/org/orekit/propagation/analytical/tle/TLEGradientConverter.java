/* Copyright 2002-2021 CS GROUP
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
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.attitudes.FieldAttitude;
import org.orekit.frames.Frame;
import org.orekit.orbits.FieldCartesianOrbit;
import org.orekit.orbits.FieldOrbit;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.integration.AbstractGradientConverter;
import org.orekit.time.TimeScale;
import org.orekit.utils.FieldAngularCoordinates;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.TimeStampedFieldAngularCoordinates;
import org.orekit.utils.TimeStampedFieldPVCoordinates;

/** Converter for TLE propagator.
 * @author Luc Maisonobe
 * @author Bryan Cazabonne
 * @author Thomas Paulet
 * @since 11.0
 */
class TLEGradientConverter extends AbstractGradientConverter {

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

    /** States with various number of additional propagation parameters. */
    private final List<FieldSpacecraftState<Gradient>> gStates;

    /** Simple constructor.
     * @param propagator TLE propagator used to access initial orbit
     */
    TLEGradientConverter(final TLEPropagator propagator) {

        super(FREE_STATE_PARAMETERS);

        // TLE and related parameters
        this.tle  = propagator.getTLE();
        this.teme = propagator.getFrame();
        this.utc  = tle.getUtc();

        // Attitude provider
        this.provider = propagator.getAttitudeProvider();

        // Spacecraft state
        final SpacecraftState state = propagator.getInitialState();

        // Position always has derivatives
        final Vector3D pos = state.getPVCoordinates().getPosition();
        final FieldVector3D<Gradient> posG = new FieldVector3D<>(Gradient.variable(FREE_STATE_PARAMETERS, 0, pos.getX()),
                                                                 Gradient.variable(FREE_STATE_PARAMETERS, 1, pos.getY()),
                                                                 Gradient.variable(FREE_STATE_PARAMETERS, 2, pos.getZ()));

        // Velocity may have derivatives or not
        final Vector3D vel = state.getPVCoordinates().getVelocity();
        final FieldVector3D<Gradient> velG = new FieldVector3D<>(Gradient.variable(FREE_STATE_PARAMETERS, 3, vel.getX()),
                                                                 Gradient.variable(FREE_STATE_PARAMETERS, 4, vel.getY()),
                                                                 Gradient.variable(FREE_STATE_PARAMETERS, 5, vel.getZ()));

        // Acceleration never has derivatives
        final Vector3D acc = state.getPVCoordinates().getAcceleration();
        final FieldVector3D<Gradient> accG = new FieldVector3D<>(Gradient.constant(FREE_STATE_PARAMETERS, acc.getX()),
                                                                 Gradient.constant(FREE_STATE_PARAMETERS, acc.getY()),
                                                                 Gradient.constant(FREE_STATE_PARAMETERS, acc.getZ()));

        // Mass never has derivatives
        final Gradient gM = Gradient.constant(FREE_STATE_PARAMETERS, state.getMass());

        final Gradient gMu = Gradient.constant(FREE_STATE_PARAMETERS, TLEPropagator.getMU());

        final FieldOrbit<Gradient> gOrbit =
                        new FieldCartesianOrbit<>(new TimeStampedFieldPVCoordinates<>(state.getDate(), posG, velG, accG),
                                                  state.getFrame(), gMu);

        // Attitude
        final FieldAttitude<Gradient> gAttitude = provider.getAttitude(gOrbit, gOrbit.getDate(), gOrbit.getFrame());

        // Initialize the list with the state having 0 force model parameters
        gStates = new ArrayList<>();
        gStates.add(new FieldSpacecraftState<>(gOrbit, gAttitude, gM));

    }

    /** Get the state with the number of parameters consistent with TLE model.
     * @return state with the number of parameters consistent with TLE model
     */
    public FieldSpacecraftState<Gradient> getState() {

        // Count the required number of parameters
        int nbParams = 0;
        for (final ParameterDriver driver : tle.getParametersDrivers()) {
            if (driver.isSelected()) {
                ++nbParams;
            }
        }

        // Fill in intermediate slots
        while (gStates.size() < nbParams + 1) {
            gStates.add(null);
        }

        if (gStates.get(nbParams) == null) {
            // It is the first time we need this number of parameters
            // We need to create the state
            final int freeParameters = FREE_STATE_PARAMETERS + nbParams;
            final FieldSpacecraftState<Gradient> s0 = gStates.get(0);

            // Orbit
            final FieldPVCoordinates<Gradient> pv0 = s0.getPVCoordinates();
            final FieldOrbit<Gradient> gOrbit =
                            new FieldCartesianOrbit<>(new TimeStampedFieldPVCoordinates<>(s0.getDate().toAbsoluteDate(),
                                                                                          extend(pv0.getPosition(),     freeParameters),
                                                                                          extend(pv0.getVelocity(),     freeParameters),
                                                                                          extend(pv0.getAcceleration(), freeParameters)),
                                                      s0.getFrame(),
                                                      extend(s0.getMu(), freeParameters));

            // Attitude
            final FieldAngularCoordinates<Gradient> ac0 = s0.getAttitude().getOrientation();
            final FieldAttitude<Gradient> gAttitude =
                            new FieldAttitude<>(s0.getAttitude().getReferenceFrame(),
                                                new TimeStampedFieldAngularCoordinates<>(gOrbit.getDate(),
                                                                                         extend(ac0.getRotation(), freeParameters),
                                                                                         extend(ac0.getRotationRate(), freeParameters),
                                                                                         extend(ac0.getRotationAcceleration(), freeParameters)));

            // Mass
            final Gradient gM = extend(s0.getMass(), freeParameters);

            gStates.set(nbParams, new FieldSpacecraftState<>(gOrbit, gAttitude, gM));
        }

        return gStates.get(nbParams);

    }

    /** Get the model parameters.
     * @param state state as returned by {@link #getState(TLE)}
     * @return TLE model parameters
     */
    public Gradient[] getParameters(final FieldSpacecraftState<Gradient> state) {
        final int freeParameters = state.getMass().getFreeParameters();
        final List<ParameterDriver> drivers = tle.getParametersDrivers();
        final Gradient[] parameters = new Gradient[drivers.size()];
        int index = FREE_STATE_PARAMETERS;
        int i = 0;
        for (ParameterDriver driver : drivers) {
            parameters[i++] = driver.isSelected() ?
                              Gradient.variable(freeParameters, index++, driver.getValue()) :
                              Gradient.constant(freeParameters, driver.getValue());
        }
        return parameters;
    }

    /** Get the converted TLE propagator.
     * @param state state as returned by {@link #getState(TLE)}
     * @param parameters model parameters as returned by {@link #getParameters(FieldSpacecraftState, TLE)}
     * @return the converted propagator
     */
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
        final double bStar                = tle.getBStar();

        // Initialize the new TLE
        final FieldTLE<Gradient> templateTLE = new FieldTLE<>(satelliteNumber, classification,
                        launchYear, launchNumber, launchPiece, ephemerisType, elementNumber, state.getDate(),
                        zero, zero, zero, zero, zero, zero, zero, zero,
                        revolutionNumberAtEpoch, bStar, utc);

        // TLE
        final FieldTLE<Gradient> gTLE = FieldTLE.stateToTLE(state, templateTLE, utc, teme);

        // Return the "Field" propagator
        return FieldTLEPropagator.selectExtrapolator(gTLE, provider, state.getMass(), teme, parameters);

    }

}
