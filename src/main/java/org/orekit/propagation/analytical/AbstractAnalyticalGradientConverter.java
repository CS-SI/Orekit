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
package org.orekit.propagation.analytical;

import java.util.ArrayList;
import java.util.List;

import org.hipparchus.analysis.differentiation.Gradient;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.attitudes.FieldAttitude;
import org.orekit.orbits.FieldCartesianOrbit;
import org.orekit.orbits.FieldOrbit;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.integration.AbstractGradientConverter;
import org.orekit.utils.FieldAngularCoordinates;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.ParameterDriversProvider;
import org.orekit.utils.TimeStampedFieldAngularCoordinates;
import org.orekit.utils.TimeStampedFieldPVCoordinates;

/**
 * Converter for analytical orbit propagator.
 *
 * @author Bryan Cazabonne
 * @since 11.1
 */
public abstract class AbstractAnalyticalGradientConverter extends AbstractGradientConverter implements ParameterDriversProvider {

    /** Attitude provider. */
    private final AttitudeProvider provider;

    /** States with various number of additional propagation parameters. */
    private final List<FieldSpacecraftState<Gradient>> gStates;

    /**
     * Constructor.
     * @param propagator analytical orbit propagator
     * @param mu central attraction coefficient
     * @param freeStateParameters number of free parameters
     */
    protected AbstractAnalyticalGradientConverter(final AbstractAnalyticalPropagator propagator,
                                                  final double mu,
                                                  final int freeStateParameters) {
        super(freeStateParameters);

        // Attitude provider
        this.provider = propagator.getAttitudeProvider();

        // Spacecraft state
        final SpacecraftState state = propagator.getInitialState();

        // Position always has derivatives
        final Vector3D pos = state.getPosition();
        final FieldVector3D<Gradient> posG = new FieldVector3D<>(Gradient.variable(freeStateParameters, 0, pos.getX()),
                                                                 Gradient.variable(freeStateParameters, 1, pos.getY()),
                                                                 Gradient.variable(freeStateParameters, 2, pos.getZ()));

        // Velocity may have derivatives or not
        final Vector3D vel = state.getPVCoordinates().getVelocity();
        final FieldVector3D<Gradient> velG = new FieldVector3D<>(Gradient.variable(freeStateParameters, 3, vel.getX()),
                                                                 Gradient.variable(freeStateParameters, 4, vel.getY()),
                                                                 Gradient.variable(freeStateParameters, 5, vel.getZ()));

        // Acceleration never has derivatives
        final Vector3D acc = state.getPVCoordinates().getAcceleration();
        final FieldVector3D<Gradient> accG = new FieldVector3D<>(Gradient.constant(freeStateParameters, acc.getX()),
                                                                 Gradient.constant(freeStateParameters, acc.getY()),
                                                                 Gradient.constant(freeStateParameters, acc.getZ()));

        // Mass never has derivatives
        final Gradient gM  = Gradient.constant(freeStateParameters, state.getMass());
        final Gradient gMu = Gradient.constant(freeStateParameters, mu);

        final FieldOrbit<Gradient> gOrbit =
                        new FieldCartesianOrbit<>(new TimeStampedFieldPVCoordinates<>(state.getDate(), posG, velG, accG),
                                                  state.getFrame(), gMu);

        // Attitude
        final FieldAttitude<Gradient> gAttitude = provider.getAttitude(gOrbit, gOrbit.getDate(), gOrbit.getFrame());

        // Initialize the list with the state having 0 force model parameters
        gStates = new ArrayList<>();
        gStates.add(new FieldSpacecraftState<>(gOrbit, gAttitude, gM));

    }

    /** Get the state with the number of parameters consistent with the propagation model.
     * @return state with the number of parameters consistent with the propagation model
     */
    public FieldSpacecraftState<Gradient> getState() {

        // Count the required number of parameters
        int nbParams = 0;
        for (final ParameterDriver driver : getParametersDrivers()) {
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
            final int freeParameters = getFreeStateParameters() + nbParams;
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

    /**
     * Get the converted analytical orbit propagator.
     * @param state state as returned by {@link #getState()}
     * @param parameters model parameters
     * @return the converted analytical orbit propagator
     */
    public abstract FieldAbstractAnalyticalPropagator<Gradient> getPropagator(FieldSpacecraftState<Gradient> state,
                                                                              Gradient[] parameters);

}
