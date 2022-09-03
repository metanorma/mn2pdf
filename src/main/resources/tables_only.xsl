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

	<xsl:output version="1.0" method="xml" encoding="UTF-8" indent="no"/>
	
	<xsl:template match="@*|node()">
		<xsl:copy>
				<xsl:apply-templates select="@*|node()"/>
		</xsl:copy>
	</xsl:template>
	
	<xsl:template match="*[local-name() = 'preface' or 
				local-name() = 'sections' or 
				local-name() = 'annex' or 
				local-name() = 'indexsect'
				]//*[local-name() = 'p' or 
						local-name() = 'ul' or 
						local-name() = 'ol' or
						local-name() = 'note' or
						local-name() = 'example' or
						local-name() = 'terms' or
						local-name() = 'term' or
						local-name() = 'quote' or 
						local-name() = 'formula' or 
						local-name() = 'figure' or 
						local-name() = 'toc'
						][not(.//*[local-name() = 'table' or local-name() = 'dl' or local-name() = 'toc'])][not(ancestor::*[local-name() = 'table' or local-name() = 'dl' or local-name() = 'toc'])]"/>

	<xsl:template match="*[local-name() = 'bookmark']"/>
	
	<xsl:template match="*[local-name() = 'fn']"/>
	
</xsl:stylesheet>
