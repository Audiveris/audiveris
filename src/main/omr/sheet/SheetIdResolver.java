//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                  S h e e t I d R e s o l v e r                                 //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet;

import omr.glyph.BasicGlyph;

import com.sun.xml.internal.bind.IDResolver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.xml.sax.SAXException;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * Class {@code SheetIdResolver} is a custom JAXB IDResolver meant to handle two
 * families of IDREF: one for Inter instances and one for Glyph instances.
 *
 * @author Hervé Bitteur
 */
public class SheetIdResolver
        extends IDResolver
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(SheetIdResolver.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** Map of glyph IDs. */
    private final Map<String, Object> glyphMap = new HashMap<String, Object>();

    /** Map of inter IDs. */
    private final Map<String, Object> interMap = new HashMap<String, Object>();

    //~ Methods ------------------------------------------------------------------------------------
    @Override
    public void bind (String id,
                      Object obj)
            throws SAXException
    {
        ///logger.info("\n*** bind id:{} obj:{}", id, obj);
        if (obj instanceof BasicGlyph) {
            glyphMap.put(id, obj);
        } else {
            interMap.put(id, obj);
        }
    }

    @Override
    public Callable<?> resolve (final String id,
                                final Class type)
            throws SAXException
    {
        return new Callable()
        {
            @Override
            public Object call ()
            {
                ///logger.info("\nresolve.Callable.call id:{} type:{}", id, type.getName());
                final Object obj;

                // For glyph, we do   get type = BasicGlyph class
                // For inter, we only get type = Object class
                if (BasicGlyph.class.isAssignableFrom(type)) {
                    obj = glyphMap.get(id);
                } else {
                    obj = interMap.get(id);
                }

                return obj;
            }
        };
    }
}
