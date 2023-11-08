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

package org.orekit.estimation.iod;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.BeforeEach;
import org.orekit.Utils;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.estimation.measurements.AngularAzEl;
import org.orekit.estimation.measurements.AngularRaDec;
import org.orekit.estimation.measurements.EstimatedMeasurementBase;
import org.orekit.estimation.measurements.GroundStation;
import org.orekit.estimation.measurements.ObservableSatellite;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.TopocentricFrame;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.AbsolutePVCoordinates;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinatesProvider;
import org.orekit.utils.TimeStampedPVCoordinates;

public abstract class AbstractIodTest {
    /**
     * gcrf frame.
     */
    protected Frame gcrf;

    /**
     * EME2OOO frame.
     */
    protected Frame eme2000;

    /**
     * ground station for the observations.
     */
    protected GroundStation observer;

    /**
     * gravitational constant.
     */
    protected double mu;

    /**
     * itrf frame.
     */
    protected Frame itrf;

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("regular-data");

        this.mu      = Constants.WGS84_EARTH_MU;
        this.gcrf    = FramesFactory.getGCRF();
        this.eme2000 = FramesFactory.getEME2000();

        this.itrf = FramesFactory.getITRF(IERSConventions.IERS_2010, false);
        // The ground station is set to Austin, Texas, U.S.A
        final OneAxisEllipsoid body = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                           Constants.WGS84_EARTH_FLATTENING, itrf);
        this.observer = new GroundStation(
                new TopocentricFrame(body, new GeodeticPoint(FastMath.toRadians(40), FastMath.toRadians(-110),
                                                             2000.0), ""));
        this.observer.getPrimeMeridianOffsetDriver().setReferenceDate(AbsoluteDate.J2000_EPOCH);
        this.observer.getPolarOffsetXDriver().setReferenceDate(AbsoluteDate.J2000_EPOCH);
        this.observer.getPolarOffsetYDriver().setReferenceDate(AbsoluteDate.J2000_EPOCH);

    }

    // Computation of LOS angles
    protected Vector3D getLOSAngles(final Propagator prop, final AbsoluteDate date) {
        final AngularRaDec raDec = new AngularRaDec(observer, gcrf, date, new double[] { 0.0, 0.0 },
                                                    new double[] { 1.0, 1.0 },
                                                    new double[] { 1.0, 1.0 }, new ObservableSatellite(0));
        return (getEstimatedLineOfSight(raDec, prop, date, gcrf));
    }

    protected AngularAzEl getAzEl(final Propagator prop, final AbsoluteDate date) {
        ObservableSatellite satellite = new ObservableSatellite(0);
        final AngularAzEl azEl = new AngularAzEl(observer, date, new double[] { 0.0, 0.0 },
                                                 new double[] { 1.0, 1.0 }, new double[] { 1.0, 1.0 },
                                                 satellite);
        EstimatedMeasurementBase<AngularAzEl> estimated = azEl.estimateWithoutDerivatives(0, 0, new SpacecraftState[] {prop.propagate(date)});
        return new AngularAzEl(observer, date, estimated.getEstimatedValue(), azEl.getBaseWeight(),
                               azEl.getTheoreticalStandardDeviation(), satellite);
    }

    protected double getRelativeRangeError(final Orbit estimatedGauss, final Orbit orbitRef) {

        return FastMath.abs(estimatedGauss.getPVCoordinates().getPosition().getNorm() -
                                    orbitRef.getPVCoordinates().getPosition().getNorm()) /
                FastMath.abs(orbitRef.getPVCoordinates().getPosition().getNorm());
    }

    // Computation of the relative error in velocity
    protected double getRelativeVelocityError(final Orbit estimatedGauss, final Orbit orbitRef) {

        return FastMath.abs(estimatedGauss.getPVCoordinates().getVelocity().getNorm() -
                                    orbitRef.getPVCoordinates().getVelocity().getNorm()) /
                FastMath.abs(orbitRef.getPVCoordinates().getVelocity().getNorm());
    }
    
    /** Calculate the estimated Line Of Sight of a RADEC measurement at a given date.
     *
     * @param pvProvider provider for satellite coordinates
     * @param date the date for which the line of sight must be computed
     * @param outputFrame output frame for the line of sight
     * @return the estimate line of Sight of the measurement at the given date.
     */
    private Vector3D getEstimatedLineOfSight(final AngularRaDec raDec, final PVCoordinatesProvider pvProvider, final AbsoluteDate date, final Frame outputFrame) {
        final TimeStampedPVCoordinates satPV       = pvProvider.getPVCoordinates(date, outputFrame);
        final AbsolutePVCoordinates    satPVInGCRF = new AbsolutePVCoordinates(outputFrame, satPV);
        final SpacecraftState[]        satState    = new SpacecraftState[] { new SpacecraftState(satPVInGCRF) };
        final double[]                 angular     = raDec.estimateWithoutDerivatives(0, 0, satState).getEstimatedValue();

        // Rotate LOS from RADEC reference frame to output frame
        return raDec.getReferenceFrame().getStaticTransformTo(outputFrame, date)
                        .transformVector(new Vector3D(angular[0], angular[1]));
    }
}