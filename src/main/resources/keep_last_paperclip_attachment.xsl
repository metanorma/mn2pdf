<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
	xmlns="http://xmlgraphics.apache.org/fop/intermediate" 
	xmlns:im="http://xmlgraphics.apache.org/fop/intermediate" 
	xmlns:nav="http://xmlgraphics.apache.org/fop/intermediate/document-navigation"
	xmlns:fo="http://www.w3.org/1999/XSL/Format" 
	xmlns:foi="http://xmlgraphics.apache.org/fop/internal"   
	xmlns:fox="http://xmlgraphics.apache.org/fop/extensions"
	xmlns:xalan="http://xml.apache.org/xalan"  
	xmlns:jeuclid="http://jeuclid.sf.net/ns/ext"
	xmlns:java="http://xml.apache.org/xalan/java"
	xmlns:math="http://exslt.org/math"
	exclude-result-prefixes="im xalan fo fox math java"
	version="1.0">

	<xsl:output version="1.0" method="xml" encoding="UTF-8" indent="yes"/>
	
	<xsl:template match="@*|node()">
		<xsl:copy>
				<xsl:apply-templates select="@*|node()"/>
		</xsl:copy>
	</xsl:template>
	
	<!-- https://github.com/metanorma/mn2pdf/issues/430 -->
	<xsl:template match="nav:link[starts-with(nav:goto-uri/@uri, 'embedded-file:')]">
		<xsl:choose>
			<xsl:when test="following-sibling::*[1][self::nav:link]/@foi:struct-ref = current()/@foi:struct-ref and
				following-sibling::*[1][self::nav:link]/nav:goto-uri/@uri = current()/nav:goto-uri/@uri"><!-- skip --></xsl:when>
			<xsl:otherwise><xsl:copy-of select="."/></xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	
</xsl:stylesheet>
