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
package org.orekit.propagation.numerical;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.commons.math.ode.DerivativeException;
import org.apache.commons.math.ode.FirstOrderIntegrator;
import org.apache.commons.math.ode.IntegratorException;
import org.apache.commons.math.ode.jacobians.FirstOrderIntegratorWithJacobians;
import org.apache.commons.math.ode.jacobians.ODEWithJacobians;
import org.apache.commons.math.ode.jacobians.ParameterizedODE;
import org.apache.commons.math.ode.nonstiff.AdaptiveStepsizeIntegrator;
import org.apache.commons.math.util.MathUtils;
import org.orekit.attitudes.Attitude;
import org.orekit.errors.OrekitException;
import org.orekit.errors.PropagationException;
import org.orekit.forces.ForceModel;
import org.orekit.forces.ForceModelWithJacobians;
import org.orekit.forces.Parameterizable;
import org.orekit.frames.Frame;
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.EventDetector;
import org.orekit.time.AbsoluteDate;


/** This class propagates {@link org.orekit.orbits.Orbit orbits} using
 *  numerical integration and enables jacobians computation for orbit parameters
 *  and partial derivatives computation with respect to some force models parameters.
 * <p>
 * As of 5.0, this class is still considered experimental, so use it with care,
 * the API could change in the future.
 * </p>
 * <p>
 * The underlying numerical integrator configuration can be exactly the same
 * as these for a simple {@link NumericalPropagator numerical integration}.
 * </p>
 * <p>
 * The Jacobian for the six {@link EquinoctialOrbit equinoctial orbit parameters}
 * (a, e<sub>x</sub>, e<sub>y</sub>, h<sub>x</sub>, h<sub>y</sub>, l<sub>v</sub>)
 * and the mass is computed as a 7x7 array such as:
 *   <pre>
 *     dFdY[i][j] = dyi/dyj
 *     with: y0 = a, y1 = ex, y2 = ey, y3 = hx, y4 = hy, y5 = lv, y6 = mass
 *   </pre>
 * </p>
 * <p>
 * Partial derivatives can also be computed for the 7 elements state vector with
 * respect to n {@link #selectParameters selected parameters} from
 * {@link ForceModelWithJacobians force models}. They are computed as a 7xn array:
 *   <pre>
 *     dFdP[i][j] = dyi/dpj
 *   </pre>
 * </p>
 *
 * @see NumericalPropagator
 * @see ForceModelWithJacobians
 *
 * @author Pascal Parraud
 * @version $Revision$ $Date$
 */
public class NumericalPropagatorWithJacobians extends NumericalPropagator {

    /** Serializable UID. */
    private static final long serialVersionUID = 4139595812211569107L;

    /** Absolute vectorial error field name. */
    private static final String ABSOLUTE_TOLERANCE = "vecAbsoluteTolerance";

    /** Relative vectorial error field name. */
    private static final String RELATIVE_TOLERANCE = "vecRelativeTolerance";

    /** Force models used when extrapolating the Orbit. */
    private final List<ForceModelWithJacobians> forceModelsWJ;

    /** State vector derivative with respect to the parameter. */
    private double[][] DY0DP = null;

    /** Gauss equations handler. */
    private TimeDerivativesEquationsWithJacobians adder;

    /** Selected parameters for jacobian computation. */
    private String[] selectedParameters = new String[0];

    /** Selected parameters with force model associated for jacobian computation. */
    private List<ParameterPair> paramPairs = new ArrayList<ParameterPair>();

    /** Create a new instance of NumericalPropagatorWithJacobians.
     * After creation, the instance is empty, i.e. the attitude law is set to an
     * unspecified default law and there are no perturbing forces at all.
     * This means that if {@link #addForceModel addForceModel} is not
     * called after creation, the integrated orbit will follow a keplerian
     * evolution only.
     * @param integrator numerical integrator to use for propagation.
     */
    public NumericalPropagatorWithJacobians(final FirstOrderIntegrator integrator) {
        super(integrator);
        this.forceModelsWJ = new ArrayList<ForceModelWithJacobians>();
    }

    /** Add a force model to the global perturbation model.
     * @param model perturbing force model to add
     * @see #removeForceModels()
     * @see #removeForceModels()
     */
    public void addForceModel(final ForceModel model) {
        forceModels.add(model);
        if (!(model instanceof ForceModelWithJacobians)) {
            forceModelsWJ.add(new ForceModelWrapper(model));
        } else {
            forceModelsWJ.add((ForceModelWithJacobians) model);
        }
    }

    /** Remove all perturbing force models from the global perturbation model.
     * <p>Once all perturbing forces have been removed (and as long as no new force
     * model is added), the integrated orbit will follow a keplerian evolution
     * only.</p>
     * @see #addForceModel(ForceModel)
     */
    public void removeForceModels() {
        forceModels.clear();
        forceModelsWJ.clear();
    }

    /** Select the parameters to consider for jacobian processing.
     * <p>Parameters names have to be consistent with some
     * {@link ForceModelWithJacobians} added elsewhere.</p>
     * @param parameters parameters to consider for jacobian processing
     * @see #addForceModel(ForceModel)
     * @see ForceModelWithJacobians
     * @see Parameterizable
     */
    public void selectParameters(final String[] parameters) {
        selectedParameters = parameters.clone();
        DY0DP = new double[7][selectedParameters.length];
        for (final double[] row : DY0DP) {
            Arrays.fill(row, 0.0);
        }
    }

    /** Get the parameters selected for jacobian processing.
     * @return parameters considered for jacobian processing
     * @see #selectParameters(String[])
     */
    public String[] getParameterNames() {
        return selectedParameters.clone();
    }

    /** Propagate towards a target date and compute partial derivatives.
     * <p>Propagation is the same as the
     * {@link NumericalPropagator#propagate(AbsoluteDate) basic one}.</p>
     * <p>Jacobian for orbit parameters is given as a 7x7 array.</p>
     * <p>Partial derivatives will be computed as a 7xn array
     * when n parameters have been {@link #selectParameters(String[]) selected}
     * (n may be 0).</p>
     * <p>Those parameters are related to some {@link ForceModelWithJacobians force models}
     * which must have been added elsewhere.</p>
     * @param finalDate target date towards which orbit state should be propagated
     * @param dFdY equinoctial orbit parameters + mass jacobian (7x7 array)
     * @param dFdP partial derivatives with respect to selected parameters (7xn)
     * @return propagated state
     * @exception PropagationException if state cannot be propagated
     */
    public SpacecraftState propagate(final AbsoluteDate finalDate,
                                     final double[][] dFdY, final double[][] dFdP)
        throws PropagationException {
        try {

            if (initialState == null) {
                throw new PropagationException("initial state not specified for orbit propagation");
            }
            if (initialState.getMass() <= 0.0) {
                throw new IllegalArgumentException("Mass is null or negative");
            }
            if (initialState.getDate().equals(finalDate)) {
                // don't extrapolate
                return initialState;
            }
            if (integrator == null) {
                throw new PropagationException("ODE integrator not set for orbit propagation");
            }

            // space dynamics view
            startDate = initialState.getDate();
            if (modeHandler != null) {
                modeHandler.initialize(startDate, initialState.getFrame(), mu, attitudeLaw);
            }

            final EquinoctialOrbit initialOrbit =
                new EquinoctialOrbit(initialState.getOrbit());

            currentState =
                new SpacecraftState(initialOrbit, initialState.getAttitude(), initialState.getMass());

            adder = new TimeDerivativesEquationsWithJacobians(initialOrbit);

            integrator.clearEventHandlers();

            // set up events related to force models
            for (final ForceModelWithJacobians forceModel : forceModelsWJ) {
                final EventDetector[] modelDetectors = forceModel.getEventsDetectors();
                if (modelDetectors != null) {
                    for (final EventDetector detector : modelDetectors) {
                        setUpEventDetector(detector);
                    }
                }
            }

            // set up events added by user
            for (final EventDetector detector : detectors) {
                setUpEventDetector(detector);
            }

            // mathematical view
            final double t0 = 0;
            final double t1 = finalDate.durationFrom(startDate);

            // Map state to array
            stateVector[0] = initialOrbit.getA();
            stateVector[1] = initialOrbit.getEquinoctialEx();
            stateVector[2] = initialOrbit.getEquinoctialEy();
            stateVector[3] = initialOrbit.getHx();
            stateVector[4] = initialOrbit.getHy();
            stateVector[5] = initialOrbit.getLv();
            stateVector[6] = initialState.getMass();

            // set up parameters for jacobian computation
            int noParam = 0;
            final int      nbParam = selectedParameters.length;
            final double[] paramWJ = new double[nbParam];
            final double[] hP      = new double[nbParam];

            for (final String parameter : selectedParameters) {
                boolean found = false;
                for (final ForceModelWithJacobians fmwj : forceModelsWJ) {
                    for (String parFMWJ : fmwj.getParametersNames()) {
                        if (parFMWJ.matches(parameter)) {
                            found = true;
                            paramWJ[noParam] = fmwj.getParameter(parFMWJ);
                            hP[noParam] = paramWJ[noParam] * Math.sqrt(MathUtils.EPSILON);
                            paramPairs.add(new ParameterPair(parameter, fmwj));
                            noParam++;
                        }
                    }
                }
                if (!found) {
                    throw new PropagationException("unknown parameter {0}", parameter);
                }
            }

            // if selectParameters was not invoked and then no parameter selected
            if (DY0DP == null) {
                DY0DP = new double[7][nbParam];
                for (final double[] row : DY0DP) {
                    Arrays.fill(row, 0.0);
                }
            }

            // get hY from integrator tolerance array
            final double[] hY = getHy(integrator);

            // resize integrator tolerance array
            expandToleranceArray(integrator);

            final FirstOrderIntegratorWithJacobians integratorWJ =
                    new FirstOrderIntegratorWithJacobians(integrator,
                                                          new DifferentialEquations(),
                                                          paramWJ, hY, hP);

            try {
                // mathematical integration
                final double stopTime = integratorWJ.integrate(t0, stateVector, DY0DP,
                                                               t1, stateVector, dFdY, DY0DP);
                // fill in jacobian
                for (int i = 0; i < DY0DP.length; i++) {
                    System.arraycopy(DY0DP[i], 0, dFdP[i], 0, DY0DP[i].length);
                }

                // back to space dynamics view
                final AbsoluteDate date = startDate.shiftedBy(stopTime);

                final EquinoctialOrbit orbit =
                    new EquinoctialOrbit(stateVector[0], stateVector[1], stateVector[2], stateVector[3],
                                         stateVector[4], stateVector[5], EquinoctialOrbit.TRUE_LATITUDE_ARGUMENT,
                                         initialOrbit.getFrame(), date, mu);

                resetInitialState(new SpacecraftState(orbit, attitudeLaw.getAttitude(orbit), stateVector[6]));
            } finally {
                if (integrator != null) {
                    resetToleranceArray(integrator);
                }
            }

            return initialState;

        } catch (OrekitException oe) {
            throw new PropagationException(oe.getMessage(), oe);
        } catch (DerivativeException de) {

            // recover a possible embedded PropagationException
            for (Throwable t = de; t != null; t = t.getCause()) {
                if (t instanceof PropagationException) {
                    throw (PropagationException) t;
                }
            }

            throw new PropagationException(de.getMessage(), de);

        } catch (IntegratorException ie) {

            // recover a possible embedded PropagationException
            for (Throwable t = ie; t != null; t = t.getCause()) {
                if (t instanceof PropagationException) {
                    throw (PropagationException) t;
                }
            }

            throw new PropagationException(ie.getMessage(), ie);

        }
    }

    /** Expand integrator tolerance array to fit compound state vector.
     * @param integrator integrator
     */
    private void expandToleranceArray(final FirstOrderIntegrator integrator) {
        if (integrator instanceof AdaptiveStepsizeIntegrator) {
            final int n = stateVector.length;
            final int k = selectedParameters.length;
            resizeArray(integrator, ABSOLUTE_TOLERANCE, n * (n + 1 + k), true);
            resizeArray(integrator, RELATIVE_TOLERANCE, n * (n + 1 + k), false);
        }
    }

    /** Reset integrator tolerance array to original size.
     * @param integrator integrator
     */
    private void resetToleranceArray(final FirstOrderIntegrator integrator) {
        if (integrator instanceof AdaptiveStepsizeIntegrator) {
            final int n = stateVector.length;
            resizeArray(integrator, ABSOLUTE_TOLERANCE, n, true);
            resizeArray(integrator, RELATIVE_TOLERANCE, n, false);
        }
    }

    /** Resize object internal array.
     * @param instance instance concerned
     * @param fieldName field name
     * @param newSize new array size
     * @param isAbsolute flag to fill the new array
     */
    private void resizeArray(final Object instance, final String fieldName,
                             final int newSize, final boolean isAbsolute) {
        try {
            final Field arrayField = AdaptiveStepsizeIntegrator.class.getDeclaredField(fieldName);
            arrayField.setAccessible(true);
            final double[] originalArray = (double[]) arrayField.get(instance);
            final int originalSize = originalArray.length;
            final double[] resizedArray = new double[newSize];
            if (newSize > originalSize) {
                // expand array
                System.arraycopy(originalArray, 0, resizedArray, 0, originalSize);
                final double filler = isAbsolute ? Double.POSITIVE_INFINITY : 0.0;
                Arrays.fill(resizedArray, originalSize, newSize, filler);
            } else {
                // shrink array
                System.arraycopy(originalArray, 0, resizedArray, 0, newSize);
            }
            arrayField.set(instance, resizedArray);
        } catch (NoSuchFieldException nsfe) {
            throw OrekitException.createInternalError(nsfe);
        } catch (IllegalAccessException iae) {
            throw OrekitException.createInternalError(iae);
        }
    }

    /** Get hY from integrator absolute tolerance array.
     * @param integrator integrator
     * @return step sizes array for df/dy computing
     */
    private double[] getHy(final FirstOrderIntegrator integrator) {
        double[] hY = new double[0];
        if (integrator instanceof AdaptiveStepsizeIntegrator) {
            try {
                final Field arrayField = AdaptiveStepsizeIntegrator.class.getDeclaredField(ABSOLUTE_TOLERANCE);
                arrayField.setAccessible(true);
                hY = (double[]) arrayField.get(integrator);
                for (int i = 0; i < hY.length; i++) {
                    hY[i] *= 10.;
                }
            } catch (NoSuchFieldException nsfe) {
                throw OrekitException.createInternalError(nsfe);
            } catch (IllegalAccessException iae) {
                throw OrekitException.createInternalError(iae);
            }
        }
        return hY;
    }

    /** Internal class for differential equations representation. */
    private class DifferentialEquations implements ParameterizedODE, ODEWithJacobians {

        /** Build a new instance. */
        protected DifferentialEquations() {
            calls = 0;
        }

        /** {@inheritDoc} */
        public int getDimension() {
            return 7;
        }

        /** {@inheritDoc} */
        public void computeDerivatives(final double t, final double[] y, final double[] yDot)
            throws DerivativeException {

            try {
                // update space dynamics view
                currentState = mapState(t, y, startDate, currentState.getFrame());

                // compute cartesian coordinates
                if (currentState.getMass() <= 0.0) {
                    throw OrekitException.createIllegalArgumentException("spacecraft mass becomes negative (m: {0})",
                                                                         currentState.getMass());
                }

                // initialize derivatives
                adder.initDerivatives(yDot, (EquinoctialOrbit) currentState.getOrbit());

                // compute the contributions of all perturbing forces
                for (final ForceModel forceModel : forceModels) {
                    forceModel.addContribution(currentState, adder);
                }

                // finalize derivatives by adding the Kepler contribution
                adder.addKeplerContribution();

                // increment calls counter
                ++calls;

            } catch (OrekitException oe) {
                throw new DerivativeException(oe.getMessage());
            }

        }

        /** {@inheritDoc} */
        public void computeJacobians(final double t, final double[] y, final double[] yDot,
                                     final double[][] dFdY, final double[][] dFdP)
            throws DerivativeException {
        }

        /** {@inheritDoc} */
        public int getParametersDimension() {
            return selectedParameters.length;
        }

        /** {@inheritDoc} */
        public void setParameter(final int index, final double value) {
            final ParameterPair pp = paramPairs.get(index);
            pp.getParamHandler().setParameter(pp.getParamName(), value);
        }

        /** Convert state array to space dynamics objects (AbsoluteDate and OrbitalParameters).
         * @param t integration time (s)
         * @param y state as a flat array
         * @param referenceDate reference date from which t is counted
         * @param frame frame in which integration is performed
         * @return state corresponding to the flat array as a space dynamics object
         * @exception OrekitException if the attitude state cannot be determined
         * by the attitude law
         */
        private SpacecraftState mapState(final double t, final double [] y,
                                         final AbsoluteDate referenceDate, final Frame frame)
            throws OrekitException {

            // convert raw mathematical data to space dynamics objects
            final AbsoluteDate date = referenceDate.shiftedBy(t);
            final EquinoctialOrbit orbit =
                new EquinoctialOrbit(y[0], y[1], y[2], y[3], y[4], y[5],
                                     EquinoctialOrbit.TRUE_LATITUDE_ARGUMENT,
                                     frame, date, mu);
            final Attitude attitude = attitudeLaw.getAttitude(orbit);

            return new SpacecraftState(orbit, attitude, y[6]);

        }

    }

    /** Internal class used to pair a parameter name with its handler. */
    private static class ParameterPair {

        /** Parameter name. */
        private final String paramName;

        /** Parameter handler. */
        private final Parameterizable paramHandler;

        /** Simple constructor.
         * @param paramName parameter name
         * @param paramHandler force model handling the parameter
         */
        public ParameterPair(final String paramName, final Parameterizable paramHandler) {
            this.paramName = paramName;
            this.paramHandler = paramHandler;
        }

        /** Get parameter name.
         * @return parameter name
         */
        public String getParamName() {
            return paramName;
        }

        /** Get parameter handler.
         * @return force model handling the parameter
         */
        public Parameterizable getParamHandler() {
            return paramHandler;
        }

    }

    /** Internal class enabling basic force model
     *  to be used when processing parameters jacobian.
     */
    private static class ForceModelWrapper implements ForceModelWithJacobians {

        /** Serializable UID. */
        private static final long serialVersionUID = 3625153851142193056L;

        /** Wrapped basic force model. */
        private final ForceModel basic;

        /** Simple constructor.
         * @param basic force model to wrap
         */
        public ForceModelWrapper(final ForceModel basic) {
            this.basic = basic;
        }

        /** {@inheritDoc} */
        public void addContribution(final SpacecraftState s,
                                    final TimeDerivativesEquations adder)
            throws OrekitException {
            basic.addContribution(s, adder);
        }

        /** {@inheritDoc} */
        public EventDetector[] getEventsDetectors() {
            return basic.getEventsDetectors();
        }

        /** {@inheritDoc} */
        public void addContributionWithJacobians(final SpacecraftState s,
                                                 final TimeDerivativesEquationsWithJacobians adder)
            throws OrekitException {
        }

        /** {@inheritDoc} */
        public double getParameter(final String name) throws IllegalArgumentException {
            return Double.NaN;
        }

        /** {@inheritDoc} */
        public Collection<String> getParametersNames() {
            return new ArrayList<String>();
        }

        /** {@inheritDoc} */
        public void setParameter(final String name, final double value)
            throws IllegalArgumentException {
        }

    }

}
