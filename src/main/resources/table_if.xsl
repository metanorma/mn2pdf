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
		
		<xsl:variable name="ids_">
			<if:ids>
				<xsl:for-each select="//if:id[starts-with(@name, $table_if_prefix)]">
					
					<xsl:if test="not(preceding-sibling::if:id[starts-with(@name, $table_if_prefix)]) or starts-with(@name,$table_if_start_prefix)"> <!-- select only first in 'g', no need select 'id' ends with '_end' -->

						<if:id>
							<xsl:copy-of select="@*"/>
							<xsl:variable name="id_cell" select="@name"/>
							
							<xsl:variable name="position_end" select="following-sibling::if:id[@name = concat($id_cell, '_end')]/following-sibling::if:text[1]/@x"/>
							<xsl:variable name="padding-left_" select="normalize-space(substring-before(substring-after(ancestor::if:g[1]/@transform, '('), ','))"/>
							<xsl:variable name="padding-left">
								<xsl:choose>
									<xsl:when test="$padding-left_ = ''">0</xsl:when>
									<xsl:otherwise><xsl:value-of select="$padding-left_"/></xsl:otherwise>
								</xsl:choose>
							</xsl:variable>
							
							<xsl:attribute name="position_end"><xsl:value-of select="$position_end"/></xsl:attribute>
							<xsl:attribute name="padding-left"><xsl:value-of select="$padding-left"/></xsl:attribute>
							
							<!-- for starts-with(@name,$table_if_start_prefix) -->
							<xsl:variable name="width_viewport" select="ancestor::if:viewport[1]/@width"/>
							<xsl:variable name="width_border-rect" select="following-sibling::if:g[1]/if:border-rect/@width"/>
							<xsl:attribute name="width_viewport"><xsl:value-of select="$width_viewport"/></xsl:attribute>
							<xsl:attribute name="width_border-rect"><xsl:value-of select="$width_border-rect"/></xsl:attribute>
							
						</if:id>
					</xsl:if>
				</xsl:for-each>
			</if:ids>
		</xsl:variable>
		
		<xsl:variable name="ids" select="xalan:nodeset($ids_)"/>
		
		<tables>
			<!-- <xsl:apply-templates select="//if:id[starts-with(@name,$table_if_start_prefix)]"> -->
			<xsl:apply-templates select="$ids//if:id[starts-with(@name,$table_if_start_prefix)]">
				<xsl:with-param name="ids" select="$ids"/>
			</xsl:apply-templates>
			
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
		<xsl:param name="ids"/>
		
		<xsl:variable name="id" select="substring-after(@name, $table_if_start_prefix)"/>
		
		<!-- <xsl:variable name="width_viewport" select="ancestor::if:viewport[1]/@width"/> -->
		<xsl:variable name="width_viewport" select="@width_viewport"/>
		<!-- <xsl:variable name="width_border-rect" select="following-sibling::if:g[1]/if:border-rect/@width"/> -->
		<xsl:variable name="width_border-rect" select="@width_border-rect"/>
		
		<xsl:variable name="page-width">
			<xsl:choose>
				<xsl:when test="$width_border-rect &lt; $width_viewport"><xsl:value-of select="$width_border-rect"/></xsl:when>
				<xsl:otherwise><xsl:value-of select="$width_viewport"/></xsl:otherwise>
			</xsl:choose>
		</xsl:variable>
		
		
		<xsl:variable name="table_id" select="concat($table_if_prefix, $id, '_')"/>
		
		<xsl:variable name="cells_">
			<!-- <xsl:for-each select="//if:id[starts-with(@name, $table_id)][1]"> --> <!-- select only first in 'g', no need select 'id' ends with '_end' -->
			<xsl:for-each select="$ids//if:id[starts-with(@name, $table_id)]"> 
			
				<xsl:variable name="id_cell" select="@name"/>
				
				<xsl:variable name="position_start" select="0"/> <!-- 0, because text/image can have left padding/margin --> <!-- following-sibling::if:*[1]/@x following-sibling::if:text[1]/@x --> <!-- text or image --> 
				<!-- <xsl:variable name="position_end" select="following-sibling::if:id[@name = concat($id_cell, '_end')]/following-sibling::if:text[1]/@x"/> -->
				<xsl:variable name="position_end" select="@position_end"/>
				
				<!-- <xsl:variable name="padding-left_" select="normalize-space(substring-before(substring-after(ancestor::if:g[1]/@transform, '('), ','))"/>
				<xsl:variable name="padding-left">
					<xsl:choose>
						<xsl:when test="$padding-left_ = ''">0</xsl:when>
						<xsl:otherwise><xsl:value-of select="$padding-left_"/></xsl:otherwise>
					</xsl:choose>
				</xsl:variable> -->
				
				<xsl:variable name="padding-left" select="@padding-left"/>
				
				<xsl:variable name="id_suffix" select="substring-after(@name, $table_id)"/>
				<!-- <xsl:variable name="id_suffix_components" select="str:split($id_suffix, '_')"/> --> <!-- slow -->
				<xsl:variable name="regex_id_suffix_components">^([^_]*)_([^_]*)_([^_]*)_([^_]*)_([^_]*).*$</xsl:variable> <!-- example: table_if_table10_1_3_word_1_6 , 6 means length divider for column spanned-->
				<xsl:variable name="id_suffix_components_row" select="java:replaceAll(java:java.lang.String.new($id_suffix), $regex_id_suffix_components, '$1')"/>
				<xsl:variable name="id_suffix_components_col" select="java:replaceAll(java:java.lang.String.new($id_suffix), $regex_id_suffix_components, '$2')"/>
				<xsl:variable name="id_suffix_components_type" select="java:replaceAll(java:java.lang.String.new($id_suffix), $regex_id_suffix_components, '$3')"/>
				<xsl:variable name="id_suffix_components_divide" select="java:replaceAll(java:java.lang.String.new($id_suffix), $regex_id_suffix_components, '$5')"/>
				
				<!-- <cell id="{$id_suffix}" row="{$id_suffix_components[1]}" col="{$id_suffix_components[2]}" type="{$id_suffix_components[3]}" length="{$position_end - $position_start + $padding-left}" position_start="{$position_start}" position_end="{$position_end}" padding-left="{$padding-left}"/> -->
				<cell id="{$id_suffix}" row="{$id_suffix_components_row}" col="{$id_suffix_components_col}" type="{$id_suffix_components_type}" length="{$position_end - $position_start + $padding-left}" divide="{$id_suffix_components_divide}" position_start="{$position_start}" position_end="{$position_end}" padding-left="{$padding-left}"/>

			</xsl:for-each>
		</xsl:variable>
		
		<xsl:variable name="cells" select="xalan:nodeset($cells_)"/>
		
		<xsl:variable name="table_body_">
			<tbody>
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
												<!-- <divide><xsl:value-of select="@divide"/></divide> -->
												<xsl:choose>
													<xsl:when test="@type = 'p'">
														<p_len><xsl:value-of select="round(@length div @divide)"/></p_len>
													</xsl:when>
													<xsl:otherwise>
														<word_len><xsl:value-of select="round(@length div @divide)"/></word_len>
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
										
										<xsl:if test="not($lengths/word_len)"> <!-- special case for empty cells -->
											<word_len><xsl:value-of select="$lengths/p_len"/></word_len>
										</xsl:if>
										
									</td>
								</xsl:if>
							</xsl:for-each> <!-- iteration by rows -->
						</tr>
					</xsl:if>
				</xsl:for-each>
			</tbody>
		</xsl:variable>
		<xsl:variable name="table_body" select="xalan:nodeset($table_body_)"/>
		
		
		<xsl:variable name="table_with_cell_widths_">
			<xsl:apply-templates select="$table_body" mode="determine_cell_widths-if"/>
		</xsl:variable>
		<xsl:variable name="table_with_cell_widths" select="xalan:nodeset($table_with_cell_widths_)"/>
		
		
		<!-- The minimum and maximum cell widths are then used to determine the corresponding minimum and maximum widths for the columns. -->
		<xsl:variable name="column_widths_">
			<!-- iteration of columns -->
			<xsl:for-each select="$table_with_cell_widths//tr[1]/td">
				<xsl:variable name="pos" select="position()"/>
				<column>
					<xsl:attribute name="width_max">
						<xsl:for-each select="ancestor::tbody//tr/td[$pos]/@width_max">
							<xsl:sort select="." data-type="number" order="descending"/>
							<xsl:if test="position() = 1"><xsl:value-of select="."/></xsl:if>
						</xsl:for-each>
					</xsl:attribute>
					<!-- <xsl:attribute name="width_max_all">
						<xsl:for-each select="ancestor::tbody//tr/td[$pos]/@width_max">
							<xsl:value-of select="."/><xsl:text> </xsl:text>
						</xsl:for-each>
					</xsl:attribute> -->
					<xsl:attribute name="width_min">
						<xsl:for-each select="ancestor::tbody//tr/td[$pos]/@width_min">
							<xsl:sort select="." data-type="number" order="descending"/>
							<xsl:if test="position() = 1"><xsl:value-of select="."/></xsl:if>
						</xsl:for-each>
					</xsl:attribute>
					<!-- <xsl:attribute name="width_min_all">
						<xsl:for-each select="ancestor::tbody//tr/td[$pos]/@width_min">
							<xsl:value-of select="."/><xsl:text> </xsl:text>
						</xsl:for-each>
					</xsl:attribute> -->
				</column>
			</xsl:for-each>
		</xsl:variable>
		<xsl:variable name="column_widths" select="xalan:nodeset($column_widths_)"/>
				
		<table id="{$id}" page-width="{$page-width}">
		
			<!-- These in turn, are used to find the minimum and maximum width for the table. -->
			<xsl:variable name="width_max" select="sum($column_widths/column/@width_max)"/>
			<xsl:variable name="width_min" select="sum($column_widths/column/@width_min)"/>
			<xsl:attribute name="width_max">
				<xsl:value-of select="$width_max"/>
			</xsl:attribute>
			<xsl:attribute name="width_min">
				<xsl:choose>
					<xsl:when test="$width_max - $width_min = 0"><xsl:value-of select="$width_min - 1"/></xsl:when> <!-- to prevent division by zero in 'w' calculation -->
					<xsl:otherwise>
						<xsl:value-of select="width_min"/>
					</xsl:otherwise>
				</xsl:choose>
			</xsl:attribute>
		
			<xsl:copy-of select="$column_widths"/>
			
			<xsl:copy-of select="$table_with_cell_widths"/>
			
		</table>
	</xsl:template>
	
	<!-- ============================= -->
	<!-- mode: determine_cell_widths-if -->
	<!-- ============================= -->
	<!-- In the first pass, line wrapping is disabled, and the user agent keeps track of the minimum and maximum width of each cell. -->
	<xsl:template match="@*|node()" mode="determine_cell_widths-if">
		<xsl:copy>
				<xsl:apply-templates select="@*|node()" mode="determine_cell_widths-if"/>
		</xsl:copy>
	</xsl:template>
	
	<xsl:template match="td | th" mode="determine_cell_widths-if">
		<xsl:copy>
			<xsl:copy-of select="@*"/>
			
			 <!-- The maximum width is given by the widest line.  -->
			<xsl:attribute name="width_max">
				<xsl:for-each select="p_len">
					<xsl:sort select="." data-type="number" order="descending"/>
					<xsl:if test="position() = 1"><xsl:value-of select="."/></xsl:if>
				</xsl:for-each>
			</xsl:attribute>
			
			<!-- The minimum width is given by the widest text element (word, image, etc.) -->
			<xsl:variable name="width_min">
				<xsl:for-each select="word_len">
					<xsl:sort select="." data-type="number" order="descending"/>
					<xsl:if test="position() = 1"><xsl:value-of select="."/></xsl:if>
				</xsl:for-each>
			</xsl:variable>
			<xsl:attribute name="width_min">
				<xsl:value-of select="$width_min"/>
			</xsl:attribute>
			
			<xsl:if test="$width_min = 0">
				<xsl:attribute name="width_min">1</xsl:attribute>
			</xsl:if>
			
			<xsl:apply-templates select="node()" mode="determine_cell_widths-if"/>
			
		</xsl:copy>
	</xsl:template>
	<!-- ============================= -->
	<!-- END mode: determine_cell_widths-if -->
	<!-- ============================= -->
	
	
</xsl:stylesheet>
