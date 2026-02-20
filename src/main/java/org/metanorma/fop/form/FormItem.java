package org.metanorma.fop.form;

import org.apache.pdfbox.pdmodel.common.PDRectangle;

public class FormItem {

    PDRectangle rect ;// = new PDRectangle(50, 750, 200, 50);

    int page;

    FormItemType type;

    public FormItem(PDRectangle rect, int page) {
        this.rect = rect;
        this.page = page;
    }

    public PDRectangle getRect() {
        return rect;
    }

    public FormItemType getType() {
        return type;
    }

    private String name;

    private double fontSize = 11;

    private String fontColor = "#000000";

    public int getPage() {
        return page;
    }

    public void setFormItemType(String formItemType) {
        switch (formItemType) {
            case "textfield":
                this.type = FormItemType.TextField;
                break;
            default:
                this.type = FormItemType.TextField;
                break;
        }

    }

    public String getName() {
        return name;
    }

    public void setName(String formItemName) {
        this.name = formItemName;
    }

    public double getFontSize() {
        return fontSize;
    }

    public void setFontSize(double fontSize) {
        this.fontSize = fontSize;
    }

    public String getFontColor() {
        return fontColor;
    }

    public void setFontColor(String fontColor) {
        this.fontColor = fontColor;
    }
}
