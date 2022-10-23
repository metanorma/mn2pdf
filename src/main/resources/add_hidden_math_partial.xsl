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

	<xsl:output version="1.0" method="xml" encoding="UTF-8" indent="yes" omit-xml-declaration="yes"/>
	
	<xsl:param name="ifo" />
	
	<xsl:variable name="instream-foreign-objects" select="$ifo"/>
	
	<xsl:template match="@*|node()">
		<xsl:copy>
				<xsl:apply-templates select="@*|node()"/>
		</xsl:copy>
	</xsl:template>
	
	
	<xsl:template match="im:font[following-sibling::*[1][self::im:image[math:math]]] | im:font[following-sibling::*[1][self::im:id]][following-sibling::*[2][self::im:image[math:math]]]">
		
		<!-- <debug>
			<instream-foreign-objects>
				<xsl:copy-of select="$instream-foreign-objects"/>
			</instream-foreign-objects>
		</debug> -->
	
		<xsl:variable name="image_" select="following-sibling::im:image[math:math]"/>
		<xsl:variable name="image" select="xalan:nodeset($image_)"/>
		
		<xsl:variable name="ref" select="normalize-space($image/@foi:struct-ref)"/>
		
		<xsl:variable name="instream-foreign-object_" select="$instream-foreign-objects//instream-foreign-object[@struct-id = $ref]"/>
		<xsl:variable name="instream-foreign-object" select="xalan:nodeset($instream-foreign-object_)"/>
		
		<xsl:variable name="instream-foreign-object_alt-text" select="$instream-foreign-object/@alt-text"/>
		<xsl:variable name="instream-foreign-object_preceding_inline_text_struct_id" select="$instream-foreign-object/@preceding_inline_text_struct_id"/>
		
		<xsl:variable name="width" select="$image/@width"/>
		<xsl:variable name="height" select="$image/@height"/>
		
		<xsl:variable name="font_family" select="@family"/>
		
		<xsl:copy>
			<xsl:copy-of select="@*"/>
			<xsl:attribute name="color">
			 <!-- add alpha-channel (transparency) for hidden math text -->
				<xsl:choose>
					<xsl:when test="@color"><xsl:value-of select="@color"/>00</xsl:when>
					<xsl:otherwise>#FFFFFF00</xsl:otherwise>
				</xsl:choose>
			</xsl:attribute>
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
					<xsl:value-of select="$instream-foreign-object_preceding_inline_text_struct_id"/>
				</xsl:attribute>
				<xsl:value-of select="$instream-foreign-object_alt-text"/>
			</xsl:element>
			
		</xsl:if>
		
	</xsl:template>

	<xsl:template match="im:envelope">
		<xsl:apply-templates />
	</xsl:template>

</xsl:stylesheet>
