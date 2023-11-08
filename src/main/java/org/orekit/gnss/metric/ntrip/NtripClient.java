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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Proxy.Type;
import java.net.SocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.hipparchus.util.FastMath;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.gnss.metric.messages.ParsedMessage;

/** Source table for ntrip streams retrieval.
 * <p>
 * Note that all authentication is performed automatically by just
 * calling the standard {@link Authenticator#setDefault(Authenticator)}
 * method to set up an authenticator.
 * </p>
 * @author Luc Maisonobe
 * @since 11.0
 */
public class NtripClient {

    /** Default timeout for connections and reads (ms). */
    public static final int DEFAULT_TIMEOUT = 10000;

    /** Default port for ntrip communication. */
    public static final int DEFAULT_PORT = 2101;

    /** Default delay before we reconnect after connection close (s). */
    public static final double DEFAULT_RECONNECT_DELAY = 1.0;

    /** Default factor by which reconnection delay is multiplied after each attempt. */
    public static final double DEFAULT_RECONNECT_DELAY_FACTOR = 1.5;

    /** Default maximum number of reconnect a attempts without readin any data. */
    public static final int DEFAULT_MAX_RECONNECT = 20;

    /** Host header. */
    private static final String HOST_HEADER_KEY = "Host";

    /** User-agent header key. */
    private static final String USER_AGENT_HEADER_KEY = "User-Agent";

    /** User-agent header value. */
    private static final String USER_AGENT_HEADER_VALUE = "NTRIP orekit/11.0";

    /** Version header key. */
    private static final String VERSION_HEADER_KEY = "Ntrip-Version";

    /** Version header value. */
    private static final String VERSION_HEADER_VALUE = "Ntrip/2.0";

    /** Connection header key. */
    private static final String CONNECTION_HEADER_KEY = "Connection";

    /** Connection header value. */
    private static final String CONNECTION_HEADER_VALUE = "close";

    /** Flags header key. */
    private static final String FLAGS_HEADER_KEY = "Ntrip-Flags";

    /** Content type for source table. */
    private static final String SOURCETABLE_CONTENT_TYPE = "gnss/sourcetable";

    /** Degrees to arc minutes conversion factor. */
    private static final double DEG_TO_MINUTES = 60.0;

    /** Caster host. */
    private final String host;

    /** Caster port. */
    private final int port;

    /** Delay before we reconnect after connection close. */
    private double reconnectDelay;

    /** Multiplication factor for reconnection delay. */
    private double reconnectDelayFactor;

    /** Max number of reconnections. */
    private int maxRetries;

    /** Timeout for connections and reads. */
    private int timeout;

    /** Proxy to use. */
    private Proxy proxy;

    /** NMEA GGA sentence (may be null). */
    private AtomicReference<String> gga;

    /** Observers for encoded messages. */
    private final List<ObserverHolder> observers;

    /** Monitors for data streams. */
    private final Map<String, StreamMonitor> monitors;

    /** Source table. */
    private SourceTable sourceTable;

    /** Executor for stream monitoring tasks. */
    private ExecutorService executorService;

    /** Build a client for NTRIP.
     * <p>
     * The default configuration uses default timeout, default reconnection
     * parameters, no GPS fix and no proxy.
     * </p>
     * @param host caster host providing the source table
     * @param port port to use for connection
     * see {@link #DEFAULT_PORT}
     */
    public NtripClient(final String host, final int port) {
        this.host         = host;
        this.port         = port;
        this.observers    = new ArrayList<>();
        this.monitors     = new HashMap<>();
        setTimeout(DEFAULT_TIMEOUT);
        setReconnectParameters(DEFAULT_RECONNECT_DELAY,
                               DEFAULT_RECONNECT_DELAY_FACTOR,
                               DEFAULT_MAX_RECONNECT);
        setProxy(Type.DIRECT, null, -1);
        this.gga             = new AtomicReference<String>(null);
        this.sourceTable     = null;
        this.executorService = null;
    }

    /** Get the caster host.
     * @return caster host
     */
    public String getHost() {
        return host;
    }

    /** Get the port to use for connection.
     * @return port to use for connection
     */
    public int getPort() {
        return port;
    }

    /** Set timeout for connections and reads.
     * @param timeout timeout for connections and reads (ms)
     */
    public void setTimeout(final int timeout) {
        this.timeout = timeout;
    }

    /** Set Reconnect parameters.
     * @param delay delay before we reconnect after connection close
     * @param delayFactor factor by which reconnection delay is multiplied after each attempt
     * @param max max number of reconnect a attempts without reading any data
     */
    public void setReconnectParameters(final double delay,
                                       final double delayFactor,
                                       final int max) {
        this.reconnectDelay       = delay;
        this.reconnectDelayFactor = delayFactor;
        this.maxRetries           = max;
    }

    /** Set proxy parameters.
     * @param type proxy type
     * @param proxyHost host name of the proxy (ignored if {@code type} is {@code Proxy.Type.DIRECT})
     * @param proxyPort port number of the proxy (ignored if {@code type} is {@code Proxy.Type.DIRECT})
     */
    public void setProxy(final Proxy.Type type, final String proxyHost, final int proxyPort) {
        try {
            if (type == Proxy.Type.DIRECT) {
                // disable proxy
                proxy = Proxy.NO_PROXY;
            } else {
                // enable proxy
                final InetAddress   hostAddress  = InetAddress.getByName(proxyHost);
                final SocketAddress proxyAddress = new InetSocketAddress(hostAddress, proxyPort);
                proxy = new Proxy(type, proxyAddress);
            }
        } catch (UnknownHostException uhe) {
            throw new OrekitException(uhe, OrekitMessages.UNKNOWN_HOST, proxyHost);
        }
    }

    /** Get proxy.
     * @return proxy to use
     */
    public Proxy getProxy() {
        return proxy;
    }

    /** Set GPS fix data to send as NMEA sentence to Ntrip caster if required.
     * @param hour hour of the fix (UTC time)
     * @param minute minute of the fix (UTC time)
     * @param second second of the fix (UTC time)
     * @param latitude latitude (radians)
     * @param longitude longitude (radians)
     * @param ellAltitude altitude above ellipsoid (m)
     * @param undulation height of the geoid above ellipsoid (m)
     */
    public void setFix(final int hour, final int minute, final double second,
                       final double latitude, final double longitude, final double ellAltitude,
                       final double undulation) {

        // convert latitude
        final double latDeg = FastMath.abs(FastMath.toDegrees(latitude));
        final int    dLat   = (int) FastMath.floor(latDeg);
        final double mLat   = DEG_TO_MINUTES * (latDeg - dLat);
        final char   cLat   = latitude >= 0.0 ? 'N' : 'S';

        // convert longitude
        final double lonDeg = FastMath.abs(FastMath.toDegrees(longitude));
        final int    dLon   = (int) FastMath.floor(lonDeg);
        final double mLon   = DEG_TO_MINUTES * (lonDeg - dLon);
        final char   cLon   = longitude >= 0.0 ? 'E' : 'W';

        // build NMEA GGA sentence
        final StringBuilder builder = new StringBuilder(82);
        try (Formatter formatter = new Formatter(builder, Locale.US)) {

            // dummy values
            final int    fixQuality = 1;
            final int    nbSat      = 4;
            final double hdop       = 1.0;

            // sentence body
            formatter.format("$GPGGA,%02d%02d%06.3f,%02d%07.4f,%c,%02d%07.4f,%c,%1d,%02d,%3.1f,%.1f,M,%.1f,M,,",
                             hour, minute, second,
                             dLat, mLat, cLat, dLon, mLon, cLon,
                             fixQuality, nbSat, hdop,
                             ellAltitude, undulation);

            // checksum
            byte sum = 0;
            for (int i = 1; i < builder.length(); ++i) {
                sum ^= builder.charAt(i);
            }
            formatter.format("*%02X", sum);

        }
        gga.set(builder.toString());

    }

    /** Get NMEA GGA sentence.
     * @return NMEA GGA sentence (may be null)
     */
    String getGGA() {
        return gga.get();
    }

    /** Add an observer for an encoded messages.
     * <p>
     * If messages of the specified type have already been retrieved from
     * a stream, the observer will be immediately notified with the last
     * message from each mount point (in unspecified order) as a side effect
     * of being added.
     * </p>
     * @param typeCode code for the message type (if set to 0, notification
     * will be triggered regardless of message type)
     * @param mountPoint mountPoint from which data must come (if null, notification
     * will be triggered regardless of mount point)
     * @param observer observer for this message type
     */
    public void addObserver(final int typeCode, final String mountPoint,
                            final MessageObserver observer) {

        // store the observer for future monitored mount points
        observers.add(new ObserverHolder(typeCode, mountPoint, observer));

        // check if we should also add it to already monitored mount points
        for (Map.Entry<String, StreamMonitor> entry : monitors.entrySet()) {
            if (mountPoint == null || mountPoint.equals(entry.getKey())) {
                entry.getValue().addObserver(typeCode, observer);
            }
        }

    }

    /** Get a sourcetable.
     * @return source table from the caster
     */
    public SourceTable getSourceTable() {
        if (sourceTable == null) {
            try {

                // perform request
                final HttpURLConnection connection = connect("");

                final int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
                    throw new OrekitException(OrekitMessages.FAILED_AUTHENTICATION, "caster");
                } else if (responseCode != HttpURLConnection.HTTP_OK) {
                    throw new OrekitException(OrekitMessages.CONNECTION_ERROR, host, connection.getResponseMessage());
                }

                // for this request, we MUST get a source table
                if (!SOURCETABLE_CONTENT_TYPE.equals(connection.getContentType())) {
                    throw new OrekitException(OrekitMessages.UNEXPECTED_CONTENT_TYPE, connection.getContentType());
                }

                final SourceTable table = new SourceTable(getHeaderValue(connection, FLAGS_HEADER_KEY));

                // parse source table records
                try (InputStream is = connection.getInputStream();
                     InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8);
                     BufferedReader br = new BufferedReader(isr)) {
                    int lineNumber = 0;
                    for (String line = br.readLine(); line != null; line = br.readLine()) {

                        ++lineNumber;
                        line = line.trim();
                        if (line.length() == 0) {
                            continue;
                        }

                        if (line.startsWith(RecordType.CAS.toString())) {
                            table.addCasterRecord(new CasterRecord(line));
                        } else if (line.startsWith(RecordType.NET.toString())) {
                            table.addNetworkRecord(new NetworkRecord(line));
                        } else if (line.startsWith(RecordType.STR.toString())) {
                            table.addDataStreamRecord(new DataStreamRecord(line));
                        } else if (line.startsWith("ENDSOURCETABLE")) {
                            // we have reached end of table
                            break;
                        } else {
                            throw new OrekitException(OrekitMessages.SOURCETABLE_PARSE_ERROR,
                                                      connection.getURL().getHost(), lineNumber, line);
                        }

                    }
                }

                sourceTable = table;
                return table;

            } catch (IOException | URISyntaxException e) {
                throw new OrekitException(e, OrekitMessages.CANNOT_PARSE_SOURCETABLE, host);
            }
        }

        return sourceTable;

    }

    /** Connect to a mount point and start streaming data from it.
     * <p>
     * This method sets up an internal dedicated thread for continuously
     * monitoring data incoming from a mount point. When new complete
     * {@link ParsedMessage parsed messages} becomes available, the
     * {@link MessageObserver observers} that have been registered
     * using {@link #addObserver(int, String, MessageObserver) addObserver()}
     * method will be notified about the message.
     * </p>
     * <p>
     * This method must be called once for each stream to monitor.
     * </p>
     * @param mountPoint mount point providing the stream
     * @param type messages type of the mount point
     * @param requiresNMEA if true, the mount point requires a NMEA GGA sentence in the request
     * @param ignoreUnknownMessageTypes if true, unknown messages types are silently ignored
     */
    public void startStreaming(final String mountPoint, final org.orekit.gnss.metric.ntrip.Type type,
                               final boolean requiresNMEA, final boolean ignoreUnknownMessageTypes) {

        if (executorService == null) {
            // lazy creation of executor service, with one thread for each possible data stream
            executorService = Executors.newFixedThreadPool(getSourceTable().getDataStreams().size());
        }

        // safety check
        if (monitors.containsKey(mountPoint)) {
            throw new OrekitException(OrekitMessages.MOUNPOINT_ALREADY_CONNECTED, mountPoint);
        }

        // create the monitor
        final StreamMonitor monitor = new StreamMonitor(this, mountPoint, type, requiresNMEA, ignoreUnknownMessageTypes,
                                                        reconnectDelay, reconnectDelayFactor, maxRetries);
        monitors.put(mountPoint, monitor);

        // set up the already known observers
        for (final ObserverHolder observerHolder : observers) {
            if (observerHolder.mountPoint == null ||
                observerHolder.mountPoint.equals(mountPoint)) {
                monitor.addObserver(observerHolder.typeCode, observerHolder.observer);
            }
        }

        // start streaming data
        executorService.execute(monitor);

    }

    /** Check if any of the streaming thread has thrown an exception.
     * <p>
     * If a streaming thread has thrown an exception, it will be rethrown here
     * </p>
     */
    public void checkException() {
        // check if any of the stream got an exception
        for (final  Map.Entry<String, StreamMonitor> entry : monitors.entrySet()) {
            final OrekitException exception = entry.getValue().getException();
            if (exception != null) {
                throw exception;
            }
        }
    }

    /** Stop streaming data from all connected mount points.
     * <p>
     * If an exception was encountered during data streaming, it will be rethrown here
     * </p>
     * @param time timeout for waiting underlying threads termination (ms)
     */
    public void stopStreaming(final int time) {

        // ask all monitors to stop retrieving data
        for (final  Map.Entry<String, StreamMonitor> entry : monitors.entrySet()) {
            entry.getValue().stopMonitoring();
        }

        try {
            // wait for proper ending
            executorService.shutdown();
            executorService.awaitTermination(time, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ie) {
            // Restore interrupted state...
            Thread.currentThread().interrupt();
        }

        checkException();

    }

    /** Connect to caster.
     * @param mountPoint mount point (empty for getting sourcetable)
     * @return performed connection
     * @throws IOException if an I/O exception occurs during connection
     * @throws URISyntaxException if the built URI is invalid
     */
    HttpURLConnection connect(final String mountPoint)
        throws IOException, URISyntaxException {

        // set up connection
        final String scheme = "http";
        final URL casterURL = new URI(scheme, null, host, port, "/" + mountPoint, null, null).toURL();
        final HttpURLConnection connection = (HttpURLConnection) casterURL.openConnection(proxy);
        connection.setConnectTimeout(timeout);
        connection.setReadTimeout(timeout);

        // common headers
        connection.setRequestProperty(HOST_HEADER_KEY,       host);
        connection.setRequestProperty(VERSION_HEADER_KEY,    VERSION_HEADER_VALUE);
        connection.setRequestProperty(USER_AGENT_HEADER_KEY, USER_AGENT_HEADER_VALUE);
        connection.setRequestProperty(CONNECTION_HEADER_KEY, CONNECTION_HEADER_VALUE);

        return connection;

    }

    /** Get an header from a response.
     * @param connection connection to analyze
     * @param key header key
     * @return header value
     */
    private String getHeaderValue(final URLConnection connection, final String key) {
        final String value = connection.getHeaderField(key);
        if (value == null) {
            throw new OrekitException(OrekitMessages.MISSING_HEADER,
                                      connection.getURL().getHost(), key);
        }
        return value;
    }

    /** Local holder for observers. */
    private static class ObserverHolder {

        /** Code for the message type. */
        private final int typeCode;

        /** Mount point. */
        private final String mountPoint;

        /** Observer to notify. */
        private final MessageObserver observer;

        /** Simple constructor.
         * @param typeCode code for the message type
         * @param mountPoint mountPoint from which data must come (if null, notification
         * will be triggered regardless of mount point)
         * @param observer observer for this message type
         */
        ObserverHolder(final int typeCode, final String mountPoint,
                            final MessageObserver observer) {
            this.typeCode   = typeCode;
            this.mountPoint = mountPoint;
            this.observer   = observer;
        }

    }

}
