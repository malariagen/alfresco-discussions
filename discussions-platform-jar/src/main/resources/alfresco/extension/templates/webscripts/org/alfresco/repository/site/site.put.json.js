function main()
{
	// Get the site
	var shortName = url.extension;
	var site = siteService.getSite(shortName);
	
	if (site != null)
	{	
		// Updafte the sites details
		if (json.has("title") == true)
		{
		   site.title = json.get("title");
		}
		if (json.has("description") == true)
		{
		   site.description = json.get("description");
		}
		
		// Use the visibility flag before the isPublic flag
		if (json.has("visibility") == true)
		{
		   site.visibility = json.get("visibility");
		}
		else if (json.has("isPublic") == true)
		{
		   // Deal with deprecated isPublic flag accordingly
		   var isPublic = json.getBoolean("isPublic");
		   if (isPublic == true)
		   {
		      site.visibility = siteService.PUBLIC_SITE;
		   }
		   else
		   {
		      site.visibility = siteService.PRIVATE_SITE;
		   }
	    }
  // Custom properties
        var siteNode = site.node;
        var scriptRef = null;
        
        var scriptNodes = search.luceneSearch("PATH:\"/app:company_home/app:dictionary/app:scripts/cm:Discussion_x0020_Scripts/cm:share_discussion_notification.js\"");
        
        if (scriptNodes.length > 0) {
        	scriptRef = scriptNodes[0];
        }
        if ((!"stcp:discussionsNotification" in siteNode.properties || siteNode.properties["stcp:discussionsNotification"] != json.has("discussionsNotification")) &&
        		scriptRef != null) {
    		var discussionsFolder = siteNode.childByNamePath("discussions");
    		
    		if (json.has("discussionsNotification") == true) {
        		if (discussionsFolder == null) {
        			discussionsFolder = siteNode.createNode("discussions", "cm:folder");
        		}
    		}
    		
    		if (discussionsFolder != null) {
    			var modifyRule = actions.create("modify-rule");
    			modifyRule.parameters.action_name = "discussionsNotification";
    			modifyRule.parameters.script_ref = scriptRef;
    			modifyRule.parameters.enable = json.has("discussionsNotification");

    			modifyRule.execute(discussionsFolder);
    		}
    		
            siteNode.properties["stcp:discussionsNotification"] = json.has("discussionsNotification");
            siteNode.save();
 
        }
		// Save the site
		site.save();
		
		// Pass the model to the template
		model.site = site;
	}
	else
	{
		// Return 404
		status.setCode(status.STATUS_NOT_FOUND, "Site " + shortName + " does not exist");
		return;
	}
}

main();
