/* Copyright 2002-2013 CS Systèmes d'Information
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
package org.orekit.files.ccsds;

/** ODM logical blocks.
 * @author sports
 * @since 6.1
 */
public enum ODMBlock {
    /** Header. */
    HEADER,
    /** Metadata. */
    METADATA,
    /** State vector data. */
    DATA_STATE_VECTOR,
    /** Mean Keplerian elements data. */
    DATA_MEAN_KEPLERIAN_ELEMENTS,
    /** Keplerian elements data. */
    DATA_KEPLERIAN_ELEMENTS,
    /** Spacecraft data. */
    DATA_SPACECRAFT,
    /** TLE Related Parameters data. */
    DATA_TLE_RELATED_PARAMETERS,
    /** Covariance data. */
    DATA_COVARIANCE,
    /** Maneuver(s) data. */
    DATA_MANEUVER,
}
