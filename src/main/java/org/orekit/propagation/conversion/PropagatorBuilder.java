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
import org.orekit.errors.OrekitIllegalArgumentException;
import org.orekit.frames.Frame;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.Propagator;
import org.orekit.time.AbsoluteDate;

/** This interface is the top-level abstraction to build propagators for conversion.
 * @author Pascal Parraud
 * @since 6.0
 */
public interface PropagatorBuilder {

    /** Build a propagator.
     * @param date date associated to the parameters to configure the initial state
     * @param parameters set of position/velocity(/free) parameters to configure the propagator
     * @return an initialized propagator
     * @exception OrekitException if propagator cannot be build
     */
    Propagator buildPropagator(final AbsoluteDate date, final double[] parameters)
        throws OrekitException;

    /** Get the orbit type expected for the 6 first parameters in
     * {@link #buildPropagator(AbsoluteDate, double[])}.
     * @return orbit type to use in {@link #buildPropagator(AbsoluteDate, double[])}
     * @see #buildPropagator(AbsoluteDate, double[])
     * @see #getPositionAngle()
     * @since 7.1
     */
    OrbitType getOrbitType();

    /** Get the position angle type expected for the 6 first parameters in
     * {@link #buildPropagator(AbsoluteDate, double[])}.
     * @return position angle type to use in {@link #buildPropagator(AbsoluteDate, double[])}
     * @see #buildPropagator(AbsoluteDate, double[])
     * @see #getOrbitType()
     * @since 7.1
     */
    PositionAngle getPositionAngle();

    /** Get the frame in which the orbit is propagated.
     * @return frame in which the orbit is propagated
     */
    Frame getFrame();

    /** Set the free parameters in order to build the propagator.
     * <p>
     * The parameters must belong to the list returned by {@link #getSupportedParameters()}
     * </p>
     * @param parameters free parameters to set when building the propagator
     * @exception OrekitIllegalArgumentException if one of the parameters is not supported
     */
    void setFreeParameters(List<String> parameters)
        throws OrekitIllegalArgumentException;

    /** Get the names of the supported parameters.
     * @return parameters names
     * @since 7.1
     */
    List<String> getSupportedParameters();

    /** Get the free parameters used to build the propagator.
     * @return free parameters used when building the propagator
     * @since 7.1
     */
    List<String> getFreeParameters();

    /** Get parameter value from its name.
     * @param name parameter name
     * @return parameter value
     * @exception OrekitIllegalArgumentException if parameter is not supported
     */
    double getParameter(String name) throws OrekitIllegalArgumentException;

    /** Set parameter value from its name.
     * @param name parameter name
     * @param value parameter value
     * @exception OrekitIllegalArgumentException if parameter is not supported
     */
    void setParameter(String name, double value) throws OrekitIllegalArgumentException;

}
