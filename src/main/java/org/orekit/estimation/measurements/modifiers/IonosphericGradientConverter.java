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
package org.orekit.estimation.measurements.modifiers;

import java.util.ArrayList;
import java.util.List;

import org.hipparchus.Field;
import org.hipparchus.analysis.differentiation.Gradient;
import org.hipparchus.analysis.differentiation.GradientField;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.attitudes.FieldAttitude;
import org.orekit.models.earth.ionosphere.IonosphericModel;
import org.orekit.orbits.FieldCartesianOrbit;
import org.orekit.orbits.FieldOrbit;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.integration.AbstractGradientConverter;
import org.orekit.utils.FieldAngularCoordinates;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.TimeStampedFieldAngularCoordinates;
import org.orekit.utils.TimeStampedFieldPVCoordinates;

/**
 * Converter for states and parameters arrays.
 * @author Bryan Cazabonne
 * @since 10.2
 */
public class IonosphericGradientConverter extends AbstractGradientConverter {

    /** Dimension of the state. */
    private final int freeStateParameters;

    /** States with various number of additional parameters for ionospheric models. */
    private final List<FieldSpacecraftState<Gradient>> gStates;

    /**
     * Simple constructor.
     * @param state regular state
     * @param freeStateParameters number of free parameters, either 3 (position) or 6 (position-velocity)
     * @param provider provider to use if attitude needs to be recomputed
     */
    public IonosphericGradientConverter(final SpacecraftState state, final int freeStateParameters,
                                        final AttitudeProvider provider) {

        super(freeStateParameters);
        this.freeStateParameters = freeStateParameters;

        // Derivative field
        final Field<Gradient> field =  GradientField.getField(freeStateParameters);

        // position always has derivatives
        final Vector3D pos = state.getPVCoordinates().getPosition();
        final FieldVector3D<Gradient> posG = new FieldVector3D<>(Gradient.variable(freeStateParameters, 0, pos.getX()),
                                                                 Gradient.variable(freeStateParameters, 1, pos.getY()),
                                                                 Gradient.variable(freeStateParameters, 2, pos.getZ()));

        // velocity may have derivatives or not
        final Vector3D vel = state.getPVCoordinates().getVelocity();
        final FieldVector3D<Gradient> velG;
        if (freeStateParameters > 3) {
            velG = new FieldVector3D<>(Gradient.variable(freeStateParameters, 3, vel.getX()),
                                       Gradient.variable(freeStateParameters, 4, vel.getY()),
                                       Gradient.variable(freeStateParameters, 5, vel.getZ()));
        } else {
            velG = new FieldVector3D<>(Gradient.constant(freeStateParameters, vel.getX()),
                                       Gradient.constant(freeStateParameters, vel.getY()),
                                       Gradient.constant(freeStateParameters, vel.getZ()));
        }

        // acceleration never has derivatives
        final Vector3D acc = state.getPVCoordinates().getAcceleration();
        final FieldVector3D<Gradient> accG = new FieldVector3D<>(Gradient.constant(freeStateParameters, acc.getX()),
                                                                 Gradient.constant(freeStateParameters, acc.getY()),
                                                                 Gradient.constant(freeStateParameters, acc.getZ()));

        // mass never has derivatives
        final Gradient gM = Gradient.constant(freeStateParameters, state.getMass());

        final FieldOrbit<Gradient> gOrbit =
                        new FieldCartesianOrbit<>(new TimeStampedFieldPVCoordinates<>(state.getDate(), posG, velG, accG),
                                                  state.getFrame(),
                                                  field.getZero().add(state.getMu()));

        final FieldAttitude<Gradient> gAttitude;
        if (freeStateParameters > 3) {
            // compute attitude partial derivatives with respect to position/velocity
            gAttitude = provider.getAttitude(gOrbit, gOrbit.getDate(), gOrbit.getFrame());
        } else {
            // force model does not depend on attitude, don't bother recomputing it
            gAttitude = new FieldAttitude<>(field, state.getAttitude());
        }

        // initialize the list with the state having 0 formce model parameters
        gStates = new ArrayList<>();
        gStates.add(new FieldSpacecraftState<>(gOrbit, gAttitude, gM));

    }

    /**
     * Get the number of free state parameters.
     * @return number of free state parameters
     */
    public int getFreeStateParameters() {
        return freeStateParameters;
    }

    /**
     * Get the state with the number of parameters consistent with ionospheric model.
     * @param ionoModel ionospheric model
     * @return state with the number of parameters consistent with ionospheric model
     */
    public FieldSpacecraftState<Gradient> getState(final IonosphericModel ionoModel) {

        // count the required number of parameters
        int nbParams = 0;
        for (final ParameterDriver driver : ionoModel.getParametersDrivers()) {
            if (driver.isSelected()) {
                ++nbParams;
            }
        }

        // fill in intermediate slots
        while (gStates.size() < nbParams + 1) {
            gStates.add(null);
        }

        if (gStates.get(nbParams) == null) {
            // it is the first time we need this number of parameters
            // we need to create the state
            final int freeParameters = freeStateParameters + nbParams;
            final FieldSpacecraftState<Gradient> s0 = gStates.get(0);

            // orbit
            final FieldPVCoordinates<Gradient> pv0 = s0.getPVCoordinates();
            final FieldOrbit<Gradient> gOrbit =
                            new FieldCartesianOrbit<>(new TimeStampedFieldPVCoordinates<>(s0.getDate().toAbsoluteDate(),
                                                                                          extend(pv0.getPosition(),     freeParameters),
                                                                                          extend(pv0.getVelocity(),     freeParameters),
                                                                                          extend(pv0.getAcceleration(), freeParameters)),
                                                      s0.getFrame(), extend(s0.getMu(), freeParameters));

            // attitude
            final FieldAngularCoordinates<Gradient> ac0 = s0.getAttitude().getOrientation();
            final FieldAttitude<Gradient> gAttitude =
                            new FieldAttitude<>(s0.getAttitude().getReferenceFrame(),
                                                new TimeStampedFieldAngularCoordinates<>(gOrbit.getDate(),
                                                                                         extend(ac0.getRotation(), freeParameters),
                                                                                         extend(ac0.getRotationRate(), freeParameters),
                                                                                         extend(ac0.getRotationAcceleration(), freeParameters)));

            // mass
            final Gradient gM = extend(s0.getMass(), freeParameters);

            gStates.set(nbParams, new FieldSpacecraftState<>(gOrbit, gAttitude, gM));

        }

        return gStates.get(nbParams);

    }

    /**
     * Get the ionospheric model parameters.
     * @param state state as returned by {@link #getState(IonosphericModel)}
     * @param ionoModel ionospheric model associated with the parameters
     * @return ionospheric model parameters
     */
    public Gradient[] getParameters(final FieldSpacecraftState<Gradient> state,
                                    final IonosphericModel ionoModel) {
        final int freeParameters = state.getMass().getFreeParameters();
        final List<ParameterDriver> drivers = ionoModel.getParametersDrivers();
        final Gradient[] parameters = new Gradient[drivers.size()];
        int index = freeStateParameters;
        for (int i = 0; i < drivers.size(); ++i) {
            parameters[i] = drivers.get(i).isSelected() ?
                            Gradient.variable(freeParameters, index++, drivers.get(i).getValue()) :
                            Gradient.constant(freeParameters, drivers.get(i).getValue());
        }
        return parameters;
    }

}
