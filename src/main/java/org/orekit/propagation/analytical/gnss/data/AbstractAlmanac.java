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
package org.orekit.propagation.analytical.gnss.data;

import org.hipparchus.util.FastMath;

/**
 * Base class for GNSS almanacs.
 * @author Pascal Parraud
 * @since 11.0
 */
public abstract class AbstractAlmanac extends CommonGnssData implements GNSSOrbitalElements {

    /**
     * Constructor.
     * @param mu Earth's universal gravitational parameter
     * @param angularVelocity mean angular velocity of the Earth for the GNSS model
     * @param weekNumber number of weeks in the GNSS cycle
     */
    public AbstractAlmanac(final double mu,
                           final double angularVelocity,
                           final int weekNumber) {
        super(mu, angularVelocity, weekNumber);
    }

    /**
     * Getter for the mean motion.
     * @return the mean motion
     */
    public double getMeanMotion() {
        final double absA = FastMath.abs(getSma());
        return FastMath.sqrt(getMu() / absA) / absA;
    }

    /**
     * Getter for the rate of inclination angle.
     * <p>
     * By default, not contained in a GNSS almanac
     * </p>
     * @return the rate of inclination angle in rad/s
     */
    public double getIDot() {
        return 0.0;
    }

    /**
     * Getter for the Cuc parameter.
     * <p>
     * By default, not contained in a GNSS almanac
     * </p>
     * @return the Cuc parameter
     */
    public double getCuc() {
        return 0.0;
    }

    /**
     * Getter for the Cus parameter.
     * <p>
     * By default, not contained in a GNSS almanac
     * </p>
     * @return the Cus parameter
     */
    public double getCus() {
        return 0.0;
    }

    /**
     * Getter for the Crc parameter.
     * <p>
     * By default, not contained in a GNSS almanac
     * </p>
     * @return the Crc parameter
     */
    public double getCrc() {
        return 0.0;
    }

    /**
     * Getter for the Crs parameter.
     * <p>
     * By default, not contained in a GNSS almanac
     * </p>
     * @return the Crs parameter
     */
    public double getCrs() {
        return 0.0;
    }

    /**
     * Getter for the Cic parameter.
     * <p>
     * By default, not contained in a GNSS almanac
     * </p>
     * @return the Cic parameter
     */
    public double getCic() {
        return 0.0;
    }

    /**
     * Getter for the Cis parameter.
     * <p>
     * By default, not contained in a GNSS almanac
     * </p>
     * @return the Cis parameter
     */
    public double getCis() {
        return 0.0;
    }

    /**
     * Getter for the Drift Rate Correction Coefficient.
     * <p>
     * By default, not contained in a GNSS almanac
     * </p>
     * @return the Drift Rate Correction Coefficient (s/s²).
     */
    public double getAf2() {
        return 0.0;
    }

}
