package org.metanorma.fop.ifhandler;

/*
  XML element:
  <instream-foreign-object alt-text="H" struct-id="a93" preceding_inline_text_struct_id="a92"/>
*/
public class InstreamForeignObject {

    String struct_id = "";

    String preceding_inline_text_struct_id = "";

    String alt_text = "";

    InstreamForeignObject(String struct_id, String preceding_inline_text_struct_id, String alt_text) {
        this.struct_id = struct_id;
        this.preceding_inline_text_struct_id = preceding_inline_text_struct_id;
        this.alt_text = alt_text;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("<instream-foreign-object alt-text=\"")
            .append(alt_text)
            .append("\" struct-id=\"")
            .append(struct_id)
            .append("\" preceding_inline_text_struct_id=\"")
            .append(preceding_inline_text_struct_id)
            .append("\"/>\n");
        return  sb.toString();
    }

}
