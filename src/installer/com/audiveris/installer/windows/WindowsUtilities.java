//----------------------------------------------------------------------------//
//                                                                            //
//                       W i n d o w s U t i l i t i e s                      //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Herve Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package com.audiveris.installer.windows;

import com.audiveris.installer.Utilities;
import com.sun.jna.Native;
import com.sun.jna.Pointer;

import hudson.util.jna.Kernel32;
import hudson.util.jna.Kernel32Utils;
import hudson.util.jna.SHELLEXECUTEINFO;
import hudson.util.jna.Shell32;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Class {@code WindowsUtilities} gathers utilities that are relevant
 * for the Windows platform only.
 *
 * @author Hervé Bitteur
 */
public class WindowsUtilities
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(
            WindowsUtilities.class);

    //~ Methods ----------------------------------------------------------------
    //----------------//
    // getCommandLine //
    //----------------//
    public static String getCommandLine ()
    {
        return Kernel32.INSTANCE.GetCommandLineW()
                .toString();
    }

    //-------------------//
    // getModuleFilename //
    //-------------------//
    public static String getModuleFilename ()
    {
        final int MAX_SIZE = 512;
        byte[] exePathname = new byte[MAX_SIZE];
        Pointer zero = new Pointer(0);
        int result = Kernel32.INSTANCE.GetModuleFileNameA(
                zero,
                exePathname,
                MAX_SIZE);

        return Native.toString(exePathname);
    }

    //-------------//
    // runElevated //
    //-------------//
    public static int runElevated (File exec,
                                   File pwd,
                                   String... args)
            throws IOException, InterruptedException
    {
        // Concatenate arguments into one string
        StringBuilder sb = new StringBuilder();

        for (String arg : args) {
            sb.append(arg)
                    .append(" ");
        }

        logger.debug("exec:   {}", exec);
        logger.debug("pwd:    {}", pwd);
        logger.debug("params: {}", sb);
        logger.debug("Calling ShellExecuteEx...");

        // error code 740 is ERROR_ELEVATION_REQUIRED, indicating that
        // we run in UAC-enabled Windows and we need to run this in an elevated privilege
        SHELLEXECUTEINFO sei = new SHELLEXECUTEINFO();
        sei.fMask = SHELLEXECUTEINFO.SEE_MASK_NOCLOSEPROCESS;
        sei.lpVerb = "runas";
        sei.lpFile = exec.getAbsolutePath();
        sei.lpParameters = sb.toString();
        sei.lpDirectory = pwd.getAbsolutePath();
        sei.nShow = SHELLEXECUTEINFO.SW_HIDE;

        if (!Shell32.INSTANCE.ShellExecuteEx(sei)) {
            throw new IOException(
                    "Failed to shellExecute: " + Native.getLastError());
        }

        try {
            int result = Kernel32Utils.waitForExitProcess(sei.hProcess);
            logger.debug("result: {}", result);

            return result;
        } finally {
            // TODO: need to print content of stdout/stderr
            //            FileInputStream fin = new FileInputStream(
            //                new File(pwd, "redirect.log"));
            //            IOUtils.copy(fin, out.getLogger());
            //            fin.close();
        }
    }

    //---------------//
    // queryRegistry //
    //---------------//
    public static int queryRegistry (List<String> output,
                                     String... args)
            throws IOException, InterruptedException
    {
        // Command arguments
        List<String> cmdArgs = new ArrayList<>();
        cmdArgs.addAll(Arrays.asList("/c", "reg", "query"));
        cmdArgs.addAll(Arrays.asList(args));
        logger.debug("cmd query: {}", cmdArgs);

        return Utilities.runProcess("cmd.exe", output,
                cmdArgs.toArray(new String[cmdArgs.size()]));
    }

    //-------------//
    // setRegistry // (not used actually)
    //-------------//
    public static int setRegistry (List<String> output,
                                   String... args)
            throws IOException, InterruptedException
    {
        // Command arguments
        List<String> cmdArgs = new ArrayList<>();
        cmdArgs.addAll(Arrays.asList("/c", "reg", "add"));
        cmdArgs.addAll(Arrays.asList(args));
        logger.debug("cmd add: {}", cmdArgs);

        return Utilities.runProcess("cmd.exe", output,
                cmdArgs.toArray(new String[cmdArgs.size()]));
    }
}
