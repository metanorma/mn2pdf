package org.metanorma.fop;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import static org.metanorma.Constants.DEBUG;
import static org.metanorma.fop.Util.logger;

/**
 *
 * @author Alexander Dyuzhev
 */
public class GraalJSEngine {
    
    private static GraalJSEngine instance;
    
    private static ScriptEngine graalEngine;
    private static Bindings bindings;
    
    private GraalJSEngine() {}
    
    public static GraalJSEngine getInstance() {
        if (instance == null) {
            // Singleton
            
            if (DEBUG) {
                logger.info("Start GraalJS initialization");
            }
            
            instance = new GraalJSEngine();
            graalEngine = new ScriptEngineManager().getEngineByName("graal.js");
            bindings = graalEngine.getBindings(ScriptContext.ENGINE_SCOPE);
            
            List<String> jsModules = new ArrayList<>();
            jsModules.add("highlight.min.js"); // this module should be first
            
            try {
                List<String> resourceFolderFiles = Util.getResourceFolderFiles("highlightjs");
                resourceFolderFiles.stream().filter(f -> (f.endsWith(".js") && !f.endsWith("highlight.min.js"))).forEach(jsModules::add);

                for(String jsModule:jsModules) {
                    if (DEBUG) {
                        logger.log(Level.INFO, "Loading \"highlight.js\" module ''{0}''", jsModule);
                    }

                    InputStream jsIS = Util.getStreamFromResources(GraalJSEngine.class.getClassLoader(), "highlightjs/" + jsModule);
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(jsIS))) {
                        graalEngine.eval(reader);
                    }
                }
            }
            catch (Exception ex) {
                logger.log(Level.SEVERE, "GraalJS initialization error: ''{0}''", ex.toString());
                ex.printStackTrace();
            }
            
            if (DEBUG) {
                logger.info("End GraalJS initialization");
            }
        }

        return instance;
    }
    
    
    public String eval(String code, String lang) {
        if (DEBUG) {
            logger.info("Start binding");
        }
        bindings.put("code", code);
        if (DEBUG) {
            logger.info("End binding");
        }
        
        try {
            String hljsFunction = "highlightAuto(code)";
            if (lang != null && !lang.isEmpty()) {
                hljsFunction = "highlight(code, {language: '" + lang + "'})";
            }
            if (DEBUG) {
                logger.info("Start highlighting");
            }
            Object result = graalEngine.eval("var result = hljs." + hljsFunction + ".value;" + 
                    "result", bindings);
            if (DEBUG) {
                logger.info("End highlighting");
            }
            String wrapper = "<syntax>" + result.toString() + "</syntax>";
            return wrapper;
        } catch (ScriptException ex) {
            //The message: Could not find the language 'adoc', did you forget to load/include a language module?
            // showed in main code
            
            //logger.log(Level.WARNING, "Can't find highlighting rules for the language '{0}'", lang);
        }
        catch (Exception ex) {
            logger.log(Level.SEVERE, "Can't highlight syntax for the string '{0}''", code);
            ex.printStackTrace();
        }
        
        return "";
    }
    
}
