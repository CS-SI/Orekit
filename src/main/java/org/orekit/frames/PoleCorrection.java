/* Copyright 2002-2016 CS Systèmes d'Information
 * Licensed to CS Systèmes d'Information (CS) under one or more
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

/** Simple container class for pole correction parameters.
 * <p>This class is a simple container, it does not provide
 * any processing method.</p>
 * @author Luc Maisonobe
 */
public class PoleCorrection implements Serializable {

    /** Null correction (xp = 0, yp = 0). */
    public static final PoleCorrection NULL_CORRECTION =
        new PoleCorrection(0, 0);

    /** Serializable UID. */
    private static final long serialVersionUID = 8695216598525302806L;

    /** x<sub>p</sub> parameter (radians). */
    private final double xp;

    /** y<sub>p</sub> parameter (radians). */
    private final double yp;

    /** Simple constructor.
     * @param xp x<sub>p</sub> parameter (radians)
     * @param yp y<sub>p</sub> parameter (radians)
     */
    public PoleCorrection(final double xp, final double yp) {
        this.xp = xp;
        this.yp = yp;
    }

    /** Get the x<sub>p</sub> parameter.
     * @return x<sub>p</sub> parameter
     */
    public double getXp() {
        return xp;
    }

    /** Get the y<sub>p</sub> parameter.
     * @return y<sub>p</sub> parameter
     */
    public double getYp() {
        return yp;
    }

}
