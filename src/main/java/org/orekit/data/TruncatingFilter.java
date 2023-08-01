/* Copyright 2023 Luc Maisonobe
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

/** Filter for truncating line-oriented files.
 * <p>
 * This filter is mainly intended for test purposes, but may also
 * be used to filter out unwanted trailing data in time series for example
 * </p>
 * @author Luc Maisonobe
 * @since 12.0
 */
public class TruncatingFilter implements DataFilter {

    /** Number of lines to keep. */
    private final int nbLines;

    /** Simple constructor.
     * @param nbLines number of lines to keep
     */
    public TruncatingFilter(final int nbLines) {
        this.nbLines = nbLines;
    }

    /** {@inheritDoc} */
    @Override
    public DataSource filter(final DataSource original) throws IOException {
        return new DataSource(original.getName() + "-truncated-after-line-" + nbLines,
                              () -> new TruncatingReader(original.getOpener().openReaderOnce()));
    }

    private class TruncatingReader extends Reader {

        /** Line-oriented reader for raw data. */
        private final BufferedReader reader;

        /** Number of lines already read. */
        private int linesRead;

        /** Pending line, read but not output. */
        private String pending;

        /** Number of characters already output in pending line. */
        private int countOut;

        TruncatingReader(final Reader reader) {
            this.reader = new BufferedReader(reader);
        }

        /** {@inheritDoc} */
        @Override
        public int read(final char[] b, final int offset, final int len) throws IOException {

            if (linesRead < nbLines) {

                if (pending == null) {
                    // we need to read another part from the underlying characters stream
                    countOut = 0;
                    pending = reader.readLine();
                    if (pending == null) {
                        // there are no lines left
                        return -1;
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
                    ++linesRead;
                    pending       = null;
                    return ++n;
                } else {
                    // there are still some pending characters
                    countOut += n;
                    return n;
                }

            } else {
                return -1;
            }

        }

        /** {@inheritDoc} */
        @Override
        public void close() throws IOException {
            reader.close();
        }

    }

}
