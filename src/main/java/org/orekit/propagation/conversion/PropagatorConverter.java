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
package org.orekit.propagation.conversion;

import java.util.List;

import org.orekit.errors.OrekitException;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;

/** This interface is the top-level abstraction for propagators conversions.
 * <p>
 * It provides a way to convert a given propagator or a set of {@link SpacecraftState}
 * into a wanted propagator that minimize the mean square error over a time span.
 * </p>
 * @author Pascal Parraud
 * @since 6.0
 */
public interface PropagatorConverter {

    /** Convert a propagator into another one.
     * @param source propagator to convert
     * @param timeSpan time span considered for conversion
     * @param nbPoints number of points for sampling over the time span
     * @param freeParameters names of the free parameters
     * @return adapted propagator
     * @exception OrekitException if propagator cannot be adapted
     */
    Propagator convert(Propagator source,
                       double timeSpan,
                       int nbPoints,
                       List<String> freeParameters) throws OrekitException;

    /** Convert a propagator into another one.
     * @param source propagator to convert
     * @param timeSpan time span considered for conversion
     * @param nbPoints number of points for sampling over the time span
     * @param freeParameters names of the free parameters
     * @return adapted propagator
     * @exception OrekitException if propagator cannot be adapted
     */
    Propagator convert(Propagator source,
                       double timeSpan,
                       int nbPoints,
                       String ... freeParameters) throws OrekitException;

    /** Find the propagator that minimize the mean square error for a sample of {@link SpacecraftState states}.
     * @param states spacecraft states sample to fit
     * @param positionOnly if true, consider only position data otherwise both position and velocity are used
     * @param freeParameters names of the free parameters
     * @return adapted propagator
     * @exception OrekitException if propagator cannot be adapted
     */
    Propagator convert(List<SpacecraftState> states,
                       boolean positionOnly,
                       List<String> freeParameters) throws OrekitException;

    /** Find the propagator that minimize the mean square error for a sample of {@link SpacecraftState states}.
     * @param states spacecraft states sample to fit
     * @param positionOnly if true, consider only position data otherwise both position and velocity are used
     * @param freeParameters names of the free parameters
     * @return adapted propagator
     * @exception OrekitException if propagator cannot be adapted
     */
    Propagator convert(List<SpacecraftState> states,
                       boolean positionOnly,
                       String ... freeParameters) throws OrekitException;

}
