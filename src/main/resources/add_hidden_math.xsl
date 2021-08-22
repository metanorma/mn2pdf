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
	exclude-result-prefixes="im xalan fo fox math"
	version="1.0">

	<xsl:output version="1.0" method="xml" encoding="UTF-8" indent="yes"/>
	
	<xsl:template match="@*|node()">
		<xsl:copy>
				<xsl:apply-templates select="@*|node()"/>
		</xsl:copy>
	</xsl:template>
	
	<xsl:variable name="instream-foreign-objects_">
		<xsl:for-each select="//fo:instream-foreign-object[@fox:actual-text != '']">
			<xsl:copy-of select="."/>
		</xsl:for-each>
	</xsl:variable>
	
	<xsl:variable name="instream-foreign-objects" select="xalan:nodeset($instream-foreign-objects_)" />
	
	
	
	<xsl:template match="im:image[math:math]">
		<xsl:variable name="ref" select="@foi:struct-ref"/>
		
		<xsl:variable name="width" select="@width"/>
		<xsl:variable name="height" select="@height"/>
		
		<xsl:variable name="instream-foreign-object_actual-text" select="$instream-foreign-objects//fo:instream-foreign-object[@foi:struct-id = $ref]/@fox:actual-text"/>
		
		<xsl:variable name="font_color_preceding" select="preceding-sibling::im:font[@color][1]/@color"/>
		<xsl:variable name="font_color_main">
			<xsl:choose>
				<xsl:when test="$font_color_preceding != ''">
					<xsl:value-of select="$font_color_preceding"/>
				</xsl:when>
				<xsl:otherwise>#000000</xsl:otherwise>
			</xsl:choose>
		</xsl:variable>
		
		<xsl:variable name="font_family" select="preceding-sibling::im:font[@family][1]/@family | ancestor::*[im:font[@family]][1]/im:font/@family"/>
		
		<xsl:element name="font" namespace="http://xmlgraphics.apache.org/fop/intermediate">
			<xsl:attribute name="color">#FFFFFF</xsl:attribute>
			<xsl:attribute name="family"><xsl:value-of select="$font_family"/></xsl:attribute>
			<!-- <xsl:variable name="fontsize" select="java:org.metanorma.fop.Util.getFontSize(font_family, $width, $height)"/> -->
			<xsl:variable name="fontsize">1000</xsl:variable>
			<xsl:attribute name="size">
				<xsl:value-of select="$fontsize"/>
			</xsl:attribute>
		</xsl:element>
		
		<xsl:variable name="text_x" select="@x"/>
		<xsl:variable name="text_y" select="@y + @height"/>
		
		
		<xsl:element name="text" namespace="http://xmlgraphics.apache.org/fop/intermediate">
			<xsl:attribute name="x"><xsl:value-of select="$text_x"/></xsl:attribute>
			<xsl:attribute name="y"><xsl:value-of select="$text_y"/></xsl:attribute>
			<xsl:attribute name="foi:struct-ref"><xsl:value-of select="$ref"/></xsl:attribute>
			<xsl:value-of select="$instream-foreign-object_actual-text"/>
		</xsl:element>
		
		
		<xsl:element name="font" namespace="http://xmlgraphics.apache.org/fop/intermediate">
			<xsl:attribute name="color"><xsl:value-of select="$font_color_main"/></xsl:attribute>
		</xsl:element>
		
		<xsl:copy-of select="."/>
	</xsl:template>

</xsl:stylesheet>
