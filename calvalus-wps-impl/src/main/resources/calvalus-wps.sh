#!/bin/bash
# Usage: calvalus-wps.sh username password type <JobId|requestXml>
# type has to be one of

if [[ $1 == "h" || $1 == "" ]] ; then
  echo "Usage: calvalus-wps.sh username password type <JobId|requestXml>"
  echo "    type MUST be one of GetCapabilities DescribeProcess Execute GetStatus FetchResults"
  echo "        GetCapabilities: Provides the capabilities of the CODE-DE WPS interface"
  echo "        DescribeProcess: Prints all usable processors of the CODE-DE processing system"
  echo "        Execute: Submits the given processing request to the CODE-DE processing system"
  echo "        GetStatus: Submits the given processing request to the CODE-DE processing system"
  echo "    if type is Execute, you MUST provide requestXml as 4th parameter"
  echo "    if type is GetStatus, you MUST provide JobId as 4th parameter"
  echo "    if type is FetchResult, you MUST provide a URL you got from GetStatus after successful processing as 4th parameter"
  exit 0
fi

#set -x

urlencode() {
  local string="${1}"
  local strlen=${#string}
  local encoded=""
  local pos c o

  for (( pos=0 ; pos<strlen ; pos++ )); do
     c=${string:$pos:1}
     case "$c" in
        [-_.~a-zA-Z0-9] ) o="${c}" ;;
        * )               printf -v o '%%%02x' "'$c"
     esac
     encoded+="${o}"
  done
  echo "${encoded}"    # You can either set a return variable (FASTER)
  REPLY="${encoded}"   #+or echo the result (EASIER)... or both... :p
}

#IP Addresses or hostnames are fine here
CAS_HOSTNAME=tsedos.eoc.dlr.de

#Authentication details. This script only supports username/password login, but curl can handle certificate login if required

USERNAME=$1
PASSWORD=$2

SERVICE_TYPE=$3

#Temporary files used by curl to store cookies and http headers
COOKIE_JAR=.cookieJar

if [[ $(find "$COOKIE_JAR" -mtime +1 -print) ]]; then
    rm -f ${COOKIE_JAR}
fi

if [[ ${SERVICE_TYPE} == "GetCapabilities" ]] ; then
    QUERY_STRING_CLEAR="?Service=WPS&Request=GetCapabilities"
elif [[ ${SERVICE_TYPE} == "DescribeProcess" ]] ; then
    QUERY_STRING_CLEAR="?Service=WPS&Request=DescribeProcess&Version=1.0.0&Identifier=all"
elif [[ ${SERVICE_TYPE} == "Execute" ]] ; then
    QUERY_STRING_CLEAR=""
elif [[ ${SERVICE_TYPE} == "GetStatus" ]] ; then
    QUERY_STRING_CLEAR="?Service=WPS&Request=GetStatus&JobId=$4"
fi

QUERY_STRING=$( urlencode ${QUERY_STRING_CLEAR} )

#echo "QUERY_STRING_CLEAR: " $QUERY_STRING_CLEAR
#echo "QUERY_STRING: " $QUERY_STRING

SERVICE_NAME_CLEAR=https://processing.code-de-ref.eoc.dlr.de/wps${QUERY_STRING_CLEAR}
SERVICE_NAME=$( urlencode ${SERVICE_NAME_CLEAR} )

#echo "SERVICE_NAME_CLEAR: " $SERVICE_NAME_CLEAR
#echo "SERVICE_NAME: " $SERVICE_NAME

loginResult="$(curl "https://tsedos.eoc.dlr.de/cas-codede/login?service=${SERVICE_NAME}" -u ${USERNAME}:${PASSWORD} -v -c ${COOKIE_JAR} 2>&1)"

# kept for reference
#ST=$( echo $loginResult | sed 's:.*Location\: ::' | sed 's:authtest.*:authtest:' | sed 's:.*ticket=::')
#curl https://tsedos.eoc.dlr.de/cas-codede/p3/serviceValidate?"service=$SERVICE_NAME&ticket=${ST}" -c $COOKIE_JAR
#curl "https://tsedos.eoc.dlr.de/cas-codede/samlCreate2?service=$SERVICE_NAME" -b $COOKIE_JAR -c $COOKIE_JAR

# Send the cookie to the WPS, and use it there

#curl "https://processing.code-de-ref.eoc.dlr.de/wps?Service=WPS&Request=GetCapabilities" -b $COOKIE_JAR -k -L -H "Cookie: queryString=%3FService%3DWPS%26Request%3DGetCapabilities"
#curl $SERVICE_NAME_CLEAR -b $COOKIE_JAR -k -L -H "Cookie: queryString=$QUERY_STRING"
#curl $SERVICE_NAME_CLEAR -b $COOKIE_JAR -k -L -F "request=@test-request.xml"
if [[ ${SERVICE_TYPE} == "Execute" ]] ; then
    COOKIE=$(cat ${COOKIE_JAR} | sed "s:.*CASTGC::" | sed "s:#.*::" | sed "s:#.*::" | sed "s:#.*::" | tr -d '[:space:]')
    curl ${SERVICE_NAME_CLEAR} -b ${COOKIE_JAR} -k -L -H "Cookie: requestId=$(uuidgen);CASTGC=${COOKIE}" -F "request=@test-request.xml"
elif [[ ${SERVICE_TYPE} == "GetStatus" ]] ; then
    curl ${SERVICE_NAME_CLEAR} -b ${COOKIE_JAR} -k -s -L -H "Cookie: queryString=${QUERY_STRING}"
elif [[ ${SERVICE_TYPE} == "FetchResult" ]] ; then
    curl ${SERVICE_NAME_CLEAR} -b ${COOKIE_JAR} -k -L -O -s ${4}
else
    curl ${SERVICE_NAME_CLEAR} -b ${COOKIE_JAR} -k -L -H "Cookie: queryString=${QUERY_STRING}"
fi

