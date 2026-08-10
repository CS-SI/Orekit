/* Copyright 2022-2026 Luc Maisonobe
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
package org.orekit.files.rinex.navigation;

/** Container for ionospheric corrections.
 * @author Luc Maisonobe
 * @since 14.0
 */
public abstract class IonosphericCorrection {

    /** Ionospheric correction type. */
    private final IonosphericCorrectionType type;

    /** Time mark. */
    private final char timeMark;

    /**
     * Constructor.
     * @param type     ionospheric correction type
     * @param timeMark time mark (A: 00h-01h, B: 01h-02h…, X: 23h-24h)
     */
    protected IonosphericCorrection(final IonosphericCorrectionType type, final char timeMark) {
        this.type     = type;
        this.timeMark = timeMark;
    }

    /** Get the ionospheric correction type.
     * @return ionospheric correction type
     */
    public IonosphericCorrectionType getType() {
        return type;
    }

    /** Get the time mark.
     * @return time mark (A: 00h-01h, B: 01h-02h…, X: 23h-24h)
     */
    public char getTimeMark() {
        return timeMark;
    }

}
