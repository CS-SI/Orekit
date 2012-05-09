/* Copyright 2010-2011 Centre National d'Études Spatiales
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
package org.orekit.propagation.numerical;

import java.io.Serializable;

/** Simple container associating a parameter name with a step to compute its jacobian
 * and the provider thant manages it.
 * @author V&eacute;ronique Pommier-Maurussane
 */
class ParameterConfiguration implements Serializable {

    /** Serializable UID. */
    private static final long serialVersionUID = 2247518849090889379L;

    /** Parameter name. */
    private String parameterName;

    /** Parameter step for finite difference computation of partial derivative with respect to that parameter. */
    private double hP;

    /** Provider handling this parameter. */
    private AccelerationJacobiansProvider provider;

    /** Parameter name and step pair constructor.
     * @param parameterName parameter name
     * @param hP parameter step */
    public ParameterConfiguration(final String parameterName, final double hP) {
        this.parameterName = parameterName;
        this.hP = hP;
        this.provider = null;
    }

    /** Get parameter name.
     * @return parameterName parameter name
     */
    public String getParameterName() {
        return parameterName;
    }

    /** Get parameter step.
     * @return hP parameter step
     */
    public double getHP() {
        return hP;
    }

    /** Set the povider handling this parameter.
     * @param provider provider handling this parameter
     */
    public void setProvider(final AccelerationJacobiansProvider provider) {
        this.provider = provider;
    }

    /** Get the povider handling this parameter.
     * @return provider handling this parameter
     */
    public AccelerationJacobiansProvider getProvider() {
        return provider;
    }

}
