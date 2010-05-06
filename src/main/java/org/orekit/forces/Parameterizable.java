/* Copyright 2002-2010 CS Communication & Systèmes
 * Licensed to CS Communication & Systèmes (CS) under one or more
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
package org.orekit.forces;

import java.util.Collection;

/** This interface enables to process partial derivatives
 *  with respect to force model parameters.
 *
 * @author Pascal Parraud
 * @version $Revision$ $Date$
 */

public interface Parameterizable {

    /** Get the names of the supported parameters for partial derivatives processing.
     * @return parameters names
     */
    Collection<String> getParametersNames();

    /** Get parameter value from its name.
     * @param name parameter name
     * @return parameter value
     * @exception IllegalArgumentException if parameter is not supported
     */
    double getParameter(String name) throws IllegalArgumentException;

    /** Set the value for a given parameter.
     * @param name parameter name
     * @param value parameter value
     * @exception IllegalArgumentException if parameter is not supported
     */
    void setParameter(String name, double value) throws IllegalArgumentException;

}
