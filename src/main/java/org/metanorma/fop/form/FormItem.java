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

    public String name;

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
}
