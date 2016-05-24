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

/**
 * OwnerCurator
 */
public class OwnerProductLinkCurator extends AbstractHibernateCurator<OwnerProductLink> {

    public OwnerProductLinkCurator() {
        super(OwnerProductLink.class);
    }
    
    @Transactional
    public Long getOwnerCount(Product p){
        return getEntityManager()
            .createQuery("SELECT count(op) FROM OwnerProductLink op " +
                "WHERE op.product = :product", 
                Long.class)
            .setParameter("product", p)
            .getSingleResult() ;
     }
    
    @Transactional
    public void deleteLinksToForProduct(Product p) {
        getEntityManager().createQuery(
            "DELETE FROM OwnerProductLink op " 
                 + "WHERE op.product = :product")
            .setParameter("product", p).executeUpdate();
    }
    
    @Transactional
    @Override
    public OwnerProductLink create(OwnerProductLink entity) {
        return super.create(entity);
    }
    
    @Transactional
    public void addOwnerToProduct(Product p, Owner o){
        if (!ownerHasProduct(o, p)){
            create(new OwnerProductLink(o.getId(),p.getUuid()));
        }
        
    }
    public boolean ownerHasProduct(Owner o, Product p) {
        return getEntityManager()
        .createQuery("SELECT count(op) FROM OwnerProductLink op WHERE op.owner = :owner AND op.product = :product", 
            Long.class)
        .setParameter("owner", o)
        .setParameter("product", p)
        .getSingleResult() > 0;
    }
}
