/* Contributed in the public domain.
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
package org.orekit.utils;

import java.util.Locale;

/** Formatter used to produce strings from data.
 * <p>
 * Interface for formatters to be passed to generators, dictating how to write doubles and datetime.
 * </p>
 * @author John Ajamian
 * @since 13.0
 */
public interface Formatter {

    /**
     * Standardized locale to use, to ensure files can be exchanged without
     * internationalization issues.
     */
    Locale STANDARDIZED_LOCALE = Locale.US;

    /** String format used for dates. **/
    String DATE_FORMAT = "%04d-%02d-%02dT%02d:%02d:%s";

    /**
     * Format a double number.
     *
     * @param value number to format
     * @return number formatted.
     */
    String toString(double value);

    /** Format a date. Does not check if date time is real or if it will meet formating requirements.
     * @param year of date to be formatted
     * @param month of date to be formatted
     * @param day of month to be formatted
     * @param hour to be formatted
     * @param minute to be formatted
     * @param seconds and sub-seconds to be formatted
     * @return date formatted to match the following format [yyyy-MM-ddTHH:mm:ss.S#]
     */
    String toString(int year, int month, int day, int hour, int minute, double seconds);

}
