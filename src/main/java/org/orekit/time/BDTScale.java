/* Copyright 2002-2025 CS GROUP
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
package org.orekit.time;

/** Beidou system time scale.
 * <p>By convention, BDT = UTC on January 1st 2006.</p>
 * <p>This is intended to be accessed thanks to {@link TimeScales},
 * so there is no public constructor.</p>
 * @see AbsoluteDate
 */
public class BDTScale extends ConstantOffsetTimeScale {

    /** Serializable UID. */
    private static final long serialVersionUID = 20240720L;

    /** Package private constructor for the factory.
     */
    BDTScale() {
        super("BDT", new TimeOffset(-33L, 0L));
    }

}
