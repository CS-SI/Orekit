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
package org.orekit.forces.drag;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.analysis.differentiation.DerivativeStructure;
import org.hipparchus.analysis.differentiation.Gradient;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.ode.events.Action;
import org.hipparchus.util.MathArrays;
import org.orekit.annotation.DefaultDataContext;
import org.orekit.frames.Frame;
import org.orekit.models.earth.atmosphere.Atmosphere;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.DateDetector;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.events.FieldDateDetector;
import org.orekit.propagation.events.FieldEventDetector;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.FieldTimeStamped;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.TimeSpanMap;
import org.orekit.utils.TimeSpanMap.Span;
import org.orekit.utils.TimeSpanMap.Transition;


/** Time span atmospheric drag force model.
 *  <p>
 *  This class is closely related to {@link org.orekit.forces.drag.DragForce DragForce} class.<br>
 *  The difference is that it has a {@link TimeSpanMap} of {@link DragSensitive} objects as attribute
 *  instead of a single {@link DragSensitive} object. <br>
 *  The idea behind this model is to allow the user to design a drag force model that can see its physical parameters
 *  (drag coefficient and lift ratio) change with time, at dates chosen by the user. <br>
 *  </p>
 *  <p>
 *  This is a behavior that can be sought in operational orbit determination.<br>
 *  Indeed the solar activity has a strong influence on the local atmospheric density, and thus on the drag force effect.<br>
 *  Solar activity is a physical phenomenon that is difficult to model and predict. <br>
 *  The errors induced by this incomplete modeling can be estimated through the drag coefficients.<br>
 *  Being able to define and estimate drag coefficients depending on user-chosen dates in a piecewise fashion allows for
 *  a better  modeling of solar activity uncertainties.
 *  </p>
 *  <p>
 *  A typical operational use case is to have a daily solar activity with three-hourly magnetic indexes provided by an
 *  international organization (NOAA for example).<br>
 *  Given this input, a user can define a piecewise drag force model with daily or three-hourly drag coefficients.<br>
 *  Each timed coefficient will absorb a part of the uncertainties in the solar activity and will allow for a more accurate
 *  orbit determination
 *  </p>
 *  <b>Usage</b>:<ul>
 *  <li><u>Construction</u>: constructor takes an atmospheric model and a DragSensitive model.<br>
 *  This last model will be your initial DragSensitive model and it will be initially valid for the whole time line.<br>
 *  The real validity of this first entry will be truncated as other DragSensitive models are added.
 *  <li><u>Time spans</u>: DragSensitive models are added using methods {@link #addDragSensitiveValidAfter(DragSensitive, AbsoluteDate)}
 *   or {@link #addDragSensitiveValidBefore(DragSensitive, AbsoluteDate)}.<br>
 *   Recommendations are the same than the ones in {@link TimeSpanMap}, meaning: <ul>
 *   <li>As an entry is added, it truncates the validity of the neighboring entries already present in the map;
 *   <li><b>The transition dates should be entered only once</b>. Repeating a transition date will lead to unexpected result and is not supported;
 *   <li>It is advised to order your DragSensitive models chronologically when adding them to avoid any confusion.
 *   </ul>
 *   <li><u>Naming the parameter drivers</u>: It is strongly advised to give a custom name to the {@link ParameterDriver}(s)
 *   of each DragSensitive model that is added to the object. This will allow you keeping track of the evolution of your models.<br>
 *   Different names are mandatory to differentiate the different drivers.<br>
 *   If you do not specify a name, a default name will be chosen. Example for the drag coefficient:<ul>
 *   <li>Initial DragSensitive model: the driver's default name is "{@link DragSensitive#DRAG_COEFFICIENT}";
 *   <li>Using {@link #addDragSensitiveValidAfter(DragSensitive, AbsoluteDate)}: the driver's default name is
 *   "{@link DragSensitive#DRAG_COEFFICIENT} + {@link #DATE_AFTER} + date.toString()"
 *   <li>Using {@link #addDragSensitiveValidBefore(DragSensitive, AbsoluteDate)}: the driver's default name is
 *   "{@link DragSensitive#DRAG_COEFFICIENT} + {@link #DATE_BEFORE} + date.toString()"
 *   </ul>
 *   </ul>
 *  <b>Example following previous recommendations</b>:<ul>
 *  <li>Given:
 *  <ul>
 *  <li><code>atmosphere</code>: an {@link Atmosphere atmospheric model};
 *  <li><code>isotropicDrag0, 1 and 2</code>: three {@link org.orekit.forces.drag.IsotropicDrag IsotropicDrag} models;
 *  <li><code>date</code>: an {@link AbsoluteDate}.
 *  </ul>
 *  <li>Name the drivers:<br>
 *  <code>isotropicDrag0.getDragParametersDrivers()[0].setName = "Cd0";</code><br>
 *  <code>isotropicDrag1.getDragParametersDrivers()[0].setName = "Cd1";</code><br>
 *  <code>isotropicDrag2.getDragParametersDrivers()[0].setName = "Cd2";</code><br>
 *  <li>Initialize the model: <br>
 *  <code>TimeSpanDragForce force = new TimeSpanDragForce(atmosphere, isotropicDrag0);</code>
 *  <li>Set the second and third model one Julian day apart each:<br>
 *  <code>force.addDragSensitiveValidAfter(isotropicDrag1, date.shiftedBy(Constants.JULIAN_DAY));</code><br>
 *  <code>force.addDragSensitiveValidAfter(isotropicDrag2, date.shiftedBy(2 * Constants.JULIAN_DAY));</code><br>
 *  <li>With this, your model will have the following properties:
 *  <ul>
 *  <li>t in ]-∞, date + 1 day [ / Cd = Cd0
 *  <li>t in [date + 1 day, date + 2days [ / Cd = Cd1
 *  <li>t in [date + 2 days, +∞ [ / Cd = Cd2
 *  </ul>
 *  </ul>
 *  <p>
 *  <b>Warning</b>:<br> The TimeSpanDragForce model is versatile and you could end up with non-physical modeling.<br>
 *  For example you could add 2 {@link org.orekit.forces.drag.IsotropicDrag IsotropicDrag} models with different areas,
 *  or one {@link org.orekit.forces.drag.IsotropicDrag IsotropicDrag} model and then one
 *  {@link org.orekit.forces.BoxAndSolarArraySpacecraft BoxAndSolarArraySpacecraft} model.<br>
 *  It is up to you to ensure that your models are consistent with each other, Orekit will not perform any check for that.
 *  </p>
 * @author Maxime Journot
 * @since 10.2
 */
public class TimeSpanDragForce extends AbstractDragForceModel {

    /** Prefix for dates before in the parameter drivers' name. */
    public static final String DATE_BEFORE = " - Before ";

    /** Prefix for dates after in the parameter drivers' name. */
    public static final String DATE_AFTER = " - After ";

    /** Atmospheric model. */
    private final Atmosphere atmosphere;

    /** TimeSpanMap of DragSensitive objects. */
    private final TimeSpanMap<DragSensitive> dragSensitiveTimeSpanMap;

    /** Time scale used for the default names of the drag parameter drivers. */
    private final TimeScale timeScale;

    /** Constructor with default UTC time scale for the default names of the drag parameter drivers.
     * @param atmosphere atmospheric model
     * @param spacecraft Time scale used for the default names of the drag parameter drivers
     */
    @DefaultDataContext
    public TimeSpanDragForce(final Atmosphere atmosphere,
                             final DragSensitive spacecraft) {
        super(atmosphere);
        this.atmosphere = atmosphere;
        this.dragSensitiveTimeSpanMap = new TimeSpanMap<>(spacecraft);
        this.timeScale = TimeScalesFactory.getUTC();
    }

    /** Constructor.
     * @param atmosphere atmospheric model
     * @param spacecraft the initial object physical and geometric information
     * @param timeScale Time scale used for the default names of the drag parameter drivers
     */
    public TimeSpanDragForce(final Atmosphere atmosphere,
                             final DragSensitive spacecraft,
                             final TimeScale timeScale) {
        super(atmosphere);
        this.atmosphere = atmosphere;
        this.dragSensitiveTimeSpanMap = new TimeSpanMap<>(spacecraft);
        this.timeScale = timeScale;
    }

    /** Add a DragSensitive entry valid before a limit date.<br>
     * Using <code>addDragSensitiveValidBefore(entry, t)</code> will make <code>entry</code>
     * valid in ]-∞, t[ (note the open bracket).
     * @param dragSensitive DragSensitive entry
     * @param latestValidityDate date before which the entry is valid
     * (must be different from <b>all</b> dates already used for transitions)
     */
    public void addDragSensitiveValidBefore(final DragSensitive dragSensitive, final AbsoluteDate latestValidityDate) {
        dragSensitiveTimeSpanMap.addValidBefore(changeDragParameterDriversNames(dragSensitive,
                                                                                latestValidityDate,
                                                                                DATE_BEFORE),
                                                latestValidityDate, false);
    }

    /** Add a DragSensitive entry valid after a limit date.<br>
     * Using <code>addDragSensitiveValidAfter(entry, t)</code> will make <code>entry</code>
     * valid in [t, +∞[ (note the closed bracket).
     * @param dragSensitive DragSensitive entry
     * @param earliestValidityDate date after which the entry is valid
     * (must be different from <b>all</b> dates already used for transitions)
     */
    public void addDragSensitiveValidAfter(final DragSensitive dragSensitive, final AbsoluteDate earliestValidityDate) {
        dragSensitiveTimeSpanMap.addValidAfter(changeDragParameterDriversNames(dragSensitive,
                                                                               earliestValidityDate,
                                                                               DATE_AFTER),
                                               earliestValidityDate, false);
    }

    /** Get the {@link DragSensitive} model valid at a date.
     * @param date the date of validity
     * @return the DragSensitive model valid at date
     */
    public DragSensitive getDragSensitive(final AbsoluteDate date) {
        return dragSensitiveTimeSpanMap.get(date);
    }

    /** Get the {@link DragSensitive} {@link Span} containing a specified date.
     * @param date date belonging to the desired time span
     * @return the DragSensitive time span containing the specified date
     */
    public Span<DragSensitive> getDragSensitiveSpan(final AbsoluteDate date) {
        return dragSensitiveTimeSpanMap.getSpan(date);
    }

    /** Extract a range of the {@link DragSensitive} map.
     * <p>
     * The object returned will be a new independent instance that will contain
     * only the transitions that lie in the specified range.
     * </p>
     * See the {@link TimeSpanMap#extractRange TimeSpanMap.extractRange method} for more.
     * @param start earliest date at which a transition is included in the range
     * (may be set to {@link AbsoluteDate#PAST_INFINITY} to keep all early transitions)
     * @param end latest date at which a transition is included in the r
     * (may be set to {@link AbsoluteDate#FUTURE_INFINITY} to keep all late transitions)
     * @return a new TimeSpanMap instance of DragSensitive with all transitions restricted to the specified range
     */
    public TimeSpanMap<DragSensitive> extractDragSensitiveRange(final AbsoluteDate start, final AbsoluteDate end) {
        return dragSensitiveTimeSpanMap.extractRange(start, end);
    }

    /** Get the first {@link Span time span} of the drag sensitive time span map.
     * @return the first {@link Span time span} of the drag sensitive time span map
     * @since 11.1
     */
    public Span<DragSensitive> getFirstSpan() {
        return dragSensitiveTimeSpanMap.getFirstSpan();
    }

    /** {@inheritDoc} */
    @Override
    public Vector3D acceleration(final SpacecraftState s, final double[] parameters) {

        // Local atmospheric density
        final AbsoluteDate date     = s.getDate();
        final Frame        frame    = s.getFrame();
        final Vector3D     position = s.getPosition();
        final double rho    = atmosphere.getDensity(date, position, frame);

        // Spacecraft relative velocity with respect to the atmosphere
        final Vector3D vAtm = atmosphere.getVelocity(date, position, frame);
        final Vector3D relativeVelocity = vAtm.subtract(s.getPVCoordinates().getVelocity());

        // Extract the proper parameters valid at date from the input array
        final double[] extractedParameters = extractParameters(parameters, date);

        // Compute and return drag acceleration
        return getDragSensitive(date).dragAcceleration(s, rho, relativeVelocity, extractedParameters);

    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public <T extends CalculusFieldElement<T>> FieldVector3D<T> acceleration(final FieldSpacecraftState<T> s,
                                                                         final T[] parameters) {
        // Local atmospheric density
        final FieldAbsoluteDate<T> date     = s.getDate();
        final Frame                frame    = s.getFrame();
        final FieldVector3D<T>     position = s.getPosition();

        // Density and its derivatives
        final T rho;

        // Check for faster computation dedicated to derivatives with respect to state
        // Using finite differences instead of automatic differentiation as it seems to be much
        // faster for the drag's derivatives' computation
        if (isGradientStateDerivative(s)) {
            rho = (T) this.getGradientDensityWrtStateUsingFiniteDifferences(date.toAbsoluteDate(), frame, (FieldVector3D<Gradient>) position);
        } else if (isDSStateDerivative(s)) {
            rho = (T) this.getDSDensityWrtStateUsingFiniteDifferences(date.toAbsoluteDate(), frame, (FieldVector3D<DerivativeStructure>) position);
        } else {
            rho = atmosphere.getDensity(date, position, frame);
        }

        // Spacecraft relative velocity with respect to the atmosphere
        final FieldVector3D<T> vAtm = atmosphere.getVelocity(date, position, frame);
        final FieldVector3D<T> relativeVelocity = vAtm.subtract(s.getPVCoordinates().getVelocity());

        // Extract the proper parameters valid at date from the input array
        final T[] extractedParameters = extractParameters(parameters, date);

        // Compute and return drag acceleration
        return getDragSensitive(date.toAbsoluteDate()).dragAcceleration(s, rho, relativeVelocity, extractedParameters);
    }

    /**{@inheritDoc}
     * <p>
     * A date detector is used to cleanly stop the propagator and reset
     * the state derivatives at transition dates.
     * </p>
     */
    @Override
    public Stream<EventDetector> getEventDetectors() {

        // Get the transitions' dates from the TimeSpanMap
        final AbsoluteDate[] transitionDates = getTransitionDates();

        // Initialize the date detector
        final DateDetector datesDetector = new DateDetector(transitionDates[0]).
                        withMaxCheck(60.).
                        withHandler((state, detector, increasing) -> Action.RESET_DERIVATIVES);
        // Add all transitions' dates to the date detector
        for (int i = 1; i < transitionDates.length; i++) {
            datesDetector.addEventDate(transitionDates[i]);
        }

        // Return the detector
        return Stream.of(datesDetector);
    }

    /** {@inheritDoc}
     * <p>
     * A date detector is used to cleanly stop the propagator and reset
     * the state derivatives at transition dates.
     * </p>
     */
    @Override
    public <T extends CalculusFieldElement<T>> Stream<FieldEventDetector<T>> getFieldEventDetectors(final Field<T> field) {

        // Get the transitions' dates from the TimeSpanMap
        final AbsoluteDate[] transitionDates = getTransitionDates();

        // Initialize the date detector
        @SuppressWarnings("unchecked")
        final FieldDateDetector<T> datesDetector =
                        new FieldDateDetector<>(field, (FieldTimeStamped<T>[]) Array.newInstance(FieldTimeStamped.class, 0)).
                        withMaxCheck(60.0).
                        withHandler((FieldSpacecraftState<T> state, FieldEventDetector<T> detector, boolean increasing) ->
                                    Action.RESET_DERIVATIVES);
        // Add all transitions' dates to the date detector
        for (int i = 0; i < transitionDates.length; i++) {
            datesDetector.addEventDate(new FieldAbsoluteDate<>(field, transitionDates[i]));
        }

        // Return the detector
        return Stream.of(datesDetector);
    }

    /** {@inheritDoc}
     * <p>
     * All the parameter drivers of all DragSensitive models are returned in an array.
     * Models are ordered chronologically.
     * </p>
     */
    @Override
    public List<ParameterDriver> getParametersDrivers() {

        // Get all transitions from the TimeSpanMap
        final List<ParameterDriver> listParameterDrivers = new ArrayList<>();

        // Loop on the spans
        for (Span<DragSensitive> span = getFirstSpan(); span != null; span = span.next()) {
            // Add all the parameter drivers of the span
            for (ParameterDriver driver : span.getData().getDragParametersDrivers()) {
                // Add the driver only if the name does not exist already
                if (!findByName(listParameterDrivers, driver.getName())) {
                    listParameterDrivers.add(driver);
                }
            }
        }

        // Return an array of parameter drivers with no duplicated name
        return listParameterDrivers;

    }

    /** Extract the proper parameter drivers' values from the array in input of the
     * {@link #acceleration(SpacecraftState, double[]) acceleration} method.
     *  Parameters are filtered given an input date.
     * @param parameters the input parameters array
     * @param date the date
     * @return the parameters given the date
     */
    public double[] extractParameters(final double[] parameters, final AbsoluteDate date) {

        // Get the drag parameter drivers of the date
        final List<ParameterDriver> dragParameterDriver = getDragSensitive(date).getDragParametersDrivers();

        // Find out the indexes of the parameters in the whole array of parameters
        final List<ParameterDriver> allParameters = getParametersDrivers();
        final double[] outParameters = new double[dragParameterDriver.size()];
        int index = 0;
        for (int i = 0; i < allParameters.size(); i++) {
            final String driverName = allParameters.get(i).getName();
            for (ParameterDriver dragDriver : dragParameterDriver) {
                if (dragDriver.getName().equals(driverName)) {
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

        // Get the drag parameter drivers of the date
        final List<ParameterDriver> dragPD = getDragSensitive(date.toAbsoluteDate()).getDragParametersDrivers();

        // Find out the indexes of the parameters in the whole array of parameters
        final List<ParameterDriver> allParameters = getParametersDrivers();
        final T[] outParameters = MathArrays.buildArray(date.getField(), dragPD.size());
        int index = 0;
        for (int i = 0; i < allParameters.size(); i++) {
            final String driverName = allParameters.get(i).getName();
            for (ParameterDriver dragDriver : dragPD) {
                if (dragDriver.getName().equals(driverName)) {
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

    /** Get the dates of the transitions for the drag sensitive models {@link TimeSpanMap}.
     * @return dates of the transitions for the drag sensitive models {@link TimeSpanMap}
     */
    private AbsoluteDate[] getTransitionDates() {

        // Get all transitions
        final List<AbsoluteDate> listDates = new ArrayList<>();

        // Extract all the transitions' dates
        for (Transition<DragSensitive> transition = getFirstSpan().getEndTransition(); transition != null; transition = transition.next()) {
            listDates.add(transition.getDate());
        }
        // Return the array of transition dates
        return listDates.toArray(new AbsoluteDate[0]);
    }

    /** Change the parameter drivers names of a {@link DragSensitive} model, if needed.
     * <p>
     * This is done to avoid that several parameter drivers have the same name.<br>
     * It is done only if the user hasn't modify the DragSensitive parameter drivers default names.
     * </p>
     * @param dragSensitive the DragSensitive model
     * @param date the date used in the parameter driver's name
     * @param datePrefix the date prefix used in the parameter driver's name
     * @return the DragSensitive with its drivers' names changed
     */
    private DragSensitive changeDragParameterDriversNames(final DragSensitive dragSensitive,
                                                          final AbsoluteDate date,
                                                          final String datePrefix) {
        // Loop on the parameter drivers of the DragSensitive model
        for (ParameterDriver driver: dragSensitive.getDragParametersDrivers()) {
            final String driverName = driver.getName();

            // If the name is the default name for DragSensitive parameter drivers
            // Modify the name to add the prefix and the date
            if (driverName.equals(DragSensitive.GLOBAL_DRAG_FACTOR) ||
                driverName.equals(DragSensitive.DRAG_COEFFICIENT) ||
                driverName.equals(DragSensitive.LIFT_RATIO)) {
                driver.setName(driverName + datePrefix + date.toString(timeScale));
            }
        }
        return dragSensitive;
    }

}
