//----------------------------------------------------------------------------//
//                                                                            //
//                          L a n g S e l e c t o r                           //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package com.audiveris.installer;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.CC;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;

/**
 * Class {@code LangSelector} handles the collection of OCR languages,
 * displays a banner of relevant languages, with the ability
 * to add or remove languages.
 * <p>
 * If no language is desired, bundle installation cannot be launched.
 *
 * @author Hervé Bitteur
 */
public class LangSelector
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(
        LangSelector.class);

    //~ Instance fields --------------------------------------------------------

    /** Related companion. */
    private final OcrCompanion companion;

    /** Component. */
    private JPanel component;

    /** Display of handled languages. */
    private Banner banner;

    /** Desired languages. */
    private Set<String> desired;

    /** Non-desired languages. */
    private Set<String> nonDesired;

    //~ Constructors -----------------------------------------------------------

    //--------------//
    // LangSelector //
    //--------------//
    /**
     * Creates a new LangSelector object.
     *
     * @param companion the language companion
     */
    public LangSelector (OcrCompanion companion)
    {
        this.companion = companion;

        desired = companion.getDesired();
        nonDesired = companion.getNonDesired();

        component = defineLayout();
    }

    //~ Methods ----------------------------------------------------------------

    //--------------//
    // getComponent //
    //--------------//
    public JPanel getComponent ()
    {
        return component;
    }

    //--------//
    // update //
    //--------//
    /**
     * Update the banner display.
     */
    public void update (final String currentLang)
    {
        // It's easier to merely rebuild the banner component
        banner.defineLayout(currentLang);
    }

    //--------------//
    // defineLayout //
    //--------------//
    private JPanel defineLayout ()
    {
        final JPanel          comp = new JPanel();
        final FormLayout      layout = new FormLayout(
            "right:40dlu, $lcgap, fill:0:grow, $lcgap, 33dlu",
            "pref");
        final CellConstraints cst = new CellConstraints();
        final PanelBuilder    builder = new PanelBuilder(layout, comp);

        // Label on left side
        builder.addROLabel("Languages", cst.xy(1, 1));

        // "Banner" for the center of the line
        banner = new Banner();
        builder.add(banner.getComponent(), cst.xy(3, 1));

        // "Add" button on right side
        JButton button = new JButton(new AddAction());
        builder.add(button, cst.xy(5, 1));

        return comp;
    }

    //~ Inner Classes ----------------------------------------------------------

    //-----------//
    // AddAction //
    //-----------//
    /** Addition of one or several languages. */
    private class AddAction
        extends AbstractAction
    {
        //~ Constructors -------------------------------------------------------

        public AddAction ()
        {
            putValue(AbstractAction.NAME, "Add");
            putValue(
                AbstractAction.SHORT_DESCRIPTION,
                "Add one or several languages");
        }

        //~ Methods ------------------------------------------------------------

        @Override
        public void actionPerformed (ActionEvent e)
        {
            // Create a dialog with a JList of possible additions
            // That is all languages, minus those already desired
            List<String> additionals = new ArrayList<String>(
                Arrays.asList(OcrCompanion.ALL_LANGUAGES));
            additionals.removeAll(desired);

            JList<String> list = new JList(
                additionals.toArray(new String[additionals.size()]));
            JScrollPane   scrollPane = new JScrollPane(list);
            list.setLayoutOrientation(JList.VERTICAL_WRAP);
            list.setVisibleRowCount(10);

            // Let the user select additional languages
            int          opt = JOptionPane.showConfirmDialog(
                Installer.getFrame(),
                scrollPane,
                "OCR languages selection",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE);
            List<String> toAdd = list.getSelectedValuesList();
            logger.debug("Opt: {} Selection: {}", opt, toAdd);

            // Save the selection, only if OK
            if (opt == JOptionPane.OK_OPTION) {
                logger.info("Additional languages: {}", toAdd);
                desired.addAll(toAdd);

                // This may impact the "installed" status of the companion
                companion.checkInstalled();
                banner.defineLayout(null);
                companion.updateView();
            }
        }
    }

    //--------//
    // Banner //
    //--------//
    /**
     * The banner displays all relevant languages with their status.
     * desired / installed : green, nothing to do
     * desired / not installed : pink, installation scheduled
     * not desired / installed : gray, removal scheduled
     * not desired / not installed : not relevant, nothing to do
     */
    private class Banner
        implements ActionListener
    {
        //~ Instance fields ----------------------------------------------------

        private final JPanel      panel = new JPanel();
        private final JScrollPane scrollPane = new JScrollPane(panel);

        //~ Constructors -------------------------------------------------------

        public Banner ()
        {
            panel.setBorder(null);
            scrollPane.setBorder(null);
            scrollPane.setVerticalScrollBarPolicy(
                JScrollPane.VERTICAL_SCROLLBAR_NEVER);

            defineLayout(null);
        }

        //~ Methods ------------------------------------------------------------

        /** Triggered by popup selection. */
        @Override
        public void actionPerformed (ActionEvent e)
        {
            // Remove the designated language
            final JMenuItem item = (JMenuItem) e.getSource();
            final String    lang = item.getName();
            logger.debug("del lang: {}", lang);

            desired.remove(lang);

            if (companion.isLangInstalled(lang)) {
                nonDesired.add(lang);
                logger.info("Language {} to be removed", lang);
            }

            // This may impact the "installed" status of the companion
            companion.checkInstalled();
            banner.defineLayout(null);
            companion.updateView();
        }

        public final void defineLayout (final String currentLang)
        {
            panel.removeAll();

            final Set<String> relevant = new TreeSet<String>();
            relevant.addAll(desired);
            relevant.addAll(nonDesired);

            final String        gap = "$lcgap";
            final StringBuilder columns = new StringBuilder();

            for (int i = 0; i < relevant.size(); i++) {
                if (columns.length() > 0) {
                    columns.append(",");
                }

                columns.append(gap)
                       .append(",")
                       .append("pref");
            }

            final CellConstraints cst = new CellConstraints();
            final FormLayout   layout = new FormLayout(
                columns.toString(),
                "center:16dlu");
            final PanelBuilder builder = new PanelBuilder(layout, panel);
            PanelBuilder.setOpaqueDefault(true);
            builder.background(Color.WHITE);

            int                   col = 2;

            for (String lang : relevant) {
                final FormLayout langLayout = new FormLayout(
                    "$lcgap,center:pref,$lcgap",
                    "center:12dlu");
                final JPanel     comp = new JPanel();
                comp.setBackground(getBackground(lang, currentLang));

                final PanelBuilder langBuilder = new PanelBuilder(
                    langLayout,
                    comp);
                langBuilder.addROLabel(lang, CC.xy(2, 1));
                comp.addMouseListener(createPopupListener(lang));
                comp.setToolTipText("Use right-click to remove this language");

                builder.add(comp, cst.xy(col, 1));
                col += 2;
            }

            panel.revalidate();
            panel.repaint();
        }

        public JComponent getComponent ()
        {
            return scrollPane;
        }

        protected Color getBackground (final String language,
                                       final String current)
        {
            if (language.equals(current)) {
                return CompanionView.COLORS.BEING;
            }

            if (companion.isLangInstalled(language)) {
                if (desired.contains(language)) {
                    return CompanionView.COLORS.INST;
                } else {
                    return CompanionView.COLORS.UNUSED;
                }
            } else {
                return CompanionView.COLORS.NOT_INST;
            }
        }

        private MouseListener createPopupListener (String lang)
        {
            // Create a very simple popup menu.
            final JPopupMenu popup = new JPopupMenu();
            final JMenuItem  menuItem = new JMenuItem("Remove " + lang);
            menuItem.setName(lang);
            menuItem.addActionListener(this);
            popup.add(menuItem);

            return new PopupListener(popup);
        }
    }

    //---------------//
    // PopupListener //
    //---------------//
    private class PopupListener
        implements MouseListener
    {
        //~ Instance fields ----------------------------------------------------

        JPopupMenu popup;

        //~ Constructors -------------------------------------------------------

        PopupListener (JPopupMenu popupMenu)
        {
            popup = popupMenu;
        }

        //~ Methods ------------------------------------------------------------

        @Override
        public void mouseClicked (MouseEvent e)
        {
        }

        @Override
        public void mouseEntered (MouseEvent e)
        {
        }

        @Override
        public void mouseExited (MouseEvent e)
        {
        }

        @Override
        public void mousePressed (MouseEvent e)
        {
            maybeShowPopup(e);
        }

        @Override
        public void mouseReleased (MouseEvent e)
        {
            maybeShowPopup(e);
        }

        private void maybeShowPopup (MouseEvent e)
        {
            if (e.isPopupTrigger()) {
                popup.show(e.getComponent(), e.getX(), e.getY());
            }
        }
    }
}
