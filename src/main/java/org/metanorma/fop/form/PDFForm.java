package org.metanorma.fop.form;

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
import org.apache.pdfbox.pdmodel.graphics.color.PDColor;
import org.apache.pdfbox.pdmodel.graphics.color.PDDeviceRGB;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationFileAttachment;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationWidget;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAppearanceCharacteristicsDictionary;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDTextField;
import org.apache.pdfbox.pdmodel.interactive.form.PDVariableText;
import org.metanorma.utils.LoggerHelper;
import org.verapdf.model.coslayer.CosName;

import java.awt.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.DecimalFormat;
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
    
    public void process(File pdf, List<FormItem> formItems) throws IOException {

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

            for (FormItem formItem: formItems) {

                int pageNum = formItem.getPage();
                PDPage page = document.getPage(pageNum - 1);

                PDTextField textBox = new PDTextField(acroForm);
                textBox.setPartialName(formItem.getName());

                DecimalFormat df = new DecimalFormat("#.#");
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

}
