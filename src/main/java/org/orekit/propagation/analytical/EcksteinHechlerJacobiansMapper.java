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
package org.orekit.propagation.analytical;

import org.hipparchus.analysis.differentiation.Gradient;
import org.orekit.annotation.DefaultDataContext;
import org.orekit.orbits.FieldCartesianOrbit;
import org.orekit.orbits.FieldOrbit;
import org.orekit.orbits.OrbitType;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.ParameterDriversList;

/** Mapper between two-dimensional Jacobian matrices and one-dimensional {@link
 * SpacecraftState#getAdditionalState(String) additional state arrays}.
 * <p>
 * This class does not hold the states by itself. Instances of this class are guaranteed
 * to be immutable.
 * </p>
 * @author Nicolas Fialton
 */

public class EcksteinHechlerJacobiansMapper extends AbstractAnalyticalJacobiansMapper {

    /** Name. */
    private String name;

    /** Propagator computing state evolution. */
    private final EcksteinHechlerPropagator propagator;

    /** Simple constructor.
     * @param name name of the Jacobians
     * @param propagator the propagator that will handle the orbit propagation
     */
    public EcksteinHechlerJacobiansMapper(final String name,
                                          final EcksteinHechlerPropagator propagator) {

        super(name, new ParameterDriversList(), EcksteinHechlerGradientConverter.FREE_STATE_PARAMETERS, null);
        this.name       = name;
        this.propagator = propagator;
    }

    /** {@inheritDoc} */
    @Override
    @DefaultDataContext
    public void analyticalDerivatives(final SpacecraftState s) {

        final double[] p = s.getAdditionalState(name);
        if (getStateTransition() == null) {
            setStateTransition(new double[p.length]);
        }

        // initialize Jacobians to zero
        final int dim = getSTATE_DIMENSION();
        final double[][] stateGrad = new double[dim][dim];
        final EcksteinHechlerGradientConverter converter = new EcksteinHechlerGradientConverter(s, propagator);
        final FieldEcksteinHechlerPropagator<Gradient> gPropagator = converter.getPropagator();
        final Gradient[] gParameters = converter.getParameters();

        // Compute Jacobian
        final AbsoluteDate init = getInitialState().getDate();
        final AbsoluteDate end  = s.getDate();
        final double dt = end.durationFrom(init);
        final FieldOrbit<Gradient> orbit = gPropagator.propagateOrbit(gPropagator.getInitialState().getDate().shiftedBy(dt), gParameters);
        final FieldCartesianOrbit<Gradient> gOrbit = (FieldCartesianOrbit<Gradient>) OrbitType.CARTESIAN.convertType(orbit);

        final double[] derivativesX    = gOrbit.getPVCoordinates().getPosition().getX().getGradient();
        final double[] derivativesY    = gOrbit.getPVCoordinates().getPosition().getY().getGradient();
        final double[] derivativesZ    = gOrbit.getPVCoordinates().getPosition().getZ().getGradient();
        final double[] derivativesXDot = gOrbit.getPVCoordinates().getVelocity().getX().getGradient();
        final double[] derivativesYDot = gOrbit.getPVCoordinates().getVelocity().getY().getGradient();
        final double[] derivativesZDot = gOrbit.getPVCoordinates().getVelocity().getZ().getGradient();

        // update Jacobian with respect to state
        addToRow(derivativesX,            0, stateGrad);
        addToRow(derivativesY,            1, stateGrad);
        addToRow(derivativesZ,            2, stateGrad);
        addToRow(derivativesXDot,         3, stateGrad);
        addToRow(derivativesYDot,         4, stateGrad);
        addToRow(derivativesZDot,         5, stateGrad);

     // the previous derivatives correspond to the state transition matrix
        for (int i = 0; i < dim; i++) {
            for (int j = 0; j < dim; j++) {
                getStateTransition()[j + dim * i] = stateGrad[i][j];
            }
        }

    }

   /** Getter for initial propagator state.
    * @return the propagator initial state
    */
    public SpacecraftState getInitialState() {
        return propagator.getInitialState();
    }

}
