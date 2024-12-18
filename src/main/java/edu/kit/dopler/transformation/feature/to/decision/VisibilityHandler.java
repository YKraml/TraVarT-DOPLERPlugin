package edu.kit.dopler.transformation.feature.to.decision;

import at.jku.cps.travart.core.common.IModelTransformer;
import de.vill.model.Feature;
import de.vill.model.FeatureModel;
import edu.kit.dopler.model.BooleanLiteralExpression;
import edu.kit.dopler.model.Dopler;
import edu.kit.dopler.model.IExpression;

/** This interface is responsible for creating the visibility expressions for the decisions. */
public interface VisibilityHandler {

    /**
     * Resolves the visibility of a decision. The visibility depends on the parent feature of the group from which the
     * decision originates. If the parent feature is the root of the {@link FeatureModel}, a
     * {@link BooleanLiteralExpression} with the value {@code true} is returned.
     */
    IExpression resolveVisibility(FeatureModel featureModel, Dopler decisionModel, Feature feature,
                                  IModelTransformer.STRATEGY level);

    IExpression resolveVisibilityForTypeDecisions(Dopler dopler, FeatureModel featureModel, Feature feature, String id,
                                                  IModelTransformer.STRATEGY level);
}
