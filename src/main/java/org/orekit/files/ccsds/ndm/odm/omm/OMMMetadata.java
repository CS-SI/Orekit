/* Copyright 2002-2021 CS GROUP
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

package org.orekit.files.ccsds.ndm.odm.omm;

import org.orekit.files.ccsds.ndm.odm.OCommonMetadata;

public class OMMMetadata extends OCommonMetadata {

    /** Constant for SGP/SGP4 mean elements theory. */
    public static final String SGP_SGP4_THEORY = "SGP/SGP4";

    /** Constant for DSST mean elements theory. */
    public static final String DSST_THEORY = "DSST";

    /** Description of the Mean Element Theory. Indicates the proper method to employ
     * to propagate the state. */
    private String meanElementTheory;

    /** Get the description of the Mean Element Theory.
     * @return the mean element theory
     */
    public String getMeanElementTheory() {
        return meanElementTheory;
    }

    /** Set the description of the Mean Element Theory.
     * @param meanElementTheory the mean element theory to be set
     */
    public void setMeanElementTheory(final String meanElementTheory) {
        refuseFurtherComments();
        this.meanElementTheory = meanElementTheory;
    }

}
