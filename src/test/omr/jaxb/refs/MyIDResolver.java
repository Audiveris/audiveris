//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     M y I D R e s o l v e r                                    //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.jaxb.refs;

import com.sun.xml.internal.bind.IDResolver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import javax.xml.bind.Unmarshaller;

/**
 * Class {@code MyIDResolver}.
 * DOES NOT WORK CORRECTLY
 *
 * @author Hervé Bitteur
 */
public class MyIDResolver
        extends IDResolver
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(MyIDResolver.class);

    //~ Instance fields ----------------------------------------------------------------------------
    Map<String, Apple> apples = new HashMap<String, Apple>();

    Map<String, Orange> oranges = new HashMap<String, Orange>();

    //~ Methods ------------------------------------------------------------------------------------
    @Override
    public void bind (String id,
                      Object obj)
    {
        logger.info("bind id:{} obj:{}", id, obj);

        if (obj instanceof Apple) {
            apples.put(id, (Apple) obj);
        } else if (obj instanceof Orange) {
            oranges.put(id, (Orange) obj);
        } else {
            logger.warn("bind. Unknown type for " + obj);
        }
    }

    @Override
    public Callable resolve (final String id,
                             final Class targetType)
    {
        // targetType is always Object !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
        logger.info("resolve id:{} targetType:{}", id, targetType);

        return new Callable()
        {
            @Override
            public Object call ()
            {
                logger.info("resolve.Callable.call id:{} targetType:{}", id, targetType);

                if (targetType == Apple.class) {
                    return apples.get(id);
                } else if (targetType == Orange.class) {
                    return oranges.get(id);
                } else {
                    logger.warn("resolve. Unsupported type" + targetType);

                    return null;
                }
            }
        };
    }

    Unmarshaller.Listener createListener ()
    {
        return new Unmarshaller.Listener()
        {
            @Override
            public void beforeUnmarshal (Object target,
                                         Object parent)
            {
                if (target instanceof Basket) {
                    apples = new HashMap<String, Apple>();
                    oranges = new HashMap<String, Orange>();
                }
            }
        };
    }

    void startDocument ()
    {
        apples.clear();
        oranges.clear();
    }
}
//class MyIDResolver extends IDResolver {
//    Map<String,Apple> apples = new HashMap<String,Apple>();
//    Map<String,Orange> oranges = new HashMap<String,Orange>();
//    Unmarshaller.Listener createListener() {
//        return new Unmarshaller.Listener() {
//            void beforeUnmarshal(Object target, Object parent) {
//                if(target instanceof Box) {
//                    apples = new HashMap<String,Apple>();
//                    oranges = new HashMap<String,Orange>();
//                }
//            }
//        }
//                }
//    void startDocument() {
//        apples.clear();
//        oranges.clear();
//    }
//    public void bind(String id, Object obj) {
//        if(obj instanceof Apple)
//            apples.put(id,(Apple)obj);
//        else
//            oranges.put(id,(Orange)obj);
//    }
//
//    Callable resolve(final String id, Class targetType) {
//        return new Callable() {
//            public Object call() throws Exception {
//                if(targetType==Apple.class)
//                    return apples.get(id);
//                else
//                    return oranges.get(id);
//            }
//        };
//    }
//}
