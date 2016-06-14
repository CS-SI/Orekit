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
package org.orekit.utils;

import org.orekit.errors.OrekitException;


/** Interface for observing parameters changes.
 * @see ParameterDriver
 * @author Luc Maisonobe
 * @since 8.0
 */
public interface ParameterObserver {

    /** Notify that a parameter value has been changed.
     * @param previousValue TODO
     * @param driver parameter driver that has been changed
     * @exception OrekitException if value is invalid for the driven model
     */
    void valueChanged(double previousValue, ParameterDriver driver) throws OrekitException;

}
