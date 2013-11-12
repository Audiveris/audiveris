//----------------------------------------------------------------------------//
//                                                                            //
//                          V o i d N o t e s S t e p                         //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2013. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.step;

import omr.image.ChamferDistance;
import omr.image.Picture;
import omr.image.PixelFilter;
import omr.image.Table;

import omr.sheet.Sheet;
import omr.sheet.SystemInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

/**
 * Class {@code VoidNotesStep} implements <b>VOID_NOTES</b> step,
 * which use distance matching technique to retrieve all possible
 * interpretations of void note heads or whole notes, as well as
 * black heads left over by {@link BlackNotesStep}.
 *
 * @author Hervé Bitteur
 */
public class VoidNotesStep
        extends AbstractSystemStep
{
    //~ Static fields/initializers ---------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(
            VoidNotesStep.class);

    //~ Constructors -----------------------------------------------------------
    //---------------//
    // VoidNotesStep //
    //---------------//
    /**
     * Creates a new VoidNotesStep object.
     */
    public VoidNotesStep ()
    {
        super(
                Steps.VOID_NOTES,
                Level.SHEET_LEVEL,
                Mandatory.MANDATORY,
                DATA_TAB,
                "Retrieve void note heads & whole notes");
    }

    //~ Methods ----------------------------------------------------------------
    //----------//
    // doSystem //
    //----------//
    @Override
    public void doSystem (SystemInfo system)
            throws StepException
    {
        system.voidNotesBuilder.buildVoidHeads(); // -> Void heads
    }

    //----------//
    // doProlog //
    //----------//
    /**
     * {@inheritDoc}
     * Compute the distance-to-foreground transform image.
     */
    @Override
    protected void doProlog (Collection<SystemInfo> systems,
                             Sheet sheet)
            throws StepException
    {
        Picture picture = sheet.getPicture();
        PixelFilter buffer = (PixelFilter) picture.getSource(
                Picture.SourceKey.BINARY);
        Table table = new ChamferDistance.Short().computeToFore(buffer);
        sheet.setDistanceImage(table);
    }
}
