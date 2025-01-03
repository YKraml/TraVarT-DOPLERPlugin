package edu.kit.dopler.transformation;

import at.jku.cps.travart.core.common.IModelTransformer;
import com.google.inject.Guice;
import com.google.inject.Injector;
import de.vill.model.FeatureModel;
import edu.kit.dopler.model.Dopler;
import edu.kit.dopler.transformation.decision.to.feature.DmToFmTransformer;
import edu.kit.dopler.transformation.decision.to.feature.DmToFmTransformerImpl;
import edu.kit.dopler.transformation.feature.to.decision.FmToDmTransformer;
import edu.kit.dopler.transformation.util.TransformationModule;

/**
 * Transforms {@link Dopler} models into {@link FeatureModel}s and vice versa.
 */
public class Transformer implements IModelTransformer<Dopler> {

    /**
     * This is the standard name for the root feature in the {@link FeatureModel}.
     */
    public static final String STANDARD_MODEL_NAME = "Root";

    private final DmToFmTransformer dmToFmTransformer;
    private final FmToDmTransformer fmToDmTransformer;

    /** Constructor of {@link Transformer} */
    public Transformer() {
        Injector injector = Guice.createInjector(new TransformationModule());
        dmToFmTransformer = injector.getInstance(DmToFmTransformerImpl.class);
        fmToDmTransformer = injector.getInstance(FmToDmTransformer.class);
    }

    @Override
    public FeatureModel transform(Dopler model, String modelName, STRATEGY level) {
        return dmToFmTransformer.transform(model, level);
    }

    @Override
    public Dopler transform(FeatureModel model, String modelName, STRATEGY level) {
        return fmToDmTransformer.transform(model, level);
    }
}
