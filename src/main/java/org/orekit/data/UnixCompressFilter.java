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
package org.orekit.data;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import org.hipparchus.util.FastMath;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitIOException;
import org.orekit.errors.OrekitMessages;

/** Filter for Unix compressed data.
 * @author Luc Maisonobe
 * @since 9.2
 */
public class UnixCompressFilter implements DataFilter {

    /** Suffix for Unix compressed files. */
    private static final String SUFFIX = ".Z";

    /** Empty constructor.
     * <p>
     * This constructor is not strictly necessary, but it prevents spurious
     * javadoc warnings with JDK 18 and later.
     * </p>
     * @since 12.0
     */
    public UnixCompressFilter() {
        // nothing to do
    }

    /** {@inheritDoc} */
    @Override
    public DataSource filter(final DataSource original) {
        final String            oName   = original.getName();
        final DataSource.Opener oOpener = original.getOpener();
        if (oName.endsWith(SUFFIX)) {
            final String                  fName   = oName.substring(0, oName.length() - SUFFIX.length());
            final DataSource.StreamOpener fOpener = () -> new ZInputStream(oName, new Buffer(oOpener.openStreamOnce()));
            return new DataSource(fName, fOpener);
        } else {
            return original;
        }
    }

    /** Filtering of Unix compressed stream. */
    private static class ZInputStream extends InputStream {

        /** First magic header byte. */
        private static final int MAGIC_HEADER_1 = 0x1f;

        /** Second magic header byte. */
        private static final int MAGIC_HEADER_2 = 0x9d;

        /** Byte bits width. */
        private static final int BYTE_WIDTH = 8;

        /** Initial bits width. */
        private static final int INIT_WIDTH = 9;

        /** Reset table code. */
        private static final int RESET_TABLE = 256;

        /** First non-predefined entry. */
        private static final int FIRST = 257;

        /** File name. */
        private final String name;

        /** Indicator for end of input. */
        private boolean endOfInput;

        /** Common sequences table. */
        private final UncompressedSequence[] table;

        /** Next available entry in the table. */
        private int available;

        /** Flag for block mode when table is full. */
        private final boolean blockMode;

        /** Maximum width allowed. */
        private final int maxWidth;

        /** Current input width in bits. */
        private int currentWidth;

        /** Maximum key that can be encoded with current width. */
        private int currentMaxKey;

        /** Number of bits read since last reset. */
        private int bitsRead;

        /** Lookahead byte, already read but not yet used. */
        private int lookAhead;

        /** Number of bits in the lookahead byte. */
        private int lookAheadWidth;

        /** Input buffer. */
        private Buffer input;

        /** Previous uncompressed sequence output. */
        private UncompressedSequence previousSequence;

        /** Uncompressed sequence being output. */
        private UncompressedSequence currentSequence;

        /** Number of bytes of the current sequence already output. */
        private int alreadyOutput;

        /** Simple constructor.
         * @param name file name
         * @param input underlying compressed stream
         * @exception IOException if first bytes cannot be read
         */
        ZInputStream(final String name, final Buffer input)
            throws IOException {

            this.name       = name;
            this.input      = input;
            this.endOfInput = false;

            // check header
            if (input.getByte() != MAGIC_HEADER_1 || input.getByte() != MAGIC_HEADER_2) {
                throw new OrekitException(OrekitMessages.NOT_A_SUPPORTED_UNIX_COMPRESSED_FILE, name);
            }

            final int header3 = input.getByte();
            this.blockMode = (header3 & 0x80) != 0;
            this.maxWidth  = header3 & 0x1f;

            // set up table, with at least all entries for one byte
            this.table = new UncompressedSequence[1 << FastMath.max(INIT_WIDTH, maxWidth)];
            for (int i = 0; i < FIRST; ++i) {
                table[i] = new UncompressedSequence(null, (byte) i);
            }

            // initialize decompression state
            initialize();

        }

        /** Initialize compression state.
         */
        private void initialize() {
            this.available        = FIRST;
            this.bitsRead         = 0;
            this.lookAhead        = 0;
            this.lookAheadWidth   = 0;
            this.currentWidth     = INIT_WIDTH;
            this.currentMaxKey    = (1 << currentWidth) - 1;
            this.previousSequence = null;
            this.currentSequence  = null;
            this.alreadyOutput    = 0;
        }

        /** Read next input key.
         * @return next input key or -1 if end of stream is reached
         * @exception IOException if a read error occurs
         */
        private int nextKey() throws IOException {

            int keyMask = (1 << currentWidth) - 1;

            while (true) {
                // initialize key with the last bits remaining from previous read
                int key = lookAhead & keyMask;

                // read more bits until key is complete
                for (int remaining = currentWidth - lookAheadWidth; remaining > 0; remaining -= BYTE_WIDTH) {
                    lookAhead       = input.getByte();
                    lookAheadWidth += BYTE_WIDTH;
                    if (lookAhead < 0) {
                        if (key == 0 || key == keyMask) {
                            // the key is either a set of padding 0 bits
                            // or a full key containing -1 if read() is called several times after EOF
                            return -1;
                        } else {
                            // end of stream encountered in the middle of a read
                            throw new OrekitIOException(OrekitMessages.UNEXPECTED_END_OF_FILE, name);
                        }
                    }
                    key = (key | lookAhead << (currentWidth - remaining)) & keyMask;
                }

                // store the extra bits already read in the lookahead byte for next call
                lookAheadWidth -= currentWidth;
                lookAhead       = lookAhead >>> (BYTE_WIDTH - lookAheadWidth);

                bitsRead += currentWidth;

                if (blockMode && key == RESET_TABLE) {

                    // skip the padding bits inserted when compressor flushed its buffer
                    final int superSize = currentWidth * 8;
                    int padding = (superSize - 1 - (bitsRead + superSize - 1) % superSize) / 8;
                    while (padding-- > 0) {
                        input.getByte();
                    }

                    // reset the table to handle a new block and read again next key
                    Arrays.fill(table, FIRST, table.length, null);
                    initialize();

                    // reset the lookahead mask as the current width has changed
                    keyMask = (1 << currentWidth) - 1;

                } else {
                    // return key at current width
                    return key;
                }

            }

        }

        /** Select next uncompressed sequence to output.
         * @return true if there is a next sequence
         * @exception IOException if a read error occurs
         */
        private boolean selectNext() throws IOException {

            // read next input key
            final int key = nextKey();
            if (key < 0) {
                // end of stream reached
                return false;
            }

            if (previousSequence != null && available < table.length) {
                // update the table with the next uncompressed byte appended to previous sequence
                final byte nextByte;
                if (key == available) {
                    nextByte = previousSequence.getByte(0);
                } else if (table[key] != null) {
                    nextByte = table[key].getByte(0);
                } else {
                    throw new OrekitIOException(OrekitMessages.CORRUPTED_FILE, name);
                }
                table[available++] = new UncompressedSequence(previousSequence, nextByte);
                if (available > currentMaxKey && currentWidth < maxWidth) {
                    // we need to increase the key size
                    currentMaxKey = (1 << ++currentWidth) - 1;
                }
            }

            currentSequence = table[key];
            if (currentSequence == null) {
                // the compressed file references a non-existent table entry
                // (this is not the well-known case of entry being used just before
                //  being defined, which is already handled above), the file is corrupted
                throw new OrekitIOException(OrekitMessages.CORRUPTED_FILE, name);
            }
            alreadyOutput   = 0;

            return true;

        }

        /** {@inheritDoc} */
        @Override
        public int read() throws IOException {
            final byte[] b = new byte[1];
            return read(b, 0, 1) < 0 ? -1 : b[0];
        }

        /** {@inheritDoc} */
        @Override
        public int read(final byte[] b, final int offset, final int len) throws IOException {

            if (currentSequence == null) {
                if (endOfInput || !selectNext()) {
                    // we have reached end of data
                    endOfInput = true;
                    return -1;
                }
            }

            // copy as many bytes as possible from current sequence
            final int n = FastMath.min(len, currentSequence.length() - alreadyOutput);
            for (int i = 0; i < n; ++i) {
                b[offset + i] = currentSequence.getByte(alreadyOutput++);
            }
            if (alreadyOutput >= currentSequence.length()) {
                // we have just exhausted the current sequence
                previousSequence = currentSequence;
                currentSequence  = null;
                alreadyOutput    = 0;
            }

            return n;

        }

        /** {@inheritDoc} */
        @Override
        public int available() {
            return currentSequence == null ? 0 : currentSequence.length() - alreadyOutput;
        }

    }

    /** Uncompressed bits sequence. */
    private static class UncompressedSequence {

        /** Prefix sequence (null if this is a start sequence). */
        private final UncompressedSequence prefix;

        /** Last byte in the sequence. */
        private final byte last;

        /** Index of the last byte in the sequence (i.e. length - 1). */
        private final int index;

        /** Simple constructor.
         * @param prefix prefix of the sequence (null if this is a start sequence)
         * @param last last byte of the sequence
         */
        UncompressedSequence(final UncompressedSequence prefix, final byte last) {
            this.prefix = prefix;
            this.last   = last;
            this.index  = prefix == null ? 0 : prefix.index + 1;
        }

        /** Get the length of the sequence.
         * @return length of the sequence
         */
        public int length() {
            return index + 1;
        }

        /** Get a byte from the sequence.
         * @param outputIndex index of the byte in the sequence, counting from 0
         * @return byte at {@code outputIndex}
         */
        public byte getByte(final int outputIndex) {
            return index == outputIndex ? last : prefix.getByte(outputIndex);
        }

    }

    /** Buffer for reading input data. */
    private static class Buffer {

        /** Size of input/output buffers. */
        private static final int BUFFER_SIZE = 4096;

        /** Underlying compressed stream. */
        private final InputStream input;

        /** Buffer data. */
        private final byte[] data;

        /** Start of pending data. */
        private int start;

        /** End of pending data. */
        private int end;

        /** Simple constructor.
         * @param input input stream
         */
        Buffer(final InputStream input) {
            this.input = input;
            this.data  = new byte[BUFFER_SIZE];
            this.start = 0;
            this.end   = start;
        }

        /** Get one input byte.
         * @return input byte, or -1 if end of input has been reached
         * @throws IOException if input data cannot be read
         */
        private int getByte() throws IOException {

            if (start == end) {
                // the buffer is empty
                start = 0;
                end   = input.read(data);
                if (end == -1) {
                    return -1;
                }
            }

            return data[start++] & 0xFF;

        }

    }
}
