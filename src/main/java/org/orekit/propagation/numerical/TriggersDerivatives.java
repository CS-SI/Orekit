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
package org.orekit.propagation.numerical;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.orekit.forces.ForceModel;
import org.orekit.forces.maneuvers.Maneuver;
import org.orekit.forces.maneuvers.trigger.AbstractManeuverTriggers;
import org.orekit.forces.maneuvers.trigger.ManeuverTriggers;
import org.orekit.forces.maneuvers.trigger.ManeuverTriggersResetter;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.ParameterDrivenDateIntervalDetector;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.DateDriver;

/** Helper for computing the partial derivatives with respect to maneuvers triggers dates.
 * @author Luc Maisonobe
 * @since 11.1
 */
class TriggersDerivatives {

    /** Name of the main partial derivatives state. */
    private final String pdeName;

    /** Parameters corresponding to maneuvers triggers dates. */
    private Map<String, ManeuverTriggersResetter> triggers;

    /** Simple constructor.
     * @param pdeName name of the main partial derivatives state
     * @param map parameters to columns numbers map
     * @param forceModels force models used by propagation
     */
    TriggersDerivatives(final String pdeName, final Map<String, Integer> map, final List<ForceModel> forceModels) {

        this.pdeName = pdeName;

        this.triggers = new HashMap<>();

        // single-out the very specific drivers used by maneuvers trigger dates
        for (final ForceModel forceModel : forceModels) {
            if (forceModel instanceof Maneuver) {
                final ManeuverTriggers maneuverTriggers = ((Maneuver) forceModel).getManeuverTriggers();
                if (maneuverTriggers instanceof AbstractManeuverTriggers) {

                    // FIXME: when issue https://gitlab.orekit.org/orekit/orekit/-/issues/854 is solved
                    // the previous if statement and the following cast should be removed as the following
                    // code should really be done for all ManeuverTriggers and not only AbstractManeuverTriggers
                    final AbstractManeuverTriggers amt = (AbstractManeuverTriggers) maneuverTriggers;

                    amt.getEventsDetectors().
                        filter(d -> d instanceof ParameterDrivenDateIntervalDetector).
                        map (d -> (ParameterDrivenDateIntervalDetector) d).
                        forEach(d -> {
                            manageDateDriver(amt, d.getStartDriver(), map);
                            manageDateDriver(amt, d.getStopDriver(),  map);
                        });

                }
            }
        }

    }

    /** Manage a date driver.
     * @param amt trigger to which the driver is bound
     * @param driver date driver
     * @param map parameters to columns numbers map
     */
    private void manageDateDriver(final AbstractManeuverTriggers amt, final DateDriver driver, final Map<String, Integer> map) {
        if (driver.isSelected()) {
            final TriggersResetter resetter = new TriggersResetter(map.get(driver.getName()));
            amt.addResetter(resetter);
            triggers.put(driver.getName(), resetter);
        }
    }

    /** Check if a parameter is a trigger date managed by this helper.
     * @param name parameter name to check
     * @return true if the parameter is managed by this helper
     */
    public boolean isTriggerParameter(final String name) {
        return triggers.containsKey(name);
    }

    /** Compute partial derivatives with respect to a trigger parameter.
     * @param name name of the trigger parameter
     * @param state spacecraft state at current date
     * @return partial derivatives with respect to trigger date
     */
    public SpacecraftState updateDerivatives(final String name, final SpacecraftState state) {
        // TODO
        return state;
    }

    /** Resetter for maneuvers triggers.
     * <p>
     * Let ∂yₜ/∂y₀ be the State Transition matrix from initial time t₀ to current time t.
     * We have ∂yₜ/∂y₀ = ∂yₜ/∂y₁ ⨉ ∂y₁/∂y₀ where y₁ is that state at trigger time t₁.
     * The effect of an offset of the trigger date t₁ is dyₜ/dt₁ = ±∂yₜ/∂y₁ dy/dt(t₁)
     * where dy/dt(t₁) is the maneuver effect (acceleration) at time t₁ and the
     * sign is minus for start time and plus for stop time.
     * As the variational equations give ∂yₜ/∂y₀ only (i.e. STM relative to propagation start),
     * we need to solve ∂yₜ/∂y₀ = ∂yₜ/∂y₁ ⨉ ∂y₁/∂y₀ in order to retrieve ∂yₜ/∂y₁.
     * Transposing this equation, we get a classical linear problem A X = B with
     * A = transpose(∂y₁/∂y₀), B = transpose(∂yₜ/∂y₀), X = transpose(∂yₜ/∂y₁)
     * </p>
     */
    private class TriggersResetter implements ManeuverTriggersResetter {

        /** Index of the column in the Jacobian with respect to parameters. */
        private final int column;

        /** Propagation direction. */
        private boolean forward;

        /** Sign to apply to acceleration. */
        private double sign;

        /** Simple constructor.
         * @param column index of the column in the Jacobian with respect to parameters
         */
        TriggersResetter(final int column) {
            this.column = column;
        }

        /** {@inheritDoc}*/
        @Override
        public void init(final SpacecraftState initialState, final AbsoluteDate target) {
            forward = target.isAfterOrEqualTo(initialState);
        }

        /** {@inheritDoc}*/
        @Override
        public void maneuverTriggered(final SpacecraftState state, final boolean start) {
            sign = start ? -1 : +1;
        }

        /** {@inheritDoc}*/
        @Override
        public SpacecraftState resetState(final SpacecraftState state) {

            // get the partial derivatives state
            final double[] p = state.getAdditionalState(pdeName);
            return state;
        }

    }

}

