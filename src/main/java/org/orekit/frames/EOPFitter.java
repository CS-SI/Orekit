/* Copyright 2023 Luc Maisonobe
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
package org.orekit.frames;

import java.io.Serializable;

/** Earth Orientation Parameters fitter for {@link PredictedEOPHistory EOP prediction}.
 * @see PredictedEOPHistory
 * @see SingleParameterFitter
 * @since 12.0
 * @author Luc Maisonobe
 */
public class EOPFitter implements Serializable {

    /** Serializable UID. */
    private static final long serialVersionUID = 20230309L;

    /** Fitter for dut1 and LOD. */
    private final SingleParameterFitter dut1Fitter;

    /** Fitter for pole x component. */
    private final SingleParameterFitter xPFitter;

    /** Fitter for pole y component. */
    private final SingleParameterFitter yPFitter;

    /** Fitter for nutation x component. */
    private final SingleParameterFitter dxFitter;

    /** Fitter for nutation y component. */
    private final SingleParameterFitter dyFitter;

    /** Simple constructor.
     * @param dut1Fitter fitter for dut1 and LOD
     * @param xPFitter fitter for pole x component
     * @param yPFitter fitter for pole y component
     * @param dxFitter fitter for nutation x component
     * @param dyFitter fitter for nutation y component
     */
    public EOPFitter(final SingleParameterFitter dut1Fitter,
                     final SingleParameterFitter xPFitter, final SingleParameterFitter yPFitter,
                     final SingleParameterFitter dxFitter, final SingleParameterFitter dyFitter) {
        this.dut1Fitter = dut1Fitter;
        this.xPFitter   = xPFitter;
        this.yPFitter   = yPFitter;
        this.dxFitter   = dxFitter;
        this.dyFitter   = dyFitter;
    }

    /** Fit raw history.
     * @param rawHistory raw EOP history to fit.
     * @return fitted model
     */
    public EOPFittedModel fit(final EOPHistory rawHistory) {
        return new EOPFittedModel(dut1Fitter.fit(rawHistory, entry -> entry.getUT1MinusUTC()),
                                  xPFitter.fit(rawHistory, entry -> entry.getX()),
                                  yPFitter.fit(rawHistory, entry -> entry.getY()),
                                  dxFitter.fit(rawHistory, entry -> entry.getDx()),
                                  dyFitter.fit(rawHistory, entry -> entry.getDy()));
    }

}
