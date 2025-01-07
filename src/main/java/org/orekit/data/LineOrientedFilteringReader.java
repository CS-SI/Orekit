/* Copyright 2022-2025 Luc Maisonobe
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
package org.orekit.data;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;

import org.hipparchus.util.FastMath;

/** Base class for implementing line-oriented data filtering readers.
 * <p>
 * This reader is intended to be used in {@link DataFilter}.
 * </p>
 * @author Luc Maisonobe
 * @since 12.1
 */
public abstract class LineOrientedFilteringReader extends Reader {

    /** Line-oriented input. */
    private final BufferedReader reader;

    /** Line number. */
    private int lastLineNumber;

    /** Pending filtered output lines. */
    private CharSequence pending;

    /** Number of characters already output in pending lines. */
    private int countOut;

    /** Simple constructor.
     * @param name file name
     * @param input underlying raw stream
     * @exception IOException if first lines cannot be read
     */
    public LineOrientedFilteringReader(final String name, final Reader input) throws IOException {
        reader         = new BufferedReader(input);
        lastLineNumber = 0;
    }

    /** Get the underlying line-oriented reader.
     * @return underlying line-oriented reader
     */
    protected BufferedReader getBufferedReader() {
        return reader;
    }

    /** {@inheritDoc} */
    @Override
    public int read(final char[] b, final int offset, final int len) throws IOException {

        if (pending == null) {
            // we need to read another line from the underlying characters stream and filter it
            countOut = 0;
            final String originalLine = reader.readLine();
            ++lastLineNumber;
            if (originalLine == null) {
                // there are no lines left
                return -1;
            } else {
                pending = filterLine(lastLineNumber, originalLine);
            }
        }

        // copy as many characters as possible from current line
        int n = FastMath.min(len, pending.length() - countOut);
        for (int i = 0; i < n; ++i) {
            b[offset + i] = pending.charAt(countOut + i);
        }

        if (n < len) {
            // line has been completed and we can still output end of line
            b[offset + n] = '\n';
            pending       = null;
            ++n;
        } else {
            // there are still some pending characters
            countOut += n;
        }

        return n;

    }

    /** Filter one line.
     * @param lineNumber line number
     * @param originalLine original line
     * @return filtered line
     * @exception IOException if line cannot be parsed
     */
    protected abstract CharSequence filterLine(int lineNumber, String originalLine) throws IOException;

    /** {@inheritDoc} */
    @Override
    public void close() throws IOException {
        reader.close();
    }

}
