//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                             J a x b                                            //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.util;

import java.nio.file.Path;
import java.nio.file.Paths;

import javax.xml.bind.annotation.adapters.XmlAdapter;

/**
 * Class {@code Jaxb}
 *
 * @author Hervé Bitteur
 */
public abstract class Jaxb
{
    //~ Inner Classes ------------------------------------------------------------------------------

    //------------------//
    // SequencesAdapter //
    //------------------//
    /**
     * Meant for customized JAXB support of sequences.
     */
    public static class PathAdapter
            extends XmlAdapter<String, Path>
    {
        //~ Methods --------------------------------------------------------------------------------

        @Override
        public String marshal (Path path)
                throws Exception
        {
            return path.toString();
        }

        @Override
        public Path unmarshal (String str)
        {
            return Paths.get(str);
        }
    }
}
