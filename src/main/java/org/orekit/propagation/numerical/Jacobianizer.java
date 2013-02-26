/* Copyright 2010-2011 Centre National d'Études Spatiales
 * Licensed to CS Systèmes d'Information (CS) under one or more
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
package org.orekit.propagation.numerical;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.math3.analysis.differentiation.DerivativeStructure;
import org.apache.commons.math3.geometry.euclidean.threed.FieldRotation;
import org.apache.commons.math3.geometry.euclidean.threed.FieldVector3D;
import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.util.FastMath;
import org.apache.commons.math3.util.Precision;
import org.orekit.attitudes.Attitude;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.forces.ForceModel;
import org.orekit.frames.Frame;
import org.orekit.frames.Transform;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.PVCoordinates;

/** Class helping implementation of partial derivatives in {@link ForceModel force models} implementations.
 * <p>
 * For better performances, {@link ForceModel force models} implementations should compute their
 * partial derivatives analytically. However, in some cases, it may be difficult. This class
 * allows to compute the derivatives by finite differences relying only on the basic acceleration.
 * </p>
 * @author V&eacute;ronique Pommier-Maurussane
 * @author Luc Maisonobe
 */
public class Jacobianizer {

    /** Wrapped force model instance. */
    private final ForceModel forceModel;

    /** Central attraction coefficient (m<sup>3</sup>/s<sup>2</sup>). */
    private final double mu;

    /** Step used for finite difference computation with respect to spacecraft position. */
    private double hPos;

    /** Step used for finite difference computation with respect to parameters value. */
    private final Map<String, Double> hParam;

    /** Simple constructor.
     * @param forceModel force model instance to wrap
     * @param mu central attraction coefficient (m<sup>3</sup>/s<sup>2</sup>)
     * @param paramsAndSteps collection of parameters and their associated steps
     * @param hPos step used for finite difference computation with respect to spacecraft position (m)
     */
    public Jacobianizer(final ForceModel forceModel, final double mu,
                        final Collection<ParameterConfiguration> paramsAndSteps, final double hPos) {

        this.forceModel = forceModel;
        this.mu         = mu;
        this.hParam     = new HashMap<String, Double>();
        this.hPos       = hPos;

        // set up parameters for jacobian computation
        for (final ParameterConfiguration param : paramsAndSteps) {
            final String name = param.getParameterName();
            if (forceModel.isSupported(name)) {
                double step = param.getHP();
                if (Double.isNaN(step)) {
                    step = FastMath.max(1.0, FastMath.abs(forceModel.getParameter(name))) *
                           FastMath.sqrt(Precision.EPSILON);
                }
                hParam.put(name, step);
            }
        }

    }

    /** Compute acceleration.
     * @param retriever acceleration retriever to use for storing acceleration
     * @param date current date
     * @param frame inertial reference frame for state (both orbit and attitude)
     * @param position position of spacecraft in reference frame
     * @param velocity velocity of spacecraft in reference frame
     * @param rotation orientation (attitude) of the spacecraft with respect to reference frame
     * @param mass spacecraft mass
     * @exception OrekitException if the underlying force models cannot compute the acceleration
     */
    private void computeShiftedAcceleration(final AccelerationRetriever retriever,
                                            final AbsoluteDate date, final Frame frame,
                                            final Vector3D position, final Vector3D velocity,
                                            final Rotation rotation, final double mass)
        throws OrekitException {
        final Orbit shiftedORbit = new CartesianOrbit(new PVCoordinates(position, velocity), frame, date, mu);
        retriever.setOrbit(shiftedORbit);
        forceModel.addContribution(new SpacecraftState(shiftedORbit,
                                                       new Attitude(date, frame, rotation, Vector3D.ZERO),
                                                       mass),
                                   retriever);
    }

    /** Compute acceleration and derivatives with respect to state.
     * @param date current date
     * @param frame inertial reference frame for state (both orbit and attitude)
     * @param position position of spacecraft in reference frame
     * @param velocity velocity of spacecraft in reference frame
     * @param rotation orientation (attitude) of the spacecraft with respect to reference frame
     * @param mass spacecraft mass
     * @return acceleration with derivatives
     * @exception OrekitException if the underlying force models cannot compute the acceleration
     */
    public FieldVector3D<DerivativeStructure> accelerationDerivatives(final AbsoluteDate date, final Frame frame,
                                                                      final FieldVector3D<DerivativeStructure> position,
                                                                      final FieldVector3D<DerivativeStructure> velocity,
                                                                      final FieldRotation<DerivativeStructure> rotation,
                                                                      final DerivativeStructure mass)
        throws OrekitException {

        final int parameters = mass.getFreeParameters();
        final int order      = mass.getOrder();

        // estimate the scalar velocity step, assuming energy conservation
        // and hence differentiating equation V = sqrt(mu (2/r - 1/a))
        final Vector3D      p0    = position.toVector3D();
        final Vector3D      v0    = velocity.toVector3D();
        final double        r2    = p0.getNormSq();
        final double        hVel  = mu * hPos / (v0.getNorm() * r2);

        // estimate mass step, applying the same relative value as position
        final double m0 = mass.getValue();
        final double hMass = m0 * hPos / FastMath.sqrt(r2);

        // TODO: we should compute attitude partial derivatives with respect to position/velocity
        final Rotation r0 = rotation.toRotation();

        // compute nominal acceleration
        final AccelerationRetriever nominal = new AccelerationRetriever();
        computeShiftedAcceleration(nominal, date, frame, p0, v0, r0, m0);
        final double[] a0 = nominal.getAcceleration().toArray();

        // compute accelerations with shifted states
        final AccelerationRetriever shifted = new AccelerationRetriever();

        // shift position by hPos alon x, y and z
        computeShiftedAcceleration(shifted, date, frame, new Vector3D(p0.getX() + hPos, p0.getY(), p0.getZ()), v0, r0, m0);
        final double[] derPx = new Vector3D(1 / hPos, shifted.getAcceleration(), -1 / hPos, nominal.getAcceleration()).toArray();
        computeShiftedAcceleration(shifted, date, frame, new Vector3D(p0.getX(), p0.getY() + hPos, p0.getZ()), v0, r0, m0);
        final double[] derPy = new Vector3D(1 / hPos, shifted.getAcceleration(), -1 / hPos, nominal.getAcceleration()).toArray();
        computeShiftedAcceleration(shifted, date, frame, new Vector3D(p0.getX(), p0.getY(), p0.getZ() + hPos), v0, r0, m0);
        final double[] derPz = new Vector3D(1 / hPos, shifted.getAcceleration(), -1 / hPos, nominal.getAcceleration()).toArray();

        // shift velocity by hVel alon x, y and z
        computeShiftedAcceleration(shifted, date, frame, p0, new Vector3D(v0.getX() + hVel, v0.getY(), v0.getZ()), r0, m0);
        final double[] derVx = new Vector3D(1 / hVel, shifted.getAcceleration(), -1 / hVel, nominal.getAcceleration()).toArray();
        computeShiftedAcceleration(shifted, date, frame, p0, new Vector3D(v0.getX(), v0.getY() + hVel, v0.getZ()), r0, m0);
        final double[] derVy = new Vector3D(1 / hVel, shifted.getAcceleration(), -1 / hVel, nominal.getAcceleration()).toArray();
        computeShiftedAcceleration(shifted, date, frame, p0, new Vector3D(v0.getX(), v0.getY(), v0.getZ() + hVel), r0, m0);
        final double[] derVz = new Vector3D(1 / hVel, shifted.getAcceleration(), -1 / hVel, nominal.getAcceleration()).toArray();

        final double[] derM;
        if (parameters < 7) {
            derM = null;
        } else {
            // shift mass by hMass
            computeShiftedAcceleration(shifted, date, frame, p0, v0, r0, m0 + hMass);
            derM = new Vector3D(1 / hMass, shifted.getAcceleration(), -1 / hMass, nominal.getAcceleration()).toArray();

        }
        final double[] derivatives = new double[1 + parameters];
        final DerivativeStructure[] accDer = new DerivativeStructure[3];
        for (int i = 0; i < 3; ++i) {

            // first element is value of acceleration
            derivatives[0] = a0[i];

            // next three elements are derivatives with respect to position
            derivatives[1] = derPx[i];
            derivatives[2] = derPy[i];
            derivatives[3] = derPz[i];

            // next three elements are derivatives with respect to velocity
            derivatives[4] = derVx[i];
            derivatives[5] = derVy[i];
            derivatives[6] = derVz[i];

            if (derM != null) {
                derivatives[7] = derM[i];
            }

            accDer[i] = new DerivativeStructure(parameters, order, derivatives);

        }

        return new FieldVector3D<DerivativeStructure>(accDer);


    }

    /** Compute acceleration and derivatives with respect to parameter.
     * @param s current state
     * @param paramName parameter with respect to which derivation is desired
     * @return acceleration with derivatives
     * @exception OrekitException if the underlying force models cannot compute the acceleration
     */
    public FieldVector3D<DerivativeStructure> accelerationDerivatives(final SpacecraftState s, final String paramName) throws OrekitException {

        if (!hParam.containsKey(paramName)) {
            throw new OrekitException(OrekitMessages.UNKNOWN_PARAMETER, paramName);
        }
        final double hP = hParam.get(paramName);

        final AccelerationRetriever nominal = new AccelerationRetriever();
        nominal.setOrbit(s.getOrbit());
        forceModel.addContribution(s, nominal);
        final double nx = nominal.getAcceleration().getX();
        final double ny = nominal.getAcceleration().getY();
        final double nz = nominal.getAcceleration().getZ();

        final double paramValue = forceModel.getParameter(paramName);
        forceModel.setParameter(paramName,  paramValue + hP);
        final AccelerationRetriever shifted = new AccelerationRetriever();
        shifted.setOrbit(s.getOrbit());
        forceModel.addContribution(s, shifted);
        final double sx = shifted.getAcceleration().getX();
        final double sy = shifted.getAcceleration().getY();
        final double sz = shifted.getAcceleration().getZ();

        forceModel.setParameter(paramName,  paramValue);

        return new FieldVector3D<DerivativeStructure>(new DerivativeStructure(1, 1, nx, (sx - nx) / hP),
                              new DerivativeStructure(1, 1, ny, (sy - ny) / hP),
                              new DerivativeStructure(1, 1, nz, (sz - nz) / hP));

    }

    /** Get parameter value from its name.
     * @param name parameter name
     * @return parameter value
     * @exception IllegalArgumentException if parameter is not supported
     */
    public double getParameter(final String name) throws IllegalArgumentException {
        return forceModel.getParameter(name);
    }

    /** Get the names of the supported parameters.
     * @return parameters names
     * @see #isSupported(String)
     */
    public Collection<String> getParametersNames() {
        return forceModel.getParametersNames();
    }

    /** Check if a parameter is supported.
     * <p>Supported parameters are those listed by {@link #getParametersNames()}.</p>
     * @param name parameter name to check
     * @return true if the parameter is supported
     * @see #getParametersNames()
     */
    public boolean isSupported(final String name) {
        return forceModel.isSupported(name);
    }

    /** Set the value for a given parameter.
     * @param name parameter name
     * @param value parameter value
     * @exception IllegalArgumentException if parameter is not supported
     */
    public void setParameter(final String name, final double value) throws IllegalArgumentException {
        forceModel.setParameter(name, value);
    }

    /** Internal class for retrieving accelerations. */
    private static class AccelerationRetriever implements TimeDerivativesEquations {

        /** Stored acceleration. */
        private Vector3D acceleration;

        /** Current orbit. */
        private Orbit orbit;

        /** Simple constructor.
         */
        protected AccelerationRetriever() {
            acceleration = Vector3D.ZERO;
            this.orbit   = null;
        }

        /** Get acceleration.
         * @return acceleration
         */
        public Vector3D getAcceleration() {
            return acceleration;
        }

        /** Set the current orbit.
         * @param orbit current orbit
         */
        public void setOrbit(final Orbit orbit) {
            acceleration = Vector3D.ZERO;
            this.orbit   = orbit;
        }

        /** {@inheritDoc} */
        public void addKeplerContribution(final double mu) {
            final Vector3D position = orbit.getPVCoordinates().getPosition();
            final double r2         = position.getNormSq();
            acceleration = acceleration.subtract(mu / (r2 * FastMath.sqrt(r2)), position);
        }

        /** {@inheritDoc} */
        public void addXYZAcceleration(final double x, final double y, final double z) {
            acceleration = acceleration.add(new Vector3D(x, y, z));
        }

        /** {@inheritDoc} */
        public void addAcceleration(final Vector3D gamma, final Frame frame)
            throws OrekitException {
            final Transform t = frame.getTransformTo(orbit.getFrame(), orbit.getDate());
            final Vector3D gammInRefFrame = t.transformVector(gamma);
            addXYZAcceleration(gammInRefFrame.getX(), gammInRefFrame.getY(), gammInRefFrame.getZ());
        }

        /** {@inheritDoc} */
        public void addMassDerivative(final double q) {
            // TODO
            // we don't compute (yet) the mass part of the Jacobian, we just ignore this
        }

    }

}
