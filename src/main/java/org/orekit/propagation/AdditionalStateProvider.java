/* Copyright 2002-2016 CS Systèmes d'Information
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
package org.orekit.propagation;

import org.orekit.errors.OrekitException;

/** This interface represents providers for additional state data beyond {@link SpacecraftState}.
 * <p>
 * This interface is the analytical (read already integrated) counterpart of
 * the {@link org.orekit.propagation.integration.AdditionalEquations} interface.
 * It allows to append various additional state parameters to any {@link
 * org.orekit.propagation.AbstractPropagator abstract propagator}.
 * </p>
 * @see org.orekit.propagation.AbstractPropagator
 * @see org.orekit.propagation.integration.AdditionalEquations
 * @author Luc Maisonobe
 */
public interface AdditionalStateProvider {

    /** Get the name of the additional state.
     * @return name of the additional state
     */
    String getName();

    /** Get the additional state.
     * @param state spacecraft state to which additional state should correspond
     * @return additional state corresponding to spacecraft state
     * @exception OrekitException if additional state cannot be computed
     */
    double[] getAdditionalState(SpacecraftState state) throws OrekitException;

}
