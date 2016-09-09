<#assign el=args.htmlid?html>
<@markup id="custom-fields" action="after" target="fields"> 
<#if profile.customProperties??>
<#if profile.customProperties?size != 0>
  <#list profile.customProperties?keys as prop>
   <#assign customValue=profile.customProperties[prop].value>
   <#assign customName=profile.customProperties[prop].name?substring(profile.customProperties[prop].name?index_of("}") + 1)>
    <div class="yui-gd">
       <div class="yui-u first"><label
       for="${el}-discussionsNotification">${customName}(${customValue}):</label></div>
       <div class="yui-u"><input id="${el}-discussionsNotification" type="checkbox" <#if
       (customValue == "true")>checked="checked"</#if>
       name="discussionsNotification" tabindex="0" value="true" /></div>
    </div>
  </#list>
</#if>
</#if>
</@markup>
