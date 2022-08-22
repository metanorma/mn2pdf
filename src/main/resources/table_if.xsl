<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
											xmlns:fo="http://www.w3.org/1999/XSL/Format" 
											xmlns:if="http://xmlgraphics.apache.org/fop/intermediate" 
											xmlns:xalan="http://xml.apache.org/xalan" 
											xmlns:java="http://xml.apache.org/xalan/java"
											xmlns:str="http://exslt.org/strings"
											exclude-result-prefixes="java str"
											version="1.0">

	<xsl:output method="xml" encoding="UTF-8" indent="no"/>
	
	<xsl:variable name="table_if_prefix">table_if_</xsl:variable>
	<xsl:variable name="table_if_start_prefix">table_if_start_</xsl:variable>
	
	<xsl:template match="/">
		<tables>
			<xsl:apply-templates select="//if:id[starts-with(@name,$table_if_start_prefix)]"/>
		</tables>
	</xsl:template>
	
	
	<!-- Example Apache IF xml:
		<id name="table_if_start__8b344565-be03-26db-afc3-602d2bd0d888"/>
		<text x="0" y="12877" foi:struct-ref="57"> </text>
		<g transform="translate(3334,12700)">
			<border-rect x="-3834" y="0" width="465527" height="16617" ... "/>
			<font size="11000"/>
			<id name="table_if__8b344565-be03-26db-afc3-602d2bd0d888_1_1_p_1"/>
			<text x="0" y="12069" foi:struct-ref="58">BC</text>
			<id name="table_if__8b344565-be03-26db-afc3-602d2bd0d888_1_1_p_1_end"/>
			<text x="12903" y="12069" foi:struct-ref="59"> </text>
		</g>
		...
	-->
	
	<xsl:template match="if:id">
		<xsl:variable name="id" select="substring-after(@name, $table_if_start_prefix)"/>
		
		<xsl:variable name="width_viewport" select="ancestor::if:viewport[1]/@width"/>
		<xsl:variable name="width_border-rect" select="following-sibling::if:g[1]/if:border-rect/@width"/>
		
		<xsl:variable name="page-width">
			<xsl:choose>
				<xsl:when test="$width_border-rect &lt; $width_viewport"><xsl:value-of select="$width_border-rect"/></xsl:when>
				<xsl:otherwise><xsl:value-of select="$width_viewport"/></xsl:otherwise>
			</xsl:choose>
		</xsl:variable>
		
		<table id="{$id}" page-width="{$page-width}">
		
			<tbody>
				
				<xsl:variable name="table_id" select="concat($table_if_prefix, $id, '_')"/>
				
				<xsl:variable name="cells_">
					<xsl:for-each select="//if:id[starts-with(@name, $table_id)][1]"> <!-- select only first in 'g', no need select 'id' ends with '_end' -->
					
						<xsl:variable name="id_cell" select="@name"/>
					
						<!-- <id_cell><xsl:value-of select="$id_cell"/></id_cell> -->
					
						<xsl:variable name="position_start" select="0"/> <!-- 0, because text/image can have left padding/margin --> <!-- following-sibling::if:*[1]/@x following-sibling::if:text[1]/@x --> <!-- text or image --> 
						<xsl:variable name="position_end" select="following-sibling::if:id[@name = concat($id_cell, '_end')]/following-sibling::if:text[1]/@x"/>
						
						<xsl:variable name="padding-left_" select="normalize-space(substring-before(substring-after(ancestor::if:g[1]/@transform, '('), ','))"/>
						<xsl:variable name="padding-left">
							<xsl:choose>
								<xsl:when test="$padding-left_ = ''">0</xsl:when>
								<xsl:otherwise><xsl:value-of select="$padding-left_"/></xsl:otherwise>
							</xsl:choose>
						</xsl:variable>
						
						<xsl:variable name="id_suffix" select="substring-after(@name, $table_id)"/>
						<!-- <xsl:variable name="id_suffix_components" select="str:split($id_suffix, '_')"/> -->
						<xsl:variable name="regex_id_suffix_components">^([^_]*)_([^_]*)_([^_]*).*$</xsl:variable>
						<xsl:variable name="id_suffix_components_row" select="java:replaceAll(java:java.lang.String.new($id_suffix), $regex_id_suffix_components, '$1')"/>
						<xsl:variable name="id_suffix_components_col" select="java:replaceAll(java:java.lang.String.new($id_suffix), $regex_id_suffix_components, '$2')"/>
						<xsl:variable name="id_suffix_components_type" select="java:replaceAll(java:java.lang.String.new($id_suffix), $regex_id_suffix_components, '$3')"/>
						
						<!-- <cell id="{$id_suffix}" row="{$id_suffix_components[1]}" col="{$id_suffix_components[2]}" type="{$id_suffix_components[3]}" length="{$position_end - $position_start + $padding-left}" position_start="{$position_start}" position_end="{$position_end}" padding-left="{$padding-left}"/> -->
						<cell id="{$id_suffix}" row="{$id_suffix_components_row}" col="{$id_suffix_components_col}" type="{$id_suffix_components_type}" length="{$position_end - $position_start + $padding-left}" position_start="{$position_start}" position_end="{$position_end}" padding-left="{$padding-left}"/>

					</xsl:for-each>
				</xsl:variable>
				
				<xsl:variable name="cells" select="xalan:nodeset($cells_)"/>
				
				<!-- <debug>
					<xsl:copy-of select="$cells"/>
				</debug> -->
				
				<xsl:for-each select="$cells/cell">
					<xsl:variable name="row" select="@row"/>
					<xsl:if test="not(preceding-sibling::cell[@row = $row])"> 
						<tr>
							<xsl:for-each select="$cells/cell[@row = $row]"> <!-- iteration by rows -->
								<xsl:variable name="col" select="@col"/>
							
								<xsl:if test="not(preceding-sibling::cell[@row = $row and @col = $col])">
									<td>
										
										<xsl:variable name="lengths_">
											<xsl:for-each select="$cells/cell[@row = $row and @col = $col]"> <!-- select all 'cell' relate to one source table cell -->
												<xsl:choose>
													<xsl:when test="@type = 'p'">
														<p_len><xsl:value-of select="@length"/></p_len>
													</xsl:when>
													<xsl:otherwise>
														<word_len><xsl:value-of select="@length"/></word_len>
													</xsl:otherwise>
												</xsl:choose>
											</xsl:for-each>
										</xsl:variable>
										<xsl:variable name="lengths" select="xalan:nodeset($lengths_)"/>
										
										<xsl:for-each select="$lengths/*">
											<xsl:copy>
												<xsl:choose>
													<xsl:when test="self::p_len and . = 'NaN'"><xsl:value-of select="sum($lengths/word_len)"/></xsl:when>
													<xsl:otherwise><xsl:value-of select="."/></xsl:otherwise>
												</xsl:choose>
											</xsl:copy>
										</xsl:for-each>
										
									</td>
								</xsl:if>
							</xsl:for-each> <!-- iteration by rows -->
						</tr>
					</xsl:if>
				</xsl:for-each>
				
			</tbody>
		</table>
	</xsl:template>
	
</xsl:stylesheet>
