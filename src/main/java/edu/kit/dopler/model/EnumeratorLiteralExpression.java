/*******************************************************************************
 * This Source Code Form is subject to the terms of the Mozilla
 * Public License, v. 2.0. If a copy of the MPL was not distributed
 * with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 *
 * Contributors: 
 * 	@author Fabian Eger
 * 	@author Kevin Feichtinger
 *
 * Copyright 2024 Karlsruhe Institute of Technology (KIT)
 * KASTEL - Dependability of Software-intensive Systems
 * All rights reserved
 *******************************************************************************/
package edu.kit.dopler.model;

import edu.kit.dopler.exceptions.InvalidTypeInLiteralExpressionCheckException;

import java.util.Objects;
import java.util.stream.Stream;

public class EnumeratorLiteralExpression extends LiteralExpression {

	private EnumerationLiteral enumerationLiteral;

	public EnumeratorLiteralExpression(EnumerationLiteral literal) {
		this.enumerationLiteral = literal;
	}

	@Override
	public boolean evaluate() {
		return false;
	}

	@Override
	public void toSMTStream(Stream.Builder<String> builder, String callingDecisionConst) {
		builder.add(" " + enumerationLiteral.getValue() + " ");
	}

	/**
	 * This methode is implemented for every LiteralExpression to check the equality
	 * in the EQUALS expression
	 * 
	 * @param value the value which need to be compared to the literal
	 * @return returns a boolean if the values are equal
	 * @throws InvalidTypeInLiteralExpressionCheckException is thrown when the value
	 *                                                      is not of type
	 *                                                      EnumerationLiteral
	 */
	@Override
	boolean equalsForLiteralExpressions(IValue<?> value) throws InvalidTypeInLiteralExpressionCheckException {
		if (value instanceof EnumerationLiteral) {
			return Objects.equals(enumerationLiteral.getValue(), ((EnumerationLiteral) value).getValue());
		} else {
			throw new InvalidTypeInLiteralExpressionCheckException("Parameter was not of Type StringValue in Equals");
		}
	}

	public EnumerationLiteral getEnumerationLiteral() {
		return enumerationLiteral;
	}

	@Override
	public String toString() {
		return enumerationLiteral.getValue();
	}
}