//----------------------------------------------------------------------------//
//                                                                            //
//                          L a n g C o m p a n i o n                         //
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

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.JOptionPane;

/**
 * Class {@code LangCompanion} handles the installation of Tesseract
 * support for a selected set of languages.
 *
 * @author Hervé Bitteur
 */
public class LangCompanion
    extends AbstractCompanion
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(
        LangCompanion.class);

    /** Companion description. */
    private static final String DESC = "<html>You can select which precise languages" +
                                       "<br/>the <b>OCR</b> should support." +
                                       "<br/>For this, please use the language selector below.</html>";

    /** Environment descriptor. */
    private static final Descriptor descriptor = DescriptorFactory.getDescriptor();

    /** Internet address to retrieve Tesseract trained data. */
    private static final String TESS_RADIX = "http://tesseract-ocr.googlecode.com/files";

    /** Precise Tesseract version. */
    private static final String TESS_VERSION = "tesseract-ocr-3.02";

    /**
     * Collection of all Tesseract supported languages.
     * Update this list according to Tesseract web site on
     * http://code.google.com/p/tesseract-ocr/downloads/list
     */
    public static final String[] ALL_LANGUAGES = new String[] {
                                                     "afr", "ara", "aze", "bel",
                                                     "ben", "bul", "cat", "ces",
                                                     "chi_sim", "chi_tra", "chr",
                                                     "dan", "deu", "ell", "eng",
                                                     "enm", "epo", "epo_alt",
                                                     "equ", "est", "eus", "fin",
                                                     "fra", "frk", "frm", "glg",
                                                     "grc", "heb", "hin", "hrv",
                                                     "hun", "ind", "isl", "ita",
                                                     "ita_old", "jpn", "kan",
                                                     "kor", "lav", "lit", "mal",
                                                     "mkd", "mlt", "msa", "nld",
                                                     "nor", "pol", "por", "ron",
                                                     "rus", "slk", "slv", "spa",
                                                     "spa_old", "sqi", "srp",
                                                     "swa", "swe", "tam", "tel",
                                                     "tgl", "tha", "tur", "ukr",
                                                     "vie"
                                                 };

    /**
     * Collection of pre-desired languages.
     */
    public static final String[] PREDESIRED_LANGUAGES = new String[] {
                                                            "deu", "eng", "fra",
                                                            "ita"
                                                        //            //  "mkd", "mlt", "msa", "nld",
    //        , "tgl", "tha", "tur", "ukr", "vie"
    };

    //~ Instance fields --------------------------------------------------------

    /**
     * The languages to be added (if not present).
     */
    private final Set<String> desired = buildDesiredLanguages();

    /**
     * The languages to be removed (if present).
     */
    private final Set<String> nonDesired = new TreeSet<String>();

    /** User selector, if any. */
    private LangSelector selector;

    //~ Constructors -----------------------------------------------------------

    //---------------//
    // LangCompanion //
    //---------------//
    /**
     * Creates a new LangCompanion object for a list of languages.
     *
     * @param hasUI true for a related view
     */
    public LangCompanion (boolean hasUI)
    {
        super("OCR", DESC);

        if (hasUI) {
            view = new BasicCompanionView(this, 60);
            selector = new LangSelector(this);
        }
    }

    //~ Methods ----------------------------------------------------------------

    //----------------//
    // checkInstalled //
    //----------------//
    @Override
    public boolean checkInstalled ()
    {
        try {
            // We check if each of the desired language actually exists
            boolean installed = true;

            for (String language : desired) {
                if (!isLangInstalled(language)) {
                    installed = false;

                    break;
                }
            }

            // We check if each of the non-desired language still exists
            for (String language : nonDesired) {
                if (isLangInstalled(language)) {
                    installed = false;

                    break;
                }
            }

            status = installed ? Status.INSTALLED : Status.NOT_INSTALLED;
        } catch (Throwable ex) {
            logger.warn("No tessdata found", ex);
            status = Status.NOT_INSTALLED;
        }

        return status == Status.INSTALLED;
    }

    //------------//
    // getDesired //
    //------------//
    /**
     * Report the set of desired languages.
     *
     * @return the current set of desired languages
     */
    public Set<String> getDesired ()
    {
        return desired;
    }

    //------------------//
    // getInstallWeight //
    //------------------//
    @Override
    public int getInstallWeight ()
    {
        return isNeeded() ? 1 : 0;
    }

    //---------------//
    // getNonDesired //
    //---------------//
    /**
     * Report the set of non-desired languages.
     *
     * @return the current set of non-desired languages
     */
    public Set<String> getNonDesired ()
    {
        return nonDesired;
    }

    //-------------//
    // getSelector //
    //-------------//
    public LangSelector getSelector ()
    {
        return selector;
    }

    //-----------------//
    // isLangInstalled //
    //-----------------//
    public boolean isLangInstalled (String language)
    {
        try {
            // Retrieve Tesseract folder 
            final File tessdata = getTessdata(false);

            if (tessdata == null) {
                return false;
            }

            // We check if the main language file actually exists
            final File lanFile = new File(tessdata, language + ".traineddata");

            return lanFile.exists();
        } catch (Exception ex) {
            logger.warn("No tessdata found", ex);

            return false;
        }
    }

    //-----------//
    // doInstall //
    //-----------//
    @Override
    protected void doInstall ()
        throws Exception
    {
        // We process languages in alphabetical order
        // To keep in sync with selector display
        final Set<String> relevant = new TreeSet<String>();
        relevant.addAll(desired);
        relevant.addAll(nonDesired);

        for (String lang : relevant) {
            if (desired.contains(lang) && !isLangInstalled(lang)) {
                if (selector != null) {
                    selector.update(lang);
                }

                installLanguage(lang);
            } else if (nonDesired.contains(lang)) {
                if (selector != null) {
                    selector.update(lang);
                }

                uninstallLanguage(lang);
                nonDesired.remove(lang);
            }

            if (selector != null) {
                selector.update(null);
            }
        }
    }

    //-----------------------//
    // buildDesiredLanguages //
    //-----------------------//
    private Set<String> buildDesiredLanguages ()
    {
        Set<String> set = new TreeSet<String>();

        // Initialize selected languages with the pre-selected ones
        set.addAll(Arrays.asList(PREDESIRED_LANGUAGES));

        // Include all the already installed languages as well
        for (String lang : ALL_LANGUAGES) {
            if (isLangInstalled(lang)) {
                set.add(lang);
            }
        }

        return set;
    }

    //-------------//
    // getTessdata //
    //-------------//
    /**
     * Report tessdata directory, after creating it if is does not
     * exist yet.
     *
     * @param create true to create the folder if it does not exist
     * @return the tessdata file or null
     */
    private File getTessdata (final boolean create)
        throws Exception
    {
        final String prefix = System.getenv(Descriptor.TESSDATA_PREFIX);
        final File   dir;

        if (prefix == null) {
            if (create) {
                dir = descriptor.getDefaultTessdataPrefix();
                // Set environment variable for future processes
                descriptor.setenv(
                    true,
                    Descriptor.TESSDATA_PREFIX,
                    dir.getAbsolutePath() +
                    System.getProperty("file.separator"));
            } else {
                return null;
            }
        } else {
            dir = new File(prefix);
        }

        File tessdata = new File(dir, Descriptor.TESSDATA);

        // Make sure all directories exist
        if (!tessdata.exists() && create) {
            if (tessdata.mkdirs()) {
                logger.info("Created folder {}", tessdata.getAbsolutePath());
            }
        }

        return tessdata;
    }

    //-----------------//
    // installLanguage //
    //-----------------//
    private void installLanguage (final String lang)
        throws Exception
    {
        try {
            final String tarName = TESS_VERSION + "." + lang + ".tar";
            final String archiveName = tarName + ".gz";
            final String archiveHttp = TESS_RADIX + "/" + archiveName;

            // Download
            final File temp = descriptor.getTempFolder();
            final File targz = new File(temp, archiveName);
            Utilities.download(archiveHttp, targz);

            // Decompress the .tar.gz
            Expander.unGzip(targz, temp);

            // Expand the .tar to the tesseract target
            final File tar = new File(temp, tarName);
            logger.debug("tar: {}", tar);

            final File tessdata = getTessdata(true);
            logger.debug("tessdata: {}", tessdata.getAbsolutePath());

            final File tessParent = tessdata.getParentFile()
                                            .getParentFile();
            logger.debug("tessParent: {}", tessParent.getAbsolutePath());
            Expander.unTar(tar, tessParent);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(
                Installer.getFrame(),
                ex.getMessage());
            throw ex;
        }
    }

    //-------------------//
    // uninstallLanguage //
    //-------------------//
    private void uninstallLanguage (final String lang)
        throws Exception
    {
        File tessdata = getTessdata(false);

        if ((tessdata == null) || !tessdata.exists()) {
            return;
        }

        // Clean up relevant files in the folder
        Files.walkFileTree(
            tessdata.toPath(),
            EnumSet.noneOf(FileVisitOption.class),
            1,
            new FileVisitor<Path>() {
                    @Override
                    public FileVisitResult preVisitDirectory (Path                dir,
                                                              BasicFileAttributes attrs)
                        throws IOException
                    {
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFile (Path                file,
                                                      BasicFileAttributes attrs)
                        throws IOException
                    {
                        Path name = file.getName(file.getNameCount() - 1);
                        logger.debug("Visiting {}, name {}", file, name);

                        if (name.toString()
                                .startsWith(lang + ".")) {
                            logger.info("Removing file {}", file);
                            Files.delete(file);
                        }

                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFileFailed (Path        file,
                                                            IOException ex)
                        throws IOException
                    {
                        if (ex == null) {
                            return FileVisitResult.CONTINUE;
                        } else {
                            // file visit failed
                            throw ex;
                        }
                    }

                    @Override
                    public FileVisitResult postVisitDirectory (Path        dir,
                                                               IOException ex)
                        throws IOException
                    {
                        if (ex == null) {
                            return FileVisitResult.CONTINUE;
                        } else {
                            // directory iteration failed
                            throw ex;
                        }
                    }
                });
    }
}
