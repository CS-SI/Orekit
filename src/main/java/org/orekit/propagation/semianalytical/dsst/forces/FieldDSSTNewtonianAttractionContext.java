/* Copyright 2002-2025 CS GROUP
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
package org.orekit.propagation.semianalytical.dsst.forces;

import org.hipparchus.CalculusFieldElement;
import org.orekit.propagation.semianalytical.dsst.utilities.FieldAuxiliaryElements;

/**
 * This class is a container for the common "field" parameters used in {@link DSSTNewtonianAttraction}.
 * <p>
 * It performs parameters initialization at each integration step for the central body attraction.
 * </p>
 * @author Bryan Cazabonne
 * @since 10.0
 * @param <T> type of the field elements
 */
public class FieldDSSTNewtonianAttractionContext<T extends CalculusFieldElement<T>> extends FieldForceModelContext<T> {

    /** Standard gravitational parameter μ for the body in m³/s². */
    private final T gm;

    /**
     * Simple constructor.
     *
     * @param auxiliaryElements auxiliary elements related to the current orbit
     * @param parameters        values of the force model parameters
     */
    FieldDSSTNewtonianAttractionContext(final FieldAuxiliaryElements<T> auxiliaryElements, final T[] parameters) {

        super(auxiliaryElements);
        this.gm = parameters[0];
    }

    /** Get standard gravitational parameter μ for the body in m³/s².
     * @return gm
     */
    public T getGM() {
        return gm;
    }
}
