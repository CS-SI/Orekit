/* Copyright 2002-2016 CS Systèmes d'Information
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
package org.orekit.propagation.analytical.gnss;

import java.io.Serializable;

import org.apache.commons.math3.util.FastMath;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeStamped;

/**
 * This class is a container for a minimal set of GPS orbital data as needed for the GPSPropagator.
 *
 * @author Pascal Parraud
 */
public class GPSOrbitalElements implements TimeStamped, Serializable {

    // Constants
    /** WGS 84 value of the Earth's universal gravitational parameter for GPS user in m3/s2 */
    public static final double GPS_MU = 3.986005e+14;

    /** Value of Pi for conversion from semi-circle to radian */
    public static final double GPS_PI = 3.1415926535898;

    /** Duration of the GPS week in seconds */
    public static final double GPS_WEEK_IN_SECONDS = 604800.;

    /** Number of weeks in the GPS cycle */
    public static final int GPS_WEEK_NB = 1024;

    /** Serializable UID. */
    private static final long serialVersionUID = 3439974091193312540L;

    // Fields
    /** The satellite PRN number */
    private final int prn;
    /** The Reference Week of the GPS orbit in [0, 1024[ */
    private final int week;
    /** The Reference Time of the GPS orbit (s) */
    private final double time;
    /** The Reference Date of the GPS orbit (from week and time) */
    private final AbsoluteDate tRef;
    /** Semi-Major Axis (m) */
    private final double sma;
    /** Mean Motion (rad/s) */
    private final double mmo;
    /** Eccentricity */
    private final double ecc;
    /** Inclination Angle at Reference Time (rad) */
    private final double i0;
    /** Rate of Inclination Angle (rad/s) */
    private final double iDot;
    /** Longitude of Ascending Node of Orbit Plane at Weekly Epoch (rad) */
    private final double omega0;
    /** Rate of Right Ascension (rad/s) */
    private final double omegaDot;
    /** Argument of Perigee (rad) */
    private final double pa;
    /** Mean Anomaly at Reference Time (rad) */
    private final double m0;
    /** Amplitude of the Cosine Harmonic Correction Term to the Argument of Latitude (rad) */
    private final double cuc;
    /** Amplitude of the Sine Harmonic Correction Term to the Argument of Latitude (rad) */
    private final double cus;
    /** Amplitude of the Cosine Harmonic Correction Term to the Orbit Radius (m) */
    private final double crc;
    /** Amplitude of the Sine Harmonic Correction Term to the Orbit Radius (m) */
    private final double crs;
    /** Amplitude of the Cosine Harmonic Correction Term to the Angle of Inclination (rad) */
    private final double cic;
    /** Amplitude of the Sine Harmonic Correction Term to the Angle of Inclination (rad) */
    private final double cis;

    /**
     * Simple constructor.
     *
     * @param prn the satellite PRN number
     * @param week the Reference Week of the GPS orbit
     * @param time the Reference Time of the GPS orbit (s)
     * @param sqrtSma the Square Root of the Semi-Major Axis (m^1/2)
     * @param deltaN the Mean Motion Difference from Computed Value (rad)
     * @param ecc the Eccentricity
     * @param i0 the Inclination Angle at Reference Time (rad)
     * @param iDot the Rate of Inclination Angle (rad/s)
     * @param omega0 the Longitude of Ascending Node of Orbit Plane at Weekly Epoch (rad)
     * @param omegaDot the Rate of Right Ascension (rad/s)
     * @param pa the Argument of Perigee (rad)
     * @param m0 the Mean Anomaly at Reference Time (rad)
     * @param cuc the Amplitude of the Cosine Harmonic Correction Term to the Argument of Latitude (rad)
     * @param cus the Amplitude of the Sine Harmonic Correction Term to the Argument of Latitude (rad)
     * @param crc the Amplitude of the Cosine Harmonic Correction Term to the Orbit Radius (m)
     * @param crs the Amplitude of the Sine Harmonic Correction Term to the Orbit Radius (m)
     * @param cic the Amplitude of the Cosine Harmonic Correction Term to the Angle of Inclination (rad)
     * @param cis the Amplitude of the Sine Harmonic Correction Term to the Angle of Inclination (rad)
     */
    public GPSOrbitalElements(final int prn, final int week, final double time, final double sqrtSma,
                    final double deltaN, final double ecc, final double i0, final double iDot,
                    final double omega0, final double omegaDot, final double pa, final double m0,
                    final double cuc, final double cus, final double crc, final double crs,
                    final double cic, final double cis) {
        this.prn      = prn;
        this.week     = week % GPS_WEEK_NB;
        this.time     = time;
        this.tRef     = AbsoluteDate.createGPSDate(week, time * 1000.);
        this.sma      = sqrtSma * sqrtSma;
        this.mmo      = getMeanMotion(deltaN);
        this.ecc      = ecc;
        this.i0       = i0;
        this.iDot     = iDot;
        this.omega0   = omega0;
        this.omegaDot = omegaDot;
        this.pa       = pa;
        this.m0       = m0;
        this.cuc      = cuc;
        this.cus      = cus;
        this.crc      = crc;
        this.crs      = crs;
        this.cic      = cic;
        this.cis      = cis;
    }

    /**
     * Gets the mean motion from deltaN and sma.
     *
     * @param deltaN the Mean Motion Difference From Computed Value (rad/s)
     * @return the mean motion (rad/s)
     */
    private double getMeanMotion(final double deltaN) {
        return FastMath.sqrt(GPS_MU / FastMath.pow(sma, 3)) + deltaN;
    }

	/**
	 * Gets the PRN number of the GPS satellite.
	 *
     * @return the PRN number of the GPS satellite
     */
    public int getPrn() {
    	return prn;
    }

    /**
     * Gets the Reference Week of the GPS orbit.
     *
     * @return the Reference Week of the GPS orbit within [0, 1024[
     */
    public int getWeek() {
    	return week;
    }

    /**
     * Gets the Reference Time of the GPS orbit as a duration from week start.
     *
     * @return the Reference Time of the GPS orbit (s)
     */
    public double getTime() {
        return time;
    }

    /**
     * Gets the Semi-Major Axis.
     *
     * @return the Semi-Major Axis (m)
     */
    public double getSma() {
    	return sma;
    }

    /**
     * Gets the Mean Motion.
     *
     * @return the Mean Motion (rad/s)
     */
    public double getMeanMotion() {
    	return mmo;
    }

    /**
     * Gets the Eccentricity.
     *
     * @return the Eccentricity
     */
    public double getE() {
    	return ecc;
    }

    /**
     * Gets the Inclination Angle at Reference Time.
     *
     * @return the Inclination Angle at Reference Time (rad)
     */
    public double getI0() {
    	return i0;
    }

    /**
     * Gets the Rate of Inclination Angle.
     *
     * @return the Rate of Inclination Angle (rad/s)
     */
    public double getIDot() {
    	return iDot;
    }

    /**
     * Gets the Longitude of Ascending Node of Orbit Plane at Weekly Epoch.
     *
     * @return the Longitude of Ascending Node of Orbit Plane at Weekly Epoch (rad)
     */
    public double getOmega0() {
    	return omega0;
    }

    /**
     * Gets the Rate of Right Ascension.
     *
     * @return the Rate of Right Ascension (rad/s)
     */
    public double getOmegaDot() {
    	return omegaDot;
    }

    /**
     * Gets the Argument of Perigee.
     *
     * @return the Argument of Perigee (rad)
     */
    public double getPa() {
    	return pa;
    }

    /**
     * Gets the Mean Anomaly at Reference Time.
     *
     * @return the Mean Anomaly at Reference Time (rad)
     */
    public double getM0() {
    	return m0;
    }

    /**
     * Gets the Amplitude of the Cosine Harmonic Correction Term to the Argument of Latitude.
     *
     * @return the Amplitude of the Cosine Harmonic Correction Term to the Argument of Latitude (rad)
     */
    public double getCuc() {
    	return cuc;
    }

    /**
     * Gets the Amplitude of the Sine Harmonic Correction Term to the Argument of Latitude.
     *
     * @return the Amplitude of the Sine Harmonic Correction Term to the Argument of Latitude (rad)
     */
    public double getCus() {
    	return cus;
    }

    /**
     * Gets the Amplitude of the Cosine Harmonic Correction Term to the Orbit Radius.
     *
	 * @return the Amplitude of the Cosine Harmonic Correction Term to the Orbit Radius (m)
	 */
	public double getCrc() {
		return crc;
	}

    /**
     * Gets the Amplitude of the Sine Harmonic Correction Term to the Orbit Radius.
     *
     * @return the Amplitude of the Sine Harmonic Correction Term to the Orbit Radius (m)
     */
	public double getCrs() {
		return crs;
	}

	/**
	 * Gets the Amplitude of the Cosine Harmonic Correction Term to the Angle of Inclination.
	 *
     * @return the Amplitude of the Cosine Harmonic Correction Term to the Angle of Inclination (rad)
     */
    public double getCic() {
    	return cic;
    }

    /**
     * Gets the Amplitude of the Sine Harmonic Correction Term to the Angle of Inclination.
     *
     * @return the Amplitude of the Sine Harmonic Correction Term to the Angle of Inclination (rad)
     */
    public double getCis() {
    	return cis;
    }

    @Override
    public AbsoluteDate getDate() {
        return tRef;
    }	
}
		