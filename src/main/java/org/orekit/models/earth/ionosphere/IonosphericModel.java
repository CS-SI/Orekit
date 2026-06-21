/* Copyright 2002-2026 CS GROUP
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
package org.orekit.models.earth.ionosphere;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.frames.FieldStaticTransform;
import org.orekit.frames.Frame;
import org.orekit.frames.StaticTransform;
import org.orekit.frames.TopocentricFrame;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.PVCoordinatesProvider;
import org.orekit.utils.ParameterDriversProvider;

/** Defines a ionospheric model, used to calculate the path delay imposed to
 * electro-magnetic signals between an orbital satellite and a ground station.
 * <p>
 * Since 10.0, this interface can be used for models that aspire to estimate
 * ionospheric parameters.
 * </p>
 *
 * @author Joris Olympio
 * @author Bryan Cazabonne
 * @author Luc Maisonobe
 * @author Brianna Aubin
 * @since 13.0.3
 */
public interface IonosphericModel extends ParameterDriversProvider {

    /** Lambda header for calculating the path delay.
     */
    @FunctionalInterface
    interface DelayCalculator {
        /** Apply delay calculation.
         * @param pos position in Earth frame
         * @return path delay
         */
        Double apply(Vector3D pos);
    }

    /** Lambda header for calculating the path delay.
     * @param <T> type of the field element
     */
    @FunctionalInterface
    interface FieldDelayCalculator<T extends CalculusFieldElement<T>> {
        /** Apply delay calculation.
         * @param pos position in Earth frame
         * @return path delay
         */
        T apply(FieldVector3D<T> pos);
    }

    /** Get the earth body shape for earth-frame calculations.
     * @return earth body shape
     * @since 14.0
     */
    OneAxisEllipsoid getEarth();

    /**
     * Calculates the ionospheric path delay for the signal path from an observation
     * object to the satellite being measured.
     * <p>
     * This method is intended to be used for orbit determination issues.
     * In that respect, if the elevation is below 0° the path delay will be equal to zero.
     * </p><p>
     * For individual use of the ionospheric model (i.e. not for orbit determination), another
     * method signature can be implemented to compute the path delay for any elevation angle.
     * </p>
     * @param state          spacecraft state
     * @param coordsProvider coordinates provider for the observing object
     * @param frequency      frequency of the signal in Hz
     * @param parameters     ionospheric model parameters at state date
     * @return the path delay due to the ionosphere in m
     */
    default double pathDelay(final SpacecraftState state,
                             final PVCoordinatesProvider coordsProvider,
                             final double frequency,
                             final double[] parameters) {

        // Solve for the lowest altitude point between p1 and p2
        final OneAxisEllipsoid earth         = getEarth();
        final Frame            bodyFrame     = earth.getFrame();
        final AbsoluteDate     receptionDate = state.getDate();
        final Vector3D         p1            = state.getPVCoordinates(bodyFrame).getPosition();
        final Vector3D         p2            = coordsProvider.getPosition(receptionDate, bodyFrame);
        final GeodeticPoint    lowAltPoint   = earth.lowestAltitudeIntermediate(p1, p2);

        // Solve for the positions of p1 and p2 in the topocentric frame of
        // the lowest altitude point
        final TopocentricFrame baseFrame  = new TopocentricFrame(earth, lowAltPoint, null);
        final StaticTransform  base2Inert = baseFrame.getStaticTransformTo(bodyFrame, receptionDate);
        final Vector3D         localP1    = base2Inert.getInverse().transformPosition(p1);
        final Vector3D         localP2    = base2Inert.getInverse().transformPosition(p2);

        return pathDelay(localP1, localP2, baseFrame, receptionDate, frequency, parameters);
    }

    /**
     * Calculates the ionospheric path delay for the signal path from a ground
     * station to an observing object (ground station or satellite).
     * <p>
     * This method is intended to be used for orbit determination issues.
     * In that respect, if the elevation is below 0° the path delay will be equal to zero.
     * </p><p>
     * For individual use of the ionospheric model (i.e. not for orbit determination), another
     * method signature can be implemented to compute the path delay for any elevation angle.
     * </p>
     * @param localP1       position of path start point in baseFrame
     * @param localP2       position of path end point in baseFrame
     * @param baseFrame     topocentric frame of point with lowest altitude between p1 and p2
     * @param receptionDate date at signal reception
     * @param frequency     frequency of the signal in Hz
     * @param parameters    ionospheric model parameters at state date
     * @return the path delay due to the ionosphere in m
     */
    double pathDelay(Vector3D localP1, Vector3D localP2,
                     TopocentricFrame baseFrame, AbsoluteDate receptionDate,
                     double frequency, double[] parameters);

    /**
     * Calculates the ionospheric path delay for the signal path from an observation
     * object to the satellite being measured.
     * <p>
     * This method is intended to be used for orbit determination issues.
     * In that respect, if the elevation is below 0° the path delay will be equal to zero.
     * </p><p>
     * For individual use of the ionospheric model (i.e. not for orbit determination), another
     * method signature can be implemented to compute the path delay for any elevation angle.
     * </p>
     * @param <T>            type of the elements
     * @param state          spacecraft state
     * @param coordsProvider coordinates provider for the observing object
     * @param frequency      frequency of the signal in Hz
     * @param parameters     ionospheric model parameters at state date
     * @return the path delay due to the ionosphere in m
     */
    default <T extends CalculusFieldElement<T>> T pathDelay(final FieldSpacecraftState<T> state,
                                                            final PVCoordinatesProvider coordsProvider,
                                                            final double frequency,
                                                            final T[] parameters) {

        // Solve for the lowest altitude point between p1 and p2
        final OneAxisEllipsoid     earth         = getEarth();
        final Frame                bodyFrame     = earth.getFrame();
        final FieldAbsoluteDate<T> receptionDate = state.getDate();
        final FieldVector3D<T>     p1            = state.getPVCoordinates(bodyFrame).getPosition();
        final Vector3D             p2            = coordsProvider.getPosition(receptionDate.toAbsoluteDate(), bodyFrame);
        final GeodeticPoint        lowAltPoint   = earth.lowestAltitudeIntermediate(p1.toVector3D(), p2);

        final TopocentricFrame        baseFrame  = new TopocentricFrame(earth, lowAltPoint, null);
        final FieldStaticTransform<T> base2Inert = baseFrame.getStaticTransformTo(bodyFrame, receptionDate);
        final FieldVector3D<T>        localP1    = base2Inert.getInverse().transformPosition(p1);
        final FieldVector3D<T>        localP2    = base2Inert.getInverse().transformPosition(p2);

        return pathDelay(localP1, localP2, baseFrame, receptionDate, frequency, parameters);
    }

    /**
     * Calculates the ionospheric path delay for the signal path from a ground
     * station to an observing object (ground station or satellite).
     * <p>
     * This method is intended to be used for orbit determination issues.
     * In that respect, if the elevation is below 0° the path delay will be equal to zero.
     * </p><p>
     * For individual use of the ionospheric model (i.e. not for orbit determination), another
     * method signature can be implemented to compute the path delay for any elevation angle.
     * </p>
     * @param <T>           type of the elements
     * @param localP1       position of path start point in baseFrame
     * @param localP2       position of path end point in baseFrame
     * @param baseFrame     topocentric frame of point with lowest altitude between p1 and p2
     * @param receptionDate date at signal reception
     * @param frequency     frequency of the signal in Hz
     * @param parameters    ionospheric model parameters at state date
     * @return the path delay due to the ionosphere in m
     */
    <T extends CalculusFieldElement<T>> T pathDelay(FieldVector3D<T> localP1, FieldVector3D<T> localP2,
                                                    TopocentricFrame baseFrame, FieldAbsoluteDate<T> receptionDate,
                                                    double frequency, T[] parameters);

}
