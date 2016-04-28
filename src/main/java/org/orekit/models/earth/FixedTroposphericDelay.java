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
package org.orekit.models.earth;

import org.hipparchus.analysis.BivariateFunction;
import org.hipparchus.analysis.interpolation.PiecewiseBicubicSplineInterpolator;
import org.hipparchus.util.FastMath;
import org.orekit.data.DataProvidersManager;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.utils.InterpolationTableLoader;

/** A static tropospheric model that interpolates the actual tropospheric delay
 * based on values read from a configuration file (tropospheric-delay.txt) via
 * the {@link DataProvidersManager}.
 * @author Thomas Neidhart
 */
public class FixedTroposphericDelay implements TroposphericModel {

    /** Serializable UID. */
    private static final long serialVersionUID = -92320711761929077L;

    /** Singleton object for the default model. */
    private static FixedTroposphericDelay defaultModel;

    /** Abscissa grid for the bi-variate interpolation function read from the file. */
    private final double[] xArr;

    /** Ordinate grid for the bi-variate interpolation function read from the file. */
    private final double[] yArr;

    /** Values samples for the bi-variate interpolation function read from the file. */
    private final double[][] fArr;

    /** Interpolation function for the tropospheric delays. */
    private transient BivariateFunction delayFunction;

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
     * delay values from the given resource via the {@link DataProvidersManager}.
     * @param supportedName a regular expression for supported resource names
     * @throws OrekitException if the resource could not be loaded
     */
    public FixedTroposphericDelay(final String supportedName) throws OrekitException {

        final InterpolationTableLoader loader = new InterpolationTableLoader();
        DataProvidersManager.getInstance().feed(supportedName, loader);

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
     * "tropospheric-delay.txt".
     * @return the default model
     * @throws OrekitException if the file could not be loaded
     */
    public static FixedTroposphericDelay getDefaultModel() throws OrekitException {
        synchronized (FixedTroposphericDelay.class) {
            if (defaultModel == null) {
                defaultModel = new FixedTroposphericDelay("^tropospheric-delay\\.txt$");
            }
        }
        return defaultModel;
    }

    /** {@inheritDoc} */
    public double pathDelay(final double elevation, final double height) {
        // limit the height to 5000 m
        final double h = FastMath.min(FastMath.max(0, height), 5000);
        // limit the elevation to 0 - π
        final double ele = FastMath.min(FastMath.PI, FastMath.max(0d, elevation));
        // mirror elevation at the right angle of π/2
        final double e = ele > 0.5 * FastMath.PI ? FastMath.PI - ele : ele;

        return delayFunction.value(h, e);
    }

    /** Make sure the unserializable bivariate interpolation function is properly rebuilt.
     * @return replacement object, with bivariate function properly set up
     */
    private Object readResolve() {
        return new FixedTroposphericDelay(xArr, yArr, fArr);
    }

}
