package at.jku.cps.travart.dopler.transformation.oneway.feature.to.decision.constraint;

import at.jku.cps.travart.dopler.decision.IDecisionModel;
import at.jku.cps.travart.dopler.decision.model.IAction;
import at.jku.cps.travart.dopler.decision.model.ICondition;
import at.jku.cps.travart.dopler.decision.model.impl.Rule;
import at.jku.cps.travart.dopler.transformation.oneway.feature.to.decision.constraint.dnf.DnfToTreeConverter;
import at.jku.cps.travart.dopler.transformation.oneway.feature.to.decision.constraint.dnf.DnfToTreeConverterImpl;
import at.jku.cps.travart.dopler.transformation.oneway.feature.to.decision.constraint.dnf.TreeToDnfConverter;
import at.jku.cps.travart.dopler.transformation.oneway.feature.to.decision.constraint.dnf.TreeToDnfConverterImpl;
import de.vill.model.FeatureModel;
import de.vill.model.constraint.AndConstraint;
import de.vill.model.constraint.Constraint;
import de.vill.model.constraint.ImplicationConstraint;
import de.vill.model.constraint.NotConstraint;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/**
 * Implementation of {@link ConstraintHandler}.
 */
public class ConstraintHandlerImpl implements ConstraintHandler {

    private final TreeToDnfConverter treeToDnfConverter;
    private final DnfToTreeConverter dnfToTreeConverter;
    private final ActionCreator actionCreator;
    private final ConditionCreator conditionCreator;

    /** Constructor of {@link ConstraintHandlerImpl} */
    public ConstraintHandlerImpl() {
        treeToDnfConverter = new TreeToDnfConverterImpl();
        actionCreator = new ActionCreatorImpl();
        conditionCreator = new ConditionCreatorImpl();
        dnfToTreeConverter = new DnfToTreeConverterImpl();
    }

    @Override
    public void handleOwnConstraints(FeatureModel featureModel, IDecisionModel decisionModel) {
        //Bring constraints into normalised form
        List<ImplicationConstraint> implicationConstraints = normaliseConstraints(featureModel);

        //Create rules from the implications
        List<Rule> rules = createRules(decisionModel, implicationConstraints);

        //Distribute the created rules to the decisions
        distributeRules(rules);
    }

    /**
     * Converts the {@link Constraint}s of the given FM in a normalised form. The form follows this pattern:
     * {@code !(A & ... & Z) => ALPHA}.
     */
    private List<ImplicationConstraint> normaliseConstraints(FeatureModel featureModel) {
        List<ImplicationConstraint> implicationConstraints = new ArrayList<>();

        for (Constraint constraint : getSanitasiedConstraints(featureModel)) {
            List<List<Constraint>> dnf = treeToDnfConverter.convertToDnf(constraint);

            if (1 == dnf.size()) { // DNF contains no OR
                //TODO
                throw new RuntimeException("DNF to short");
            } else { //Contains at least one OR
                List<Constraint> rightSide = dnf.getLast();
                List<List<Constraint>> leftSide = new ArrayList<>(dnf);
                leftSide.remove(rightSide);

                //Convert dnf to several implications
                for (Constraint right : rightSide) {
                    implicationConstraints.add(new ImplicationConstraint(createLeft(leftSide), right));
                }
            }
        }
        return implicationConstraints;
    }

    /** Converts the given left side of the DNF to a tree and removes double negation if needed. */
    private Constraint createLeft(List<List<Constraint>> leftSide) {
        Constraint left = new NotConstraint(dnfToTreeConverter.createDnfFromList(leftSide));
        if (((NotConstraint) left).getContent() instanceof NotConstraint) {
            left = ((NotConstraint) ((NotConstraint) left).getContent()).getContent();
        }
        return left;
    }

    /**
     * If the {@link Constraint}s in the given  {@link FeatureModel} has {@link AndConstraint}s as roots, then split
     * them up into several {@link Constraint}s. E.g.: {@code (A => B) & (C => D) ~> A => B, C => D}
     *
     * @return {@link List} of all constraints in the given {@link FeatureModel} where no root {@link Constraint} is a
     * {@link AndConstraint}
     */
    private static List<Constraint> getSanitasiedConstraints(FeatureModel featureModel) {
        Stack<Constraint> stack = new Stack<>();
        featureModel.getConstraints().reversed().forEach(stack::push);
        List<Constraint> sanitisedConstrains = new ArrayList<>();
        while (!stack.empty()) {
            Constraint current = stack.pop();
            if (current instanceof AndConstraint andConstraint) {
                stack.push(andConstraint.getLeft());
                stack.push(andConstraint.getRight());
            } else {
                sanitisedConstrains.add(current);
            }
        }
        return sanitisedConstrains;
    }

    /** Create {@link Rule}s for the DM from the normalised {@link Constraint}s from the FM. */
    private List<Rule> createRules(IDecisionModel decisionModel, List<ImplicationConstraint> implicationConstraints) {
        List<Rule> rules = new ArrayList<>();
        for (ImplicationConstraint implicationConstraint : implicationConstraints) {
            ICondition condition = conditionCreator.createCondition(decisionModel, implicationConstraint.getLeft());
            IAction action = actionCreator.createAction(decisionModel, implicationConstraint.getRight());
            rules.add(new Rule(condition, action));
        }
        return rules;
    }

    /** Distribute the generated rules to the decisions */
    private void distributeRules(List<Rule> rules) {
        rules.forEach(rule -> rule.getAction().getVariable().addRule(rule));
    }
}