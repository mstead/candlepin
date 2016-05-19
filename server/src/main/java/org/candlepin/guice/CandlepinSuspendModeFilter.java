package org.candlepin.guice;

import org.candlepin.common.exceptions.ForbiddenException;
import org.candlepin.controller.ModeManager;
import org.candlepin.model.CandlepinMode.Mode;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

@Singleton
public class CandlepinSuspendModeFilter  implements Filter {
    private ModeManager modeManager;

    @Inject
    public CandlepinSuspendModeFilter(ModeManager modeManager) {
        this.modeManager = modeManager;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
        throws IOException, ServletException {

        Mode cur = modeManager.getCurrentMode();
        if (cur == Mode.SUSPEND) {
            HttpServletResponse httpResp = (HttpServletResponse)response;
            httpResp.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, "Candlepin in Suspend Mode");
            return;
        }
                
        chain.doFilter(request, response);
    }

    @Override
    public void init(FilterConfig arg0) throws ServletException {
    }

    @Override
    public void destroy() {
    }

}
