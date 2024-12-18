package edu.kit.dopler.transformation.feature.to.decision;

import at.jku.cps.travart.core.common.IModelTransformer;
import com.google.inject.Inject;
import de.vill.model.Feature;
import de.vill.model.FeatureModel;
import edu.kit.dopler.model.Dopler;
import edu.kit.dopler.transformation.feature.to.decision.constraint.ConstraintHandler;

/** Implementation of {@link FmToDmTransformer} */
public class FmToDmTransformerImpl implements FmToDmTransformer {

    private final FeatureAndGroupHandler featureAndGroupHandler;
    private final ConstraintHandler constraintHandler;
    private final AttributeHandler attributeHandler;

    /**
     * Constructor of {@link FmToDmTransformerImpl}
     */
    @Inject
    public FmToDmTransformerImpl(FeatureAndGroupHandler featureAndGroupHandler, ConstraintHandler constraintHandler,
                                 AttributeHandler attributeHandler) {
        this.featureAndGroupHandler = featureAndGroupHandler;
        this.constraintHandler = constraintHandler;
        this.attributeHandler = attributeHandler;
    }

    @Override
    public Dopler transform(FeatureModel featureModel, String modelName, IModelTransformer.STRATEGY level) {
        Dopler decisionModel = new Dopler();
        decisionModel.setName(modelName);

        Feature rootFeature = featureModel.getRootFeature();
        featureAndGroupHandler.handleFeature(rootFeature, decisionModel, featureModel, level);
        attributeHandler.handleAttributes(decisionModel, featureModel, level);
        constraintHandler.handleOwnConstraints(featureModel, decisionModel);

        return decisionModel;
    }
}
