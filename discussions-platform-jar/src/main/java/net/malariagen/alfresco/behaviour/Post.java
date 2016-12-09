package net.malariagen.alfresco.behaviour;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.alfresco.model.ContentModel;
import org.alfresco.model.ForumModel;
import org.alfresco.repo.content.ContentServicePolicies.OnContentUpdatePolicy;
import org.alfresco.repo.node.NodeServicePolicies.OnCreateNodePolicy;
import org.alfresco.repo.policy.Behaviour.NotificationFrequency;
import org.alfresco.repo.policy.JavaBehaviour;
import org.alfresco.repo.policy.PolicyComponent;
import org.alfresco.service.cmr.repository.AssociationRef;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.util.PropertyMap;
import org.apache.log4j.Logger;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.html.HtmlParser;
import org.apache.tika.sax.BodyContentHandler;
import org.apache.tika.sax.Link;
import org.apache.tika.sax.LinkContentHandler;
import org.apache.tika.sax.TeeContentHandler;
import org.apache.tika.sax.ToHTMLContentHandler;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

import net.malariagen.alfresco.model.MDGContentModel;

public class Post implements OnCreateNodePolicy, OnContentUpdatePolicy {

	// Dependencies
	private NodeService nodeService;
	private ContentService contentService;

	public void setContentService(ContentService contentService) {
		this.contentService = contentService;
	}

	private PolicyComponent policyComponent;

	public void setNodeService(NodeService nodeService) {
		this.nodeService = nodeService;
	}

	public void setPolicyComponent(PolicyComponent policyComponent) {
		this.policyComponent = policyComponent;
	}

	private Logger logger = Logger.getLogger(Post.class);

	public void init() {
		policyComponent.bindClassBehaviour(OnContentUpdatePolicy.QNAME, ForumModel.TYPE_POST,
				new JavaBehaviour(this, OnContentUpdatePolicy.QNAME.getLocalName(), NotificationFrequency.EVERY_EVENT));

	}

	public List<Link> parseAttachments(NodeRef nodeRef) {

		ContentReader contentReader = contentService.getReader(nodeRef, ContentModel.PROP_CONTENT);
		InputStream is = contentReader.getContentInputStream();

		LinkContentHandler linkHandler = new LinkContentHandler();
		BodyContentHandler textHandler = new BodyContentHandler();
		ToHTMLContentHandler toHTMLHandler = new ToHTMLContentHandler();
		TeeContentHandler teeHandler = new TeeContentHandler(linkHandler, textHandler, toHTMLHandler);
		Metadata metadata = new Metadata();
		ParseContext parseContext = new ParseContext();
		HtmlParser parser = new HtmlParser();
		try {
			parser.parse(is, teeHandler, metadata, parseContext);
		} catch (Exception e) {
			// Just logging as it doesn't really matter and shouldn't happen as it's a rich text editor
			logger.error("Failed to parse post:" + nodeRef.getId(), e);
		} finally {
			try {
				is.close();
			} catch (IOException e) {
				logger.error(e);
			}
		}
		return (linkHandler.getLinks());
	}

	public void contentChanged(NodeRef nodeRef) {
		if (this.nodeService.exists(nodeRef) == true
				&& this.nodeService.getType(nodeRef).equals(ForumModel.TYPE_POST)) {
			if (logger.isDebugEnabled()) {
				logger.debug("Post behaviour");
			}
			boolean discussion = true;
			NodeRef topicNodeRef = null;
			List<ChildAssociationRef> parents = nodeService.getParentAssocs(nodeRef);
			for (ChildAssociationRef parent : parents) {
				NodeRef parentNodeRef = parent.getParentRef();
				// Parent of post is always topic
				if (nodeService.getType(parentNodeRef).equals(ForumModel.TYPE_TOPIC)) {
					List<ChildAssociationRef> siblings = nodeService.getChildAssocs(parentNodeRef);
					if (siblings.isEmpty()) {
						return;
					}
					topicNodeRef = siblings.get(0).getChildRef();
				}
				List<ChildAssociationRef> grandParents = nodeService.getParentAssocs(parentNodeRef);
				for (ChildAssociationRef grandParent : grandParents) {
					// But only a comment has a grandParent of forum
					if (nodeService.getType(grandParent.getParentRef()).equals(ForumModel.TYPE_FORUM)) {
						discussion = false;
					}
				}
			}
			if (discussion) {
				if (logger.isDebugEnabled()) {
					logger.debug("Discussion:" + nodeRef);
				}
				List<AssociationRef> existingAttachments = new ArrayList<AssociationRef>();
				List<AssociationRef> currentAttachments = new ArrayList<AssociationRef>();
				existingAttachments = nodeService.getSourceAssocs(nodeRef, MDGContentModel.ASSOC_ATTACHMENT);
				List<Link> attachmentLinks = parseAttachments(nodeRef);

				//String encoding = contentService.getReader(nodeRef, ContentModel.PROP_CONTENT).getEncoding();

				PropertyMap properties = new PropertyMap(3);
				for (Link link : attachmentLinks) {
					try {
						URI lnk = new URI(link.getUri());
						MultiValueMap<String, String> parameters = UriComponentsBuilder.fromUri(lnk).build()
								.getQueryParams();

						List<String> nodeRefs = parameters.get("nodeRef");
						if (nodeRefs != null) {
							NodeRef attachmentRef = new NodeRef(nodeRefs.get(0));
							if (nodeService.exists(attachmentRef)) {

								if (logger.isDebugEnabled()) {
									logger.debug("Setting attachment aspect for:" + nodeRefs.get(0));
								}
								AssociationRef newAssoc = null;
								if (!nodeService.hasAspect(attachmentRef, MDGContentModel.ASPECT_ATTACHMENT)) {
									properties.clear();
									nodeService.addAspect(attachmentRef, MDGContentModel.ASPECT_ATTACHMENT, properties);
									newAssoc = nodeService.createAssociation(attachmentRef, topicNodeRef,
											MDGContentModel.ASSOC_ATTACHMENT);
									currentAttachments.add(newAssoc);
								} else {
									boolean found = false;
									for (AssociationRef old : existingAttachments) {
										if (old.getSourceRef().getId().equals(attachmentRef.getId())) {
											currentAttachments.add(old);
											found = true;
										}
									}
									if (!found) {
										newAssoc = nodeService.createAssociation(attachmentRef, topicNodeRef,
												MDGContentModel.ASSOC_ATTACHMENT);
										currentAttachments.add(newAssoc);
									}
								}
							}
						}

					} catch (URISyntaxException e) {
						// Only logging
						logger.debug("Invalid link:" + link.getUri(), e);
					}

				}

				for (AssociationRef old : existingAttachments) {
					if (!currentAttachments.contains(old)) {
						nodeService.removeAssociation(old.getSourceRef(), old.getTargetRef(),
								MDGContentModel.ASSOC_ATTACHMENT);
						List<AssociationRef> oldAttachments = nodeService.getTargetAssocs(nodeRef,
								MDGContentModel.ASSOC_ATTACHMENT);
						if (oldAttachments.isEmpty()) {
							nodeService.removeAspect(old.getSourceRef(), MDGContentModel.ASPECT_ATTACHMENT);
						}
					}
				}
			} else {
				if (logger.isDebugEnabled()) {
					logger.debug("Comment");
				}
			}

		}

	}

	public void onCreateNode(ChildAssociationRef childAssocRef) {
		if (logger.isDebugEnabled()) {
			logger.debug("Creating post");
		}
		contentChanged(childAssocRef.getChildRef());
	}

	public void onContentUpdate(NodeRef nodeRef, boolean newContent) {
		if (logger.isDebugEnabled()) {
			logger.debug("Updating post content");
		}
		contentChanged(nodeRef);
	}

}
