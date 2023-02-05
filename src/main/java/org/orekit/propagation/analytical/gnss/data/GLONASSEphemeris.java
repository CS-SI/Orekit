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

import org.orekit.annotation.DefaultDataContext;
import org.orekit.data.DataContext;
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

    /** Date of applicability. */
    private final AbsoluteDate date;

    /**
     * Build a new instance.
     *
     * <p>This method uses the {@link DataContext#getDefault() default data context}.
     *
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
     * @see #GLONASSEphemeris(int, int, double, double, double, double, double, double,
     * double, double, double, double, AbsoluteDate)
     */
    @DefaultDataContext
    public GLONASSEphemeris(final int n4, final int nt, final double tb,
                            final double x, final double xDot, final double xDotDot,
                            final double y, final double yDot, final double yDotDot,
                            final double z, final double zDot, final double zDotDot) {
        this(n4, nt, tb, x, xDot, xDotDot, y, yDot, yDotDot, z, zDot, zDotDot,
            new GLONASSDate(nt, n4, tb,
                    DataContext.getDefault().getTimeScales().getGLONASS()).getDate());
    }

    /**
     * Build a new instance.
     *
     * @param n4      number of the current four year interval
     * @param nt      number of the current day in a four year interval
     * @param tb      reference time, s
     * @param x       ECEF-X component of satellite coordinates, m
     * @param xDot    ECEF-X component of satellite velocity, m/s
     * @param xDotDot ECEF-X component of satellite acceleration, m/s²
     * @param y       ECEF-Y component of satellite coordinates, m
     * @param yDot    ECEF-Y component of satellite velocity, m/s
     * @param yDotDot ECEF-Y component of satellite acceleration, m/s²
     * @param z       ECEF-Z component of satellite coordinates, m
     * @param zDot    ECEF-Z component of satellite velocity, m/s
     * @param zDotDot ECEF-Z component of satellite acceleration, m/s²
     * @param date    of applicability corresponding to {@code nt}, {@code n4}, and {@code
     *                tb}.
     * @since 10.1
     */
    public GLONASSEphemeris(final int n4, final int nt, final double tb,
                            final double x, final double xDot, final double xDotDot,
                            final double y, final double yDot, final double yDotDot,
                            final double z, final double zDot, final double zDotDot,
                            final AbsoluteDate date) {
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
        this.date = date;
    }

    @Override
    public AbsoluteDate getDate() {
        return date;
    }

    @Override
    public int getN4() {
        return n4;
    }

    @Override
    public int getNa() {
        return nt;
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
