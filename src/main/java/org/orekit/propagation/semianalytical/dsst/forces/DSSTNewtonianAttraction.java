/* Copyright 2002-2020 CS Group
 * Licensed to CS Group (CS) under one or more
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
package org.orekit.propagation.semianalytical.dsst.forces;

import java.util.Collections;
import java.util.List;

import org.hipparchus.Field;
import org.hipparchus.RealFieldElement;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathArrays;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitInternalError;
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.orbits.FieldEquinoctialOrbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.PropagationType;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.events.FieldEventDetector;
import org.orekit.propagation.semianalytical.dsst.DSSTPropagator;
import org.orekit.propagation.semianalytical.dsst.utilities.AuxiliaryElements;
import org.orekit.propagation.semianalytical.dsst.utilities.FieldAuxiliaryElements;
import org.orekit.utils.ParameterDriver;

/** Force model for Newtonian central body attraction for the {@link DSSTPropagator DSST propagator}.
 *  @author Bryan Cazabonne
 *  @author Luc Maisonobe
 *  @since 10.0
 */
public class DSSTNewtonianAttraction implements DSSTForceModel {

    /** Name of the single parameter of this model: the central attraction coefficient. */
    public static final String CENTRAL_ATTRACTION_COEFFICIENT = "central attraction coefficient";

    /** Central attraction scaling factor.
     * <p>
     * We use a power of 2 to avoid numeric noise introduction
     * in the multiplications/divisions sequences.
     * </p>
     */
    private static final double MU_SCALE = FastMath.scalb(1.0, 32);

    /** Driver for gravitational parameter. */
    private final ParameterDriver gmParameterDriver;

    /** Simple constructor.
     * @param mu central attraction coefficient (m^3/s^2)
     */
    public DSSTNewtonianAttraction(final double mu) {
        try {
            gmParameterDriver = new ParameterDriver(DSSTNewtonianAttraction.CENTRAL_ATTRACTION_COEFFICIENT,
                                                    mu, MU_SCALE,
                                                    0.0, Double.POSITIVE_INFINITY);
        } catch (OrekitException oe) {
            // this should never occur as valueChanged above never throws an exception
            throw new OrekitInternalError(oe);
        }
    }

    /** Get the central attraction coefficient μ.
     * @return mu central attraction coefficient (m³/s²)
     */
    public double getMu() {
        return gmParameterDriver.getValue();
    }

    /** {@inheritDoc} */
    @Override
    public List<ShortPeriodTerms> initialize(final AuxiliaryElements auxiliaryElements,
                                             final PropagationType type,
                                             final double[] parameters) {
        return Collections.emptyList();
    }

    /** {@inheritDoc} */
    @Override
    public <T extends RealFieldElement<T>> List<FieldShortPeriodTerms<T>> initialize(final FieldAuxiliaryElements<T> auxiliaryElements,
                                                                                     final PropagationType type,
                                                                                     final T[] parameters) {
        return Collections.emptyList();
    }

    /** Performs initialization at each integration step for the current force model.
     *  <p>
     *  This method aims at being called before mean elements rates computation.
     *  </p>
     *  @param auxiliaryElements auxiliary elements related to the current orbit
     *  @param parameters values of the force model parameters
     *  @return new force model context
     */
    private DSSTNewtonianAttractionContext initializeStep(final AuxiliaryElements auxiliaryElements, final double[] parameters) {
        return new DSSTNewtonianAttractionContext(auxiliaryElements, parameters);
    }

    /** Performs initialization at each integration step for the current force model.
     *  <p>
     *  This method aims at being called before mean elements rates computation.
     *  </p>
     *  @param <T> type of the elements
     *  @param auxiliaryElements auxiliary elements related to the current orbit
     *  @param parameters values of the force model parameters
     *  @return new force model context
     */
    private <T extends RealFieldElement<T>> FieldDSSTNewtonianAttractionContext<T> initializeStep(final FieldAuxiliaryElements<T> auxiliaryElements,
                                                                                                  final T[] parameters) {
        return new FieldDSSTNewtonianAttractionContext<>(auxiliaryElements, parameters);
    }

    /** {@inheritDoc} */
    @Override
    public double[] getMeanElementRate(final SpacecraftState state,
                                       final AuxiliaryElements auxiliaryElements,
                                       final double[] parameters) {

        // Container for attributes
        final DSSTNewtonianAttractionContext context = initializeStep(auxiliaryElements, parameters);

        final double[] yDot = new double[7];
        final EquinoctialOrbit orbit = (EquinoctialOrbit) OrbitType.EQUINOCTIAL.convertType(state.getOrbit());
        orbit.addKeplerContribution(PositionAngle.MEAN, context.getGM(), yDot);

        return yDot;

    }

    /** {@inheritDoc} */
    @Override
    public <T extends RealFieldElement<T>> T[] getMeanElementRate(final FieldSpacecraftState<T> state,
                                                                  final FieldAuxiliaryElements<T> auxiliaryElements,
                                                                  final T[] parameters) {

        // Field for array building
        final Field<T> field = state.getMu().getField();
        // Container for attributes
        final FieldDSSTNewtonianAttractionContext<T> context = initializeStep(auxiliaryElements, parameters);

        final T[] yDot = MathArrays.buildArray(field, 7);
        final FieldEquinoctialOrbit<T> orbit = (FieldEquinoctialOrbit<T>) OrbitType.EQUINOCTIAL.convertType(state.getOrbit());
        orbit.addKeplerContribution(PositionAngle.MEAN, context.getGM(), yDot);

        return yDot;
    }

    /** {@inheritDoc} */
    @Override
    public EventDetector[] getEventsDetectors() {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public <T extends RealFieldElement<T>> FieldEventDetector<T>[] getFieldEventsDetectors(final Field<T> field) {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public void registerAttitudeProvider(final AttitudeProvider provider) {
      //nothing is done since this contribution is not sensitive to attitude
    }

    /** {@inheritDoc} */
    @Override
    public void updateShortPeriodTerms(final double[] parameters,
                                       final SpacecraftState... meanStates) {
    }

    /** {@inheritDoc} */
    @Override
    @SuppressWarnings("unchecked")
    public <T extends RealFieldElement<T>> void updateShortPeriodTerms(final T[] parameters,
                                                                       final FieldSpacecraftState<T>... meanStates) {
    }

    /** {@inheritDoc} */
    @Override
    public ParameterDriver[] getParametersDrivers() {
        return new ParameterDriver[] {
            gmParameterDriver
        };
    }

}
