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


package org.orekit.models.earth.atmosphere.data;

import java.io.BufferedReader;
import java.io.IOException;


/**
 * Helper class to parse line data.
 * @since 11.2
  */
class CommonLineReader {

    /** The input stream. */
    private final BufferedReader in;

    /** The last line read from the file. */
    private String line;

    /** The number of the last line read from the file. */
    private long lineNo;

    /**
     * Create a line reader.
     *
     * @param in   the input data stream.
     */
    CommonLineReader(final BufferedReader in) {
        this.in = in;
        this.line = null;
        this.lineNo = 0;
    }

    /**
     * Read a line from the input data stream.
     *
     * @return the next line without the line termination character, or {@code null}
     *         if the end of the stream has been reached.
     * @throws IOException if an I/O error occurs.
     * @see BufferedReader#readLine()
     */
    public String readLine() throws IOException {
        line = in.readLine();
        lineNo++;
        return line;
    }

    /**
     * Check if the last line read is empty.
     *
     * @return whether a line is empty or not
     */
    public boolean isEmptyLine() {
        return line.length() == 0;
    }


    /**
     * Get the last line read from the stream.
     *
     * @return May be {@code null} if no lines have been read or the end of stream
     *         has been reached.
     */
    public String getLine() {
        return line;
    }

    /**
     * Get the line number of the last line read from the file.
     *
     * @return the line number.
     */
    public long getLineNumber() {
        return lineNo;
    }
}
