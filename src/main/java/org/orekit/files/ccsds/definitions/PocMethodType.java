/* Copyright 2002-2022 CS GROUP
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
package org.orekit.files.ccsds.definitions;

import org.orekit.files.ccsds.ndm.cdm.Cdm;

/** Type of probability of collision method used in CCSDS {@link Cdm Conjunction Data Messages}.
 * <p>
 * The list of available methods is available on the SANA.
 * </p>
 * @see <a href="https://sanaregistry.org/r/cdm_cpm/">SANA CDM Collision Probability Methods</a>
 * @author Bryan Cazabonne
 * @since 11.2
 */
public enum PocMethodType {

    /** Akella and Alfriend - 2000 method.*/
    AKELLAALFRIEND_2000,

    /** Alfano 2005 method. */
    ALFANO_2005,

    /** Maximum conjunction probability method from Alfano. */
    ALFANO_MAX_PROBABILITY,

    /** Adjoining parallelepipeds method from Alfano. */
    ALFANO_PARAL_2007,

    /** Adjoining tubes method from Alfano. */
    ALFANO_TUBES_2007,

    /** Voxels method from Alfano. */
    ALFANO_VOXELS_2006,

    /** Alfriend 1999 method. */
    ALFRIEND_1999,

    /** Chan 1997 method. */
    CHAN_1997,

    /** Chan 2003 method. */
    CHAN_2003,

    /** Foster 1992 method. */
    FOSTER_1992,

    /** McKinley 2006 method. */
    MCKINLEY_2006,

    /** Patera 2001 method. */
    PATERA_2001,

    /** Patera 2003 method. */
    PATERA_2003,

    /** Patera 2005 method. */
    PATERA_2005;

}
