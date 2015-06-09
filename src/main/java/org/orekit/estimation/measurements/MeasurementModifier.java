/* Copyright 2002-2015 CS Systèmes d'Information
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

import java.util.List;
import java.util.Map;

import org.orekit.errors.OrekitException;
import org.orekit.estimation.Parameter;
import org.orekit.propagation.SpacecraftState;


/** Interface for measurements modifiers used for orbit determination.
 * <p>
 * Modifiers are used to take some physical corrections into account in
 * the theoretical {@link Measurement measurement} model. They can be
 * used to model for example:
 * <ul>
 *   <li>on board delays</li>
 *   <li>ground delays</li>
 *   <li>antennas mount and center of phase offsets</li>
 *   <li>tropospheric effects</li>
 *   <li>clock drifts</li>
 *   <li>ground station displacements due to tidal effects</li>
 *   <li>...</li>
 * </ul>
 * </p>
 * @author Luc Maisonobe
 * @since 7.1
 */
public interface MeasurementModifier {

    /** Get the parameters used by this modifier.
     * @return parameters used by this modifier
     */
    List<Parameter> getParameters();

    /** Check if a parameter is used by this modifier.
     * @param parameter parameter
     * @return true if parameter is used by this modifier
     */
    boolean isSupported(Parameter parameter);

    /** Apply a modifier to a simulated measurement.
     * @param state orbital state at measurement date
     * @param parameters model parameters map (the map keys are the
     * parameters names)
     * @param baseValue measurement value before the modifier is applied
     * (sum of theoretical value and previous modifiers already applied)
     * @param measurement measurement to which the modifier applies
     * @return simulated measurement value with modifier applied
     * @exception OrekitException if modifier cannot be applied
     */
    double[] apply(SpacecraftState state, Map<String, Parameter> parameters,
                   double[] baseValue, Measurement measurement)
        throws OrekitException;

}
