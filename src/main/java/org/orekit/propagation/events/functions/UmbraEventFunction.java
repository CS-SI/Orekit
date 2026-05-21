/* Copyright 2022-2026 Romain Serra
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
package org.orekit.propagation.events.functions;

import org.hipparchus.CalculusFieldElement;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.utils.OccultationEngine;

/**
 * Class for umbra event with conical shadows from ellipsoidal occulting bodies.
 * It is negative in umbra, positive otherwise.
 * @author Romain Serra
 * @see OccultationEngine
 * @since 14.0
 */
public class UmbraEventFunction implements EventFunction {

    /** Occultation engine. */
    private final OccultationEngine occultationEngine;

    /**
     * Constructor.
     * @param occultationEngine occultation engine
     */
    public UmbraEventFunction(final OccultationEngine occultationEngine) {
        this.occultationEngine = occultationEngine;
    }

    @Override
    public double value(final SpacecraftState state) {
        final OccultationEngine.OccultationAngles angles = occultationEngine.angles(state);
        return angles.getSeparation() - angles.getLimbRadius() + angles.getOccultedApparentRadius();
    }

    @Override
    public <T extends CalculusFieldElement<T>> T value(final FieldSpacecraftState<T> fieldState) {
        final OccultationEngine.FieldOccultationAngles<T> angles = occultationEngine.angles(fieldState);
        return angles.getSeparation().subtract(angles.getLimbRadius()).add(angles.getOccultedApparentRadius());
    }
}
