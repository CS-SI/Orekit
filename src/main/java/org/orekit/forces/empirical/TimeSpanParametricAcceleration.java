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
package org.orekit.forces.empirical;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.geometry.euclidean.threed.FieldRotation;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.MathArrays;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.forces.ForceModel;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.events.FieldEventDetector;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.TimeSpanMap;
import org.orekit.utils.TimeSpanMap.Span;

/** Time span parametric acceleration model.
 *  <p>
 *  This class is closely related to {@link org.orekit.forces.empirical.ParametricAcceleration ParametricAcceleration} class.<br>
 *  The difference is that it has a {@link TimeSpanMap} of {@link AccelerationModel} objects as attribute
 *  instead of a single {@link AccelerationModel} object. <br>
 *  The idea behind this model is to allow the user to design a parametric acceleration model that can see its physical parameters
 *  change with time, at dates chosen by the user. <br>
 *  </p>
 *  <p>
 *  This is a behavior that can be sought in precise orbit determination.<br>
 *  Indeed for this type of application, the empirical parameters must be revalued at
 *  each new orbit.
 *  </p>
 *  <b>Usage</b>:<ul>
 *  <li><u>Construction</u>: constructor takes an acceleration direction, an attitude mode (or an inertial flag) and
 *  an AccelerationModel model.<br>
 *  This last model will be your initial AccelerationModel model and it will be initially valid for the whole time line.<br>
 *  The real validity of this first entry will be truncated as other AccelerationModel models are added.
 *  <li><u>Time spans</u>: AccelerationModel models are added using methods {@link #addAccelerationModelValidAfter(AccelerationModel, AbsoluteDate)}
 *   or {@link #addAccelerationModelValidBefore(AccelerationModel, AbsoluteDate)}.<br>
 *   Recommendations are the same than the ones in {@link TimeSpanMap}, meaning: <ul>
 *   <li>As an entry is added, it truncates the validity of the neighboring entries already present in the map;
 *   <li><b>The transition dates should be entered only once</b>. Repeating a transition date will lead to unexpected result and is not supported;
 *   <li>It is advised to order your AccelerationModel models chronologically when adding them to avoid any confusion.
 *   </ul>
 *   <li><u>Naming the parameter drivers</u>: It is strongly advised to give a custom name to the {@link ParameterDriver}(s)
 *   of each AccelerationModel model that is added to the object. This will allow you keeping track of the evolution of your models.<br>
 *   Different names are mandatory to differentiate the different drivers.<br>
 *   Since there is no default name for acceleration model parameters, you must handle the driver names to consider
 *   different names when adding a new acceleration model.
 *   </ul>
 * @author Bryan Cazabonne
 * @since 10.3
 */
public class TimeSpanParametricAcceleration implements ForceModel {

    /** Prefix for dates before in the parameter drivers' name. */
    public static final String DATE_BEFORE = " - Before ";

    /** Prefix for dates after in the parameter drivers' name. */
    public static final String DATE_AFTER = " - After ";

    /** Direction of the acceleration in defining frame. */
    private final Vector3D direction;

    /** Flag for inertial acceleration direction. */
    private final boolean isInertial;

    /** The attitude to override, if set. */
    private final AttitudeProvider attitudeOverride;

    /** TimeSpanMap of AccelerationModel objects. */
    private final TimeSpanMap<AccelerationModel> accelerationModelTimeSpanMap;

    /** Simple constructor.
     * @param direction acceleration direction in overridden spacecraft frame
     * @param isInertial if true, direction is defined in the same inertial
     * frame used for propagation (i.e. {@link SpacecraftState#getFrame()}),
     * otherwise direction is defined in spacecraft frame (i.e. using the
     * propagation {@link
     * org.orekit.propagation.Propagator#setAttitudeProvider(AttitudeProvider)
     * attitude law})
     * @param accelerationModel acceleration model used to compute the contribution of the empirical acceleration
     */
    public TimeSpanParametricAcceleration(final Vector3D direction,
                                          final boolean isInertial,
                                          final AccelerationModel accelerationModel) {
        this(direction, isInertial, null, accelerationModel);
    }

    /** Simple constructor.
     * @param direction acceleration direction in overridden spacecraft frame
     * frame used for propagation (i.e. {@link SpacecraftState#getFrame()}),
     * otherwise direction is defined in spacecraft frame (i.e. using the
     * propagation {@link
     * org.orekit.propagation.Propagator#setAttitudeProvider(AttitudeProvider)
     * attitude law})
     * @param attitudeOverride provider for attitude used to compute acceleration
     * @param accelerationModel acceleration model used to compute the contribution of the empirical acceleration
     */
    public TimeSpanParametricAcceleration(final Vector3D direction,
                                          final AttitudeProvider attitudeOverride,
                                          final AccelerationModel accelerationModel) {
        this(direction, false, attitudeOverride, accelerationModel);
    }

    /** Simple constructor.
     * @param direction acceleration direction in overridden spacecraft frame
     * @param isInertial if true, direction is defined in the same inertial
     * frame used for propagation (i.e. {@link SpacecraftState#getFrame()}),
     * otherwise direction is defined in spacecraft frame (i.e. using the
     * propagation {@link
     * org.orekit.propagation.Propagator#setAttitudeProvider(AttitudeProvider)
     * attitude law})
     * @param attitudeOverride provider for attitude used to compute acceleration
     * @param accelerationModel acceleration model used to compute the contribution of the empirical acceleration
     */
    private TimeSpanParametricAcceleration(final Vector3D direction,
                                           final boolean isInertial,
                                           final AttitudeProvider attitudeOverride,
                                           final AccelerationModel accelerationModel) {
        this.direction                    = direction;
        this.isInertial                   = isInertial;
        this.attitudeOverride             = attitudeOverride;
        this.accelerationModelTimeSpanMap = new TimeSpanMap<>(accelerationModel);
    }

    /** {@inheritDoc} */
    @Override
    public void init(final SpacecraftState initialState, final AbsoluteDate target) {
        accelerationModelTimeSpanMap.forEach(accelerationModel -> accelerationModel.init(initialState, target));
    }

    /** Add an AccelerationModel entry valid before a limit date.<br>
     * <p>
     * Using <code>addAccelerationModelValidBefore(entry, t)</code> will make <code>entry</code>
     * valid in ]-∞, t[ (note the open bracket).
     * <p>
     * <b>WARNING</b>: Since there is no default name for acceleration model parameters,
     * the user must handle itself the driver names to consider different names
     * (i.e. different parameters) when adding a new acceleration model.
     * @param accelerationModel AccelerationModel entry
     * @param latestValidityDate date before which the entry is valid
     * (must be different from <b>all</b> dates already used for transitions)
     */
    public void addAccelerationModelValidBefore(final AccelerationModel accelerationModel, final AbsoluteDate latestValidityDate) {
        accelerationModelTimeSpanMap.addValidBefore(accelerationModel, latestValidityDate, false);
    }

    /** Add a AccelerationModel entry valid after a limit date.<br>
     * <p>
     * Using <code>addAccelerationModelValidAfter(entry, t)</code> will make <code>entry</code>
     * valid in [t, +∞[ (note the closed bracket).
     * <p>
     * <b>WARNING</b>: Since there is no default name for acceleration model parameters,
     * the user must handle itself the driver names to consider different names
     * (i.e. different parameters) when adding a new acceleration model.
     * @param accelerationModel AccelerationModel entry
     * @param earliestValidityDate date after which the entry is valid
     * (must be different from <b>all</b> dates already used for transitions)
     */
    public void addAccelerationModelValidAfter(final AccelerationModel accelerationModel, final AbsoluteDate earliestValidityDate) {
        accelerationModelTimeSpanMap.addValidAfter(accelerationModel, earliestValidityDate, false);
    }

    /** Get the {@link AccelerationModel} model valid at a date.
     * @param date the date of validity
     * @return the AccelerationModel model valid at date
     */
    public AccelerationModel getAccelerationModel(final AbsoluteDate date) {
        return accelerationModelTimeSpanMap.get(date);
    }

    /** Get the {@link AccelerationModel} {@link Span} containing a specified date.
     * @param date date belonging to the desired time span
     * @return the AccelerationModel time span containing the specified date
     */
    public Span<AccelerationModel> getAccelerationModelSpan(final AbsoluteDate date) {
        return accelerationModelTimeSpanMap.getSpan(date);
    }

    /** Extract a range of the {@link AccelerationModel} map.
     * <p>
     * The object returned will be a new independent instance that will contain
     * only the transitions that lie in the specified range.
     * </p>
     * See the {@link TimeSpanMap#extractRange TimeSpanMap.extractRange method} for more.
     * @param start earliest date at which a transition is included in the range
     * (may be set to {@link AbsoluteDate#PAST_INFINITY} to keep all early transitions)
     * @param end latest date at which a transition is included in the r
     * (may be set to {@link AbsoluteDate#FUTURE_INFINITY} to keep all late transitions)
     * @return a new TimeSpanMap instance of AccelerationModel with all transitions restricted to the specified range
     */
    public TimeSpanMap<AccelerationModel> extractAccelerationModelRange(final AbsoluteDate start, final AbsoluteDate end) {
        return accelerationModelTimeSpanMap.extractRange(start, end);
    }

    /** Get the first {@link Span time span} of the acceleration model time span map.
     * @return the first {@link Span time span} of the acceleration model time span map
     * @since 11.1
     */
    public Span<AccelerationModel> getFirstSpan() {
        return accelerationModelTimeSpanMap.getFirstSpan();
    }

    /** {@inheritDoc} */
    @Override
    public boolean dependsOnPositionOnly() {
        return isInertial;
    }

    /** {@inheritDoc} */
    @Override
    public Vector3D acceleration(final SpacecraftState state,
                                 final double[] parameters) {

        // Date
        final AbsoluteDate date = state.getDate();

        // Compute inertial direction
        final Vector3D inertialDirection;
        if (isInertial) {
            // the acceleration direction is already defined in the inertial frame
            inertialDirection = direction;
        } else {
            final Rotation rotation;
            if (attitudeOverride == null) {
                // the acceleration direction is defined in spacecraft frame as set by the propagator
                rotation = state.getAttitude().getRotation();
            } else {
                // the acceleration direction is defined in a dedicated frame
                rotation = attitudeOverride.getAttitudeRotation(state.getOrbit(), date, state.getFrame());
            }
            inertialDirection = rotation.applyInverseTo(direction);
        }

        // Extract the proper parameters valid at date from the input array
        final double[] extractedParameters = extractParameters(parameters, date);

        // Compute and return the parametric acceleration
        return new Vector3D(getAccelerationModel(date).signedAmplitude(state, extractedParameters), inertialDirection);

    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> FieldVector3D<T> acceleration(final FieldSpacecraftState<T> state,
                                                                         final T[] parameters) {

        // Date
        final FieldAbsoluteDate<T> date = state.getDate();

        // Compute inertial direction
        final FieldVector3D<T> inertialDirection;
        if (isInertial) {
            // the acceleration direction is already defined in the inertial frame
            inertialDirection = new FieldVector3D<>(state.getDate().getField(), direction);
        } else {
            final FieldRotation<T> rotation;
            if (attitudeOverride == null) {
                // the acceleration direction is defined in spacecraft frame as set by the propagator
                rotation = state.getAttitude().getRotation();
            } else {
                // the acceleration direction is defined in a dedicated frame
                rotation = attitudeOverride.getAttitudeRotation(state.getOrbit(), date, state.getFrame());
            }
            inertialDirection = rotation.applyInverseTo(direction);
        }

        // Extract the proper parameters valid at date from the input array
        final T[] extractedParameters = extractParameters(parameters, date);

        // Compute and return the parametric acceleration
        return new FieldVector3D<>(getAccelerationModel(date.toAbsoluteDate()).signedAmplitude(state, extractedParameters), inertialDirection);

    }

    /** {@inheritDoc} */
    @Override
    public Stream<EventDetector> getEventDetectors() {
        return Stream.empty();
    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> Stream<FieldEventDetector<T>> getFieldEventDetectors(final Field<T> field) {
        return Stream.empty();
    }

    /** {@inheritDoc}
     * <p>
     * All the parameter drivers of all AccelerationModel models are returned in an array.
     * Models are ordered chronologically.
     * </p>
     */
    @Override
    public List<ParameterDriver> getParametersDrivers() {

        // Get all transitions from the TimeSpanMap
        final List<ParameterDriver> listParameterDrivers = new ArrayList<>();

        // Loop on the spans
        for (Span<AccelerationModel> span = getFirstSpan(); span != null; span = span.next()) {
            // Add all the parameter drivers of the time span
            for (ParameterDriver driver : span.getData().getParametersDrivers()) {
                // Add the driver only if the name does not exist already
                if (!findByName(listParameterDrivers, driver.getName())) {
                    listParameterDrivers.add(driver);
                }
            }
        }

        // Return an array of parameter drivers with no duplicated name
        return Collections.unmodifiableList(listParameterDrivers);

    }

    /** Extract the proper parameter drivers' values from the array in input of the
     * {@link #acceleration(SpacecraftState, double[]) acceleration} method.
     *  Parameters are filtered given an input date.
     * @param parameters the input parameters array
     * @param date the date
     * @return the parameters given the date
     */
    public double[] extractParameters(final double[] parameters, final AbsoluteDate date) {

        // Get the acceleration model parameter drivers of the date
        final List<ParameterDriver> empiricalParameterDriver = getAccelerationModel(date).getParametersDrivers();

        // Find out the indexes of the parameters in the whole array of parameters
        final List<ParameterDriver> allParameters = getParametersDrivers();
        final double[] outParameters = new double[empiricalParameterDriver.size()];
        int index = 0;
        for (int i = 0; i < allParameters.size(); i++) {
            final String driverName = allParameters.get(i).getName();
            for (ParameterDriver accDriver : empiricalParameterDriver) {
                if (accDriver.getName().equals(driverName)) {
                    outParameters[index++] = parameters[i];
                }
            }
        }
        return outParameters;
    }

    /** Extract the proper parameter drivers' values from the array in input of the
     * {@link #acceleration(FieldSpacecraftState, CalculusFieldElement[]) acceleration} method.
     *  Parameters are filtered given an input date.
     * @param parameters the input parameters array
     * @param date the date
     * @param <T> extends CalculusFieldElement
     * @return the parameters given the date
     */
    public <T extends CalculusFieldElement<T>> T[] extractParameters(final T[] parameters,
                                                                 final FieldAbsoluteDate<T> date) {

        // Get the acceleration parameter drivers of the date
        final List<ParameterDriver> empiricalParameterDriver = getAccelerationModel(date.toAbsoluteDate()).getParametersDrivers();

        // Find out the indexes of the parameters in the whole array of parameters
        final List<ParameterDriver> allParameters = getParametersDrivers();
        final T[] outParameters = MathArrays.buildArray(date.getField(), empiricalParameterDriver.size());
        int index = 0;
        for (int i = 0; i < allParameters.size(); i++) {
            final String driverName = allParameters.get(i).getName();
            for (ParameterDriver accDriver : empiricalParameterDriver) {
                if (accDriver.getName().equals(driverName)) {
                    outParameters[index++] = parameters[i];
                }
            }
        }
        return outParameters;
    }

    /** Find if a parameter driver with a given name already exists in a list of parameter drivers.
     * @param driversList the list of parameter drivers
     * @param name the parameter driver's name to filter with
     * @return true if the name was found, false otherwise
     */
    private boolean findByName(final List<ParameterDriver> driversList, final String name) {
        for (final ParameterDriver d : driversList) {
            if (d.getName().equals(name)) {
                return true;
            }
        }
        return false;
    }

}
