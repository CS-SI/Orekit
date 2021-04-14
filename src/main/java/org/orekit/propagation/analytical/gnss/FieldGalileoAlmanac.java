/* Copyright 2002-2021 CS GROUP
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
package org.orekit.propagation.analytical.gnss;

import org.hipparchus.Field;
import org.hipparchus.RealFieldElement;
import org.hipparchus.util.FastMath;
import org.orekit.annotation.DefaultDataContext;
import org.orekit.data.DataContext;
import org.orekit.gnss.GalileoAlmanac;
import org.orekit.gnss.SatelliteSystem;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;

/**
 * Class for Galileo almanac.
 *
 * @see "European GNSS (Galileo) Open Service, Signal In Space, Interface
 *      Control Document, Table 75"
 *
 * @author Bryan Cazabonne
 * @author Nicolas Fialton (field translation)
 *
 */
public class FieldGalileoAlmanac<T extends RealFieldElement<T>> implements FieldGalileoOrbitalElements<T> {

	private final T zero;
	
	// Nominal parameters
	/** Nominal inclination (Ref: Galileo ICD - Table 75). */
	private final static double I0 = FastMath.toRadians(56.0);

	/** Nominal semi-major axis in meters (Ref: Galileo ICD - Table 75). */
	private final static double A0 = 29600000;

	/** PRN number. */
	private final int prn;

	/** Satellite E5a signal health status. */
	private final int healthE5a;

	/** Satellite E5b signal health status. */
	private final int healthE5b;

	/** Satellite E1-B/C signal health status. */
	private final int healthE1;

	/** Galileo week. */
	private final int week;

	/** Time of applicability. */
	private final T toa;

	/** Semi-major axis. */
	private final T sma;

	/** Eccentricity. */
	private final T ecc;

	/** Inclination. */
	private final T inc;

	/** Longitude of Orbital Plane. */
	private final T om0;

	/** Rate of Right Ascension. */
	private final T dom;

	/** Argument of perigee. */
	private final T aop;

	/** Mean anomaly. */
	private final T anom;

	/** Zeroth order clock correction. */
	private final T af0;

	/** First order clock correction. */
	private final T af1;

	/** Almanac Issue Of Data. */
	private final int iod;

	private final FieldAbsoluteDate<T> date;

	
	/**
     * Build a new almanac.
     *
     * <p>This method uses the {@link DataContext#getDefault() default data context}.
     *
     * @param prn the PRN number
     * @param week the Galileo week
     * @param toa the Almanac Time of Applicability (s)
     * @param dsqa difference between the square root of the semi-major axis
     *        and the square root of the nominal semi-major axis
     * @param ecc the eccentricity
     * @param dinc the correction of orbit reference inclination at reference time (rad)
     * @param iod the issue of data
     * @param om0 the geographic longitude of the orbital plane at the weekly epoch (rad)
     * @param dom the Rate of Right Ascension (rad/s)
     * @param aop the Argument of Perigee (rad)
     * @param anom the Mean Anomaly (rad)
     * @param af0 the Zeroth Order Clock Correction (s)
     * @param af1 the First Order Clock Correction (s/s)
     * @param healthE5a the E5a signal health status
     * @param healthE5b the E5b signal health status
     * @param healthE1 the E1-B/C signal health status
     * @see #GalileoAlmanac(int, int, T, T, T, T, int, T, T,
     * T, T, T, T, int, int, int, AbsoluteDate)
     */
    @DefaultDataContext
    public FieldGalileoAlmanac(final int prn, final int week, final T toa,
                          final T dsqa, final T ecc, final T dinc,
                          final int iod, final T om0, final T dom,
                          final T aop, final T anom, final T af0,
                          final T af1, final int healthE5a, final int healthE5b,
                          final int healthE1) {
        this(prn, week, toa, dsqa, ecc, dinc, iod, om0, dom, aop, anom, af0, af1,
                healthE5a, healthE5b, healthE1,
                new FieldGNSSDate<>(week, toa.multiply(1000), SatelliteSystem.GALILEO,
                        DataContext.getDefault().getTimeScales()).getDate());
    }

	/**
     * Build a new almanac.
     *
     * @param prn the PRN number
     * @param week the Galileo week
     * @param toa the Almanac Time of Applicability (s)
     * @param dsqa difference between the square root of the semi-major axis
     *        and the square root of the nominal semi-major axis
     * @param ecc the eccentricity
     * @param dinc the correction of orbit reference inclination at reference time (rad)
     * @param iod the issue of data
     * @param om0 the geographic longitude of the orbital plane at the weekly epoch (rad)
     * @param dom the Rate of Right Ascension (rad/s)
     * @param aop the Argument of Perigee (rad)
     * @param anom the Mean Anomaly (rad)
     * @param af0 the Zeroth Order Clock Correction (s)
     * @param af1 the First Order Clock Correction (s/s)
     * @param healthE5a the E5a signal health status
     * @param healthE5b the E5b signal health status
     * @param healthE1 the E1-B/C signal health status
     * @param date corresponding to {@code week} and {@code toa}.
     * @since 10.1
     */
    public FieldGalileoAlmanac(final int prn, final int week, final T toa,
                          final T dsqa, final T ecc, final T dinc,
                          final int iod, final T om0, final T dom,
                          final T aop, final T anom, final T af0,
                          final T af1, final int healthE5a, final int healthE5b,
                          final int healthE1, final FieldAbsoluteDate<T> date) {
    	this.zero = toa.getField().getZero();
        this.prn = prn;
        this.week = week;
        this.toa = toa;
        this.ecc = ecc;
        this.inc = dinc.add(I0);
        this.iod = iod;
        this.om0 = om0;
        this.dom = dom;
        this.aop = aop;
        this.anom = anom;
        this.af0 = af0;
        this.af1 = af1;
        this.healthE1 = healthE1;
        this.healthE5a = healthE5a;
        this.healthE5b = healthE5b;
        this.date = date;

        // semi-major axis computation
        final T sqa = dsqa.add(FastMath.sqrt(A0));
        this.sma = sqa.multiply(sqa);
    }
    
    /**
	 * Constructor
	 * 
	 * This constructor converts a GalileoAlmanac into a FieldGalileoAlmanac
	 * 
	 * @param field
	 * @param almanac a GalileoAlmanac
	 */
	public FieldGalileoAlmanac(Field<T> field, GalileoAlmanac almanac) {
		this.zero = field.getZero();
		this.prn = almanac.getPRN();
		this.week = almanac.getWeek();
		this.toa = zero.add(almanac.getTime());
		this.ecc = zero.add(almanac.getE());
		this.inc = zero.add(almanac.getI0());
		this.iod = almanac.getIOD();
		this.om0 = zero.add(almanac.getOmega0());
		this.dom = zero.add(almanac.getOmegaDot());
		this.aop = zero.add(almanac.getPa());
		this.anom = zero.add(almanac.getM0());
		this.af0 = zero.add(almanac.getAf0());
		this.af1 = zero.add(almanac.getAf1());
		this.healthE1 = almanac.getHealthE1();
		this.healthE5a = almanac.getHealthE5a();
        this.healthE5b = almanac.getHealthE5b();
		this.date = new FieldAbsoluteDate<>(field, almanac.getDate());
		this.sma = zero.add(almanac.getSma());
	}

	@Override
	public FieldAbsoluteDate<T> getDate() {
		return date;
	}

	@Override
	public int getPRN() {
		return prn;
	}

	@Override
	public int getWeek() {
		return week;
	}

	@Override
	public T getTime() {
		return toa;
	}

	@Override
	public T getSma() {
		return sma;
	}

	@Override
	public T getMeanMotion() {
		final T absA = FastMath.abs(sma);
		return FastMath.sqrt(zero.add(GALILEO_MU).divide(absA)).divide(absA);
	}

	@Override
	public T getE() {
		return ecc;
	}

	@Override
	public T getI0() {
		return inc;
	}

	@Override
	public T getIDot() {
		return zero;
	}

	@Override
	public T getOmega0() {
		return om0;
	}

	@Override
	public T getOmegaDot() {
		return dom;
	}

	@Override
	public T getPa() {
		return aop;
	}

	@Override
	public T getM0() {
		return anom;
	}

	@Override
	public T getCuc() {
		return zero;
	}

	@Override
	public T getCus() {
		return zero;
	}

	@Override
	public T getCrc() {
		return zero;
	}

	@Override
	public T getCrs() {
		return zero;
	}

	@Override
	public T getCic() {
		return zero;
	}

	@Override
	public T getCis() {
		return zero;
	}

	@Override
	public T getAf0() {
		return af0;
	}

	@Override
	public T getAf1() {
		return af1;
	}

	/**
	 * Get the Issue of Data (IOD).
	 * 
	 * @return the Issue Of Data
	 */
	public int getIOD() {
		return iod;
	}

	/**
	 * Gets the E1-B/C signal health status.
	 *
	 * @return the E1-B/C signal health status
	 */
	public int getHealthE1() {
		return healthE1;
	}

	/**
	 * Gets the E5a signal health status.
	 *
	 * @return the E5a signal health status
	 */
	public int getHealthE5a() {
		return healthE5a;
	}

	/**
	 * Gets the E5b signal health status.
	 *
	 * @return the E5b signal health status
	 */
	public int getHealthE5b() {
		return healthE5b;
	}

	@Override
	public T getAf2() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public T getToc() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getIODNav() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public T getBGD() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public T getBGDE1E5a() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public T getBGDE5bE1() {
		// TODO Auto-generated method stub
		return null;
	}

}
