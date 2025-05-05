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

import org.orekit.files.iirv.terms.base.LongValuedIIRVTerm;

/**
 * Source of the data message.
 * <p>
 * Valid values:
 * <ul>
 * <li> 1 = Nominal/planning
 * <li> 2 = Real-time
 * <li> 3 = Off-line
 * <li> 4 = Off-line/mean
 * </ul>
 * <p>
 *
 * @author Nick LaFarge
 * @since 13.0
 */
public class DataSourceTerm extends LongValuedIIRVTerm {

    /** Nominal/planning DataSource. */
    public static final DataSourceTerm NOMINAL = new DataSourceTerm("1");

    /** Real-time DataSource. */
    public static final DataSourceTerm REAL_TIME = new DataSourceTerm("2");

    /** Off-line DataSource. */
    public static final DataSourceTerm OFFLINE = new DataSourceTerm("3");

    /** Off-line/mean DataSource. */
    public static final DataSourceTerm OFFLINE_MEAN = new DataSourceTerm("4");

    /** The length of the IIRV term within the message. */
    public static final int DATA_SOURCE_TERM_LENGTH = 1;

    /** Regular expression that ensures the validity of string values for this term. */
    public static final String DATA_SOURCE_TERM_PATTERN = "[1-4]";

    /**
     * Constructor.
     * <p>
     * See {@link LongValuedIIRVTerm#LongValuedIIRVTerm(String, String, int, boolean)}
     *
     * @param value value of the data source term
     */
    public DataSourceTerm(final String value) {
        super(DATA_SOURCE_TERM_PATTERN, value, DATA_SOURCE_TERM_LENGTH, false);
    }

    /**
     * Constructor.
     * <p>
     * See {@link LongValuedIIRVTerm#LongValuedIIRVTerm(String, long, int, boolean)}
     *
     * @param value value of the data source term
     */
    public DataSourceTerm(final long value) {
        super(DATA_SOURCE_TERM_PATTERN, value, DATA_SOURCE_TERM_LENGTH, false);
    }

}
