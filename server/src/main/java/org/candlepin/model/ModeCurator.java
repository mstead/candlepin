/**
 * Copyright (c) 2009 - 2012 Red Hat, Inc.
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
package org.candlepin.model;

import com.google.inject.persist.Transactional;

import org.hibernate.criterion.Restrictions;

import java.util.List;

/**
 * Subscription manager.
 */
public class ModeCurator
    extends AbstractHibernateCurator<CandlepinMode> {

    public ModeCurator() {
        super(CandlepinMode.class);
    }

    /**
     * Return CDN for the given label.
     * @param label CDN label
     * @return CDN whose label matches the given value.
     */
    public Cdn lookupByLabel(String label) {
        return (Cdn) currentSession()
            .createCriteria(Cdn.class)
            .add(Restrictions.eq("label", label)).uniqueResult();
    }

    /**
     * Return a list of the CDN's known .
     *
     * @return a list of CDN's
     */
    @SuppressWarnings("unchecked")
    public List<Cdn> list() {
        return currentSession().createCriteria(Cdn.class).list();
    }

    @Transactional
    public CandlepinMode findLastMode() {
        List<CandlepinMode> mode = getEntityManager()
            .createQuery("SELECT m FROM CandlepinMode m ORDER BY m.changeTime DESC"
                , CandlepinMode.class)
        .setMaxResults(1).getResultList();
        
        if (mode.isEmpty())
            return null;
        
        return mode.get(0);
    }

    public int findLastModeActiveTime() {
        // TODO Auto-generated method stub
        return 0;
    }

}
