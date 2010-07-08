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
package org.fedoraproject.candlepin.model.test;

import static org.junit.Assert.assertEquals;

import org.fedoraproject.candlepin.model.ProductAttribute;
import org.fedoraproject.candlepin.test.DatabaseTestFixture;
import org.junit.Test;

public class AttributeTest extends DatabaseTestFixture {

    @Test
    public void testLookup() {
        ProductAttribute newAttr = new ProductAttribute("OwesUsMoney_100", "100");
        attributeCurator.create(newAttr);

        ProductAttribute foundAttr = attributeCurator.find(newAttr.getId());
        assertEquals(newAttr.getName(), foundAttr.getName());
        assertEquals(newAttr.getValue(), foundAttr.getValue());
    }

}
