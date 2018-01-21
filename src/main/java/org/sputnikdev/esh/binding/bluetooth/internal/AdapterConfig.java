package org.sputnikdev.esh.binding.bluetooth.internal;

import org.sputnikdev.bluetooth.manager.AdapterGovernor;

/**
 * Bluetooth adapter thing configuration.
 */
public class AdapterConfig {

    private Double signalPropagationExponent;

    /**
     * Returns signal propagation exponent. See {@link AdapterGovernor#getSignalPropagationExponent()}
     * @return signal propagation exponent
     */
    public Double getSignalPropagationExponent() {
        return signalPropagationExponent;
    }

    /**
     * Sets signal propagation exponent. See {@link AdapterGovernor#setSignalPropagationExponent(double)} ()}
     * @param signalPropagationExponent signal propagation exponent
     */
    public void setSignalPropagationExponent(Double signalPropagationExponent) {
        this.signalPropagationExponent = signalPropagationExponent;
    }

}
