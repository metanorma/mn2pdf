<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
											xmlns:fo="http://www.w3.org/1999/XSL/Format" 
											xmlns:if="http://xmlgraphics.apache.org/fop/intermediate" 
											xmlns:xalan="http://xml.apache.org/xalan" 
											xmlns:java="http://xml.apache.org/xalan/java"
											xmlns:str="http://exslt.org/strings"
											exclude-result-prefixes="fo if xalan java str"
											version="1.0">

	<xsl:output method="xml" encoding="UTF-8" indent="no"/>
	
	<xsl:template match="@*|node()">
		<xsl:copy>
			<xsl:apply-templates select="@*|node()" />
		</xsl:copy>
	</xsl:template>
		
	<xsl:template match="tbody"/>
		
</xsl:stylesheet>
