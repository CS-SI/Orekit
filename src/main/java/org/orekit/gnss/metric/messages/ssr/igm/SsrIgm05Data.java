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
package org.orekit.gnss.metric.messages.ssr.igm;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.orekit.gnss.metric.messages.common.CodeBias;

/**
 * Container for SSR IGM05 data.
 * @author Bryan Cazabonne
 * @since 11.0
 */
public class SsrIgm05Data extends SsrIgmData {

    /** Number of biases processed for the current satellite. */
    private int numberOfBiasesProcessed;

    /** Map of code biases.
     * First key: the signal ID
     * Second key: the code bias object
     */
    private Map<Integer, CodeBias> biases;

    /** Constructor. */
    public SsrIgm05Data() {
        // Initialize an empty map
        this.biases = new HashMap<>();
    }

    /**
     * Get the number of biases processed for the current satellite.
     * @return the number of biases processed
     */
    public int getNumberOfBiasesProcessed() {
        return numberOfBiasesProcessed;
    }

    /**
     * Set the number of biases processed for the current satellite.
     * @param numberOfBiasesProcessed the number to set
     */
    public void setNumberOfBiasesProcessed(final int numberOfBiasesProcessed) {
        this.numberOfBiasesProcessed = numberOfBiasesProcessed;
    }

    /**
     * Add a code bias value for the current satellite.
     * @param bias the code bias to add
     */
    public void addCodeBias(final CodeBias bias) {
        this.biases.put(bias.getSignalID(), bias);
    }

    /**
     * Get the code biases for the current satellite.
     * <p>
     * First key: signal ID
     * Second key: the code bias object
     * </p>
     * @return the code biases for the current satellite
     */
    public Map<Integer, CodeBias> getCodeBiases() {
        return Collections.unmodifiableMap(biases);
    }

    /**
     * Get the code bias for a given signal ID.
     * @param signalID the signal IF
     * @return the corresponding code bias (null if not provided)
     */
    public CodeBias getCodeBias(final int signalID) {
        return biases.get(signalID);
    }

}
