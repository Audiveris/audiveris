//----------------------------------------------------------------------------//
//                                                                            //
//                             U t i l i t i e s                              //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package com.audiveris.installer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.jar.JarFile;

/**
 * Class {@code Utilities} gathers helper methods for installation
 * whatever the current environment.
 *
 * @author Hervé Bitteur
 */
public class Utilities
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(
            Utilities.class);

    //~ Methods ----------------------------------------------------------------
    //----------//
    // download //
    //----------//
    /**
     * Download data from a given url to the specified target file.
     *
     * @param urlString the url to download from
     * @param target    the target file
     */
    public static void download (String urlString,
                                 File target)
    {
        // Check url 
        URL url;
        try {
            url = new URL(urlString);
        } catch (MalformedURLException ex) {
            logger.warn("MalformedURLException", ex);
            throw new RuntimeException(ex);
        }

        // Check target
        File parentDir = target.getParentFile();
        if (!parentDir.exists()) {
            throw new RuntimeException("Target directory of " + target
                                       + " does not exist");
        }
        if (!parentDir.isDirectory()) {
            throw new RuntimeException(parentDir + " is not a directory");
        }

        logger.info("Downloading {} to {} ...", url, target.getAbsolutePath());
        Jnlp.extensionInstallerService.setStatus("Downloading " + url);

        try {
            URLConnection uc = url.openConnection();
            uc.setUseCaches(true);

            try (InputStream is = uc.getInputStream()) {
                Files.copy(is, target.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            logger.debug("End of download.");
            Jnlp.extensionInstallerService.setStatus("");
            try {
                Thread.sleep(500);
            } catch (InterruptedException ex) {
                logger.warn("Error on Thread.sleep", ex);
            }
        } catch (IOException ex) {
            throw new RuntimeException("Error downloading " + urlString
                                       + " to " + target.getAbsolutePath(), ex);
        }
    }

    //------------------------//
    // downloadExecAndInstall //
    //------------------------//
    /**
     * Download an executable from provided url and launch the executable with
     * the installOption.
     *
     * @param title         A title for the software to process
     * @param url           url to download from
     * @param temp          temp directory to be used for download
     * @param installOption installation option if any, perhaps empty but not
     *                      null
     * @throws Throwable
     */
    public static void downloadExecAndInstall (String title,
                                               String url,
                                               File temp,
                                               String installOption)
            throws Exception
    {
        try {
            // Download
            final String fileName = new File(url).getName();
            final File exec = new File(temp, fileName);
            download(url, exec);

            // Install
            final List<String> output = new ArrayList<>();
            final int res = Utilities.runProcess(
                    exec.getAbsolutePath(), output, installOption);
            if (res != 0) {
                logger.warn(Utilities.dumpOfLines(output));
                throw new RuntimeException("Could not run " + exec + ", exit: " + res);
            }
        } catch (IOException | InterruptedException | RuntimeException ex) {
            logger.warn("Could not install " + title, ex);
            throw ex;
        }
    }

    //----------------------//
    // downloadJarAndExpand //
    //----------------------//
    /**
     * Download a .jar file from a provided url and expand it to the
     * desired target directory.
     *
     * @param title     A title for the item to process
     * @param url       url to download from
     * @param temp      temp directory to be used for download
     * @param root      root filter
     * @param targetDir target directory
     * @throws Throwable
     */
    public static void downloadJarAndExpand (String title,
                                             String url,
                                             File temp,
                                             String root,
                                             File targetDir)
            throws Exception
    {
        // Download the .jar file
        final String fileName = new File(url).getName();
        final File jarFile = new File(temp, fileName);
        download(url, jarFile);
        JarFile jar = new JarFile(jarFile);

        // Expand the jar file
        if (!targetDir.exists()) {
            if (targetDir.mkdirs()) {
                logger.info("Created folder {}", targetDir.getAbsolutePath());
            }
        }
        JarExpander exp = new JarExpander(jar, root, targetDir);
        exp.install();
    }

    //------------//
    // runProcess //
    //------------//
    /**
     * Launch a process.
     *
     * @param execPath (input) the path to the executable file
     * @param output   (output) the output lines (stdout and stderr)
     * @param args     (input) arguments for the executable
     * @return the process exit code
     * @throws IOException
     * @throws InterruptedException
     */
    public static int runProcess (String execPath,
                                  List<String> output,
                                  String... args)
            throws IOException, InterruptedException
    {
        // Command arguments
        List<String> cmdArgs = new ArrayList<>();
        cmdArgs.add(execPath);
        cmdArgs.addAll(Arrays.asList(args));
        logger.debug("cmdArgs: {}", cmdArgs);

        try {
            // Spawn process
            ProcessBuilder pb = new ProcessBuilder(cmdArgs);
            pb.redirectErrorStream(true);

            Process process = pb.start();

            // Read output
            InputStream is = process.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            String line;

            while ((line = br.readLine()) != null) {
                logger.trace("line: {}", line);
                output.add(line);
            }

            // Wait for process completion
            int exitValue = process.waitFor();
            logger.debug("Exit value is: {}", exitValue);

            return exitValue;
        } catch (Exception ex) {
            logger.warn("Error running " + cmdArgs, ex);
            throw ex;
        }
    }

    //-------//
    // toURI //
    //-------//
    /**
     * Convenient method to simulate a parent/child composition
     *
     * @param parent the URI to parent directory
     * @param child  the child name
     * @return the resulting URI
     */
    public static URI toURI (URI parent,
                             String child)
    {
        try {
            // Make sure parent ends with a '/'
            if (parent == null) {
                throw new IllegalArgumentException("Parent is null");
            }

            StringBuilder dirName = new StringBuilder(parent.toString());

            if (dirName.charAt(dirName.length() - 1) != '/') {
                dirName.append('/');
            }

            // Make sure child does not start with a '/'
            if ((child == null) || child.isEmpty()) {
                throw new IllegalArgumentException("Child is null or empty");
            }

            if (child.startsWith("/")) {
                throw new IllegalArgumentException(
                        "Child is absolute: " + child);
            }

            return new URI(dirName.append(child).toString());
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException(ex.getMessage(), ex);
        }
    }

    //-------------//
    // dumpOfLines //
    //-------------//
    /**
     * Meant for dumping a bunch of lines
     *
     * @param lines the lines to dump
     * @return one string for all lines
     */
    public static String dumpOfLines (List<String> lines)
    {
        StringBuilder sb = new StringBuilder();

        for (String line : lines) {
            sb.append("\n>>> ").append(line);
        }

        return sb.toString();
    }
}
