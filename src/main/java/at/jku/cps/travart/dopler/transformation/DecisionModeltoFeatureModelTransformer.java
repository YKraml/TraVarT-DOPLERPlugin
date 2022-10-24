package at.jku.cps.travart.dopler.transformation;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.prop4j.Literal;
import org.prop4j.Node;

import at.jku.cps.travart.core.common.IModelTransformer;
import at.jku.cps.travart.core.common.Prop4JUtils;
import at.jku.cps.travart.core.common.TraVarTUtils;
import at.jku.cps.travart.core.common.exc.ConditionCreationException;
import at.jku.cps.travart.core.common.exc.NotSupportedVariablityTypeException;
import at.jku.cps.travart.core.exception.NotSupportedVariabilityTypeException;
import at.jku.cps.travart.dopler.common.DecisionModelUtils;
import at.jku.cps.travart.dopler.decision.IDecisionModel;
import at.jku.cps.travart.dopler.decision.exc.CircleInConditionException;
import at.jku.cps.travart.dopler.decision.model.ABinaryCondition;
import at.jku.cps.travart.dopler.decision.model.ADecision;
import at.jku.cps.travart.dopler.decision.model.AFunction;
import at.jku.cps.travart.dopler.decision.model.ARangeValue;
import at.jku.cps.travart.dopler.decision.model.IAction;
import at.jku.cps.travart.dopler.decision.model.ICondition;
import at.jku.cps.travart.dopler.decision.model.IDecision;
import at.jku.cps.travart.dopler.decision.model.IValue;
import at.jku.cps.travart.dopler.decision.model.impl.And;
import at.jku.cps.travart.dopler.decision.model.impl.BooleanDecision;
import at.jku.cps.travart.dopler.decision.model.impl.BooleanValue;
import at.jku.cps.travart.dopler.decision.model.impl.Cardinality;
import at.jku.cps.travart.dopler.decision.model.impl.DeSelectDecisionAction;
import at.jku.cps.travart.dopler.decision.model.impl.DecisionValueCondition;
import at.jku.cps.travart.dopler.decision.model.impl.DisAllowAction;
import at.jku.cps.travart.dopler.decision.model.impl.EnumDecision;
import at.jku.cps.travart.dopler.decision.model.impl.Equals;
import at.jku.cps.travart.dopler.decision.model.impl.Greater;
import at.jku.cps.travart.dopler.decision.model.impl.GreaterEquals;
import at.jku.cps.travart.dopler.decision.model.impl.IsSelectedFunction;
import at.jku.cps.travart.dopler.decision.model.impl.Less;
import at.jku.cps.travart.dopler.decision.model.impl.LessEquals;
import at.jku.cps.travart.dopler.decision.model.impl.Not;
import at.jku.cps.travart.dopler.decision.model.impl.NumberDecision;
import at.jku.cps.travart.dopler.decision.model.impl.Rule;
import at.jku.cps.travart.dopler.decision.model.impl.SelectDecisionAction;
import at.jku.cps.travart.dopler.decision.model.impl.SetValueAction;
import at.jku.cps.travart.dopler.decision.model.impl.StringDecision;
import at.jku.cps.travart.dopler.decision.model.impl.StringValue;
import de.ovgu.featureide.fm.core.ExtensionManager.NoSuchExtensionException;
import de.ovgu.featureide.fm.core.base.FeatureUtils;
import de.ovgu.featureide.fm.core.base.IConstraint;
import de.ovgu.featureide.fm.core.base.IFeature;
import de.ovgu.featureide.fm.core.base.IFeatureModel;
import de.ovgu.featureide.fm.core.base.IFeatureModelFactory;
import de.ovgu.featureide.fm.core.base.impl.DefaultFeatureModelFactory;
import de.ovgu.featureide.fm.core.base.impl.FMFactoryManager;
import de.ovgu.featureide.fm.core.init.FMCoreLibrary;
import de.ovgu.featureide.fm.core.init.LibraryManager;
import de.vill.main.UVLModelFactory;
import de.vill.model.Feature;
import de.vill.model.FeatureModel;
import de.vill.model.Group;
import de.vill.model.Group.GroupType;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class DecisionModeltoFeatureModelTransformer implements IModelTransformer<IDecisionModel> {

	static {
		LibraryManager.registerLibrary(FMCoreLibrary.getInstance());
	}

	private FeatureModel fm;
	private IDecisionModel dm;

	public FeatureModel transform(IDecisionModel arg0, String arg1) throws NotSupportedVariabilityTypeException {
		this.dm = dm;
		this.fm = new FeatureModel();
		try {
			createFeatures();
			createFeatureTree();
			createConstraints();
			TraVarTUtils.deriveFeatureModelRoot(factory, fm, true);
			return fm;
		} catch (NoSuchExtensionException | CircleInConditionExaception | ConditionCreationException e) {
			throw new NotSupportedVariablityTypeException(e);
		}
		return null;
	}

	public IDecisionModel transform(FeatureModel arg0, String arg1) throws NotSupportedVariabilityTypeException {
		// TODO Auto-generated method stub
		return null;
	}

	private void createFeatures() throws CircleInConditionException {
		// first add all Boolean decisions
		for (BooleanDecision decision : DecisionModelUtils.getBooleanDecisions(dm)) {
			String featureName = retriveFeatureName(decision);
			Feature feature = new Feature(featureName);
			// Features are optional by default
			fm.getFeatureMap().put(featureName, feature);
		}
		// second add all Number decisions
		for (NumberDecision decision : DecisionModelUtils.getNumberDecisions(dm)) {
			String featureName = retriveFeatureName(decision);
			Feature feature = new Feature(featureName);
			Group alternativeGroup = null;
			if (!decision.getRange().isEmpty()) {
				alternativeGroup = new Group(GroupType.ALTERNATIVE);
			}
			for (Object o : decision.getRange()) {
				IValue value = (IValue) o;
				String childName = featureName
						+ DefaultDecisionModelTransformationProperties.CONFIGURATION_VALUE_SEPERATOR
						+ value.getValue().toString();
				// first check if you can find a feature with the same name as the value
				// if so use it, otherwise create it
				Feature child = fm.getFeatureMap().get(childName);
				if (child != null) {
					alternativeGroup.getFeatures().add(child);
				} else {
					child = new Feature(childName);
					fm.getFeatureMap().put(childName, child);
					alternativeGroup.getFeatures().add(child);
				}
			}
			feature.addChildren(alternativeGroup);
		}
		// third add all String decisions
		for (StringDecision decision : DecisionModelUtils.getStringDecisions(dm)) {
			String featureName = retriveFeatureName(decision);
			Feature feature = new Feature(featureName);
			// Features are optional by default
			fm.getFeatureMap().put(featureName, feature);
		}
		// finally build enumeration decisions from exiting features
		for (EnumDecision decision : DecisionModelUtils.getEnumDecisions(dm)) {
			if (!DecisionModelUtils.isEnumDecisionConstraint(decision)) {
				String featureName = retriveFeatureName(decision);
				Feature feature = fm.getFeatureMap().get(featureName);
				if (feature == null) {
					feature = new Feature(featureName);
					fm.getFeatureMap().put(featureName, feature);
				}
				Cardinality cardinality = decision.getCardinality();
				Group enumGroup;
				if (cardinality.isAlternative()) {
					enumGroup = new Group(GroupType.ALTERNATIVE);
				} else if (cardinality.isOr()) {
					enumGroup = new Group(GroupType.OR);
				}
				for (Object o : decision.getRange()) {
					IValue value = (IValue) o;
					String childName = value.getValue().toString();
					// first check if you can find a feature with the same name as the value
					// if so use it, otherwise create it
					Feature child = fm.getFeatureMap().get(childName);
					if (child != null) {
						fm.getFeatureMap().put(childName, child);
						enumGroup.getFeatures().add(child);
					}
					// If None value is read, don't add it, as it is special added for optional
					// groups
					else if (!DecisionModelUtils.isEnumNoneOption(decision, value)) {
						child = new Feature(childName);
						fm.getFeatureMap().put(childName, child);
						enumGroup.getFeatures().add(child);
					}
				}
				// Features are optional by default
				feature.addChildren(enumGroup);
			} else {
				// TODO: return transformation if it contains at least 2 # in decision id, then
				// it is a constraint --> build constraint based on cardinality and setdecision
				// actions in rules
				List<Feature> features = new ArrayList<>(decision.getRange().size());
				decision.getRange()
						.forEach(val -> features.add(fm.getFeatureMap().get(String.valueOf(val.getValue()))));
				IConstraint constraint = null;
				if (decision.getCardinality().isOr()) {
					Node ruleCondition = TraVarTUtils.consumeToBinaryCondition(features, org.prop4j.Or.class, false);
					constraint = factory.createConstraint(fm, ruleCondition);
				} else if (decision.getCardinality().isAnd()) {
					Node ruleCondition = TraVarTUtils.consumeToBinaryCondition(features, org.prop4j.And.class, false);
					constraint = factory.createConstraint(fm, ruleCondition);
				}
				addConstraintIfEligible(constraint);
			}
		}
	}

	private void createFeatureTree() {
		for (IDecision decision : dm.getDecisions()) {
			if (!DecisionModelUtils.isEnumDecisionConstraint(decision)) {
				Feature feature = fm.getFeatureMap().get(retriveFeatureName(decision));
				if (getParent(fm,feature) == null) {
					ICondition visiblity = decision.getVisiblity();
					if (DecisionModelUtils.isMandatoryVisibilityCondition(visiblity)) {
//						FeatureUtils.setMandatory(feature, true);
						IDecision parentD = DecisionModelUtils.retriveMandatoryVisibilityCondition(visiblity);
						String parentFName = retriveFeatureName(parentD);
						Feature parentF = fm.getFeatureMap().get(parentFName);
						if(!parentF.getChildren().stream().anyMatch(g->g.GROUPTYPE.equals(GroupType.MANDATORY))) {
							parentF.getChildren().add(new Group(GroupType.MANDATORY));
						}
						Group mandatoryGroup=parentF.getChildren().stream().filter(g->g.GROUPTYPE.equals(GroupType.MANDATORY)).findFirst().get();
						mandatoryGroup.getFeatures().add(feature);
					} else if (visiblity instanceof IsSelectedFunction) {
						IsSelectedFunction isSelected = (IsSelectedFunction) visiblity;
						ADecision parentD = (ADecision) isSelected.getParameters().get(0);
						String parentFName = retriveFeatureName(parentD);
						Feature parentF = fm.getFeatureMap().get(parentFName);
						if (parentF != feature) {
							FeatureUtils.addChild(parentF, feature);
						}
					}
				}
			}
		}
	}

	/**
	 * Gets the parent of a feature by iterating the featuremap and checking the children for all features.
	 * May take a long time for large models, use with care.
	 * @param fm		the feature model containing the feature
	 * @param feat		the feature whose parent is desired
	 * @return			the parent feature
	 */
	private static Feature getParent(FeatureModel fm, Feature feat) {
		Optional<Feature> parent = fm.getFeatureMap().values().stream().filter(f -> f.getChildren().stream()
				.flatMap(g -> g.getFeatures().stream()).anyMatch(c -> c.getFeatureName().equals(feat.getFeatureName())))
				.findFirst();
		return parent.isPresent() ? parent.get() : null;
	}

	private String retriveFeatureName(final IDecision decision) {
		return DecisionModelUtils.retriveFeatureName(decision, decision.getType() == ADecision.DecisionType.ENUM);
	}

	private void createConstraints() {
		for (IDecision decision : dm.getDecisions()) {
			if (!DecisionModelUtils.isEnumDecisionConstraint(decision)) {
				// first store complex visibility condition in properties, to avoid losing the
				// information
				if (DecisionModelUtils.isComplexVisibilityCondition(decision.getVisiblity())
						&& !DecisionModelUtils.isMandatoryVisibilityCondition(decision.getVisiblity())) {
					IFeature feature = fm.getFeature(retriveFeatureName(decision));
					// add visibility as property for restoring them later if necessary
					feature.getCustomProperties().set(
							DefaultDecisionModelTransformationProperties.PROPERTY_KEY_VISIBILITY,
							DefaultDecisionModelTransformationProperties.PROPERTY_KEY_VISIBILITY_TYPE,
							decision.getVisiblity().toString());
				}
				// second transform constraints from the rules
				for (Object o : decision.getRules()) {
					Rule rule = (Rule) o;
					if (!DecisionModelUtils.isInItSelfRule(rule)) {
						if (DecisionModelUtils.isCompareCondition(rule.getCondition())) {
							deriveCompareConstraints(decision, rule.getCondition(), rule.getAction());
						} else {
							deriveConstraint(decision, rule.getCondition(), rule.getAction());
						}
					}
				}
			}
		}
	}

	private void deriveCompareConstraints(final IDecision decision, final ICondition condition, final IAction action) {
		if (decision.getType() == ADecision.DecisionType.NUMBER && action instanceof DisAllowAction) {
			NumberDecision numberDecision = (NumberDecision) decision;
			ABinaryCondition binCondition = (ABinaryCondition) condition;
			ARangeValue<Double> conditionValue = (ARangeValue<Double>) binCondition.getRight();
			IFeature disAllowFeature = fm.getFeature(((DisAllowAction) action).getValue().toString());
			Literal disAllowLiteral = Prop4JUtils.createLiteral(disAllowFeature);
			if (condition instanceof Equals) {
				createExcludesConstraint(numberDecision, disAllowLiteral, conditionValue);
			} else if (condition instanceof Greater) {
				Set<ARangeValue<Double>> values = numberDecision.getRange().stream()
						.filter(v -> v.getValue() > conditionValue.getValue()).collect(Collectors.toSet());
				for (ARangeValue<Double> value : values) {
					createExcludesConstraint(numberDecision, disAllowLiteral, value);
				}
			} else if (condition instanceof Less) {
				Set<ARangeValue<Double>> values = numberDecision.getRange().stream()
						.filter(v -> v.getValue() < conditionValue.getValue()).collect(Collectors.toSet());
				for (ARangeValue<Double> value : values) {
					createExcludesConstraint(numberDecision, disAllowLiteral, value);
				}
			} else if (condition instanceof GreaterEquals) {
				Set<ARangeValue<Double>> values = numberDecision.getRange().stream()
						.filter(v -> v.getValue() >= conditionValue.getValue()).collect(Collectors.toSet());
				for (ARangeValue<Double> value : values) {
					createExcludesConstraint(numberDecision, disAllowLiteral, value);
				}
			} else if (condition instanceof LessEquals) {
				Set<ARangeValue<Double>> values = numberDecision.getRange().stream()
						.filter(v -> v.getValue() <= conditionValue.getValue()).collect(Collectors.toSet());
				for (ARangeValue<Double> value : values) {
					createExcludesConstraint(numberDecision, disAllowLiteral, value);
				}
			}
		}
	}

	private void createExcludesConstraint(final NumberDecision numberDecision, final Literal disAllowLiteral,
			final ARangeValue<Double> value) {
		IFeature valueFeature = fm.getFeature(
				numberDecision.getId() + DefaultDecisionModelTransformationProperties.CONFIGURATION_VALUE_SEPERATOR
						+ value.getValue().toString());
		Literal valueLiteral = Prop4JUtils.createLiteral(valueFeature);
		IConstraint constraint = factory.createConstraint(fm,
				Prop4JUtils.createImplies(valueLiteral, Prop4JUtils.createNot(disAllowLiteral)));
		addConstraintIfEligible(constraint);
	}

	private void deriveConstraint(final IDecision decision, final ICondition condition, final IAction action) {
		Node conditionNode = deriveConditionNode(decision, condition);
		// case: if decision is selected another one has to be selected as well: implies
		if (condition instanceof IsSelectedFunction && action.getVariable() instanceof ADecision
				&& action.getValue().equals(BooleanValue.getTrue())) {
			IsSelectedFunction isSelected = (IsSelectedFunction) condition;
			ADecision conditionDecision = (ADecision) isSelected.getParameters().get(0);
			String conditionFeatureName = retriveFeatureName(conditionDecision);
			IFeature conditionFeature = fm.getFeature(conditionFeatureName);
			ADecision variableDecision = (ADecision) action.getVariable();
			String variableFeatureName = retriveFeatureName(variableDecision);
			IFeature variableFeature = fm.getFeature(variableFeatureName);
			if (FeatureUtils.getParent(variableFeature) != conditionFeature
					|| !FeatureUtils.isMandatorySet(variableFeature)) {
				IConstraint constraint = factory.createConstraint(fm,
						Prop4JUtils.createImplies(conditionNode, Prop4JUtils.createLiteral(variableFeature)));
				addConstraintIfEligible(constraint);
			}
		}
		// case: excludes constraint
		else if (condition instanceof IsSelectedFunction && action.getVariable() instanceof ADecision
				&& action.getValue().equals(BooleanValue.getFalse())) {
			ADecision variableDecision = (ADecision) action.getVariable();
			String featureName = retriveFeatureName(variableDecision);
			IFeature feature = fm.getFeature(featureName);
			IConstraint constraint = factory.createConstraint(fm, Prop4JUtils.createImplies(conditionNode,
					Prop4JUtils.createNot(Prop4JUtils.createLiteral(feature))));
			addConstraintIfEligible(constraint);
		}
		// case: if the action allows or disallows/sets a decision to a None value
		else if (condition instanceof IsSelectedFunction && action instanceof SetValueAction
				&& DecisionModelUtils.isNoneAction(action)) {
			ADecision variableDecision = (ADecision) action.getVariable();
			String featureName = retriveFeatureName(variableDecision);
			// set cardinality of variable feature
			IFeature varFeature = fm.getFeature(featureName);
			IConstraint constraint = factory.createConstraint(fm, Prop4JUtils.createImplies(conditionNode,
					Prop4JUtils.createNot(Prop4JUtils.createLiteral(varFeature))));
			addConstraintIfEligible(constraint);
		}
		// case: condition is negated: if both bool then it is an or constraint,
		// otherwise implies
		else if (condition instanceof Not && DecisionModelUtils.isNoneCondition(condition)
				&& action instanceof SelectDecisionAction) {
			String conditionFeatureName = retriveFeatureName(decision);
			ADecision variableDecision = (ADecision) action.getVariable();
			String variableDecisionFeatureName = retriveFeatureName(variableDecision);
			IFeature conditionFeature = fm.getFeature(conditionFeatureName);
			IFeature variableFeature = fm.getFeature(variableDecisionFeatureName);
			IConstraint constraint = factory.createConstraint(fm, Prop4JUtils.createImplies(
					Prop4JUtils.createLiteral(conditionFeature), Prop4JUtils.createLiteral(variableFeature)));
			addConstraintIfEligible(constraint);
		}
		// case: if the condition is a enum value and sets the value of a different
		// decision (Number/String decision)
		else if (!(condition instanceof Not) && action.getVariable() instanceof ADecision
				&& !DecisionModelUtils.isEnumDecisionConstraint(action.getVariable())
				&& action.getValue() instanceof ARangeValue && !action.getValue().equals(BooleanValue.getTrue())
				&& !action.getValue().equals(BooleanValue.getFalse())) {
			// create a new child feature for the feature the rule writes - if it is not a
			// none value
			ADecision variableDecision = (ADecision) action.getVariable();
			// feature name
			String variableDecisionFeatureName = retriveFeatureName(variableDecision);
			// set cardinality of variable feature
			IFeature variableFeature = fm.getFeature(variableDecisionFeatureName);
			if (DecisionModelUtils.isNoneAction(action)) {
				IConstraint constraint = factory.createConstraint(fm,
						Prop4JUtils.createImplies(conditionNode, Prop4JUtils.createLiteral(variableFeature)));
				addConstraintIfEligible(constraint);
			} else {
				IFeature valueFeature = fm.getFeature(action.getValue().toString());
				// if it was not found, then create it
				if (valueFeature == null) {// && !isNoneAction(action)) {
					valueFeature = factory.createFeature(fm,
							variableDecisionFeatureName
									+ DefaultDecisionModelTransformationProperties.CONFIGURATION_VALUE_SEPERATOR
									+ action.getValue());
					// add value feature as part of the feature model and as child to the variable
					// feature if it was created
					FeatureUtils.addFeature(fm, valueFeature);
					FeatureUtils.addChild(variableFeature, valueFeature);
				}
				if (variableDecision instanceof EnumDecision) {
					Cardinality cardinality = ((EnumDecision) variableDecision).getCardinality();
					if (cardinality.isAlternative()) {
						FeatureUtils.setAlternative(variableFeature);
					} else if (cardinality.isOr()) {
						FeatureUtils.setOr(variableFeature);
					}
				}
				// create and add constraint for setting the condition to set, if is no value
				// condition
				IConstraint constraint = null;
				if (action instanceof DisAllowAction) {
					constraint = factory.createConstraint(fm, Prop4JUtils.createImplies(conditionNode,
							Prop4JUtils.createNot(Prop4JUtils.createLiteral(valueFeature))));
				} else {
					constraint = factory.createConstraint(fm,
							Prop4JUtils.createImplies(conditionNode, Prop4JUtils.createLiteral(valueFeature)));
				}
				addConstraintIfEligible(constraint);
			}
		} else if (DecisionModelUtils.isComplexCondition(condition)
				&& (action instanceof SelectDecisionAction || action instanceof DeSelectDecisionAction)) {
			Set<IDecision> conditionDecisions = DecisionModelUtils.retriveConditionDecisions(condition);
			List<Literal> conditionLiterals = new ArrayList<>(conditionDecisions.size());
			for (IDecision conditionDecision : conditionDecisions) {
				String conditionFeatureName = retriveFeatureName(conditionDecision);
				IFeature conditionFeature = fm.getFeature(conditionFeatureName);
				conditionLiterals.add(Prop4JUtils.createLiteral(conditionFeature));
			}
			if (Prop4JUtils.isAnd(conditionNode)) {
//				if () {
//					conditionNode = Prop4JUtils.consumeToOrGroup(conditionLiterals, true);
//				} else {
				conditionNode = Prop4JUtils.consumeToAndGroup(conditionLiterals,
						Prop4JUtils.hasNegativeLiteral(conditionNode));
				// }
//			} else if (Prop4JUtils.hasNegativeLiteral(conditionNode)) {
//				conditionNode = Prop4JUtils.consumeToAndGroup(conditionLiterals, true);
			} else {
				conditionNode = Prop4JUtils.consumeToOrGroup(conditionLiterals,
						!Prop4JUtils.hasNegativeLiteral(conditionNode));
			}
			ADecision variableDecision = (ADecision) action.getVariable();
			String variableDecisionFeatureName = retriveFeatureName(variableDecision);
			IFeature variableFeature = fm.getFeature(variableDecisionFeatureName);
			Literal variableLiteral = Prop4JUtils.createLiteral(variableFeature);
			IConstraint constraint;
			if (DecisionModelUtils.isNotCondition(condition)) {
				constraint = factory.createConstraint(fm, Prop4JUtils.createOr(conditionNode, variableLiteral));
			} else if (action instanceof SelectDecisionAction) {
				constraint = factory.createConstraint(fm, Prop4JUtils.createImplies(conditionNode, variableLiteral));
			} else if (action instanceof DeSelectDecisionAction) {
				variableLiteral.positive = false;
				constraint = factory.createConstraint(fm, Prop4JUtils.createImplies(conditionNode, variableLiteral));
			} else {
				variableLiteral.positive = false;
				constraint = factory.createConstraint(fm, Prop4JUtils.createOr(conditionNode, variableLiteral));
			}
			addConstraintIfEligible(constraint);
		}
	}

	private void addConstraintIfEligible(final IConstraint constraint) {
		if (constraint == null || TraVarTUtils.isInItSelfConstraint(constraint)) {
			return;
		}
		for (IConstraint constr : FeatureUtils.getConstraints(fm)) {
			if (constr.getNode().equals(constraint.getNode())) {
				return;
			}
		}
		FeatureUtils.addConstraint(fm, constraint);
	}

	private Node deriveConditionNode(final IDecision decision, final ICondition condition) {
		if (DecisionModelUtils.isBinaryCondition(condition)) {
			ABinaryCondition binCondition = (ABinaryCondition) condition;
			return deriveConditionNodeRecursive(binCondition.getLeft(), binCondition.getRight(), condition);
		}
		if (condition instanceof DecisionValueCondition) {
			ARangeValue value = ((DecisionValueCondition) condition).getValue();
			IFeature feature = fm.getFeature(value.toString());
			return Prop4JUtils.createLiteral(feature);
		}
		if (DecisionModelUtils.isNot(condition)) {
			Not notNode = (Not) condition;
			ICondition operand = notNode.getOperand();
			Literal literal;
			if (operand instanceof IsSelectedFunction) {
				IsSelectedFunction isSelected = (IsSelectedFunction) operand;
				ADecision d = (ADecision) isSelected.getParameters().get(0);
				String featureName = retriveFeatureName(d);
				IFeature feature = fm.getFeature(featureName);
				literal = Prop4JUtils.createLiteral(feature);
			} else {
				literal = Prop4JUtils.createLiteral(operand);
			}
			literal.positive = false;
			return literal;
		}
		if (condition instanceof IsSelectedFunction) {
			IsSelectedFunction isSelected = (IsSelectedFunction) condition;
			ADecision d = (ADecision) isSelected.getParameters().get(0);
			String featureName = retriveFeatureName(d);
			IFeature feature = fm.getFeature(featureName);
			return Prop4JUtils.createLiteral(feature);

		}
		if (condition instanceof StringValue) {
			return Prop4JUtils.createLiteral(((StringValue) condition).getValue());
		}
		return Prop4JUtils.createLiteral(condition);
	}

	private Node deriveConditionNodeRecursive(final ICondition left, final ICondition right,
			final ICondition condition) {
		Node cLeft = deriveNode(left);
		Node cRight = deriveNode(right);
		if (cLeft == null && cRight != null) {
			return cRight;
		}
		if (cLeft != null && cRight == null) {
			return cLeft;
		}
		if (condition instanceof And) {
			return Prop4JUtils.createAnd(cLeft, cRight);
		}
		return Prop4JUtils.createAnd(Prop4JUtils.createNot(cLeft), Prop4JUtils.createNot(cRight));
	}

	private Node deriveNode(final ICondition node) {
		if (DecisionModelUtils.isBinaryCondition(node)) {
			ABinaryCondition binVis = (ABinaryCondition) node;
			return deriveConditionNodeRecursive(binVis.getLeft(), binVis.getRight(), binVis);
		}
		if (node instanceof Not) {
			Not notNode = (Not) node;
			Literal literal = Prop4JUtils.createLiteral(notNode.getOperand());
			literal.positive = false;
			return literal;
		}
		if (node instanceof IsSelectedFunction) {
			IDecision decision = ((IsSelectedFunction) node).getParameters().get(0);
			IFeature feature = fm.getFeature(decision.getId());
			return Prop4JUtils.createLiteral(feature);
		}
		if (node instanceof AFunction) {
			return null;
		}
		return Prop4JUtils.createLiteral(node);
	}

}