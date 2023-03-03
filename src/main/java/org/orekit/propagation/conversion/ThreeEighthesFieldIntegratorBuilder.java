/* Copyright 2002-2022 CS GROUP
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
package org.orekit.propagation.conversion;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.ode.AbstractFieldIntegrator;
import org.hipparchus.ode.nonstiff.ThreeEighthesFieldIntegrator;
import org.orekit.orbits.FieldOrbit;
import org.orekit.orbits.OrbitType;

/** Builder for ThreeEighthesFieldIntegrator.
 * @author Pascal Parraud
 * @author Vincent Cucchietti
 * @since 12.0
 */
public class ThreeEighthesFieldIntegratorBuilder <T extends CalculusFieldElement<T>> implements FieldODEIntegratorBuilder<T> {

    /** Step size (s). */
    private final T step;

    /** Build a new instance.
     * @param step step size (s)
     * @see ThreeEighthesFieldIntegrator
     */
    public ThreeEighthesFieldIntegratorBuilder(final T step) {
        this.step = step;
    }

    @Override
    public AbstractFieldIntegrator<T> buildIntegrator(final Field<T> field, final FieldOrbit<T> orbit,
                                                      final OrbitType orbitType) {
        return new ThreeEighthesFieldIntegrator<>(field, step);
    }
}
