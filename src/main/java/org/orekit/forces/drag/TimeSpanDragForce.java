/* Copyright 2002-2020 CS Group
 * Licensed to CS Group (CS) under one or more
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

import java.util.ArrayList;
import java.util.List;
import java.util.NavigableSet;
import java.util.stream.Stream;

import org.hipparchus.Field;
import org.hipparchus.RealFieldElement;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.ode.events.Action;
import org.hipparchus.util.MathArrays;
import org.orekit.forces.AbstractForceModel;
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
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.TimeSpanMap;
import org.orekit.utils.TimeSpanMap.Span;
import org.orekit.utils.TimeSpanMap.Transition;


/** Atmospheric drag force model.
 *
 * The drag acceleration is computed as follows :
 *
 * γ = (1/2 * ρ * V² * S / Mass) * DragCoefVector
 *
 * With DragCoefVector = {C<sub>x</sub>, C<sub>y</sub>, C<sub>z</sub>} and S given by the user through the interface
 * {@link DragSensitive}
 *
 * @author Maxime Journot
 */

public class TimeSpanDragForce extends AbstractForceModel {

    /** Prefix for dates before in the parameter drivers' name. */
    public static final String DATE_BEFORE = " - Before ";

    /** Prefix for dates after in the parameter drivers' name. */
    public static final String DATE_AFTER = " - After ";

    /** Atmospheric model. */
    private final Atmosphere atmosphere;

    /** Spacecraft. */
    private final TimeSpanMap<DragSensitive> dragSensitiveTimeSpanMap;

    /** Simple constructor.
     * @param atmosphere atmospheric model
     * @param spacecraft the object physical and geometrical information
     */
    public TimeSpanDragForce(final Atmosphere atmosphere, final DragSensitive spacecraft) {
        this.atmosphere = atmosphere;
        this.dragSensitiveTimeSpanMap = new TimeSpanMap<>(spacecraft);
    }

    /**
     * @param dragSensitive d
     * @param latestValidityDate l
     */
    public void addDragSensitiveValidBefore(final DragSensitive dragSensitive, final AbsoluteDate latestValidityDate) {
        dragSensitiveTimeSpanMap.addValidBefore(changeDragParameterDriversNames(dragSensitive,
                                                                                latestValidityDate,
                                                                                DATE_BEFORE),
                                                latestValidityDate);
    }

    /**
     * @param dragSensitive d
     * @param earliestValidityDate e
     */
    public void addDragSensitiveValidAfter(final DragSensitive dragSensitive, final AbsoluteDate earliestValidityDate) {
        dragSensitiveTimeSpanMap.addValidAfter(changeDragParameterDriversNames(dragSensitive,
                                                                               earliestValidityDate,
                                                                               DATE_AFTER),
                                               earliestValidityDate);
    }

    /**
     * @param date d
     * @return r
     */
    public DragSensitive getDragSensitive(final AbsoluteDate date) {
        return dragSensitiveTimeSpanMap.get(date);
    }

    /**
     * @param date d
     * @return r
     */
    public Span<DragSensitive> getDragSensitiveSpan(final AbsoluteDate date) {
        return dragSensitiveTimeSpanMap.getSpan(date);
    }

    /**
     * @param start s
     * @param end e
     * @return d
     */
    public TimeSpanMap<DragSensitive> extractDragSensitiveRange(final AbsoluteDate start, final AbsoluteDate end) {
        return dragSensitiveTimeSpanMap.extractRange(start, end);
    }

    /** Get the transitions for the drag sensitive time span map.
     * @return the transitions for the drag sensitive time span map
     */
    public NavigableSet<Transition<DragSensitive>> getTransitions() {
        return dragSensitiveTimeSpanMap.getTransitions();
    }

    /** {@inheritDoc} */
    @Override
    public boolean dependsOnPositionOnly() {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public Vector3D acceleration(final SpacecraftState s, final double[] parameters) {

        final AbsoluteDate date     = s.getDate();
        final Frame        frame    = s.getFrame();
        final Vector3D     position = s.getPVCoordinates().getPosition();

        final double rho    = atmosphere.getDensity(date, position, frame);
        final Vector3D vAtm = atmosphere.getVelocity(date, position, frame);
        final Vector3D relativeVelocity = vAtm.subtract(s.getPVCoordinates().getVelocity());

        //FIXME: Extract proper parameterdriver
        final double[] extractedParameters = extractParameters(parameters, date);

        return getDragSensitive(date).dragAcceleration(date, frame, position, s.getAttitude().getRotation(),
                                                       s.getMass(), rho, relativeVelocity, extractedParameters);

    }

    /** {@inheritDoc} */
    @Override
    public <T extends RealFieldElement<T>> FieldVector3D<T> acceleration(final FieldSpacecraftState<T> s,
                                                                         final T[] parameters) {

        final FieldAbsoluteDate<T> date     = s.getDate();
        final Frame                frame    = s.getFrame();
        final FieldVector3D<T>     position = s.getPVCoordinates().getPosition();

        // Density and its derivatives
//        final T rho;
//
//        // Check for faster computation dedicated to derivatives with respect to state
//        // Using finite differences instead of automatic differentiation as it seems to be much
//        // faster for the drag's derivatives' computation
//        if (isStateDerivative(s)) {
//            rho = this.getDensityWrtStateUsingFiniteDifferences(date.toAbsoluteDate(), frame, position);
//        } else {
//            rho    = atmosphere.getDensity(date, position, frame);
//        }

        final T rho = atmosphere.getDensity(date, position, frame);

        // Spacecraft relative velocity with respect to the atmosphere
        final FieldVector3D<T> vAtm = atmosphere.getVelocity(date, position, frame);
        final FieldVector3D<T> relativeVelocity = vAtm.subtract(s.getPVCoordinates().getVelocity());

        // Drag acceleration along with its derivatives
//        return getDragSensitive(date.toAbsoluteDate()).dragAcceleration(date, frame, position, s.getAttitude().getRotation(),
//                                                                        s.getMass(), rho, relativeVelocity, parameters);
        //FIXME: Extracted parameters
        final T[] extractedParameters = extractParameters(parameters, date);
        return getDragSensitive(date.toAbsoluteDate()).dragAcceleration(date, frame, position, s.getAttitude().getRotation(),
                                                                        s.getMass(), rho, relativeVelocity, extractedParameters);

    }

    /** {@inheritDoc} */
    @Override
    public Stream<EventDetector> getEventsDetectors() {
        final AbsoluteDate[] transitionDates = getTransitionDates();
        final DateDetector datesDetector = new DateDetector(transitionDates[0]).
                        withMaxCheck(60.).
                        withHandler((SpacecraftState state, DateDetector d, boolean increasing) -> {
                            return Action.RESET_DERIVATIVES;
                        });
        for (int i = 1; i < transitionDates.length; i++) {
            datesDetector.addEventDate(transitionDates[i]);
        }

        return Stream.of(datesDetector);
    }

    /** {@inheritDoc} */
    @Override
    public <T extends RealFieldElement<T>> Stream<FieldEventDetector<T>> getFieldEventsDetectors(final Field<T> field) {
        final AbsoluteDate[] transitionDates = getTransitionDates();
        final FieldDateDetector<T> datesDetector =
                        new FieldDateDetector<>(new FieldAbsoluteDate<>(field, transitionDates[0])).
                        withMaxCheck(field.getZero().add(60.)).
                        withHandler((FieldSpacecraftState<T> state, FieldDateDetector<T> d, boolean increasing) -> {
                            return Action.RESET_DERIVATIVES;
                        });
        for (int i = 1; i < transitionDates.length; i++) {
            datesDetector.addEventDate(new FieldAbsoluteDate<>(field, transitionDates[i]));
        }

        return Stream.of(datesDetector);
    }

    /** {@inheritDoc} */
    @Override
    public ParameterDriver[] getParametersDrivers() {

        final List<ParameterDriver> listParameterDrivers = new ArrayList<>();
        final NavigableSet<Transition<DragSensitive>> dragSensitiveTransitions =  getTransitions();

        for (Transition<DragSensitive> transition : dragSensitiveTransitions) {
            for (ParameterDriver driver : transition.getBefore().getDragParametersDrivers()) {
                // Adds only if the name does not exist already
                if (!findByName(listParameterDrivers, driver.getName())) {
                    listParameterDrivers.add(driver);
                }
            }
        }
        for (ParameterDriver driver : dragSensitiveTransitions.last().getAfter().getDragParametersDrivers()) {
            // Adds only if the name does not exist already
            if (!findByName(listParameterDrivers, driver.getName())) {
                listParameterDrivers.add(driver);
            }
        }

        return listParameterDrivers.toArray(new ParameterDriver[0]);

    }

    /**
     * @param driversList d
     * @param name n
     * @return r
     */
    private boolean findByName(final List<ParameterDriver> driversList, final String name) {
        for (final ParameterDriver d : driversList) {
            if (d.getName().equals(name)) {
                return true;
            }
        }
        return false;
    }

    /** Get the dates of the transitions for the drag sensitive models.
     * @return dates of the transitions for the drag sensitive models
     */
    private AbsoluteDate[] getTransitionDates() {
        final List<AbsoluteDate> listDates = new ArrayList<>();
        final NavigableSet<Transition<DragSensitive>> dragSensitiveTransitions =  getTransitions();

        for (Transition<DragSensitive> transition : dragSensitiveTransitions) {
            listDates.add(transition.getDate());
        }
        return listDates.toArray(new AbsoluteDate[0]);
    }

    /** Extract the proper parameter drivers' values in input of acceleration methods.
     *  Parameters are filtered given the date
     * @param parameters the parameters array
     * @param date the date
     * @return the parameters given the date
     */
    public double[] extractParameters(final double[] parameters, final AbsoluteDate date) {

        // Get the drag parameter drivers of the date
        final ParameterDriver[] dragPD = getDragSensitive(date).getDragParametersDrivers();

        // Find out the indexes of the parameters in the whole array of parameters
        final ParameterDriver[] allParameters = getParametersDrivers();
        final double[] outParameters = new double[dragPD.length];
        int index = 0;
        for (int i = 0; i < allParameters.length; i++) {
            final String driverName = allParameters[i].getName();
            for (ParameterDriver dragDriver : dragPD) {
                if (dragDriver.getName().equals(driverName)) {
                    outParameters[index++] = parameters[i];
                }
            }
        }
        return outParameters;
    }

    /** Extract the proper parameter drivers' values in input of acceleration methods.
     *  Parameters are filtered given the date
     * @param parameters the parameters array
     * @param date the date
     * @param <T> the field
     * @return the parameters given the date
     */
    public <T extends RealFieldElement<T>> T[] extractParameters(final T[] parameters,
                                                                 final FieldAbsoluteDate<T> date) {

        // Get the drag parameter drivers of the date
        final ParameterDriver[] dragPD = getDragSensitive(date.toAbsoluteDate()).getDragParametersDrivers();

        // Find out the indexes of the parameters in the whole array of parameters
        final ParameterDriver[] allParameters = getParametersDrivers();
        final T[] outParameters = MathArrays.buildArray(date.getField(), dragPD.length);
        int index = 0;
        for (int i = 0; i < allParameters.length; i++) {
            final String driverName = allParameters[i].getName();
            for (ParameterDriver dragDriver : dragPD) {
                if (dragDriver.getName().equals(driverName)) {
                    outParameters[index++] = parameters[i];
                }
            }
        }
        return outParameters;
    }

    /**
     * @param dragSensitive d
     * @param date d
     * @param datePrefix p
     * @return r
     */
    private DragSensitive changeDragParameterDriversNames(final DragSensitive dragSensitive,
                                                          final AbsoluteDate date,
                                                          final String datePrefix) {
        for (ParameterDriver driver: dragSensitive.getDragParametersDrivers()) {
            final String driverName = driver.getName();
            if (driverName.equals(DragSensitive.DRAG_COEFFICIENT) || driverName.equals(DragSensitive.LIFT_RATIO)) {
                driver.setName(driverName + datePrefix + date.toString());
            }
        }
        return dragSensitive;
    }
}
