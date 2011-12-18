/* Licensed to CS Communication & Systèmes (CS) under one or more
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
package org.orekit.files.sp3;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Scanner;

import org.apache.commons.math.geometry.euclidean.threed.Vector3D;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.OrbitFileParser;
import org.orekit.files.SatelliteInformation;
import org.orekit.files.SatelliteTimeCoordinate;
import org.orekit.files.OrbitFile.TimeSystem;
import org.orekit.files.sp3.SP3File.FileType;
import org.orekit.files.sp3.SP3File.OrbitType;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.PVCoordinates;

/** A parser for the SP3 orbit file format. It supports the original format as
 * well as the latest SP3-c version.
 * <p>
 * <b>Note:</b> this parser is thread-safe, so calling {@link #parse} from
 * different threads is allowed.
 * </p>
 * @see http://igscb.jpl.nasa.gov/igscb/data/format/sp3_docu.txt
 * @see http://igscb.jpl.nasa.gov/igscb/data/format/sp3c.txt
 * @author Thomas Neidhart
 */
public class SP3Parser implements OrbitFileParser {

    /** {@inheritDoc} */
    public SP3File parse(final String fileName) throws OrekitException {
        try {
            return parse(new FileInputStream(fileName));
        } catch (FileNotFoundException e) {
            // TODO: use a different error message, this seems to be used for loading 
            //       common property file instead of user-defined files.
            throw new OrekitException(OrekitMessages.UNABLE_TO_FIND_FILE, fileName);
        }
    }

    /** {@inheritDoc} */
    public SP3File parse(final InputStream stream) throws OrekitException {
        try {
            return parseInternal(stream);
        } catch (IOException e) {
            // TODO: use a more meaningful error message
            throw new OrekitException(OrekitMessages.INTERNAL_ERROR);
        }
    }

    /** Parses the SP3 file from the given {@link InputStream} and
     * returns a {@link SP3File} object.
     * @param stream the stream to read the SP3File from
     * @return the parsed {@link SP3File} object
     * @throws OrekitException if the file could not be parsed successfully
     * @throws IOException if an error occurs while reading from the stream
     */
    private SP3File parseInternal(final InputStream stream)
        throws OrekitException, IOException {

        BufferedReader reader = new BufferedReader(
                new InputStreamReader(stream));

        // initialize internal data structures
        ParseInfo pi = new ParseInfo();

        String line = null;
        int lineNumber = 1;
        try {
            while (lineNumber < 23) {
                line = reader.readLine();
                parseHeaderLine(lineNumber++, line, pi);
            }

            // now handle the epoch/position/velocity entries

            boolean done = false;
            do {
                line = reader.readLine();
                if (line != null) {
                    if ("EOF".equalsIgnoreCase(line)) {
                        done = true;
                    } else if (line.length() > 0) {
                        parseContentLine(line, pi);
                    }
                }
            } while (!done);
        } finally {
            try {
                reader.close();
            } catch (IOException e1) {
                // ignore
            }
        }

        return pi.file;
    }

    /** Parses a header line from the SP3 file (line number 1 - 22).
     * @param lineNumber the current line number
     * @param line the line as read from the SP3 file
     * @param pi the current {@link ParseInfo} object
     * @throws OrekitException
     */
    private void parseHeaderLine(final int lineNumber, final String line, final ParseInfo pi)
        throws OrekitException {

        SP3File file = pi.file;
        Scanner scanner = new Scanner(line).useDelimiter("\\s+");

        switch (lineNumber) {
        // version, epoch, data used and agency information
        case 1: {
            scanner.skip("#");
            String v = scanner.next();

            char version = v.substring(0, 1).toLowerCase().charAt(0);
            if (version != 'a' && version != 'b' && version != 'c') {
                // TODO: use a more meaningful error message
                throw new OrekitException(OrekitMessages.INTERNAL_ERROR);
            }

            pi.hasVelocityEntries = "V".equals(v.substring(1, 2));

            int year = Integer.valueOf(v.substring(2));
            int month = scanner.nextInt();
            int day = scanner.nextInt();
            int hour = scanner.nextInt();
            int minute = scanner.nextInt();
            double second = scanner.nextDouble();

            AbsoluteDate epoch = new AbsoluteDate(year, month, day, hour,
                    minute, second, TimeScalesFactory.getGPS());

            file.setEpoch(epoch);

            int numEpochs = scanner.nextInt();
            file.setNumberOfEpochs(numEpochs);

            // data used indicator
            file.setDataUsed(scanner.next());

            file.setCoordinateSystem(scanner.next());
            file.setOrbitType(OrbitType.valueOf(scanner.next()));
            file.setAgency(scanner.next());
        }
            break;

        // additional date/time references in gps/julian day notation
        case 2: {
            scanner.skip("##");

            // gps week
            file.setGpsWeek(scanner.nextInt());
            // seconds of week
            file.setSecondsOfWeek(scanner.nextDouble());
            // epoch interval
            file.setEpochInterval(scanner.nextDouble());
            // julian day
            file.setJulianDay(scanner.nextInt());
            // day fraction
            file.setDayFraction(scanner.nextDouble());
        }
            break;

        // line 3 contains the number of satellites
        case 3:
            pi.maxSatellites = Integer.valueOf(line.substring(4, 6).trim());
            // fall-through intended - the line contains already the first
            // entries

            // the following 4 lines contain additional satellite ids
        case 4:
        case 5:
        case 6:
        case 7: {
            int lineLength = line.length();
            int count = file.getSatelliteCount(), startIdx = 9;
            while (count++ < pi.maxSatellites && (startIdx + 3) <= lineLength) {
                String satId = line.substring(startIdx, startIdx + 3).trim();
                file.addSatellite(satId);
                startIdx += 3;
            }
        }
            break;

        // the following 5 lines contain general accuracy information for each
        // satellite
        case 8:
        case 9:
        case 10:
        case 11:
        case 12: {
            int lineLength = line.length();
            int satIdx = (lineNumber - 8) * 17, startIdx = 9;
            while (satIdx < pi.maxSatellites && (startIdx + 3) <= lineLength) {
                SatelliteInformation satInfo = file.getSatellite(satIdx++);
                int exponent = Integer.valueOf(line.substring(startIdx,
                        startIdx + 3).trim());
                // the accuracy is calculated as 2**exp (in m) -> can be safely
                // converted to an integer as
                // there will be no fraction
                satInfo.setAccuracy((int) Math.pow(2d, exponent));
                startIdx += 3;
            }
        }
            break;

        case 13: {
            file.setType(getFileType(line.substring(3, 5).trim()));

            // now identify the time system in use
            String tsStr = line.substring(9, 12).trim();
            TimeSystem ts = TimeSystem.GPS;
            if (!tsStr.equalsIgnoreCase("ccc")) {
                TimeSystem.valueOf(tsStr);
            }
            file.setTimeSystem(ts);

            switch (ts) {
            case GPS:
                pi.timeScale = TimeScalesFactory.getGPS();
                break;

            case GAL:
                pi.timeScale = TimeScalesFactory.getGST();
                break;

            case GLO:
            case QZS:
                // TODO: use a more meaningful error message
                throw new OrekitException(OrekitMessages.INTERNAL_ERROR);

            case TAI:
                pi.timeScale = TimeScalesFactory.getTAI();
                break;

            case UTC:
                pi.timeScale = TimeScalesFactory.getUTC();
                break;
            }
        }
            break;

        case 14:
            // ignore additional custom fields
            break;

        // load base numbers for the standard deviations of
        // position/velocity/clock components
        case 15: {
            String base = line.substring(3, 13).trim();
            if (!base.equals("0.0000000")) {
                // (mm or 10**-4 mm/sec)
                pi.posVelBase = Double.valueOf(base);
            }

            base = line.substring(14, 26).trim();
            if (!base.equals("0.000000000")) {
                // (psec or 10**-4 psec/sec)
                pi.clockBase = Double.valueOf(base);
            }
        }
            break;

        case 16:
        case 17:
        case 18:
            // ignore additional custom parameters
            break;

        case 19:
        case 20:
        case 21:
        case 22:
            // ignore comment lines
            break;
        default:
            // ignore -> method should only be called up to line 22
            break;
        }
    }

    /** Parses a single content line as read from the SP3 file.
     * @param line a string containing the line
     * @param pi the current {@link ParseInfo} object
     */
    private void parseContentLine(final String line, final ParseInfo pi) {
        // TODO: EP and EV lines are ignored so far

        SP3File file = pi.file;

        switch (line.charAt(0)) {
        case '*': {
            int year = Integer.valueOf(line.substring(3, 7).trim());
            int month = Integer.valueOf(line.substring(8, 10).trim());
            int day = Integer.valueOf(line.substring(11, 13).trim());
            int hour = Integer.valueOf(line.substring(14, 16).trim());
            int minute = Integer.valueOf(line.substring(17, 19).trim());
            double second = Double.valueOf(line.substring(20, 31).trim());

            pi.latestEpoch = new AbsoluteDate(year, month, day,
                                              hour, minute, second,
                                              pi.timeScale);
        }
            break;

        case 'P': {
            String satelliteId = line.substring(1, 4).trim();

            if (!file.containsSatellite(satelliteId)) {
                // TODO: consider writing a debug log message
                pi.latestPosition = null;
            } else {
                double x = Double.valueOf(line.substring(4, 18).trim());
                double y = Double.valueOf(line.substring(18, 32).trim());
                double z = Double.valueOf(line.substring(32, 46).trim());

                pi.latestPosition = new Vector3D(x, y, z);

                // clock (microsec)
                pi.latestClock = Double.valueOf(line.substring(46, 60).trim());

                // TODO: the additional items are optional and not read yet

                // if (line.length() >= 73) {
                // // x-sdev (b**n mm)
                // int xStdDevExp = Integer.valueOf(line.substring(61,
                // 63).trim());
                // // y-sdev (b**n mm)
                // int yStdDevExp = Integer.valueOf(line.substring(64,
                // 66).trim());
                // // z-sdev (b**n mm)
                // int zStdDevExp = Integer.valueOf(line.substring(67,
                // 69).trim());
                // // c-sdev (b**n psec)
                // int cStdDevExp = Integer.valueOf(line.substring(70,
                // 73).trim());
                //
                // pi.posStdDevRecord =
                // new PositionStdDevRecord(Math.pow(pi.posVelBase, xStdDevExp),
                // Math.pow(pi.posVelBase,
                // yStdDevExp), Math.pow(pi.posVelBase, zStdDevExp),
                // Math.pow(pi.clockBase, cStdDevExp));
                //
                // String clockEventFlag = line.substring(74, 75);
                // String clockPredFlag = line.substring(75, 76);
                // String maneuverFlag = line.substring(78, 79);
                // String orbitPredFlag = line.substring(79, 80);
                // }

                if (!pi.hasVelocityEntries) {
                    SatelliteTimeCoordinate coord =
                        new SatelliteTimeCoordinate(pi.latestEpoch,
                                                    pi.latestPosition,
                                                    pi.latestClock);
                    file.addSatelliteCoordinate(satelliteId, coord);
                }
            }
        }
            break;

        case 'V': {
            String satelliteId = line.substring(1, 4).trim();

            if (!file.containsSatellite(satelliteId)) {
                // TODO: consider writing a debug log message
            } else {
                double xv = Double.valueOf(line.substring(4, 18).trim());
                double yv = Double.valueOf(line.substring(18, 32).trim());
                double zv = Double.valueOf(line.substring(32, 46).trim());

                // the velocity values are in dm/s and have to be converted to
                // m/s
                Vector3D velocity = new Vector3D(xv / 10d, yv / 10d, zv / 10d);

                double clockRateChange = Double.valueOf(line.substring(46, 60)
                        .trim());

                // TODO: the additional items are optional and not read yet

                // if (line.length() >= 73) {
                // // xvel-sdev (b**n 10**-4 mm/sec)
                // int xVstdDevExp = Integer.valueOf(line.substring(61,
                // 63).trim());
                // // yvel-sdev (b**n 10**-4 mm/sec)
                // int yVstdDevExp = Integer.valueOf(line.substring(64,
                // 66).trim());
                // // zvel-sdev (b**n 10**-4 mm/sec)
                // int zVstdDevExp = Integer.valueOf(line.substring(67,
                // 69).trim());
                // // clkrate-sdev (b**n 10**-4 psec/sec)
                // int clkStdDevExp = Integer.valueOf(line.substring(70,
                // 73).trim());
                // }

                PVCoordinates pv = new PVCoordinates(pi.latestPosition,
                                                     velocity);
                SatelliteTimeCoordinate coord =
                    new SatelliteTimeCoordinate(pi.latestEpoch,
                                                pv,
                                                pi.latestClock,
                                                clockRateChange);
                file.addSatelliteCoordinate(satelliteId, coord);
            }
        }
            break;
        }
    }

    /** Returns the {@link FileType} that corresponds to a given string
     * in a SP3 file.
     * @param fileType file type as string
     * @return file type as enum
     */
    private FileType getFileType(final String fileType) {
        FileType type = FileType.UNDEFINED;
        if ("G".equalsIgnoreCase(fileType)) {
            type = FileType.GPS;
        } else if ("M".equalsIgnoreCase(fileType)) {
            type = FileType.MIXED;
        } else if ("R".equalsIgnoreCase(fileType)) {
            type = FileType.GLONASS;
        } else if ("L".equalsIgnoreCase(fileType)) {
            type = FileType.LEO;
        } else if ("E".equalsIgnoreCase(fileType)) {
            type = FileType.GALILEO;
        } else if ("C".equalsIgnoreCase(fileType)) {
            type = FileType.COMPASS;
        } else if ("J".equalsIgnoreCase(fileType)) {
            type = FileType.QZSS;
        }
        return type;
    }

    /** Transient data used for parsing a sp3 file. The data is kept in a
     * separate data structure to make the parser thread-safe.
     * <p><b>Note</b>: The class intentionally does not provide accessor
     * methods, as it is only used internally for parsing a SP3 file.</p>
     */
    private static class ParseInfo {

        /** The corresponding SP3File object. */
        SP3File file;

        /** The latest epoch as read from the SP3 file. */
        AbsoluteDate latestEpoch;

        /** The latest position as read from the SP3 file. */
        Vector3D latestPosition;

        /** The latest clock value as read from the SP3 file. */
        double latestClock;

        /** Indicates if the SP3 file has velocity entries. */
        boolean hasVelocityEntries;

        /** The timescale used in the SP3 file. */
        TimeScale timeScale;

        /** The number of satellites as contained in the SP3 file. */
        int maxSatellites;

        @SuppressWarnings("unused")
        double posVelBase;
        @SuppressWarnings("unused")
        double clockBase;

        public ParseInfo() {
            file = new SP3File();
            latestEpoch = null;
            latestPosition = null;
            latestClock = 0.0d;
            hasVelocityEntries = false;
            timeScale = TimeScalesFactory.getGPS();
            maxSatellites = 0;
            posVelBase = 2d;
            clockBase = 2d;
        }
    }
}
