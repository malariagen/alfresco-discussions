package net.malariagen.alfresco.email.server.handler;

import org.alfresco.email.server.handler.TopicEmailMessageHandler;
import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.model.ForumModel;
import org.alfresco.model.ApplicationModel;
import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.email.EmailMessage;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.cmr.repository.NodeService;

import java.text.SimpleDateFormat;
import java.util.Date;
import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.service.cmr.email.EmailMessagePart;
import org.alfresco.util.PropertyMap;

import java.io.InputStream;
import org.alfresco.service.cmr.repository.MimetypeService;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.namespace.NamespaceService;
/**
 * Handler implementation address to topic node.
 * 
 * @author maxim
 * @since 2.2
 */
public class CustomTopicEmailMessageHandler extends TopicEmailMessageHandler
{

    /**
     * {@inheritDoc}
     */
    public void processMessage(NodeRef nodeRef, EmailMessage message)
    {
        QName nodeTypeQName = getNodeService().getType(nodeRef);
        NodeRef topicNode = null;

        if (getDictionaryService().isSubClass(nodeTypeQName, ForumModel.TYPE_TOPIC))
        {
            topicNode = nodeRef;
        }
        else if (getDictionaryService().isSubClass(nodeTypeQName, ForumModel.TYPE_POST))
        {
            topicNode = getNodeService().getPrimaryParent(nodeRef).getParentRef();
            if (topicNode == null)
            {
                throw new AlfrescoRuntimeException("A POST node has no primary parent: " + nodeRef);
            }
        }
        else
        {
            throw new AlfrescoRuntimeException("\n" +
                    "Message handler " + this.getClass().getName() + " cannot handle type " + nodeTypeQName + ".\n" +
                    "Check the message handler mappings.");
        }
        addPostNode(nodeRef, topicNode, message);
    }

    /**
     * Posts content
     * 
     * @param nodeRef   Reference to node
     * @param message    Mail parser
     * @return          Returns the new post node
     */
    protected NodeRef addPostNode(NodeRef srcNodeRef, NodeRef nodeRef, EmailMessage message)
    {
        NodeService nodeService = getNodeService();
        Date now = new Date();
        String nodeName = "posted-" + new SimpleDateFormat("dd-MM-yyyy-hh-mm-ss").format(now) + ".html";

        PropertyMap properties = new PropertyMap(3);
        properties.put(ContentModel.PROP_NAME, nodeName);

        NodeRef postNodeRef = nodeService.getChildByName(nodeRef, ContentModel.ASSOC_CONTAINS, nodeName);
        if (postNodeRef == null)
        {
            ChildAssociationRef childAssoc = nodeService.createNode(
                    nodeRef,
                    ContentModel.ASSOC_CONTAINS,
                    QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, nodeName),
                    ForumModel.TYPE_POST,
                    properties);
            postNodeRef = childAssoc.getChildRef();
        }

        // Add necessary aspects
        properties.clear();
        properties.put(ContentModel.PROP_TITLE, nodeName);
        nodeService.addAspect(postNodeRef, ContentModel.ASPECT_TITLED, properties);
        properties.clear();
        properties.put(ApplicationModel.PROP_EDITINLINE, true);
        nodeService.addAspect(postNodeRef, ApplicationModel.ASPECT_INLINEEDITABLE, properties);
        properties.clear();
        nodeService.addAspect(postNodeRef, ContentModel.ASPECT_REFERENCING, properties);
        nodeService.addAspect(postNodeRef, ContentModel.ASPECT_SYNDICATION, properties);


        // Write content
        if (message.getBody() != null)
        {
	    EmailMessagePart body = message.getBody();
	//For a multi-part mime message the body can be converted to an attachment
            EmailMessagePart[] attachments = message.getAttachments();
            for (EmailMessagePart attachment : attachments)
            {
                String fileName = attachment.getFileName();
                  
                InputStream contentIs = attachment.getContent();
                    
                MimetypeService mimetypeService = getMimetypeService();
                String mimetype = mimetypeService.guessMimetype(fileName);
                String encoding = attachment.getEncoding();
                if (fileName.startsWith(message.getSubject() + " (part ") && mimetype.startsWith("text/html")) {
              	    body = attachment;
                }
            }
            writeContent(
                    postNodeRef,
                    body.getContent(),
                    body.getContentType(),
                    body.getEncoding());
        }
        else
        {
            writeContent(postNodeRef, "<The message was empty>", MimetypeMap.MIMETYPE_TEXT_PLAIN);
        }
        addEmailedAspect(postNodeRef, message);
	nodeService.createAssociation(postNodeRef, srcNodeRef, ContentModel.ASSOC_REFERENCES);
        
        // Done
        return postNodeRef;
    }
}
