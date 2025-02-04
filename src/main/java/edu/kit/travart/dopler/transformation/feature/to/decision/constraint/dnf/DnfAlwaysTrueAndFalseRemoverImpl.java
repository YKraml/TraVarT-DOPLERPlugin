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
package edu.kit.travart.dopler.transformation.feature.to.decision.constraint.dnf;

import de.vill.model.FeatureModel;
import de.vill.model.constraint.Constraint;
import de.vill.model.constraint.LiteralConstraint;
import de.vill.model.constraint.NotConstraint;
import edu.kit.travart.dopler.transformation.exceptions.ConjunctionAlwaysTrueException;
import edu.kit.travart.dopler.transformation.exceptions.ConjunctionAlwaysFalseException;
import edu.kit.travart.dopler.transformation.exceptions.DnfAlwaysFalseException;
import edu.kit.travart.dopler.transformation.exceptions.DnfAlwaysTrueException;
import edu.kit.travart.dopler.transformation.util.FeatureFinder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Implementation of {@link DnfAlwaysTrueAndFalseRemover}. */
public class DnfAlwaysTrueAndFalseRemoverImpl implements DnfAlwaysTrueAndFalseRemover {

    private final FeatureFinder featureFinder;

    /**
     * Constructor of {@link DnfAlwaysTrueAndFalseRemoverImpl}.
     *
     * @param featureFinder {@link FeatureFinder}
     */
    public DnfAlwaysTrueAndFalseRemoverImpl(FeatureFinder featureFinder) {
        this.featureFinder = featureFinder;
    }

    @Override
    public List<List<Constraint>> removeAlwaysTruOrFalseConstraints(FeatureModel featureModel,
                                                                    List<List<Constraint>> dnf)
            throws DnfAlwaysFalseException, DnfAlwaysTrueException {
        List<List<Constraint>> newDnf = new ArrayList<>();

        //Create copy of DNF but remove unnecessary variables from the conjunctions
        for (List<Constraint> conjunction : dnf) {
            try {
                newDnf.add(createNewConjunction(featureModel, conjunction));
            } catch (ConjunctionAlwaysTrueException e) {
                throw new DnfAlwaysTrueException(e, dnf);
            } catch (ConjunctionAlwaysFalseException e) {
                //Don't add conjunction to the new DNF
            }
        }

        //If the DNF is empty, then the DNF is always false
        if (newDnf.isEmpty()) {
            throw new DnfAlwaysFalseException(dnf);
        }

        return newDnf;
    }

    private List<Constraint> createNewConjunction(FeatureModel featureModel, List<Constraint> conjunction)
            throws ConjunctionAlwaysTrueException, ConjunctionAlwaysFalseException {
        List<Constraint> newConjunction = new ArrayList<>();

        for (Constraint constraint : conjunction) {
            Optional<? extends Constraint> parent = getNonMandatoryParent(featureModel, constraint);

            if (constraint instanceof NotConstraint) {
                //If parent was not found, then conjunction is always false and is not added to the new DNF
                if (parent.isEmpty()) {
                    throw new ConjunctionAlwaysFalseException(conjunction);
                } else {
                    newConjunction.add(new NotConstraint(parent.get()));
                }
            } else if (constraint instanceof LiteralConstraint) {
                //If parent was not found, constraint is always true and does not need to be added to the conjunction
                parent.ifPresent(newConjunction::add);
            } else {
                newConjunction.add(constraint);
            }
        }

        //If the conjunction does not contain any variables, then the conjunction is always true
        if (newConjunction.isEmpty()) {
            throw new ConjunctionAlwaysTrueException(conjunction);
        }

        return newConjunction;
    }

    private Optional<? extends Constraint> getNonMandatoryParent(FeatureModel featureModel, Constraint constraint) {
        return switch (constraint) {
            case NotConstraint notConstraint when notConstraint.getContent() instanceof LiteralConstraint inner ->
                    featureFinder.findFirstNonMandatoryParent(featureModel, inner);
            case LiteralConstraint literalConstraint ->
                    featureFinder.findFirstNonMandatoryParent(featureModel, literalConstraint);
            case null, default -> Optional.ofNullable(constraint);
        };
    }
}
