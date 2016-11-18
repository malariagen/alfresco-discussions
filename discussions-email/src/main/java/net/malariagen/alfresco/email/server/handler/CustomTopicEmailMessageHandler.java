package net.malariagen.alfresco.email.server.handler;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;

import org.alfresco.email.server.handler.TopicEmailMessageHandler;
import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.model.ApplicationModel;
import org.alfresco.model.ContentModel;
import org.alfresco.model.ForumModel;
import org.alfresco.repo.admin.SysAdminParams;
import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.service.cmr.email.EmailMessage;
import org.alfresco.service.cmr.email.EmailMessagePart;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.MimetypeService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.datatype.DefaultTypeConverter;
import org.alfresco.service.cmr.site.SiteInfo;
import org.alfresco.service.cmr.site.SiteService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.PropertyMap;
import org.alfresco.util.UrlUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Handler implementation address to topic node.
 * 
 * @author maxim
 * @since 2.2
 */
public class CustomTopicEmailMessageHandler extends TopicEmailMessageHandler {

	private static Log logger = LogFactory.getLog(CustomTopicEmailMessageHandler.class);

	private final static String DOCLIB = "documentLibrary";
	private final static String ATTACHMENTS_FOLDER = "discussionAttachments";

	protected SiteService siteService;
	protected SysAdminParams sysAdminParams;
	
	public void setSysAdminParams(SysAdminParams sysAdminParams) {
		this.sysAdminParams = sysAdminParams;
	}

	public void setSiteService(SiteService siteService) {
		this.siteService = siteService;
	}

	/**
	 * {@inheritDoc}
	 */
	public void processMessage(NodeRef nodeRef, EmailMessage message) {
		QName nodeTypeQName = getNodeService().getType(nodeRef);
		NodeRef topicNode = null;

		if (getDictionaryService().isSubClass(nodeTypeQName, ForumModel.TYPE_TOPIC)) {
			topicNode = nodeRef;
		} else if (getDictionaryService().isSubClass(nodeTypeQName, ForumModel.TYPE_POST)) {
			topicNode = getNodeService().getPrimaryParent(nodeRef).getParentRef();
			if (topicNode == null) {
				throw new AlfrescoRuntimeException("A POST node has no primary parent: " + nodeRef);
			}
		} else {
			throw new AlfrescoRuntimeException("\n" + "Message handler " + this.getClass().getName()
					+ " cannot handle type " + nodeTypeQName + ".\n" + "Check the message handler mappings.");
		}
		addPostNode(nodeRef, topicNode, message);
	}

	/**
	 * 
	 * Posts content
	 * 
	 * @param nodeRef
	 *            Reference to node
	 * @param message
	 *            Mail parser
	 * @return Returns the new post node
	 */
	protected NodeRef addPostNode(final NodeRef srcNodeRef, final NodeRef nodeRef, final EmailMessage message) {

		NodeService nodeService = getNodeService();

		Date now = new Date();

		String nodeName = "posted-" + new SimpleDateFormat("dd-MM-yyyy-hh-mm-ss").format(now) + ".html";

		PropertyMap properties = new PropertyMap(3);

		properties.put(ContentModel.PROP_NAME, nodeName);

		NodeRef postNodeRef = nodeService.getChildByName(nodeRef, ContentModel.ASSOC_CONTAINS, nodeName);

		if (postNodeRef == null) {
			ChildAssociationRef childAssoc = nodeService.createNode(nodeRef, ContentModel.ASSOC_CONTAINS,
					QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, nodeName), ForumModel.TYPE_POST,
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

		properties.clear();

        properties.put(ContentModel.PROP_TITLE, message.getSubject());
        
        nodeService.addAspect(nodeRef, ContentModel.ASPECT_TITLED, properties);
        
		// Write content

		if (message.getBody() != null)

		{

			ArrayList<NodeRef> attachmentRefs = new ArrayList<NodeRef>();
			SiteInfo siteInfo = siteService.getSite(nodeRef);

			EmailMessagePart body = message.getBody();
			// For a multi-part mime message the body can be converted to an
			// attachment
			EmailMessagePart[] attachments = message.getAttachments();
			for (EmailMessagePart attachment : attachments) {
				String fileName = attachment.getFileName();

				MimetypeService mimetypeService = getMimetypeService();
				String mimetype = mimetypeService.guessMimetype(fileName);

				//String encoding = attachment.getEncoding();
				if (fileName.startsWith(message.getSubject() + " (part ")) {
					if (mimetype.startsWith(MimetypeMap.MIMETYPE_HTML)) {
						body = attachment;
					}
				} else {
					// It is an actual attachment so add it to the
					// ATTACHMENTS_FOLDER
					if (logger.isDebugEnabled()) {
						logger.debug("Found attachment:" + fileName);
					}

					NodeRef docLib = siteService.getContainer(siteInfo.getShortName(), DOCLIB);
					NodeRef attachmentFolder = nodeService.getChildByName(docLib, ContentModel.ASSOC_CONTAINS,
							ATTACHMENTS_FOLDER);
					if (attachmentFolder == null) {
						if (logger.isDebugEnabled()) {
							logger.debug("Creating attachments folder:" + siteInfo.getShortName() + " " + DOCLIB + " "
									+ ATTACHMENTS_FOLDER);
						}
						properties.clear();

						properties.put(ContentModel.PROP_NAME, ATTACHMENTS_FOLDER);

						ChildAssociationRef childAssoc = nodeService.createNode(docLib, ContentModel.ASSOC_CONTAINS,
								QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, ATTACHMENTS_FOLDER),
								ContentModel.TYPE_FOLDER, properties);
						attachmentFolder = childAssoc.getChildRef();

					}
					String attachmentName = fileName;
					int i = 0;
					// Make sure that the name is unique
					while (nodeService.getChildByName(attachmentFolder, ContentModel.ASSOC_CONTAINS,
							fileName) != null) {
						i++;
						fileName = attachmentName + i;
					}
					;

					if (logger.isDebugEnabled()) {
						logger.debug("Creating attachment file:" + fileName);
					}

					properties.clear();

					properties.put(ContentModel.PROP_NAME, fileName);
					properties.put(ContentModel.PROP_DESCRIPTION, message.getSubject());
					ChildAssociationRef childAssoc = nodeService.createNode(attachmentFolder,
							ContentModel.ASSOC_CONTAINS,
							QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, fileName),
							ContentModel.TYPE_CONTENT, properties);
					NodeRef attachmentRef = childAssoc.getChildRef();
					String contentType = attachment.getContentType();
					// Beware - new lines in contentType can break the json
					String[] contentTypes = contentType.split(";");
					String mimeType = "";
					if (contentTypes.length > 0) {
						mimeType = contentTypes[0];
					}
					String charset = "";
					for (int j = 1; j < contentTypes.length; j++) {
						if (contentTypes[j].trim().startsWith("charset")) {
							String[] cs = contentTypes[j].trim().split("=");
							if (cs.length > 1) {
								charset = cs[1];
							}
						}
					}
					int idx = mimeType.indexOf(';');
					if (idx > 0) {
						mimeType = mimeType.substring(0, idx);
					}
					writeContent(attachmentRef, attachment.getContent(), mimetype, charset);

					/* Done by behaviour
					properties.clear();
					nodeService.addAspect(attachmentRef, MDGContentModel.ASPECT_ATTACHMENT, properties);
					nodeService.createAssociation(attachmentRef, postNodeRef, MDGContentModel.ASSOC_ATTACHMENT);
					 */
					
					addEmailedAspect(attachmentRef, message);

					attachmentRefs.add(attachmentRef);
				}
			}

			writeContent(postNodeRef, body.getContent(), body.getContentType(), body.getEncoding());

			formatPost(postNodeRef, attachmentRefs, siteInfo, body);
		} else {
			writeContent(postNodeRef, "<The message was empty>", MimetypeMap.MIMETYPE_TEXT_PLAIN);
		}

		addEmailedAspect(postNodeRef, message);

		nodeService.createAssociation(postNodeRef, srcNodeRef, ContentModel.ASSOC_REFERENCES);

		// Done

		return postNodeRef;

	}

	/**
	 * Modify the post contents based on the incoming mail
	 * 
	 * @param postNodeRef the new post
	 * @param attachmentRefs a list of NodeRefs with any attachments
	 * @param siteInfo the current site
	 * @param body the original message
	 * 
	 */
	private void formatPost(NodeRef postNodeRef, ArrayList<NodeRef> attachmentRefs,
			SiteInfo siteInfo, EmailMessagePart body) {

		NodeService nodeService = getNodeService();

		boolean isPlainText = body.getContentType().startsWith(MimetypeMap.MIMETYPE_TEXT_PLAIN);
		// Surround plain text with pre
		if (isPlainText || attachmentRefs.size() > 0) {
			ContentService contentService = getContentService();
			ContentReader contentReader = contentService.getReader(postNodeRef, ContentModel.PROP_CONTENT);
			// It's reasonably safe to assume that the message will be quite
			// small
			String content = contentReader.getContentString();

			ContentWriter writer = contentService.getWriter(postNodeRef, ContentModel.PROP_CONTENT, true);
			writer.setMimetype(MimetypeMap.MIMETYPE_HTML);
			if (isPlainText) {
				content = "<pre>" + content + "</pre>";
			}

			if (attachmentRefs.size() > 0) {
				content += "<ul>";
				for (NodeRef attach : attachmentRefs) {
					
					
					final String siteName = siteInfo.getShortName();
					final Map<QName, Serializable> contentProps = nodeService.getProperties(attach);
					final String shareUrl = UrlUtil.getShareUrl(sysAdminParams);
					String workSpace = (String) contentProps.get(ContentModel.PROP_STORE_PROTOCOL);
					String spacesStore = (String) contentProps.get(ContentModel.PROP_STORE_IDENTIFIER);
					String uiid = (String) contentProps.get(ContentModel.PROP_NODE_UUID);
					String url = shareUrl + "/page/site/" + siteName + "/document-details?nodeRef=" + workSpace + "://" + spacesStore + "/" + uiid;
					/*
					String url = "/share/page/site/" + siteInfo.getShortName()
							+ "/document-details?nodeRef=" + attach;
							*/
					String name = DefaultTypeConverter.INSTANCE.convert(String.class,
							nodeService.getProperty(attach, ContentModel.PROP_NAME));
					content += "<li><a href=\"" + url + "\">" + name + "</a></li>";

				}
				content += "</ul>";
			}
			writer.putContent(content);

		}
	}
}
