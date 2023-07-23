/* Copyright 2023 Thales Alenia Space
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
package org.orekit.gnss.observation;

import java.io.IOException;
import java.util.Locale;
import java.util.function.Supplier;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.time.DateTimeComponents;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;

/** Writer for Rinex observation file.
 * @author Luc Maisonobe
 * @since 12.0
 */
public class RinexObservationWriter {

    /** Index of label in header lines. */
    private static final int LABEL_INDEX = 60;

    /** Format for two 20 characters fields. */
    private static final String TWO_TWENTY = "%20s%20s";

    /** Format for three 20 characters fields. */
    private static final String THREE_TWENTY = "%20s%20s%20s";

    /** Format for one 14 digits characters field. */
    private static final String ONE_FOURTEEN_DIGIT = "%14.4f";

    /** Format for three 14 digits characters fields. */
    private static final String THREE_FOURTEEN_DIGIT = "%14.4f%14.4f%14.4f";

    /** Format for first/last observation times. */
    private static final String FIRST_LAST_OBS = "%6d%6d%6d%6d%6d%13.7f     %3s";

    /** Label for leap seconds. */
    private static final String LEAP_SECONDS = "LEAP SECONDS";

    /** Destination of generated output. */
    private final Appendable output;

    /** Output name for error messages. */
    private final String outputName;

    /** Saved header. */
    private RinexObservationHeader savedHeader;

    /** Simple constructor.
     * @param output destination of generated output
     * @param outputName output name for error messages
     */
    public RinexObservationWriter(final Appendable output, final String outputName)
        throws IOException {
        this.output      = output;
        this.outputName  = outputName;
        this.savedHeader = null;
    }

    /** Write a complete observation file.
     * <p>
     * This method calls {@link #writeHeader(RinexObservationHeader)} once
     * and then loops on calling {@link #writeObservationDataSet(ObservationDataSet)}
     * for all observation data sets in the file
     * </p>
     * @param rinexObservation Rinex observation file to write
     * @see #writeHeader(RinexObservationHeader)
     * @see #writeObservationDataSet(ObservationDataSet)
     * @exception IOException if an I/O error occurs.
     */
    public void writeCompleteFile(final RinexObservation rinexObservation)
        throws IOException {
        writeHeader(rinexObservation.getHeader());
        for (final ObservationDataSet observationDataSet : rinexObservation.getObservationDataSets()) {
            writeObservationDataSet(observationDataSet);
        }
    }

    /** Write header.
     * <p>
     * This method must be called exactly once at the beginning
     * (directly or by {@link #writeCompleteFile(RinexObservation)})
     * </p>
     * @param header header to write
     * @exception IOException if an I/O error occurs.
     */
    public void writeHeader(final RinexObservationHeader header)
        throws IOException {

        // check header is written exactly once
        if (savedHeader != null) {
            throw new OrekitException(OrekitMessages.HEADER_ALREADY_WRITTEN, outputName);
        }
        savedHeader = header;

        final ObservationTimeScale observationTimeScale = header.getSatelliteSystem().getObservationTimeScale() != null ?
                                                          header.getSatelliteSystem().getObservationTimeScale() :
                                                              ObservationTimeScale.GPS;
        final TimeScale timeScale = observationTimeScale.getTimeScale(TimeScalesFactory.getTimeScales());

        writeHeaderLine(String.format(Locale.US,
                                      "%9.2f           %c                   %c",
                                      header.getFormatVersion(),
                                      'O',
                                      header.getSatelliteSystem().getKey()),
                        "RINEX VERSION / TYPE");

        final DateTimeComponents dtc = header.getCreationDateComponents();
        final String creationDateAndZone;
        if (header.getFormatVersion() < 3.0) {
            creationDateAndZone = String.format(Locale.US, "%02d-%3s-%02d %02d:%02d %s",
                                                dtc.getDate().getDay(),
                                                dtc.getDate().getMonthEnum().getUpperCaseAbbreviation(),
                                                dtc.getDate().getYear() % 100,
                                                dtc.getTime().getHour(),
                                                dtc.getTime().getMinute(),
                                                header.getCreationTimeZone());
        } else {
            creationDateAndZone = String.format(Locale.US, "%4d%02d%02d %02d:%02d:%02d%s",
                                                dtc.getDate().getYear(),
                                                dtc.getDate().getMonth(),
                                                dtc.getDate().getDay(),
                                                dtc.getTime().getHour(),
                                                dtc.getTime().getMinute(),
                                                dtc.getTime().getSecond(),
                                                header.getCreationTimeZone());
        }
        writeHeaderLine(String.format(Locale.US, THREE_TWENTY,
                                      header.getProgramName(),
                                      header.getRunByName(),
                                      dtc.getDate().getDay(),
                                      creationDateAndZone),
                        "PGM / RUN BY / DATE");

        for (final String comment : header.getComments()) {
            writeHeaderLine(comment, "COMMENT");
        }

        writeHeaderLine(header::getMarkerName,   "MARKER NAME");
        writeHeaderLine(header::getMarkerNumber, "MARKER NUMBER");
        writeHeaderLine(header::getMarkerType,   "MARKER TYPE");
        writeHeaderLine(String.format(Locale.US, "%20s%40s",
                                      header.getObserverName(),
                                      header.getAgencyName()),
                        "OBSERVER / AGENCY");
        writeHeaderLine(String.format(Locale.US, THREE_TWENTY,
                                      header.getReceiverNumber(),
                                      header.getReceiverType(),
                                      header.getReceiverVersion()),
                        "REC # / TYPE / VERS");
        writeHeaderLine(String.format(Locale.US, TWO_TWENTY,
                                      header.getAntennaNumber(),
                                      header.getAntennaType()),
                        "ANT # / TYPE");
        writeHeaderLine(header.getApproxPos(), "APPROX POSITION XYZ");
        if (!Double.isNaN(header.getAntennaHeight())) {
            writeHeaderLine(String.format(Locale.US, THREE_FOURTEEN_DIGIT,
                                          header.getAntennaHeight(),
                                          header.getEccentricities().getX(),
                                          header.getEccentricities().getY()),
                            "ANTENNA: DELTA H/E/N");
        }
        writeHeaderLine(header.getAntennaReferencePoint(), "ANTENNA: DELTA X/Y/Z");
        if (header.getAntennaPhaseCenter() != null) {
            writeHeaderLine(String.format(Locale.US, "%c %3s%9.4f%14.4f%14.4f",
                                          header.getSatelliteSystem().getKey(),
                                          header.getObservationCode(),
                                          header.getAntennaPhaseCenter().getX(),
                                          header.getAntennaPhaseCenter().getY(),
                                          header.getAntennaPhaseCenter().getZ()),
                            "ANTENNA: PHASECENTER");
        }
        writeHeaderLine(header.getAntennaBSight(), "ANTENNA: B.SIGHT XYZ");
        if (!Double.isNaN(header.getAntennaAzimuth())) {
            writeHeaderLine(String.format(Locale.US, ONE_FOURTEEN_DIGIT,
                                          FastMath.toDegrees(header.getAntennaAzimuth())),
                            "ANTENNA: ZERODIR AZI");
        }
        writeHeaderLine(header.getAntennaZeroDirection(), "ANTENNA: ZERODIR XYZ");
        if (header.getClkOffset() >= 0) {
            writeHeaderLine(String.format(Locale.US, "%6d", header.getClkOffset()),
                            "RCV CLOCK OFFS APPL");
        }
        if (!Double.isNaN(header.getInterval())) {
            writeHeaderLine(String.format(Locale.US, "%10.3f", header.getInterval()),
                            "INTERVAL");
        }
        final DateTimeComponents dtcFirst = header.getTFirstObs().getComponents(timeScale);
        writeHeaderLine(String.format(Locale.US, FIRST_LAST_OBS,
                                      dtcFirst.getDate().getYear(),
                                      dtcFirst.getDate().getMonth(),
                                      dtcFirst.getDate().getDay(),
                                      dtcFirst.getTime().getHour(),
                                      dtcFirst.getTime().getMinute(),
                                      dtcFirst.getTime().getSecond(),
                                      observationTimeScale.name()),
                        "TIME OF FIRST OBS");
        final DateTimeComponents dtcLast = header.getTLastObs().getComponents(timeScale);
        writeHeaderLine(String.format(Locale.US, FIRST_LAST_OBS,
                                      dtcLast.getDate().getYear(),
                                      dtcLast.getDate().getMonth(),
                                      dtcLast.getDate().getDay(),
                                      dtcLast.getTime().getHour(),
                                      dtcLast.getTime().getMinute(),
                                      dtcLast.getTime().getSecond(),
                                      observationTimeScale.name()),
                        "TIME OF LAST OBS");
        if (header.getLeapSeconds() > 0) {
            if (header.getFormatVersion() < 3.0) {
                writeHeaderLine(String.format(Locale.US, "%6d",
                                              header.getLeapSeconds()),
                                LEAP_SECONDS);
            } else {
                writeHeaderLine(String.format(Locale.US, "%6d%6d%6d%6d",
                                              header.getLeapSeconds(),
                                              header.getLeapSecondsFuture(),
                                              header.getLeapSecondsWeekNum(),
                                              header.getLeapSecondsDayNum()),
                                LEAP_SECONDS);
            }
        }
        writeHeaderLine(String.format(Locale.US,
                                      ""),
                        "PRN / # OF OBS");
        writeHeaderLine(String.format(Locale.US,
                                      ""),
                        "# OF SATELLITES");
        writeHeaderLine(String.format(Locale.US,
                                      ""),
                        "SYS / # / OBS TYPES");
        writeHeaderLine(String.format(Locale.US,
                                      ""),
                        "CENTER OF MASS: XYZ");
        writeHeaderLine(String.format(Locale.US,
                                      ""),
                        "SIGNAL STRENGTH UNIT");
        writeHeaderLine(String.format(Locale.US,
                                      ""),
                        "SYS / DCBS APPLIED");
        writeHeaderLine(String.format(Locale.US,
                                      ""),
                        "SYS / PCVS APPLIED");
        writeHeaderLine(String.format(Locale.US,
                                      ""),
                        "SYS / SCALE FACTOR");
        writeHeaderLine(String.format(Locale.US,
                                      ""),
                        "SYS / PHASE SHIFT");
        writeHeaderLine(String.format(Locale.US,
                                      ""),
                        "GLONASS SLOT / FRQ #");
        writeHeaderLine(String.format(Locale.US,
                                      ""),
                        "GLONASS COD/PHS/BIS");
        writeHeaderLine(String.format(Locale.US,
                                      ""),
                        "OBS SCALE FACTOR");
        writeHeaderLine(String.format(Locale.US,
                                      ""),
                        "DOI");
        writeHeaderLine(String.format(Locale.US,
                                      ""),
                        "LICENSE OF USE");
        writeHeaderLine(String.format(Locale.US,
                                      ""),
                        "STATION INFORMATION");
        writeHeaderLine("", "END OF HEADER");

    }

    /** Write one observation data set.
     * @param observationDataSet observation data set to write
     * @exception IOException if an I/O error occurs.
     */
    public void writeObservationDataSet(final ObservationDataSet observationDataSet)
        throws IOException {

        // check header has already been written
        if (savedHeader == null) {
            throw new OrekitException(OrekitMessages.HEADER_NOT_WRITTEN, outputName);
        }

        // TODO

    }

    /** Write one header vector.
     * @param vector vector data (may be null)
     * @param label line label
     * @throws IOException if an I/O error occurs.
     */
    private void writeHeaderLine(final Vector3D vector, final CharSequence label) throws IOException {
        if (vector != null) {
            writeHeaderLine(String.format(Locale.US, THREE_FOURTEEN_DIGIT,
                                          vector.getX(), vector.getY(), vector.getZ()),
                            label);
        }
    }

    /** Write one header line.
     * @param extractor extractor for line data
     * @param label line label
     * @throws IOException if an I/O error occurs.
     */
    private void writeHeaderLine(final Supplier<String> extractor, final CharSequence label) throws IOException {
        final String data = extractor.get();
        writeHeaderLine(data == null ? "" : data, label);
    }

    /** Write one header line.
     * @param data data part of the line
     * @param label line label
     * @throws IOException if an I/O error occurs.
     */
    private void writeHeaderLine(final CharSequence data, final CharSequence label) throws IOException {
        output.append(data);
        for (int i = data.length(); i < LABEL_INDEX; ++i) {
            output.append(' ');
        }
        output.append(label);
    }

}
