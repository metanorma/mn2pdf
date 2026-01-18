package org.metanorma.fop.form;

import org.apache.pdfbox.pdmodel.common.PDRectangle;

public class FormItem {

    PDRectangle rect ;// = new PDRectangle(50, 750, 200, 50);

    FormItemType formItemType;

    public FormItem(PDRectangle rect) {
        this.rect = rect;
    }

    public PDRectangle getRect() {
        return rect;
    }

    public FormItemType getFormItemType() {
        return formItemType;
    }

    public void setFormItemType(String formItemType) {
        switch (formItemType) {
            case "textfield":
                this.formItemType = FormItemType.TextField;
                break;
            default:
                this.formItemType = FormItemType.TextField;
                break;
        }

    }
}
