
//If you want to run in the javascript console to test
var debug = false;

if (debug) {
	//Put your post path here
	var docs = search.luceneSearch("PATH:\"/app:company_home/st:sites/cm:test1/cm:discussions/post-1473412876665_9834/post-1473412876665_9834\"");
	var document = docs[0];
	logger.log(document);
}

//Not ===
if (document.getType() == "{http://www.alfresco.org/model/forum/1.0}post") {
  var site = document.parent.parent.parent.name;
  var siteGroup = "GROUP_site_" + site;
  var template = "Data Dictionary/Email Templates/Notify Email Templates/Discussion Templates/share_discussion_notification.ftl";
 
  if (document.mimetype.startsWith("text/plain")) {
    template = "Data Dictionary/Email Templates/Notify Email Templates/Discussion Templates/share_discussion_notification_plain_content.ftl";
  }

    // create mail action
    var mail = actions.create("custom-mail");
    mail.parameters.to_many = siteGroup;

    // unfortunately this doesn't work!
    currentUserName = ""; // workaround?
  
    //For normal mail action
    //Need to set mail.from.enabled=true in alfresco-global.properties
    //mail.parameters.from = document.properties["sys:node-dbid"] + "@alfresco.malariagen.net";

    //For custom mail action
	mail.parameters.reply_to_node = document.properties["sys:node-dbid"];
	mail.parameters.list_id = site;
	mail.parameters.site_activity = site;
	  
    mail.parameters.subject="New Post in Discussion: "+document.parent.childAssocs["cm:contains"][0].properties.title;

    mail.parameters.template = companyhome.childByNamePath(template);
 
    //execute action against a document
    mail.execute(document);

}