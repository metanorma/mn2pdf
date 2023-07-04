package org.metanorma.experiments;

import sun.awt.Win32FontManager;
import sun.font.*;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

public class FontReregister {

    public static void main(String[] args) throws IOException, FontFormatException, NoSuchFieldException, IllegalAccessException, InstantiationException {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        System.out.println("FONT LIST BEFORE");

        Font ttfFont = Font.createFont(Font.TRUETYPE_FONT, new File("D:\\Work\\Metanorma\\Fonts\\Cambria_MacOs\\Cambria Math.ttf"));
        String fontName = ttfFont.getFontName();

        boolean result = ge.registerFont(ttfFont);
        System.out.println("Font register 1st attempt result=" + result);

        FontManager fm = FontManagerFactory.getInstance();

        // just for allFonts initialization
        //boolean b1 = fm.registerFont(ttfFont);

        //sun.font.CFontManager
        //sun.awt.Win32FontManager wfm = new Win32FontManager();
        //sun.awt.Win32FontManager wfm = (Win32FontManager)fm;

        printFontsInfo(ge);
        //printFontsInfo(wfm);

        // for allFonts initialization
        //Font[] aif = wfm.getAllInstalledFonts();
        //wfm.registerFont(ttfFont);
        //fm.registerFont(ttfFont);

        Field fieldAllFonts = fm.getClass().getSuperclass().getDeclaredField("allFonts");
        fieldAllFonts.setAccessible(true);
        Object oAllFonts = fieldAllFonts.get(fm); //SunFontManager.getInstance()
        Font[] allFonts = (Font[]) oAllFonts;

        // remove font
        java.util.List<Font> resultListAllFonts = new LinkedList();
        for(Font f: allFonts) {
            if (!f.getFontName().equals(fontName)) {
                resultListAllFonts.add(f);
            }
        }

        allFonts = resultListAllFonts.toArray(new Font[0]);
        // replace fonts array
        fieldAllFonts.set(fm, allFonts);//SunFontManager.getInstance()
        fieldAllFonts.setAccessible(false);


        // for allFamilies initialization
        /*String[] fl = SunFontManager.getInstance().getInstalledFontFamilyNames(null);
        Field fieldAllFamilies = wfm.getClass().getSuperclass().getDeclaredField("allFamilies");
        fieldAllFamilies.setAccessible(true);
        Object oAllFamilies = fieldAllFamilies.get(SunFontManager.getInstance());
        String[] allFamilies = (String[]) oAllFamilies;

        // remove font
        java.util.List<String> resultListAllFamilies = new LinkedList();
        for(String f: allFamilies) {
            if (!f.equals(fontName)) {
                resultListAllFamilies.add(f);
            }
        }

        allFamilies = resultListAllFamilies.toArray(new String[0]);
        // replace fonts array
        fieldAllFamilies.set(wfm, allFamilies);//SunFontManager.getInstance()

        fieldAllFamilies.setAccessible(false);

*/


        //HashSet<String> installedNames

        Field fieldInstalledNames = fm.getClass().getSuperclass().getDeclaredField("installedNames");
        fieldInstalledNames.setAccessible(true);
        Object oInstalledNames = fieldInstalledNames.get(SunFontManager.getInstance());
        HashSet<String> installedNames = (HashSet<String>) oInstalledNames;

        // remove font
        HashSet<String> resultListInstalledNames = new HashSet<String>();
        for(String f: installedNames) {
            if (!f.equals(fontName.toLowerCase())) {
                resultListInstalledNames.add(f);
            }
        }
        installedNames = resultListInstalledNames;
        // replace fonts array
        fieldInstalledNames.set(fm, installedNames);//SunFontManager.getInstance()

        fieldInstalledNames.setAccessible(false);


        Field fieldFontNameCache = fm.getClass().getSuperclass().getDeclaredField("fontNameCache");
        fieldFontNameCache.setAccessible(true);
        Object oFontNameCache = fieldFontNameCache.get(fm);
        ConcurrentHashMap<String, Font2D> fontNameCache = (ConcurrentHashMap<String, Font2D>) oFontNameCache;

        // remove font
        ConcurrentHashMap<String, Font2D> resultFontNameCache = new ConcurrentHashMap<String, Font2D>();
        String[] STR_ARRAY = new String[0];
        String[] keys = (String[])(fontNameCache.keySet().toArray(STR_ARRAY));
        for (int k=0; k<keys.length;k++) {
            if (keys[k].equals(fontName.toLowerCase() + ".plain")) {
                fontNameCache.remove(keys[k]);
            }
        }
        // replace fonts array
        fieldFontNameCache.set(fm, fontNameCache);//SunFontManager.getInstance()
        fieldFontNameCache.setAccessible(false);

        //physicalFonts
        Field fieldPhysicalFonts = fm.getClass().getSuperclass().getDeclaredField("physicalFonts");
        fieldPhysicalFonts.setAccessible(true);
        Object oPhysicalFonts = fieldPhysicalFonts.get(fm);
        ConcurrentHashMap<String, Font2D> physicalFonts = (ConcurrentHashMap<String, Font2D>) oPhysicalFonts;

        // remove font
        keys = (String[])(physicalFonts.keySet().toArray(STR_ARRAY));
        for (int k=0; k<keys.length;k++) {
            if (keys[k].equals(fontName)) {
                physicalFonts.remove(keys[k]);
            }
        }
        // replace fonts array
        fieldPhysicalFonts.set(fm, physicalFonts);//SunFontManager.getInstance()
        fieldPhysicalFonts.setAccessible(false);


        //fullNameToFont
        Field fieldFullNameToFont = fm.getClass().getSuperclass().getDeclaredField("fullNameToFont");
        fieldFullNameToFont.setAccessible(true);

        Object oFullNameToFont = fieldFullNameToFont.get(fm);
        ConcurrentHashMap<String, Font2D> fullNameToFont = (ConcurrentHashMap<String, Font2D>) oFullNameToFont;

        // remove font
        keys = (String[])(fullNameToFont.keySet().toArray(STR_ARRAY));
        for (int k=0; k<keys.length;k++) {
            if (keys[k].equals(fontName.toLowerCase())) {
                fullNameToFont.remove(keys[k]);
            }
        }
        // replace fonts array
        fieldFullNameToFont.set(fm, fullNameToFont);//SunFontManager.getInstance()
        fieldFullNameToFont.setAccessible(false);

        //localeFullNamesToFont
        Field fieldLocaleFullNameToFont = fm.getClass().getSuperclass().getDeclaredField("localeFullNamesToFont");
        fieldLocaleFullNameToFont.setAccessible(true);

        Object oLocaleFullNameToFont = fieldLocaleFullNameToFont.get(fm);
        HashMap<String, TrueTypeFont> localeFullNameToFont = (HashMap<String, TrueTypeFont>) oLocaleFullNameToFont;

        // remove font
        keys = (String[])(localeFullNameToFont.keySet().toArray(STR_ARRAY));
        for (int k=0; k<keys.length;k++) {
            if (keys[k].startsWith(fontName)) {
                localeFullNameToFont.remove(keys[k]);
            }
        }
        // replace fonts array
        fieldLocaleFullNameToFont.set(fm, localeFullNameToFont);//SunFontManager.getInstance()
        fieldLocaleFullNameToFont.setAccessible(false);


        /*




        Field fieldFontName = ttfFont.getClass().getDeclaredField("name");
        fieldFontName.setAccessible(true);
        Object test = fieldFontName.get(ttfFont);
        fieldFontName.set(ttfFont, "Cambria Math Metanorma");
        fieldFontName.setAccessible(false);

        Font2D f2d = FontUtilities.getFont2D(ttfFont);


        //Field fieldFont2Dfullname = f2d.getClass().getDeclaredField("localeFullName");
        Field fieldFont2Dfullname = Font2D.class.getDeclaredField("fullName");
        fieldFont2Dfullname.setAccessible(true);
        Object test123 = fieldFont2Dfullname.get(f2d);
        fieldFont2Dfullname.set(f2d, "Cambria Math Metanorma");
        fieldFont2Dfullname.setAccessible(false);

        Field fieldFont2Dlocalefullname = f2d.getClass().getDeclaredField("localeFullName");
        fieldFont2Dlocalefullname.setAccessible(true);
        fieldFont2Dlocalefullname.set(f2d, "Cambria Math Metanorma");
        fieldFont2Dlocalefullname.setAccessible(false);

        Field fieldFont2Dfamilyname = Font2D.class.getDeclaredField("familyName");
        fieldFont2Dfamilyname.setAccessible(true);
        Object test1234 = fieldFont2Dfamilyname.get(f2d);
        fieldFont2Dfamilyname.set(f2d, "Cambria Math Metanorma");
        fieldFont2Dfamilyname.setAccessible(false);

        Field fieldFont2Dlocalefamilyname = f2d.getClass().getDeclaredField("localeFamilyName");
        fieldFont2Dlocalefamilyname.setAccessible(true);
        fieldFont2Dlocalefamilyname.set(f2d, "Cambria Math Metanorma");
        fieldFont2Dlocalefamilyname.setAccessible(false);

        Field fieldfont2DHandle = ttfFont.getClass().getDeclaredField("font2DHandle");
        fieldfont2DHandle.setAccessible(true);
        Object test12345 = fieldfont2DHandle.get(ttfFont);
        fieldfont2DHandle.set(ttfFont, new Font2DHandle(f2d));
        fieldfont2DHandle.setAccessible(false);

        //Method methodgetFont2D = ttfFont.getClass().getDeclaredMethod("getFont2D", null);

*/
        /*Field fieldcreatedfont = ttfFont.getClass().getDeclaredField("createdFont");;
        fieldcreatedfont.setAccessible(true);
        Object fieldcreatedfont_value = fieldcreatedfont.get(ttfFont);
        fieldcreatedfont.set(ttfFont, false);
        fieldcreatedfont.setAccessible(false);*/

//ttfFont
       // getFont2D

  /*      System.out.println("getFontName()=" + ttfFont.getFontName());
        System.out.println("getFamily()=" + ttfFont.getFamily());

        Locale l = getSystemStartupLocale();
        Locale nameLocale = sun.awt.SunToolkit.getStartupLocale();



        System.out.println("getFontName()=" + ttfFont.getFontName(l));
        System.out.println("getFamily()=" + ttfFont.getFamily(l));

    */
        /*FontManager fm = FontManagerFactory.getInstance();
        Field fnt = fm.getClass().getField("registeredFontFiles");
        fnt.setAccessible(true);
        HashSet<String> registeredFontFiles =(HashSet<String>) fnt.get(ttfFont);*/
        //boolean d = FontAccess.getFontAccess().isCreatedFont(ttfFont);

        //HashSet<String> names = getInstalledNames();

        //boolean result = ge.registerFont(ttfFont);
        //result = wfm.registerFont(ttfFont);
        result = ge.registerFont(ttfFont);
        //result = wfm.registerFont(ttfFont);
        System.out.println("result=" + result);
        System.out.println("FONT LIST AFTER");
        printFontsInfo(ge);
        //printFontsInfo(wfm);
    }

    private static Locale getSystemStartupLocale() {
        String fileEncoding = System.getProperty("file.encoding", "");
        String sysEncoding = System.getProperty("sun.jnu.encoding");
        if (sysEncoding != null && !sysEncoding.equals(fileEncoding)) {
            return Locale.ROOT;
        }

        String language = System.getProperty("user.language", "en");
        String country  = System.getProperty("user.country","");
        String variant  = System.getProperty("user.variant","");
        return new Locale(language, country, variant);

    }

    private static void printFontsInfo (GraphicsEnvironment ge) {
    //private static void printFontsInfo (sun.awt.Win32FontManager ge) {
        Font[] allfonts = ge.getAllFonts();
        //Font[] allfonts = ge.getCreatedFonts();
        for (int i = 0; i < allfonts.length; i++) {
            if (allfonts[i] == null) {
                System.out.println("null i: " + i);
            }
            String font_name = allfonts[i].getFontName();
            if (font_name.contains("Cambria Math")) {
                System.out.println("font: " + font_name);
                // https://stackoverflow.com/questions/2019249/get-font-file-as-a-file-object-or-get-its-path
                // use reflection on Font2D (<B>PhysicalFont.platName</B>) e.g.
                Font2D f2d = sun.font.FontUtilities.getFont2D(allfonts[i]);
                try {
                    Field platName = PhysicalFont.class.getDeclaredField("platName");
                    platName.setAccessible(true);
                    String fontPath = (String) platName.get(f2d);
                    platName.setAccessible(false);
                    System.out.println("fontPath=" + fontPath);
                } catch (Exception ex) {
                }
            }
        }
    }
}
