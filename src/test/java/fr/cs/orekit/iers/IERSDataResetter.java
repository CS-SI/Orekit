package fr.cs.orekit.iers;

import java.lang.reflect.Field;
import java.security.AccessController;
import java.security.PrivilegedAction;

import fr.cs.orekit.frames.EarthOrientationHistory;
import fr.cs.orekit.time.UTCScale;


public class IERSDataResetter {

    public static void setUp(String directory) {
        System.setProperty("orekit.iers.directory", directory);
        AccessController.doPrivileged(new SingletonResetter());
    }

    public static void tearDown() {
        System.setProperty("orekit.iers.directory", "");
        AccessController.doPrivileged(new SingletonResetter());
    }

    private static class SingletonResetter implements PrivilegedAction {
        public Object run() {
            try {
                Field instance;
                instance = UTCScale.class.getDeclaredField("instance");
                instance.setAccessible(true);
                instance.set(null, null);
                instance.setAccessible(false);

                instance = EarthOrientationHistory.class.getDeclaredField("instance");
                instance.setAccessible(true);
                instance.set(null, null);
                instance.setAccessible(false);


            } catch (SecurityException e) {

            } catch (NoSuchFieldException e) {

            } catch (IllegalArgumentException e) {

            } catch (IllegalAccessException e) {

            }
            return null;
        }
    }
}
