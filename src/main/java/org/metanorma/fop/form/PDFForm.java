package org.metanorma.fop.form;

import org.apache.fontbox.afm.CharMetric;
import org.apache.fontbox.afm.FontMetrics;
import org.apache.fontbox.util.BoundingBox;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.common.filespecification.PDComplexFileSpecification;
import org.apache.pdfbox.pdmodel.common.filespecification.PDFileSpecification;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.font.encoding.GlyphList;
import org.apache.pdfbox.pdmodel.graphics.color.PDColor;
import org.apache.pdfbox.pdmodel.graphics.color.PDDeviceRGB;
import org.apache.pdfbox.pdmodel.interactive.annotation.*;
import org.apache.pdfbox.pdmodel.interactive.form.*;
import org.metanorma.utils.LoggerHelper;
import org.verapdf.model.coslayer.CosName;

import javax.xml.bind.DatatypeConverter;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.DecimalFormat;
import java.text.Normalizer;
import java.util.*;
import java.util.List;
import java.util.logging.Logger;

/**
 *
 * @author Alexander Dyuzhev
 */
public class PDFForm {
    
    protected static final Logger logger = Logger.getLogger(LoggerHelper.LOGGER_NAME);
    
    private boolean DEBUG = false;

    private PDColor colorBlack = new PDColor(new float[]{0, 0, 0}, PDDeviceRGB.INSTANCE);
    private PDColor colorWhite = new PDColor(new float[]{1, 1, 1}, PDDeviceRGB.INSTANCE);

    public void process(File pdf, Map<String, List<FormItem>> formItems) throws IOException {

        Path pdf_tmp = Paths.get(pdf.getAbsolutePath() + "_forms_tmp");
        Files.copy(Paths.get(pdf.getAbsolutePath()), pdf_tmp, StandardCopyOption.REPLACE_EXISTING);
        try (PDDocument document = Loader.loadPDF(pdf_tmp.toFile())) {

            // Add a new AcroForm and add that to the document
            PDAcroForm acroForm = new PDAcroForm(document);
            document.getDocumentCatalog().setAcroForm(acroForm);

            PDFont font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            PDResources resources = new PDResources();
            resources.put(COSName.HELV, font);

            acroForm.setDefaultResources(resources);

            String defaultAppearanceString = "/Helv 0 Tf 0 g";
            acroForm.setDefaultAppearance(defaultAppearanceString);

            DecimalFormat df = new DecimalFormat("#.#");

            for (Map.Entry<String, List<FormItem>> formEntry : formItems.entrySet()) {

                String formName = formEntry.getKey();

                for (FormItem formItem: formEntry.getValue()) {

                    int pageNum = formItem.getPage();
                    PDPage page = document.getPage(pageNum - 1);

                    if (formItem.getType() == FormItemType.TextField) {
                        PDTextField textBox = new PDTextField(acroForm);
                        textBox.setPartialName(formItem.getName());

                        String fontSize = df.format(formItem.getFontSize());
                        String fontColor = getNormalizedRGB(formItem.getFontColor());

                        // /Helv 11 Tf 0 0 1 rg
                        /* From Google AI:
                        /Helv: This is the name under which the Helvetica font is known in the PDF's resource dictionary. The actual font file might be embedded in the PDF, but it's referenced by this name.
                        11: This number specifies the font size in points (11 points).
                        Tf: This is the "Set text font and size" operator. It sets the specified font and size as the current text state parameters for any subsequent text-showing operations.
                        0 0 1: These three numbers represent the RGB color values for the non-stroking (filling) color, with values ranging from 0 to 1. In this case, 0 0 1 corresponds to blue (0% Red, 0% Green, 100% Blue).
                        rg: This is the "Set non-stroking color space to RGB and set the color" operator. It sets the color used for filling text (and other shapes) to the color specified by the preceding values.
                        */
                        defaultAppearanceString = "/Helv " + fontSize + " Tf " + fontColor +" rg";
                        textBox.setDefaultAppearance(defaultAppearanceString);

                        acroForm.getFields().add(textBox);

                        PDAnnotationWidget widget = textBox.getWidgets().get(0);
                        PDRectangle rect = formItem.getRect();
                        widget.setRectangle(rect);
                        widget.setPage(page);

                        PDAppearanceCharacteristicsDictionary fieldAppearance
                                = new PDAppearanceCharacteristicsDictionary(new COSDictionary());
                        //fieldAppearance.setBorderColour(new PDColor(new float[]{0,1,0}, PDDeviceRGB.INSTANCE));
                        //fieldAppearance.setBackground(new PDColor(new float[]{1,1,0}, PDDeviceRGB.INSTANCE));
                        widget.setAppearanceCharacteristics(fieldAppearance);

                        widget.setPrinted(true);

                        page.getAnnotations().add(widget);

                        // alignment
                        textBox.setQ(PDVariableText.QUADDING_CENTERED);
                        //textBox.setValue("Sample field content");

                    }  else if (formItem.getType() == FormItemType.CheckBox) {
                        // from PDFBox CreateCheckBox
                        PDCheckBox checkbox = new PDCheckBox(acroForm);
                        checkbox.setPartialName(formItem.getName());

                        PDAnnotationWidget widget = checkbox.getWidgets().get(0);
                        widget.setPage(page);
                        PDRectangle rect = formItem.getRect();
                        widget.setRectangle(rect);
                        widget.setPrinted(true);

                        PDAppearanceCharacteristicsDictionary appearanceCharacteristics = new PDAppearanceCharacteristicsDictionary(new COSDictionary());

                        float[] rgb = getFloatRGB(formItem.getFontColor());

                        // To do: https://github.com/metanorma/mn2pdf/issues/403
                        appearanceCharacteristics.setBorderColour(new PDColor(rgb, PDDeviceRGB.INSTANCE));
                        // default white background
                        appearanceCharacteristics.setBackground(colorWhite);
                        // 8 = cross; 4 = checkmark; H = star; u = diamond; n = square, l = dot
                        appearanceCharacteristics.setNormalCaption("4");
                        widget.setAppearanceCharacteristics(appearanceCharacteristics);

                        PDBorderStyleDictionary borderStyleDictionary = new PDBorderStyleDictionary();
                        borderStyleDictionary.setWidth(0.5f); // To do - pass border width
                        borderStyleDictionary.setStyle(PDBorderStyleDictionary.STYLE_SOLID);
                        widget.setBorderStyle(borderStyleDictionary);

                        PDAppearanceDictionary ap = new PDAppearanceDictionary();
                        widget.setAppearance(ap);
                        PDAppearanceEntry normalAppearance = ap.getNormalAppearance();

                        COSDictionary normalAppearanceDict = normalAppearance.getCOSObject();
                        PDFont zapfDingbats = new PDType1Font(Standard14Fonts.FontName.ZAPF_DINGBATS);
                        normalAppearanceDict.setItem(COSName.Off, createCheckBoxAppearanceStream(document, widget, false, zapfDingbats));
                        normalAppearanceDict.setItem(COSName.YES, createCheckBoxAppearanceStream(document, widget, true, zapfDingbats));

                        // If we ever decide to implement a /D (down) appearance, just
                        // replace the background colors c with c * 0.75

                        page.getAnnotations().add(checkbox.getWidgets().get(0));
                        acroForm.getFields().add(checkbox);

                        // always call check() or unCheck(), or the box will remain invisible.
                        checkbox.unCheck();
                        if (formItem.getValue().equals("true")) {
                            checkbox.check();
                        }
                    }  else if (formItem.getType() == FormItemType.RadioButton) {
                        // skip, see processing below
                    }
                }

                // create groups of radiobuttons
                Map<String, List<FormItem>> radioButtonsGroups = new HashMap<>();
                for (FormItem formItem: formEntry.getValue()) {
                    if (formItem.getType() == FormItemType.RadioButton) {
                        String groupName = formItem.getName();
                        radioButtonsGroups.computeIfAbsent(groupName, k -> new ArrayList<>()).add(formItem);
                    }
                }

                for (Map.Entry<String, List<FormItem>> radioButtonEntry : radioButtonsGroups.entrySet()) {

                    String groupName = radioButtonEntry.getKey();

                    int pageNum = radioButtonEntry.getValue().get(0).getPage(); // all RadioButtons on the same page
                    PDPage page = document.getPage(pageNum - 1);

                    List<String> options = new ArrayList<>();
                    for (FormItem radioButtonItem : radioButtonEntry.getValue()) {
                        options.add(radioButtonItem.getValue());
                    }

                    PDRadioButton radioButton = new PDRadioButton(acroForm);
                    radioButton.setPartialName(groupName);
                    radioButton.setExportValues(options);

                    PDAppearanceCharacteristicsDictionary appearanceCharacteristics = new PDAppearanceCharacteristicsDictionary(new COSDictionary());
                    appearanceCharacteristics.setBorderColour(colorBlack);
                    appearanceCharacteristics.setBackground(colorWhite);
                    // no caption => round
                    // with caption => see checkbox example

                    List<PDAnnotationWidget> widgets = new ArrayList<>();
                    for (int i = 0; i < options.size(); i++)
                    {
                        PDAnnotationWidget widget = new PDAnnotationWidget();
                        PDRectangle rect = radioButtonEntry.getValue().get(i).getRect();
                        widget.setRectangle(rect);
                        widget.setPrinted(true);
                        widget.setAppearanceCharacteristics(appearanceCharacteristics);
                        PDBorderStyleDictionary borderStyleDictionary = new PDBorderStyleDictionary();
                        borderStyleDictionary.setWidth(0.5f);
                        borderStyleDictionary.setStyle(PDBorderStyleDictionary.STYLE_SOLID);
                        widget.setBorderStyle(borderStyleDictionary);
                        widget.setPage(page);

                        COSDictionary apNDict = new COSDictionary();
                        apNDict.setItem(COSName.Off, createRadioButtonAppearanceStream(document, widget, false));
                        apNDict.setItem(options.get(i), createRadioButtonAppearanceStream(document, widget, true));

                        PDAppearanceDictionary appearance = new PDAppearanceDictionary();
                        PDAppearanceEntry appearanceNEntry = new PDAppearanceEntry(apNDict);
                        appearance.setNormalAppearance(appearanceNEntry);
                        widget.setAppearance(appearance);
                        widget.setAppearanceState("Off"); // don't forget this, or button will be invisible
                        widgets.add(widget);
                        page.getAnnotations().add(widget);
                    }
                    radioButton.setWidgets(widgets);

                    acroForm.getFields().add(radioButton);
                }
            }

            Files.deleteIfExists(pdf.toPath());
            document.save(pdf);
                
        } catch (IOException ex) {
            logger.severe("Can't read annotation data from PDF.");
            ex.printStackTrace();
        }
        finally {
            /*if( document != null ) {
                document.close();
            }*/
            Files.deleteIfExists(pdf_tmp);
        }
    }

    private String getNormalizedRGB(String hexColor) {
        try {
            Color awtColor = Color.decode(hexColor);

            // integer RGB components (0-255)
            int r255 = awtColor.getRed();
            int g255 = awtColor.getGreen();
            int b255 = awtColor.getBlue();

            // normalized RGB components (0.0-1.0)
            float rNormalized = r255 / 255.0f;
            float gNormalized = g255 / 255.0f;
            float bNormalized = b255 / 255.0f;

            DecimalFormat df = new DecimalFormat("#.#");

            return df.format(rNormalized) + " " + df.format(gNormalized) + " " + df.format(bNormalized);

        } catch (Exception ex) {
            return "0 0 0";
        }
    }

    private float[] getFloatRGB(String hexColor) {
        float[] rgb = new float[]{0, 0, 0};

        hexColor = hexColor.replace("#", "");
        byte[] rgb_bytes =  DatatypeConverter.parseHexBinary(hexColor);
        rgb[0] = rgb_bytes[0] / 255.0f;
        rgb[1] = rgb_bytes[1] / 255.0f;
        rgb[2] = rgb_bytes[2] / 255.0f;

        return rgb;
    }

    private static PDAppearanceStream createCheckBoxAppearanceStream(
            final PDDocument document, PDAnnotationWidget widget, boolean on, PDFont font) throws IOException
    {
        PDRectangle rect = widget.getRectangle();
        PDAppearanceCharacteristicsDictionary appearanceCharacteristics;
        PDAppearanceStream yesAP = new PDAppearanceStream(document);
        yesAP.setBBox(new PDRectangle(rect.getWidth(), rect.getHeight()));
        yesAP.setResources(new PDResources());
        try (PDAppearanceContentStream yesAPCS = new PDAppearanceContentStream(yesAP))
        {
            appearanceCharacteristics = widget.getAppearanceCharacteristics();
            PDColor backgroundColor = appearanceCharacteristics.getBackground();
            PDColor borderColor = appearanceCharacteristics.getBorderColour();
            float lineWidth = getLineWidth(widget);
            yesAPCS.setBorderLine(lineWidth, widget.getBorderStyle(), widget.getBorder());
            yesAPCS.setNonStrokingColor(backgroundColor);
            yesAPCS.addRect(0, 0, rect.getWidth(), rect.getHeight());
            yesAPCS.fill();
            yesAPCS.setStrokingColor(borderColor);
            yesAPCS.addRect(lineWidth / 2, lineWidth / 2, rect.getWidth() - lineWidth, rect.getHeight() - lineWidth);
            yesAPCS.stroke();
            if (!on)
            {
                return yesAP;
            }

            yesAPCS.addRect(lineWidth, lineWidth, rect.getWidth() - lineWidth * 2, rect.getHeight() - lineWidth * 2);
            yesAPCS.clip();

            String normalCaption = appearanceCharacteristics.getNormalCaption();
            if (normalCaption == null)
            {
                normalCaption = "4"; // Adobe behaviour
            }
            if ("8".equals(normalCaption))
            {
                // Adobe paints a cross instead of using the Zapf Dingbats cross symbol
                yesAPCS.setStrokingColor(0f);
                yesAPCS.moveTo(lineWidth * 2, rect.getHeight() - lineWidth * 2);
                yesAPCS.lineTo(rect.getWidth() - lineWidth * 2, lineWidth * 2);
                yesAPCS.moveTo(rect.getWidth() - lineWidth * 2, rect.getHeight() - lineWidth * 2);
                yesAPCS.lineTo(lineWidth * 2, lineWidth * 2);
                yesAPCS.stroke();
            }
            else
            {
                Rectangle2D bounds = new Rectangle2D.Float();
                String unicode = null;

                // ZapfDingbats font may be missing or substituted, let's use AFM resources instead.
                FontMetrics metric = Standard14Fonts.getAFM(Standard14Fonts.FontName.ZAPF_DINGBATS.getName());
                for (CharMetric cm : metric.getCharMetrics())
                {
                    // The caption is not unicode, but the Zapf Dingbats code in the PDF.
                    // Assume that only the first character is used.
                    if (normalCaption.codePointAt(0) == cm.getCharacterCode())
                    {
                        BoundingBox bb = cm.getBoundingBox();
                        bounds = new Rectangle2D.Float(bb.getLowerLeftX(), bb.getLowerLeftY(),
                                bb.getWidth(), bb.getHeight());
                        unicode = GlyphList.getZapfDingbats().toUnicode(cm.getName());
                        break;
                    }
                }
                if (bounds.isEmpty())
                {
                    throw new IOException("Bounds rectangle for chosen glyph is empty");
                }
                float size = (float) Math.min(bounds.getWidth(), bounds.getHeight()) / 1000;
                // assume that checkmark has square size
                // the calculations approximate what Adobe is doing, i.e. put the glyph in the middle
                float fontSize = (rect.getWidth() - lineWidth * 2) / size * 0.6666f;
                float xOffset = (float) (rect.getWidth() - (bounds.getWidth()) / 1000 * fontSize) / 2;
                xOffset -= bounds.getX() / 1000 * fontSize;
                float yOffset = (float) (rect.getHeight() - (bounds.getHeight()) / 1000 * fontSize) / 2;
                yOffset -= bounds.getY() / 1000 * fontSize;
                yesAPCS.setNonStrokingColor(0f);
                yesAPCS.beginText();
                yesAPCS.setFont(font, fontSize);
                yesAPCS.newLineAtOffset(xOffset, yOffset);
                yesAPCS.showText(unicode);
                yesAPCS.endText();
            }
        }
        return yesAP;
    }

    private static PDAppearanceStream createRadioButtonAppearanceStream(
            final PDDocument document, PDAnnotationWidget widget, boolean on) throws IOException
    {
        PDRectangle rect = widget.getRectangle();
        PDAppearanceStream onAP = new PDAppearanceStream(document);
        onAP.setBBox(new PDRectangle(rect.getWidth(), rect.getHeight()));
        try (PDAppearanceContentStream onAPCS = new PDAppearanceContentStream(onAP))
        {
            PDAppearanceCharacteristicsDictionary appearanceCharacteristics = widget.getAppearanceCharacteristics();
            PDColor backgroundColor = appearanceCharacteristics.getBackground();
            PDColor borderColor = appearanceCharacteristics.getBorderColour();
            float lineWidth = getLineWidth(widget);
            onAPCS.setBorderLine(lineWidth, widget.getBorderStyle(), widget.getBorder());
            onAPCS.setNonStrokingColor(backgroundColor);
            float radius = Math.min(rect.getWidth() / 2, rect.getHeight() / 2);
            drawCircle(onAPCS, rect.getWidth() / 2, rect.getHeight() / 2, radius);
            onAPCS.fill();
            onAPCS.setStrokingColor(borderColor);
            drawCircle(onAPCS, rect.getWidth() / 2, rect.getHeight() / 2, radius - lineWidth / 2);
            onAPCS.stroke();
            if (on)
            {
                onAPCS.setNonStrokingColor(0f);
                drawCircle(onAPCS, rect.getWidth() / 2, rect.getHeight() / 2, (radius - lineWidth) / 2);
                onAPCS.fill();
            }
        }
        return onAP;
    }

    static float getLineWidth(PDAnnotationWidget widget)
    {
        PDBorderStyleDictionary bs = widget.getBorderStyle();
        if (bs != null)
        {
            return bs.getWidth();
        }
        return 1;
    }

    static void drawCircle(PDAppearanceContentStream cs, float x, float y, float r) throws IOException
    {
        // http://stackoverflow.com/a/2007782/535646
        float magic = r * 0.551784f;
        cs.moveTo(x, y + r);
        cs.curveTo(x + magic, y + r, x + r, y + magic, x + r, y);
        cs.curveTo(x + r, y - magic, x + magic, y - r, x, y - r);
        cs.curveTo(x - magic, y - r, x - r, y - magic, x - r, y);
        cs.curveTo(x - r, y + magic, x - magic, y + r, x, y + r);
        cs.closePath();
    }
}
