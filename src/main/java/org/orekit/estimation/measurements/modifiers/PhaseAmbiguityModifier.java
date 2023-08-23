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
package org.orekit.estimation.measurements.modifiers;

import java.util.List;

import org.orekit.estimation.measurements.EstimatedMeasurement;
import org.orekit.estimation.measurements.EstimatedMeasurementBase;
import org.orekit.estimation.measurements.EstimationModifier;
import org.orekit.estimation.measurements.gnss.Phase;
import org.orekit.utils.ParameterDriver;

/** Class modifying theoretical phase measurement with ambiguity.
 *
 * @author Luc Maisonobe
 * @since 9.2
 */
public class PhaseAmbiguityModifier extends AbstractAmbiguityModifier implements EstimationModifier<Phase> {

    /** Constructor.
     * <p>
     * It is expected that many different ambiguities will be used at the
     * same time during an orbit determination, therefore they are keyed
     * using a simple integer. All ambiguities using the same key will
     * be enforced to be equal. It is the responsibility of the caller to
     * use a proper counter to manage the ambiguities properly.
     * </p>
     * @param key key to identify the ambiguity
     * @param ambiguity initial value of ambiguity
     */
    public PhaseAmbiguityModifier(final int key, final double ambiguity) {
        super(key, ambiguity);
    }

    /** {@inheritDoc} */
    @Override
    public List<ParameterDriver> getParametersDrivers() {
        return getDrivers();
    }

    @Override
    public void modifyWithoutDerivatives(final EstimatedMeasurementBase<Phase> estimated) {
        doModifyWithoutDerivatives(estimated);
    }

    @Override
    public void modify(final EstimatedMeasurement<Phase> estimated) {
        doModify(estimated);
    }

}
