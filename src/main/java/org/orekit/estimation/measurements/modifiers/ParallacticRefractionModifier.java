/* Copyright 2022-2026 Romain Serra
 * Licensed to CS GROUP (CS) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * Mark Rutten licenses this file to You under the Apache License, Version 2.0
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
package org.orekit.estimation.measurements.modifiers;

import java.util.Collections;
import java.util.List;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathUtils;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.estimation.measurements.AngularRaDec;
import org.orekit.estimation.measurements.EstimatedMeasurementBase;
import org.orekit.estimation.measurements.EstimationModifier;
import org.orekit.estimation.measurements.GroundObserver;
import org.orekit.estimation.measurements.Observer;
import org.orekit.frames.Frame;
import org.orekit.frames.StaticTransform;
import org.orekit.frames.TopocentricFrame;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.ParameterDriver;


/**
 * Class modifying theoretical angular measurement with so-called parallactic refraction. This only applies to astrometric
 * data obtained from ground-based observations.
 * <p>
 * This class implements equation (14) in Kaplan, Atmospheric Refraction of Light from Nearby Objects in Space,
 * itself coming from: Nugent, L. J., & Condon, R. J., 1966, “Velocity Aberration and Atmospheric Refraction in Satellite
 * Laser Communication Experiments,” Applied Optics, Vol. 5, pp. 1832–1837.
 * </p>
 *
 * @author Romain Serra
 * @since 14.0
 */
public class ParallacticRefractionModifier implements EstimationModifier<AngularRaDec> {

    /** Default altitude of the troposphere in meters (m). */
    private static final double DEFAULT_TROPOSPHERE_ALTITUDE = 8e3;

    /** Default refraction index. */
    private static final double DEFAULT_REFRACTION_INDEX = 1.000292;

    /** Altitude of the troposphere in meters (m). */
    private final double troposphereAltitude;

    /** Refraction index. */
    private final double refractionIndex;

    /** Simple constructor. */
    public ParallacticRefractionModifier() {
        this(DEFAULT_TROPOSPHERE_ALTITUDE, DEFAULT_REFRACTION_INDEX);
    }

    /** Constructor.
     * @param troposphereAltitude altitude of the troposphere in meters
     * @param refractionIndex refraction index
     */
    public ParallacticRefractionModifier(final double troposphereAltitude, final double refractionIndex) {
        this.troposphereAltitude = troposphereAltitude;
        this.refractionIndex = refractionIndex;
    }

    /**
     * Getter for the troposphere altitude.
     * @return troposphere altitude
     */
    public double getTroposphereAltitude() {
        return troposphereAltitude;
    }

    /**
     * Getter for the refraction index.
     * @return index
     */
    public double getRefractionIndex() {
        return refractionIndex;
    }

    /** {@inheritDoc} */
    @Override
    public String getEffectName() {
        return "parallactic refraction";
    }

    /** {@inheritDoc} */
    @Override
    public List<ParameterDriver> getParametersDrivers() {
        return Collections.emptyList();
    }

    /** {@inheritDoc} */
    @Override
    public void modifyWithoutDerivatives(final EstimatedMeasurementBase<AngularRaDec> estimated) {

        // Observation object
        final Observer observer = estimated.getObservedMeasurement().getObserver();
        if (observer instanceof GroundObserver groundObserver) {
            // Observation date
            final AbsoluteDate date = estimated.getDate();

            // Satellite height
            final SpacecraftState state = estimated.getStates()[0];
            final double altitude = groundObserver.getParentShape().transform(state.getPosition(), state.getFrame(), date).getAltitude();

            // Compute Azimuth/Elevation
            final double[] estimatedRaDec = estimated.getEstimatedValue();
            final Frame frame = estimated.getObservedMeasurement().getReferenceFrame();
            final GeodeticPoint gp = groundObserver.getOffsetGeodeticPoint(date);
            final TopocentricFrame topocentricFrame = new TopocentricFrame(groundObserver.getParentShape(), gp, "station");
            final StaticTransform transform = frame.getTransformTo(topocentricFrame, date);
            final Vector3D los = transform.transformVector(new Vector3D(estimatedRaDec[0], estimatedRaDec[1]));
            final double elevation = los.getDelta();

            if (elevation > 0.) {
                // Apply correction on elevation
                final double ratio = altitude / troposphereAltitude;
                final double parallacticCorrection = (refractionIndex - 1.) * FastMath.tan(MathUtils.SEMI_PI - elevation) * (FastMath.exp(-ratio) - 1.) / ratio;
                final double modifiedElevation = elevation + parallacticCorrection;

                // Convert back to RA/Dec
                final Vector3D convertedBack = transform.getInverse().transformVector(new Vector3D(los.getAlpha(), modifiedElevation));
                final double   baseRightAscension = convertedBack.getAlpha();
                final double observedRightAscension = estimated.getObservedMeasurement().getObservedValue()[0];
                final double   twoPiWrap          = MathUtils.normalizeAngle(baseRightAscension, observedRightAscension) - baseRightAscension;
                final double   rightAscension     = baseRightAscension + twoPiWrap;

                // Update estimated values
                estimated.modifyEstimatedValue(this, rightAscension, convertedBack.getDelta());
            }
        } else {
            throw new OrekitException(OrekitMessages.WRONG_OBSERVER_TYPE);
        }

    }

}
