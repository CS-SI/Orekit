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
package org.orekit.propagation.analytical.gnss;

import org.orekit.estimation.measurements.EstimationModifier;
import org.orekit.propagation.ClosedFormAdapter;
import org.orekit.propagation.analytical.gnss.data.GNSSClockElements;

/** Provider for clock corrections as additional states.
 * <p>
 * The value of this additional state is a three elements array containing
 * </p>
 * <ul>
 *   <li>at index 0, the polynomial satellite clock model
 *       Δtₛₐₜ = {@link GNSSClockElements#getAf0() a₀} +
 *               {@link GNSSClockElements#getAf1() a₁} (t - {@link GNSSClockElements#getToc() toc}) +
 *               {@link GNSSClockElements#getAf1() a₂} (t - {@link GNSSClockElements#getToc() toc})²
 *   </li>
 *   <li>at index 1 the relativistic clock correction due to eccentricity</li>
 *   <li>at index 2 the estimated group delay differential {@link GNSSClockElements#getTGD() TGD} for L1-L2 correction</li>
 * </ul>
 * <p>
 * Since Orekit 10.3 the relativistic clock correction can be used as an {@link EstimationModifier}
 * in orbit determination applications to take into consideration this effect
 * in measurement modeling.
 * <p>
 *
 * @author Luc Maisonobe
 * @since 9.3
 * @deprecated as of 11.1, replaced by {@link ClockCorrectionsGenerator}
 */
@Deprecated
public class ClockCorrectionsProvider extends ClosedFormAdapter {

    /** Name of the additional state for satellite clock corrections.
     * @since 9.3
     */
    public static final String CLOCK_CORRECTIONS = "";

    /** Simple constructor.
     * @param gnssClk GNSS clock elements
     */
    public ClockCorrectionsProvider(final GNSSClockElements gnssClk) {
        super(new ClockCorrectionsGenerator(gnssClk));
    }

}
