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

import org.orekit.propagation.semianalytical.dsst.utilities.AuxiliaryElements;

/** Base class for dsst force models attributes containers.
 * @author Bryan Cazabonne
 * @since 10.0
 */
public abstract class ForceModelContext {

    /** Common parameters used by all DSST forces. */
    private final AuxiliaryElements auxiliaryElements;

    /** Simple constructor.
     * @param auxiliaryElements auxiliary elements related to the current orbit
     */
    protected ForceModelContext(final AuxiliaryElements auxiliaryElements) {
        this.auxiliaryElements = auxiliaryElements;
    }

    /** Method to get the auxiliary elements related to the {@link ForceModelContext}.
     * @return auxiliary elements
     */
    public AuxiliaryElements getAuxiliaryElements() {
        return auxiliaryElements;
    }

}
