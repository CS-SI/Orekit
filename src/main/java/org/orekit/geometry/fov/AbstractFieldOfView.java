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
package org.orekit.geometry.fov;

/** Abstract class representing a spacecraft sensor Field Of View.
 * @author Luc Maisonobe
 * @since 10.1
 */
public abstract class AbstractFieldOfView implements FieldOfView {

    /** Margin to apply to the zone. */
    private final double margin;

    /** Build a new instance.
     * @param margin angular margin to apply to the zone (if positive,
     * points outside of the raw FoV but close enough to the boundary are
     * considered visible; if negative, points inside of the raw FoV
     * but close enough to the boundary are considered not visible)
     */
    protected AbstractFieldOfView(final double margin) {
        this.margin = margin;
    }

    /** {@inheritDoc} */
    @Override
    public double getMargin() {
        return margin;
    }

}
