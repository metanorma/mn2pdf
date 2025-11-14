<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
											xmlns:fo="http://www.w3.org/1999/XSL/Format" 
											xmlns:xalan="http://xml.apache.org/xalan" 
											xmlns:java="http://xml.apache.org/xalan/java"
											xmlns="http://ns.adobe.com/xfdf/"
											exclude-result-prefixes="java xalan"
											version="1.0">

	<xsl:output method="xml" encoding="UTF-8" indent="no"/>
	
	<xsl:param name="override_xsl"/>
	
	<!-- <xsl:variable name="override_xsl_xml" select="xalan:nodeset($override_xsl)"/> -->
	<xsl:variable name="override_xsl_xml" select="document($override_xsl)"/>
	
	<xsl:variable name="main_xsl_xml_">
		<xsl:copy-of select="."/>
	</xsl:variable>
	<xsl:variable name="main_xsl_xml" select="xalan:nodeset($main_xsl_xml_)"/>
	
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
	
	<!-- Example: <xsl:template name="...", or <xsl:template match="..." name="..." -->
	<xsl:template match="xsl:stylesheet/xsl:template[@name]">
		<xsl:choose>
			<xsl:when test="$override_xsl_xml/xsl:stylesheet/xsl:template[@name = current()/@name]">
				<!-- replace/extend the content of the template -->
				<xsl:copy>
					<xsl:copy-of select="@*"/> <!-- copy all attributes, includes 'match' -->
					<!-- if 'extend' then add existing code -->
					<xsl:if test="$override_xsl_xml/xsl:stylesheet/xsl:template[@name = current()/@name]/processing-instruction('extend')">
						<xsl:copy-of select="node()"/>
					</xsl:if>
					<!-- add code from the override xsl -->
					<xsl:copy-of select="$override_xsl_xml/xsl:stylesheet/xsl:template[@name = current()/@name]/node()"/>
				</xsl:copy>
			</xsl:when>
			<xsl:otherwise><xsl:copy-of select="."/></xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	
	<xsl:template match="xsl:stylesheet/xsl:template[@name = 'layout-master-set']">
		<xsl:copy>
			<xsl:copy-of select="@*"/>
			<xsl:apply-templates select="node()"/>
		</xsl:copy>
	</xsl:template>
	
	<xsl:template match="xsl:stylesheet/xsl:template[@name = 'layout-master-set']/fo:layout-master-set">
		<xsl:copy>
			<xsl:copy-of select="@*"/>
			<xsl:apply-templates select="node()"/>
			
			<xsl:variable name="nodes_">
				<xsl:copy-of select="node()"/>
			</xsl:variable>
			<xsl:variable name="nodes" select="xalan:nodeset($nodes_)"/>
			
			<!-- add elements from the override xsl which non exist in the fo:layout-master-set-->
			<xsl:for-each select="$override_xsl_xml/xsl:stylesheet/xsl:template[@name = 'layout-master-set']/fo:layout-master-set/node()">
				<xsl:choose>
					<xsl:when test="$nodes//*[@master-name = current()/@master-name]"><!-- skip --></xsl:when>
					<xsl:otherwise><xsl:copy-of select="."/></xsl:otherwise>
				</xsl:choose>
			</xsl:for-each>
		</xsl:copy>
	</xsl:template>
	
	<xsl:template match="xsl:stylesheet/xsl:template[@name = 'layout-master-set']/fo:layout-master-set/*">
		<xsl:choose>
			<!-- if in override xslt there is <fo:simple-page-master or <fo:page-sequence-master with @master-name with same name, then replace it -->
			<xsl:when test="$override_xsl_xml/xsl:stylesheet/xsl:template[@name = 'layout-master-set']/fo:layout-master-set/*[@master-name = current()/@master-name]">
				<xsl:copy-of select="$override_xsl_xml/xsl:stylesheet/xsl:template[@name = 'layout-master-set']/fo:layout-master-set/*[@master-name = current()/@master-name]"/>
			</xsl:when>
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
			<xsl:when test="$override_xsl_xml/xsl:stylesheet/xsl:attribute-set[@name = current()/@name]">
				<xsl:choose>
					<xsl:when test="$override_xsl_xml/xsl:stylesheet/xsl:attribute-set[@name = current()/@name]/processing-instruction('extend')">
						<xsl:copy>
							<xsl:copy-of select="@*"/>
							<xsl:copy-of select="node()"/>
							<xsl:copy-of select="$override_xsl_xml/xsl:stylesheet/xsl:attribute-set[@name = current()/@name]/node()"/>
						</xsl:copy>
					</xsl:when>
					<xsl:otherwise><!-- skip from the main xsl --></xsl:otherwise>
				</xsl:choose>
			</xsl:when>
			<xsl:otherwise><xsl:copy-of select="."/></xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	
	<xsl:template match="@*|node()" mode="override">
		<xsl:copy>
			<xsl:apply-templates select="@*|node()" mode="override"/>
		</xsl:copy>
	</xsl:template>
	
	<xsl:template match="xsl:template[@name]" mode="override">
		<xsl:choose>
			<xsl:when test="$main_xsl_xml/xsl:stylesheet/xsl:template[@name = current()/@name]"><!-- skip, replaced above --></xsl:when>
			<xsl:otherwise><xsl:copy-of select="."/></xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	
	<xsl:template match="xsl:stylesheet/xsl:attribute-set[processing-instruction('extend')]" mode="override">
		<xsl:choose>
			<xsl:when test="$main_xsl_xml/xsl:stylesheet/xsl:attribute-set[@name = current()/@name]"><!-- skip, replaced/extended above --></xsl:when>
			<xsl:otherwise><xsl:copy-of select="."/></xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	
	<!-- the elements from xsl:template name="layout-master-set" added above -->
	<xsl:template match="xsl:template[@name = 'layout-master-set']" mode="override"/>
	
</xsl:stylesheet>
