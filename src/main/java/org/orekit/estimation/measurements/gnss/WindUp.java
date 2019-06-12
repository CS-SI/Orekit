/* Copyright 2002-2019 CS Systèmes d'Information
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
package org.orekit.estimation.measurements.gnss;

import java.util.Collections;
import java.util.List;

import org.orekit.estimation.measurements.EstimatedMeasurement;
import org.orekit.estimation.measurements.EstimationModifier;
import org.orekit.utils.ParameterDriver;

/** Modifier for wind-up effect in GNSS {@link Phase phase measurements}.
 * @see WindUpFactory
 * @author Luc Maisonobe
 * @since 10.1
 */
public class WindUp implements EstimationModifier<Phase> {

    /** Simple constructor.
     * <p>
     * The constructor is package protected to enforce use of {@link WindUpFactory}
     * to preserve phase continuity for successive measurements involving the same
     * satellite/receiver pair.
     * </p>
     */
    WindUp() {
        // TODO
    }

    /** {@inheritDoc}
     * <p>
     * Wind-up effect has no parameters, the returned list is always empty.
     * </p>
     */
    @Override
    public List<ParameterDriver> getParametersDrivers() {
        return Collections.emptyList();
    }

    /** {@inheritDoc} */
    @Override
    public void modify(final EstimatedMeasurement<Phase> estimated) {
        // TODO
    }

}
