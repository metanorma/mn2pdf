<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
	xmlns="http://xmlgraphics.apache.org/fop/intermediate" 
	xmlns:im="http://xmlgraphics.apache.org/fop/intermediate" 
	xmlns:fo="http://www.w3.org/1999/XSL/Format" 
	xmlns:foi="http://xmlgraphics.apache.org/fop/internal"   
	xmlns:fox="http://xmlgraphics.apache.org/fop/extensions"
	xmlns:math="http://www.w3.org/1998/Math/MathML"
	xmlns:xalan="http://xml.apache.org/xalan"  
	xmlns:jeuclid="http://jeuclid.sf.net/ns/ext"
	xmlns:java="http://xml.apache.org/xalan/java"
	exclude-result-prefixes="im xalan fo fox math java"
	version="1.0">

	<xsl:output version="1.0" method="xml" encoding="UTF-8" indent="yes"/>
	
	<xsl:template match="@*|node()">
		<xsl:copy>
				<xsl:apply-templates select="@*|node()"/>
		</xsl:copy>
	</xsl:template>
	
	<xsl:variable name="instream-foreign-objects_">
		<xsl:for-each select="//fo:instream-foreign-object[@fox:alt-text != '']">
			<xsl:copy>
				<xsl:copy-of select="@*"/>
				<xsl:variable name="preceding_inline_text_struct_id" select="normalize-space(preceding-sibling::*[1][self::fo:inline]/im:marked-content/@foi:struct-id)"/>
				<xsl:if test="$preceding_inline_text_struct_id != ''">
					<!-- add previous helper element id -->
					<xsl:attribute name="preceding_inline_text_struct_id"><xsl:value-of select="$preceding_inline_text_struct_id"/></xsl:attribute>
				</xsl:if>
			</xsl:copy>
		</xsl:for-each>
	</xsl:variable>
	
	<xsl:variable name="instream-foreign-objects" select="xalan:nodeset($instream-foreign-objects_)" />
	
	<xsl:template match="im:content">
		<xsl:copy>
			<xsl:copy-of select="@*"/>
			
			<!-- move hidden math before another content, i.e. behind another elements (text, images, svg) -->
			<xsl:apply-templates select=".//im:font[following-sibling::*[1][self::im:image[math:math]]] | .//im:font[following-sibling::*[1][self::im:id]][following-sibling::*[2][self::im:image[math:math]]]" mode="hidden_math"/>
			
			<xsl:apply-templates />
		</xsl:copy>
	</xsl:template>
	
	
	<xsl:template match="im:font" mode="hidden_math">
		<xsl:variable name="image_" select="following-sibling::im:image[math:math]"/>
		<xsl:variable name="image" select="xalan:nodeset($image_)"/>
		
		<xsl:variable name="ref" select="$image/@foi:struct-ref"/>
		
		<xsl:variable name="instream-foreign-object_" select="$instream-foreign-objects//fo:instream-foreign-object[@foi:struct-id = $ref]"/>
		<xsl:variable name="instream-foreign-object" select="xalan:nodeset($instream-foreign-object_)"/>
		
		<xsl:variable name="instream-foreign-object_alt-text" select="$instream-foreign-object/@fox:alt-text"/>
		
		<xsl:variable name="width" select="$image/@width"/>
		<xsl:variable name="height" select="$image/@height"/>
		
		<xsl:variable name="font_family" select="@family"/>
		
		<xsl:variable name="elements">
			<xsl:copy>
				<xsl:copy-of select="@*"/>
        <xsl:attribute name="color">
         <!-- add alpha-channel (transparency) for hidden math text -->
          <xsl:choose>
            <xsl:when test="@color"><xsl:value-of select="@color"/>00</xsl:when>
            <xsl:otherwise>#FFFFFF00</xsl:otherwise>
          </xsl:choose>
        </xsl:attribute>
				<xsl:attribute name="style">normal</xsl:attribute>
				<xsl:attribute name="weight">400</xsl:attribute>
				<xsl:attribute name="variant">normal</xsl:attribute>
				<xsl:if test="normalize-space($instream-foreign-object_alt-text) != '' and $instream-foreign-object_alt-text != 'Math'">
					<xsl:variable name="fontsize" select="java:org.metanorma.fop.Util.getFontSize($instream-foreign-object_alt-text, $font_family, $width, $height)"/>
					<xsl:attribute name="size">
						<xsl:value-of select="$fontsize"/>
					</xsl:attribute>
				</xsl:if>
			</xsl:copy>
			
			<xsl:if test="normalize-space($instream-foreign-object_alt-text) != '' and $instream-foreign-object_alt-text != 'Math'">
				<xsl:variable name="text_x" select="$image/@x"/>
				<xsl:variable name="text_y" select="$image/@y + $image/@height"/>

				<xsl:element name="text" namespace="http://xmlgraphics.apache.org/fop/intermediate">
					<xsl:attribute name="x"><xsl:value-of select="$text_x"/></xsl:attribute>
					<xsl:attribute name="y"><xsl:value-of select="$text_y"/></xsl:attribute>
					<xsl:attribute name="foi:struct-ref">
						<xsl:value-of select="$instream-foreign-object/@preceding_inline_text_struct_id"/>
					</xsl:attribute> <!-- <xsl:value-of select="$ref"/> -->
					<xsl:value-of select="$instream-foreign-object_alt-text"/>
				</xsl:element>
				
			</xsl:if>
		</xsl:variable>
		
		<xsl:variable name="viewports">
			<xsl:for-each select="ancestor::*[ancestor::im:content]"> <!-- viewport or g -->
				<xsl:copy>
					<xsl:copy-of select="@*"/>
				</xsl:copy>
			</xsl:for-each>
		</xsl:variable>
		
		<!-- enclose elements into viewports tree -->
		<xsl:apply-templates select="xalan:nodeset($viewports)/*[1]" mode="viewport">
			<xsl:with-param name="elements" select="$elements"/>
		</xsl:apply-templates>
		<!-- <xsl:copy-of select="$viewports"/> -->
		
	</xsl:template>

	<xsl:template match="*" mode="viewport">
		<xsl:param name="elements"/>
		<xsl:copy>
			<xsl:copy-of select="@*"/>
			<xsl:choose>
				<xsl:when test="following-sibling::*">
					<xsl:apply-templates select="following-sibling::*[1]" mode="viewport">
						<xsl:with-param name="elements" select="$elements"/>
					</xsl:apply-templates>
				</xsl:when>
				<xsl:otherwise>
					<xsl:copy-of select="$elements"/>
				</xsl:otherwise>
			</xsl:choose>
		</xsl:copy>
	</xsl:template>

</xsl:stylesheet>
