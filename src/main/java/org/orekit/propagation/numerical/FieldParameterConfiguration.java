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

import org.hipparchus.RealFieldElement;
import org.orekit.forces.ForceModel;


/** Simple container associating a parameter name with a step to compute its jacobian
 * and the provider that manages it.
 * @author V&eacute;ronique Pommier-Maurussane
 */
public class FieldParameterConfiguration<T extends RealFieldElement<T>> {

    /** Parameter name. */
    private String parameterName;

    /** Parameter step for finite difference computation of partial derivative with respect to that parameter. */
    private T hP;

    /** Provider handling this parameter. */
    private ForceModel provider;

    /** Parameter name and step pair constructor.
     * @param parameterName parameter name
     * @param hP parameter step */
    public FieldParameterConfiguration(final String parameterName, final T hP) {
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
    public T getHP() {
        return hP;
    }

    /** Set the povider handling this parameter.
     * @param provider provider handling this parameter
     */
    public void setProvider(final ForceModel provider) {
        this.provider = provider;
    }

    /** Get the povider handling this parameter.
     * @return provider handling this parameter
     */
    public ForceModel getProvider() {
        return provider;
    }

}
