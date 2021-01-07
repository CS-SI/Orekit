/* Copyright 2002-2020 CS GROUP
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

import org.orekit.data.DataContext;
import org.orekit.files.ccsds.ndm.odm.OCommonMetadata;
import org.orekit.utils.IERSConventions;

public class OMMMetadata extends OCommonMetadata {

    /** Description of the Mean Element Theory. Indicates the proper method to employ
     * to propagate the state. */
    private String meanElementTheory;

    /** Create a new meta-data.
     * @param conventions IERS conventions to use
     * @param dataContext data context to use
     */
    public OMMMetadata(final IERSConventions conventions, final DataContext dataContext) {
        super(conventions, dataContext);
    }

    /** Get the description of the Mean Element Theory.
     * @return the mean element theory
     */
    public String getMeanElementTheory() {
        return meanElementTheory;
    }

    /** Set the description of the Mean Element Theory.
     * @param meanElementTheory the mean element theory to be set
     */
    void setMeanElementTheory(final String meanElementTheory) {
        this.meanElementTheory = meanElementTheory;
    }

}
