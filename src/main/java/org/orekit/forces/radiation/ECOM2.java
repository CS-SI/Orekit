/* Copyright 2002-2020 CS GROUP
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
package org.orekit.forces.radiation;

import org.hipparchus.RealFieldElement;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.FieldSinCos;
import org.hipparchus.util.SinCos;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.utils.ExtendedPVCoordinatesProvider;
import org.orekit.utils.ParameterDriver;

/**
 * The Empirical CODE Orbit Model 2 (ECOM2) of the Center for Orbit Determination in Europe (CODE).
 * <p>
 * The drag acceleration is computed as follows :
 * γ = γ<sub>0</sub> + D(u)e<sub>D</sub> + Y(u)e<sub>Y</sub> + B(u)e<sub>B</sub>
 * </p> <p>
 * In the above equation, γ<sub>0</sub> is a selectable a priori model. Since 2013, no
 * a priori model is used for CODE IGS contribution (i.e. γ<sub>0</sub> = 0). Moreover,
 * u denotes the satellite's argument of latitude.
 * </p> <p>
 * D(u), Y(u) and B(u) are three functions of the ECOM2 model that can be represented
 * as Fourier series. The coefficients of the Fourier series are estimated during the
 * estimation process. he ECOM2 model has user-defines upper limits <i>nD</i> and
 * <i>nB</i> for the Fourier series (i.e. <i>nD</i> for D(u) and <i>nB</i> for
 * B(u). Y(u) is defined as a constant value).
 * </p> <p>
 * It exists several configurations to initialize <i>nD</i> and <i>nB</i> values. However,
 * Arnold et al recommend to use <b>D2B1</b> (i.e. <i>nD</i> = 1 and <i>nB</i> = 1) and
 * <b>D4B1</b> (i.e. <i>nD</i> = 2 an <i>nB</i> = 1) configurations. At the opposite, in Arnold paper, it
 * is recommend to not use <b>D2B0</b> (i.e. <i>nD</i> = 1 and <i>nB</i> = 0) configuration.
 * </p>
 *
 * @see "Arnold, Daniel, et al, CODE’s new solar radiation pressure model for GNSS orbit determination,
 *       Journal of geodesy 89.8 (2015): 775-791."
 *
 * @see "Tzu-Pang tseng and Michael Moore, Impact of solar radiation pressure mis-modeling on
 *       GNSS satellite orbit determination, IGS Worshop, Wuhan, China, 2018."
 *
 * @author David Soulard
 * @since 10.2
 */
public class ECOM2 extends AbstractRadiationForceModel {

    /** Parameter name for ECOM model coefficients enabling Jacobian processing. */
    public static final String ECOM_COEFFICIENT = "ECOM coefficient";

    /** Minimum value for ECOM2 estimated parameters. */
    private static final double MIN_VALUE = Double.NEGATIVE_INFINITY;

    /** Maximum value for ECOM2 estimated parameters. */
    private static final double MAX_VALUE = Double.POSITIVE_INFINITY;

    /** Parameters scaling factor.
     * <p>
     * We use a power of 2 to avoid numeric noise introduction
     * in the multiplications/divisions sequences.
     * </p>
     */
    private final double SCALE = FastMath.scalb(1.0, -22);

    /** Highest order for parameter along eD axis (satellite --> sun direction). */
    private final int nD;

    /** Highest order for parameter along eB axis. */
    private final int nB;

    /** Estimated acceleration coefficients.
     * <p>
     * The 2 * nD first driver are Fourier driver along eD, axis,
     * then along eY, then 2*nB following are along eB axis.
     * </p>
     */
    private final ParameterDriver[] coefficients;

    /** Sun model. */
    private final ExtendedPVCoordinatesProvider sun;

    /**
     * Constructor.
     * @param nD truncation rank of Fourier series in D term.
     * @param nB truncation rank of Fourier series in B term.
     * @param value parameters initial value
     * @param sun provide for Sun parameter
     * @param equatorialRadius spherical shape model (for umbra/penumbra computation)
     */
    public ECOM2(final int nD, final int nB, final double value,
                 final ExtendedPVCoordinatesProvider sun, final double equatorialRadius) {
        super(sun, equatorialRadius);
        this.nB = nB;
        this.nD = nD;
        this.coefficients = new ParameterDriver[2 * (nD + nB) + 3];
        ParameterDriver driver;
        // Add parameter along eB axis in alphabetical order
        driver = new ParameterDriver(ECOM_COEFFICIENT + " B0", value, SCALE, MIN_VALUE, MAX_VALUE);
        driver.setSelected(true);
        coefficients[0] = driver;
        for (int i = 1; i < nB + 1; i++) {
            driver = new ParameterDriver(ECOM_COEFFICIENT + " Bcos" + Integer.toString(i - 1), value, SCALE, MIN_VALUE, MAX_VALUE);
            driver.setSelected(true);
            coefficients[i]  = driver;
        }
        for (int i = nB + 1; i < 2 * nB + 1; i++) {
            driver = new ParameterDriver(ECOM_COEFFICIENT + " Bsin" + Integer.toString(i - (nB + 1)), value, SCALE, MIN_VALUE, MAX_VALUE);
            driver.setSelected(true);
            coefficients[i] = driver;
        }
        // Add driver along eD axis in alphabetical order
        driver = new ParameterDriver(ECOM_COEFFICIENT + " D0", value, SCALE, MIN_VALUE, MAX_VALUE);
        driver.setSelected(true);
        coefficients[2 * nB + 1 ] = driver;
        for (int i = 2 * nB + 2; i < 2 * nB + 2 + nD; i++) {
            driver = new ParameterDriver(ECOM_COEFFICIENT + " Dcos" + Integer.toString(i - (2 * nB + 2)), value, SCALE, MIN_VALUE, MAX_VALUE);
            driver.setSelected(true);
            coefficients[i] = driver;
        }
        for (int i = 2 * nB + 2 + nD; i < 2 * (nB + nD) + 2; i++) {
            driver = new ParameterDriver(ECOM_COEFFICIENT + " Dsin" + Integer.toString(i - (2 * nB + nD + 2)), value, SCALE, MIN_VALUE, MAX_VALUE);
            driver.setSelected(true);
            coefficients[i] = driver;
        }
        // Add  Y0
        driver = new ParameterDriver(ECOM_COEFFICIENT + " Y0", value, SCALE, MIN_VALUE, MAX_VALUE);
        driver.setSelected(true);
        coefficients[2 * (nB + nD) + 2] = driver;
        this.sun = sun;
    }

    /** {@inheritDoc} */
    @Override
    public Vector3D acceleration(final SpacecraftState s, final double[] parameters) {
        // Build the coordinate system
        final Vector3D Z = s.getPVCoordinates().getMomentum().normalize();
        final Vector3D sunPos = sun.getPVCoordinates(s.getDate(), s.getFrame()).getPosition().normalize();
        final Vector3D Y = Z.crossProduct(sunPos);
        final Vector3D X = Y.crossProduct(Z);
        // Build eD, eY, eB vectors
        final Vector3D position = s.getPVCoordinates().getPosition().normalize();
        final Vector3D eD = sunPos.add(-1.0, position);
        final Vector3D eY = position.crossProduct(eD);
        final Vector3D eB = eD.crossProduct(eY);

        // Angular argument difference u_s - u
        final double  delta_u =  FastMath.atan2(position.dotProduct(Y), position.dotProduct(X));

        // Compute B(u)
        double b_u = parameters[0];
        for (int i = 1; i < nB + 1; i++) {
            final SinCos sc = FastMath.sinCos(2 * i * delta_u);
            b_u += parameters[i] * sc.cos() + parameters[i + nB] * sc.sin();
        }
        // Compute D(u)
        double d_u = parameters[2 * nB + 1];
        for (int i = 1; i < nD + 1; i++) {
            final SinCos sc = FastMath.sinCos((2 * i - 1) * delta_u);
            d_u += parameters[2 * nB + 1 + i] * sc.cos() + parameters[2 * nB + 1 + i + nD] * sc.sin();
        }
        // Return acceleration
        return new Vector3D(d_u, eD, parameters[2 * (nD + nB) + 2], eY, b_u, eB);
    }

    /** {@inheritDoc} */
    @Override
    public <T extends RealFieldElement<T>> FieldVector3D<T> acceleration(final FieldSpacecraftState<T> s, final T[] parameters) {
        // Build the coordinate system
        final FieldVector3D<T> Z = s.getPVCoordinates().getMomentum().normalize();
        final FieldVector3D<T> sunPos = sun.getPVCoordinates(s.getDate(), s.getFrame()).getPosition().normalize();
        final FieldVector3D<T> Y = Z.crossProduct(sunPos);
        final FieldVector3D<T> X = Y.crossProduct(Z);

        // Build eD, eY, eB vectors
        final FieldVector3D<T> position = s.getPVCoordinates().getPosition().normalize();
        final FieldVector3D<T> eD = sunPos.add(-1.0, position);
        final FieldVector3D<T> eY = position.crossProduct(eD);
        final FieldVector3D<T> eB = eD.crossProduct(eY);

        // Angular argument difference u_s - u
        final T  delta_u = FastMath.atan2(position.dotProduct(Y), position.dotProduct(X));

        // Compute B(u)
        T b_u =  parameters[0];
        for (int i = 1; i < nB + 1; i++) {
            final FieldSinCos<T> sc = FastMath.sinCos(delta_u.multiply(2 * i));
            b_u = b_u.add(sc.cos().multiply(parameters[i])).add(sc.sin().multiply(parameters[i + nB]));
        }
        // Compute D(u)
        T d_u = parameters[2 * nB + 1];

        for (int i = 1; i < nD + 1; i++) {
            final FieldSinCos<T> sc = FastMath.sinCos(delta_u.multiply(2 * i - 1));
            d_u =  d_u.add(sc.cos().multiply(parameters[2 * nB + 1 + i])).add(sc.sin().multiply(parameters[2 * nB + 1 + i + nD]));
        }
        // Return the acceleration
        return new FieldVector3D<>(d_u, eD, parameters[2 * (nD + nB) + 2], eY, b_u, eB);
    }

    /** {@inheritDoc} */
    @Override
    public ParameterDriver[] getParametersDrivers() {
        return coefficients.clone();
    }

}

