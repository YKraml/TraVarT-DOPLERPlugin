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
package edu.kit.travart.dopler.transformation.decision.to.feature.rules;

import de.vill.model.Feature;
import de.vill.model.constraint.AndConstraint;
import de.vill.model.constraint.Constraint;
import de.vill.model.constraint.LiteralConstraint;
import de.vill.model.constraint.NotConstraint;
import edu.kit.dopler.model.BooleanEnforce;
import edu.kit.dopler.model.BooleanValue;
import edu.kit.dopler.model.DisAllows;
import edu.kit.dopler.model.EnumEnforce;
import edu.kit.dopler.model.IAction;
import edu.kit.travart.dopler.transformation.exceptions.FeatureNotPresentException;
import edu.kit.travart.dopler.transformation.exceptions.UnexpectedTypeException;
import edu.kit.travart.dopler.transformation.util.FeatureFinder;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** Implementation of {@link ActionHandler}. */
public class ActionHandlerImpl implements ActionHandler {

    private final FeatureFinder featureFinder;

    /**
     * Constructor of {@link ActionHandlerImpl}.
     *
     * @param featureFinder {@link FeatureFinder}
     */
    public ActionHandlerImpl(FeatureFinder featureFinder) {
        this.featureFinder = featureFinder;
    }

    /** Create recursively a constraint from the actions. */
    @Override
    public Constraint createRight(Feature root, Set<IAction> actions) {
        List<IAction> sortedActions = new ArrayList<>(actions);
        sortedActions.sort(Comparator.comparing(Object::toString));

        IAction firstAction = sortedActions.stream().findFirst().orElseThrow();

        if (1 == sortedActions.size()) {
            return createRightLiteral(root, firstAction);
        }

        sortedActions.remove(firstAction);
        return new AndConstraint(createRightLiteral(root, firstAction),
                createRight(root, new LinkedHashSet<>(sortedActions)));
    }

    /** Create a single constraint from the given {@link IAction}. */
    private Constraint createRightLiteral(Feature root, IAction action) {
        //TODO: Probably some cases are missing here
        return switch (action) {
            case BooleanEnforce booleanEnforce -> handleBooleanEnforce(root, booleanEnforce);
            case EnumEnforce enumEnforce -> handleEnumEnforce(root, enumEnforce);
            case DisAllows disAllows -> handleDisAllows(root, disAllows);
            case null, default -> throw new UnexpectedTypeException(action);
        };
    }

    private NotConstraint handleDisAllows(Feature root, DisAllows disAllows) {
        String displayId = disAllows.getDisAllowValue().getValue().toString();
        Optional<Feature> feature = featureFinder.findFeatureByName(root, displayId);

        if (feature.isEmpty()) {
            throw new FeatureNotPresentException(displayId);
        }

        return new NotConstraint(new LiteralConstraint(feature.get().getFeatureName()));
    }

    private LiteralConstraint handleEnumEnforce(Feature root, EnumEnforce enumEnforce) {
        String value = enumEnforce.getValue().toString();
        Optional<Feature> feature = featureFinder.findFeatureByName(root, value);

        if (feature.isEmpty()) {
            throw new FeatureNotPresentException(value);
        }

        return new LiteralConstraint(feature.get().getFeatureName());
    }

    private Constraint handleBooleanEnforce(Feature root, BooleanEnforce booleanEnforce) {
        String displayId = booleanEnforce.getDecision().getDisplayId();
        Optional<Feature> feature = featureFinder.findFeatureByName(root, displayId);

        if (feature.isEmpty()) {
            throw new FeatureNotPresentException(displayId);
        }

        if (booleanEnforce.getValue() instanceof BooleanValue booleanValue && booleanValue.getValue()) {
            //decision = true
            return new LiteralConstraint(feature.get().getFeatureName());
        } else {
            //decision = false
            return new NotConstraint(new LiteralConstraint(feature.get().getFeatureName()));
        }
    }
}
