/* Copyright 2024-2025 The Johns Hopkins University Applied Physics Laboratory
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
package org.orekit.files.iirv.terms;

import org.orekit.files.iirv.terms.base.StringValuedIIRVTerm;

/**
 * 1-character alphabetic character indicating originator of message.
 * <p>
 * See {@link RoutingIndicatorTerm RoutingIndicatorTerm} for the related four-character routing indicator
 * <p>
 * Valid values:
 * <ul>
 * <li> ASCII space  = GSFC
 * <li> Z            = WLP
 * <li> E            = ETR
 * <li> L            = JPL
 * <li> W            = WTR
 * <li> J            = JSC
 * <li> P            = PMR
 * <li> A            = CSTC
 * <li> K            = KMR
 * <li> C            = CNES
 * </ul>
 *
 * @author Nick LaFarge
 * @since 13.0
 */
public class OriginIdentificationTerm extends StringValuedIIRVTerm {

    /** NASA Goddard Space Flight Center (GSFC) OriginIdentification. */
    public static final OriginIdentificationTerm GSFC = new OriginIdentificationTerm(" ");

    /** Wallops Island tracking radars (WLP) OriginIdentification. */
    public static final OriginIdentificationTerm WLP = new OriginIdentificationTerm("Z");

    /** NASA/USFC Eastern Test Range (ETR) OriginIdentification. */
    public static final OriginIdentificationTerm ETR = new OriginIdentificationTerm("E");

    /** NASA Jet Propulsion Laboratory (JPL) OriginIdentification. */
    public static final OriginIdentificationTerm JPL = new OriginIdentificationTerm("L");

    /** NASA/USFC Western Test Range (WTR) OriginIdentification. */
    public static final OriginIdentificationTerm WTR = new OriginIdentificationTerm("W");

    /** NASA Johnson Space Center (JSC) OriginIdentification. */
    public static final OriginIdentificationTerm JSC = new OriginIdentificationTerm("J");

    /** Navy Pacific Missile Range (PMR) OriginIdentification. */
    public static final OriginIdentificationTerm PMR = new OriginIdentificationTerm("P");

    /** Air Force Satellite Control Facility (CSTC) OriginIdentification. */
    public static final OriginIdentificationTerm CSTC = new OriginIdentificationTerm("A");

    /** Army Kwajalein Missile Range (KMR) OriginIdentification. */
    public static final OriginIdentificationTerm KMR = new OriginIdentificationTerm("K");

    /** French Space Agency National Centre for Space Studies (CNES) OriginIdentification. */
    public static final OriginIdentificationTerm CNES = new OriginIdentificationTerm("C");

    /** The length of the origin identification term within the IIRV vector. */
    public static final int ORIGIN_IDENTIFICATION_TERM_LENGTH = 1;

    /** Regular expression that ensures the validity of string values for this term. */
    public static final String ORIGIN_IDENTIFICATION_TERM_PATTERN = "[A-Z ]";

    /**
     * Constructor.
     * <p>
     * See {@link StringValuedIIRVTerm#StringValuedIIRVTerm(String, String, int)}
     *
     * @param value value of the origin ID term
     */
    public OriginIdentificationTerm(final String value) {
        super(ORIGIN_IDENTIFICATION_TERM_PATTERN, value, ORIGIN_IDENTIFICATION_TERM_LENGTH);
    }
}
