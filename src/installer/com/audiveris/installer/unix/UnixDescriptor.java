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
import com.audiveris.installer.SpecificFile;
import com.audiveris.installer.Utilities;
import static com.audiveris.installer.unix.UnixUtilities.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

    //----------------//
    // getCopyCommand //
    //----------------//
    @Override
    public String getCopyCommand (String source,
                                  String target)
    {
        return "cp -r \"" + source + "\" \"" + target + "\"";
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
        return new File("/usr/share/" + Descriptor.TESSERACT_OCR + "/");
    }

    //------------------//
    // getDeleteCommand //
    //------------------//
    @Override
    public String getDeleteCommand (String file)
    {
        return "rm -f -v \"" + file + "\"";
    }

    //------------------//
    // getSpecificFiles //
    //------------------//
    @Override
    public List<SpecificFile> getSpecificFiles ()
    {
        return Arrays.asList(
                new SpecificFile("unix/audiveris.sh", "/usr/bin/audiveris", true),
                new SpecificFile(
                "unix/AddPlugins.sh",
                "/usr/share/audiveris/AddPlugins.sh",
                true));
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
                final String lines = Utilities.dumpOfLines(output);
                logger.warn(lines);
                throw new RuntimeException(
                        "Error checking admin, exit: " + res + "\n" + lines);
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

    //----------//
    // runShell //
    //----------//
    @Override
    public boolean runShell (boolean asAdmin,
                             List<String> commands)
            throws Exception
    {
        // Build a single compound command
        StringBuilder sb = new StringBuilder();

        for (String command : commands) {
            if (sb.length() > 0) {
                sb.append(" ; ");
            }

            sb.append(command);
        }

        String cmdLine = sb.toString();

        List<String> output = new ArrayList<String>();

        // If asAdmin, we try gksudo then kdesudo if needed
        String[] sudos = asAdmin ? new String[]{"kdesudo", "gksudo"}
                : new String[]{""};

        for (String sudo : sudos) {
            if (!sudo.isEmpty()) {
                output.add("Trying command: " + sudo);
            }

            int res = Utilities.runProcess(
                    "bash",
                    output,
                    "-c",
                    sudo + " \"" + cmdLine.replace('"', '\'') + "\"");

            if (res == 0) {
                return true; // Normal exit
            } else {
                output.add("Exit code = " + res);
            }
        }

        // If we get here, we totally failed
        final String lines = Utilities.dumpOfLines(output);
        logger.warn(lines);
        throw new RuntimeException("Failure in runShell().\n" + lines);
    }

    //---------------//
    // setExecutable //
    //---------------//
    @Override
    public void setExecutable (String file)
            throws Exception
    {
        List<String> output = new ArrayList<String>();
        int res = Utilities.runProcess(
                "bash",
                output,
                "-c",
                "\"chmod a:x '" + file + "'\"");

        if (res != 0) {
            final String lines = Utilities.dumpOfLines(output);
            logger.warn(lines);
            throw new RuntimeException("Failure in setExecutable().\n" + lines);
        }
    }
}
