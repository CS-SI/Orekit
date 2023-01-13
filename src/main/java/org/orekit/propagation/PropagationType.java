/* Copyright 2002-2023 CS GROUP
 * Licensed to CS GROUP (CS) under one or more
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
package org.orekit.propagation;

/** Enumerate to define the propagation type used by the propagator.
 * <p>
 * This enumerate can also be used to define if the orbital state is
 * defined with osculating or mean elements at the propagator initialization.
 * </p>
 * @author Bryan Cazabonne
 */
public enum PropagationType {

    /** Mean propagation. */
    MEAN,

    /** Osculating propagation. */
    OSCULATING;

}
