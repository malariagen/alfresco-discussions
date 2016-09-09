<@markup id="custom-properties" target="fields" action="after">
<#assign el=args.htmlid?html>
    <div class="yui-gd">
       <div class="yui-u first"><label for="${el}-idEntity">Enable discussions
       email:</label></div>
       <div class="yui-u"><input id="${el}-discussionsNotification"
       type="checkbox"
       name="discussionsNotification" value="true" tabindex="0" /></div>
    </div>
</@markup>
