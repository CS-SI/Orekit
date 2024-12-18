/* Copyright 2022-2024 Romain Serra
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
package org.orekit.control.indirect.shooting.propagation;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.orekit.propagation.conversion.FieldODEIntegratorBuilder;
import org.orekit.propagation.conversion.ODEIntegratorBuilder;

/**
 * Defines integration settings for indirect shooting methods. Gives standard and Field integrator builders.
 *
 * @author Romain Serra
 * @since 12.2
 * @see ShootingPropagationSettings
 * @see ODEIntegratorBuilder
 * @see FieldODEIntegratorBuilder
 */
public interface ShootingIntegrationSettings {

    /**
     * Returns an ODE integrator builder.
     * @return builder
     */
    ODEIntegratorBuilder getIntegratorBuilder();

    /**
     * Returns a Field ODE integrator builder.
     * @param field field for builder
     * @return builder
     * @param <T> field type
     */
    <T extends CalculusFieldElement<T>> FieldODEIntegratorBuilder<T> getFieldIntegratorBuilder(Field<T> field);
}
