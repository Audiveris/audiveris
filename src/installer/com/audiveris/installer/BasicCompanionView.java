//----------------------------------------------------------------------------//
//                                                                            //
//                     B a s i c C o m p a n i o n V i e w                    //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package com.audiveris.installer;

import com.audiveris.installer.Companion.Need;
import com.audiveris.installer.Companion.Status;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * Class {@code BasicCompanionView} is the basis for a View on a
 * Companion.
 *
 * @author Hervé Bitteur
 */
public class BasicCompanionView
    implements CompanionView, ItemListener
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(
        BasicCompanionView.class);

    /** Height of companion rectangle. */
    protected static final int HEIGHT = 40;

    //~ Instance fields --------------------------------------------------------

    /** Related companion. */
    protected final Companion companion;

    /** Preferred width. */
    private final int width;

    /** Actual Swing component. */
    protected final JComponent component;

    /** Title. */
    protected final JLabel titleLabel = new JLabel("");

    /** Need. */
    protected final JCheckBox needBox = new JCheckBox("");

    //~ Constructors -----------------------------------------------------------

    //--------------------//
    // BasicCompanionView //
    //--------------------//
    /**
     * Creates a new BasicCompanionView object.
     *
     * @param companion the related companion
     */
    public BasicCompanionView (Companion companion,
                               int       width)
    {
        this.companion = companion;
        this.width = width;

        titleLabel.setText(companion.getIndex() + ") " + companion.getTitle());

        component = defineLayout();
        component.setToolTipText(companion.getDescription());
        needBox.addItemListener(this);
    }

    //~ Methods ----------------------------------------------------------------

    //--------------//
    // getCompanion //
    //--------------//
    @Override
    public Companion getCompanion ()
    {
        return companion;
    }

    //--------------//
    // getComponent //
    //--------------//
    @Override
    public JComponent getComponent ()
    {
        return component;
    }

    //------------------//
    // itemStateChanged //
    //------------------//
    @Override
    public void itemStateChanged (ItemEvent evt)
    {
        // The need checkbox has changed
        if (evt.getStateChange() == ItemEvent.SELECTED) {
            companion.setNeed(Need.SELECTED);
        } else {
            companion.setNeed(Need.NOT_SELECTED);
        }

        update();
    }

    //--------//
    // update //
    //--------//
    @Override
    public void update ()
    {
        // Need
        switch (companion.getNeed()) {
        case MANDATORY :
            needBox.setEnabled(false);
            needBox.setSelected(true);

            break;

        case SELECTED :
            needBox.setEnabled(true);
            needBox.setSelected(true);

            break;

        case NOT_SELECTED :
            needBox.setEnabled(true);
            needBox.setSelected(false);

            break;
        }

        // Status
        component.setBackground(
            getBackground(companion.getStatus(), companion.getNeed()));
        component.repaint();
    }

    //---------------//
    // getBackground //
    //---------------//
    protected Color getBackground (Status status,
                                   Need   need)
    {
        switch (companion.getStatus()) {
        case NOT_INSTALLED :

            if (need != Need.NOT_SELECTED) {
                return COLORS.NOT_INST;
            } else {
                return COLORS.UNUSED;
            }

        case BEING_INSTALLED :
        case BEING_UNINSTALLED :
            return COLORS.BEING;

        case INSTALLED :

            if (need != Need.NOT_SELECTED) {
                return COLORS.INST;
            } else {
                return COLORS.UNUSED;
            }

        case FAILED_TO_INSTALL :
        case FAILED_TO_UNINSTALL :default :
            return COLORS.FAILED;
        }
    }

    //--------------//
    // defineLayout //
    //--------------//
    private JPanel defineLayout ()
    {
        // Prepare layout elements
        final boolean         optional = companion.getNeed() != Need.MANDATORY;
        final CellConstraints cst = new CellConstraints();
        final String          colSpec = optional ? "pref,1dlu,center:pref"
                                        : "center:pref";
        final FormLayout      layout = new FormLayout(colSpec, "center:20dlu");
        final JPanel          panel = new MyPanel();
        final PanelBuilder    builder = new PanelBuilder(layout, panel);

        // Now add the desired components, using provided order
        if (optional) {
            builder.add(needBox, cst.xy(1, 1));
            builder.add(titleLabel, cst.xy(3, 1));
        } else {
            builder.add(titleLabel, cst.xy(1, 1));
        }

        panel.setPreferredSize(new Dimension(width, HEIGHT));
        panel.setOpaque(true);

        return panel;
    }

    //~ Inner Classes ----------------------------------------------------------

    private static class MyPanel
        extends JPanel
    {
        //~ Static fields/initializers -----------------------------------------

        private static final Insets insets = new Insets(3, 6, 3, 6);

        //~ Methods ------------------------------------------------------------

        @Override
        public Insets getInsets ()
        {
            return insets;
        }
    }
}
