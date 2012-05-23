<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:wps="http://www.opengis.net/wps/1.0.0"
                xmlns:ows="http://www.opengis.net/ows/1.1"
                xmlns:xlink="http://www.w3.org/1999/xlink">
  <xsl:output method="text"/>

  <!-- parameters -->

  <xsl:param name="calvalus.input" />
  <xsl:param name="calvalus.task.id">default-task-id</xsl:param>
  <xsl:param name="calvalus.package.dir">/home/hadoop/opt/megs-8.0</xsl:param>
  <xsl:param name="calvalus.archive.mount">/mnt/hdfs</xsl:param>
  <xsl:param name="calvalus.tmp.dir">/home/hadoop/tmp/<xsl:value-of select="$calvalus.task.id" /></xsl:param>
  <xsl:variable name="megs.executable">bin/megs.sh</xsl:variable>
  <xsl:variable name="megs.reportprogress">bin/megs-reportprogress.sh</xsl:variable>

  <xsl:variable name="newline"><xsl:text>
</xsl:text></xsl:variable>

  <!-- variables computed from parameters -->

  <xsl:variable name="calvalus.input.filename" select="tokenize($calvalus.input,'/')[last()]" />


  <xsl:variable name="calvalus.input.year"  select="substring($calvalus.input.filename,15,4)" />
  <xsl:variable name="calvalus.input.month" select="substring($calvalus.input.filename,19,2)" />
  <xsl:variable name="calvalus.input.day"   select="substring($calvalus.input.filename,21,2)" />


  <xsl:variable name="calvalus.output" select="/wps:Execute/wps:DataInputs/wps:Input[ows:Identifier='calvalus.output.dir']/wps:Data/wps:Reference/@xlink:href" />

  <xsl:variable name="calvalus.input.physical">
    <xsl:choose>
      <xsl:when test="starts-with($calvalus.input,'hdfs:')">
        <xsl:value-of select="$calvalus.archive.mount" />
        <xsl:text>/</xsl:text>
        <xsl:value-of select="substring-after(substring-after($calvalus.input,'hdfs://'),'/')" />
      </xsl:when>
      <xsl:when test="starts-with($calvalus.input,'file://')">
        <xsl:value-of select="substring-after($calvalus.input,'file://')" />
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="$calvalus.input" />
      </xsl:otherwise>
    </xsl:choose>
  </xsl:variable>

  <!-- sets variable calvalus.output.physical to final output dir -->

  <xsl:variable name="calvalus.output.physical">
    <xsl:variable name="output" select="/wps:Execute/wps:DataInputs/wps:Input[ows:Identifier='calvalus.output.dir']/wps:Data/wps:Reference/@xlink:href" />
    <xsl:choose>
      <xsl:when test="starts-with($output,'hdfs:')">
        <xsl:value-of select="$calvalus.archive.mount" />
        <xsl:text>/</xsl:text>
        <xsl:value-of select="substring-after(substring-after($output,'hdfs://'),'/')" />
      </xsl:when>
      <xsl:when test="starts-with($output,'file://')">
        <xsl:value-of select="substring-after($output,'file://')" />
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="$output" />
      </xsl:otherwise>
    </xsl:choose>
  </xsl:variable>

  <!-- constructs call with env script, executable, input, output, move of temporal output to target dir -->
  <!-- NAME_DEM_DIR="<xsl:value-of select="$calvalus.archive.mount" />/calvalus/auxiliary/amorgos-3.0/GETASSE30/" -->

  <xsl:template match="/">
      <!-- write parameter file // -->

<!--          NAME_INPUT_DIR="<xsl:value-of select="$calvalus.tmp.dir" />"
          NAME_AUX_DIR="<xsl:value-of select="$calvalus.package.dir" />/AuxDir/"
          NAME_DEM_DIR="/home/hadoop/opt/GETASSE30/"
          NAME_OUTPUT_DIR="<xsl:value-of select="$calvalus.tmp.dir" />"
-->

      <xsl:result-document href="{$calvalus.tmp.dir}/modifiers.db">bshow comment show programming messages
bshow format integer(3)
bshow module level1 level2
bshow range [0,1]
bshow value 0 0 0
centre_lat comment FR product limits : latitude of center for requested FR scene (FR only)
centre_lat format  real*8
centre_lat module  level1
centre_lat range   [-90,90]
centre_lat unit    degrees
centre_lat value    52.6500
centre_lon comment FR product limits : longitude of center for requested FR scene (FR only)
centre_lon format  real*8
centre_lon module  level1
centre_lon range   [-180,180]
centre_lon unit    degrees
centre_lon value     4.7500
conso_switch comment Switch enabling Consolidated Processing (1=Consolidated)
conso_switch format integer
conso_switch module level1
conso_switch range [0,3]
conso_switch value 1
dbugstr comment Trace flags for Straylight Correction routines
dbugstr format integer(10)
dbugstr module level1
dbugstr range [0,1]
dbugstr value 0 0 0 0 0 0 0 0 0 0
first_col_bloc comment Column coordinate of first bloc to process
first_col_bloc format integer
first_col_bloc module level2
first_col_bloc range [1,1000]
first_col_bloc value 1
first_lin_bloc comment Line coordinate of first bloc to process
first_lin_bloc format integer
first_lin_bloc module level2
first_lin_bloc range [1,1000]
first_lin_bloc value 1
first_time comment RR product limits : time of first frame to extract and process (RR only)
first_time format  real*8
first_time module  level1
first_time range   [-18262,36525]
first_time unit    mjd2000
first_time value 1259.41997918419
force_class comment Switch to force level 2 classification (0:bright 1:cloud 2:land 3:case2(S) 4:DDV)
force_class format integer(10)
force_class module level2
force_class range [-1,1]
force_class unit dl
force_class value -1 -1 -1 -1 -1 -1 -1 -1 -1 -1
frame_limits comment indices of first and last frames to process from Level 0 product (if both >0 overrides normal product limits parameters for both resolutions, else ignored)
frame_limits format  integer(2)
frame_limits module  level1
frame_limits range   [0,30000]
frame_limits value   1 20000
kmax comment Number of columns (per module) for Straylight Correction trace outputs
kmax format integer
kmax module level1
kmax range [0,740]
kmax value 8
last_time comment RR product limits : time of last frame to extract and process (RR only)
last_time format  real*8
last_time module  level1
last_time range   [-18262,36525]
last_time unit    mjd2000
last_time value 1259.42023990178
mode comment Simulation Mode (resolution is full if odd, reduced if even) (automatic detection of FR or RR should be inserted here)
mode format  integer
mode module  level1 level2
mode range   [0,5]
mode value 11
n_col_bloc comment Number of Columns of Blocs to process
n_col_bloc format integer
n_col_bloc module level2
n_col_bloc range [1,1000]
n_col_bloc value 1
n_lin_bloc comment Number of Lines of Blocs to process
n_lin_bloc format integer
n_lin_bloc module level2
n_lin_bloc range [1,1000]
n_lin_bloc value 1
o3unit comment Switch for input ozone data unit (0:DU, 1:kg/m2)
o3unit format integer
o3unit module level1
o3unit range [0,1]
o3unit value 1
oknonlin comment Switch enabling non linearity correction (1=enabled,0=off,-1=keep database value)
oknonlin format integer
oknonlin module level1
oknonlin range [-1,1]
oknonlin value -1
okpixclas comment Switch enabling level1B pixel classification (1=enabled,0=off)
okpixclas format integer
okpixclas module level1
okpixclas range [0,1]
okpixclas value 1
okresamp comment Switch enabling resampling (1=enabled,0=off,-1=keep database value)
okresamp format integer
okresamp module level1
okresamp range [-1,1]
okresamp value -1
okstray comment Switch enabling stray light correction (1=enabled,0=off,-1=keep database value)
okstray format integer
okstray module level1
okstray range [-1,1]
okstray value  -1
ozone comment Default value for total ozone content in Level 2 processing (used only if user_aux_dat[3]=1)
ozone format real*8
ozone module level2
ozone range [0,1000]
ozone unit  Dobson Units (DU)
ozone value 350.
pressure comment Default value for surface atmospheric pressure in Level 2 processing (used only if user_aux_dat[2]=1)
pressure format real*8
pressure module level2
pressure range [100,2000]
pressure unit  hPa
pressure value 1013.25
step12_int_file comment Switch activating Level 2 Step 1 to Step 2 intermediate file creation
step12_int_file format integer
step12_int_file module level2
step12_int_file range [0,1]
step12_int_file value 0 
step1_aux_file comment Switch activating Level 2 Step 1 aux. file creation
step1_aux_file format integer
step1_aux_file module level2
step1_aux_file range [0,1]
step1_aux_file value 0 
step23_int_file comment Switch activating Level 2 Step 2 to Step 3 intermediate file creation
step23_int_file format integer
step23_int_file module level2
step23_int_file range [0,1]
step23_int_file value 0
step2_aux_file comment Switch activating Level 2 Step 2 aux. file creation
step2_aux_file format integer
step2_aux_file module level2
step2_aux_file range [0,1]
step2_aux_file value 0
step3_aux_file comment Switch activating Level 2 Step 3 aux. file creation
step3_aux_file format integer
step3_aux_file module level2
step3_aux_file range [0,1]
step3_aux_file value 0 
switch_zone comment Switch activating L2 processing on specified zone only (1=enabled,0=disabled)
switch_zone format integer
switch_zone module level2
switch_zone range [0,1]
switch_zone value 0
teststr comment Test flags for Straylight Correction routines
teststr format integer(10)
teststr module level1
teststr range [0,1]
teststr value 0 0 0 0 0 0 0 0 0 0
tie_limits comment indices of first and last tie points to includein level1 product (if both >0 overrides AC product limits parameters for FR, else ignored)
tie_limits format integer(2)
tie_limits module level1
tie_limits range [0,71]
tie_limits value 0 0
user_aux_dat comment Switches forcing Level 2 environment data to default values (wind_u, wind_v, pressure and ozone in that order ; 0:use ECMWF files, 1:force to default)
user_aux_dat format integer(4)
user_aux_dat module level2
user_aux_dat range [0,1]
user_aux_dat value 0 0 0 0
wind_u comment Default value for zonal wind in Level 2 processing (used only if user_aux_dat[0]=1)
wind_u format real*8
wind_u module level2
wind_u range [-100.,100.]
wind_u unit  m/s
wind_u value 3
wind_v comment Default value for meridional wind in Level 2 processing (used only if user_aux_dat[1]=1)
wind_v format real*8
wind_v module level2
wind_v range [-100.,100.]
wind_v unit  m/s
wind_v value 4
netcdf comment Output a product in NetCDF format (0=disabled, 1=netcdf3, 2=netcdf4)
netcdf format integer
netcdf range [0,2]
netcdf value 2
netcdf_comp comment Use internal (gzip) compression in NetCDF products (0=no compression, 1-9=gzip compression level)
netcdf_comp format integer
netcdf_comp range [0,9]
netcdf_comp value 0
std_n1 comment Output a standard Envisat product in N1 format (0=disabled, 1=enabled)
std_n1 format integer
std_n1 range [0,1]
std_n1 value 1
mermaid_output_window_size comment MERMAID CSV output window size (1=1x1, 3=3x3, 5=5x5, 7=7x7, 9=9x9, 11=11x11, 13=13x13, 15=15x15)
mermaid_output_window_size format integer
mermaid_output_window_size range [1,15]
mermaid_output_window_size value 5 
processing_version comment Processing version, defined by a MEGS version and ADF configuration version (e.g. MEGS_8.22_18)
processing_version format character*33
processing_version value MEGS_8.0_Reference_Configuration
breakpoints_list comment Comma separated list of ODESA selected breakpoints (warning: spaces forbidden in the list!!!)
breakpoints_list format character*6721
breakpoints_list value LAT,LON,RN_01,RN_02,RN_03,RN_04,RN_05,RN_06,RN_07,RN_08,RN_09,RN_10,RN_12,RN_13,RN_14,VAPR,CHL1,TOAVI,CTP,ODOC,RRIR,SPM,RRNIR,CHL2,BOAVI,PRESS,PAR,CALB,ALPHA,CTYPE,AOPT,COPT,L2FLAGS,RHO_WN_01,RHO_WN_02,RHO_WN_03,RHO_WN_04,RHO_WN_05,RHO_WN_06,RHO_WN_07,RHO_WN_08,RHO_WN_09,RHO_WN_10,RHO_WN_12,RHO_WN_13,RHO_WN_14,NRRS_01,NRRS_02,NRRS_03,NRRS_04,NRRS_05,NRRS_06,NRRS_07,NRRS_08,NRRS_09,NRRS_10,NRRS_12,NRRS_13,NRRS_14,GSM_CHL1,GSM_CDM,GSM_BBP,GSM_CHL1_STD,GSM_CDM_STD,GSM_BBP_STD,GSM_CHI2,GSM_NB_ITER,GSM_COV_CHL1_CDM,GSM_COV_CHL1_BBP,GSM_COV_CDM_BBP,GSM_NRRS_01,GSM_NRRS_02,GSM_NRRS_03,GSM_NRRS_04,GSM_NRRS_05,GSM_NRRS_06,GSM_NRRS_07,GSM_NRRS_08,GSM_NRRS_09,GSM_NRRS_10,GSM_NRRS_11,GSM_NRRS_12,GSM_NRRS_13,GSM_NRRS_14,GSM_NRRS_15,GSM_TOA_CHL1,GSM_TOA_CDM,GSM_TOA_BBP,GSM_TOA_T865,GSM_TOA_CHL1_STD,GSM_TOA_CDM_STD,GSM_TOA_BBP_STD,GSM_TOA_T865_STD,GSM_TOA_CHI2,GSM_TOA_NB_ITER,GSM_TOA_COV_CHL1_CDM,GSM_TOA_COV_CHL1_BBP,GSM_TOA_COV_CDM_BBP,GSM_TOA_RHO_STAR_NG_01,GSM_TOA_RHO_STAR_NG_02,GSM_TOA_RHO_STAR_NG_03,GSM_TOA_RHO_STAR_NG_04,GSM_TOA_RHO_STAR_NG_05,GSM_TOA_RHO_STAR_NG_06,GSM_TOA_RHO_STAR_NG_07,GSM_TOA_RHO_STAR_NG_08,GSM_TOA_RHO_STAR_NG_09,GSM_TOA_RHO_STAR_NG_10,GSM_TOA_RHO_STAR_NG_11,GSM_TOA_RHO_STAR_NG_12,GSM_TOA_RHO_STAR_NG_13,GSM_TOA_RHO_STAR_NG_14,GSM_TOA_RHO_STAR_NG_15,GSM_TOA_GLINT_01,GSM_TOA_GLINT_02,GSM_TOA_GLINT_03,GSM_TOA_GLINT_04,GSM_TOA_GLINT_05,GSM_TOA_GLINT_06,GSM_TOA_GLINT_07,GSM_TOA_GLINT_08,GSM_TOA_GLINT_09,GSM_TOA_GLINT_10,GSM_TOA_GLINT_11,GSM_TOA_GLINT_12,GSM_TOA_GLINT_13,GSM_TOA_GLINT_14,GSM_TOA_GLINT_15,GSM_TOA_GLINT_STD_01,GSM_TOA_GLINT_STD_02,GSM_TOA_GLINT_STD_03,GSM_TOA_GLINT_STD_04,GSM_TOA_GLINT_STD_05,GSM_TOA_GLINT_STD_06,GSM_TOA_GLINT_STD_07,GSM_TOA_GLINT_STD_08,GSM_TOA_GLINT_STD_09,GSM_TOA_GLINT_STD_10,GSM_TOA_GLINT_STD_11,GSM_TOA_GLINT_STD_12,GSM_TOA_GLINT_STD_13,GSM_TOA_GLINT_STD_14,GSM_TOA_GLINT_STD_15,NNBOA_CHL2,NNBOA_SPM,NNBOA_ODOC,COSMETIC,DUPLICATED,GLINT_RISK,SUSPECT,LAND_OCEAN,BRIGHT,COASTLINE,INVALID,LAND,CLOUD,WATER,PCD_1_13,PCD_14,PCD_15,PCD_16,PCD_17,PCD_18,PCD_19,OADB,ABSOA_DUST,CASE2_S,SNOW_ICE,CASE2_ANOM,TOAVI_BRIGHT,CASE2_Y,TOAVI_BAD,ICE_HAZE,TOAVI_CSI,MEDIUM_GLINT,TOAVI_WS,BPAC_ON,DDV,HIGH_GLINT,TOAVI_INVAL_REC,LOW_SUN,WHITE_SCATTERER,L2FLAGS1,L2FLAGS2,HIINLD,ICE_HIGHAERO,ISLAND,LOINLD,ORINP1,ORINP2,UNCGLINT,WHITECAPS,ACFAIL,ORINP0,OROUT0,HIGH_MDSI,PCD_NN,BRIGHT_RC,BRIGHT_TOA,LOW_P,SLOPE_1,SLOPE_2,UNCERTAIN,WVHIGLINT,TOAVI_VEG,CLOSE2COAST,ANNOT_F,SHALLOW,ORLUT,ANNOT_ALPHA,AERO_B,ABSO_D,ACLIM,ABSOA,MIXR1,TAU06,DROUT,RWNEG_01,RWNEG_02,RWNEG_03,RWNEG_04,RWNEG_05,RWNEG_06,RWNEG_07,RWNEG_08,RWNEG_09,RWNEG_10,RWNEG_11,RWNEG_12,RWNEG_13,RWNEG_14,RWNEG_15,SATURATED_F,SATURATED_01,SATURATED_02,SATURATED_03,SATURATED_04,SATURATED_05,SATURATED_06,SATURATED_07,SATURATED_08,SATURATED_09,SATURATED_10,SATURATED_11,SATURATED_12,SATURATED_13,SATURATED_14,SATURATED_15,DETECTOR,PRESS_APP,SURF_ALBEDO,RHO_AER_01,RHO_AER_02,RHO_AER_03,RHO_AER_04,RHO_AER_05,RHO_AER_06,RHO_AER_07,RHO_AER_08,RHO_AER_09,RHO_AER_10,RHO_AER_12,RHO_AER_13,RHO_AER_14,TAU_AER_01,TAU_AER_02,TAU_AER_03,TAU_AER_04,TAU_AER_05,TAU_AER_06,TAU_AER_07,TAU_AER_08,TAU_AER_09,TAU_AER_10,TAU_AER_11,TAU_AER_12,TAU_AER_13,TAU_AER_14,TAU_AER_15,RHO_RAY0_01,RHO_RAY0_02,RHO_RAY0_03,RHO_RAY0_04,RHO_RAY0_05,RHO_RAY0_06,RHO_RAY0_07,RHO_RAY0_08,RHO_RAY0_09,RHO_RAY0_10,RHO_RAY0_12,RHO_RAY0_13,RHO_RAY0_14,RHO_RAY1_01,RHO_RAY1_02,RHO_RAY1_03,RHO_RAY1_04,RHO_RAY1_05,RHO_RAY1_06,RHO_RAY1_07,RHO_RAY1_08,RHO_RAY1_09,RHO_RAY1_10,RHO_RAY1_11,RHO_RAY1_12,RHO_RAY1_13,RHO_RAY1_14,RHO_RAY1_15,TOAR_01,TOAR_02,TOAR_03,TOAR_04,TOAR_05,TOAR_06,TOAR_07,TOAR_08,TOAR_09,TOAR_10,TOAR_11,TOAR_12,TOAR_13,TOAR_14,TOAR_15,RHO_TOA_01,RHO_TOA_02,RHO_TOA_03,RHO_TOA_04,RHO_TOA_05,RHO_TOA_06,RHO_TOA_07,RHO_TOA_08,RHO_TOA_09,RHO_TOA_10,RHO_TOA_11,RHO_TOA_12,RHO_TOA_13,RHO_TOA_14,RHO_TOA_15,RHO_01,RHO_02,RHO_03,RHO_04,RHO_05,RHO_06,RHO_07,RHO_08,RHO_09,RHO_10,RHO_11,RHO_12,RHO_13,RHO_14,RHO_15,RHO_NG_01,RHO_NG_02,RHO_NG_03,RHO_NG_04,RHO_NG_05,RHO_NG_06,RHO_NG_07,RHO_NG_08,RHO_NG_09,RHO_NG_10,RHO_NG_11,RHO_NG_12,RHO_NG_13,RHO_NG_14,RHO_NG_15,RHO_STAR_NG_01,RHO_STAR_NG_02,RHO_STAR_NG_03,RHO_STAR_NG_04,RHO_STAR_NG_05,RHO_STAR_NG_06,RHO_STAR_NG_07,RHO_STAR_NG_08,RHO_STAR_NG_09,RHO_STAR_NG_10,RHO_STAR_NG_11,RHO_STAR_NG_12,RHO_STAR_NG_13,RHO_STAR_NG_14,RHO_STAR_NG_15,IAER_SA,RHO_TOP_01,RHO_TOP_02,RHO_TOP_03,RHO_TOP_04,RHO_TOP_05,RHO_TOP_06,RHO_TOP_07,RHO_TOP_08,RHO_TOP_09,RHO_TOP_10,RHO_TOP_11,RHO_TOP_12,RHO_TOP_13,RHO_TOP_14,RHO_TOP_15,DDV_MODEL,C_CORR_1,C_CORR_2,C_CORR_3,ROB_AG_1,ROB_AG_2,ROB_AG_3,ROB_RA_1,ROB_RA_2,ROB_RA_3,RHO_GROUND_1,RHO_GROUND_2,RHO_GROUND_3,MDSI,ARVI,ARVI_THRESHOLD,AER_MIX,AER_1,AER_2,SPM_BR,T_RHO_W_C2_1,T_RHO_W_C2_2,T_RHO_W_C2_3,T_RHO_W_C2_4,BBP_775_BR_1,BBP_775_BR_2,RHO_W_775_BR_1,RHO_W_775_BR_2,ANG_EXP_1,ANG_EXP_2,ANNOT_BPAC_F,DO_BANDSET_LOW,DO_BANDSET_HIGH,CONVERGE_LOW,CONVERGE_HIGH,ERROR_LOW,ERROR_HIGH,RHO_G,OMEGA_AER_01,OMEGA_AER_02,OMEGA_AER_03,OMEGA_AER_04,OMEGA_AER_05,OMEGA_AER_06,OMEGA_AER_07,OMEGA_AER_08,OMEGA_AER_09,OMEGA_AER_10,OMEGA_AER_11,OMEGA_AER_12,OMEGA_AER_13,OMEGA_AER_14,OMEGA_AER_15,FA_01,FA_02,FA_03,FA_04,FA_05,FA_06,FA_07,FA_08,FA_09,FA_10,FA_11,FA_12,FA_13,FA_14,FA_15,T_DOWN_01,T_DOWN_02,T_DOWN_03,T_DOWN_04,T_DOWN_05,T_DOWN_06,T_DOWN_07,T_DOWN_08,T_DOWN_09,T_DOWN_10,T_DOWN_12,T_DOWN_13,T_DOWN_14,T_UP_01,T_UP_02,T_UP_03,T_UP_04,T_UP_05,T_UP_06,T_UP_07,T_UP_08,T_UP_09,T_UP_10,T_UP_12,T_UP_13,T_UP_14,RHO_RAY_01,RHO_RAY_02,RHO_RAY_03,RHO_RAY_04,RHO_RAY_05,RHO_RAY_06,RHO_RAY_07,RHO_RAY_08,RHO_RAY_09,RHO_RAY_10,RHO_RAY_11,RHO_RAY_12,RHO_RAY_13,RHO_RAY_14,RHO_RAY_15,RHO_GC_01,RHO_GC_02,RHO_GC_03,RHO_GC_04,RHO_GC_05,RHO_GC_06,RHO_GC_07,RHO_GC_08,RHO_GC_09,RHO_GC_10,RHO_GC_12,RHO_GC_13,RHO_GC_14,TOAROG_01,TOAROG_02,TOAROG_03,TOAROG_04,TOAROG_05,TOAROG_06,TOAROG_07,TOAROG_08,TOAROG_09,TOAROG_10,TOAROG_11,TOAROG_12,TOAROG_13,TOAROG_14,TOAROG_15,RHO_SURF_01,RHO_SURF_02,RHO_SURF_03,RHO_SURF_04,RHO_SURF_05,RHO_SURF_06,RHO_SURF_07,RHO_SURF_08,RHO_SURF_09,RHO_SURF_10,RHO_SURF_11,RHO_SURF_12,RHO_SURF_13,RHO_SURF_14,RHO_SURF_15,C2R_tau443,C2R_tau550,C2R_tau778,C2R_tau865,C2R_angstrom,C2R_glintrat,C2R_chi_sum_atm,C2R_chi_sum_wat,C2R_RLpath_01,C2R_RLpath_02,C2R_RLpath_03,C2R_RLpath_04,C2R_RLpath_05,C2R_RLpath_06,C2R_RLpath_07,C2R_RLpath_08,C2R_RLpath_09,C2R_RLw_01,C2R_RLw_02,C2R_RLw_03,C2R_RLw_04,C2R_RLw_05,C2R_RLw_06,C2R_RLw_07,C2R_RLw_08,C2R_RLw_09,C2R_RLw_a_01,C2R_RLw_a_02,C2R_RLw_a_03,C2R_RLw_a_04,C2R_RLw_a_05,C2R_RLw_a_06,C2R_RLw_a_07,C2R_RLw_a_08,C2R_RLw_a_09,C2R_RL_tosa_01,C2R_RL_tosa_02,C2R_RL_tosa_03,C2R_RL_tosa_04,C2R_RL_tosa_05,C2R_RL_tosa_06,C2R_RL_tosa_07,C2R_RL_tosa_08,C2R_RL_tosa_09,C2R_trans_ed_01,C2R_trans_ed_02,C2R_trans_ed_03,C2R_trans_ed_04,C2R_trans_ed_05,C2R_trans_ed_06,C2R_trans_ed_07,C2R_trans_ed_08,C2R_trans_ed_09
do_land_branch comment Do all the land branch processing (0=disabled, 1=enabled)
do_land_branch format integer
do_land_branch range [0,1]
do_land_branch value 1
do_water_branch comment Do all the water branch processing (0=disabled, 1=enabled)
do_water_branch format integer
do_water_branch range [0,1]
do_water_branch value 1
do_cloud_branch comment Do all the cloud branch processing (0=disabled, 1=enabled)
do_cloud_branch format integer
do_cloud_branch range [0,1]
do_cloud_branch value 1
</xsl:result-document>

# create the netcdf.db (so far has zero length, maybe some config option could go there - no documentation available at the moment)

<xsl:result-document href="{$calvalus.tmp.dir}/netcdf.db">
</xsl:result-document>

# specific commands for running the job...
# switch on job control
set -m

# create tmp dir and change into it
mkdir -p <xsl:value-of select="$calvalus.tmp.dir" />
cd <xsl:value-of select="$calvalus.tmp.dir" />

# list parameter file (for logging purposes)
# cat modifiers.db 

# debug output
echo "MEGS: Starting script"

# make the required directories

mkdir -p files

# link in the database files

for file in $(ls <xsl:value-of select="$calvalus.package.dir" />/aux_data/database/)
do
  ln -s <xsl:value-of select="$calvalus.package.dir" />/aux_data/database/$file files/$file
done

# link in the ADF files

export PRD_DIR=<xsl:value-of select="$calvalus.package.dir" />/aux_data/megs/

ln -s "$PRD_DIR/aeroclim/20/aeroclim.20.08.prd" files/aeroclim.prd
ln -s "$PRD_DIR/atmosphere/34/atmosphere.34.03.01.prd" files/atmosphere.prd
ln -s "$PRD_DIR/case1/63/case1.63.02.01.prd" files/case1.prd
ln -s "$PRD_DIR/case2/46/case2.46.02.02.prd" files/case2.prd
ln -s "$PRD_DIR/conf_map/20/conf_map.20.00.prd" files/conf_map.prd
ln -s "$PRD_DIR/cloud/41/cloud.41.02.prd" files/cloud.prd
ln -s "$PRD_DIR/landaero/44/landaero.44.03.01.prd" files/landaero.prd
ln -s "$PRD_DIR/oceanaero/42/oceanaero.42.04.03.prd" files/oceanaer.prd
ln -s "$PRD_DIR/lv2conf/43/lv2conf.43.01.03.prd" files/lv2conf.prd
ln -s "$PRD_DIR/vapour/22/vapour.22.00.prd" files/vapour.prd
ln -s "$PRD_DIR/vegetation/33/vegetation.33.01.prd" files/vegetation.prd


# link the Resource files

for f in fLOV.dat globcolour_config.txt ang_net.net atm_net_back.net atm_net_for.net wat_net_back.net wat_net_for.net
   do
    ln -s <xsl:value-of select="$calvalus.package.dir" />/aux_data/resources/$f files/$f
   done


# start concurrent status reporting job
<xsl:value-of select="$calvalus.package.dir" />/<xsl:value-of select="$megs.reportprogress" />  &amp;


# link / copy the  input file - (just copy for the moment)
hadoop fs -get <xsl:value-of select="$calvalus.input" /><xsl:text> </xsl:text><xsl:value-of select="$calvalus.tmp.dir" />/files/l1_frs.prd


# call MEGS executable wrapper for automatic cleanup on failure.
<xsl:value-of select="$calvalus.package.dir" />/<xsl:value-of select="$megs.executable" />


# stop concurrent status reporting job
kill %1


# rename result file
outputfile=`head -1 files/l2_frs.prd | cut -d'=' -f2 | sed s/\"//g`


# netcdf file as well
outnetcdf=`head -1 files/l2_frs.prd | cut -d'=' -f2 | sed s/\"//g | sed 's/\(.*\).../\1/'`.nc
mv files/output.nc $outnetcdf


# rename configuration files

configfile=modifiers_`head -1 files/l2_frs.prd | cut -d'=' -f2 | sed s/\"//g | sed 's/\(.*\).../\1/'`.db

netcdfconfigfile=netcdf_`head -1 files/l2_frs.prd | cut -d'=' -f2 | sed s/\"//g | sed 's/\(.*\).../\1/'`.db

mv files/l2_frs.prd $outputfile

# move tmp output to (hdfs) destination
if test -e <xsl:value-of select="$calvalus.tmp.dir" />/*.N1 ; then
  hadoop fs -put $outputfile <xsl:value-of select="$calvalus.output" />/<xsl:value-of
          select="$calvalus.input.year" />/<xsl:value-of
          select="$calvalus.input.month" />/<xsl:value-of
          select="$calvalus.input.day" />/$outputfile
  rm -f $outputfile
fi

if test -e <xsl:value-of select="$calvalus.tmp.dir" />/*.nc ; then
  hadoop fs -put $outnetcdf <xsl:value-of select="$calvalus.output" />/<xsl:value-of
          select="$calvalus.input.year" />/<xsl:value-of
          select="$calvalus.input.month" />/<xsl:value-of
          select="$calvalus.input.day" />/$outnetcdf
  rm -f $outnetcdf
fi


# copy the configuration files to hdfs as well.

if test -e <xsl:value-of select="$calvalus.tmp.dir" />/modifiers.db ; then
  hadoop fs -put modifiers.db <xsl:value-of select="$calvalus.output" />/<xsl:value-of
          select="$calvalus.input.year" />/<xsl:value-of
          select="$calvalus.input.month" />/<xsl:value-of
          select="$calvalus.input.day" />/$configfile
fi

if test -e <xsl:value-of select="$calvalus.tmp.dir" />/netcdf.db ; then
  hadoop fs -put netcdf.db <xsl:value-of select="$calvalus.output" />/<xsl:value-of
          select="$calvalus.input.year" />/<xsl:value-of
          select="$calvalus.input.month" />/<xsl:value-of
          select="$calvalus.input.day" />/$netcdfconfigfile
fi

ls -ltr ; cat errors.txt ; cd

# cleanup - remove the temporary directory
#rm -r <xsl:value-of select="$calvalus.tmp.dir" />


#
date
test -f <xsl:value-of select="$calvalus.output.physical" />/<xsl:value-of
          select="$calvalus.input.year" />/<xsl:value-of
          select="$calvalus.input.month" />/<xsl:value-of
          select="$calvalus.input.day" />/$outputfile
  </xsl:template>

  <!-- catches all other stuff -->

  <xsl:template match="@*|node()" >
    <xsl:apply-templates select="@*|node()"/>
  </xsl:template>

</xsl:stylesheet>
