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

package org.orekit.files.sinex;

import org.orekit.gnss.TimeSystem;

/**
 * Class to store the DCB description parameters.
 * <p>
 * This class gives important parameters from the analysis and defines the fields in the block ’BIAS/SOLUTION’
 * of the loaded Sinex file.
 * </p>
 * @author Louis Aucouturier
 * @since 12.0
 */
public class DcbDescription {

    /** Determination mode used to generate the bias results. */
    private String determinationMethod;

    /** Describes how the included GNSS bias values have to be interpreted and applied. */
    private String biasMode;

    /** Time system. */
    private TimeSystem timeSystem;

    /** Observation sampling interval used for data analysis (s). */
    private int observationSampling;

    /** Parameter spacing interval between the bias value (s). */
    private int parameterSpacing;

    /** Simple constructor. */
    public DcbDescription() {
        this.determinationMethod = "";
        this.observationSampling = -1;
        this.parameterSpacing    = -1;
    }

    /**
     * Get the determination mode used to generate the bias results.
     * <p>
     * This value is optional. If the value is not present in the file, the method returns an empty string.
     * </p>
     * @return the determination mode used to generate the bias results.
     */
    public final String getDeterminationMethod() {
        return determinationMethod;
    }

    /**
     * Get the bias mode
     * <p>
     * The bias mode describes how the included GNSS bias values have to be interpreted and applied.
     * </p>
     * @return the bias mode
     */
    public final String getBiasMode() {
        return biasMode;
    }

    /**
     * Get the time system for DCB data.
     *
     * @return the time system
     */
    public final TimeSystem getTimeSystem() {
        return timeSystem;
    }

    /**
     * Get the observation sampling interval used for data analysis.
     * <p>
     * This value is optional. If the value is not present in the file, the method returns -1.
     * </p>
     * @return the observation sampling interval used for data analysis in seconds
     */
    public final int getObservationSampling() {
        return observationSampling;
    }

    /**
     * Get the parameter spacing interval between the bias value.
     * <p>
     * This value is optional. If the value is not present in the file, the method returns -1.
     * </p>
     * @return the pParameter spacing interval between the bias value in seconds
     */
    public final int getParameterSpacing() {
        return parameterSpacing;
    }

    /**
     * Set the determination mode used to generate the bias results.
     *
     * @param determinationMethod the determination method to set
     */
    public void setDeterminationMethod(final String determinationMethod) {
        this.determinationMethod = determinationMethod;
    }

    /**
     * Set the bias mode.
     *
     * @param biasMode the bias mode to set
     */
    public void setBiasMode(final String biasMode) {
        this.biasMode = biasMode;
    }

    /**
     * Set the time system used for DCB data.
     *
     * @param timeSystem the time system to set
     */
    public void setTimeSystem(final TimeSystem timeSystem) {
        this.timeSystem = timeSystem;
    }

    /**
     * Set the observation sampling interval used for data analysis.
     *
     * @param observationSampling the observation sampling to set in seconds
     */
    public void setObservationSampling(final int observationSampling) {
        this.observationSampling = observationSampling;
    }

    /**
     * Set the parameter spacing interval between the bias value.
     *
     * @param parameterSpacing the parameter spacing to set in seconds
     */
    public void setParameterSpacing(final int parameterSpacing) {
        this.parameterSpacing = parameterSpacing;
    }

}
