<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<xsl:stylesheet version="2.0"
xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
<xsl:output method="xml" indent="yes"/>
<xsl:template match="/">
	<profile chip="{Schematic/IC/PartNumber}">
		<program target="{Schematic/IC/Name}">
			<metadata>
				<xsl:apply-templates select="Schematic/IC/Module"/>
			</metadata>
			<xsl:apply-templates select="Schematic/IC/Register|Schematic/IC/Program"/>
		</program>
	</profile>
</xsl:template>
<xsl:template match="Register[starts-with(Name,'__SafeLoad_Module')]"/>
<xsl:template match="Register[ends-with(Name,'Delay')]">
	<action type="delay"><xsl:value-of select="replace(replace(replace(Data,',',''),'0x',''),'  *$','')"/></action>
</xsl:template>
<xsl:template match="Register|Program">
	<action type="poke" name="{Name}" addr="{Address}"><xsl:value-of select="replace(replace(replace(Data,',',''),'0x',''),'  *$','')"/></action>
</xsl:template>
<xsl:template match="Module[starts-with(Algorithm/DetailedName,'Mute')]">
	<register type="mute" name="{CellName}" addr="{Algorithm/ModuleParameter[ends-with(Name,'mute')]/Address}"/>
</xsl:template>
<xsl:template match="Module[starts-with(Algorithm/DetailedName,'HWGain')]">
	<register type="gain" name="{CellName}" addr="{Algorithm/ModuleParameter[ends-with(Name,'target')]/Address}"/>
</xsl:template>
<xsl:template match="Module[starts-with(Algorithm/DetailedName,'DCInp')]">
	<register type="data" name="{CellName}" addr="{Algorithm/ModuleParameter[ends-with(Name,'value')]/Address}"/>
</xsl:template>
<xsl:template match="Module[starts-with(Algorithm/DetailedName,'RegisterRead')]">
	<register type="register" name="{CellName}" addr="{Algorithm/ModuleParameter[ends-with(Name,'address0')]/Value}"/>
</xsl:template>
<xsl:template match="Module"/>
</xsl:stylesheet>