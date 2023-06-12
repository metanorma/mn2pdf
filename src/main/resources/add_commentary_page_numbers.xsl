<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
	xmlns="http://xmlgraphics.apache.org/fop/intermediate" 
	xmlns:im="http://xmlgraphics.apache.org/fop/intermediate" 
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
	
	<xsl:template match="im:text[preceding-sibling::*[1][self::im:id and @name = '_independent_page_number_commentary']]/text()">
		<!-- set page number for commentary -->
		<xsl:value-of select="count(ancestor::im:page/preceding-sibling::im:page[.//im:viewport[@region-type = 'Footer']/im:id[@name = '_independent_page_number_commentary']]) + 1"/>
	</xsl:template>
	
</xsl:stylesheet>
