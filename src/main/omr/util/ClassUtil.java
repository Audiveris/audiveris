//----------------------------------------------------------------------------//
//                                                                            //
//                             C l a s s U t i l                              //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2009. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.util;

import omr.Main;

import omr.log.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

/**
 * Class <code>ClassUtil</code> provides utilities related to Class handling.
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public class ClassUtil
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(ClassUtil.class);

    //~ Methods ----------------------------------------------------------------

    //-----------------//
    // getCallingFrame //
    //-----------------//
    /**
     * Infer the calling frame, skipping the given classes if so provided.
     * Code was derived from a private method found in the JDK Logger class
     *
     * @param skippedClasses the classes to skip
     * @return the frame found, just before the skipped classes (or just before
     * the caller of this method)
     */
    public static StackTraceElement getCallingFrame (Class... skippedClasses)
    {
        // Get the current stack trace.
        StackTraceElement[] stack = (new Throwable()).getStackTrace();

        // Simple case, no classes to skip, just return the caller of the caller
        if (skippedClasses.length == 0) {
            return stack[2];
        }

        // More complex case, return the caller, just before the skipped classes
        // First, search back to a method in the skipped classes, if any
        int ix;
        searchingForSkipped: 
        for (ix = 0; ix < stack.length; ix++) {
            StackTraceElement frame = stack[ix];
            String            cname = frame.getClassName();

            for (Class skipped : skippedClasses) {
                if (cname.equals(skipped.getName())) {
                    break searchingForSkipped;
                }
            }
        }

        // Now search for the first frame before the skipped classes
        searchingForNonSkipped: 
        for (; ix < stack.length; ix++) {
            StackTraceElement frame = stack[ix];
            String            cname = frame.getClassName();

            for (Class skipped : skippedClasses) {
                if (cname.equals(skipped.getName())) {
                    continue searchingForNonSkipped;
                }
            }

            // We've found the relevant frame.
            return frame;
        }

        // We haven't found a suitable frame
        return null;
    }

    //-----------------//
    // getProperStream //
    //-----------------//
    /**
     * Retrieve the proper input stream for desired information, by looking first
     * in the local folder if any, then in the related resource
     * @param folder the containing local folder
     * @param fileName just the name.ext info
     * @return the proper input stream, or null if failed
     */
    public static InputStream getProperStream (File   folder,
                                               String fileName)
    {
        InputStream input = null;

        // Look for a local file first
        File file = new File(folder, fileName);

        if (file.exists()) {
            try {
                input = new FileInputStream(file);
            } catch (FileNotFoundException ex) {
                logger.warning("Cannot find config file " + file, ex);
            }
        }

        String resName = null;

        if (input == null) {
            // Then look for a resource
            resName = "/" + folder.getName() + "/" + fileName;
            input = Main.class.getResourceAsStream(resName);
        }

        if (input == null) {
            logger.warning("Cannot find config resource " + resName);
        }

        return input;
    }

    //------//
    // load //
    //------//
    /**
     * Try to load a (library) file
     * @param file the file to load, which must point to the precise location
     */
    public static void load (File file)
    {
        String path = file.getAbsolutePath();

        try {
            System.load(path);
        } catch (Throwable ex) {
            logger.warning("Error while loading " + path, ex);
            throw new RuntimeException(ex);
        }
    }

    //--------//
    // nameOf //
    //--------//
    /**
     * Report the full name of the object class, without the package information
     *
     * @param obj the object to name
     * @return the concatenation of (enclosing) simple names
     */
    public static String nameOf (Object obj)
    {
        StringBuilder sb = new StringBuilder();

        for (Class cl = obj.getClass(); cl != null;
             cl = cl.getEnclosingClass()) {
            if (sb.length() > 0) {
                sb.insert(0, "-");
            }

            sb.insert(0, cl.getSimpleName());
        }

        return sb.toString();
    }
}
