/* Copyright 2002-2016 CS Systèmes d'Information
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
package org.orekit.time;

import java.util.HashMap;
import java.util.Map;

import org.orekit.errors.OrekitIllegalArgumentException;
import org.orekit.errors.OrekitMessages;

/** Enumerate representing a calendar month.
 * <p>This enum is mainly useful to parse data files that use month names
 * like Jan or JAN or January or numbers like 1 or 01. It handles month numbers
 * as well as three letters abbreviation and full names, independently of capitalization.</p>
 * @see DateComponents
 * @author Luc Maisonobe
 */
public enum Month {

    /** January. */
    JANUARY( 1),

    /** February. */
    FEBRUARY( 2),

    /** March. */
    MARCH( 3),

    /** April. */
    APRIL( 4),

    /** May. */
    MAY( 5),

    /** June. */
    JUNE( 6),

    /** July. */
    JULY( 7),

    /** August. */
    AUGUST( 8),

    /** September. */
    SEPTEMBER( 9),

    /** October. */
    OCTOBER(10),

    /** November. */
    NOVEMBER(11),

    /** December. */
    DECEMBER(12);

    /** Parsing map. */
    private static final Map<String, Month> STRINGS_MAP = new HashMap<String, Month>();
    static {
        for (final Month month : values()) {
            STRINGS_MAP.put(month.getLowerCaseName(),         month);
            STRINGS_MAP.put(month.getLowerCaseAbbreviation(), month);
        }
    }

    /** Numbers map. */
    private static final Map<Integer, Month> NUMBERS_MAP = new HashMap<Integer, Month>();
    static {
        for (final Month month : values()) {
            NUMBERS_MAP.put(month.getNumber(), month);
        }
    }

    /** Month number. */
    private final int number;

    /** Lower case full name. */
    private final String lowerCaseName;

    /** Capitalized full name. */
    private final String capitalizedName;

    /** Upper case three letters abbreviation. */
    private final String upperCaseAbbreviation;

    /** Lower case three letters abbreviation. */
    private final String lowerCaseAbbreviation;

    /** Capitalized three letters abbreviation. */
    private final String capitalizedAbbreviation;

    /** Simple constructor.
     * @param number month number
     */
    Month(final int number) {
        this.number             = number;
        lowerCaseName           = toString().toLowerCase();
        capitalizedName         = toString().charAt(0) + lowerCaseName.substring(1);
        upperCaseAbbreviation   = toString().substring(0, 3);
        lowerCaseAbbreviation   = lowerCaseName.substring(0, 3);
        capitalizedAbbreviation = capitalizedName.substring(0, 3);
    }

    /** Get the month number.
     * @return month number between 1 and 12
     */
    public int getNumber() {
        return number;
    }

    /** Get the upper case full name.
     * @return upper case full name
     */
    public String getUpperCaseName() {
        return toString();
    }

    /** Get the lower case full name.
     * @return lower case full name
     */
    public String getLowerCaseName() {
        return lowerCaseName;
    }

    /** Get the capitalized full name.
     * @return capitalized full name
     */
    public String getCapitalizedName() {
        return capitalizedName;
    }

    /** Get the upper case three letters abbreviation.
     * @return upper case three letters abbreviation
     */
    public String getUpperCaseAbbreviation() {
        return upperCaseAbbreviation;
    }

    /** Get the lower case three letters abbreviation.
     * @return lower case three letters abbreviation
     */
    public String getLowerCaseAbbreviation() {
        return lowerCaseAbbreviation;
    }

    /** Get the capitalized three letters abbreviation.
     * @return capitalized three letters abbreviation
     */
    public String getCapitalizedAbbreviation() {
        return capitalizedAbbreviation;
    }

    /** Parse the string to get the month.
     * <p>
     * The string can be either the month number, the full name or the
     * three letter abbreviation. The parsing ignore the case of the specified
     * string and trims surrounding blanks.
     * </p>
     * @param s string to parse
     * @return the month corresponding to the string
     * @exception IllegalArgumentException if the string does not correspond to a month
     */
    public static Month parseMonth(final String s) {
        final String normalizedString = s.trim().toLowerCase();
        final Month month = STRINGS_MAP.get(normalizedString);
        if (month == null) {
            try {
                return getMonth(Integer.parseInt(normalizedString));
            } catch (NumberFormatException nfe) {
                throw new OrekitIllegalArgumentException(OrekitMessages.UNKNOWN_MONTH, s);
            }
        }
        return month;
    }

    /** Get the month corresponding to a number.
     * @param number month number
     * @return the month corresponding to the string
     * @exception IllegalArgumentException if the string does not correspond to a month
     */
    public static Month getMonth(final int number) {
        final Month month = NUMBERS_MAP.get(number);
        if (month == null) {
            throw new OrekitIllegalArgumentException(OrekitMessages.UNKNOWN_MONTH, number);
        }
        return month;
    }

}
