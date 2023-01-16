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
package org.orekit.models.earth.troposphere;

import java.util.ArrayList;
import java.util.List;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.util.MathArrays;
import org.orekit.annotation.DefaultDataContext;
import org.orekit.bodies.FieldGeodeticPoint;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.TimeSpanMap;
import org.orekit.utils.TimeSpanMap.Span;

/**
 * Time span estimated tropospheric model.
 * <p>
 * This class is closely related to {@link org.orekit.models.earth.troposphere EstimatedTroposphericModel} class.<br>
 * The difference is that it has a {@link TimeSpanMap} of {@link EstimatedTroposphericModel} objects as attribute. <br>
 * The idea behind this model is to allow the user to design a tropospheric model that can see its physical parameters
 * (total zenith delay) change with time, at dates chosen by the user. <br>
 * </p>
 * @author Bryan Cazabonne
 * @since 10.2
 */
public class TimeSpanEstimatedTroposphericModel implements DiscreteTroposphericModel {

    /** Prefix for dates before in the tropospheric parameter drivers' name. */
    public static final String DATE_BEFORE = " - Before ";

    /** Prefix for dates after in the tropospheric parameter drivers' name. */
    public static final String DATE_AFTER = " - After ";

    /** Time scale for transition dates. */
    private final TimeScale timeScale;

    /** It contains all the models use for the whole period of measurements. */
    private final TimeSpanMap<EstimatedTroposphericModel> troposphericModelMap;

    /**
     * Constructor with default UTC time scale.
     * @param model the initial model which going to be used for all the models initialization.
     */
    @DefaultDataContext
    public TimeSpanEstimatedTroposphericModel(final EstimatedTroposphericModel model) {
        this(model, TimeScalesFactory.getUTC());
    }

    /**
     * Constructor with default UTC time scale.
     * @param model the initial model which going to be used for all the models initialization.
     * @param timeScale  timeScale Time scale used for the default names of the tropospheric parameter drivers
     */
    public TimeSpanEstimatedTroposphericModel(final EstimatedTroposphericModel model,
                                              final TimeScale timeScale) {
        this.troposphericModelMap = new TimeSpanMap<>(model);
        this.timeScale            = timeScale;
    }

    /** {@inheritDoc}
     * <p>
     * All the parameter drivers of all Estimated models are returned in an array.
     * Models are ordered chronologically.
     * </p>
     */
    @Override
    public List<ParameterDriver> getParametersDrivers() {

        // Get all transitions from the TimeSpanMap
        final List<ParameterDriver> listTroposphericParameterDrivers = new ArrayList<>();

        // Loop on the spans
        for (Span<EstimatedTroposphericModel> span = getFirstSpan(); span != null; span = span.next()) {
            // Add all the parameter drivers of each span
            for (ParameterDriver tropoDriver : span.getData().getParametersDrivers()) {
                // Add the driver only if the name does not exist already
                if (!findByName(listTroposphericParameterDrivers, tropoDriver.getName())) {
                    listTroposphericParameterDrivers.add(tropoDriver);
                }
            }
        }

        // Return an array of parameter drivers with no duplicated name
        return listTroposphericParameterDrivers;

    }

    /** Add an EstimatedTroposphericModel entry valid before a limit date.<br>
     * Using <code>addTroposphericValidBefore(entry, t)</code> will make <code>entry</code>
     * valid in ]-∞, t[ (note the open bracket).
     * @param model EstimatedTroposphericModel entry
     * @param latestValidityDate date before which the entry is valid
     * (must be different from <b>all</b> dates already used for transitions)
     */
    public void addTroposphericModelValidBefore(final EstimatedTroposphericModel model, final AbsoluteDate latestValidityDate) {
        troposphericModelMap.addValidBefore(changeTroposphericParameterDriversNames(model,
                                                                                    latestValidityDate,
                                                                                    DATE_BEFORE),
                                            latestValidityDate, false);
    }

    /** Add a EstimatedTroposphericModel entry valid after a limit date.<br>
     * Using <code>addTroposphericModelValidAfter(entry, t)</code> will make <code>entry</code>
     * valid in [t, +∞[ (note the closed bracket).
     * @param model EstimatedTroposphericModel entry
     * @param earliestValidityDate date after which the entry is valid
     * (must be different from <b>all</b> dates already used for transitions)
     */
    public void addTroposphericModelValidAfter(final EstimatedTroposphericModel model, final AbsoluteDate earliestValidityDate) {
        troposphericModelMap.addValidAfter(changeTroposphericParameterDriversNames(model,
                                                                                   earliestValidityDate,
                                                                                   DATE_AFTER),
                                           earliestValidityDate, false);
    }

    /** Get the {@link EstimatedTroposphericModel} model valid at a date.
     * @param date the date of validity
     * @return the EstimatedTroposphericModel model valid at date
     */
    public EstimatedTroposphericModel getTroposphericModel(final AbsoluteDate date) {
        return troposphericModelMap.get(date);
    }

    /** Get the first {@link Span time span} of the tropospheric model time span map.
     * @return the first {@link Span time span} of the tropospheric model time span map
     * @since 11.1
     */
    public Span<EstimatedTroposphericModel> getFirstSpan() {
        return troposphericModelMap.getFirstSpan();
    }

    /** Extract the proper parameter drivers' values from the array in input of the
     * {@link #pathDelay(double, GeodeticPoint, double[], AbsoluteDate) pathDelay} method.
     *  Parameters are filtered given an input date.
     * @param parameters the input parameters array
     * @param date the date
     * @return the parameters given the date
     */
    public double[] extractParameters(final double[] parameters, final AbsoluteDate date) {

        // Get the tropospheric parameter drivers of the date
        final List<ParameterDriver> troposphericParameterDriver = getTroposphericModel(date).getParametersDrivers();

        // Find out the indexes of the parameters in the whole array of parameters
        final List<ParameterDriver> allTroposphericParameters = getParametersDrivers();
        final double[] outParameters = new double[troposphericParameterDriver.size()];
        int index = 0;
        for (int i = 0; i < allTroposphericParameters.size(); i++) {
            final String driverName = allTroposphericParameters.get(i).getName();
            for (ParameterDriver tropoDriver : troposphericParameterDriver) {
                if (tropoDriver.getName().equals(driverName)) {
                    outParameters[index++] = parameters[i];
                }
            }
        }
        return outParameters;
    }

    /** Extract the proper parameter drivers' values from the array in input of the
     * {@link #pathDelay(double, GeodeticPoint, double[], AbsoluteDate) pathDelay} method.
     *  Parameters are filtered given an input date.
     * @param parameters the input parameters array
     * @param date the date
     * @param <T> extends CalculusFieldElements
     * @return the parameters given the date
     */
    public <T extends CalculusFieldElement<T>> T[] extractParameters(final T[] parameters,
                                                                 final FieldAbsoluteDate<T> date) {

        // Get the tropospheric parameter drivers of the date
        final List<ParameterDriver> troposphericParameterDriver = getTroposphericModel(date.toAbsoluteDate()).getParametersDrivers();

        // Find out the indexes of the parameters in the whole array of parameters
        final List<ParameterDriver> allTroposphericParameters = getParametersDrivers();
        final T[] outParameters = MathArrays.buildArray(date.getField(), troposphericParameterDriver.size());
        int index = 0;
        for (int i = 0; i < allTroposphericParameters.size(); i++) {
            final String driverName = allTroposphericParameters.get(i).getName();
            for (ParameterDriver tropoDriver : troposphericParameterDriver) {
                if (tropoDriver.getName().equals(driverName)) {
                    outParameters[index++] = parameters[i];
                }
            }
        }
        return outParameters;
    }

    /** {@inheritDoc} */
    @Override
    public double pathDelay(final double elevation, final GeodeticPoint point,
                            final double[] parameters, final AbsoluteDate date) {
        // Extract the proper parameters valid at date from the input array
        final double[] extractedParameters = extractParameters(parameters, date);
        // Compute and return the path delay
        return getTroposphericModel(date).pathDelay(elevation, point,
                                                    extractedParameters, date);
    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> T pathDelay(final T elevation, final  FieldGeodeticPoint<T> point,
                                                       final T[] parameters, final FieldAbsoluteDate<T> date) {
        // Extract the proper parameters valid at date from the input array
        final T[] extractedParameters = extractParameters(parameters, date);
        // Compute and return the path delay
        return getTroposphericModel(date.toAbsoluteDate()).pathDelay(elevation, point,
                                                                     extractedParameters, date);
    }

    /** Find if a parameter driver with a given name already exists in a list of parameter drivers.
     * @param driversList the list of parameter drivers
     * @param name the parameter driver's name to filter with
     * @return true if the name was found, false otherwise
     */
    private boolean findByName(final List<ParameterDriver> driversList, final String name) {
        for (final ParameterDriver driver : driversList) {
            if (driver.getName().equals(name)) {
                return true;
            }
        }
        return false;
    }

    /** Change the parameter drivers names of a {@link EstimatedTroposphericModel} model, if needed.
     * <p>
     * This is done to avoid that several parameter drivers have the same name.<br>
     * It is done only if the user hasn't modify the EstimatedTroposphericModel parameter drivers default names.
     * </p>
     * @param troposphericModel the EstimatedTroposphericModel model
     * @param date the date used in the parameter driver's name
     * @param datePrefix the date prefix used in the parameter driver's name
     * @return the EstimatedTroposphericModel with its drivers' names changed
     */
    private EstimatedTroposphericModel changeTroposphericParameterDriversNames(final EstimatedTroposphericModel troposphericModel,
                                                                               final AbsoluteDate date,
                                                                               final String datePrefix) {
        // Loop on the parameter drivers of the EstimatedTroposphericModel model
        for (ParameterDriver driver: troposphericModel.getParametersDrivers()) {
            final String driverName = driver.getName();

            // If the name is the default name for EstimatedTroposphericModel parameter drivers
            // Modify the name to add the prefix and the date
            if (driverName.equals(EstimatedTroposphericModel.TOTAL_ZENITH_DELAY)) {
                driver.setName(driverName + datePrefix + date.toString(timeScale));
            }
        }
        return troposphericModel;
    }

}
