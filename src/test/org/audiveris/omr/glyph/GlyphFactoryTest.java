/*
 * Copyright © Audiveris 2019. All rights reserved.
 * This software is released under the GNU General Public License.
 * Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
 */
package org.audiveris.omr.glyph;

import static org.audiveris.omr.run.Orientation.HORIZONTAL;

import org.audiveris.omr.run.Run;
import org.audiveris.omr.run.RunTable;

import org.junit.Test;

import java.awt.Dimension;

/**
 *
 * @author Hervé Bitteur
 */
public class GlyphFactoryTest
{

    private static final Dimension dim = new Dimension(15, 10);

    /**
     * Creates a new {@code GlyphFactoryTest} object.
     */
    public GlyphFactoryTest ()
    {
    }

    /**
     * Test of createGlyphs method, of class GlyphFactory.
     */
    @Test
    public void testCreateGlyphs ()
    {
        System.out.println("createGlyphs");

        RunTable runTable = createHorizontalInstance();
        GlyphFactory.buildGlyphs(runTable, null);
    }

    //--------------------------//
    // createHorizontalInstance //
    //--------------------------//
    /**
     * <pre>
     * +===============+   +===============+   +===============+
     * |-XX--XXX---X-X-| 0 |-11--222---3-4-| 0 |-11--222---2-2-|
     * |X--XXX-----X-X-| 1 |5--222-----3-4-| 1 |5--222-----2-2-|
     * |---X-XXXX--XXX-| 2 |---2-2222--333-| 2 |---2-2222--222-|
     * |XX--X---XX-X---| 3 |66--7---22-3---| 3 |22--2---22-2---|
     * |-XXXX-XXXX-X---| 4 |-6666-2222-3---| 4 |-2222-2222-2---|
     * |--XXXXXXX--X---| 5 |--6666666--3---| 5 |--2222222--2---|
     * |------XXXXXXXX-| 6 |------66666666-| 6 |------22222222-|
     * |---------------| 7 |---------------| 7 |---------------|
     * |---------------| 8 |---------------| 8 |---------------|
     * |---------------| 9 |---------------| 9 |---------------|
     * +===============+   +===============+   +===============+
     * </pre>
     */
    private RunTable createHorizontalInstance ()
    {
        RunTable instance = new RunTable(HORIZONTAL, dim.width, dim.height);

        instance.addRun(0, new Run(1, 2));
        instance.addRun(0, new Run(5, 3));
        instance.addRun(0, new Run(11, 1));
        instance.addRun(0, new Run(13, 1));

        instance.addRun(1, new Run(0, 1));
        instance.addRun(1, new Run(3, 3));
        instance.addRun(1, new Run(11, 1));
        instance.addRun(1, new Run(13, 1));

        instance.addRun(2, new Run(3, 1));
        instance.addRun(2, new Run(5, 4));
        instance.addRun(2, new Run(11, 3));

        instance.addRun(3, new Run(0, 2));
        instance.addRun(3, new Run(4, 1));
        instance.addRun(3, new Run(8, 2));
        instance.addRun(3, new Run(11, 1));

        instance.addRun(4, new Run(1, 4));
        instance.addRun(4, new Run(6, 4));
        instance.addRun(4, new Run(11, 1));

        instance.addRun(5, new Run(2, 7));
        instance.addRun(5, new Run(11, 1));

        instance.addRun(6, new Run(6, 8));

        ///System.out.println("createHorizontalInstance:\n" + instance.dumpOf());
        return instance;
    }
}
