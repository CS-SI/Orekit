/* Copyright 2025-2026 Hawkeye 360 (HE360)
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

package org.orekit.estimation.measurements;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hipparchus.Field;
import org.hipparchus.analysis.differentiation.Gradient;
import org.hipparchus.util.FastMath;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.FieldCartesianOrbit;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.clocks.QuadraticClockModel;
import org.orekit.utils.AbsolutePVCoordinates;
import org.orekit.utils.FieldAbsolutePVCoordinates;
import org.orekit.utils.FieldPVCoordinatesProvider;
import org.orekit.utils.PVCoordinatesProvider;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.TimeStampedFieldPVCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;

/** Abstract class underlying both observed and observing measurement
 * objects.  Contains the QuadraticClockModel and the ability to store a
 * master list of all parameter drivers associated with the object.
 *
 * @author Brianna Aubin
 * @since 14.0
 */
public abstract class AbstractMeasurementObject implements MeasurementParticipant {

    /** Checkstyle is annoying sometimes. */
    private static final String CLOCK_STRING = "-clock";

    /** Clock offset scaling factor.
     * <p>
     * We use a power of 2 to avoid numeric noise introduction
     * in the multiplications/divisions sequences.
     * </p>
     */
    private static final double CLOCK_OFFSET_SCALE = FastMath.scalb(1.0, -10);

    /** Stores quadratic clock model. */
    private final QuadraticClockModel quadraticClockModel;

    /** Stores list of all ParameterDriver values. */
    private final List<ParameterDriver> parameterDrivers = new ArrayList<>();

    /** Name of the satellite. */
    private final String name;

    /** Simple constructor.
     * @param name name of MeasurementObject
     */
    protected AbstractMeasurementObject(final String name) {
        this(name, createEmptyQuadraticClock(name));
    }

    /** Simple constructor.
     * @param name name of MeasurementObject
     * @param quadraticClock clock belonging to MeasurementObject
     */
    protected AbstractMeasurementObject(final String name, final QuadraticClockModel quadraticClock) {

        // Initialize member variables
        this.name = name;
        this.quadraticClockModel = quadraticClock;

        // Add clock parameters
        parameterDrivers.add(quadraticClockModel.getClockOffsetDriver());
        parameterDrivers.add(quadraticClockModel.getClockDriftDriver());
        parameterDrivers.add(quadraticClockModel.getClockAccelerationDriver());
    }

    /** Get the MeasurementObject name.
     * @return name for the object
     */
    public final String getName() {
        return name;
    }

    /** Creates an quadratic clock with zero displacement.
     * @param name name of object that is holding the clock
     * @return new quadratic clock model
     */
    protected static QuadraticClockModel createEmptyQuadraticClock(final String name) {
        return new QuadraticClockModel(new ParameterDriver(name + CLOCK_STRING + OFFSET_SUFFIX,
                                                    0.0, CLOCK_OFFSET_SCALE,
                                                    Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY),
                                           new ParameterDriver(name + CLOCK_STRING + DRIFT_SUFFIX,
                                                    0.0, CLOCK_OFFSET_SCALE,
                                                    Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY),
                                           new ParameterDriver(name + CLOCK_STRING + ACCELERATION_SUFFIX,
                                                    0.0, CLOCK_OFFSET_SCALE,
                                                    Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY));
    }

    /** Get a quadratic clock model valid at some date.
     * @return quadratic clock model
     */
    public final QuadraticClockModel getQuadraticClockModel() {
        return quadraticClockModel;
    }

    /** Get model parameters.
     * @return model parameters, will throw an
     * exception if one PDriver has several values driven. If
     * it's the case (if at least 1 PDriver of the model has several values
     * driven) the method {@link org.orekit.utils.ParameterDriversProvider#getParameters(AbsoluteDate)} must be used.
     */
    public List<ParameterDriver> getParametersDrivers() {
        return Collections.unmodifiableList(parameterDrivers);
    }

    /**
     * Add a single parameter.
     * @param parameterDriver parameter being added to the MeasurementObject
     */
    protected final void addParameterDriver(final ParameterDriver parameterDriver) {
        parameterDrivers.add(parameterDriver);
    }

    /**
     * Create PV provider from position-velocity-acceleration vector and template state.
     * @param templateState template state
     * @param pvCoordinates position-velocity-acceleration
     * @return position-velocity-acceleration provider
     */
    public static PVCoordinatesProvider extractPVCoordinatesProvider(final SpacecraftState templateState,
                                                                     final TimeStampedPVCoordinates pvCoordinates) {
        if (templateState.isOrbitDefined()) {
            final CartesianOrbit cartesianOrbit = new CartesianOrbit(pvCoordinates, templateState.getFrame(),
                    templateState.getOrbit().getMu());
            return templateState.getOrbit().getType().convertType(cartesianOrbit);
        } else {
            return new AbsolutePVCoordinates(templateState.getFrame(), pvCoordinates);
        }
    }

    /**
     * Create PV provider from position-velocity-acceleration vector and template state.
     *
     * @param templateState template state
     * @param pvCoordinates position-velocity-acceleration
     * @return position-velocity-acceleration provider
     */
    public static FieldPVCoordinatesProvider<Gradient> extractFieldPVCoordinatesProvider(final SpacecraftState templateState,
                                                                                         final TimeStampedFieldPVCoordinates<Gradient> pvCoordinates) {
        final Field<Gradient> field = pvCoordinates.getDate().getField();
        if (templateState.isOrbitDefined()) {
            final FieldCartesianOrbit<Gradient> cartesianOrbit = new FieldCartesianOrbit<>(pvCoordinates, templateState.getFrame(),
                    field.getZero().newInstance(templateState.getOrbit().getMu()));
            return templateState.getOrbit().getType().convertType(cartesianOrbit);
        } else {
            return new FieldAbsolutePVCoordinates<>(templateState.getFrame(), pvCoordinates);
        }
    }
}
