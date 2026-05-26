/* Copyright 2002-2026 CS GROUP
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

import org.orekit.estimation.measurements.EstimatedMeasurementBase;
import org.orekit.estimation.measurements.gnss.Phase;

/** Class modifying theoretical phase measurement with Shapiro time delay.
 * <p>
 * Shapiro time delay is a relativistic effect due to gravity.
 * </p>
 *
 * @author Bryan Cazabonne
 * @author Luc Maisonobe
 * @since 10.2
 */
public class ShapiroPhaseModifier extends AbstractShapiroPhaseModifier<Phase> {

    /** Simple constructor from gravitational constant.
     * @param gm gravitational constant for main body in signal path vicinity.
     */
    public ShapiroPhaseModifier(final double gm) {
        this(new ShapiroModel(gm));
    }

    /** Constructor.
     * @param shapiroModel Shapiro delay computer
     * @since 14.0
     */
    public ShapiroPhaseModifier(final ShapiroModel shapiroModel) {
        super(shapiroModel);
    }

    /** {@inheritDoc} */
    @Override
    public void modifyWithoutDerivatives(final EstimatedMeasurementBase<Phase> estimated) {
        final double wavelength = estimated.getObservedMeasurement().getWavelength();
        modifyWithoutDerivatives(estimated, wavelength);
    }

}
