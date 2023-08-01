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
package org.orekit.files.ccsds.ndm.adm.acm;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.orekit.utils.units.Unit;

/** Attitude covariance set type used in CCSDS {@link Acm Attitude Comprehensive Messages}.
 * @author Luc Maisonobe
 * @since 12.0
 */
public enum AttitudeCovarianceType {

    // CHECKSTYLE: stop MultipleStringLiterals check

    /** Angles. */
    ANGLE("°²", "°²", "°²"),

    /** Angles and gyro biases. */
    ANGLE_GYROBIAS("°²", "°²", "°²", "°²/s²", "°²/s²", "°²/s²"),

    /** Angles and angular velocities. */
    ANGLE_ANGVEL("°²", "°²", "°²", "°²/s²", "°²/s²", "°²/s²"),

    /** Quaternion. */
    QUATERNION("n/a", "n/a", "n/a", "n/a"),

    /** Quaternion and gyro biases. */
    QUATERNION_GYROBIAS("n/a", "n/a", "n/a", "n/a", "°²/s²", "°²/s²", "°²/s²"),

    /** Quaternion and angular velocities. */
    QUATERNION_ANGVEL("n/a", "n/a", "n/a", "n/a", "°²/s²", "°²/s²", "°²/s²");

    // CHECKSTYLE: resume MultipleStringLiterals check

    /** Elements units. */
    private final List<Unit> units;

    /** Simple constructor.
     * @param unitsSpecifications elements units specifications
     */
    AttitudeCovarianceType(final String... unitsSpecifications) {
        this.units       = Stream.of(unitsSpecifications).
                           map(s -> Unit.parse(s)).
                           collect(Collectors.toList());
    }

    /** Get the elements units.
     * @return elements units (they correspond to diagonal elements, hence they are already squared)
     */
    public List<Unit> getUnits() {
        return units;
    }

}
