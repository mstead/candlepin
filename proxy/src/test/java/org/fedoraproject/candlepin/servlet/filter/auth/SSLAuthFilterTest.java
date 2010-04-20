/**
 * Copyright (c) 2009 Red Hat, Inc.
 *
 * This software is licensed to you under the GNU General Public License,
 * version 2 (GPLv2). There is NO WARRANTY for this software, express or
 * implied, including the implied warranties of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. You should have received a copy of GPLv2
 * along with this software; if not, see
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.txt.
 *
 * Red Hat trademarks are not licensed under GPLv2. No permission is
 * granted to use or replicate Red Hat trademarks that are incorporated
 * in this software or its documentation.
 */
package org.fedoraproject.candlepin.servlet.filter.auth;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.fedoraproject.candlepin.auth.ConsumerPrincipal;
import org.fedoraproject.candlepin.model.Consumer;
import org.fedoraproject.candlepin.model.ConsumerCurator;
import org.fedoraproject.candlepin.model.ConsumerType;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.security.Principal;
import java.security.cert.X509Certificate;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class SSLAuthFilterTest {

    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;
    @Mock private FilterChain chain;
    @Mock private ConsumerCurator consumerCurator;

    private SSLAuthFilter filter;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        this.filter = new SSLAuthFilter(this.consumerCurator);
    }

    /**
     * No cert
     *
     * @throws Exception
     */
    @Test
    public void noCert() throws Exception {
        this.filter.doFilter(request, response, chain);

        verify(request, never()).setAttribute(eq(FilterConstants.PRINCIPAL_ATTR),
                any(Principal.class));
    }

    /**
     * Happy path - parses the username from the cert's DN correctly.
     *
     * @throws Exception
     */
    @Test
    public void correctUserName() throws Exception {
        Consumer consumer = new Consumer("machine_name", null,
                new ConsumerType(ConsumerType.SYSTEM));
        ConsumerPrincipal expected = new ConsumerPrincipal(consumer);

        String dn = "CN=machine_name, OU=someguy@itcenter.org, " +
            "O=Green Mountain, UID=453-44423-235";

        mockCert(dn);
        when(this.consumerCurator.lookupByUuid("453-44423-235")).thenReturn(consumer);
        this.filter.doFilter(request, response, chain);

        verify(request).setAttribute(FilterConstants.PRINCIPAL_ATTR, expected);
    }

    /**
     * DN is set but does not contain UID
     *
     * @throws Exception
     */
    @Test
    public void noUuidOnCert() throws Exception {
        mockCert("CN=something, OU=jimmy@ibm.com, O=IBM");
        when(this.consumerCurator.lookupByUuid(anyString())).thenReturn(
                new Consumer("machine_name", null, null));
        this.filter.doFilter(request, response, chain);

        verify(request, never()).setAttribute(eq(FilterConstants.PRINCIPAL_ATTR), 
                any(Principal.class));
    }

    /**
     * Uuid in the cert is not found by the curator.
     *
     * @throws Exception
     */
    @Test
    public void noValidConsumerEntity() throws Exception {
        mockCert("CN=my_box, OU=billy@jaspersoft.com, O=Jaspersoft, UID=235-8");
        when(this.consumerCurator.lookupByUuid("235-8")).thenReturn(null);
        this.filter.doFilter(request, response, chain);

        verify(request, never()).setAttribute(eq(FilterConstants.PRINCIPAL_ATTR),
                any(Principal.class));
    }


    private void mockCert(String dn) {
        X509Certificate idCert =  mock(X509Certificate.class);
        Principal principal = mock(Principal.class);

        when(principal.getName()).thenReturn(dn);
        when(idCert.getSubjectDN()).thenReturn(principal);
        when(this.request.getAttribute("javax.servlet.request.X509Certificate"))
                .thenReturn(new X509Certificate[]{idCert});
    }

}
