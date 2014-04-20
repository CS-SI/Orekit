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

/** Enumerate for components to use in {@link AngularCoordinates} interpolation.
 * @see AngularCoordinates#interpolate(org.orekit.time.AbsoluteDate, RRASampleFilter, java.util.Collection)
 * @author Luc Maisonobe
 * @since 7.0
 */
public enum RRASampleFilter {

    /** Use only rotations from sample, ignoring rotation rates and rotation accelerations. */
    SAMPLE_R,

    /** Use rotations and rotation rates from sample, ignoring rotation accelerations. */
    SAMPLE_RR,

    /** Use rotations, rotation rates and rotation accelerations from sample. */
    SAMPLE_RRA;

}
