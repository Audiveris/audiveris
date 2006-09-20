//----------------------------------------------------------------------------//
//                                                                            //
//                          S e c t i o n B o a r d                           //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.               //
//  This software is released under the terms of the GNU General Public       //
//  License. Please contact the author at herve.bitteur@laposte.net           //
//  to report bugs & suggestions.                                             //
//----------------------------------------------------------------------------//
//
package omr.lag;

import omr.selection.Selection;
import omr.selection.SelectionHint;

import omr.stick.StickSection;

import omr.ui.Board;
import omr.ui.field.LIntegerField;
import static omr.ui.field.SpinnerUtilities.*;
import omr.ui.util.Panel;

import omr.util.Logger;

import com.jgoodies.forms.builder.*;
import com.jgoodies.forms.layout.*;

import java.awt.*;
import java.awt.event.*;
import java.util.Collections;

import javax.swing.*;
import javax.swing.event.*;

/**
 * Class <code>SectionBoard</code> defines a board dedicated to the display of
 * {@link omr.lag.Section} and {@link omr.lag.Run} information, it can also be
 * used as an input means by directly entering the section id in the proper Id
 * spinner.
 *
 * <dl>
 * <dt><b>Selection Inputs:</b></dt><ul>
 * <li>*_SECTION
 * </ul>
 *
 * <dt><b>Selection Outputs:</b></dt><ul>
 * <li>*_SECTION_ID (flagged with SECTION_INIT hint)
 * </ul>
 * </dl>
 *
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class SectionBoard
    extends Board
{
    //~ Static fields/initializers ---------------------------------------------

    private static final Logger logger = Logger.getLogger(SectionBoard.class);

    //~ Instance fields --------------------------------------------------------

    // Section input devices
    private final JButton       dump = new JButton("Dump");
    private final JSpinner      id = new JSpinner();
    private final JTextField    role = new JTextField();
    private final LIntegerField direction = new LIntegerField(
        false,
        "Dir",
        "Direction from the stick core");
    private final LIntegerField height = new LIntegerField(
        false,
        "Height",
        "Vertical height in pixels");

    // Additional output for StickSection
    private final LIntegerField layer = new LIntegerField(
        false,
        "Layer",
        "Layer number for this stick section");
    private final LIntegerField weight = new LIntegerField(
        false,
        "Weight",
        "Number of pixels in this section");
    private final LIntegerField width = new LIntegerField(
        false,
        "Width",
        "Horizontal width in pixels");

    // Output for plain Section
    private final LIntegerField x = new LIntegerField(
        false,
        "X",
        "Left abscissa in pixels");
    private final LIntegerField y = new LIntegerField(
        false,
        "Y",
        "Top ordinate in pixels");
    private boolean             idSelecting = false;

    // To avoid loop, indicate that update() method id being processed
    private boolean updating = false;

    //~ Constructors -----------------------------------------------------------

    //--------------//
    // SectionBoard //
    //--------------//
    /**
     * Create a Section Board
     *
     * @param unitName name for the owning unit
     * @param maxSectionId the upper bound for section id
     * @param inputSelection the selection for section input
     * @param outputSelection the selection for section id output
     */
    public SectionBoard (String    unitName,
                         int       maxSectionId,
                         Selection inputSelection,
                         Selection outputSelection)
    {
        super(Board.Tag.SECTION, unitName + "-SectionBoard");

        // Dependencies on Selections
        setOutputSelection(outputSelection);
        setInputSelectionList(Collections.singletonList(inputSelection));

        // Dump button
        dump.setToolTipText("Dump this section");
        dump.addActionListener(
            new ActionListener() {
                    public void actionPerformed (ActionEvent e)
                    {
                        // Retrieve current section selection
                        Selection input = inputSelectionList.get(0);
                        Section   section = (Section) input.getEntity();

                        if (section != null) {
                            section.dump();
                        }
                    }
                });
        dump.setEnabled(false); // Until a section selection is made

        // ID Spinner
        id.setToolTipText("General spinner for any glyph id");
        id.addChangeListener(
            new ChangeListener() {
                    public void stateChanged (ChangeEvent e)
                    {
                        // Make sure this new Id value is due to user
                        // action on an Id spinner, and not the mere update
                        // of section fields (which include this id).
                        if (!updating) {
                            Selection output = SectionBoard.this.outputSelection;

                            if (output != null) {
                                Integer sectionId = (Integer) id.getValue();

                                if (logger.isFineEnabled()) {
                                    logger.fine("sectionId=" + sectionId);
                                }

                                idSelecting = true;
                                output.setEntity(
                                    sectionId,
                                    SelectionHint.SECTION_INIT);
                                idSelecting = false;
                            }
                        }
                    }
                });
        id.setModel(new SpinnerNumberModel(0, 0, maxSectionId, 1));

        // Role
        role.setEditable(false);
        role.setHorizontalAlignment(JTextField.CENTER);
        role.setToolTipText("Role in the composition of the containing stick");

        // Component layout
        defineLayout();
    }

    //~ Methods ----------------------------------------------------------------

    //--------//
    // update //
    //--------//
    /**
     * Call-back triggered when Section Selection has been modified
     *
     * @param selection the (Section) Selection
     * @param hint potential notification hint
     */
    public void update (Selection     selection,
                        SelectionHint hint)
    {
        Object entity = selection.getEntity();

        if (logger.isFineEnabled()) {
            logger.fine("SectionBoard " + selection.getTag() + ": " + entity);
        }

        switch (selection.getTag()) {
        case SKEW_SECTION : // Section of initial skewed lag
        case HORIZONTAL_SECTION : // Section of horizontal lag
        case VERTICAL_SECTION : // Section of vertical lag

            if (updating) {
                ///logger.warning("double updating");
                return;
            }

            // Update section fields in this board
            updating = true;

            Section section = (Section) entity;
            dump.setEnabled(section != null);

            Integer sectionId = null;

            if (idSelecting) {
                sectionId = (Integer) id.getValue();
            }

            emptyFields(getComponent());

            if (section == null) {
                // If the user is currently using the Id spinner, make sure we
                // display the right Id value in the spinner, even if there is
                // no corresponding section
                if (idSelecting) {
                    id.setValue(sectionId);
                } else {
                    id.setValue(NO_VALUE);
                }
            } else {
                // We have a valid section, let's display its fields
                id.setValue(section.getId());

                Rectangle box = section.getContourBox();
                x.setValue(box.x);
                y.setValue(box.y);
                width.setValue(box.width);
                height.setValue(box.height);
                weight.setValue(section.getWeight());

                // Additional fields for a StickSection
                if (section instanceof StickSection) {
                    StickSection ss = (StickSection) section;
                    layer.setValue(ss.layer);
                    direction.setValue(ss.direction);

                    if (ss.role != null) {
                        role.setText(ss.role.toString());
                    }
                }
            }

            updating = false;

            break;

        default :
            logger.severe("Unexpected selection event from " + selection);
        }
    }

    //--------------//
    // defineLayout //
    //--------------//
    private void defineLayout ()
    {
        FormLayout   layout = Panel.makeFormLayout(4, 3);
        PanelBuilder builder = new PanelBuilder(layout, getComponent());
        builder.setDefaultDialogBorder();

        CellConstraints cst = new CellConstraints();

        int             r = 1; // --------------------------------
        builder.addSeparator("Section", cst.xyw(1, r, 9));
        builder.add(dump, cst.xy(11, r));

        r += 2; // --------------------------------
        builder.addLabel("Id", cst.xy(1, r));
        builder.add(id, cst.xy(3, r));

        builder.add(x.getLabel(), cst.xy(5, r));
        builder.add(x.getField(), cst.xy(7, r));

        builder.add(width.getLabel(), cst.xy(9, r));
        builder.add(width.getField(), cst.xy(11, r));

        r += 2; // --------------------------------
        builder.add(weight.getLabel(), cst.xy(1, r));
        builder.add(weight.getField(), cst.xy(3, r));

        builder.add(y.getLabel(), cst.xy(5, r));
        builder.add(y.getField(), cst.xy(7, r));

        builder.add(height.getLabel(), cst.xy(9, r));
        builder.add(height.getField(), cst.xy(11, r));

        r += 2; // --------------------------------
        builder.add(layer.getLabel(), cst.xy(1, r));
        builder.add(layer.getField(), cst.xy(3, r));

        builder.add(direction.getLabel(), cst.xy(5, r));
        builder.add(direction.getField(), cst.xy(7, r));

        builder.add(role, cst.xyw(9, r, 3));
    }
}
