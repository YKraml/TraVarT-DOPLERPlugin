/*******************************************************************************
 * SPDX-License-Identifier: MPL-2.0
 * <p>
 * This Source Code Form is subject to the terms of the Mozilla
 * Public License, v. 2.0. If a copy of the MPL was not distributed
 * with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 * <p>
 * Contributors:
 *    @author Yannick Kraml
 *    @author Kevin Feichtinger
 * <p>
 * Copyright 2024 Karlsruhe Institute of Technology (KIT)
 * KASTEL - Dependability of Software-intensive Systems
 *******************************************************************************/
package edu.kit.travart.dopler.transformation.feature.to.decision;

import at.jku.cps.travart.core.common.IModelTransformer;
import de.vill.model.Feature;
import de.vill.model.FeatureModel;
import de.vill.model.FeatureType;
import de.vill.model.Group;
import edu.kit.dopler.model.BooleanDecision;
import edu.kit.dopler.model.Dopler;
import edu.kit.dopler.model.Enumeration;
import edu.kit.dopler.model.EnumerationDecision;
import edu.kit.dopler.model.EnumerationLiteral;
import edu.kit.dopler.model.NumberDecision;
import edu.kit.dopler.model.StringDecision;
import edu.kit.travart.dopler.transformation.exceptions.UnexpectedTypeException;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

/** Implementation of {@link FeatureAndGroupHandler} */
public class FeatureAndGroupHandlerImpl implements FeatureAndGroupHandler {

    //The question are not private, because there are other locations in the code, where decisions are created.
    static final String BOOLEAN_QUESTION = "%s?";
    static final String STRING_QUESTION = "What %s?";
    static final String NUMBER_QUESTION = "How much %s?";
    private static final String ENUM_QUESTION = "Which %s?";
    private final VisibilityHandler visibilityHandler;
    private final IdHandler idHandler;

    /**
     * Constructor of {@link FeatureAndGroupHandlerImpl}
     *
     * @param visibilityHandler {@link VisibilityHandler}
     * @param idHandler         {@link IdHandler}
     */
    public FeatureAndGroupHandlerImpl(VisibilityHandler visibilityHandler, IdHandler idHandler) {
        this.visibilityHandler = visibilityHandler;
        this.idHandler = idHandler;
    }

    /** Checks all groups of the given feature. */
    @Override
    public void handleFeature(FeatureModel featureModel, Dopler decisionModel, Feature feature,
                              IModelTransformer.STRATEGY level) {

        //Create decisions for types
        if (null != feature.getFeatureType()) {
            switch (feature.getFeatureType()) {
                case STRING -> createStringDecision(decisionModel, featureModel, feature, level);
                case INT, REAL -> createNumberDecision(decisionModel, featureModel, feature, level);
                case BOOL -> {
                    //Do nothing because BOOL is standard type
                }
                default -> throw new UnexpectedTypeException(feature.getFeatureType());
            }
        }

        //Go through all children
        for (Group group : feature.getChildren()) {
            handleGroup(featureModel, decisionModel, group, level);
        }
    }

    /** Create a decision from the given group. Also check all features inside the group for more groups. */
    public void handleGroup(FeatureModel featureModel, Dopler decisionModel, Group group,
                            IModelTransformer.STRATEGY level) {
        switch (group.GROUPTYPE) {
            case OR -> handleOrGroup(featureModel, decisionModel, group, level);
            case ALTERNATIVE -> handleAlternativeGroup(group, decisionModel, featureModel, level);
            case MANDATORY -> handleMandatoryGroup(featureModel, group, decisionModel, level);
            case OPTIONAL -> handleOptionalGroup(group, decisionModel, featureModel, level);
            default -> throw new UnexpectedTypeException(group.GROUPTYPE);
        }

        //Handle features of group
        for (Feature feature : group.getFeatures()) {
            handleFeature(featureModel, decisionModel, feature, level);
        }
    }

    /** Create a single decision for the or-group */
    private void handleOrGroup(FeatureModel featureModel, Dopler decisionModel, Group group,
                               IModelTransformer.STRATEGY level) {
        createEnumDecision(decisionModel, group, featureModel, group.getFeatures().size(), level);
    }

    /** Create a single decision for the alternative-group */
    private void handleAlternativeGroup(Group group, Dopler decisionModel, FeatureModel featureModel,
                                        IModelTransformer.STRATEGY level) {
        createEnumDecision(decisionModel, group, featureModel, 1, level);
    }

    /** Create a decision for each child in the optional-group */
    private void handleOptionalGroup(Group group, Dopler dopler, FeatureModel featureModel,
                                     IModelTransformer.STRATEGY level) {
        for (Feature feature : group.getFeatures()) {
            //Only create decision if it has not feature-type or the feature type is boolean
            if (null == feature.getFeatureType() || FeatureType.BOOL == feature.getFeatureType()) {
                createBooleanDecision(dopler, featureModel, feature, level);
            }
        }
    }

    /** Create a decision for each child in the mandatory-group */
    private void handleMandatoryGroup(FeatureModel featureModel, Group group, Dopler dopler,
                                      IModelTransformer.STRATEGY level) {
        if (IModelTransformer.STRATEGY.ROUNDTRIP != level) {
            return;
        }

        for (Feature feature : group.getFeatures()) {
            //Only create decision if it has no feature-type or the feature type is boolean
            if (null == feature.getFeatureType() || FeatureType.BOOL == feature.getFeatureType()) {
                createMandatoryDecision(dopler, featureModel, feature, level);
            }
        }
    }

    private void createMandatoryDecision(Dopler dopler, FeatureModel featureModel, Feature feature,
                                         IModelTransformer.STRATEGY level) {
        dopler.addDecision(new EnumerationDecision("%s#".formatted(idHandler.resolveId(dopler, feature)),
                String.format(ENUM_QUESTION, idHandler.resolveId(dopler, feature)), "",
                visibilityHandler.resolveVisibility(featureModel, dopler, feature.getParentFeature(), level),
                new LinkedHashSet<>(),
                new Enumeration(Set.of(new EnumerationLiteral(idHandler.resolveId(dopler, feature)))), 1, 1));
    }

    private void createBooleanDecision(Dopler dopler, FeatureModel featureModel, Feature feature,
                                       IModelTransformer.STRATEGY level) {
        String id = idHandler.resolveId(dopler, feature);
        dopler.addDecision(new BooleanDecision(id, String.format(BOOLEAN_QUESTION, id), "",
                visibilityHandler.resolveVisibility(featureModel, dopler, feature.getParentFeature(), level),
                new LinkedHashSet<>()));
    }

    private void createNumberDecision(Dopler dopler, FeatureModel featureModel, Feature feature,
                                      IModelTransformer.STRATEGY level) {
        String id = idHandler.resolveId(dopler, feature);
        dopler.addDecision(new NumberDecision(id, String.format(NUMBER_QUESTION, feature.getFeatureName()), "",
                visibilityHandler.resolveVisibilityForTypeDecisions(dopler, featureModel, feature, id, level),
                new LinkedHashSet<>(), new HashSet<>()));
    }

    private void createStringDecision(Dopler dopler, FeatureModel featureModel, Feature feature,
                                      IModelTransformer.STRATEGY level) {
        String id = idHandler.resolveId(dopler, feature);
        dopler.addDecision(new StringDecision(id, String.format(STRING_QUESTION, feature.getFeatureName()), "",
                visibilityHandler.resolveVisibilityForTypeDecisions(dopler, featureModel, feature, id, level),
                new LinkedHashSet<>(), new HashSet<>()));
    }

    private void createEnumDecision(Dopler decisionModel, Group group, FeatureModel featureModel, int maxCardinality,
                                    IModelTransformer.STRATEGY level) {
        decisionModel.addDecision(new EnumerationDecision(idHandler.resolveId(decisionModel, group.getParentFeature()),
                String.format(ENUM_QUESTION, idHandler.resolveId(decisionModel, group.getParentFeature())), "",
                visibilityHandler.resolveVisibility(featureModel, decisionModel, group.getParentFeature(), level),
                new LinkedHashSet<>(), new Enumeration(
                group.getFeatures().stream().map(Feature::getFeatureName).map(EnumerationLiteral::new)
                        .collect(Collectors.toCollection(LinkedHashSet::new))), 1, maxCardinality));
    }
}
