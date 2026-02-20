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

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
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

                defaultAppearanceString = "/Helv 12 Tf 0 0 1 rg";
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
    
}
