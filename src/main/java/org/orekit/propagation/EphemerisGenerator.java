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
package org.orekit.propagation;


/** Generator for ephemerides.
 *
 * <p>
 * This interface is mainly implemented by nested classes
 * within propagators. These classes monitor the ongoing
 * propagation and stores in memory all the necessary data.
 * Once the initial propagation has completed, the data stored
 * allows them to build an {@link BoundedPropagator
 * ephemeris} that can be used to rerun the propagation (perhaps
 * with different event detectors and step handlers) without
 * doing the full computation.
 * </p>
 * <p>
 * Analytical propagators will mainly store only the start and stop date
 * and the model itself, so ephemeris will just call the model back.
 * Integration-based propagators will mainly store the {@link
 * org.orekit.propagation.sampling.OrekitStepInterpolator interpolators}
 * at each step so the ephemeris can select the proper interpolator
 * and evaluate it for any date covered by the initial propagation.
 * </p>
 * @author Luc Maisonobe
 * @since 11.0
 */
public interface EphemerisGenerator {

    /** Get the ephemeris generated during the propagation.
     * @return generated ephemeris
     */
    BoundedPropagator getGeneratedEphemeris();

}
