/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
 * 
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 * 
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.beam.binning.operator.ui;

import org.esa.beam.binning.AggregatorDescriptor;

/**
* Simple configuration class containing a number of public final fields.
*
* @author Thomas Storm
*/
class VariableConfig {

    final String name;
    final String expression;
    final AggregatorDescriptor aggregator;
    final Double weight;
    final Double fillValue;

    VariableConfig(String name, String expression, AggregatorDescriptor aggregator, Double weight, Double fillValue) {
        this.expression = expression;
        this.fillValue = fillValue;
        this.weight = weight;
        this.aggregator = aggregator;
        this.name = name;
    }

    @Override
    public String toString() {
        return "VariableConfig{" +
               "aggregator=" + aggregator +
               ", name='" + name + '\'' +
               ", expression='" + expression + '\'' +
               ", weight=" + weight +
               ", fillValue=" + fillValue +
               '}';
    }
}
