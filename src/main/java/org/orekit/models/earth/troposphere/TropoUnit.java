/* Copyright 2023 Thales Alenia Space
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
package org.orekit.models.earth.troposphere;

import org.orekit.utils.units.Unit;

/** Units used in tropospheric models.
 * @author Luc Maisonobe
 * @since 12.1
 */
public class TropoUnit {

    /** Nanometers unit. */
    public static final Unit NANO_M = Unit.parse("nm");

    /** Micrometers unit. */
    public static final Unit MICRO_M = Unit.parse("µm");

    /** HectoPascal unit. */
    public static final Unit HECTO_PASCAL = Unit.parse("hPa");

    /** Private constructor for a constants class. */
    private TropoUnit() {
        // nothing to do
    }

}
