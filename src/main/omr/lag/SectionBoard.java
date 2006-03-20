//-----------------------------------------------------------------------//
//                                                                       //
//                        S e c t i o n B o a r d                        //
//                                                                       //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.          //
//  This software is released under the terms of the GNU General Public  //
//  License. Please contact the author at herve.bitteur@laposte.net      //
//  to report bugs & suggestions.                                        //
//-----------------------------------------------------------------------//

package omr.lag;

import omr.stick.StickSection;
import omr.ui.*;
import omr.ui.field.LIntegerField;
import omr.ui.util.Panel;
import omr.util.Logger;

import com.jgoodies.forms.builder.*;
import com.jgoodies.forms.layout.*;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;

/**
 * Class <code>SectionBoard</code> defines a board dedicated to the display
 * of {@link omr.lag.Section} and {@link omr.lag.Run} information, it can
 * also be used as an input means by directly entering the section id.
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class SectionBoard
    extends Board
    implements SectionObserver
{
    //~ Static variables/initializers -------------------------------------

    private static final Logger logger = Logger.getLogger(SectionBoard.class);

    //~ Instance variables ------------------------------------------------

    // Run
    private JPanel runPanel;
    private final LIntegerField rStart = new LIntegerField
        (false, "Start", "Pixel coordinate at start of run");
    private final LIntegerField rLength = new LIntegerField
        (false, "Length", "Length of run in pixels");
    private final LIntegerField rLevel = new LIntegerField
        (false, "Level", "Average pixel level on this run");

    // Section
    private Section section;
    private JPanel sectionPanel;
    private final JButton dump = new JButton("Dump");
    private final JSpinner id = new JSpinner();

    // Output for plain Section
    private final LIntegerField x = new LIntegerField
        (false, "X", "Left abscissa in pixels");
    private final LIntegerField y = new LIntegerField
        (false, "Y", "Top ordinate in pixels");
    private final LIntegerField width = new LIntegerField
        (false, "Width", "Horizontal width in pixels");
    private final LIntegerField height = new LIntegerField
        (false, "Height", "Vertical height in pixels");
    private final LIntegerField weight = new LIntegerField
        (false, "Weight", "Number of pixels in this section");

    // Additional output for StickSection
    private final LIntegerField layer = new LIntegerField
        (false, "Layer", "Layer number for this stick section");
    private final LIntegerField direction = new LIntegerField
        (false, "Dir", "Direction from the stick core");
    private final JTextField role = new JTextField();

    // Section Focus if any
    private SectionFocus sectionFocus;

    // To differentiate between an id as selected from the id spinner
    // (which triggers a focus on related section), and the simple display
    // of section info (with no explicit section focus)
    protected volatile boolean focusWanted = true;

    //~ Constructors ------------------------------------------------------

    //--------------//
    // SectionBoard //
    //--------------//
    /**
     * Create a Section Board
     *
     * @param maxSectionId the upper bound for section id
     */
    public SectionBoard (int maxSectionId)
    {
        this(new SpinnerNumberModel(0, 0, maxSectionId, 1));
    }

    //--------------//
    // SectionBoard //
    //--------------//
    /**
     * Create a Section Board with a specific model for the section id
     *
     * @param model the specific id model
     */
    public SectionBoard (SpinnerModel model)
    {
        super(Board.Tag.SECTION);

        // Dump button
        dump.setToolTipText("Dump this section");
        dump.addActionListener
            (new ActionListener()
                {
                    public void actionPerformed (ActionEvent e)
                    {
                        if (section != null) {
                            section.dump();
                        }
                    }
                });
        dump.setEnabled(section != null);

        // ID Spinner
        id.setToolTipText("General spinner for any glyph id");
        id.addChangeListener
            (new ChangeListener()
                {
                    public void stateChanged(ChangeEvent e)
                    {
                        if (sectionFocus != null) {
                            if (focusWanted) {
                                int sectionId = (Integer) id.getValue();
                                if (logger.isFineEnabled()) {
                                    logger.fine("sectionId=" + sectionId);
                                }
                                if (sectionId != NO_VALUE) {
                                    sectionFocus.setFocusSection(sectionId);
                                }
                            }
                        }
                    }
                });
        id.setModel(model);

        // Role
        role.setEditable(false);
        role.setHorizontalAlignment(JTextField.CENTER);
        role.setToolTipText
            ("Role in the composition of the containing stick");

        defineLayout();
    }

    //~ Methods -----------------------------------------------------------

    //--------------//
    // defineLayout //
    //--------------//
    private void defineLayout()
    {
        FormLayout layout = new FormLayout
            ("pref",
             "pref," + Panel.getPanelInterline() + "," +
             "pref");

        PanelBuilder builder = new PanelBuilder(layout, getComponent());
        builder.setDefaultDialogBorder();

        CellConstraints cst = new CellConstraints();

        int r = 1;                      // --------------------------------
        runPanel = getRunPanel();
        builder.add(runPanel,           cst.xy (1,  r));

        r += 2;                         // --------------------------------
        sectionPanel = getSectionPanel();
        builder.add(sectionPanel,       cst.xy (1,  r));
    }

    //-------------//
    // getRunPanel //
    //-------------//
    private JPanel getRunPanel()
    {
        FormLayout layout = Panel.makeFormLayout(2, 3);

        Panel panel = new Panel();
        panel.setNoInsets();
        PanelBuilder builder = new PanelBuilder(layout, panel);
        builder.setDefaultDialogBorder();

        CellConstraints cst = new CellConstraints();

        int r = 1;                      // --------------------------------
        builder.addSeparator("Run",     cst.xyw(1,  r, 11));

        r += 2;                         // --------------------------------
        builder.add(rStart.getLabel(),  cst.xy (1,  r));
        builder.add(rStart.getField(),  cst.xy (3,  r));

        builder.add(rLength.getLabel(), cst.xy (5,  r));
        builder.add(rLength.getField(), cst.xy (7,  r));

        builder.add(rLevel.getLabel(),  cst.xy (9,  r));
        builder.add(rLevel.getField(),  cst.xy (11, r));

        return builder.getPanel();
    }

    //-----------------//
    // getSectionPanel //
    //-----------------//
    private JPanel getSectionPanel()
    {
        FormLayout layout = Panel.makeFormLayout(4, 3);

        Panel panel = new Panel();
        panel.setNoInsets();
        PanelBuilder builder = new PanelBuilder(layout, panel);
        builder.setDefaultDialogBorder();

        CellConstraints cst = new CellConstraints();

        int r = 1;                      // --------------------------------
        builder.addSeparator("Section", cst.xyw(1,  r, 9));
        builder.add(dump,               cst.xy (11, r));

        r += 2;                         // --------------------------------
        builder.addLabel("Id",          cst.xy (1,  r));
        builder.add(id,                 cst.xy (3,  r));

        builder.add(x.getLabel(),       cst.xy (5,  r));
        builder.add(x.getField(),       cst.xy (7,  r));

        builder.add(width.getLabel(),   cst.xy (9,  r));
        builder.add(width.getField(),   cst.xy (11, r));

        r += 2;                         // --------------------------------
        builder.add(weight.getLabel(),  cst.xy (1,  r));
        builder.add(weight.getField(),  cst.xy (3,  r));

        builder.add(y.getLabel(),       cst.xy (5,  r));
        builder.add(y.getField(),       cst.xy (7,  r));

        builder.add(height.getLabel(),  cst.xy (9,  r));
        builder.add(height.getField(),  cst.xy (11, r));

        r += 2;                         // --------------------------------
        builder.add(layer.getLabel(),   cst.xy (1,  r));
        builder.add(layer.getField(),   cst.xy (3,  r));

        builder.add(direction.getLabel(), cst.xy (5,  r));
        builder.add(direction.getField(), cst.xy (7,  r));

        builder.add(role,               cst.xyw(9, r, 3));

        return builder.getPanel();
    }

    //-----------------//
    // setSectionFocus //
    //-----------------//
    /**
     * Connect an entity to be later notified of section focus, as input by
     * a user (when a section ID is entered)
     *
     * @param sectionFocus
     */
    public void setSectionFocus (SectionFocus sectionFocus)
    {
        this.sectionFocus = sectionFocus;
    }

    //--------//
    // update //
    //--------//
    public void update (Section section)
    {
        this.section = section;
        dump.setEnabled(section != null);

        focusWanted = false;
        emptyFields(sectionPanel);
        emptyFields(runPanel);

        if (section == null) {
            id.setValue(NO_VALUE);
        } else {
            id.setValue(section.getId());

            Rectangle box = section.getContourBox();
            x.setValue(box.x);
            y.setValue(box.y);
            width.setValue(box.width);
            height.setValue(box.height);
            weight.setValue(section.getWeight());

            if (section instanceof StickSection) {
                StickSection ss = (StickSection) section;

                layer.setValue(ss.layer);
                direction.setValue(ss.direction);

                if (ss.role != null) {
                    role.setText(ss.role.toString());
                }
            }
        }
        focusWanted = true;
    }

    //--------//
    // update //
    //--------//
    public void update (Run run)
    {
        if (run != null) {
            rStart.setValue(run.getStart());
            rLength.setValue(run.getLength());
            rLevel.setValue(run.getLevel());
        } else {
            emptyFields(runPanel);
        }
    }
}
