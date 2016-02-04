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
 * families of IDREF in a sheet, each with its own scope: one for Inter instances and
 * one for Glyph instances.
 * <p>
 * NOTA: this class extends com.sun.xml.internal.bind.IDResolver which is not publicly available
 * though advertised by JAXB.
 * <p>
 * Compiler javac does not link against rt.jar but uses lib/ct.sym symbol which does not reference
 * this class.
 * <p>
 * Two workarounds are possible until a better JAXB customization feature is provided: <ul>
 * <li>Explicitly include rt.jar in the compile dependencies, or
 * <li>Use -XDignore.symbol.file as javac option
 * </ul>
 *
 * @see http://stackoverflow.com/questions/4065401/using-internal-sun-classes-with-javac
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
    //------//
    // bind //
    //------//
    /**
     * Binds the given object to the specified ID.
     *
     * @param id  The ID value found in the document being unmarshalled. Always non-null.
     * @param obj The object being unmarshalled which is going to own the ID. Always non-null.
     * @throws SAXException
     */
    @Override
    public void bind (String id,
                      Object obj)
            throws SAXException
    {
        logger.info("\n*** bind id:{} obj:{}", id, obj);

        if (obj instanceof BasicGlyph) {
            glyphMap.put(id, obj);
        } else {
            interMap.put(id, obj);
        }
    }

    //---------//
    // resolve //
    //---------//
    /**
     * Obtains the object to be pointed by the IDREF value.
     *
     * @param id         The IDREF value found in the document being unmarshalled. Always non-null.
     * @param targetType The expected type to which ID resolves to.
     *                   JAXB infers this information from the signature of the fields that has
     *                   {@link javax.xml.bind.annotation.XmlIDREF}.
     *                   When a property is a collection, this parameter will be the type of the
     *                   individual item in the collection.
     * @return null if the implementation is sure that the parameter combination will never yield a
     *         valid object. Otherwise non-null.
     * @throws SAXException
     */
    @Override
    public Callable<?> resolve (final String id,
                                final Class targetType)
            throws SAXException
    {
        return new Callable()
        {
            @Override
            public Object call ()
            {
                logger.info("\nresolve.Callable.call id:{} type:{}", id, targetType.getName());

                final Object obj;

                // For glyph, we do   get type = BasicGlyph class
                // For inter, we only get type = Object class
                if (BasicGlyph.class.isAssignableFrom(targetType)) {
                    obj = glyphMap.get(id);
                } else {
                    obj = interMap.get(id);
                }

                if (obj != null) {
                    logger.warn("\ngot id:{} {}", id, obj);
                }

                return obj;
            }
        };
    }
}
