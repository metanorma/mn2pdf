<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
											xmlns:xalan="http://xml.apache.org/xalan" 
											xmlns:java="http://xml.apache.org/xalan/java"
											xmlns="http://ns.adobe.com/xfdf/"
											exclude-result-prefixes="java xalan"
											version="1.0">

	<xsl:output method="xml" encoding="UTF-8" indent="no"/>
	
	<xsl:param name="override_xsl"/>
	
	<!-- <xsl:variable name="override_xsl_xml" select="xalan:nodeset($override_xsl)"/> -->
	<xsl:variable name="override_xsl_xml" select="document($override_xsl)"/>
	
	<xsl:template match="@*|node()">
		<xsl:copy>
			<xsl:apply-templates select="@*|node()"/>
		</xsl:copy>
	</xsl:template>
	
	<xsl:template match="xsl:stylesheet">
		<xsl:copy>
			<xsl:apply-templates select="@*|node()"/>
			<!-- add templates from the override xsl -->
			<xsl:apply-templates select="$override_xsl_xml/xsl:stylesheet/node()" mode="override"/>
		</xsl:copy>
	</xsl:template>
	
	<xsl:template match="xsl:stylesheet/xsl:template[@name]">
		<xsl:choose>
			<xsl:when test="$override_xsl_xml/xsl:stylesheet/xsl:template[@name = current()/@name]"><!-- skip from the main xsl --></xsl:when>
			<xsl:otherwise><xsl:copy-of select="."/></xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	
	<xsl:template match="xsl:stylesheet/xsl:variable">
		<xsl:choose>
			<xsl:when test="$override_xsl_xml/xsl:stylesheet/xsl:variable[@name = current()/@name]"><!-- skip from the main xsl --></xsl:when>
			<xsl:otherwise><xsl:copy-of select="."/></xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	
	<xsl:template match="xsl:stylesheet/xsl:attribute-set">
		<xsl:choose>
			<xsl:when test="$override_xsl_xml/xsl:stylesheet/xsl:attribute-set[@name = current()/@name]"><!-- skip from the main xsl --></xsl:when>
			<xsl:otherwise><xsl:copy-of select="."/></xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	
	<xsl:template match="@*|node()" mode="override">
		<xsl:copy>
			<xsl:apply-templates select="@*|node()" mode="override"/>
		</xsl:copy>
	</xsl:template>
	
</xsl:stylesheet>
