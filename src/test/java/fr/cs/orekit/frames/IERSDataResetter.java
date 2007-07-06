package fr.cs.orekit.frames;

import java.io.File;
import java.io.FileNotFoundException;
import java.lang.reflect.Field;
import java.security.AccessController;
import java.security.PrivilegedAction;
import fr.cs.orekit.FindFile;
import fr.cs.orekit.time.UTCScale;


public class IERSDataResetter {
  
  private static final File rootDir;
  static {
    try {
      rootDir = FindFile.find("/tests-src/fr/cs/orekit/data", "/");
    } catch (FileNotFoundException fnfe) {
      throw new RuntimeException("unexpected failure");
    }
  }
  
  public static void setUp(String directory) {
    System.setProperty("orekit.iers.directory",
                       new File(rootDir, directory).getAbsolutePath());
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
