package net.malariagen.alfresco.action;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.action.ParameterDefinitionImpl;
import org.alfresco.repo.action.evaluator.IsSubTypeEvaluator;
import org.alfresco.repo.action.evaluator.NoConditionEvaluator;
import org.alfresco.repo.action.executer.ActionExecuterAbstractBase;
import org.alfresco.repo.action.executer.ScriptActionExecuter;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ActionCondition;
import org.alfresco.service.cmr.action.ActionService;
import org.alfresco.service.cmr.action.CompositeAction;
import org.alfresco.service.cmr.action.ParameterDefinition;
import org.alfresco.service.cmr.dictionary.DataTypeDefinition;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.rule.Rule;
import org.alfresco.service.cmr.rule.RuleService;
import org.alfresco.service.cmr.rule.RuleType;

public class ModifyRule extends ActionExecuterAbstractBase {

	public static final String NAME = "modify-rule";
	public static final String PARAM_ENABLE = "enable";
	public static final String PARAM_ACTION_NAME = "action_name";
	public static final String PARAM_SCRIPTREF = "script_ref";

	private ActionService actionService;
	private RuleService ruleService;

	public void setActionService(ActionService actionService) {
		this.actionService = actionService;
	}

	public void setRuleService(RuleService ruleService) {
		this.ruleService = ruleService;
	}

	@Override
	protected void executeImpl(Action action, NodeRef actionedUponNodeRef) {
		// TODO Auto-generated method stub

		NodeRef scriptref = (NodeRef) action.getParameterValue(PARAM_SCRIPTREF);
		String actionName = (String) action.getParameterValue(PARAM_ACTION_NAME);
		Boolean enable = (Boolean) action.getParameterValue(PARAM_ENABLE);

		List<Rule> rulesForNode = ruleService.getRules(actionedUponNodeRef);

		boolean found = false;
		for (Rule existingRule : rulesForNode) {

			if (existingRule.getTitle().equals(actionName)) {
				if (enable) {
					if (existingRule.getRuleDisabled()) {
						existingRule.setRuleDisabled(false);
						ruleService.saveRule(actionedUponNodeRef, existingRule);
					}
				} else {
					if (!existingRule.getRuleDisabled()) {
						existingRule.setRuleDisabled(true);
						ruleService.saveRule(actionedUponNodeRef, existingRule);
					}
				}
				found = true;
				break;
			}
		}
		if (!found && enable) {
			createRule(actionedUponNodeRef, scriptref, actionName);
		}
		
	}

	private void createRule(NodeRef actionedUponNodeRef, NodeRef scriptref, String actionName) {
		Rule rule = new Rule();
		rule.setRuleType(RuleType.INBOUND);
		rule.setTitle(actionName);
		rule.applyToChildren(true); // set this to true if you want to cascade
										// to sub folders

		CompositeAction compositeAction = actionService.createCompositeAction();
		rule.setAction(compositeAction);

		ActionCondition actionCondition = actionService.createActionCondition(NoConditionEvaluator.NAME);

/*		Map<String, Serializable> conditionParameters = new HashMap<String, Serializable>(1);
		conditionParameters.put(IsSubTypeEvaluator.PARAM_TYPE, ContentModel.TYPE_CONTENT);

		actionCondition.setParameterValues(conditionParameters);
*/
		compositeAction.addActionCondition(actionCondition);

		Action createAction = actionService.createAction(ScriptActionExecuter.NAME);

		createAction.setTitle(actionName);
		createAction.setExecuteAsynchronously(false);

		Map<String, Serializable> ruleParameters = new HashMap<String, Serializable>(1);
		ruleParameters.put(ScriptActionExecuter.PARAM_SCRIPTREF, scriptref);

		createAction.setParameterValues(ruleParameters);

		compositeAction.addAction(createAction);

		ruleService.saveRule(actionedUponNodeRef, rule); 
	}

	@Override
	protected void addParameterDefinitions(List<ParameterDefinition> paramList) {
		// TODO Auto-generated method stub
		paramList.add(new ParameterDefinitionImpl(PARAM_SCRIPTREF, DataTypeDefinition.NODE_REF, true,
				getParamDisplayLabel(PARAM_SCRIPTREF)));
		paramList.add(new ParameterDefinitionImpl(PARAM_ACTION_NAME, DataTypeDefinition.TEXT, true,
				getParamDisplayLabel(PARAM_ACTION_NAME)));
		paramList.add(new ParameterDefinitionImpl(PARAM_ENABLE, DataTypeDefinition.BOOLEAN, true,
				getParamDisplayLabel(PARAM_ENABLE)));
	}

}
