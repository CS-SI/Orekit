/* Copyright 2024 The Johns Hopkins University Applied Physics Laboratory
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
 * 4-character originating routing indicator.
 * <p>
 * Valid values: GCQU, GAQD
 *
 * @author Nick LaFarge
 * @since 13.0
 */
public class OriginatorRoutingIndicatorTerm extends StringValuedIIRVTerm {

    /** GCQU OriginatorRoutingIndicator. */
    public static final OriginatorRoutingIndicatorTerm GCQU = new OriginatorRoutingIndicatorTerm("GCQU");

    /** GAQD OriginatorRoutingIndicator. */
    public static final OriginatorRoutingIndicatorTerm GAQD = new OriginatorRoutingIndicatorTerm("GAQD");

    /** The length of the IIRV term within the message. */
    public static final int ORIGINATOR_ROUTING_INDICATOR_TERM_LENGTH = 4;

    /** Regular expression that ensures the validity of string values for this term. */
    public static final String ORIGINATOR_ROUTING_INDICATOR_TERM_PATTERN = "(GCQU|GAQD)";

    /**
     * Constructor.
     * <p>
     * See {@link StringValuedIIRVTerm#StringValuedIIRVTerm(String, String, int)}
     *
     * @param value value of the originator routing indicator term (dimensionless)
     */
    public OriginatorRoutingIndicatorTerm(final String value) {
        super(ORIGINATOR_ROUTING_INDICATOR_TERM_PATTERN, value, ORIGINATOR_ROUTING_INDICATOR_TERM_LENGTH);
    }
}
