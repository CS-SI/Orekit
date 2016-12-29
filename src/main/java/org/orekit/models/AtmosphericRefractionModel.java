/* Copyright 2013 Applied Defense Solutions, Inc.
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
package org.orekit.models;

import java.io.Serializable;

/**
 * Defines an refraction model that can be used to correct
 * for the apparent position of an object due to atmospheric effects.
 * @author Hank Grabowski
 * @since 6.1
 */
public interface AtmosphericRefractionModel extends Serializable {

    /** Compute the refraction angle from the true (geometrical) elevation.
     * @param trueElevation true elevation (rad)
     * @return refraction angle (rad)
     */
    double getRefraction(double trueElevation);

}
