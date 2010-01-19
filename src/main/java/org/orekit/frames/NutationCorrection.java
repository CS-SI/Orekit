/* Copyright 2002-2010 CS Communication & Systèmes
 * Licensed to CS Communication & Systèmes (CS) under one or more
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

/** Simple container class for nutation correction (IAU 1980) parameters.
 * <p>This class is a simple container, it does not provide any processing method.</p>
 * @author Pascal Parraud
 * @version $Revision$ $Date$
 */
public class NutationCorrection implements Serializable {

    /** Null correction (ddeps = 0, ddpsi = 0). */
    public static final NutationCorrection NULL_CORRECTION =
        new NutationCorrection(0, 0);

    /** Serializable UID. */
    private static final long serialVersionUID = -2075750534145826411L;

    /** &delta;&Delta;&epsilon;<sub>1980</sub> parameter (radians). */
    private final double ddeps;

    /** &delta;&Delta;&psi;<sub>1980</sub> parameter (radians). */
    private final double ddpsi;

    /** Simple constructor.
     * @param ddeps &delta;&Delta;&epsilon;<sub>1980</sub> parameter (radians)
     * @param ddpsi &delta;&Delta;&psi;<sub>1980</sub> parameter (radians)
     */
    public NutationCorrection(final double ddeps, final double ddpsi) {
        this.ddeps = ddeps;
        this.ddpsi = ddpsi;
    }

    /** Get the &delta;&Delta;&epsilon;<sub>1980</sub> parameter.
     * @return &delta;&Delta;&epsilon;<sub>1980</sub> parameter
     */
    public double getDdeps() {
        return ddeps;
    }

    /** Get the &delta;&Delta;&psi;<sub>1980</sub> parameter.
     * @return &delta;&Delta;&psi;<sub>1980</sub> parameter
     */
    public double getDdpsi() {
        return ddpsi;
    }

}
