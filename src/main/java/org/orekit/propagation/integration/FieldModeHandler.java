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
package org.orekit.propagation.integration;

import org.hipparchus.RealFieldElement;
import org.orekit.time.FieldAbsoluteDate;

/** Common interface for all propagator mode handlers initialization.
 * @author Luc Maisonobe
 */
public interface FieldModeHandler<T extends RealFieldElement<T>> {

    /**
     * Initialize the mode handler.
     *
     * @param activateHandlers if handlers shall be active
     * @param targetDate       propagation is expected to end on this date, but
     *                         it may end early due to event detectors or
     *                         exceptions.
     */
    void initialize(boolean activateHandlers, FieldAbsoluteDate<T> targetDate);

}
