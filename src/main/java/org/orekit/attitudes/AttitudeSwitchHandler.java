/* Copyright 2022-2024 Romain Serra
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
package org.orekit.attitudes;

import org.orekit.propagation.SpacecraftState;

/** Interface for attitude switch notifications.
 * <p>
 * This interface is intended to be implemented by users who want to be
 * notified when an attitude switch occurs.
 * </p>
 * @since 13.0
 * @see AbstractSwitchingAttitudeProvider
 */
@FunctionalInterface
public interface AttitudeSwitchHandler {

    /** Method called when attitude is switched from one law to another law.
     * @param preceding attitude law used preceding the switch (i.e. in the past
     * of the switch event for a forward propagation, or in the future
     * of the switch event for a backward propagation)
     * @param following attitude law used following the switch (i.e. in the future
     * of the switch event for a forward propagation, or in the past
     * of the switch event for a backward propagation)
     * @param state state at switch time (with attitude computed using the past law)
     */
    void switchOccurred(AttitudeProvider preceding, AttitudeProvider following, SpacecraftState state);
}
