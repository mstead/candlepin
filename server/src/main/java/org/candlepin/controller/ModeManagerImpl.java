package org.candlepin.controller;

import org.candlepin.common.exceptions.SuspendedException;
import org.candlepin.model.CandlepinMode;
import org.candlepin.model.CandlepinMode.Mode;
import org.candlepin.pinsetter.core.PinsetterKernel;
import org.candlepin.model.ModeCurator;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * This class drives Suspend Mode functionality. Suspend Mode is a 
 * mode where Candlepin stops serving requests and only responds to
 * Status resource.
 * 
 * Candlepin's mode is stored as a database flag because its necessary
 * to propagate the mode changes throughout the cluster.
 * 
 * Polling the database for mode change may become a performance
 * bottleneck. To mitigate it, this class also offers methods that
 * cache the current mode for a period of time, so that the database
 * is not hit too often. 
 * 
 * @author fnguyen
 *
 */
@Singleton
public class ModeManagerImpl implements ModeManager {
    private static Logger log = LoggerFactory.getLogger(ModeManagerImpl.class);
    private ModeCurator modeCurator;
    private Mode cachedMode = Mode.NORMAL;
    private long nextCheck = System.currentTimeMillis();
    private int checkIntervalSeconds = 10;
    private int minModeChangeIntervalSeconds = 10;
    private List<ModeChangeListener> listeners = new ArrayList<ModeChangeListener>();
    
    @Inject
    public ModeManagerImpl(ModeCurator modeCurator) {
        super();
        this.modeCurator = modeCurator;
    }

    @Override
    public Mode getCurrentMode() {
        log.debug("Retrieving current mode");
        if (System.currentTimeMillis() > nextCheck){
            log.debug("Refreshing mode cache");
            Mode newMode = getCurrentModeImmediate();
            nextCheck = System.currentTimeMillis() + 1000*checkIntervalSeconds;
            
            if (cachedMode != newMode){
                fireModeChangeEvent(newMode);
            }
            cachedMode = newMode;
        }
        
        return cachedMode;
    }
    
    @Override
    public void enterMode(Mode m, String reason) {
        log.debug("Entering new mode {} for reason {}", m, reason);
        Integer activeTime = getLastModeActiveTimeSeconds();
        if (activeTime != null &&
            activeTime < minModeChangeIntervalSeconds) {
            log.debug("Minimal interval for mode change has not elapsed, ignoring " +
                "new mode change request");
            return;
        }

        CandlepinMode cm = new CandlepinMode(new Date(), m, reason);
        modeCurator.create(cm);
        
        if (m != cachedMode)
            fireModeChangeEvent(m);
        
        cachedMode = m;    
    } 
    
    @Override
    public void throwRestEasyExceptionIfInSuspendMode() {
        if (getCurrentMode() == Mode.SUSPEND) {
            log.debug("Mode manager detected SUSPEND mode and will throw SuspendException");
            throw new SuspendedException("Candlepin is in Suspend Mode");
        }
    }

    @Override
    public void registerModeChangeListener(ModeChangeListener l) {
        log.debug("Registering ModeChangeListener {} ", l.getClass().getSimpleName());
        listeners.add(l);
    }
    
    private void fireModeChangeEvent(Mode newMode) {
        log.debug("Mode changed event fired {}", newMode);
        for (ModeChangeListener l : listeners)
            l.modeChanged(newMode);        
    }

    private Mode getCurrentModeImmediate() {
        //TODO 
        //   IF config.suspend mode disabled then return Mode.NORMAL
        
        CandlepinMode mode = modeCurator.findLastMode();
        if (mode == null)
            return Mode.NORMAL;
        else
            return mode.getMode();
    }

    private Integer getLastModeActiveTimeSeconds() {
        CandlepinMode mode = modeCurator.findLastMode();
        if (mode == null)
            return null;
        
        return (int) ((new Date().getTime()-mode.getChangeTime().getTime())/1000);
    }
}
