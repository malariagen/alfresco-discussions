# Discussions

The main purpose of this extension is to send an email to all site members whenever a discussion topic is created/updated.
This extension also allows you to reply to the notification via email (assuming inbound email is configured - see http://docs.alfresco.com/5.1/concepts/email-intro.html)

There is a cronjob that runs the script cron-site-email-contributors.js which adds all site users to the EMAIL_CONTRIBUTORS group.
 
You will want a working email server to test this. I suggest fakeSMTP as a good option for testing locally.

If you just want email notification you can set it up by hand using the files in /discussions-platform-jar/src/main/resources/alfresco/module/discussions-platform-jar/bootstrap to the appropriate places and manually creating rules on the discussion folder for which you wish to set up notifications.
N.B. If you do this you will need to edit the notifications script to use the "mail" action instead of the "custom-mail" action and edit the parameters according, as well as manually adding people to EMAIL_CONTRIBUTORS

This is a fairly basic implementation of the script, for a more advanced discussion see http://jared.ottleys.net/alfresco/alfresco-share-discussion-notification/

For an existing site you need to Edit Site Details and save it, then the option will appear the second time.

Also see the wiki if you have any problems

## Installation

discussions-email-version.jar and discussions-platform-jar-version.jar go into alfresco/WEB-INF/lib
discussions-share-jar-version.jar goes into share/WEB-INF/lib

Configuring inbound and outbound email is as normal.


# Inbound Mail 
 
The custom inbound email server has two extensions:
  * Checks the incoming email address against the company email field (from the user profile) as well as the contact email
  * Allows the use of the + separator to define the target e.g. default+<<sys:node-dbid>>@example.com

More importantly there is a fix to the associations when the reply node is created see: https://forums.alfresco.com/forum/installation-upgrades-configuration-integration/configuration/inbound-email-forums-05042016
(This can also be done with a script if you so desire)

## Testing
You can telnet to the test instance on the port configured in alfresco-global.properties and use the contents of test.msg
e.g. telnet localhost 8025 < test.msg
You will need to make sure that:
  * the node-dbid value in the Reply-To field matches a discussion topic in your repository.
  * the from header contains an email address that matches a user with the correct permissions, and is a member of EMAIL_CONTRIBUTORS

## Postfix Inbound Configuration

For reference to help setting up inbound mail.

For postfix you will probably want to set up a transport map
e.g. in /etc/postfix/main.cf
```
transport_maps = hash:/etc/postfix/transport
```
in /etc/postfix/transport
```
.mydomain.com smtp:localhost:8025
```
then
```
postmap /etc/postfix/transport
postfix reload
```

# Outbound Mail

The custom email action also has extensions:
  *	parameters.reply_to_node : sets the reply to header with the value of the parameter between + and @
  *	parameters.list_id : sets headers to indicate that the message is from a list (to reduce out of office responses)
  * parameters.site_activity: if this is set and the user has disabled activity feeds for the named site in their profile then 
                                  they will not receive the notification
  
The template url parameter has been extended to include ${url.shareServerPath} and ${url.shareContext}
   
# Create site/Edit Site Details

There is a checkbox that creates/enables/disables an action on the discussions folder that results in an email being sent via the share_discussion_notification script

# Topic/Reply editing

It is possible to paste images into the editor directly

# Topic filtering

The default filter is changed from New to All, and the order changed so that All is on top

# Custom ActivityService (see ALF-21752)
Allows the service to be called using RunAs so as to support the outbound mail site_activity parameter
