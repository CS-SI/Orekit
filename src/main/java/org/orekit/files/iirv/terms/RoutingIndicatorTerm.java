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
 * 4-character destination routing indicator that specifies the site for which the message was generated.
 * <p>
 * See {@link OriginIdentificationTerm OriginIdentificationTerm} for the related alphabetic character
 * <p>
 * Valid values:
 * <ul>
 * <li>GSFC    = NASA Goddard Space Flight Center
 * <li>WLP     = Wallops Island tracking radars
 * <li>ETR     = NASA/USFC Eastern Test Range
 * <li>JPL     = NASA Jet Propulsion Laboratory
 * <li>WTR     = NASA/USFC Western Test Range
 * <li>JSC     = NASA Johnson Space Center
 * <li>PMR     = Navy Pacific Missile Range
 * <li>CSTC    = Air Force Satellite Control Facility
 * <li>KMR     = Army Kwajalein Missile Range
 * <li>CNES    = French Space Agency National Centre for Space Studies (CNES)
 * <li>MANY    = Message originated from more than one of the above stations
 * </ul>
 *
 * @author Nick LaFarge
 * @since 13.0
 */
public class RoutingIndicatorTerm extends StringValuedIIRVTerm {

    /** NASA Goddard Space Flight Center (GSFC) RoutingIndicator. */
    public static final RoutingIndicatorTerm GSFC = new RoutingIndicatorTerm("GSFC");

    /** Wallops Island tracking radars (WLP) RoutingIndicator. */
    public static final RoutingIndicatorTerm WLP = new RoutingIndicatorTerm(" WLP");

    /** NASA/USFC Eastern Test Range (ETR) RoutingIndicator. */
    public static final RoutingIndicatorTerm ETR = new RoutingIndicatorTerm(" ETR");

    /** NASA Jet Propulsion Laboratory (JPL) RoutingIndicator. */
    public static final RoutingIndicatorTerm JPL = new RoutingIndicatorTerm(" JPL");

    /** NASA/USFC Western Test Range (WTR) RoutingIndicator. */
    public static final RoutingIndicatorTerm WTR = new RoutingIndicatorTerm(" WTR");

    /** NASA Johnson Space Center (JSC) RoutingIndicator. */
    public static final RoutingIndicatorTerm JSC = new RoutingIndicatorTerm(" JSC");

    /** Navy Pacific Missile Range (PMR) RoutingIndicator. */
    public static final RoutingIndicatorTerm PMR = new RoutingIndicatorTerm(" PMR");

    /** Air Force Satellite Control Facility (CSTC) RoutingIndicator. */
    public static final RoutingIndicatorTerm CSTC = new RoutingIndicatorTerm("CSTC");

    /** Army Kwajalein Missile Range (KMR) RoutingIndicator. */
    public static final RoutingIndicatorTerm KMR = new RoutingIndicatorTerm(" KMR");

    /** French Space Agency National Centre for Space Studies (CNES) RoutingIndicator. */
    public static final RoutingIndicatorTerm CNES = new RoutingIndicatorTerm("CNES");

    /** Message originated from more than one of the above stations RoutingIndicator. */
    public static final RoutingIndicatorTerm MANY = new RoutingIndicatorTerm("MANY");

    /** The length of the IIRV term within the message. */
    public static final int ROUTING_INDICATOR_TERM_LENGTH = 4;

    /** Regular expression that ensures the validity of string values for this term. */
    public static final String ROUTING_INDICATOR_TERM_PATTERN = "([A-Z ]+)";

    /**
     * Constructor.
     * <p>
     * See {@link StringValuedIIRVTerm#StringValuedIIRVTerm(String, String, int)}
     *
     * @param value value of the routing indicator term
     */
    public RoutingIndicatorTerm(final String value) {
        super(ROUTING_INDICATOR_TERM_PATTERN, value, ROUTING_INDICATOR_TERM_LENGTH);
    }
}
