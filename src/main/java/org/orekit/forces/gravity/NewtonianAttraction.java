/* Copyright 2010-2011 Centre National d'Études Spatiales
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
package org.orekit.forces.gravity;

import java.util.Collections;
import java.util.List;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.orekit.forces.ForceModel;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.numerical.FieldTimeDerivativesEquations;
import org.orekit.propagation.numerical.TimeDerivativesEquations;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.ParameterDriver;

/** Force model for Newtonian central body attraction.
 * @author Luc Maisonobe
 */
public class NewtonianAttraction implements ForceModel {

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
    public NewtonianAttraction(final double mu) {
        gmParameterDriver = new ParameterDriver(NewtonianAttraction.CENTRAL_ATTRACTION_COEFFICIENT,
                                                mu, MU_SCALE,
                                                0.0, Double.POSITIVE_INFINITY);
    }

    /** {@inheritDoc} */
    @Override
    public boolean dependsOnPositionOnly() {
        return true;
    }

    /** Get the central attraction coefficient μ.
     * @param date date at which the mu value wants to be known
     * @return mu central attraction coefficient (m³/s²)
     */
    public double getMu(final AbsoluteDate date) {
        return gmParameterDriver.getValue(date);
    }

    /** Get the central attraction coefficient μ.
     * @param <T> the type of the field element
     * @param field field to which the state belongs
     * @param date date at which the mu value wants to be known
     * @return mu central attraction coefficient (m³/s²)
     */
    public <T extends CalculusFieldElement<T>> T getMu(final Field<T> field, final FieldAbsoluteDate<T> date) {
        final T zero = field.getZero();
        return zero.add(gmParameterDriver.getValue(date.toAbsoluteDate()));
    }

    /** {@inheritDoc} */
    @Override
    public void addContribution(final SpacecraftState s, final TimeDerivativesEquations adder) {
        adder.addKeplerContribution(getMu(s.getDate()));
    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> void addContribution(final FieldSpacecraftState<T> s,
                                                                final FieldTimeDerivativesEquations<T> adder) {
        final Field<T> field = s.getDate().getField();
        adder.addKeplerContribution(getMu(field, s.getDate()));
    }

    /** {@inheritDoc} */
    @Override
    public Vector3D acceleration(final SpacecraftState s, final double[] parameters) {
        final double mu = parameters[0];
        final double r2 = s.getPosition().getNormSq();
        return new Vector3D(-mu / (FastMath.sqrt(r2) * r2), s.getPosition());
    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> FieldVector3D<T> acceleration(final FieldSpacecraftState<T> s,
                                                                         final T[] parameters) {
        final T mu = parameters[0];
        final T r2 = s.getPosition().getNormSq();
        return new FieldVector3D<>(r2.sqrt().multiply(r2).reciprocal().multiply(mu).negate(), s.getPosition());
    }

    /** {@inheritDoc} */
    @Override
    public List<ParameterDriver> getParametersDrivers() {
        return Collections.singletonList(gmParameterDriver);
    }

}

