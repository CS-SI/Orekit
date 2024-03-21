/* Copyright 2002-2024 CS GROUP
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

/** QZSS time scale.
 * <p>By convention, TQZSS = TAI - 19 s.</p>
 * <p>The time scale is defined in <a
 * href="http://qzss.go.jp/en/technical/download/pdf/ps-is-qzss/is-qzss-pnt-003.pdf?t=1549268771755">
 * Quasi-Zenith Satellite System Navigation Service - Interface Specification for QZSS</a> version 1.6, 2014.
 * </p>
 * <p>This is intended to be accessed thanks to {@link TimeScales},
 * so there is no public constructor.</p>
 * @author Luc Maisonobe
 * @see AbsoluteDate
 */
public class QZSSScale extends ConstantOffsetTimeScale {

    /** Serializable UID. */
    private static final long serialVersionUID = 20240321L;

    /** Package private constructor for the factory.
     */
    QZSSScale() {
        super("QZSS", -19);
    }

}
