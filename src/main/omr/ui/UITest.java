//-----------------------------------------------------------------------//
//                                                                       //
//                              U I T e s t                              //
//                                                                       //
//  Copyright (C) Herve Bitteur 2000-2007. All rights reserved.          //
//  This software is released under the terms of the GNU General Public  //
//  License. Please contact the author at herve.bitteur@laposte.net      //
//  to report bugs & suggestions.                                        //
//-----------------------------------------------------------------------//
package omr.ui;

import omr.glyph.Glyph;

import omr.selection.Selection;
import omr.selection.SelectionTag;

import omr.sheet.PixelRectangle;
import omr.sheet.Sheet;
import omr.sheet.SheetManager;
import omr.sheet.SystemInfo;

import omr.util.Logger;

import java.awt.Rectangle;

/**
 * A utility class, just used for small test action triggered from UI
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class UITest
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(UITest.class);

    //~ Constructors -----------------------------------------------------------

    private UITest ()
    {
    }

    //~ Methods ----------------------------------------------------------------

    public static void test ()
    {
        Sheet sheet = SheetManager.getSelectedSheet();

        for (SystemInfo system : sheet.getSystems()) {
            for (Glyph glyph : system.getGlyphs()) {
                if (!glyph.isActive()) {
                    logger.warning("Inactive " + glyph + " in " + system);
                }
            }
        }

        Selection      rectSelection = sheet.getSelection(
            SelectionTag.SHEET_RECTANGLE);

        Rectangle      r = (Rectangle) rectSelection.getEntity();
        PixelRectangle rect = new PixelRectangle(r.x, r.y, r.width, r.height);
        SystemInfo     system = sheet.getSystemAtY(rect.y);

        System.out.println(
            "\n" + system.getVerticalSections().size() + " Sections " + system +
            " " + rect);
        system.dumpSections(rect);
        System.out.println(
            "\n" + system.getGlyphs().size() + " Glyphs " + system + " " +
            rect);
        system.dumpGlyphs(rect);
        System.out.println();
    }
}
