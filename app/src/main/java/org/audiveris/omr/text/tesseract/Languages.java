//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        L a n g u a g e s                                       //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2024. All rights reserved.
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
package org.audiveris.omr.text.tesseract;

import org.audiveris.omr.OMR;
import static org.audiveris.omr.text.Language.DEFINED_LANGUAGES;
import static org.audiveris.omr.text.Language.getSupportedLanguages;
import static org.audiveris.omr.text.tesseract.TesseractOCR.LANGUAGE_FILE_EXT;
import org.audiveris.omr.ui.util.Panel;

import org.jdesktop.application.Application;
import org.jdesktop.application.ResourceMap;

import org.kohsuke.github.GHContent;
import org.kohsuke.github.GHOrganization;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jgoodies.forms.builder.FormBuilder;
import com.jgoodies.forms.layout.FormLayout;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;

/**
 * Class <code>Languages</code> defines the user interface to browse and download
 * all the possible Tesseract languages files.
 *
 * @author Hervé Bitteur
 */
public class Languages
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(Languages.class);

    private static final String GITHUB_ORGANIZATION = "tesseract-ocr";

    private static final String GITHUB_REPOSITORY = "tessdata";

    //~ Instance fields ----------------------------------------------------------------------------

    /** Content of the GitHub Tesseract repository to interact with. */
    private final List<GHContent> remoteContent;

    /** The interface window. */
    private final JDialog dialog;

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new <code>Languages</code> object.
     */
    private Languages (List<GHContent> remoteContent,
                       List<String> remoteCodes)
    {
        this.remoteContent = remoteContent;

        dialog = new JDialog(OMR.gui.getFrame());
        dialog.setName("LanguagesFrame"); // For SAF life cycle
        dialog.setPreferredSize(new Dimension(340, 600));

        final JComponent framePane = (JComponent) dialog.getContentPane();
        framePane.setLayout(new BorderLayout());

        final JPanel panel = defineLayout(remoteCodes);
        framePane.add(new JScrollPane(panel), BorderLayout.WEST);

        // Resources injection
        final ResourceMap resources = Application.getInstance().getContext().getResourceMap(
                getClass());
        resources.injectComponents(dialog);
    }

    //~ Methods ------------------------------------------------------------------------------------

    //--------------//
    // defineLayout //
    //--------------//
    private JPanel defineLayout (List<String> remoteCodes)
    {
        final ScrollablePanel panel = new ScrollablePanel();

        // JGoodies columns:           code        checkbox        fullName
        final String colSpec = "right:50dlu,5dlu,center:10dlu,5dlu,left:200dlu";
        final int perLine = 19;
        final int height = remoteCodes.size() * perLine;
        panel.setPreferredSize(new Dimension(320, height));

        final FormLayout layout = new FormLayout(colSpec, Panel.makeRows(remoteCodes.size()));
        final FormBuilder builder = FormBuilder.create().layout(layout).panel(panel);
        int r = 1;

        for (String name : remoteCodes) {
            final LangLine line = new LangLine(name);
            line.label.setHorizontalAlignment(SwingConstants.LEFT);
            builder.addRaw(line.label).xy(1, r);
            builder.addRaw(line.box).xy(3, r);
            builder.addRaw(line.fullName).xy(5, r);
            r += 2;
        }

        return panel;
    }

    //----------//
    // download //
    //----------//
    /**
     * Download the data file for the provided language code.
     *
     * @param code the language code
     */
    private void download (String code)
    {
        try {
            final Path ocrFolder = TesseractOCR.getInstance().getOcrFolder();
            logger.debug("ocrFolder: {}", ocrFolder);

            if (!Files.isWritable(ocrFolder)) {
                logger.warn("No write access to the OCR folder {}", ocrFolder);
                return;
            }

            final String fileName = code + LANGUAGE_FILE_EXT;

            for (GHContent c : remoteContent) {
                if (c.isFile() && c.getName().equals(fileName)) {
                    final URI uri = new URI(c.getDownloadUrl());
                    final Path targetPath = ocrFolder.resolve(fileName);
                    logger.info("Downloading '{}' as {} ...", code, targetPath);

                    try (InputStream is = uri.toURL().openStream()) {
                        final long size = Files.copy(is, targetPath, REPLACE_EXISTING);
                        logger.info("Downloaded file: {} size: {}", fileName, size);

                        // Update the current collection of supported codes
                        getSupportedLanguages().addCode(code);

                        return;
                    } catch (IOException ex) {
                        logger.warn(
                                "{} while downloading {} to {}",
                                ex.getClass().getName(),
                                fileName,
                                ocrFolder);
                    }
                }
            }
        } catch (IOException | URISyntaxException ex) {
            logger.error("Error downloading from  remote site {}", ex.getMessage(), ex);
        }
    }

    //--------------//
    // getComponent //
    //--------------//
    public JDialog getComponent ()
    {
        return dialog;
    }

    //~ Static Methods -----------------------------------------------------------------------------

    //--------//
    // create //
    //--------//
    public static Languages create ()
    {
        try {
            // Retrieve the list of language codes available on the GitHub/Tesseract site.
            final GitHub github = GitHub.connectAnonymously();

            final GHOrganization organization = github.getOrganization(GITHUB_ORGANIZATION);
            logger.debug("{}", organization);

            final GHRepository repository = organization.getRepository(GITHUB_REPOSITORY);
            logger.debug("{}", repository);

            if (repository == null) {
                logger.warn("Unknown repository: {}", GITHUB_REPOSITORY);
                return null;
            }

            // Retrieve the remote codes
            final List<GHContent> remoteContent = repository.getDirectoryContent("");
            final List<String> codes = new ArrayList<>();

            for (GHContent c : remoteContent) {
                final String fileName = c.getName();

                if (c.isFile() && fileName.endsWith(LANGUAGE_FILE_EXT)) {
                    codes.add(fileName.replace(LANGUAGE_FILE_EXT, ""));
                    logger.debug("code: {} size: {}", fileName, c.getSize());
                }
            }

            logger.info("Languages available on Tesseract site: {}", codes.size());

            if (!codes.isEmpty()) {
                return new Languages(remoteContent, codes);
            }
        } catch (IOException ex) {
            logger.warn("Error getting remote languages.\n   {}", ex.getMessage());

            if (ex.getCause() != null) {
                logger.warn("   Cause: {}", ex.getCause().toString());
            }

            logger.warn("   Please make sure you have access to Internet.");
        }

        return null;
    }

    //~ Inner Classes ------------------------------------------------------------------------------

    //----------//
    // LangLine //
    //----------//
    /**
     * A label (code) followed by a check box and a fullName label.
     */
    private class LangLine
            implements ActionListener
    {
        public final JLabel label;

        public final JCheckBox box;

        public final JLabel fullName;

        public LangLine (String code)
        {
            label = new JLabel(code);
            final String fn = DEFINED_LANGUAGES.fullNameOf(code);

            if (fn != null) {
                box = new JCheckBox();
                fullName = new JLabel(fn);

                label.setEnabled(true);
                box.setEnabled(true);
                fullName.setEnabled(true);

                if (getSupportedLanguages().contains(code)) {
                    box.setSelected(true);
                }

                box.addActionListener(this);
            } else {
                logger.debug("Skipping {}", code);
                box = null;
                fullName = null;

                label.setEnabled(false);
            }
        }

        @Override
        public void actionPerformed (ActionEvent e)
        {
            final String code = label.getText();

            if (box.isSelected()) {
                if (getSupportedLanguages().contains(code)) {
                    logger.info("No need to re-download '{}'", code);
                } else {
                    download(code);
                }
            }
        }
    }

    //-----------------//
    // ScrollablePanel //
    //-----------------//
    private static class ScrollablePanel
            extends JPanel
            implements Scrollable
    {
        @Override
        public Dimension getPreferredScrollableViewportSize ()
        {
            return getPreferredSize();
        }

        @Override
        public int getScrollableBlockIncrement (Rectangle visibleRect,
                                                int orientation,
                                                int direction)
        {
            return (orientation == SwingConstants.HORIZONTAL) //
                    ? visibleRect.width
                    : visibleRect.height;
        }

        @Override
        public boolean getScrollableTracksViewportHeight ()
        {
            return false;
        }

        @Override
        public boolean getScrollableTracksViewportWidth ()
        {
            return true;
        }

        @Override
        public int getScrollableUnitIncrement (Rectangle visibleRect,
                                               int orientation,
                                               int direction)
        {
            return 40; // Minimum cell height. TODO: Could be improved.
        }
    }
}
