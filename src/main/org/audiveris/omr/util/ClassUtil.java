//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        C l a s s U t i l                                       //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2019. All rights reserved.
//
//  This program is free software: you can redistribute it and/or modify it under the terms of the
//  GNU Affero General Public License as published by the Free Software Foundation, either version
//  3 of the License, or (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
//  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
//  See the GNU Affero General Public License for more details.
//
//  You should have received a copy of the GNU Affero General Public License along with this
//  program.  If not, see <http://www.gnu.org/licenses/>.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package org.audiveris.omr.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * Class {@code ClassUtil} provides utilities related to Class handling.
 *
 * @author Hervé Bitteur
 */
public abstract class ClassUtil
{

    private static final Logger logger = LoggerFactory.getLogger(ClassUtil.class);

    private ClassUtil ()
    {
    }

    //-----------------//
    // getCallingFrame //
    //-----------------//
    /**
     * Infer the calling frame, skipping the given classes if so provided.
     * Code was derived from a private method found in the JDK Logger class
     *
     * @param skippedClasses the classes to skip
     * @return the frame found, just before the skipped classes (or just before the caller of this
     *         method)
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
            String cname = frame.getClassName();

            for (Class<?> skipped : skippedClasses) {
                if (cname.equals(skipped.getName())) {
                    break searchingForSkipped;
                }
            }
        }

        // Now search for the first frame before the skipped classes
        searchingForNonSkipped:
        for (; ix < stack.length; ix++) {
            StackTraceElement frame = stack[ix];
            String cname = frame.getClassName();

            for (Class<?> skipped : skippedClasses) {
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
    // getCallingFrame //
    //-----------------//
    /**
     * Infer the calling frame, skipping the given classes if so provided.
     * Code was derived from a private method found in the JDK Logger class
     *
     * @param skipped predicate to skip class(es)
     * @return the frame found, just before the skipped classes (or just before the caller of this
     *         method)
     */
    public static StackTraceElement getCallingFrame (Predicate<String> skipped)
    {
        // Get the current stack trace.
        StackTraceElement[] stack = (new Throwable()).getStackTrace();

        // Simple case, no classes to skip, just return the caller of the caller
        if (skipped == null) {
            return stack[2];
        } else {
            // More complex case, skip the unwanted classes
            int ix;
            searchingForSkipped:
            for (ix = 2; ix < stack.length; ix++) {
                StackTraceElement frame = stack[ix];
                String cname = frame.getClassName();

                if (!skipped.check(cname)) {
                    return frame;
                }
            }

            // We haven't found a suitable frame
            return null;
        }
    }

    //------//
    // load //
    //------//
    /**
     * Try to load a (library) file.
     *
     * @param file    the file to load, which must point to the precise location
     * @param verbose true for verbose output
     * @return true if succeeded, false otherwise (no exception is thrown)
     */
    public static boolean load (File file,
                                boolean verbose)
    {
        String path = file.getAbsolutePath();

        if (verbose) {
            logger.info("Loading file {} ...", path);
        }

        try {
            System.load(path);

            if (verbose) {
                logger.info("Loaded  file {}", path);
            }

            return true;
        } catch (Throwable ex) {
            logger.warn("Error while loading file " + path, ex);

            return false;
        }
    }

    //-------------//
    // loadLibrary //
    //-------------//
    /**
     * Try to load a library.
     *
     * @param library the library to load (without ".dll" suffix for Windows, without "lib" prefix
     *                and ".so" suffix for Linux
     * @param verbose true for verbose output
     * @return true if succeeded, false otherwise (no exception is thrown)
     */
    public static boolean loadLibrary (String library,
                                       boolean verbose)
    {
        if (verbose) {
            logger.info("loadLibrary for {} ...", library);
        }

        try {
            System.loadLibrary(library);

            if (verbose) {
                logger.info("Loaded  library {}", library);
            }

            return true;
        } catch (Throwable ex) {
            logger.warn("Error while loading library " + library, ex);

            return false;
        }
    }

    //--------//
    // nameOf //
    //--------//
    /**
     * Report the full name of the object class, without the package information.
     *
     * @param obj the object to name
     * @return the concatenation of (enclosing) simple names
     */
    public static String nameOf (Object obj)
    {
        StringBuilder sb = new StringBuilder();

        for (Class<?> cl = obj.getClass(); cl != null; cl = cl.getEnclosingClass()) {
            if (sb.length() > 0) {
                sb.insert(0, "-");
            }

            sb.insert(0, cl.getSimpleName());
        }

        return sb.toString();
    }
}
