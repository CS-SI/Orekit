/* Copyright 2002-2019 CS Systèmes d'Information
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
package org.orekit.gnss;

import org.orekit.propagation.analytical.gnss.GLONASSOrbitalElements;
import org.orekit.propagation.numerical.GLONASSNumericalPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.GLONASSDate;

/**
 * Class for GLONASS ephemeris used by the {@link GLONASSNumericalPropagator}.
 *
 * @author Bryan Cazabonne
 * @since 10.0
 *
 */
public class GLONASSEphemeris implements GLONASSOrbitalElements {

    /** Number of the current four year interval. */
    private final int n4;

    /** Number of the current day in a four year interval. */
    private final int nt;

    /** GLONASS ephemeris reference time. */
    private final double tb;

    /** ECEF-X component of satellite coordinates. */
    private final double x;

    /** ECEF-X component of satellite velocity. */
    private final double xDot;

    /** ECEF-X component of satellite acceleration. */
    private final double xDotDot;

    /** ECEF-Y component of satellite coordinates. */
    private final double y;

    /** ECEF-Y component of satellite velocity. */
    private final double yDot;

    /** ECEF-Y component of satellite acceleration. */
    private final double yDotDot;

    /** ECEF-Z component of satellite coordinates. */
    private final double z;

    /** ECEF-Z component of satellite velocity. */
    private final double zDot;

    /** ECEF-Z component of satellite acceleration. */
    private final double zDotDot;

    /**
     * Build a new instance.
     * @param n4 number of the current four year interval
     * @param nt number of the current day in a four year interval
     * @param tb reference time, s
     * @param x ECEF-X component of satellite coordinates, m
     * @param xDot ECEF-X component of satellite velocity, m/s
     * @param xDotDot ECEF-X component of satellite acceleration, m/s²
     * @param y ECEF-Y component of satellite coordinates, m
     * @param yDot ECEF-Y component of satellite velocity, m/s
     * @param yDotDot ECEF-Y component of satellite acceleration, m/s²
     * @param z ECEF-Z component of satellite coordinates, m
     * @param zDot ECEF-Z component of satellite velocity, m/s
     * @param zDotDot ECEF-Z component of satellite acceleration, m/s²
     */
    public GLONASSEphemeris(final int n4, final int nt, final double tb,
                             final double x, final double xDot, final double xDotDot,
                             final double y, final double yDot, final double yDotDot,
                             final double z, final double zDot, final double zDotDot) {
        this.n4 = n4;
        this.nt = nt;
        this.tb = tb;
        this.x = x;
        this.xDot = xDot;
        this.xDotDot = xDotDot;
        this.y = y;
        this.yDot = yDot;
        this.yDotDot = yDotDot;
        this.z = z;
        this.zDot = zDot;
        this.zDotDot = zDotDot;
    }

    @Override
    public AbsoluteDate getDate() {
        return new GLONASSDate(nt, n4, tb).getDate();
    }

    @Override
    public double getTime() {
        return tb;
    }

    @Override
    public double getXDot() {
        return xDot;
    }

    @Override
    public double getX() {
        return x;
    }

    @Override
    public double getXDotDot() {
        return xDotDot;
    }

    @Override
    public double getYDot() {
        return yDot;
    }

    @Override
    public double getY() {
        return y;
    }

    @Override
    public double getYDotDot() {
        return yDotDot;
    }

    @Override
    public double getZDot() {
        return zDot;
    }

    @Override
    public double getZ() {
        return z;
    }

    @Override
    public double getZDotDot() {
        return zDotDot;
    }

}
