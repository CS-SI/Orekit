/* Copyright 2002-2021 CS GROUP
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
package org.orekit.files.ccsds.ndm.odm.ocm;

import org.orekit.utils.units.PredefinedUnit;
import org.orekit.utils.units.Unit;

/** CCSDS/SANA units for {@link ElementsType}.
 * @author Luc Maisonobe
 * @since 11.0
 */
enum ElementsUnit {

    /** Kilometers. */
    KM(Unit.parse("km")),

    /** Kilometers per seconds. */
    KM_S(Unit.parse("km/s")),

    /** Kilometers per squared seconds. */
    KM_S2(Unit.parse("km/s²")),

    /** Square kilometers. */
    KM2(Unit.parse("km²")),

    /** Square kilometers per second. */
    KM2_S(Unit.parse("km²/s")),

    /** Kilometers square roots. */
    SQKM(Unit.parse("√km")),

    /** Kilometers per seconds square roots. */
    KM_SQS(Unit.parse("km/√s")),

    /** Degrees. */
    DEG(Unit.parse("°")),

    /** Radians. */
    RAD(Unit.parse("rad")),

    /** Dimensionless values. */
    ND(PredefinedUnit.ONE.toUnit().alias("nd"));

    /** General unit. */
    private final Unit unit;

    /** Simple constructor.
     * @param unit general unit
     */
    ElementsUnit(final Unit unit) {
        this.unit = unit;
    }

    /** Get as a {@link Unit}.
     * @return unit
     */
    public Unit toUnit() {
        return unit;
    }

}
