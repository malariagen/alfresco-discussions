package net.malariagen.alfresco.model;

import org.alfresco.service.namespace.QName;

public interface MDGContentModel {
	static final QName ASPECT_ATTACHMENT = QName.createQName(MGDNamespaceService.DISCUSSIONS_MODEL_1_0_URI, "discussionAttachment");
	static final QName ASSOC_ATTACHMENT = QName.createQName(MGDNamespaceService.DISCUSSIONS_MODEL_1_0_URI, "relatedTopic");
}
