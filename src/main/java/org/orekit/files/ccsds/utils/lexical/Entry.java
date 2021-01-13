/* Copyright 2002-2021 CS GROUP
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
package org.orekit.files.ccsds.utils.lexical;

import java.util.Arrays;
import java.util.List;

import org.orekit.errors.OrekitException;

/** Container for a simple entry.
 * <ul>
 *   <li>for KVN files, an entry represents one key and one value, i.e. one line of the file</li>
 *   <li>fox XML files, an entry represents one name and one content, i.e. one simple XML element</li>
 * </ul>
 * @author Luc Maisonobe
 * @since 11.0
 */
public abstract class Entry {

    /** Regular expression for splitting comma-separated lists. */
    private final String COMMA_SEPARATORS = "\\s*,\\s*";

    /** Name of the entry. */
    private final String name;

    /** Content of the entry. */
    private final String content;

    /** Simple constructor.
     * @param name name of the entry
     * @param content content of the entry
     */
    protected Entry(final String name, final String content) {
        this.name    = name;
        this.content = content;
    }

    /** Get the name.
     * @return name of the entry
     */
    public String getName() {
        return name;
    }

    /** Get the content.
     * @return content of the entry
     */
    public String getContent() {
        return content;
    }

    /** Get the content as a double value.
     * @return content as a double value
     */
    public double asDouble() {
        try {
            return Double.parseDouble(content);
        } catch (NumberFormatException nfe) {
            throw generateException();
        }
    }

    /** Get the content as an integer number.
     * @return content as in integer number
     */
    public int asInteger() {
        try {
            return Integer.parseInt(content);
        } catch (NumberFormatException nfe) {
            throw generateException();
        }
    }

    /** Get the value as a list.
     * @return value
     * @since 10.1
     */
    public List<String> getListValue() {
        return Arrays.asList(content.split(COMMA_SEPARATORS));
    }

    /** Generate a parse exception for this entry.
     * @return exception for this entry
     */
    protected abstract OrekitException generateException();

}
