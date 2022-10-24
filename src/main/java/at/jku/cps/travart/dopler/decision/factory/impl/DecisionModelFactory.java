package at.jku.cps.travart.dopler.decision.factory.impl;

import at.jku.cps.travart.dopler.decision.factory.IDecisionModelFactory;
import at.jku.cps.travart.dopler.decision.impl.DecisionModel;
import at.jku.cps.travart.dopler.decision.model.ARangeValue;
import at.jku.cps.travart.dopler.decision.model.IAction;
import at.jku.cps.travart.dopler.decision.model.ICondition;
import at.jku.cps.travart.dopler.decision.model.IDecision;
import at.jku.cps.travart.dopler.decision.model.impl.And;
import at.jku.cps.travart.dopler.decision.model.impl.BooleanDecision;
import at.jku.cps.travart.dopler.decision.model.impl.Cardinality;
import at.jku.cps.travart.dopler.decision.model.impl.DoubleValue;
import at.jku.cps.travart.dopler.decision.model.impl.EnumDecision;
import at.jku.cps.travart.dopler.decision.model.impl.Equals;
import at.jku.cps.travart.dopler.decision.model.impl.GetValueFunction;
import at.jku.cps.travart.dopler.decision.model.impl.IsTakenFunction;
import at.jku.cps.travart.dopler.decision.model.impl.NumberDecision;
import at.jku.cps.travart.dopler.decision.model.impl.Range;
import at.jku.cps.travart.dopler.decision.model.impl.Rule;
import at.jku.cps.travart.dopler.decision.model.impl.SelectDecisionAction;
import at.jku.cps.travart.dopler.decision.model.impl.StringDecision;
import at.jku.cps.travart.dopler.decision.model.impl.StringValue;

public class DecisionModelFactory implements IDecisionModelFactory {

	public static final String ID = "at.jku.cps.travart.dopler.decision.factory.DecisionModelFactory";

	public static DecisionModelFactory getInstance() {
		return new DecisionModelFactory();
	}

	@Override
	public String getId() {
		return ID;
	}

	@Override
	public boolean initExtension() {
		return true;
	}

	@Override
	public DecisionModel create() {
		return new DecisionModel(ID);
	}

	@Override
	public BooleanDecision createBooleanDecision(final String id) {
		BooleanDecision decision = new BooleanDecision(id);
		decision.setQuestion(id.concat("?"));
		decision.setDescription(id.concat(" description"));
		return decision;
	}

	@Override
	public Cardinality createCardinality(final int min, final int max) {
		return new Cardinality(min, max);
	}

	@Override
	public EnumDecision createEnumDecision(final String id) {
		EnumDecision decision = new EnumDecision(id);
		decision.setQuestion(id.concat("?"));
		decision.setDescription(id.concat(" description"));
		return decision;
	}

	@Override
	public Range<String> createEnumValueOptions(final String[] options) {
		Range<String> range = new Range<>();
		for (String o : options) {
			range.add(new StringValue(o));
		}
		return range;
	}

	@Override
	public NumberDecision createNumberDecision(final String id) {
		NumberDecision decision = new NumberDecision(id);
		decision.setQuestion(id.concat("?"));
		decision.setDescription(id.concat(" description"));
		return decision;
	}

	@Override
	public Range<Double> createNumberValueRange(final String[] ranges) {
		if (ranges.length % 2 != 0) {
			throw new IllegalArgumentException("Range does not have even number of range values");
		}
		Range<Double> range = new Range<>();
		for (int i = 0; i < ranges.length; i += 2) {
			double min = parseDouble(ranges[i]);
			double max = parseDouble(ranges[i + 1]);
			for (double value = min; Math.round(value) <= Math.round(max); value++) {
				DoubleValue v = new DoubleValue(value);
				range.add(v);
			}
		}
		return range;
	}

	private double parseDouble(final String str) {
		try {
			return Double.parseDouble(str);
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("Given String " + str + " is not a number.");
		}
	}

	@Override
	public StringDecision createStringDecision(final String id) {
		StringDecision decision = new StringDecision(id);
		decision.setQuestion(id.concat("?"));
		decision.setDescription(id.concat(" description"));
		return decision;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public IsTakenFunction createIsTakenFunction(final IDecision decision) {
		return new IsTakenFunction(decision);
	}

	@Override
	public And createAndCondition(final ICondition left, final ICondition right) {
		return new And(left, right);
	}

	@SuppressWarnings("rawtypes")
	@Override
	public GetValueFunction createGetValueFunction(final IDecision decision) {
		return new GetValueFunction(decision);
	}

	@Override
	public ICondition createEquals(final ICondition getValue, final ARangeValue<?> value) {
		return new Equals(getValue, value);
	}

	@Override
	public IAction createSelectDecisionAction(final BooleanDecision decision) {
		return new SelectDecisionAction(decision);
	}

	@Override
	public Rule createRule(final ICondition condition, final IAction action) {
		return new Rule(condition, action);
	}
}