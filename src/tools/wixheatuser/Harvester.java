//----------------------------------------------------------------------------//
//                                                                            //
//                             H a r v e s t e r                              //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package wixheatuser;

import java.io.*;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Class {@code Harvester} is a specific Harvester, derived from
 * WiX heat utility, meant for harvesting a directory targetted to
 * a user location, and therefore using registry keys rather than
 * files as KeyPath.
 *
 * @author Hervé Bitteur
 */
public class Harvester
{
    //~ Instance fields --------------------------------------------------------

    /**
     * The command line arguments
     * <exec executable="heat">
     *      <arg value="dir"/>
     *      <arg value="${train.dir}"/>
     *      <arg line="${heat.options}"/>
     *      <arg line="-dr adProductDirId"/>
     *      <arg line="-cg train.CID"/>
     *      <arg line="-var var.myTrain"/>
     *      <arg line="-out ${dev.dir}/wix/generated-train.wxs"/>
     * </exec>
     */
    private final CLI cli;

    /** Output PrintStream */
    private PrintStream ps;

    /** Current indentation level in the output */
    private int indent = 0;

    /** All the directories processed */
    private Set<DirId> dirs = new LinkedHashSet<DirId>();

    //~ Constructors -----------------------------------------------------------

    //-----------//
    // Harvester //
    //-----------//
    /**
     * Creates a new Harvester object.
     * @param cli the Command Line Interface parameters
     */
    private Harvester (CLI cli)
    {
        this.cli = cli;
    }

    //~ Methods ----------------------------------------------------------------

    //------//
    // main //
    //------//
    public static void main (String[] args)
        throws FileNotFoundException, UnsupportedEncodingException, IOException
    {
        // Retrieve user parameters
        Harvester instance = new Harvester(new CLI(args));

        // Launch processing
        instance.harvest();
    }

    //-----------------//
    // groupComponents //
    //-----------------//
    /**
     * Handle the components fragment.
     */
    private void groupComponents ()
    {
        pl("<Fragment>");
        indent++;
        pl("<ComponentGroup Id=\"" + cli.cg + "\">");
        indent++;

        for (DirId did : dirs) {
            pl("<ComponentRef Id=\"" + did + "\" />");
        }

        indent--;
        pl("</ComponentGroup>");
        indent--;
        pl("</Fragment>");
    }

    //--------------//
    // groupFolders //
    //--------------//
    /**
     * Handle the folders fragment.
     */
    private void groupFolders ()
    {
        pl("<Fragment>");
        indent++;
        pl("<DirectoryRef Id=\"" + cli.dr + "\">");
        indent++;
        harvestFolder(cli.dir);
        indent--;
        pl("</DirectoryRef>");
        indent--;
        pl("</Fragment>");
    }

    //---------//
    // harvest //
    //---------//
    /**
     * Process the whole folder hierarchy.
     * @throws FileNotFoundException
     * @throws UnsupportedEncodingException
     * @throws IOException
     */
    private void harvest ()
        throws FileNotFoundException, UnsupportedEncodingException, IOException
    {
        System.out.println(cli.toString());

        if (!cli.dir.exists()) {
            throw new Error("Could not find " + cli.dir);
        }

        if (!cli.dir.isDirectory()) {
            throw new Error("Not a directory " + cli.dir);
        }

        FileOutputStream os = new FileOutputStream(cli.out);

        ps = new PrintStream(os, false, "UTF8");
        pl("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        pl("<Wix xmlns=\"http://schemas.microsoft.com/wix/2006/wi\">");
        indent++;

        groupFolders(); // First fragment

        groupComponents(); // Second fragment

        indent--;
        pl("</Wix>");
        os.flush();
        os.close();
    }

    //---------------//
    // harvestFolder //
    //---------------//
    /**
     * Process the directory provided (and recursively its subdirectories).
     * @param dir the directory to process
     */
    private void harvestFolder (File dir)
    {
        DirId did = new DirId(dir);
        dirs.add(did);
        pl("<Directory Id=\"" + did + "\" Name=\"" + dir.getName() + "\">");
        indent++;

        pl("<Component Id=\"" + did + "\" Guid=\"" + did.getUuid() + "\">");
        indent++;

        pl(
            "<RegistryValue Root=\"HKCU\" Key=\"Software\\[Manufacturer]\\[ProductName]\" Name=\"" +
            did.getPath() +
            "\" Type=\"string\" Value=\"installed\" KeyPath=\"yes\" />");
        pl("<RemoveFolder Id=\"" + did + "\" On=\"uninstall\" />");

        Set<File> subdirs = new LinkedHashSet<File>();

        for (File file : dir.listFiles()) {
            if (file.isDirectory()) {
                subdirs.add(file);
            } else {
                FilId fid = new FilId(file);
                pl(
                    "<File Id=\"" + fid + "\" Source=\"" + fid.getPath() +
                    "\" />");
            }
        }

        indent--;
        pl("</Component>");

        // Handle sub directories, if any
        for (File subdir : subdirs) {
            harvestFolder(subdir);
        }

        indent--;
        pl("</Directory>");
    }

    //----//
    // pl //
    //----//
    /**
     * Print one line on the xml output.
     * @param str the line to print
     */
    private void pl (String str)
    {
        for (int i = 0; i < indent; i++) {
            ps.print("    ");
        }

        ps.println(str);
    }

    //~ Inner Classes ----------------------------------------------------------

    //-----//
    // CLI //
    //-----//
    /**
     * Decodes and remembers the command line arguments.
     */
    private static class CLI
    {
        //~ Enumerations -------------------------------------------------------

        enum Status {
            //~ Enumeration constant initializers ------------------------------

            DIR,DR, CG,
            VAR,
            OUT,
            NONE;
        }

        //~ Instance fields ----------------------------------------------------

        File   dir;
        String dr;
        String cg;
        String var;
        File   out;

        //~ Constructors -------------------------------------------------------

        public CLI (String[] args)
        {
            // Decode all parameters
            Status status = Status.DIR;

            for (String arg : args) {
                switch (status) {
                case DIR :
                    dir = new File(arg);
                    status = Status.NONE;

                    break;

                case NONE :
                    status = Status.valueOf(arg.substring(1).toUpperCase());

                    break;

                case DR :
                    dr = arg;
                    status = Status.NONE;

                    break;

                case CG :
                    cg = arg;
                    status = Status.NONE;

                    break;

                case VAR :
                    var = arg;
                    status = Status.NONE;

                    break;

                case OUT :
                    out = new File(arg);
                    status = Status.NONE;

                    break;
                }
            }
        }

        //~ Methods ------------------------------------------------------------

        @Override
        public String toString ()
        {
            StringBuilder sb = new StringBuilder("{CLI");

            sb.append(" dir=")
              .append(dir);
            sb.append(" dr=")
              .append(dr);
            sb.append(" cg=")
              .append(cg);
            sb.append(" var=")
              .append(var);
            sb.append(" out=")
              .append(out);

            sb.append("}");

            return sb.toString();
        }
    }

    //-------//
    // DirId //
    //-------//
    /**
     * Handles the WIX reference to a directory.
     */
    private class DirId
        extends FilId
    {
        //~ Constructors -------------------------------------------------------

        public DirId (File file)
        {
            super(file);
        }

        //~ Methods ------------------------------------------------------------

        @Override
        protected String getPrefix ()
        {
            return "dir";
        }

        @Override
        protected String getSeparator ()
        {
            return "-";
        }
    }

    //-------//
    // Error //
    //-------//
    private class Error
        extends RuntimeException
    {
        //~ Constructors -------------------------------------------------------

        public Error (String message)
        {
            super(message);
        }
    }

    //-------//
    // FilId //
    //-------//
    /**
     * Handles the WIX reference to a file.
     */
    private class FilId
    {
        //~ Instance fields ----------------------------------------------------

        protected final UUID uuid;
        protected final File file;

        //~ Constructors -------------------------------------------------------

        public FilId (File file)
        {
            uuid = UUID.randomUUID();
            this.file = file;
        }

        //~ Methods ------------------------------------------------------------

        @Override
        public String toString ()
        {
            return getPrefix() +
                   uuid.toString()
                       .replaceAll("-", "")
                       .toUpperCase();
        }

        protected String getPrefix ()
        {
            return "fil";
        }

        protected String getSeparator ()
        {
            return "\\";
        }

        String getPath ()
        {
            StringBuilder path = new StringBuilder();
            File          f = file;

            while ((f != null) && !f.equals(cli.dir)) {
                path.insert(0, f.getName());
                path.insert(0, getSeparator());
                f = f.getParentFile();
            }

            path.insert(0, "$(" + cli.var + ")");

            return path.toString();
        }

        UUID getUuid ()
        {
            return uuid;
        }
    }
}
