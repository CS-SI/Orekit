/* Copyright 2002-2015 CS Systèmes d'Information
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
package org.orekit.frames;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.math3.exception.util.LocalizedFormats;
import org.orekit.data.DataLoader;
import org.orekit.data.DataProvidersManager;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

/** Loader for IERS rapid data and prediction file in XML format (finals file).
 * <p>Rapid data and prediction file contain {@link EOPEntry
 * Earth Orientation Parameters} for several years periods, in one file
 * only that is updated regularly.</p>
 * <p>The XML EOP files are recognized thanks to their base names, which
 * must match one of the the patterns <code>finals.2000A.*.xml</code> or
 * <code>finals.*.xml</code> (or the same ending with <code>.gz</code> for
 * gzip-compressed files) where * stands for a word like "all", "daily",
 * or "data".</p>
 * <p>Files containing data (back to 1973) are available at IERS web site: <a
 * href="http://www.iers.org/IERS/EN/DataProducts/EarthOrientationData/eop.html">Earth orientation data</a>.</p>
 * <p>
 * This class is immutable and hence thread-safe
 * </p>
 * @author Luc Maisonobe
 */
class RapidDataAndPredictionXMLLoader implements EOPHistoryLoader {

    /** Conversion factor for milli-arc seconds entries. */
    private static final double MILLI_ARC_SECONDS_TO_RADIANS = Constants.ARC_SECONDS_TO_RADIANS / 1000.0;

    /** Conversion factor for milli seconds entries. */
    private static final double MILLI_SECONDS_TO_SECONDS = 1.0 / 1000.0;

    /** Regular expression for supported files names. */
    private final String supportedNames;

    /** Build a loader for IERS XML EOP files.
     * @param supportedNames regular expression for supported files names
     */
    RapidDataAndPredictionXMLLoader(final String supportedNames) {
        this.supportedNames = supportedNames;
    }

    /** {@inheritDoc} */
    public void fillHistory(final IERSConventions.NutationCorrectionConverter converter,
                            final SortedSet<EOPEntry> history)
        throws OrekitException {
        final Parser parser = new Parser(converter);
        DataProvidersManager.getInstance().feed(supportedNames, parser);
        history.addAll(parser.history);
    }

    /** Internal class performing the parsing. */
    private static class Parser implements DataLoader {

        /** Converter for nutation corrections. */
        private final IERSConventions.NutationCorrectionConverter converter;

        /** History entries. */
        private final List<EOPEntry> history;

        /** Simple constructor.
         * @param converter converter to use
         */
        Parser(final IERSConventions.NutationCorrectionConverter converter) {
            this.converter = converter;
            this.history   = new ArrayList<EOPEntry>();
        }

        /** {@inheritDoc} */
        public boolean stillAcceptsData() {
            return true;
        }

        /** {@inheritDoc} */
        public void loadData(final InputStream input, final String name)
            throws IOException, OrekitException {
            try {
                // set up a reader for line-oriented bulletin B files
                final XMLReader reader = SAXParserFactory.newInstance().newSAXParser().getXMLReader();
                reader.setContentHandler(new EOPContentHandler(name));

                // read all file, ignoring header
                reader.parse(new InputSource(new InputStreamReader(input, "UTF-8")));

            } catch (SAXException se) {
                if ((se.getCause() != null) && (se.getCause() instanceof OrekitException)) {
                    throw (OrekitException) se.getCause();
                }
                throw new OrekitException(se, LocalizedFormats.SIMPLE_MESSAGE, se.getMessage());
            } catch (ParserConfigurationException pce) {
                throw new OrekitException(pce, LocalizedFormats.SIMPLE_MESSAGE, pce.getMessage());
            }
        }

        /** Local content handler for XML EOP files. */
        private class EOPContentHandler extends DefaultHandler {

            // CHECKSTYLE: stop JavadocVariable check

            // elements and attributes used in both daily and finals data files
            private static final String MJD_ELT           = "MJD";
            private static final String LOD_ELT           = "LOD";
            private static final String X_ELT             = "X";
            private static final String Y_ELT             = "Y";
            private static final String DPSI_ELT          = "dPsi";
            private static final String DEPSILON_ELT      = "dEpsilon";
            private static final String DX_ELT            = "dX";
            private static final String DY_ELT            = "dY";

            // elements and attributes specific to daily data files
            private static final String DATA_EOP_ELT      = "dataEOP";
            private static final String TIME_SERIES_ELT   = "timeSeries";
            private static final String DATE_YEAR_ELT     = "dateYear";
            private static final String DATE_MONTH_ELT    = "dateMonth";
            private static final String DATE_DAY_ELT      = "dateDay";
            private static final String POLE_ELT          = "pole";
            private static final String UT_ELT            = "UT";
            private static final String UT1_U_UTC_ELT     = "UT1_UTC";
            private static final String NUTATION_ELT      = "nutation";
            private static final String SOURCE_ATTR       = "source";
            private static final String BULLETIN_A_SOURCE = "BulletinA";

            // elements and attributes specific to finals data files
            private static final String FINALS_ELT        = "Finals";
            private static final String DATE_ELT          = "date";
            private static final String EOP_SET_ELT       = "EOPSet";
            private static final String BULLETIN_A_ELT    = "bulletinA";
            private static final String UT1_M_UTC_ELT     = "UT1-UTC";

            private boolean inBulletinA;
            private int     year;
            private int     month;
            private int     day;
            private int     mjd;
            private AbsoluteDate mjdDate;
            private double  dtu1;
            private double  lod;
            private double  x;
            private double  y;
            private double  dpsi;
            private double  deps;
            private double  dx;
            private double  dy;

            // CHECKSTYLE: resume JavadocVariable check

            /** File name. */
            private final String name;

            /** Buffer for read characters. */
            private final StringBuffer buffer;

            /** Indicator for daily data XML format or final data XML format. */
            private DataFileContent content;

            /** Simple constructor.
             * @param name file name
             */
            EOPContentHandler(final String name) {
                this.name = name;
                buffer  = new StringBuffer();
            }

            /** {@inheritDoc} */
            @Override
            public void startDocument() {
                content = DataFileContent.UNKNOWN;
            }

            /** {@inheritDoc} */
            @Override
            public void characters(final char[] ch, final int start, final int length) {
                buffer.append(ch, start, length);
            }

            /** {@inheritDoc} */
            @Override
            public void startElement(final String uri, final String localName,
                                     final String qName, final Attributes atts) {

                // reset the buffer to empty
                buffer.delete(0, buffer.length());

                if (content == DataFileContent.UNKNOWN) {
                    // try to identify file content
                    if (qName.equals(TIME_SERIES_ELT)) {
                        // the file contains final data
                        content = DataFileContent.DAILY;
                    } else if (qName.equals(FINALS_ELT)) {
                        // the file contains final data
                        content = DataFileContent.FINAL;
                    }
                }

                if (content == DataFileContent.DAILY) {
                    startDailyElement(qName, atts);
                } else if (content == DataFileContent.FINAL) {
                    startFinalElement(qName, atts);
                }

            }

            /** Handle end of an element in a daily data file.
             * @param qName name of the element
             * @param atts element attributes
             */
            private void startDailyElement(final String qName, final Attributes atts) {
                if (qName.equals(TIME_SERIES_ELT)) {
                    // reset EOP data
                    resetEOPData();
                } else if (qName.equals(POLE_ELT) || qName.equals(UT_ELT) || qName.equals(NUTATION_ELT)) {
                    final String source = atts.getValue(SOURCE_ATTR);
                    if (source != null) {
                        inBulletinA = source.equals(BULLETIN_A_SOURCE);
                    }
                }
            }

            /** Handle end of an element in a final data file.
             * @param qName name of the element
             * @param atts element attributes
             */
            private void startFinalElement(final String qName, final Attributes atts) {
                if (qName.equals(EOP_SET_ELT)) {
                    // reset EOP data
                    resetEOPData();
                } else if (qName.equals(BULLETIN_A_ELT)) {
                    inBulletinA = true;
                }
            }

            /** Reset EOP data.
             */
            private void resetEOPData() {
                inBulletinA = false;
                year        = -1;
                month       = -1;
                day         = -1;
                mjd         = -1;
                mjdDate     = null;
                dtu1        = Double.NaN;
                lod         = Double.NaN;
                x           = Double.NaN;
                y           = Double.NaN;
                dpsi        = Double.NaN;
                deps        = Double.NaN;
                dx          = Double.NaN;
                dy          = Double.NaN;
            }

            /** {@inheritDoc} */
            @Override
            public void endElement(final String uri, final String localName, final String qName)
                throws SAXException {
                try {
                    if (content == DataFileContent.DAILY) {
                        endDailyElement(qName);
                    } else if (content == DataFileContent.FINAL) {
                        endFinalElement(qName);
                    }
                } catch (OrekitException oe) {
                    throw new SAXException(oe);
                }
            }

            /** Handle end of an element in a daily data file.
             * @param qName name of the element
             * @exception OrekitException if an EOP element cannot be built
             */
            private void endDailyElement(final String qName) throws OrekitException {
                if (qName.equals(DATE_YEAR_ELT) && (buffer.length() > 0)) {
                    year = Integer.parseInt(buffer.toString());
                } else if (qName.equals(DATE_MONTH_ELT) && (buffer.length() > 0)) {
                    month = Integer.parseInt(buffer.toString());
                } else if (qName.equals(DATE_DAY_ELT) && (buffer.length() > 0)) {
                    day = Integer.parseInt(buffer.toString());
                } else if (qName.equals(MJD_ELT) && (buffer.length() > 0)) {
                    mjd     = Integer.parseInt(buffer.toString());
                    mjdDate = new AbsoluteDate(new DateComponents(DateComponents.MODIFIED_JULIAN_EPOCH, mjd),
                                               TimeScalesFactory.getUTC());
                } else if (qName.equals(UT1_M_UTC_ELT)) {
                    dtu1 = overwrite(dtu1, 1.0);
                } else if (qName.equals(LOD_ELT)) {
                    lod = overwrite(lod, MILLI_SECONDS_TO_SECONDS);
                } else if (qName.equals(X_ELT)) {
                    x = overwrite(x, Constants.ARC_SECONDS_TO_RADIANS);
                } else if (qName.equals(Y_ELT)) {
                    y = overwrite(y, Constants.ARC_SECONDS_TO_RADIANS);
                } else if (qName.equals(DPSI_ELT)) {
                    dpsi = overwrite(dpsi, MILLI_ARC_SECONDS_TO_RADIANS);
                } else if (qName.equals(DEPSILON_ELT)) {
                    deps = overwrite(deps, MILLI_ARC_SECONDS_TO_RADIANS);
                } else if (qName.equals(DX_ELT)) {
                    dx   = overwrite(dx, MILLI_ARC_SECONDS_TO_RADIANS);
                } else if (qName.equals(DY_ELT)) {
                    dy   = overwrite(dy, MILLI_ARC_SECONDS_TO_RADIANS);
                } else if (qName.equals(POLE_ELT) || qName.equals(UT_ELT) || qName.equals(NUTATION_ELT)) {
                    inBulletinA = false;
                } else if (qName.equals(DATA_EOP_ELT)) {
                    checkDates();
                    if ((!Double.isNaN(dtu1)) && (!Double.isNaN(lod)) && (!Double.isNaN(x)) && (!Double.isNaN(y))) {
                        final double[] equinox;
                        final double[] nro;
                        if (Double.isNaN(dpsi)) {
                            nro = new double[] {
                                dx, dy
                            };
                            equinox = converter.toEquinox(mjdDate, nro[0], nro[1]);
                        } else {
                            equinox = new double[] {
                                dpsi, deps
                            };
                            nro = converter.toNonRotating(mjdDate, equinox[0], equinox[1]);
                        }
                        history.add(new EOPEntry(mjd, dtu1, lod, x, y, equinox[0], equinox[1], nro[0], nro[1]));
                    }
                }
            }

            /** Handle end of an element in a final data file.
             * @param qName name of the element
             * @exception OrekitException if an EOP element cannot be built
             */
            private void endFinalElement(final String qName) throws OrekitException {
                if (qName.equals(DATE_ELT) && (buffer.length() > 0)) {
                    final String[] fields = buffer.toString().split("-");
                    if (fields.length == 3) {
                        year  = Integer.parseInt(fields[0]);
                        month = Integer.parseInt(fields[1]);
                        day   = Integer.parseInt(fields[2]);
                    }
                } else if (qName.equals(MJD_ELT) && (buffer.length() > 0)) {
                    mjd     = Integer.parseInt(buffer.toString());
                    mjdDate = new AbsoluteDate(new DateComponents(DateComponents.MODIFIED_JULIAN_EPOCH, mjd),
                                               TimeScalesFactory.getUTC());
                } else if (qName.equals(UT1_U_UTC_ELT)) {
                    dtu1 = overwrite(dtu1, 1.0);
                } else if (qName.equals(LOD_ELT)) {
                    lod = overwrite(lod, MILLI_SECONDS_TO_SECONDS);
                } else if (qName.equals(X_ELT)) {
                    x = overwrite(x, Constants.ARC_SECONDS_TO_RADIANS);
                } else if (qName.equals(Y_ELT)) {
                    y = overwrite(y, Constants.ARC_SECONDS_TO_RADIANS);
                } else if (qName.equals(DPSI_ELT)) {
                    dpsi = overwrite(dpsi, MILLI_ARC_SECONDS_TO_RADIANS);
                } else if (qName.equals(DEPSILON_ELT)) {
                    deps = overwrite(deps, MILLI_ARC_SECONDS_TO_RADIANS);
                } else if (qName.equals(DX_ELT)) {
                    dx   = overwrite(dx, MILLI_ARC_SECONDS_TO_RADIANS);
                } else if (qName.equals(DY_ELT)) {
                    dy   = overwrite(dy, MILLI_ARC_SECONDS_TO_RADIANS);
                } else if (qName.equals(BULLETIN_A_ELT)) {
                    inBulletinA = false;
                } else if (qName.equals(EOP_SET_ELT)) {
                    checkDates();
                    if ((!Double.isNaN(dtu1)) && (!Double.isNaN(lod)) && (!Double.isNaN(x)) && (!Double.isNaN(y))) {
                        final double[] equinox;
                        final double[] nro;
                        if (Double.isNaN(dpsi)) {
                            nro = new double[] {
                                dx, dy
                            };
                            equinox = converter.toEquinox(mjdDate, nro[0], nro[1]);
                        } else {
                            equinox = new double[] {
                                dpsi, deps
                            };
                            nro = converter.toNonRotating(mjdDate, equinox[0], equinox[1]);
                        }
                        history.add(new EOPEntry(mjd, dtu1, lod, x, y, equinox[0], equinox[1], nro[0], nro[1]));
                    }
                }
            }

            /** Overwrite a value if it is not set or if we are in a bulletinB.
             * @param oldValue old value to overwrite (may be NaN)
             * @param factor multiplicative factor to apply to raw read data
             * @return a new value
             */
            private double overwrite(final double oldValue, final double factor) {
                if (buffer.length() == 0) {
                    // there is nothing to overwrite with
                    return oldValue;
                } else if (inBulletinA && (!Double.isNaN(oldValue))) {
                    // the value is already set and bulletin A values have a low priority
                    return oldValue;
                } else {
                    // either the value is not set or it is a high priority bulletin B value
                    return Double.parseDouble(buffer.toString()) * factor;
                }
            }

            /** Check if the year, month, day date and MJD date are consistent.
             * @exception OrekitException if dates are not consistent
             */
            private void checkDates() throws OrekitException {
                if (new DateComponents(year, month, day).getMJD() != mjd) {
                    throw new OrekitException(OrekitMessages.INCONSISTENT_DATES_IN_IERS_FILE,
                                              name, year, month, day, mjd);
                }
            }

        }

        /** Enumerate for data file content. */
        private enum DataFileContent {

            /** Unknown content. */
            UNKNOWN,

            /** Daily data. */
            DAILY,

            /** Final data. */
            FINAL;

        }

    }

}
