/* Copyright 2002-2019 CS Systèmes d'Information
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
package org.orekit.estimation.measurements;

/** Class modeling a satellite that can be observed.
 *
 * @author Luc Maisonobe
 * @since 9.3
 */
public class ObservableSatellite {

    /** Index of the propagator related to this satellite. */
    private final int propagatorIndex;

    /** Simple constructor.
     * @param propagatorIndex index of the propagator related to this satellite
     */
    public ObservableSatellite(final int propagatorIndex) {
        this.propagatorIndex = propagatorIndex;
    }

    /** Get the index of the propagator related to this satellite.
     * @return index of the propagator related to this satellite
     */
    public int getPropagatorIndex() {
        return propagatorIndex;
    }

}
