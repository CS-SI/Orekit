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
package org.orekit.propagation.semianalytical.dsst.forces;

import org.hipparchus.CalculusFieldElement;
import org.orekit.propagation.semianalytical.dsst.utilities.FieldAuxiliaryElements;

/** Base class for dsst force models parameter containers.
 * @author Bryan Cazabonne
 * @since 10.0
 * @param <T> type of the field elements
 */
public abstract class FieldForceModelContext<T extends CalculusFieldElement<T>> {

    /** Common parameters used by all DSST forces. */
    private final FieldAuxiliaryElements<T> auxiliaryElements;

    /** Simple constructor.
     * @param auxiliaryElements auxiliary elements related to the current orbit
     */
    protected FieldForceModelContext(final FieldAuxiliaryElements<T> auxiliaryElements) {
        this.auxiliaryElements = auxiliaryElements;
    }

    /** Method to get the auxiliary elementsrelated to the {@link ForceModelContext}.
     * @return field auxiliary elements
     */
    public FieldAuxiliaryElements<T> getFieldAuxiliaryElements() {
        return auxiliaryElements;
    }

}
