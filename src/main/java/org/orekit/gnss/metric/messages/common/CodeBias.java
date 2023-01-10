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
package org.orekit.gnss.metric.messages.common;

/**
 * Container for code bias data.
 * @author Bryan Cazabonne
 * @since 11.0
 */
public class CodeBias {

    /** GNSS Signal and Tracking Mode Identifier. */
    private final int signalID;

    /** Code bias for the corresponding signal identifier. */
    private final double codeBias;

    /**
     * Constructor.
     * @param signalID GNSS signal and tracking mode identifier
     * @param codeBias code bias associated to the signal ID in meters
     */
    public CodeBias(final int signalID, final double codeBias) {
        // Initialize fields
        this.signalID = signalID;
        this.codeBias = codeBias;
    }

    /**
     * Get the GNSS signal and tracking mode identifier.
     * @return the GNSS signal and tracking mode identifier
     */
    public int getSignalID() {
        return signalID;
    }

    /**
     * Get the code bias associated to the signal ID.
     * @return the code bias in meters
     */
    public double getCodeBias() {
        return codeBias;
    }

}
