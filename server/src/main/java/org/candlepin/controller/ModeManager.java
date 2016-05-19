package org.candlepin.controller;

import org.candlepin.model.CandlepinMode.Mode;

public interface ModeManager {

    Mode getCurrentMode();
    void enterMode(Mode m, String reason);
    void throwRestEasyExceptionIfInSuspendMode();
    void registerModeChangeListener(ModeChangeListener list);
}
