package fr.cs.aerospace.orekit;

import java.io.File;
import java.io.FileNotFoundException;
import java.text.ParseException;

import fr.cs.aerospace.orekit.errors.OrekitException;
import fr.cs.aerospace.orekit.frames.PoleCorrection;
import fr.cs.aerospace.orekit.time.AbsoluteDate;
import fr.cs.aerospace.orekit.time.TTScale;
import fr.cs.aerospace.orekit.time.TimeScale;
import fr.cs.aerospace.orekit.time.UTCScale;

import junit.framework.TestCase;

public class IERSDataTest extends TestCase {

	public void testExeptions() {
		
		File directory;
		IERSData iers;
		
		try {
		      directory = FindFile.find("/tests-src/fr/cs/aerospace/orekit/fichiers-eopc04/fakes/exeptiontester", "/");
		    } catch (FileNotFoundException fnfe) {
		      throw new RuntimeException(fnfe);
		    }
	
			try {
				iers = new IERSData(directory);
				fail("an exeption should have been thrown");
			} catch (OrekitException e) {
				// exepted behaviour
			}	 
		
			try {
			      directory = FindFile.find("/tests-src/fr/cs/aerospace/orekit/fichiers-eopc04/fakes", "/");
			    } catch (FileNotFoundException fnfe) {
			      throw new RuntimeException(fnfe);
			    }
		
				try {
					iers = new IERSData(directory);
					fail("an exeption should have been thrown");
				} catch (OrekitException e) {
					// exepted behaviour
				}	 
				try {
				      directory = FindFile.find("/tests-src/fr/cs/aerospace/orekit/fichiers-eopc04/fakes", "/");
				    } catch (FileNotFoundException fnfe) {
				      throw new RuntimeException(fnfe);
				    }
			
					try {
						iers = new IERSData(directory);
						fail("an exeption should have been thrown");
					} catch (OrekitException e) {
						// exepted behaviour
					}	
	}
	
	public void testIERSDatas() throws ParseException, OrekitException {
		File directory;
		IERSData iers;
		TimeScale scale = UTCScale.getInstance();
		
		AbsoluteDate date = new AbsoluteDate("2002-01-01T00:00:01", scale);
		// TODO verifier les calculs		
		try {
		      directory = FindFile.find("/tests-src/fr/cs/aerospace/orekit/fichiers-eopc04", "/");
		    } catch (FileNotFoundException fnfe) {
		      throw new RuntimeException(fnfe);
		    }
	
			iers = new IERSData(directory);
	 
		assertEquals(-0.1158223 , iers.getUT1MinusUTC(date), 10e-8);
			
			
		PoleCorrection pole = iers.getPoleCorrection(date);	
		
		double x = pole.xp;
		double y = pole.yp;
		
		
		assertEquals(-0.176980 , x, 10e-8);
		assertEquals(0.293952 , y, 10e-8);
	}

	/*public void testGetUT1MinusUTC() {
		fail("Not yet implemented");
	}

	public void testGetPoleCorrection() {
		fail("Not yet implemented");
	}*/

}
