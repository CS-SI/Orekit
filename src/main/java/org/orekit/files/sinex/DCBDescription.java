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

package org.orekit.files.sinex;

import org.orekit.gnss.SatelliteSystem;

/**
 * Class to store the DCB description data parsed in the SinexLoader class.
 * This class is a container for data, with its methods being only getters and setters.
 *
 * @author Louis Aucouturier
 * @since 11.2
 */
class DCBDescription {
    /** Determination mode used for the DCB computation.*/
    private String determinationMethod;

    /** Bias Mode used for the DCB computation.*/
    private String biasMode;

    /** Time System used as for the measurements, expressed as a string corresponding to
     * the satellite system it is associated to.*/
    private SatelliteSystem timeSystem;

    /** Observation Sampling parameter.*/
    private int observationSampling;

    /** Parameter spacing between the bias value.*/
    private int parameterSpacing;

    /** Simple constructor. */
    DCBDescription() {
        this.setDeterminationMethod(null);
        this.setBiasMode(null);
        this.setTimeSystem(null);
        this.setObservationSampling(0);
        this.setParameterSpacing(0);
    }

    /**
     * @return the determinationMethod
     */
    public final String getDeterminationMethod() {
        return determinationMethod;
    }

    /**
     * @return the biasMode
     */
    public final String getBiasMode() {
        return biasMode;
    }

    /**
     * @return the timeSystem
     */
    public final SatelliteSystem getTimeSystem() {
        return timeSystem;
    }

    /**
     * @return the observationSampling
     */
    public final int getObservationSampling() {
        return observationSampling;
    }

    /**
     * @return the parameterSpacing
     */
    public final int getParameterSpacing() {
        return parameterSpacing;
    }

    /**
     * @param determinationMethod the determinationMethod to set
     */
    public void setDeterminationMethod(final String determinationMethod) {
        this.determinationMethod = determinationMethod;
    }

    /**
     * @param biasMode the biasMode to set
     */
    public void setBiasMode(final String biasMode) {
        this.biasMode = biasMode;
    }

    /**
     * @param timeSystem the timeSystem to set
     */
    public void setTimeSystem(final SatelliteSystem timeSystem) {
        this.timeSystem = timeSystem;
    }

    /**
     * @param observationSampling the observationSampling to set
     */
    public void setObservationSampling(final int observationSampling) {
        this.observationSampling = observationSampling;
    }

    /**
     * @param parameterSpacing the parameterSpacing to set
     */
    public void setParameterSpacing(final int parameterSpacing) {
        this.parameterSpacing = parameterSpacing;
    }


}
