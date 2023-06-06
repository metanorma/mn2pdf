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

	<xsl:template match="*[starts-with(local-name(), 'semantic__')]" />
	
	<xsl:template match="*[local-name() = 'presentation-metadata'][*[local-name() = 'name']/text() = 'coverpage-image']"/>
	
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
						][not(.//*[local-name() = 'table' or local-name() = 'dl' or local-name() = 'toc' or local-name() = 'pagebreak'])][not(ancestor::*[local-name() = 'table' or local-name() = 'dl' or local-name() = 'toc'])]"/>

	<xsl:template match="*[local-name() = 'bibliography' or local-name() = 'references'][not(.//*[local-name() = 'table' or local-name() = 'dl' or local-name() = 'toc' or local-name() = 'pagebreak'])][not(ancestor::*[local-name() = 'table' or local-name() = 'dl' or local-name() = 'toc'])]"/>

	<!-- no need calculate table width, if there are column widths -->
	<xsl:template match="*[local-name() = 'table'][*[local-name() = 'colgroup']/*[local-name() = 'col']]" priority="2"/>

	<xsl:template match="*[local-name() = 'bookmark']"/>
	
	<xsl:template match="*[local-name() = 'fn']"/>
	
	<xsl:template match="*[local-name() = 'table' or local-name() = 'dl']">
		<xsl:choose>
			<xsl:when test="ancestor::*[local-name() = 'table' or local-name() = 'dl']"> <!-- if there is parent table / definition list -->
				
				
				<xsl:choose>
					<xsl:when test="local-name() = 'table'">
					
						<xsl:for-each select=".//*[local-name() = 'tr']">
							<xsl:variable name="ns" select="namespace-uri()"/>
							<xsl:element name="p" namespace="{$ns}">
								<xsl:for-each select=".//*[local-name() = 'td']">
									<xsl:apply-templates mode="simple_td"/>
									<xsl:text> </xsl:text>
								</xsl:for-each>
							</xsl:element>
						</xsl:for-each>
					
					</xsl:when> <!-- table -->
					
					<xsl:when test="local-name() = 'dl'">
					
						<!-- convert definition list to paragraphs -->
						<xsl:for-each select=".//*[local-name() = 'dt']">
							<xsl:variable name="ns" select="namespace-uri()"/>
							<xsl:element name="p" namespace="{$ns}">
								<xsl:attribute name="from_dl">true</xsl:attribute>
								<xsl:copy-of select="node()"/>
								<xsl:text> </xsl:text>
								<xsl:apply-templates select="following-sibling::*[local-name()='dd'][1]/*[local-name() = 'p']/node()" />
							</xsl:element>
						</xsl:for-each>
					</xsl:when> <!-- dl -->
					
				</xsl:choose>
			</xsl:when>
			<xsl:otherwise> <!-- table/dl doesn't have parent tables / definition lists, i.e. most upper element -->
				
				<xsl:copy>
					<xsl:apply-templates select="@*|node()"/>
				</xsl:copy>
				
				<!-- put child tables / definition lists after table -->
				
				<!-- isolate child table and dl from parent table context -->
				<xsl:variable name="table_dl_id" select="@id"/>
				<xsl:variable name="child_tables_dl">
					<xsl:for-each select=".//*[local-name() = 'table' or local-name() = 'dl'][ancestor::*[local-name() = 'table' or local-name() = 'dl'][1][@id = $table_dl_id]]">
						<xsl:copy-of select="."/>
					</xsl:for-each>
				</xsl:variable>
				<!-- <iter> -->
				<xsl:apply-templates select="xalan:nodeset($child_tables_dl)/*" />
				<!-- </iter> -->
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	
	<xsl:template match="*[local-name() = 'table'][@type = 'sourcecode']" priority="2"/>

	<xsl:template match="@*|node()" mode="simple_td">
		<xsl:copy>
				<xsl:apply-templates select="@*|node()" mode="simple_td"/>
		</xsl:copy>
	</xsl:template>
	
	<xsl:template match="*[local-name() = 'dl']" mode="simple_td">
		<!-- convert definition list to paragraphs -->
		<xsl:for-each select=".//*[local-name() = 'dt']">
			<xsl:variable name="ns" select="namespace-uri()"/>
			<xsl:element name="p" namespace="{$ns}">
				<xsl:attribute name="from_dl">true</xsl:attribute>
				<xsl:copy-of select="node()"/>
				<xsl:text> </xsl:text>
				<xsl:apply-templates select="following-sibling::*[local-name()='dd'][1]/*[local-name() = 'p']/node()" mode="simple_td"/>
			</xsl:element>
		</xsl:for-each>
	</xsl:template>
	
</xsl:stylesheet>
