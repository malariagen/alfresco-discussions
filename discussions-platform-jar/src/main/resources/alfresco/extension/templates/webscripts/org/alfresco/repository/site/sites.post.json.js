function main()
{
   // Ensure the user has Create Site capability
   if (!siteService.hasCreateSitePermissions())
   {
      status.setCode(status.STATUS_FORBIDDEN, "error.noPermissions");
      return;
   }
   
   // Get the details of the site
   if (json.has("shortName") == false || json.get("shortName").length == 0)
   {
      status.setCode(status.STATUS_BAD_REQUEST, "Short name missing when creating site");
      return;
   }
   var shortName = json.get("shortName");
   
   // See if the shortName is available
   if (siteService.hasSite(shortName))
   {
      status.setCode(status.STATUS_BAD_REQUEST, "error.duplicateShortName");
      return;
   }
   
   if (json.has("sitePreset") == false || json.get("sitePreset").length == 0)
   {
      status.setCode(status.STATUS_BAD_REQUEST, "Site preset missing when creating site");
      return;
   }
   var sitePreset = json.get("sitePreset");
   
   var title = null;
   if (json.has("title"))
   {
      title = json.get("title");
   }
      
   var description = null;
   if (json.has("description"))
   {
      description = json.get("description");
   }
   
   var sitetype = null;
   if (json.has("type") == true)
   {
      sitetype = json.get("type");
   }
   
   // Use the visibility flag before the isPublic flag
   var visibility = siteService.PUBLIC_SITE;
   if (json.has("visibility"))
   {
      visibility = json.get("visibility");
   }
   else if (json.has("isPublic"))
   {
      var isPublic = json.getBoolean("isPublic");
      if (isPublic == true)
      {
         visibility = siteService.PUBLIC_SITE;
      }
      else
      {
         visibility = siteService.PRIVATE_SITE;
      }
   }
   
   // Create the site 
   var site = null;
   if (sitetype == null)
   {
      site = siteService.createSite(sitePreset, shortName, title, description, visibility);
   }
   else
   {
      site = siteService.createSite(sitePreset, shortName, title, description, visibility, sitetype);
   }
   
   // Custom properties
   var siteNode = site.node;
   
   siteNode.properties["stcp:discussionsNotification"] = json.has("discussionsNotification");
   siteNode.save();
	
   if (json.has("discussionsNotification") == true) {

       var scriptRef = null;
	   var scriptNodes = search.luceneSearch("PATH:\"/app:company_home/app:dictionary/app:scripts/cm:Discussion_x0020_Scripts/cm:share_discussion_notification.js\"");

	   if (scriptNodes.length > 0) {
		   scriptRef = scriptNodes[0];
	   }
       var discussionsFolder = siteNode.createNode("discussions", "cm:folder");
       if (scriptRef != null) {
           var modifyRule = actions.create("modify-rule");
           modifyRule.parameters.action_name = "discussionsNotification";
           modifyRule.parameters.script_ref = scriptRef;
           modifyRule.parameters.enable = json.has("discussionsNotification");

           modifyRule.execute(discussionsFolder);
       }
   } 

   // Put the created site into the model
   model.site = site;
}

main();

