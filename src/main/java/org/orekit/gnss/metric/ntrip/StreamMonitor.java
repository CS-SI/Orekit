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

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.hipparchus.util.FastMath;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitInternalError;
import org.orekit.errors.OrekitMessages;
import org.orekit.gnss.metric.messages.ParsedMessage;
import org.orekit.gnss.metric.parser.AbstractEncodedMessage;
import org.orekit.gnss.metric.parser.MessagesParser;

/** Monitor for retrieving streamed data from one mount point.
 * @author Luc Maisonobe
 * @since 11.0
 */
public class StreamMonitor extends AbstractEncodedMessage implements Runnable {

    /** GGA header key. */
    private static final String GGA_HEADER_KEY = "Ntrip-GGA";

    /** Content type for GNSS data. */
    private static final String GNSS_DATA_CONTENT_TYPE = "gnss/data";

    /** Size of buffer for retrieving data. */
    private static final int BUFFER_SIZE = 0x4000;

    /** Frame preamble. */
    private static final int PREAMBLE = 0xD3;

    /** Frame preamble size. */
    private static final int PREAMBLE_SIZE = 3;

    /** Frame CRC size. */
    private static final int CRC_SIZE = 3;

    /** Generator polynomial for CRC. */
    private static final int GENERATOR = 0x1864CFB;

    /** High bit of the generator polynomial. */
    private static final int HIGH = 0x1000000;

    /** CRC 24Q lookup table. */
    private static final int[] CRC_LOOKUP = new int[256];

    static {

        // set up lookup table
        CRC_LOOKUP[0] = 0;
        CRC_LOOKUP[1] = GENERATOR;

        int h = GENERATOR;
        for (int i = 2; i < 256; i <<= 1) {
            h <<= 1;
            if ((h & HIGH) != 0) {
                h ^= GENERATOR;
            }
            for (int j = 0; j < i; ++j) {
                CRC_LOOKUP[i + j] = CRC_LOOKUP[j] ^ h;
            }
        }

    }

    /** Associated NTRIP client. */
    private final NtripClient client;

    /** Mount point providing the stream. */
    private final String mountPoint;

    /** Messages type of the mount point. */
    private final Type type;

    /** Indicator for required NMEA. */
    private final boolean nmeaRequired;

    /** Indicator for ignoring unknown messages. */
    private final boolean ignoreUnknownMessageTypes;

    /** Delay before we reconnect after connection close. */
    private final double reconnectDelay;

    /** Multiplication factor for reconnection delay. */
    private final double reconnectDelayFactor;

    /** Max number of reconnections. */
    private final int maxRetries;

    /** Stop flag. */
    private AtomicBoolean stop;

    /** Circular buffer. */
    private byte[] buffer;

    /** Read index. */
    private int readIndex;

    /** Message end index. */
    private int messageEndIndex;

    /** Write index. */
    private int writeIndex;

    /** Observers for encoded messages. */
    private final Map<Integer, List<MessageObserver>> observers;

    /** Last available message for each type. */
    private final Map<Integer, ParsedMessage> lastMessages;

    /** Exception caught during monitoring. */
    private final AtomicReference<OrekitException> exception;

    /** Build a monitor for streaming data from a mount point.
     * @param client associated NTRIP client
     * @param mountPoint mount point providing the stream
     * @param type messages type of the mount point
     * @param requiresNMEA if true, the mount point requires a NMEA GGA sentence in the request
     * @param ignoreUnknownMessageTypes if true, unknown messages types are silently ignored
     * @param reconnectDelay delay before we reconnect after connection close
     * @param reconnectDelayFactor factor by which reconnection delay is multiplied after each attempt
     * @param maxRetries max number of reconnect a attempts without reading any data
     */
    public StreamMonitor(final NtripClient client,
                         final String mountPoint, final Type type,
                         final boolean requiresNMEA, final boolean ignoreUnknownMessageTypes,
                         final double reconnectDelay, final double reconnectDelayFactor,
                         final int maxRetries) {
        this.client                    = client;
        this.mountPoint                = mountPoint;
        this.type                      = type;
        this.nmeaRequired              = requiresNMEA;
        this.ignoreUnknownMessageTypes = ignoreUnknownMessageTypes;
        this.reconnectDelay            = reconnectDelay;
        this.reconnectDelayFactor      = reconnectDelayFactor;
        this.maxRetries                = maxRetries;
        this.stop                      = new AtomicBoolean(false);
        this.observers                 = new HashMap<>();
        this.lastMessages              = new HashMap<>();
        this.exception                 = new AtomicReference<OrekitException>(null);
    }

    /** Add an observer for encoded messages.
     * <p>
     * If messages of the specified type have already been retrieved from
     * a stream, the observer will be immediately notified with the last
     * message as a side effect of being added.
     * </p>
     * @param typeCode code for the message type (if set to 0, notification
     * will be triggered regardless of message type)
     * @param observer observer for this message type
     */
    public void addObserver(final int typeCode, final MessageObserver observer) {
        synchronized (observers) {

            // register the observer
            List<MessageObserver> list = observers.get(typeCode);
            if (list == null) {
                // create a new list the first time we register an observer for a message
                list =  new ArrayList<>();
                observers.put(typeCode, list);
            }
            list.add(observer);

            // if we already have a message of the proper type
            // immediately notify the new observer about it
            final ParsedMessage last = lastMessages.get(typeCode);
            if (last != null) {
                observer.messageAvailable(mountPoint, last);
            }

        }
    }

    /** Stop monitoring. */
    public void stopMonitoring() {
        stop.set(true);
    }

    /** Retrieve exception caught during monitoring.
     * @return exception caught
     */
    public OrekitException getException() {
        return exception.get();
    }

    /** {@inheritDoc} */
    @Override
    public void run() {

        try {

            final MessagesParser parser = type.getParser(extractUsedMessages());
            int nbAttempts = 0;
            double delay = reconnectDelay;
            while (nbAttempts < maxRetries) {

                try {
                    // prepare request
                    final HttpURLConnection connection = client.connect(mountPoint);
                    if (nmeaRequired) {
                        if (client.getGGA() == null) {
                            throw new OrekitException(OrekitMessages.STREAM_REQUIRES_NMEA_FIX, mountPoint);
                        } else {
                            // update NMEA GGA sentence in the extra headers for this mount point
                            connection.setRequestProperty(GGA_HEADER_KEY, client.getGGA());
                        }
                    }

                    // perform request
                    final int responseCode = connection.getResponseCode();
                    if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
                        throw new OrekitException(OrekitMessages.FAILED_AUTHENTICATION, mountPoint);
                    } else if (responseCode != HttpURLConnection.HTTP_OK) {
                        throw new OrekitException(OrekitMessages.CONNECTION_ERROR,
                                                  connection.getURL().getHost(),
                                                  connection.getResponseMessage());
                    }

                    // for this request, we MUST get GNSS data
                    if (!GNSS_DATA_CONTENT_TYPE.equals(connection.getContentType())) {
                        throw new OrekitException(OrekitMessages.UNEXPECTED_CONTENT_TYPE, connection.getContentType());
                    }

                    // data extraction loop
                    resetCircularBuffer();
                    try (InputStream is = connection.getInputStream()) {

                        for (int r = fillUp(is); r >= 0; r = fillUp(is)) {

                            // we have read something, reset reconnection attempts counters
                            nbAttempts = 0;
                            delay      = reconnectDelay;

                            if (stop.get()) {
                                // stop monitoring immediately
                                // (returning closes the input stream automatically)
                                return;
                            }

                            while (bufferSize() >= 3) {
                                if (peekByte(0) != PREAMBLE) {
                                    // we are out of synch with respect to frame structure
                                    // drop the unknown byte
                                    moveRead(1);
                                } else {
                                    final int size = (peekByte(1) & 0x03) << 8 | peekByte(2);
                                    if (bufferSize() >= PREAMBLE_SIZE + size + CRC_SIZE) {
                                        // check CRC
                                        final int crc = (peekByte(PREAMBLE_SIZE + size)     << 16) |
                                                        (peekByte(PREAMBLE_SIZE + size + 1) <<  8) |
                                                         peekByte(PREAMBLE_SIZE + size + 2);
                                        if (crc == computeCRC(PREAMBLE_SIZE + size)) {
                                            // we have a complete and consistent frame
                                            // we can extract the message it contains
                                            messageEndIndex = (readIndex + PREAMBLE_SIZE + size) % BUFFER_SIZE;
                                            moveRead(PREAMBLE_SIZE);
                                            start();
                                            final ParsedMessage message = parser.parse(this, ignoreUnknownMessageTypes);
                                            if (message != null) {
                                                storeAndNotify(message);
                                            }
                                            // jump to expected message end, in case the message was corrupted
                                            // and parsing did not reach message end
                                            readIndex = (messageEndIndex + CRC_SIZE) % BUFFER_SIZE;
                                        } else {
                                            // CRC is not consistent, we are probably not really synched
                                            // and the preamble byte was just a random byte
                                            // we drop this single byte and continue looking for sync
                                            moveRead(1);
                                        }
                                    } else {
                                        // the frame is not complete, we need more data
                                        break;
                                    }
                                }
                            }

                        }

                    }
                } catch (SocketTimeoutException ste) {
                    // ignore exception, it will be handled by reconnection attempt below
                } catch (IOException | URISyntaxException e) {
                    throw new OrekitException(e, OrekitMessages.CANNOT_PARSE_GNSS_DATA, client.getHost());
                }

                // manage reconnection
                try {
                    Thread.sleep((int) FastMath.rint(delay * 1000));
                } catch (InterruptedException ie) {
                    // Restore interrupted state...
                    Thread.currentThread().interrupt();
                }
                ++nbAttempts;
                delay *= reconnectDelayFactor;

            }

        } catch (OrekitException oe) {
            // store the exception so it can be retrieved by Ntrip client
            exception.set(oe);
        }

    }

    /** Store a parsed encoded message and notify observers.
     * @param message parsed message
     */
    private void storeAndNotify(final ParsedMessage message) {
        synchronized (observers) {

            for (int typeCode : Arrays.asList(0, message.getTypeCode())) {

                // store message
                lastMessages.put(typeCode, message);

                // notify observers
                final List<MessageObserver> list = observers.get(typeCode);
                if (list != null) {
                    for (final MessageObserver observer : list) {
                        // notify observer
                        observer.messageAvailable(mountPoint, message);
                    }
                }

            }

        }
    }

    /** Reset the circular buffer.
     */
    private void resetCircularBuffer() {
        buffer     = new byte[BUFFER_SIZE];
        readIndex  = 0;
        writeIndex = 0;
    }

    /** Extract data from input stream.
     * @param is input stream to extract data from
     * @return number of byes read or -1
     * @throws IOException if data cannot be extracted properly
     */
    private int fillUp(final InputStream is) throws IOException {
        final int max = bufferMaxWrite();
        if (max == 0) {
            // this should never happen
            // the buffer is large enough for almost 16 encoded messages, including wrapping frame
            throw new OrekitInternalError(null);
        }
        final int r = is.read(buffer, writeIndex, max);
        if (r >= 0) {
            writeIndex = (writeIndex + r) % BUFFER_SIZE;
        }
        return r;
    }

    /** {@inheritDoc} */
    @Override
    protected int fetchByte() {
        if (readIndex == messageEndIndex || readIndex == writeIndex) {
            return -1;
        }

        final int ret = buffer[readIndex] & 0xFF;
        moveRead(1);
        return ret;
    }

    /** Get the number of bytes currently in the buffer.
     * @return number of bytes currently in the buffer
     */
    private int bufferSize() {
        final int n = writeIndex - readIndex;
        return n >= 0 ? n : BUFFER_SIZE + n;
    }

    /** Peek a buffer byte without moving read pointer.
     * @param offset offset counted from read pointer
     * @return value of the byte at given offset
     */
    private int peekByte(final int offset) {
        return buffer[(readIndex + offset) % BUFFER_SIZE] & 0xFF;
    }

    /** Move read pointer.
     * @param n number of bytes to move read pointer
     */
    private void moveRead(final int n) {
        readIndex = (readIndex + n) % BUFFER_SIZE;
    }

    /** Get the number of bytes that can be added to the buffer without wrapping around.
     * @return number of bytes that can be added
     */
    private int bufferMaxWrite() {
        if (writeIndex >= readIndex) {
            return (readIndex == 0 ? BUFFER_SIZE - 1 : BUFFER_SIZE) - writeIndex;
        } else {
            return readIndex - writeIndex - 1;
        }
    }

    /** Compute QualCom CRC.
     * @param length length of the byte stream
     * @return QualCom CRC
     */
    private int computeCRC(final int length) {
        int crc = 0;
        for (int i = 0; i < length; ++i) {
            crc = ((crc << 8) ^ CRC_LOOKUP[peekByte(i) ^ (crc >>> 16)]) & (HIGH - 1);
        }
        return crc;
    }

    /** Extract the used messages.
     * @return the extracted messages
     */
    private List<Integer> extractUsedMessages() {
        synchronized (observers) {

            // List of needed messages
            final List<Integer> messages = new ArrayList<>();

            // Loop on observers entries
            for (Map.Entry<Integer, List<MessageObserver>> entry : observers.entrySet()) {
                // Extract message type code
                final int typeCode = entry.getKey();
                // Add to the list
                messages.add(typeCode);
            }

            return messages;
        }
    }

}
