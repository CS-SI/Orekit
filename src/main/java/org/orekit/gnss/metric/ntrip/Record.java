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

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/** Record in source table.
 * @author Luc Maisonobe
 * @since 11.0
 */
public abstract class Record {

    /** Pattern for delimiting fields. */
    private static final Pattern SEPARATOR = Pattern.compile(";");

    /** Quoting character. */
    private static final String QUOTE = "\"";

    /** Fields from the parsed sourcetable. */
    private final List<String> fields;

    /** Build a record by parsing a source table line.
     * @param line source table line
     */
    protected Record(final String line) {


        // split the line, taking care of possible quoted separators ";"
        final String[] chunks = SEPARATOR.split(line);

        // prepare storage
        fields = new ArrayList<>(chunks.length);

        // distribute all fields, taking care of possible quoted separators ";"
        for (int i = 0; i < chunks.length; ++i) {
            if (i > 0 && chunks[i - 1].endsWith(QUOTE) &&
                i < chunks.length && chunks[i].startsWith(QUOTE)) {
                // chunks i-1 and i belong to the same field, with an embedded quoted separator
                final String before = fields.remove(fields.size() - 1);
                fields.add(before.substring(0, before.length() - 1) +
                           SEPARATOR +
                           chunks[i].substring(1));
            } else {
                fields.add(chunks[i]);
            }
        }

    }

    /** Get the type of record.
     * @return type of record
     */
    public abstract RecordType getRecordType();

    /** Get the number of fields.
     * @return number of fields
     */
    protected int getFieldsNumber() {
        return fields.size();
    }

    /** Get one field from the parsed sourcetable.
     * @param index field index
     * @return field value
     */
    protected String getField(final int index) {
        return fields.get(index);
    }

    /** Get miscellaneous information.
     * @return miscellaneous information
     */
    public String getMisc() {
        return getField(getFieldsNumber() - 1);
    }

}
