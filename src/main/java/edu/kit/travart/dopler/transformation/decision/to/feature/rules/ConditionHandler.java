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

import de.vill.model.constraint.Constraint;
import de.vill.model.constraint.ImplicationConstraint;
import edu.kit.dopler.model.IExpression;
import edu.kit.dopler.model.Rule;

import java.util.Optional;

/** This interface is responsible for translating the condition of a given {@link Rule}. */
public interface ConditionHandler {

    /**
     * Translates the given condition from the left side of a rule and creates a {@link Constraint} with it. This
     * {@link Constraint} will be the left side of a {@link ImplicationConstraint}.
     *
     * @param condition Condition to translate.
     *
     * @return {@link Optional} of the translated {@link Constraint}. An empty {@link Optional} means, that the
     * condition is always true.
     */
    Optional<Constraint> handleCondition(IExpression condition);
}
