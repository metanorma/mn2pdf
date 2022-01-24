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
import org.metanorma.utils.PropertiesReader;


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
            
            try {
                
                PropertiesReader propertiesReader = new PropertiesReader("properties-from-pom.properties"); 
                String highlightis_version = propertiesReader.getProperty("highlightis.version");

                String folder_highlightjs = String.join("/", "highlightjs", "cdn-release-" + highlightis_version, "build");

                List<String> jsModules = new ArrayList<>();
                jsModules.add(folder_highlightjs + "/highlight.min.js"); // this module should be first
                
                List<String> resourceFolderFiles = Util.getResourceFolderFiles(folder_highlightjs + "/languages");
                resourceFolderFiles.stream().filter(f -> (f.endsWith(".js") &&
                        !(f.contains("/bash.min.js") || f.contains("/c.min.js") || f.contains("/cpp.min.js") ||
                          f.contains("/csharp.min.js") || f.contains("/css.min.js") || f.contains("/diff.min.js") ||
                        f.contains("/go.min.js") || f.contains("/ini.min.js") || f.contains("/java.min.js") ||
                        f.contains("/javascript.min.js") || f.contains("/json.min.js") || f.contains("/kotlin.min.js") ||
                        f.contains("/less.min.js") || f.contains("/lua.min.js") || f.contains("/makefile.min.js") ||
                        f.contains("/markdown.min.js") || f.contains("/objectivec.min.js") || f.contains("/perl.min.js") ||
                        f.contains("/php-template.min.js") || f.contains("/php.min.js") || f.contains("/plaintext.min.js") || 
                        f.contains("/python-repl.min.js") || f.contains("/python.min.js") || f.contains("/r.min.js") || 
                        f.contains("/ruby.min.js") || f.contains("/rust.min.js") || f.contains("/scss.min.js") || 
                        f.contains("/shell.min.js") || f.contains("/sql.min.js") || f.contains("/swift.min.js") || 
                        f.contains("/typescript.min.js") || f.contains("/vbnet.min.js") || f.contains("/xml.min.js") || 
                        f.contains("/yaml.min.js") ))).forEach(jsModules::add);

                for(String jsModule:jsModules) {
                    if (DEBUG) {
                        logger.log(Level.INFO, "Loading \"highlight.js\" module ''{0}''", jsModule);
                    }
                    InputStream jsIS = Util.getStreamFromResources(GraalJSEngine.class.getClassLoader(), jsModule);
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
