package edu.kit.dopler.transformation.feature.to.decision.constraint;

import com.google.inject.Inject;
import de.vill.model.constraint.Constraint;
import de.vill.model.constraint.LiteralConstraint;
import de.vill.model.constraint.NotConstraint;
import edu.kit.dopler.model.*;
import edu.kit.dopler.transformation.exceptions.DecisionNotPresentException;
import edu.kit.dopler.transformation.exceptions.UnexpectedTypeException;
import edu.kit.dopler.transformation.util.DecisionFinder;
import edu.kit.dopler.transformation.util.FeatureFinder;

import java.util.Optional;

/**
 * Implementation of {@link ActionCreator}
 */
public class ActionCreatorImpl implements ActionCreator {

    private final DecisionFinder decisionFinder;

    @Inject
    public ActionCreatorImpl(DecisionFinder decisionFinder) {
        this.decisionFinder = decisionFinder;
    }

    @Override
    public IAction createAction(Dopler decisionModel, Constraint constraint) {
        return switch (constraint) {
            case LiteralConstraint literalConstraint -> handleLiteral(decisionModel, literalConstraint);
            case NotConstraint notConstraint -> handleNot(decisionModel, notConstraint);
            case null, default -> throw new UnexpectedTypeException(constraint);
        };
    }

    private IAction handleLiteral(Dopler decisionModel, LiteralConstraint literalConstraint) {
        String literal = literalConstraint.getLiteral();
        Optional<IDecision<?>> decisionById = decisionFinder.findDecisionById(decisionModel, literal);
        Optional<IDecision<?>> decisionByValue = decisionFinder.findDecisionByValue(decisionModel, literal);

        IAction action;
        if (decisionByValue.isPresent()) {
            action = new EnumEnforce(decisionByValue.get(), new StringValue(literal));
        } else if (decisionById.isPresent()) {
            action = switch (decisionById.get().getDecisionType()) {
                case BOOLEAN ->
                        new BooleanEnforce((BooleanDecision) decisionById.get(), new BooleanValue(Boolean.TRUE));
                case ENUM -> new EnumEnforce(decisionById.get(), new StringValue(literal));
                case null, default -> throw new UnexpectedTypeException(decisionById.get().getDecisionType());
            };
        } else {
            throw new DecisionNotPresentException(literal);
        }

        return action;
    }

    private IAction handleNot(Dopler decisionModel, NotConstraint notConstraint) {

        //NotConstraint should contain a literal (thanks to working on DNFs)
        if (!(notConstraint.getContent() instanceof LiteralConstraint literalConstraint)) {
            throw new UnexpectedTypeException(notConstraint.getContent());
        }

        String literal = literalConstraint.getLiteral();
        Optional<IDecision<?>> decisionById = decisionFinder.findDecisionById(decisionModel, literal);
        Optional<IDecision<?>> decisionByValue = decisionFinder.findDecisionByValue(decisionModel, literal);

        IAction action;
        if (decisionByValue.isPresent()) {
            action = new DisAllows(decisionByValue.get(), new StringValue(literal));
        } else if (decisionById.isPresent()) {
            action = switch (decisionById.get().getDecisionType()) {
                case BOOLEAN -> new BooleanEnforce((BooleanDecision) decisionById.get(), BooleanValue.getFalse());
                case null, default -> throw new UnexpectedTypeException(decisionById.get().getDecisionType());
            };
        } else {
            throw new DecisionNotPresentException(literal);
        }
        return action;
    }
}
