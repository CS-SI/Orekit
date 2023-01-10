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
package org.orekit.gnss.metric.ntrip;

import java.util.HashMap;
import java.util.Map;

import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;

/** Enumerate for carrier phase in {@link DataStreamRecord}.
 * @author Luc Maisonobe
 * @since 11.0
 */
public enum CarrierPhase {

    /** No. */
    NO(0),

    /** Yes, L1. */
    L1(1),

    /** Yes, L1&amp;L2. */
    L1_L2(2);

    /** code map. */
    private static final Map<Integer, CarrierPhase> CODES_MAP = new HashMap<Integer, CarrierPhase>();
    static {
        for (final CarrierPhase type : values()) {
            CODES_MAP.put(type.getCode(), type);
        }
    }

    /** Code. */
    private final int code;

    /** Simple constructor.
     * @param code code in the sourcetable records
     */
    CarrierPhase(final int code) {
        this.code = code;
    }

    /** Get code.
     * @return code
     */
    private int getCode() {
        return code;
    }

    /** Get the carrier phase corresponding to a code.
     * @param code carrier phase code
     * @return the carrier phase corresponding to the code
     */
    public static CarrierPhase getCarrierPhase(final String code) {
        CarrierPhase carrierPhase = null;
        try {
            carrierPhase = CODES_MAP.get(Integer.parseInt(code));
        } catch (NumberFormatException nfe) {
            // error will be handled by the if below
        }
        if (carrierPhase == null) {
            throw new OrekitException(OrekitMessages.UNKNOWN_CARRIER_PHASE_CODE, code);
        }
        return carrierPhase;
    }

}
