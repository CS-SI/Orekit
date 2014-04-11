/* Copyright 2002-2014 CS Systèmes d'Information
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

/** Enumerate for components to use in {@link PVCoordinates} interpolation.
 * @see PVCoordinates#interpolate(org.orekit.time.AbsoluteDate, PVASampleFilter, java.util.Collection)
 * @author Luc Maisonobe
 * @since 7.0
 */
public enum PVASampleFilter {

    /** Use only positions from sample, ignoring velocities and accelerations. */
    SAMPLE_P,

    /** Use positions and velocities from sample, ignoring accelerations. */
    SAMPLE_PV,

    /** Use positions, velocities and accelerations from sample. */
    SAMPLE_PVA;

}
