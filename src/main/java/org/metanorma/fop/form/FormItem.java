package org.metanorma.fop.form;

import org.apache.pdfbox.pdmodel.common.PDRectangle;

public class FormItem {

    PDRectangle rect;// = new PDRectangle(50, 750, 200, 50);

    int page;

    FormItemType type;

    String value = "";

    private String name;

    private double fontSize = 11;

    private String fontColor = "#000000";

    public FormItem(PDRectangle rect, int page) {
        this.rect = rect;
        this.page = page;
    }

    public PDRectangle getRect() {
        return rect;
    }

    public int getPage() {
        return page;
    }

    public void setType(String formItemType) {
        switch (formItemType) {
            case "textfield":
                this.type = FormItemType.TextField;
                break;
            case "checkbox":
                this.type = FormItemType.CheckBox;
                break;
            default:
                this.type = FormItemType.TextField;
                break;
        }
    }

    public FormItemType getType() {
        return type;
    }

    public void setName(String formItemName) {
        this.name = formItemName;
    }

    public String getName() {
        return name;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public void setFontSize(double fontSize) {
        this.fontSize = fontSize;
    }

    public double getFontSize() {
        return fontSize;
    }

    public void setFontColor(String fontColor) {
        this.fontColor = fontColor;
    }

    public String getFontColor() {
        return fontColor;
    }
}
