package at.jku.cps.travart.dopler.transformation.oneway.feature.to.decision.constraint.dnf.rule;

import de.vill.model.constraint.Constraint;

import java.util.Optional;

/**
 * Interface for a single rule for the DNF algorithm.
 */
public interface Rule {

    /**
     * Check if the {@link Rule} matches the given {@link Constraint}. If the {@link Constraint} matches, then return a
     * new {@link Constraint} that replaces the old one. Both are semantically equal.
     *
     * @param constraint {@link Constraint} to replace (if {@link Rule} matches)
     *
     * @return New {@link Constraint} that replaces the old one
     */
    Optional<Constraint> replace(Constraint constraint);
}