/* Copyright 2002-2017 CS Systèmes d'Information
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
package org.orekit.propagation.numerical;

import java.util.ArrayList;
import java.util.List;

import org.hipparchus.analysis.differentiation.DSFactory;
import org.hipparchus.analysis.differentiation.DerivativeStructure;
import org.hipparchus.geometry.euclidean.threed.FieldRotation;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.attitudes.FieldAttitude;
import org.orekit.errors.OrekitException;
import org.orekit.forces.ForceModel;
import org.orekit.orbits.FieldCartesianOrbit;
import org.orekit.orbits.FieldOrbit;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.utils.FieldAngularCoordinates;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.TimeStampedFieldAngularCoordinates;
import org.orekit.utils.TimeStampedFieldPVCoordinates;

/** Converter for states and parameters arrays.
 * @author Luc Maisonobe
 * @since 9.0
 */
class DSConverter {

    /** Dimension of the state. */
    private final int freeStateParameters;

    /** States with various number of additional parameters for force models. */
    private final List<FieldSpacecraftState<DerivativeStructure>> dsStates;

    /** Simple constructor.
     * @param state
     * @param state regular state
     * @param freeStateParameters number of free parameters, either 3 (position),
     * 6 (position-velocity) or 7 (position-velocity-mass)
     * @param provider provider to use if attitude needs to be recomputed
     * @exception OrekitException if attitude cannot be computed
     */
    DSConverter(final SpacecraftState state, final int freeStateParameters, final AttitudeProvider provider)
        throws OrekitException {

        this.freeStateParameters = freeStateParameters;

        // prepare derivation variables, position, optionally velocity and mass
        final DSFactory factory = new DSFactory(freeStateParameters, 1);

        // position always has derivatives
        final Vector3D pos = state.getPVCoordinates().getPosition();
        final FieldVector3D<DerivativeStructure> posDS = new FieldVector3D<>(factory.variable(0, pos.getX()),
                        factory.variable(1, pos.getY()),
                        factory.variable(2, pos.getZ()));

        // velocity may have derivatives or not
        final Vector3D vel = state.getPVCoordinates().getVelocity();
        final FieldVector3D<DerivativeStructure> velDS;
        if (freeStateParameters > 3) {
            velDS = new FieldVector3D<>(factory.variable(3, vel.getX()),
                            factory.variable(4, vel.getY()),
                            factory.variable(5, vel.getZ()));
        } else {
            velDS = new FieldVector3D<>(factory.constant(vel.getX()),
                            factory.constant(vel.getY()),
                            factory.constant(vel.getZ()));
        }

        // acceleration never has derivatives
        final Vector3D acc = state.getPVCoordinates().getAcceleration();
        final FieldVector3D<DerivativeStructure> accDS = new FieldVector3D<>(factory.constant(acc.getX()),
                        factory.constant(acc.getY()),
                        factory.constant(acc.getZ()));

        // mass may have derivatives or not
        final DerivativeStructure dsM = (freeStateParameters > 6) ?
                                                                   factory.variable(6, state.getMass()) :
                                                                       factory.constant(state.getMass());

        final FieldOrbit<DerivativeStructure> dsOrbit =
                        new FieldCartesianOrbit<>(new TimeStampedFieldPVCoordinates<>(state.getDate(), posDS, velDS, accDS),
                                        state.getFrame(), state.getMu());

        final FieldAttitude<DerivativeStructure> dsAttitude;
        if (freeStateParameters > 3) {
            // compute attitude partial derivatives with respect to position/velocity
            dsAttitude = provider.getAttitude(dsOrbit, dsOrbit.getDate(), dsOrbit.getFrame());
        } else {
            // force model does not depend on attitude, don't bother recomputing it
            dsAttitude = new FieldAttitude<>(factory.getDerivativeField(), state.getAttitude());
        }

        // initialize the list with the state having 0 formce model parameters
        dsStates = new ArrayList<>();
        dsStates.add(new FieldSpacecraftState<>(dsOrbit, dsAttitude, dsM));

    }

    /** Get the number of free state parameters.
     * @return number of free state parameters
     */
    public int getFreeStateParameters() {
        return freeStateParameters;
    }

    /** Get the state with the number of parameters consistent with force model.
     * @param forceModel force model
     * @return state with the number of parameters consistent with force model
     */
    public FieldSpacecraftState<DerivativeStructure> getState(final ForceModel forceModel) {

        // count the required number of parameters
        int nbParams = 0;
        for (final ParameterDriver driver : forceModel.getParametersDrivers()) {
            if (driver.isSelected()) {
                ++nbParams;
            }
        }

        // fill in intermediate slots
        while (dsStates.size() < nbParams + 1) {
            dsStates.add(null);
        }

        if (dsStates.get(nbParams) == null) {
            // it is the first time we need this number of parameters
            // we need to create the state
            final DSFactory factory = new DSFactory(freeStateParameters + nbParams, 1);
            final FieldSpacecraftState<DerivativeStructure> s0 = dsStates.get(0);

            // orbit
            final FieldPVCoordinates<DerivativeStructure> pv0 = s0.getPVCoordinates();
            final FieldOrbit<DerivativeStructure> dsOrbit =
                            new FieldCartesianOrbit<>(new TimeStampedFieldPVCoordinates<>(s0.getDate().toAbsoluteDate(),
                                                                                          extend(pv0.getPosition(),     factory),
                                                                                          extend(pv0.getVelocity(),     factory),
                                                                                          extend(pv0.getAcceleration(), factory)),
                                                      s0.getFrame(), s0.getMu());

            // attitude
            final FieldAngularCoordinates<DerivativeStructure> ac0 = s0.getAttitude().getOrientation();
            final FieldAttitude<DerivativeStructure> dsAttitude =
                            new FieldAttitude<>(s0.getAttitude().getReferenceFrame(),
                                                new TimeStampedFieldAngularCoordinates<>(dsOrbit.getDate(),
                                                                                         extend(ac0.getRotation(), factory),
                                                                                         extend(ac0.getRotationRate(), factory),
                                                                                         extend(ac0.getRotationAcceleration(), factory)));

            // mass
            final DerivativeStructure dsM = extend(s0.getMass(), factory);

            dsStates.set(nbParams, new FieldSpacecraftState<>(dsOrbit, dsAttitude, dsM));

        }

        return dsStates.get(nbParams);

    }

    /** Add zero derivatives.
     * @param original original scalar
     * @param factory factory for the extended derivatives
     * @return extended scalar
     */
    private DerivativeStructure extend(final DerivativeStructure original, final DSFactory factory) {
        final double[] originalDerivatives = original.getAllDerivatives();
        final double[] extendedDerivatives = new double[factory.getCompiler().getSize()];
        System.arraycopy(originalDerivatives, 0, extendedDerivatives, 0, originalDerivatives.length);
        return factory.build(extendedDerivatives);
    }

    /** Add zero derivatives.
     * @param original original vector
     * @param factory factory for the extended derivatives
     * @return extended vector
     */
    private FieldVector3D<DerivativeStructure> extend(final FieldVector3D<DerivativeStructure> original, final DSFactory factory) {
        return new FieldVector3D<>(extend(original.getX(), factory),
                        extend(original.getY(), factory),
                        extend(original.getZ(), factory));
    }

    /** Add zero derivatives.
     * @param original original rotation
     * @param factory factory for the extended derivatives
     * @return extended rotation
     */
    private FieldRotation<DerivativeStructure> extend(final FieldRotation<DerivativeStructure> original, final DSFactory factory) {
        return new FieldRotation<>(extend(original.getQ0(), factory),
                        extend(original.getQ1(), factory),
                        extend(original.getQ2(), factory),
                        extend(original.getQ3(), factory),
                        false);
    }

    /** Get the force model parameters.
     * @param state state as returned by {@link #getState(ForceModel)}
     * @param forceModel force model associated with the parameters
     * @return force model parameters
     * @since 9.0
     */
    public DerivativeStructure[] getParameters(final FieldSpacecraftState<DerivativeStructure> state,
                                               final ForceModel forceModel) {
        final DSFactory factory = state.getMass().getFactory();
        final ParameterDriver[] drivers = forceModel.getParametersDrivers();
        final DerivativeStructure[] parameters = new DerivativeStructure[drivers.length];
        int index = freeStateParameters;
        for (int i = 0; i < drivers.length; ++i) {
            parameters[i] = drivers[i].isSelected() ?
                            factory.variable(index++, drivers[i].getValue()) :
                            factory.constant(drivers[i].getValue());
        }
        return parameters;
    }

}
