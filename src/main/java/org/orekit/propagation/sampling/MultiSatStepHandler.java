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
package org.orekit.propagation.sampling;

import java.util.List;

import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;

/** This interface is a space-dynamics aware step handler for {@link
 * org.orekit.propagation.PropagatorsParallelizer multi-sat propagation}.
 *
 * <p>It is a multi-satellite version of the {@link OrekitStepHandler}.</p>
 * @author Luc Maisonobe
 * @since 9.0
 */
public interface MultiSatStepHandler {

    /** Initialize step handler at the start of a propagation.
     * <p>
     * This method is called once at the start of the propagation. It
     * may be used by the step handler to initialize some internal data
     * if needed.
     * </p>
     * <p>
     * The default method does nothing
     * </p>
     * @param states0 initial states, one for each satellite in the same order
     * used to {@link org.orekit.propagation.PropagatorsParallelizer#PropagatorsParallelizer(List, MultiSatStepHandler)
     * build} the {@link org.orekit.propagation.PropagatorsParallelizer multi-sat propagator}.
     * @param t target time for the integration
     */
    default void init(final List<SpacecraftState> states0, final AbsoluteDate t) {
        // nothing by default
    }

    /** Handle the current step.
     * <p>
     * When called by {@link org.orekit.propagation.PropagatorsParallelizer PropagatorsParallelizer},
     * all interpolators have the same time range.
     * </p>
     * @param interpolators interpolators set up for the current step in the same order
     * used to {@link org.orekit.propagation.PropagatorsParallelizer#PropagatorsParallelizer(List, MultiSatStepHandler)
     * build} the {@link org.orekit.propagation.PropagatorsParallelizer multi-sat propagator}
     */
    void handleStep(List<OrekitStepInterpolator> interpolators);

    /**
     * Finalize propagation.
     * @param finalStates states at propagation end
     * @since 11.0
     */
    default void finish(final List<SpacecraftState> finalStates) {
        // nothing by default
    }

}
