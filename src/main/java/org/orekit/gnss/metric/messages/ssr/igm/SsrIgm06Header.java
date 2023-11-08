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

/**
 * Container for SSR IGM06 header.
 * @author Bryan Cazabonne
 * @since 11.0
 */
public class SsrIgm06Header extends SsrIgmHeader {

    /** Flag indicating if phase biases maintain consistency between non-dispersive and all original dispersive phase signals. */
    private boolean isConsistencyMaintained;

    /** Flag indicating if consistency between code and phase biases is maintained for the MW combinations. */
    private boolean isMelbourneWubbenaConsistencyMaintained;

    /** Constructor. */
    public SsrIgm06Header() {
        // Nothing to do ...
    }

    /**
     * Get the flag indicating if phase biases maintain consistency
     * between non-dispersive and all original dispersive phase signals.
     * @return true if consistency is maintained
     */
    public boolean isConsistencyMaintained() {
        return isConsistencyMaintained;
    }

    /**
     * Get the flag indicating if consistency between
     * code and phase biases is maintained for the MW combinations.
     * @return true if phase biases are consistent for MW combinations
     */
    public boolean isMelbourneWubbenaConsistencyMaintained() {
        return isMelbourneWubbenaConsistencyMaintained;
    }

    /**
     * Set the flag indicating if phase biases maintain consistency
     * between non-dispersive and all original dispersive phase signals.
     * @param isConsistencyMaintained the flag to set
     */
    public void setIsConsistencyMaintained(final boolean isConsistencyMaintained) {
        this.isConsistencyMaintained = isConsistencyMaintained;
    }

    /**
     * Set the flag indicating if consistency between
     * code and phase biases is maintained for the MW combinations.
     * @param isMelbourneWubbenaConsistencyMaintained the flag to set
     */
    public void setIsMelbourneWubbenaConsistencyMaintained(final boolean isMelbourneWubbenaConsistencyMaintained) {
        this.isMelbourneWubbenaConsistencyMaintained = isMelbourneWubbenaConsistencyMaintained;
    }

}
