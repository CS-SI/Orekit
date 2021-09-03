/* Copyright 2011-2012 Space Applications Services
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
package org.orekit.models.earth.troposphere;

import java.util.Collections;
import java.util.List;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.analysis.interpolation.PiecewiseBicubicSplineInterpolatingFunction;
import org.hipparchus.analysis.interpolation.PiecewiseBicubicSplineInterpolator;
import org.hipparchus.util.FastMath;
import org.orekit.annotation.DefaultDataContext;
import org.orekit.bodies.FieldGeodeticPoint;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.data.DataContext;
import org.orekit.data.DataProvidersManager;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.InterpolationTableLoader;
import org.orekit.utils.ParameterDriver;

/** A static tropospheric model that interpolates the actual tropospheric delay
 * based on values read from a configuration file (tropospheric-delay.txt) via
 * the {@link DataProvidersManager}.
 * @author Thomas Neidhart
 */
public class FixedTroposphericDelay implements DiscreteTroposphericModel {

    /** Singleton object for the default model. */
    private static FixedTroposphericDelay defaultModel;

    /** Abscissa grid for the bi-variate interpolation function read from the file. */
    private final double[] xArr;

    /** Ordinate grid for the bi-variate interpolation function read from the file. */
    private final double[] yArr;

    /** Values samples for the bi-variate interpolation function read from the file. */
    private final double[][] fArr;

    /** Interpolation function for the tropospheric delays. */
    private PiecewiseBicubicSplineInterpolatingFunction delayFunction;

    /** Creates a new {@link FixedTroposphericDelay} instance.
     * @param xArr abscissa grid for the interpolation function
     * @param yArr ordinate grid for the interpolation function
     * @param fArr values samples for the interpolation function
     */
    public FixedTroposphericDelay(final double[] xArr, final double[] yArr, final double[][] fArr) {
        this.xArr = xArr.clone();
        this.yArr = yArr.clone();
        this.fArr = fArr.clone();
        delayFunction = new PiecewiseBicubicSplineInterpolator().interpolate(xArr, yArr, fArr);
    }

    /** Creates a new {@link FixedTroposphericDelay} instance, and loads the
     * delay values from the given resource via the {@link DataContext#getDefault()
     * default data context}.
     *
     * @param supportedName a regular expression for supported resource names
     * @see #FixedTroposphericDelay(String, DataProvidersManager)
     */
    @DefaultDataContext
    public FixedTroposphericDelay(final String supportedName) {
        this(supportedName, DataContext.getDefault().getDataProvidersManager());
    }

    /**
     * Creates a new {@link FixedTroposphericDelay} instance, and loads the delay values
     * from the given resource via the specified data manager.
     *
     * @param supportedName a regular expression for supported resource names
     * @param dataProvidersManager provides access to auxiliary data.
     * @since 10.1
     */
    public FixedTroposphericDelay(final String supportedName,
                                  final DataProvidersManager dataProvidersManager) {

        final InterpolationTableLoader loader = new InterpolationTableLoader();
        dataProvidersManager.feed(supportedName, loader);

        if (!loader.stillAcceptsData()) {
            xArr = loader.getAbscissaGrid();
            yArr = loader.getOrdinateGrid();
            for (int i = 0; i < yArr.length; ++i) {
                yArr[i] = FastMath.toRadians(yArr[i]);
            }
            fArr = loader.getValuesSamples();
            delayFunction = new PiecewiseBicubicSplineInterpolator().interpolate(xArr, yArr, fArr);
        } else {
            throw new OrekitException(OrekitMessages.UNABLE_TO_FIND_RESOURCE, supportedName);
        }
    }

    /** Returns the default model, loading delay values from the file
     * "tropospheric-delay.txt" via the {@link DataContext#getDefault() default data
     * context}.
     *
     * <p>This method uses the {@link DataContext#getDefault() default data context}.
     *
     * @return the default model
     */
    @DefaultDataContext
    public static FixedTroposphericDelay getDefaultModel() {
        synchronized (FixedTroposphericDelay.class) {
            if (defaultModel == null) {
                defaultModel = new FixedTroposphericDelay("^tropospheric-delay\\.txt$");
            }
        }
        return defaultModel;
    }

    /** {@inheritDoc} */
    @Override
    public double pathDelay(final double elevation, final GeodeticPoint point,
                            final double[] parameters, final AbsoluteDate date) {
        // limit the height to 5000 m
        final double h = FastMath.min(FastMath.max(0, point.getAltitude()), 5000);
        // limit the elevation to 0 - π
        final double ele = FastMath.min(FastMath.PI, FastMath.max(0d, elevation));
        // mirror elevation at the right angle of π/2
        final double e = ele > 0.5 * FastMath.PI ? FastMath.PI - ele : ele;

        return delayFunction.value(h, e);
    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> T pathDelay(final T elevation, final FieldGeodeticPoint<T> point,
                                                       final T[] parameters, final FieldAbsoluteDate<T> date) {
        final T zero = date.getField().getZero();
        final T pi   = zero.getPi();
        // limit the height to 5000 m
        final T h = FastMath.min(FastMath.max(zero, point.getAltitude()), zero.add(5000));
        // limit the elevation to 0 - π
        final T ele = FastMath.min(pi, FastMath.max(zero, elevation));
        // mirror elevation at the right angle of π/2
        final T e = ele.getReal() > pi.multiply(0.5).getReal() ? ele.negate().add(pi) : ele;

        return delayFunction.value(h, e);
    }

    /** {@inheritDoc} */
    @Override
    public List<ParameterDriver> getParametersDrivers() {
        return Collections.emptyList();
    }

}
