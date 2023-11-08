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
package org.orekit.frames;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.SortedSet;
import java.util.function.Supplier;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.hipparchus.exception.LocalizedCoreFormats;
import org.orekit.data.DataProvidersManager;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.TimeScale;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.units.Unit;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/** Loader for IERS EOP data in XML format (finals and EOPC04 files).
 * <p>The XML EOP files are recognized thanks to their base names, which
 * must match one of the the patterns {@code finals.2000A.*.xml} or
 * {@code finals.*.xml} or {@code eopc04_*.xml} (or the same ending with
 * {@.gz} for gzip-compressed files) where * stands for any string of characters.</p>
 * <p>Files containing data (back to 1962) are available at IERS web site: <a
 * href="https://datacenter.iers.org/products/eop/">IERS https data download</a>.</p>
 * <p>
 * This class is immutable and hence thread-safe
 * </p>
 * @author Luc Maisonobe
 */
class EopXmlLoader extends AbstractEopLoader implements EopHistoryLoader {

    /** Millisecond unit. */
    private static final Unit MILLI_SECOND = Unit.parse("ms");

    /** Milli arcsecond unit. */
    private static final Unit MILLI_ARC_SECOND = Unit.parse("mas");

    /**Arcsecond per day unit.
     * @since 12.0
     */
    private static final Unit ARC_SECOND_PER_DAY = Unit.parse("as/day");

    /**
     * Build a loader for IERS XML EOP files.
     *
     * @param supportedNames regular expression for supported files names
     * @param manager        provides access to the XML EOP files.
     * @param utcSupplier    UTC time scale.
     */
    EopXmlLoader(final String supportedNames,
                 final DataProvidersManager manager,
                 final Supplier<TimeScale> utcSupplier) {
        super(supportedNames, manager, utcSupplier);
    }

    /** {@inheritDoc} */
    public void fillHistory(final IERSConventions.NutationCorrectionConverter converter,
                            final SortedSet<EOPEntry> history) {
        final ItrfVersionProvider itrfVersionProvider = new ITRFVersionLoader(
                ITRFVersionLoader.SUPPORTED_NAMES,
                getDataProvidersManager());
        final Parser parser = new Parser(converter, itrfVersionProvider, getUtc());
        final EopParserLoader loader = new EopParserLoader(parser);
        this.feed(loader);
        history.addAll(loader.getEop());
    }

    /** Internal class performing the parsing. */
    static class Parser extends AbstractEopParser {

        /** History entries. */
        private List<EOPEntry> history;

        /**
         * Simple constructor.
         *
         * @param converter           converter to use
         * @param itrfVersionProvider to use for determining the ITRF version of the EOP.
         * @param utc                 time scale for parsing dates.
         */
        Parser(final IERSConventions.NutationCorrectionConverter converter,
               final ItrfVersionProvider itrfVersionProvider,
               final TimeScale utc) {
            super(converter, itrfVersionProvider, utc);
        }

        /** {@inheritDoc} */
        @Override
        public Collection<EOPEntry> parse(final InputStream input, final String name)
            throws IOException, OrekitException {
            try {
                this.history = new ArrayList<>();
                // set up a parser for line-oriented bulletin B files
                final SAXParser parser = SAXParserFactory.newInstance().newSAXParser();

                // read all file, ignoring header
                parser.parse(new InputSource(new InputStreamReader(input, StandardCharsets.UTF_8)),
                             new EOPContentHandler(name));

                return history;

            } catch (SAXException | ParserConfigurationException e) {
                throw new OrekitException(e, LocalizedCoreFormats.SIMPLE_MESSAGE, e.getMessage());
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
            private static final String X_RATE_ELT        = "x_rate";
            private static final String Y_RATE_ELT        = "y_rate";
            private static final String DPSI_ELT          = "dPsi";
            private static final String DEPSILON_ELT      = "dEpsilon";
            private static final String DX_ELT            = "dX";
            private static final String DY_ELT            = "dY";

            // elements and attributes specific to bulletinA, bulletinB and EOP C04 files
            private static final String DATA_ELT            = "data";
            private static final String PRODUCT_ATTR        = "product";
            private static final String BULLETIN_A_PROD     = "BulletinA";
            private static final String BULLETIN_B_PROD     = "BulletinB";
            private static final String EOP_C04_PROD_PREFIX = "EOP";
            private static final String EOP_C04_PROD_SUFFIX = "C04";

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
            private double  xRate;
            private double  yRate;
            private double  dpsi;
            private double  deps;
            private double  dx;
            private double  dy;

            // CHECKSTYLE: resume JavadocVariable check

            /** File name. */
            private final String name;

            /** Buffer for read characters. */
            private final StringBuilder buffer;

            /** Indicator for daily data XML format or final data XML format. */
            private DataFileContent content;

            /** ITRF version configuration. */
            private ITRFVersionLoader.ITRFVersionConfiguration configuration;

            /** Simple constructor.
             * @param name file name
             */
            EOPContentHandler(final String name) {
                this.name   = name;
                this.buffer = new StringBuilder();
            }

            /** {@inheritDoc} */
            @Override
            public void startDocument() {
                content       = DataFileContent.UNKNOWN;
                configuration = null;
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
                    } else if (qName.equals(DATA_ELT)) {
                        final String product = atts.getValue(PRODUCT_ATTR);
                        if (product != null) {
                            if (product.startsWith(BULLETIN_A_PROD)) {
                                // the file contains bulletinA
                                content     = DataFileContent.BULLETIN_A;
                                inBulletinA = true;
                            } else if (product.startsWith(BULLETIN_B_PROD)) {
                                // the file contains bulletinB
                                content = DataFileContent.BULLETIN_B;
                            } else if (product.startsWith(EOP_C04_PROD_PREFIX) && product.endsWith(EOP_C04_PROD_SUFFIX)) {
                                // the file contains EOP C04
                                content = DataFileContent.EOP_C04;
                            }
                        }
                    }
                }

                if (content == DataFileContent.DAILY      || content == DataFileContent.BULLETIN_A ||
                    content == DataFileContent.BULLETIN_B || content == DataFileContent.EOP_C04) {
                    startDailyElement(qName, atts);
                } else if (content == DataFileContent.FINAL) {
                    startFinalElement(qName);
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
                        inBulletinA = source.equals(BULLETIN_A_PROD);
                    }
                }
            }

            /** Handle end of an element in a final data file.
             * @param qName name of the element
             */
            private void startFinalElement(final String qName) {
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
                xRate       = Double.NaN;
                yRate       = Double.NaN;
                dpsi        = Double.NaN;
                deps        = Double.NaN;
                dx          = Double.NaN;
                dy          = Double.NaN;
            }

            /** {@inheritDoc} */
            @Override
            public void endElement(final String uri, final String localName, final String qName) {
                if (content == DataFileContent.DAILY      || content == DataFileContent.BULLETIN_A ||
                    content == DataFileContent.BULLETIN_B || content == DataFileContent.EOP_C04) {
                    endDailyElement(qName);
                } else if (content == DataFileContent.FINAL) {
                    endFinalElement(qName);
                }
            }

            /** Handle end of an element in a daily data file.
             * @param qName name of the element
             */
            private void endDailyElement(final String qName) {
                if (qName.equals(DATE_YEAR_ELT) && buffer.length() > 0) {
                    year = Integer.parseInt(buffer.toString());
                } else if (qName.equals(DATE_MONTH_ELT) && buffer.length() > 0) {
                    month = Integer.parseInt(buffer.toString());
                } else if (qName.equals(DATE_DAY_ELT) && buffer.length() > 0) {
                    day = Integer.parseInt(buffer.toString());
                } else if (qName.equals(MJD_ELT) && buffer.length() > 0) {
                    mjd     = Integer.parseInt(buffer.toString());
                    mjdDate = new AbsoluteDate(new DateComponents(DateComponents.MODIFIED_JULIAN_EPOCH, mjd),
                                               getUtc());
                } else if (qName.equals(UT1_M_UTC_ELT)) {
                    dtu1 = overwrite(dtu1, Unit.SECOND);
                } else if (qName.equals(LOD_ELT)) {
                    lod = overwrite(lod, MILLI_SECOND);
                } else if (qName.equals(X_ELT)) {
                    x = overwrite(x, Unit.ARC_SECOND);
                } else if (qName.equals(Y_ELT)) {
                    y = overwrite(y, Unit.ARC_SECOND);
                } else if (qName.equals(X_RATE_ELT)) {
                    xRate = overwrite(xRate, ARC_SECOND_PER_DAY);
                } else if (qName.equals(Y_RATE_ELT)) {
                    yRate = overwrite(yRate, ARC_SECOND_PER_DAY);
                } else if (qName.equals(DPSI_ELT)) {
                    dpsi = overwrite(dpsi, MILLI_ARC_SECOND);
                } else if (qName.equals(DEPSILON_ELT)) {
                    deps = overwrite(deps, MILLI_ARC_SECOND);
                } else if (qName.equals(DX_ELT)) {
                    dx   = overwrite(dx, MILLI_ARC_SECOND);
                } else if (qName.equals(DY_ELT)) {
                    dy   = overwrite(dy, MILLI_ARC_SECOND);
                } else if (qName.equals(POLE_ELT) || qName.equals(UT_ELT) || qName.equals(NUTATION_ELT)) {
                    inBulletinA = false;
                } else if (qName.equals(DATA_EOP_ELT)) {
                    checkDates();
                    if (!Double.isNaN(dtu1) && !Double.isNaN(x) && !Double.isNaN(y)) {
                        final double[] equinox;
                        final double[] nro;
                        if (Double.isNaN(dpsi)) {
                            nro = new double[] {
                                dx, dy
                            };
                            equinox = getConverter().toEquinox(mjdDate, nro[0], nro[1]);
                        } else {
                            equinox = new double[] {
                                dpsi, deps
                            };
                            nro = getConverter().toNonRotating(mjdDate, equinox[0], equinox[1]);
                        }
                        if (configuration == null || !configuration.isValid(mjd)) {
                            // get a configuration for current name and date range
                            configuration = getItrfVersionProvider().getConfiguration(name, mjd);
                        }
                        history.add(new EOPEntry(mjd, dtu1, lod, x, y, Double.NaN, Double.NaN,
                                                 equinox[0], equinox[1], nro[0], nro[1],
                                                 configuration.getVersion(), mjdDate));
                    }
                }
            }

            /** Handle end of an element in a final data file.
             * @param qName name of the element
             */
            private void endFinalElement(final String qName) {
                if (qName.equals(DATE_ELT) && buffer.length() > 0) {
                    final String[] fields = buffer.toString().split("-");
                    if (fields.length == 3) {
                        year  = Integer.parseInt(fields[0]);
                        month = Integer.parseInt(fields[1]);
                        day   = Integer.parseInt(fields[2]);
                    }
                } else if (qName.equals(MJD_ELT) && buffer.length() > 0) {
                    mjd     = Integer.parseInt(buffer.toString());
                    mjdDate = new AbsoluteDate(new DateComponents(DateComponents.MODIFIED_JULIAN_EPOCH, mjd),
                                               getUtc());
                } else if (qName.equals(UT1_U_UTC_ELT)) {
                    dtu1 = overwrite(dtu1, Unit.SECOND);
                } else if (qName.equals(LOD_ELT)) {
                    lod = overwrite(lod, MILLI_SECOND);
                } else if (qName.equals(X_ELT)) {
                    x = overwrite(x, Unit.ARC_SECOND);
                } else if (qName.equals(Y_ELT)) {
                    y = overwrite(y, Unit.ARC_SECOND);
                } else if (qName.equals(X_RATE_ELT)) {
                    xRate = overwrite(xRate, ARC_SECOND_PER_DAY);
                } else if (qName.equals(Y_RATE_ELT)) {
                    yRate = overwrite(yRate, ARC_SECOND_PER_DAY);
                } else if (qName.equals(DPSI_ELT)) {
                    dpsi = overwrite(dpsi, MILLI_ARC_SECOND);
                } else if (qName.equals(DEPSILON_ELT)) {
                    deps = overwrite(deps, MILLI_ARC_SECOND);
                } else if (qName.equals(DX_ELT)) {
                    dx   = overwrite(dx, MILLI_ARC_SECOND);
                } else if (qName.equals(DY_ELT)) {
                    dy   = overwrite(dy, MILLI_ARC_SECOND);
                } else if (qName.equals(BULLETIN_A_ELT)) {
                    inBulletinA = false;
                } else if (qName.equals(EOP_SET_ELT)) {
                    checkDates();
                    if (!Double.isNaN(dtu1) && !Double.isNaN(x) && !Double.isNaN(y)) {
                        final double[] equinox;
                        final double[] nro;
                        if (Double.isNaN(dpsi)) {
                            nro = new double[] {
                                dx, dy
                            };
                            equinox = getConverter().toEquinox(mjdDate, nro[0], nro[1]);
                        } else {
                            equinox = new double[] {
                                dpsi, deps
                            };
                            nro = getConverter().toNonRotating(mjdDate, equinox[0], equinox[1]);
                        }
                        if (configuration == null || !configuration.isValid(mjd)) {
                            // get a configuration for current name and date range
                            configuration = getItrfVersionProvider().getConfiguration(name, mjd);
                        }
                        history.add(new EOPEntry(mjd, dtu1, lod, x, y, xRate, yRate,
                                                 equinox[0], equinox[1], nro[0], nro[1],
                                                 configuration.getVersion(), mjdDate));
                    }
                }
            }

            /** Overwrite a value if it is not set or if we are in a bulletinB.
             * @param oldValue old value to overwrite (may be NaN)
             * @param units units of raw data
             * @return a new value
             */
            private double overwrite(final double oldValue, final Unit units) {
                if (buffer.length() == 0) {
                    // there is nothing to overwrite with
                    return oldValue;
                } else if (inBulletinA && !Double.isNaN(oldValue)) {
                    // the value is already set and bulletin A values have a low priority
                    return oldValue;
                } else {
                    // either the value is not set or it is a high priority bulletin B value
                    return units.toSI(Double.parseDouble(buffer.toString()));
                }
            }

            /** Check if the year, month, day date and MJD date are consistent.
             */
            private void checkDates() {
                if (new DateComponents(year, month, day).getMJD() != mjd) {
                    throw new OrekitException(OrekitMessages.INCONSISTENT_DATES_IN_IERS_FILE,
                                              name, year, month, day, mjd);
                }
            }

            /** {@inheritDoc} */
            @Override
            public InputSource resolveEntity(final String publicId, final String systemId) {
                // disable external entities
                return new InputSource();
            }

        }

    }

    /** Enumerate for data file content. */
    private enum DataFileContent {

        /** Unknown content. */
        UNKNOWN,

        /** Bulletin A data.
         * @since 12.0
         */
        BULLETIN_A,

        /** Bulletin B data.
         * @since 12.0
         */
        BULLETIN_B,

        /** EOP_C04 data.
         * @since 12.0
         */
        EOP_C04,

        /** Daily data. */
        DAILY,

        /** Final data. */
        FINAL

    }

}
