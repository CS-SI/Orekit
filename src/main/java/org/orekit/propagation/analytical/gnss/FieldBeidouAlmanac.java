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
import org.orekit.gnss.BeidouAlmanac;
import org.orekit.gnss.SatelliteSystem;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;

/**
 * Class for Field BeiDou almanac.
 *
 * @see "BeiDou Navigation Satellite System, Signal In Space, Interface Control
 *      Document, Version 2.1, Table 5-12"
 *
 * @author Bryan Cazabonne
 * @author Nicolas Fialton (field translation)
 *
 */
public class FieldBeidouAlmanac<T extends RealFieldElement<T>> implements FieldBeidouOrbitalElements<T> {

	private final T zero;
	/** PRN number. */
	private final int prn;

	/** Health status. */
	private final int health;

	/** BeiDou week. */
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

	/** Date of validity. */
	private final FieldAbsoluteDate<T> date;

	/**
	 * Build a new almanac.
	 *
	 * <p>
	 * This method uses the {@link DataContext#getDefault() default data context}.
	 *
	 * @param prn    the PRN number
	 * @param week   the BeiDou week
	 * @param toa    the Almanac Time of Applicability (s)
	 * @param sqa    the Square Root of Semi-Major Axis (m^1/2)
	 * @param ecc    the eccentricity
	 * @param inc0   the orbit reference inclination 0.0 for GEO satellites and 0.30
	 *               * BEIDOU_PI for MEO/IGSO satellites (rad)
	 * @param dinc   the correction of orbit reference inclination at reference time
	 *               (rad)
	 * @param om0    the geographic longitude of the orbital plane at the weekly
	 *               epoch (rad)
	 * @param dom    the Rate of Right Ascension (rad/s)
	 * @param aop    the Argument of Perigee (rad)
	 * @param anom   the Mean Anomaly (rad)
	 * @param af0    the Zeroth Order Clock Correction (s)
	 * @param af1    the First Order Clock Correction (s/s)
	 * @param health the Health status
	 * @see #BeidouAlmanac(int, int, T, T, T, T, T, T, T, T, T, T, T, int,
	 *      AbsoluteDate)
	 */
	@DefaultDataContext
	public FieldBeidouAlmanac(final int prn, final int week, final T toa, final T sqa, final T ecc, final T inc0,
			final T dinc, final T om0, final T dom, final T aop, final T anom, final T af0, final T af1,
			final int health) {
		this(prn, week, toa, sqa, ecc, inc0, dinc, om0, dom, aop, anom, af0, af1, health,
				new FieldGNSSDate<T>(week, toa.multiply(1000), SatelliteSystem.BEIDOU, DataContext.getDefault().getTimeScales())
						.getDate());
	}

	/**
	 * Build a new almanac.
	 * 
	 * @param prn    the PRN number
	 * @param week   the BeiDou week
	 * @param toa    the Almanac Time of Applicability (s)
	 * @param sqa    the Square Root of Semi-Major Axis (m^1/2)
	 * @param ecc    the eccentricity
	 * @param inc0   the orbit reference inclination 0.0 for GEO satellites and 0.30
	 *               * BEIDOU_PI for MEO/IGSO satellites (rad)
	 * @param dinc   the correction of orbit reference inclination at reference time
	 *               (rad)
	 * @param om0    the geographic longitude of the orbital plane at the weekly
	 *               epoch (rad)
	 * @param dom    the Rate of Right Ascension (rad/s)
	 * @param aop    the Argument of Perigee (rad)
	 * @param anom   the Mean Anomaly (rad)
	 * @param af0    the Zeroth Order Clock Correction (s)
	 * @param af1    the First Order Clock Correction (s/s)
	 * @param health the Health status
	 * @param date   that corresponds to {@code week} and {@code toa}.
	 * @since 10.1
	 */
	public FieldBeidouAlmanac(final int prn, final int week, final T toa, final T sqa, final T ecc, final T inc0,
			final T dinc, final T om0, final T dom, final T aop, final T anom, final T af0, final T af1,
			final int health, final FieldAbsoluteDate<T> date) {
		this.zero = toa.getField().getZero();
		this.prn = prn;
		this.week = week;
		this.toa = toa;
		this.sma = sqa.multiply(sqa);
		this.ecc = ecc;
		this.inc = inc0.add(dinc);
		this.om0 = om0;
		this.dom = dom;
		this.aop = aop;
		this.anom = anom;
		this.af0 = af0;
		this.af1 = af1;
		this.health = health;
		this.date = date;
	}
	
	/**
	 * Constructor
	 * 
	 * This constructor converts a BeidouAlmanac into a FieldBeidouAlmanac
	 * 
	 * @param field
	 * @param almanac a BeidouAlmanac
	 */
	public FieldBeidouAlmanac(Field<T> field, BeidouAlmanac almanac) {
		this.zero = field.getZero();
		this.prn = almanac.getPRN();
		this.week = almanac.getWeek();
		this.toa = zero.add(almanac.getTime());
		this.sma = zero.add(almanac.getSma());
		this.ecc = zero.add(almanac.getE());
		this.inc = zero.add(almanac.getI0());
		this.om0 = zero.add(almanac.getOmega0());
		this.dom = zero.add(almanac.getOmegaDot());
		this.aop = zero.add(almanac.getPa());
		this.anom = zero.add(almanac.getM0());
		this.af0 = zero.add(almanac.getAf0());
		this.af1 = zero.add(almanac.getAf1());
		this.health = almanac.getHealth();
		this.date = new FieldAbsoluteDate<>(field, almanac.getDate());
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
		return FastMath.sqrt(date.getDate().getField().getZero().add(BEIDOU_MU).divide(absA)).divide(absA);
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
		return date.getDate().getField().getZero();
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
		return date.getDate().getField().getZero();
	}

	@Override
	public T getCus() {
		return date.getDate().getField().getZero();
	}

	@Override
	public T getCrc() {
		return date.getDate().getField().getZero();
	}

	@Override
	public T getCrs() {
		return date.getDate().getField().getZero();
	}

	@Override
	public T getCic() {
		return date.getDate().getField().getZero();
	}

	@Override
	public T getCis() {
		return date.getDate().getField().getZero();
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
	 * Gets the Health status.
	 *
	 * @return the Health status
	 */
	public int getHealth() {
		return health;
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
	public int getAODC() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getAODE() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getIOD() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public T getTGD1() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public T getTGD2() {
		// TODO Auto-generated method stub
		return null;
	}

}
