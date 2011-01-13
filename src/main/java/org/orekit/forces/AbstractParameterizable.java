/* Copyright 2010-2011 Centre National d'Études Spatiales
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

import java.util.ArrayList;
import java.util.Collection;

import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;

/** Simple abstract class providing boilerplate parameters list.
 *
 * @author Luc Maisonobe
 * @version $Revision$ $Date$
 */

public abstract class AbstractParameterizable implements Parameterizable {

    /** Serializable UID. */
    private static final long serialVersionUID = 3408804493769459294L;

   /** List of the parameters names. */
    private final Collection<String> parametersNames;

    /** Simple constructor.
     * @param names names of the supported parameters
     */
    protected AbstractParameterizable(final String ... names) {
        parametersNames = new ArrayList<String>();
        for (final String name : names) {
            parametersNames.add(name);
        }
    }

    /** Simple constructor.
     * @param names names of the supported parameters
     */
    protected AbstractParameterizable(final Collection<String> names) {
        parametersNames = new ArrayList<String>();
        parametersNames.addAll(names);
    }

    /** {@inheritDoc} */
    public Collection<String> getParametersNames() {
        return parametersNames;
    }

    /** {@inheritDoc} */
    public boolean isSupported(final String name) {
        for (final String supportedName : parametersNames) {
            if (supportedName.equals(name)) {
                return true;
            }
        }
        return false;
    }

    /** Check if a parameter is supported and throw an IllegalArgumentException if not.
     * @param name name of the parameter to check
     * @exception IllegalArgumentException if the parameter is not supported
     * @see #isSupported(String)
     */
    public void complainIfNotSupported(final String name) throws IllegalArgumentException {
        if (!isSupported(name)) {
            throw OrekitException.createIllegalArgumentException(OrekitMessages.UNKNOWN_PARAMETER, name);
        }
    }

    /** {@inheritDoc} */
    public abstract double getParameter(String name) throws IllegalArgumentException;

    /** {@inheritDoc} */
    public abstract void setParameter(String name, double value) throws IllegalArgumentException;

}
