package edu.kit.dopler.transformation.feature.to.decision.constraint.dnf;

import de.vill.model.FeatureModel;
import de.vill.model.constraint.Constraint;
import edu.kit.dopler.transformation.exceptions.DnfAlwaysFalseException;
import edu.kit.dopler.transformation.exceptions.DnfAlwaysTrueException;

import java.util.List;

/** Removes redundant literals and conjunctions from the DNF. */
public interface DnfAlwaysTrueAndFalseRemover {

    /**
     * Removes conjunctions from the DNF that are always false and removes Literals from conjunctions that are always
     * true. Special cases exist, if the conjunctions in the DNF or the DNF itself become empty. E.g.: a mandatory
     * features that directly sits under the root feature is always taken. If a constraint contains exactly this
     * mandatory feature as a literals, then that literal is always true.
     *
     * @param featureModel {@link FeatureModel} that contains the constraint from which the given dnf was created
     * @param dnf          Constraint in DNF
     *
     * @return Constraint in DNF without redundancies
     *
     * @throws DnfAlwaysFalseException Gets thrown, when the DNF is always false
     * @throws DnfAlwaysTrueException  Gets thrown, when the DNF is always true
     */
    List<List<Constraint>> removeAlwaysTruOrFalseConstraints(FeatureModel featureModel, List<List<Constraint>> dnf)
            throws DnfAlwaysFalseException, DnfAlwaysTrueException;
}
