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
package org.orekit.frames;

import org.hipparchus.CalculusFieldElement;

/** Simple container class for pole correction parameters.
 * <p>This class is a simple container, it does not provide
 * any processing method.</p>
 * @param <T> type of the field elements
 * @since 9.0
 * @author Luc Maisonobe
 */
public class FieldPoleCorrection<T extends CalculusFieldElement<T>> {

    /** x<sub>p</sub> parameter (radians). */
    private final T xp;

    /** y<sub>p</sub> parameter (radians). */
    private final T yp;

    /** Simple constructor.
     * @param xp x<sub>p</sub> parameter (radians)
     * @param yp y<sub>p</sub> parameter (radians)
     */
    public FieldPoleCorrection(final T xp, final T yp) {
        this.xp = xp;
        this.yp = yp;
    }

    /** Get the x<sub>p</sub> parameter.
     * @return x<sub>p</sub> parameter
     */
    public T getXp() {
        return xp;
    }

    /** Get the y<sub>p</sub> parameter.
     * @return y<sub>p</sub> parameter
     */
    public T getYp() {
        return yp;
    }

}
