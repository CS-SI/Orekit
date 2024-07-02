/* Contributed in the public domain.
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

import java.io.Serializable;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.geometry.euclidean.threed.FieldRotation;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.RotationConvention;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.annotation.DefaultDataContext;
import org.orekit.data.DataContext;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitInternalError;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeScalarFunction;
import org.orekit.time.TimeScales;
import org.orekit.utils.IERSConventions;

/**
 * An inertial frame aligned with the ecliptic.
 * <p>
 * The IAU defines the ecliptic as "the plane perpendicular to the mean heliocentric
 * orbital angular momentum vector of the Earth-Moon barycentre in the BCRS (IAU 2006
 * Resolution B1)." The +z axis is aligned with the angular momentum vector, and the +x
 * axis is aligned with +x axis of {@link Frames#getMOD(IERSConventions) MOD}.
 * </p>
 *
 * <p>
 * This implementation agrees with the JPL 406 ephemerides to within 0.5 arc seconds.
 * </p>
 *
 * @since 7.0
 */
public class EclipticProvider implements TransformProvider {

    /** Serializable UID. */
    private static final long serialVersionUID = 20140516L;

    /** IERS conventions. */
    private final IERSConventions conventions;

    /** the obliquity of the ecliptic, in radians as a function of time. */
    private final transient TimeScalarFunction obliquity;

    /**
     * Create a transform provider from MOD to an ecliptically aligned frame.
     *
     * <p>This constructor uses the {@link DataContext#getDefault() default data context}.
     *
     * @param conventions IERS conventions
     * @see #EclipticProvider(IERSConventions, TimeScales)
     */
    @DefaultDataContext
    public EclipticProvider(final IERSConventions conventions) {
        this(conventions, DataContext.getDefault().getTimeScales());
    }

    /**
     * Create a transform provider from MOD to an ecliptically aligned frame.
     * @param conventions IERS conventions
     * @param timeScales to use in computing the transformation.
     * @since 10.1
     */
    public EclipticProvider(final IERSConventions conventions,
                            final TimeScales timeScales) {
        this.conventions = conventions;
        this.obliquity   = conventions.getMeanObliquityFunction(timeScales);
    }

    @Override
    public Transform getTransform(final AbsoluteDate date) {
        //mean obliquity of date
        final double epsA = obliquity.value(date);
        return new Transform(date, new Rotation(Vector3D.MINUS_I, epsA, RotationConvention.VECTOR_OPERATOR));
    }

    @Override
    public <T extends CalculusFieldElement<T>> FieldTransform<T> getTransform(final FieldAbsoluteDate<T> date) {
        //mean obliquity of date
        final T epsA = obliquity.value(date);
        return new FieldTransform<>(date, new FieldRotation<>(FieldVector3D.getMinusI(date.getField()),
                                                              epsA,
                                                              RotationConvention.VECTOR_OPERATOR));
    }

    /** Replace the instance with a data transfer object for serialization.
     * <p>
     * This intermediate class serializes only the frame key.
     * </p>
     * @return data transfer object that will be serialized
     */
    @DefaultDataContext
    private Object writeReplace() {
        return new DataTransferObject(conventions);
    }

    /** Internal class used only for serialization. */
    @DefaultDataContext
    private static class DataTransferObject implements Serializable {

        /** Serializable UID. */
        private static final long serialVersionUID = 20140516L;

        /** IERS conventions. */
        private final IERSConventions conventions;

        /** Simple constructor.
         * @param conventions IERS conventions
         */
        DataTransferObject(final IERSConventions conventions) {
            this.conventions = conventions;
        }

        /** Replace the deserialized data transfer object with a {@link EclipticProvider}.
         * @return replacement {@link EclipticProvider}
         */
        private Object readResolve() {
            try {
                // retrieve a transform
                return new EclipticProvider(conventions);
            } catch (OrekitException oe) {
                throw new OrekitInternalError(oe);
            }
        }

    }

}
