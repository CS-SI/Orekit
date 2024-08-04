/* Copyright 2002-2024 Luc Maisonobe
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
package org.orekit.attitudes;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.analysis.differentiation.FieldUnivariateDerivative2;
import org.hipparchus.analysis.differentiation.UnivariateDerivative2;
import org.hipparchus.analysis.differentiation.UnivariateDerivative2Field;
import org.hipparchus.geometry.euclidean.threed.FieldRotation;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.AngularCoordinates;
import org.orekit.utils.ExtendedPVCoordinatesProvider;
import org.orekit.utils.FieldAngularCoordinates;
import org.orekit.utils.FieldPVCoordinatesProvider;
import org.orekit.utils.PVCoordinatesProvider;
import org.orekit.utils.TimeStampedFieldPVCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;

import java.util.HashMap;
import java.util.Map;

/**
 * Attitude provider with one satellite vector aligned and another one constrained to two targets.
 * @author Luc Maisonobe
 * @since 12.2
 */
public class AlignedAndConstrained implements AttitudeProvider
{

    /** Satellite vector for primary target. */
    private final FieldVector3D<UnivariateDerivative2> primarySat;

    /** Primary target. */
    private final TargetProvider primaryTarget;

    /** Satellite vector for secondary target. */
    private final FieldVector3D<UnivariateDerivative2> secondarySat;

    /** Secondary target. */
    private final TargetProvider secondaryTarget;

    /** Sun model. */
    private final ExtendedPVCoordinatesProvider sun;

    /** Earth model. */
    private final OneAxisEllipsoid earth;

    /** Cached field-based satellite vectors. */
    private final transient Map<Field<? extends CalculusFieldElement<?>>, Cache<? extends CalculusFieldElement<?>>>
        cachedSatelliteVectors;

    /**
     * Simple constructor.
     * @param primarySat      satellite vector for primary target
     * @param primaryTarget   primary target
     * @param secondarySat    satellite vector for secondary target
     * @param secondaryTarget secondary target
     * @param sun             Sun model
     * @param earth           Earth model
     */
    public AlignedAndConstrained(final Vector3D primarySat, final TargetProvider primaryTarget,
                                 final Vector3D secondarySat, final TargetProvider secondaryTarget,
                                 final ExtendedPVCoordinatesProvider sun,
                                 final OneAxisEllipsoid earth)
    {
        this.primarySat             = new FieldVector3D<>(UnivariateDerivative2Field.getInstance(), primarySat);
        this.primaryTarget          = primaryTarget;
        this.secondarySat           = new FieldVector3D<>(UnivariateDerivative2Field.getInstance(), secondarySat);
        this.secondaryTarget        = secondaryTarget;
        this.sun                    = sun;
        this.earth                  = earth;
        this.cachedSatelliteVectors = new HashMap<>();
    }

    /** {@inheritDoc} */
    @Override
    public Attitude getAttitude(final PVCoordinatesProvider pvProv,
                                final AbsoluteDate date,
                                final Frame frame)
    {
        final TimeStampedPVCoordinates satPV = pvProv.getPVCoordinates(date, frame);

        // compute targets references at the specified date
        final FieldVector3D<UnivariateDerivative2> primaryDirection   = primaryTarget.getTargetDirection(sun, earth, satPV, frame);
        final FieldVector3D<UnivariateDerivative2> secondaryDirection = secondaryTarget.getTargetDirection(sun, earth, satPV, frame);

        // compute transform from inertial frame to satellite frame
        final FieldRotation<UnivariateDerivative2> inertToSatRotation =
            new FieldRotation<>(primaryDirection, secondaryDirection, primarySat, secondarySat);

        // build the attitude
        return new Attitude(date, frame, new AngularCoordinates(inertToSatRotation));

    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> FieldAttitude<T> getAttitude(final FieldPVCoordinatesProvider<T> pvProv,
                                                                            final FieldAbsoluteDate<T> date,
                                                                            final Frame frame)
    {
        // get the satellite vectors for specified field
        @SuppressWarnings("unchecked")
        final Cache<T> satVectors =
            (Cache<T>) cachedSatelliteVectors.computeIfAbsent(date.getField(),
                                                              f -> new Cache<>(date.getField(), primarySat, secondarySat));

        final TimeStampedFieldPVCoordinates<T> satPV = pvProv.getPVCoordinates(date, frame);

        // compute targets references at the specified date
        final FieldVector3D<FieldUnivariateDerivative2<T>> primaryDirection   = primaryTarget.getTargetDirection(sun, earth, satPV, frame);
        final FieldVector3D<FieldUnivariateDerivative2<T>> secondaryDirection = secondaryTarget.getTargetDirection(sun, earth, satPV, frame);

        // compute transform from inertial frame to satellite frame
        final FieldRotation<FieldUnivariateDerivative2<T>> inertToSatRotation =
            new FieldRotation<>(primaryDirection, secondaryDirection, satVectors.primarySat, satVectors.secondarySat);

        // build the attitude
        return new FieldAttitude<>(date, frame, new FieldAngularCoordinates<>(inertToSatRotation));

    }

    /** Container for cached satellite vectors. */
    private static class Cache<T extends CalculusFieldElement<T>> {

        /** Satellite vector for primary target. */
        private final FieldVector3D<FieldUnivariateDerivative2<T>> primarySat;

        /** Satellite vector for primary target. */
        private final FieldVector3D<FieldUnivariateDerivative2<T>> secondarySat;

        /** Simple constructor.
         * @param field field to which the elements belong
         * @param primarySat satellite vector for primary target
         * @param secondarySat satellite vector for primary target
         */
        Cache(final Field<T> field,
              final FieldVector3D<UnivariateDerivative2> primarySat,
              final FieldVector3D<UnivariateDerivative2> secondarySat) {
            final FieldUnivariateDerivative2<T> zero =
                new FieldUnivariateDerivative2<>(field.getZero(), field.getZero(), field.getZero());
            this.primarySat   = new FieldVector3D<>(zero.newInstance(primarySat.getX().getValue()),
                                                    zero.newInstance(primarySat.getY().getValue()),
                                                    zero.newInstance(primarySat.getZ().getValue()));
            this.secondarySat = new FieldVector3D<>(zero.newInstance(secondarySat.getX().getValue()),
                                                    zero.newInstance(secondarySat.getY().getValue()),
                                                    zero.newInstance(secondarySat.getZ().getValue()));
        }

    }

}
