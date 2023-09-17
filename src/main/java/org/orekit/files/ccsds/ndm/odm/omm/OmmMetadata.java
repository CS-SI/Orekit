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

package org.orekit.files.ccsds.ndm.odm.omm;

import java.util.regex.Pattern;

import org.orekit.files.ccsds.ndm.odm.OdmCommonMetadata;

/** Metadata for Orbit Mean Messages.
 * @author Luc Maisonobe
 * @since 12.0
 */
public class OmmMetadata extends OdmCommonMetadata {

    /** Constant for SGP/SGP4 mean elements theory. */
    public static final String SGP_SGP4_THEORY = "SGP/SGP4";

    /** Constant for SGP4-XP mean elements theory.
     * @since 12.0
     */
    public static final String SGP4_XP_THEORY = "SGP4-XP";

    /** Constant for DSST mean elements theory. */
    public static final String DSST_THEORY = "DSST";

    /** Pattern for SGP or SDP theory. */
    private static final Pattern SGP_SDP_PATTERN = Pattern.compile(".*S[GD]P.*");

    /** Description of the Mean Element Theory. Indicates the proper method to employ
     * to propagate the state. */
    private String meanElementTheory;

    /** Empty constructor.
     * <p>
     * This constructor is not strictly necessary, but it prevents spurious
     * javadoc warnings with JDK 18 and later.
     * </p>
     * @since 12.0
     */
    public OmmMetadata() {
        // nothing to do
    }

    /** Check if mean element theory in SGP or SDP.
     * @return true if mean element theory in SGP or SDP
     */
    public boolean theoryIsSgpSdp() {
        return SGP_SDP_PATTERN.matcher(meanElementTheory).matches();
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
    public void setMeanElementTheory(final String meanElementTheory) {
        refuseFurtherComments();
        this.meanElementTheory = meanElementTheory;
    }

}
