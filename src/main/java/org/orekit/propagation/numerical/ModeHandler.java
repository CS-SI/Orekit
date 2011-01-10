/* Copyright 2010 Centre National d'Études Spatiales
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
package org.orekit.propagation.numerical;

import java.util.List;

import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;

/** Common interface for all propagator mode handlers initialization.
 * @author Luc Maisonobe
 * @version $Revision$ $Date$
 */
public interface ModeHandler {

    /** Initialize the mode handler.
     * @param  mapper mapper between spacecraft state and simple array
     * @param addStateAndEqu list of additional state and equations
     * @param reference reference date
     * @param frame reference frame
     * @param mu central body attraction coefficient
     */
    void initialize(StateMapper mapper, List <AdditionalStateAndEquations> addStateAndEqu,
                    AbsoluteDate reference, Frame frame, double mu);

}
