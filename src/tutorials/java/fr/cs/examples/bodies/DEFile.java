/* Copyright 2002-2016 CS Systèmes d'Information
 * Licensed to CS Systèmes d'Information (CS) under one or more
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
package fr.cs.examples.bodies;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.hipparchus.util.FastMath;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitInternalError;
import org.orekit.errors.OrekitMessages;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.DateTimeComponents;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;

import fr.cs.examples.Autoconfiguration;

public class DEFile {

    private static final int INPOP_DE_NUMBER                = 100;
    private static final int CONSTANTS_MAX_NUMBER           = 400;

    private static final int HEADER_LABEL_SIZE              = 84;
    private static final int HEADER_LABEL_1_OFFSET          = 0;
    private static final int HEADER_LABEL_2_OFFSET          = HEADER_LABEL_1_OFFSET + HEADER_LABEL_SIZE;
    private static final int HEADER_LABEL_3_OFFSET          = HEADER_LABEL_2_OFFSET + HEADER_LABEL_SIZE;
    private static final int HEADER_EPHEMERIS_TYPE_OFFSET   = 2840;
    private static final int HEADER_RECORD_SIZE_OFFSET      = 2856;
    private static final int HEADER_START_EPOCH_OFFSET      = 2652;
    private static final int HEADER_END_EPOCH_OFFSET        = 2660;
    private static final int HEADER_CONSTANTS_NAMES_OFFSET  = 252;
    private static final int HEADER_CONSTANTS_VALUES_OFFSET = 0;
    private static final int DATA_START_RANGE_OFFSET        = 0;
    private static final int DATE_END_RANGE_OFFSET          = 8;

    private String       inName;
    private String       outName;
    private InputStream  input;
    private byte[]       first;
    private byte[]       second;
    private List<byte[]> selected;
    private int          recordSize;
    private boolean      bigEndian;
    private int          deNum;
    private String       label1;
    private String       label2;
    private String       label3;
    private AbsoluteDate headerStartEpoch;
    private AbsoluteDate headerFinalEpoch;
    private TimeScale    timeScale;
    private final Map<String, Double> headerConstants;

    /** Program entry point.
     * @param args program arguments (unused here)
     */
    public static void main(String[] args) {
        try {
            Autoconfiguration.configureOrekit();
            String inName  = null;
            String outName = null;
            List<String> constants = new ArrayList<String>();
            boolean allConstants = false;
            AbsoluteDate start = AbsoluteDate.PAST_INFINITY;
            AbsoluteDate end   = AbsoluteDate.FUTURE_INFINITY;
            for (int i = 0; i < args.length; ++i) {
                if ("-in".equals(args[i])) {
                    inName = args[++i];
                } else if ("-help".equals(args[i])) {
                    displayUsage(System.out);
                } else if ("-constant".equals(args[i])) {
                    constants.add(args[++i]);
                } else if ("-all-constants".equals(args[i])) {
                    allConstants = true;
                } else if ("-start".equals(args[i])) {
                    start = new AbsoluteDate(args[++i], TimeScalesFactory.getUTC());
                } else if ("-end".equals(args[i])) {
                    end = new AbsoluteDate(args[++i], TimeScalesFactory.getUTC());
                } else if ("-out".equals(args[i])) {
                    outName = args[++i];
                } else {
                    System.err.println("unknown command line option \"" + args[i] + "\"");
                    displayUsage(System.err);
                    System.exit(1);
                }
            }

            if (inName == null) {
                displayUsage(System.err);
                System.exit(1);
            }
            DEFile de = new DEFile(inName, outName);

            de.processHeader();
            System.out.println("header label 1     " + de.label1);
            System.out.println("header label 2     " + de.label2);
            System.out.println("header label 3     " + de.label3);
            System.out.println("header start epoch " + de.headerStartEpoch.toString(de.timeScale) +
                               " (" + de.timeScale.getName() + ")");
            System.out.println("header end epoch   " + de.headerFinalEpoch.toString(de.timeScale) +
                               " (" + de.timeScale.getName() + ")");

            for (String constant : constants) {
                Double value = de.headerConstants.get(constant);
                System.out.println(constant + "     " + ((value == null) ? "not present" : value));
            }

            if (allConstants) {
                for (Map.Entry<String,Double> entry : de.headerConstants.entrySet()) {
                    System.out.println(entry.getKey() + "     " + entry.getValue());
                }
            }

            int processed = de.processData(start, end);
            System.out.println("data records: " + processed);

            if (outName != null) {
                de.write();
                System.out.println(outName + " file created with " +
                                   de.selected.size() + " selected data records");
            }

        } catch (IOException ioe) {
            ioe.printStackTrace(System.err);
        } catch (OrekitException oe) {
            oe.printStackTrace(System.err);
        }
    }

    private static void displayUsage(final PrintStream stream) {
        stream.print("usage: java DEFile");
        stream.print(" -in filename");
        stream.print(" [-help]");
        stream.print(" [-constant name]");
        stream.print(" [-all-constants]");
        stream.print(" [-start date]");
        stream.print(" [-end date]");
        stream.print(" [-out filename]");
        stream.println();
    }

    private DEFile(String inName, String outName) throws FileNotFoundException {
        this.inName          = inName;
        this.outName         = outName;
        this.input           = new FileInputStream(inName);
        this.headerConstants = new HashMap<String, Double>();
        this.selected        = new ArrayList<byte[]>();
    }

    private void processHeader() throws IOException, OrekitException {

        first = readFirstRecord();
        second = new byte[recordSize];
        readInRecord(second, 0);

        label1 = extractString(first, HEADER_LABEL_1_OFFSET, HEADER_LABEL_SIZE);
        label2 = extractString(first, HEADER_LABEL_2_OFFSET, HEADER_LABEL_SIZE);
        label3 = extractString(first, HEADER_LABEL_3_OFFSET, HEADER_LABEL_SIZE);

        // constants defined in the file
        for (int i = 0; i < CONSTANTS_MAX_NUMBER; ++i) {
            // Note: for extracting the strings from the binary file, it makes no difference
            //       if the file is stored in big-endian or little-endian notation
            final String constantName = extractString(first, HEADER_CONSTANTS_NAMES_OFFSET + i * 6, 6);
            if (constantName.length() == 0) {
                // no more constants to read
                break;
            }
            final double constantValue = extractDouble(second, HEADER_CONSTANTS_VALUES_OFFSET + 8 * i, bigEndian);
            headerConstants.put(constantName, constantValue);
        }

        final Double timesc = headerConstants.get("TIMESC");
        if (timesc != null && !Double.isNaN(timesc) && timesc.intValue() == 1) {
            timeScale = TimeScalesFactory.getTCB();
        } else {
            timeScale = TimeScalesFactory.getTDB();
        }

        headerStartEpoch = extractDate(first, HEADER_START_EPOCH_OFFSET, bigEndian);
        headerFinalEpoch = extractDate(first, HEADER_END_EPOCH_OFFSET, bigEndian);

    }

    private int processData(AbsoluteDate selectStart, AbsoluteDate selectEnd)
        throws IOException, OrekitException {

        byte[] data = new byte[recordSize];
        int processed = 0;
        while (readInRecord(data, 0)) {
            // extract time range covered by the record
            final AbsoluteDate rangeStart = extractDate(data, DATA_START_RANGE_OFFSET, bigEndian);
            final AbsoluteDate rangeEnd   = extractDate(data, DATE_END_RANGE_OFFSET,   bigEndian);
            if (rangeEnd.compareTo(selectStart) >= 0 && rangeStart.compareTo(selectEnd) <= 0) {
                selected.add(data.clone());
            }
            ++processed;
        }

        return processed;

    }

    private void write() throws IOException {

        if (!selected.isEmpty()) {
            if (outName != null) {
                try (OutputStream out = new FileOutputStream(outName)) {

                    // patch header epoch
                    System.arraycopy(selected.get(0), DATA_START_RANGE_OFFSET,
                                     first, HEADER_START_EPOCH_OFFSET,
                                     8);
                    System.arraycopy(selected.get(selected.size() - 1), DATE_END_RANGE_OFFSET,
                                     first, HEADER_END_EPOCH_OFFSET,
                                     8);

                    // patch header labels
                    final AbsoluteDate       start = extractDate(first, HEADER_START_EPOCH_OFFSET, bigEndian);
                    final DateTimeComponents sc    = start.getComponents(timeScale);
                    final AbsoluteDate       end   = extractDate(first, HEADER_END_EPOCH_OFFSET, bigEndian);
                    final DateTimeComponents ec    = end.getComponents(timeScale);
                    System.arraycopy(padString("THIS IS NOT A GENUINE JPL DE FILE," +
                                    " THIS IS AN EXCERPT WITH A LIMITED TIME RANGE",
                                    HEADER_LABEL_SIZE), 0,
                                     first, HEADER_LABEL_1_OFFSET,
                                     HEADER_LABEL_SIZE);
                    System.arraycopy(padString(String.format(Locale.US,
                                                             "Start Epoch: JED=  %.1f %4d-%s-%02d %02d:%02d:%02.0f",
                                                             (start.durationFrom(AbsoluteDate.JULIAN_EPOCH)) /
                                                             Constants.JULIAN_DAY,
                                                             sc.getDate().getYear(),
                                                             sc.getDate().getMonthEnum().getUpperCaseAbbreviation(),
                                                             sc.getDate().getDay(),
                                                             sc.getTime().getHour(),
                                                             sc.getTime().getMinute(),
                                                             sc.getTime().getSecond()),
                                               HEADER_LABEL_SIZE), 0,
                                     first, HEADER_LABEL_2_OFFSET,
                                     HEADER_LABEL_SIZE);
                    System.arraycopy(padString(String.format(Locale.US,
                                                             "Final Epoch: JED=  %.1f %4d-%s-%02d %02d:%02d:%02.0f",
                                                             (end.durationFrom(AbsoluteDate.JULIAN_EPOCH)) /
                                                             Constants.JULIAN_DAY,
                                                             ec.getDate().getYear(),
                                                             ec.getDate().getMonthEnum().getUpperCaseAbbreviation(),
                                                             ec.getDate().getDay(),
                                                             ec.getTime().getHour(),
                                                             ec.getTime().getMinute(),
                                                             ec.getTime().getSecond()),
                                               HEADER_LABEL_SIZE), 0,
                                     first, HEADER_LABEL_3_OFFSET,
                                     HEADER_LABEL_SIZE);

                    // write patched header
                    out.write(first);
                    out.write(second);

                    // write selected data
                    for (byte[] data : selected) {
                        out.write(data);
                    }
                }
            }
        }

    }

    private byte[] readFirstRecord()
        throws OrekitException, IOException {

        // read first part of record, up to the record number
        final byte[] firstPart = new byte[HEADER_RECORD_SIZE_OFFSET + 4];
        if (!readInRecord(firstPart, 0)) {
            throw new OrekitException(OrekitMessages.UNABLE_TO_READ_JPL_HEADER, inName);
        }

        // detect the endian format
        bigEndian = detectEndianess(firstPart);

        // get the ephemerides type
        deNum = extractInt(firstPart, HEADER_EPHEMERIS_TYPE_OFFSET, bigEndian);

        // the record size for this file
        recordSize = 0;

        if (deNum == INPOP_DE_NUMBER) {
            // INPOP files have an extended DE format, which includes also the record size
            recordSize = extractInt(firstPart, HEADER_RECORD_SIZE_OFFSET, bigEndian) << 3;
        } else {
            // compute the record size for original JPL files
            recordSize = computeRecordSize(firstPart, bigEndian, inName);
        }

        if (recordSize <= 0) {
            throw new OrekitException(OrekitMessages.UNABLE_TO_READ_JPL_HEADER, inName);
        }

        // build a record with the proper size and finish read of the first complete record
        final byte[] record = new byte[recordSize];
        System.arraycopy(firstPart, 0, record, 0, firstPart.length);
        if (!readInRecord(record, firstPart.length)) {
            throw new OrekitException(OrekitMessages.UNABLE_TO_READ_JPL_HEADER, inName);
        }

        return record;

    }

    private boolean readInRecord(final byte[] record, final int start)
        throws IOException {
        int index = start;
        while (index != record.length) {
            final int n = input.read(record, index, record.length - index);
            if (n < 0) {
                return false;
            }
            index += n;
        }
        return true;
    }

    /** Detect whether the JPL ephemerides file is stored in big-endian or
     * little-endian notation.
     * @param record the array containing the binary JPL header
     * @return <code>true</code> if the file is stored in big-endian,
     * <code>false</code> otherwise
     */
    private static boolean detectEndianess(final byte[] record) {

        // default to big-endian
        boolean bigEndian = true;

        // first try to read the DE number in big-endian format
        // the number is stored as unsigned int, so we have to convert it properly
        final long deNum = extractInt(record, HEADER_EPHEMERIS_TYPE_OFFSET, true) & 0xffffffffL;

        // simple heuristic: if the read value is larger than half the range of an integer
        //                   assume the file is in little-endian format
        if (deNum > (1 << 15)) {
            bigEndian = false;
        }

        return bigEndian;

    }

    /** Calculate the record size of a JPL ephemerides file.
     * @param record the byte array containing the header record
     * @param bigEndian indicates the endianess of the file
     * @param name the name of the data file
     * @return the record size for this file
     * @throws OrekitException if the file contains unexpected data
     */
    private static int computeRecordSize(final byte[] record,
                                         final boolean bigEndian,
                                         final String name)
        throws OrekitException {

        int recordSize = 0;
        boolean ok = true;
        // JPL files always have 3 position components
        final int nComp = 3;

        // iterate over the coefficient ptr array and sum up the record size
        // the coeffPtr array has the dimensions [12][nComp]
        for (int j = 0; j < 12; j++) {
            final int nCompCur = (j == 11) ? 2 : nComp;

            // the coeffPtr array starts at offset 2696
            // Note: the array element coeffPtr[j][0] is not needed for the calculation
            final int idx = 2696 + j * nComp * 4;
            final int coeffPtr1 = extractInt(record, idx + 4, bigEndian);
            final int coeffPtr2 = extractInt(record, idx + 8, bigEndian);

            // sanity checks
            ok = ok && (coeffPtr1 >= 0 || coeffPtr2 >= 0);

            recordSize += coeffPtr1 * coeffPtr2 * nCompCur;
        }

        // the libration ptr array starts at offset 2844 and has the dimension [3]
        // Note: the array element libratPtr[0] is not needed for the calculation
        final int libratPtr1 = extractInt(record, 2844 + 4, bigEndian);
        final int libratPtr2 = extractInt(record, 2844 + 8, bigEndian);

        // sanity checks
        ok = ok && (libratPtr1 >= 0 || libratPtr2 >= 0);

        recordSize += libratPtr1 * libratPtr2 * nComp + 2;
        recordSize <<= 3;

        if (!ok || recordSize <= 0) {
            throw new OrekitException(OrekitMessages.NOT_A_JPL_EPHEMERIDES_BINARY_FILE, name);
        }

        return recordSize;

    }

    /** Extract a date from a record.
     * @param record record to parse
     * @param offset offset of the double within the record
     * @param bigEndian if <code>true</code> the parsed date is extracted in big-endian
     * format, otherwise it is extracted in little-endian format
     * @return extracted date
     */
    private AbsoluteDate extractDate(final byte[] record,
                                     final int offset,
                                     final boolean bigEndian) {

        final double t = extractDouble(record, offset, bigEndian);
        int    jDay    = (int) FastMath.floor(t);
        double seconds = (t + 0.5 - jDay) * Constants.JULIAN_DAY;
        if (seconds >= Constants.JULIAN_DAY) {
            ++jDay;
            seconds -= Constants.JULIAN_DAY;
        }
        final AbsoluteDate date =
            new AbsoluteDate(new DateComponents(DateComponents.JULIAN_EPOCH, jDay),
                             new TimeComponents(seconds), timeScale);
        return date;
    }

    /** Extract a double from a record.
     * <p>Double numbers are stored according to IEEE 754 standard, with
     * most significant byte first.</p>
     * @param record record to parse
     * @param offset offset of the double within the record
     * @param bigEndian if <code>true</code> the parsed double is extracted in big-endian
     * format, otherwise it is extracted in little-endian format
     * @return extracted double
     */
    private static double extractDouble(final byte[] record, final int offset, final boolean bigEndian) {
        final long l8 = ((long) record[offset + 0]) & 0xffl;
        final long l7 = ((long) record[offset + 1]) & 0xffl;
        final long l6 = ((long) record[offset + 2]) & 0xffl;
        final long l5 = ((long) record[offset + 3]) & 0xffl;
        final long l4 = ((long) record[offset + 4]) & 0xffl;
        final long l3 = ((long) record[offset + 5]) & 0xffl;
        final long l2 = ((long) record[offset + 6]) & 0xffl;
        final long l1 = ((long) record[offset + 7]) & 0xffl;
        long l;
        if (bigEndian) {
            l = (l8 << 56) | (l7 << 48) | (l6 << 40) | (l5 << 32) |
                (l4 << 24) | (l3 << 16) | (l2 <<  8) | l1;
        } else {
            l = (l1 << 56) | (l2 << 48) | (l3 << 40) | (l4 << 32) |
                (l5 << 24) | (l6 << 16) | (l7 <<  8) | l8;
        }
        return Double.longBitsToDouble(l);
    }

    /** Extract an int from a record.
     * @param record record to parse
     * @param offset offset of the double within the record
     * @param bigEndian if <code>true</code> the parsed int is extracted in big-endian
     * format, otherwise it is extracted in little-endian format
     * @return extracted int
     */
    private static int extractInt(final byte[] record, final int offset, final boolean bigEndian) {
        final int l4 = ((int) record[offset + 0]) & 0xff;
        final int l3 = ((int) record[offset + 1]) & 0xff;
        final int l2 = ((int) record[offset + 2]) & 0xff;
        final int l1 = ((int) record[offset + 3]) & 0xff;

        if (bigEndian) {
            return (l4 << 24) | (l3 << 16) | (l2 <<  8) | l1;
        } else {
            return (l1 << 24) | (l2 << 16) | (l3 <<  8) | l4;
        }
    }

    /** Extract a String from a record.
     * @param record record to parse
     * @param offset offset of the string within the record
     * @param length maximal length of the string
     * @return extracted string, with whitespace characters stripped
     */
    private static String extractString(final byte[] record, final int offset, final int length) {
        try {
            return new String(record, offset, length, "US-ASCII").trim();
        } catch (UnsupportedEncodingException uee) {
            throw new OrekitInternalError(uee);
        }
    }

    /** Pad a string into a bytes array.
     * @param s string to pad
     * @param length length of the padded bytes array
     * @return padded bytes array
     */
    private static byte[] padString(final String s, final int length) {
        final Charset charSet = Charset.forName("US-ASCII");
        final byte[] array = new byte[length];
        Arrays.fill(array, charSet.encode(" ").get());
        final byte[] sb = charSet.encode(s).array();
        System.arraycopy(sb, 0, array, 0, FastMath.min(sb.length, array.length));
        return array;
    }

}
