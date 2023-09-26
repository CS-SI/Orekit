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
package org.orekit.propagation.analytical.tle.generation;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathUtils;
import org.orekit.annotation.DefaultDataContext;
import org.orekit.attitudes.FrameAlignedProvider;
import org.orekit.data.DataContext;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.Frame;
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.orbits.FieldEquinoctialOrbit;
import org.orekit.orbits.FieldKeplerianOrbit;
import org.orekit.orbits.FieldOrbit;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.tle.FieldTLE;
import org.orekit.propagation.analytical.tle.FieldTLEPropagator;
import org.orekit.propagation.analytical.tle.TLE;
import org.orekit.propagation.analytical.tle.TLEPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.utils.ParameterDriver;

/**
 * Fixed Point method to reverse SGP4 and SDP4 propagation algorithm
 * and generate a usable TLE from a spacecraft state.
 * <p>
 * Using this algorithm, the B* value is not computed. In other words,
 * the B* value from the template TLE is set to the generated one.
 * </p>
 * @author Thomas Paulet
 * @author Bryan Cazabonne
 * @since 12.0
 */
public class FixedPointTleGenerationAlgorithm implements TleGenerationAlgorithm {

    /** Default value for epsilon. */
    public static final double EPSILON_DEFAULT = 1.0e-10;

    /** Default value for maxIterations. */
    public static final int MAX_ITERATIONS_DEFAULT = 100;

    /** Default value for scale. */
    public static final double SCALE_DEFAULT = 1.0;

    /** Used to compute threshold for convergence check. */
    private final double epsilon;

    /** Maximum number of iterations for convergence. */
    private final int maxIterations;

    /** Scale factor of the Fixed Point algorithm. */
    private final double scale;

    /** UTC scale. */
    private final TimeScale utc;

    /** TEME frame. */
    private final Frame teme;

    /**
     * Default constructor.
     * <p>
     * Uses the {@link DataContext#getDefault() default data context}
     * as well as {@link #EPSILON_DEFAULT}, {@link #MAX_ITERATIONS_DEFAULT},
     * {@link #SCALE_DEFAULT} for method convergence.
     * </p>
     */
    @DefaultDataContext
    public FixedPointTleGenerationAlgorithm() {
        this(EPSILON_DEFAULT, MAX_ITERATIONS_DEFAULT, SCALE_DEFAULT);
    }

    /**
     * Constructor.
     * <p>
     * Uses the {@link DataContext#getDefault() default data context}.
     * </p>
     * @param epsilon used to compute threshold for convergence check
     * @param maxIterations maximum number of iterations for convergence
     * @param scale scale factor of the Fixed Point algorithm
     */
    @DefaultDataContext
    public FixedPointTleGenerationAlgorithm(final double epsilon, final int maxIterations,
                                            final double scale) {
        this(epsilon, maxIterations, scale,
             DataContext.getDefault().getTimeScales().getUTC(),
             DataContext.getDefault().getFrames().getTEME());
    }

    /**
     * Constructor.
     * @param epsilon used to compute threshold for convergence check
     * @param maxIterations maximum number of iterations for convergence
     * @param scale scale factor of the Fixed Point algorithm
     * @param utc UTC time scale
     * @param teme TEME frame
     */
    public FixedPointTleGenerationAlgorithm(final double epsilon, final int maxIterations,
                                            final double scale, final TimeScale utc,
                                            final Frame teme) {
        this.epsilon       = epsilon;
        this.maxIterations = maxIterations;
        this.scale         = scale;
        this.utc           = utc;
        this.teme          = teme;
    }

    /** {@inheritDoc} */
    @Override
    public TLE generate(final SpacecraftState state, final TLE templateTLE) {

        // Generation epoch
        final AbsoluteDate epoch = state.getDate();

        // gets equinoctial parameters in TEME frame from state
        final EquinoctialOrbit equinoctialOrbit = convert(state.getOrbit());
        double sma = equinoctialOrbit.getA();
        double ex  = equinoctialOrbit.getEquinoctialEx();
        double ey  = equinoctialOrbit.getEquinoctialEy();
        double hx  = equinoctialOrbit.getHx();
        double hy  = equinoctialOrbit.getHy();
        double lv  = equinoctialOrbit.getLv();

        // rough initialization of the TLE
        final KeplerianOrbit keplerianOrbit = (KeplerianOrbit) OrbitType.KEPLERIAN.convertType(equinoctialOrbit);
        TLE current = TleGenerationUtil.newTLE(keplerianOrbit, templateTLE, templateTLE.getBStar(epoch), utc);

        // threshold for each parameter
        final double thrA = epsilon * (1 + sma);
        final double thrE = epsilon * (1 + FastMath.hypot(ex, ey));
        final double thrH = epsilon * (1 + FastMath.hypot(hx, hy));
        final double thrV = epsilon * FastMath.PI;

        int k = 0;
        while (k++ < maxIterations) {

            // recompute the state from the current TLE
            final TLEPropagator propagator = TLEPropagator.selectExtrapolator(current,
                                                                              new FrameAlignedProvider(Rotation.IDENTITY, teme),
                                                                              state.getMass(), teme);
            final Orbit recoveredOrbit = propagator.getInitialState().getOrbit();
            final EquinoctialOrbit recoveredEquiOrbit = (EquinoctialOrbit) OrbitType.EQUINOCTIAL.convertType(recoveredOrbit);

            // adapted parameters residuals
            final double deltaSma = equinoctialOrbit.getA() - recoveredEquiOrbit.getA();
            final double deltaEx  = equinoctialOrbit.getEquinoctialEx() - recoveredEquiOrbit.getEquinoctialEx();
            final double deltaEy  = equinoctialOrbit.getEquinoctialEy() - recoveredEquiOrbit.getEquinoctialEy();
            final double deltaHx  = equinoctialOrbit.getHx() - recoveredEquiOrbit.getHx();
            final double deltaHy  = equinoctialOrbit.getHy() - recoveredEquiOrbit.getHy();
            final double deltaLv  = MathUtils.normalizeAngle(equinoctialOrbit.getLv() - recoveredEquiOrbit.getLv(), 0.0);

            // check convergence
            if (FastMath.abs(deltaSma) < thrA &&
                FastMath.abs(deltaEx)  < thrE &&
                FastMath.abs(deltaEy)  < thrE &&
                FastMath.abs(deltaHx)  < thrH &&
                FastMath.abs(deltaHy)  < thrH &&
                FastMath.abs(deltaLv)  < thrV) {

                // verify if parameters are estimated
                for (final ParameterDriver templateDrivers : templateTLE.getParametersDrivers()) {
                    if (templateDrivers.isSelected()) {
                        // set to selected for the new TLE
                        current.getParameterDriver(templateDrivers.getName()).setSelected(true);
                    }
                }

                // return
                return current;
            }

            // update state
            sma += scale * deltaSma;
            ex  += scale * deltaEx;
            ey  += scale * deltaEy;
            hx  += scale * deltaHx;
            hy  += scale * deltaHy;
            lv  += scale * deltaLv;
            final EquinoctialOrbit newEquinoctialOrbit =
                                    new EquinoctialOrbit(sma, ex, ey, hx, hy, lv, PositionAngleType.TRUE,
                                                         equinoctialOrbit.getFrame(), equinoctialOrbit.getDate(), equinoctialOrbit.getMu());
            final KeplerianOrbit newKeplerianOrbit = (KeplerianOrbit) OrbitType.KEPLERIAN.convertType(newEquinoctialOrbit);

            // update TLE
            current = TleGenerationUtil.newTLE(newKeplerianOrbit, templateTLE, templateTLE.getBStar(epoch), utc);

        }

        // unable to generate a TLE
        throw new OrekitException(OrekitMessages.UNABLE_TO_COMPUTE_TLE, k);

    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> FieldTLE<T> generate(final FieldSpacecraftState<T> state,
                                                                    final FieldTLE<T> templateTLE) {

        // gets equinoctial parameters in TEME frame from state
        final FieldEquinoctialOrbit<T> equinoctialOrbit = convert(state.getOrbit());
        T sma = equinoctialOrbit.getA();
        T ex  = equinoctialOrbit.getEquinoctialEx();
        T ey  = equinoctialOrbit.getEquinoctialEy();
        T hx  = equinoctialOrbit.getHx();
        T hy  = equinoctialOrbit.getHy();
        T lv  = equinoctialOrbit.getLv();

        // rough initialization of the TLE
        final T bStar = state.getA().getField().getZero().add(templateTLE.getBStar());
        final FieldKeplerianOrbit<T> keplerianOrbit = (FieldKeplerianOrbit<T>) OrbitType.KEPLERIAN.convertType(equinoctialOrbit);
        FieldTLE<T> current = TleGenerationUtil.newTLE(keplerianOrbit, templateTLE, bStar, utc);

        // field
        final Field<T> field = state.getDate().getField();

        // threshold for each parameter
        final T thrA = sma.add(1).multiply(epsilon);
        final T thrE = FastMath.hypot(ex, ey).add(1).multiply(epsilon);
        final T thrH = FastMath.hypot(hx, hy).add(1).multiply(epsilon);
        final T thrV = sma.getPi().multiply(epsilon);

        int k = 0;
        while (k++ < maxIterations) {

            // recompute the state from the current TLE
            final FieldTLEPropagator<T> propagator = FieldTLEPropagator.selectExtrapolator(current, new FrameAlignedProvider(Rotation.IDENTITY, teme),
                                                                                           state.getMass(), teme, templateTLE.getParameters(field));
            final FieldOrbit<T> recoveredOrbit = propagator.getInitialState().getOrbit();
            final FieldEquinoctialOrbit<T> recoveredEquinoctialOrbit = (FieldEquinoctialOrbit<T>) OrbitType.EQUINOCTIAL.convertType(recoveredOrbit);

            // adapted parameters residuals
            final T deltaSma = equinoctialOrbit.getA().subtract(recoveredEquinoctialOrbit.getA());
            final T deltaEx  = equinoctialOrbit.getEquinoctialEx().subtract(recoveredEquinoctialOrbit.getEquinoctialEx());
            final T deltaEy  = equinoctialOrbit.getEquinoctialEy().subtract(recoveredEquinoctialOrbit.getEquinoctialEy());
            final T deltaHx  = equinoctialOrbit.getHx().subtract(recoveredEquinoctialOrbit.getHx());
            final T deltaHy  = equinoctialOrbit.getHy().subtract(recoveredEquinoctialOrbit.getHy());
            final T deltaLv  = MathUtils.normalizeAngle(equinoctialOrbit.getLv().subtract(recoveredEquinoctialOrbit.getLv()), field.getZero());

            // check convergence
            if (FastMath.abs(deltaSma.getReal()) < thrA.getReal() &&
                FastMath.abs(deltaEx.getReal())  < thrE.getReal() &&
                FastMath.abs(deltaEy.getReal())  < thrE.getReal() &&
                FastMath.abs(deltaHx.getReal())  < thrH.getReal() &&
                FastMath.abs(deltaHy.getReal())  < thrH.getReal() &&
                FastMath.abs(deltaLv.getReal())  < thrV.getReal()) {

                // return
                return current;

            }

            // update state
            sma = sma.add(deltaSma.multiply(scale));
            ex  = ex.add(deltaEx.multiply(scale));
            ey  = ey.add(deltaEy.multiply(scale));
            hx  = hx.add(deltaHx.multiply(scale));
            hy  = hy.add(deltaHy.multiply(scale));
            lv  = lv.add(deltaLv.multiply(scale));
            final FieldEquinoctialOrbit<T> newEquinoctialOrbit =
                                    new FieldEquinoctialOrbit<>(sma, ex, ey, hx, hy, lv, PositionAngleType.TRUE,
                                    equinoctialOrbit.getFrame(), equinoctialOrbit.getDate(), equinoctialOrbit.getMu());
            final FieldKeplerianOrbit<T> newKeplerianOrbit = (FieldKeplerianOrbit<T>) OrbitType.KEPLERIAN.convertType(newEquinoctialOrbit);

            // update TLE
            current = TleGenerationUtil.newTLE(newKeplerianOrbit, templateTLE, bStar, utc);

        }

        throw new OrekitException(OrekitMessages.UNABLE_TO_COMPUTE_TLE, k);

    }

    /**
     * Converts an orbit into an equinoctial orbit expressed in TEME frame.
     * @param orbitIn the orbit to convert
     * @return the converted orbit, i.e. equinoctial in TEME frame
     */
    private EquinoctialOrbit convert(final Orbit orbitIn) {
        return new EquinoctialOrbit(orbitIn.getPVCoordinates(teme), teme, orbitIn.getMu());
    }

    /**
     * Converts an orbit into an equinoctial orbit expressed in TEME frame.
     * @param orbitIn the orbit to convert
     * @param <T> type of the element
     * @return the converted orbit, i.e. equinoctial in TEME frame
     */
    private <T extends CalculusFieldElement<T>> FieldEquinoctialOrbit<T> convert(final FieldOrbit<T> orbitIn) {
        return new FieldEquinoctialOrbit<T>(orbitIn.getPVCoordinates(teme), teme, orbitIn.getMu());
    }

}
