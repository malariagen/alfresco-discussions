USERNAME=admin
PASSWORD=admin
SITE_NAME=site1
TARGET=http://localhost:8080/alfresco
SETTINGS_FILE=${SITE_NAME}.site.json
curl -u${USERNAME}:${PASSWORD} -X GET "${TARGET}/service/api/sites/${SITE_NAME}" -H "content-type: application/json" > ${SETTINGS_FILE}
ENABLED=`grep -A 2 '"name": "{http:\\\\/\\\\/www.alfresco.org\\\\/model\\\\/sitecustomproperty\\\\/1.0}discussionsNotification"' ${SETTINGS_FILE} | grep -v '"name"' | grep -v '"value"' | awk -F\" '{print $2}'`
echo ${ENABLED}
if [ ${ENABLED} == "false" ]
then
    ENABLED=true
    MOD_FILE=${SITE_NAME}.site.enable.json
    egrep '(^{|visibili|shortName|title|description|^})' ${SITE_NAME}.site.json | grep -v null | sed -e "s/\"visibility/\"discussionsNotification\": \"${ENABLED}\",\n&/" > ${MOD_FILE}
    RESULT_FILE=${SITE_NAME}.site.result.json
    curl -u${USERNAME}:${PASSWORD} -X PUT "${TARGET}/service/api/sites/${SITE_NAME}" -H "content-type: application/json" -d@${MOD_FILE} > ${RESULT_FILE}
    MODIFIED_FILE=${SITE_NAME}.site.modified.json
    curl -u${USERNAME}:${PASSWORD} -X GET "${TARGET}/service/api/sites/${SITE_NAME}" -H "content-type: application/json" > ${MODIFIED_FILE}
fi

