//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        L a n g u a g e s                                       //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2025. All rights reserved.
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
import org.audiveris.omr.ui.GuiActions;
import org.audiveris.omr.ui.OmrGui;
import org.audiveris.omr.ui.util.Panel;
import org.audiveris.omr.ui.util.WaitingTask;

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

import static java.awt.Component.CENTER_ALIGNMENT;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.stream.Collectors;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;

/**
 * Class <code>Languages</code> defines the user dialogues to check and install languages files.
 *
 * @author Hervé Bitteur
 */
public class Languages
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(Languages.class);

    private static final String GITHUB_ORGANIZATION = "tesseract-ocr";

    private static final String GITHUB_REPOSITORY = "tessdata";

    private static Languages INSTANCE;

    //~ Instance fields ----------------------------------------------------------------------------

    /** Component resources. */
    private ResourceMap resources;

    /** Remote data (languages, codes). */
    private RemoteData remoteData;

    /** The user interface to browse, select and install languages. */
    private Selector selector;

    //~ Methods ------------------------------------------------------------------------------------

    //---------------//
    // buildSelector //
    //---------------//
    /**
     * Build the languages selector, based on the data remotely available.
     *
     * @return the selector newly built
     */
    private Selector buildSelector ()
    {
        getRemoteData();

        if ((remoteData != null) && !remoteData.codes.isEmpty()) {
            return new Selector();
        }

        return null;
    }

    //--------------//
    // checkSupport //
    //--------------//
    /**
     * Check the current set of installed languages and prompt the user to install some
     * if the set is empty.
     */
    public void checkSupport ()
    {
        final SortedSet<String> installed = TesseractOCR.getInstance().getSupportedLanguages();
        if (!installed.isEmpty()) {
            logger.info(
                    "Installed OCR languages: {}",
                    installed.stream().collect(Collectors.joining(",")));
        } else if (OMR.gui == null) {
            logger.warn("*** No installed OCR languages ***");
        } else {
            // Prompt user
            final String install = getResources().getString("Check.install");
            final String later = getResources().getString("Check.later");
            final Object[] options = { install, later };
            final String message = getResources().getString("Check.message");

            final int choice = JOptionPane.showOptionDialog(
                    OMR.gui.getFrame(),
                    message,
                    getResources().getString("Check.title"),
                    JOptionPane.DEFAULT_OPTION,
                    JOptionPane.WARNING_MESSAGE,
                    null,
                    options,
                    options[0]);

            if (choice == 0) {
                GuiActions.getInstance().installLanguages(null).execute();
            }
        }
    }

    //--------------------//
    // downloadRemoteData //
    //--------------------//
    /**
     * Download the collection of languages names and codes available on Github Tesseract site.
     */
    private void downloadRemoteData ()
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
                return;
            }

            // Retrieve the remote content and codes
            remoteData = new RemoteData();
            remoteData.content = repository.getDirectoryContent("");
            remoteData.codes = new ArrayList<>();

            for (GHContent c : remoteData.content) {
                final String fileName = c.getName();

                if (c.isFile() && fileName.endsWith(LANGUAGE_FILE_EXT)) {
                    remoteData.codes.add(fileName.replace(LANGUAGE_FILE_EXT, ""));
                    logger.debug("code: {} size: {}", fileName, c.getSize());
                }
            }

            logger.info("Languages available on Tesseract site: {}", remoteData.codes.size());
        } catch (IOException ex) {
            logger.warn("Error getting remote languages.\n   {}", ex.getMessage());

            if (ex.getCause() != null) {
                logger.warn("   Cause: {}", ex.getCause().toString());
            }

            logger.warn("   Please make sure you have access to Internet.");
        }
    }

    //---------------//
    // getRemoteData //
    //---------------//
    /**
     * Return the collection of languages remotely available on Github Tesseract site.
     *
     * @return the RemoteData or null
     */
    private RemoteData getRemoteData ()
    {
        if (remoteData == null) {
            new DownloadRemoteTask().run();
        }

        return remoteData;
    }

    //--------------//
    // getResources //
    //--------------//
    private ResourceMap getResources ()
    {
        if (resources == null) {
            resources = Application.getInstance().getContext().getResourceMap(getClass());
        }

        return resources;
    }

    //-------------//
    // getSelector //
    //-------------//
    /**
     * Report the UI dialog to browse, select and install OCR languages.
     *
     * @return the selector
     */
    public Selector getSelector ()
    {
        if (selector == null) {
            selector = buildSelector();
        }

        return selector;
    }

    //~ Static Methods -----------------------------------------------------------------------------

    //-------------//
    // getInstance //
    //-------------//
    public static Languages getInstance ()
    {
        if (INSTANCE == null) {
            INSTANCE = new Languages();
        }

        return INSTANCE;
    }

    //~ Inner Classes ------------------------------------------------------------------------------

    //--------------------//
    // DownloadRemoteTask //
    //--------------------//
    /**
     * Task to download the lists of languages names and codes from Tesseract GitHub site.
     */
    private class DownloadRemoteTask
            extends WaitingTask<Languages.RemoteData, Void>
    {
        DownloadRemoteTask ()
        {
            super(OmrGui.getApplication(), getResources().getString("downloadTask.message"));
        }

        @Override
        protected Languages.RemoteData doInBackground ()
            throws Exception
        {
            downloadRemoteData();
            return remoteData;
        }
    }

    //------------//
    // RemoteData //
    //------------//
    /**
     * The collection of names and codes remotely available on Github Tesseract site.
     */
    public static class RemoteData
    {
        /** List of languages files names. */
        public List<GHContent> content;

        /** List of 3-letter codes. */
        public List<String> codes;
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

    //----------//
    // Selector //
    //----------//
    /**
     * The UI to browse, select and install languages.
     */
    public class Selector
            implements ActionListener
    {
        private final JDialog dialog;

        private final String boxTip = getResources().getString("box.shortDescription");

        /**
         * Creates a new <code>Selector</code> object.
         */
        public Selector ()
        {
            dialog = new JDialog(OMR.gui.getFrame(), true); // True for a modal dialog
            dialog.setName("LanguagesFrame"); // For SAF life cycle
            dialog.setPreferredSize(new Dimension(340, 600));

            final JComponent framePane = (JComponent) dialog.getContentPane();
            framePane.setLayout(new BoxLayout(framePane, BoxLayout.Y_AXIS));

            framePane.add(new JScrollPane(defineLayout()));

            // Closing (via exit button)
            JButton button = new JButton();
            button.setName("exitButton");
            button.setAlignmentX(CENTER_ALIGNMENT);
            button.addActionListener(this);
            framePane.add(button);

            // Closing (via close window)
            dialog.addWindowListener(new WindowAdapter()
            {
                @Override
                public void windowClosing (WindowEvent e)
                {
                    closeDialog();
                }
            });

            getResources().injectComponents(dialog);
            dialog.pack();
        }

        @Override
        public void actionPerformed (ActionEvent e)
        {
            closeDialog();
        }

        private void closeDialog ()
        {
            dialog.setVisible(false);
            dialog.dispose();
        }

        private JPanel defineLayout ()
        {
            final List<String> codes = remoteData.codes;
            final ScrollablePanel panel = new ScrollablePanel();

            // JGoodies columns:           code        checkbox        fullName
            final String colSpec = "right:50dlu,5dlu,center:10dlu,5dlu,left:200dlu";
            final int perLine = 19;
            final int height = codes.size() * perLine;
            panel.setPreferredSize(new Dimension(320, height));

            final FormLayout layout = new FormLayout(colSpec, Panel.makeRows(codes.size()));
            final FormBuilder builder = FormBuilder.create().layout(layout).panel(panel);
            int r = 1;

            for (String name : codes) {
                final LangLine line = new LangLine(name);
                line.label.setHorizontalAlignment(SwingConstants.LEFT);
                builder.addRaw(line.label).xy(1, r);
                builder.addRaw(line.box).xy(3, r);
                builder.addRaw(line.fullName).xy(5, r);
                r += 2;
            }

            return panel;
        }

        /**
         * Install the data file for the provided language code.
         *
         * @param code the language code
         */
        private void install (String code)
        {
            try {
                final Path ocrFolder = TesseractOCR.getInstance().getOcrFolder();
                logger.debug("ocrFolder: {}", ocrFolder);

                if (!Files.isWritable(ocrFolder)) {
                    logger.warn("No write access to the OCR folder {}", ocrFolder);
                    return;
                }

                final String fileName = code + LANGUAGE_FILE_EXT;

                for (GHContent c : remoteData.content) {
                    if (c.isFile() && c.getName().equals(fileName)) {
                        final URI uri = new URI(c.getDownloadUrl());
                        final Path targetPath = ocrFolder.resolve(fileName);
                        logger.info("Downloading '{}' as {} ...", code, targetPath);

                        try (InputStream is = uri.toURL().openStream()) {
                            final long size = Files.copy(is, targetPath, REPLACE_EXISTING);
                            logger.info("Installed file: {} size: {}", fileName, size);

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

        public JDialog getComponent ()
        {
            return dialog;
        }

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
                    box.setToolTipText(boxTip);
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
                        install(code);
                    }
                }
            }
        }
    }
}
