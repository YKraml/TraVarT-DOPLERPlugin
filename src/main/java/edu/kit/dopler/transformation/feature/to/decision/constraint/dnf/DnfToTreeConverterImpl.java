package edu.kit.dopler.transformation.feature.to.decision.constraint.dnf;

import de.vill.model.constraint.AndConstraint;
import de.vill.model.constraint.Constraint;
import de.vill.model.constraint.OrConstraint;

import java.util.Comparator;
import java.util.List;

public class DnfToTreeConverterImpl implements DnfToTreeConverter {

    @Override
    public Constraint createDnfFromList(List<List<Constraint>> dnf) {

        //Sort dnf by name
        for (List<Constraint> constraints : dnf) {
            constraints.sort(Comparator.comparing(Constraint::toString));
        }

        //Sort dnf by size of conjunctions
        dnf.sort(Comparator.comparingInt(List::size));

        //If the dnf only contains one conjunction
        if (1 == dnf.size()) {
            List<Constraint> constraints = dnf.getFirst();
            return createConjunction(constraints);
        }

        //If the dnf contains several conjunctions
        Constraint lastOrConstraint = new OrConstraint(createConjunction(dnf.get(0)), createConjunction(dnf.get(1)));
        for (int i = 2; i < dnf.size(); i++) {
            lastOrConstraint = new OrConstraint(lastOrConstraint, createConjunction(dnf.get(i)));
        }
        return lastOrConstraint;
    }

    private static Constraint createConjunction(List<Constraint> constraints) {

        //If the conjunction only contains one literal
        if (1 == constraints.size()) {
            return constraints.getFirst();
        }

        //If the conjunction only contains several literal
        Constraint lastAndConstraint = new AndConstraint(constraints.get(0), constraints.get(1));
        for (int i = 2; i < constraints.size(); i++) {
            lastAndConstraint = new AndConstraint(lastAndConstraint, constraints.get(i));
        }

        return lastAndConstraint;
    }
}