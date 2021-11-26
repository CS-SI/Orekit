/* Copyright 2002-2021 CS GROUP
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
package org.orekit.propagation.numerical.cr3bp;

import org.hipparchus.linear.RealMatrix;
import org.orekit.bodies.CR3BPSystem;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.integration.IntegrableAdapter;

/** Class calculating the state transition matrix coefficient for CR3BP Computation.
 * @see "Dynamical systems, the three-body problem, and space mission design, Koon, Lo, Marsden, Ross"
 * @author Vincent Mouraux
 * @since 10.2
 * @deprecated as of 11.1, replaced by {@link StateTransitionMatrix]
 */
@Deprecated
public class STMEquations extends IntegrableAdapter {

    /** Simple constructor.
     * @param syst CR3BP System considered
     */
    public STMEquations(final CR3BPSystem syst) {
        super(new StateTransitionMatrix(syst));
    }

    /** Method adding the standard initial values of the additional state to the initial spacecraft state.
     * @param s Initial state of the system
     * @return s Initial augmented (with the additional equations) state
     */
    public SpacecraftState setInitialPhi(final SpacecraftState s) {
        return ((StateTransitionMatrix) getGenerator()).setInitialPhi(s);
    }

    /** Method returning the State Transition Matrix.
     * @param s SpacecraftState of the system
     * @return phiM State Transition Matrix
     */
    public RealMatrix getStateTransitionMatrix(final SpacecraftState s) {
        return ((StateTransitionMatrix) getGenerator()).getStateTransitionMatrix(s);
    }

}
