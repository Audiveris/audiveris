//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                         V e r s i o n s                                        //
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
package org.audiveris.omr.sheet;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import org.audiveris.omr.OMR;
import org.audiveris.omr.WellKnowns;
import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.ui.field.LComboBox;
import org.audiveris.omr.ui.field.LLabel;
import org.audiveris.omr.ui.util.BrowserLinkListener;
import org.audiveris.omr.ui.util.Panel;
import org.audiveris.omr.util.Version;

import org.kohsuke.github.GHOrganization;
import org.kohsuke.github.GHRelease;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextPane;

/**
 * Class {@code Versions} gathers key versions for upgrade checks.
 * <p>
 * It can poll Audiveris project site (GitHub) to check for availability of a more recent release
 * than the current software.
 * <p>
 * The poll can be manual (via interactive GuiActions) or automatic according to a chosen polling
 * period in batch or interactive modes.
 *
 * @author Hervé Bitteur
 */
public abstract class Versions
{

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(Versions.class);

    /** How to format dates. */
    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("dd-MMM-yyyy");

    /** GitHub organization name. */
    public static String AUDIVERIS_ORGANIZATION_NAME = WellKnowns.TOOL_NAME;

    /** GitHub repository name. */
    public static String AUDIVERIS_REPOSITORY_NAME = WellKnowns.TOOL_ID;

    /** Version of current Audiveris software. */
    public final static Version CURRENT_SOFTWARE = new Version(WellKnowns.TOOL_REF);

    /**
     * Version focused on better precision in inter geometry.
     * <ul>
     * <li>Migration from Point to Point2D for many inter segments (horizontal or vertical).
     * <li>Barline and Bracket now include staff line height.
     * <li>Related BarConnector and BracketConnector are shortened accordingly.
     * <li>Stem now uses thickness and vertical median line in lieu of top/bottom points.
     * <li>Ledger now uses thickness and horizontal median line.
     * </ul>
     */
    public final static Version INTER_GEOMETRY = new Version("5.2.1");
    //
    // NOTA: Add here below any new version for which some upgrade is necessary.
    //

    /**
     * Sequence of upgrade versions to check.
     * NOTA: This sequence must be manually updated when a new version is added above.
     */
    public final static List<Version> UPGRADE_VERSIONS = Arrays.asList(INTER_GEOMETRY);

    /** Latest upgrade version. */
    public final static Version LATEST_UPGRADE = UPGRADE_VERSIONS.get(UPGRADE_VERSIONS.size() - 1);

    // No instance needed for this functional class
    private Versions ()
    {
    }

    //-------//
    // check //
    //-------//
    /**
     * Check a provided (".omr" file) version against current software version.
     *
     * @param version version to check
     * @return check result
     */
    public static CheckResult check (Version version)
    {
        if (version.major < CURRENT_SOFTWARE.major) {
            // Non compatible, reprocess from binary?
            return CheckResult.BOOK_TOO_OLD;
        }

        if (version.major > CURRENT_SOFTWARE.major) {
            // Non compatible, use more recent program
            return CheckResult.PROGRAM_TOO_OLD;
        }

        if (version.minor > CURRENT_SOFTWARE.minor) {
            // Non compatible, use more recent program
            return CheckResult.PROGRAM_TOO_OLD;
        }

        // Compatible (though book file may be upgraded automatically)
        return CheckResult.COMPATIBLE;
    }

    //-------------//
    // getUpgrades //
    //-------------//
    /**
     * Report the sequence of upgrade versions to apply on the provided sheet version.
     *
     * @param sheetVersion current version of sheet
     * @return sequence of upgrades to perform, perhaps empty but never null
     */
    public static List<Version> getUpgrades (Version sheetVersion)
    {
        List<Version> list = null;

        for (Version v : UPGRADE_VERSIONS) {
            if (sheetVersion.compareTo(v) < 0) {
                if (list == null) {
                    list = new ArrayList<>();
                }

                list.add(v);
            }
        }

        return (list == null) ? Collections.EMPTY_LIST : list;
    }

    //-------------//
    // CheckResult //
    //-------------//
    public enum CheckResult
    {
        COMPATIBLE,
        BOOK_TOO_OLD,
        PROGRAM_TOO_OLD;
    }

    //------------------//
    // getLatestRelease //
    //------------------//
    /**
     * Retrieve the latest release available on Audiveris project site.
     *
     * @return the latest release, or null if something went wrong
     */
    public static GHRelease getLatestRelease ()
    {
        try {
            GitHub github = GitHub.connectAnonymously();

            GHOrganization organization = github.getOrganization(AUDIVERIS_ORGANIZATION_NAME);
            logger.debug("{}", organization);

            GHRepository repository = organization.getRepository(AUDIVERIS_REPOSITORY_NAME);
            logger.debug("{}", repository);

            if (repository == null) {
                logger.warn("Unknown repository: {}", AUDIVERIS_REPOSITORY_NAME);
                return null;
            }

            GHRelease latestRelease = repository.getLatestRelease();
            logger.debug("Latest release: {}", latestRelease);

            // Remember the date this poll  was made
            Calendar now = new GregorianCalendar();
            constants.lastReleaseCheckDate.setValue(now.getTime());

            return latestRelease;
        } catch (IOException ex) {
            logger.warn("Could not connect to Audiveris project.\n{}", ex.toString());

            if (ex.getCause() != null) {
                logger.warn("Cause: {}", ex.getCause().toString());
            }

            return null;
        }
    }

    //-----------------//
    // considerPolling //
    //-----------------//
    /**
     * Check whether poll time has come and, if so, do poll the project site.
     */
    public static void considerPolling ()
    {
        if (isTimeToPoll()) {
            poll(false /* manual */);
        } else {
            logger.debug("Versions. Not yet time to poll");
        }
    }

    //------//
    // poll //
    //------//
    /**
     * Poll the GitHub site for a new Audiveris release.
     *
     * @param manual true for a user manual poll, false for an automatic poll
     */
    public static void poll (boolean manual)
    {
        GHRelease latest = getLatestRelease();
        Version latestVersion = new Version(latest.getTagName());

        if (Versions.CURRENT_SOFTWARE.compareTo(latestVersion) < 0) {
            logger.info("A new software release is available: {}", latestVersion);

            if (OMR.gui == null) {
                logger.info("See {}", latest.getHtmlUrl());
            } else {
                BasicPanel panel = new PositivePanel(latest);

                JOptionPane.showMessageDialog(
                        OMR.gui.getFrame(),
                        panel,
                        panel.getTitle(),
                        JOptionPane.INFORMATION_MESSAGE);
            }
        } else {
            logger.info("Software version is up-to-date");

            if ((OMR.gui != null) && manual) {
                // Explicitly tell the user that check result is negative
                BasicPanel panel = new NegativePanel(latest);

                JOptionPane.showMessageDialog(
                        OMR.gui.getFrame(),
                        panel,
                        panel.getTitle(),
                        JOptionPane.INFORMATION_MESSAGE);
            }
        }
    }

    //-----------------//
    // getNextPollDate //
    //-----------------//
    /**
     * Report the next date when project site is to be polled.
     *
     * @return next poll date
     */
    private static Calendar getNextPollDate ()
    {
        Calendar next = new GregorianCalendar();
        next.setTime(constants.lastReleaseCheckDate.getValue());

        final Frequency frequency = constants.releaseCheckFrequency.getValue();
        switch (frequency) {
        case Always:
            break;
        case Daily:
            next.add(Calendar.DAY_OF_MONTH, 1);
            break;
        case Weekly:
            next.add(Calendar.WEEK_OF_MONTH, 1);
            break;
        case Monthly:
            next.add(Calendar.MONTH, 1);
            break;
        case Yearly:
            next.add(Calendar.YEAR, 1);
            break;
        case Never:
            next = null;
        }

        logger.info("Versions. Poll frequency: {}, next poll on: {}",
                    frequency,
                    next != null ? DATE_FORMAT.format(next.getTime()) : null);

        return next;
    }

    //--------------//
    // isTimeToPoll //
    //--------------//
    /**
     * Report whether the time has come to poll project site for a new release.
     *
     * @return true if so
     */
    private static boolean isTimeToPoll ()
    {
        Calendar now = new GregorianCalendar();
        Calendar next = getNextPollDate();

        if (next == null) {
            return false;
        }

        return now.compareTo(next) > 0;
    }

    //------------//
    // BasicPanel //
    //------------//
    private static abstract class BasicPanel
            extends Panel
    {

        protected final String title;

        protected LLabel status = new LLabel("Status:", "Software status", JLabel.LEFT);

        protected LLabel tag = new LLabel("Release:", "Latest release", JLabel.LEFT);

        protected LComboBox<Frequency> polling = new LComboBox<>("Polling:",
                                                                 "Frequency for release check",
                                                                 Frequency.values());

        /** The JGoodies/Form layout to be used by all subclasses. */
        protected final FormLayout layout = new FormLayout(getColumnsSpec(), getRowsSpec());

        /** The JGoodies/Form builder to be used by all subclasses. */
        protected final PanelBuilder builder;

        /** Handling of entered / selected values. */
        private final Action paramAction;

        BasicPanel (String title,
                    GHRelease release)
        {
            ///super(OMR.gui.getFrame(), title, false /* modal */);
            this.title = title;

            paramAction = new ParamAction();
            builder = new PanelBuilder(layout, this);
            defineLayout();

            tag.setText(release.getTagName());
            polling.setSelectedItem(constants.releaseCheckFrequency.getValue());
        }

        String getTitle ()
        {
            return title;
        }

        protected String getColumnsSpec ()
        {
            return "right:pref, 5dlu, 45dlu, 5dlu, 75dlu";
        }

        protected String getRowsSpec ()
        {
            return "pref, 3dlu,pref, 3dlu,pref";
        }

        private void defineLayout ()
        {

            final CellConstraints cst = new CellConstraints();

            // Status
            int r = 1; // --------------------------------
            builder.add(status.getLabel(), cst.xy(1, r));
            builder.add(status.getField(), cst.xyw(3, r, 3));

            // Tag
            r += 2; // -----------------------------------
            builder.add(tag.getLabel(), cst.xy(1, r));
            builder.add(tag.getField(), cst.xyw(3, r, 3));

            // Polling
            r += 2; // -----------------------------------
            builder.add(polling.getLabel(), cst.xy(1, r));
            builder.add(polling.getField(), cst.xyw(3, r, 1));
            polling.addActionListener(paramAction);

            setInsets(10, 10, 10, 10);
            setOpaque(true);
            setBackground(Color.WHITE);
        }

        private class ParamAction
                extends AbstractAction
        {

            /**
             * Method run when user presses Return/Enter in one of the parameter fields
             *
             * @param e the triggering event
             */
            @Override
            public void actionPerformed (ActionEvent e)
            {
                if (polling.getField() == e.getSource()) {
                    final Frequency newFrequency = polling.getSelectedItem();
                    constants.releaseCheckFrequency.setValue(newFrequency);
                }
            }
        }
    }

    //---------------//
    // NegativePanel //
    //---------------//
    private static class NegativePanel
            extends BasicPanel
    {

        NegativePanel (GHRelease release)
        {
            super("Software is up-to-date", release);
            setName("PollingNegativeDialog"); // For SAF life cycle

            // Status
            status.setText("You already have the latest release");
        }
    }

    //---------------//
    // PositivePanel //
    //---------------//
    private static class PositivePanel
            extends BasicPanel
    {

        private final LLabel published
                = new LLabel("Published on:", "Publication date", JLabel.LEFT);

        private final JLabel urlLabel = new JLabel("URL:");

        private final JEditorPane urlField = new JEditorPane();

        private final LLabel title = new LLabel("Title:", "Release title", JLabel.LEFT);

        private final JLabel contentLabel = new JLabel("Content:");

        private final JTextPane contentField = new JTextPane();

        PositivePanel (GHRelease release)
        {
            super("New Audiveris release available", release);
            setName("PollingPositiveDialog"); // For SAF life cycle

            // Status
            status.setText("A new Audiveris release is available!");

            defineLayout();

            // Published
            published.setText(release.getPublished_at().toString());

            // Url
            final URL url = release.getHtmlUrl();
            urlField.addHyperlinkListener(new BrowserLinkListener());
            urlField.setContentType("text/html");
            urlLabel.setToolTipText("Hyperlink to release web page");
            urlField.setToolTipText("Hyperlink to release web page");
            urlField.setEditable(false);
            urlField.setBackground(Color.WHITE);
            urlField.setText("<style> body {font-family: sans-serif; font-size: 9px;} </style>"
                                     + " <A HREF=" + url + ">" + url + "</A>");

            // Title
            title.setText(release.getName());

            // Content
            contentField.setBackground(Color.WHITE);
            contentLabel.setToolTipText("Release content");
            contentField.setToolTipText("Release content");
            contentField.setEditable(false);
            contentField.setMargin(new Insets(5, 5, 5, 5));
            contentField.setText(release.getBody().trim());
        }

        @Override
        protected String getColumnsSpec ()
        {
            return "right:pref, 5dlu, 45dlu, 5dlu, 250dlu";
        }

        @Override
        protected String getRowsSpec ()
        {
            return super.getRowsSpec() + ", 3dlu,pref, 3dlu,pref, 3dlu,pref, 3dlu,pref";
        }

        private void defineLayout ()
        {
            final CellConstraints cst = new CellConstraints();

            // Published
            int r = 7; // --------------------------------
            builder.add(published.getLabel(), cst.xy(1, r));
            builder.add(published.getField(), cst.xyw(3, r, 3));

            // Url
            r += 2; // -----------------------------------
            builder.add(urlLabel, cst.xy(1, r));
            builder.add(urlField, cst.xyw(3, r, 3));

            // Title
            r += 2; // -----------------------------------
            builder.add(title.getLabel(), cst.xy(1, r));
            builder.add(title.getField(), cst.xyw(3, r, 3));

            // Content
            r += 2; // -----------------------------------
            builder.add(contentLabel, cst.xy(1, r));
            builder.add(contentField, cst.xyw(3, r, 3));
        }
    }

    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {

        private final Constant.Enum<Frequency> releaseCheckFrequency = new Constant.Enum<>(
                Frequency.class,
                Frequency.Weekly,
                "Frequency of release check");

        private final Constant.Date lastReleaseCheckDate = new Constant.Date(
                "1-Jan-2000",
                "Date when last release check was made");
    }

    //-----------//
    // Frequency //
    //-----------//
    /** Frequency for polling project site. */
    private static enum Frequency
    {
        Always,
        Daily,
        Weekly,
        Monthly,
        Yearly,
        Never;
    }
}
