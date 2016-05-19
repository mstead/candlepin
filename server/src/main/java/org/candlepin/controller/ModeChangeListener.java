package org.candlepin.controller;

import org.candlepin.model.CandlepinMode.Mode;

public interface ModeChangeListener {
    public void modeChanged(Mode newMode);
}
