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

import org.apache.commons.math3.analysis.BivariateFunction;
import org.orekit.data.DataProvidersManager;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.utils.Constants;
import org.orekit.utils.InterpolationTableLoader;

/** A static tropospheric model that interpolates the actual tropospheric delay
 * based on values read from a configuration file (tropospheric-delay.txt) via
 * the {@link DataProvidersManager}.
 * @author Thomas Neidhart
 */
public class FixedTroposphericDelay implements TroposphericDelayModel {

    /** Serializable UID. */
    private static final long serialVersionUID = -92320711761929077L;

    /** Singleton object for the default model. */
    private static FixedTroposphericDelay defaultModel;

    /** Interpolation function for the tropospheric delays. */
    private final BivariateFunction delayFunction;

    /** Creates a new {@link FixedTroposphericDelay} instance, and loads the
     * delay values from the given resource via the {@link DataProvidersManager}.
     * @param supportedName a regular expression for supported resource names
     * @throws OrekitException if the resource could not be loaded
     */
    public FixedTroposphericDelay(final String supportedName) throws OrekitException {

        final InterpolationTableLoader loader = new InterpolationTableLoader();
        DataProvidersManager.getInstance().feed(supportedName, loader);

        if (!loader.stillAcceptsData()) {
            delayFunction = loader.getInterpolationFunction();
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
    public double calculatePathDelay(final double elevation, final double height) {
        // limit the height to 5000 m
        final double h = Math.min(Math.max(0, height), 5000);
        // limit the elevation to 0 - 180 degree
        final double ele = Math.min(180d, Math.max(0d, elevation));
        // mirror elevation at the right angle of 90 degree
        final double e = ele > 90d ? 180d - ele : ele;

        return delayFunction.value(h, e);
    }

    /** {@inheritDoc} */
    public double calculateSignalDelay(final double elevation, final double height) {
        return calculatePathDelay(elevation, height) / Constants.SPEED_OF_LIGHT;
    }
}
