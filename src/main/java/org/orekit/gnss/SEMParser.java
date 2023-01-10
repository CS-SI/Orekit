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
package org.orekit.gnss;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.orekit.annotation.DefaultDataContext;
import org.orekit.data.AbstractSelfFeedingLoader;
import org.orekit.data.DataContext;
import org.orekit.data.DataLoader;
import org.orekit.data.DataProvidersManager;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.propagation.analytical.gnss.data.GNSSConstants;
import org.orekit.propagation.analytical.gnss.data.GPSAlmanac;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.GNSSDate;
import org.orekit.time.TimeScales;


/**
 * This class reads SEM almanac files and provides {@link GPSAlmanac GPS almanacs}.
 *
 * <p>The definition of a SEM almanac comes from the
 * <a href="http://www.navcen.uscg.gov/?pageName=gpsSem">U.S. COAST GUARD NAVIGATION CENTER</a>.</p>
 *
 * <p>The format of the files holding SEM almanacs is not precisely specified,
 * so the parsing rules have been deduced from the downloadable files at
 * <a href="http://www.navcen.uscg.gov/?pageName=gpsAlmanacs">NAVCEN</a>
 * and at <a href="https://celestrak.com/GPS/almanac/SEM/">CelesTrak</a>.</p>
 *
 * @author Pascal Parraud
 * @since 8.0
 *
 */
public class SEMParser extends AbstractSelfFeedingLoader implements DataLoader {

    // Constants
    /** The source of the almanacs. */
    private static final String SOURCE = "SEM";

    /** the reference value for the inclination of GPS orbit: 0.30 semicircles. */
    private static final double INC_REF = 0.30;

    /** Default supported files name pattern. */
    private static final String DEFAULT_SUPPORTED_NAMES = ".*\\.al3$";

    /** Separator for parsing. */
    private static final Pattern SEPARATOR = Pattern.compile("\\s+");

    // Fields
    /** the list of all the almanacs read from the file. */
    private final List<GPSAlmanac> almanacs;

    /** the list of all the PRN numbers of all the almanacs read from the file. */
    private final List<Integer> prnList;

    /** Set of time scales to use. */
    private final TimeScales timeScales;

    /** Simple constructor.
     *
     * <p>This constructor does not load any data by itself. Data must be loaded
     * later on by calling one of the {@link #loadData() loadData()} method or
     * the {@link #loadData(InputStream, String) loadData(inputStream, fileName)}
     * method.</p>
     *
     * <p>The supported files names are used when getting data from the
     * {@link #loadData() loadData()} method that relies on the
     * {@link DataContext#getDefault() default data context}. They are useless when
     * getting data from the {@link #loadData(InputStream, String) loadData(input, name)}
     * method.</p>
     *
     * @param supportedNames regular expression for supported files names
     * (if null, a default pattern matching files with a ".al3" extension will be used)
     * @see #loadData()
     * @see #SEMParser(String, DataProvidersManager, TimeScales)
     */
    @DefaultDataContext
    public SEMParser(final String supportedNames) {
        this(supportedNames,
                DataContext.getDefault().getDataProvidersManager(),
                DataContext.getDefault().getTimeScales());
    }

    /**
     * Create a SEM loader/parser with the given source of SEM auxiliary data files.
     *
     * <p>This constructor does not load any data by itself. Data must be loaded
     * later on by calling one of the {@link #loadData() loadData()} method or
     * the {@link #loadData(InputStream, String) loadData(inputStream, fileName)}
     * method.</p>
     *
     * <p>The supported files names are used when getting data from the
     * {@link #loadData() loadData()} method that relies on the
     * {@code dataProvidersManager}. They are useless when
     * getting data from the {@link #loadData(InputStream, String) loadData(input, name)}
     * method.</p>
     *
     * @param supportedNames regular expression for supported files names
     * (if null, a default pattern matching files with a ".al3" extension will be used)
     * @param dataProvidersManager provides access to auxiliary data.
     * @param timeScales to use when parsing the GPS dates.
     * @see #loadData()
     * @since 10.1
     */
    public SEMParser(final String supportedNames,
                     final DataProvidersManager dataProvidersManager,
                     final TimeScales timeScales) {
        super((supportedNames == null) ? DEFAULT_SUPPORTED_NAMES : supportedNames,
                dataProvidersManager);
        this.almanacs = new ArrayList<>();
        this.prnList = new ArrayList<>();
        this.timeScales = timeScales;
    }

    /**
     * Loads almanacs.
     *
     * <p>The almanacs already loaded in the instance will be discarded
     * and replaced by the newly loaded data.</p>
     * <p>This feature is useful when the file selection is already set up by
     * the {@link DataProvidersManager data providers manager} configuration.</p>
     *
     */
    public void loadData() {
        // load the data from the configured data providers
        feed(this);
        if (almanacs.isEmpty()) {
            throw new OrekitException(OrekitMessages.NO_SEM_ALMANAC_AVAILABLE);
        }
    }

    @Override
    public void loadData(final InputStream input, final String name)
        throws IOException, ParseException, OrekitException {

        // Clears the lists
        almanacs.clear();
        prnList.clear();

        // Creates the reader
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            // Reads the number of almanacs in the file from the first line
            String[] token = getTokens(reader);
            final int almanacNb = Integer.parseInt(token[0].trim());

            // Reads the week number and the time of applicability from the second line
            token = getTokens(reader);
            final int week = Integer.parseInt(token[0].trim());
            final double toa = Double.parseDouble(token[1].trim());

            // Loop over data blocks
            for (int i = 0; i < almanacNb; i++) {
                // Reads the next lines to get one almanac from
                readAlmanac(reader, week, toa);
            }
        } catch (IndexOutOfBoundsException | IOException e) {
            throw new OrekitException(e, OrekitMessages.NOT_A_SUPPORTED_SEM_ALMANAC_FILE, name);
        }
    }

    @Override
    public boolean stillAcceptsData() {
        return almanacs.isEmpty();
    }

    /**
     * Gets all the {@link GPSAlmanac GPS almanacs} read from the file.
     *
     * @return the list of {@link GPSAlmanac} from the file
     */
    public List<GPSAlmanac> getAlmanacs() {
        return almanacs;
    }

    /**
     * Gets the PRN numbers of all the {@link GPSAlmanac GPS almanacs} read from the file.
     *
     * @return the PRN numbers of all the {@link GPSAlmanac GPS almanacs} read from the file
     */
    public List<Integer> getPRNNumbers() {
        return prnList;
    }

    @Override
    public String getSupportedNames() {
        return super.getSupportedNames();
    }

    /**
     * Builds {@link GPSAlmanac GPS almanacs} from data read in the file.
     *
     * @param reader the reader
     * @param week the GPS week
     * @param toa the Time of Applicability
     * @throws IOException if GPSAlmanacs can't be built from the file
     */
    private void readAlmanac(final BufferedReader reader, final int week, final double toa)
        throws IOException {
        // Skips the empty line
        reader.readLine();

        // Create an empty GPS almanac and set the source
        final GPSAlmanac almanac = new GPSAlmanac();
        almanac.setSource(SOURCE);

        try {
            // Reads the PRN number from the first line
            String[] token = getTokens(reader);
            almanac.setPRN(Integer.parseInt(token[0].trim()));

            // Reads the SV number from the second line
            token = getTokens(reader);
            almanac.setSVN(Integer.parseInt(token[0].trim()));

            // Reads the average URA number from the third line
            token = getTokens(reader);
            almanac.setURA(Integer.parseInt(token[0].trim()));

            // Reads the fourth line to get ecc, inc and dom
            token = getTokens(reader);
            almanac.setE(Double.parseDouble(token[0].trim()));
            almanac.setI0(getInclination(Double.parseDouble(token[1].trim())));
            almanac.setOmegaDot(toRadians(Double.parseDouble(token[2].trim())));

            // Reads the fifth line to get sqa, raan and aop
            token = getTokens(reader);
            almanac.setSqrtA(Double.parseDouble(token[0].trim()));
            almanac.setOmega0(toRadians(Double.parseDouble(token[1].trim())));
            almanac.setPa(toRadians(Double.parseDouble(token[2].trim())));

            // Reads the sixth line to get anom, af0 and af1
            token = getTokens(reader);
            almanac.setM0(toRadians(Double.parseDouble(token[0].trim())));
            almanac.setAf0(Double.parseDouble(token[1].trim()));
            almanac.setAf1(Double.parseDouble(token[2].trim()));

            // Reads the seventh line to get health
            token = getTokens(reader);
            almanac.setHealth(Integer.parseInt(token[0].trim()));

            // Reads the eighth line to get Satellite Configuration
            token = getTokens(reader);
            almanac.setSatConfiguration(Integer.parseInt(token[0].trim()));

            // Adds the almanac to the list
            final AbsoluteDate date = new GNSSDate(week, toa, SatelliteSystem.GPS, timeScales).getDate();
            almanac.setDate(date);
            almanac.setTime(toa);
            almanac.setWeek(week);
            almanacs.add(almanac);

            // Adds the PRN to the list
            prnList.add(almanac.getPRN());
        } catch (IndexOutOfBoundsException aioobe) {
            throw new IOException(aioobe);
        }
    }

    /** Read a line and get tokens from.
     *  @param reader the reader
     *  @return the tokens from the read line
     *  @throws IOException if the line is null
     */
    private String[] getTokens(final BufferedReader reader) throws IOException {
        final String line = reader.readLine();
        if (line != null) {
            return SEPARATOR.split(line.trim());
        } else {
            throw new IOException();
        }
    }

    /**
     * Gets the inclination from the inclination offset.
     *
     * @param incOffset the inclination offset (semicircles)
     * @return the inclination (rad)
     */
    private double getInclination(final double incOffset) {
        return toRadians(INC_REF + incOffset);
    }

    /**
     * Converts an angular value from semicircles to radians.
     *
     * @param semicircles the angular value in semicircles
     * @return the angular value in radians
     */
    private double toRadians(final double semicircles) {
        return GNSSConstants.GNSS_PI * semicircles;
    }

}
