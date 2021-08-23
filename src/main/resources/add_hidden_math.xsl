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
		<xsl:for-each select="//fo:instream-foreign-object[@fox:alt-text != '']">
			<xsl:copy-of select="."/>
		</xsl:for-each>
	</xsl:variable>
	
	<xsl:variable name="instream-foreign-objects" select="xalan:nodeset($instream-foreign-objects_)" />
	
	<xsl:template match="im:font[following-sibling::*[1][self::im:image[math:math]]]">
		<xsl:variable name="image_" select="following-sibling::*[1][self::im:image[math:math]]"/>
		<xsl:variable name="image" select="xalan:nodeset($image_)"/>
		
		<xsl:variable name="ref" select="$image/@foi:struct-ref"/>
		
		<xsl:variable name="instream-foreign-object_alt-text" select="$instream-foreign-objects//fo:instream-foreign-object[@foi:struct-id = $ref]/@fox:alt-text"/>
		
		<xsl:variable name="width" select="$image/@width"/>
		<xsl:variable name="height" select="$image/@height"/>
		
		<xsl:copy>
			<xsl:copy-of select="@*"/>
			
				<xsl:if test="normalize-space($instream-foreign-object_alt-text) != '' and $instream-foreign-object_alt-text != 'Math'">
				<!-- <xsl:variable name="fontsize" select="java:org.metanorma.fop.Util.getFontSize($font_family, $width, $height)"/> -->
				<xsl:variable name="fontsize">10000</xsl:variable>
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
				<xsl:attribute name="foi:struct-ref"><xsl:value-of select="$ref"/></xsl:attribute>
				<xsl:value-of select="$instream-foreign-object_alt-text"/>
			</xsl:element>
		</xsl:if>
		
	</xsl:template>

</xsl:stylesheet>
