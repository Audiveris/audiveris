//----------------------------------------------------------------------------//
//                                                                            //
//                          U n i x D e s c r i p t o r                       //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Herve Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package com.audiveris.installer.unix;

import com.audiveris.installer.Descriptor;
import com.audiveris.installer.Installer;
import com.audiveris.installer.Utilities;
import static com.audiveris.installer.unix.UnixUtilities.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JOptionPane;

/**
 * Class {@code UnixDescriptor} implements Installer descriptor for
 * Linux Ubuntu (32 and 64 bits)
 *
 * @author Hervé Bitteur
 */
public class UnixDescriptor
        implements Descriptor
{
    //~ Static fields/initializers ---------------------------------------------

    /**
     * Usual logger utility
     */
    private static final Logger logger = LoggerFactory.getLogger(
            UnixDescriptor.class);

    /**
     * Specific prefix for application folders. {@value}
     */
    private static final String TOOL_PREFIX = "/" + COMPANY_ID + "/"
                                              + TOOL_NAME;

    /**
     * Set of requirements for c/c++.
     */
    private static final Package[] cReqs = new Package[]{
        new Package("libc6", "2.15"),
        new Package("libgcc1", "4.6.3"),
        new Package(
        "libstdc++6",
        "4.6.3")
    };

    /**
     * Requirement for ghostscript.
     */
    private static final Package gsReq = new Package(
            "ghostscript",
            "9.06~dfsg");

    /**
     * Requirement for tesseract.
     */
    private static final Package tessReq = new Package("libtesseract3", "3.02");

    //~ Methods ----------------------------------------------------------------
    //-----------------//
    // getConfigFolder //
    //-----------------//
    @Override
    public File getConfigFolder ()
    {
        String config = System.getenv("XDG_CONFIG_HOME");

        if (config != null) {
            return new File(config + TOOL_PREFIX);
        }

        String home = System.getenv("HOME");

        if (home != null) {
            return new File(home + "/.config" + TOOL_PREFIX);
        }

        throw new RuntimeException("HOME environment variable is not set");
    }

    //---------------//
    // getDataFolder //
    //---------------//
    @Override
    public File getDataFolder ()
    {
        String data = System.getenv("XDG_DATA_HOME");

        if (data != null) {
            return new File(data + TOOL_PREFIX);
        }

        String home = System.getenv("HOME");

        if (home != null) {
            return new File(home + "/.local/share" + TOOL_PREFIX);
        }

        throw new RuntimeException("HOME environment variable is not set");
    }

    //--------------------------//
    // getDefaultTessdataPrefix //
    //--------------------------//
    @Override
    public File getDefaultTessdataPrefix ()
    {
        return new File("/usr/share/tesseract-ocr/");
    }

    //---------------//
    // getTempFolder //
    //---------------//
    @Override
    public File getTempFolder ()
    {
        final File folder = new File(getDataFolder(), "temp/installation");
        logger.debug("getTempFolder: {}", folder.getAbsolutePath());

        return folder;
    }

    //------------//
    // installCpp //
    //------------//
    @Override
    public void installCpp ()
            throws Exception
    {
        for (Package pkg : cReqs) {
            if (!pkg.isInstalled()) {
                pkg.install();
            }
        }
    }

    //--------------------//
    // installGhostscript //
    //--------------------//
    @Override
    public void installGhostscript ()
            throws Exception
    {
        gsReq.install();
    }

    //------------------//
    // installTesseract //
    //------------------//
    @Override
    public void installTesseract ()
            throws Exception
    {
        tessReq.install();
    }

    //---------//
    // isAdmin //
    //---------//
    @Override
    public boolean isAdmin ()
    {
        //      whoami -> herve
        // sudo whoami -> root
        try {
            List<String> output = new ArrayList<String>();
            int res = Utilities.runProcess(
                    "bash",
                    output,
                    "-c",
                    "whoami");

            if (res != 0) {
                logger.warn(Utilities.dumpOfLines(output));
                throw new RuntimeException(
                        "Error checking admin, exit: " + res);
            } else {
                return !output.isEmpty() && output.get(0)
                        .equals("root");
            }
        } catch (Exception ex) {
            logger.warn("Error checking admin", ex);

            return true; // Safer
        }
    }

    //----------------//
    // isCppInstalled //
    //----------------//
    @Override
    public boolean isCppInstalled ()
    {
        for (Package pkg : cReqs) {
            if (!pkg.isInstalled()) {
                return false;
            }
        }

        return true;
    }

    //------------------------//
    // isGhostscriptInstalled //
    //------------------------//
    @Override
    public boolean isGhostscriptInstalled ()
    {
        return gsReq.isInstalled();
    }

    //----------------------//
    // isTesseractInstalled //
    //----------------------//
    @Override
    public boolean isTesseractInstalled ()
    {
        return tessReq.isInstalled();
    }

    //-----------------//
    // relaunchAsAdmin //
    //-----------------//
    @Override
    public void relaunchAsAdmin ()
            throws Exception
    {
        // My command line
        String cmdLine = getCommandLine();

        // Relaunch as root
        List<String> output = new ArrayList<String>();
        int res = Utilities.runProcess(
                "bash",
                output,
                "-c",
                "gksudo \"" + cmdLine.replace('"', '\'') + "\"");

        if (res != 0) {
            logger.warn(Utilities.dumpOfLines(output));
            throw new RuntimeException("Failure, exit: " + res);
        }
    }

    //--------//
    // setenv //
    //--------//
    @Override
    public void setenv (boolean system,
                        String var,
                        String value)
            throws Exception
    {
        logger.trace("setenv() not really needed on Unix");
    }
}
