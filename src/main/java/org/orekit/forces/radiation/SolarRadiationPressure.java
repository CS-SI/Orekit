/* Copyright 2002-2010 CS Communication & Systèmes
 * Licensed to CS Communication & Systèmes (CS) under one or more
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
package org.orekit.forces.radiation;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.math.geometry.Vector3D;
import org.orekit.errors.OrekitException;
import org.orekit.forces.ForceModelWithJacobians;
import org.orekit.frames.Frame;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.AbstractDetector;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.numerical.TimeDerivativesEquations;
import org.orekit.propagation.numerical.TimeDerivativesEquationsWithJacobians;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.PVCoordinatesProvider;

/** Solar radiation pressure force model.
 *
 * @author Fabien Maussion
 * @author &Eacute;douard Delente
 * @author V&eacute;ronique Pommier-Maurussane
 * @author Pascal Parraud
 * @version $Revision:1665 $ $Date:2008-06-11 12:12:59 +0200 (mer., 11 juin 2008) $
 */
public class SolarRadiationPressure implements ForceModelWithJacobians {

    /** Parameter name for absorption coefficient enabling jacobian processing. */
    public static final String ABSORPTION_COEFFICIENT = "ABSORPTION COEFFICIENT";

    /** Parameter name for specular reflection coefficient enabling jacobian processing. */
    public static final String REFLECTION_COEFFICIENT = "REFLECTION COEFFICIENT";

    /** Serializable UID. */
    private static final long serialVersionUID = 8874297900604482921L;

    /** Error message for too low trajectory. */
    private static final String LOW_TRAJECTORY_MESSAGE =
        "trajectory inside the Brillouin sphere (r = {0})";

    /** Error message for unknown parameter. */
    private static final String UNKNOWN_PARAMETER_MESSAGE = "unknown parameter {0}";

    /** Sun radius (m). */
    private static final double SUN_RADIUS = 6.95e8;

    /** Reference flux normalized for a 1m distance (N). */
    private final double kRef;

    /** Sun model. */
    private final PVCoordinatesProvider sun;

    /** Earth model. */
    private final double equatorialRadius;

    /** Spacecraft. */
    private final RadiationSensitive spacecraft;

    /** List of the parameters names. */
    private final ArrayList<String> parametersNames = new ArrayList<String>();

    /** Simple constructor with default reference values.
     * <p>When this constructor is used, the reference values are:</p>
     * <ul>
     *   <li>d<sub>ref</sub> = 149597870000.0 m</li>
     *   <li>p<sub>ref</sub> = 4.56 10<sup>-6</sup> N/m<sup>2</sup></li>
     * </ul>
     * @param sun Sun model
     * @param equatorialRadius spherical shape model (for umbra/penumbra computation)
     * @param spacecraft the object physical and geometrical information
     */
    public SolarRadiationPressure(final PVCoordinatesProvider sun, final double equatorialRadius,
                                  final RadiationSensitive spacecraft) {
        this(149597870000.0, 4.56e-6, sun, equatorialRadius, spacecraft);
    }

    /** Complete constructor.
     * <p>Note that reference solar radiation pressure <code>pRef</code> in
     * N/m<sup>2</sup> is linked to solar flux SF in W/m<sup>2</sup> using
     * formula pRef = SF/c where c is the speed of light (299792458 m/s). So
     * at 1UA a 1367 W/m<sup>2</sup> solar flux is a 4.56 10<sup>-6</sup>
     * N/m<sup>2</sup> solar radiation pressure.</p>
     * @param dRef reference distance for the solar radiation pressure (m)
     * @param pRef reference solar radiation pressure at dRef (N/m<sup>2</sup>)
     * @param sun Sun model
     * @param equatorialRadius spherical shape model (for umbra/penumbra computation)
     * @param spacecraft the object physical and geometrical information
     */
    public SolarRadiationPressure(final double dRef, final double pRef,
                                  final PVCoordinatesProvider sun,
                                  final double equatorialRadius,
                                  final RadiationSensitive spacecraft) {
        this.kRef  = pRef * dRef * dRef;
        this.sun   = sun;
        this.equatorialRadius = equatorialRadius;
        this.spacecraft = spacecraft;
        this.parametersNames.add(ABSORPTION_COEFFICIENT);
        this.parametersNames.add(REFLECTION_COEFFICIENT);
    }

    /** {@inheritDoc} */
    public void addContribution(final SpacecraftState s, final TimeDerivativesEquations adder)
        throws OrekitException {

        final AbsoluteDate date     = s.getDate();
        final Frame        frame    = s.getFrame();
        final Vector3D     position = s.getPVCoordinates().getPosition();

        // raw radiation pressure
        final Vector3D satSunVector = getSatSunVector(s);
        final double r2             = satSunVector.getNormSq();
        final double rawP           = kRef * getLightningRatio(position, frame, date) / r2;
        final Vector3D flux         = new Vector3D(-rawP / Math.sqrt(r2), satSunVector);

        // provide the perturbing acceleration to the derivatives adder
        adder.addAcceleration(spacecraft.radiationPressureAcceleration(s, flux), frame);

    }

    /** Get the lightning ratio ([0-1]).
     * @param position the satellite's position in the selected frame.
     * @param frame in which is defined the position
     * @param date the date
     * @return lightning ratio
     * @exception OrekitException if the trajectory is inside the Earth
     */
    public double getLightningRatio(final Vector3D position, final Frame frame,
                                    final AbsoluteDate date)
        throws OrekitException {

        final Vector3D satSunVector =
            sun.getPVCoordinates(date, frame).getPosition().subtract(position);

        // Earth apparent radius
        final double r = position.getNorm();
        if (r <= equatorialRadius) {
            throw new OrekitException(LOW_TRAJECTORY_MESSAGE, r);
        }

        final double alphaEarth = Math.asin(equatorialRadius / r);

        // Definition of the Sun's apparent radius
        final double alphaSun = Math.asin(SUN_RADIUS / satSunVector.getNorm());

        // Retrieve the Sat-Sun / Sat-Central body angle
        final double sunEarthAngle = Vector3D.angle(satSunVector, position.negate());

        double result = 1.0;

        // Is the satellite in complete umbra ?
        if (sunEarthAngle - alphaEarth + alphaSun < 0.0) {
            result = 0.0;
        }
        // Compute a lightning ratio in penumbra
        if ((sunEarthAngle - alphaEarth + alphaSun >= 0.0) &&
            (sunEarthAngle - alphaEarth - alphaSun <= 0.0)) {

            //result = (alphaSun + sunEarthAngle - alphaEarth) / (2*alphaSun);

            final double alpha1 =
                (sunEarthAngle * sunEarthAngle -
                        (alphaEarth - alphaSun) * (alphaSun + alphaEarth)) / (2 * sunEarthAngle);

            final double alpha2 =
                (sunEarthAngle * sunEarthAngle +
                        (alphaEarth - alphaSun) * (alphaSun + alphaEarth)) / (2 * sunEarthAngle);

            final double P1 = Math.PI * alphaSun * alphaSun -
                alphaSun * alphaSun * Math.acos(alpha1 / alphaSun) +
                alpha1 * Math.sqrt(alphaSun * alphaSun - alpha1 * alpha1);

            final double P2 = alphaEarth * alphaEarth * Math.acos(alpha2 / alphaEarth) -
                alpha2 * Math.sqrt(alphaEarth * alphaEarth - alpha2 * alpha2);

            result = (P1 - P2) / (Math.PI * alphaSun * alphaSun);


        }

        return result;

    }

    /** Compute sat-Sun vector in spacecraft state frame.
     * @param state current spacecraft state
     * @return sat-Sun vector in spacecraft state frame
     * @exception OrekitException if sun position cannot be computed
     */
    private Vector3D getSatSunVector(final SpacecraftState state)
        throws OrekitException {
        final PVCoordinates sunPV = sun.getPVCoordinates(state.getDate(), state.getFrame());
        final PVCoordinates satPV = state.getPVCoordinates();
        return sunPV.getPosition().subtract(satPV.getPosition());
    }

    /** {@inheritDoc} */
    public EventDetector[] getEventsDetectors() {
        return new EventDetector[] {
            new UmbraDetector(), new PenumbraDetector()
        };
    }

    /** {@inheritDoc} */
    public void addContributionWithJacobians(final SpacecraftState s,
                                             final TimeDerivativesEquationsWithJacobians adder)
        throws OrekitException {
    }

    /** {@inheritDoc} */
    public Collection<String> getParametersNames() {
        return parametersNames;
    }

    /** {@inheritDoc} */
    public double getParameter(final String name)
        throws IllegalArgumentException {
        if (name.matches(ABSORPTION_COEFFICIENT)) {
            return spacecraft.getAbsorptionCoefficient();
        } else if (name.matches(REFLECTION_COEFFICIENT)) {
            return spacecraft.getReflectionCoefficient();
        } else {
            throw OrekitException.createIllegalArgumentException(UNKNOWN_PARAMETER_MESSAGE, name);
        }
    }

    /** {@inheritDoc} */
    public void setParameter(final String name, final double value)
        throws IllegalArgumentException {
        if (name.matches(ABSORPTION_COEFFICIENT)) {
            spacecraft.setAbsorptionCoefficient(value);
        } else if (name.matches(REFLECTION_COEFFICIENT)) {
            spacecraft.setReflectionCoefficient(value);
        } else {
            throw OrekitException.createIllegalArgumentException(UNKNOWN_PARAMETER_MESSAGE, name);
        }
    }

    /** This class defines the umbra entry/exit detector. */
    private class UmbraDetector extends AbstractDetector {

        /** Serializable UID. */
        private static final long serialVersionUID = -165934451905681928L;

        /** Build a new instance. */
        public UmbraDetector() {
            super(60.0, 1.0e-3);
        }

        /** {@inheritDoc} */
        public int eventOccurred(final SpacecraftState s, final boolean increasing) {
            return RESET_DERIVATIVES;
        }

        /** The G-function is the difference between the Sat-Sun-Sat-Earth angle and
         * the Earth's apparent radius.
         * @param s the current state information : date, kinematics, attitude
         * @return value of the g function
         * @exception OrekitException if sun or spacecraft position cannot be computed
         */
        public double g(final SpacecraftState s)
            throws OrekitException {

            final Vector3D satPos = s.getPVCoordinates().getPosition();
            final double sunEarthAngle = Math.PI - Vector3D.angle(getSatSunVector(s), satPos);
            final double r = satPos.getNorm();
            if (r <= equatorialRadius) {
                throw new OrekitException(LOW_TRAJECTORY_MESSAGE, r);
            }
            final double alphaEarth = Math.asin(equatorialRadius / r);
            return sunEarthAngle - alphaEarth;
        }

    }

    /** This class defines the penumbra entry/exit detector. */
    private class PenumbraDetector extends AbstractDetector {

        /** Serializable UID. */
        private static final long serialVersionUID = -6128481192702533563L;

        /** Build a new instance. */
        public PenumbraDetector() {
            super(60.0, 1.0e-3);
        }

        /** {@inheritDoc} */
        public int eventOccurred(final SpacecraftState s, final boolean increasing) {
            return RESET_DERIVATIVES;
        }

        /** The G-function is the difference between the Sat-Sun-Sat-Earth angle and
         * the sum of the Earth's and Sun's apparent radius.
         * @param s the current state information : date, kinematics, attitude
         * @return value of the g function
         * @exception OrekitException if sun or spacecraft position cannot be computed
         */
        public double g(final SpacecraftState s)
            throws OrekitException {

            final Vector3D satPos       = s.getPVCoordinates().getPosition();
            final Vector3D satSunVector = getSatSunVector(s);
            final double sunEarthAngle  = Math.PI - Vector3D.angle(satSunVector, satPos);
            final double r = satPos.getNorm();
            if (r <= equatorialRadius) {
                throw new OrekitException(LOW_TRAJECTORY_MESSAGE, r);
            }
            final double alphaEarth = Math.asin(equatorialRadius / r);
            final double alphaSun   = Math.asin(SUN_RADIUS / satSunVector.getNorm());
            return sunEarthAngle - alphaEarth - alphaSun;
        }

    }

}
