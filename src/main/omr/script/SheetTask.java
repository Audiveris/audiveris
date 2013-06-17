//----------------------------------------------------------------------------//
//                                                                            //
//                             S h e e t T a s k                              //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.script;

import omr.sheet.Sheet;

import javax.xml.bind.annotation.XmlAttribute;

/**
 * Class {@code SheetTask} is a {@link ScriptTask} that applies to a
 * given sheet.
 *
 * @author Hervé Bitteur
 */
public abstract class SheetTask
    extends ScriptTask
{
    //~ Instance fields --------------------------------------------------------

    /** Page index */
    @XmlAttribute(name = "page")
    protected Integer page;

    /** The related sheet */
    protected Sheet sheet;

    //~ Constructors -----------------------------------------------------------

    //-----------//
    // SheetTask //
    //-----------//
    /**
     * Creates a new SheetTask object.
     */
    protected SheetTask (Sheet sheet)
    {
        page = sheet.getPage()
                    .getIndex();
    }

    //-----------//
    // SheetTask //
    //-----------//
    /** No-arg constructor for JAXB only */
    protected SheetTask ()
    {
    }

    //~ Methods ----------------------------------------------------------------

    //--------------//
    // getPageIndex //
    //--------------//
    /**
     * Report the id of the page/sheet if any
     * @return the sheet index (counted from 1) or null if none
     */
    public Integer getPageIndex ()
    {
        return page;
    }

    //-----------------//
    // internalsString //
    //-----------------//
    @Override
    protected String internalsString ()
    {
        StringBuilder sb = new StringBuilder(super.internalsString());

        if (page != null) {
            sb.append(" page#")
              .append(page);
        }

        return sb.toString();
    }
}
